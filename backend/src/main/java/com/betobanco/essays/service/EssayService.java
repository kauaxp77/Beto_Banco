package com.betobanco.essays.service;

import com.betobanco.audit.api.AuditLogger;
import com.betobanco.essays.entity.Essay;
import com.betobanco.essays.entity.EssayCorrection;
import com.betobanco.essays.entity.EssayRubric;
import com.betobanco.essays.repository.EssayCorrectionRepository;
import com.betobanco.essays.repository.EssayRepository;
import com.betobanco.essays.repository.EssayRubricRepository;
import com.betobanco.shared.exception.BusinessException;
import com.betobanco.shared.exception.ErrorCode;
import com.betobanco.shared.exception.NotFoundException;
import com.betobanco.shared.tenant.TenantContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Envio, fila e correcao de redacao. Documento Mestre V4.0, secao 14.
 *
 * <p>Fluxo: envio -> fila do corretor -> correcao por rubrica -> devolutiva ->
 * reescrita opcional.
 */
@Service
public class EssayService {

    private static final Logger log = LoggerFactory.getLogger(EssayService.class);

    private final EssayRepository essays;
    private final EssayCorrectionRepository corrections;
    private final EssayRubricRepository rubrics;
    private final EssayQuotaService cotas;
    private final AuditLogger auditoria;
    private final ObjectMapper json;

    public EssayService(EssayRepository essays, EssayCorrectionRepository corrections,
                        EssayRubricRepository rubrics, EssayQuotaService cotas,
                        AuditLogger auditoria, ObjectMapper json) {
        this.essays = essays;
        this.corrections = corrections;
        this.rubrics = rubrics;
        this.cotas = cotas;
        this.auditoria = auditoria;
        this.json = json;
    }

    // ------------------------------------------------------------------
    // Aluno
    // ------------------------------------------------------------------

    /**
     * Passo 1 do fluxo. A cota e debitada aqui, no envio, e nao na conclusao: o
     * custo do corretor comeca quando a redacao entra na fila.
     */
    @Transactional
    public Essay enviar(UUID userId, String prompt, String board, String fileUrl) {
        cotas.consumir(userId);

        Essay redacao = essays.save(new Essay(userId, prompt, board, fileUrl));
        auditoria.registrar("ESSAY_SUBMITTED", "Essay", redacao.getId().toString(),
                Map.of("userId", userId.toString(), "dueAt", redacao.getDueAt().toString()));

        log.info("Redacao {} recebida de {}. Prazo ate {}.",
                redacao.getId(), userId, redacao.getDueAt());
        return redacao;
    }

    /**
     * Passo 5: reescrita. Nao consome cota nova — o documento a chama de
     * "reescrita opcional" dentro do mesmo ciclo, e cobrar de novo puniria
     * exatamente o aluno que fez o que a devolutiva pediu.
     */
    @Transactional
    public Essay enviarReescrita(UUID userId, UUID redacaoOriginalId, String fileUrl) {
        Essay original = essays.findByIdAndUserId(redacaoOriginalId, userId)
                .orElseThrow(() -> new NotFoundException("Redação não encontrada"));

        if (!Essay.CORRECTED.equals(original.getStatus())) {
            throw new BusinessException(ErrorCode.CONFLICT,
                    "A reescrita só faz sentido depois da devolutiva. Aguarde a correção.");
        }
        return essays.save(Essay.reescritaDe(original, fileUrl));
    }

    @Transactional(readOnly = true)
    public List<Essay> minhas(UUID userId) {
        return essays.findByUserIdOrderBySubmittedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public Essay minha(UUID userId, UUID essayId) {
        return essays.findByIdAndUserId(essayId, userId)
                .orElseThrow(() -> new NotFoundException("Redação não encontrada"));
    }

    @Transactional(readOnly = true)
    public Optional<EssayCorrection> devolutiva(UUID essayId) {
        // So devolve o que ja foi publicado: uma correcao em andamento nao pode
        // vazar nota parcial para o aluno.
        return corrections.findByEssayId(essayId).filter(EssayCorrection::publicada);
    }

    // ------------------------------------------------------------------
    // Corretor
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<Essay> fila(int limite) {
        return essays.fila(PageRequest.of(0, Math.min(limite, 100)));
    }

    /**
     * Passo 2. O indice unico em essay_corrections(essay_id) e o que impede dois
     * corretores de assumirem a mesma redacao: o segundo colide, em vez de
     * produzir uma segunda nota para o mesmo texto.
     */
    @Transactional
    public EssayCorrection assumir(UUID essayId, UUID correctorId) {
        Essay redacao = essays.findById(essayId)
                .orElseThrow(() -> new NotFoundException("Redação não encontrada"));

        if (!redacao.aguardandoCorrecao()) {
            throw new BusinessException(ErrorCode.CONFLICT,
                    "Esta redação já foi corrigida ou cancelada.");
        }
        if (corrections.findByEssayId(essayId).isPresent()) {
            throw new BusinessException(ErrorCode.CONFLICT,
                    "Outro corretor já assumiu esta redação.");
        }

        UUID rubricaId = rubrics
                .ativaDaBanca(TenantContext.atual(), redacao.getBoard() == null ? "" : redacao.getBoard())
                .map(EssayRubric::getId)
                .orElse(null);

        redacao.marcarEmCorrecao();
        essays.save(redacao);
        return corrections.save(new EssayCorrection(essayId, correctorId, rubricaId));
    }

    /**
     * Passos 3 e 4: correcao por rubrica e devolutiva.
     *
     * <p>As notas sao validadas contra a rubrica antes de publicar. Sem isso, um
     * erro de digitacao vira 1600 em um criterio que vale 200, e o aluno recebe
     * uma nota impossivel que ninguem consegue explicar depois.
     */
    @Transactional
    public EssayCorrection publicarDevolutiva(UUID essayId, UUID correctorId,
                                              Map<String, BigDecimal> notas, String comentario,
                                              String audioUrl, String anotacoes) {
        EssayCorrection correcao = corrections.findByEssayId(essayId)
                .orElseThrow(() -> new NotFoundException("Correção não iniciada para esta redação"));

        if (!correcao.getCorrectorId().equals(correctorId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN,
                    "Esta correção está atribuída a outro corretor.");
        }
        if (correcao.publicada()) {
            throw new BusinessException(ErrorCode.CONFLICT,
                    "Esta devolutiva já foi publicada.");
        }

        BigDecimal total = validarContraRubrica(correcao.getRubricId(), notas);

        Essay redacao = essays.findById(essayId).orElseThrow();
        try {
            correcao.publicar(json.writeValueAsString(notas), total, comentario, audioUrl, anotacoes);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Notas em formato inválido");
        }
        redacao.marcarCorrigida();

        essays.save(redacao);
        auditoria.registrar("ESSAY_CORRECTED", "Essay", essayId.toString(),
                Map.of("correctorId", correctorId.toString(), "total", String.valueOf(total)));

        return corrections.save(correcao);
    }

    /**
     * Confere cada nota contra o criterio correspondente e devolve o total.
     *
     * <p>Sem rubrica associada (banca sem rubrica cadastrada) apenas soma, para
     * nao travar a correcao — mas registra, porque e uma lacuna de cadastro que
     * alguem precisa fechar.
     */
    private BigDecimal validarContraRubrica(UUID rubricaId, Map<String, BigDecimal> notas) {
        if (notas == null || notas.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "Informe a nota de cada critério da rubrica.");
        }

        if (rubricaId == null) {
            log.warn("Correcao publicada sem rubrica: a banca da redacao nao tem rubrica ativa.");
            return notas.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        EssayRubric rubrica = rubrics.findById(rubricaId)
                .orElseThrow(() -> new NotFoundException("Rubrica não encontrada"));

        Map<String, BigDecimal> tetos = tetosDe(rubrica);
        BigDecimal total = BigDecimal.ZERO;

        for (Map.Entry<String, BigDecimal> nota : notas.entrySet()) {
            BigDecimal teto = tetos.get(nota.getKey());
            if (teto == null) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                        "Critério '%s' não pertence à rubrica %s.".formatted(nota.getKey(), rubrica.getBoard()));
            }
            if (nota.getValue().signum() < 0) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                        "Nota negativa no critério '%s'.".formatted(nota.getKey()));
            }
            // Teto zero e o criterio de desconto do Cebraspe (nota de gramatica):
            // ele nao soma, so subtrai, entao nao tem limite superior a validar.
            if (teto.signum() > 0 && nota.getValue().compareTo(teto) > 0) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                        "Nota %s no critério '%s' passa do máximo (%s)."
                                .formatted(nota.getValue(), nota.getKey(), teto));
            }
            total = total.add(nota.getValue());
        }
        return total;
    }

    private Map<String, BigDecimal> tetosDe(EssayRubric rubrica) {
        Map<String, BigDecimal> tetos = new LinkedHashMap<>();
        try {
            for (JsonNode criterio : json.readTree(rubrica.getCriteria())) {
                tetos.put(criterio.path("code").asText(),
                        new BigDecimal(criterio.path("max_score").asText("0")));
            }
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR,
                    "Rubrica %s está com os critérios em formato inválido.".formatted(rubrica.getBoard()));
        }
        return tetos;
    }

    @Transactional(readOnly = true)
    public List<EssayRubric> rubricasAtivas() {
        return rubrics.ativas(TenantContext.atual());
    }
}
