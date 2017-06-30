package objective.taskboard.sizing.export;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;

import javax.transaction.Transactional;

import org.springframework.stereotype.Service;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeRequestUrl;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.sheets.v4.SheetsScopes;


public class GoogleAuthorization {

	private static final String APPLICATION_NAME = "TOOLTANK";

	private static final java.io.File DATA_STORE_DIR = new java.io.File("/home/deploy/store");

	private static FileDataStoreFactory DATA_STORE_FACTORY;

	private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

	private static HttpTransport HTTP_TRANSPORT;

	private static final List<String> SCOPES = Arrays.asList(SheetsScopes.SPREADSHEETS_READONLY);;

	private static final String clientid = "clientid";
	private static final String clientsecret = "clientsecret";

	private static final String CALLBACK_URI = "http://01aa4e7a.ngrok.io/getgooglelogin";

	private String stateToken;

	private final GoogleAuthorizationCodeFlow flow;

	public GoogleAuthorization() {
		try {
			HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
			DATA_STORE_FACTORY = new FileDataStoreFactory(DATA_STORE_DIR);

		} catch (GeneralSecurityException | IOException e) {
			e.printStackTrace();
		}

		flow = new GoogleAuthorizationCodeFlow.Builder(HTTP_TRANSPORT, JSON_FACTORY, clientid, clientsecret, SCOPES)
				.setAccessType("offline").setApprovalPrompt("force").build();
		generateStateToken();

	}

	/**
	 * Builds a login URL based on client ID, secret, callback URI, and scope
	 */
	public String buildLoginUrl() {

		final GoogleAuthorizationCodeRequestUrl url = flow.newAuthorizationUrl();

		return url.setRedirectUri(CALLBACK_URI).setState(stateToken).build();
	}

	/**
	 * Generates a secure state token
	 */
	private void generateStateToken() {
		SecureRandom sr1 = new SecureRandom();
		stateToken = "google;" + sr1.nextInt();
	}

	/**
	 * s Accessor for state token
	 */
	public String getStateToken() {
		return stateToken;
	}

	/**
	 * Expects an Authentication Code, and makes an authenticated request for
	 * the user's profile information * @param authCode authentication code
	 * provided by google
	 */
	public void saveCredentials(final String authCode) throws IOException {

		GoogleTokenResponse response = flow.newTokenRequest(authCode).setRedirectUri(CALLBACK_URI).execute();
		Credential credential = flow.createAndStoreCredential(response, null);
		System.out.println(" Credential access token is " + credential.getAccessToken());
		System.out.println("Credential refresh token is " + credential.getRefreshToken());
		// The line below gives me a NPE.
		//this.driveQuickstart.storeCredentials(credential);
	}
}