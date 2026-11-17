package metajavbots.escritorio;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;

import metajavbots.Juego;

/** MAIN — Abre la ventana de MetajavBots (escritorio: Windows, Linux, Mac). */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration c = new Lwjgl3ApplicationConfiguration();
        c.setTitle("MetajavBots");
        c.setWindowedMode(1600, 900);
        c.setResizable(true);
        c.setWindowSizeLimits(960, 600, -1, -1);
        c.useVsync(true);
        c.setForegroundFPS(60);
        // Bordes suaves en las formas (MSAA x4).
        c.setBackBufferConfig(8, 8, 8, 8, 16, 0, 4);
        c.setAudioConfig(32, 2048, 9);
        new Lwjgl3Application(new Juego(), c);
    }
}
