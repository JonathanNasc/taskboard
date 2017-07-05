package objective.taskboard.controller;

import com.google.common.collect.Lists;
import objective.taskboard.followup.FollowUpFacade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by herbert on 04/07/17.
 */

@RestController
@RequestMapping("/api/templates")
public class TemplateController {

    @Autowired
    private FollowUpFacade followUpFacade;

    @RequestMapping
    public List<TemplateData> get() {
        List<TemplateData> list = new ArrayList();
        {
            TemplateData item1 = new TemplateData();
            item1.name = "template1";
            item1.projects = Lists.newArrayList("TASKB");
            list.add(item1);
        }
        {
            TemplateData item2 = new TemplateData();
            item2.name = "template2";
            item2.projects = Lists.newArrayList("PROJ1");
            list.add(item2);
        }
        {
            TemplateData item3 = new TemplateData();
            item3.name = "template3";
            item3.projects = Lists.newArrayList("TASKB", "PROJ1");
            list.add(item3);
        }
        return list;
    }

    @RequestMapping(method = RequestMethod.POST, consumes="multipart/form-data")
    public void upload(@RequestParam("file") MultipartFile file
            , @RequestParam("name") String templateName
            , @RequestParam("projects") String projects) throws IOException {
        //followUpFacade.updateTemplate(file);
        followUpFacade.createTemplate(templateName, projects, file);
    }
}
