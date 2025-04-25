package NodeClient.RingAPI;

import Utilities.NodeEntity.NodeEntity;
import org.springframework.stereotype.Service;
import schnitzel.NamingServer.NamingServerHash;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RingStorage {
    private final ConcurrentHashMap<String, NodeEntity> dataMap = new ConcurrentHashMap<>();

    public NodeEntity setNode(String direction, NodeEntity node) {
        return dataMap.put(direction, node);
    }

    public Optional<NodeEntity> getNode(String direction) {
        return Optional.ofNullable(dataMap.getOrDefault(direction, null));
    }

    public Long currentHash() {
        return NamingServerHash.hash(System.getProperty("user.name"));
    }
}