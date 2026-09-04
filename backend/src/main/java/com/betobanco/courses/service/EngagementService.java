package com.betobanco.courses.service;

import com.betobanco.courses.dto.LessonCardResponse;
import com.betobanco.courses.entity.LessonFavorite;
import com.betobanco.courses.entity.LessonPlayback;
import com.betobanco.courses.repository.LessonFavoriteRepository;
import com.betobanco.courses.repository.LessonPlaybackRepository;
import com.betobanco.courses.repository.LessonRepository;
import com.betobanco.shared.exception.BusinessException;
import com.betobanco.shared.exception.ErrorCode;
import com.betobanco.shared.exception.NotFoundException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * "Continue assistindo", historico e favoritos. Documento Mestre Premium V3.0,
 * secao 5 — recursos obrigatorios da area do aluno.
 */
@Service
public class EngagementService {

    /**
     * A tela mostra poucos cartoes. Buscar tudo e cortar no navegador traria a
     * carreira inteira de estudo de um aluno antigo a cada abertura da home.
     */
    private static final int RETOMADA_PADRAO = 6;
    private static final int HISTORICO_MAX = 100;

    private final LessonPlaybackRepository playback;
    private final LessonFavoriteRepository favoritos;
    private final LessonRepository aulas;

    public EngagementService(LessonPlaybackRepository playback,
                             LessonFavoriteRepository favoritos,
                             LessonRepository aulas) {
        this.playback = playback;
        this.favoritos = favoritos;
        this.aulas = aulas;
    }

    /**
     * Grava onde o aluno parou.
     *
     * <p>Chamada com frequencia pelo player, entao e um upsert simples e sem
     * leitura extra de aula alem da checagem de existencia — o custo por
     * chamada vira custo por segundo assistido.
     */
    @Transactional
    public void marcarPosicao(UUID userId, UUID lessonId, int segundos) {
        if (!aulas.existsById(lessonId)) {
            throw new NotFoundException("Aula não encontrada");
        }

        LessonPlayback marca = playback.findByUserIdAndLessonId(userId, lessonId)
                .orElseGet(() -> new LessonPlayback(userId, lessonId));
        try {
            marca.marcar(segundos);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, e.getMessage());
        }
        playback.save(marca);
    }

    @Transactional(readOnly = true)
    public List<LessonCardResponse> continuarAssistindo(UUID userId, Integer limite) {
        int quantos = (limite == null || limite < 1) ? RETOMADA_PADRAO : Math.min(limite, 50);
        return playback.continuarAssistindo(userId, PageRequest.of(0, quantos));
    }

    @Transactional(readOnly = true)
    public List<LessonCardResponse> historico(UUID userId, Integer limite) {
        int quantos = (limite == null || limite < 1) ? 20 : Math.min(limite, HISTORICO_MAX);
        return playback.historico(userId, PageRequest.of(0, quantos));
    }

    @Transactional(readOnly = true)
    public List<LessonCardResponse> favoritos(UUID userId) {
        return favoritos.favoritosDe(userId);
    }

    /** Idempotente: favoritar duas vezes deixa favoritado, nao devolve erro. */
    @Transactional
    public void favoritar(UUID userId, UUID lessonId) {
        if (!aulas.existsById(lessonId)) {
            throw new NotFoundException("Aula não encontrada");
        }
        if (!favoritos.existsByUserIdAndLessonId(userId, lessonId)) {
            favoritos.save(new LessonFavorite(userId, lessonId));
        }
    }

    @Transactional
    public void desfavoritar(UUID userId, UUID lessonId) {
        favoritos.deleteByUserIdAndLessonId(userId, lessonId);
    }
}
