# Foreword
The services identified to be configured in Resillence4J that were present in Histryx through OpenFeign are
- Circuit Breaking
- TIme Limiter
- Execution Isolation

The retry mechanism is not implemented in Histryx and for so is not migrated in Circuit breaking, although it is present in OpenFeign by itself (we'll dive in detail bellow)

## A note on test's mechanism
In order to check if the given configuration is properly working, a minimal project for testing purposes has been created and stored in my personal TWS Bitbucket repo LeREpo. It consists in a OpenFeign client for which the Circuit Breaker configuration is going to be tested against. The server on which the client interacts is mocked with _WireMock_

# TimeLimiter
This is the most straighforward migration of all, just a simple property
```yaml
resilience4j:  
  timeLimiter:  
    configs:  
      default:  
        timeoutDuration: 30s
```

Which is the equivalent of

```yaml
hystrix.command: 
  default: 
    execution.isolation.thread.timeoutInMilliseconds: 820
```

> NOTE: The Timeout in the resilience4j is a `Duration`, meaning that you have to specify not only the timeOut but also the unit

# Circuit Breaker
## Properties
From the Histryx present configuration
- [requestVolumeThreshold](https://github.com/Netflix/Hystrix/wiki/Configuration#circuitbreakerrequestvolumethreshold): This property sets the minimum number of requests in a rolling window that will trip the circuit.
- [errorThresholdPercentage](https://github.com/Netflix/Hystrix/wiki/Configuration#circuitBreaker.errorThresholdPercentage): the error percentage at or above which the circuit should trip open and start short-circuiting requests to fallback logic.
- [sleepWindowInMilliseconds](https://github.com/Netflix/Hystrix/wiki/Configuration#circuitBreaker.sleepWindowInMilliseconds): This property sets the amount of time, after tripping the circuit, to reject requests before allowing attempts again to determine if the circuit should again be closed.

We can stablish a one to one correlation with the [ResilienceCircuitBreaker](https://resilience4j.readme.io/docs/circuitbreaker#create-and-configure-a-circuitbreaker) through
- minimumNumberOfCalls: Configures the minimum number of calls which are required (per sliding window period) before the CircuitBreaker can calculate the error rate or slow call rate.
- failureRateThreshold: When the failure rate is equal or greater than the threshold the CircuitBreaker transitions to open and starts short-circuiting calls.
- waitDurationInOpenState: The time that the CircuitBreaker should wait before transitioning from open to half-open.

## Exceptions
The are currently a collection of execeptions implemented across the services which implements `ExceptionNotWrappedByHystrix` and get automatcally ignored by the circuit breaking mechanism.

The equivalent on _Resilience4j_ is the `ignoreExceptions` property, but in the example project I've unable to make them work by  simply pointing them. The breaker logic that evaluates the state of the _window_, `io.github.resilience4j.circuitbreaker.internal.CircuitBreakerStateMachine`, **doesn´t** have any logic around any list of exceptions whatsoever. In the provided [demo](https://github.com/resilience4j/resilience4j-spring-boot2-demo) from the _Resilience4j_ documentation there isn't any test showing this behaviour, although it's configured. There is even a [PR ongoing](https://github.com/resilience4j/resilience4j/issues/1586) to fix this problem
To make them work in the mean time, an `ignoreExceptionPredicate` can be supplied to evaluate the _thrown exceptions_
```java 
public class IgnoreExceptionPredicate implements Predicate<Throwable> {
	@Override
	public boolean test(Throwable throwable) {
		return throwable instanceof IgnorableException || (throwable.getCause() != null && throwable.getCause() instanceof IgnorableException);
	}
}
```
> The ignored Exceptions list will have to be supplied to make the predicate work, this example is just a particularization


# Retry

There is no retry mechanics in _Hystrix_ to be migrated to _Resilience4J._ However the OpenFeign **does have** a retry mechanism, as stated in the Spring Cloud Open Feign [documentation](https://docs.spring.io/spring-cloud-openfeign/docs/current/reference/html/#spring-cloud-feign-overriding-defaults "Follow link")

>Spring Cloud OpenFeign does not provide the following beans by default for feign, but still looks up beans of these types from the application context to create the feign client:
...
Retryer
...
A bean of Retryer.NEVER_RETRY with the type Retryer is created by default, which will disable retrying

If needed, a _Retryer_ bean has to be created in the context to provide that functionality to the Feign client

# Execution Isolation

Known has the _Bulkhead_ pattern, Hystrix is built in the core of Hystrix, so the OpenFeign client support's it out of the box. _Resilience4j_ also has a _bulkhead_ pattern solution that [activates](https://github.com/spring-cloud/spring-cloud-circuitbreaker/blob/main/README.adoc#116-bulkhead-pattern-supporting "Follow link") if _resilience4j-bulkhead_ is present on the classpath (this dependency is not included in SC Circuit Breaker to start with) but it's implementation to work with threads is only suitable for _CompletableFuture<>_ return types (reactive applications).

Spring Cloud OpenFeign [does not support](https://docs.spring.io/spring-cloud-openfeign/docs/current/reference/html/#reactive-support "Follow link") reactive clients.

## Mitigations

Assuming that the threadpool config for the feign client is needed (that need to be asserted)

-   Semaphore bulkhead: NOT TESTED YET, UNKNOWN TO WORK
-   Refactor to add support to [Reactive-Feign](https://github.com/PlaytikaOSS/feign-reactive "Follow link") (recomended by Pivotal itself)