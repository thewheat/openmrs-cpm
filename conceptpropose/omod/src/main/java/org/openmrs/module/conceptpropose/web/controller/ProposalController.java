package org.openmrs.module.conceptpropose.web.controller;

import org.directwebremoting.util.Logger;
import org.joda.time.DateTime;
import org.openmrs.Concept;
import org.openmrs.ConceptSearchResult;
import org.openmrs.PersonName;
import org.openmrs.User;
import org.openmrs.api.ConceptService;
import org.openmrs.api.context.Context;
import org.openmrs.module.conceptpropose.web.dto.CommentDto;
import org.openmrs.module.conceptpropose.web.service.ConceptProposeMapperService;
import org.openmrs.module.conceptpropose.PackageStatus;
import org.openmrs.module.conceptpropose.ProposedConcept;
import org.openmrs.module.conceptpropose.ProposedConceptPackage;
import org.openmrs.module.conceptpropose.api.ProposedConceptService;
import org.openmrs.module.conceptpropose.web.common.CpmConstants;
import org.openmrs.module.conceptpropose.web.dto.ProposedConceptDto;
import org.openmrs.module.conceptpropose.web.dto.ProposedConceptPackageDto;
import org.openmrs.module.conceptpropose.web.dto.concept.ConceptDto;
import org.openmrs.module.conceptpropose.web.dto.concept.SearchConceptResultDto;
import org.openmrs.module.conceptpropose.web.dto.factory.DescriptionDtoFactory;
import org.openmrs.module.conceptpropose.web.dto.factory.NameDtoFactory;
import org.openmrs.util.OpenmrsConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;

import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;


@Controller
public class ProposalController {
	private final SubmitProposal submitProposal;

	private final UpdateProposedConceptPackage updateProposedConceptPackage;

    private final DescriptionDtoFactory descriptionDtoFactory;

    private final NameDtoFactory nameDtoFactory;

	private final ConceptProposeMapperService mapperService;

	protected final Logger log = Logger.getLogger(getClass());

    public static class ProposalSubmissionException extends RuntimeException {
        private final HttpStatus httpStatus;
        public ProposalSubmissionException(final String message, final HttpStatus status) {
            super(message);
            this.httpStatus = status;
        }

        public HttpStatus getHttpStatus() {
            return httpStatus;
        }
    }


    @Autowired
    public ProposalController (final SubmitProposal submitProposal,
                               final UpdateProposedConceptPackage updateProposedConceptPackage,
                               final DescriptionDtoFactory descriptionDtoFactory,
                               final NameDtoFactory nameDtoFactory,
                               final ConceptProposeMapperService mapperService) {
        this.submitProposal = submitProposal;
        this.updateProposedConceptPackage = updateProposedConceptPackage;
        this.descriptionDtoFactory = descriptionDtoFactory;
        this.nameDtoFactory = nameDtoFactory;
	    this.mapperService = mapperService;
    }

	//
	// Pages
	//

	@RequestMapping(value = "module/conceptpropose/proposals.list", method = RequestMethod.GET)
	public String listProposals() {
		return CpmConstants.LIST_PROPOSAL_URL;
	}

	//
	// Service endpoints
	//


    @RequestMapping(value = "/conceptpropose/concepts", method = RequestMethod.GET)
    public @ResponseBody SearchConceptResultDto findConcepts(@RequestParam final String query,
                                                             @RequestParam final String requestNum) {
        final ArrayList<ConceptDto> results = new ArrayList<ConceptDto>();
        final ConceptService conceptService = Context.getConceptService();

        if (query.equals("")) {
            final List<Concept> allConcepts = conceptService.getAllConcepts("name", true, false);
            for (final Concept concept : allConcepts) {
                ConceptDto conceptDto = createConceptDto(concept);
                    results.add(conceptDto);
            }
        } else {
            final List<ConceptSearchResult> concepts = conceptService.getConcepts(query, Context.getLocale(), false);
            for (final ConceptSearchResult conceptSearchResult : concepts) {
                ConceptDto conceptDto = createConceptDto(conceptSearchResult.getConcept());
                results.add(conceptDto);

                }

        }
        SearchConceptResultDto resultDto = new SearchConceptResultDto();
        resultDto.setConcepts(results);
        resultDto.setRequestNum(requestNum);
        return resultDto;
    }


	@RequestMapping(value = "/conceptpropose/proposals", method = RequestMethod.GET)
	public @ResponseBody ArrayList<ProposedConceptPackageDto> getProposals() {

		final List<ProposedConceptPackage> allConceptProposalPackages = Context.getService(ProposedConceptService.class).getAllProposedConceptPackages();
		final ArrayList<ProposedConceptPackageDto> response = new ArrayList<ProposedConceptPackageDto>();

		for (final ProposedConceptPackage conceptProposalPackage : allConceptProposalPackages) {

			final ProposedConceptPackageDto conceptProposalPackageDto = createProposedConceptPackageDto(conceptProposalPackage);
			response.add(conceptProposalPackageDto);
		}

		return response;
	}

	@RequestMapping(value = "/conceptpropose/proposals/{proposalId}", method = RequestMethod.GET)
	public @ResponseBody ProposedConceptPackageDto getProposalById(@PathVariable final String proposalId) {
		final ProposedConceptPackage proposedConceptPackage = Context.getService(ProposedConceptService.class).getProposedConceptPackageById(Integer.valueOf(proposalId));
//		return createProposedConceptPackageDto(proposedConceptPackageById);
		return mapperService.convertProposedConceptPackageToProposedConceptDto(proposedConceptPackage);
	}

	@RequestMapping(value = "/conceptpropose/proposals/empty", method = RequestMethod.GET)
	public @ResponseBody ProposedConceptPackageDto getEmptyProposal() {
		User user = Context.getAuthenticatedUser();
		ProposedConceptPackageDto proposal = new ProposedConceptPackageDto();
		proposal.setStatus(PackageStatus.DRAFT);
		proposal.setConcepts(new ArrayList<ProposedConceptDto>());
		proposal.setName(getDisplayName(user));
		proposal.setEmail(getNotificationEmail(user));

		if (Strings.isNullOrEmpty(proposal.getEmail())) {
			ProposedConceptPackage mostRecentProposal = Context.getService(ProposedConceptService.class).getMostRecentConceptProposalPackage();
			if (mostRecentProposal != null) {
				proposal.setEmail(mostRecentProposal.getEmail());
			}
		}
		
		return proposal;
	}
	
	private String getDisplayName(User user) {
		PersonName name = user.getPersonName();
		ArrayList<String> components = Lists.newArrayList(name.getGivenName(), name.getMiddleName(), name.getFamilyName());
		String displayName = "";
		for (String component : components) {
			if (!Strings.isNullOrEmpty(component)) {
				displayName += String.format("%s ", component);
			}
		}
		return displayName.trim();
	}
	
	private String getNotificationEmail(User user) {
		Map<String, String> userProperties = user.getUserProperties();
		return userProperties.get(OpenmrsConstants.USER_PROPERTY_NOTIFICATION_ADDRESS);
	}
	
	@RequestMapping(value = "/conceptpropose/proposals/", method = RequestMethod.POST)
	public @ResponseBody ProposedConceptPackageDto addProposal(@RequestBody final ProposedConceptPackageDto newPackage) {
		return addProposal(String.valueOf(newPackage.getId()), newPackage);
	}

	@RequestMapping(value = "/conceptpropose/proposals/{proposalId}", method = RequestMethod.POST)
	public @ResponseBody ProposedConceptPackageDto addProposal(@PathVariable final String proposalId,
	                                                           @RequestBody final ProposedConceptPackageDto newPackage) {
		// TODO: some server side validation here... not null fields, valid email?

		final ProposedConceptPackage conceptPackage = new ProposedConceptPackage();
		conceptPackage.setName(newPackage.getName());
		conceptPackage.setEmail(newPackage.getEmail());
		conceptPackage.setDescription(newPackage.getDescription());

		updateProposedConceptPackage.updateProposedConcepts(conceptPackage, newPackage);

		Context.getService(ProposedConceptService.class).saveProposedConceptPackage(conceptPackage);

		// Return the DTO with the new ID for the benefit of the client
		newPackage.setId(conceptPackage.getId());

		if (newPackage.getStatus() == PackageStatus.TBS) {
			submitProposal.submitProposedConcept(conceptPackage);
		}

		return newPackage;
	}
	//
	// Proposer-Reviewer webservice endpoints
	//
	@RequestMapping(value = "/conceptpropose/concept/{proposalUuid}/{conceptUuid}/comment", method = RequestMethod.PUT)
	public @ResponseBody ProposedConceptPackageDto setComment(@PathVariable String proposalUuid, @PathVariable String conceptUuid, @RequestBody final CommentDto incomingComment) {
		// TODO - throw exception on error?

		final ProposedConceptService proposedConceptService = Context.getService(ProposedConceptService.class);
		final ProposedConceptPackage conceptPackage = proposedConceptService.getProposedConceptPackageByUuid(proposalUuid);
		final ConceptService conceptService = Context.getConceptService();
		final Concept sourceConcept = conceptService.getConceptByUuid(conceptUuid);

		if(conceptPackage != null) {
			for (ProposedConcept proposedConcept : conceptPackage.getProposedConcepts()) {
				// if (concept.getUuid().equals(sourceConceptUUID)) {
				if(proposedConcept.getConcept().getId().equals(sourceConcept.getId())) {
					proposedConcept.setComment(incomingComment.getComment());
					incomingComment.setProposedConceptPackageUuid(proposalUuid);
					incomingComment.setProposedConceptUuid(conceptUuid);
					if (proposedConceptService.saveProposedConceptPackage(conceptPackage) != null){
						// submitProposal.addComment(incomingComment);
						return createProposedConceptPackageDto(conceptPackage);
					}
					else {
						log.error("Error saving comment");
					}
				}
			}
		}
		else{
			log.error((conceptPackage != null ? "Concept not found (searching for UUID: " + conceptUuid + ")" : "Proposal Package not found (searching for UUID: " + proposalUuid + ")"));
		}
		return null;
	}

	// dictionary manager pushes the latest conversation to the proposer
	@RequestMapping(value = "/conceptpropose/concept/{proposalUuid}/{conceptUuid}/comment", method = RequestMethod.POST)
	public @ResponseBody ProposedConceptPackageDto addComment(@PathVariable String proposalUuid, @PathVariable String conceptUuid, @RequestBody final CommentDto incomingComment) {
		// TODO - throw exception on error?

		final ProposedConceptService proposedConceptService = Context.getService(ProposedConceptService.class);
		final ProposedConceptPackage conceptPackage = proposedConceptService.getProposedConceptPackageByUuid(proposalUuid);
		final ConceptService conceptService = Context.getConceptService();
		final Concept sourceConcept = conceptService.getConceptByUuid(conceptUuid);

		if(conceptPackage != null) {
			for (ProposedConcept proposedConcept : conceptPackage.getProposedConcepts()) {
				// if (concept.getUuid().equals(sourceConceptUUID)) {
				if(proposedConcept.getConcept().getId().equals(sourceConcept.getId())) {
					String currentComment = proposedConcept.getComment();

//					proposedConcept.setComment((currentComment == null ? "" : currentComment + "\n") +
//							"=======================================================\n" +
//							DateTime.now().toString("yyyy-MM-dd:HH:mm:ss ") + incomingComment.getName() + " (" + incomingComment.getEmail() + ")\n" +
//							"-------------------------------------------------------\n" +
//							incomingComment.getComment());
					proposedConcept.setComment(incomingComment.getComment());

					incomingComment.setProposedConceptPackageUuid(proposalUuid);
					incomingComment.setProposedConceptUuid(conceptUuid);
					if (proposedConceptService.saveProposedConceptPackage(conceptPackage) != null){
						// submitProposal.addComment(incomingComment);
						return createProposedConceptPackageDto(conceptPackage);
					}
					else {
						log.error("Error saving comment");
					}
				}
			}
		}
		else{
			log.error((conceptPackage != null ? "Concept not found (searching for UUID: " + conceptUuid + ")" : "Proposal Package not found (searching for UUID: " + proposalUuid + ")"));
		}
		return null;
	}

	// proposer adds comments to local concept, send to dictionary manager
	@RequestMapping(value = "/conceptpropose/proposals/{proposalId}/{conceptId}/comment", method = RequestMethod.POST)
	public @ResponseBody ProposedConceptPackageDto addCommentByIds(@PathVariable int proposalId, @PathVariable int  conceptId, @RequestBody final CommentDto incomingComment) {
		// TODO - throw exception on error?

		final ProposedConceptService proposedConceptService = Context.getService(ProposedConceptService.class);
		final ProposedConceptPackage conceptPackage = proposedConceptService.getProposedConceptPackageById(proposalId);
		final ConceptService conceptService = Context.getConceptService();
		final Concept sourceConcept = conceptService.getConcept(conceptId);

		log.error("Package N: " + conceptPackage);
		log.error("Concept N: " + sourceConcept);
		if(conceptPackage!= null)
			log.error("Package: " + conceptPackage.getUuid());
		if(sourceConcept!= null)
			log.error("Concept: " + sourceConcept.getUuid());
		if(conceptPackage != null) {
			for (ProposedConcept proposedConcept : conceptPackage.getProposedConcepts()) {
				// if (concept.getUuid().equals(sourceConceptUUID)) {
				if(proposedConcept.getConcept().getId().equals(sourceConcept.getId())) {
					String currentComment = proposedConcept.getComment();

					proposedConcept.setComment((currentComment == null ? "" : currentComment + "\n") +
							"=======================================================\n" +
							DateTime.now().toString("yyyy-MM-dd:HH:mm:ss ") + incomingComment.getName() + " (" + incomingComment.getEmail() + ")\n" +
							"-------------------------------------------------------\n" +
							incomingComment.getComment());
					incomingComment.setProposedConceptPackageUuid(conceptPackage.getUuid());
					incomingComment.setProposedConceptUuid(sourceConcept.getUuid());
					if (proposedConceptService.saveProposedConceptPackage(conceptPackage) != null){
						submitProposal.addComment(incomingComment);
						return createProposedConceptPackageDto(conceptPackage);
					}
					else {
						log.error("Error saving comment");
					}
				}
			}
		}
		else{
			log.error((conceptPackage != null ? "Concept not found (searching for id: " + conceptId + ")" : "Proposal Package not found (searching for id: " + proposalId + ")"));
		}
		return null;
	}
	@RequestMapping(value = "/conceptpropose/proposals/{proposalId}", method = RequestMethod.PUT)
	public @ResponseBody ProposedConceptPackageDto updateProposal(@PathVariable final String proposalId,
                                                                  @RequestBody final ProposedConceptPackageDto updatedPackage) {

		final ProposedConceptService proposedConceptService = Context.getService(ProposedConceptService.class);
		final ProposedConceptPackage conceptPackage = proposedConceptService.getProposedConceptPackageById(Integer.valueOf(proposalId));

		// TODO: some server side validation here... not null fields, valid email?

		conceptPackage.setName(updatedPackage.getName());
		conceptPackage.setEmail(updatedPackage.getEmail());
		conceptPackage.setDescription(updatedPackage.getDescription());
		updateProposedConceptPackage.updateProposedConcepts(conceptPackage, updatedPackage);
        proposedConceptService.saveProposedConceptPackage(conceptPackage);

		if (conceptPackage.getStatus() == PackageStatus.DRAFT && updatedPackage.getStatus() == PackageStatus.TBS) {
			submitProposal.submitProposedConcept(conceptPackage);
		}

		return updatedPackage;
	}
    @ExceptionHandler(ProposalSubmissionException.class)

    @ResponseBody
    public void errorResponse(HttpServletResponse httpRes,ProposalSubmissionException ex) {
        try {
            httpRes.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, ex.getHttpStatus()+"");
        }
        catch(Exception e){
            httpRes.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @RequestMapping(value = "/conceptpropose/proposals/{proposalId}", method = RequestMethod.DELETE)
	public void deleteProposal(@PathVariable final String proposalId) {
		final ProposedConceptService service = Context.getService(ProposedConceptService.class);
		service.deleteProposedConceptPackage(service.getProposedConceptPackageById(Integer.valueOf(proposalId)));
	}

	private ProposedConceptPackageDto createProposedConceptPackageDto(final ProposedConceptPackage conceptProposalPackage) {

		final ProposedConceptPackageDto conceptProposalPackageDto = new ProposedConceptPackageDto();
		conceptProposalPackageDto.setId(conceptProposalPackage.getId());
		conceptProposalPackageDto.setName(conceptProposalPackage.getName());
		conceptProposalPackageDto.setEmail(conceptProposalPackage.getEmail());
		conceptProposalPackageDto.setDescription(conceptProposalPackage.getDescription());
		conceptProposalPackageDto.setStatus(conceptProposalPackage.getStatus());

		final Set<ProposedConcept> proposedConcepts = conceptProposalPackage.getProposedConcepts();
		final List<ProposedConceptDto> list = new ArrayList<ProposedConceptDto>();

		for (final ProposedConcept conceptProposal : proposedConcepts) {

			final Concept concept = conceptProposal.getConcept();
			final ProposedConceptDto conceptProposalDto = new ProposedConceptDto();
			conceptProposalDto.setId(concept.getConceptId());
			conceptProposalDto.setNames(nameDtoFactory.create(concept));
			conceptProposalDto.setPreferredName(concept.getName().getName());
			conceptProposalDto.setDescriptions(descriptionDtoFactory.create(concept));
            if(concept.getDescription() != null) {
                conceptProposalDto.setCurrLocaleDescription(concept.getDescription().getDescription());
            }
			conceptProposalDto.setDatatype(concept.getDatatype().getName());
			conceptProposalDto.setStatus(conceptProposal.getStatus());
            conceptProposalDto.setComment(conceptProposal.getComment());
			list.add(conceptProposalDto);
		}

		conceptProposalPackageDto.setConcepts(list);
		return conceptProposalPackageDto;
	}

	public SubmitProposal getSubmitProposal() {
		return submitProposal;
	}


    private ConceptDto createConceptDto(final Concept concept) {

        final ConceptDto dto = new ConceptDto();
        dto.setId(concept.getConceptId());
        dto.setNames(nameDtoFactory.create(concept));
        dto.setPreferredName(concept.getName().getName());
        dto.setDatatype(concept.getDatatype().getName());
        dto.setDescriptions(descriptionDtoFactory.create(concept));
        if(concept.getDescription()!=null)  {
            dto.setCurrLocaleDescription(concept.getDescription().getDescription());
        }

        return dto;
    }



}
