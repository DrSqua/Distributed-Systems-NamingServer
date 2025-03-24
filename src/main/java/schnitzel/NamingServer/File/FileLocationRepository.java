package schnitzel.NamingServer.File;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.map.repository.config.EnableMapRepositories;
import org.springframework.data.repository.CrudRepository;

import java.net.InetAddress;
import java.util.List;


@Configuration
@EnableMapRepositories
class KeyValueConfig {

}

interface FileRepository extends CrudRepository<Integer, InetAddress> {
    InetAddress findByFileHash(Integer fileHash);
}