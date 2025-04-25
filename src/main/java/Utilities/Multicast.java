package Utilities;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

public class Multicast {
    private String groupIP;
    private int port;
    private MulticastSocket socket;
    private final String IP;

    public Multicast(String IP, String groupIP, int port) throws IOException {
        this.IP = IP;
        this.groupIP = groupIP;
        this.port = port;
        this.socket = new MulticastSocket(this.port);
    }

    public  InetAddress getGroupIP() {
        try {
            return InetAddress.getByName(groupIP);
        } catch (IOException e){
            e.printStackTrace();
            return InetAddress.getLoopbackAddress();
        }
    }

    public void setGroupIP(String groupIP) {
        this.groupIP = groupIP;
    }

    public  int getPORT() {
        return port;
    }

    public  void setPORT(int port) {
        this.port = port;
    }

    public  MulticastSocket getSocket() {
        return socket;
    }

    public  void setSocket(MulticastSocket socket) {
        this.socket = socket;
    }

    public void JoinMulticast() throws IOException {
        socket.joinGroup(InetAddress.getByName(groupIP));
    }

    /// This send method is used to send a custom message
    public void SendMulticast(String message) throws IOException {
        DatagramPacket sendMessage = new DatagramPacket(
                message.getBytes(),
                message.getBytes().length,
                InetAddress.getByAddress(groupIP.getBytes()), port
        );
        socket.send(sendMessage);
        System.out.println("Multicast sent to " + IP + ":" + port);

    }

    /// This send method is used to send the information (nodeName, nodeIP) during Bootstrap.
    /// It sends the node name and IP seperated by IP.
    public void SendNodeInfo(String nodeName) throws IOException {
        String message = nodeName+","+IP;
        SendMulticast(message);
    }
}
