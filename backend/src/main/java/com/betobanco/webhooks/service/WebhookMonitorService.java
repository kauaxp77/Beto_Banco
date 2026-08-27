package com.betobanco.webhooks.service;

import com.betobanco.webhooks.api.WebhookMonitor;
import com.betobanco.webhooks.entity.WebhookEvent;
import com.betobanco.webhooks.repository.WebhookEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class WebhookMonitorService implements WebhookMonitor {

    private final WebhookEventRepository eventos;

    public WebhookMonitorService(WebhookEventRepository eventos) {
        this.eventos = eventos;
    }

    @Override
    @Transactional(readOnly = true)
    public long aguardandoAtencao() {
        return eventos.countByStatusIn(List.of(WebhookEvent.FAILED, WebhookEvent.MANUAL));
    }
}
