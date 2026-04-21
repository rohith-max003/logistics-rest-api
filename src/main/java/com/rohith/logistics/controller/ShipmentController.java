package com.rohith.logistics.controller;

import com.rohith.logistics.model.Shipment;
import com.rohith.logistics.model.ShipmentStatus;
import com.rohith.logistics.model.TrackingEvent;
import com.rohith.logistics.service.ShipmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/shipments")
@RequiredArgsConstructor
@Tag(name = "Shipments", description = "Shipment management and tracking API")
public class ShipmentController {

    private final ShipmentService shipmentService;

    @PostMapping
    @Operation(summary = "Create a new shipment")
    public ResponseEntity<Shipment> createShipment(@Valid @RequestBody Shipment shipment) {
        log.info("Creating shipment from {} to {}", shipment.getOriginAddress(), shipment.getDestinationAddress());
        return ResponseEntity.status(HttpStatus.CREATED).body(shipmentService.create(shipment));
    }

    @GetMapping("/{trackingId}")
    @Operation(summary = "Track a shipment by tracking ID")
    public ResponseEntity<Shipment> getByTrackingId(@PathVariable String trackingId) {
        return ResponseEntity.ok(shipmentService.findByTrackingId(trackingId));
    }

    @GetMapping("/{id}/events")
    @Operation(summary = "Get full tracking event history")
    public ResponseEntity<List<TrackingEvent>> getTrackingEvents(@PathVariable Long id) {
        return ResponseEntity.ok(shipmentService.getTrackingEvents(id));
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "Update shipment status")
    public ResponseEntity<Shipment> updateStatus(
            @PathVariable Long id,
            @RequestParam ShipmentStatus status,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String description) {
        return ResponseEntity.ok(shipmentService.updateStatus(id, status, location, description));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Cancel a shipment")
    public ResponseEntity<Void> cancelShipment(@PathVariable Long id) {
        shipmentService.cancel(id);
        return ResponseEntity.noContent().build();
    }
}