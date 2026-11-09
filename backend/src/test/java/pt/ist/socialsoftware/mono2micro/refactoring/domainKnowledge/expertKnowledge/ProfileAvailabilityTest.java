package pt.ist.socialsoftware.mono2micro.refactoring.domainKnowledge.expertKnowledge;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import pt.ist.socialsoftware.mono2micro.feature.DerivationRecord;
import pt.ist.socialsoftware.mono2micro.feature.DerivationRecord.FeatureAvailability;
import pt.ist.socialsoftware.mono2micro.feature.UnavailableFeatureException;
import pt.ist.socialsoftware.mono2micro.representation.analytical.domain.AccessesRepresentation;
import pt.ist.socialsoftware.mono2micro.representation.analytical.domain.CodeEmbeddingsRepresentation;
import pt.ist.socialsoftware.mono2micro.representation.analytical.domain.StructureRepresentation;
import pt.ist.socialsoftware.mono2micro.representation.domain.Representation;
import pt.ist.socialsoftware.mono2micro.representation.entityDictionary.EntityToIDRepresentation;
import pt.ist.socialsoftware.mono2micro.representation.entityDictionary.IDToEntityRepresentation;
import pt.ist.socialsoftware.mono2micro.representation.repository.RepresentationRepository;
import pt.ist.socialsoftware.mono2micro.similarity.kind.AccessesKind;
import pt.ist.socialsoftware.mono2micro.similarity.kind.ClassVectorizationKind;
import pt.ist.socialsoftware.mono2micro.similarity.kind.StructureKind;

import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Where a profile is offered, at the {@link ProfileService} seam (D-017, D-025): {@link Profile} requires one of the Similarity kinds that
 * declare a representation a profile scopes, with the kind as input. No Spring and no Mongo: the software system's representations come from a
 * mocked repository.
 */
public class ProfileAvailabilityTest {
	private static final String SYSTEM = "system";

	@Mock
	private RepresentationRepository representationRepository;
	@Mock
	private ProfileRepository profileRepository;

	@InjectMocks
	private ProfileService profileService;

	@BeforeEach
	public void initMocks() {
		MockitoAnnotations.initMocks(this);
	}

	@Test
	public void aProfileIsOfferedForAKindItCanScope() {
		holds(new IDToEntityRepresentation(), new AccessesRepresentation());

		assertThat(profileFeature(profileService.getAvailability(SYSTEM, AccessesKind.ACCESSES)).isAvailable()).isTrue();
	}

	/** The record names the alternatives the kind is not among, so the reason is visible (D-005). */
	@Test
	public void aProfileIsWithheldForAKindItCannotScope() {
		holds(new IDToEntityRepresentation(), new AccessesRepresentation(), new EntityToIDRepresentation(), new CodeEmbeddingsRepresentation());

		FeatureAvailability profile = profileFeature(profileService.getAvailability(SYSTEM, ClassVectorizationKind.CLASS_VECTORIZATION));

		assertThat(profile.isAvailable()).isFalse();
		assertThat(profile.getFailedRequirements()).hasSize(1);
		assertThat(profile.getFailedRequirements().get(0))
				.contains("any of").contains("Accesses").contains("Structure").doesNotContain("Class Vectorization");
	}

	@Test
	public void anUnknownKindIsRefused() {
		holds(new IDToEntityRepresentation(), new AccessesRepresentation());

		assertThatThrownBy(() -> profileService.getAvailability(SYSTEM, "Accesses and Structure")).isInstanceOf(IllegalArgumentException.class);
	}

	/** Only a kind the system offers counts: Structure profiles need the Structure kind, so its representations too. */
	@Test
	public void onlyTheRepresentationsAnOfferedKindScopesLinkToProfiles() {
		holds(new IDToEntityRepresentation(), new AccessesRepresentation(), new StructureRepresentation());

		assertThat(profileService.getProfiledRepresentationTypes(SYSTEM)).containsExactly(AccessesRepresentation.ACCESSES);
	}

	@Test
	public void bothScopedRepresentationsLinkToProfilesWhenBothKindsAreOffered() {
		holds(new IDToEntityRepresentation(), new AccessesRepresentation(), new EntityToIDRepresentation(), new StructureRepresentation());

		assertThat(profileService.getProfiledRepresentationTypes(SYSTEM))
				.containsExactly(AccessesRepresentation.ACCESSES, StructureRepresentation.STRUCTURE);
	}

	@Test
	public void noRepresentationLinksToProfilesWhenNoKindIsOffered() {
		holds(new AccessesRepresentation());

		assertThat(profileService.getProfiledRepresentationTypes(SYSTEM)).isEmpty();
	}

	/** A crafted request is refused by the same check that withholds the selector (D-003), before the profile is even read. */
	@Test
	public void aProfileOnAKindThatTakesNoneIsRefused() {
		holds(new IDToEntityRepresentation(), new AccessesRepresentation(), new EntityToIDRepresentation(), new CodeEmbeddingsRepresentation());

		assertThatThrownBy(() -> profileService.getScopingProfile("someProfile", SYSTEM, new ClassVectorizationKind()))
				.isInstanceOf(UnavailableFeatureException.class);
	}

	/** The selector lists the profiles of the representation the kind scopes, so the frontend never picks it (review fix). */
	@Test
	public void theSelectorListsTheProfilesOfTheRepresentationTheKindScopes() {
		AccessesRepresentation accesses = new AccessesRepresentation();
		accesses.setId("accesses");
		StructureRepresentation structure = new StructureRepresentation();
		structure.setId("structure");
		holds(new IDToEntityRepresentation(), accesses, new EntityToIDRepresentation(), structure);
		Profile quizzes = new Profile("Quizzes", Collections.emptySet(), accesses);
		when(profileRepository.findByRepresentationIdAndRetiredFalse("accesses")).thenReturn(Collections.singletonList(quizzes));
		when(profileRepository.findByRepresentationIdAndRetiredFalse("structure")).thenReturn(Collections.emptyList());

		assertThat(profileService.getOfferedProfiles(SYSTEM, AccessesKind.ACCESSES)).containsExactly(quizzes);
		assertThat(profileService.getOfferedProfiles(SYSTEM, StructureKind.STRUCTURE)).isEmpty();
	}

	@Test
	public void theSelectorListsNoProfilesForAKindThatTakesNone() {
		holds(new IDToEntityRepresentation(), new AccessesRepresentation(), new EntityToIDRepresentation(), new CodeEmbeddingsRepresentation());

		assertThatThrownBy(() -> profileService.getOfferedProfiles(SYSTEM, ClassVectorizationKind.CLASS_VECTORIZATION))
				.isInstanceOf(UnavailableFeatureException.class);
	}

	@Test
	public void noProfileIsAlwaysAccepted() {
		assertThat(profileService.getScopingProfile(null, SYSTEM, new ClassVectorizationKind())).isNull();
	}

	private void holds(Representation... representations) {
		when(representationRepository.findBySoftwareSystemId(SYSTEM)).thenReturn(Arrays.asList(representations));
		when(representationRepository.findBySoftwareSystemIdAndType(anyString(), anyString())) // Mockito stubs the default method too
				.thenAnswer(invocation -> RepresentationRepository.ofType(Arrays.asList(representations), invocation.getArgument(1)));
	}

	private static FeatureAvailability profileFeature(DerivationRecord record) {
		assertThat(record.getFeatures()).extracting(FeatureAvailability::getName).containsExactly(Profile.PROFILE);
		return record.getFeatures().get(0);
	}
}
