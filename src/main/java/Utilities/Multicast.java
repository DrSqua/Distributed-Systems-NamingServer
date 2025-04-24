package Utilities;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

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
        System.out.println("Multicast sent to " + IP + ":" + PORT);

    }

    /// This send method is used to send the information (nodeName, nodeIP) during Bootstrap.
    /// It sends the node name and IP seperated by IP.
    public static void SendNodeInfo(String nodeName) throws IOException {
        String message = nodeName+","+IP;
        SendMulticast(message);
    }

    public static String ReceiveMulticast(int bufSize) throws IOException {
        byte[] buf = new byte[bufSize];
        DatagramPacket receiveMessage = new DatagramPacket(buf, buf.length);
        while (true) {
            // Receive the message
            socket.receive(receiveMessage);
            // Convert the byte array to a string
            String message = new String(receiveMessage.getData(), 0, receiveMessage.getLength());

            // Print the received message (node's name and IP)
            System.out.println("Receiver: Received message: " + message);
            if (receiveMessage.getLength() != 0) {
                return message;
            }
        }
    }
}
