package demo;

import demo.cache.PriceService;
import demo.lifecycle.Steps;
import demo.scope.Prototype;
import demo.scope.SingletonAskingEachTime;
import demo.scope.SingletonBean;
import demo.scope.SingletonHoldingPrototype;
import demo.tx.OrderRepo;
import demo.tx.PropagationDemo;
import demo.tx.SelfCallDemo;

import jakarta.servlet.Filter;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.stereotype.Component;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.web.servlet.HandlerAdapter;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

@Component
public class Runner implements ApplicationRunner {

    private static final String BASE = "http://localhost:8099";

    private final OrderRepo repo;
    private final SelfCallDemo selfCall;
    private final PropagationDemo propagation;
    private final ConfigurableApplicationContext ctx;
    private final SingletonHoldingPrototype holding;
    private final SingletonAskingEachTime asking;
    private final PriceService prices;
    private final HttpClient http = HttpClient.newHttpClient();

    private int countUp;

    public Runner(OrderRepo repo, SelfCallDemo selfCall, PropagationDemo propagation,
                  ConfigurableApplicationContext ctx, SingletonHoldingPrototype holding,
                  SingletonAskingEachTime asking, PriceService prices) {
        this.repo = repo;
        this.selfCall = selfCall;
        this.propagation = propagation;
        this.ctx = ctx;
        this.holding = holding;
        this.asking = asking;
        this.prices = prices;
    }

    static void head(String s) {
        System.out.println();
        System.out.println("=== " + s + " ===");
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        selfCallTrap();
        propagationTrap();
        scopes();
        requestPath();
        controllerVersusRestController();
        exceptionHandling();
        caching();
        lifecycleOnTheWayUp();
        ctx.close();
        lifecycleOnTheWayDown();
        System.out.println();
        // this is a web application, so the embedded server would otherwise keep
        // the JVM alive after the demos have finished
        System.exit(0);
    }

    // ------------------------------------------------------------- 1 and 2

    private void selfCallTrap() {
        head("1. the self call trap");

        repo.deleteAll();
        System.out.println("A. calling the transactional method from OUTSIDE the bean");
        try {
            selfCall.saveThenFail("outside-1");
        } catch (IllegalStateException e) {
            System.out.println("  threw: " + e.getMessage());
        }
        long afterOutside = repo.count();
        System.out.println("  rows left in the database: " + afterOutside
                + (afterOutside == 0 ? "   rolled back, as expected" : "   NOT rolled back"));

        repo.deleteAll();
        System.out.println();
        System.out.println("B. the same method, reached by a call from INSIDE the same bean");
        try {
            selfCall.viaSelfCall("inside-1");
        } catch (IllegalStateException e) {
            System.out.println("  threw: " + e.getMessage());
        }
        long afterInside = repo.count();
        System.out.println("  rows left in the database: " + afterInside
                + (afterInside == 0 ? "   rolled back" : "   NOT rolled back, the data survived"));

        System.out.println();
        System.out.println("  Same method. Same exception. Outside: " + afterOutside
                + " row. Inside: " + afterInside + " row.");
    }

    private void propagationTrap() {
        head("2. I caught the exception, so why did it roll back");

        repo.deleteAll();
        System.out.println("A. inner method uses REQUIRED, the default");
        try {
            propagation.outerCatchesRequired("outer-a", "inner-a");
            System.out.println("  outer finished normally");
        } catch (UnexpectedRollbackException e) {
            System.out.println("  " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
        System.out.println("  rows left: " + repo.count() + "   the outer save is gone too");

        repo.deleteAll();
        System.out.println();
        System.out.println("B. inner method uses REQUIRES_NEW");
        try {
            propagation.outerCatchesRequiresNew("outer-b", "inner-b");
            System.out.println("  outer finished normally");
        } catch (UnexpectedRollbackException e) {
            System.out.println("  " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
        System.out.println("  rows left: " + repo.count() + "   only the inner one rolled back");
        repo.findAll().forEach(r -> System.out.println("    kept: " + r.getId() + ", " + r.getNote()));
    }

    // ------------------------------------------------------------------- 3

    private void scopes() {
        head("3. singleton, prototype, and the trap between them");

        System.out.println("asking the context for the same singleton three times:");
        for (int i = 0; i < 3; i++) {
            System.out.println("  serial " + ctx.getBean(SingletonBean.class).serial());
        }
        System.out.println("asking the context for a prototype three times:");
        for (int i = 0; i < 3; i++) {
            System.out.println("  serial " + ctx.getBean(Prototype.class).serial());
        }

        System.out.println();
        System.out.println("a prototype INJECTED into a singleton, called three times:");
        for (int i = 0; i < 3; i++) {
            System.out.println("  serial " + holding.serialOfHeldPrototype()
                    + "   the same one every time, so the scope did nothing");
        }
        System.out.println("the same singleton using ObjectProvider instead:");
        for (int i = 0; i < 3; i++) {
            System.out.println("  serial " + asking.serialOfFreshPrototype() + "   a new one");
        }
        System.out.println();
        System.out.println("  Prototype instances created in total: " + Prototype.totalCreated());
    }

    // ------------------------------------------------------------------- 4

    private void requestPath() {
        head("4. what a request passes through, read from the running context");

        List<Filter> filters = new ArrayList<>(ctx.getBeansOfType(Filter.class).values());
        filters.sort(AnnotationAwareOrderComparator.INSTANCE);
        System.out.println("servlet filters registered, in order:");
        filters.forEach(f -> System.out.println("  " + f.getClass().getSimpleName()));

        System.out.println();
        System.out.println("HandlerMappings, in the order DispatcherServlet asks them:");
        ctx.getBeansOfType(HandlerMapping.class).values().stream()
           .sorted(AnnotationAwareOrderComparator.INSTANCE)
           .forEach(h -> System.out.println("  " + h.getClass().getSimpleName()));

        System.out.println();
        System.out.println("HandlerAdapters:");
        ctx.getBeansOfType(HandlerAdapter.class).values()
           .forEach(h -> System.out.println("  " + h.getClass().getSimpleName()));

        System.out.println();
        var converters = ctx.getBean(RequestMappingHandlerAdapter.class).getMessageConverters();
        System.out.println("message converters, " + converters.size() + " of them:");
        converters.forEach(c -> System.out.printf("  %-42s %s%n",
                c.getClass().getSimpleName(), c.getSupportedMediaTypes()));
        long distinct = converters.stream().map(System::identityHashCode).distinct().count();
        System.out.println("  distinct objects among those " + converters.size()
                + ": " + distinct + ". The first one that can handle the type wins,");
        System.out.println("  so a repeat further down the list is never reached.");
    }

    // ------------------------------------------------------------- 5 and 6

    private void show(String path) throws Exception {
        HttpResponse<String> r = http.send(
                HttpRequest.newBuilder(URI.create(BASE + path)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        String body = r.body().replace(System.lineSeparator(), " ").replace("\n", " ");
        if (body.length() > 140) {
            body = body.substring(0, 140) + " ...";
        }
        System.out.printf("  %-34s %d  %s%n", path, r.statusCode(),
                r.headers().firstValue("content-type").orElse("no content type"));
        System.out.println("      " + (body.isBlank() ? "(empty body)" : body));
    }

    private void controllerVersusRestController() throws Exception {
        head("5. @Controller and @RestController, the same method twice");
        System.out.println("  every line below is a real HTTP call to this application");
        System.out.println();
        show("/api/text");
        show("/api/json");
        show("/api/order/A-77?count=4");
        show("/mvc/text");
        show("/mvc/text-with-responsebody");
    }

    private void exceptionHandling() throws Exception {
        head("6. one place that turns exceptions into responses");
        show("/api/boom");
        show("/api/unhandled");
    }

    // ------------------------------------------------------------------- 7

    private void caching() {
        head("7. what @Cacheable is worth, timed");

        long first = timed(() -> prices.priceOf("SKU-ABCDE"));
        long second = timed(() -> prices.priceOf("SKU-ABCDE"));
        long third = timed(() -> prices.priceOf("SKU-ABCDE"));

        System.out.printf("  first call    %6d ms   went and did the work%n", first);
        System.out.printf("  second call   %6d ms   came from the cache%n", second);
        System.out.printf("  third call    %6d ms%n", third);
        System.out.println("  real lookups behind three calls: " + prices.realLookups());

        prices.forget("SKU-ABCDE");
        long afterEvict = timed(() -> prices.priceOf("SKU-ABCDE"));
        System.out.printf("  after evict   %6d ms   the work happened again%n", afterEvict);
        System.out.println("  real lookups now: " + prices.realLookups());
    }

    private long timed(Runnable r) {
        long start = System.nanoTime();
        r.run();
        return (System.nanoTime() - start) / 1_000_000;
    }

    // ------------------------------------------------------------------- 8

    private void lifecycleOnTheWayUp() {
        head("8. the order the lifecycle hooks actually run in");
        Steps.print();
        countUp = Steps.all().size();
    }

    private void lifecycleOnTheWayDown() {
        System.out.println("  ... context closed ...");
        Steps.all().stream().skip(countUp).forEach(s -> System.out.println("  " + s));
    }
}
