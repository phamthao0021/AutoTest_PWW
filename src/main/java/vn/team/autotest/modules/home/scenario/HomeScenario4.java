package vn.team.autotest.modules.home.scenario;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.openqa.selenium.Dimension;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

import vn.team.autotest.modules.home.page.HomePage;
import vn.team.autotest.modules.home.page.HomePage.HomeCard;

public class HomeScenario4 extends HomeBaseScenario {

    private static final String HOME_URL =
            "https://learnenglish.plt.pro.vn/";

    private static final int WIDTH = 1366;
    private static final int HEIGHT = 768;

    // ID | Khu vực | Kiểm tra | Expected | Actual | Status
    private static final int[] COLUMN_WIDTHS = {
            13, 18, 26, 44, 44, 6
    };

    // =========================================================
    // EXPECTED TRONG CODE - KHÔNG DÙNG CSV
    // =========================================================

    @DataProvider(name = "homeContentData", parallel = false)
    public Object[][] homeContentData() {

        return new Object[][] {

                {
                        "KB-04-001",
                        "STATISTICS",
                        List.of(
                                "15,000+",
                                "50,000+",
                                "98%",
                                "4.9/5"
                        )
                },

                {
                        "KB-04-002",
                        "AUDIENCE",
                        List.of(
                                "Học Sinh & Sinh Viên",
                                "Người Đi Làm & Công Sở",
                                "Người Mới Bắt Đầu / Mất Gốc"
                        )
                },

                {
                        "KB-04-003",
                        "BENEFIT",
                        List.of(
                                "Tiết Kiệm Thời Gian, Tối Ưu Kết Quả",
                                "Quản Lý Tiến Độ Trực Quan",
                                "Kho Chủ Đề Đa Dạng & Phong Phú",
                                "Duy Trì Động Lực Học Hàng Ngày"
                        )
                },

                {
                        "KB-04-004",
                        "MODE",
                        List.of(
                                "Flashcards (Thẻ Lật)",
                                "Quiz Trắc Nghiệm",
                                "Game Nối Từ (Match)"
                        )
                }
        };
    }

    // =========================================================
    // PWW_AUTO_04
    // =========================================================

    @Test(
        dataProvider = "homeContentData",
        description = "PWW_AUTO_04 - Guest - Home Content Validation",
        groups = {
                "home",
                "thao",
                "functional",
                "positive",
                "data-driven",
                "high"
        }
    )
    public void PWW_AUTO_04_verifyContent(
            String caseId,
            String group,
            List<String> expected) {

        SoftAssert softly = new SoftAssert();

        printHeader(caseId, screenName(group));

        try {
            // -------------------------------------------------
            // 1. MỞ TRANG CHỦ
            // -------------------------------------------------

            driver().manage().window()
                    .setSize(new Dimension(WIDTH, HEIGHT));

            open("/");

            HomePage homePage = new HomePage(driver());

            homePage.waitUntilHeroReady();

            String actualUrl = driver().getCurrentUrl();

            boolean homePass = isHomeUrl(actualUrl);

            checkpoint(
                    softly,
                    caseId,
                    group,
                    "URL Trang chủ",
                    HOME_URL,
                    actualUrl,
                    homePass
            );

            if (!homePass) {
                throw new IllegalStateException(
                        "Không ở đúng Trang chủ: " + actualUrl
                );
            }

            // -------------------------------------------------
            // 2. ĐỌC TOÀN BỘ CARD TỪ DOM
            // HomePage scroll từng card trước khi đọc.
            // Không dùng Expected để tìm Actual.
            // -------------------------------------------------

            List<HomeCard> actual =
                    homePage.readHomeCards(group);

            // -------------------------------------------------
            // 3. SO SỐ LƯỢNG
            // -------------------------------------------------

            checkpoint(
                    softly,
                    caseId,
                    group,
                    "Số lượng card",
                    expected.size(),
                    actual.size(),
                    expected.size() == actual.size()
            );

            // -------------------------------------------------
            // 4. SO TỪNG DÒNG
            // Giữ kiểm tra cả card thiếu hoặc dư.
            // -------------------------------------------------

            int totalRows = Math.max(
                    expected.size(),
                    actual.size()
            );

            Set<String> seenTexts = new HashSet<>();

            for (int i = 0; i < totalRows; i++) {

                int index = i + 1;

                boolean hasExpected = i < expected.size();
                boolean hasActual = i < actual.size();

                String expectedText = hasExpected
                        ? expected.get(i)
                        : "<KHÔNG CÓ EXPECTED>";

                String actualText = hasActual
                        ? actual.get(i).text()
                        : "<THIẾU CARD>";

                boolean textPass =
                        hasExpected
                        && hasActual
                        && normalize(expectedText)
                                .equals(normalize(actualText));

                checkpoint(
                        softly,
                        caseId,
                        group,
                        "Dòng " + index + " - Text",
                        expectedText,
                        actualText,
                        textPass
                );

                if (!hasActual) {
                    continue;
                }

                HomeCard card = actual.get(i);

                // Đúng một node title/value.
                checkpoint(
                        softly,
                        caseId,
                        group,
                        "Dòng " + index + " - Node",
                        "Đúng 1 node title/value",
                        card.titleNodeCount(),
                        card.titleNodeCount() == 1
                );

                // Nội dung không rỗng.
                boolean nonEmpty =
                        !normalize(card.text()).isBlank()
                        && !normalize(card.body()).isBlank();

                checkpoint(
                        softly,
                        caseId,
                        group,
                        "Dòng " + index + " - Nội dung",
                        "Title/value và card không rỗng",
                        normalize(card.text()).isBlank()
                                ? "<TITLE/VALUE RỖNG>"
                                : card.text(),
                        nonEmpty
                );

                // Card hiển thị.
                checkpoint(
                        softly,
                        caseId,
                        group,
                        "Dòng " + index + " - Hiển thị",
                        "displayed=true",
                        "displayed=" + card.displayed(),
                        card.displayed()
                );

                // Card nằm trong viewport sau khi scroll.
                checkpoint(
                        softly,
                        caseId,
                        group,
                        "Dòng " + index + " - Scroll",
                        "Card giao với viewport",
                        "inViewport=" + card.inViewport(),
                        card.inViewport()
                );

                // Không trùng title/value trong cùng nhóm.
                String normalizedText = normalize(card.text());

                boolean unique =
                        !normalizedText.isBlank()
                        && seenTexts.add(normalizedText);

                checkpoint(
                        softly,
                        caseId,
                        group,
                        "Dòng " + index + " - Trùng",
                        "Title/value không trùng",
                        normalizedText,
                        unique
                );
            }

            // -------------------------------------------------
            // 5. TRẠNG THÁI CUỐI
            // -------------------------------------------------

            String finalUrl = driver().getCurrentUrl();

            checkpoint(
                    softly,
                    caseId,
                    group,
                    "URL cuối",
                    "Vẫn ở Trang chủ",
                    finalUrl,
                    isHomeUrl(finalUrl)
            );

            boolean errorPage = homePage.isErrorPage();

            checkpoint(
                    softly,
                    caseId,
                    group,
                    "Lỗi trang",
                    "Không phát hiện trang lỗi",
                    errorPage
                            ? "Phát hiện dấu hiệu trang lỗi"
                            : "Không phát hiện trang lỗi",
                    !errorPage
            );

        } catch (Exception ex) {

            checkpoint(
                    softly,
                    caseId,
                    group,
                    "Thực thi",
                    "Đọc DOM và đối chiếu thành công",
                    ex.getClass().getSimpleName()
                            + ": " + ex.getMessage(),
                    false
            );
        }

        // -----------------------------------------------------
        // KẾT QUẢ TESTNG
        // Không dùng softly.getErrors().
        // -----------------------------------------------------

        System.out.println();

        try {

            softly.assertAll(
                    caseId + " - " + screenName(group)
            );

            System.out.println(
                    caseId + " RESULT: PASS"
            );

        } catch (AssertionError error) {

            System.out.println(
                    caseId + " RESULT: FAIL"
            );

            System.out.println(error.getMessage());

            throw error;
        }
    }

    // =========================================================
    // HELPERS
    // =========================================================

    private String normalize(String value) {

        return HomePage.normalizeHomeContent(value);
    }

    private boolean isHomeUrl(String value) {

        if (value == null || value.isBlank()) {
            return false;
        }

        try {

            URI uri = URI.create(value);
            String path = uri.getPath();

            return "https".equalsIgnoreCase(uri.getScheme())
                    && "learnenglish.plt.pro.vn"
                            .equalsIgnoreCase(uri.getHost())
                    && (path == null
                            || path.isEmpty()
                            || path.equals("/")
                            || path.equals("/index.php"));

        } catch (IllegalArgumentException ex) {

            return false;
        }
    }

    private String screenName(String group) {

        return switch (group) {

            case "STATISTICS" -> "Trang chủ - Thống kê";
            case "AUDIENCE" -> "Trang chủ - Đối tượng";
            case "BENEFIT" -> "Trang chủ - Lợi ích";
            case "MODE" -> "Trang chủ - Dạng bài tập";

            default -> "Trang chủ";
        };
    }

    // =========================================================
    // CONSOLE TABLE
    // =========================================================

    private void checkpoint(
            SoftAssert softly,
            String caseId,
            String group,
            String check,
            Object expected,
            Object actual,
            boolean passed) {

        printRow(
                caseId,
                group,
                check,
                String.valueOf(expected),
                String.valueOf(actual),
                passed ? "PASS" : "FAIL"
        );

        softly.assertTrue(
                passed,
                caseId + " | " + group
                        + " | " + check
                        + " | Expected: " + expected
                        + " | Actual: " + actual
        );
    }

    private void printHeader(
            String caseId,
            String screen) {

        System.out.println();

        System.out.println(
                "PWW_AUTO_04 | Guest | "
                        + caseId + " | " + screen
        );

        printSeparator();

        printRow(
                "ID",
                "Khu vực",
                "Kiểm tra",
                "Expected",
                "Actual",
                "Status"
        );
    }

    private void printRow(String... values) {

        List<List<String>> columns = new ArrayList<>();

        int maxLines = 1;

        for (int i = 0; i < COLUMN_WIDTHS.length; i++) {

            List<String> lines =
                    wrapText(values[i], COLUMN_WIDTHS[i]);

            columns.add(lines);

            maxLines = Math.max(
                    maxLines,
                    lines.size()
            );
        }

        for (int line = 0; line < maxLines; line++) {

            StringBuilder output = new StringBuilder("|");

            for (int column = 0;
                    column < COLUMN_WIDTHS.length;
                    column++) {

                List<String> lines = columns.get(column);

                String text = line < lines.size()
                        ? lines.get(line)
                        : "";

                output.append(" ")
                        .append(text)
                        .append(
                                " ".repeat(
                                        COLUMN_WIDTHS[column]
                                                - text.length()
                                )
                        )
                        .append(" |");
            }

            System.out.println(output);
        }

        printSeparator();
    }

    private List<String> wrapText(
            String value,
            int width) {

        String text = normalize(value);
        List<String> lines = new ArrayList<>();

        if (text.isEmpty()) {
            lines.add("");
            return lines;
        }

        while (text.length() > width) {

            int cut = text.lastIndexOf(' ', width);

            if (cut <= 0) {
                cut = width;
            }

            lines.add(text.substring(0, cut));

            text = text.substring(cut).trim();
        }

        if (!text.isEmpty()) {
            lines.add(text);
        }

        return lines;
    }

    private void printSeparator() {

        StringBuilder output = new StringBuilder("+");

        for (int width : COLUMN_WIDTHS) {

            output.append(
                    "-".repeat(width + 2)
            ).append("+");
        }

        System.out.println(output);
    }
}