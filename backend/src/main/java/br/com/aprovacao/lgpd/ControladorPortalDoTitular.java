package br.com.aprovacao.lgpd;

import br.com.aprovacao.config.FiltroTenant;
import br.com.aprovacao.config.PropriedadesPlataforma;
import br.com.aprovacao.config.UsuarioAutenticado;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Secao 22 -- "Portal do titular no perfil: exportar meus dados (JSON), corrigir,
 * revogar consentimento, excluir conta."
 *
 * <p>O documento e explicito: isso precisa ser autoatendimento na Fase 1, nao
 * pedido por e-mail. Secao 30 lista incidente com dado pessoal como risco ALTO,
 * com sancao da ANPD de ate 2% do faturamento, e manda implementar a secao 22 na
 * Fase 1 -- "nao depois".
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "LGPD", description = "Portal do titular e documentos legais (secao 22)")
public class ControladorPortalDoTitular {

    private final ServicoPortalDoTitular portal;
    private final ServicoConsentimento consentimento;
    private final PropriedadesPlataforma props;

    public ControladorPortalDoTitular(ServicoPortalDoTitular portal,
                                      ServicoConsentimento consentimento,
                                      PropriedadesPlataforma props) {
        this.portal = portal;
        this.consentimento = consentimento;
        this.props = props;
    }

    /** Documentos versionados, publicos: o checkout precisa exibi-los antes do aceite. */
    @GetMapping("/legal/{tipo}")
    @SecurityRequirements
    @Operation(summary = "Texto vigente de termos de uso, politica de privacidade ou de cookies")
    public Map<String, Object> documento(@PathVariable String tipo) {
        return portal.documentoVigente(FiltroTenant.atual(props.tenantPadrao()), tipo.toUpperCase());
    }

    @GetMapping("/me/dados")
    @Operation(summary = "Exporta em JSON todos os dados pessoais do titular")
    public Map<String, Object> exportar() {
        return portal.exportar(UsuarioAutenticado.obrigatorio().id());
    }

    @GetMapping("/me/consentimentos")
    @Operation(summary = "Consentimentos vigentes do titular")
    public List<Map<String, Object>> consentimentos() {
        return portal.consentimentosVigentes(UsuarioAutenticado.obrigatorio().id());
    }

    @PostMapping("/me/consentimentos")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Concede ou revoga um consentimento, com registro de data, hora e IP")
    public void registrarConsentimento(@Valid @RequestBody ConsentimentoRequest req, HttpServletRequest http) {
        consentimento.registrarConsentimento(
                UsuarioAutenticado.obrigatorio().id(),
                req.finalidade(),
                req.concedido(),
                req.textoAceito(),
                ip(http));
    }

    /**
     * Secao 22: "Exclusao anonimiza o cadastro mas preserva o registro fiscal do
     * pedido -- obrigacao legal se sobrepoe."
     */
    @DeleteMapping("/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Exclui a conta: anonimiza o cadastro e preserva o registro fiscal")
    public void excluirConta() {
        portal.excluirConta(UsuarioAutenticado.obrigatorio().id());
    }

    public record ConsentimentoRequest(
            @NotBlank String finalidade,
            boolean concedido,
            @NotBlank String textoAceito) {}

    private String ip(HttpServletRequest req) {
        String encaminhado = req.getHeader("X-Forwarded-For");
        return (encaminhado != null && !encaminhado.isBlank())
                ? encaminhado.split(",")[0].trim()
                : req.getRemoteAddr();
    }
}
