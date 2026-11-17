# MetajavBots

**Español** · [Deutsch](#deutsch) · [English](#english)

Robots que se programan en Java y combaten por rondas, para aprender Java jugando: de cero absoluto a nivel avanzado. Al estilo de Robocode, con robots de diseño propio, cada uno con su personalidad, y un locutor robot loco que narra las peleas.

![Los dieciséis modelos](docs/imagenes/robots.png)

## Cómo se juega

1. **Instala un JDK 21 o más nuevo** (por ejemplo, [Eclipse Temurin](https://adoptium.net)). Hace falta un JDK, no solo un JRE: el juego compila tu código Java.
2. **Abre el juego**: `jugar.bat` en Windows, `./jugar.sh` en Linux y Mac. La primera vez, Gradle baja las bibliotecas.
3. Crea tu partida. Si nunca has programado, empiezas en **Noob I**; si no, una prueba corta te pone tu etiqueta.
4. Elige tu primer robot y ve al **Taller**: ahí está su código. Cámbialo, pulsa **Compilar** (Ctrl+S) y **¡a pelear!**
5. Una pelea son **cuatro rondas** de unos 40 segundos. Entre ronda y ronda se para: lees el informe, cambias el código y las piezas. **Gana quien haya recibido menos daño**; al final, el que pierde explota.

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

**Qué hay**: 16 modelos de robot con su silueta y su personalidad; 236 piezas (armas láser, balas, misiles, cuerpo a cuerpo, lanzallamas, escudos, cabezas con radar y especiales, piernas, ruedas, orugas, alas, núcleos); 262 ejercicios que se comprueban solos (al estilo CodingBat y CodeStepByStep) y 70 lecciones en 22 temas de Java, de los primeros pasos a hilos, streams y diseño; 12 etiquetas (Noob, Junior y Senior I-IV) y una secreta; la computadora en cinco dificultades, adaptada a tu nivel; modo azar, desafío, jefes por tema, tope de cálculo, depurador en vivo, laboratorio de pruebas, robots de otros jugadores y tu propio temario; historial con estadísticas; todo en español, alemán e inglés.

**Cerebro de IA (opcional)**: el juego funciona entero sin IA. Si quieres, en *Ajustes › Cerebro de IA* puedes usar un modelo local con [KoboldCpp](https://github.com/LostRuins/koboldcpp) o uno en línea con tu propia clave (que se guarda fuera del juego). Con IA, los tutoriales son conversaciones, el código se revisa tras cada pelea y el temario se convierte en ejercicios. Más en [docs/CEREBRO.md](docs/CEREBRO.md).

## Cómo citarlo

MetajavBots es de **Francis García Reyes**. El código tiene licencia [Apache-2.0](LICENSE) (conserva el aviso de [NOTICE](NOTICE)); los textos, ejercicios, tutoriales, diálogos y el arte, [CC BY 4.0](docs/LICENCIA_CONTENIDO.md). Si lo usas, cítalo así:

> García Reyes, Francis (2026). *MetajavBots: aprender Java programando robots que combaten.* https://github.com/ (enlace del repositorio)

---

## Deutsch

Roboter, die in Java programmiert werden und in Runden kämpfen – um Java spielend zu lernen: von null bis Fortgeschritten. Im Stil von Robocode, mit eigenen Roboter-Designs, jeder mit seiner Persönlichkeit, und einem verrückten Roboter-Ansager.

**So wird gespielt**

1. **Installiere ein JDK 21 oder neuer** (z. B. [Eclipse Temurin](https://adoptium.net)). Ein JRE reicht nicht: Das Spiel kompiliert deinen Java-Code.
2. **Starte das Spiel**: `jugar.bat` unter Windows, `./jugar.sh` unter Linux und Mac.
3. Lege dein Spiel an. Wer noch nie programmiert hat, beginnt bei **Noob I**; sonst stuft dich ein kurzer Test ein.
4. Wähle deinen ersten Roboter und geh in die **Werkstatt**: Dort ist sein Code. Ändere ihn, drück **Kompilieren** (Strg+S) und **ab in den Kampf!**
5. Ein Kampf hat **vier Runden** zu je etwa 40 Sekunden. Zwischen den Runden kannst du Code und Teile ändern. **Es gewinnt, wer weniger Schaden eingesteckt hat**; am Ende explodiert der Verlierer.

Dein Roboter ist eine Java-Klasse, die von `MetajavbotDe` erbt (Methoden auf Deutsch: `jedenZug()`, `beiSicht(Gegner g)`, `vorwaerts(…)`, `drehen(…)`, `zielenNach(…)`, `angreifen()`).

**Zitieren**: MetajavBots ist von **Francis García Reyes**. Code unter [Apache-2.0](LICENSE) (mit dem Hinweis aus [NOTICE](NOTICE)); Texte, Übungen, Tutorials, Dialoge und Grafik unter [CC BY 4.0](docs/LICENCIA_CONTENIDO.md).

> García Reyes, Francis (2026). *MetajavBots: Java lernen mit programmierten Kampfrobotern.*

---

## English

Robots you program in Java that fight in rounds – to learn Java by playing: from absolute zero to advanced. In the spirit of Robocode, with original robot designs, each with its own personality, and a crazy robot announcer.

**How to play**

1. **Install a JDK 21 or newer** (e.g. [Eclipse Temurin](https://adoptium.net)). A JRE is not enough: the game compiles your Java code.
2. **Start the game**: `jugar.bat` on Windows, `./jugar.sh` on Linux and Mac.
3. Create your game. If you have never programmed, you start at **Noob I**; otherwise a short test places you.
4. Choose your first robot and go to the **Workshop**: its code is there. Change it, press **Compile** (Ctrl+S) and **fight!**
5. A fight has **four rounds** of about 40 seconds. Between rounds you can change the code and the parts. **Whoever took less damage wins**; at the end, the loser explodes.

Your robot is a Java class extending `MetajavbotEn` (English methods: `onTick()`, `onSeeEnemy(Enemy e)`, `ahead(…)`, `turn(…)`, `aimTo(…)`, `attack()`).

**How to cite**: MetajavBots is by **Francis García Reyes**. Code under [Apache-2.0](LICENSE) (keep the [NOTICE](NOTICE)); texts, exercises, tutorials, dialogues and art under [CC BY 4.0](docs/LICENCIA_CONTENIDO.md).

> García Reyes, Francis (2026). *MetajavBots: learning Java by programming fighting robots.*
