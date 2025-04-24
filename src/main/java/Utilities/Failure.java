package Utilities;

import schnitzel.NamingServer.NamingServerBootstrap;

import java.rmi.RemoteException;

public class Failure {
//    private final NamingServerBootstrap namingServer;
//    public Failure(NamingServerBootstrap bootstrap) {
//        this.namingServer = bootstrap;
//    }
//
//    public void handleFailure(String failedNodeId) {
//        try{
//            String previousNodeId = namingServer.getPreviousNode(failedNodeId);
//            String nextNodeId = namingServer.getNextNode(failedNodeId);
//
//            // Step 2: Notify previous node to update its 'next' pointer
//            NodeInterface previousNode = namingServer.getNodeStub(previousNodeId);
//            previousNode.setNextNode(nextNodeId);
//
//            // Step 3: Notify next node to update its 'previous' pointer
//            NodeInterface nextNode = namingServer.getNodeStub(nextNodeId);
//            nextNode.setPreviousNode(previousNodeId);
//
//            // Step 4: Remove the failed node from the naming server
//            namingServer.deregisterNode(failedNodeId);
//
//            System.out.println("Node " + failedNodeId + " removed and neighbors updated.");
//        } catch(RemoteException e){
//            System.err.println("Error handling failure of node: " + failedNodeId);
//            e.printStackTrace();
//        }
//    }
}
