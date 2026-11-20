# Copilot instructions for MetajavBots

## Project overview

MetajavBots is a Java/libGDX desktop game in which users program Java robots and fight them while learning Java. It is a Gradle multi-project build:

- `motor/` is the reusable CastillaFantasX/libGDX engine. Keep it independent of the game and desktop launcher.
- `juego/` contains the game: robot API, compiler/editor, combat, pieces, progression, AI integrations, screens, sound, and content loading.
- `lwjgl3/` is the desktop application and capture runner. It supplies the LWJGL3 window and desktop-only file handling.
- `pruebas/` is a headless executable test harness. Tests are registered in `Pruebas` and run without JUnit.
- `res/` is the authoritative UTF-8 content source. `juego` places it on the runtime classpath.

The application starts in `lwjgl3/src/main/java/metajavbots/escritorio/Main.java`, creates `metajavbots.Juego`, and switches between `Pantalla` implementations. `Juego` owns shared input, rendering, UI, sound, player state, and screen navigation. Combat and robot behavior are in `juego` but use engine abstractions from `motor`. Player robot source is compiled at runtime by `metajavbots.codigo.Compilador` against the multilingual API.

## Build, run, test, and lint

Use a JDK, not only a JRE. The README recommends JDK 21 or newer because the game compiles player code; Gradle compiles project sources with `--release 17`.

From the repository root:

```text
.\gradlew.bat build
.\gradlew.bat lwjgl3:run
.\gradlew.bat pruebas:prueba
.\gradlew.bat estilo
.\gradlew.bat lwjgl3:capturas
```

Equivalent launcher shortcuts are `jugar.bat`, `jugar.bat prueba`, `jugar.bat estilo`, and `jugar.bat capturas`. On Linux/macOS, use `./gradlew` or `./jugar.sh`.

The headless test runner accepts a substring filter through `-Psolo`, so run one focused case or group with:

```text
.\gradlew.bat pruebas:prueba -Psolo=editor
.\gradlew.bat pruebas:prueba -Psolo=compilador
```

Test groups are registered in `pruebas/src/main/java/metajavbots/pruebas/Pruebas.java`; the filter matches the registered case name, not a JUnit method. `estilo` runs Checkstyle on every project except `motor`, using `config/checkstyle/checkstyle.xml`.

If the checkout is under OneDrive, `build.gradle` redirects Gradle outputs to `%LOCALAPPDATA%\MetajavBots\build` to avoid cloud-placeholder `.class` files and stale-output cleanup failures. Do not rely on old module-local `build/` artifacts.

## Content and generated API

Content changes normally belong in `res/`, not copied files under a build directory. Dialogues, tutorials, exercises, and UI text must remain synchronized in Spanish, German, and English. Their formats and validation rules are documented in `docs/FORMATOS.md`; `pruebas:prueba` checks multilingual content and exercise solutions.

The public robot API has one implementation in `juego/src/main/java/metajavbots/api/Metajavbot.java` plus generated German and English facades and API documentation. After changing the API, regenerate the dependent files from the repository root:

```text
python herramientas/fachadas.py juego/src/main/java/metajavbots/api
python herramientas/api_doc.py
```

Keep the three language APIs behaviorally aligned. Shared concepts are exposed through parallel interfaces such as `Enemigo`/`Gegner`/`Enemy`, while the player sees only the selected language’s names.

## Repository-specific conventions

- Use the existing package boundaries: `metajavbots.api` for the player-facing API, `codigo` for source analysis/compilation/editor support, `combate` for simulation, `pantallas` for screen flows, `progreso` for saved player state, and `cerebro` for optional AI providers.
- Treat `motor` as a copied, reusable engine layer. Avoid adding game-specific dependencies or coupling it to `juego`.
- Screens implement `metajavbots.pantalla.Pantalla` and are entered through `Juego.ir`, `irSolo`, or `volver`; preserve that navigation and lifecycle model.
- Runtime resources are loaded from the classpath, commonly with paths such as `/datos/...`; update source files in `res/` and test resource completeness rather than editing generated `build/resources`.
- The project deliberately uses a small custom test framework in `metajavbots.junit` for the in-game laboratory and a separate registration-based headless harness in `pruebas`; follow those patterns instead of introducing a test dependency casually.
- Keep user-facing text and content data in UTF-8 and preserve the established three-language keys/order and content formats.
- Optional AI integrations must remain optional: the game works without an AI model or API key. Configuration and model files are external to the repository.
- Follow the existing Checkstyle configuration and four-space Java formatting. `motor` is excluded from Checkstyle because it is maintained as a copied engine.
- Generated/cached output such as module `build/`, `.gradle/`, and captured screenshots is not the source of truth; make changes in source, resources, or the generation scripts.
