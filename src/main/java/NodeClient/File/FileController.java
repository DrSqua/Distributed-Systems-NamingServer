package NodeClient.File;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

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
}
