package org.aag.testing.feigncircuittest;

import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import org.springframework.stereotype.Service;

@Service
public class FeignClientService {

    private FeignService client;

    public FeignClientService(FeignService client) {
        this.client = client;
    }
    //@Bulkhead(name = "default", type = Bulkhead.Type.THREADPOOL)
    public String getAllStores(int status){
        return client.getAllStores(status);
    }
}
