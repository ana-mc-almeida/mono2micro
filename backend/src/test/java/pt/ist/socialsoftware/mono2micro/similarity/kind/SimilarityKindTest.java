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
import pt.ist.socialsoftware.mono2micro.similarity.domain.similarityMatrix.weights.StructureWeights;
import pt.ist.socialsoftware.mono2micro.similarity.domain.similarityMatrix.weights.Weights;
import pt.ist.socialsoftware.mono2micro.similarity.dto.SimilarityDto;
import pt.ist.socialsoftware.mono2micro.utils.SelfRegistration;

/**
 * What each Similarity kind declares besides its requirements: the class that computes it, the information blocks of what is clustered from
 * it (D-019 point 5), and the weights it is computed with (D-013).
 */
class SimilarityKindTest {

	/** Each kind's information blocks, one per representation group it reads; Code Embeddings has no block of its own. */
	@ParameterizedTest(name = "{0}")
	@MethodSource("informationBlocksPerKind")
	void eachKindGetsTheInformationBlocksOfTheGroupsItReads(String kind, List<String> blocks) {
		assertThat(SimilarityKind.getByName(kind).informationBlocks().stream().map(RepresentationInformation::getType).collect(Collectors.toList()))
				.containsExactlyElementsOf(blocks);
	}

	static Stream<Arguments> informationBlocksPerKind() {
		return Stream.of(
				Arguments.of(AccessesKind.ACCESSES, Arrays.asList(ACCESSES_TYPE)),
				Arguments.of(RepositoryKind.REPOSITORY, Arrays.asList(ACCESSES_TYPE, REPOSITORY_TYPE)),
				// Accesses and Repository is its own kind (D-026)
				Arguments.of(AccessesAndRepositoryKind.ACCESSES_AND_REPOSITORY, Arrays.asList(ACCESSES_TYPE, REPOSITORY_TYPE)),
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
				Arguments.of(AccessesAndRepositoryKind.ACCESSES_AND_REPOSITORY, Arrays.asList("ACCESSES_WEIGHTS", "REPOSITORY_WEIGHTS")),
				Arguments.of(StructureKind.STRUCTURE, Arrays.asList("STRUCTURE_WEIGHTS")),
				Arguments.of(ClassVectorizationKind.CLASS_VECTORIZATION, Collections.emptyList()),
				Arguments.of(EntityVectorizationKind.ENTITY_VECTORIZATION, Collections.emptyList()),
				Arguments.of(FunctionalityVectorizationCallGraphKind.FUNCTIONALITY_VECTORIZATION_CALLGRAPH,
						Arrays.asList("FUNCTIONALITY_VECTORIZATION_CALLGRAPH_WEIGHTS")),
				Arguments.of(FunctionalityVectorizationSequenceOfAccessesKind.FUNCTIONALITY_VECTORIZATION_SEQUENCE_ACCESSES,
						Arrays.asList("FUNCTIONALITY_VECTORIZATION_ACCESSES_WEIGHTS")));
	}

	/** The default weights each kind's form starts from: the values upstream's form starts from (upstream {@code WeightsFactory.ts}). */
	@ParameterizedTest(name = "{0}")
	@MethodSource("defaultWeightsPerKind")
	void eachKindStartsFromItsDefaultWeights(String kind, List<String> defaultWeights) {
		assertThat(SimilarityKind.getByName(kind).defaultWeights().stream()
				.map(weights -> weights.getType() + Arrays.toString(weights.getWeights())).collect(Collectors.toList()))
				.containsExactlyElementsOf(defaultWeights);
	}

	static Stream<Arguments> defaultWeightsPerKind() {
		return Stream.of(
				Arguments.of(AccessesKind.ACCESSES, Arrays.asList("ACCESSES_WEIGHTS[25.0, 25.0, 25.0, 25.0]")),
				Arguments.of(RepositoryKind.REPOSITORY, Arrays.asList("REPOSITORY_WEIGHTS[50.0, 50.0]")),
				Arguments.of(AccessesAndRepositoryKind.ACCESSES_AND_REPOSITORY,
						Arrays.asList("ACCESSES_WEIGHTS[17.0, 17.0, 17.0, 17.0]", "REPOSITORY_WEIGHTS[16.0, 16.0]")),
				Arguments.of(StructureKind.STRUCTURE, Arrays.asList("STRUCTURE_WEIGHTS[33.0, 33.0, 34.0]")),
				Arguments.of(ClassVectorizationKind.CLASS_VECTORIZATION, Collections.emptyList()),
				Arguments.of(EntityVectorizationKind.ENTITY_VECTORIZATION, Collections.emptyList()),
				Arguments.of(FunctionalityVectorizationCallGraphKind.FUNCTIONALITY_VECTORIZATION_CALLGRAPH,
						Arrays.asList("FUNCTIONALITY_VECTORIZATION_CALLGRAPH_WEIGHTS[25.0, 25.0, 25.0, 25.0]")),
				Arguments.of(FunctionalityVectorizationSequenceOfAccessesKind.FUNCTIONALITY_VECTORIZATION_SEQUENCE_ACCESSES,
						Arrays.asList("FUNCTIONALITY_VECTORIZATION_ACCESSES_WEIGHTS[50.0, 50.0]")));
	}

	/** The settings each kind's form shows besides the linkage type, the profile and the weights: the ones upstream's form for the kind shows. */
	@ParameterizedTest(name = "{0}")
	@MethodSource("settingsPerKind")
	void eachKindTakesTheSettingsUpstreamsFormShows(String kind, List<SimilaritySetting> settings) {
		assertThat(SimilarityKind.getByName(kind).settings()).containsExactlyElementsOf(settings);
	}

	static Stream<Arguments> settingsPerKind() {
		List<SimilaritySetting> traces = Arrays.asList(SimilaritySetting.TRACES_MAX_LIMIT, SimilaritySetting.TRACE_TYPE);
		return Stream.of(
				Arguments.of(AccessesKind.ACCESSES, traces),
				Arguments.of(RepositoryKind.REPOSITORY, traces),
				Arguments.of(AccessesAndRepositoryKind.ACCESSES_AND_REPOSITORY, traces),
				Arguments.of(StructureKind.STRUCTURE, Arrays.asList(SimilaritySetting.TRACE_TYPE)),
				Arguments.of(ClassVectorizationKind.CLASS_VECTORIZATION, Collections.emptyList()),
				Arguments.of(EntityVectorizationKind.ENTITY_VECTORIZATION, Collections.emptyList()),
				Arguments.of(FunctionalityVectorizationCallGraphKind.FUNCTIONALITY_VECTORIZATION_CALLGRAPH, Arrays.asList(SimilaritySetting.MAX_DEPTH)),
				Arguments.of(FunctionalityVectorizationSequenceOfAccessesKind.FUNCTIONALITY_VECTORIZATION_SEQUENCE_ACCESSES, Collections.emptyList()));
	}

	/** Each call returns new objects, since a form edits the weights it is given. */
	@Test
	void theDefaultWeightsAreNotShared() {
		SimilarityKind kind = SimilarityKind.getByName(AccessesKind.ACCESSES);
		assertThat(kind.defaultWeights().get(0)).isNotSameAs(kind.defaultWeights().get(0));
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

	/**
	 * The criteria checkboxes allowed Accesses+Structure, which threw while its matrix was built, and Repository+Structure, in which
	 * Structure added nothing (D-013, O-12). A kind's criteria are its weights, so no kind may weigh Structure together with anything else.
	 */
	@Test
	void noKindCombinesStructureWithOtherCriteria() {
		for (SimilarityKind kind : SelfRegistration.prototypes(SimilarityKind.class).values())
			if (kind.weightsTypes().contains(StructureWeights.STRUCTURE_WEIGHTS))
				assertThat(kind.weightsTypes()).as(SelfRegistration.nameOf(kind)).containsExactly(StructureWeights.STRUCTURE_WEIGHTS);
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
