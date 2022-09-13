package org.aag.testing.feigncircuittest;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Slf4j
@Component
class FallBackMethods implements FallbackFactory<FallbackWithFactory> {

    @Override
    public FallbackWithFactory create(Throwable cause) {
        log.info(cause.getMessage());
        return new FallbackWithFactory();
    }

}
class FallbackWithFactory implements FeignService {

    @Override
    public String getAllStores(int statusToRaise){
        return "fallbacked";
    }

}
