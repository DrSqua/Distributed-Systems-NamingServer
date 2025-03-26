package schnitzel.distributedsystems;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import schnitzel.NamingServer.File.FileController;
import schnitzel.NamingServer.NamingServer;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = NamingServer.class)
class NamingServerTests {

    @Autowired
    private FileController fileController;

    @Test
    void contextLoads() {
        assertThat(fileController).isNotNull();
    }
}
