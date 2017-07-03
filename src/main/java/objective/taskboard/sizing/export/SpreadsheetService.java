package objective.taskboard.sizing.export;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Component;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;

import objective.taskboard.auth.CredentialsHolder;

@Component
public class SpreadsheetService {

	private final String APPLICATION_NAME = "Google Sheets API Java Quickstart";

	private java.io.File DATA_STORE_DIR;

	private FileDataStoreFactory DATA_STORE_FACTORY;

	private final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

	private HttpTransport HTTP_TRANSPORT;

	private final List<String> SCOPES = Arrays.asList(SheetsScopes.SPREADSHEETS_READONLY);

	{
		try {
			DATA_STORE_DIR = new File(System.getProperty("user.home"),
					".credentials/sheets.googleapis.com-java-quickstart");
			HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
			DATA_STORE_FACTORY = new FileDataStoreFactory(DATA_STORE_DIR);
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	public String getUrl(String callbackUrl) {

		try {
			GoogleAuthorizationCodeFlow flow = getFlow();
			String build = flow.newAuthorizationUrl().setState("xyz").setRedirectUri(callbackUrl).build();
			return build;

		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	public void storeCode(String code, String callbackUrl) {
		try {
			TokenResponse response = new TokenResponse();
			response.setAccessToken(code);
			GoogleAuthorizationCodeFlow flow = getFlow();

			TokenResponse tokenResponse = flow.newTokenRequest(code).setRedirectUri(callbackUrl).execute();

			flow.createAndStoreCredential(tokenResponse, CredentialsHolder.username());
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	public Credential authorize() throws IOException {
		GoogleAuthorizationCodeFlow flow = getFlow();

		LocalServerReceiver receiver = new LocalServerReceiver();
		AuthorizationCodeInstalledApp authorization = new AuthorizationCodeInstalledApp(flow, receiver);
		Credential credential = authorization.authorize(CredentialsHolder.username());
		return credential;
	}

	public Sheets getSheetsService() throws IOException {
		Credential credential = authorize();
		return new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential).setApplicationName(APPLICATION_NAME)
				.build();
	}

	private GoogleAuthorizationCodeFlow getFlow() throws IOException {
		InputStream in = SpreadsheetService.class.getResourceAsStream("/googleapps-credentials.json");
		GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

		GoogleAuthorizationCodeFlow.Builder builder = new GoogleAuthorizationCodeFlow.Builder(HTTP_TRANSPORT,
				JSON_FACTORY, clientSecrets, SCOPES);
		GoogleAuthorizationCodeFlow flow = builder.setDataStoreFactory(DATA_STORE_FACTORY).setAccessType("offline")
				.build();
		return flow;
	}

}