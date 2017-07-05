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

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.text.StrSubstitutor;
import org.springframework.core.io.ByteArrayResource;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang.ObjectUtils.defaultIfNull;

@Slf4j
public class FollowUpGenerator {

    private static final int BUFFER = 2048;

    private static final String PROPERTY_XML_SPACE_PRESERVE = " xml:space=\"preserve\"";
    private static final String TAG_T_IN_SHARED_STRINGS = "t";

    private static final String PATH_SHEET7 = "xl/worksheets/sheet7.xml";
    private static final String PATH_SHARED_STRINGS = "xl/sharedStrings.xml";
    private static final String PATH_TABLE7 = "xl/tables/table7.xml";
    private final FollowUpTemplate template;

    private FollowupDataProvider provider;

    public FollowUpGenerator(FollowupDataProvider provider, FollowUpTemplate template) {
        this.provider = provider;
        this.template = template;
    }

    public ByteArrayResource generate(String [] includedProjects) throws Exception {
        File directoryTempFollowup = null;
        Path pathFollowupXLSM = null;
        try {
            directoryTempFollowup = decompressTemplate();

            Map<String, Long> sharedStrings = getSharedStringsInitial();

            File fileSheet7 = new File(directoryTempFollowup, PATH_SHEET7);
            
            
            List<FollowUpData> jiraData = provider.getJiraData(includedProjects);
            writeXML(fileSheet7, generateJiraDataSheet(sharedStrings, includedProjects, jiraData));
            File fileSharedStrings = new File(directoryTempFollowup, PATH_SHARED_STRINGS);
            writeXML(fileSharedStrings, generateSharedStrings(sharedStrings));
            File table7 = new File(directoryTempFollowup, PATH_TABLE7);
            writeXML(table7, generateTable7(jiraData.size()));

            pathFollowupXLSM = compressXLSM(directoryTempFollowup);
            ByteArrayResource resource = new ByteArrayResource(Files.readAllBytes(pathFollowupXLSM));

            return resource;
        } catch (Exception e) {
            log.error(e.getMessage() == null ? e.toString() : e.getMessage());
            throw e;
        } finally {
            if (directoryTempFollowup != null && directoryTempFollowup.exists())
                FileUtils.deleteDirectory(directoryTempFollowup);
            if (pathFollowupXLSM != null && pathFollowupXLSM.toFile().exists())
                Files.delete(pathFollowupXLSM);
        }
    }

    private File decompressTemplate() throws Exception {
        Path pathFollowup = Files.createTempDirectory("Followup");

        ZipInputStream zipInputStream = null;
        BufferedOutputStream bufferedOutput = null;
        try {
            InputStream inputStream = template.getPathFollowupTemplateXLSM().openStream();
            zipInputStream = new ZipInputStream(new BufferedInputStream(inputStream));

            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                Path entryPath = pathFollowup.resolve(entry.getName());
                if (entry.isDirectory()) {
                    Files.createDirectories(entryPath);
                    continue;
                }
    
                int count;
                byte data[] = new byte[BUFFER];
                FileOutputStream fos = new FileOutputStream(entryPath.toFile());
                bufferedOutput = new BufferedOutputStream(fos, BUFFER);
                while ((count = zipInputStream.read(data, 0, BUFFER)) != -1)
                    bufferedOutput.write(data, 0, count);
                bufferedOutput.flush();
                bufferedOutput.close();
            }

            return pathFollowup.toFile();
        } finally {
            if (bufferedOutput != null)
                bufferedOutput.close();
            if (zipInputStream != null)
                zipInputStream.close();
        }
    }

    private void writeXML(File file, String xml) throws Exception {
        FileOutputStream output = null;
        try {
            output = new FileOutputStream(file);
            output.write(xml.getBytes("UTF-8"));
        } finally {
            if (output != null)
                output.close();
        }
    }

    private Path compressXLSM(File directoryFollowup) throws Exception {
        Path pathFollowupXLSM = Files.createTempFile("Followup", ".xlsm");
        ZipOutputStream zipOutputStream = null;
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(pathFollowupXLSM.toFile());
            zipOutputStream = new ZipOutputStream(new BufferedOutputStream(fileOutputStream));
            for (File fileInFollowup : directoryFollowup.listFiles())
                compressFile(fileInFollowup, null, zipOutputStream);
            return pathFollowupXLSM;
        } finally {
            if (zipOutputStream != null)
                zipOutputStream.close();
        }
    }

    private void compressFile(File file, String parentDirectory, ZipOutputStream zipOutputStream) throws Exception {
        if (file == null || !file.exists())
            return;

        String zipEntryName = parentDirectory == null ? file.getName() : parentDirectory + "/" + file.getName();

        if (file.isDirectory()) {
            for (File f : file.listFiles())
                compressFile(f, zipEntryName, zipOutputStream);
            return;
        }

        zipOutputStream.putNextEntry(new ZipEntry(zipEntryName));

        BufferedInputStream bufferedInput = null;
        try {
            FileInputStream fis = new FileInputStream(file);
            bufferedInput = new BufferedInputStream(fis, BUFFER);
            int count;
            byte data[] = new byte[BUFFER];
            while((count = bufferedInput.read(data, 0, BUFFER)) != -1)
                zipOutputStream.write(data, 0, count);
        } finally {
            if (bufferedInput != null)
                bufferedInput.close();
        }
    }

    Map<String, Long> getSharedStringsInitial() throws ParserConfigurationException, SAXException, IOException {
        Map<String, Long> sharedStrings = new HashMap<String, Long>();
        InputStream inputStream = template.getPathSharedStringsInitial().openStream();
        DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
        Document doc = docBuilderFactory.newDocumentBuilder().parse(inputStream);
        doc.getDocumentElement().normalize();
        NodeList nodes = doc.getElementsByTagName(TAG_T_IN_SHARED_STRINGS);

        for (Long index = 0L; index < nodes.getLength(); index++) {
            Node node = nodes.item(index.intValue());
            if (node.getNodeType() != Node.ELEMENT_NODE)
                continue;

            sharedStrings.put(node.getTextContent(), index);
        }
        return sharedStrings;
    }
    String generateJiraDataSheet(Map<String, Long> sharedStrings, String [] includedProjects) {
        List<FollowUpData> jiraData = provider.getJiraData(includedProjects);
        return generateJiraDataSheet(sharedStrings, includedProjects, jiraData);
    }

    String generateJiraDataSheet(Map<String, Long> sharedStrings, String [] includedProjects, List<FollowUpData> jiraData) {
        String rowTemplate = getStringFromXML(template.getPathSheet7RowTemplate());
        StringBuilder rows = new StringBuilder();
        int rowNumber = 2;

        for (FollowUpData followUpData : jiraData) {
            Map<String, Object> rowValues = new HashMap<String, Object>();
            rowValues.put("rowNumber", rowNumber);
            rowValues.put("project", getOrSetIndexInSharedStrings(sharedStrings, followUpData.project));
            rowValues.put("demandType", getOrSetIndexInSharedStrings(sharedStrings, followUpData.demandType));
            rowValues.put("demandStatus", getOrSetIndexInSharedStrings(sharedStrings, followUpData.demandStatus));
            rowValues.put("demandNum", getOrSetIndexInSharedStrings(sharedStrings, followUpData.demandNum));
            rowValues.put("demandSummary", getOrSetIndexInSharedStrings(sharedStrings, followUpData.demandSummary));
            rowValues.put("demandDescription", getOrSetIndexInSharedStrings(sharedStrings, followUpData.demandDescription));
            rowValues.put("taskType", getOrSetIndexInSharedStrings(sharedStrings, followUpData.taskType));
            rowValues.put("taskStatus", getOrSetIndexInSharedStrings(sharedStrings, followUpData.taskStatus));
            rowValues.put("taskNum", getOrSetIndexInSharedStrings(sharedStrings, followUpData.taskNum));
            rowValues.put("taskSummary", getOrSetIndexInSharedStrings(sharedStrings, followUpData.taskSummary));
            rowValues.put("taskDescription", getOrSetIndexInSharedStrings(sharedStrings, followUpData.taskDescription));
            rowValues.put("taskFullDescription", getOrSetIndexInSharedStrings(sharedStrings, followUpData.taskFullDescription));
            rowValues.put("subtaskType", getOrSetIndexInSharedStrings(sharedStrings, followUpData.subtaskType));
            rowValues.put("subtaskStatus", getOrSetIndexInSharedStrings(sharedStrings, followUpData.subtaskStatus));
            rowValues.put("subtaskNum", getOrSetIndexInSharedStrings(sharedStrings, followUpData.subtaskNum));
            rowValues.put("subtaskSummary", getOrSetIndexInSharedStrings(sharedStrings, followUpData.subtaskSummary));
            rowValues.put("subtaskDescription", getOrSetIndexInSharedStrings(sharedStrings, followUpData.subtaskDescription));
            rowValues.put("subtaskFullDescription", getOrSetIndexInSharedStrings(sharedStrings, followUpData.subtaskFullDescription));
            rowValues.put("demandId", defaultIfNull(followUpData.demandId, ""));
            rowValues.put("taskId", defaultIfNull(followUpData.taskId, ""));
            rowValues.put("subtaskId", defaultIfNull(followUpData.subtaskId, ""));
            rowValues.put("planningType", getOrSetIndexInSharedStrings(sharedStrings, followUpData.planningType));
            rowValues.put("taskRelease", getOrSetIndexInSharedStrings(sharedStrings, followUpData.taskRelease));
            rowValues.put("worklog", defaultIfNull(followUpData.worklog, ""));
            rowValues.put("wrongWorklog", defaultIfNull(followUpData.wrongWorklog, ""));
            rowValues.put("demandBallpark", defaultIfNull(followUpData.demandBallpark, ""));
            rowValues.put("taskBallpark", defaultIfNull(followUpData.taskBallpark, ""));
            rowValues.put("tshirtSize", getOrSetIndexInSharedStrings(sharedStrings, followUpData.tshirtSize));
            rowValues.put("queryType", getOrSetIndexInSharedStrings(sharedStrings, followUpData.queryType));
            rows.append(StrSubstitutor.replace(rowTemplate, rowValues));
            rowNumber++;
        }

        String sheetTemplate = getStringFromXML(template.getPathSheet7Template());
        Map<String, String> sheetValues = new HashMap<String, String>();
        sheetValues.put("rows", rows.toString());
        return StrSubstitutor.replace(sheetTemplate, sheetValues);
    }

    private String getStringFromXML(URL pathXML) {
        try {
            return IOUtils.toString(pathXML, "UTF-8");
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private Object getOrSetIndexInSharedStrings(Map<String, Long> sharedStrings, String followUpDataAttrValue) {
        if (followUpDataAttrValue == null || followUpDataAttrValue.isEmpty())
            return "";

        Long index = sharedStrings.get(followUpDataAttrValue);
        if (index != null)
            return index;

        index = Long.valueOf(sharedStrings.size());
        sharedStrings.put(followUpDataAttrValue, index);
        return index;
    }

    String generateSharedStrings(Map<String, Long> sharedStrings) throws IOException {
        String siSharedStringsTemplate = getStringFromXML(template.getPathSISharedStringsTemplate());
        List<String> sharedStringsSorted = sharedStrings.keySet().stream()
            .sorted((s1, s2) -> sharedStrings.get(s1).compareTo(sharedStrings.get(s2)))
            .collect(toList());
        StringBuilder allSharedStrings = new StringBuilder();

        for (String sharedString : sharedStringsSorted) {
            Map<String, Object> siValues = new HashMap<String, Object>();
            siValues.put("preserve", sharedString.endsWith(" ") ? PROPERTY_XML_SPACE_PRESERVE : "");
            siValues.put("sharedString", StringEscapeUtils.escapeXml(sharedString));
            allSharedStrings.append(StrSubstitutor.replace(siSharedStringsTemplate, siValues));
        }

        String sharedStringsTemplate = getStringFromXML(template.getPathSharedStringsTemplate());
        Map<String, Object> sharedStringsValues = new HashMap<String, Object>();
        sharedStringsValues.put("sharedStringsSize", sharedStringsSorted.size());
        sharedStringsValues.put("allSharedStrings", allSharedStrings.toString());
        return StrSubstitutor.replace(sharedStringsTemplate, sharedStringsValues);
    }
    
    String generateTable7(int lineCount) {
        String template = getStringFromXML(this.template.getPathTable7Template());
        Map<String, Object> values = new HashMap<String, Object>();
        values.put("LINE_COUNT", lineCount+1);
        return StrSubstitutor.replace(template, values);
    }
}
