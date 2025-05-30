package NodeClient.RingAPI;

import Utilities.NodeEntity.NodeEntity;
import org.springframework.stereotype.Service;
import schnitzel.NamingServer.NamingServerHash;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RingStorage {
    private final ConcurrentHashMap<String, NodeEntity> dataMap = new ConcurrentHashMap<>();
    int nodeCount = 0;

    private String namingServerIP;
    public NodeEntity setNode(String direction, NodeEntity node) {
        return dataMap.put(direction, node);
    }

    public Optional<NodeEntity> getNode(String direction) {
        return Optional.ofNullable(dataMap.getOrDefault(direction, null));
    }

    public NodeEntity getSelf() throws UnknownHostException {
        return new NodeEntity(this.getOwnIp(), this.currentName());
    }

    public void setCurrentNodeCount(int nodeCount) {
        this.nodeCount = nodeCount;
    }

    public int getCurrentNodeCount() {
        return this.nodeCount;
    }

    public String currentName() {
        return System.getProperty("user.name");
    }

    public boolean currentIsLargest() throws UnknownHostException {
        NodeEntity nextNode = this.getNode("NEXT").orElseThrow(() ->
                new IllegalStateException("Existing Node does not have next set")
        );
        NodeEntity previousNode = this.getNode("PREVIOUS").orElseThrow(() ->
                new IllegalStateException("Existing Node does not have previous set")
        );
        return nextNode.getNodeHash() < currentHash() && previousNode.getNodeHash() < currentHash();
    }

    public String getOwnIp() throws UnknownHostException {
        String ownIp = InetAddress.getLocalHost().getHostAddress();
        System.out.println("Own IP: " + ownIp);
        return ownIp;
    }

    public Long currentHash() throws UnknownHostException {
        return NamingServerHash.hashNode(this.currentName(),
                this.getOwnIp()); // :)
    }

    public String getNamingServerIP() {
        return namingServerIP;
    }

    public void setNamingServerIP(String namingServerIP) {
        this.namingServerIP = namingServerIP;
    }
}