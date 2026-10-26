package pt.ist.socialsoftware.mono2micro.feature;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * A feature of the tool that is not always available: whether the tool can offer it depends on
 * what a software system has collected, or on the input it would act on. The feature declares what it
 * needs as a list of named {@link Requirement}s, and one shared {@link AvailabilityCheck} derives
 * from those declarations which features of a family are offered, and why the others are not.
 *
 * <p>This replaces declarations the tool kept away from the features they describe: a static map
 * per strategy type on {@code Strategy}, a per-algorithm list on the clustering algorithm, and
 * if/else chains in the frontend. Each declaration now lives in the class that owns it, and is
 * read at runtime by the check (D-019).
 *
 * <h2>Requirements are in tool terms</h2>
 *
 * <p>A requirement is named in the tool's own vocabulary ("has an Accesses representation",
 * "Similarity kind in {…}"), because that is what can be checked against a real software system. The
 * model feature each requirement corresponds to is recorded outside the code, in the thesis
 * repository, and the correspondence test reads it there in both directions: model to tool gives
 * a coverage figure, and tool to model is an assertion (D-003, D-019 point 2).
 *
 * <p>The list is a conjunction: every requirement must be met. A disjunction is one requirement,
 * {@link AnyOf}, so the check never has to know about it.
 *
 * <h2>Identity</h2>
 *
 * <p>A feature's name is its {@code @JsonTypeName}, which is also its JSON type, registry key and
 * frontend lookup key. There is deliberately no separate name method, so nothing can drift. The
 * self-registration scan keeps one prototype of each feature class, built with its no-arg
 * constructor, and asks it for its requirements.
 *
 * <p>Every method has a default, so a family can adopt this interface one member at a time, and a
 * feature with nothing to declare is always available.
 */
public interface VariableFeature {

	/**
	 * What the tool must have before it can offer this feature. Defaults to empty: a feature that
	 * needs nothing is always available.
	 */
	default List<Requirement> requirements() {
		return Collections.emptyList();
	}

	/**
	 * Features this one is genuinely incompatible with.
	 *
	 * <p><strong>This is expected to return empty for every feature.</strong> All 21 of the
	 * model's cross-tree constraints are implications ({@code requires}, never {@code excludes}),
	 * so on the model as it stands there is nothing to declare here.
	 *
	 * <p>It exists anyway so that a genuine incompatibility has somewhere to be declared if one is
	 * ever found. <strong>A non-empty return is a finding about the model, not a bug in the
	 * code.</strong> It means the tool has an incompatibility the model does not express, which is
	 * evidence the model is incomplete. A future session should report it, not delete it.
	 *
	 * <p>The {@link AvailabilityCheck} evaluates it: the feature is withheld while a feature it excludes is present (D-025 point 4).
	 */
	default Set<FeatureId> excludes() {
		return Collections.emptySet();
	}
}
