package pt.ist.socialsoftware.mono2micro.feature;

import java.util.Collections;
import java.util.Set;

/**
 * A feature of the tool that is not always available — one whose presence depends on what a
 * given codebase actually carries. A feature implementing this interface declares what it needs,
 * and generic availability code derives from those declarations which features a codebase can be
 * offered.
 *
 * <p>This inverts a mechanism the tool already had in the wrong place. {@code
 * Representation.representationGroupToRepresentations} ({@code Representation.java:30-35})
 * already stated that "Repository Based" needs {@code ID_TO_ENTITY}, {@code ACCESSES}, {@code
 * AUTHOR} and {@code COMMIT} — but as a static map on a base class, keyed by group, so the
 * requirement lived somewhere other than the feature that owns it. Every hierarchy that varies
 * grew its own version of the same idea: {@code Strategy} carries two parallel maps
 * ({@code Strategy.java:43-61}), {@code Clustering} carries a per-hierarchy method
 * ({@code Clustering.java:13}), and the frontend carries if/else chains. This interface replaces
 * all of them with one declaration made by the feature itself.
 *
 * <h2>Why there are two vocabularies</h2>
 *
 * <p>{@link #requiresRepresentations()} and {@link #requiresFeatures()} will often carry parallel
 * information, and to a reader coming to this file cold they will look redundant. <strong>They
 * are separate deliberately, and collapsing them destroys the reason this interface exists.</strong>
 *
 * <ul>
 * <li>{@link #requiresRepresentations()} names the <em>tool's</em> representation types — the
 * vocabulary its own subtypes already speak ({@code author}, {@code commit}). This is what
 * makes a declaration checkable against real input: a codebase either uploaded that file or
 * it did not.
 * <li>{@link #requiresFeatures()} names the <em>model's</em> features, in the model's own casing
 * ({@code Source code}, {@code Version analysis}). This is what makes the declaration an
 * instantiation of the unified feature model rather than ordinary plumbing.
 * </ul>
 *
 * <p>Keeping only the representation types would give tidier plumbing that demonstrates nothing
 * about the feature model. Keeping only the model names would give a model that cannot be checked
 * against a real codebase's uploads. The pair is the contribution; either half alone is not.
 * A future reader tempted to "simplify" one away should read this paragraph as the reason not to.
 *
 * <p>The two are also checked against each other from outside: the model's cross-tree constraints
 * are compared to what these methods declare, in both directions. Model → tool yields a coverage
 * metric, where a miss means only that the tool does not instantiate that part of the model yet.
 * Tool → model is an assertion, where a declared requirement with no corresponding model
 * constraint is a failure.
 *
 * <p>Both methods return flat sets, which express a conjunction: every named requirement must be
 * satisfied. Several of the model's constraints are disjunctive ("Structure requires Static
 * analysis <em>or</em> Model analysis <em>or</em> External"), and a flat set cannot say that. A
 * feature whose real requirement is a disjunction should declare only the alternative it actually
 * relies on, and the gap is the correspondence check's to report rather than this interface's to
 * paper over.
 *
 * <p>Every method has a default, so a hierarchy can start implementing this interface without
 * declaring anything and without changing behavior. That is deliberate: it lets the interface be
 * adopted one feature at a time. There is intentionally no method for a feature's own identity —
 * the hierarchies already carry {@code getType()}, and what the derivation record and the
 * correspondence check need to key on is settled when those are built, not guessed at here.
 */
public interface VariableFeature {

	/**
	 * The <em>tool's</em> representation types this feature needs, as its subtypes already name
	 * them ({@code author}, {@code commit}, {@code accesses}). A codebase whose uploads do not
	 * carry all of them cannot be offered this feature.
	 *
	 * <p>Defaults to empty: a feature that needs nothing declares nothing.
	 */
	default Set<String> requiresRepresentations() {
		return Collections.emptySet();
	}

	/**
	 * The <em>model's</em> features this feature requires, in the model's own casing ({@code
	 * Source code}, {@code Version analysis}) — sentence case for leaf features, Title Case for
	 * groups. These names must match the feature model's own, because the correspondence check
	 * matches them against the {@code <var>} names in its cross-tree constraints.
	 *
	 * <p>Defaults to empty: a feature that has nothing to say about the model says nothing.
	 */
	default Set<String> requiresFeatures() {
		return Collections.emptySet();
	}

	/**
	 * Features this one is genuinely incompatible with, in the model's vocabulary.
	 *
	 * <p><strong>This is expected to return empty for every feature.</strong> All 21 of the
	 * model's cross-tree constraints are implications — {@code requires}, never {@code excludes} —
	 * so on the model as it stands there is nothing for any feature to declare here.
	 *
	 * <p>It exists anyway so that a genuine incompatibility has somewhere to be declared if one is
	 * ever found. <strong>A non-empty return is a finding about the model, not a bug in the
	 * code.</strong> It means the tool has an incompatibility the model does not express, which is
	 * evidence the model is incomplete. The correspondence check reports such a return as a
	 * discovery and names it as one, precisely so that a future session does not read it as a
	 * defect and delete the declaration.
	 */
	default Set<String> excludes() {
		return Collections.emptySet();
	}
}
