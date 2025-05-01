package Utilities.NodeEntity;
import org.springframework.stereotype.Component;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.io.File;


public class NodeEntity {
    /*
     * Node Entity is the value in a Key-value pair of Hash and NodeEntity
     */
    private Long nodeHash;  // Hash of this name is used as key

    private String nodeName;
    private String ipAddress;
    Path path = Paths.get("").toAbsolutePath().normalize();
    private Path filePathLocal =path.resolve("local_files");
    private Path filePathReplica = path.resolve("replica_files");

    public NodeEntity(String ipAddress, Long nodeHash, String nodeName) {
        this.ipAddress = ipAddress;
        this.nodeName = nodeName;
        this.nodeHash = nodeHash;
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
}
