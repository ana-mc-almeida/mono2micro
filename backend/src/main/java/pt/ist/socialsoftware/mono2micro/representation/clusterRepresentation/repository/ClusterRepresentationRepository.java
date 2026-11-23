package pt.ist.socialsoftware.mono2micro.representation.clusterRepresentation.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import pt.ist.socialsoftware.mono2micro.representation.clusterRepresentation.domain.ClusterRepresentation;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public interface ClusterRepresentationRepository extends MongoRepository<ClusterRepresentation, String> {
	List<ClusterRepresentation> findBySimilarityId(String similarityId);

	/** The names a similarity's decompositions already use, for picking a new one. */
	default Set<String> findNamesBySimilarityId(String similarityId) {
		return findBySimilarityId(similarityId).stream()
				.map(ClusterRepresentation::getName)
				.collect(Collectors.toSet());
	}

	default ClusterRepresentation getById(String id) {
		return findById(id).orElseThrow(() -> new RuntimeException("No decomposition " + id + " found."));
	}
}
