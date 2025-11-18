package com.github.pgrest.client.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@FeignClient(name = "${pgrest.service-name}", contextId = "${pgrest.service-name}-pgrestClient", configuration = PgRestFeignConfig.class)
public interface PgRestDirectFeignClient {
    @GetMapping("/{resource}")
    ResponseEntity<List<Map<String, Object>>> list(@PathVariable("resource") String resource, @RequestParam Map<String, String> query);

    @GetMapping("/{resource}")
    ResponseEntity<List<Map<String, Object>>> list(
            @PathVariable("resource") String resource,
            @RequestParam Map<String, String> query,
            @RequestHeader("Range") String range,
            @RequestHeader(value = "Range-Unit", required = false, defaultValue = "items") String rangeUnit);

    @PostMapping("/{resource}")
    ResponseEntity<List<Map<String, Object>>> insert(@PathVariable("resource") String resource, @RequestBody Object payload, @RequestParam Map<String, String> query);

    @PatchMapping("/{resource}")
    ResponseEntity<List<Map<String, Object>>> update(@PathVariable("resource") String resource, @RequestBody Object payload, @RequestParam Map<String, String> query);

    @DeleteMapping("/{resource}")
    ResponseEntity<Void> delete(@PathVariable("resource") String resource, @RequestParam Map<String, String> query);
}
