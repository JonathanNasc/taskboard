package objective.taskboard.sizing.export;

import java.io.IOException;

public interface SizingDataProvider {

	public String getAuthentication(String callbackUrl);

	public void storeAuthentication(String token, String callbackUrl);

	public String getGoogleSpreedsheetData(String spreedsheetId) throws IOException;

}
