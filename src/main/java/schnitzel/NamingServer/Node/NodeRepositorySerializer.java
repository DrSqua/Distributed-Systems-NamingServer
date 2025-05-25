package schnitzel.NamingServer.Node;

import Utilities.NodeEntity.NodeEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.print.DocFlavor;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class NodeRepositorySerializer {

    private static final String BACKUP_FILE = "node_repository_backup.json";
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private NodeStorageService nodeRepository;

    // Save all entities to JSON file
    public void saveToJson() {
        Iterable<NodeEntity> nodes = nodeRepository.getAll();
        Map<Long, String> nodeMap = new HashMap<>();

        for (NodeEntity node : nodes) {
            nodeMap.put(node.getNodeHash(), node.getIpAddress());
        }

        try {
            objectMapper.writeValue(new File(BACKUP_FILE), nodeMap);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
