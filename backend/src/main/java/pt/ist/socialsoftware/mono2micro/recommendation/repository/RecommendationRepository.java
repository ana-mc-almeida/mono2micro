package pt.ist.socialsoftware.mono2micro.recommendation.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import pt.ist.socialsoftware.mono2micro.recommendation.domain.Recommendation;

import java.util.List;

public interface RecommendationRepository extends MongoRepository<Recommendation, String> {
	List<Recommendation> findByStrategyId(String strategyId);

	/** The underscore walks the profile reference; plain ProfileId would clash with getProfileId(). */
	boolean existsByProfile_Id(String profileId);

	default Recommendation getById(String id) {
		return findById(id).orElseThrow(() -> new RuntimeException("No recommendation " + id + " found."));
	}
}
