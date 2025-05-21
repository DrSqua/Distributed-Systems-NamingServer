package NodeClient.Status;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StatusController {

    @PostMapping("/fail")
    void manual_fail() {
        throw new IllegalStateException("Throwing an error ourselves");
    }

    @GetMapping("/health")
    String health_check() {
        System.out.println("NodeClient: Received GET /health request. Responding OK.");
        return "OK";
    }
}
