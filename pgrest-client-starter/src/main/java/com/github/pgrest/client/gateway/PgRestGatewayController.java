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
@RequestMapping(path = "/api/pg", produces = MediaType.APPLICATION_JSON_VALUE)
public class PgRestGatewayController {
    private final PgRestClient client;

    public PgRestGatewayController(PgRestClient client) { this.client = client; }

    @GetMapping("/{resource}")
    public ResponseEntity<List<Map>> list(@PathVariable("resource") String resource,
                                          @RequestParam Map<String, String> query,
                                          @RequestHeader(value = "Range", required = false) String range,
                                          @RequestHeader(value = "Range-Unit", required = false) String rangeUnit) {
        PgQueryBuilder b = PgQueryBuilder.fromQuery(query);
        if (range != null && (rangeUnit == null || "items".equalsIgnoreCase(rangeUnit))) {
            int dash = range.indexOf('-');
            if (dash > 0) {
                try {
                    int start = Integer.parseInt(range.substring(0, dash).trim());
                    int end = Integer.parseInt(range.substring(dash + 1).trim());
                    int size = end - start + 1;
                    if (size > 0) b.limit(size);
                    if (start >= 0) b.offset(start);
                    int page = size > 0 ? (start / size) + 1 : 1;
                    PageResult<Map> pr = client.page(resource, b, page, size, Map.class);
                    org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
                    headers.add("Content-Range", "items " + start + "-" + end + "/" + pr.getTotal());
                    return ResponseEntity.ok().headers(headers).body((List) pr.getItems());
                } catch (Exception ignored) {}
            }
        }
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
}