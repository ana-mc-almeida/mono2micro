package pt.ist.socialsoftware.mono2micro.similarity;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import pt.ist.socialsoftware.mono2micro.JsonSubtypeConfiguration;
import pt.ist.socialsoftware.mono2micro.similarity.domain.similarityMatrix.weights.Weights;
import pt.ist.socialsoftware.mono2micro.similarity.domain.similarityMatrix.weights.WeightsFactory;
import pt.ist.socialsoftware.mono2micro.similarity.dto.SimilarityDto;
import pt.ist.socialsoftware.mono2micro.similarity.dto.SimilarityScipyStructureDto;
import pt.ist.socialsoftware.mono2micro.utils.SelfRegistration;

/**
 * {@code Weights} and {@code SimilarityDto} find their subtypes by the self-registration scan instead of an {@code @JsonSubTypes} list
 * (D-019 point 8): a request body names its type, and the scan finds the class.
 */
class SelfRegisteredJsonTypesTest {

	private ObjectMapper mapper;

	@BeforeEach
	void mapperAsSpringBuildsIt() {
		mapper = new ObjectMapper().registerModule(new JsonSubtypeConfiguration().selfRegisteredSubtypes());
	}

	@Test
	void everyWeightsIsReadByItsName() throws Exception {
		Map<String, Weights> weights = SelfRegistration.prototypes(Weights.class);

		assertThat(weights).containsOnlyKeys("ACCESSES_WEIGHTS", "REPOSITORY_WEIGHTS", "STRUCTURE_WEIGHTS",
				"FUNCTIONALITY_VECTORIZATION_CALLGRAPH_WEIGHTS", "FUNCTIONALITY_VECTORIZATION_ACCESSES_WEIGHTS");
		for (String name : weights.keySet())
			assertThat(mapper.readValue("{\"type\": \"" + name + "\"}", Weights.class).getType()).isEqualTo(name);
	}

	@Test
	void theFactoryBuildsAFreshWeightsByName() {
		for (String name : SelfRegistration.prototypes(Weights.class).keySet())
			assertThat(WeightsFactory.getWeights(name)).isNotSameAs(SelfRegistration.prototypes(Weights.class).get(name))
					.extracting(Weights::getType).isEqualTo(name);
	}

	@Test
	void aSimilarityRequestIsReadByItsType() throws Exception {
		SimilarityDto dto = mapper.readValue("{\"type\": \"SIMILARITY_SCIPY_STRUCTURE\"}", SimilarityDto.class);

		assertThat(dto).isInstanceOf(SimilarityScipyStructureDto.class);
	}
}
