package com.ecom.inventory.dto;

public record StockReservationResponse(
        Long productId,
        boolean reserved,
        Integer availableQuantity,
        String message
) {
}
