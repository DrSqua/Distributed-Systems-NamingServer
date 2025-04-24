package schnitzel.distributedsystems.unittest.node;

import NodeClient.NodeClient;
import Utilities.NodeEntity.NodeEntityIn;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = NodeClient.class)
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class TestRingAPI {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @ParameterizedTest
    @ValueSource(strings = {"left", "right"})
    void getNotSet(String direction) throws Exception {
        this.mockMvc.perform(get("/ring/" + direction))  // Via nodeName
                .andExpect(status().isNotFound());
    }

    @ParameterizedTest
    @ValueSource(strings = {"left", "right"})
    void set(String direction) throws Exception {
        /*
         *  SETUP
         */
        String NODE_NAME = "set";
        NodeEntityIn newNode = new NodeEntityIn(
                NODE_NAME
        );
        String jsonRequest = objectMapper.writeValueAsString(newNode);

        /*
         * EXECUTE
         */
        this.mockMvc.perform(post("/ring/" + direction).contentType("application/json")
                        .content(jsonRequest))
                .andExpect(status().isOk());

        this.mockMvc.perform(get("/ring/" + direction))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nodeName").value(NODE_NAME));  // Check if the name matches
    }

    @ParameterizedTest
    @ValueSource(strings = {"left", "right"})
    void set_and_patch(String direction) throws Exception {
        /*
         *  SETUP
         */
        String NODE_NAME = "set_and_patch";
        String NODE_NAME_TWO = "set_and_patch_2";
        NodeEntityIn newNode = new NodeEntityIn(
                NODE_NAME
        );
        NodeEntityIn patchNode = new NodeEntityIn(
                NODE_NAME_TWO
        );
        String jsonRequest = objectMapper.writeValueAsString(newNode);
        String jsonRequestPatch = objectMapper.writeValueAsString(patchNode);

        this.mockMvc.perform(post("/ring/" + direction).contentType("application/json")
                        .content(jsonRequest))
                .andExpect(status().isOk());
        /*
         * EXECUTE
         */
        this.mockMvc.perform(post("/ring/" + direction).contentType("application/json")
                        .content(jsonRequestPatch))
                .andExpect(status().isOk());

        this.mockMvc.perform(get("/ring/" + direction))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nodeName").value(NODE_NAME_TWO));  // Check if the name matches
    }

    // TODO Make sure left and right can't be the same
}
