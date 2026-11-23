package pt.ist.socialsoftware.mono2micro.representation.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import pt.ist.socialsoftware.mono2micro.representation.clusterRepresentation.domain.ClusterRepresentation;
import pt.ist.socialsoftware.mono2micro.representation.domain.Representation;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public interface RepresentationRepository extends MongoRepository<Representation, String> {
	List<Representation> findBySoftwareSystemId(String softwareSystemId);

	/**
	 * The representations read from the files the software system uploaded: all its representations but the cluster representations, which are
	 * imported or produced and are listed by their Similarity.
	 */
	default List<Representation> findUploadedBySoftwareSystemId(String softwareSystemId) {
		return uploaded(findBySoftwareSystemId(softwareSystemId));
	}

	/** Those of {@code representations} read from uploaded files. */
	static List<Representation> uploaded(List<Representation> representations) {
		return representations.stream()
				.filter(representation -> !(representation instanceof ClusterRepresentation))
				.collect(Collectors.toList());
	}

	/** A kind is the class's constant, not a stored field, so the filter runs here rather than in the query. */
	default Optional<Representation> findBySoftwareSystemIdAndType(String softwareSystemId, String type) {
		return ofType(findBySoftwareSystemId(softwareSystemId), type);
	}

	/** The one representation of the given kind; a software system holds at most one per kind. */
	static Optional<Representation> ofType(List<Representation> representations, String type) {
		return representations.stream()
				.filter(representation -> representation.getType().equals(type))
				.findFirst();
	}

	default Representation getById(String id) {
		return findById(id).orElseThrow(() -> new RuntimeException("No representation " + id + " found."));
	}
}
