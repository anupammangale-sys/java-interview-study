# Episode 12 demos

Spring Boot 3.5.3 on Java 24. One application that runs every demo on startup,
makes real HTTP calls to itself, prints what happened and exits.

```bash
mvn spring-boot:run
```

Everything below is captured output from that command. Nothing is described
from memory.

## 1. The self call trap

The most asked Spring transaction question, and the answer is visible rather
than argued:

```
A. calling the transactional method from OUTSIDE the bean
  inside saveThenFail: transaction active? true
  rows left in the database: 0   rolled back, as expected

B. the same method, reached by a call from INSIDE the same bean
  outer: transaction active? false
  inside saveThenFail: transaction active? false
  rows left in the database: 1   NOT rolled back, the data survived
```

**Same method. Same exception. Outside: 0 rows. Inside: 1 row.**

`@Transactional` works by wrapping the bean in a proxy. Spring hands everyone
else the proxy, not your object. `this.something()` never leaves the object, so
the proxy is not involved, so no transaction is ever started. The
`transaction active? false` line is that fact, printed from inside the method.

## 2. I caught the exception, so why did it roll back

```
A. inner method uses REQUIRED, the default
  outer caught: inner failed, carrying on
  UnexpectedRollbackException: Transaction silently rolled back because it has
  been marked as rollback-only
  rows left: 0   the outer save is gone too

B. inner method uses REQUIRES_NEW
  outer caught: inner failed, carrying on
  outer finished normally
  rows left: 1   only the inner one rolled back
```

With `REQUIRED` the inner method joins the caller's transaction. When it throws,
Spring marks that one shared transaction rollback only. Catching the exception
does not unmark it, so the commit at the end fails and the outer save is lost
too.

## 3. Singleton, prototype, and the trap between them

```
same singleton three times:            serial 1, 1, 1
a prototype three times:               serial 2, 3, 4

a prototype INJECTED into a singleton, called three times:
                                       serial 1, 1, 1   the scope did nothing
the same singleton using ObjectProvider:
                                       serial 5, 6, 7   a new one each time

Prototype instances created in total: 7
```

A prototype injected into a singleton is resolved once, when the singleton is
built. After that the singleton holds one instance for ever. `ObjectProvider` is
a handle to the factory rather than to an instance, so asking it each time gives
a new object.

## 4. What a request passes through

Read out of the running context, not from a diagram:

```
servlet filters, in order:
  OrderedCharacterEncodingFilter
  OrderedFormContentFilter
  OrderedRequestContextFilter

HandlerMappings, in the order DispatcherServlet asks them:
  RouterFunctionMapping
  RequestMappingHandlerMapping
  WelcomePageHandlerMapping
  BeanNameUrlHandlerMapping
  WelcomePageNotAcceptableHandlerMapping
  SimpleUrlHandlerMapping

HandlerAdapters:
  RequestMappingHandlerAdapter
  HandlerFunctionAdapter
  HttpRequestHandlerAdapter
  SimpleControllerHandlerAdapter

message converters, 9 of them:
  ByteArrayHttpMessageConverter              [application/octet-stream, */*]
  StringHttpMessageConverter                 [text/plain, */*]
  StringHttpMessageConverter                 [text/plain, */*]
  ResourceHttpMessageConverter               [*/*]
  ResourceRegionHttpMessageConverter         [*/*]
  AllEncompassingFormHttpMessageConverter    [form and multipart types]
  MappingJackson2HttpMessageConverter        [application/json, application/*+json]
  MappingJackson2HttpMessageConverter        [application/json, application/*+json]
  Jaxb2RootElementHttpMessageConverter       [application/xml, text/xml]
  distinct objects among those 9: 9
```

Two of those converters appear twice with identical media types, and the
identity check confirms they are **nine separate objects**, so it is two
registrations rather than one object listed twice. The first converter that can
handle the type wins, so the repeat further down is never reached. Harmless, and
worth seeing.

## 5. @Controller and @RestController

Real HTTP calls against the running application:

```
/api/text                     200  text/plain          hello
/api/json                     200  application/json    {"message":"hello","count":3}
/api/order/A-77?count=4       200  application/json    {"message":"order A-77","count":4}
/mvc/text                     404  application/json    {"status":404,"error":"Not Found"}
/mvc/text-with-responsebody   200  text/plain          hello
```

**The two `text` methods have identical bodies.** `return "hello";` from a
`@RestController` is the response. The same line from a plain `@Controller` is
the name of a view to render, nothing can find a view called "hello", and the
request ends as a **404**. Adding `@ResponseBody` to that method makes it behave
like the first one, because `@RestController` is exactly `@Controller` plus
`@ResponseBody`.

## 6. One place that turns exceptions into responses

```
/api/boom       409  application/problem+json
  {"type":"about:blank","title":"Out of stock","status":409,
   "detail":"out of stock: SKU-42","instance":"/api/boom","sku":"SKU-42"}

/api/unhandled  500  application/json
  {"status":500,"error":"Internal Server Error","path":"/api/unhandled"}
```

A `@RestControllerAdvice` with one `@ExceptionHandler` turned a domain exception
into a 409 with a useful body, including the custom `sku` field. `ProblemDetail`
is the built in shape from RFC 7807, so there is no need to invent an error
format.

The unhandled one gets a generic 500 and Spring logs the whole stack trace. That
logging is switched off in `application.properties` for this demo, which is why
the output stays readable.

## 7. What @Cacheable is worth

```
first call       203 ms   went and did the work
second call        0 ms   came from the cache
third call         0 ms
real lookups behind three calls: 1

after evict      200 ms   the work happened again
real lookups now: 2
```

## 8. The lifecycle order

Captured from a bean that takes part in every hook Spring offers:

```
 1. constructor
 2. @Autowired setter, dependency injected
 3. BeanNameAware.setBeanName
 4. ApplicationContextAware.setApplicationContext
 5. BeanPostProcessor.postProcessBeforeInitialization
 6. @PostConstruct
 7. InitializingBean.afterPropertiesSet
 8. custom init method from @Bean(initMethod)
 9. BeanPostProcessor.postProcessAfterInitialization
    ... context closed ...
10. @PreDestroy
11. DisposableBean.destroy
12. custom destroy method from @Bean(destroyMethod)
```

Step 9 is where `@Transactional` and `@Cacheable` get their proxies in: the
"after initialization" step is where Spring can swap your object for a wrapper.
That is the same mechanism that makes demo 1 fail.

## What is not here: Redis

The caching demo uses an in memory `ConcurrentMapCacheManager`, because
`spring-boot-starter-data-redis` is not available on this machine and there is
no Redis server to talk to. Rather than pretend, here is exactly what changes:

- `@Cacheable`, `@CacheEvict` and the service code do not change at all. Only
  the `CacheManager` bean does.
- Values have to be serialized to go over a network, so cached types need to be
  serializable and you choose a serializer. This is where most real problems
  start.
- Every hit becomes a network call. A local map hit is measured above at 0 ms;
  a Redis hit is typically under a millisecond on the same network but it is no
  longer free, and it can fail.
- The cache becomes shared between instances, which is the actual reason to use
  it, and it survives a restart.
- You get a time to live, which the in memory manager above does not have.

## One thing that cost time

The first version put `@SpringBootApplication` in the default package. Component
scanning then has no base package, so it scans everything on the classpath, and
the application failed on startup trying to autoconfigure a database driver that
was never asked for. The main class belongs in a real package.
