package com.pets.base;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class BaseTest {

    protected WebDriver driver;

    private Process ffmpegProcess;
    private Path videoPath;

    private int screenshotCount = 0;
    private static final int MAX_SCREENSHOTS = 3;

    protected String testName;

    private static final String REPORTS_DIR     = "reports";
    private static final String VIDEOS_DIR      = REPORTS_DIR + "/videos";
    private static final String SCREENSHOTS_DIR = REPORTS_DIR + "/screenshots";

    // ── Ruta absoluta de FFmpeg — ajustar si cambia de ubicación ─────────────
    private static final String FFMPEG_PATH =
        "C:\\Users\\bolis\\Downloads\\ffmpeg-8.1.1-essentials_build\\bin\\ffmpeg.exe";

    // ── Setup ─────────────────────────────────────────────────────────────────

    @BeforeEach
    void setUp(TestInfo testInfo) throws Exception {
        Files.createDirectories(Paths.get(VIDEOS_DIR));
        Files.createDirectories(Paths.get(SCREENSHOTS_DIR));

        testName = testInfo.getDisplayName()
                .replaceAll("[^a-zA-Z0-9_\\-]", "_")
                .replaceAll("_+", "_");

        screenshotCount = 0;

        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.setExperimentalOption("excludeSwitches", new String[]{"enable-automation"});
        driver = new ChromeDriver(options);
        driver.manage().window().maximize();

        startRecording();
    }

    // ── Teardown ──────────────────────────────────────────────────────────────

    @AfterEach
    void tearDown() {
        stopRecording();
        if (driver != null) {
            driver.quit();
        }
    }

    // ── Grabación de video con FFmpeg ─────────────────────────────────────────

    private void startRecording() throws IOException {
        // Verificar que el ejecutable existe antes de intentar lanzarlo
        File ffmpegExe = new File(FFMPEG_PATH);
        if (!ffmpegExe.exists()) {
            throw new IOException(
                "FFmpeg no encontrado en: " + FFMPEG_PATH +
                "\nVerifica la ruta en BaseTest.FFMPEG_PATH"
            );
        }

        String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));

        videoPath = Paths.get(VIDEOS_DIR, testName + "__" + timestamp + ".mp4");

        ProcessBuilder pb = new ProcessBuilder(
            FFMPEG_PATH,
            "-y",
            "-f",        "gdigrab",
            "-framerate","15",
            "-i",        "desktop",
            "-vcodec",   "libx264",
            "-preset",   "ultrafast",
            "-pix_fmt",  "yuv420p",
            videoPath.toAbsolutePath().toString()
        );

        pb.redirectErrorStream(true);
        pb.redirectOutput(new File(REPORTS_DIR + "/ffmpeg_log_" + testName + ".txt"));

        ffmpegProcess = pb.start();
    }

    private void stopRecording() {
        if (ffmpegProcess != null && ffmpegProcess.isAlive()) {
            try {
                ffmpegProcess.getOutputStream().write("q\n".getBytes());
                ffmpegProcess.getOutputStream().flush();

                boolean ended = ffmpegProcess.waitFor(8, java.util.concurrent.TimeUnit.SECONDS);
                if (!ended) {
                    ffmpegProcess.destroyForcibly();
                }
            } catch (Exception e) {
                ffmpegProcess.destroyForcibly();
            }
        }
    }

    // ── Screenshots ───────────────────────────────────────────────────────────

    protected void takeScreenshot(String label) {
        if (screenshotCount >= MAX_SCREENSHOTS) return;

        try {
            File src = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            String timestamp = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));

            String fileName = String.format("%02d_%s__%s.png",
                    screenshotCount + 1, label, timestamp);

            Path dest = Paths.get(SCREENSHOTS_DIR, testName + "__" + fileName);
            Files.copy(src.toPath(), dest, StandardCopyOption.REPLACE_EXISTING);

            screenshotCount++;
        } catch (Exception e) {
            System.err.println("[WARN] Screenshot fallido: " + label + " — " + e.getMessage());
        }
    }
}