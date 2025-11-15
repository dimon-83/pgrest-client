package io.github.dimon83.examples.bootcrud;

import com.github.pgrest.client.PageResult;
import com.github.pgrest.client.PgQueryBuilder;
import com.github.pgrest.client.PgRestClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/users")
public class UserController {
    private final PgRestClient client;
    public UserController(PgRestClient client) { this.client = client; }

    @GetMapping
    public List<UserVO> list(@RequestParam(required = false) String status) {
        PgQueryBuilder b = new PgQueryBuilder();
        if (status != null) b.eq("status", status);
        return client.list("users", b.select("id,user_name,status"), UserVO.class);
    }

    @GetMapping("/page")
    public PageResult<UserVO> page(@RequestParam int page, @RequestParam int size) {
        return client.page("users", new PgQueryBuilder().orderDesc("created_at"), page, size, UserVO.class);
    }

    @PostMapping
    public List<UserVO> create(@RequestBody Map<String,Object> payload) {
        return client.insert("users", payload, new PgQueryBuilder().select("id,user_name,status"), UserVO.class);
    }

    @PatchMapping("/{id}")
    public List<UserVO> update(@PathVariable Long id, @RequestBody Map<String,Object> payload) {
        return client.update("users", new PgQueryBuilder().eq("id", id).select("id,user_name,status"), payload, UserVO.class);
    }

    @DeleteMapping("/{id}")
    public int delete(@PathVariable Long id) {
        return client.delete("users", new PgQueryBuilder().eq("id", id));
    }
}