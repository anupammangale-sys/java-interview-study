package demo.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * A plain @Controller. The returned String is treated as the name of a view to
 * render, not as the response body. With no view resolver that can find a view
 * called "hello", the request fails.
 */
@Controller
public class PlainController {

    @GetMapping("/mvc/text")
    public String text() { return "hello"; }

    /** The same method with @ResponseBody behaves like @RestController. */
    @GetMapping("/mvc/text-with-responsebody")
    @ResponseBody
    public String textWithBody() { return "hello"; }
}
