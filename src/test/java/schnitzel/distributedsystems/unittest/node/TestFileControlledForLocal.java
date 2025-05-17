package schnitzel.distributedsystems.unittest.node;

import NodeClient.NodeClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = NodeClient.class)
@TestPropertySource(properties = {
        "server.port=8081",
        "multicast.port=4040",
        "multicast.groupIP=230.0.0.1"
})
@AutoConfigureMockMvc
public class TestFileControlledForLocal {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void uploadNewFile() throws Exception {
        /*
         * SETUP
         */
        // Create a mock multipart file
        String fileContent = "test content";
        MockMultipartFile file = new MockMultipartFile(
                "file", // parameter name
                "test.txt", // filename
                MediaType.TEXT_PLAIN_VALUE,
                fileContent.getBytes()
        );

        /*
         * EXECUTE
         */
        MvcResult result = this.mockMvc.
                perform(multipart("/node/file/upload").
                file(file).
                contentType(MediaType.MULTIPART_FORM_DATA)).
                andExpect(status().isCreated()).
                andReturn();

        /*
         * ASSERT
         */
        assertEquals(HttpStatus.CREATED.value(), result.getResponse().getStatus());
    }
}
