package Utilities;

import java.io.IOException;
import java.net.*;

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
        this.socket.setOption(StandardSocketOptions.IP_MULTICAST_LOOP,true);
        this.socket.setTimeToLive(1);
    }

    public InetAddress getGroupIP() {
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
        InetAddress group = InetAddress.getByName(groupIP);
        String Name = System.getProperty("user.name");
        NetworkInterface networkInterface = NetworkInterface.getByInetAddress(InetAddress.getLocalHost());
        System.out.println(Name+" Joining Multicast: "+group+", port: "+port+", network interface: "+networkInterface);
        socket.joinGroup(new InetSocketAddress(group, port), networkInterface);
    }

    /// This send method is used to send a custom message
    public void SendMulticast(String message) throws IOException {
        DatagramPacket sendMessage = new DatagramPacket(
                message.getBytes(),
                message.getBytes().length,
                InetAddress.getByName(groupIP), port
        );
        socket.send(sendMessage);
        System.out.println("Multicast sent to " + groupIP + ":" + port);
    }

    /// This send method is used to send the information (nodeName, nodeIP) during Bootstrap.
    /// It sends the node name and IP seperated by IP.
    public void SendNodeInfo(String nodeName) throws IOException {
        String message = nodeName+","+IP;
        SendMulticast(message);
    }
}
