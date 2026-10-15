package pt.ist.socialsoftware.mono2micro.representation.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import pt.ist.socialsoftware.mono2micro.representation.domain.Representation;

import java.util.Optional;

public interface RepresentationRepository extends MongoRepository<Representation, String> {
	Optional<Representation> findByName(String name);

	void deleteByName(String name);
}
