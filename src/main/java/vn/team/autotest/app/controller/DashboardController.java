package vn.team.autotest.app.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import vn.team.autotest.automation.registry.TestCatalog;

@Controller
public class DashboardController {
    @GetMapping("/")
    public String dashboard(Model model) {
        model.addAttribute("modules", TestCatalog.all());
        model.addAttribute("totalTests", TestCatalog.all().stream().mapToInt(m -> m.testCount()).sum());
        return "index";
    }
}
