package schnitzel.NamingServer.GUI;

import schnitzel.NamingServer.GUI.DTO.FileInfoDisplay; // Assuming this DTO exists
import schnitzel.NamingServer.GUI.DTO.NodeConfigDisplay; // Assuming this DTO exists
import schnitzel.NamingServer.GUI.DTO.NodeInfoDisplay;   // Assuming this DTO exists
import schnitzel.NamingServer.GUI.DTO.NodeClientFileListResponse; // Assuming this DTO exists for WebClient response
import schnitzel.NamingServer.Node.NodeStorageService;
import Utilities.NodeEntity.NodeEntity; // Your actual NodeEntity

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
    // NamingServer's NodeController is no longer needed if NodeStorageService is used directly for removal
    // private final schnitzel.NamingServer.Node.NodeController nsNodeController;
    private final int NODE_CLIENT_PORT = 8081;

    @Autowired
    public DashboardGuiController(NodeStorageService nodeStorageService,
                                  WebClient.Builder webClientBuilder
            /*, schnitzel.NamingServer.Node.NodeController nsNodeController */) {
        this.nodeStorageService = nodeStorageService;
        this.webClientBuilder = webClientBuilder;
        // this.nsNodeController = nsNodeController;
    }

    // Helper to convert NodeEntity to NodeInfoDisplay DTO for the GUI
    // Assumes NodeInfoDisplay constructor takes NodeEntity and handles setting fixed port
    private NodeInfoDisplay convertToDisplay(NodeEntity entity) {
        if (entity == null) return null;
        return new NodeInfoDisplay(entity);
    }

    @GetMapping("/gui/dashboard")
    public String dashboard(Model model,
                            @RequestParam(name = "node_ip", required = false) String selectedNodeIp,
                            @RequestParam(name = "node_hash", required = false) Long selectedNodeHashParam,
                            // Removed 'action' param, health check has its own GET mapping now
                            // @RequestParam(name = "action", required = false) String action,
                            @RequestParam(name = "status_message", required = false) String statusMessage,
                            @RequestParam(name = "error_message", required = false) String errorMessage) {

        log.info("Dashboard Request: node_ip=[{}], node_hash=[{}]", selectedNodeIp, selectedNodeHashParam);

        // Pass redirect messages to the model
        if (statusMessage != null) model.addAttribute("statusMessage", statusMessage);
        if (errorMessage != null) model.addAttribute("errorMessage", errorMessage);

        // 1. Fetch all nodes
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
        if (systemErrorMessage != null) {
            model.addAttribute("systemError", systemErrorMessage);
        }

        // Initialize model attributes for the details section
        model.addAttribute("selectedNodeIdentifier", null);
        model.addAttribute("selectedNodeConfig", null);
        model.addAttribute("selectedNodeLocalFiles", Collections.emptyList());
        model.addAttribute("selectedNodeReplicatedFiles", Collections.emptyList());
        model.addAttribute("selectedNodeConfigError", null);
        model.addAttribute("selectedNodeFilesError", null);
        if (selectedNodeIp != null && !model.containsAttribute("healthStatus_" + selectedNodeIp.replace(".", "_"))) {
            model.addAttribute("healthStatus_" + selectedNodeIp.replace(".", "_"), null); // For health status display
        }


        if (selectedNodeIp != null) {
            model.addAttribute("selectedNodeIp", selectedNodeIp);
            model.addAttribute("selectedNodeHashParam", selectedNodeHashParam);

            String nodeBaseUrl = "http://" + selectedNodeIp + ":" + NODE_CLIENT_PORT;
            WebClient targetNodeClient = webClientBuilder.baseUrl(nodeBaseUrl).build();
            model.addAttribute("selectedNodeIdentifier", selectedNodeIp + ":" + NODE_CLIENT_PORT);

            NodeConfigDisplay nodeConfigDisplay = new NodeConfigDisplay();
            String currentConfigError = null; // Local var for config errors

            // --- Get Current Node Info directly from Naming Server's storage ---
            NodeEntity currentSelectedEntity = null;
            if (selectedNodeHashParam != null) {
                currentSelectedEntity = nodeStorageService.findById(selectedNodeHashParam).orElse(null);
                if (currentSelectedEntity == null) {
                    log.warn("Node with hash {} NOT FOUND in NodeStorageService via HASH.", selectedNodeHashParam);
                    currentConfigError = "Selected node (hash: " + selectedNodeHashParam + ") details not found in Naming Server.";
                }
            } else {
                log.warn("Selected node_hash parameter was null. Cannot reliably find node by IP {} without iterating.", selectedNodeIp);
                currentConfigError = "Node hash not provided for selection.";
                // Optionally, implement IP fallback if truly needed, but hash is better.
            }
            if (currentSelectedEntity != null) {
                nodeConfigDisplay.setCurrentNode(convertToDisplay(currentSelectedEntity));
            }
            if (currentConfigError != null) {
                model.addAttribute("selectedNodeConfigError", currentConfigError);
            }

            // --- Fetch Previous Node ---
            try {
                NodeEntity prevEntity = targetNodeClient.get()
                        .uri("/ring/PREVIOUS")
                        .retrieve().bodyToMono(NodeEntity.class)
                        .onErrorResume(WebClientResponseException.NotFound.class, e -> Mono.empty())
                        .onErrorResume(WebClientRequestException.class, e -> { log.warn("Conn err PREVIOUS {}: {}", nodeBaseUrl, e.getMessage()); return Mono.empty(); })
                        .onErrorResume(WebClientResponseException.class, e -> { log.warn("HTTP err PREVIOUS {}: {}", nodeBaseUrl, e.getStatusCode()); return Mono.empty(); })
                        .block();
                nodeConfigDisplay.setPreviousNode(convertToDisplay(prevEntity));
            } catch (Exception e) { log.warn("Sync exc PREVIOUS {}: {}", nodeBaseUrl, e.getMessage());}

            // --- Fetch Next Node ---
            try {
                NodeEntity nextEntity = targetNodeClient.get()
                        .uri("/ring/NEXT")
                        .retrieve().bodyToMono(NodeEntity.class)
                        .onErrorResume(WebClientResponseException.NotFound.class, e -> Mono.empty())
                        .onErrorResume(WebClientRequestException.class, e -> { log.warn("Conn err NEXT {}: {}", nodeBaseUrl, e.getMessage()); return Mono.empty(); })
                        .onErrorResume(WebClientResponseException.class, e -> { log.warn("HTTP err NEXT {}: {}", nodeBaseUrl, e.getStatusCode()); return Mono.empty(); })
                        .block();
                nodeConfigDisplay.setNextNode(convertToDisplay(nextEntity));
            } catch (Exception e) { log.warn("Sync exc NEXT {}: {}", nodeBaseUrl, e.getMessage());}
            model.addAttribute("selectedNodeConfig", nodeConfigDisplay);


            // --- Fetch Node Files ---
            try {
                NodeClientFileListResponse clientFileResponse = targetNodeClient.get()
                        .uri("/node/file/list") // Your NodeClient endpoint
                        .retrieve().bodyToMono(NodeClientFileListResponse.class)
                        .onErrorResume(WebClientResponseException.NotFound.class, e_resp_nf -> {
                            log.warn("/node/file/list not found (404) on {}. Assuming no files.", nodeBaseUrl);
                            return Mono.just(createEmptyFileListResponse());
                        })
                        .onErrorResume(WebClientRequestException.class, e_req -> {
                            log.error("Connection error fetching files from {}: {}", nodeBaseUrl, e_req.getMessage());
                            model.addAttribute("selectedNodeFilesError", "Node unreachable for file list.");
                            return Mono.just(createEmptyFileListResponse());
                        })
                        .onErrorResume(WebClientResponseException.class, e_resp -> {
                            log.warn("HTTP error fetching files for {}: status={}", nodeBaseUrl, e_resp.getStatusCode());
                            model.addAttribute("selectedNodeFilesError", "Error from Node API fetching files: " + e_resp.getStatusCode());
                            return Mono.just(createEmptyFileListResponse());
                        })
                        .block();

                if (clientFileResponse != null) {
                    model.addAttribute("selectedNodeLocalFiles",
                            clientFileResponse.getLocalFiles() != null ? clientFileResponse.getLocalFiles() : Collections.emptyList());
                    model.addAttribute("selectedNodeReplicatedFiles",
                            clientFileResponse.getReplicatedFiles() != null ? clientFileResponse.getReplicatedFiles() : Collections.emptyList());
                }
            } catch (Exception e) {
                log.error("Synchronous exception during files fetch for {}: {}", nodeBaseUrl, e.getMessage(), e);
                model.addAttribute("selectedNodeFilesError", "Error fetching files (Node API error or unavailable).");
            }
        }
        return "gui_dashboard";
    }

    private NodeClientFileListResponse createEmptyFileListResponse() {
        NodeClientFileListResponse emptyResp = new NodeClientFileListResponse();
        emptyResp.setLocalFiles(Collections.emptyList());
        emptyResp.setReplicatedFiles(Collections.emptyList());
        return emptyResp;
    }

    @GetMapping("/gui/node/check-health")
    public String checkNodeHealth(@RequestParam("nodeIp") String nodeIp,
                                  @RequestParam("nodeHash") Long nodeHash,
                                  RedirectAttributes redirectAttributes) {
        String nodeBaseUrl = "http://" + nodeIp + ":" + NODE_CLIENT_PORT;
        WebClient targetNodeClient = webClientBuilder.baseUrl(nodeBaseUrl).build();
        String healthStatus;

        log.info("Attempting health check for node: {} ({})", nodeIp, nodeHash);
        try {
            String healthResponse = targetNodeClient.get() // GET request
                    .uri("/health") // NodeClient's GET /health endpoint
                    .retrieve()
                    .bodyToMono(String.class)
                    .onErrorResume(WebClientResponseException.class, ex ->
                            Mono.just("ERROR: " + ex.getStatusCode() + " - " + ex.getResponseBodyAsString())
                    )
                    .onErrorResume(WebClientRequestException.class, ex ->
                            Mono.just("ERROR: Node Unreachable - " + ex.getMessage())
                    )
                    .block();
            healthStatus = "Health (" + nodeIp + "): " + healthResponse;
        } catch (Exception e) {
            log.error("Exception during health check for {}: {}", nodeIp, e.getMessage(), e);
            healthStatus = "Health (" + nodeIp + "): FAILED (Exception: " + e.getMessage() + ")";
        }
        redirectAttributes.addFlashAttribute("healthStatus_" + nodeIp.replace(".", "_"), healthStatus);
        return "redirect:/gui/dashboard?node_ip=" + nodeIp + "&node_hash=" + nodeHash;
    }

    @PostMapping("/gui/node/remove")
    public String removeNode(@RequestParam("nodeHash") Long nodeHash,
                             RedirectAttributes redirectAttributes) {
        log.info("Attempting to remove node with hash: {}", nodeHash);
        try {
            if (nodeStorageService.existsById(nodeHash)) {
                NodeEntity removedNode = nodeStorageService.findById(nodeHash).orElse(null);
                nodeStorageService.deleteById(nodeHash); // Direct call to NamingServer's service
                String nodeName = (removedNode != null) ? removedNode.getNodeName() : String.valueOf(nodeHash);
                log.info("Node {} (Hash: {}) removed successfully.", nodeName, nodeHash);
                redirectAttributes.addFlashAttribute("statusMessage", "Node " + nodeName + " (Hash: " + nodeHash + ") removed successfully.");
            } else {
                log.warn("Attempted to remove non-existent node with hash: {}", nodeHash);
                redirectAttributes.addFlashAttribute("errorMessage", "Node with hash " + nodeHash + " not found for removal.");
            }
        } catch (Exception e) {
            log.error("Error removing node with hash {}: {}", nodeHash, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Error removing node: " + e.getMessage());
        }
        return "redirect:/gui/dashboard"; // Go back to main dashboard, node will be gone from list
    }
}