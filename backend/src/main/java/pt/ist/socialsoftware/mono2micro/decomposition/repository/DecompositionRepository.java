package pt.ist.socialsoftware.mono2micro.decomposition.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import pt.ist.socialsoftware.mono2micro.decomposition.domain.Decomposition;

public interface DecompositionRepository extends MongoRepository<Decomposition, String> {
	default Decomposition getById(String id) {
		return findById(id).orElseThrow(() -> new RuntimeException("No decomposition " + id + " found."));
	}
}
