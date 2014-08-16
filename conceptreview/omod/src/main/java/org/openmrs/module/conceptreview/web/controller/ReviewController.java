package org.openmrs.module.conceptreview.web.controller;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import org.directwebremoting.util.Logger;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.openmrs.PersonName;
import org.openmrs.User;
import org.openmrs.api.context.Context;
import org.openmrs.module.conceptpropose.web.dto.CommentDto;
import org.openmrs.module.conceptpropose.web.dto.ProposedConceptReviewDto;
import org.openmrs.module.conceptreview.ProposedConceptReview;
import org.openmrs.module.conceptreview.ProposedConceptReviewPackage;
import org.openmrs.module.conceptpropose.web.dto.ProposedConceptReviewPackageDto;
import org.openmrs.module.conceptreview.api.ProposedConceptReviewService;
import org.openmrs.module.conceptreview.web.dto.factory.DtoFactory;
import org.openmrs.util.OpenmrsConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.*;

@Controller
public class ReviewController {
	protected final Logger log = Logger.getLogger(getClass());
	private final AddComment addComment;

	@Autowired
	public ReviewController (final AddComment addComment) {
		this.addComment = addComment;
	}

	//
	// Pages
	//

	@RequestMapping(value = "module/conceptreview/proposalReview.list", method = RequestMethod.GET)
	public String listProposalReview() {
		return "/module/conceptreview/proposalReview";
	}

	//
	// REST endpoints
	//

	@RequestMapping(value = "/conceptreview/proposalReviews", method = RequestMethod.GET)
	public @ResponseBody ArrayList<ProposedConceptReviewPackageDto> getProposalReviews() {

		final List<ProposedConceptReviewPackage> allConceptProposalReviewPackages = Context.getService(ProposedConceptReviewService.class).getAllProposedConceptReviewPackages();
		final ArrayList<ProposedConceptReviewPackageDto> response = new ArrayList<ProposedConceptReviewPackageDto>();

		for (final ProposedConceptReviewPackage conceptProposalReviewPackage : allConceptProposalReviewPackages) {

			final ProposedConceptReviewPackageDto conceptProposalReviewPackageDto = createProposedConceptReviewPackageDto(conceptProposalReviewPackage);
			response.add(conceptProposalReviewPackageDto);
		}

		return response;
	}

	@RequestMapping(value = "/conceptreview/proposalReviews/{proposalId}", method = RequestMethod.GET)
	public @ResponseBody ProposedConceptReviewPackageDto getProposalReview(@PathVariable int proposalId) {
		return createProposedConceptReviewPackageDto(Context.
				getService(ProposedConceptReviewService.class).
				getProposedConceptReviewPackageById(proposalId));
	}

	@RequestMapping(value = "/conceptreview/proposalReviews/{proposalId}", method = RequestMethod.DELETE)
	public @ResponseBody void deleteProposalReview(@PathVariable int proposalId) {
		Context.getService(ProposedConceptReviewService.class).deleteProposedConceptReviewPackageById(proposalId);
	}

	@RequestMapping(value = "/conceptreview/proposalReviews/{proposalId}/concepts/{conceptId}", method = RequestMethod.GET)
	public @ResponseBody
	ProposedConceptReviewDto getConceptReview(@PathVariable int proposalId, @PathVariable int conceptId) {
		final ProposedConceptReviewService service = Context.getService(ProposedConceptReviewService.class);
		final ProposedConceptReview proposedConcept = service.getProposedConceptReviewPackageById(proposalId).getProposedConcept(conceptId);
		return DtoFactory.createProposedConceptReviewDto(proposedConcept);
	}

	@RequestMapping(value = "/conceptreview/proposalReviews/{proposalId}/concepts/{conceptId}", method = RequestMethod.PUT)
	public @ResponseBody ProposedConceptReviewDto updateConceptReview(@PathVariable int proposalId, @PathVariable int conceptId, @RequestBody ProposedConceptReviewDto updatedProposalReview) {
		final ProposedConceptReviewService service = Context.getService(ProposedConceptReviewService.class);
		final ProposedConceptReviewPackage aPackage = service.getProposedConceptReviewPackageById(proposalId);
		final ProposedConceptReview proposedConcept = aPackage.getProposedConcept(conceptId);
		if (proposedConcept != null) {
			boolean commentUnchanged = proposedConcept.getReviewComment().equals(updatedProposalReview.getReviewComment());

			proposedConcept.setReviewComment(updatedProposalReview.getReviewComment());
			proposedConcept.setStatus(updatedProposalReview.getStatus());

			if (updatedProposalReview.getConceptId() != 0) {
				proposedConcept.setConcept(Context.getConceptService().getConcept(updatedProposalReview.getConceptId()));
			}

			service.saveProposedConceptReviewPackage(aPackage);

			log.error("commentUnchanged: " + commentUnchanged);
			log.error("addComment: " + addComment);
			log.error("package: " + aPackage.getUuid());

			log.error("concept id : " + conceptId);
			log.error("concept uuid: " + proposedConcept.getProposedConceptUuid());
			log.error("concept : " + proposedConcept.getConcept());
			try{
				log.error("concept: " + proposedConcept.getConcept().getUuid());
			}
			catch(Exception e){ log.error("EX: " + e.getMessage());}



			if(!commentUnchanged)
			{
				CommentDto commentDto = new CommentDto();
				User user = Context.getAuthenticatedUser();
				log.error("user: " + user);
				//commentDto.setEmail(getNotificationEmail(user));
				//commentDto.setName(getDisplayName(user));
				commentDto.setEmail("me@u.com");
				commentDto.setName("user");
				commentDto.setComment(proposedConcept.getReviewComment());
				commentDto.setProposedConceptPackageUuid(aPackage.getUuid());
				commentDto.setProposedConceptUuid(proposedConcept.getProposedConceptUuid());
			 	addComment.addComment(commentDto, aPackage);
			}
		}
		return DtoFactory.createProposedConceptReviewDto(proposedConcept);
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
	private ProposedConceptReviewPackageDto createProposedConceptReviewPackageDto(final ProposedConceptReviewPackage responsePackage) {

		final ProposedConceptReviewPackageDto dto = new ProposedConceptReviewPackageDto();
		dto.setId(responsePackage.getId());
		dto.setName(responsePackage.getName());
		dto.setEmail(responsePackage.getEmail());
		dto.setDescription(responsePackage.getDescription());

		if (responsePackage.getDateCreated() == null) {
			throw new NullPointerException("Date created is null");
		}
		Days d = Days.daysBetween(new DateTime(responsePackage.getDateCreated()), new DateTime(new Date()));
		dto.setAge(String.valueOf(d.getDays()));

		final Set<ProposedConceptReview> proposedConcepts = responsePackage.getProposedConcepts();
		final List<ProposedConceptReviewDto> list = new ArrayList<ProposedConceptReviewDto>();
		if (proposedConcepts != null) {
			for (final ProposedConceptReview conceptProposal : proposedConcepts) {
				list.add(DtoFactory.createProposedConceptReviewDto(conceptProposal));
			}
		}

		dto.setConcepts(list);
		return dto;
	}
}
