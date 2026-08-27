package com.betobanco.email.service;

import com.betobanco.email.api.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class SmtpEmailSender implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(SmtpEmailSender.class);

    private final JavaMailSender mailSender;
    private final String remetente;
    private final String urlBase;

    public SmtpEmailSender(JavaMailSender mailSender,
                           @Value("${betobanco.email.from:nao-responda@betobanco.com}")
                           String remetente,
                           @Value("${betobanco.email.base-url:http://localhost:5173}")
                           String urlBase) {
        this.mailSender = mailSender;
        this.remetente = remetente;
        this.urlBase = urlBase;
    }

    @Override
    public void enviar(String destinatario, String template, Map<String, Object> dados) {
        SimpleMailMessage mensagem = new SimpleMailMessage();
        mensagem.setFrom(remetente);
        mensagem.setTo(destinatario);
        mensagem.setSubject(assunto(template));
        mensagem.setText(corpo(template, dados));

        mailSender.send(mensagem);
        log.info("E-mail {} enviado para {}", template, destinatario);
    }

    private String assunto(String template) {
        return switch (template) {
            case EmailService.Templates.PRIMEIRO_ACESSO -> "Seu acesso ao Beto Banco está liberado";
            case EmailService.Templates.ACESSO_LIBERADO -> "Novo conteúdo liberado para você";
            case EmailService.Templates.RECUPERACAO_SENHA -> "Redefinição de senha";
            case EmailService.Templates.ACESSO_REVOGADO -> "Seu acesso foi encerrado";
            case EmailService.Templates.ANUNCIO -> "Aviso do professor — Beto Banco";
            default -> "Beto Banco";
        };
    }

    private String corpo(String template, Map<String, Object> dados) {
        String nome = String.valueOf(dados.getOrDefault("nome", "aluno"));
        String token = String.valueOf(dados.getOrDefault("token", ""));

        return switch (template) {
            case EmailService.Templates.PRIMEIRO_ACESSO -> """
                    Olá, %s!

                    Seu pagamento foi confirmado e o acesso à plataforma está liberado.

                    Defina sua senha neste link (válido por 72 horas):
                    %s/definir-senha/%s

                    Bons estudos!
                    Equipe Beto Banco""".formatted(nome, urlBase, token);

            case EmailService.Templates.RECUPERACAO_SENHA -> """
                    Olá, %s!

                    Recebemos um pedido para redefinir sua senha.

                    Use este link (válido por 1 hora):
                    %s/definir-senha/%s

                    Se não foi você, ignore esta mensagem: sua senha continua a mesma.

                    Equipe Beto Banco""".formatted(nome, urlBase, token);

            case EmailService.Templates.ACESSO_LIBERADO -> """
                    Olá, %s!

                    Seu pagamento foi confirmado e o novo conteúdo já está disponível
                    na sua área de membros.

                    Acesse em: %s

                    Equipe Beto Banco""".formatted(nome, urlBase);

            case EmailService.Templates.ACESSO_REVOGADO -> """
                    Olá, %s.

                    Seu acesso ao conteúdo foi encerrado em razão do estorno do pagamento.

                    Se acredita que houve engano, responda a este e-mail.

                    Equipe Beto Banco""".formatted(nome);

            case EmailService.Templates.ANUNCIO -> """
                    Olá, %s!

                    %s

                    %s

                    Veja na plataforma: %s

                    Equipe Beto Banco""".formatted(nome,
                    String.valueOf(dados.getOrDefault("titulo", "Novo aviso do professor")),
                    String.valueOf(dados.getOrDefault("mensagem", "")),
                    urlBase);

            default -> "Mensagem do Beto Banco.";
        };
    }
}
