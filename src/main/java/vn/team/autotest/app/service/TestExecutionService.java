package vn.team.autotest.app.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.testng.TestNG;
import vn.team.autotest.automation.registry.ScenarioRegistry;
import vn.team.autotest.automation.registry.TestCatalog;
import vn.team.autotest.core.model.RunSummary;
import vn.team.autotest.core.model.TestDefinition;
import vn.team.autotest.core.model.TestResult;
import vn.team.autotest.core.listener.TestResultListener;
import vn.team.autotest.core.model.TestStatus;

@Service
public class TestExecutionService {
    private final Executor executor;
    private final Map<String, RunSummary> runs = new ConcurrentHashMap<>();

    public TestExecutionService(@Qualifier("testExecutor") Executor executor) { this.executor = executor; }

    public RunSummary start(String moduleId) {
        String runId = UUID.randomUUID().toString();
        List<TestDefinition> selected = select(moduleId);
        int maxScore = selected.stream().mapToInt(TestDefinition::maxPoints).sum();
        RunSummary pending = new RunSummary(runId, TestStatus.PENDING, Instant.now(), null, 0, 0, 0, 0, maxScore, List.of());
        runs.put(runId, pending);
        executor.execute(() -> execute(runId, selected, maxScore));
        return pending;
    }

    public RunSummary find(String runId) {
        RunSummary summary = runs.get(runId);
        if (summary == null) throw new IllegalArgumentException("Không tồn tại runId: " + runId);
        return summary;
    }

    public List<RunSummary> history() {
        return runs.values().stream().sorted((a,b) -> b.startedAt().compareTo(a.startedAt())).toList();
    }

    private void execute(String runId, List<TestDefinition> selected, int maxScore) {
        Instant started = Instant.now();
        runs.put(runId, new RunSummary(runId, TestStatus.RUNNING, started, null, 0, 0, 0, 0, maxScore, List.of()));
        TestResultListener collector = new TestResultListener(runId);
        try {
            TestNG testng = new TestNG(false);
            testng.setUseDefaultListeners(false);
            testng.setVerbose(1);
            testng.addListener(collector);
            testng.setTestClasses(selected.stream().map(d -> ScenarioRegistry.byModuleId(d.moduleId())).toArray(Class<?>[]::new));
            testng.run();
        } catch (RuntimeException failure) {
            List<TestResult> failed = new ArrayList<>(collector.results());
            failed.add(new TestResult(runId, "framework", "framework", TestStatus.FAIL, 0, failure.getMessage(), null));
            finish(runId, started, selected, maxScore, failed);
            return;
        }
        finish(runId, started, selected, maxScore, collector.results());
    }

    private void finish(String runId, Instant started, List<TestDefinition> selected, int maxScore, List<TestResult> results) {
        int passed = (int) results.stream().filter(r -> r.status() == TestStatus.PASS).count();
        int failed = (int) results.stream().filter(r -> r.status() == TestStatus.FAIL).count();
        int skipped = (int) results.stream().filter(r -> r.status() == TestStatus.SKIP).count();
        Set<String> failedModules = new LinkedHashSet<>();
        results.stream().filter(r -> r.status() != TestStatus.PASS).map(TestResult::moduleId).forEach(failedModules::add);
        int score = selected.stream().filter(d -> !failedModules.contains(d.moduleId())).mapToInt(TestDefinition::maxPoints).sum();
        TestStatus status = failed == 0 && skipped == 0 ? TestStatus.PASS : TestStatus.FAIL;
        runs.put(runId, new RunSummary(runId, status, started, Instant.now(), passed, failed, skipped, score, maxScore, results));
    }

    private List<TestDefinition> select(String moduleId) {
        if (moduleId == null || moduleId.isBlank() || "all".equalsIgnoreCase(moduleId)) return TestCatalog.all();
        return List.of(TestCatalog.byId(moduleId));
    }
}
