package pt.ist.socialsoftware.mono2micro.refactoring.domainKnowledge.expertKnowledge;

import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pt.ist.socialsoftware.mono2micro.fileManager.GridFsService;
import pt.ist.socialsoftware.mono2micro.representation.domain.Representation;
import pt.ist.socialsoftware.mono2micro.representation.repository.RepresentationRepository;

import javax.management.openmbean.KeyAlreadyExistsException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ProfileService {
	@Autowired
	ProfileRepository profileRepository;

	@Autowired
	RepresentationRepository representationRepository;

	@Autowired
	GridFsService gridFsService;

	public Profile createProfile(String representationId, String name) throws Exception {
		Representation representation = representationRepository.getById(representationId);
		getUnits(representation); // fails when no profile can scope the representation
		if (profileRepository.findByRepresentationIdAndRetiredFalse(representationId).stream().anyMatch(profile -> profile.getName().equals(name)))
			throw new KeyAlreadyExistsException("Profile " + name + " already exists.");

		return profileRepository.save(new Profile(name, Collections.emptySet(), representation));
	}

	/**
	 * Moves the units into the target profile, out of whichever profile holds them. A null target moves them to
	 * Unassigned. Every profile the move changes is replaced by a new version, so nothing built on the old one changes.
	 */
	public void moveUnits(String representationId, Set<String> units, String targetProfileId) {
		List<Profile> profiles = profileRepository.findByRepresentationIdAndRetiredFalse(representationId);
		if (targetProfileId != null && profiles.stream().noneMatch(profile -> profile.getId().equals(targetProfileId)))
			throw new IllegalArgumentException("No profile " + targetProfileId + " offered for representation " + representationId + ".");

		for (Profile profile : profiles) {
			Set<String> versionUnits = new HashSet<>(profile.getUnits());
			boolean changed = profile.getId().equals(targetProfileId) ? versionUnits.addAll(units) : versionUnits.removeAll(units);
			if (changed) {
				profileRepository.save(profile.withUnits(versionUnits));
				profileRepository.delete(profile);
			}
		}
	}

	/** The profile's units go back to Unassigned. */
	public void deleteProfile(String profileId) {
		profileRepository.delete(profileRepository.getById(profileId));
	}

	public ProfilesDto getProfiles(String representationId) throws Exception {
		List<Profile> profiles = profileRepository.findByRepresentationIdAndRetiredFalse(representationId);

		Set<String> unassigned = new TreeSet<>(getUnits(representationRepository.getById(representationId)));
		profiles.forEach(profile -> unassigned.removeAll(profile.getUnits()));

		return new ProfilesDto(
				profiles.stream().map(ProfileDto::new).collect(Collectors.toList()),
				new ArrayList<>(unassigned));
	}

	/** Every unit the representation's file holds; what a Similarity with no profile is built on. */
	private Set<String> getUnits(Representation representation) throws Exception {
		Set<String> units = representation.readUnits(IOUtils.toByteArray(gridFsService.getFile(representation.getId())));
		if (units == null)
			throw new IllegalArgumentException("A " + representation.getType() + " representation has no units a profile can select.");

		return units;
	}
}
