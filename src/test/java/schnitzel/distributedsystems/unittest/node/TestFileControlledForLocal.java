package schnitzel.distributedsystems.unittest.node;

import NodeClient.Components.FileCheckerBean;
import NodeClient.NodeClient;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// With this test you need to run the Naming Server ourselves
@SpringBootTest(classes = NodeClient.class)
@TestPropertySource(properties = {
        "server.port=8081",
        "multicast.port=4040",
        "multicast.groupIP=230.0.0.1"
})
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TestFileControlledForLocal {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void uploadNewFile() throws Exception {

        // Create a mock multipart file
        String parameterName = "file";
        String fileName = "test.txt";
        String fileContent = "test content";
        MockMultipartFile file = createMockFile(parameterName, fileName, fileContent);

        MvcResult result = this.mockMvc.perform(
                multipart("/node/file/upload").
                file(file).
                contentType(MediaType.MULTIPART_FORM_DATA)).
                andExpect(status().isCreated()).
                andReturn();


        assertEquals(HttpStatus.CREATED.value(), result.getResponse().getStatus());
    }

    @Test
    void uploadThenDownloadFile() throws Exception {
        // create a mock multipart file
        String parameterName = "file";
        String fileName = "downloadFile.txt";
        String fileContent = "Test Download";
        MockMultipartFile file = createMockFile(parameterName, fileName, fileContent);
        /*
         * EXECUTE Upload
         */
        MvcResult uploadResult = this.mockMvc.perform(
                        multipart("/node/file/upload").
                        file(file).
                        contentType(MediaType.MULTIPART_FORM_DATA)).
                        andExpect(status().isCreated()).
                        andReturn();
        assertEquals(HttpStatus.CREATED.value(), uploadResult.getResponse().getStatus());
        /*
         * EXECUTE Download
         */
        MvcResult downloadResult = this.mockMvc.perform(
                        get("/node/file/" + fileName)).
                        andExpect(status().isOk()).
                        andReturn();

        String downloadContent = downloadResult.getResponse().getContentAsString();
        System.out.println(downloadContent);
        /*
         * ASSERT
         */
        assertEquals(fileContent, downloadContent);
    }

    private MockMultipartFile createMockFile(String parameterName, String fileName, String fileContent) {
        MockMultipartFile file = new MockMultipartFile(
                parameterName,
                fileName,
                MediaType.TEXT_PLAIN_VALUE,
                fileContent.getBytes()
        );
        return file;
    }
}
