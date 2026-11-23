package pt.ist.socialsoftware.mono2micro.recommendation;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import pt.ist.socialsoftware.mono2micro.feature.UnavailableFeatureException;
import pt.ist.socialsoftware.mono2micro.recommendation.dto.RecommendMatrixSciPyDto;
import pt.ist.socialsoftware.mono2micro.recommendation.repository.RecommendationRepository;
import pt.ist.socialsoftware.mono2micro.recommendation.service.RecommendationService;
import pt.ist.socialsoftware.mono2micro.similarity.domain.similarityMatrix.weights.AccessesWeights;
import pt.ist.socialsoftware.mono2micro.similarity.domain.similarityMatrix.weights.RepositoryWeights;
import pt.ist.socialsoftware.mono2micro.similarity.kind.SimilarityKind;
import pt.ist.socialsoftware.mono2micro.similarity.service.SimilarityService;

/** Which recommendation requests {@link RecommendationService#createRecommendation} refuses before computing anything. No Spring, no Mongo. */
class RecommendationServiceTest {
	private static final String SYSTEM = "system";

	@Mock
	private SimilarityService similarityService;

	@Mock
	private RecommendationRepository recommendationRepository;

	@InjectMocks
	private RecommendationService recommendationService;

	@BeforeEach
	void initMocks() {
		MockitoAnnotations.initMocks(this);
	}

	/** A kind fixes its criteria (D-013), so weights for another kind's criteria are refused. */
	@Test
	void weightsTheKindIsNotComputedWithAreRefused() {
		RecommendMatrixSciPyDto request = request("Accesses");
		request.setWeightsList(Collections.singletonList(new RepositoryWeights()));

		assertThatThrownBy(() -> recommendationService.createRecommendation(request))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("The Accesses kind is computed with the weights [ACCESSES_WEIGHTS], not [REPOSITORY_WEIGHTS].");
		verifyZeroInteractions(recommendationRepository);
	}

	@Test
	void aRequestWithoutWeightsIsRefusedForAKindThatHasThem() {
		assertThatThrownBy(() -> recommendationService.createRecommendation(request("Structure"))).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void anUnknownKindIsRefused() {
		assertThatThrownBy(() -> recommendationService.createRecommendation(request("Accesses and Structure")))
				.isInstanceOf(IllegalArgumentException.class);
	}

	/** The same check that withholds the kind refuses the request (D-003), before anything is saved. */
	@Test
	void aKindTheSoftwareSystemDoesNotOfferIsRefused() {
		RecommendMatrixSciPyDto request = request("Accesses");
		request.setWeightsList(Collections.singletonList(new AccessesWeights()));
		when(similarityService.resolveRepresentations(any(SimilarityKind.class), eq(SYSTEM)))
				.thenThrow(new UnavailableFeatureException("Accesses", Collections.singletonList("requires an Accesses representation")));

		assertThatThrownBy(() -> recommendationService.createRecommendation(request)).isInstanceOf(UnavailableFeatureException.class);
		verifyZeroInteractions(recommendationRepository);
	}

	private static RecommendMatrixSciPyDto request(String kindName) {
		RecommendMatrixSciPyDto request = new RecommendMatrixSciPyDto();
		request.setSoftwareSystemId(SYSTEM);
		request.setKindName(kindName);
		return request;
	}
}
