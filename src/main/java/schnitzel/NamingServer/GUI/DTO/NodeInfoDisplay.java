package schnitzel.NamingServer.GUI.DTO;

import Utilities.NodeEntity.NodeEntity;

public class NodeInfoDisplay {
    private String nodeName;
    private Long nodeHash; // Match type in NodeEntity
    private String ipAddress;
    private final int port = 8081; // Hardcoded known port

    public NodeInfoDisplay() {}

    public NodeInfoDisplay(NodeEntity entity) {
        if (entity != null) {
            this.nodeName = entity.getNodeName();
            this.nodeHash = entity.getNodeHash();
            this.ipAddress = entity.getIpAddress();
        }
    }

    // Getters
    public String getNodeName() { return nodeName; }
    public Long getNodeHash() { return nodeHash; }
    public String getIpAddress() { return ipAddress; }
    public int getPort() { return port; } // Returns 8081

    // Setters if needed by any framework, though primarily for construction from NodeEntity
    public void setNodeName(String nodeName) { this.nodeName = nodeName; }
    public void setNodeHash(Long nodeHash) { this.nodeHash = nodeHash; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    // No setPort needed as it's fixed
}