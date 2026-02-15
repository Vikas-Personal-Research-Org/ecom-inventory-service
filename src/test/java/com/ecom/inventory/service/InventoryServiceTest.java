package com.ecom.inventory.service;

import com.ecom.inventory.dto.InventoryRequest;
import com.ecom.inventory.dto.InventoryResponse;
import com.ecom.inventory.dto.StockReservationRequest;
import com.ecom.inventory.dto.StockReservationResponse;
import com.ecom.inventory.exception.InsufficientStockException;
import com.ecom.inventory.exception.InventoryNotFoundException;
import com.ecom.inventory.model.Inventory;
import com.ecom.inventory.model.InventoryEvent;
import com.ecom.inventory.repository.InventoryEventRepository;
import com.ecom.inventory.repository.InventoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private InventoryEventRepository inventoryEventRepository;

    @InjectMocks
    private InventoryService inventoryService;

    private Inventory inventory;

    @BeforeEach
    void setUp() {
        inventory = new Inventory(1L, 100, 10, 10);
        inventory.setId(1L);
        inventory.setLastUpdated(LocalDateTime.now());
    }

    @Test
    void getInventoryByProductId_shouldReturnInventory() {
        when(inventoryRepository.findByProductId(1L)).thenReturn(Optional.of(inventory));

        InventoryResponse response = inventoryService.getInventoryByProductId(1L);

        assertNotNull(response);
        assertEquals(1L, response.productId());
        assertEquals(100, response.quantity());
        assertEquals(10, response.reservedQuantity());
        assertEquals(90, response.availableQuantity());
    }

    @Test
    void getInventoryByProductId_shouldThrowWhenNotFound() {
        when(inventoryRepository.findByProductId(999L)).thenReturn(Optional.empty());

        assertThrows(InventoryNotFoundException.class,
                () -> inventoryService.getInventoryByProductId(999L));
    }

    @Test
    void getAllInventory_shouldReturnAllItems() {
        Inventory inventory2 = new Inventory(2L, 50, 5, 10);
        inventory2.setId(2L);
        inventory2.setLastUpdated(LocalDateTime.now());

        when(inventoryRepository.findAll()).thenReturn(Arrays.asList(inventory, inventory2));

        List<InventoryResponse> responses = inventoryService.getAllInventory();

        assertEquals(2, responses.size());
    }

    @Test
    void addOrUpdateInventory_shouldCreateNew() {
        InventoryRequest request = new InventoryRequest(99L, 50, 15);

        when(inventoryRepository.findByProductId(99L)).thenReturn(Optional.empty());
        when(inventoryRepository.save(any(Inventory.class))).thenAnswer(invocation -> {
            Inventory saved = invocation.getArgument(0);
            saved.setId(10L);
            saved.setLastUpdated(LocalDateTime.now());
            return saved;
        });
        when(inventoryEventRepository.save(any(InventoryEvent.class))).thenReturn(new InventoryEvent());

        InventoryResponse response = inventoryService.addOrUpdateInventory(request);

        assertNotNull(response);
        assertEquals(99L, response.productId());
        assertEquals(50, response.quantity());
        verify(inventoryRepository).save(any(Inventory.class));
    }

    @Test
    void addOrUpdateInventory_shouldUpdateExisting() {
        InventoryRequest request = new InventoryRequest(1L, 200, 20);

        when(inventoryRepository.findByProductId(1L)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(Inventory.class))).thenAnswer(invocation -> {
            Inventory saved = invocation.getArgument(0);
            saved.setLastUpdated(LocalDateTime.now());
            return saved;
        });
        when(inventoryEventRepository.save(any(InventoryEvent.class))).thenReturn(new InventoryEvent());

        InventoryResponse response = inventoryService.addOrUpdateInventory(request);

        assertEquals(200, response.quantity());
        assertEquals(20, response.reorderLevel());
    }

    @Test
    void reserveStock_shouldReserveSuccessfully() {
        StockReservationRequest request = new StockReservationRequest(1L, 20);

        when(inventoryRepository.findByProductId(1L)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(inventory);
        when(inventoryEventRepository.save(any(InventoryEvent.class))).thenReturn(new InventoryEvent());

        StockReservationResponse response = inventoryService.reserveStock(request);

        assertTrue(response.reserved());
        assertEquals(1L, response.productId());
        verify(inventoryRepository).save(any(Inventory.class));
    }

    @Test
    void reserveStock_shouldThrowWhenInsufficientStock() {
        StockReservationRequest request = new StockReservationRequest(1L, 200);

        when(inventoryRepository.findByProductId(1L)).thenReturn(Optional.of(inventory));

        assertThrows(InsufficientStockException.class,
                () -> inventoryService.reserveStock(request));
    }

    @Test
    void reserveStock_shouldThrowWhenProductNotFound() {
        StockReservationRequest request = new StockReservationRequest(999L, 10);

        when(inventoryRepository.findByProductId(999L)).thenReturn(Optional.empty());

        assertThrows(InventoryNotFoundException.class,
                () -> inventoryService.reserveStock(request));
    }

    @Test
    void releaseStock_shouldReleaseSuccessfully() {
        StockReservationRequest request = new StockReservationRequest(1L, 5);

        when(inventoryRepository.findByProductId(1L)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(inventory);
        when(inventoryEventRepository.save(any(InventoryEvent.class))).thenReturn(new InventoryEvent());

        StockReservationResponse response = inventoryService.releaseStock(request);

        assertTrue(response.reserved());
        assertEquals(1L, response.productId());
    }

    @Test
    void releaseStock_shouldThrowWhenProductNotFound() {
        StockReservationRequest request = new StockReservationRequest(999L, 5);

        when(inventoryRepository.findByProductId(999L)).thenReturn(Optional.empty());

        assertThrows(InventoryNotFoundException.class,
                () -> inventoryService.releaseStock(request));
    }

    @Test
    void checkLowStock_shouldReturnLowStockItems() {
        Inventory lowStock = new Inventory(3L, 5, 0, 10);
        lowStock.setId(3L);
        lowStock.setLastUpdated(LocalDateTime.now());

        when(inventoryRepository.findAll()).thenReturn(Arrays.asList(inventory, lowStock));

        List<InventoryResponse> responses = inventoryService.checkLowStock();

        assertEquals(1, responses.size());
        assertEquals(3L, responses.get(0).productId());
    }

    @Test
    void reserveStock_shouldLogEvent() {
        StockReservationRequest request = new StockReservationRequest(1L, 5);

        when(inventoryRepository.findByProductId(1L)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(inventory);
        when(inventoryEventRepository.save(any(InventoryEvent.class))).thenReturn(new InventoryEvent());

        inventoryService.reserveStock(request);

        verify(inventoryEventRepository, times(1)).save(any(InventoryEvent.class));
    }
}
