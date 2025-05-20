package NodeClient.File;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/node/file")
public class FileController {
    private final FileService fileService;

    @Autowired
    public FileController(FileService fileService) {
       this.fileService = fileService;
    }

    // User calls the right node to download the file no extra checkups
    // User has to first call the Naming Server to get the right node IP which stores the file
    // Maybe (if it has to) we can add here that we call the naming server ourselves
    @GetMapping("/{name}")
    public ResponseEntity<byte[]> downloadFile(@PathVariable String name) throws IOException {
        byte[] fileData = fileService.readFile(name);
        if (fileData == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(fileData, HttpStatus.OK);
    }
    
    @PostMapping("/upload")
    @ResponseStatus(HttpStatus.CREATED)
    public void uploadFile(@RequestParam("file") MultipartFile file) throws IOException {
        FileMessage message = new FileMessage(file.getName(), "CREATE", file.getBytes());
        fileService.handleFileOperations(message);
        // NO replicate to neighbors as FileCheckerBean handles this
    }

    @DeleteMapping("/{name}")
    public void deleteFile(@PathVariable String name) throws IOException {
        FileMessage message = new FileMessage(name, "DELETE_LOCAL", null);
        fileService.handleFileOperations(message);
        fileService.replicateToNeighbors(name, "DELETE_REPLICA", null);
    }

    @PostMapping("/replication")
    public void handleReplication(@RequestBody FileMessage message) throws IOException {
        fileService.handleFileOperations(message);
    }

    @PostMapping("/transfer")
    public void handleTransfer(@RequestBody FileMessage message) throws IOException {
        fileService.handleTransfer(message);
    }

    /**
     * Lists all files (local and replicated) on this node.
     * The GUI will call this endpoint as GET /node/file/api/list (due to RequestMapping)
     * or if you prefer GET /api/files/list, then this endpoint should be in a different controller
     * or this controller's RequestMapping should be adjusted.
     *
     * Let's make it /node/file/list for consistency with this controller.
     * The GUI controller will need to call: http://<node_ip>:8081/node/file/list
     */
    @GetMapping("/list") // This will be accessible at /node/file/list
    public ResponseEntity<List<SimpleFileDefinition>> listAllNodeFiles() {
        List<SimpleFileDefinition> filesDtoList = new ArrayList<>();

        try {
            List<String> localFileNames = fileService.getLocalFileNames();
            if (localFileNames != null) {
                for (String name : localFileNames) {
                    filesDtoList.add(new SimpleFileDefinition(name, "local"));
                }
            }
        } catch (Exception e) {
            System.err.println("Error retrieving local files for /list endpoint: " + e.getMessage());
            // Optionally, you could return an error response here if this part fails critically
        }

        try {
            List<String> replicatedFileNames = fileService.getReplicatedFileNames();
            if (replicatedFileNames != null) {
                for (String name : replicatedFileNames) {
                    filesDtoList.add(new SimpleFileDefinition(name, "replicated"));
                }
            }
        } catch (Exception e) {
            System.err.println("Error retrieving replicated files for /list endpoint: " + e.getMessage());
        }

        return ResponseEntity.ok(filesDtoList);
    }

    // Inner static DTO class for the file list response.
    // This makes the FileController self-contained for this endpoint's response structure.
    // The GUI's FileInfoDisplay DTO should have compatible field names ("fileName", "type").
    public static class SimpleFileDefinition {
        private String fileName;
        private String type;

        public SimpleFileDefinition(String fileName, String type) {
            this.fileName = fileName;
            this.type = type;
        }

        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
    }
}
