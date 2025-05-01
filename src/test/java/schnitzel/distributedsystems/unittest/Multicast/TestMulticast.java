package schnitzel.distributedsystems.unittest.Multicast;

import NodeClient.MulticastListening.NodeMulticastListener;
import NodeClient.NodeClient;
import Utilities.NodeEntity.NodeEntityIn;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.stereotype.Component;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import Utilities.Multicast;
import schnitzel.NamingServer.MulticastListening.ServerMulticastListener;
import schnitzel.NamingServer.Node.NodeStorageService;
import NodeClient.RingAPI.RingStorage;
import Utilities.NodeEntity.NodeEntity;
import schnitzel.NamingServer.NamingServer;

import NodeClient.NodeClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.io.IOException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = {schnitzel.NamingServer.NamingServer.class, NodeClient.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class TestMulticast {
    @Autowired
    private NodeStorageService nodeStorage;
    @Autowired
    private RingStorage ringStorage;
    @Autowired
    private ServerMulticastListener serverListener;
    @Autowired
    private NodeMulticastListener nodeListener;

    @Value("${multicast.port}")
    private int port;

    @Value("${multicast.groupIP}")
    private String groupIP;

    @Test
    public void testMulticastMessageReception() throws Exception {
        // Arrange
        String nodeName = "TestNode";
        String ip = "127.0.0.1"; // Loopback for testing

        Multicast multicast = new Multicast(ip, groupIP, port);

        // Wait for listeners to be ready (basic synchronization, adjust if needed)
        Thread.sleep(1000);

        // Act
        multicast.SendNodeInfo(nodeName);

        // Wait for messages to propagate
        Thread.sleep(2000);

        // Assert (server side)
        Long expectedHash = schnitzel.NamingServer.NamingServerHash.hash(nodeName);
        Optional<NodeEntity> storedNode = nodeStorage.findById(expectedHash);
        System.out.println("Here is what we get out of the storage: "+storedNode);
        assertTrue(storedNode.isPresent(), "Server should have received the node info");

        // Assert (client side)
        // We assume the ring has been initialized correctly in app context before the test
        assertDoesNotThrow(() -> {
            NodeEntity left = ringStorage.getNode("PREVIOUS").orElseThrow();
            NodeEntity right = ringStorage.getNode("NEXT").orElseThrow();
            assertNotNull(left);
            assertNotNull(right);
        });
    }
}
