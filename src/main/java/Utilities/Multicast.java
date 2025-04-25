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
}
