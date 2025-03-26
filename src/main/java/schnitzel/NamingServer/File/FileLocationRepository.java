package schnitzel.NamingServer.File;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.map.repository.config.EnableMapRepositories;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.net.InetAddress;
import java.util.List;


@Configuration
@EnableMapRepositories
class KeyValueConfig {

}

@Repository
interface FileRepository extends CrudRepository<FileMapping, InetAddress> {
    FileMapping findByFileHash(Integer fileHash);
}