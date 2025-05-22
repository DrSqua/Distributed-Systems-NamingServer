package schnitzel.NamingServer.Node;


import Utilities.NodeEntity.NodeEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class NodeStorageService {
    private final ConcurrentHashMap<Long, NodeEntity> dataMap = new ConcurrentHashMap<>();

    /**
     * @param key Hash of the node's name
     * @param value NodeEntity object holding ipaddress, name, ..
     */
    public void put(Long key, NodeEntity value) {
        System.out.println("key: " + key + ", value: " + value);
        dataMap.put(key, value);
    }

    public void deleteById(Long key) {
        dataMap.remove(key);
    }

    public boolean existsById(Long key) {
        return dataMap.containsKey(key);
    }

    public Optional<NodeEntity> findById(Long key) {
        return Optional.ofNullable(dataMap.getOrDefault(key, null));
    }

    public Iterable<NodeEntity> getAll() {
        return dataMap.values();
    }

    public ArrayList<Long> keys() {
        // Enumeration is a legacy implementation
        // We cast to iterator
        return Collections.list(dataMap.keys());
    }

    public int count() {
        return dataMap.size();
    }
}