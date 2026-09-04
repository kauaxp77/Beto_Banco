package com.betobanco.leads.service;

import com.betobanco.leads.api.LeadCapture;
import com.betobanco.leads.entity.Lead;
import com.betobanco.leads.entity.LeadEvent;
import com.betobanco.leads.entity.LeadMagnet;
import com.betobanco.leads.repository.LeadEventRepository;
import com.betobanco.leads.repository.LeadMagnetRepository;
import com.betobanco.leads.repository.LeadRepository;
import com.betobanco.shared.exception.BusinessException;
import com.betobanco.shared.exception.ErrorCode;
import com.betobanco.shared.exception.NotFoundException;
import com.betobanco.shared.tenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Captacao de leads e funil. Documento Mestre Premium V3.0, secoes 11 e 8.
 */
@Service
public class LeadService implements LeadCapture {

    private static final Logger log = LoggerFactory.getLogger(LeadService.class);

    private final LeadRepository leads;
    private final LeadEventRepository eventos;
    private final LeadMagnetRepository materiais;

    public LeadService(LeadRepository leads, LeadEventRepository eventos,
                       LeadMagnetRepository materiais) {
        this.leads = leads;
        this.eventos = eventos;
        this.materiais = materiais;
    }

    // ------------------------------------------------------------------
    // Captacao publica
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<LeadMagnet> materiaisDisponiveis() {
        return materiais.findByTenantIdAndActiveTrueOrderByTitleAsc(TenantContext.atual());
    }

    /**
     * Troca o cadastro pelo material e devolve a URL do arquivo.
     *
     * <p>A URL so e resolvida depois de o lead estar gravado: devolve-la antes
     * transformaria o formulario em enfeite, porque bastaria abrir a rede do
     * navegador para pegar o link sem se cadastrar.
     */
    @Transactional
    public LeadMagnet capturar(String nome, String email, String whatsapp, String slugDoMaterial) {
        LeadMagnet material = materiais
                .findByTenantIdAndSlugAndActiveTrue(TenantContext.atual(), slugDoMaterial)
                .orElseThrow(() -> new NotFoundException("Material não encontrado ou indisponível"));

        Lead lead = registrarContato(nome, email, whatsapp);
        eventos.save(new LeadEvent(lead.getId(), LeadEvent.MATERIAL).comMaterial(material.getId()));

        return material;
    }

    // ------------------------------------------------------------------
    // Administracao dos materiais
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<LeadMagnet> materiaisTodos() {
        return materiais.findByTenantIdOrderByTitleAsc(TenantContext.atual());
    }

    @Transactional
    public LeadMagnet criarMaterial(String slug, String titulo, String tipo, String fileUrl) {
        try {
            return materiais.save(new LeadMagnet(slug, titulo, tipo, fileUrl));
        } catch (DataIntegrityViolationException e) {
            // Slug repetido e tipo fora da lista chegam os dois como violacao
            // de integridade; a mensagem crua do Postgres nao ajuda quem esta
            // preenchendo o formulario.
            throw new BusinessException(ErrorCode.CONFLICT,
                    "Já existe um material com esse identificador, ou o tipo informado não é válido.");
        }
    }

    @Transactional
    public LeadMagnet atualizarMaterial(UUID id, String titulo, String fileUrl, boolean ativo) {
        LeadMagnet material = materiais.findById(id)
                .filter(m -> m.getTenantId().equals(TenantContext.atual()))
                .orElseThrow(() -> new NotFoundException("Material não encontrado"));

        material.setTitle(titulo);
        material.setFileUrl(fileUrl);
        material.setActive(ativo);
        return materiais.save(material);
    }

    // ------------------------------------------------------------------
    // Recuperacao de vendas (secao 8)
    // ------------------------------------------------------------------

    /**
     * {@code REQUIRES_NEW} de proposito: o lead nasce na propria transacao. Se
     * o processamento do pagamento que originou a chamada for revertido depois,
     * o contato do comprador continua registrado — e ele e justamente o que a
     * secao 8 quer preservar quando a venda nao se concretiza.
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarVendaPerdida(VendaPerdida venda) {
        try {
            Lead lead = registrarContato(venda.nome(), venda.email(), null);
            String origem = venda.motivo() == Motivo.RECUSADO
                    ? LeadEvent.PAGAMENTO_RECUSADO
                    : LeadEvent.PAGAMENTO_CANCELADO;

            eventos.save(new LeadEvent(lead.getId(), origem)
                    .comVendaPerdida(venda.productId(), venda.amountCents(), venda.detalhe()));

            log.info("Lead de recuperacao registrado para {} ({}).", lead.getId(), origem);
        } catch (RuntimeException e) {
            // Perder um lead e ruim; derrubar por isso o processamento do
            // pagamento seria pior. O razao financeiro nao depende do CRM.
            log.error("Falha ao registrar lead de venda perdida; pagamento segue.", e);
        }
    }

    // ------------------------------------------------------------------
    // CRM
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public Page<Lead> listar(String status, String busca, int pagina, int tamanho) {
        Pageable paginacao = PageRequest.of(Math.max(pagina, 0), Math.min(Math.max(tamanho, 1), 100));
        return leads.buscar(TenantContext.atual(),
                vazioComoNulo(status), vazioComoNulo(busca), paginacao);
    }

    @Transactional(readOnly = true)
    public Lead porId(UUID id) {
        return leads.findById(id)
                .filter(l -> l.getTenantId().equals(TenantContext.atual()))
                .orElseThrow(() -> new NotFoundException("Lead não encontrado"));
    }

    @Transactional(readOnly = true)
    public List<LeadEvent> historico(UUID leadId) {
        return eventos.findByLeadIdOrderByOccurredAtDesc(porId(leadId).getId());
    }

    @Transactional
    public Lead mudarStatus(UUID id, String novo) {
        Lead lead = porId(id);
        try {
            lead.mudarStatus(novo);
        } catch (IllegalStateException e) {
            throw new BusinessException(ErrorCode.CONFLICT, e.getMessage());
        }
        return leads.save(lead);
    }

    @Transactional
    public Lead atribuir(UUID id, UUID ownerId) {
        Lead lead = porId(id);
        lead.setOwnerId(ownerId);
        return leads.save(lead);
    }

    @Transactional
    public Lead anotar(UUID id, String notas) {
        Lead lead = porId(id);
        lead.setNotes(notas);
        return leads.save(lead);
    }

    /** Contagem por etapa, na ordem do funil — que e a ordem em que se lê. */
    @Transactional(readOnly = true)
    public Map<String, Long> funil() {
        UUID tenant = TenantContext.atual();
        Map<String, Long> etapas = new LinkedHashMap<>();
        for (String status : List.of(Lead.NEW, Lead.CONTACTED, Lead.NEGOTIATING,
                Lead.WON, Lead.LOST)) {
            etapas.put(status, leads.countByTenantIdAndStatus(tenant, status));
        }
        return etapas;
    }

    // ------------------------------------------------------------------

    /**
     * Encontra a pessoa pelo e-mail ou cria uma nova.
     *
     * <p>O {@code catch} nao e paranoia: duas capturas simultaneas do mesmo
     * e-mail passam as duas pelo {@code findBy} antes de qualquer uma gravar, e
     * a segunda esbarra no indice unico. Reler e o comportamento correto — a
     * pessoa ja existe, que era o objetivo.
     */
    private Lead registrarContato(String nome, String email, String whatsapp) {
        UUID tenant = TenantContext.atual();
        String normalizado = Lead.normalizar(email);
        String nomeUsavel = nomeOuTrechoDoEmail(nome, normalizado);

        Lead lead = leads.findByTenantIdAndEmail(tenant, normalizado)
                .orElseGet(() -> new Lead(nomeUsavel, normalizado, whatsapp));
        lead.registrarContato(nomeUsavel, whatsapp);

        try {
            return leads.save(lead);
        } catch (DataIntegrityViolationException e) {
            Lead existente = leads.findByTenantIdAndEmail(tenant, normalizado)
                    .orElseThrow(() -> e);
            existente.registrarContato(nome, whatsapp);
            return leads.save(existente);
        }
    }

    /**
     * O gateway nem sempre manda o nome do comprador, e o CRM exige um: uma
     * linha em branco na lista de quem ligar nao e acionavel. O trecho antes do
     * arroba e o melhor palpite disponivel, e a equipe corrige no primeiro
     * contato.
     */
    private static String nomeOuTrechoDoEmail(String nome, String emailNormalizado) {
        if (nome != null && !nome.isBlank()) {
            return nome.trim();
        }
        int arroba = emailNormalizado.indexOf('@');
        return arroba > 0 ? emailNormalizado.substring(0, arroba) : emailNormalizado;
    }

    private static String vazioComoNulo(String valor) {
        return (valor == null || valor.isBlank()) ? null : valor.trim();
    }
}
