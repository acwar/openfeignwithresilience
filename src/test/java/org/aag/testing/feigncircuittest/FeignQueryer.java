package org.aag.testing.feigncircuittest;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.vavr.collection.Seq;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@Slf4j
@SpringBootTest(webEnvironment= SpringBootTest.WebEnvironment.DEFINED_PORT, properties = "spring.main.allow-circular-references=true")
@ExtendWith(SpringExtension.class)
@WireMockTest(httpPort = 8080)
public class FeignQueryer {

    private FeignClientService subject;

    private CircuitBreakerRegistry circuitBreakerRegistry;
    private CircuitBreaker circuitBreaker;

    @Autowired
    public FeignQueryer(FeignClientService client
            ,CircuitBreakerRegistry registry){
        this.subject = client;
        this.circuitBreakerRegistry = registry;

        assertNotNull(subject.getAllStores(200));

        Seq<CircuitBreaker> allCircuitBreakers = circuitBreakerRegistry.getAllCircuitBreakers();
        allCircuitBreakers.iterator().forEach(breaker -> log.info(breaker.getName()));

        circuitBreaker = allCircuitBreakers.get(0);
        log.info("The state of the circuit is currently {}", circuitBreaker.getState());
    }


    /**
     * Taylored test to check the closing window behaviuor
     *
     * 10 calls are maded for a 500 code to show if the circuit get opened.
     * Ass the 500 code is not decoded as and IgnoreException they should count as fail to open the state of the circuit
     */
    @Test
    void TestClient(WireMockRuntimeInfo wmRuntimeInfo) {
        log.info("Test a proper closing window");
        WireMock wireMock = wmRuntimeInfo.getWireMock();
        wireMock.register(WireMock.get("/stores/500").willReturn(WireMock.serverError().withFixedDelay(200)));
        int i = 0;
        for (i = 0; i < 10; i++)
            subject.getAllStores(500);

    }

    /**
     * Taylored test to check the ignore exception
     *
     * 10 calls are maded for a 400 code to show if the circuit get opened.
     * Ass the 400 code is decoded as and IgnoreException they shouldn't count as fail to open the state of the circuit
     */
    @Test
    void TestIgnoredException(WireMockRuntimeInfo wmRuntimeInfo){
        log.info("Test an ignoring exception");
        WireMock wireMock = wmRuntimeInfo.getWireMock();
        wireMock.register(WireMock.get("/stores/400").willReturn(WireMock.badRequest().withFixedDelay(200)));
        int i = 0;
        for (i = 0; i < 100; i++)
            subject.getAllStores(400);

    }

    /**
     * Test for the Bulkhead Pattern. An executor with 10 thread exists to execute a Bulkheaded
     * Feign cliente with a Thread limit defined in properties. It's to be expected a
     * "Bulkhead 'testService' is full and does not permit further calls" once the bulkhead reach it's petition
     * process limit, this is, maxThreadPoolSize + queueCapacity
     * @param wmRuntimeInfo
     */
    @Test
    void TestBulkHead(WireMockRuntimeInfo wmRuntimeInfo) throws Exception {
        int i = 0;

        wmRuntimeInfo
                .getWireMock()
                .register(
                    WireMock.get("/stores/400")
                    .willReturn(WireMock.badRequest())
                );

        List<Callable<String>> callables = new ArrayList<>();
        ExecutorService executorService = Executors.newFixedThreadPool(10);

        log.info("Feeding 100 petitions to a 10 Threads executor");
        for (i = 0; i < 100; i++)
            callables.add(new GetStoresAsync(subject));
        List<Future<String>> futures = executorService.invokeAll(callables);

        executorService.shutdown();
        while (!executorService.isTerminated()){
            log.info("Waiting for Executor to finish");
            Thread.sleep(2000);
        }
        for (Future<String> stringFuture : futures) {
            log.info(stringFuture.get());
        }
    }

    @BeforeEach
    void b4(){
        circuitBreaker.reset();
    }

}
@Slf4j
class GetStoresAsync implements Callable<String> {

    private FeignClientService subject;

    public GetStoresAsync(FeignClientService subject) {
        this.subject = subject;
    }

    @Override
    public String call() throws Exception {
        log.info("Calling getAllStores");
        Thread.sleep(400);
        return subject.getAllStores(400);
    }
}
