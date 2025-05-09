package NodeClient.File;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/node/file")
public class FileController {
    private final FileService fileService;

    @Autowired
    public FileController(FileService fileService) throws IOException {
       this.fileService = fileService;
    }

    @PostMapping("/upload")
    @ResponseStatus(HttpStatus.CREATED)
    public void uploadFile(@RequestParam("file") MultipartFile file) throws IOException {
        fileService.storeFile(file);
        fileService.replicateToNeighbors(file.getOriginalFilename(), "CREATE", file.getBytes());
    }

    @DeleteMapping("/{name}")
    public void deleteFile(@PathVariable String name) throws IOException {
        fileService.deleteFile(name);
        fileService.replicateToNeighbors(name, "DELETE", null);
    }

    @PostMapping("/replication")
    public void handleReplication(@RequestBody ReplicationMessage message) throws IOException {
        fileService.handleReplication(message);
    }
}
