package pt.ist.socialsoftware.mono2micro.similarity.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * A recommendation reads the ID-to-entity map once and hands it to every candidate's Similarity, so a run of about 2,000 candidates does not read
 * it 2,000 times. No Spring context runs here, so a Similarity that tried to read the map from storage would fail.
 */
class IDToEntityNameTest {

	@Test
	@DisplayName("a Similarity handed the ID-to-entity map returns it without reading storage")
	void aHandedMapIsNotReadAgain() throws Exception {
		Map<Short, String> idToEntity = Collections.singletonMap((short) 1, "Order");
		SimilarityScipy similarity = new SimilarityScipyAccessesAndRepository();

		similarity.useIDToEntityName(idToEntity);

		assertThat(similarity.getIDToEntityName()).isEqualTo(idToEntity);
	}

	@Test
	@DisplayName("a candidate cannot change the map the other candidates share")
	void theHandedMapIsReadOnly() throws Exception {
		SimilarityScipy similarity = new SimilarityScipyAccessesAndRepository();
		similarity.useIDToEntityName(Collections.singletonMap((short) 1, "Order"));

		assertThatThrownBy(() -> similarity.getIDToEntityName().put((short) 2, "Customer")).isInstanceOf(UnsupportedOperationException.class);
	}
}
