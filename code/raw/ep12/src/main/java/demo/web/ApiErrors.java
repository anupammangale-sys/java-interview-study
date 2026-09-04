package demo.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * One place that turns exceptions into responses, for every controller.
 * @RestControllerAdvice is @ControllerAdvice plus @ResponseBody.
 *
 * ProblemDetail is the built in shape from RFC 7807, so there is no need to
 * invent a custom error body.
 */
@RestControllerAdvice
public class ApiErrors {

    @ExceptionHandler(OutOfStockException.class)
    public ProblemDetail outOfStock(OutOfStockException e) {
        ProblemDetail p = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
        p.setTitle("Out of stock");
        p.setProperty("sku", e.getSku());
        return p;
    }
}
