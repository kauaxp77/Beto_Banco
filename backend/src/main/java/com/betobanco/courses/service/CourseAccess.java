package com.betobanco.courses.service;

import com.betobanco.courses.entity.CourseProduct;
import com.betobanco.courses.repository.CourseProductRepository;
import com.betobanco.entitlements.api.EntitlementService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * A pergunta central da area de membros — "quais cursos este aluno pode
 * ver?" — respondida num unico lugar, para todos os servicos do modulo.
 */
@Service
public class CourseAccess {

    private final CourseProductRepository courseProducts;
    private final EntitlementService entitlements;

    public CourseAccess(CourseProductRepository courseProducts,
                        EntitlementService entitlements) {
        this.courseProducts = courseProducts;
        this.entitlements = entitlements;
    }

    @Transactional(readOnly = true)
    public Set<UUID> cursosAcessiveis(UUID userId) {
        List<UUID> produtosDoAluno = entitlements.listarDe(userId).stream()
                .map(EntitlementService.Item::productId)
                .toList();
        if (produtosDoAluno.isEmpty()) {
            return Set.of();
        }
        return courseProducts.findByProductIdIn(produtosDoAluno).stream()
                .map(CourseProduct::getCourseId)
                .collect(Collectors.toSet());
    }
}
