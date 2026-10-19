package pt.ist.socialsoftware.mono2micro.decomposition.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import pt.ist.socialsoftware.mono2micro.decomposition.domain.Decomposition;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public interface DecompositionRepository extends MongoRepository<Decomposition, String> {
	List<Decomposition> findBySimilarityId(String similarityId);

	List<Decomposition> findByStrategyId(String strategyId);

	/** The names a similarity's decompositions already use, for picking a new one. */
	default Set<String> findNamesBySimilarityId(String similarityId) {
		return findBySimilarityId(similarityId).stream()
				.map(Decomposition::getName)
				.collect(Collectors.toSet());
	}

	default Decomposition getById(String id) {
		return findById(id).orElseThrow(() -> new RuntimeException("No decomposition " + id + " found."));
	}
}
