package schnitzel.NamingServer;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class FileController {
    static class FileNotFoundException extends RuntimeException {
        FileNotFoundException(long fileName) {
            super("Could not find file with name:  " + fileName);
        }
    }

    // TODO private final AccountBalanceRepository repository;
    FileController() {

    }

    // Query
    @GetMapping("/file")
    List<FileMapping> query() {
        return null; // TODO repository.findAll();
    }

    // Get Unique
    @GetMapping("/file/{file_name}")
    FileMapping get(@PathVariable String file_name) {
        // TODO Look up file and location in mapping repository
        return null;
    }

    // Create New
    @PostMapping("/file")
    FileMapping newFile(@RequestBody InputFile newFile) {
        // TODO
        // HASH File and store in repository

        return null; // TODO repository.save(accountBalance);
    }

    // Delete
    @DeleteMapping("/file/{file_name}")
    void delete(@PathVariable String file_name) {
        // TODO Delete file from repository
    }
}
