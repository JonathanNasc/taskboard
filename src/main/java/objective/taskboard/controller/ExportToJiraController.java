package objective.taskboard.controller;

import java.io.IOException;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import objective.taskboard.sizing.export.SizingDataProvider;

@RestController
@RequestMapping("export-to-jira")
public class ExportToJiraController {

	@Autowired
	private SizingDataProvider dataProvider;

	@RequestMapping("xxx")
	public String xxx(@RequestParam Map<String, String> requestParams) throws IOException {
		String code = requestParams.get("code");
		dataProvider.storeAuthentication(code, urlToRedirect());
		return dataProvider.getGoogleSpreedsheetData("1iJqwMxLWOr1fDcYDNuV-CUp-1smHpYtYqvaTk14Nwek");
	}

	@RequestMapping("export")
	public void export(HttpServletResponse response) throws IOException {
		String urlToRedirect = dataProvider.getAuthentication(urlToRedirect());
		response.sendRedirect(urlToRedirect);
	}

	private String urlToRedirect() {
		return "http://localhost:8080/export-to-jira/xxx";
	}

}