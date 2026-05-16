package com.pets.tests;

import com.pets.base.BaseTest;
import com.pets.utils.ExcelReader;
import org.junit.jupiter.api.*;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Escenario 5 — CRUD completo de Mascota
 * Flujo: Crear → Editar → Eliminar con confirmación
 *
 * RF09 — Crear mascota con todos sus campos
 * RF11 — Editar mascota existente
 * RF12 — Eliminar mascota con diálogo de confirmación
 *
 * Precondiciones:
 *  - M-V-01:   MASC-001 NO debe existir antes de ejecutar
 *  - M-EDIT-01: depende de que M-V-01 haya PASADO (MASC-001 creado)
 *  - M-DEL-01:  MASC-003 debe existir en BD antes de ejecutar
 *
 * Dataset: ESC05_CRUD_MASC
 * Ejecución: mvn test -Dtest=CrudMascotaTest
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CrudMascotaTest extends BaseTest {

    private static final String BASE_URL = "https://eyderalexis26.pythonanywhere.com";
    private static final String SHEET    = "ESC05_CRUD_MASC";

    // ── login helper ──────────────────────────────────────────────────────────

    private WebDriverWait login(Map<String, String> data) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(12));
        driver.get(BASE_URL + "/login/");
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("id_username")));
        driver.findElement(By.id("id_username")).sendKeys(data.get("${username}"));
        driver.findElement(By.id("id_password")).sendKeys(data.get("${password}"));
        driver.findElement(By.cssSelector("button.btn-green")).click();
        wait.until(ExpectedConditions.urlContains("/mascotas/"));
        return wait;
    }

    // Hace login y navega directo a /mascotas/
    private WebDriverWait loginYNavegar() throws Exception {
        Map<String, String> data = ExcelReader.getRowById(SHEET, "M-V-01");
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(12));
        driver.get(BASE_URL + "/login/");
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("id_username")));
        driver.findElement(By.id("id_username")).sendKeys(data.get("${username}"));
        driver.findElement(By.id("id_password")).sendKeys(data.get("${password}"));
        driver.findElement(By.cssSelector("button.btn-green")).click();
        wait.until(d -> !d.getCurrentUrl().contains("/login/"));
        driver.get(BASE_URL + "/mascotas/");
        wait.until(ExpectedConditions.urlContains("/mascotas/"));
        return wait;
    }

    // ── resolver locator ──────────────────────────────────────────────────────

    private By loc(String s) {
        if (s == null || s.isBlank()) return By.cssSelector("body"); // fallback seguro
        if (s.startsWith("id:"))  return By.id(s.substring(3));
        if (s.startsWith("css:")) return By.cssSelector(s.substring(4));
        if (s.startsWith("xpath:")) return By.xpath(s.substring(6));
        throw new IllegalArgumentException("Locator no reconocido: " + s);
    }

    // ── llenar campo de texto o select ────────────────────────────────────────

    private void fill(By locator, String value) {
        if (value == null || value.isBlank()) return;
        WebElement el = driver.findElement(locator);
        String tag = el.getTagName();
        if (tag.equalsIgnoreCase("select")) {
            try {
                new Select(el).selectByVisibleText(value);
            } catch (Exception e) {
                // Si no hay coincidencia exacta, intenta por texto parcial
                new Select(el).getOptions().stream()
                    .filter(opt -> opt.getText().toLowerCase().contains(value.toLowerCase()))
                    .findFirst()
                    .ifPresent(opt -> new Select(el).selectByVisibleText(opt.getText()));
            }
        } else {
            el.clear();
            el.sendKeys(value);
        }
    }

    // ── buscar fila en la tabla por texto ─────────────────────────────────────

    /**
     * Busca la primera fila de la tabla cuyo texto contenga 'searchText'.
     * Devuelve null si no la encuentra (no lanza excepción).
     */
    private WebElement buscarFilaEnTabla(String searchText) {
        try {
            return driver.findElements(By.cssSelector("table tbody tr"))
                .stream()
                .filter(row -> row.getText().contains(searchText))
                .findFirst()
                .orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // TEST 1 — CREAR mascota (M-V-01)
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @Order(1)
    @DisplayName("M-V-01 | Crear mascota MASC-001 (RF09)")
    void testCrearMascota() throws Exception {
        Map<String, String> data = ExcelReader.getRowById(SHEET, "M-V-01");
        WebDriverWait wait = loginYNavegar();

        // Abrir formulario de nueva mascota
        wait.until(ExpectedConditions.elementToBeClickable(
                By.cssSelector("a[href='/mascotas/nueva/']"))).click();

        wait.until(ExpectedConditions.visibilityOfElementLocated(
                loc(data.get("${campo_id_locator}"))));

        takeScreenshot("formulario_nuevo");

        // Rellenar campos
        fill(loc(data.get("${campo_id_locator}")),     data.get("${pet_id}"));
        fill(loc(data.get("${campo_nombre_locator}")), data.get("${nombre}"));
        fill(loc(data.get("${campo_raza_locator}")),   data.get("${raza}"));
        fill(loc(data.get("${campo_edad_locator}")),   data.get("${edad}"));
        fill(loc(data.get("${campo_peso_locator}")),   data.get("${peso}"));

        // Medicamento y cliente pueden ser <select>
        if (data.get("${medicamento_id}") != null && !data.get("${medicamento_id}").isBlank()) {
            fill(loc(data.get("${campo_medicamento_locator}")), data.get("${medicamento_id}"));
        }
        if (data.get("${cliente_id}") != null && !data.get("${cliente_id}").isBlank()) {
            fill(loc(data.get("${campo_cliente_locator}")), data.get("${cliente_id}"));
        }

        takeScreenshot("campos_rellenos");

        // Guardar
        driver.findElement(loc(data.get("${campo_submit_locator}"))).click();

        // Esperar redirección al listado
        wait.until(ExpectedConditions.urlContains("/mascotas/"));

        takeScreenshot("resultado_crear");

        // Verificar que MASC-001 aparece en la tabla
        WebElement fila = buscarFilaEnTabla(data.get("${campo_row_search_text}"));

        assertAll("Crear mascota (M-V-01) — RF09",
            () -> assertTrue(driver.getCurrentUrl().contains("/mascotas/"),
                    "[FALLO] No redirigió al listado de mascotas tras crear."),
            () -> assertNotNull(fila,
                    "[FALLO] MASC-001 no aparece en la tabla después de crear.\n" +
                    "  Buscado: " + data.get("${campo_row_search_text}"))
        );
    }

    // ══════════════════════════════════════════════════════════════════════════
    // TEST 2 — EDITAR mascota (M-EDIT-01)
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @Order(2)
    @DisplayName("M-EDIT-01 | Editar mascota MASC-001 → MaxEditado (RF11)")
    void testEditarMascota() throws Exception {
        Map<String, String> data = ExcelReader.getRowById(SHEET, "M-EDIT-01");
        WebDriverWait wait = loginYNavegar();

        // Esperar tabla y buscar fila de MASC-001
        wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector("table tbody tr")));

        WebElement fila = buscarFilaEnTabla(data.get("${campo_row_search_text}"));

        assertNotNull(fila,
            "[FALLO PRECONDICIÓN] MASC-001 no existe en la tabla. " +
            "Ejecuta M-V-01 primero o crea la mascota manualmente.");

        takeScreenshot("tabla_antes_editar");

        // Clic en botón de editar (css:a.btn-yellow) dentro de la fila
        WebElement btnEditar = fila.findElement(By.cssSelector("a.btn-yellow"));
        btnEditar.click();

        // Esperar que cargue el formulario de edición
        wait.until(ExpectedConditions.visibilityOfElementLocated(
                loc(data.get("${campo_nombre_locator}"))));

        takeScreenshot("formulario_edicion");

        // Actualizar campos editables (el id no se cambia)
        fill(loc(data.get("${campo_nombre_locator}")), data.get("${nombre}"));
        fill(loc(data.get("${campo_raza_locator}")),   data.get("${raza}"));
        fill(loc(data.get("${campo_edad_locator}")),   data.get("${edad}"));
        fill(loc(data.get("${campo_peso_locator}")),   data.get("${peso}"));

        // Guardar edición
        driver.findElement(loc(data.get("${campo_submit_locator}"))).click();

        // Esperar redirección al listado
        wait.until(ExpectedConditions.urlContains("/mascotas/"));

        takeScreenshot("resultado_editar");

        // Verificar que los datos actualizados aparecen en la tabla
        WebElement filaEditada = buscarFilaEnTabla("MaxEditado");

        assertAll("Editar mascota (M-EDIT-01) — RF11",
            () -> assertTrue(driver.getCurrentUrl().contains("/mascotas/"),
                    "[FALLO] No redirigió al listado tras editar."),
            () -> assertNotNull(filaEditada,
                    "[FALLO] El nombre 'MaxEditado' no aparece en la tabla tras editar.\n" +
                    "  URL actual: " + driver.getCurrentUrl())
        );
    }

    // ══════════════════════════════════════════════════════════════════════════
    // TEST 3 — ELIMINAR mascota con confirmación (M-DEL-01)
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @Order(3)
    @DisplayName("M-DEL-01 | Eliminar mascota MASC-003 con confirmación (RF12)")
    void testEliminarMascota() throws Exception {
        Map<String, String> data = ExcelReader.getRowById(SHEET, "M-DEL-01");
        WebDriverWait wait = loginYNavegar();

        // Esperar tabla
        wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector("table tbody tr")));

        WebElement fila = buscarFilaEnTabla(data.get("${campo_row_search_text}"));

        assertNotNull(fila,
            "[FALLO PRECONDICIÓN] MASC-003 no existe en la tabla. " +
            "Créala manualmente antes de ejecutar este test.");

        takeScreenshot("tabla_antes_eliminar");

        // Clic en botón de eliminar (css:a.btn-red) — abre diálogo de confirmación
        WebElement btnEliminar = fila.findElement(
        By.cssSelector("a[href*='/eliminar/'].btn-red"));
btnEliminar.click();

        /*
         * Estrategia de confirmación:
         *  Opción A — modal/diálogo HTML con css:button.btn-red
         *  Opción B — alert nativo del navegador (driver.switchTo().alert())
         *
         * Se intenta A primero; si no aparece en 3 seg, se intenta B.
         */
        boolean confirmado = false;

        try {
            WebDriverWait shortWait = new WebDriverWait(driver, Duration.ofSeconds(3));
            WebElement btnConfirmar = shortWait.until(
                    ExpectedConditions.elementToBeClickable(
                            By.cssSelector("button.btn-red")));
            takeScreenshot("dialogo_confirmacion");
            btnConfirmar.click();
            confirmado = true;
        } catch (Exception noModal) {
            // Intentar con alert nativo
            try {
                WebDriverWait alertWait = new WebDriverWait(driver, Duration.ofSeconds(3));
                alertWait.until(ExpectedConditions.alertIsPresent());
                takeScreenshot("alert_confirmacion");
                driver.switchTo().alert().accept();
                confirmado = true;
            } catch (Exception noAlert) {
                takeScreenshot("sin_confirmacion_detectada");
            }
        }

        // Esperar redirección al listado tras eliminación
        wait.until(ExpectedConditions.urlContains("/mascotas/"));

        takeScreenshot("resultado_eliminar");

        // Verificar que MASC-003 ya NO aparece en la tabla
        WebElement filaEliminada = buscarFilaEnTabla(data.get("${campo_row_search_text}"));

        final boolean confirmadoFinal = confirmado;

        assertAll("Eliminar mascota (M-DEL-01) — RF12",
            () -> assertTrue(confirmadoFinal,
                    "[FALLO] No se detectó diálogo de confirmación (ni modal ni alert). " +
                    "Verifica el tipo de confirmación que usa la app."),
            () -> assertTrue(driver.getCurrentUrl().contains("/mascotas/"),
                    "[FALLO] No redirigió al listado tras eliminar."),
            () -> assertNull(filaEliminada,
                    "[FALLO] MASC-003 sigue apareciendo en la tabla después de eliminar.")
        );
    }
}