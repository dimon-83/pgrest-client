package io.github.dimon83.examples.bootcrud;

import com.github.pgrest.client.feign.PgRestFeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@FeignClient(name = "${pgrest.gateway-service-name}", configuration = PgRestFeignConfig.class, contextId = "usersFeignExample")
public interface UsersFeignClient {
    @GetMapping("/api/pg/users")
    List<UserVO> list(@RequestParam Map<String,String> query);

    @PostMapping("/api/pg/users")
    List<UserVO> insert(@RequestBody Map<String,Object> payload, @RequestParam Map<String,String> query);

    @PatchMapping("/api/pg/users")
    List<UserVO> update(@RequestBody Map<String,Object> payload, @RequestParam Map<String,String> query);

    @DeleteMapping("/api/pg/users")
    Integer delete(@RequestParam Map<String,String> query);
}