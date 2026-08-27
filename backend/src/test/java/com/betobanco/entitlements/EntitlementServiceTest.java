package com.betobanco.entitlements;

import com.betobanco.catalog.entity.Product;
import com.betobanco.catalog.repository.ProductRepository;
import com.betobanco.entitlements.api.EntitlementService;
import com.betobanco.entitlements.entity.Entitlement;
import com.betobanco.entitlements.repository.EntitlementRepository;
import com.betobanco.support.PostgresTestBase;
import com.betobanco.users.entity.User;
import com.betobanco.users.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class EntitlementServiceTest extends PostgresTestBase {

    @Autowired
    private EntitlementService entitlements;

    @Autowired
    private EntitlementRepository repo;

    @Autowired
    private PlatformTransactionManager txManager;

    @Autowired
    private UserRepository users;

    @Autowired
    private ProductRepository produtos;

    private UUID aluno(String email) {
        return users.saveAndFlush(new User(email, "{bcrypt}x", "Aluno")).getId();
    }

    private UUID produto(String sku) {
        return produtos.saveAndFlush(new Product(sku, "Mentoria " + sku, null, 19900L)).getId();
    }

    @Test
    void concederDuasVezesNaoDuplica() {
        UUID u = aluno("dup@ent.com");
        UUID p = produto("SKU-DUP");

        var primeira = entitlements.conceder(u, p, "PAYMENT", "pay-1");
        var segunda = entitlements.conceder(u, p, "PAYMENT", "pay-1");

        assertThat(primeira.criadoAgora()).isTrue();
        assertThat(segunda.criadoAgora()).isFalse();
        assertThat(segunda.entitlementId()).isEqualTo(primeira.entitlementId());
        assertThat(entitlements.listarDe(u)).hasSize(1);
    }

    @Test
    void concessaoConcorrenteDevolveAConcessaoVencedoraSemQuebrarATransacao() throws Exception {
        UUID u = aluno("corrida@ent.com");
        UUID p = produto("SKU-CORRIDA");

        // A vencedora insere e segura a transacao aberta; a perdedora chama
        // conceder(): a consulta inicial nao ve nada (insert nao commitado),
        // o insert bloqueia no indice unico ate a vencedora commitar e entao
        // colide — exatamente a corrida que o catch de conceder() recupera.
        CountDownLatch vencedoraInseriu = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        TransactionTemplate tx = new TransactionTemplate(txManager);

        Future<UUID> vencedora = pool.submit(() -> tx.execute(status -> {
            UUID id = repo.saveAndFlush(
                    new Entitlement(u, p, "PAYMENT", "pay-vencedora")).getId();
            vencedoraInseriu.countDown();
            try {
                // Janela para a perdedora consultar e ficar bloqueada no insert.
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
            }
            return id;
        }));

        Future<EntitlementService.Concessao> perdedora = pool.submit(() -> {
            vencedoraInseriu.await();
            return entitlements.conceder(u, p, "PAYMENT", "pay-perdedora");
        });

        UUID idVencedora = vencedora.get(15, TimeUnit.SECONDS);
        EntitlementService.Concessao concessao = perdedora.get(15, TimeUnit.SECONDS);
        pool.shutdown();

        assertThat(concessao.criadoAgora()).isFalse();
        assertThat(concessao.entitlementId()).isEqualTo(idVencedora);
        assertThat(entitlements.listarDe(u)).hasSize(1);
    }

    @Test
    void semConcessaoNaoHaAcesso() {
        UUID u = aluno("sem@ent.com");
        UUID p = produto("SKU-SEM");

        assertThat(entitlements.temAcesso(u, p)).isFalse();
    }

    @Test
    void concederDaAcessoERevogarTira() {
        UUID u = aluno("acesso@ent.com");
        UUID p = produto("SKU-ACESSO");

        entitlements.conceder(u, p, "PAYMENT", "pay-2");
        assertThat(entitlements.temAcesso(u, p)).isTrue();

        entitlements.revogar(u, p);
        assertThat(entitlements.temAcesso(u, p)).isFalse();
        assertThat(entitlements.listarDe(u)).isEmpty();
    }

    @Test
    void depoisDeRevogarEhPossivelConcederDeNovo() {
        UUID u = aluno("recompra@ent.com");
        UUID p = produto("SKU-RECOMPRA");

        entitlements.conceder(u, p, "PAYMENT", "pay-3");
        entitlements.revogar(u, p);

        // O indice unico e parcial (WHERE revoked_at IS NULL), entao uma
        // recompra depois do estorno funciona.
        var nova = entitlements.conceder(u, p, "PAYMENT", "pay-4");

        assertThat(nova.criadoAgora()).isTrue();
        assertThat(entitlements.temAcesso(u, p)).isTrue();
    }

    @Test
    void estornoRevogaTudoQueAquelePagamentoConcedeu() {
        UUID u = aluno("estorno@ent.com");
        UUID p1 = produto("SKU-EST-1");
        UUID p2 = produto("SKU-EST-2");

        entitlements.conceder(u, p1, "PAYMENT", "pay-estorno");
        entitlements.conceder(u, p2, "PAYMENT", "pay-estorno");
        assertThat(entitlements.listarDe(u)).hasSize(2);

        int revogados = entitlements.revogarPorOrigem("pay-estorno");

        assertThat(revogados).isEqualTo(2);
        assertThat(entitlements.listarDe(u)).isEmpty();
    }

    @Test
    void oAcessoDeUmAlunoNaoAlcancaOutro() {
        UUID a = aluno("a@ent.com");
        UUID b = aluno("b@ent.com");
        UUID p = produto("SKU-ISOLADO");

        entitlements.conceder(a, p, "PAYMENT", "pay-5");

        assertThat(entitlements.temAcesso(a, p)).isTrue();
        assertThat(entitlements.temAcesso(b, p)).isFalse();
    }
}
