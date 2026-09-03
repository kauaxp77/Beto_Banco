package com.betobanco.essays.service;

import com.betobanco.essays.entity.EssayQuota;
import com.betobanco.essays.repository.EssayQuotaRepository;
import com.betobanco.shared.exception.BusinessException;
import com.betobanco.shared.exception.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Cota de correcao de redacao. Documento Mestre V4.0, secao 14.
 *
 * <p>Este servico e uma regra financeira disfarcada de regra de produto. O
 * documento e explicito: "Correcao ilimitada destroi a margem: 4 correcoes
 * mensais por 12 meses custam R$ 864 por aluno de mentoria contra um ticket de
 * R$ 3.564. A cota e o que mantem a margem da secao 04 de pe."
 *
 * <p>Por isso o consumo acontece <b>no envio</b>, e nao na conclusao da
 * correcao: o custo de R$ 18 e do corretor, que ja e acionado quando a redacao
 * entra na fila. Debitar so no fim deixaria a fila encher de graca.
 */
@Service
public class EssayQuotaService {

    private static final Logger log = LoggerFactory.getLogger(EssayQuotaService.class);

    private final EssayQuotaRepository quotas;
    private final int cotaMensalMentoria;

    public EssayQuotaService(EssayQuotaRepository quotas,
                             @Value("${betobanco.essays.cota-mensal-mentoria:4}") int cotaMensalMentoria) {
        this.quotas = quotas;
        this.cotaMensalMentoria = cotaMensalMentoria;
    }

    @Transactional(readOnly = true)
    public EssayQuota cotaAtual(UUID userId) {
        return quotas.findByUserIdAndPeriod(userId, EssayQuota.competenciaAtual())
                .orElseGet(() -> new EssayQuota(userId, EssayQuota.competenciaAtual(), 0));
    }

    /**
     * Debita uma correcao. Falha com mensagem util quando nao ha saldo — o aluno
     * precisa saber que existe cota e quando ela vira, nao levar um erro seco.
     */
    @Transactional
    public void consumir(UUID userId) {
        LocalDate competencia = EssayQuota.competenciaAtual();
        EssayQuota cota = quotas.findByUserIdAndPeriod(userId, competencia)
                .orElseGet(() -> quotas.save(new EssayQuota(userId, competencia, 0)));

        if (!cota.temSaldo()) {
            throw new BusinessException(ErrorCode.CONFLICT,
                    "Sua cota de correções desta competência acabou (%d de %d usadas). "
                            .formatted(cota.getUsed(), cota.getAvailable())
                            + "A cota da mentoria renova no dia 1º; você também pode comprar uma correção avulsa.");
        }

        cota.consumir();
        quotas.save(cota);
    }

    /** Devolve a cota quando o envio e cancelado antes de qualquer corretor pegar. */
    @Transactional
    public void devolver(UUID userId) {
        quotas.findByUserIdAndPeriod(userId, EssayQuota.competenciaAtual()).ifPresent(cota -> {
            cota.acrescentar(1);
            quotas.save(cota);
        });
    }

    /**
     * Acrescenta cota na competencia corrente. Usado pela renovacao mensal da
     * mentoria, pela compra avulsa e pela concessao do admin.
     */
    @Transactional
    public EssayQuota conceder(UUID userId, int quantidade, String motivo) {
        LocalDate competencia = EssayQuota.competenciaAtual();
        EssayQuota cota = quotas.findByUserIdAndPeriod(userId, competencia)
                .orElseGet(() -> new EssayQuota(userId, competencia, 0));

        cota.acrescentar(quantidade);
        log.info("Cota de redacao +{} para {} ({}). Disponivel agora: {}.",
                quantidade, userId, motivo, cota.getAvailable());
        return quotas.save(cota);
    }

    /** Secao 14: "Mentoria: 4/mes." */
    @Transactional
    public EssayQuota concederCotaMensalDaMentoria(UUID userId) {
        return conceder(userId, cotaMensalMentoria, "renovação mensal da mentoria");
    }
}
