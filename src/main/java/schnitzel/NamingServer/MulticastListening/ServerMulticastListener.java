package schnitzel.NamingServer.MulticastListening;

import NodeClient.RingAPI.RingStorage;
import Utilities.Multicast;
import Utilities.NodeEntity.NodeEntity;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import schnitzel.NamingServer.NamingServer;
import schnitzel.NamingServer.NamingServerHash;
import schnitzel.NamingServer.Node.NodeStorageService;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.nio.charset.StandardCharsets;

@Component
public class ServerMulticastListener {
    private final NodeStorageService storage;
    private final String groupIP;
    private final int PORT;
    private final String IP;
    private final Multicast multicast;

    public ServerMulticastListener(NodeStorageService storage) throws IOException {
        this.storage = storage;
        this.groupIP = "224.0.0.1";
        this.IP = "192.168.43.100";
        this.PORT = 4446;
        this.multicast = new Multicast(IP,groupIP,PORT);
    }

    @PostConstruct
    public void start() {
        new Thread(this::listen).start();
    }

    private void listen() {
        try(MulticastSocket socket = new MulticastSocket(PORT)){
            InetAddress groupIP = InetAddress.getByName(this.groupIP);
            this.multicast.JoinMulticast();
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
