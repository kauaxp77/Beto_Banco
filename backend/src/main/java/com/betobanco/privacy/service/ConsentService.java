package com.betobanco.privacy.service;

import com.betobanco.audit.api.AuditLogger;
import com.betobanco.privacy.entity.Consent;
import com.betobanco.privacy.entity.LegalAcceptance;
import com.betobanco.privacy.entity.LegalDocument;
import com.betobanco.privacy.repository.ConsentRepository;
import com.betobanco.privacy.repository.LegalAcceptanceRepository;
import com.betobanco.privacy.repository.LegalDocumentRepository;
import com.betobanco.shared.exception.NotFoundException;
import com.betobanco.shared.tenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Consentimento e aceite de textos legais. Documento Mestre V4.0, secao 22.
 *
 * <p>Duas coisas diferentes, de proposito em servicos separados na cabeca de
 * quem le: <b>aceite</b> e concordar com um contrato (termos de uso), e vale
 * como bloco; <b>consentimento</b> e autorizar uma finalidade especifica
 * (WhatsApp de marketing, cookie de analytics), e cada uma e independente das
 * outras. Misturar os dois e como se acaba mandando marketing para quem so
 * aceitou os termos para poder comprar.
 */
@Service
public class ConsentService {

    private static final Logger log = LoggerFactory.getLogger(ConsentService.class);

    private final ConsentRepository consents;
    private final LegalDocumentRepository documents;
    private final LegalAcceptanceRepository acceptances;
    private final AuditLogger auditoria;

    public ConsentService(ConsentRepository consents,
                          LegalDocumentRepository documents,
                          LegalAcceptanceRepository acceptances,
                          AuditLogger auditoria) {
        this.consents = consents;
        this.documents = documents;
        this.acceptances = acceptances;
        this.auditoria = auditoria;
    }

    // ------------------------------------------------------------------
    // Consentimento por finalidade
    // ------------------------------------------------------------------

    /**
     * Grava uma decisao. Sempre INSERT, nunca UPDATE: revogar precisa deixar
     * rastro de que houve consentimento antes.
     */
    @Transactional
    public Consent registrar(UUID userId, String purpose, boolean granted,
                             String acceptedText, String ip) {
        Consent registro = consents.save(
                new Consent(userId, purpose, granted, acceptedText, ip));

        auditoria.registrar(granted ? "CONSENT_GRANTED" : "CONSENT_WITHDRAWN",
                "Consent", registro.getId().toString(),
                Map.of("userId", userId.toString(), "purpose", purpose));

        return registro;
    }

    /**
     * A decisao vigente. Ausencia de registro e recusa — nunca aceite tacito
     * (secao 16: "caixa pre-marcada nao e consentimento valido").
     */
    @Transactional(readOnly = true)
    public boolean concedido(UUID userId, String purpose) {
        return consents.vigente(userId, purpose).map(Consent::isGranted).orElse(false);
    }

    /** Situacao atual de cada finalidade, para o portal do titular. */
    @Transactional(readOnly = true)
    public Map<String, Boolean> situacaoAtual(UUID userId) {
        Map<String, Boolean> situacao = new LinkedHashMap<>();
        for (String finalidade : List.of(Consent.MARKETING_WHATSAPP, Consent.MARKETING_EMAIL,
                Consent.COOKIE_ANALYTICS, Consent.COOKIE_MARKETING)) {
            situacao.put(finalidade, concedido(userId, finalidade));
        }
        return situacao;
    }

    @Transactional(readOnly = true)
    public List<Consent> historico(UUID userId) {
        return consents.findByUserIdOrderByRecordedAtDesc(userId);
    }

    /**
     * Revoga tudo de uma vez. Secao 22 lista "revogar consentimento" como
     * direito do titular, e exerce-lo nao pode custar quatro cliques quando
     * conceder custou um.
     */
    @Transactional
    public int revogarTudo(UUID userId, String ip) {
        int revogados = 0;
        for (Map.Entry<String, Boolean> e : situacaoAtual(userId).entrySet()) {
            if (Boolean.TRUE.equals(e.getValue())) {
                registrar(userId, e.getKey(), false,
                        "Revogacao total solicitada pelo titular no portal de privacidade.", ip);
                revogados++;
            }
        }
        return revogados;
    }

    // ------------------------------------------------------------------
    // Aceite de texto legal
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public LegalDocument vigente(String type) {
        return documents.vigente(TenantContext.atual(), type)
                .orElseThrow(() -> new NotFoundException("Documento legal indisponível: " + type));
    }

    /**
     * Registra o aceite da versao vigente de um texto.
     *
     * @param version versao que o cliente declara ter exibido. Divergindo da
     *                vigente, o aceite e recusado: a pessoa concordou com um
     *                texto que nao esta mais no ar, e gravar isso como valido
     *                seria registrar uma prova falsa.
     */
    @Transactional
    public LegalAcceptance registrarAceite(UUID userId, String type, String version,
                                           String ip, String userAgent) {
        LegalDocument documento = documents
                .findByTenantIdAndTypeAndVersion(TenantContext.atual(), type, version)
                .orElseThrow(() -> new NotFoundException(
                        "Versão %s de %s não encontrada".formatted(version, type)));

        Optional<LegalDocument> atual = documents.vigente(TenantContext.atual(), type);
        if (atual.isPresent() && !atual.get().getId().equals(documento.getId())) {
            log.warn("Aceite de {} na versao {} enquanto a vigente e {}. "
                            + "Cliente provavelmente com pagina em cache.",
                    type, version, atual.get().getVersion());
        }

        LegalAcceptance aceite = acceptances.save(
                new LegalAcceptance(userId, documento.getId(), ip, userAgent));

        auditoria.registrar("LEGAL_TERMS_ACCEPTED", "LegalAcceptance",
                aceite.getId().toString(),
                Map.of("userId", userId.toString(), "type", type, "version", version));

        return aceite;
    }

    @Transactional(readOnly = true)
    public List<LegalAcceptance> aceitesDe(UUID userId) {
        return acceptances.findByUserIdOrderByAcceptedAtDesc(userId);
    }
}
