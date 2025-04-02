package NodeClient;

import Utilities.Multicast;

import java.io.IOException;

public class Bootstrap {
    public static void main(String[] args) {
        try {
            // Define the multicast group address and port (can be customized)
            String clientIP = "192.168.43.11";
            String groupIP = "224.0.0.1"; // Example multicast address
            int port = 4446; // Example port number

            // Initialize the Multicast object
            Multicast multicast = new Multicast(clientIP,groupIP, port);

            // Join the multicast group
            multicast.JoinMulticast();

            // Get the node's name from system property or environment (or hardcode for now)
            String nodeName = System.getProperty("user.name"); // Using the system's username as the node name
            // Alternatively, you can hardcode the name like:
            // String nodeName = "Node1";

            // Send the node information (name and IP) to the multicast group
            multicast.SendNodeInfo(nodeName);

            System.out.println("Node information sent: " + nodeName);

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Error during bootstrap: " + e.getMessage());
        }
    }
}
