package pt.ist.socialsoftware.mono2micro.softwareSystem;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import pt.ist.socialsoftware.mono2micro.fixtures.SoftwareSystemFixtures;
import pt.ist.socialsoftware.mono2micro.representation.repository.RepresentationRepository;
import pt.ist.socialsoftware.mono2micro.strategy.domain.Strategy;

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

	@BeforeEach
	public void givenTheFixtureSoftwareSystemsExist() {
		MockitoAnnotations.initMocks(this);
		SoftwareSystemFixtures.stubbed(representationRepository,
				SoftwareSystemFixtures.empty(),
				SoftwareSystemFixtures.accessesBased(),
				SoftwareSystemFixtures.repositoryBased());
	}

	@Test
	public void uploadsCarryingAuthorshipDataMakeTheRepositoryStrategyAvailable() {
		assertThat(softwareSystemService.getAllowableSoftwareSystemStrategyTypes(REPOSITORY_BASED_NAME))
				.contains(Strategy.REPOSITORY_STRATEGY);
	}

	@Test
	public void uploadsWithoutAuthorshipDataWithholdTheRepositoryStrategy() {
		assertThat(softwareSystemService.getAllowableSoftwareSystemStrategyTypes(ACCESSES_BASED_NAME))
				.contains(Strategy.ACCESSES_STRATEGY)
				.doesNotContain(Strategy.REPOSITORY_STRATEGY);
	}

	@Test
	public void aSoftwareSystemWithNoUploadsIsOfferedNothing() {
		assertThat(softwareSystemService.getAllowableSoftwareSystemStrategyTypes(EMPTY_NAME)).isEmpty();
	}

	/**
	 * The same tool, two software systems, two different answers — the claim D-005 rests on. Asserted
	 * as a difference rather than as two absolute sets, so it keeps meaning as strategies are
	 * added.
	 */
	@Test
	public void twoSoftwareSystemsWithDifferentUploadsGetDifferentOffers() {
		assertThat(softwareSystemService.getAllowableSoftwareSystemStrategyTypes(REPOSITORY_BASED_NAME))
				.isNotEqualTo(softwareSystemService.getAllowableSoftwareSystemStrategyTypes(ACCESSES_BASED_NAME));
	}

	@Test
	public void representationGroupsAreDerivedFromTheSameUploads() {
		assertThat(softwareSystemService.getSoftwareSystemRepresentationGroups(ACCESSES_BASED_NAME))
				.containsExactly(ACCESSES_TYPE);
	}
}
