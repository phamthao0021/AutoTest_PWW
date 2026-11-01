package vn.team.autotest.modules.responsive.base;

import java.io.OutputStream;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openqa.selenium.*;
import org.testng.SkipException;
import org.testng.annotations.*;
import vn.team.autotest.modules.responsive.factory.WebDriverFactory;
import vn.team.autotest.modules.responsive.page.HomePage;
import vn.team.autotest.modules.responsive.page.ResponsiveLayoutAudit;

/** Mỗi TestNG worker có ChromeDriver/Page Object riêng; không dùng static WebDriver. */
public abstract class BaseScenario {
    protected static final String BASE = "https://learnenglish.plt.pro.vn/";
    private final ThreadLocal<WebDriver> drivers = new ThreadLocal<>();
    private final ThreadLocal<HomePage> homes = new ThreadLocal<>();
    private final List<String[]> results = new CopyOnWriteArrayList<>();

    protected WebDriver driver() { return Objects.requireNonNull(drivers.get(), "Driver chưa khởi tạo"); }
    protected HomePage home() { return Objects.requireNonNull(homes.get(), "HomePage chưa khởi tạo"); }

    @DataProvider(name = "mobile")
    public Object[][] mobile() { return new Object[][] {
        {"Android 393x873", 393, 873},
        {"iPhone XR 414x896", 414, 896}
    }; }

    @FunctionalInterface protected interface Action { String perform() throws Exception; }

    protected void check(String id, String title, String device, int w, int h, Action action) {
        long start = System.nanoTime();
        String actual = "", status = "PASS", screenshot = "";
        Throwable problem = null;
        try {
            WebDriver d = WebDriverFactory.mobile(w, h);
            drivers.set(d);
            homes.set(new HomePage(d));
            actual = action.perform();
            actual += "; " + new ResponsiveLayoutAudit(driver()).inspect(id + " - " + title);
        } catch (Throwable e) {
            problem = e;
            status = e instanceof SkipException ? "SKIP" : "FAIL";
            actual = e.getClass().getSimpleName() + ": " + e.getMessage();
            WebDriver d = drivers.get();
            if (d instanceof TakesScreenshot) {
                try {
                    Path p = Paths.get("PWW_RWD_Screenshots",
                        id + "_" + device.replaceAll("[^a-zA-Z0-9]", "_") + ".png");
                    Files.createDirectories(p.getParent());
                    Files.copy(((TakesScreenshot)d).getScreenshotAs(OutputType.FILE).toPath(),
                        p, StandardCopyOption.REPLACE_EXISTING);
                    screenshot = p.toString();
                } catch (Exception ignored) { }
            }
        } finally {
            WebDriver d = drivers.get();
            try { if (d != null) d.quit(); }
            finally { drivers.remove(); homes.remove(); }
        }
        String time = String.format(Locale.US, "%.2f", (System.nanoTime() - start)/1e9);
        results.add(new String[]{id, device, title, actual, status, screenshot, time});
        System.out.println(id + " | " + device + " | " + status + " | " + actual);
        if (problem instanceof SkipException) throw (SkipException)problem;
        if (problem != null) throw new AssertionError(id + " " + device + " FAIL: " + actual, problem);
    }

    /** Tạo Chrome đúng một lần cho toàn bộ chuỗi 16 bước của một viewport. */
    protected void beginSession(int width, int height) {
        if (drivers.get() != null) throw new IllegalStateException("Chrome session đã tồn tại");
        WebDriver d = WebDriverFactory.mobile(width, height);
        drivers.set(d);
        homes.set(new HomePage(d));
    }

    /** Kết thúc Chrome sau cả nhóm User và Admin, kể cả khi có FAIL. */
    protected void endSession() {
        WebDriver d = drivers.get();
        try { if (d != null) d.quit(); }
        finally { drivers.remove(); homes.remove(); }
    }

    /** Kiểm thử TC trên Chrome hiện tại; KHÔNG tạo Chrome, KHÔNG đăng nhập, KHÔNG quit. */
    protected void checkInSession(String id, String title, String device, Action action) {
        long start = System.nanoTime();
        String actual = "", status = "PASS", screenshot = "";
        Throwable problem = null;
        try {
            actual = action.perform();
            actual += "; " + new ResponsiveLayoutAudit(driver()).inspect(id + " - " + title);
        } catch (Throwable e) {
            problem = e;
            status = e instanceof SkipException ? "SKIP" : "FAIL";
            actual = e.getClass().getSimpleName() + ": " + e.getMessage();
            WebDriver d = drivers.get();
            if (d instanceof TakesScreenshot) {
                try {
                    String unique = Long.toString(System.nanoTime());
                    Path p = Paths.get("PWW_RWD_Screenshots", id + "_"
                        + device.replaceAll("[^a-zA-Z0-9]", "_") + "_" + unique + ".png");
                    Files.createDirectories(p.getParent());
                    Files.copy(((TakesScreenshot)d).getScreenshotAs(OutputType.FILE).toPath(),
                        p, StandardCopyOption.REPLACE_EXISTING);
                    screenshot = p.toString();
                } catch (Exception ignored) { }
            }
        }
        String duration = String.format(Locale.US, "%.2f", (System.nanoTime()-start)/1e9);
        results.add(new String[]{id, device, title, actual, status, screenshot, duration});
        System.out.println(id + " | " + device + " | " + status + " | " + actual);
        if (problem instanceof SkipException) throw (SkipException)problem;
        if (problem != null) throw new AssertionError(id + " " + device + " FAIL: " + actual, problem);
    }

    protected String reportFileName() { return "PWW_Home_01_05_RWD.xlsx"; }
    protected String reportSheetName() { return "HOME_RWD_01_05"; }

    @AfterClass(alwaysRun = true)
    public void exportExcel() {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet(reportSheetName());
            Row head = sheet.createRow(0);
            String[] cols = {"STT", "TCs ID", "Device", "Title Test", "Actual",
                "Result", "Screenshot", "Duration(s)"};
            CellStyle style = wb.createCellStyle();
            style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            org.apache.poi.ss.usermodel.Font font = wb.createFont();
            font.setBold(true); font.setColor(IndexedColors.WHITE.getIndex());
            style.setFont(font);
            for (int i = 0; i < cols.length; i++) {
                Cell cell = head.createCell(i); cell.setCellValue(cols[i]); cell.setCellStyle(style);
            }
            List<String[]> sorted = new ArrayList<>(results);
            sorted.sort(Comparator.comparing((String[] a) -> a[0]).thenComparing(a -> a[1]));
            for (int i = 0; i < sorted.size(); i++) {
                Row row = sheet.createRow(i+1); row.createCell(0).setCellValue(i+1);
                for (int j = 0; j < sorted.get(i).length; j++) row.createCell(j+1).setCellValue(sorted.get(i)[j]);
            }
            int[] widths = {7,32,25,44,100,13,55,16};
            for (int i = 0; i < widths.length; i++) sheet.setColumnWidth(i, widths[i]*256);
            sheet.createFreezePane(0,1);
            try (OutputStream stream = Files.newOutputStream(Paths.get(reportFileName()))) { wb.write(stream); }
            System.out.println("[RWD EXCEL] " + Paths.get(reportFileName()).toAbsolutePath());
        } catch (Exception e) { throw new RuntimeException("Xuất Excel thất bại",e); }
    }
}