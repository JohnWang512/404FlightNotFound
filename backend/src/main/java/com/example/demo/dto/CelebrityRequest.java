package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO for creating or updating a Celebrity.
 *
 * Why a DTO instead of the entity directly:
 * - Validates user input before it reaches the database layer.
 * - Prevents a blank name from causing a DB constraint violation or a confusing server error.
 * - Include {@code id} so the endpoint supports both insert (id=null) and update (id=provided) semantics.
 */
public record CelebrityRequest(
        Long id,

        @NotBlank(message = "Celebrity name must not be blank")
        @Size(max = 255, message = "Name must be 255 characters or fewer")
        String name,

        @Size(max = 255, message = "Industry must be 255 characters or fewer")
        String industry
) {}
