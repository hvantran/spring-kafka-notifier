package com.hoatv.kafka.notifier.controller;

import com.hoatv.kafka.notifier.dto.NotifierConfigurationRequest;
import com.hoatv.kafka.notifier.dto.NotifierConfigurationResponse;
import com.hoatv.kafka.notifier.service.NotifierConfigurationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/v1/notifier-configurations")
@RequiredArgsConstructor
@Validated
@Tag(name = "Notifier Configuration", description = "APIs for managing Kafka notifier configurations")
public class NotifierConfigurationController {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(NotifierConfigurationController.class);
    
    private final NotifierConfigurationService service;
    
    @Operation(summary = "Create a new notifier configuration")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Configuration created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input"),
        @ApiResponse(responseCode = "409", description = "Configuration already exists")
    })
    @PostMapping
    public ResponseEntity<NotifierConfigurationResponse> create(
            @Valid @RequestBody NotifierConfigurationRequest request) {
        LOGGER.info("Creating new notifier configuration for notifier: {}, topic: {}", 
                request.getNotifier(), request.getTopic());
        
        NotifierConfigurationResponse response = service.create(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
    
    @Operation(summary = "Update an existing notifier configuration")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Configuration updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input"),
        @ApiResponse(responseCode = "404", description = "Configuration not found"),
        @ApiResponse(responseCode = "409", description = "Configuration already exists with the same notifier/topic combination")
    })
    @PutMapping("/{id}")
    public ResponseEntity<NotifierConfigurationResponse> update(
            @Parameter(description = "Configuration ID") @PathVariable String id,
            @Valid @RequestBody NotifierConfigurationRequest request) {
        LOGGER.info("Updating notifier configuration with ID: {}", id);
        
        NotifierConfigurationResponse response = service.update(id, request);
        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "Get notifier configuration by ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Configuration found"),
        @ApiResponse(responseCode = "404", description = "Configuration not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<NotifierConfigurationResponse> findById(
            @Parameter(description = "Configuration ID") @PathVariable String id) {
        LOGGER.debug("Finding notifier configuration with ID: {}", id);
        
        NotifierConfigurationResponse response = service.findById(id);
        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "Get all notifier configurations with pagination")
    @ApiResponse(responseCode = "200", description = "Configurations retrieved successfully")
    @GetMapping
    public ResponseEntity<Page<NotifierConfigurationResponse>> findAll(
            @PageableDefault(size = 20) Pageable pageable) {
        LOGGER.debug("Finding all notifier configurations with pagination");
        
        Page<NotifierConfigurationResponse> response = service.findAll(pageable);
        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "Get notifier configurations by topic")
    @ApiResponse(responseCode = "200", description = "Configurations retrieved successfully")
    @GetMapping("/topic/{topic}")
    public ResponseEntity<List<NotifierConfigurationResponse>> findByTopic(
            @Parameter(description = "Kafka topic name") @PathVariable String topic) {
        LOGGER.debug("Finding notifier configurations for topic: {}", topic);
        
        List<NotifierConfigurationResponse> response = service.findByTopic(topic);
        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "Get all enabled notifier configurations")
    @ApiResponse(responseCode = "200", description = "Enabled configurations retrieved successfully")
    @GetMapping("/enabled")
    public ResponseEntity<List<NotifierConfigurationResponse>> findEnabledConfigurations() {
        LOGGER.debug("Finding all enabled notifier configurations");
        
        List<NotifierConfigurationResponse> response = service.findEnabledConfigurations();
        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "Toggle enabled status of a notifier configuration")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Status toggled successfully"),
        @ApiResponse(responseCode = "404", description = "Configuration not found")
    })
    @PatchMapping("/{id}/toggle")
    public ResponseEntity<NotifierConfigurationResponse> toggleEnabled(
            @Parameter(description = "Configuration ID") @PathVariable String id) {
        LOGGER.info("Toggling enabled status for notifier configuration with ID: {}", id);
        
        NotifierConfigurationResponse response = service.toggleEnabled(id);
        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "Delete a notifier configuration")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Configuration deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Configuration not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @Parameter(description = "Configuration ID") @PathVariable String id) {
        LOGGER.info("Deleting notifier configuration with ID: {}", id);
        
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
