package Utilities.NodeEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;
import schnitzel.NamingServer.NamingServerHash;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.io.File;
import java.util.Objects;


public class NodeEntity {
    /*
     * Node Entity is the value in a Key-value pair of Hash and NodeEntity
     */
    private Long nodeHash;  // Hash of this name is used as key

    private String nodeName;
    private String ipAddress;
    Path path = Paths.get("").toAbsolutePath().normalize();
    private final Path filePathLocal =path.resolve("local_files");
    private final Path filePathReplica = path.resolve("replicated_files");

    public NodeEntity(String ipAddress, String nodeName) {
        this.ipAddress = ipAddress;
        this.nodeName = nodeName;
        this.nodeHash = NamingServerHash.hashNode(nodeName, ipAddress);
    }

    public NodeEntity() {}

    public Long getNodeHash() {  // Ensure getter exists
        return nodeHash;
    }

    public String getIpAddress() {  // Ensure getter exists
        return ipAddress;
    }

    @Override
    public String toString() {
        return "NodeEntity{" +
                "nodeHash=" + nodeHash +
                ", ipAddress='" + ipAddress + '\'' +
                '}';
    }

    public NodeEntityIn asEntityIn() {
        return new NodeEntityIn(ipAddress);
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;  // If function pointers are equal
        if (!(o instanceof NodeEntity that)) return false;  // If other is not NodeEntity
        return Objects.equals(nodeHash, that.nodeHash);  // If both hashes are equal
    }

    @Override
    public int hashCode() {
        try {
            throw new Exception();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
