package schnitzel.NamingServer.MulticastListening;

import Utilities.Multicast;
import Utilities.NodeEntity.NodeEntity;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import schnitzel.NamingServer.NamingServerHash;
import schnitzel.NamingServer.Node.NodeStorageService;
import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;

@Component
public class ServerMulticastListener {
    private final NodeStorageService storage;
    private final String IP;
    public Multicast multicast;

    @Value("${multicast.port}")
    private int multicastPort;

    @Value("${unicast.port}")
    private int unicastPort;

    @Value("${server.port}")
    private int unicast_PORT;

    @Value("${multicast.groupIP}")
    private String groupIP;


    public ServerMulticastListener(NodeStorageService storage){
        this.storage = storage;
        this.IP = "192.168.43.100";
    }

    @PostConstruct
    public void start() {
        try {
            this.multicast = new Multicast(IP, groupIP, multicastPort);
            new Thread(this::listen).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void listen() {
        try(MulticastSocket socket = new MulticastSocket(multicastPort)) {
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
            socket.joinGroup(new InetSocketAddress(group, multicastPort), networkInterface);
            System.out.println("Joined group: " + group.getHostAddress() + ":"+multicastPort);

            byte[] buffer = new byte[1024];
            while(true){
                DatagramPacket packet = new DatagramPacket(buffer,0, buffer.length, InetAddress.getByName(groupIP), multicastPort);
                socket.receive(packet);
                String message = new String(packet.getData(), 0, packet.getLength());
                System.out.println(message);
                String nodeName = message.split(",")[0];
                String nodeIP = message.split(",")[1];
                System.out.println("Received a multicast message from a new node with ip: " + nodeIP);
                Long hash = NamingServerHash.hashNode(nodeName, nodeIP);
                NodeEntity node = new NodeEntity(nodeIP, nodeName);
                storage.put(hash,node);

                /*
                 *  Now the namingServer will respond. By sending the number of existing nodes to the new node.
                 *  We made this a multicast, because this number needs to be updated by all nodes (otherwise
                 *  they will have an incorrect number).
                 */
                String response = String.valueOf(storage.count());
                System.out.println("number of nodes: " + response);
                byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);

                // Send a direct (unicast) reply back to the sender
                try (DatagramSocket respSocket = new DatagramSocket()) {
                    DatagramPacket respPacket = new DatagramPacket(
                            responseBytes,
                            responseBytes.length,
                            packet.getAddress(),   // the node’s IP
                            unicastPort       // the node’s source port
                    );
                    respSocket.send(respPacket);
                    System.out.println("packet send to node");
                    System.out.printf("Unicast reply (%s) sent to %s:%d%n",
                            response, packet.getAddress().getHostAddress(), unicastPort);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                System.out.println("Multicast Listener Stopped");
            }
        } catch (IOException e){
            e.printStackTrace();
        }
    }
}
