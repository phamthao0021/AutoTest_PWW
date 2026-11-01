package vn.team.autotest.app.controller;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import vn.team.autotest.app.dto.RunRequest;
import vn.team.autotest.app.service.TestExecutionService;
import vn.team.autotest.automation.registry.TestCatalog;
import vn.team.autotest.core.model.RunSummary;
import vn.team.autotest.core.model.TestDefinition;

@RestController
@RequestMapping("/api")
public class TestRunController {
    private final TestExecutionService service;
    public TestRunController(TestExecutionService service) { this.service = service; }

    @GetMapping("/modules") public List<TestDefinition> modules() { return TestCatalog.all(); }
    @PostMapping("/runs") public RunSummary start(@RequestBody(required = false) RunRequest request) { return service.start(request == null ? "all" : request.moduleId()); }
    @GetMapping("/runs/{runId}") public RunSummary status(@PathVariable String runId) { return service.find(runId); }
    @GetMapping("/runs") public List<RunSummary> history() { return service.history(); }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(IllegalArgumentException.class)
    public java.util.Map<String,String> badRequest(IllegalArgumentException ex) { return java.util.Map.of("error", ex.getMessage()); }
}
