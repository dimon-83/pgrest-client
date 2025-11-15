package io.github.dimon83.examples.bootcrud;

import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/feign/users")
public class UsersFeignExampleController {
    private final UsersFeignClient feign;
    public UsersFeignExampleController(UsersFeignClient feign) { this.feign = feign; }

    @GetMapping
    public List<UserVO> list(@RequestParam Map<String,String> query) {
        return feign.list(query);
    }

    @PostMapping
    public List<UserVO> create(@RequestBody Map<String,Object> payload) {
        return feign.insert(payload, Map.of("select","id,user_name,status"));
    }

    @PatchMapping("/{id}")
    public List<UserVO> update(@PathVariable Long id, @RequestBody Map<String,Object> payload) {
        return feign.update(payload, Map.of("id","eq." + id, "select","id,user_name,status"));
    }

    @DeleteMapping("/{id}")
    public Integer delete(@PathVariable Long id) {
        return feign.delete(Map.of("id","eq." + id));
    }
}