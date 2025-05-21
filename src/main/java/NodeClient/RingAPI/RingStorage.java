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

    private String namingServerIP;
    public NodeEntity setNode(String direction, NodeEntity node) {
        return dataMap.put(direction, node);
    }

    public Optional<NodeEntity> getNode(String direction) {
        return Optional.ofNullable(dataMap.getOrDefault(direction, null));
    }

    public String currentName() {
        return System.getProperty("user.name");
    }

    public Long currentHash() throws UnknownHostException {
        return NamingServerHash.hashNode(this.currentName(),
                InetAddress.getByName(String.valueOf(InetAddress.getLocalHost())).toString()); // :)
    }

    public String getNamingServerIP() {
        return namingServerIP;
    }

    public void setNamingServerIP(String namingServerIP) {
        this.namingServerIP = namingServerIP;
    }
}