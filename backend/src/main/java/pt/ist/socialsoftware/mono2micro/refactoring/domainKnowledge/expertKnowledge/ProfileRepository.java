package pt.ist.socialsoftware.mono2micro.refactoring.domainKnowledge.expertKnowledge;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ProfileRepository extends MongoRepository<Profile, String> {
	/** The profiles offered for a representation: every one not retired. */
	List<Profile> findByRepresentationIdAndRetiredFalse(String representationId);

	void deleteByRepresentationId(String representationId);

	default Profile getById(String id) {
		return findById(id).orElseThrow(() -> new RuntimeException("No profile " + id + " found."));
	}
}
