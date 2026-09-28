package pt.ist.socialsoftware.mono2micro.codebase;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import pt.ist.socialsoftware.mono2micro.codebase.repository.CodebaseRepository;
import pt.ist.socialsoftware.mono2micro.fixtures.CodebaseFixtures;
import pt.ist.socialsoftware.mono2micro.strategy.domain.Strategy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static pt.ist.socialsoftware.mono2micro.fixtures.CodebaseFixtures.ACCESSES_BASED_NAME;
import static pt.ist.socialsoftware.mono2micro.fixtures.CodebaseFixtures.EMPTY_NAME;
import static pt.ist.socialsoftware.mono2micro.fixtures.CodebaseFixtures.REPOSITORY_BASED_NAME;
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
@RunWith(MockitoJUnitRunner.class)
public class CodebaseServiceAvailabilityTest {

	@Mock
	private CodebaseRepository codebaseRepository;

	@InjectMocks
	private CodebaseService codebaseService;

	@Before
    public void givenTheFixtureCodebasesExist() {
        when(codebaseRepository.findByName(EMPTY_NAME))
                .thenReturn(CodebaseFixtures.empty());
        when(codebaseRepository.findByName(ACCESSES_BASED_NAME))
                .thenReturn(CodebaseFixtures.accessesBased());
        when(codebaseRepository.findByName(REPOSITORY_BASED_NAME))
                .thenReturn(CodebaseFixtures.repositoryBased());
    }

	@Test
	public void uploadsCarryingAuthorshipDataMakeTheRepositoryStrategyAvailable() {
		assertThat(codebaseService.getAllowableCodebaseStrategyTypes(REPOSITORY_BASED_NAME))
				.contains(Strategy.REPOSITORY_STRATEGY);
	}

	@Test
	public void uploadsWithoutAuthorshipDataWithholdTheRepositoryStrategy() {
		assertThat(codebaseService.getAllowableCodebaseStrategyTypes(ACCESSES_BASED_NAME))
				.contains(Strategy.ACCESSES_STRATEGY)
				.doesNotContain(Strategy.REPOSITORY_STRATEGY);
	}

	@Test
	public void aCodebaseWithNoUploadsIsOfferedNothing() {
		assertThat(codebaseService.getAllowableCodebaseStrategyTypes(EMPTY_NAME)).isEmpty();
	}

	/**
	 * The same tool, two codebases, two different answers — the claim D-005 rests on. Asserted
	 * as a difference rather than as two absolute sets, so it keeps meaning as strategies are
	 * added.
	 */
	@Test
	public void twoCodebasesWithDifferentUploadsGetDifferentOffers() {
		assertThat(codebaseService.getAllowableCodebaseStrategyTypes(REPOSITORY_BASED_NAME))
				.isNotEqualTo(codebaseService.getAllowableCodebaseStrategyTypes(ACCESSES_BASED_NAME));
	}

	@Test
	public void representationGroupsAreDerivedFromTheSameUploads() {
		assertThat(codebaseService.getCodebaseRepresentationGroups(ACCESSES_BASED_NAME))
				.containsExactly(ACCESSES_TYPE);
	}
}
