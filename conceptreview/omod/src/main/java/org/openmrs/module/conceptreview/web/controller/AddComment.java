package org.openmrs.module.conceptreview.web.controller;

import org.directwebremoting.util.Logger;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.context.Context;
import org.openmrs.module.conceptpropose.web.authentication.factory.AuthHttpHeaderFactory;
import org.openmrs.module.conceptpropose.web.dto.ProposedConceptPackageDto;
import org.openmrs.module.conceptreview.ProposedConceptReviewPackage;
import org.openmrs.module.conceptreview.web.common.CpmConstants;
import org.openmrs.module.conceptpropose.web.dto.CommentDto;
import org.openmrs.module.conceptpropose.web.dto.ProposedConceptReviewDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestOperations;

import static com.google.common.base.Preconditions.checkNotNull;

@Component
public class AddComment {

	protected final Logger log = Logger.getLogger(getClass());

	private final RestOperations submissionRestTemplate;


	private final AuthHttpHeaderFactory httpHeaderFactory;


	@Autowired
	public AddComment(final RestOperations submissionRestTemplate,
					  final AuthHttpHeaderFactory httpHeaderFactory) {
		this.submissionRestTemplate = submissionRestTemplate;
		this.httpHeaderFactory = httpHeaderFactory;
	}

	void addComment(final CommentDto comment, final ProposedConceptReviewPackage conceptReviewPackage) {
		log.error("submissionRestTemplate: " + submissionRestTemplate);
		checkNotNull(submissionRestTemplate);

		//
		// Could not figure out how to get Spring to send a basic authentication request using the "proper" object approach
		// see: https://github.com/johnsyweb/openmrs-cpm/wiki/Gotchas
		//

		AdministrationService service = Context.getAdministrationService();
//		HttpHeaders headers = httpHeaderFactory.create(service.getGlobalProperty(CpmConstants.SETTINGS_USER_NAME_PROPERTY),
//				service.getGlobalProperty(CpmConstants.SETTINGS_PASSWORD_PROPERTY));
		HttpHeaders headers = httpHeaderFactory.create("admin", "Admin123");   // TODO - needs a fix
		final HttpEntity requestEntity = new HttpEntity<CommentDto>(comment, headers);

		// TODO - needs a fix
		// final String url = service.getGlobalProperty(conceptReviewPackage.getServer()) + "/ws/conceptpropose/concept/" + comment.getProposedConceptPackageUuid() + "/" + comment.getProposedConceptUuid() + "/comment";
		final String url = "http://192.168.33.10:8080/openmrs" + "/ws/conceptpropose/concept/" + comment.getProposedConceptPackageUuid() + "/" + comment.getProposedConceptUuid() + "/comment";
		log.error("url: " + url);
		try {
			final ProposedConceptPackageDto result = submissionRestTemplate.postForObject(url, requestEntity, ProposedConceptPackageDto.class);
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
}
