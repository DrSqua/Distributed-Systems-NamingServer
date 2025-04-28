package schnitzel.NamingServer.File;

import Utilities.NodeEntity.NodeEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import schnitzel.NamingServer.NamingServerHash;
import schnitzel.NamingServer.Node.NodeStorageService;

import static java.lang.Math.abs;

@RestController
public class FileController {
    private final NodeStorageService nodeStorageService;
    FileController(NodeStorageService nodeStorageService) {
        this.nodeStorageService = nodeStorageService;
    }

    /**
     * @param fileName: Name of the file which the CLIENT is looking for
     * @return ip address of Node where the file is located
     */
    @GetMapping("/file/{fileName}")
    String getFilenameLocation(@PathVariable String fileName) {
        long filenameHash = NamingServerHash.hash(fileName);

        // We set max hash as default
        // As per project requirements
        long locatedNodeHash = this.nodeStorageService.keys().stream().max(Long::compare).orElseThrow();

        // Iterate all keys, calculate difference between node hash and filename hash
        for (Long key: this.nodeStorageService.keys()) {
            if ((abs(key - filenameHash)) < abs(locatedNodeHash - filenameHash)) {
                locatedNodeHash = key;
            }
        }
        NodeEntity locatedNode = this.nodeStorageService.findById(locatedNodeHash).orElseThrow();
        return locatedNode.getIpAddress();
    }
}
