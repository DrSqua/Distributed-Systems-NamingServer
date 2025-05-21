package schnitzel.NamingServer.GUI;

import schnitzel.NamingServer.GUI.DTO.FileInfoDisplay;
import schnitzel.NamingServer.GUI.DTO.NodeConfigDisplay;
import schnitzel.NamingServer.GUI.DTO.NodeInfoDisplay;
import schnitzel.NamingServer.GUI.DTO.NodeClientFileListResponse;
import schnitzel.NamingServer.GUI.NodeClientFileDTO;
import schnitzel.NamingServer.Node.NodeStorageService; // Direct access
import schnitzel.NamingServer.Node.NodeController; // To use its parseIdentifier method (or replicate logic)
import Utilities.NodeEntity.NodeEntity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.reactive.function.client.WebClientRequestException;
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
    private final schnitzel.NamingServer.Node.NodeController nsNodeController; // For parseIdentifier
    private final int NODE_CLIENT_PORT = 8081;

    @Autowired
    public DashboardGuiController(NodeStorageService nodeStorageService,
                                  WebClient.Builder webClientBuilder,
                                  schnitzel.NamingServer.Node.NodeController nsNodeController) {
        this.nodeStorageService = nodeStorageService;
        this.webClientBuilder = webClientBuilder;
        this.nsNodeController = nsNodeController;
    }

    private NodeInfoDisplay convertToDisplay(NodeEntity entity) {
        if (entity == null) return null;
        return new NodeInfoDisplay(entity);
    }

    @GetMapping("/gui/dashboard")
    public String dashboard(Model model,
                            @RequestParam(name = "node_ip", required = false) String selectedNodeIp,
                            // We might also get node_name or node_hash from the link to easily find it in NodeStorageService
                            @RequestParam(name = "node_hash", required = false) Long selectedNodeHashParam) {

        Iterable<NodeEntity> allEntities = nodeStorageService.getAll();
        List<NodeInfoDisplay> allNodesForGui = StreamSupport.stream(allEntities.spliterator(), false)
                .map(this::convertToDisplay)
                .collect(Collectors.toList());
        model.addAttribute("allNodes", allNodesForGui);

        model.addAttribute("selectedNodeConfig", null);
        model.addAttribute("selectedNodeFiles", Collections.emptyList());
        model.addAttribute("selectedNodeConfigError", null);
        model.addAttribute("selectedNodeFilesError", null);

        if (selectedNodeIp != null) { // We still need IP to call NodeClient for files/neighbors
            String nodeBaseUrl = "http://" + selectedNodeIp + ":" + NODE_CLIENT_PORT;
            WebClient targetNodeClient = webClientBuilder.baseUrl(nodeBaseUrl).build();
            model.addAttribute("selectedNodeIdentifier", selectedNodeIp + ":" + NODE_CLIENT_PORT);

            NodeConfigDisplay nodeConfigDisplay = new NodeConfigDisplay();

            // --- Get Current Node Info directly from Naming Server's storage ---
            NodeEntity currentSelectedEntity = null;
            if (selectedNodeHashParam != null) { // If hash is passed from link
                currentSelectedEntity = nodeStorageService.findById(selectedNodeHashParam).orElse(null);
            } else {
                // Fallback: Iterate to find by IP if only IP is passed (less efficient)
                // This assumes IPs are unique for registered nodes, which they generally should be.
                for (NodeEntity entity : allEntities) {
                    if (entity.getIpAddress().equals(selectedNodeIp)) {
                        currentSelectedEntity = entity;
                        break;
                    }
                }
            }

            if (currentSelectedEntity != null) {
                nodeConfigDisplay.setCurrentNode(convertToDisplay(currentSelectedEntity));
            } else {
                log.warn("Could not find selected node with IP {} (or hash {}) in Naming Server storage.", selectedNodeIp, selectedNodeHashParam);
                model.addAttribute("selectedNodeConfigError", "Selected node details not found in Naming Server.");
            }
            // --- End of Current Node Info ---


            // Fetch Previous Node from NodeClient
            try {
                NodeEntity prevEntity = targetNodeClient.get()
                        .uri("/ring/PREVIOUS")
                        .retrieve()
                        .bodyToMono(NodeEntity.class)
                        .onErrorResume(WebClientResponseException.class, ex -> Mono.empty())
                        .onErrorResume(e -> { log.warn("Error fetching PREVIOUS for {}: {}", nodeBaseUrl, e.getMessage()); return Mono.empty(); })
                        .block();
                nodeConfigDisplay.setPreviousNode(convertToDisplay(prevEntity));
            } catch (Exception e) { log.warn("Exception fetching PREVIOUS for {}: {}", nodeBaseUrl, e.getMessage()); }

            // Fetch Next Node from NodeClient
            try {
                NodeEntity nextEntity = targetNodeClient.get()
                        .uri("/ring/NEXT")
                        .retrieve()
                        .bodyToMono(NodeEntity.class)
                        .onErrorResume(WebClientResponseException.class, ex -> Mono.empty())
                        .onErrorResume(e -> { log.warn("Error fetching NEXT for {}: {}", nodeBaseUrl, e.getMessage()); return Mono.empty(); })
                        .block();
                nodeConfigDisplay.setNextNode(convertToDisplay(nextEntity));
            } catch (Exception e) { log.warn("Exception fetching NEXT for {}: {}", nodeBaseUrl, e.getMessage()); }
            model.addAttribute("selectedNodeConfig", nodeConfigDisplay);


            // Fetch Node Files from NodeClient
            try {
                NodeClientFileListResponse clientFileResponse = targetNodeClient.get()
                        .uri("/node/file/list") // This matches your NodeClient's FileController
                        .retrieve()
                        .bodyToMono(NodeClientFileListResponse.class) // Deserializes into the NamingServer's DTO
                        .onErrorResume(WebClientResponseException.NotFound.class, e -> {
                            log.warn("/node/file/list not found (404) on {}. Assuming no files.", nodeBaseUrl);
                            // Return an empty response object to avoid nulls later
                            NodeClientFileListResponse emptyResp = new NodeClientFileListResponse();
                            emptyResp.setLocalFiles(Collections.emptyList());
                            emptyResp.setReplicatedFiles(Collections.emptyList());
                            return Mono.just(emptyResp);
                        })
                        .onErrorResume(WebClientRequestException.class, e -> { // Catches connection errors
                            log.error("Connection error fetching files from {}: {}", nodeBaseUrl, e.getMessage());
                            model.addAttribute("selectedNodeFilesError", "Node unreachable for file list.");
                            NodeClientFileListResponse emptyResp = new NodeClientFileListResponse();
                            emptyResp.setLocalFiles(Collections.emptyList());
                            emptyResp.setReplicatedFiles(Collections.emptyList());
                            return Mono.just(emptyResp);
                        })
                        .onErrorResume(e -> { // Catchall for other reactive errors
                            log.error("Generic reactive error fetching files from {}: {}", nodeBaseUrl, e.getMessage());
                            model.addAttribute("selectedNodeFilesError", "Unexpected error fetching files.");
                            NodeClientFileListResponse emptyResp = new NodeClientFileListResponse();
                            emptyResp.setLocalFiles(Collections.emptyList());
                            emptyResp.setReplicatedFiles(Collections.emptyList());
                            return Mono.just(emptyResp);
                        })
                        .block();

                if (clientFileResponse != null) {
                    model.addAttribute("selectedNodeLocalFiles",
                            clientFileResponse.getLocalFiles() != null ? clientFileResponse.getLocalFiles() : Collections.emptyList());
                    model.addAttribute("selectedNodeReplicatedFiles",
                            clientFileResponse.getReplicatedFiles() != null ? clientFileResponse.getReplicatedFiles() : Collections.emptyList());
                } else {
                    // This case should be rare if onErrorResume returns an empty object
                    model.addAttribute("selectedNodeLocalFiles", Collections.emptyList());
                    model.addAttribute("selectedNodeReplicatedFiles", Collections.emptyList());
                }
                // selectedNodeFilesError might have been set by onErrorResume blocks if it was a non-404 error

            } catch (Exception e) { // Catch synchronous exceptions from .block() or other issues
                log.error("Synchronous exception during files fetch for {}: {}", nodeBaseUrl, e.getMessage(), e);
                model.addAttribute("selectedNodeFilesError", "Error fetching files (Node API error or unavailable).");
                model.addAttribute("selectedNodeLocalFiles", Collections.emptyList());
                model.addAttribute("selectedNodeReplicatedFiles", Collections.emptyList());
            }
        }
        return "gui_dashboard";
    }
}