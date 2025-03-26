package schnitzel.NamingServer.Node;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.map.repository.config.EnableMapRepositories;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.w3c.dom.Node;
import schnitzel.NamingServer.File.FileMapping;

import java.net.InetAddress;
import java.util.Optional;

@Repository
interface NodeRepository extends CrudRepository<NodeEntity, Integer> {
    Optional<NodeEntity> findByNodeHash(Long nodeHash);
}
