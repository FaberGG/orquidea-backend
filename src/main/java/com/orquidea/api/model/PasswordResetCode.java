package com.orquidea.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Código de recuperación de contraseña. Cada usuario tiene a lo sumo uno: pedir otro reemplaza al anterior
 * en la misma fila. Se guarda con BCrypt porque un hash rápido de 6 dígitos se revierte probando el millón de valores.
 */
@Entity
@Table(name = "codigos_recuperacion")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetCode {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false, unique = true)
    private User usuario;

    @Column(name = "codigo_hash", nullable = false, length = 100)
    private String codigoHash;

    /** Intentos fallidos con este código; al llegar al máximo el código se elimina. */
    @Column(nullable = false)
    private int intentos;

    @Column(name = "fecha_emision", nullable = false)
    private Instant fechaEmision;

    @Column(name = "fecha_expiracion", nullable = false)
    private Instant fechaExpiracion;
}
