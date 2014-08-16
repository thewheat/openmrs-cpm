package org.openmrs.module.conceptreview.web.common;

import org.apache.commons.codec.CharEncoding;

public interface CpmConstants {

    String SETTINGS_USER_NAME_PROPERTY = "conceptreview.username";

    String SETTINGS_PASSWORD_PROPERTY = "conceptreview.password";

    String SETTINGS_URL_PROPERTY = "conceptreview.url";

    String LIST_PROPOSAL_URL = "/module/conceptpropose/proposals";

    String AUTH_DATA_DELIMITER = ":";

    String AUTH_CHAR_SET = CharEncoding.UTF_8;

    String AUTH_TYPE = "Basic";

}
