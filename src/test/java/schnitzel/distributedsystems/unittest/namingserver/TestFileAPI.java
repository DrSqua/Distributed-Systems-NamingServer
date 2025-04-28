package schnitzel.distributedsystems.unittest.namingserver;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import schnitzel.NamingServer.NamingServer;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = NamingServer.class)
@AutoConfigureMockMvc
public class TestFileAPI {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testEmptyList() throws Exception {
        String fileName = "Boop.txt";
        this.mockMvc.perform(get("/file/" + fileName)).andExpect(status().isNotFound());
    }

    @Test
    void noNodesInSystem() throws Exception {
        // TODO
    }

    @Test
    void withOneNode() throws Exception {
        // TODO
    }
}

