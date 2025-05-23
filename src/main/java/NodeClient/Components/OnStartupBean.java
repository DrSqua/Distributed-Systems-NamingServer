package NodeClient.Components;

import NodeClient.RingAPI.RingStorage;
import Utilities.Multicast;
import Utilities.NodeEntity.NodeEntity;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.*;
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
    public void notifyNetwork() throws IOException {
        try (MulticastSocket socket = new MulticastSocket(PORT)) {
            // Define the multicast group address and port (can be customized)
            String clientIP = this.ringStorage.getOwnIp();
            Multicast multicast = new Multicast(clientIP,groupIP, port);

            // Join multicast and send own username
            multicast.JoinMulticast();
            String nodeName = this.ringStorage.currentName();
            multicast.SendNodeInfo(nodeName+","+clientIP+","+responsePORT);

            byte[] buffer = new byte[1024];
            System.out.println("waiting for the response of the namingServer");
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

            System.out.println("Waiting for naming-server’s unicast reply… Listening on ip " + clientIP);
            DatagramSocket socket2 = new DatagramSocket(responsePORT);
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
        }
    }
}
