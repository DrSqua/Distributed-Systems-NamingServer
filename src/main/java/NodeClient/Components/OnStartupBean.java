package NodeClient.Components;

import NodeClient.RingAPI.RingStorage;
import Utilities.Multicast;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.InetAddress;

@Component
public class OnStartupBean {

    private final RingStorage ringStorage;

    public OnStartupBean(RingStorage ringStorage) {
        this.ringStorage = ringStorage;
    }

    @PostConstruct
    public void notifyNetwork() {
        try {
            // Define the multicast group address and port (can be customized)
            String clientIP = InetAddress.getLocalHost().getHostAddress();
            String groupIP = "224.0.0.1";
            int port = 4446;

            // Initialize the Multicast object
            Multicast multicast = new Multicast(clientIP,groupIP, port);

            // Join the multicast group
            multicast.JoinMulticast();
            // Get the node's name from system property or environment (or hardcode for now)
            String nodeName = System.getProperty("user.name"); // Using the system's username as the node name
            // Alternatively, you can hardcode the name like:
            // String nodeName = "Node1";

            // Send the node information (name and IP) to the multicast group
            multicast.SendNodeInfo(nodeName+","+clientIP);

            // TODO Accept response from server and store the namingserver's ip address
            this.ringStorage.setNamingServerIP("TODO");

            // Arbitrary logging
            System.out.println("Node information sent: " + nodeName);

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Error during bootstrap: " + e.getMessage());
        }
    }
}
