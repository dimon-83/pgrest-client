package com.github.pgrest.client.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@FeignClient(name = "${pgrest.gateway-service-name}", configuration = PgRestFeignConfig.class)
public interface PgRestGatewayFeignClient {
    @GetMapping("/api/pg/{resource}")
    ResponseEntity<List<Map<String, Object>>> list(@PathVariable("resource") String resource, @RequestParam Map<String, String> query);

    @GetMapping("/api/pg/{resource}")
    ResponseEntity<List<Map<String, Object>>> list(
            @PathVariable("resource") String resource,
            @RequestParam Map<String, String> query,
            @RequestHeader("Range") String range,
            @RequestHeader(value = "Range-Unit", required = false, defaultValue = "items") String rangeUnit);

    @PostMapping("/api/pg/{resource}")
    ResponseEntity<List<Map<String, Object>>> insert(@PathVariable("resource") String resource, @RequestBody Object payload, @RequestParam Map<String, String> query);

    @PatchMapping("/api/pg/{resource}")
    ResponseEntity<List<Map<String, Object>>> update(@PathVariable("resource") String resource, @RequestBody Object payload, @RequestParam Map<String, String> query);

    @DeleteMapping("/api/pg/{resource}")
    ResponseEntity<Integer> delete(@PathVariable("resource") String resource, @RequestParam Map<String, String> query);
}