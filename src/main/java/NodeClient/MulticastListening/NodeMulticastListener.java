package NodeClient.MulticastListening;

import NodeClient.RingAPI.RingStorage;
import Utilities.Multicast;
import Utilities.NodeEntity.NodeEntity;
import Utilities.NodeEntity.NodeEntityIn;
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
    private int PORT;

    @Value("${multicast.groupIP}")
    private String groupIP;

    public NodeMulticastListener(RingStorage ringStorage) throws IOException {
        this.ringStorage = ringStorage;
        this.IP = InetAddress.getLocalHost().getHostAddress();
    }

    @PostConstruct
    public void start(){
        try{
            this.multicast = new Multicast(IP,groupIP,PORT);
            new Thread(() -> {
                try {
                    listen();
                }catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void listen() throws IOException {
        System.out.print("Node Multicast Listening\n");
        try (MulticastSocket socket = new MulticastSocket(PORT)) {
            InetAddress group = InetAddress.getByName(groupIP);

            // Enable loopback mode
            socket.setOption(StandardSocketOptions.IP_MULTICAST_LOOP, true);

            // Specify network interface (optional, set to null for default)
            // local
            // NetworkInterface networkInterface = NetworkInterface.getByName("NPF_Loopback"); // Replace or set to null

            //remote
            NetworkInterface networkInterface = NetworkInterface.getByName("eth0"); // Replace or set to null
            if (networkInterface == null) {
                System.out.println("Using default network interface");
            } else {
                System.out.println("Using interface: " + networkInterface.getName());
            }

            // Join the multicast group
            socket.joinGroup(new InetSocketAddress(group, PORT), networkInterface);
            System.out.println("Joined group: " + group.getHostAddress() + ":" + PORT);

            byte[] buffer = new byte[1024];
            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, 0, buffer.length, InetAddress.getByName(groupIP), PORT);
                socket.receive(packet);

                String message = new String(packet.getData(), 0, packet.getLength());
                System.out.println("node received a multicast message of a new node (prev/next): " + message);

                // Parsing
                String nodeName = message.split(",")[0];
                String nodeIP = message.split(",")[1];
                Long hashedOtherNode = NamingServerHash.hashNode(nodeName, nodeIP);

                if (hashedOtherNode.equals(ringStorage.currentHash())) {
                    System.out.println("Node " + nodeName + " is the same as the current node");
                    continue;
                }

                // Received the startup broadcast from new node
                // ReceivedNode is new node
                NodeEntity receivedNode = new NodeEntity(nodeIP, nodeName);
                System.out.println("Node received " + receivedNode.getNodeName() + " on " + receivedNode.getIpAddress() + "and port: " + receivedNode);
                this.ringStorage.setCurrentNodeCount(this.ringStorage.getCurrentNodeCount() + 1);

                NodeEntity nextNode = this.ringStorage.getNode("NEXT").orElseThrow(() ->
                        new IllegalStateException("Existing Node does not have next set")
                );
                NodeEntity previousNode = this.ringStorage.getNode("PREVIOUS").orElseThrow(() ->
                        new IllegalStateException("Existing Node does not have previous set")
                );
                /*
                 * If only one node in network, neighbours are self
                 *
                 * If currentID < hash < nextID, nexID=hash, current node updates its own parameter nextID,
                 * and sends response to node giving the information on currentID and nextID
                 *
                 * If previousID < hash < currentID, previousID=hash, current node updates its own parameter previousID,
                 * and sends response to node giving the information on currentID and previousID
                 *
                 * If hash > currentID AND currentID is highest (both neighbours are smaller)
                 * Give new node "next=currentNext & previous=self"
                 * Give next as "previous=hash"
                 * Set own next=hash
                 */
                NodeEntity currentNode = this.ringStorage.getSelf();
                if (this.ringStorage.getCurrentNodeCount() == 2) {
                    // 2 because the current has already been updated a couple lines earlier
                    System.out.println("Received neighbour, only 2 total in network so setting both neighbours for received node");
                    // Setting on local
                    ringStorage.setNode("NEXT", receivedNode);
                    ringStorage.setNode("PREVIOUS", receivedNode);

                    // Setting on remote
                    System.out.println("received node is " + receivedNode);
                    System.out.println("currentNode is " + currentNode.asEntityIn());
                    RestMessagesRepository.updateNeighbour(receivedNode, "NEXT", currentNode.asEntityIn());
                    RestMessagesRepository.updateNeighbour(receivedNode, "PREVIOUS", currentNode.asEntityIn());
                } else if (this.ringStorage.currentHash() < hashedOtherNode && hashedOtherNode < nextNode.getNodeHash()) {
                    System.out.println("Received hash" + hashedOtherNode + "falls in [SELF, PREVIOUS]" + this.ringStorage.currentHash() + ", " + nextNode.getNodeHash() + "region, updating!");
                    this.ringStorage.setNode("NEXT", receivedNode);
                    RestMessagesRepository.updateNeighbour(nextNode, "PREVIOUS", receivedNode.asEntityIn());
                } else if (previousNode.getNodeHash() < hashedOtherNode && hashedOtherNode < this.ringStorage.currentHash()) {
                    System.out.println("Received hash" + hashedOtherNode + "falls in [PREVIOUS, SELF]" + this.ringStorage.currentHash() + ", " + nextNode.getNodeHash() + "region, updating!");
                    this.ringStorage.setNode("PREVIOUS", receivedNode);
                    RestMessagesRepository.updateNeighbour(previousNode, "NEXT", receivedNode.asEntityIn());
                } else if (this.ringStorage.currentHash() > hashedOtherNode && this.ringStorage.currentIsLargest()) {
                    // Adjust PREVIOUS's NEXT to received
                    RestMessagesRepository.updateNeighbour(previousNode, "NEXT", receivedNode.asEntityIn());

                    // Give the new node correct neighbours
                    RestMessagesRepository.updateNeighbour(receivedNode, "PREVIOUS", currentNode.asEntityIn());
                    RestMessagesRepository.updateNeighbour(receivedNode, "NEXT", nextNode.asEntityIn());

                    // Adjust own next
                    this.ringStorage.setNode("NEXT", receivedNode);
                } else {
                    throw new IllegalStateException("Unexpected state");
                }

            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
