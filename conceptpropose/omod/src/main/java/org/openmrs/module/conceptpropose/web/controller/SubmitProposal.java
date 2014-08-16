package org.openmrs.module.conceptpropose.web.controller;

import org.directwebremoting.util.Logger;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.context.Context;
import org.openmrs.api.db.DAOException;
import org.openmrs.module.conceptpropose.PackageStatus;
import org.openmrs.module.conceptpropose.ProposedConceptPackage;
import org.openmrs.module.conceptpropose.SubmissionResponseStatus;
import org.openmrs.module.conceptpropose.api.ProposedConceptService;
import org.openmrs.module.conceptpropose.web.authentication.factory.AuthHttpHeaderFactory;
import org.openmrs.module.conceptpropose.web.common.CpmConstants;
import org.openmrs.module.conceptpropose.web.dto.CommentDto;
import org.openmrs.module.conceptpropose.web.dto.ProposedConceptReviewDto;
import org.openmrs.module.conceptpropose.web.dto.SubmissionDto;
import org.openmrs.module.conceptpropose.web.dto.SubmissionResponseDto;
import org.openmrs.module.conceptpropose.web.dto.factory.SubmissionDtoFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestOperations;

import static com.google.common.base.Preconditions.checkNotNull;

@Component
public class SubmitProposal {

	protected final Logger log = Logger.getLogger(getClass());

	private final RestOperations submissionRestTemplate;

	private final SubmissionDtoFactory submissionDtoFactory;

	private final AuthHttpHeaderFactory httpHeaderFactory;


	@Autowired
	public SubmitProposal(final RestOperations submissionRestTemplate,
						  final SubmissionDtoFactory submissionDtoFactory,
						  final AuthHttpHeaderFactory httpHeaderFactory) {
		this.submissionRestTemplate = submissionRestTemplate;
		this.httpHeaderFactory = httpHeaderFactory;
		this.submissionDtoFactory = submissionDtoFactory;
	}

	void addComment(final CommentDto comment) {
		checkNotNull(submissionRestTemplate);

		AdministrationService service = Context.getAdministrationService();
		HttpHeaders headers = httpHeaderFactory.create(service.getGlobalProperty(CpmConstants.SETTINGS_USER_NAME_PROPERTY),
				service.getGlobalProperty(CpmConstants.SETTINGS_PASSWORD_PROPERTY));
		final HttpEntity requestEntity = new HttpEntity<CommentDto>(comment, headers);

		final String url = service.getGlobalProperty(CpmConstants.SETTINGS_URL_PROPERTY) + "/ws/conceptreview/dictionarymanager/" + comment.getProposedConceptPackageUuid() + "/" + comment.getProposedConceptUuid() + "/comment";

		try {
			final ProposedConceptReviewDto result = submissionRestTemplate.postForObject(url, requestEntity, ProposedConceptReviewDto.class);
			log.error("Result: " + result);
//			if (result.getStatus() == SubmissionResponseStatus.FAILURE) {
//				log.error("Failed submitting proposal. Server Responded (200) but with Failure Status.");
//				throw new ProposalController.ProposalSubmissionException("", HttpStatus.INTERNAL_SERVER_ERROR);
//			}
		}catch(HttpClientErrorException e) { // exception with Dictionary manager's server, should handle all cases: internal server error / auth / bad request
//			log.error("Failed submitting proposal. HttpClientErrorException Exception: " + e.getMessage() + ": " + e.getStatusCode() + "/" + e.getStatusText());
//			throw new ProposalController.ProposalSubmissionException("", e.getStatusCode());
		}catch(RestClientException e){
//			log.error("Failed submitting proposal. REST Exception: " + e.getMessage());
//			throw new ProposalController.ProposalSubmissionException("", HttpStatus.BAD_GATEWAY);
		}catch(IllegalArgumentException e){ // 404, due to invalid URL
//			log.error("Failed submitting proposal. Invalid URL: " + e.getMessage());
//			throw new ProposalController.ProposalSubmissionException("", HttpStatus.NOT_FOUND);
//		}catch(ProposalController.ProposalSubmissionException e){
//			throw e;
		}catch(Exception e){
			log.error("Failed adding comment. Unknown error: " + e.getMessage() + "(" + e.getClass() + ")");
//			throw new ProposalController.ProposalSubmissionException("", HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	void submitProposedConcept(final ProposedConceptPackage conceptPackage) {

		checkNotNull(submissionRestTemplate);

		//
		// Could not figure out how to get Spring to send a basic authentication request using the "proper" object approach
		// see: https://github.com/johnsyweb/openmrs-cpm/wiki/Gotchas
		//

		AdministrationService service = Context.getAdministrationService();
		SubmissionDto submission = submissionDtoFactory.create(conceptPackage, service.getGlobalProperty(CpmConstants.SETTINGS_URL_PROPERTY));

		HttpHeaders headers = httpHeaderFactory.create(service.getGlobalProperty(CpmConstants.SETTINGS_USER_NAME_PROPERTY),
				service.getGlobalProperty(CpmConstants.SETTINGS_PASSWORD_PROPERTY));
		final HttpEntity requestEntity = new HttpEntity<SubmissionDto>(submission, headers);

		final String url = service.getGlobalProperty(CpmConstants.SETTINGS_URL_PROPERTY) + "/ws/conceptreview/dictionarymanager/proposals";

		try {
			final SubmissionResponseDto result = submissionRestTemplate.postForObject(url, requestEntity, SubmissionResponseDto.class);
			if (result.getStatus() == SubmissionResponseStatus.FAILURE) {
				log.error("Failed submitting proposal. Server Responded (200) but with Failure Status.");
				throw new ProposalController.ProposalSubmissionException("", HttpStatus.INTERNAL_SERVER_ERROR);
			}
		}catch(HttpClientErrorException e) { // exception with Dictionary manager's server, should handle all cases: internal server error / auth / bad request
			log.error("Failed submitting proposal. HttpClientErrorException Exception: " + e.getMessage() + ": " + e.getStatusCode() + "/" + e.getStatusText());
			throw new ProposalController.ProposalSubmissionException("", e.getStatusCode());
		}catch(RestClientException e){
			log.error("Failed submitting proposal. REST Exception: " + e.getMessage());
			throw new ProposalController.ProposalSubmissionException("", HttpStatus.BAD_GATEWAY);
		}catch(IllegalArgumentException e){ // 404, due to invalid URL
			log.error("Failed submitting proposal. Invalid URL: " + e.getMessage());
			throw new ProposalController.ProposalSubmissionException("", HttpStatus.NOT_FOUND);
		}catch(ProposalController.ProposalSubmissionException e){
			throw e;
		}catch(Exception e){
			log.error("Failed submitting proposal. Unknown error: " + e.getMessage() + "(" + e.getClass() + ")");
			throw new ProposalController.ProposalSubmissionException("", HttpStatus.INTERNAL_SERVER_ERROR);
		}
		conceptPackage.setStatus(PackageStatus.SUBMITTED);
		Context.getService(ProposedConceptService.class).saveProposedConceptPackage(conceptPackage);

	}







}
