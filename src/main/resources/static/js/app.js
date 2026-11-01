(() => {
  const modules = Array.isArray(window.PWW_MODULES) ? window.PWW_MODULES : [];
  let selected = modules.find(item => item.moduleId === "dashboard") || modules[0];
  let selectedCase = null;
  let activeRun = null;

  const casePrefixes = {home:"HOM",dashboard:"DSH",learning:"LRN",flashcard:"FLC",quiz:"QIZ",matching:"MAT",game:"GAM",notification:"NTF",wordtype:"WTP",vocabulary:"VOC",folder:"FLD",library:"LIB",customflashcard:"CFC",users:"USR"};
  const caseTopics = {
    home:["Hiển thị khu vực giới thiệu chính","Hiển thị menu điều hướng","Điều hướng đến trang đăng nhập","Hiển thị danh sách bài học nổi bật","Tìm kiếm nội dung từ trang chủ","Hiển thị giao diện trên thiết bị di động"],
    dashboard:["Hiển thị tổng quan tiến độ học tập","Hiển thị số từ đã học","Hiển thị lịch sử hoạt động gần nhất","Điều hướng nhanh đến bài học","Hiển thị mục tiêu học tập trong ngày","Cập nhật thống kê sau khi hoàn thành bài","Ẩn dữ liệu của người dùng khác","Hiển thị dashboard trên màn hình nhỏ"],
    learning:["Hiển thị danh sách bài học","Mở một bài học khả dụng","Đánh dấu nội dung đã hoàn thành","Tiếp tục bài học đang dở","Lọc bài học theo trạng thái","Lưu tiến độ học tập","Chặn truy cập bài học không hợp lệ"],
    flashcard:["Hiển thị bộ thẻ ghi nhớ","Lật mặt trước và mặt sau của thẻ","Chuyển đến thẻ tiếp theo","Quay lại thẻ trước","Đánh dấu từ đã nhớ","Đánh dấu từ cần học lại","Hiển thị tiến độ của bộ thẻ","Hoàn thành phiên học flashcard","Khôi phục trạng thái khi tải lại trang"],
    quiz:["Hiển thị câu hỏi và các đáp án","Chọn một đáp án","Không cho chọn đồng thời nhiều đáp án","Xác nhận đáp án đúng","Hiển thị phản hồi khi trả lời sai","Chuyển sang câu hỏi tiếp theo","Tính điểm sau khi hoàn thành","Hiển thị kết quả bài kiểm tra","Làm lại bài kiểm tra","Lưu kết quả vào lịch sử học tập"],
    matching:["Hiển thị danh sách từ và nghĩa","Chọn một cặp từ và nghĩa","Đánh dấu cặp nối chính xác","Thông báo khi nối sai","Không cho chọn mục đã hoàn thành","Tính số cặp đã nối","Hoàn thành trò chơi nối từ","Khởi động lại màn chơi"],
    game:["Hiển thị danh sách trò chơi","Mở trò chơi được chọn","Hiển thị hướng dẫn chơi","Cập nhật điểm trong khi chơi","Kết thúc khi hết thời gian","Lưu điểm cao nhất","Chơi lại sau khi kết thúc"],
    notification:["Hiển thị danh sách thông báo","Đánh dấu một thông báo đã đọc","Đánh dấu tất cả đã đọc","Mở nội dung liên kết từ thông báo","Phân trang danh sách thông báo","Không hiển thị thông báo của tài khoản khác"],
    wordtype:["Hiển thị danh sách loại từ","Tìm kiếm loại từ","Thêm loại từ hợp lệ","Không thêm tên loại từ trống","Không thêm loại từ trùng tên","Mở thông tin loại từ","Cập nhật tên loại từ","Hủy thao tác cập nhật","Xóa loại từ chưa được sử dụng","Cảnh báo khi xóa loại từ đang dùng","Chặn người dùng thường truy cập quản trị"],
    vocabulary:["Hiển thị danh sách từ vựng","Tìm kiếm từ vựng","Lọc từ theo loại từ","Thêm từ vựng hợp lệ","Kiểm tra từ bắt buộc","Kiểm tra nghĩa bắt buộc","Không thêm từ bị trùng","Mở chi tiết từ vựng","Cập nhật từ và nghĩa","Thay đổi loại từ","Hủy cập nhật từ vựng","Xóa từ vựng","Chặn người dùng thường truy cập quản trị"],
    folder:["Hiển thị danh sách thư mục","Tạo thư mục mới","Không tạo thư mục rỗng tên","Đổi tên thư mục","Mở nội dung thư mục","Thêm bộ từ vào thư mục","Xóa bộ từ khỏi thư mục","Xóa thư mục và xác nhận"],
    library:["Hiển thị thư viện học tập","Tìm kiếm bộ từ","Lọc bộ từ theo chủ đề","Mở chi tiết bộ từ","Lưu bộ từ vào thư viện cá nhân","Không lưu trùng một bộ từ","Bỏ lưu bộ từ","Phân trang danh sách thư viện"],
    customflashcard:["Hiển thị bộ thẻ cá nhân","Tạo bộ thẻ mới","Kiểm tra tên bộ thẻ bắt buộc","Thêm thẻ vào bộ","Kiểm tra từ và nghĩa bắt buộc","Mở chi tiết bộ thẻ","Cập nhật tên bộ thẻ","Chỉnh sửa một thẻ","Xóa một thẻ","Xóa bộ thẻ và xác nhận","Học bằng bộ thẻ cá nhân","Chỉ chủ sở hữu được chỉnh sửa"],
    users:["Hiển thị danh sách người dùng","Tìm kiếm người dùng","Lọc người dùng theo trạng thái","Mở chi tiết người dùng","Tạo tài khoản hợp lệ","Kiểm tra email bắt buộc","Kiểm tra email đúng định dạng","Không tạo email trùng","Cập nhật thông tin người dùng","Thay đổi vai trò người dùng","Khóa tài khoản","Mở khóa tài khoản","Đặt lại mật khẩu","Xóa người dùng và xác nhận","Chặn người dùng thường truy cập quản trị"]
  };
  const groups = ["Hiển thị", "Luồng xử lý", "Validation", "Phân quyền"];
  const casesByModule = new Map();

  const $ = selector => document.querySelector(selector);
  const $$ = selector => [...document.querySelectorAll(selector)];
  const cap = value => value.charAt(0).toUpperCase() + value.slice(1);
  const viewCopy = {
    overview: ["Tổng quan kiểm thử", "Theo dõi chất lượng toàn bộ website PWW"],
    modules: ["Kiểm thử chức năng", "Quản lý và thực thi nội dung kiểm thử theo từng trang"],
    source: ["Cấu trúc source code", "Theo dõi Page Object và Scenario của từng module"],
    suite: ["Test Suite", "Tạo và thực thi các bộ kiểm thử theo phạm vi"],
    reports: ["Báo cáo kiểm thử", "Kết quả và minh chứng của các lượt chạy gần nhất"]
  };

  function showView(id) {
    $$(".view").forEach(view => view.classList.toggle("active", view.id === id));
    $$(".nav-item").forEach(button => button.classList.toggle("active", button.dataset.target === id));
    const copy = viewCopy[id] || viewCopy.overview;
    $("#topbar-title").textContent = copy[0];
    $("#topbar-subtitle").textContent = copy[1];
    window.scrollTo({top: 0, behavior: "smooth"});
  }

  function selectModule(id) {
    const module = modules.find(item => item.moduleId === id);
    if (!module) return;
    selected = module;
    $$(".feature-item").forEach(row => row.classList.toggle("selected", row.dataset.module === id));
    $("#case-breadcrumb-name").textContent = module.name;
    $("#case-module-name").textContent = module.name;
    $("#case-module-route").textContent = module.route;
    $("#case-module-count").textContent = `${module.testCount} nội dung kiểm thử`;
    $("#case-score").textContent = "0";
    $("#case-max-score").textContent = module.maxPoints;
    $("#case-progress-bar").style.width = "0%";
    $("#case-progress-copy").textContent = "0% hoàn thành";
    $("#console-module").textContent = module.name;
    $("#source-module").value = module.moduleId;
    renderCases(module);
    updateSource(module);
  }

  function buildCases(module) {
    if (casesByModule.has(module.moduleId)) return casesByModule.get(module.moduleId);
    const topics = caseTopics[module.moduleId] || [];
    const prefix = casePrefixes[module.moduleId] || module.moduleId.slice(0, 3).toUpperCase();
    const rawPoint = module.maxPoints / module.testCount;
    const cases = Array.from({length: module.testCount}, (_, index) => ({
      id: `${prefix}-${String(index + 1).padStart(3, "0")}`,
      title: topics[index] || `Kiểm tra ${module.name.toLowerCase()} - trường hợp ${index + 1}`,
      group: groups[index % groups.length],
      status: "PENDING",
      maxPoint: Number(rawPoint.toFixed(2)),
      score: 0,
      precondition: module.role === "Guest" ? "Website PWW đang hoạt động và trình duyệt đã được mở." : `Tài khoản kiểm thử vai trò ${module.role} đang hoạt động và đã đăng nhập đúng quyền.`,
      steps: [`Truy cập trang ${module.route}`, `Thực hiện: ${topics[index] || `trường hợp kiểm thử ${index + 1}`}`, "Quan sát và đối chiếu kết quả hiển thị"],
      expected: `${topics[index] || "Chức năng"} hoạt động đúng, không phát sinh lỗi và dữ liệu được cập nhật chính xác.`
    }));
    casesByModule.set(module.moduleId, cases);
    return cases;
  }

  function renderCases(module) {
    const rows = buildCases(module);
    $("#test-case-body").innerHTML = rows.map(item => `<tr class="test-case-row" data-case="${item.id}" data-group="${item.group}" data-status="${item.status}" data-title="${escapeHtml(item.title.toLowerCase())}">
      <td><input class="case-check" type="checkbox" aria-label="Chọn ${item.id}"></td>
      <td><b class="case-code">${item.id}</b></td>
      <td><strong>${escapeHtml(item.title)}</strong></td>
      <td><span class="case-group">${item.group}</span></td>
      <td>${formatPoint(item.maxPoint)}</td>
      <td><span class="case-status pending"><i></i>Chưa chạy</span></td>
      <td><b>${formatPoint(item.score)} / ${formatPoint(item.maxPoint)}</b></td>
      <td><button class="case-play" aria-label="Mở ${item.id}">▷</button></td>
    </tr>`).join("");
    $("#select-all-cases").checked = false;
    updateCheckedCount();
    filterCases();
  }

  function formatPoint(value) {
    return Number.isInteger(value) ? String(value) : Number(value).toFixed(2).replace(/0$/, "");
  }

  function openCase(caseId) {
    const item = buildCases(selected).find(testCase => testCase.id === caseId);
    if (!item) return;
    selectedCase = item;
    $("#drawer-id").textContent = item.id;
    $("#drawer-title").textContent = item.title;
    $("#drawer-status").textContent = "● Chưa chạy";
    $("#drawer-status").className = "pending";
    $("#drawer-group").textContent = item.group;
    $("#drawer-score").textContent = formatPoint(item.score);
    $("#drawer-max").textContent = formatPoint(item.maxPoint);
    $("#drawer-precondition").textContent = item.precondition;
    $("#drawer-route").textContent = selected.route;
    $("#drawer-role").textContent = selected.role;
    $("#drawer-steps").innerHTML = item.steps.map(step => `<li>${escapeHtml(step)}</li>`).join("");
    $("#drawer-expected").textContent = item.expected;
    $("#drawer-actual").textContent = "Chưa có kết quả. Hãy chạy nội dung kiểm thử này.";
    $("#case-overlay").classList.add("open");
    $("#case-overlay").setAttribute("aria-hidden", "false");
    document.body.classList.add("drawer-open");
  }

  function closeCase() {
    $("#case-overlay").classList.remove("open");
    $("#case-overlay").setAttribute("aria-hidden", "true");
    document.body.classList.remove("drawer-open");
  }

  function updateSource(module) {
    const className = cap(module.packageName);
    $("#tree-module").textContent = `⌄ ${module.packageName}`;
    $$(".source-class").forEach(node => node.textContent = className);
    $("#editor-file").textContent = `${className}Scenario.java`;
    $("#editor-module").textContent = module.packageName;
    $("#code-preview").textContent = `package vn.team.autotest.modules.${module.packageName}.scenario;

import vn.team.autotest.automation.base.AbstractModuleScenario;
import vn.team.autotest.modules.${module.packageName}.page.${className}Page;

public final class ${className}Scenario extends AbstractModuleScenario {
    protected String route() { return "${module.route}"; }

    protected PwwModulePage page() {
        return new ${className}Page(driver(), config());
    }
}`;
  }

  function filterFeatureList() {
    const query = $("#feature-search").value.trim().toLowerCase();
    $$(".feature-item").forEach(row => row.hidden = !row.dataset.name.toLowerCase().includes(query));
  }

  function filterCases() {
    const query = $("#case-search").value.trim().toLowerCase();
    const group = $("#case-group-filter").value;
    const status = $("#case-status-filter").value;
    let visible = 0;
    $$(".test-case-row").forEach(row => {
      const show = (`${row.dataset.case} ${row.dataset.title}`).toLowerCase().includes(query) && (!group || row.dataset.group === group) && (!status || row.dataset.status === status);
      row.hidden = !show;
      if (show) visible += 1;
    });
    $("#case-table-summary").textContent = `Hiển thị ${visible} / ${selected ? selected.testCount : 0} nội dung`;
  }

  function updateCheckedCount() {
    const count = $$(".case-check:checked").length;
    $("#checked-count").textContent = count;
  }

  async function startRun(moduleId) {
    if (activeRun) return;
    showView("suite");
    activeRun = "starting";
    setRunning(true);
    writeConsole(`[${time()}] Gửi yêu cầu đến Spring Boot runner...\n[${time()}] Scope: ${moduleId === "all" ? "full-website.xml" : selected.suiteFile}`);
    try {
      const response = await fetch("/api/runs", {method:"POST", headers:{"Content-Type":"application/json"}, body:JSON.stringify({moduleId})});
      if (!response.ok) throw new Error(await response.text());
      const run = await response.json();
      activeRun = run.runId;
      writeConsole(`[${time()}] Run ID: ${run.runId}\n[${time()}] TestNG đang khởi tạo WebDriver...`);
      poll(run.runId);
    } catch (error) {
      finishWithError(error);
    }
  }

  async function poll(runId) {
    try {
      const response = await fetch(`/api/runs/${runId}`);
      if (!response.ok) throw new Error(await response.text());
      const run = await response.json();
      if (["PENDING", "RUNNING"].includes(run.status)) {
        $("#run-state").textContent = "ĐANG CHẠY";
        setTimeout(() => poll(runId), 700);
        return;
      }
      renderRun(run);
      activeRun = null;
      setRunning(false);
    } catch (error) { finishWithError(error); }
  }

  function renderRun(run) {
    const resultByModule = new Map(run.results.map(result => [result.moduleId, result]));
    $$(".suite-row").forEach(row => {
      const result = resultByModule.get(row.dataset.suiteModule);
      const badge = row.querySelector(":scope > i");
      badge.className = result ? result.status.toLowerCase() : "";
      badge.textContent = result ? result.status : "CHƯA CHẠY";
    });
    $("#run-state").textContent = run.status;
    const lines = [`[${time()}] Hoàn tất: ${run.status}`, `PASS: ${run.passed} · FAIL: ${run.failed} · SKIP: ${run.skipped}`, `SCORE: ${run.score} / ${run.maxScore}`];
    run.results.forEach(result => lines.push(`${result.status.padEnd(5)} ${result.moduleId}/${result.testId} ${result.durationMillis}ms${result.error ? `\n      ${result.error}` : ""}${result.evidencePath ? `\n      ${result.evidencePath}` : ""}`));
    writeConsole(lines.join("\n"));
    renderReport(run);
  }

  function renderReport(run) {
    $("#report-score").textContent = `${run.score} / ${run.maxScore}`;
    $("#report-copy").textContent = `${run.passed} PASS · ${run.failed} FAIL · ${run.skipped} SKIP`;
    $("#score-ring").textContent = run.maxScore ? `${Math.round(run.score * 100 / run.maxScore)}%` : "—";
    $("#report-body").innerHTML = run.results.length ? run.results.map(result => `<tr><td>${escapeHtml(result.testId)}</td><td>${escapeHtml(result.moduleId)}</td><td class="status-${result.status.toLowerCase()}">${result.status}</td><td>${result.durationMillis} ms</td><td>${escapeHtml(result.error || result.evidencePath || "—")}</td></tr>`).join("") : `<tr><td colspan="5">Không có kết quả.</td></tr>`;
  }

  function setRunning(running) {
    $$(".run-all,.run-selected").forEach(button => button.disabled = running);
    $("#run-state").textContent = running ? "ĐANG CHẠY" : "SẴN SÀNG";
    if (running) $$(".suite-row > i").forEach(badge => {badge.className = "running"; badge.textContent = "CHỜ CHẠY";});
  }

  function finishWithError(error) {
    writeConsole(`[${time()}] Lỗi runner\n${error.message}`);
    activeRun = null;
    setRunning(false);
    $("#run-state").textContent = "LỖI";
  }

  function writeConsole(text) { $("#console-output").textContent = text; }
  function time() { return new Date().toLocaleTimeString("vi-VN", {hour12:false}); }
  function escapeHtml(value) { const node = document.createElement("div"); node.textContent = String(value); return node.innerHTML; }

  $$(".nav-item").forEach(button => button.addEventListener("click", () => showView(button.dataset.target)));
  $$('[data-target-jump]').forEach(button => button.addEventListener("click", () => showView(button.dataset.targetJump)));
  $$(".feature-item,.coverage-row").forEach(row => row.addEventListener("click", () => {selectModule(row.dataset.module); if(row.classList.contains("coverage-row")) showView("modules");}));
  $$(".run-all").forEach(button => button.addEventListener("click", () => startRun("all")));
  $$(".run-selected").forEach(button => button.addEventListener("click", () => startRun(selected.moduleId)));
  $("#feature-search").addEventListener("input", filterFeatureList);
  $("#case-search").addEventListener("input", filterCases);
  $("#case-group-filter").addEventListener("change", filterCases);
  $("#case-status-filter").addEventListener("change", filterCases);
  $("#test-case-body").addEventListener("click", event => {
    const row = event.target.closest(".test-case-row");
    if (!row || event.target.matches("input")) return;
    openCase(row.dataset.case);
  });
  $("#test-case-body").addEventListener("change", event => { if (event.target.matches(".case-check")) updateCheckedCount(); });
  $("#select-all-cases").addEventListener("change", event => { $$(".test-case-row:not([hidden]) .case-check").forEach(check => check.checked = event.target.checked); updateCheckedCount(); });
  $("#close-case").addEventListener("click", closeCase);
  $("#case-backdrop").addEventListener("click", closeCase);
  $("#run-case").addEventListener("click", () => { closeCase(); startRun(selected.moduleId); });
  $("#run-checked").addEventListener("click", () => { if ($$(".case-check:checked").length) startRun(selected.moduleId); });
  $("#add-suite").addEventListener("click", event => { const old = event.currentTarget.textContent; event.currentTarget.textContent = "✓ Đã thêm vào Test Suite"; setTimeout(() => event.currentTarget.textContent = old, 1400); });
  $("#source-module").addEventListener("change", event => selectModule(event.target.value));
  $("#clear-console").addEventListener("click", () => writeConsole(""));
  if (selected) selectModule(selected.moduleId);
})();
