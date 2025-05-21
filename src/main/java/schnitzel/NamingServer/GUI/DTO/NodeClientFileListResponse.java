package schnitzel.NamingServer.GUI.DTO;

import java.util.List;

public class NodeClientFileListResponse {
    private List<String> localFiles;
    private List<String> replicatedFiles;

    // Default constructor for Jackson deserialization
    public NodeClientFileListResponse() {}

    // Getters - names must match the field names in the JSON response
    // which will be derived from your FileListResponse record's component names
    public List<String> getLocalFiles() {
        return localFiles;
    }

    public void setLocalFiles(List<String> localFiles) {
        this.localFiles = localFiles;
    }

    public List<String> getReplicatedFiles() {
        return replicatedFiles;
    }

    public void setReplicatedFiles(List<String> replicatedFiles) {
        this.replicatedFiles = replicatedFiles;
    }
}