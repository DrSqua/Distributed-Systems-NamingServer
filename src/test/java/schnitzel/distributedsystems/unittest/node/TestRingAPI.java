package schnitzel.distributedsystems.unittest.node;

import NodeClient.NodeClient;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = NodeClient.class)
@AutoConfigureMockMvc
public class TestRingAPI {
    @Autowired
    private MockMvc mockMvc;


    @ParameterizedTest
    @ValueSource(strings = {"left", "right"})
    void getNotSet(String direction) throws Exception {
        this.mockMvc.perform(get("/ring/" + direction))  // Via nodeName
                .andExpect(status().isNotFound());
    }

//    @ParameterizedTest
//    @ValueSource(strings = {"left", "right"})
//    void set(
//
//    )
}
