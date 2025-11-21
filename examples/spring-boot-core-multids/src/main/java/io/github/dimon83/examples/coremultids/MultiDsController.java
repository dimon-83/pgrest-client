package io.github.dimon83.examples.coremultids;

import com.github.pgrest.client.PgQueryBuilder;
import com.github.pgrest.client.PgRestClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/core-multids")
public class MultiDsController {
    private final PgRestClient main;
    private final PgRestClient audit;

    public MultiDsController(@Qualifier("pgrestClient.main") PgRestClient main,
                             @Qualifier("pgrestClient.audit") PgRestClient audit) {
        this.main = main;
        this.audit = audit;
    }

    @GetMapping("/{resource}/main")
    public List<Map> listMain(@PathVariable String resource) {
        return main.list(resource, new PgQueryBuilder().limit(5), Map.class);
    }

    @GetMapping("/{resource}/audit")
    public List<Map> listAudit(@PathVariable String resource) {
        return audit.list(resource, new PgQueryBuilder().limit(5), Map.class);
    }
}