package objective.taskboard.sizing.export;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.ValueRange;

import java.io.IOException;
import java.util.List;

@Service
public class SizingDataProviderImpl implements SizingDataProvider {

	@Autowired
	SpreadsheetService spreadsheetService;

	@Override
	public String getAuthentication(String callbackUrl) {
		return spreadsheetService.getUrl(callbackUrl);
	}

	@Override
	public void storeAuthentication(String token, String callbackUrl) {
		spreadsheetService.storeCode(token, callbackUrl);
	}

	@SuppressWarnings("rawtypes")
	public String getGoogleSpreedsheetData(String spreedsheetId) throws IOException {

		Sheets service = spreadsheetService.getSheetsService();

		String spreadsheetId = spreedsheetId;
		String range = "A5:AB200";
		ValueRange response = service.spreadsheets().values().get(spreadsheetId, range).execute();

		List<List<Object>> values = response.getValues();
		if (values == null || values.size() == 0) {
			System.out.println("No data found.");
			System.exit(0);
		}

		String teste = "";

		for (List row : values) {
			for (Object object : row) {
				System.out.print(object.toString() + "\t");
				teste = teste + " " + object.toString();
			}
			System.out.println();
			break;
		}

		return teste;
	}

}