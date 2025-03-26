package schnitzel.distributedsystems;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import schnitzel.NamingServer.NamingServer;

@SpringBootTest(classes = NamingServer.class)
@AutoConfigureMockMvc
public class TestNamingServerFileAPI {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testEmptyList() throws Exception {
        this.mockMvc.perform(get("/file")).andDo(print()).andExpect(status().isOk());
    }
}