package com.ecom.inventory.repository;

import com.ecom.inventory.model.InventoryEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InventoryEventRepository extends JpaRepository<InventoryEvent, Long> {

    List<InventoryEvent> findByProductId(Long productId);
}
