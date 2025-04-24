package schnitzel.distributedsystems.unittest.namingserver;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import schnitzel.NamingServer.NamingServer;
import Utilities.NodeEntity.NodeEntityIn;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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
        String NODE_NAME = "postThenQuery";
        NodeEntityIn newNode = new NodeEntityIn(
                NODE_NAME
        );
        String jsonRequest = objectMapper.writeValueAsString(newNode);

        /*
         *  EXECUTE
         */
        this.mockMvc.perform(post("/node").contentType("application/json")
                .content(jsonRequest))
                .andExpect(status().isOk())
                .andExpect(content().string(String.valueOf(1)));

        /*
         * ASSERT
         */
        // First query Get
        this.mockMvc.perform(get("/node"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))  // Check that the list size is 1
                .andExpect(jsonPath("$[0].nodeName").value(NODE_NAME));  // Check if the name matches

        // Then resource Get
        this.mockMvc.perform(get("/node/" + NODE_NAME))  // Via nodeName
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nodeName").value(NODE_NAME));  // Check if the name matches
    }

    @Test
    void postThenDelete() throws Exception {
        /*
         *  SETUP
         */
        String NODE_NAME = "postThenDelete";
        NodeEntityIn newNode = new NodeEntityIn(
                NODE_NAME
        );
        String jsonRequest = objectMapper.writeValueAsString(newNode);

        this.mockMvc.perform(post("/node").contentType("application/json")
                        .content(jsonRequest))
                        .andExpect(status().isOk());
        /*
         *  EXECUTE
         */
        this.mockMvc.perform(delete("/node/" + NODE_NAME)).andExpect(status().isOk());

        /*
         * ASSERT
         */
        // This should fail
        this.mockMvc.perform(get("/node/" + NODE_NAME))  // Via nodeName
                .andExpect(status().isNotFound());
    }
}