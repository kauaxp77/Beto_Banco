package br.com.aprovacao.consumo;

import br.com.aprovacao.auth.Usuario;
import br.com.aprovacao.common.ProblemaNegocio;
import br.com.aprovacao.config.PropriedadesPlataforma;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Secao 10 -- video, materiais e protecao de conteudo.
 *
 * <p>Secao 30 classifica pirataria como risco ALTO: "conteudo proprio e o ativo".
 * As tres mitigacoes listadas la aparecem aqui -- marca d'agua com identificacao do
 * aluno, link expiravel e alerta de uso anomalo.
 */
@Service
public class ServicoPlayer {

    private static final Logger log = LoggerFactory.getLogger(ServicoPlayer.class);

    private final EntityManager em;
    private final PropriedadesPlataforma props;

    public ServicoPlayer(EntityManager em, PropriedadesPlataforma props) {
        this.em = em;
        this.props = props;
    }

    @Transactional
    public Map<String, Object> autorizar(Usuario usuario, UUID aulaId) {
        exigirAcessoAAula(usuario.getId(), aulaId);

        Object[] aula = dadosDaAula(aulaId);
        String pandaVideoId = (String) aula[1];
        if (pandaVideoId == null || pandaVideoId.isBlank()) {
            // Secao 06 -- tela de excecao "video indisponivel", desenhada, nao improvisada.
            throw new ProblemaNegocio(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,
                    "video-indisponivel", "Esta aula ainda nao tem video publicado.");
        }

        Map<String, Object> resposta = new LinkedHashMap<>();
        resposta.put("aula_id", aulaId);
        resposta.put("titulo", aula[0]);
        resposta.put("panda_video_id", pandaVideoId);
        resposta.put("expira_em", Instant.now().plusSeconds(props.acesso().urlAssinadaSegundos()));
        // Secao 10: "Marca d'agua dinamica com nome e CPF parcial do aluno sobre o video."
        resposta.put("marca_dagua", marcaDagua(usuario));
        return resposta;
    }

    /**
     * Secao 10: "Alerta automatico quando uma conta e acessada de mais de 4 IPs
     * distintos em 24h." A contagem roda no acesso ao player porque e ali que a
     * conta compartilhada aparece -- login o assinante faz uma vez e repassa o token.
     */
    @Transactional
    public void exigirAcessoAAula(UUID usuarioId, UUID aulaId) {
        Object resultado = em.createNativeQuery("""
                SELECT 1
                  FROM aula a
                  JOIN modulo mo ON mo.id = a.modulo_id
                  JOIN matricula m ON m.curso_id = mo.curso_id
                 WHERE a.id = :aulaId
                   AND m.usuario_id = :usuarioId
                   AND m.status = 'ATIVA'
                   AND m.excluido_em IS NULL
                   AND m.expira_em > now()
                   AND a.publicado_em IS NOT NULL
                 LIMIT 1
                """)
                .setParameter("aulaId", aulaId)
                .setParameter("usuarioId", usuarioId)
                .getResultList()
                .stream().findFirst().orElse(null);

        if (resultado == null) {
            throw ProblemaNegocio.proibido(
                    "Voce nao tem acesso liberado a esta aula ou sua matricula venceu.");
        }
    }

    /** Registra o IP e alerta quando a conta passa do limite de IPs distintos em 24h. */
    @Transactional
    public void registrarAcesso(UUID usuarioId, String ip) {
        if (ip == null || ip.isBlank()) {
            return;
        }
        em.createNativeQuery("INSERT INTO acesso_ip (usuario_id, ip) VALUES (:usuarioId, :ip)")
                .setParameter("usuarioId", usuarioId)
                .setParameter("ip", ip)
                .executeUpdate();

        Number distintos = (Number) em.createNativeQuery("""
                SELECT count(DISTINCT ip) FROM acesso_ip
                 WHERE usuario_id = :usuarioId AND criado_em > now() - INTERVAL '24 hours'
                """)
                .setParameter("usuarioId", usuarioId)
                .getSingleResult();

        if (distintos.intValue() > props.acesso().alertaIpsDistintos24h()) {
            log.warn("Conta {} acessada de {} IPs distintos em 24h. Possivel compartilhamento.",
                    usuarioId, distintos);
        }
    }

    /**
     * CPF parcial, nunca completo: identifica o aluno para efeito de dissuasao sem
     * expor o documento inteiro em um frame que pode ser fotografado e repassado.
     */
    private Map<String, String> marcaDagua(Usuario usuario) {
        String cpf = usuario.getCpf();
        String parcial = "";
        if (cpf != null) {
            String digitos = cpf.replaceAll("\\D", "");
            if (digitos.length() >= 11) {
                parcial = "***." + digitos.substring(3, 6) + "." + digitos.substring(6, 9) + "-**";
            }
        }
        return Map.of("nome", usuario.getNome(), "cpf_parcial", parcial);
    }

    private Object[] dadosDaAula(UUID aulaId) {
        @SuppressWarnings("unchecked")
        List<Object[]> linhas = em.createNativeQuery(
                "SELECT titulo, panda_video_id FROM aula WHERE id = :id")
                .setParameter("id", aulaId)
                .getResultList();
        if (linhas.isEmpty()) {
            throw ProblemaNegocio.naoEncontrado("Aula");
        }
        return linhas.get(0);
    }
}
