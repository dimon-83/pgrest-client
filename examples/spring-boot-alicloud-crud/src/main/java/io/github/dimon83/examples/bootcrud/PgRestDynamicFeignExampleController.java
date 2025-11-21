package io.github.dimon83.examples.bootcrud;

import com.github.pgrest.client.feign.PgRestDirectFeignClient;
import com.github.pgrest.client.feign.PgRestFeignClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class PgRestDynamicFeignExampleController implements ApplicationRunner {
    private final PgRestFeignClient aGateway;
    private final PgRestDirectFeignClient bDirect;

    public PgRestDynamicFeignExampleController(@Qualifier("pgrestFeignClient.a-service") PgRestFeignClient aGateway,
                                               @Qualifier("pgrestDirectFeignClient.b-service") PgRestDirectFeignClient bDirect) {
        this.aGateway = aGateway;
        this.bDirect = bDirect;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            ResponseEntity<List<Map<String,Object>>> a = aGateway.list("users", Map.of("select","id,user_name,status"));
            System.out.println("A service users size=" + (a.getBody() == null ? 0 : a.getBody().size()));
        } catch (Exception e) {
            System.out.println("A service call failed: " + e.getMessage());
        }
        try {
            ResponseEntity<List<Map<String,Object>>> b = bDirect.list("users", Map.of("select","id,user_name,status"));
            System.out.println("B service users size=" + (b.getBody() == null ? 0 : b.getBody().size()));
        } catch (Exception e) {
            System.out.println("B service call failed: " + e.getMessage());
        }
    }
}