package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * DTO for creating or updating an Aircraft.
 *
 * Why a DTO:
 * - Validates that tailNumber is present and matches the expected registration format before
 *   hitting the database.  An invalid tail number (e.g., "L@X123") would otherwise either
 *   silently persist junk data or cause a confusing UNIQUE-constraint error.
 *
 * Tail-number format:
 * - Must start with an uppercase letter or digit.
 * - May contain uppercase letters, digits, and hyphens.
 * - Total length: 2–10 characters.
 * - Examples: N12345, N-628TS, G-ABCD, C-GABC.
 */
public record AircraftRequest(
        Long id,

        @NotBlank(message = "Tail number must not be blank")
        @Pattern(
                regexp = "^[A-Z0-9][A-Z0-9-]{1,9}$",
                message = "Tail number must be 2–10 uppercase alphanumeric characters, hyphens allowed (e.g., N12345 or G-ABCD)"
        )
        String tailNumber,

        @Size(max = 100, message = "Model must be 100 characters or fewer")
        String model,

        Long ownerId
) {}
