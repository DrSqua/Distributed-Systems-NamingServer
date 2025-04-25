package Utilities.NodeEntity;

public class NodeEntity {
    /*
     * Node Entity is the value in a Key-value pair of Hash and NodeEntity
     */
    private Long nodeHash;  // Hash of this name is used as key

    private String nodeName;
    private String ipAddress;

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
