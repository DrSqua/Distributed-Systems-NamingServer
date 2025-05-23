package NodeClient.MulticastListening;

import NodeClient.RingAPI.RingStorage;
import Utilities.Multicast;
import Utilities.NodeEntity.NodeEntity;
import Utilities.RestMessagesRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import schnitzel.NamingServer.NamingServerHash;

import java.io.IOException;
import java.net.*;


@Component
public class NodeMulticastListener {
    private final RingStorage ringStorage;
    private final String IP;
    private Multicast multicast;
    @Value("${multicast.port}")
    private int multicastPort;

    @Value("${multicast.groupIP}")
    private String groupIP;

    public NodeMulticastListener(RingStorage ringStorage) throws IOException {
        this.ringStorage = ringStorage;
        this.IP = InetAddress.getLocalHost().getHostAddress();
    }


    /**
     * Starting a multicast listener on separate thread
     */
    @PostConstruct
    public void start() throws IOException {
        this.multicast = new Multicast(IP,groupIP, multicastPort);
        new Thread(() -> {
            try {
                listen();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }).start();
    }

    /**
     * Nodes listen to other nodes starting up on the network.
     * Steps:
     *  1) Join multicast group and listen
     *  2) When a different node performs startup code, multicast will be received and parsed
     *  3) Depending on current ring configuration (see further for options) perform config and REST messaging.
     */
    private void listen() throws InterruptedException {
        Thread.sleep(500);
        while (true){
            System.out.print("Node Multicast Listening\n");
            try (MulticastSocket socket = new MulticastSocket(multicastPort)) {
                InetAddress group = InetAddress.getByName(groupIP);

                // Enable loop back mode
                socket.setOption(StandardSocketOptions.IP_MULTICAST_LOOP, true);

                // Specify network interface (optional, set to null for default)
                // local
                // NetworkInterface networkInterface = NetworkInterface.getByName("NPF_Loopback"); // Replace or set to null
                NetworkInterface networkInterface = NetworkInterface.getByName("eth0"); // Replace or set to null

                // Join the multicast group
                socket.joinGroup(new InetSocketAddress(group, multicastPort), networkInterface);
                System.out.println("Joined group: " + group.getHostAddress() + ":"+ multicastPort);
                byte[] buffer = new byte[1024];
                while (true) {
                    DatagramPacket packet = new DatagramPacket(buffer,0, buffer.length, InetAddress.getByName(groupIP), multicastPort);
                    socket.receive(packet);
                    String message = new String(packet.getData(), 0, packet.getLength());
                    System.out.println("node received a multicast message of a new node (prev/next): "+message);
                    String nodeName = message.split(",")[0];
                    String nodeIP = message.split(",")[1];
                    Long hashedNodeName = NamingServerHash.hashNode(nodeName, nodeIP);

                    // Received the startup broadcast from new node
                    // ReceivedNode is new node
                    NodeEntity receivedNode = new NodeEntity(nodeIP, nodeName);
                    System.out.println("Node received " + receivedNode.getNodeName() + " on " + receivedNode.getIpAddress() + "and port: "+receivedNode);
                    this.ringStorage.setCurrentNodeCount(this.ringStorage.getCurrentNodeCount() + 1);

                    NodeEntity nextNode = this.ringStorage.getNode("NEXT").orElseThrow(() ->
                            new IllegalStateException("Existing Node does not have next set")
                    );
                    NodeEntity previousNode = this.ringStorage.getNode("PREVIOUS").orElseThrow(() ->
                            new IllegalStateException("Existing Node does not have previous set")
                    );
                    /*
                     * If currentID < hash < nextID, nexID=hash, current node updates its own parameter nextID,
                     * and sends response to node giving the information on currentID and nextID
                     *
                     * If previousID < hash < currentID, previousID=hash, current node updates its own parameter previousID,
                     * and sends response to node giving the information on currentID and previousID
                     */
                    if (this.ringStorage.currentHash() < hashedNodeName && hashedNodeName < nextNode.hashCode()) {
                        System.out.println("Received hash falls in [NEXT, PREVIOUS] region, updating!");
                        this.ringStorage.setNode("NEXT", receivedNode);
                        RestMessagesRepository.updateNeighbour(nextNode, "PREVIOUS", receivedNode.asEntityIn());
                    } else if (previousNode.hashCode() < hashedNodeName && hashedNodeName < this.ringStorage.currentHash()) {
                        System.out.println("Received hash falls in [NEXT, PREVIOUS] region, updating!");
                        this.ringStorage.setNode("PREVIOUS", receivedNode);
                        RestMessagesRepository.updateNeighbour(previousNode, "NEXT", receivedNode.asEntityIn());
                    } else if (this.ringStorage.getCurrentNodeCount() == 2) {
                        // 2 because the current has already been updated a couple lines earlier
                        System.out.println("Received hash does not fall in [NEXT, PREVIOUS] region, updating to self!");
                        // Setting on local
                        ringStorage.setNode("NEXT", receivedNode);
                        ringStorage.setNode("PREVIOUS", receivedNode);

                        // Setting on remote
                        NodeEntity currentNode = this.ringStorage.getSelf();
                        System.out.println("received node is " + receivedNode);
                        System.out.println("cuurentNode is " + currentNode.asEntityIn());
                        RestMessagesRepository.updateNeighbour(receivedNode, "NEXT", currentNode.asEntityIn());
                        RestMessagesRepository.updateNeighbour(receivedNode, "PREVIOUS", currentNode.asEntityIn());
                    }
                    else {
                        throw new IllegalStateException("Unexpected state");
                    }

                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }
}
