package com.pets.tests;

import com.pets.base.BaseTest;
import com.pets.utils.ExcelReader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.NoAlertPresentException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Escenario 1 — Inyección XSS y SQLi en campos de texto
 * Valida RNF04: el sistema debe rechazar o escapar payloads maliciosos.
 * Dataset: ESC01_XSS_SQLI → C-I-05 (Clientes), MED-I-03 (Medicamentos)
 * BUG-006 y BUG-007 están ABIERTOS: los asserts documentan el comportamiento
 * real sin detener la ejecución (assertAll).
 *
 * Screenshots por test (3 fijos):
 *   1. formulario_abierto  — antes de ingresar datos
 *   2. campos_rellenos     — payloads visibles en el formulario
 *   3. resultado_final     — respuesta de la aplicación tras submit
 */
public class XssSqliTest extends BaseTest {

    private static final String BASE_URL = "https://eyderalexis26.pythonanywhere.com";
    private static final String SHEET    = "ESC01_XSS_SQLI";

    // ── Utilidad: login (sin screenshot — no es parte del escenario) ──────────

    private void login(String username, String password) {
        driver.get(BASE_URL + "/login/");
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("id_username")));
        driver.findElement(By.id("id_username")).sendKeys(username);
        driver.findElement(By.id("id_password")).sendKeys(password);
        driver.findElement(By.cssSelector("button.btn-green")).click();
        // Esperar redirección post-login antes de continuar
        wait.until(ExpectedConditions.not(
                ExpectedConditions.urlContains("/login/")));
    }

    // ── Utilidad: verificar alert JS ──────────────────────────────────────────

    private boolean alertPresent() {
        try {
            Alert alert = driver.switchTo().alert();
            String alertText = alert.getText();
            alert.dismiss();
            System.out.println("[WARN] Alert JS ejecutado: " + alertText);
            return true;
        } catch (NoAlertPresentException e) {
            return false;
        }
    }

    // ── Utilidad: resolver locator ────────────────────────────────────────────

    private By resolveLocator(String locatorStr) {
        if (locatorStr.startsWith("id:"))  return By.id(locatorStr.substring(3));
        if (locatorStr.startsWith("css:")) return By.cssSelector(locatorStr.substring(4));
        throw new IllegalArgumentException("Locator no reconocido: " + locatorStr);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // TEST 1 — XSS + SQLi en módulo Clientes (C-I-05 / BUG-006)
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("C-I-05 | XSS y SQLi en formulario Clientes (BUG-006 ABIERTO)")
    void testXssSqliClientes() throws Exception {
        Map<String, String> data = ExcelReader.getRowById(SHEET, "C-I-05");

        login(data.get("${username}"), data.get("${password}"));

        // Navegar al formulario de nuevo cliente
        driver.get(BASE_URL + "/clientes/");
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        wait.until(ExpectedConditions.urlContains("/clientes/"));

        driver.findElement(By.cssSelector("a[href*='nuevo'], a[href*='crear'], button.btn-green"))
              .click();

        wait.until(ExpectedConditions.visibilityOfElementLocated(
                resolveLocator(data.get("${campo_f1_locator}"))));

        // Screenshot 1/3 — formulario vacío antes de ingresar payloads
        takeScreenshot("formulario_abierto");

        // Ingresar payloads del dataset
        driver.findElement(resolveLocator(data.get("${campo_f1_locator}")))
              .sendKeys(data.get("${cedula}"));
        driver.findElement(resolveLocator(data.get("${campo_f2_locator}")))
              .sendKeys(data.get("${nombres}"));
        driver.findElement(resolveLocator(data.get("${campo_f3_locator}")))
              .sendKeys(data.get("${apellidos}"));
        driver.findElement(resolveLocator(data.get("${campo_f4_locator}")))
              .sendKeys(data.get("${telefono}"));
        driver.findElement(resolveLocator(data.get("${campo_f5_locator}")))
              .sendKeys(data.get("${direccion}"));

        // Screenshot 2/3 — payloads visibles en el formulario
        takeScreenshot("campos_rellenos");

        driver.findElement(resolveLocator(data.get("${campo_submit_locator}"))).click();

        // Dar tiempo a la página para responder antes del screenshot final
        try { Thread.sleep(1500); } catch (InterruptedException ignored) {}

        boolean xssEjecutado = alertPresent();

        boolean registroGuardado = false;
        try {
            wait.until(ExpectedConditions.presenceOfElementLocated(
                    resolveLocator(data.get("${campo_listado_locator}"))));
            registroGuardado = driver.getPageSource().contains(data.get("${cedula}"));
        } catch (Exception ignored) {}

        boolean errorMostrado = false;
        try {
            WebElement errorEl = driver.findElement(
                    resolveLocator(data.get("${campo_error_locator}")));
            errorMostrado = errorEl.isDisplayed();
        } catch (Exception ignored) {}

        // Screenshot 3/3 — estado final de la página tras el submit
        takeScreenshot("resultado_final");

        final boolean xssEjecutadoFinal     = xssEjecutado;
        final boolean registroGuardadoFinal = registroGuardado;
        final boolean errorMostradoFinal    = errorMostrado;

        assertAll("XSS/SQLi en Clientes (C-I-05) — BUG-006 ABIERTO",
            () -> assertFalse(xssEjecutadoFinal,
                    "[FALLO SEGURIDAD] El payload XSS ejecutó un alert(). BUG-006"),
            () -> assertFalse(registroGuardadoFinal,
                    "[FALLO SEGURIDAD] El registro con payload se guardó en la tabla. BUG-006"),
            () -> assertTrue(errorMostradoFinal || !registroGuardadoFinal,
                    "[INFO] Se esperaba mensaje de error o rechazo del formulario. BUG-006")
        );
    }

    // ══════════════════════════════════════════════════════════════════════════
    // TEST 2 — XSS + SQLi en módulo Medicamentos (MED-I-03 / BUG-007)
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("MED-I-03 | XSS y SQLi en formulario Medicamentos (BUG-007 ABIERTO)")
    void testXssSqliMedicamentos() throws Exception {
        Map<String, String> data = ExcelReader.getRowById(SHEET, "MED-I-03");

        login(data.get("${username}"), data.get("${password}"));

        // Navegar al formulario de nuevo medicamento
        driver.get(BASE_URL + "/medicamentos/");
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        wait.until(ExpectedConditions.urlContains("/medicamentos/"));

        driver.findElement(By.cssSelector("a[href*='nuevo'], a[href*='crear'], button.btn-green"))
              .click();

        wait.until(ExpectedConditions.visibilityOfElementLocated(
                resolveLocator(data.get("${campo_f1_locator}"))));

        // Screenshot 1/3 — formulario vacío
        takeScreenshot("formulario_abierto");

        // Ingresar payloads
        driver.findElement(resolveLocator(data.get("${campo_f1_locator}")))
              .sendKeys(data.get("${med_nombre}"));
        driver.findElement(resolveLocator(data.get("${campo_f2_locator}")))
              .sendKeys(data.get("${med_descripcion}"));
        driver.findElement(resolveLocator(data.get("${campo_f3_locator}")))
              .sendKeys(data.get("${med_dosis}"));

        // Screenshot 2/3 — payloads visibles
        takeScreenshot("campos_rellenos");

        driver.findElement(resolveLocator(data.get("${campo_submit_locator}"))).click();

        try { Thread.sleep(1500); } catch (InterruptedException ignored) {}

        boolean xssEjecutado = alertPresent();

        boolean registroGuardado = false;
        try {
            wait.until(ExpectedConditions.presenceOfElementLocated(
                    resolveLocator(data.get("${campo_listado_locator}"))));
            // Verificar presencia literal de la etiqueta <script> en el HTML renderizado
            registroGuardado = driver.getPageSource().contains("<script>");
        } catch (Exception ignored) {}

        // Screenshot 3/3 — resultado final
        takeScreenshot("resultado_final");

        final boolean xssEjecutadoFinal     = xssEjecutado;
        final boolean registroGuardadoFinal = registroGuardado;

        assertAll("XSS/SQLi en Medicamentos (MED-I-03) — BUG-007 ABIERTO",
            () -> assertFalse(xssEjecutadoFinal,
                    "[FALLO SEGURIDAD] El payload XSS ejecutó un alert(). BUG-007"),
            () -> assertFalse(registroGuardadoFinal,
                    "[FALLO SEGURIDAD] El payload se guardó en la base de datos. BUG-007")
        );
    }
}