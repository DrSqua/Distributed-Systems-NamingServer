package NodeClient.Components;

import NodeClient.RingAPI.RingStorage;
import Utilities.Multicast;
import Utilities.NodeEntity.NodeEntity;
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

    public OnStartupBean(RingStorage ringStorage) {
        this.ringStorage = ringStorage;
    }

    @Value("${multicast.port}")
    private int multicastPort;

    @Value("${unicast.port}")
    private int unicastPort;

    @Value("${multicast.groupIP}")
    private String groupIP;

    /**
     * On startup, node send out a multicast message with own name and IP Address
     * IMPORTANT: StartupBean needs to wait for the Tomcat REST Controller to start up.
     * Otherwise, other Node might get a connection refused as this node won't be listening on the correct port.
     * Steps:
     *  1) Sleep
     *  2) Join multicast group and send "startup" multicast message
     *  3) Receives back nodesInNetwork as integer
     *  4) Depending on nodesInNetwork, performs branching neighbour logic
     */
    @PostConstruct
    public void notifyNetwork() throws InterruptedException {
        // Tomcat is often still not initialised when asking to set the neighbours so give it some extra time
        // this will make sure we can send the message when everything is set
        Thread.sleep(500);

        try(MulticastSocket socket = new MulticastSocket(multicastPort)) {
            // Define the multicast group address and port (can be customized)
            String clientIP = this.ringStorage.getOwnIp();
            Multicast multicast = new Multicast(clientIP,groupIP, multicastPort);

            multicast.JoinMulticast();
            String nodeName = System.getProperty("user.name"); // Using the system's username as the node name
            multicast.SendNodeInfo(nodeName+","+clientIP);

            byte[] buffer = new byte[1024];
            System.out.println("waiting for the response of the namingServer");
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

            System.out.println("Waiting for naming-server’s unicast reply… Listening on ip " + clientIP);
            DatagramSocket socket2 = new DatagramSocket(unicastPort);
            socket2.receive(packet);

            System.out.println("we received a response: "+packet.getLength());
            String message = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
            int numberOfNodes = Integer.parseInt(message);
            String namingServerIP = packet.getAddress().getHostAddress();

            // Storing node count
            this.ringStorage.setCurrentNodeCount(numberOfNodes);

            // Neighbour information will be sent by other nodes in the network if there are any
            if (numberOfNodes == 1) {
                System.out.println("Setting self as neighbour");
                // Become own neighbour
                NodeEntity ownNode = new NodeEntity(
                        clientIP,
                        nodeName
                );
                this.ringStorage.setNode("NEXT", ownNode);
                this.ringStorage.setNode("PREVIOUS", ownNode);
            } else {
                System.out.println("Waiting for other nodes to give");
            }
            this.ringStorage.setNamingServerIP(namingServerIP);

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Error during bootstrap: " + e.getMessage());
        }
    }
}
