package schnitzel.NamingServer;
import Utilities.Multicast;
import Utilities.NodeEntity.NodeEntity;
import schnitzel.NamingServer.Node.NodeStorageService;

import java.io.IOException;
import java.net.DatagramPacket;

public class NamingServerBootstrap {
    private String namingServerIP;
    private int port;
    private String groupIP;
    private Multicast multicast;

    private NodeStorageService nodeStorageService;

    public NamingServerBootstrap(String NamingServerIP, int port, String groupIP, NodeStorageService nodeStorageService) throws IOException {
        this.namingServerIP = NamingServerIP;
        this.port = port;
        this.groupIP = groupIP;
        this.multicast = new Multicast(namingServerIP,groupIP, port);
        this.nodeStorageService = nodeStorageService;
    }

    public void joinGroup() throws IOException {
        multicast.JoinMulticast();
    }

    public String[] receiveMessage(int bufSize) throws IOException {
        byte buf[] = new byte[bufSize];
        DatagramPacket receiveMessage = new DatagramPacket(buf, buf.length);
        String message[] = multicast.ReceiveMulticast(bufSize).split(",");
        storeNode(message);
        return message;
    }

    public void sendMessage(int numberOfNodes) throws IOException {
        String strNodes = numberOfNodes + "";
        multicast.SendMulticast(strNodes);
    }

    public void storeNode(String[] newNode) throws IOException {
        String name = newNode[0];
        String ip = newNode[1];
        NamingServerHash hash = new NamingServerHash();
        Long hashCode = hash.hash(name);
        NodeEntity newNodeEntity = new NodeEntity(
                ip,
                hashCode,
                name
        );
        nodeStorageService.put(hashCode, newNodeEntity);
    }

    public static void main(String[] args) throws IOException {
        String NamingServerIP = "192.168.43.100";
        String groupIP = "224.0.0.1";
        int port = 4446;

        // Initialize the Multicast object
        Multicast multicast = new Multicast(NamingServerIP,groupIP, port);

        // Join the multicast group
        multicast.JoinMulticast();
        // Get the node's name from system property or environment (or hardcode for now)
        String nodeName = System.getProperty("user.name"); // Using the system's username as the node name
        // Alternatively, you can hardcode the name like:
        // String nodeName = "Node1";

        // Send the node information (name and IP) to the multicast group
        multicast.SendNodeInfo(nodeName);

        System.out.println("Node information sent: " + nodeName);
    }
}