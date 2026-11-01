package vn.team.autotest.app.dto;

public record RunRequest(String moduleId) {
    public boolean runAll() { return moduleId == null || moduleId.isBlank() || "all".equalsIgnoreCase(moduleId); }
}
