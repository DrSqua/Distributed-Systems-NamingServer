package NodeClient.File;

import NodeClient.NodeClient;
import NodeClient.Agents.FailureAgent;
import Utilities.RestMessagesRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.List;

@RestController
@RequestMapping("/node/file")
public class FileController {
    private final FileService fileService;
    private final ApplicationContext applicationContext;

    @Autowired
    public FileController(FileService fileService, ApplicationContext applicationContext) {
        this.fileService = fileService;
        this.applicationContext = applicationContext;
    }

    // User calls the right node to download the file no extra checkups
    // If the user doesn't know where the file belongs then the
    // user has to first call the Naming Server to get the right node IP which stores the file
    // Maybe (if it has to) we can add here that we call the naming server ourselves
    @GetMapping("/{name}")
    public ResponseEntity<byte[]> downloadFile(@PathVariable String name) throws IOException {
        byte[] fileData = fileService.downloadFile(name);

        if (fileData == null) {
            return new ResponseEntity<>(("File " + name + " not found").getBytes(), HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(fileData, HttpStatus.OK);
    }

    @PostMapping("/upload")
    @ResponseStatus(HttpStatus.CREATED)
    public void uploadFile(@RequestParam("file") MultipartFile file) throws IOException {
        FileMessage message = new FileMessage(file.getOriginalFilename(), "CREATE", file.getBytes());
        fileService.handleFileOperations(message);
        // NO replicate to neighbors as FileCheckerBean handles this
    }

    @DeleteMapping("/{name}")
    public void deleteFile(@PathVariable String name) throws IOException {
        FileMessage message = new FileMessage(name, "DELETE_LOCAL", null);
        fileService.handleFileOperations(message);
        fileService.replicateToNeighbors(name, "DELETE_REPLICA", null);
    }

    @PostMapping("/edit/{name}")
    public void editFile(@PathVariable String name) throws IOException {
        fileService.editFile(name);
    }

    // Return a FileListResponse which has a localFileList and a replicatedFileList
    // Respectively containing all local files and all replicated files
    @GetMapping("/list")
    public ResponseEntity<FileListResponse> listAllFiles() throws IOException {
        List<String> localFiles = fileService.listLocalFiles();
        List<String> replicatedFiles = fileService.listReplicatedFiles();
        FileListResponse response = new FileListResponse(localFiles, replicatedFiles);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("/replication")
    public void handleReplication(@RequestBody FileMessage message) throws IOException {
        fileService.handleFileOperations(message);
    }

    @PostMapping("/transfer")
    public void handleTransfer(@RequestBody FileMessage message) throws IOException {
        fileService.handleTransfer(message);
    }

    @PostMapping("/agent/receive")
    public ResponseEntity<Void> receiveAgent(@RequestBody byte[] agentBytes) throws Exception {
        // read the incoming bytes and check if it's a FailureAgent
        ByteArrayInputStream bis = new ByteArrayInputStream(agentBytes);
        ObjectInputStream ois = new ObjectInputStream(bis);
        Object obj = ois.readObject();
        if (obj instanceof FailureAgent receivedAgent) {
            NodeClient nodeClient = applicationContext.getBean(NodeClient.class);
            Object[] arguments = new Object[] {
                    fileService,
                    fileService.getRingStorage(),
                    receivedAgent.getFailingNodeName(),
                    receivedAgent.getStartedNodeName()
            };
            nodeClient.startReceivedAgent(receivedAgent, arguments);
            return new ResponseEntity<>(HttpStatus.OK);
        }
        else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
}