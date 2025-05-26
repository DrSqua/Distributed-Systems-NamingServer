package schnitzel.NamingServer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;
import java.util.Map;

@SpringBootApplication(scanBasePackages = "schnitzel.NamingServer")
public class NamingServer {
    public static void main(String[] args){
        SpringApplication app = new SpringApplication(NamingServer.class);
        app.setDefaultProperties(Map.of(
                "spring.config.location", "src/main/java/schnitzel/NamingServer/resources/application.properties"
        ));
        app.run(args);
    }
}
