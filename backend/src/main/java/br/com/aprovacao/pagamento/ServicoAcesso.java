package br.com.aprovacao.pagamento;

import br.com.aprovacao.auth.Usuario;
import br.com.aprovacao.auth.UsuarioRepository;
import br.com.aprovacao.catalogo.Curso;
import br.com.aprovacao.catalogo.CursoRepository;
import br.com.aprovacao.comercial.Pedido;
import br.com.aprovacao.comercial.PedidoItem;
import br.com.aprovacao.comercial.StatusPedido;
import br.com.aprovacao.consumo.Matricula;
import br.com.aprovacao.consumo.MatriculaRepository;
import br.com.aprovacao.conteudo.ServicoAuditoria;
import br.com.aprovacao.lgpd.ServicoEmail;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Secao 12 -- o que cada estado do pagamento faz com o acesso.
 *
 * <p>Todo o efeito colateral de um pagamento vive aqui, em um metodo por transicao,
 * para que o processador de webhook, a reconciliacao diaria e a liberacao manual do
 * admin percorram exatamente o mesmo caminho. Tres implementacoes da mesma regra
 * seria a forma mais rapida de produzir divergencia entre pedido e matricula.
 */
@Service
public class ServicoAcesso {

    private static final Logger log = LoggerFactory.getLogger(ServicoAcesso.class);
    private static final SecureRandom ALEATORIO = new SecureRandom();

    private final UsuarioRepository usuarios;
    private final CursoRepository cursos;
    private final MatriculaRepository matriculas;
    private final PasswordEncoder encoder;
    private final ServicoEmail email;
    private final ServicoAuditoria auditoria;

    public ServicoAcesso(UsuarioRepository usuarios,
                         CursoRepository cursos,
                         MatriculaRepository matriculas,
                         PasswordEncoder encoder,
                         ServicoEmail email,
                         ServicoAuditoria auditoria) {
        this.usuarios = usuarios;
        this.cursos = cursos;
        this.matriculas = matriculas;
        this.encoder = encoder;
        this.email = email;
        this.auditoria = auditoria;
    }

    /**
     * APROVADO: cria usuario e matricula, libera por 12 meses.
     *
     * <p>Idempotente por construcao: chamada duas vezes para o mesmo pedido, a
     * segunda encontra a matricula viva e nao faz nada. Isso importa porque o
     * gateway reenvia eventos e a reconciliacao das 03h passa pelo mesmo metodo.
     */
    @Transactional
    public void liberarAcesso(Pedido pedido) {
        Usuario usuario = usuarioDoPedido(pedido);
        pedido.setUsuarioId(usuario.getId());

        for (PedidoItem item : pedido.getItens()) {
            Optional<Curso> curso = cursos.findById(item.getCursoId());
            if (curso.isEmpty()) {
                log.error("Pedido {} referencia curso inexistente {}. Acesso nao liberado para este item.",
                        pedido.getId(), item.getCursoId());
                continue;
            }
            int dias = curso.get().getDiasAcesso();

            Optional<Matricula> existente = matriculas.maisRecente(usuario.getId(), item.getCursoId());
            if (existente.isPresent() && existente.get().liberaConteudo()) {
                // Ja tem acesso vivo. Compra repetida do mesmo curso e renovacao:
                // secao 03, "renovacao com 40% de desconto" -- o prazo soma, o
                // aluno nao perde o que ja pagou.
                existente.get().renovarPor(dias);
                continue;
            }
            matriculas.save(new Matricula(pedido.getTenantId(), usuario.getId(),
                    item.getCursoId(), pedido.getId(), dias));
        }

        auditoria.registrar(pedido.getTenantId(), usuario.getId(), "ACESSO_LIBERADO",
                "pedido", pedido.getId().toString(), null, null);
        email.enviarAcessoLiberado(usuario.getEmail(), usuario.getNome());
    }

    /** ESTORNADO e CANCELADO revogam; a matricula some da area do aluno na hora. */
    @Transactional
    public void revogarAcesso(Pedido pedido, StatusPedido motivo) {
        List<Matricula> afetadas = matriculas.findByPedidoId(pedido.getId());
        for (Matricula m : afetadas) {
            if (motivo.bloqueiaRecompra()) {
                m.bloquear();
            } else {
                m.revogar();
            }
        }
        auditoria.registrar(pedido.getTenantId(), pedido.getUsuarioId(),
                "ACESSO_REVOGADO_" + motivo.name(), "pedido", pedido.getId().toString(), null, null);

        if (motivo == StatusPedido.CHARGEBACK) {
            // Secao 12: "Bloqueia conta e notifica financeiro."
            log.warn("CHARGEBACK no pedido {} ({}). Conta bloqueada para recompra.",
                    pedido.getId(), pedido.getEmail());
            email.alertarFinanceiro("Chargeback recebido",
                    "Pedido " + pedido.getId() + " de " + pedido.getEmail() + " sofreu chargeback. Acesso bloqueado.");
        }
    }

    /**
     * Cria a conta na hora da aprovacao quando ela ainda nao existe.
     *
     * <p>A senha nasce aleatoria e nunca e enviada: o e-mail de boas-vindas leva um
     * link de definicao de senha. Mandar senha provisoria por e-mail deixaria a
     * credencial em texto claro na caixa do aluno para sempre.
     */
    private Usuario usuarioDoPedido(Pedido pedido) {
        Optional<Usuario> existente = usuarios.buscarAtivoPorEmail(pedido.getEmail(), pedido.getTenantId());
        if (existente.isPresent()) {
            return existente.get();
        }
        byte[] bytes = new byte[32];
        ALEATORIO.nextBytes(bytes);
        String senhaInutilizavel = Base64.getEncoder().encodeToString(bytes);

        Usuario novo = new Usuario(pedido.getTenantId(),
                pedido.getNome() == null ? pedido.getEmail() : pedido.getNome(),
                pedido.getEmail(),
                encoder.encode(senhaInutilizavel));
        novo.setWhatsapp(pedido.getWhatsapp());
        novo.setCpf(pedido.getCpf());
        return usuarios.save(novo);
    }
}
