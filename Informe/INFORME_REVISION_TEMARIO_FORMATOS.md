# Informe de revisión: reconocimiento de formatos y extracción de texto en «Mi temario»

**Fecha:** 2026-11-19  
**Alcance:** Revisión en profundidad de la detección de formatos, lectura de archivos, extracción de PDF y gestión de archivos no soportados o ilegibles en la función «Mi temario».

---

## 1. Archivos revisados

- `juego/src/main/java/metajavbots/ejercicios/LectorDeTemario.java` — Cargador y extractor de texto de documentos.
- `juego/src/main/java/metajavbots/pantallas/Temario.java` — Pantalla «Mi temario» (campo de ruta, arrastrar y soltar, carga en editor, detección de temas).
- `lwjgl3/src/main/java/metajavbots/escritorio/Main.java` — Recepción de archivos arrastrados a la ventana (`archivosSoltados`).
- `res/textos/ui.txt` — Textos de interfaz trilingües (`ES | DE | EN`) para `temario.*`.
- `pruebas/src/main/java/metajavbots/pruebas/PruebasDelTemario.java` — Pruebas unitarias de lectura de temario y guardado.
- `pruebas/src/main/java/metajavbots/pruebas/PruebasDelContenido.java` — Verificación automática de claves de `T.t(...)`/`T.f(...)` en los tres idiomas.
- `gradle.properties`, `juego/build.gradle`, `pruebas/build.gradle`, `CREDITOS.md`.

---

## 2. Diagnóstico de hallazgos

### 2.1. Causa raíz del fallo con PDFs reales (texto corrupto/ilegible)

La implementación anterior de `LectorDeTemario.pdf(byte[] b)` usaba un analizador artesanal basado en expresiones regulares y lectura directa de bytes en `ISO-8859-1`. Al abrir PDFs reales generados por LaTeX, Word, PowerPoint, LibreOffice o navegadores (como `PR1_Uebungsbuch_nach_Folien.pdf`), se producían múltiples fallos combinados:

1. **Expresión regular rota ante diccionarios anidados (`<< ... << ... >> ... >>`):**  
   El patrón `(<<(?:(?!>>).)*?>>)\s*stream\r?\n` se detenía en el primer `>>` interno (p. ej. `/DecodeParms << ... >>` o `/Resources << ... >>`), perdiendo claves del diccionario exterior como `/Filter /FlateDecode`, `/Subtype /Image` o `/FontFile`. Al no detectar `/FlateDecode`, **los streams binarios comprimidos no se descomprimían** y se pasaban en crudo al extractor de texto.
2. **Lectura indiscriminada de todos los streams del archivo:**  
   En lugar de recorrer el árbol de páginas (`/Catalog` → `/Pages` → `/Page` → `/Contents` y Form XObjects), recorría cualquier `stream ... endstream` del PDF, incluyendo `/ObjStm` (object streams), `/XRef`, `/CMap` (`/ToUnicode`), `/Metadata`, perfiles ICC o revisiones incrementales borradas.
3. **Ausencia total de resolución de fuentes, codificaciones y mapas `/ToUnicode`:**  
   `textoDeContenido()` interpretaba los bytes dentro de `(...)` directamente como `ISO-8859-1` e ignoraba por completo las cadenas hexadecimales `<...>`. Los PDFs modernos utilizan fuentes **Type0 / CIDFont (`Identity-H`)** (donde cada glifo es un índice binario de 2 bytes que solo se traduce a texto mediante la tabla `/ToUnicode` CMap) o fuentes Type1/TrueType con `/Encoding` + `/Differences` (muy habitual en `pdflatex` para `ä`, `ö`, `ü`, `ß`, tildes y ligaduras). Leer esos bytes en crudo generaba basura ilegible.
4. **`textoDeContenido()` no comprobaba estar dentro de bloques `BT ... ET`:**  
   Solo comprobaba `if (!c.contains("BT")) return;` y extraía cualquier paréntesis `(...)` de todo el stream. Si un stream binario comprimido contenía casualmente los dos bytes `BT` y algún byte `0x28` (`(`), volcaba líneas enteras de basura binaria y la pantalla informaba `Cargado «...» (N líneas)`.
5. **Filtros y cifrado no soportados:**  
   No soportaba predictores PNG/TIFF en `/FlateDecode`, ni otros filtros PDF (`/LZWDecode`, `/ASCII85Decode`, `/ASCIIHexDecode`, arrays de filtros), ni PDFs con cifrado de propietario sin contraseña de usuario.
6. **Sin comprobación de legibilidad del texto extraído:**  
   Solo comprobaba `isBlank()`, por lo que cualquier extracción corrupta o de fuentes sin mapa Unicode se mostraba directamente en el editor.

### 2.2. Fallos en detección de formatos, binarios e idioma de los avisos

1. **Formatos no soportados aceptados en el `default:` del `switch`:**  
   `LectorDeTemario.FORMATOS` declaraba `txt, md, java, csv, docx, odt, odp, pptx, html, htm, rtf, pdf`, pero `leer(Path)` no validaba la extensión. Cualquier formato no soportado (`.png`, `.jpg`, `.doc`, `.xlsx`, `.zip`, `.exe`, `.jar`, etc.) caía en `default: new String(b, StandardCharsets.UTF_8)` y volcaba basura binaria en el editor.
2. **Sin detección de archivos binarios ni codificación Windows-1252/BOM:**  
   Un archivo binario renombrado a `.txt` o sin extensión se volcaba tal cual en el editor. Por otro lado, archivos `.txt` reales en Windows-1252 (con tildes o diéresis) mostraban caracteres de reemplazo `` al leerse solo como UTF-8 sin respaldo.
3. **Mensajes de error fijos en español:**  
   Las `IOException` de `LectorDeTemario` tenían textos en español fijo (`"sin texto"`, `"no es un PDF"`, `"el PDF no tiene texto (¿escaneado?)"`, `"no es un documento: falta ..."`). Cuando el idioma activo era alemán (`DE`) o inglés (`EN`), el aviso salía mezclado en dos idiomas.

---

## 3. Correcciones aplicadas

1. **Extracción real de PDF con Apache PDFBox 3.0.4 (licencia Apache-2.0):**
   - Añadida `pdfboxVersion=3.0.4` en `gradle.properties`, dependencia `org.apache.pdfbox:pdfbox` en `juego/build.gradle` (y en `pruebas/build.gradle` para generar PDFs reales en tests), y registrada en `CREDITOS.md`.
   - Reemplazado el analizador artesanal de PDF en `LectorDeTemario.pdf(byte[] b)` por `Loader.loadPDF(b)` + `PDFTextStripper` con `setSortByPosition(true)`, leyendo página a página hasta cubrir `MAXIMO` (evita bloquearse en libros de cientos de páginas).
   - Soporta correctamente streams comprimidos (`FlateDecode` y resto de filtros estándar), `ObjStm`/`XRef`, fuentes Type0/CIDFont (`Identity-H`), mapas `/ToUnicode`, codificaciones Type1/TrueType y orden de lectura.
   - Gestiona limpiamente PDFs inválidos/dañados (`temario.error_pdf_invalido`), protegidos con contraseña (`InvalidPasswordException` → `temario.error_pdf_protegido`), escaneados sin texto (`temario.error_pdf_sin_texto`) o con texto corrupto/sin Unicode (`temario.error_pdf_ilegible`).

2. **Validación estricta de formatos, detección de binarios y legibilidad:**
   - `LectorDeTemario.leer(Path)` comprueba primero que la ruta existe y es un archivo regular (no carpeta) y que no está vacío.
   - Rechaza explícitamente cualquier extensión fuera de los formatos soportados (`txt`, `md`, `java`, `csv`, `docx`, `odt`, `odp`, `pptx`, `html`, `htm`, `rtf`, `pdf`; además de archivos sin extensión solo si son texto plano válido) con `temario.error_formato`.
   - Añadidos `esBinario(byte[])` (detecta bytes NUL y proporción anómala de caracteres de control) y `decodificarTexto(byte[])` (UTF-8 con/sin BOM, UTF-16LE/BE con BOM, y respaldo automático a `windows-1252` si no es UTF-8 válido pero tampoco binario).
   - Añadida validación de cabecera `{\rtf` y no-binario en `rtfDe(byte[])`.
   - Añadida comprobación `esTextoLegible(String)` para detectar y rechazar texto corrupto (alta proporción de `U+FFFD`, caracteres de control, Área de Uso Privado `U+E000..U+F8FF` o ausencia de caracteres alfanuméricos).
   - Normalización limpia de saltos de página (`\f` → `\n`), espacios no separables (`\u00A0`), BOM (`\uFEFF`) y guiones blandos (`\u00AD`).

3. **Avisos claros en el idioma activo (`ES`, `DE`, `EN`):**
   - Añadidas 9 claves trilingües en `res/textos/ui.txt`: `temario.error_no_existe`, `temario.error_formato`, `temario.error_binario`, `temario.error_sin_texto`, `temario.error_documento`, `temario.error_pdf_invalido`, `temario.error_pdf_sin_texto`, `temario.error_pdf_ilegible`, `temario.error_pdf_protegido`.
   - En `Temario.cargar(String)`, añadida guarda defensiva por si una `RuntimeException` inesperada carece de `getMessage()`.

4. **Pruebas unitarias ampliadas en `PruebasDelTemario.java`:**
   - Conservadas las pruebas de DOCX, ODT, PPTX, HTML, RTF, TXT y guardado de campaña propia (añadiendo `/F1 12 Tf` y recurso de fuente estándar a los PDFs sintéticos para cumplir la especificación PDF).
   - Añadidas pruebas de texto `Windows-1252` y archivo de texto sin extensión.
   - Añadido caso de **PDF real multipágina y PDF real con fuente TrueType embebida (`DejaVuSansMono.ttf`, Type0/Identity-H, mapa `ToUnicode` y `FlateDecode`)** con texto en alemán y español (`Übung`, `Datentypen`, `Schleifen`, `Herencia`, `colecciones`, `recursión`, `äöüß`, `áéíóúñ`) y verificación de que `Temario.temasDe(...)` reconoce todos los temas.
   - Añadido caso de **rechazo limpio en los tres idiomas (`ES`, `DE`, `EN`)** para PDF escaneado, falso PDF, PDF protegido con contraseña, formatos no soportados (`.png`, `.doc`), archivo binario disfrazado de `.txt` y basura Unicode (`U+E001..`, `U+FFFD`).

---

## 4. Elementos revisados sin encontrar fallos (dejados intactos)

- **Extracción de Word (`.docx`), OpenDocument (`.odt`, `.odp`) y PowerPoint (`.pptx`)** en `LectorDeTemario` (`docx()`, `entradas()`, `deXml()`, `entidades()`): abren correctamente el ZIP, ordenan diapositivas numéricamente (`slide1`, `slide2`, `slide10`), separan párrafos y decodifican entidades básicas. Se dejó intacta la lógica (solo se internacionalizó el mensaje de error cuando falta la entrada XML).
- **Extracción de HTML (`.html`, `.htm`)** (`html()`): elimina `<script>`/`<style>`, convierte saltos y bloques en líneas nuevas y limpia etiquetas. Dejada intacta.
- **Extracción de RTF (`.rtf`)** (`rtf()`): elimina cabeceras (`fonttbl`, `colortbl`, `stylesheet`, `info`, `\*`), convierte `\par` y escapes hexadecimales `\'hh`. Dejada intacta.
- **Pantalla `Temario.java`**: flujo de arrastrar y soltar (`archivosSoltados`), campo de ruta, botón «Cargar», reconocimiento de temas por palabras clave (`temasDe`), combates temáticos y validación/guardado de propuestas de la IA (`TemarioGuardado`). Dejados intactos (salvo la guarda de mensaje nulo en `cargar()`).
- **Resto del juego**: pantallas, mecánicas de combate, editor, compilador y dibujos/animaciones de los robots no fueron tocados.

---

## 5. Dudas y puntos dejados pendientes para revisión

1. **Prueba manual con `PR1_Uebungsbuch_nach_Folien.pdf`:**  
   Ese archivo concreto no está incluido en el repositorio, por lo que no se ha podido ejecutar sobre él directamente. Se ha reproducido su escenario técnico mediante PDFs reales con fuentes Type0/CIDFont, mapas `ToUnicode`, `FlateDecode` y caracteres alemanes (`äöüß`). Conviene hacer una prueba manual en el juego arrastrando `PR1_Uebungsbuch_nach_Folien.pdf` para confirmar el resultado final.
2. **Escapes Unicode `\uN` en RTF y entidades numéricas `&#...;` en XML/HTML:**  
   Los extractores existentes de RTF y XML/HTML funcionan bien con archivos modernos en UTF-8 y escapes `\'hh`, pero no decodifican `\uN` de RTF ni entidades numéricas `&#160;` / `&#xE4;`. Al no haber fallos reportados en esos formatos, se han dejado intactos para no introducir cambios innecesarios; pueden añadirse en una iteración futura si hiciera falta.
3. **Clave `temario.ruta` en `res/textos/ui.txt`:**  
   `Temario.java` llama a `T.f("temario.ruta", LectorDeTemario.formatos())`, pero el texto de `temario.ruta` en `res/textos/ui.txt` no incluye `{0}` (los formatos ya se muestran en el tooltip `temario.cargar_ayuda`). No provoca error y se ha dejado sin tocar por ser un detalle de diseño de interfaz.
4. **Tamaño de la nueva dependencia (Apache PDFBox 3.0.4):**  
   Aporta ~4,5 MB al classpath de ejecución (licencia Apache-2.0, 100 % compatible con el proyecto, sin dependencias LGPL/GPL y con carga perezosa solo al abrir un PDF). Dejado anotado en `CREDITOS.md` para revisión antes de publicar.
5. **Avisos previos del compilador fuera del alcance:**  
   Al compilar `:juego:compileJava` aparecieron 2 avisos preexistentes de cast redundante en `juego/src/main/java/metajavbots/pantallas/EscenaDeArena.java` (líneas 58 y 95). Se dejaron intactos por estar fuera del alcance de «Mi temario».

---

## 6. Resultado de compilación, estilo y pruebas

Comando ejecutado:

```powershell
.\gradlew.bat compileJava estilo pruebas:prueba
```

Resultados obtenidos:

- **Compilación (`compileJava`):** `BUILD SUCCESSFUL` (sin avisos en los archivos modificados).
- **Estilo (`estilo` / Checkstyle en `juego`, `lwjgl3` y `pruebas`):** `PASSED` (0 errores, 0 avisos).
- **Pruebas unitarias (`pruebas:prueba`):** **`112 casos bien, 0 mal`** (64 s), incluyendo:
  - `temario: se lee de PDF, Word, OpenDocument, PowerPoint, HTML, RTF y texto` — **bien**
  - `temario: PDF real con fuentes CID/Type0, mapa ToUnicode, compresión y varias páginas` — **bien**
  - `temario: rechaza con aviso en el idioma activo los formatos no soportados, binarios y PDFs ilegibles` — **bien**
  - `temario: la propuesta comprobada se guarda como campaña propia y se juega sin IA` — **bien**
  - `textos: cada texto de la interfaz que usa el código está en los tres idiomas` — **bien**
