package schnitzel.NamingServer.GUI; // Or schnitzel.NamingServer.Gui.Controller

import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import schnitzel.NamingServer.GUI.DTO.NodeClientFileListResponse;
import schnitzel.NamingServer.GUI.DTO.NodeConfigDisplay;
import schnitzel.NamingServer.GUI.DTO.NodeInfoDisplay;
import schnitzel.NamingServer.Node.NodeStorageService;
import Utilities.NodeEntity.NodeEntity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Controller
public class DashboardGuiController {

    private static final Logger log = LoggerFactory.getLogger(DashboardGuiController.class);

    private final NodeStorageService nodeStorageService;
    private final WebClient.Builder webClientBuilder;
    private final int NODE_CLIENT_PORT = 8081;
    private final ApplicationContext appContext;

    @Autowired
    public DashboardGuiController(NodeStorageService nodeStorageService,
                                  WebClient.Builder webClientBuilder,
                                  ApplicationContext appContext) { // Add ApplicationContext
        this.nodeStorageService = nodeStorageService;
        this.webClientBuilder = webClientBuilder;
        this.appContext = appContext; // Store it
    }

    private NodeInfoDisplay convertToDisplay(NodeEntity entity) {
        if (entity == null) return null;
        return new NodeInfoDisplay(entity);
    }

    @GetMapping("/gui/dashboard")
    public String dashboard(Model model,
                            @RequestParam(name = "node_ip", required = false) String selectedNodeIp,
                            @RequestParam(name = "node_hash", required = false) Long selectedNodeHashParam,
                            @RequestParam(name = "statusMessage", required = false) String statusMessage,
                            @RequestParam(name = "errorMessage", required = false) String errorMessage) {

        log.debug("Dashboard Request: node_ip=[{}], node_hash=[{}]", selectedNodeIp, selectedNodeHashParam);

        if (statusMessage != null) model.addAttribute("statusMessage", statusMessage);
        if (errorMessage != null) model.addAttribute("errorMessage", errorMessage);

        List<NodeInfoDisplay> allNodesForGui = Collections.emptyList();
        String systemErrorMessage = null;
        try {
            Iterable<NodeEntity> allEntities = nodeStorageService.getAll();
            if (allEntities != null) {
                allNodesForGui = StreamSupport.stream(allEntities.spliterator(), false)
                        .map(this::convertToDisplay)
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            log.error("Error fetching all nodes from NodeStorageService: {}", e.getMessage(), e);
            systemErrorMessage = "Error loading node list from Naming Server.";
        }
        model.addAttribute("allNodes", allNodesForGui);
        if (systemErrorMessage != null) model.addAttribute("systemError", systemErrorMessage);

        model.addAttribute("selectedNodeIp", selectedNodeIp);
        model.addAttribute("selectedNodeHashParam", selectedNodeHashParam);
        model.addAttribute("selectedNodeIdentifier", null);
        model.addAttribute("selectedNodeConfig", null);
        model.addAttribute("selectedNodeLocalFiles", Collections.emptyList());
        model.addAttribute("selectedNodeReplicatedFiles", Collections.emptyList());
        model.addAttribute("selectedNodeConfigError", null);
        model.addAttribute("selectedNodeFilesError", null);
        String healthStatusKeyBase = (selectedNodeIp != null) ? "healthStatus_" + selectedNodeIp.replace(".", "_") : null;
        if (healthStatusKeyBase != null && !model.containsAttribute(healthStatusKeyBase)) {
            model.addAttribute(healthStatusKeyBase, null);
        }

        if (selectedNodeIp != null) {
            model.addAttribute("selectedNodeIdentifier", selectedNodeIp + ":" + NODE_CLIENT_PORT);
            String nodeBaseUrl = "http://" + selectedNodeIp + ":" + NODE_CLIENT_PORT;
            WebClient targetNodeClient = webClientBuilder.baseUrl(nodeBaseUrl).build();
            NodeConfigDisplay nodeConfigDisplay = new NodeConfigDisplay();
            StringBuilder currentConfigErrorAccumulator = new StringBuilder();

            NodeEntity currentSelectedEntity = null;
            if (selectedNodeHashParam != null) {
                currentSelectedEntity = nodeStorageService.findById(selectedNodeHashParam).orElse(null);
                if (currentSelectedEntity == null) {
                    log.warn("Node with hash {} NOT FOUND in NodeStorageService via HASH.", selectedNodeHashParam);
                    currentConfigErrorAccumulator.append("Selected node (hash: ").append(selectedNodeHashParam).append(") details not found in Naming Server. ");
                }
            } else {
                log.warn("Selected node_hash parameter was null for IP {}. Cannot reliably find node.", selectedNodeIp);
                currentConfigErrorAccumulator.append("Node hash not provided for selection. ");
            }
            if (currentSelectedEntity != null) nodeConfigDisplay.setCurrentNode(convertToDisplay(currentSelectedEntity));

            try {
                NodeEntity prevEntity = targetNodeClient.get().uri("/ring/PREVIOUS").retrieve().bodyToMono(NodeEntity.class)
                        .onErrorResume(e -> {log.debug("Error/Empty PREVIOUS for {}: {}",nodeBaseUrl,e.getMessage());return Mono.empty();}).block();
                nodeConfigDisplay.setPreviousNode(convertToDisplay(prevEntity));
            } catch (Exception e) { log.warn("Sync exc PREVIOUS {}: {}", nodeBaseUrl, e.getMessage()); currentConfigErrorAccumulator.append("Error fetching previous. "); }
            try {
                NodeEntity nextEntity = targetNodeClient.get().uri("/ring/NEXT").retrieve().bodyToMono(NodeEntity.class)
                        .onErrorResume(e -> {log.debug("Error/Empty NEXT for {}: {}",nodeBaseUrl,e.getMessage());return Mono.empty();}).block();
                nodeConfigDisplay.setNextNode(convertToDisplay(nextEntity));
            } catch (Exception e) { log.warn("Sync exc NEXT {}: {}", nodeBaseUrl, e.getMessage()); currentConfigErrorAccumulator.append("Error fetching next. "); }

            if (!currentConfigErrorAccumulator.isEmpty()) model.addAttribute("selectedNodeConfigError", currentConfigErrorAccumulator.toString().trim());
            model.addAttribute("selectedNodeConfig", nodeConfigDisplay);

            try {
                NodeClientFileListResponse clientFileResponse = targetNodeClient.get().uri("/node/file/list").retrieve()
                        .bodyToMono(NodeClientFileListResponse.class)
                        .onErrorResume(e -> {log.warn("Error files list for {}: {}",nodeBaseUrl,e.getMessage()); return Mono.just(new NodeClientFileListResponse());}).block(); // Ensure empty response on error
                model.addAttribute("selectedNodeLocalFiles", clientFileResponse.getLocalFiles());
                model.addAttribute("selectedNodeReplicatedFiles", clientFileResponse.getReplicatedFiles());
            } catch (Exception e) { log.error("Sync ex files list {}: {}", nodeBaseUrl, e.getMessage(), e); model.addAttribute("selectedNodeFilesError", "Error fetching files list from node.");}
        }
        return "gui_dashboard";
    }

    @GetMapping("/gui/node/check-health")
    public String checkNodeHealth(@RequestParam("nodeIp") String nodeIp,
                                  @RequestParam("nodeHash") Long nodeHash,
                                  RedirectAttributes redirectAttributes) {
        String nodeBaseUrl = "http://" + nodeIp + ":" + NODE_CLIENT_PORT;
        WebClient targetNodeClient = webClientBuilder.baseUrl(nodeBaseUrl).build();
        String healthStatus;
        String healthStatusKey = "healthStatus_" + nodeIp.replace(".", "_");

        log.info("Attempting health check for node: {} (Hash: {})", nodeIp, nodeHash);
        try {
            String healthResponse = targetNodeClient.get().uri("/health").retrieve().bodyToMono(String.class)
                    .onErrorResume(e -> Mono.just("ERROR: " + e.getMessage().split("\n")[0])).block();
            healthStatus = healthResponse;
        } catch (Exception e) { healthStatus = "FAILED (Exception: " + e.getMessage().split("\n")[0] + ")"; }
        redirectAttributes.addFlashAttribute(healthStatusKey, healthStatus);
        return "redirect:/gui/dashboard?node_ip=" + nodeIp + "&node_hash=" + nodeHash;
    }

    // MODIFIED: This method now triggers NodeClient shutdown instead of direct NamingServer deletion
    @PostMapping("/gui/node/remove")
    public String triggerNodeClientShutdown(@RequestParam("node_ip") String nodeIpToShutdown, // Renamed for clarity
                                            @RequestParam("node_hash") Long nodeHashForMessage, // For user messages
                                            RedirectAttributes redirectAttributes) {

        String nodeNameForMessage = "Node (Hash: " + nodeHashForMessage + ")";
        Optional<NodeEntity> nodeOpt = nodeStorageService.findById(nodeHashForMessage);
        if (nodeOpt.isPresent() && nodeOpt.get().getNodeName() != null && !nodeOpt.get().getNodeName().isEmpty()) {
            nodeNameForMessage = nodeOpt.get().getNodeName();
        }

        String nodeClientShutdownUrl = "http://" + nodeIpToShutdown + ":" + NODE_CLIENT_PORT + "/shutdown-trigger";
        log.info("Sending shutdown signal to node: {} (IP: {}) at URL: {}", nodeNameForMessage, nodeIpToShutdown, nodeClientShutdownUrl);

        try {
            WebClient client = webClientBuilder.baseUrl("http://" + nodeIpToShutdown + ":" + NODE_CLIENT_PORT).build();
            String response = client.post()
                    .uri("/shutdown-trigger") // Call NodeClient's shutdown endpoint
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            log.info("Node {} (IP: {}) responded to shutdown signal: {}", nodeNameForMessage, nodeIpToShutdown, response);
            redirectAttributes.addFlashAttribute("statusMessage",
                    "Shutdown signal sent to node " + nodeNameForMessage + ". It will de-register itself from the Naming Server if its shutdown process is successful.");

        } catch (WebClientRequestException e) {
            log.error("Failed to send shutdown signal to node {} at {}: {}", nodeNameForMessage, nodeIpToShutdown, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Could not connect to node " + nodeNameForMessage + " ("+ nodeIpToShutdown + ") to trigger shutdown. Is it running or reachable?");
        } catch (WebClientResponseException e) {
            log.error("Node {} (IP: {}) responded with error to shutdown signal: {} - {}", nodeNameForMessage, nodeIpToShutdown, e.getStatusCode(), e.getResponseBodyAsString(), e);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Node " + nodeNameForMessage + " ("+nodeIpToShutdown+") responded with error (" + e.getStatusCode() + ") to shutdown signal. Check NodeClient logs.");
        } catch (Exception e) {
            log.error("Unexpected error sending shutdown signal to node {} (IP: {}): {}", nodeNameForMessage, nodeIpToShutdown, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "An error occurred sending shutdown signal to node " + nodeNameForMessage + ".");
        }
        // Redirect back to the main dashboard. The node will disappear from the list
        // once its @PreDestroy (OnShutdownBean) calls the NamingServer's delete endpoint.
        return "redirect:/gui/dashboard";
    }
    @PostMapping("/gui/namingserver/shutdown")
    public String shutdownNamingServer(RedirectAttributes redirectAttributes) {
        log.warn("Received request to SHUTDOWN THE NAMING SERVER.");
        // You can't really redirect after this if it's successful,
        // as the server will be shutting down.
        // The response might not even reach the client fully.
        // The main purpose is to trigger the shutdown.

        // Optionally, add a small delay if you want to try and send a message back,
        // but it's not guaranteed.
        new Thread(() -> {
            try {
                Thread.sleep(500); // Give a moment for a potential response partial send
                log.info("Initiating Naming Server shutdown via SpringApplication.exit()...");
                int exitCode = SpringApplication.exit(appContext, () -> 0);
                log.info("Naming Server SpringApplication.exit() completed with code {}. Exiting JVM.", exitCode);
                System.exit(exitCode); // Ensure JVM process terminates
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Naming Server shutdown thread interrupted.", e);
            } catch (Exception e) {
                log.error("Exception during Naming Server shutdown: {}", e.getMessage(), e);
                // If shutdown fails, the server might still be running.
            }
        }).start();

        // This message is unlikely to be seen if shutdown is quick.
        // Consider displaying a "Shutting down..." message on the page via JavaScript before submitting the form.
        redirectAttributes.addFlashAttribute("statusMessage", "Naming Server shutdown initiated. The server will now close.");
        // If you redirect to the dashboard, it will likely fail to load as the server is stopping.
        // A static "Shutting down..." page or just letting the connection drop might be more realistic.
        // For now, let's redirect and see what happens.
        return "redirect:/gui/dashboard";
    }
}