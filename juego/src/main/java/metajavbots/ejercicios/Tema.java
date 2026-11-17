package metajavbots.ejercicios;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import metajavbots.textos.Idioma;
import metajavbots.textos.Trilingue;

/**
 * TEMA — Un tema de Java (res/datos/temas.txt): ordena la API, los
 * ejercicios, los tutoriales y el manual, de los primeros pasos a las
 * pruebas y el diseño. Cada tema se aprende en una etiqueta (Noob I…).
 */
public final class Tema {

    public final String id, bloque, etiqueta;
    public final int java;
    public final Trilingue nombre;

    Tema(String id, String bloque, String etiqueta, int java, Trilingue nombre) {
        this.id = id;
        this.bloque = bloque;
        this.etiqueta = etiqueta;
        this.java = java;
        this.nombre = nombre;
    }

    public String nombre(Idioma i) {
        return nombre.en(i);
    }

    public int numero() {
        try {
            return Integer.parseInt(id);
        } catch (NumberFormatException e) {
            return 99;
        }
    }

    /** El nombre del bloque en cada idioma. */
    public static String bloque(String bloque, Idioma i) {
        switch (bloque) {
            case "FUNDAMENTOS": return i.elegir("Fundamentos", "Grundlagen", "Fundamentals");
            case "CONTROL": return i.elegir("Control del flujo", "Kontrollfluss", "Control flow");
            case "DATOS": return i.elegir("Datos", "Daten", "Data");
            case "POO": return i.elegir("Programación orientada a objetos", "Objektorientierte Programmierung", "Object-oriented programming");
            case "AVANZADO": return i.elegir("Avanzado", "Fortgeschritten", "Advanced");
            case "PROFESIONAL": return i.elegir("Profesional", "Professionell", "Professional");
            default: return bloque;
        }
    }

    private static List<Tema> todos;

    public static synchronized List<Tema> todos() {
        if (todos == null) {
            List<Tema> l = new ArrayList<>();
            try (InputStream in = Tema.class.getResourceAsStream("/datos/temas.txt");
                 BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String linea;
                while ((linea = r.readLine()) != null) {
                    linea = linea.strip();
                    if (linea.isEmpty() || linea.startsWith("#")) continue;
                    String[] p = linea.split("\\|");
                    if (p.length < 7) continue;
                    l.add(new Tema(p[0].strip(), p[1].strip(), p[2].strip(), Integer.parseInt(p[3].strip()),
                            new Trilingue(p[4].strip(), p[5].strip(), p[6].strip())));
                }
            } catch (IOException | RuntimeException e) {
                throw new IllegalStateException("No se pudo leer res/datos/temas.txt", e);
            }
            todos = Collections.unmodifiableList(l);
        }
        return todos;
    }

    public static Tema de(String id) {
        for (Tema t : todos()) if (t.id.equals(id)) return t;
        return null;
    }
}
