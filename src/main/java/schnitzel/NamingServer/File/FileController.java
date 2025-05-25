package schnitzel.NamingServer.File;

import Utilities.NodeEntity.NodeEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import schnitzel.NamingServer.NamingServerHash;
import schnitzel.NamingServer.Node.NodeStorageService;

import java.util.ArrayList;
import java.util.Collections;

import static java.lang.Math.abs;

@RestController
public class FileController {
    @ResponseStatus(value = HttpStatus.NOT_FOUND)
    public static class ResourceNotFoundException extends RuntimeException {
        ResourceNotFoundException(String message) {
            super(message);
        }
    }

    private final NodeStorageService nodeStorageService;
    private final FileOriginalOwnerService fileOriginalOwnerService;

    FileController(NodeStorageService nodeStorageService, FileOriginalOwnerService fileOriginalOwnerService) {
        this.nodeStorageService = nodeStorageService;
        this.fileOriginalOwnerService = fileOriginalOwnerService;
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
        long locatedNodeHash = this.nodeStorageService.keys().stream().
                max(Long::compare).
                orElseThrow(() -> new ResourceNotFoundException("No Nodes in system"));

        // Iterate all keys, calculate difference between node hash and filename hash
        for (Long key: this.nodeStorageService.keys()) {
            if ((abs(key - filenameHash)) < abs(locatedNodeHash - filenameHash)) {
                locatedNodeHash = key;
            }
        }
        NodeEntity locatedNode = this.nodeStorageService.findById(locatedNodeHash).orElseThrow();
        return locatedNode.getIpAddress();
    }

    /**
     *
     * @param fileHash: Hash value of the file
     * @return the owner of the given file
     */
    @GetMapping("/file/owner")
    public NodeEntity getFileOwner(@RequestParam long fileHash) {
        NodeEntity owner = findResponsibleNode(fileHash);
        if (owner == null) {
            throw new FileController.ResourceNotFoundException("Node with hash " + fileHash + " does not exist");
        }
        return owner;
    }
    /**
     * @param fileHash: Hash value of the file
     * @return the original (first) owner of the given file
     */
    @GetMapping("/file/original-owner")
    public NodeEntity getOriginalFileOwner(@RequestParam long fileHash) {
        NodeEntity originalOwner = fileOriginalOwnerService.getOriginalFileOwner(fileHash);
        if (originalOwner == null) {
            throw new FileController.ResourceNotFoundException("Node with hash " + fileHash + " does not exist");
        }
        return originalOwner;
    }

    @PostMapping("/file/register-original-owner")
    @ResponseStatus(value = HttpStatus.CREATED)
    public void registerOriginalOwner(@RequestParam long fileHash, @RequestParam long originalNodeHash) {
        boolean success = fileOriginalOwnerService.registerOriginalFileOwner(fileHash, originalNodeHash);
        if (!success) {
            throw new FileController.ResourceNotFoundException("Couldn't register original owner!");
        }
    }

    /**
     * @param fileHash: Hash value of the file
     * @param nodeHash: Hash value of the originating node
     * @return Informs originating node that he needs to replicate or ignore the file
     */
    @GetMapping("/file/replication")
    public String checkReplicationResponsibility(@RequestParam long fileHash, @RequestParam long nodeHash) {
        NodeEntity responsibleNode = findResponsibleNode(fileHash);

        assert responsibleNode != null;
        if (responsibleNode.getNodeHash().equals(nodeHash)) {
            return "REPLICATE";
        }
        return "IGNORE";
    }

    private NodeEntity findResponsibleNode(long fileHash) {
        ArrayList<Long> sortedHashes = nodeStorageService.keys();
        if (sortedHashes.isEmpty()) {
            return null;
        }
        // sort the hashes so the smallest node hash is first
        Collections.sort(sortedHashes);
        for (Long nodeHash : sortedHashes) {
            // if the fileHash is smaller, then the node has this node is the owner
            if (fileHash <= nodeHash) {
                return nodeStorageService.findById(nodeHash).orElse(null);
            }
        }
        // Wrap around, fileHash is less than all node hashes -> smallest nodeHash owns it
        return nodeStorageService.findById(sortedHashes.get(0)).orElse(null);
    }
}
