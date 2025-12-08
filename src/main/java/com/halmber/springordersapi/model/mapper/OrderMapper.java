package com.halmber.springordersapi.model.mapper;

import com.halmber.springordersapi.model.OrderFilter;
import com.halmber.springordersapi.model.OrderReportFilter;
import com.halmber.springordersapi.model.dto.request.order.OrderCreateDto;
import com.halmber.springordersapi.model.dto.request.order.OrderEditDto;
import com.halmber.springordersapi.model.dto.request.order.OrderFilterDto;
import com.halmber.springordersapi.model.dto.request.order.OrderReportFilterDto;
import com.halmber.springordersapi.model.dto.response.order.OrderResponseDto;
import com.halmber.springordersapi.model.dto.response.order.OrderShortResponseDto;
import com.halmber.springordersapi.model.entity.Order;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", uses = CustomerMapper.class)
public interface OrderMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "customer", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "status", expression = "java(toEnum(dto.status(), com.halmber.springordersapi.model.enums.StatusEnum.class, \"status\", true))")
    @Mapping(target = "paymentMethod", expression = "java(toEnum(dto.paymentMethod(), com.halmber.springordersapi.model.enums.PaymentEnum.class, \"payment\", true))")
    Order toEntity(OrderCreateDto dto);

    @Mapping(target = "customer", source = "customer")
    OrderResponseDto toResponse(Order entity);

    List<OrderResponseDto> toList(List<Order> orders);

    OrderShortResponseDto toShortDto(Order entity);

    List<OrderShortResponseDto> toShortDtoList(List<Order> entities);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "customer", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "status", expression = "java(toEnum(dto.status(), com.halmber.springordersapi.model.enums.StatusEnum.class, \"status\", true))")
    @Mapping(target = "paymentMethod", expression = "java(toEnum(dto.paymentMethod(), com.halmber.springordersapi.model.enums.PaymentEnum.class, \"payment\", true))")
    void updateEntityFromDto(OrderEditDto dto, @MappingTarget Order entity);

    @Mapping(target = "status", expression = "java(toEnum(dto.status(), com.halmber.springordersapi.model.enums.StatusEnum.class, \"status\", false))")
    @Mapping(target = "paymentMethod", expression = "java(toEnum(dto.paymentMethod(), com.halmber.springordersapi.model.enums.PaymentEnum.class, \"payment\", false))")
    OrderFilter toOrderFilter(OrderFilterDto dto);

    @Mapping(target = "status", expression = "java(toEnum(dto.status(), com.halmber.springordersapi.model.enums.StatusEnum.class, \"status\", false))")
    @Mapping(target = "paymentMethod", expression = "java(toEnum(dto.paymentMethod(), com.halmber.springordersapi.model.enums.PaymentEnum.class, \"payment\", false))")
    @Mapping(target = "fileType", expression = "java(com.halmber.springordersapi.model.enums.ReportFileTypeEnum.fromString(dto.fileType()))")
    OrderReportFilter toOrderReportFilter(OrderReportFilterDto dto);

    default <E extends Enum<E>> E toEnum(String value, Class<E> enumClass, String fieldName, Boolean blankCheck) {
        if (value == null || value.isBlank()) {
            if (!blankCheck) return null; // Allow blank or null values when it's not need
            throw new IllegalArgumentException("'%s' cannot be blank".formatted(fieldName));
        }

        try {
            return Enum.valueOf(enumClass, value);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Invalid %s value: '%s'.".formatted(fieldName, value)
            );
        }
    }
}
