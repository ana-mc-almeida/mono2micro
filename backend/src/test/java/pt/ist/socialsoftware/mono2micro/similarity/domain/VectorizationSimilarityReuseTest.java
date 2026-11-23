package pt.ist.socialsoftware.mono2micro.similarity.domain;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pt.ist.socialsoftware.mono2micro.representation.analytical.domain.AccessesRepresentation;
import pt.ist.socialsoftware.mono2micro.representation.analytical.domain.CodeEmbeddingsRepresentation;
import pt.ist.socialsoftware.mono2micro.representation.domain.Representation;
import pt.ist.socialsoftware.mono2micro.representation.entityDictionary.EntityToIDRepresentation;
import pt.ist.socialsoftware.mono2micro.representation.entityDictionary.IDToEntityRepresentation;
import pt.ist.socialsoftware.mono2micro.similarity.domain.similarityMatrix.weights.FunctionalityVectorizationCallGraphWeights;
import pt.ist.socialsoftware.mono2micro.similarity.domain.similarityMatrix.weights.FunctionalityVectorizationSequenceOfAccessesWeights;
import pt.ist.socialsoftware.mono2micro.similarity.dto.SimilarityDto;
import pt.ist.socialsoftware.mono2micro.similarity.dto.SimilarityScipyAccessesAndRepositoryDto;
import pt.ist.socialsoftware.mono2micro.similarity.dto.SimilarityScipyClassVectorizationDto;
import pt.ist.socialsoftware.mono2micro.similarity.dto.SimilarityScipyEntityVectorizationDto;
import pt.ist.socialsoftware.mono2micro.similarity.dto.SimilarityScipyFunctionalityVectorizationByCallGraphDto;
import pt.ist.socialsoftware.mono2micro.similarity.dto.SimilarityScipyFunctionalityVectorizationBySequenceOfAccessesDto;
import pt.ist.socialsoftware.mono2micro.similarity.kind.ClassVectorizationKind;
import pt.ist.socialsoftware.mono2micro.similarity.kind.EntityVectorizationKind;
import pt.ist.socialsoftware.mono2micro.similarity.kind.FunctionalityVectorizationCallGraphKind;
import pt.ist.socialsoftware.mono2micro.similarity.kind.FunctionalityVectorizationSequenceOfAccessesKind;
import pt.ist.socialsoftware.mono2micro.softwareSystem.domain.SoftwareSystem;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A Similarity is unique on its kind, representations, profile, settings and weights, so an equal request reuses it (spec, Domain model).
 *
 * <p><b>Known defect, inherited from upstream: an equal vectorization request never reuses its Similarity.</b> The four vectorization
 * Similarities compare a request only with the Accesses family's DTO, so a request of their own type never matches and every repeat makes
 * a new Similarity, while an Accesses-family request with the same linkage does match. The call-graph kind also ignores its depth.
 *
 * <p>The tests whose display name starts with "today" pin the defect. The {@code @Disabled} twins assert the fix: the fix enables them and
 * deletes the "today" tests. See {@code ../implementation/docs/gap-analysis.md}, Gap 10, and {@code docs/decisions.md}, D-028.
 */
@DisplayName("Vectorization Similarities, as reused today")
class VectorizationSimilarityReuseTest {
	private static final String REUSE_DEFECT = "Known defect, inherited from upstream: an equal vectorization request never reuses its "
			+ "Similarity. See ../implementation/docs/gap-analysis.md, Gap 10";

	private final List<Representation> representations = representationsOfOneSystem();

	/** Pins the known defect; see the class javadoc. */
	@Test
	@DisplayName("today, an equal request of each vectorization kind is not reused")
	void todayAnEqualVectorizationRequestIsNotReused() {
		assertThat(new SimilarityScipyClassVectorization(ClassVectorizationKind.CLASS_VECTORIZATION, representations, "name",
				classVectorization("average")).equalsDto(classVectorization("average"))).as("Class Vectorization").isFalse();
		assertThat(new SimilarityScipyEntityVectorization(EntityVectorizationKind.ENTITY_VECTORIZATION, representations, "name",
				entityVectorization("average")).equalsDto(entityVectorization("average"))).as("Entity Vectorization").isFalse();
		assertThat(new SimilarityScipyFunctionalityVectorizationByCallGraph(FunctionalityVectorizationCallGraphKind.FUNCTIONALITY_VECTORIZATION_CALLGRAPH,
				representations, "name", callGraph("average", 2, 25)).equalsDto(callGraph("average", 2, 25))).as("Call Graph").isFalse();
		assertThat(new SimilarityScipyFunctionalityVectorizationBySequenceOfAccesses(
				FunctionalityVectorizationSequenceOfAccessesKind.FUNCTIONALITY_VECTORIZATION_SEQUENCE_ACCESSES, representations, "name",
				sequenceOfAccesses("average", 50)).equalsDto(sequenceOfAccesses("average", 50))).as("Sequence of Accesses").isFalse();
	}

	/** Pins the known defect; see the class javadoc. */
	@Test
	@DisplayName("today, a Class Vectorization Similarity matches an Accesses-family request with its linkage")
	void todayARequestOfAnotherTypeIsReused() {
		SimilarityScipyClassVectorization similarity = new SimilarityScipyClassVectorization(ClassVectorizationKind.CLASS_VECTORIZATION,
				representations, "name", classVectorization("average"));
		SimilarityScipyAccessesAndRepositoryDto accesses = sourced(new SimilarityScipyAccessesAndRepositoryDto(), ClassVectorizationKind.CLASS_VECTORIZATION);
		accesses.setLinkageType("average");

		assertThat(similarity.equalsDto(accesses)).isTrue();
	}

	@Disabled(REUSE_DEFECT)
	@Test
	void anEqualClassVectorizationRequestReusesTheSimilarity() {
		SimilarityScipyClassVectorization similarity = new SimilarityScipyClassVectorization(ClassVectorizationKind.CLASS_VECTORIZATION,
				representations, "name", classVectorization("average"));

		assertThat(similarity.equalsDto(classVectorization("average"))).isTrue();
		assertThat(similarity.equalsDto(classVectorization("single"))).as("another linkage").isFalse();
	}

	@Disabled(REUSE_DEFECT)
	@Test
	void anEqualEntityVectorizationRequestReusesTheSimilarity() {
		SimilarityScipyEntityVectorization similarity = new SimilarityScipyEntityVectorization(EntityVectorizationKind.ENTITY_VECTORIZATION,
				representations, "name", entityVectorization("average"));

		assertThat(similarity.equalsDto(entityVectorization("average"))).isTrue();
		assertThat(similarity.equalsDto(entityVectorization("single"))).as("another linkage").isFalse();
	}

	@Disabled(REUSE_DEFECT)
	@Test
	void anEqualCallGraphRequestReusesTheSimilarity() {
		SimilarityScipyFunctionalityVectorizationByCallGraph similarity = new SimilarityScipyFunctionalityVectorizationByCallGraph(
				FunctionalityVectorizationCallGraphKind.FUNCTIONALITY_VECTORIZATION_CALLGRAPH, representations, "name", callGraph("average", 2, 25));

		assertThat(similarity.equalsDto(callGraph("average", 2, 25))).isTrue();
		assertThat(similarity.equalsDto(callGraph("single", 2, 25))).as("another linkage").isFalse();
		assertThat(similarity.equalsDto(callGraph("average", 3, 25))).as("another depth").isFalse();
		assertThat(similarity.equalsDto(callGraph("average", 2, 10))).as("other weights").isFalse();
	}

	@Disabled(REUSE_DEFECT)
	@Test
	void anEqualSequenceOfAccessesRequestReusesTheSimilarity() {
		SimilarityScipyFunctionalityVectorizationBySequenceOfAccesses similarity = new SimilarityScipyFunctionalityVectorizationBySequenceOfAccesses(
				FunctionalityVectorizationSequenceOfAccessesKind.FUNCTIONALITY_VECTORIZATION_SEQUENCE_ACCESSES, representations, "name",
				sequenceOfAccesses("average", 50));

		assertThat(similarity.equalsDto(sequenceOfAccesses("average", 50))).isTrue();
		assertThat(similarity.equalsDto(sequenceOfAccesses("single", 50))).as("another linkage").isFalse();
		assertThat(similarity.equalsDto(sequenceOfAccesses("average", 30))).as("other weights").isFalse();
	}

	/** A request of another type is never the same Similarity, whatever its fields. */
	@Disabled(REUSE_DEFECT)
	@Test
	void aRequestOfAnotherTypeIsNotReused() {
		SimilarityScipyClassVectorization similarity = new SimilarityScipyClassVectorization(ClassVectorizationKind.CLASS_VECTORIZATION,
				representations, "name", classVectorization("average"));
		SimilarityScipyAccessesAndRepositoryDto accesses = sourced(new SimilarityScipyAccessesAndRepositoryDto(), ClassVectorizationKind.CLASS_VECTORIZATION);
		accesses.setLinkageType("average");

		assertThat(similarity.equalsDto(accesses)).isFalse();
	}

	private SimilarityScipyClassVectorizationDto classVectorization(String linkageType) {
		SimilarityScipyClassVectorizationDto dto = sourced(new SimilarityScipyClassVectorizationDto(), ClassVectorizationKind.CLASS_VECTORIZATION);
		dto.setLinkageType(linkageType);
		return dto;
	}

	private SimilarityScipyEntityVectorizationDto entityVectorization(String linkageType) {
		SimilarityScipyEntityVectorizationDto dto = sourced(new SimilarityScipyEntityVectorizationDto(), EntityVectorizationKind.ENTITY_VECTORIZATION);
		dto.setLinkageType(linkageType);
		return dto;
	}

	private SimilarityScipyFunctionalityVectorizationByCallGraphDto callGraph(String linkageType, int depth, float controllersWeight) {
		SimilarityScipyFunctionalityVectorizationByCallGraphDto dto = sourced(new SimilarityScipyFunctionalityVectorizationByCallGraphDto(),
				FunctionalityVectorizationCallGraphKind.FUNCTIONALITY_VECTORIZATION_CALLGRAPH);
		dto.setLinkageType(linkageType);
		dto.setDepth(depth);
		dto.setWeightsList(Collections.singletonList(
				new FunctionalityVectorizationCallGraphWeights(controllersWeight, 25, 25, 75 - controllersWeight)));
		return dto;
	}

	private SimilarityScipyFunctionalityVectorizationBySequenceOfAccessesDto sequenceOfAccesses(String linkageType, float readMetricWeight) {
		SimilarityScipyFunctionalityVectorizationBySequenceOfAccessesDto dto = sourced(new SimilarityScipyFunctionalityVectorizationBySequenceOfAccessesDto(),
				FunctionalityVectorizationSequenceOfAccessesKind.FUNCTIONALITY_VECTORIZATION_SEQUENCE_ACCESSES);
		dto.setLinkageType(linkageType);
		dto.setWeightsList(Collections.singletonList(new FunctionalityVectorizationSequenceOfAccessesWeights(readMetricWeight, 100 - readMetricWeight)));
		return dto;
	}

	/** The kind and representations the service fills in before it looks for an equal Similarity. */
	private <T extends SimilarityDto> T sourced(T dto, String kindName) {
		dto.setKindName(kindName);
		dto.setRepresentationIds(representations.stream().map(Representation::getId).collect(Collectors.toList()));
		return dto;
	}

	private static List<Representation> representationsOfOneSystem() {
		SoftwareSystem system = new SoftwareSystem("system");
		List<Representation> representations = Arrays.asList(new IDToEntityRepresentation(), new AccessesRepresentation(),
				new EntityToIDRepresentation(), new CodeEmbeddingsRepresentation());
		representations.forEach(representation -> representation.setSoftwareSystem(system));
		return representations;
	}
}
