package pt.ist.socialsoftware.mono2micro.similarity.kind;

import pt.ist.socialsoftware.mono2micro.decomposition.domain.representationInformation.RepresentationInformation;
import pt.ist.socialsoftware.mono2micro.feature.VariableFeature;
import pt.ist.socialsoftware.mono2micro.utils.SelfRegistration;

import java.util.List;

/**
 * One of the seven Similarity kinds the tool offers (today's strategy types), as a feature that
 * declares what a software system must have collected before the kind can be offered.
 *
 * <p>A kind is not a {@code SimilarityScipy*} class. Those six classes compute the matrices and
 * stay fused (Gap 5, D-009), and one of them, {@code SimilarityScipyAccessesAndRepository},
 * computes two kinds. So each kind is its own small class, named by its {@code @JsonTypeName} and
 * found by the self-registration scan (D-021), and it names the class that computes it with {@link #similarityType()}. The kinds are one
 * {@link SimilarityKindFamily}, which shares the requirements every kind has.
 */
public abstract class SimilarityKind implements VariableFeature {

	/** The type of the {@code SimilarityScipy*} class that computes this kind, which a request for the kind must use. */
	public abstract String similarityType();

	/**
	 * The information blocks a decomposition gets when it is clustered from a Similarity of this kind, built afresh on each call.
	 *
	 * <p>Provisional (D-019 point 5): a plain method, not a requirement, replacing the map it came from. Every kind keeps Accesses.
	 */
	public abstract List<RepresentationInformation> informationBlocks();

	/**
	 * The types of the {@code Weights} a Similarity of this kind is computed with, in order: the kind fixes its criteria (D-013), so a
	 * request for it must send exactly these. Empty for a kind computed without weights.
	 */
	public abstract List<String> weightsTypes();

	/**
	 * The kind named {@code name}.
	 *
	 * @throws IllegalArgumentException
	 * if no kind has that name
	 */
	public static SimilarityKind getByName(String name) {
		SimilarityKind kind = SelfRegistration.prototypes(SimilarityKind.class).get(name);
		if (kind == null)
			throw new IllegalArgumentException("No Similarity kind is named \"" + name + "\"");
		return kind;
	}
}
