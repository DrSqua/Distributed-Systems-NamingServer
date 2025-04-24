package NodeClient;

public class Node {
    private volatile String nextNodeIp;
    public Node(){

    }

    private synchronized void updateNextNodeIP(){
        if(nextNodeIp == null || nextNodeIp.equals("") || nextNodeIp.isEmpty()){
            getNextIp();
        }
    }

    private void getNextIp() {
        try {
            helpMethods.sendUnicast("Requesting IP from ID", serverIP, "GET_IP_FROM_ID" + ":" + IP , Ports.unicastPort);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Unable to get IP from ID", e);
        }
    }
}
