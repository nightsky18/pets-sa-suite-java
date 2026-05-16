# PETS S.A. — Suite de Pruebas Automatizadas (Java)

Suite de pruebas de automatización funcional y de seguridad para el sistema **PETS S.A.**, desarrollada con **Java + Selenium + JUnit 5** como parte de la asignatura *Pruebas y Gestión de la Configuración*.

| | |
|---|---|
| **Materia** | Pruebas y Gestión de la Configuración |
| **Docente** | David Fernando Mejía Tabares |
| **Sitio bajo prueba** | https://eyderalexis26.pythonanywhere.com |
| **Suite Robot Framework** | https://github.com/JackBS703/pets_sa_suite.git |
| **Suite Java (este repo)** | https://github.com/nightsky18/pets-sa-suite-java.git |

### Integrantes

- Mariana Montoya Sepúlveda
- Mateo Berrío Cardona
- Esteban Cano Ramírez
- Yeimy Daniela Herrera Bedoya

---

## Descripción de la actividad

La actividad consiste en automatizar pruebas sobre el sitio web del equipo contrario usando dos tecnologías:

1. **Robot Framework** — suite en el repositorio [pets_sa_suite](https://github.com/JackBS703/pets_sa_suite.git)
2. **Java (este repositorio)** — mismos escenarios implementados con Selenium + JUnit 5

**Condiciones obligatorias:**
- La automatización graba video de pantalla mientras ejecuta (FFmpeg — `gdigrab`) y toma screenshots por cada paso relevante.
- Los datos de prueba se leen desde un archivo Excel (`Dataset-Escenarios-PETS-SA.xlsx`) usando Apache POI; ningún valor está hardcodeado en las clases.

---

## Escenarios automatizados

| # | Escenario | Clase Java | Hoja dataset | RF/RNF |
|---|-----------|------------|--------------|--------|
| 1 | Inyección XSS y SQLi en campos de texto | `XssSqliTest.java` | ESC01_XSS_SQLI | RNF04 |
| 2 | Control de acceso por rol cruzado | `AccesoRolTest.java` | ESC02_ACCESO | RF04, RNF02 |
| 3 | Validación de unicidad de cédula e ID de mascota | `UnicidadTest.java` | ESC03_UNICIDAD | RF24, RF25 |
| 4 | Bloqueo de cuenta por 3 intentos fallidos | `BloqueoCuentaTest.java` | ESC04_BLOQUEO | RF03, RNF03 |
| 5 | CRUD completo de Mascota (crear → editar → eliminar) | `CrudMascotaTest.java` | ESC05_CRUD_MASC | RF09, RF11, RF12 |

> **BUG-006** (C-I-05) y **BUG-007** (MED-I-03) están declarados abiertos en el dataset.
> Los tests del Escenario 1 ejecutan el flujo completo pero el `assert` final puede reportar fallo esperado mientras los bugs permanezcan abiertos.

---

## Estructura del proyecto

```
pets-sa-suite-java/
├── pom.xml
├── src/
│   └── test/
│       ├── java/
│       │   └── com/pets/
│       │       ├── base/
│       │       │   └── BaseTest.java          # Setup, video, screenshots
│       │       ├── utils/
│       │       │   └── ExcelReader.java       # Lectura del dataset .xlsx
│       │       └── tests/
│       │           ├── XssSqliTest.java       # Escenario 1
│       │           ├── AccesoRolTest.java     # Escenario 2
│       │           ├── UnicidadTest.java      # Escenario 3
│       │           ├── BloqueoCuentaTest.java # Escenario 4
│       │           └── CrudMascotaTest.java   # Escenario 5
│       └── resources/
│           └── dataset/
│               └── Dataset-Escenarios-PETS-SA.xlsx
└── reports/
    ├── videos/          # 1 archivo MP4 por escenario
    └── screenshots/     # 1 carpeta por escenario → subcarpetas por test
```

---

## Stack tecnológico

| Librería | Versión | Uso |
|---|---|---|
| `selenium-java` | 4.21.x | Control del navegador Chrome |
| `junit-jupiter` | 5.10.x | Framework de pruebas y aserciones |
| `webdrivermanager` | 5.9.x | Descarga automática de ChromeDriver |
| `apache-poi` + `poi-ooxml` | 5.3.x | Lectura del dataset Excel |
| FFmpeg (`gdigrab`) | 8.x | Grabación de video de escritorio |
| Java | 17+ | Lenguaje de implementación |
| Maven | 3.9.x | Gestión de dependencias y ejecución |

---

## Requisitos previos

### 1. Java 17 o superior

```bash
java -version
```

Si no está instalado: descargar desde [Adoptium Temurin 21 LTS](https://adoptium.net/).
En Windows, configurar la variable de entorno `JAVA_HOME` y agregar `%JAVA_HOME%\bin` al `PATH`.

### 2. Maven 3.9+

```bash
mvn -version
```

Si no está instalado: descargar desde [maven.apache.org](https://maven.apache.org/download.cgi), descomprimir y agregar `bin/` al `PATH`.

### 3. Google Chrome

ChromeDriver se descarga automáticamente vía WebDriverManager. Solo se necesita tener Chrome instalado.

### 4. FFmpeg

Descargar desde [ffmpeg.org/download.html](https://ffmpeg.org/download.html) (build `essentials` para Windows).

Actualizar la constante `FFMPEG_PATH` en `BaseTest.java` con la ruta al ejecutable:

```java
private static final String FFMPEG_PATH =
    "C:\\ruta\\a\\ffmpeg\\bin\\ffmpeg.exe";
```

---

## Instalación y configuración

```bash
# 1. Clonar el repositorio
git clone https://github.com/nightsky18/pets-sa-suite-java.git
cd pets-sa-suite-java

# 2. Descargar dependencias
mvn dependency:resolve

# 3. Verificar que el dataset está en su lugar
ls src/test/resources/dataset/Dataset-Escenarios-PETS-SA.xlsx
```

---

## Ejecución

### Suite completa

```bash
mvn test
```

### Un escenario específico

```bash
mvn test -Dtest=XssSqliTest
mvn test -Dtest=AccesoRolTest
mvn test -Dtest=UnicidadTest
mvn test -Dtest=BloqueoCuentaTest
mvn test -Dtest=CrudMascotaTest
```

### Con salida detallada en consola

```bash
mvn test -Dtest=CrudMascotaTest -pl . --no-transfer-progress
```

---

## Precondiciones por escenario

| Escenario | Precondición antes de ejecutar |
|---|---|
| 1 — XSS/SQLi | Ninguna. `qa_admin` debe existir. |
| 2 — Control de acceso | Usuarios `qa_vet` y `qa_recep` deben existir con sus roles asignados. |
| 3 — Unicidad | `C-V-01` (cliente con cédula `1012345678`) y `MASC-001` (mascota) deben existir previamente para intentar duplicarlos. |
| 4 — Bloqueo | `qa_bloqueo` debe existir **sin bloqueo activo**. Si quedó bloqueado de una ejecución anterior, desbloquearlo en `/admin/`. |
| 5 — CRUD Mascota | `MASC-001` NO debe existir (la crea el test). `MASC-003` debe existir para el test de eliminación. |

---

## Salidas generadas

Después de ejecutar, la carpeta `reports/` contiene:

```
reports/
├── videos/
│   ├── XssSqliTest__20260515_220000.mp4
│   ├── AccesoRolTest__20260515_220120.mp4
│   ├── UnicidadTest__20260515_220300.mp4
│   ├── BloqueoCuentaTest__20260515_220430.mp4
│   └── CrudMascotaTest__20260515_220600.mp4
└── screenshots/
    ├── XssSqliTest/
    │   ├── C_I_05___XSS_SQLi_en_Clientes__RNF04_/
    │   │   ├── 01_formulario_abierto__220001.png
    │   │   ├── 02_campos_rellenos__220002.png
    │   │   └── 03_resultado_xss__220003.png
    │   └── MED_I_03___XSS_SQLi_en_Medicamentos__RNF04_/
    │       └── ...
    ├── BloqueoCuentaTest/
    │   ├── BLOQUEO_INTENTO_1_.../
    │   ├── BLOQUEO_INTENTO_2_.../
    │   ├── BLOQUEO_INTENTO_3_.../
    │   └── BLOQUEO_VERIFY_.../
    └── CrudMascotaTest/
        ├── M_V_01___Crear_mascota_MASC_001___RF09_/
        ├── M_EDIT_01___Editar_mascota_MASC_001___/
        └── M_DEL_01___Eliminar_mascota_MASC_003__/
```

Cada escenario produce:
- **1 video MP4** — grabación completa de la ejecución (inicio a fin del escenario)
- **Máximo 3 screenshots por test** — estado inicial, datos ingresados y resultado

---

## Ciclo de vida de las pruebas

```
@BeforeAll  → crea carpeta de screenshots + inicia grabación FFmpeg  (1 vez por clase)
@BeforeEach → abre Chrome maximizado                                  (1 vez por @Test)
  [ejecución del @Test]
@AfterEach  → cierra Chrome                                           (1 vez por @Test)
@AfterAll   → envía 'q' a FFmpeg y detiene la grabación              (1 vez por clase)
```

`@TestInstance(PER_CLASS)` permite que `@BeforeAll` y `@AfterAll` sean métodos de instancia, lo que habilita compartir `ffmpegProcess` entre todos los tests de la misma clase sin necesidad de campos estáticos.

---

## Decisiones de diseño

**Lectura de datos desde Excel (`ExcelReader.java`):** cada test recibe un `Map<String, String>` con los valores de la fila del dataset identificada por `dataset_id`. Ningún valor de prueba está hardcodeado en las clases — todos provienen del `.xlsx`.

**Selectores resilientes a IDs dinámicos:** los botones de editar y eliminar usan URLs con IDs de BD variables (ej. `/mascotas/35/eliminar/`). Se usan selectores con `href*=` dentro del contexto de la fila filtrada:
```java
fila.findElement(By.cssSelector("a[href*='/eliminar/'].btn-red"))
```

**Orden garantizado en escenarios con estado acumulado:** los escenarios 4 y 5 usan `@TestMethodOrder(MethodOrderer.OrderAnnotation.class)` porque cada test depende del estado que dejó el anterior (bloqueo acumulado, mascota creada).

**Doble estrategia de confirmación en eliminar:** el test intenta primero un modal HTML (`button.btn-red`) y si no aparece en 3 segundos, captura un `alert` nativo del navegador. Esto evita que el test falle por diferencias en el patrón de confirmación de la app.

---

## Relación con Robot Framework

Esta suite replica los mismos 5 escenarios implementados en Robot Framework:

| Escenario | Robot Framework | Java |
|---|---|---|
| XSS / SQLi | `esc01_xss_sqli.robot` | `XssSqliTest.java` |
| Control de acceso | `esc02_acceso_rol.robot` | `AccesoRolTest.java` |
| Unicidad | `esc03_unicidad.robot` | `UnicidadTest.java` |
| Bloqueo de cuenta | `esc04_bloqueo.robot` | `BloqueoCuentaTest.java` |
| CRUD Mascota | `esc05_crud_mascota.robot` | `CrudMascotaTest.java` |

Repositorio Robot Framework: https://github.com/JackBS703/pets_sa_suite.git

---

## Evidencias de ejecución

Las evidencias (screenshots y videos) se generan localmente en `reports/` al ejecutar `mvn test`.
Para cada escenario se obtienen:

- **Screenshots** nombrados con el paso (`01_formulario_abierto`, `02_campos_rellenos`, `03_resultado_*`) y timestamp.
- **Video MP4** continuo que cubre todos los tests del escenario desde `@BeforeAll` hasta `@AfterAll`.

Ejemplo de salida en consola al ejecutar:

```
[Video] Grabación iniciada → reports/videos/BloqueoCuentaTest__20260515_224500.mp4
[Screenshot 1/3] → reports/screenshots/BloqueoCuentaTest/BLOQUEO_INTENTO_1_.../01_formulario_login__224501.png
[Screenshot 2/3] → reports/screenshots/BloqueoCuentaTest/BLOQUEO_INTENTO_1_.../02_credenciales_ingresadas__224502.png
[Screenshot 3/3] → reports/screenshots/BloqueoCuentaTest/BLOQUEO_INTENTO_1_.../03_resultado_intento__224503.png
[Video] Grabación finalizada → reports/videos/BloqueoCuentaTest__20260515_224500.mp4
```
