package com.ecom.inventory.dto;

import java.time.LocalDateTime;

public record InventoryResponse(
        Long id,
        Long productId,
        Integer quantity,
        Integer reservedQuantity,
        Integer availableQuantity,
        Integer reorderLevel,
        LocalDateTime lastUpdated
) {
}
