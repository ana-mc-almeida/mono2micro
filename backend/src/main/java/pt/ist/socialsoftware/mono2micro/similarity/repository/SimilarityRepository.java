package pt.ist.socialsoftware.mono2micro.similarity.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import pt.ist.socialsoftware.mono2micro.representation.domain.Representation;
import pt.ist.socialsoftware.mono2micro.similarity.domain.Similarity;

import java.util.Collection;
import java.util.List;

public interface SimilarityRepository extends MongoRepository<Similarity, String> {

	/** The Similarities that read the representation. The underscore walks the reference, which Spring Data turns into a DBRef. */
	List<Similarity> findByRepresentations_Id(String representationId);

	/**
	 * The Similarities that read any of the representations. Passed the entities, not their ids: an {@code _IdIn} query compares the stored
	 * DBRefs with plain id strings and never matches.
	 */
	List<Similarity> findByRepresentationsIn(Collection<Representation> representations);

	/** The underscore walks the profile reference; plain ProfileId would clash with getProfileId(). */
	boolean existsByProfile_Id(String profileId);

	default Similarity getById(String id) {
		return findById(id).orElseThrow(() -> new RuntimeException("No similarity " + id + " found."));
	}
}
