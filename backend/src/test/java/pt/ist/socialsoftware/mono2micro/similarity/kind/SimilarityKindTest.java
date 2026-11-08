package pt.ist.socialsoftware.mono2micro.similarity.kind;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static pt.ist.socialsoftware.mono2micro.representation.domain.Representation.ACCESSES_TYPE;
import static pt.ist.socialsoftware.mono2micro.representation.domain.Representation.REPOSITORY_TYPE;
import static pt.ist.socialsoftware.mono2micro.representation.domain.Representation.STRUCTURE_TYPE;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import pt.ist.socialsoftware.mono2micro.decomposition.domain.representationInformation.RepresentationInformation;
import pt.ist.socialsoftware.mono2micro.similarity.domain.similarityMatrix.weights.Weights;
import pt.ist.socialsoftware.mono2micro.similarity.dto.SimilarityDto;
import pt.ist.socialsoftware.mono2micro.utils.SelfRegistration;

/**
 * What each Similarity kind declares besides its requirements: the class that computes it, the information blocks of what is clustered from
 * it (D-019 point 5), and the weights it is computed with (D-013).
 */
class SimilarityKindTest {

	/** The same answers the strategy map {@code strategiesToRepresentationTypes} gives, Code Embeddings having no block of its own. */
	@ParameterizedTest(name = "{0}")
	@MethodSource("informationBlocksPerKind")
	void eachKindGetsTheInformationBlocksItsStrategyGot(String kind, List<String> blocks) {
		assertThat(SimilarityKind.getByName(kind).informationBlocks().stream().map(RepresentationInformation::getType).collect(Collectors.toList()))
				.containsExactlyElementsOf(blocks);
	}

	static Stream<Arguments> informationBlocksPerKind() {
		return Stream.of(
				Arguments.of(AccessesKind.ACCESSES, Arrays.asList(ACCESSES_TYPE)),
				Arguments.of(RepositoryKind.REPOSITORY, Arrays.asList(ACCESSES_TYPE, REPOSITORY_TYPE)),
				Arguments.of(StructureKind.STRUCTURE, Arrays.asList(ACCESSES_TYPE, STRUCTURE_TYPE)),
				Arguments.of(ClassVectorizationKind.CLASS_VECTORIZATION, Arrays.asList(ACCESSES_TYPE)),
				Arguments.of(EntityVectorizationKind.ENTITY_VECTORIZATION, Arrays.asList(ACCESSES_TYPE)),
				Arguments.of(FunctionalityVectorizationCallGraphKind.FUNCTIONALITY_VECTORIZATION_CALLGRAPH, Arrays.asList(ACCESSES_TYPE)),
				Arguments.of(FunctionalityVectorizationSequenceOfAccessesKind.FUNCTIONALITY_VECTORIZATION_SEQUENCE_ACCESSES, Arrays.asList(ACCESSES_TYPE)));
	}

	/** The weights each kind's form sends today ({@code WeightsFactory.ts}); the two kinds without a weights type send none. */
	@ParameterizedTest(name = "{0}")
	@MethodSource("weightsPerKind")
	void eachKindIsComputedWithTheWeightsItsFormSends(String kind, List<String> weights) {
		assertThat(SimilarityKind.getByName(kind).weightsTypes()).containsExactlyElementsOf(weights);
	}

	static Stream<Arguments> weightsPerKind() {
		return Stream.of(
				Arguments.of(AccessesKind.ACCESSES, Arrays.asList("ACCESSES_WEIGHTS")),
				Arguments.of(RepositoryKind.REPOSITORY, Arrays.asList("REPOSITORY_WEIGHTS")),
				Arguments.of(StructureKind.STRUCTURE, Arrays.asList("STRUCTURE_WEIGHTS")),
				Arguments.of(ClassVectorizationKind.CLASS_VECTORIZATION, Collections.emptyList()),
				Arguments.of(EntityVectorizationKind.ENTITY_VECTORIZATION, Collections.emptyList()),
				Arguments.of(FunctionalityVectorizationCallGraphKind.FUNCTIONALITY_VECTORIZATION_CALLGRAPH,
						Arrays.asList("FUNCTIONALITY_VECTORIZATION_CALLGRAPH_WEIGHTS")),
				Arguments.of(FunctionalityVectorizationSequenceOfAccessesKind.FUNCTIONALITY_VECTORIZATION_SEQUENCE_ACCESSES,
						Arrays.asList("FUNCTIONALITY_VECTORIZATION_ACCESSES_WEIGHTS")));
	}

	@Test
	void everyKindNamesAWeightsTypeThatExists() {
		for (SimilarityKind kind : SelfRegistration.prototypes(SimilarityKind.class).values())
			assertThat(SelfRegistration.prototypes(Weights.class)).containsKeys(kind.weightsTypes().toArray(new String[0]));
	}

	@Test
	void everyKindIsComputedByASimilarityTypeARequestCanName() {
		for (SimilarityKind kind : SelfRegistration.prototypes(SimilarityKind.class).values())
			assertThat(SelfRegistration.prototypes(SimilarityDto.class)).containsKey(kind.similarityType());
	}

	@Test
	void aKindIsFoundByItsName() {
		assertThat(SimilarityKind.getByName("Structure")).isInstanceOf(StructureKind.class);
	}

	@Test
	void anUnknownKindIsRefused() {
		assertThatThrownBy(() -> SimilarityKind.getByName("Accesses and Structure"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("No Similarity kind is named \"Accesses and Structure\"");
	}
}
