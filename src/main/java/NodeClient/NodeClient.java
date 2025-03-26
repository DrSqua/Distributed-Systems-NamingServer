package NodeClient;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class NodeClient {
    private String name;
    public static void main(String[] args) {
        SpringApplication.run(NodeClient.class, args);

    }
}
