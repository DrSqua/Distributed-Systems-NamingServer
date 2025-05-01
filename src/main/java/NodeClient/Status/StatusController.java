package NodeClient.Status;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StatusController {

    @PostMapping("/fail")
    void manual_fail() {
        throw new IllegalStateException("Throwing an error ourselves");
    }

    @PostMapping("/health")
    String health_check() {
        return "OK";
    }
}
