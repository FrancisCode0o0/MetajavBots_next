# MetajavBots

<!-- trailer -->
[![MetajavBots: tráiler · Trailer](docs/imagenes/trailer.gif)](https://github.com/FrancisCode0o0/MetajavBots_next/releases/download/trailer/MetajavBots-trailer.mp4)

**▶ [Ver el tráiler completo](https://github.com/FrancisCode0o0/MetajavBots_next/releases/download/trailer/MetajavBots-trailer.mp4)** · **[Den ganzen Trailer ansehen](https://github.com/FrancisCode0o0/MetajavBots_next/releases/download/trailer/MetajavBots-trailer.mp4)** · **[Watch the full trailer](https://github.com/FrancisCode0o0/MetajavBots_next/releases/download/trailer/MetajavBots-trailer.mp4)**
<!-- /trailer -->

[![Pruebas](https://github.com/FrancisCode0o0/MetajavBots_next/actions/workflows/pruebas.yml/badge.svg)](https://github.com/FrancisCode0o0/MetajavBots_next/actions/workflows/pruebas.yml)

**Español** · [Deutsch](#deutsch) · [English](#english)

Robots que se programan en Java y combaten por rondas, para aprender Java jugando: de cero absoluto a nivel avanzado. Al estilo de Robocode, con robots de diseño propio, cada uno con su personalidad, y un locutor robot loco que narra las peleas.

## Descargar y jugar

Sin instalar Java ni nada más: el juego lleva su propio Java dentro.

1. Entra en la **[última versión](https://github.com/FrancisCode0o0/MetajavBots_next/releases/latest)** y, en *Assets*, baja el archivo de tu sistema:
   - **Windows**: `MetajavBots-…-windows.zip`
   - **Linux**: `MetajavBots-…-linux.tar.gz`
   - **Mac** con chip Apple (M1 o posterior): `MetajavBots-…-mac-arm64.zip`
2. **Descomprímelo** (en Windows: clic derecho › *Extraer todo…*).
3. Abre la carpeta y haz **doble clic en MetajavBots** (`MetajavBots.exe` en Windows; en Linux, `MetajavBots/bin/MetajavBots`).

**Si Windows avisa** de que la aplicación es desconocida («Windows protegió su PC»), pulsa **Más información › Ejecutar de todos modos**. Sale porque el juego no lleva una firma de pago, no porque tenga nada malo. En Mac, si dice que no se puede comprobar, ve a *Ajustes del Sistema › Privacidad y seguridad* y pulsa **Abrir igualmente**.

Tus partidas se guardan en la carpeta `MetajavBots` de tu carpeta personal: puedes borrar el juego o bajar una versión nueva sin perderlas.

**Para quien quiera tocar el código**:

1. **Instala un JDK 21 o más nuevo** (por ejemplo, [Eclipse Temurin](https://adoptium.net)). Hace falta un JDK, no solo un JRE: el juego compila tu código Java.
2. **Baja el código**: botón verde **Code › Download ZIP** (y descomprímelo) o `git clone https://github.com/FrancisCode0o0/MetajavBots_next.git`.
3. **Abre el juego**: `jugar.bat` en Windows, `./jugar.sh` en Linux y Mac. **La primera vez tarda unos minutos y necesita internet**: Gradle baja las bibliotecas. Así, las partidas se guardan en la carpeta del código (`partidas/`). El paquete listo para jugar se hace con `gradlew lwjgl3:paqueteZip`.

![Los dieciséis modelos](docs/imagenes/robots.png)

![Battle royale](docs/imagenes/royale.png)

## Cómo se juega

1. Crea tu partida. Si nunca has programado, empiezas en **Noob I**; si no, una prueba corta te pone tu etiqueta.
2. Elige tu primer robot y ve al **Taller**: ahí está su código. Cámbialo, pulsa **Compilar** (Ctrl+S) y **¡a pelear!**
3. Una pelea tiene **varias rondas** de unos 40 segundos: 6 de entrada, y en Desafíos eliges 3, 4, 6, 10 o 12, como en el boxeo. Entre ronda y ronda se para: lees el informe, cambias el código y las piezas. **Gana quien haya recibido menos daño**; al final, el que pierde explota.

Tu robot es una clase de Java que hereda de `Metajavbot` (o `MetajavbotDe` en alemán, `MetajavbotEn` en inglés):

```java
public class MiRobot extends Metajavbot {
    @Override
    public void cadaTurno() {
        if (restanteAvance() == 0) {
            avanzar(120);
            girar(35);
        }
        apuntar(12);
    }

    @Override
    public void alVerEnemigo(Enemigo e) {
        apuntarHacia(e.angulo());
        atacar();
    }
}
```

**Qué hay**: 16 modelos de robot con su silueta y su personalidad; 236 piezas (armas láser, balas, misiles, cuerpo a cuerpo, lanzallamas, escudos, cabezas con radar y especiales, piernas, ruedas, orugas, alas, núcleos); 262 ejercicios que se comprueban solos (al estilo CodingBat y CodeStepByStep) y 70 lecciones en 22 temas de Java, de los primeros pasos a hilos, streams y diseño; 12 etiquetas (Noob, Junior y Senior I-IV) y una secreta; modo Historia con dos campañas largas con escenas ilustradas y dieciocho historias de combate; la computadora en cinco dificultades, adaptada a tu nivel; desafíos con modo azar, el desafío, battle royale, combate por equipos (con la computadora u otro jugador de compañero), máquina contra máquina (con la línea que se ejecuta a la vista), jefes por tema, tope de cálculo, depurador en vivo, laboratorio de pruebas, robots de otros jugadores y tu propio temario; estadísticas completas; todo en español, alemán e inglés.

**Cerebro de IA (opcional)**: el juego funciona entero sin IA. Si quieres, en *Ajustes › Cerebro de IA* puedes usar un modelo local con [KoboldCpp](https://github.com/LostRuins/koboldcpp) o uno en línea con tu propia clave (que se guarda fuera del juego). Con IA, los tutoriales son conversaciones, el código se revisa tras cada pelea y el temario se convierte en ejercicios. Más en [docs/CEREBRO.md](docs/CEREBRO.md).

## Cómo se hizo

MetajavBots lo he dirigido yo, Francis García Reyes, estudiante de Informática de primer semestre en la TH Mannheim; la mayor parte del código la han escrito modelos de IA bajo mi dirección. Mi trabajo:

- **Diseño y decisiones**: la idea, los modos de juego, el equilibrio, los textos, la música que quería y cada cambio, con sus prioridades.
- **Encargos**: cada función nueva empezó como un encargo escrito y corto (qué se necesita y cómo se comprueba que está bien), redactado conmigo por un chat de Claude que hacía de tutor.
- **Cadena de trabajo**: los encargos se ejecutaban uno tras otro, cada uno en su propia sesión, con scripts propios que lanzaban el siguiente, pausaban la cadena y avisaban de los permisos delicados.
- **Pruebas y depuración**: jugaba cada versión, buscaba los fallos y los convertía en nuevos encargos.
- **Revisión cruzada**: el trabajo de un modelo lo revisaba otro antes de darlo por bueno.

**Herramientas**: Claude Code (Claude Opus y Fable), Google Antigravity (Gemini), OpenCode y GitHub Copilot; Gradle, libGDX, Checkstyle y GitHub Actions.

**Reglas de calidad**: un commit solo con `gradlew compileJava estilo pruebas:prueba` en verde (Checkstyle sin avisos y más de 300 pruebas sin ventana), que [GitHub Actions](.github/workflows/pruebas.yml) repite en cada push; capturas de cada pantalla con letra normal y grande en los tres idiomas, revisadas antes de cerrar cada encargo. Los encargos y los informes de los modelos están en [docs/proceso/](docs/proceso/).

## Cómo citarlo

MetajavBots es de **Francis García Reyes**. El código tiene licencia [Apache-2.0](LICENSE) (conserva el aviso de [NOTICE](NOTICE)); los textos, ejercicios, tutoriales, diálogos y el arte, [CC BY 4.0](docs/LICENCIA_CONTENIDO.md). Si lo usas, cítalo así:

> García Reyes, Francis (2026). *MetajavBots: aprender Java programando robots que combaten.* https://github.com/ (enlace del repositorio)

---

## Deutsch

Roboter, die in Java programmiert werden und in Runden kämpfen – um Java spielend zu lernen: von null bis Fortgeschritten. Im Stil von Robocode, mit eigenen Roboter-Designs, jeder mit seiner Persönlichkeit, und einem verrückten Roboter-Ansager.

**Herunterladen und spielen**

Ohne Java oder sonst etwas zu installieren: Das Spiel bringt sein eigenes Java mit.

1. Öffne die **[neueste Version](https://github.com/FrancisCode0o0/MetajavBots_next/releases/latest)** und lade unter *Assets* die Datei für dein System herunter:
   - **Windows**: `MetajavBots-…-windows.zip`
   - **Linux**: `MetajavBots-…-linux.tar.gz`
   - **Mac** mit Apple-Chip (M1 oder neuer): `MetajavBots-…-mac-arm64.zip`
2. **Entpacke sie** (unter Windows: Rechtsklick › *Alle extrahieren…*).
3. Öffne den Ordner und **doppelklicke auf MetajavBots** (`MetajavBots.exe` unter Windows; unter Linux `MetajavBots/bin/MetajavBots`).

**Wenn Windows warnt**, die App sei unbekannt („Der Computer wurde durch Windows geschützt“), klicke auf **Weitere Informationen › Trotzdem ausführen**. Die Warnung kommt, weil das Spiel keine kostenpflichtige Signatur hat, nicht weil etwas damit nicht stimmt. Auf dem Mac: Wenn er meldet, dass die App nicht überprüft werden kann, geh zu *Systemeinstellungen › Datenschutz & Sicherheit* und klicke auf **Dennoch öffnen**.

Deine Spielstände liegen im Ordner `MetajavBots` in deinem persönlichen Ordner: Du kannst das Spiel löschen oder eine neue Version herunterladen, ohne sie zu verlieren.

**Für alle, die am Code arbeiten wollen**:

1. **Installiere ein JDK 21 oder neuer** (z. B. [Eclipse Temurin](https://adoptium.net)). Ein JRE reicht nicht: Das Spiel kompiliert deinen Java-Code.
2. **Hol dir den Code**: grüner Knopf **Code › Download ZIP** (und entpacken) oder `git clone https://github.com/FrancisCode0o0/MetajavBots_next.git`.
3. **Starte das Spiel**: `jugar.bat` unter Windows, `./jugar.sh` unter Linux und Mac. **Beim ersten Mal dauert es ein paar Minuten und braucht Internet**: Gradle lädt die Bibliotheken herunter.

**So wird gespielt**

1. Lege dein Spiel an. Wer noch nie programmiert hat, beginnt bei **Noob I**; sonst stuft dich ein kurzer Test ein.
2. Wähle deinen ersten Roboter und geh in die **Werkstatt**: Dort ist sein Code. Ändere ihn, drück **Kompilieren** (Strg+S) und **ab in den Kampf!**
3. Ein Kampf hat **mehrere Runden** zu je etwa 40 Sekunden: anfangs 6, und unter Herausforderungen wählst du 3, 4, 6, 10 oder 12 – wie beim Boxen. Zwischen den Runden kannst du Code und Teile ändern. **Es gewinnt, wer weniger Schaden eingesteckt hat**; am Ende explodiert der Verlierer.

Dein Roboter ist eine Java-Klasse, die von `MetajavbotDe` erbt (Methoden auf Deutsch: `jedenZug()`, `beiSicht(Gegner g)`, `vorwaerts(…)`, `drehen(…)`, `zielenNach(…)`, `angreifen()`).

**Wie es entstanden ist**

MetajavBots habe ich, Francis García Reyes, Informatikstudent im ersten Semester an der TH Mannheim, geleitet; den größten Teil des Codes haben KI-Modelle unter meiner Anleitung geschrieben. Meine Arbeit:

- **Design und Entscheidungen**: die Idee, die Spielmodi, das Balancing, die Texte, die gewünschte Musik und jede Änderung mit ihrer Priorität.
- **Aufträge**: Jede neue Funktion begann als kurzer schriftlicher Auftrag (was gebraucht wird und wie man prüft, dass es stimmt), den ich mit einem Claude-Chat als Tutor formuliert habe.
- **Arbeitskette**: Die Aufträge liefen nacheinander, jeder in einer eigenen Sitzung, mit eigenen Skripten, die den nächsten starteten, die Kette pausierten und bei heiklen Berechtigungen warnten.
- **Testen und Debugging**: Ich habe jede Version gespielt, Fehler gesucht und daraus neue Aufträge gemacht.
- **Gegenseitige Prüfung**: Die Arbeit eines Modells hat ein anderes geprüft, bevor sie übernommen wurde.

**Werkzeuge**: Claude Code (Claude Opus und Fable), Google Antigravity (Gemini), OpenCode und GitHub Copilot; Gradle, libGDX, Checkstyle und GitHub Actions.

**Qualitätsregeln**: Ein Commit nur mit `gradlew compileJava estilo pruebas:prueba` im grünen Bereich (Checkstyle ohne Warnungen und über 300 Tests ohne Fenster), was [GitHub Actions](.github/workflows/pruebas.yml) bei jedem Push wiederholt; Screenshots jedes Bildschirms in normaler und großer Schrift in allen drei Sprachen, geprüft vor dem Abschluss jedes Auftrags. Die Aufträge und die Berichte der Modelle liegen in [docs/proceso/](docs/proceso/).

**Zitieren**: MetajavBots ist von **Francis García Reyes**. Code unter [Apache-2.0](LICENSE) (mit dem Hinweis aus [NOTICE](NOTICE)); Texte, Übungen, Tutorials, Dialoge und Grafik unter [CC BY 4.0](docs/LICENCIA_CONTENIDO.md).

> García Reyes, Francis (2026). *MetajavBots: Java lernen mit programmierten Kampfrobotern.*

---

## English

Robots you program in Java that fight in rounds – to learn Java by playing: from absolute zero to advanced. In the spirit of Robocode, with original robot designs, each with its own personality, and a crazy robot announcer.

**Download and play**

No need to install Java or anything else: the game comes with its own Java.

1. Go to the **[latest release](https://github.com/FrancisCode0o0/MetajavBots_next/releases/latest)** and, under *Assets*, download the file for your system:
   - **Windows**: `MetajavBots-…-windows.zip`
   - **Linux**: `MetajavBots-…-linux.tar.gz`
   - **Mac** with Apple chip (M1 or later): `MetajavBots-…-mac-arm64.zip`
2. **Unzip it** (on Windows: right-click › *Extract All…*).
3. Open the folder and **double-click MetajavBots** (`MetajavBots.exe` on Windows; on Linux, `MetajavBots/bin/MetajavBots`).

**If Windows warns** that the app is unrecognized ("Windows protected your PC"), click **More info › Run anyway**. The warning appears because the game has no paid signature, not because anything is wrong with it. On a Mac, if it says the app cannot be verified, go to *System Settings › Privacy & Security* and click **Open Anyway**.

Your saved games live in the `MetajavBots` folder in your home folder: you can delete the game or download a new version without losing them.

**For those who want to work on the code**:

1. **Install a JDK 21 or newer** (e.g. [Eclipse Temurin](https://adoptium.net)). A JRE is not enough: the game compiles your Java code.
2. **Get the code**: green button **Code › Download ZIP** (and unzip it) or `git clone https://github.com/FrancisCode0o0/MetajavBots_next.git`.
3. **Start the game**: `jugar.bat` on Windows, `./jugar.sh` on Linux and Mac. **The first time takes a few minutes and needs internet**: Gradle downloads the libraries.

**How to play**

1. Create your game. If you have never programmed, you start at **Noob I**; otherwise a short test places you.
2. Choose your first robot and go to the **Workshop**: its code is there. Change it, press **Compile** (Ctrl+S) and **fight!**
3. A fight has **several rounds** of about 40 seconds: 6 by default, and under Challenges you pick 3, 4, 6, 10 or 12, like in boxing. Between rounds you can change the code and the parts. **Whoever took less damage wins**; at the end, the loser explodes.

Your robot is a Java class extending `MetajavbotEn` (English methods: `onTick()`, `onSeeEnemy(Enemy e)`, `ahead(…)`, `turn(…)`, `aimTo(…)`, `attack()`).

**How it was made**

MetajavBots was directed by me, Francis García Reyes, a first-semester computer science student at TH Mannheim; most of the code was written by AI models under my direction. My part:

- **Design and decisions**: the idea, the game modes, the balance, the texts, the music I wanted, and every change with its priority.
- **Work orders**: every new feature started as a short written work order (what is needed and how to check it works), drafted with me by a Claude chat acting as a tutor.
- **Work chain**: the orders ran one after another, each in its own session, with my own scripts that launched the next one, paused the chain, and flagged sensitive permissions.
- **Testing and debugging**: I played every version, hunted bugs, and turned them into new work orders.
- **Cross-review**: one model's work was reviewed by another before it was accepted.

**Tools**: Claude Code (Claude Opus and Fable), Google Antigravity (Gemini), OpenCode, and GitHub Copilot; Gradle, libGDX, Checkstyle, and GitHub Actions.

**Quality rules**: a commit only with `gradlew compileJava estilo pruebas:prueba` green (Checkstyle with no warnings and 300+ headless tests), which [GitHub Actions](.github/workflows/pruebas.yml) repeats on every push; screenshots of every screen at normal and large font size in all three languages, reviewed before closing each work order. The work orders and the models' reports are in [docs/proceso/](docs/proceso/).

**How to cite**: MetajavBots is by **Francis García Reyes**. Code under [Apache-2.0](LICENSE) (keep the [NOTICE](NOTICE)); texts, exercises, tutorials, dialogues and art under [CC BY 4.0](docs/LICENCIA_CONTENIDO.md).

> García Reyes, Francis (2026). *MetajavBots: learning Java by programming fighting robots.*
