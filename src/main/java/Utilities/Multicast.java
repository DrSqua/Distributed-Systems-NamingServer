package Utilities;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import schnitzel.NamingServer.NamingServerHash;
import Utilities.NodeEntity.NodeEntity;
import schnitzel.NamingServer.Node.NodeStorageService;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.nio.charset.StandardCharsets;

@Component
public class Multicast {
    private static String groupIP;
    private static int PORT;
    private static MulticastSocket socket;
    private static String IP;
    private final NodeStorageService storage;

    public Multicast(String IP, String groupIP, int port) throws IOException {
        this.IP = IP;
        this.groupIP = groupIP;
        this.PORT = port;
        this.socket = new MulticastSocket(this.PORT);
        this.storage = new NodeStorageService();
    }

    @PostConstruct
    public void start(){
        new Thread(this::ReceiveMulticast).start();
    }

    public static InetAddress getGroupIP() {
        try {
            return InetAddress.getByName(groupIP);
        } catch (IOException e){
            e.printStackTrace();
            return InetAddress.getLoopbackAddress();
        }
    }

    public static void setGroupIP(String groupIP) {
            Multicast.groupIP = groupIP;
    }

    public static int getPORT() {
        return PORT;
    }

    public static void setPORT(int PORT) {
        Multicast.PORT = PORT;
    }

    public static MulticastSocket getSocket() {
        return socket;
    }

    public static void setSocket(MulticastSocket socket) {
        Multicast.socket = socket;
    }

    public static void JoinMulticast() throws IOException {
        socket.joinGroup(InetAddress.getByName(groupIP));
    }

    /// This send method is used to send a custom message
    public static void SendMulticast(String message) throws IOException {
        DatagramPacket sendMessage = new DatagramPacket(message.getBytes(), message.getBytes().length, InetAddress.getByAddress(groupIP.getBytes()), PORT);
        socket.send(sendMessage);
        System.out.println("Multicast sent to " + IP + ":" + PORT);

    }

    /// This send method is used to send the information (nodeName, nodeIP) during Bootstrap.
    /// It sends the node name and IP seperated by IP.
    public static void SendNodeInfo(String nodeName) throws IOException {
        String message = nodeName+","+IP;
        SendMulticast(message);
    }

    public void ReceiveMulticast() {
        try(MulticastSocket socket = new MulticastSocket(PORT)){
            InetAddress groupIP = InetAddress.getByName(this.groupIP);
            JoinMulticast();
            byte[] buffer = new byte[1024];
            while(true){
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                String message = new String(packet.getData(), 0, packet.getLength());
                String nodeName = message.split(",")[0];
                String nodeIP = message.split(",")[1];
                Long hash = NamingServerHash.hash(nodeName);
                NodeEntity node = new NodeEntity(nodeIP,hash, nodeName);
                storage.put(hash,node);
                /*
                 *  Now the namingServer will respond. By sending the number of existing nodes to the new node.
                 *  We made this a multicast, because this number needs to be updated by all nodes (otherwise
                 *  they will have an incorrect number).
                 */
                String response = String.valueOf(storage.count());
                byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
                DatagramSocket responseSocket = new DatagramSocket();
                DatagramPacket responsePacket = new DatagramPacket(responseBytes, responseBytes.length, packet.getAddress(), packet.getPort());
                responseSocket.send(responsePacket);
                responseSocket.close();
            }
        } catch (IOException e){
            e.printStackTrace();
        }
    }
}
