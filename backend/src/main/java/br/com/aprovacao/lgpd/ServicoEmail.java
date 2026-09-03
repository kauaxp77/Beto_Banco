package br.com.aprovacao.lgpd;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Secao 09 (regua de e-mail), secao 12 (recuperacao de venda) e secao 23 (SPF,
 * DKIM e DMARC configurados no provedor).
 *
 * <p>Todo envio e assincrono: SMTP lento nao pode segurar a liberacao de acesso de
 * um aluno que acabou de pagar. Falha de envio e registrada e nao propaga -- o
 * acesso ja esta liberado no banco, e reenviar o e-mail e trivial; desfazer a
 * matricula por causa de um SMTP fora do ar nao seria.
 */
@Service
public class ServicoEmail {

    private static final Logger log = LoggerFactory.getLogger(ServicoEmail.class);

    private final JavaMailSender remetente;
    private final String de;
    private final String urlApp;
    private final String emailFinanceiro;

    public ServicoEmail(JavaMailSender remetente,
                        @Value("${plataforma.email.remetente:nao-responda@plataforma.com.br}") String de,
                        @Value("${plataforma.email.url-app:http://localhost:5173}") String urlApp,
                        @Value("${plataforma.email.financeiro:financeiro@plataforma.com.br}") String emailFinanceiro) {
        this.remetente = remetente;
        this.de = de;
        this.urlApp = urlApp;
        this.emailFinanceiro = emailFinanceiro;
    }

    @Async
    public void enviarRecuperacaoDeSenha(String para, String token) {
        enviar(para, "Redefinicao de senha",
                """
                Recebemos um pedido para redefinir a senha da sua conta.

                Abra o link abaixo. Ele vale por 30 minutos e so pode ser usado uma vez:
                %s/senha/redefinir?token=%s

                Se nao foi voce quem pediu, ignore esta mensagem: sua senha continua a mesma.
                """.formatted(urlApp, token));
    }

    /**
     * Secao 12, D0 da regua da secao 09. Nao carrega senha: o aluno define a dele
     * pelo fluxo de recuperacao, e nenhuma credencial fica parada na caixa de entrada.
     */
    @Async
    public void enviarAcessoLiberado(String para, String nome) {
        enviar(para, "Seu acesso foi liberado",
                """
                Ola, %s.

                Seu pagamento foi aprovado e o acesso ja esta liberado.

                Defina sua senha para entrar:
                %s/senha/recuperar

                Bons estudos.
                """.formatted(nome, urlApp));
    }

    /** Secao 12 -- recuperacao de venda: e-mail em 24h apos recusa ou abandono. */
    @Async
    public void enviarRecuperacaoDeVenda(String para, String nome) {
        enviar(para, "Seu pagamento nao foi concluido",
                """
                Ola, %s.

                O pagamento do seu pedido nao foi aprovado pelo banco emissor.
                Isso costuma ser limite ou dado divergente, e da para tentar de novo:
                %s/checkout

                Se precisar de ajuda, e so responder este e-mail.
                """.formatted(nome == null ? "tudo bem" : nome, urlApp));
    }

    /** Secao 23 -- alerta operacional para webhook na fila morta e chargeback. */
    @Async
    public void alertarFinanceiro(String assunto, String corpo) {
        enviar(emailFinanceiro, "[ALERTA] " + assunto, corpo);
    }

    private void enviar(String para, String assunto, String corpo) {
        try {
            SimpleMailMessage mensagem = new SimpleMailMessage();
            mensagem.setFrom(de);
            mensagem.setTo(para);
            mensagem.setSubject(assunto);
            mensagem.setText(corpo);
            remetente.send(mensagem);
        } catch (RuntimeException e) {
            log.error("Falha ao enviar e-mail '{}' para {}: {}", assunto, para, e.getMessage());
        }
    }
}
