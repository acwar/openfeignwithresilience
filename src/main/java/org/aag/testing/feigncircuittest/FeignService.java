package org.aag.testing.feigncircuittest;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@FeignClient(value = "testService", url = "http://localhost:8080/", fallbackFactory = FallBackMethods.class)
public interface FeignService {

    @RequestMapping(method = RequestMethod.GET, value = "/stores/{statusToRaise}")
    String getAllStores(@PathVariable int statusToRaise);

}
