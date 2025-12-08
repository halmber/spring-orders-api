package com.halmber.springordersapi.model.mapper;

import com.halmber.springordersapi.model.dto.request.customer.CustomerCreateDto;
import com.halmber.springordersapi.model.dto.request.customer.CustomerEditDto;
import com.halmber.springordersapi.model.dto.response.customer.CustomerResponseDto;
import com.halmber.springordersapi.model.dto.response.customer.CustomerShortResponseDto;
import com.halmber.springordersapi.model.entity.Customer;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CustomerMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Customer toEntity(CustomerCreateDto dto);

    CustomerResponseDto toResponse(Customer entity);

    List<CustomerResponseDto> toList(List<Customer> entity);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromDto(
            CustomerEditDto dto,
            @MappingTarget Customer entity
    );

    @Mapping(target = "fullName", expression = "java(entity.getFullName())")
    CustomerShortResponseDto toShortDto(Customer entity);
}
