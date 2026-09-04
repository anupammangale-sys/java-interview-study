package demo.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @RestController is exactly @Controller plus @ResponseBody on every method.
 * That one difference decides whether a returned String is a view name or the
 * response body.
 */
@RestController
public class RestApi {

    @GetMapping("/api/text")
    public String text() { return "hello"; }

    @GetMapping("/api/json")
    public Greeting json() { return new Greeting("hello", 3); }

    @GetMapping("/api/order/{id}")
    public Greeting byId(@PathVariable String id, @RequestParam(defaultValue = "1") int count) {
        return new Greeting("order " + id, count);
    }

    @GetMapping("/api/boom")
    public String boom() { throw new OutOfStockException("SKU-42"); }

    @GetMapping("/api/unhandled")
    public String unhandled() { throw new IllegalStateException("nothing handles this one"); }
}
