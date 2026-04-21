package com.rohith.logistics.service;

import com.rohith.logistics.exception.ShipmentNotFoundException;
import com.rohith.logistics.model.Shipment;
import com.rohith.logistics.model.ShipmentStatus;
import com.rohith.logistics.model.TrackingEvent;
import com.rohith.logistics.repository.ShipmentRepository;
import com.rohith.logistics.repository.TrackingEventRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShipmentService {

    private final ShipmentRepository shipmentRepository;
    private final TrackingEventRepository trackingEventRepository;

    @Transactional
    public Shipment create(Shipment shipment) {
        shipment.setStatus(ShipmentStatus.CREATED);
        Shipment saved = shipmentRepository.save(shipment);
        addTrackingEvent(saved, ShipmentStatus.CREATED, "Origin facility", "Shipment created and ready for pickup");
        log.info("Created shipment: {}", saved.getTrackingId());
        return saved;
    }

    @CircuitBreaker(name = "shipmentLookup", fallbackMethod = "fallbackFindByTrackingId")
    @Retry(name = "shipmentLookup")
    @Transactional(readOnly = true)
    public Shipment findByTrackingId(String trackingId) {
        return shipmentRepository.findByTrackingId(trackingId)
                .orElseThrow(() -> new ShipmentNotFoundException("Shipment not found: " + trackingId));
    }

    public Shipment fallbackFindByTrackingId(String trackingId, Exception e) {
        log.warn("Circuit breaker fallback for tracking ID: {} — {}", trackingId, e.getMessage());
        throw new ShipmentNotFoundException("Service temporarily unavailable. Try again shortly.");
    }

    @Transactional(readOnly = true)
    public List<TrackingEvent> getTrackingEvents(Long shipmentId) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new ShipmentNotFoundException("Shipment not found: " + shipmentId));
        return trackingEventRepository.findByShipmentOrderByEventTimeDesc(shipment);
    }

    @Transactional
    public Shipment updateStatus(Long id, ShipmentStatus status, String location, String description) {
        Shipment shipment = shipmentRepository.findById(id)
                .orElseThrow(() -> new ShipmentNotFoundException("Shipment not found: " + id));
        shipment.setStatus(status);
        if (status == ShipmentStatus.DELIVERED) {
            shipment.setActualDelivery(LocalDateTime.now());
        }
        Shipment updated = shipmentRepository.save(shipment);
        addTrackingEvent(updated, status, location, description);
        return updated;
    }

    @Transactional
    public void cancel(Long id) {
        Shipment shipment = shipmentRepository.findById(id)
                .orElseThrow(() -> new ShipmentNotFoundException("Shipment not found: " + id));
        shipment.setStatus(ShipmentStatus.CANCELLED);
        shipmentRepository.save(shipment);
        addTrackingEvent(shipment, ShipmentStatus.CANCELLED, null, "Shipment cancelled by customer request");
    }

    private void addTrackingEvent(Shipment shipment, ShipmentStatus status, String location, String description) {
        TrackingEvent event = TrackingEvent.builder()
                .shipment(shipment)
                .status(status)
                .location(location)
                .description(description)
                .eventTime(LocalDateTime.now())
                .build();
        trackingEventRepository.save(event);
    }
}