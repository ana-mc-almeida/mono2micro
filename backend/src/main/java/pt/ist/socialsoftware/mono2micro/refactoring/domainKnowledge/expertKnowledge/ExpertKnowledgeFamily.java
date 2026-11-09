package pt.ist.socialsoftware.mono2micro.refactoring.domainKnowledge.expertKnowledge;

import pt.ist.socialsoftware.mono2micro.feature.FeatureFamily;

/**
 * The model's Expert knowledge (D-017). Its features are the kinds of {@link ExpertKnowledge}; today only {@link Profile}, so the boot check
 * finds it claimed (D-025 point 7). None is ever present in a software system or as an input: each is offered where its requirements hold,
 * never required.
 */
public class ExpertKnowledgeFamily extends FeatureFamily<ExpertKnowledge> {

	public ExpertKnowledgeFamily() {
		super("expert knowledge", ExpertKnowledge.class);
	}
}
