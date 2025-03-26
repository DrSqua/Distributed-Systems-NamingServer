package schnitzel.NamingServer.File;

import org.springframework.web.bind.annotation.*;

import java.net.InetAddress;

@RestController
public class FileController {
    static class FileNotFoundException extends RuntimeException {
        FileNotFoundException(long fileName) {
            super("Could not find file with name:  " + fileName);
        }
    }

    static class InputFile {

    }

    private final FileRepository repository;
    FileController(FileRepository repository) {
        this.repository = repository;
    }

    // Query
    @GetMapping("/file")
    Iterable<FileMapping> query() {
        return repository.findAll(); // TODO This is useless?
    }

    // Get Unique
    @GetMapping("/file/{fileName}")
    FileMapping get(@PathVariable String fileName) {
        Integer fileHash = fileName.length(); // TODO, better hash
        return repository.findByFileHash(fileHash);
    }

    // Create New
    @PostMapping("/file")
    FileMapping newFile(@RequestBody InputFile newFile) {
        // TODO
        // HASH File and store in repository

        return null; // TODO repository.save(accountBalance);
    }

    // Delete
    @DeleteMapping("/file/{fileName}")
    void delete(@PathVariable String fileName) {
        // TODO Delete file from repository
    }
}
