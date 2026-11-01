package vn.team.autotest.app;

import java.util.concurrent.Executor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.SimpleAsyncTaskExecutor;

@SpringBootApplication(scanBasePackages = "vn.team.autotest")
public class AutoTestApplication {
    public static void main(String[] args) { SpringApplication.run(AutoTestApplication.class, args); }

    @Bean("testExecutor")
    Executor testExecutor() {
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor("pww-test-");
        executor.setConcurrencyLimit(1);
        return executor;
    }
}
