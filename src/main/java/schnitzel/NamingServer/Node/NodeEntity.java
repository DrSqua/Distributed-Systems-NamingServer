package schnitzel.NamingServer.Node;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class NodeEntity {
    /*
     * Node Entity is the value in a Key-value pair of Hash and Ip Address
     */
    @Id
    private Long nodeHash;  // Hash of this name is used as key

    private String ipAddress;

    public NodeEntity(String ipAddress, Long nodeHash) {
        this.ipAddress = ipAddress;
        this.nodeHash = nodeHash;
    }

    public NodeEntity() {}
}
