package com.ecoomerce.sportscenter.service;

import com.ecoomerce.sportscenter.entity.Type;
import com.ecoomerce.sportscenter.model.TypeResponse;
import com.ecoomerce.sportscenter.repository.TypeRepository;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * TypeServiceImpl — concrete implementation of TypeService.
 *
 * Mirrors BrandServiceImpl but for product types/categories (e.g. Shoes, Rackets, Gloves).
 * Types are lookup data fetched once on Angular store load to populate the type filter sidebar.
 *
 * @Service : registers this as a Spring-managed singleton service bean
 * @Log4j2  : injects a Log4j2 logger via Lombok
 */
@Service
@Log4j2
public class TypeServiceImpl implements TypeService {
    private final TypeRepository typeRepository; // Spring Data JPA repository for the Type table

    /**
     * Constructor injection — Spring injects the TypeRepository dependency.
     *
     * @param typeRepository provides findAll() and CRUD for Type entities
     */
    public TypeServiceImpl(TypeRepository typeRepository) {
        this.typeRepository = typeRepository;
    }

    /**
     * {@inheritDoc}
     *
     * Fetches all type rows from MySQL and converts each to a TypeResponse DTO.
     * The lazy List<Product> on Type is never accessed here — no N+1 query risk.
     */
    @Override
    public List<TypeResponse> getAllTypes() {
        log.info("Fetching all Types");
        // SELECT Id, Name FROM Type
        List<Type> typeList = typeRepository.findAll();
        // Map each Type entity → TypeResponse DTO (id + name only)
        List<TypeResponse> typeResponses = typeList.stream()
                .map(this::convertToTypeResponse)
                .collect(Collectors.toList());
        log.info("Fetched all Types");
        return typeResponses;
    }

    /**
     * convertToTypeResponse — maps a Type JPA entity to a TypeResponse DTO.
     *
     * Exposes only id and name — the products list is never included in the response.
     *
     * @param type the Type entity from MySQL
     * @return a TypeResponse DTO with id and name
     */
    private TypeResponse convertToTypeResponse(Type type) {
        return TypeResponse.builder()
                .id(type.getId())
                .name(type.getName())
                .build();
    }
}
