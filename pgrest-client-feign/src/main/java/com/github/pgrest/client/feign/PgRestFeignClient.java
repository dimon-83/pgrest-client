package com.github.pgrest.client.feign;

import com.github.pgrest.client.PageResult;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@FeignClient(name = "${pgrest.service-name:${spring.application.name}}", contextId = "${pgrest.service-name:${spring.application.name}}-pgrestClient", configuration = PgRestFeignConfig.class)
public interface PgRestFeignClient {
    @GetMapping("/pgrest/{resource}")
    ResponseEntity<List<Map<String, Object>>> list(@PathVariable("resource") String resource, @RequestParam Map<String, String> query);

    @GetMapping("/pgrest/{resource}")
    ResponseEntity<List<Map<String, Object>>> list(
            @PathVariable("resource") String resource,
            @RequestParam Map<String, String> query,
            @RequestHeader("Range") String range,
            @RequestHeader(value = "Range-Unit", required = false, defaultValue = "items") String rangeUnit);

    @PostMapping("/pgrest/{resource}")
    ResponseEntity<List<Map<String, Object>>> insert(@PathVariable("resource") String resource, @RequestBody Object payload, @RequestParam Map<String, String> query);

    @PatchMapping("/pgrest/{resource}")
    ResponseEntity<List<Map<String, Object>>> update(@PathVariable("resource") String resource, @RequestBody Object payload, @RequestParam Map<String, String> query);

    @DeleteMapping("/pgrest/{resource}")
    ResponseEntity<Integer> delete(@PathVariable("resource") String resource, @RequestParam Map<String, String> query);

    @GetMapping("/pgrest/{resource}/page")
    ResponseEntity<PageResult<Map<String,Object>>> page(@PathVariable("resource") String resource,
                                                        @RequestParam Map<String,String> query,
                                                        @RequestParam("page") int page,
                                                        @RequestParam("size") int size);
}