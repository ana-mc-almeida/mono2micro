package pt.ist.socialsoftware.mono2micro.recommendation.domain;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import pt.ist.socialsoftware.mono2micro.representation.analytical.domain.AccessesRepresentation;
import pt.ist.socialsoftware.mono2micro.representation.entityDictionary.IDToEntityRepresentation;
import pt.ist.socialsoftware.mono2micro.similarity.domain.similarityMatrix.weights.AccessesWeights;
import pt.ist.socialsoftware.mono2micro.similarity.domain.similarityMatrix.weights.Weights;
import pt.ist.socialsoftware.mono2micro.similarity.dto.SimilarityDto;
import pt.ist.socialsoftware.mono2micro.similarity.dto.SimilarityScipyAccessesAndRepositoryDto;
import pt.ist.socialsoftware.mono2micro.similarity.kind.AccessesKind;
import pt.ist.socialsoftware.mono2micro.similarity.kind.SimilarityKind;
import pt.ist.socialsoftware.mono2micro.softwareSystem.domain.SoftwareSystem;
import pt.ist.socialsoftware.mono2micro.utils.Constants;
import pt.ist.socialsoftware.mono2micro.utils.SelfRegistration;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A recommendation builds the request for each candidate's Similarity itself, as the frontend builds a Similarity request (D-029 item 10): of the
 * type that computes its kind, with its kind, representations and settings and the candidate's weights. The run builds one for every kind, the
 * vectorization kinds included; only saving their candidates is refused (D-027).
 */
class CandidateRequestTest {

	@ParameterizedTest(name = "{0}: a recommendation describes its candidates with a request of the type that computes the kind")
	@MethodSource("kindNames")
	void theRequestIsOfTheTypeThatComputesTheKind(String kindName) {
		RecommendMatrixSciPy recommendation = recommendation(kindName);

		SimilarityDto request = recommendation.candidateRequest(Collections.emptyList());

		assertThat(request.getType()).isEqualTo(SimilarityKind.getByName(kindName).similarityType());
		assertThat(request.getKindName()).isEqualTo(kindName);
		assertThat(request.getRepresentationIds()).isEqualTo(recommendation.getRepresentationIds());
		assertThat(request.getSoftwareSystemId()).isEqualTo(recommendation.getSoftwareSystem().getId());
		assertThat(request.getProfileId()).isNull();
	}

	@ParameterizedTest(name = "an Accesses recommendation's request carries its settings and the candidate's weights")
	@MethodSource("accesses")
	void theRequestCarriesTheSettingsAndTheCandidatesWeights(String kindName) {
		RecommendMatrixSciPy recommendation = recommendation(kindName);
		recommendation.setLinkageType("average");
		recommendation.setTraceType(Constants.TraceType.LONGEST);
		recommendation.setTracesMaxLimit(7);
		List<Weights> weights = Collections.singletonList(new AccessesWeights(10, 20, 30, 40));

		SimilarityScipyAccessesAndRepositoryDto request = (SimilarityScipyAccessesAndRepositoryDto) recommendation.candidateRequest(weights);

		assertThat(request.getLinkageType()).isEqualTo("average");
		assertThat(request.getTraceType()).isEqualTo(Constants.TraceType.LONGEST);
		assertThat(request.getTracesMaxLimit()).isEqualTo(7);
		assertThat(request.getWeightsList()).isEqualTo(weights);
	}

	static Stream<String> kindNames() {
		return SelfRegistration.prototypes(SimilarityKind.class).keySet().stream();
	}

	static Stream<String> accesses() {
		return Stream.of(AccessesKind.ACCESSES);
	}

	private static RecommendMatrixSciPy recommendation(String kindName) {
		RecommendMatrixSciPy recommendation = new RecommendMatrixSciPy();
		recommendation.setKindName(kindName);
		recommendation.setSoftwareSystem(new SoftwareSystem("system"));
		recommendation.setRepresentations(Arrays.asList(new IDToEntityRepresentation(), new AccessesRepresentation()));
		return recommendation;
	}
}
