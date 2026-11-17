package metajavbots.pantallas;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import metajavbots.api.Enemigo;
import metajavbots.api.Enemy;
import metajavbots.api.Gegner;
import metajavbots.api.Golpe;
import metajavbots.api.Hit;
import metajavbots.api.Metajavbot;
import metajavbots.api.MetajavbotDe;
import metajavbots.api.MetajavbotEn;
import metajavbots.api.Treffer;
import metajavbots.textos.Idioma;

/** PALABRAS — Lo que propone el autocompletado del editor: la API del idioma del jugador y lo más usado de Java. */
public final class Palabras {

    private Palabras() {
    }

    private static final String[] JAVA = {"System.out.println", "String", "Math.abs", "Math.max", "Math.min", "Math.sqrt", "Math.hypot",
            "Math.atan2", "Math.toDegrees", "Math.toRadians", "Math.random", "Integer", "Double", "Boolean", "ArrayList", "HashMap", "HashSet",
            "List", "Map", "Set", "Override", "public", "private", "protected", "static", "final", "return", "extends", "implements",
            "interface", "abstract", "record", "switch", "default", "instanceof"};

    public static List<String> api(Idioma idioma) {
        Set<String> s = new LinkedHashSet<>();
        Class<?>[] clases;
        switch (idioma) {
            case DE: clases = new Class<?>[]{MetajavbotDe.class, Gegner.class, Treffer.class}; break;
            case EN: clases = new Class<?>[]{MetajavbotEn.class, Enemy.class, Hit.class}; break;
            default: clases = new Class<?>[]{Metajavbot.class, Enemigo.class, Golpe.class}; break;
        }
        for (Class<?> c : clases) {
            s.add(c.getSimpleName());
            for (Method m : c.getDeclaredMethods()) {
                if (!Modifier.isPublic(m.getModifiers())) continue;
                s.add(m.getName() + "(" + (m.getParameterCount() == 0 ? ")" : ""));
            }
        }
        for (String j : JAVA) s.add(j);
        return new ArrayList<>(s);
    }
}
