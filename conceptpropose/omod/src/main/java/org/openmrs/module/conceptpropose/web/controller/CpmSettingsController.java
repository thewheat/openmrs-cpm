package org.openmrs.module.conceptpropose.web.controller;

import com.google.common.base.Joiner;
import org.apache.commons.codec.binary.Base64;
import org.omg.DynamicAny._DynValueStub;
import org.openmrs.GlobalProperty;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.context.Context;
import org.openmrs.module.conceptpropose.web.authentication.factory.AuthHttpHeaderFactory;
import org.openmrs.module.conceptpropose.web.common.CpmConstants;
import org.openmrs.module.conceptpropose.web.dto.Settings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestOperations;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;

@Controller
public class CpmSettingsController {
	private final RestOperations submissionRestTemplate;

	private final AuthHttpHeaderFactory httpHeaderFactory;


    @Autowired
    public CpmSettingsController(final RestOperations submissionRestTemplate,
                          final AuthHttpHeaderFactory httpHeaderFactory) {
        this.submissionRestTemplate = submissionRestTemplate;
        this.httpHeaderFactory = httpHeaderFactory;
    }

    @RequestMapping(value = "/conceptpropose/settings", method = RequestMethod.GET)
    public @ResponseBody Settings getSettings() {
        AdministrationService service = Context.getAdministrationService();
        Settings settings = new Settings();
        settings.setUrl(service.getGlobalProperty(CpmConstants.SETTINGS_URL_PROPERTY));
        settings.setUsername(service.getGlobalProperty(CpmConstants.SETTINGS_USER_NAME_PROPERTY));
        settings.setPassword(service.getGlobalProperty(CpmConstants.SETTINGS_PASSWORD_PROPERTY));
        settings.setUrlInvalid(checkSettingsUrlInvalid2()); // will hang UI and fail functional test if url is invalid
        return settings;
    }
    @RequestMapping(value = "/conceptpropose/settingsold", method = RequestMethod.GET)
    public @ResponseBody Settings getSettingsold() {
        AdministrationService service = Context.getAdministrationService();
        Settings settings = new Settings();
        settings.setUrl(service.getGlobalProperty(CpmConstants.SETTINGS_URL_PROPERTY));
        settings.setUsername(service.getGlobalProperty(CpmConstants.SETTINGS_USER_NAME_PROPERTY));
        settings.setPassword(service.getGlobalProperty(CpmConstants.SETTINGS_PASSWORD_PROPERTY));
        settings.setUrlInvalid(checkSettingsUrlInvalid());
        return settings;
    }
    @RequestMapping(value = "/conceptpropose/settingsnew", method = RequestMethod.GET)
    public @ResponseBody Settings getSettingsnew() {
        AdministrationService service = Context.getAdministrationService();
        Settings settings = new Settings();
        settings.setUrl(service.getGlobalProperty(CpmConstants.SETTINGS_URL_PROPERTY));
        settings.setUsername(service.getGlobalProperty(CpmConstants.SETTINGS_USER_NAME_PROPERTY));
        settings.setPassword(service.getGlobalProperty(CpmConstants.SETTINGS_PASSWORD_PROPERTY));
        settings.setUrlInvalid(checkSettingsUrlInvalid2());
        return settings;
    }

    @RequestMapping(value = "/conceptpropose/settingstest1", method = RequestMethod.GET)
    public @ResponseBody Settings getSettingstest1() {
        AdministrationService service = Context.getAdministrationService();
        Settings settings = new Settings();
        final String url = service.getGlobalProperty(CpmConstants.SETTINGS_URL_PROPERTY) + "/ws/conceptpropose/settings";
        settings.setUrl(url);
        settings.setUsername("This is the test settings");
        return settings;
    }
    @RequestMapping(value = "/conceptpropose/settingstest2", method = RequestMethod.GET)
    public @ResponseBody Settings getSettingstest2() {
        AdministrationService service = Context.getAdministrationService();
        Settings settings = new Settings();
        final String url = service.getGlobalProperty(CpmConstants.SETTINGS_URL_PROPERTY) + "/ws/conceptpropose/settings";
        settings.setUrl(url);
        settings.setUsername("This is the test settings2 " + canPingURL(url, 1000));
        return settings;
    }

    @RequestMapping(value = "/conceptpropose/settingstest3", method = RequestMethod.GET)
    public @ResponseBody Settings getSettingstest3() {
        AdministrationService service = Context.getAdministrationService();
        Settings settings = new Settings();
        final String url = "http://yahoo.com";
        settings.setUrl(url);
        settings.setUsername("This is the test settings3 " + canPingURL(url, 1000));
        return settings;
    }

    @RequestMapping(value = "/conceptpropose/settings", method = RequestMethod.POST)
    public @ResponseBody Settings postNewSettings(@RequestBody Settings settings) {
        AdministrationService service = Context.getAdministrationService();
        service.saveGlobalProperty(new GlobalProperty(CpmConstants.SETTINGS_URL_PROPERTY, settings.getUrl()));
        service.saveGlobalProperty(new GlobalProperty(CpmConstants.SETTINGS_USER_NAME_PROPERTY, settings.getUsername()));
        service.saveGlobalProperty(new GlobalProperty(CpmConstants.SETTINGS_PASSWORD_PROPERTY, settings.getPassword()));
        settings.setUrlInvalid(checkSettingsUrlInvalid2());
        return settings;
    }

    private boolean checkSettingsUrlInvalid() {
        AdministrationService service = Context.getAdministrationService();

        HttpHeaders headers = httpHeaderFactory.create(
                service.getGlobalProperty(CpmConstants.SETTINGS_USER_NAME_PROPERTY),
                service.getGlobalProperty(CpmConstants.SETTINGS_PASSWORD_PROPERTY)
        );

        final String url = service.getGlobalProperty(CpmConstants.SETTINGS_URL_PROPERTY) + "/ws/conceptpropose/settings";
        try {
            ResponseEntity<Settings> responseEntity = submissionRestTemplate.getForEntity(url, Settings.class, headers);
            return responseEntity.getStatusCode() != HttpStatus.OK;
        }
        catch (Exception ex) {
            return true;
        }
    }
    private boolean checkSettingsUrlInvalid2() {
        AdministrationService service = Context.getAdministrationService();

        HttpHeaders headers = httpHeaderFactory.create(
                service.getGlobalProperty(CpmConstants.SETTINGS_USER_NAME_PROPERTY),
                service.getGlobalProperty(CpmConstants.SETTINGS_PASSWORD_PROPERTY)
        );

        final String url = service.getGlobalProperty(CpmConstants.SETTINGS_URL_PROPERTY) + "/ws/conceptpropose/settings";
        return !canPingURL(url, 5000);
    }
    public boolean canPingURL(String url, int timeout) {
        // url = url.replaceFirst("https", "http"); // Otherwise an exception may be thrown on invalid SSL certificates.
        AdministrationService service = Context.getAdministrationService();

        final String auth = Joiner.on(CpmConstants.AUTH_DATA_DELIMITER).skipNulls().join(service.getGlobalProperty(CpmConstants.SETTINGS_USER_NAME_PROPERTY), service.getGlobalProperty(CpmConstants.SETTINGS_PASSWORD_PROPERTY));
        byte[] encodedAuth = Base64.encodeBase64(
                auth.getBytes(Charset.forName(CpmConstants.AUTH_CHAR_SET)));
        final String authHeader = CpmConstants.AUTH_TYPE + " " + new String( encodedAuth );
        //httpHeaders.set("Authorization", authHeader);

        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();

            connection.setConnectTimeout(timeout);
            connection.setReadTimeout(timeout);
            connection.setRequestProperty("Authorization",authHeader);
            connection.setRequestMethod("GET");
            connection.setUseCaches(false);

            int responseCode = connection.getResponseCode();
            return (200 <= responseCode && responseCode <= 399);
        } catch (IOException exception) {
            return false;
        }
    }

}
