package com.betobanco.courses.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

/** Vinculo compra→conteudo: qual produto do catalogo libera qual curso. */
@Entity
@Table(name = "course_products")
public class CourseProduct {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "course_id", nullable = false)
    private UUID courseId;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    protected CourseProduct() {
    }

    public CourseProduct(UUID courseId, UUID productId) {
        this.courseId = courseId;
        this.productId = productId;
    }

    public UUID getId() {
        return id;
    }

    public UUID getCourseId() {
        return courseId;
    }

    public UUID getProductId() {
        return productId;
    }
}
