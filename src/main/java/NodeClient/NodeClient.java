package NodeClient;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Map;

@SpringBootApplication(scanBasePackages = "NodeClient")
public class NodeClient {
    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(NodeClient.class);
        app.setDefaultProperties(Map.of(
                "spring.config.location", "src/main/java/NodeClient/resources/application.properties"
        ));
        app.run(args);
    }
}
