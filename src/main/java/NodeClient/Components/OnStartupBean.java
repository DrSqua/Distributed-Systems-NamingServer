package NodeClient.Components;

import NodeClient.RingAPI.RingStorage;
import Utilities.Multicast;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.swing.plaf.basic.BasicInternalFrameTitlePane;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.nio.charset.StandardCharsets;

@Component
public class OnStartupBean {

    private final RingStorage ringStorage;
    @Value("${multicast.port}")
    private int PORT;
    private int responsePORT = 50000;


    public OnStartupBean(RingStorage ringStorage) {
        this.ringStorage = ringStorage;
    }

    @Value("${multicast.port}")
    private int port;

    @Value("${multicast.groupIP}")
    private String groupIP;

    @PostConstruct
    public void notifyNetwork() {
        try(MulticastSocket socket = new MulticastSocket(PORT)) {
            // Define the multicast group address and port (can be customized)
            String clientIP = InetAddress.getLocalHost().getHostAddress();

            // Initialize the Multicast object
            Multicast multicast = new Multicast(clientIP,groupIP, port);

            // Join the multicast group
            multicast.JoinMulticast();
            // Get the node's name from system property or environment (or hardcode for now)
            String nodeName = System.getProperty("user.name"); // Using the system's username as the node name
            // Alternatively, you can hardcode the name like:
            // String nodeName = "Node1";

            // Send the node information (name and IP) to the multicast group
            multicast.SendNodeInfo(nodeName+","+clientIP+","+responsePORT);

            byte[] buffer = new byte[1024];
            System.out.println("waiting for the response of the namingServer");
            //DatagramPacket packet = new DatagramPacket(buffer,0, buffer.length, InetAddress.getByName(groupIP), PORT);
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            System.out.println("Waiting for naming-server’s unicast reply…");
            DatagramSocket socket2 = new DatagramSocket(responsePORT);
            socket2.receive(packet);
            //socket.receive(packet);
            System.out.println("we received a response: "+packet.getLength());
            String message = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
            int numberOfNodes = Integer.parseInt(message);
            String namingServerIP = packet.getAddress().getHostAddress();
            // TODO @Robbe store "numberOfNodes" in the storage
            System.out.println("namingServerIP stored in nodeStorage: "+namingServerIP);
            this.ringStorage.setNamingServerIP(namingServerIP);

            // Arbitrary logging
            System.out.println("Node information sent: " + nodeName);


        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Error during bootstrap: " + e.getMessage());
        }
    }
}
