package schnitzel.NamingServer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "schnitzel.NamingServer")
public class NamingServer {
    public static void main(String[] args) {
        SpringApplication.run(NamingServer.class, args);
    }
}
