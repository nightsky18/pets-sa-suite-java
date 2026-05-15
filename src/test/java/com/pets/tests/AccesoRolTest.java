package com.pets.tests;

import com.pets.base.BaseTest;
import com.pets.utils.ExcelReader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Escenario 2 — Control de acceso por rol cruzado (acceso denegado)
 * Valida RF04: un usuario con rol X no puede acceder a módulos de otro rol.
 * Valida RNF02: el sistema debe proteger rutas según el rol autenticado.
 *
 * Dataset: ESC02_ACCESO
 *   ACC-VET-CLI  → Veterinario intenta acceder a /clientes/
 *   ACC-REC-MED  → Recepcionista intenta acceder a /medicamentos/
 *   ACC-REC-USR  → Recepcionista intenta acceder a /usuarios/
 *   ACC-VET-USR  → Veterinario intenta acceder a /usuarios/
 *
 * Screenshots por test (3):
 *   1. login_exitoso         — dashboard visible tras autenticarse
 *   2. intento_acceso        — justo antes de navegar a la URL restringida
 *   3. resultado_acceso      — respuesta de la app (denegado o no)
 */
public class AccesoRolTest extends BaseTest {

    private static final String BASE_URL = "https://eyderalexis26.pythonanywhere.com";
    private static final String SHEET    = "ESC02_ACCESO";

    // ── Utilidad: login y verificar dashboard ─────────────────────────────────

    private void login(Map<String, String> data, WebDriverWait wait) {
        driver.get(BASE_URL + data.get("${campo_url_login}"));
        wait.until(ExpectedConditions.visibilityOfElementLocated(
                resolveLocator(data.get("${campo_user_locator}"))));

        driver.findElement(resolveLocator(data.get("${campo_user_locator}")))
              .sendKeys(data.get("${username}"));
        driver.findElement(resolveLocator(data.get("${campo_pass_locator}")))
              .sendKeys(data.get("${password}"));
        driver.findElement(resolveLocator(data.get("${campo_submit_locator}"))).click();

        // Esperar que el dashboard sea visible (confirma login exitoso)
        wait.until(ExpectedConditions.presenceOfElementLocated(
                resolveLocator(data.get("${campo_dashboard_locator}"))));
    }

    // ── Utilidad: logout limpio entre tests ───────────────────────────────────

    private void logout(Map<String, String> data) {
        try {
            driver.findElement(resolveLocator(data.get("${campo_logout_locator}"))).click();
        } catch (Exception ignored) {
            // Si el logout falla, el @AfterEach cierra el navegador de todos modos
        }
    }

    // ── Utilidad: resolver locator ────────────────────────────────────────────

    private By resolveLocator(String locatorStr) {
        if (locatorStr.startsWith("id:"))  return By.id(locatorStr.substring(3));
        if (locatorStr.startsWith("css:")) return By.cssSelector(locatorStr.substring(4));
        throw new IllegalArgumentException("Locator no reconocido: " + locatorStr);
    }

    // ── Lógica central compartida por los 4 tests ─────────────────────────────

    /**
     * Ejecuta el flujo completo de un caso de acceso denegado:
     * login → screenshot dashboard → navegar a URL restringida →
     * screenshot intento → verificar denegación → screenshot resultado.
     */
    private void ejecutarCasoAccesoDenegado(String datasetId) throws Exception {
        Map<String, String> data = ExcelReader.getRowById(SHEET, datasetId);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        // Login con el usuario de rol restringido
        login(data, wait);

        // Screenshot 1/3 — dashboard del usuario autenticado
        takeScreenshot("login_exitoso");

        // Intentar acceder directamente a la URL restringida
        // (navegación directa simula acceso cruzado sin pasar por el menú)
        String targetUrl = BASE_URL + data.get("${campo_url_target}");
        String deniedUrl = BASE_URL + data.get("${campo_url_denied}");

        // Screenshot 2/3 — antes de navegar (documenta el rol activo)
        takeScreenshot("intento_acceso");

        driver.get(targetUrl);

        // Esperar a que la página responda (redirección o mensaje de error)
        try {
            wait.until(ExpectedConditions.or(
                ExpectedConditions.urlContains(data.get("${campo_url_denied}")),
                ExpectedConditions.visibilityOfElementLocated(
                        resolveLocator(data.get("${campo_error_locator}")))
            ));
        } catch (Exception e) {
            // Si ninguna condición se cumple en 10s, continuamos y el assert fallará
        }

        // Screenshot 3/3 — resultado real de la aplicación
        takeScreenshot("resultado_acceso");

        // ── Asserts ───────────────────────────────────────────────────────────

        String urlActual       = driver.getCurrentUrl();
        String expectedError   = data.get("${expected_error_text}");

        boolean redirigioADenegado = urlActual.contains(
                data.get("${campo_url_denied}").replace("/", ""));

        boolean mensajeErrorVisible = false;
        String mensajeReal = "";
        try {
            WebElement errorEl = driver.findElement(
                    resolveLocator(data.get("${campo_error_locator}")));
            mensajeErrorVisible = errorEl.isDisplayed();
            mensajeReal = errorEl.getText().trim();
        } catch (Exception ignored) {}

        // Verificar que NO se accedió al módulo restringido
        boolean accesoPermitido = urlActual.equals(targetUrl) ||
                                  urlActual.contains(data.get("${campo_url_target}").replace("/", ""));

        final boolean redirigioFinal        = redirigioADenegado;
        final boolean mensajeVisibleFinal   = mensajeErrorVisible;
        final boolean accesoPermitidoFinal  = accesoPermitido;
        final String  mensajeRealFinal      = mensajeReal;
        final String  expectedErrorFinal    = expectedError;
        final String  urlActualFinal        = urlActual;

        assertAll("Acceso denegado para " + datasetId + " [rol: " + data.get("${role}") + "]",
            () -> assertFalse(accesoPermitidoFinal,
                    "[FALLO SEGURIDAD] El sistema permitió acceso a " +
                    data.get("${campo_url_target}") +
                    " con rol " + data.get("${role}") +
                    ". URL actual: " + urlActualFinal),
            () -> assertTrue(redirigioFinal || mensajeVisibleFinal,
                    "[FALLO] No hubo redirección a /acceso-denegado/ ni mensaje de error visible. " +
                    "URL actual: " + urlActualFinal),
            () -> {
                if (mensajeVisibleFinal) {
                    assertTrue(mensajeRealFinal.contains("permisos") || mensajeRealFinal.contains("acceso"),
                            "[FALLO] Mensaje de error no contiene texto esperado." +
                            "\n  Esperado: " + expectedErrorFinal +
                            "\n  Real:     " + mensajeRealFinal);
                }
            }
        );

        logout(data);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // TEST 1 — Veterinario intenta acceder a /clientes/ (ACC-VET-CLI)
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("ACC-VET-CLI | Veterinario intenta acceder a /clientes/ — debe ser denegado")
    void testVeterinarioAccedeClientes() throws Exception {
        ejecutarCasoAccesoDenegado("ACC-VET-CLI");
    }

    // ══════════════════════════════════════════════════════════════════════════
    // TEST 2 — Recepcionista intenta acceder a /medicamentos/ (ACC-REC-MED)
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("ACC-REC-MED | Recepcionista intenta acceder a /medicamentos/ — debe ser denegado")
    void testRecepcionistaAccedeMedicamentos() throws Exception {
        ejecutarCasoAccesoDenegado("ACC-REC-MED");
    }

    // ══════════════════════════════════════════════════════════════════════════
    // TEST 3 — Recepcionista intenta acceder a /usuarios/ (ACC-REC-USR)
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("ACC-REC-USR | Recepcionista intenta acceder a /usuarios/ — debe ser denegado")
    void testRecepcionistaAccedeUsuarios() throws Exception {
        ejecutarCasoAccesoDenegado("ACC-REC-USR");
    }

    // ══════════════════════════════════════════════════════════════════════════
    // TEST 4 — Veterinario intenta acceder a /usuarios/ (ACC-VET-USR)
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("ACC-VET-USR | Veterinario intenta acceder a /usuarios/ — debe ser denegado")
    void testVeterinarioAccedeUsuarios() throws Exception {
        ejecutarCasoAccesoDenegado("ACC-VET-USR");
    }
}