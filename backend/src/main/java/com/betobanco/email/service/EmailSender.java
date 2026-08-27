package com.betobanco.email.service;

import java.util.Map;

/**
 * Transporte de e-mail. Abstrair aqui evita amarrar o sistema a um provedor:
 * trocar SMTP por um servico de API e escrever outra implementacao.
 */
public interface EmailSender {

    void enviar(String destinatario, String template, Map<String, Object> dados);
}
