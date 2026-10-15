package pt.ist.socialsoftware.mono2micro.softwareSystem.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import pt.ist.socialsoftware.mono2micro.softwareSystem.domain.SoftwareSystem;

import java.util.List;

public interface SoftwareSystemRepository extends MongoRepository<SoftwareSystem, String> {

	boolean existsByName(String softwareSystemName);

	@Query(value = "{}", fields = "{'_id': 1, 'name': 1, 'isEmpty': 1}") // Will only get id, name and if it contains strategies
	List<SoftwareSystem> getSoftwareSystems();
}