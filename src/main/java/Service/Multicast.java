package Service;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;

public class Multicast {
    private static InetAddress groupIP;
    private static int PORT;
    private static MulticastSocket socket;
    private static String IP;

    public Multicast(String IP, String groupIP, int port) throws IOException {
        this.IP = IP;
        this.groupIP = InetAddress.getByName(groupIP);
        this.PORT = port;
        this.socket = new MulticastSocket(this.PORT);
    }

    public static InetAddress getGroupIP() {
        return groupIP;
    }

    public static void setGroupIP(InetAddress groupIP) {
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
        socket.joinGroup(groupIP);
    }

    /// This send method is used to send a custom message
    public static void SendMulticast(String message) throws IOException {
        DatagramPacket sendMessage = new DatagramPacket(message.getBytes(), message.getBytes().length, groupIP, PORT);
        socket.send(sendMessage);
    }

    /// This send method is used to send the information (nodeName, nodeIP) during Bootstrap.
    /// It sends the node name and IP seperated by IP.
    public static void SendNodeInfo(String nodeName) throws IOException {
        String message = nodeName+","+IP;
        SendMulticast(message);
    }

    public static void ReceiveMulticast() throws IOException {
        byte[] buf = new byte[1024];
        DatagramPacket receiveMessage = new DatagramPacket(buf, buf.length);
        socket.receive(receiveMessage);
    }
}
