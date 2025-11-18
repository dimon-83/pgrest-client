package com.github.pgrest.client.gateway;

import com.github.pgrest.client.PgQueryBuilder;
import com.github.pgrest.client.PgRestClient;
import com.github.pgrest.client.PageResult;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(path = "/pgrest", produces = MediaType.APPLICATION_JSON_VALUE)
public class PgRestController {
    private final PgRestClient client;

    public PgRestController(PgRestClient client) { this.client = client; }

    @GetMapping("/{resource}")
    public ResponseEntity<List<Map>> list(@PathVariable("resource") String resource,
                                          @RequestParam Map<String, String> query,
                                          @RequestHeader(value = "Range", required = false) String range,
                                          @RequestHeader(value = "Range-Unit", required = false) String rangeUnit) {
        PgQueryBuilder b = PgQueryBuilder.fromQuery(query);
        List<Map> list = client.list(resource, b, Map.class);
        return ResponseEntity.ok(list);
    }

    @PostMapping("/{resource}")
    public List<Map> insert(@PathVariable("resource") String resource, @RequestBody Object payload, @RequestParam Map<String, String> query) {
        PgQueryBuilder b = PgQueryBuilder.fromQuery(query);
        return client.insert(resource, payload, b, Map.class);
    }

    @PatchMapping("/{resource}")
    public List<Map> update(@PathVariable("resource") String resource, @RequestBody Object payload, @RequestParam Map<String, String> query) {
        PgQueryBuilder b = PgQueryBuilder.fromQuery(query);
        return client.update(resource, b, payload, Map.class);
    }

    @DeleteMapping("/{resource}")
    public int delete(@PathVariable("resource") String resource, @RequestParam Map<String, String> query) {
        PgQueryBuilder b = PgQueryBuilder.fromQuery(query);
        return client.delete(resource, b);
    }

    @GetMapping("/{resource}/page")
    public PageResult<Map> page(@PathVariable("resource") String resource,
                                @RequestParam Map<String, String> query,
                                @RequestParam("page") int page,
                                @RequestParam("size") int size) {
        java.util.Map<String,String> q = new java.util.HashMap<>(query == null ? java.util.Collections.emptyMap() : query);
        q.remove("page");
        q.remove("size");
        PgQueryBuilder b = PgQueryBuilder.fromQuery(q);
        return client.page(resource, b, page, size, Map.class);
    }
}