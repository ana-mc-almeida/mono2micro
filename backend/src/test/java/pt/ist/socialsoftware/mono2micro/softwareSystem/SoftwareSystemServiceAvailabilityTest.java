package pt.ist.socialsoftware.mono2micro.softwareSystem;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import pt.ist.socialsoftware.mono2micro.softwareSystem.domain.SoftwareSystem;
import pt.ist.socialsoftware.mono2micro.softwareSystem.repository.SoftwareSystemRepository;
import pt.ist.socialsoftware.mono2micro.fixtures.SoftwareSystemFixtures;
import pt.ist.socialsoftware.mono2micro.strategy.domain.Strategy;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
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
	private SoftwareSystemRepository softwareSystemRepository;

	@InjectMocks
	private SoftwareSystemService softwareSystemService;

	/** The fixtures by name, so a test can name the software system it asks about. */
	private final Map<String, SoftwareSystem> softwareSystems = new HashMap<>();

	@BeforeEach
	public void givenTheFixtureSoftwareSystemsExist() {
		MockitoAnnotations.initMocks(this);
		givenTheRepositoryFinds(SoftwareSystemFixtures.empty());
		givenTheRepositoryFinds(SoftwareSystemFixtures.accessesBased());
		givenTheRepositoryFinds(SoftwareSystemFixtures.repositoryBased());
	}

	@Test
	public void uploadsCarryingAuthorshipDataMakeTheRepositoryStrategyAvailable() {
		assertThat(softwareSystemService.getAllowableSoftwareSystemStrategyTypes(idOf(REPOSITORY_BASED_NAME)))
				.contains(Strategy.REPOSITORY_STRATEGY);
	}

	@Test
	public void uploadsWithoutAuthorshipDataWithholdTheRepositoryStrategy() {
		assertThat(softwareSystemService.getAllowableSoftwareSystemStrategyTypes(idOf(ACCESSES_BASED_NAME)))
				.contains(Strategy.ACCESSES_STRATEGY)
				.doesNotContain(Strategy.REPOSITORY_STRATEGY);
	}

	@Test
	public void aSoftwareSystemWithNoUploadsIsOfferedNothing() {
		assertThat(softwareSystemService.getAllowableSoftwareSystemStrategyTypes(idOf(EMPTY_NAME))).isEmpty();
	}

	/**
	 * The same tool, two software systems, two different answers — the claim D-005 rests on. Asserted
	 * as a difference rather than as two absolute sets, so it keeps meaning as strategies are
	 * added.
	 */
	@Test
	public void twoSoftwareSystemsWithDifferentUploadsGetDifferentOffers() {
		assertThat(softwareSystemService.getAllowableSoftwareSystemStrategyTypes(idOf(REPOSITORY_BASED_NAME)))
				.isNotEqualTo(softwareSystemService.getAllowableSoftwareSystemStrategyTypes(idOf(ACCESSES_BASED_NAME)));
	}

	@Test
	public void representationGroupsAreDerivedFromTheSameUploads() {
		assertThat(softwareSystemService.getSoftwareSystemRepresentationGroups(idOf(ACCESSES_BASED_NAME)))
				.containsExactly(ACCESSES_TYPE);
	}

	private void givenTheRepositoryFinds(SoftwareSystem softwareSystem) {
		softwareSystems.put(softwareSystem.getName(), softwareSystem);
		when(softwareSystemRepository.findById(softwareSystem.getId())).thenReturn(Optional.of(softwareSystem));
	}

	private String idOf(String softwareSystemName) {
		return softwareSystems.get(softwareSystemName).getId();
	}
}
