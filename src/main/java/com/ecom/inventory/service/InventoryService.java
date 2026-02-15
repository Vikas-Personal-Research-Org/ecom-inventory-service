package com.ecom.inventory.service;

import com.ecom.inventory.dto.InventoryRequest;
import com.ecom.inventory.dto.InventoryResponse;
import com.ecom.inventory.dto.StockReservationRequest;
import com.ecom.inventory.dto.StockReservationResponse;
import com.ecom.inventory.exception.InsufficientStockException;
import com.ecom.inventory.exception.InventoryNotFoundException;
import com.ecom.inventory.model.Inventory;
import com.ecom.inventory.model.InventoryEvent;
import com.ecom.inventory.model.InventoryEventType;
import com.ecom.inventory.repository.InventoryEventRepository;
import com.ecom.inventory.repository.InventoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final InventoryEventRepository inventoryEventRepository;

    public InventoryService(InventoryRepository inventoryRepository,
                            InventoryEventRepository inventoryEventRepository) {
        this.inventoryRepository = inventoryRepository;
        this.inventoryEventRepository = inventoryEventRepository;
    }

    public InventoryResponse getInventoryByProductId(Long productId) {
        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new InventoryNotFoundException(
                        "Inventory not found for product ID: " + productId));
        return mapToResponse(inventory);
    }

    public List<InventoryResponse> getAllInventory() {
        return inventoryRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public InventoryResponse addOrUpdateInventory(InventoryRequest request) {
        Inventory inventory = inventoryRepository.findByProductId(request.productId())
                .orElse(new Inventory());

        inventory.setProductId(request.productId());
        inventory.setQuantity(request.quantity());
        if (request.reorderLevel() != null) {
            inventory.setReorderLevel(request.reorderLevel());
        }

        Inventory saved = inventoryRepository.save(inventory);

        logEvent(saved.getProductId(), InventoryEventType.STOCK_UPDATED, request.quantity());

        if (saved.getQuantity() <= saved.getReorderLevel()) {
            logEvent(saved.getProductId(), InventoryEventType.LOW_STOCK_ALERT, saved.getQuantity());
        }

        return mapToResponse(saved);
    }

    public StockReservationResponse reserveStock(StockReservationRequest request) {
        Inventory inventory = inventoryRepository.findByProductId(request.productId())
                .orElseThrow(() -> new InventoryNotFoundException(
                        "Inventory not found for product ID: " + request.productId()));

        int available = inventory.getAvailableQuantity();
        if (available < request.quantity()) {
            throw new InsufficientStockException(
                    "Insufficient stock for product ID: " + request.productId()
                            + ". Available: " + available + ", Requested: " + request.quantity());
        }

        inventory.setReservedQuantity(inventory.getReservedQuantity() + request.quantity());
        inventoryRepository.save(inventory);

        logEvent(inventory.getProductId(), InventoryEventType.STOCK_RESERVED, request.quantity());

        if (inventory.getAvailableQuantity() <= inventory.getReorderLevel()) {
            logEvent(inventory.getProductId(), InventoryEventType.LOW_STOCK_ALERT, inventory.getAvailableQuantity());
        }

        return new StockReservationResponse(
                request.productId(),
                true,
                inventory.getAvailableQuantity(),
                "Stock reserved successfully"
        );
    }

    public StockReservationResponse releaseStock(StockReservationRequest request) {
        Inventory inventory = inventoryRepository.findByProductId(request.productId())
                .orElseThrow(() -> new InventoryNotFoundException(
                        "Inventory not found for product ID: " + request.productId()));

        int releaseQty = Math.min(request.quantity(), inventory.getReservedQuantity());
        inventory.setReservedQuantity(inventory.getReservedQuantity() - releaseQty);
        inventoryRepository.save(inventory);

        logEvent(inventory.getProductId(), InventoryEventType.STOCK_RELEASED, releaseQty);

        return new StockReservationResponse(
                request.productId(),
                true,
                inventory.getAvailableQuantity(),
                "Stock released successfully. Released: " + releaseQty
        );
    }

    public List<InventoryResponse> checkLowStock() {
        return inventoryRepository.findAll().stream()
                .filter(inv -> inv.getAvailableQuantity() <= inv.getReorderLevel())
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private void logEvent(Long productId, InventoryEventType eventType, Integer quantity) {
        InventoryEvent event = new InventoryEvent(productId, eventType, quantity, LocalDateTime.now());
        inventoryEventRepository.save(event);
    }

    private InventoryResponse mapToResponse(Inventory inventory) {
        return new InventoryResponse(
                inventory.getId(),
                inventory.getProductId(),
                inventory.getQuantity(),
                inventory.getReservedQuantity(),
                inventory.getAvailableQuantity(),
                inventory.getReorderLevel(),
                inventory.getLastUpdated()
        );
    }
}
