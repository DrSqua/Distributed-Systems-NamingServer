package NodeClient;

import NodeClient.Agents.FailureAgent;
import NodeClient.Agents.SyncAgent;
import NodeClient.File.FileService;
import NodeClient.RingAPI.RingStorage;

import jade.core.Agent;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Map;

@SpringBootApplication(scanBasePackages = {"NodeClient"})
// uncomment when using Agents
//public class NodeClient implements ApplicationRunner {
public class NodeClient {

    @Autowired
    private AgentContainer agentContainer;
    @Autowired
    private RingStorage ringStorage;
    @Autowired
    private FileService fileService;

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(NodeClient.class);
        app.setDefaultProperties(Map.of(
                "spring.config.location", "src/main/java/NodeClient/resources/application.properties"
        ));
        app.run(args);
    }
    // we don't use SyncAgent as it isn't tested yet
/*
    @Override
    public void run(ApplicationArguments args) throws Exception {
        AgentController syncAgent = agentContainer.createNewAgent(
                "SyncAgent",
                SyncAgent.class.getName(),
                new Object[]{fileService, ringStorage}
        );
        syncAgent.start();
    }
 */

    public void startFailureAgent(String failingNodeName, String startedNodeName) throws Exception {
        AgentController failureAgent = agentContainer.createNewAgent(
                // name has to be unique
                "FailureAgent_" + failingNodeName + System.currentTimeMillis(),
                FailureAgent.class.getName(),
                new Object[]{fileService, ringStorage, failingNodeName, startedNodeName}
        );
        failureAgent.start();
    }

    public void startReceivedAgent(Agent receivedAgent, Object[] arguments) throws Exception {
        AgentController failureAgent = agentContainer.createNewAgent(
                receivedAgent.getLocalName(),
                receivedAgent.getClass().getName(),
                arguments
        );
        failureAgent.start();
    }
}
