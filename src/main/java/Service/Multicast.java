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

    public Multicast(String groupIP, int port) throws IOException {
        this.groupIP = InetAddress.getByName(groupIP);
        this.PORT = port;
        this.socket = new MulticastSocket(this.PORT);
    }

    public static void JoinMulticast() throws IOException {
        socket.joinGroup(groupIP);
    }

    public static void SendMulticast(String message) throws IOException {
        DatagramPacket sendMessage = new DatagramPacket(message.getBytes(), message.getBytes().length, groupIP, PORT);
        socket.send(sendMessage);
    }

    public static void ReceiveMulticast() throws IOException {
        byte[] buf = new byte[1024];
        DatagramPacket receiveMessage = new DatagramPacket(buf, buf.length);
        socket.receive(receiveMessage);
    }
}
