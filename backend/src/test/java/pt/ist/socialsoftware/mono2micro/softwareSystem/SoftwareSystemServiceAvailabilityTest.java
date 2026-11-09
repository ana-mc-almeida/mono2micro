package pt.ist.socialsoftware.mono2micro.softwareSystem;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import pt.ist.socialsoftware.mono2micro.fixtures.SoftwareSystemFixtures;
import pt.ist.socialsoftware.mono2micro.representation.repository.RepresentationRepository;
import pt.ist.socialsoftware.mono2micro.similarity.kind.AccessesKind;
import pt.ist.socialsoftware.mono2micro.similarity.kind.RepositoryKind;
import pt.ist.socialsoftware.mono2micro.similarity.service.SimilarityService;

import static org.assertj.core.api.Assertions.assertThat;
import static pt.ist.socialsoftware.mono2micro.fixtures.SoftwareSystemFixtures.ACCESSES_BASED_NAME;
import static pt.ist.socialsoftware.mono2micro.fixtures.SoftwareSystemFixtures.EMPTY_NAME;
import static pt.ist.socialsoftware.mono2micro.fixtures.SoftwareSystemFixtures.REPOSITORY_BASED_NAME;
import static pt.ist.socialsoftware.mono2micro.representation.domain.Representation.ACCESSES_TYPE;

/**
 * Shows the fixture pattern in use against the availability seam, and is the worked example
 * later Phase 1 tickets should copy.
 *
 * <p>Two things this test deliberately does not do. It does not start Spring — the availability
 * question is answered by domain logic over representation types, so a stubbed repository is
 * enough and the suite stays fast and machine-independent. And it asserts only the derived
 * answer, never which map was read or which method was called, because those internals are
 * exactly what the Phase 1 refactor is meant to be free to change.
 */
public class SoftwareSystemServiceAvailabilityTest {

	@Mock
	private RepresentationRepository representationRepository;

	@InjectMocks
	private SoftwareSystemService softwareSystemService;

	@InjectMocks
	private SimilarityService similarityService;

	@BeforeEach
	public void givenTheFixtureSoftwareSystemsExist() {
		MockitoAnnotations.initMocks(this);
		SoftwareSystemFixtures.stubbed(representationRepository,
				SoftwareSystemFixtures.empty(),
				SoftwareSystemFixtures.accessesBased(),
				SoftwareSystemFixtures.repositoryBased());
	}

	@Test
	public void uploadsCarryingAuthorshipDataMakeTheRepositoryKindAvailable() {
		assertThat(similarityService.getAvailableKinds(REPOSITORY_BASED_NAME).availableNames())
				.contains(RepositoryKind.REPOSITORY);
	}

	@Test
	public void uploadsWithoutAuthorshipDataWithholdTheRepositoryKind() {
		assertThat(similarityService.getAvailableKinds(ACCESSES_BASED_NAME).availableNames())
				.contains(AccessesKind.ACCESSES)
				.doesNotContain(RepositoryKind.REPOSITORY);
	}

	@Test
	public void aSoftwareSystemWithNoUploadsIsOfferedNothing() {
		assertThat(similarityService.getAvailableKinds(EMPTY_NAME).availableNames()).isEmpty();
	}

	/**
	 * The same tool, two software systems, two different answers — the claim D-005 rests on. Asserted
	 * as a difference rather than as two absolute sets, so it keeps meaning as kinds are
	 * added.
	 */
	@Test
	public void twoSoftwareSystemsWithDifferentUploadsGetDifferentOffers() {
		assertThat(similarityService.getAvailableKinds(REPOSITORY_BASED_NAME).availableNames())
				.isNotEqualTo(similarityService.getAvailableKinds(ACCESSES_BASED_NAME).availableNames());
	}

	@Test
	public void representationGroupsAreDerivedFromTheSameUploads() {
		assertThat(softwareSystemService.getSoftwareSystemRepresentationGroups(ACCESSES_BASED_NAME))
				.containsExactly(ACCESSES_TYPE);
	}
}
