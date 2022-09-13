package org.aag.testing.feigncircuittest;

import feign.Response;
import feign.codec.ErrorDecoder;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;

import static feign.FeignException.errorStatus;

public class FeignConfigErrorDecoder implements ErrorDecoder {
    @Override
    public Exception decode(String methodKey, Response response) {
        if (HttpStatus.valueOf(response.status()).is4xxClientError()){
            return new IgnorableException("This Exception SHOULDN't trigger opening the Circuit");
        }
        return errorStatus(methodKey, response);
    }
}