package com.pets.tests;

import com.pets.base.BaseTest;
import com.pets.utils.ExcelReader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Escenario 4 — Bloqueo de cuenta por 3 intentos fallidos
 * Valida RF03: tras 3 contraseñas incorrectas el sistema bloquea la cuenta.
 * Valida RNF03: el bloqueo debe ser persistente (ni siquiera la clave correcta desbloquea).
 *
 * Precondición: qa_bloqueo debe existir y NO estar bloqueado antes de ejecutar.
 * Si quedó bloqueado de una ejecución anterior, desbloquearlo en /admin/.
 *
 * Dataset: ESC04_BLOQUEO
 *   BLOQUEO-INTENTO-1 → primer intento fallido
 *   BLOQUEO-INTENTO-2 → segundo intento fallido
 *   BLOQUEO-INTENTO-3 → tercer intento fallido + verificar mensaje de bloqueo
 *   BLOQUEO-VERIFY    → intento con clave correcta para confirmar que sigue bloqueado
 *
 * Los 4 tests se ejecutan en orden estricto con @TestMethodOrder.
 * Cada test abre su propia sesión de Chrome (BaseTest @BeforeEach).
 *
 * Screenshots por test (3):
 *   1. formulario_login     — login vacío al iniciar
 *   2. credenciales_ingresadas — usuario y contraseña incorrecta visibles
 *   3. resultado_intento    — respuesta del sistema tras el submit
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class BloqueoCuentaTest extends BaseTest {

    private static final String BASE_URL = "https://eyderalexis26.pythonanywhere.com";
    private static final String SHEET    = "ESC04_BLOQUEO";

    // ── Utilidad: navegar al login y esperar que cargue ───────────────────────

    private WebDriverWait irAlLogin(Map<String, String> data) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        driver.get(BASE_URL + data.get("${campo_url_login}"));
        wait.until(ExpectedConditions.visibilityOfElementLocated(
                resolveLocator(data.get("${campo_user_locator}"))));
        return wait;
    }

    // ── Utilidad: ingresar credenciales y hacer submit ────────────────────────

    private void submitLogin(Map<String, String> data, String password) {
        driver.findElement(resolveLocator(data.get("${campo_user_locator}")))
              .sendKeys(data.get("${username}"));
        driver.findElement(resolveLocator(data.get("${campo_pass_locator}")))
              .sendKeys(password);
        driver.findElement(resolveLocator(data.get("${campo_submit_locator}"))).click();
    }

    // ── Utilidad: resolver locator ────────────────────────────────────────────

    private By resolveLocator(String locatorStr) {
        if (locatorStr.startsWith("id:"))  return By.id(locatorStr.substring(3));
        if (locatorStr.startsWith("css:")) return By.cssSelector(locatorStr.substring(4));
        throw new IllegalArgumentException("Locator no reconocido: " + locatorStr);
    }

    // ── Utilidad: leer texto de un elemento o devolver vacío si no existe ─────

    private String leerMensaje(By locator) {
        try {
            WebElement el = driver.findElement(locator);
            return el.isDisplayed() ? el.getText().trim() : "";
        } catch (Exception e) {
            return "";
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // TEST 1 — Primer intento fallido (BLOQUEO-INTENTO-1)
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @Order(1)
    @DisplayName("BLOQUEO-INTENTO-1 | Primer intento fallido — mensaje de credenciales incorrectas")
    void testPrimerIntentoFallido() throws Exception {
        Map<String, String> data = ExcelReader.getRowById(SHEET, "BLOQUEO-INTENTO-1");
        WebDriverWait wait = irAlLogin(data);

        takeScreenshot("formulario_login");

        driver.findElement(resolveLocator(data.get("${campo_user_locator}")))
              .sendKeys(data.get("${username}"));
        driver.findElement(resolveLocator(data.get("${campo_pass_locator}")))
              .sendKeys(data.get("${password}"));

        takeScreenshot("credenciales_ingresadas");

        driver.findElement(resolveLocator(data.get("${campo_submit_locator}"))).click();

        // Esperar mensaje de error de credenciales
        try {
            wait.until(ExpectedConditions.visibilityOfElementLocated(
                    resolveLocator(data.get("${campo_error_locator}"))));
        } catch (Exception ignored) {}

        takeScreenshot("resultado_intento");

        String mensajeReal    = leerMensaje(resolveLocator(data.get("${campo_error_locator}")));
        String expectedError  = data.get("${expected_error_text}");
        boolean sigueEnLogin  = driver.getCurrentUrl().contains("/login/");

        assertAll("Intento 1 fallido (BLOQUEO-INTENTO-1)",
            () -> assertTrue(sigueEnLogin,
                    "[FALLO] El sistema permitió el acceso con credenciales incorrectas."),
            () -> assertFalse(mensajeReal.isEmpty(),
                    "[FALLO] No se mostró mensaje de error tras credenciales incorrectas."),
            () -> assertTrue(
                    mensajeReal.toLowerCase().contains("contraseña") ||
                    mensajeReal.toLowerCase().contains("usuario") ||
                    mensajeReal.toLowerCase().contains("correctos") ||
                    mensajeReal.toLowerCase().contains("introduzca"),
                    "[FALLO] Mensaje inesperado en intento 1." +
                    "\n  Esperado contener: " + expectedError +
                    "\n  Real: " + mensajeReal)
        );
    }

    // ══════════════════════════════════════════════════════════════════════════
    // TEST 2 — Segundo intento fallido (BLOQUEO-INTENTO-2)
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @Order(2)
    @DisplayName("BLOQUEO-INTENTO-2 | Segundo intento fallido — mensaje de credenciales incorrectas")
    void testSegundoIntentoFallido() throws Exception {
        Map<String, String> data = ExcelReader.getRowById(SHEET, "BLOQUEO-INTENTO-2");
        WebDriverWait wait = irAlLogin(data);

        takeScreenshot("formulario_login");

        driver.findElement(resolveLocator(data.get("${campo_user_locator}")))
              .sendKeys(data.get("${username}"));
        driver.findElement(resolveLocator(data.get("${campo_pass_locator}")))
              .sendKeys(data.get("${password}"));

        takeScreenshot("credenciales_ingresadas");

        driver.findElement(resolveLocator(data.get("${campo_submit_locator}"))).click();

        try {
            wait.until(ExpectedConditions.visibilityOfElementLocated(
                    resolveLocator(data.get("${campo_error_locator}"))));
        } catch (Exception ignored) {}

        takeScreenshot("resultado_intento");

        String mensajeReal   = leerMensaje(resolveLocator(data.get("${campo_error_locator}")));
        boolean sigueEnLogin = driver.getCurrentUrl().contains("/login/");

        assertAll("Intento 2 fallido (BLOQUEO-INTENTO-2)",
            () -> assertTrue(sigueEnLogin,
                    "[FALLO] El sistema permitió el acceso con credenciales incorrectas."),
            () -> assertFalse(mensajeReal.isEmpty(),
                    "[FALLO] No se mostró mensaje de error tras credenciales incorrectas.")
        );
    }

    // ══════════════════════════════════════════════════════════════════════════
    // TEST 3 — Tercer intento fallido + verificar bloqueo (BLOQUEO-INTENTO-3)
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @Order(3)
    @DisplayName("BLOQUEO-INTENTO-3 | Tercer intento fallido — cuenta debe quedar bloqueada")
    void testTercerIntentoYBloqueo() throws Exception {
        Map<String, String> data = ExcelReader.getRowById(SHEET, "BLOQUEO-INTENTO-3");
        WebDriverWait wait = irAlLogin(data);

        takeScreenshot("formulario_login");

        driver.findElement(resolveLocator(data.get("${campo_user_locator}")))
              .sendKeys(data.get("${username}"));
        driver.findElement(resolveLocator(data.get("${campo_pass_locator}")))
              .sendKeys(data.get("${password}"));

        takeScreenshot("credenciales_ingresadas");

        driver.findElement(resolveLocator(data.get("${campo_submit_locator}"))).click();

        // Esperar mensaje de bloqueo (css:div.alert según dataset)
        try {
            wait.until(ExpectedConditions.or(
                ExpectedConditions.visibilityOfElementLocated(
                        resolveLocator(data.get("${campo_bloqueo_locator}"))),
                ExpectedConditions.visibilityOfElementLocated(
                        resolveLocator(data.get("${campo_error_locator}")))
            ));
        } catch (Exception ignored) {}

        takeScreenshot("resultado_intento");

        String mensajeBloqueo  = leerMensaje(resolveLocator(data.get("${campo_bloqueo_locator}")));
        String mensajeError    = leerMensaje(resolveLocator(data.get("${campo_error_locator}")));
        String expectedBloqueo = data.get("${expected_bloqueo_text}");
        boolean sigueEnLogin   = driver.getCurrentUrl().contains("/login/");

        // Si el mensaje de bloqueo aparece en el div de error, también es válido
        boolean bloqueoDetectado =
                mensajeBloqueo.toLowerCase().contains("bloqueado") ||
                mensajeError.toLowerCase().contains("bloqueado") ||
                mensajeBloqueo.toLowerCase().contains("3 intentos") ||
                mensajeError.toLowerCase().contains("3 intentos");

        final String mensajeBloFinal  = mensajeBloqueo;
        final String mensajeErrFinal  = mensajeError;
        final String expectedBloFinal = expectedBloqueo;

        assertAll("Tercer intento y bloqueo (BLOQUEO-INTENTO-3)",
            () -> assertTrue(sigueEnLogin,
                    "[FALLO] El sistema permitió el acceso en el tercer intento fallido."),
            () -> assertTrue(bloqueoDetectado,
                    "[FALLO] No se detectó mensaje de bloqueo tras 3 intentos." +
                    "\n  Esperado contener: " + expectedBloFinal +
                    "\n  div.alert: "  + mensajeBloFinal +
                    "\n  div.error-box: " + mensajeErrFinal)
        );
    }

    // ══════════════════════════════════════════════════════════════════════════
    // TEST 4 — Verificar bloqueo persistente con clave correcta (BLOQUEO-VERIFY)
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @Order(4)
    @DisplayName("BLOQUEO-VERIFY | Clave correcta tras bloqueo — acceso debe seguir denegado")
    void testVerificarBloqueoPersistente() throws Exception {
        Map<String, String> data = ExcelReader.getRowById(SHEET, "BLOQUEO-VERIFY");
        WebDriverWait wait = irAlLogin(data);

        takeScreenshot("formulario_login");

        // Ingresar la clave CORRECTA — si el bloqueo funciona, igual debe denegar
        driver.findElement(resolveLocator(data.get("${campo_user_locator}")))
              .sendKeys(data.get("${username}"));
        driver.findElement(resolveLocator(data.get("${campo_pass_locator}")))
              .sendKeys(data.get("${password}")); // Bloq@2026! — clave real del usuario

        takeScreenshot("credenciales_ingresadas");

        driver.findElement(resolveLocator(data.get("${campo_submit_locator}"))).click();

        try {
            wait.until(ExpectedConditions.or(
                // Opción A: redirige a login con mensaje de bloqueo
                ExpectedConditions.visibilityOfElementLocated(
                        resolveLocator(data.get("${campo_bloqueo_locator}"))),
                // Opción B: el dashboard aparece (caso de FALLO — no debería ocurrir)
                ExpectedConditions.urlContains("/dashboard/"),
                ExpectedConditions.urlContains("/inicio/"),
                ExpectedConditions.urlContains("/home/")
            ));
        } catch (Exception ignored) {}

        takeScreenshot("resultado_intento");

        String urlActual       = driver.getCurrentUrl();
        String mensajeBloqueo  = leerMensaje(resolveLocator(data.get("${campo_bloqueo_locator}")));
        String expectedBloqueo = data.get("${expected_bloqueo_text}");

        boolean accesoPermitido = urlActual.contains("/dashboard/") ||
                                  urlActual.contains("/inicio/")    ||
                                  urlActual.contains("/home/")      ||
                                  (!urlActual.contains("/login/") && !mensajeBloqueo.isEmpty() == false);

        boolean bloqueoConfirmado =
                urlActual.contains("/login/") &&
                (mensajeBloqueo.toLowerCase().contains("bloqueado") ||
                 mensajeBloqueo.toLowerCase().contains("administrador"));

        final boolean accesoPermitidoFinal  = accesoPermitido;
        final boolean bloqueoConfirmadoFinal = bloqueoConfirmado;
        final String  mensajeBloFinal        = mensajeBloqueo;
        final String  expectedBloFinal       = expectedBloqueo;
        final String  urlFinal               = urlActual;

        assertAll("Bloqueo persistente (BLOQUEO-VERIFY)",
            () -> assertFalse(accesoPermitidoFinal,
                    "[FALLO SEGURIDAD] El sistema permitió el acceso con clave correcta " +
                    "pese al bloqueo activo. URL: " + urlFinal),
            () -> assertTrue(bloqueoConfirmadoFinal,
                    "[FALLO] No se confirmó el bloqueo persistente." +
                    "\n  Esperado contener: " + expectedBloFinal +
                    "\n  Mensaje real: " + mensajeBloFinal +
                    "\n  URL actual: " + urlFinal)
        );
    }
}