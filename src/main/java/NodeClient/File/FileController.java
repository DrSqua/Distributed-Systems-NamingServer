package NodeClient.File;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@RestController
@RequestMapping("/node/file")
public class FileController {
    private final Path localPath = Paths.get("").toAbsolutePath().normalize().resolve("local_files");

    public FileController() throws IOException {
        Files.createDirectories(localPath);
    }

    @PostMapping("/create")
    @ResponseStatus(HttpStatus.CREATED)
    public void createFile(@RequestParam String name) throws IOException {
        Path target = localPath.resolve(name);
        Files.createFile(target);
    }

    @PostMapping("/upload")
    @ResponseStatus(HttpStatus.CREATED)
    public void uploadFile(@RequestParam("file") MultipartFile file) throws IOException {
        Path targetPath = localPath.resolve(file.getOriginalFilename());
        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
    }

    @DeleteMapping("/{name}")
    public void deleteFile(@PathVariable String name) throws IOException {
        Path targetPath = localPath.resolve(name);
        Files.deleteIfExists(targetPath);
    }
}
