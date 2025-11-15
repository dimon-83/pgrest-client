package io.github.dimon83.examples.bootcrud;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients(basePackages = "io.github.dimon83.examples.bootcrud")
public class BootCrudApplication {
    public static void main(String[] args) {
        SpringApplication.run(BootCrudApplication.class, args);
    }
}