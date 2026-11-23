package pt.ist.socialsoftware.mono2micro.softwareSystem.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import pt.ist.socialsoftware.mono2micro.softwareSystem.domain.SoftwareSystem;

import java.util.List;

public interface SoftwareSystemRepository extends MongoRepository<SoftwareSystem, String> {

	boolean existsByName(String softwareSystemName);

	// Only the id and the name; 'isEmpty' names a field no software system stores (ticket 20)
	@Query(value = "{}", fields = "{'_id': 1, 'name': 1, 'isEmpty': 1}")
	List<SoftwareSystem> getSoftwareSystems();

	default SoftwareSystem getById(String id) {
		return findById(id).orElseThrow(() -> new RuntimeException("No software system " + id + " found."));
	}
}
