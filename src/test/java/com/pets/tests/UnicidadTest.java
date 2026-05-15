package com.pets.tests;

import com.pets.base.BaseTest;
import com.pets.utils.ExcelReader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.Select;

import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Escenario 3 — Validación de unicidad de cédula e ID de mascota
 * Valida RF24: el sistema no permite registrar dos clientes con la misma cédula.
 * Valida RF25: el sistema no permite registrar dos mascotas con el mismo ID.
 *
 * Dataset: ESC03_UNICIDAD
 *   C-I-01 → intento de crear cliente con cédula duplicada (1012345678)
 *   M-I-01 → intento de crear mascota con ID duplicado (MASC-001)
 *
 * Precondición C-I-01: cliente con cédula 1012345678 debe existir previamente.
 * Precondición M-I-01: mascota con ID MASC-001 debe existir previamente.
 *
 * Screenshots por test (3):
 *   1. formulario_abierto   — formulario vacío antes de ingresar datos
 *   2. campos_rellenos      — datos duplicados visibles en el formulario
 *   3. resultado_unicidad   — respuesta del sistema (error esperado)
 */
public class UnicidadTest extends BaseTest {

    private static final String BASE_URL = "https://eyderalexis26.pythonanywhere.com";
    private static final String SHEET    = "ESC03_UNICIDAD";

    // ── Utilidad: login ───────────────────────────────────────────────────────

    private void login(String username, String password, WebDriverWait wait) {
        driver.get(BASE_URL + "/login/");
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("id_username")));
        driver.findElement(By.id("id_username")).sendKeys(username);
        driver.findElement(By.id("id_password")).sendKeys(password);
        driver.findElement(By.cssSelector("button.btn-green")).click();
        wait.until(ExpectedConditions.not(
                ExpectedConditions.urlContains("/login/")));
    }

    // ── Utilidad: resolver locator ────────────────────────────────────────────

    private By resolveLocator(String locatorStr) {
        if (locatorStr.startsWith("id:"))  return By.id(locatorStr.substring(3));
        if (locatorStr.startsWith("css:")) return By.cssSelector(locatorStr.substring(4));
        throw new IllegalArgumentException("Locator no reconocido: " + locatorStr);
    }

    // ── Utilidad: llenar campo select o texto según el elemento encontrado ─────

    private void fillField(By locator, String value) {
        if (value == null || value.isBlank()) return;
        WebElement el = driver.findElement(locator);
        String tag = el.getTagName().toLowerCase();
        if (tag.equals("select")) {
            new Select(el).selectByVisibleText(value);
        } else {
            el.clear();
            el.sendKeys(value);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // TEST 1 — Unicidad de cédula en Clientes (C-I-01)
    // Precondición: cliente con cédula 1012345678 debe existir en la BD.
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("C-I-01 | Cédula duplicada en Clientes — sistema debe rechazar (RF24)")
    void testUnicidadCedulaCliente() throws Exception {
        Map<String, String> data = ExcelReader.getRowById(SHEET, "C-I-01");
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        login(data.get("${username}"), data.get("${password}"), wait);

        // Navegar al formulario de nuevo cliente
        driver.get(BASE_URL + "/clientes/");
        wait.until(ExpectedConditions.urlContains("/clientes/"));
        driver.findElement(By.cssSelector("a[href*='nuevo'], a[href*='crear'], button.btn-green"))
              .click();

        wait.until(ExpectedConditions.visibilityOfElementLocated(
                resolveLocator(data.get("${campo_f1_locator}"))));

        // Screenshot 1/3 — formulario vacío
        takeScreenshot("formulario_abierto");

        // Ingresar datos con cédula duplicada
        fillField(resolveLocator(data.get("${campo_f1_locator}")), data.get("${cedula}"));
        fillField(resolveLocator(data.get("${campo_f2_locator}")), data.get("${nombres}"));
        fillField(resolveLocator(data.get("${campo_f3_locator}")), data.get("${apellidos}"));
        fillField(resolveLocator(data.get("${campo_f4_locator}")), data.get("${telefono}"));
        fillField(resolveLocator(data.get("${campo_f5_locator}")), data.get("${direccion}"));

        // Screenshot 2/3 — cédula duplicada visible en el formulario
        takeScreenshot("campos_rellenos");

        driver.findElement(resolveLocator(data.get("${campo_submit_locator}"))).click();

        // Esperar mensaje de error o permanecer en el formulario
        try {
            wait.until(ExpectedConditions.visibilityOfElementLocated(
                    resolveLocator(data.get("${campo_error_locator}"))));
        } catch (Exception ignored) {}

        // Screenshot 3/3 — respuesta del sistema
        takeScreenshot("resultado_unicidad");

        // ── Asserts ───────────────────────────────────────────────────────────

        String expectedError = data.get("${expected_error_text}");

        boolean errorVisible = false;
        String mensajeReal   = "";
        try {
            WebElement errorEl = driver.findElement(
                    resolveLocator(data.get("${campo_error_locator}")));
            errorVisible = errorEl.isDisplayed();
            mensajeReal  = errorEl.getText().trim();
        } catch (Exception ignored) {}

        // El sistema no debe haber navegado al listado (el form debe rechazar)
        boolean formularioRechazado = !driver.getCurrentUrl().equals(BASE_URL + "/clientes/")
                || errorVisible;

        final boolean errorVisibleFinal       = errorVisible;
        final boolean formularioRechazadoFinal = formularioRechazado;
        final String  mensajeRealFinal        = mensajeReal;
        final String  expectedErrorFinal      = expectedError;

        assertAll("Unicidad cédula (C-I-01) — RF24",
            () -> assertTrue(formularioRechazadoFinal,
                    "[FALLO] El sistema aceptó una cédula duplicada. " +
                    "El formulario debió rechazar el envío."),
            () -> assertTrue(errorVisibleFinal,
                    "[FALLO] No se mostró ningún mensaje de error por cédula duplicada."),
            () -> {
                if (errorVisibleFinal) {
                    assertTrue(
                        mensajeRealFinal.toLowerCase().contains("existe") ||
                        mensajeRealFinal.toLowerCase().contains("cédula") ||
                        mensajeRealFinal.toLowerCase().contains("duplicad"),
                        "[FALLO] Mensaje de error no coincide con el esperado." +
                        "\n  Esperado: " + expectedErrorFinal +
                        "\n  Real:     " + mensajeRealFinal
                    );
                }
            }
        );
    }

    // ══════════════════════════════════════════════════════════════════════════
    // TEST 2 — Unicidad de ID en Mascotas (M-I-01)
    // Precondición: mascota con ID MASC-001 debe existir en la BD.
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("M-I-01 | ID de mascota duplicado — sistema debe rechazar (RF25)")
    void testUnicidadIdMascota() throws Exception {
        Map<String, String> data = ExcelReader.getRowById(SHEET, "M-I-01");
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        login(data.get("${username}"), data.get("${password}"), wait);

        // Navegar al formulario de nueva mascota
        driver.get(BASE_URL + "/mascotas/");
        wait.until(ExpectedConditions.urlContains("/mascotas/"));
        driver.findElement(By.cssSelector("a[href*='nueva'], a[href*='crear'], button.btn-green"))
              .click();

        wait.until(ExpectedConditions.visibilityOfElementLocated(
                resolveLocator(data.get("${campo_f1_locator}"))));

        // Screenshot 1/3 — formulario vacío
        takeScreenshot("formulario_abierto");

        // Ingresar datos con ID duplicado
        // campo_f1 → id_identificacion (MASC-001)
        fillField(resolveLocator(data.get("${campo_f1_locator}")), data.get("${pet_id}"));
        fillField(resolveLocator(data.get("${campo_f2_locator}")), data.get("${nombre_mascota}"));
        fillField(resolveLocator(data.get("${campo_f3_locator}")), data.get("${raza}"));
        fillField(resolveLocator(data.get("${campo_f4_locator}")), data.get("${edad}"));
        fillField(resolveLocator(data.get("${campo_f5_locator}")), data.get("${peso}"));

        // campo_cliente es un <select> con texto visible del dueño
        if (data.get("${campo_cliente_locator}") != null &&
            !data.get("${campo_cliente_locator}").isBlank()) {
            fillField(resolveLocator(data.get("${campo_cliente_locator}")),
                      data.get("${cliente_id}"));
        }

        // Screenshot 2/3 — ID duplicado visible en el formulario
        takeScreenshot("campos_rellenos");

        driver.findElement(resolveLocator(data.get("${campo_submit_locator}"))).click();

        // Esperar mensaje de error
        try {
            wait.until(ExpectedConditions.visibilityOfElementLocated(
                    resolveLocator(data.get("${campo_error_locator}"))));
        } catch (Exception ignored) {}

        // Screenshot 3/3 — respuesta del sistema
        takeScreenshot("resultado_unicidad");

        // ── Asserts ───────────────────────────────────────────────────────────

        String expectedError = data.get("${expected_error_text}");

        boolean errorVisible = false;
        String mensajeReal   = "";
        try {
            WebElement errorEl = driver.findElement(
                    resolveLocator(data.get("${campo_error_locator}")));
            errorVisible = errorEl.isDisplayed();
            mensajeReal  = errorEl.getText().trim();
        } catch (Exception ignored) {}

        boolean formularioRechazado = !driver.getCurrentUrl().equals(BASE_URL + "/mascotas/")
                || errorVisible;

        final boolean errorVisibleFinal        = errorVisible;
        final boolean formularioRechazadoFinal = formularioRechazado;
        final String  mensajeRealFinal         = mensajeReal;
        final String  expectedErrorFinal       = expectedError;

        assertAll("Unicidad ID mascota (M-I-01) — RF25",
            () -> assertTrue(formularioRechazadoFinal,
                    "[FALLO] El sistema aceptó un ID de mascota duplicado. " +
                    "El formulario debió rechazar el envío."),
            () -> assertTrue(errorVisibleFinal,
                    "[FALLO] No se mostró ningún mensaje de error por ID duplicado."),
            () -> {
                if (errorVisibleFinal) {
                    assertTrue(
                        mensajeRealFinal.toLowerCase().contains("existe") ||
                        mensajeRealFinal.toLowerCase().contains("identificaci") ||
                        mensajeRealFinal.toLowerCase().contains("duplicad"),
                        "[FALLO] Mensaje de error no coincide con el esperado." +
                        "\n  Esperado: " + expectedErrorFinal +
                        "\n  Real:     " + mensajeRealFinal
                    );
                }
            }
        );
    }
}