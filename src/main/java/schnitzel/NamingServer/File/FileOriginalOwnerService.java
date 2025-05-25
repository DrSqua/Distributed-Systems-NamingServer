package schnitzel.NamingServer.File;

import Utilities.NodeEntity.NodeEntity;
import org.springframework.stereotype.Service;
import schnitzel.NamingServer.Node.NodeStorageService;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class FileOriginalOwnerService {
    private final ConcurrentHashMap<Long, NodeEntity> originalFileOwners = new ConcurrentHashMap<>();
    private final NodeStorageService nodeStorageService;

    public FileOriginalOwnerService(NodeStorageService nodeStorageService) {
        this.nodeStorageService = nodeStorageService;
    }

    public boolean registerOriginalFileOwner(long fileHash, long originalFileOwner) {
        Optional<NodeEntity> ownerNodeOpt = nodeStorageService.findById(originalFileOwner);
        if (ownerNodeOpt.isPresent()) {
            originalFileOwners.put(fileHash, ownerNodeOpt.get());
            return true;
        }
        return false;
    }

    public NodeEntity getOriginalFileOwner(long fileHash) {
        return originalFileOwners.get(fileHash);
    }
}
