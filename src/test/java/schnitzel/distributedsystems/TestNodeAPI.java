package schnitzel.distributedsystems;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import schnitzel.NamingServer.NamingServer;
import schnitzel.NamingServer.Node.NodeEntityIn;
import com.fasterxml.jackson.databind.ObjectMapper;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.hamcrest.Matchers.hasSize;

@SpringBootTest(classes = NamingServer.class)
@AutoConfigureMockMvc
public class TestNodeAPI {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testEmptyList() throws Exception {
        this.mockMvc.perform(get("/node")).andExpect(status().isOk());
    }

    @Test
    void postThenQuery() throws Exception {
        /*
         *  SETUP
         */
        String NODE_NAME = "Boop";
        NodeEntityIn newNode = new NodeEntityIn(
                NODE_NAME
        );
        String jsonRequest = objectMapper.writeValueAsString(newNode);

        /*
         *  EXECUTE
         */
        this.mockMvc.perform(post("/node").contentType("application/json")
                .content(jsonRequest))
                // .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nodeNameLength").value(NODE_NAME.length())) // Validate the node name length
                .andExpect(jsonPath("$.ipAddress").exists()); // Check if IP address exists;

        this.mockMvc.perform(get("/node"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))  // Check that the list size is 1
                .andExpect(jsonPath("$[0].name").value(NODE_NAME))  // Check if the name matches
                // .andDo(print())
        ;
    }
}