package pt.ist.socialsoftware.mono2micro.refactoring.servicesRefactoring;

import org.junit.jupiter.api.Test;
import pt.ist.socialsoftware.mono2micro.feature.FeatureFamilies;
import pt.ist.socialsoftware.mono2micro.feature.FeatureId;
import pt.ist.socialsoftware.mono2micro.refactoring.servicesRefactoring.algorithms.SciPyOperation;

import static org.assertj.core.api.Assertions.assertThat;

/** The algorithm operations are a family (D-025), so the boot check finds every one claimed. No algorithm operation is ever present. */
public class AlgorithmOperationFamilyTest {

	@Test
	public void everyAlgorithmOperationIsAFeatureOfTheFamily() {
		assertThat(FeatureFamilies.scanned().familyWithMemberType(AlgorithmOperation.class)).hasValueSatisfying(family -> assertThat(family.features())
				.extracting(FeatureId::getName)
				.containsExactly(SciPyOperation.SCIPY));
	}
}
