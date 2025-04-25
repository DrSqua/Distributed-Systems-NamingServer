package NodeClient.MulticastListening;

import NodeClient.RingAPI.RingStorage;
import Utilities.Multicast;
import Utilities.NodeEntity.NodeEntity;
import Utilities.RestMessagesRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import schnitzel.NamingServer.NamingServerHash;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;


@Component
public class NodeMulticastListener {
    private final RingStorage ringStorage;
    private final String groupIP;
    private final int PORT;
    private final String IP;
    private final Multicast multicast;

    public NodeMulticastListener(RingStorage ringStorage) throws IOException {
        this.ringStorage = ringStorage;
        this.groupIP = "224.0.0.1";
        this.IP = InetAddress.getLocalHost().getHostAddress();
        this.PORT = 4446;
        this.multicast = new Multicast(IP,groupIP,PORT);
    }

    @PostConstruct
    public void start() {
        new Thread(this::listen).start();
    }

    private void listen() {
        try (MulticastSocket socket = new MulticastSocket(PORT)) {
            InetAddress groupIP = InetAddress.getByName(this.groupIP);
            this.multicast.JoinMulticast();
            byte[] buffer = new byte[1024];
            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                String message = new String(packet.getData(), 0, packet.getLength());
                String nodeName = message.split(",")[0];
                String nodeIP = message.split(",")[1];
                Long hashedNodeName = NamingServerHash.hash(nodeName);
                NodeEntity receivedNode = new NodeEntity(nodeIP, hashedNodeName, nodeName);

                NodeEntity nextNode = this.ringStorage.getNode("NEXT").orElseThrow(() ->
                        new IllegalStateException("Existing Node does not have next set")
                );
                NodeEntity previousNode = this.ringStorage.getNode("PREVIOUS").orElseThrow(() ->
                        new IllegalStateException("Existing Node does not have previous set")
                );
                /*
                 * If currentID< hash < nextID, nexID= hash, current node updates its own parameter nextID,
                 * and sends response to node giving the information on currentID and nextID
                 *
                 * If previousID< hash < currentID, previousID= hash, current node updates its own parameter previousID,
                 * and sends response to node giving the information on currentIDand previousID
                 */
                if (this.ringStorage.currentHash() < hashedNodeName && hashedNodeName < nextNode.hashCode()) {
                    this.ringStorage.setNode("NEXT", receivedNode);
                    RestMessagesRepository.updateNeighbour(nextNode, "PREVIOUS", receivedNode.asEntityIn());
                } else if (previousNode.hashCode() < hashedNodeName && hashedNodeName < this.ringStorage.currentHash()) {
                    this.ringStorage.setNode("PREVIOUS", receivedNode);
                    RestMessagesRepository.updateNeighbour(previousNode, "NEXT", receivedNode.asEntityIn());
                } else {
                    throw new IllegalStateException("Unexpected state");
                }

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
