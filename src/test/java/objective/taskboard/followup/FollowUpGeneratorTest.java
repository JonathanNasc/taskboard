package objective.taskboard.followup;

/*-
 * [LICENSE]
 * Taskboard
 * ---
 * Copyright (C) 2015 - 2017 Objective Solutions
 * ---
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * [/LICENSE]
 */

import static java.util.Arrays.asList;
import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.xml.sax.SAXException;

@RunWith(MockitoJUnitRunner.class)
public class FollowUpGeneratorTest {

    @InjectMocks
    private FollowUpGenerator subject;

    @Mock
    private FollowupDataProvider provider;

    private FollowUpData getFollowUpDataDefault() {
        FollowUpData followUpData = new FollowUpData();
        followUpData.planningType = "Ballpark";
        followUpData.project = "PROJECT TEST";
        followUpData.demandType = "Demand";
        followUpData.demandStatus = "Doing";
        followUpData.demandId = "1";
        followUpData.demandNum = "I-1";
        followUpData.demandSummary = "Summary Demand";
        followUpData.demandDescription = "Description Demand";
        followUpData.taskType = "Feature";
        followUpData.taskStatus = "Doing";
        followUpData.taskId = 2L;
        followUpData.taskNum = "I-2";
        followUpData.taskSummary = "Summary Feature";
        followUpData.taskDescription = "Description Feature";
        followUpData.taskFullSescription = "Full Description Feature";
        followUpData.taskRelease = "Release";
        followUpData.subtaskType = "Sub-task";
        followUpData.subtaskStatus = "Doing";
        followUpData.subtaskId = 3L;
        followUpData.subtaskNum = "I-3";
        followUpData.subtaskSummary = "Summary Sub-task";
        followUpData.subtaskDescription = "Description Sub-task";
        followUpData.subtaskFullDescription = "Full Description Sub-task";
        followUpData.tshirtSize = "M";
        followUpData.worklog = 1D;
        followUpData.wrongWorklog = 1D;
        followUpData.demandBallpark = 1D;
        followUpData.taskBallpark = 1D;
        followUpData.queryType = "Type";
        return followUpData;
    }

    @Test
    public void getSharedStringsTemplateTest() throws ParserConfigurationException, SAXException, IOException {
        Map<String, Long> sharedStrings = subject.getSharedStringsTemplate();
        assertEquals("Shared strings size", 457, sharedStrings.size());
        assertEquals("First shared string", 0L, sharedStrings.get("project").longValue());
        assertEquals("Any shared string", 223L, sharedStrings.get("Feature | 00236 - Global Solutions").longValue());
        assertEquals("Last shared string", 456L, sharedStrings.get("AllocatedHours").longValue());
    }

    @Test
    public void generateJiraDataSheetTest() throws ParserConfigurationException, SAXException, IOException {
        when(provider.getJiraData()).thenReturn(asList(getFollowUpDataDefault()));
        Map<String, Long> sharedStrings = subject.getSharedStringsTemplate();
        String jiraDataSheet = subject.generateJiraDataSheet(sharedStrings);

        InputStream inputStream = getClass().getClassLoader()
                .getResourceAsStream("followup/generateJiraDataSheetTest.xml");
        String jiraDataSheetExpected = IOUtils.toString(inputStream, "UTF-8");

        assertEquals("Jira data sheet", jiraDataSheetExpected, jiraDataSheet);
    }

    @Test
    public void generateSharedStringsTest() throws ParserConfigurationException, SAXException, IOException {
        Map<String, Long> sharedStrings = subject.getSharedStringsTemplate();
        when(provider.getJiraData()).thenReturn(asList(getFollowUpDataDefault()));
        subject.generateJiraDataSheet(sharedStrings);
        String sharedStringsGenerated = subject.generateSharedStrings(sharedStrings);

        InputStream inputStream = getClass().getClassLoader()
                .getResourceAsStream("followup/generateSharedStringsTest.xml");
        String sharedStringsExpected = IOUtils.toString(inputStream, "UTF-8");

        assertEquals("Shared strings", sharedStringsExpected, sharedStringsGenerated);
    }

}