package pt.ist.socialsoftware.mono2micro.representation.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import pt.ist.socialsoftware.mono2micro.representation.domain.Representation;

import java.util.List;
import java.util.Optional;

public interface RepresentationRepository extends MongoRepository<Representation, String> {
	List<Representation> findBySoftwareSystemId(String softwareSystemId);

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
