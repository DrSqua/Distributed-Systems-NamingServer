package NodeClient.Status;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StatusController {

    private final ApplicationContext appContext;

    @Autowired
    public StatusController(ApplicationContext appContext) {
        this.appContext = appContext;
    }

    @PostMapping("/fail")
    void manual_fail() {
        throw new IllegalStateException("Throwing an error ourselves");
    }

    @GetMapping("/health")
    String health_check() {
        System.out.println("NodeClient: Received GET /health request. Responding OK.");
        return "OK";
    }
    @PostMapping("/shutdown-trigger")
    public ResponseEntity<String> triggerShutdown() {
        System.out.println("NodeClient: Received /shutdown-trigger request. Initiating SpringApplication.exit()...");

        // Start shutdown in a new thread so the HTTP response can be sent
        new Thread(() -> {
            try {
                // Optional small delay
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("Shutdown thread interrupted before exiting Spring context.");
            }
            // This triggers Spring Boot's graceful shutdown, including @PreDestroy methods
            int exitCode = SpringApplication.exit(appContext, () -> 0);
            System.out.println("NodeClient: SpringApplication.exit() completed with code " + exitCode + ". Exiting JVM.");
            System.exit(exitCode); // Ensure the JVM process actually exits
        }).start();

        return ResponseEntity.ok("NodeClient: Shutdown signal received. Application will terminate shortly.");
    }
}
