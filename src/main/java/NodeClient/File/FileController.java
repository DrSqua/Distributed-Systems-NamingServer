package NodeClient.File;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

@RestController
@RequestMapping("/node/file")
public class FileController {
    private final FileService fileService;

    @Autowired
    public FileController(FileService fileService) {
       this.fileService = fileService;
    }
    
    @GetMapping("/{name}")
    public File downloadFile(@PathVariable String name) {
        //TODO download log + actually giving file back
        return null;
    }
    
    @PostMapping("/upload")
    @ResponseStatus(HttpStatus.CREATED)
    public void uploadFile(@RequestParam("file") MultipartFile file) throws IOException {
        FileMessage message = new FileMessage(file.getName(), "CREATE", file.getBytes());
        fileService.handleFileOperations(message);
        fileService.replicateToNeighbors(file.getOriginalFilename(),"REPLICATE", file.getBytes());
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
}
