package NodeClient.RingAPI;

import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RingStorage {
    private final ConcurrentHashMap<String, String> dataMap = new ConcurrentHashMap<>();

    public String setNodeIp(String direction, String ipAddress) {
        return dataMap.put(direction, ipAddress);
    }

    public Optional<String> getNodeIP(String direction) {
        return Optional.ofNullable(dataMap.getOrDefault(direction, null));
    }
}