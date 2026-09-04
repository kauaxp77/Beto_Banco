package com.betobanco.leads.entity;

import com.betobanco.shared.tenant.TenantContext;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

/**
 * A pessoa no CRM. Documento Mestre Premium V3.0, secao 11.
 *
 * <p>Uma linha por e-mail, nao uma por captacao: quem baixa tres materiais e um
 * lead com tres eventos. Com uma linha por captacao, o funil contaria a mesma
 * pessoa tres vezes e a equipe ligaria tres vezes para ela.
 */
@Entity
@Table(name = "leads")
public class Lead {

    public static final String NEW = "NEW";
    public static final String CONTACTED = "CONTACTED";
    public static final String NEGOTIATING = "NEGOTIATING";
    public static final String WON = "WON";
    public static final String LOST = "LOST";

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId = TenantContext.atual();

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String email;

    @Column
    private String whatsapp;

    @Column(nullable = false)
    private String status = NEW;

    @Column(name = "owner_id")
    private UUID ownerId;

    @Column
    private String notes;

    @Column(name = "first_seen_at", nullable = false)
    private Instant firstSeenAt = Instant.now();

    @Column(name = "last_seen_at", nullable = false)
    private Instant lastSeenAt = Instant.now();

    protected Lead() {
    }

    public Lead(String name, String email, String whatsapp) {
        this.name = name;
        this.email = normalizar(email);
        this.whatsapp = whatsapp;
    }

    /** O indice unico e sobre {@code lower(email)}; gravar cru geraria duplicata. */
    public static String normalizar(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * Registra que a pessoa apareceu de novo.
     *
     * <p>Nome e WhatsApp so sao sobrescritos quando vem preenchidos: um segundo
     * formulario onde a pessoa nao digitou o telefone nao pode apagar o que ela
     * ja tinha dado — esse numero costuma ser o unico jeito de falar com ela.
     */
    public void registrarContato(String nome, String whatsapp) {
        if (nome != null && !nome.isBlank()) {
            this.name = nome.trim();
        }
        if (whatsapp != null && !whatsapp.isBlank()) {
            this.whatsapp = whatsapp.trim();
        }
        this.lastSeenAt = Instant.now();
    }

    /**
     * Move o lead no funil.
     *
     * <p>Nao sai de WON nem de LOST por esta via: sao estados finais, e um lead
     * que voltou a negociar e uma negociacao nova, nao a antiga ressuscitada —
     * misturar as duas falsearia a taxa de conversao do periodo.
     */
    public void mudarStatus(String novo) {
        if (WON.equals(status) || LOST.equals(status)) {
            throw new IllegalStateException(
                    "Lead já fechado como " + status
                            + "; registre um novo contato em vez de reabrir.");
        }
        this.status = novo;
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getWhatsapp() {
        return whatsapp;
    }

    public String getStatus() {
        return status;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public String getNotes() {
        return notes;
    }

    public Instant getFirstSeenAt() {
        return firstSeenAt;
    }

    public Instant getLastSeenAt() {
        return lastSeenAt;
    }

    public void setOwnerId(UUID ownerId) {
        this.ownerId = ownerId;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
