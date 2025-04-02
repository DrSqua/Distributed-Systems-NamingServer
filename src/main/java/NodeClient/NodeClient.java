package NodeClient;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "NodeClient")
public class NodeClient {
    public static void main(String[] args) {
        SpringApplication.run(NodeClient.class, args);
    }
}
