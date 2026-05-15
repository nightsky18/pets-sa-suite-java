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

    // ── Directorios de salida ─────────────────────────────────────────────────
    private static final String REPORTS_DIR     = "reports";
    private static final String VIDEOS_DIR      = REPORTS_DIR + "/videos";
    private static final String SCREENSHOTS_DIR = REPORTS_DIR + "/screenshots";

    // Subdirectorio específico de este test: reports/screenshots/{testName}/
    private Path testScreenshotDir;

    private static final String FFMPEG_PATH =
        "C:\\Users\\bolis\\Downloads\\ffmpeg-8.1.1-essentials_build\\bin\\ffmpeg.exe";

    // ── Setup ─────────────────────────────────────────────────────────────────

    @BeforeEach
    void setUp(TestInfo testInfo) throws Exception {
        Files.createDirectories(Paths.get(VIDEOS_DIR));
        Files.createDirectories(Paths.get(SCREENSHOTS_DIR));

        testName = testInfo.getDisplayName()
                .replaceAll("[^a-zA-Z0-9_\\-]", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "");  // quitar guiones al inicio/fin

        // Crear carpeta exclusiva para los screenshots de este test
        // Resultado: reports/screenshots/ACC_VET_CLI___Veterinario_intenta.../
        testScreenshotDir = Paths.get(SCREENSHOTS_DIR, testName);
        Files.createDirectories(testScreenshotDir);

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
            "-f",         "gdigrab",
            "-framerate", "15",
            "-i",         "desktop",
            "-vcodec",    "libx264",
            "-preset",    "ultrafast",
            "-pix_fmt",   "yuv420p",
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
                if (!ended) ffmpegProcess.destroyForcibly();
            } catch (Exception e) {
                ffmpegProcess.destroyForcibly();
            }
        }
    }

    // ── Screenshots organizados por carpeta de test ───────────────────────────

    /**
     * Guarda el screenshot dentro de la carpeta exclusiva del test actual.
     *
     * Estructura resultante:
     * reports/
     * └── screenshots/
     *     ├── ACC_VET_CLI__Veterinario_intenta_acceder_a_clientes__/
     *     │   ├── 01_login_exitoso__20260515_133000.png
     *     │   ├── 02_intento_acceso__20260515_133010.png
     *     │   └── 03_resultado_acceso__20260515_133015.png
     *     └── C_I_05__XSS_y_SQLi_en_formulario_Clientes__/
     *         ├── 01_formulario_abierto__20260515_132000.png
     *         ├── 02_campos_rellenos__20260515_132010.png
     *         └── 03_resultado_final__20260515_132020.png
     *
     * @param label nombre descriptivo del momento (ej: "formulario_abierto")
     */
    protected void takeScreenshot(String label) {
        if (screenshotCount >= MAX_SCREENSHOTS) return;

        try {
            File src = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);

            String timestamp = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));

            // Nombre del archivo: 01_label__timestamp.png
            String fileName = String.format("%02d_%s__%s.png",
                    screenshotCount + 1, label, timestamp);

            // Destino: reports/screenshots/{testName}/{fileName}
            Path dest = testScreenshotDir.resolve(fileName);
            Files.copy(src.toPath(), dest, StandardCopyOption.REPLACE_EXISTING);

            screenshotCount++;
            System.out.println("[Screenshot " + screenshotCount + "/3] → " + dest);

        } catch (Exception e) {
            System.err.println("[WARN] Screenshot fallido: " + label + " — " + e.getMessage());
        }
    }
}