package vn.team.autotest.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;
import vn.team.autotest.automation.registry.ScenarioRegistry;
import vn.team.autotest.automation.registry.TestCatalog;

class TestCatalogTest {
    @Test void catalogContainsFourteenIndependentModulesAndOneHundredPoints() {
        assertEquals(14, TestCatalog.all().size());
        assertEquals(100, TestCatalog.all().stream().mapToInt(item -> item.maxPoints()).sum());
        TestCatalog.all().forEach(item -> assertNotNull(ScenarioRegistry.byModuleId(item.moduleId())));
    }
}
