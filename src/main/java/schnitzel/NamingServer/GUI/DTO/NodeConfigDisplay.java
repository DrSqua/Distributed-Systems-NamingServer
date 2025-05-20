package schnitzel.NamingServer.GUI.DTO;

public class NodeConfigDisplay {
    private NodeInfoDisplay currentNode;
    private NodeInfoDisplay previousNode;
    private NodeInfoDisplay nextNode;

    public NodeConfigDisplay() {}

    // Getters and Setters
    public NodeInfoDisplay getCurrentNode() { return currentNode; }
    public void setCurrentNode(NodeInfoDisplay currentNode) { this.currentNode = currentNode; }
    public NodeInfoDisplay getPreviousNode() { return previousNode; }
    public void setPreviousNode(NodeInfoDisplay previousNode) { this.previousNode = previousNode; }
    public NodeInfoDisplay getNextNode() { return nextNode; }
    public void setNextNode(NodeInfoDisplay nextNode) { this.nextNode = nextNode; }
}