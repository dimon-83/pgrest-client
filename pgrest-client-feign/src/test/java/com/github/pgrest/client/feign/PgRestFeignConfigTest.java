package com.github.pgrest.client.feign;

import feign.RequestTemplate;
import feign.RequestInterceptor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class PgRestFeignConfigTest {
    @Test
    public void interceptorAppliesPreferFromSupplier() {
        PgPreferSupplier supplier = () -> "handling=strict,count=exact";
        PgRestFeignConfig cfg = new PgRestFeignConfig();
        RequestInterceptor itc = cfg.pgRestHeadersInterceptor(supplier);
        RequestTemplate tpl = new RequestTemplate();
        itc.apply(tpl);
        String prefer = tpl.headers().get("Prefer").iterator().next();
        String accept = tpl.headers().get("Accept").iterator().next();
        Assertions.assertEquals("handling=strict,count=exact", prefer);
        Assertions.assertEquals("application/json", accept);
    }

    @Test
    public void interceptorDefaultsWhenNoSupplier() {
        PgRestFeignConfig cfg = new PgRestFeignConfig();
        RequestInterceptor itc = cfg.pgRestHeadersInterceptor(null);
        RequestTemplate tpl = new RequestTemplate();
        itc.apply(tpl);
        String prefer = tpl.headers().get("Prefer").iterator().next();
        Assertions.assertEquals("count=exact", prefer);
    }
}

