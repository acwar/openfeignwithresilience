package org.aag.testing.feigncircuittest;

import feign.Feign;
import feign.codec.ErrorDecoder;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.ThreadPoolBulkheadConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JCircuitBreakerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigBuilder;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4jBulkheadConfigurationBuilder;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4jBulkheadProvider;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;

import java.time.Duration;

@SpringBootApplication
@EnableFeignClients
public class FeignCircuitTestApplication {

    public static void main(String[] args) {
        SpringApplication.run(FeignCircuitTestApplication.class, args);
    }

    @Bean
    public FeignConfigErrorDecoder feignErrorDecoder(){
        return new FeignConfigErrorDecoder();
    }

    @Bean
    public Customizer<Resilience4JCircuitBreakerFactory> defaultCustomizer() {
        return factory -> factory.configureDefault(id -> new Resilience4JConfigBuilder(id)
                .timeLimiterConfig(TimeLimiterConfig.ofDefaults())
                .circuitBreakerConfig(CircuitBreakerConfig.custom()
                        .minimumNumberOfCalls(5)
                        .slidingWindowSize(10)
                        .ignoreException(throwable -> {
                            return throwable instanceof IgnorableException || (throwable.getCause() != null && throwable.getCause() instanceof IgnorableException);
                        })
                        .build())
                .build());
    }

//    @Bean
//    public Customizer<Resilience4jBulkheadProvider> defaultBulkheadCustomizer() {
//        return provider -> provider.configure(builder -> builder
//                .bulkheadConfig(BulkheadConfig.custom().maxConcurrentCalls(4).build())
//                .threadPoolBulkheadConfig(ThreadPoolBulkheadConfig.custom().coreThreadPoolSize(1).maxThreadPoolSize(1).build())
//                .build()
//        );
//    }
//
//    @Bean
//    public Customizer<Resilience4jBulkheadProvider> slowBulkheadProviderCustomizer() {
//        return provider -> provider.configure(builder -> builder
//                .bulkheadConfig(BulkheadConfig.custom().maxConcurrentCalls(1).build())
//                .threadPoolBulkheadConfig(ThreadPoolBulkheadConfig.ofDefaults()), "testService");
//    }

}
