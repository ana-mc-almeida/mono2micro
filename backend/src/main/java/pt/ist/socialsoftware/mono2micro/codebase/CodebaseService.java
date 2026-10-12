package pt.ist.socialsoftware.mono2micro.codebase;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pt.ist.socialsoftware.mono2micro.codebase.domain.Codebase;
import pt.ist.socialsoftware.mono2micro.codebase.repository.CodebaseRepository;
import pt.ist.socialsoftware.mono2micro.feature.AvailabilityCheck;
import pt.ist.socialsoftware.mono2micro.feature.AvailabilityContext;
import pt.ist.socialsoftware.mono2micro.representation.domain.Representation;
import pt.ist.socialsoftware.mono2micro.representation.dto.RepresentationDto;
import pt.ist.socialsoftware.mono2micro.representation.dto.RepresentationDtoFactory;
import pt.ist.socialsoftware.mono2micro.representation.service.RepresentationService;
import pt.ist.socialsoftware.mono2micro.similarity.kind.SimilarityKind;
import pt.ist.socialsoftware.mono2micro.strategy.domain.Strategy;
import pt.ist.socialsoftware.mono2micro.strategy.service.StrategyService;
import pt.ist.socialsoftware.mono2micro.utils.SelfRegistration;

import javax.management.openmbean.KeyAlreadyExistsException;
import java.util.List;
import java.util.Map;

@Service
public class CodebaseService {

	@Autowired
	RepresentationService representationService;

	@Autowired
	StrategyService strategyService;

	@Autowired
	CodebaseRepository codebaseRepository;

	public void createCodebase(String codebaseName) {
		if (codebaseRepository.existsByName(codebaseName))
			throw new KeyAlreadyExistsException();
		codebaseRepository.save(new Codebase(codebaseName));
	}

	public Map<String, List<String>> getRepresentationGroups() {
		return Representation.representationGroupToRepresentations;
	}

	public List<String> getCodebaseRepresentationGroups(String codebaseName) {
		Codebase codebase = codebaseRepository.findByName(codebaseName);
		return codebase.getRepresentationGroups();
	}

	public List<Codebase> getCodebases() {
		return codebaseRepository.getCodebases();
	}

	public Codebase getCodebase(String codebaseName) {
		return codebaseRepository.findByName(codebaseName);
	}

	public List<RepresentationDto> getRepresentationTypes(String codebaseName) {
		Codebase codebase = codebaseRepository.findByName(codebaseName);
		return RepresentationDtoFactory.getFactory().getRepresentationDtos(codebase.getRepresentations());
	}

	public List<Strategy> getCodebaseStrategies(String codebaseName) {
		Codebase codebase = codebaseRepository.findByName(codebaseName);
		return codebase.getStrategies();
	}

	public List<String> getAllowableCodebaseStrategyTypes(String codebaseName) {
		Codebase codebase = codebaseRepository.findByName(codebaseName);
		return AvailabilityCheck.derive(
				SelfRegistration.prototypes(SimilarityKind.class).values(),
				AvailabilityContext.of(codebase)).availableNames();
	}

	public void deleteCodebase(String codebaseName) {
		Codebase codebase = codebaseRepository.findByName(codebaseName);
		for (Strategy strategy : codebase.getStrategies())
			strategyService.deleteStrategy(strategy);
		for (Representation representation : codebase.getRepresentations())
			representationService.deleteRepresentation(representation.getName());
		codebaseRepository.deleteById(codebaseName);
	}

}
