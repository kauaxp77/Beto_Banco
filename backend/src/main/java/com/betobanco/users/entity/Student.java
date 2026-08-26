package com.betobanco.users.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * O id de Student E o id do usuario, atribuido explicitamente — nao gerado.
 * Gerar um novo criaria duas identidades para a mesma pessoa e quebraria as
 * chaves estrangeiras legadas de attempts.student_id.
 */
@Entity
@Table(name = "students")
public class Student {

    @Id
    private UUID id;

    private String phone;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private Instant updatedAt;

    protected Student() {
    }

    public Student(UUID id) {
        this.id = id;
    }

    public UUID getId() {
        return id;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }
}
