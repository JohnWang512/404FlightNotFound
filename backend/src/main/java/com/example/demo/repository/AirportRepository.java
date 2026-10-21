package com.example.demo.repository;

import com.example.demo.entity.Airport;
import org.springframework.data.repository.CrudRepository;
import java.util.List;

/**
 * Persistence queries for {@link com.example.demo.entity.Airport}.
 *
 * Why:
 * - Airport rows are reference data keyed by a string code (`apCode`).
 * - Filtering by country/city helps seeders or UI tools locate subsets of airports
 *   without requiring additional indexing logic in the service layer.
 */
public interface AirportRepository extends CrudRepository<Airport, String> {
    /**
     * Find airports by country.
     *
     * Why a list:
     * - Multiple airports can exist in the same country, so the lookup is not expected to be unique.
     */
    List<Airport> findByCountry(String country);

    /**
     * Find airports by city.
     *
     * Why a list:
     * - A city can contain multiple airports (or multiple codes in a dataset).
     */
    List<Airport> findByCity(String city);
}
