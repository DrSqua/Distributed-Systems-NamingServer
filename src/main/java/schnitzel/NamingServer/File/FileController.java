package schnitzel.NamingServer.File;

import org.springframework.web.bind.annotation.*;

import java.net.InetAddress;
import java.util.Map;

import static java.lang.Math.abs;

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
        final double max = 2147483647;
        Integer fileHash = (int) ((fileName.hashCode() + max) * (32768/max + max));
        return repository.findByFileHash(fileHash);
    }

    // Create New
    @PostMapping("/file")
    FileMapping newFile(@RequestBody InputFile newFile) {
        Integer IP_node = null; // TODO get nodes IP address
        final double max = 2147483647;
        Integer fileHash = (int) ((newFile.hashCode() + max) * (32768/max + max));
        Map<Integer, Integer> map = FileMapping.loadMapFromJson();
        map.put(fileHash, IP_node);
        FileMapping.saveMapToJson(map);
        return null;
    }

    // Delete
    @DeleteMapping("/file/{fileName}")
    void delete(@PathVariable String fileName) {
        Integer IP_node = null; // TODO get nodes IP address
        final double max = 2147483647;
        Integer fileHash = (int) ((fileName.hashCode() + max) * (32768/max + max));
        Map<Integer, Integer> map = FileMapping.loadMapFromJson();
        map.remove(fileHash);
        FileMapping.saveMapToJson(map);
    }
}
