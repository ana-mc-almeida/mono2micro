package pt.ist.socialsoftware.mono2micro.refactoring.domainKnowledge.expertKnowledge;

import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pt.ist.socialsoftware.mono2micro.fileManager.GridFsService;
import java.util.Collection;
import pt.ist.socialsoftware.mono2micro.utils.SelfRegistration;
import pt.ist.socialsoftware.mono2micro.similarity.kind.SimilarityKind;
import pt.ist.socialsoftware.mono2micro.feature.FeatureFamilies;
import pt.ist.socialsoftware.mono2micro.feature.DerivationRecord;
import pt.ist.socialsoftware.mono2micro.feature.AvailabilityContext;
import pt.ist.socialsoftware.mono2micro.feature.AvailabilityCheck;
import pt.ist.socialsoftware.mono2micro.recommendation.repository.RecommendationRepository;
import pt.ist.socialsoftware.mono2micro.representation.domain.Representation;
import pt.ist.socialsoftware.mono2micro.representation.repository.RepresentationRepository;
import pt.ist.socialsoftware.mono2micro.similarity.repository.SimilarityRepository;

import javax.management.openmbean.KeyAlreadyExistsException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

@Service
public class ProfileService {
	@Autowired
	ProfileRepository profileRepository;

	@Autowired
	RepresentationRepository representationRepository;

	@Autowired
	SimilarityRepository similarityRepository;

	@Autowired
	RecommendationRepository recommendationRepository;

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
				discard(profile);
			}
		}
	}

	/** The profile's units go back to Unassigned. */
	public void deleteProfile(String profileId) {
		discard(profileRepository.getById(profileId));
	}

	/**
	 * Whether a profile is offered for a Similarity of {@code kindName} on the software system: the {@link Profile} feature's derivation record,
	 * with the kind as input (D-013, D-017). The profile selector follows it.
	 */
	public DerivationRecord getAvailability(String softwareSystemId, String kindName) {
		return AvailabilityCheck.derive(SelfRegistration.prototypes(Profile.class).values(),
				systemContext(softwareSystemId).withInput(SimilarityKind.getByName(kindName)));
	}

	/**
	 * The profiles the selector lists for a Similarity of {@code kindName}: the offered profiles of the software system's representation the kind
	 * scopes, so the frontend never decides which representation that is.
	 *
	 * @throws pt.ist.socialsoftware.mono2micro.feature.UnavailableFeatureException
	 * if no profile is offered for the kind
	 */
	public List<Profile> getOfferedProfiles(String softwareSystemId, String kindName) {
		SimilarityKind kind = SimilarityKind.getByName(kindName);
		AvailabilityCheck.requireAvailable(profilePrototype(), systemContext(softwareSystemId).withInput(kind));

		return representationRepository.findBySoftwareSystemIdAndType(softwareSystemId, kind.scopedRepresentationType())
				.map(representation -> profileRepository.findByRepresentationIdAndRetiredFalse(representation.getId()))
				.orElse(Collections.emptyList());
	}

	/**
	 * The types of the software system's representations that offer profiles: those a Similarity kind scopes, where the kind is offered and a
	 * profile is offered for it. The Profiles link follows it: whether it shows comes from the {@link Profile} feature, and which cards show it
	 * from the kind's {@link SimilarityKind#scopedRepresentationType()}.
	 */
	public Set<String> getProfiledRepresentationTypes(String softwareSystemId) {
		AvailabilityContext context = systemContext(softwareSystemId);
		Collection<SimilarityKind> kinds = SelfRegistration.prototypes(SimilarityKind.class).values();
		Set<String> offeredKinds = new HashSet<>(AvailabilityCheck.derive(kinds, context).availableNames());
		return kinds.stream()
				.filter(kind -> offeredKinds.contains(SelfRegistration.nameOf(kind)))
				.filter(kind -> isProfileOfferedFor(kind, context))
				.map(SimilarityKind::scopedRepresentationType)
				.collect(Collectors.toCollection(TreeSet::new));
	}

	/**
	 * The profile a new Similarity or recommendation of {@code kind} on the software system is scoped by, or null for none. A profile must be
	 * offered for the kind (422 otherwise), and must be an offered profile of that software system that selects units of the representation the
	 * kind scopes.
	 *
	 * @throws pt.ist.socialsoftware.mono2micro.feature.UnavailableFeatureException
	 * if a profile is requested for a kind that takes none
	 */
	public Profile getScopingProfile(String profileId, String softwareSystemId, SimilarityKind kind) {
		if (profileId == null)
			return null;
		AvailabilityCheck.requireAvailable(profilePrototype(), systemContext(softwareSystemId).withInput(kind));

		Profile profile = profileRepository.getById(profileId);
		if (profile.isRetired() || !profile.belongsTo(softwareSystemId))
			throw new IllegalArgumentException("Profile " + profileId + " is not offered for software system " + softwareSystemId + ".");
		if (!profile.getRepresentation().getType().equals(kind.scopedRepresentationType()))
			throw new IllegalArgumentException("Profile " + profileId + " selects " + profile.getRepresentation().getType()
					+ " units, but a " + SelfRegistration.nameOf(kind) + " Similarity reads " + kind.scopedRepresentationType() + " units.");
		return profile;
	}

	private boolean isProfileOfferedFor(SimilarityKind kind, AvailabilityContext context) {
		return !AvailabilityCheck.derive(Collections.singletonList(profilePrototype()), context.withInput(kind)).availableNames().isEmpty();
	}

	private static Profile profilePrototype() {
		return SelfRegistration.prototypes(Profile.class).get(Profile.PROFILE);
	}

	private AvailabilityContext systemContext(String softwareSystemId) {
		return FeatureFamilies.scanned().contextFor(representationRepository.findBySoftwareSystemId(softwareSystemId));
	}

	/** Stops offering the profile: retires it while a Similarity or recommendation uses it, and deletes it otherwise. */
	private void discard(Profile profile) {
		if (similarityRepository.existsByProfile_Id(profile.getId()) || recommendationRepository.existsByProfile_Id(profile.getId())) {
			profile.retire();
			profileRepository.save(profile);
		} else
			profileRepository.delete(profile);
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
