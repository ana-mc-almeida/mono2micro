package pt.ist.socialsoftware.mono2micro.similarity.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import pt.ist.socialsoftware.mono2micro.similarity.domain.Similarity;

import java.util.List;

public interface SimilarityRepository extends MongoRepository<Similarity, String> {
	List<Similarity> findByStrategyId(String strategyId);

	/** The underscore walks the profile reference; plain ProfileId would clash with getProfileId(). */
	boolean existsByProfile_Id(String profileId);

	default Similarity getById(String id) {
		return findById(id).orElseThrow(() -> new RuntimeException("No similarity " + id + " found."));
	}
}
