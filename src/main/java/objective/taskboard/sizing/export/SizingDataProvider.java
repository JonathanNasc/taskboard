package objective.taskboard.sizing.export;

import java.io.IOException;

public interface SizingDataProvider {
	
	public String getAuthentication();
	
	public void storeAuthentication(String token);
	
	public String getGoogleSpreedsheetData(String spreedsheetId) throws IOException;
	
}
