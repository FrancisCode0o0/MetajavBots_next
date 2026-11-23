package metajavbots.pantallas;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.badlogic.gdx.Input;

import metajavbots.Juego;
import metajavbots.codigo.TraductorDeApi;
import metajavbots.pantalla.Pantalla;
import metajavbots.pantalla.Pincel;
import metajavbots.pantalla.Ui;
import metajavbots.progreso.Jugador;
import metajavbots.textos.Idioma;
import metajavbots.textos.T;
import metajavbots.textos.Trilingue;
import metajavbots.tutor.CabezaDeEntrenador;
import metajavbots.tutor.Concepto;
import metajavbots.tutor.Entrenador;

/**
 * ¿CÓMO SE JUEGA? — La tarjeta que cada modo enseña en su pantalla antes de
 * empezar (res/datos/como_se_juega.txt): el objetivo, cómo se gana, una nota
 * con el tropiezo típico, los métodos del robot que importan ahí (en la API del
 * idioma del jugador), una idea en pseudocódigo y las fichas de sus mecánicas.
 * Sale sola la primera vez que se entra en el modo (Jugador.comoSeJuegaVisto) y
 * luego con el botón «¿Cómo se juega?» de la cabecera (o de la barra de la
 * pelea). Corta, encima de todo y con un solo botón para seguir: clara, pero
 * nunca un muro de texto.
 */
public final class ComoSeJuega {

    /** Lo que cuenta un modo. */
    public record Modo(String id, Trilingue titulo, Trilingue objetivo, Trilingue gana, Trilingue nota, List<String> metodos,
            List<String> mecanicas, Map<Idioma, String> pseudo) {

        /** El pseudocódigo en ese idioma. */
        public String pseudo(Idioma i) {
            return pseudo.getOrDefault(i, "");
        }

        /** Los métodos del robot que importan, con los nombres de la API de ese idioma. */
        public List<String> metodos(Idioma i) {
            List<String> l = new ArrayList<>();
            for (String m : metodos) l.add(TraductorDeApi.traducir(m, i));
            return l;
        }
    }

    private static Map<String, Modo> modos;

    private boolean abierto;
    private Modo modo;
    private float tiempo;
    private final Ui.Desplazable despl = new Ui.Desplazable();

    /** Todos los modos con su «¿Cómo se juega?», en el orden del archivo. */
    public static synchronized Map<String, Modo> todos() {
        if (modos == null) modos = Collections.unmodifiableMap(leer());
        return modos;
    }

    /** El de ese id (o null). */
    public static Modo de(String id) {
        return id == null ? null : todos().get(id);
    }

    /** El id del «¿Cómo se juega?» de esa pantalla, si tiene uno que exista (o null). */
    public static String idDe(Pantalla p) {
        String id = p == null ? null : p.comoSeJuega();
        return de(id) == null ? null : id;
    }

    /** Al entrar en una pantalla: si su modo tiene «¿Cómo se juega?» y el jugador aún no lo ha visto, sale solo. */
    public void alEntrar(Juego j, Pantalla p) {
        if (j == null || j.jugador == null) return;
        String id = idDe(p);
        if (id != null && !j.jugador.comoSeJuegaVisto.contains(id)) abrir(j, id);
    }

    /** Lo abre (con el botón «¿Cómo se juega?»). */
    public void abrir(Juego j, String id) {
        Modo m = de(id);
        if (m == null) return;
        modo = m;
        abierto = true;
        tiempo = 0;
        despl.alPrincipio();
        if (j != null && j.jugador != null) j.jugador.comoSeJuegaVisto.add(id);
    }

    public boolean abierto() {
        return abierto;
    }

    /** El modo que enseña (o null). */
    public Modo modo() {
        return abierto ? modo : null;
    }

    public void cerrar() {
        abierto = false;
    }

    /** Bloquea la entrada de la pantalla de debajo mientras está abierto. */
    public void antes(Juego j) {
        if (abierto && j != null && j.entrada != null) j.entrada.bloqueada = true;
    }

    /** La tarjeta, en el centro, sobre la pantalla oscurecida. */
    public void dibujar(Juego j, float dt) {
        if (!abierto || modo == null || j == null || j.jugador == null) return;
        tiempo += dt;
        j.entrada.bloqueada = false;
        Pincel p = j.pincel;
        Ui ui = j.ui;
        Jugador jug = j.jugador;
        Idioma id = jug.idioma;
        p.rect(0, 0, p.ancho, p.alto, 0x000000, 0.62f);
        float w = Math.min(1000f, p.ancho - 60f), h = Math.min(p.alto - 60f, Math.max(420f, p.alto * 0.86f));
        float x = (p.ancho - w) / 2f, y = (p.alto - h) / 2f;
        p.caja(x + 4, y + 6, w, h, 16, 0x000000, 0.42f);
        p.caja(x, y, w, h, 16, ui.tema.panelAlto, 1f);
        p.bordeRedondeado(x, y, w, h, 16, ui.tema.acento, 1f, 2.5f);
        Entrenador en = Entrenador.de(jug.entrenador);
        float radio = 34;
        CabezaDeEntrenador.dibujar(p, en, x + 28 + radio, y + 24 + radio, radio, tiempo < 3, tiempo);
        float tx = x + 28 + radio * 2 + 20, tw = w - (tx - x) - 28;
        String cab = T.t("como.titulo");
        p.texto(cab, tx, y + 22, Math.min(18, ui.letra * 0.8f), true, ui.tema.textoSuave, 1);
        String tit = modo.titulo().en(id);
        float tTit = ui.cabe(tit, tw, Math.min(34, ui.letra * 1.5f), true);
        p.texto(tit, tx, y + 46, tTit, true, ui.tema.acento, 1);
        float arriba = y + 24 + radio * 2 + 18, abajo = y + h - 86;
        float d = ui.desplazable(despl, x + 16, arriba, w - 32, abajo - arriba);
        float cy = arriba - d, cx = x + 32, cw = w - 76;
        float inicio = cy;
        float tL = Math.min(18, ui.letra * 0.8f), tT = Math.min(24, ui.letra * 1.02f);
        cy = bloque(p, ui, T.t("como.objetivo"), modo.objetivo().en(id), cx, cy, cw, tL, tT);
        cy = bloque(p, ui, T.t("como.gana"), modo.gana().en(id), cx, cy, cw, tL, tT);
        List<String> ms = modo.metodos(id);
        if (!ms.isEmpty()) cy = bloque(p, ui, T.t("como.metodos"), String.join(" · ", ms), cx, cy, cw, tL, tT);
        String pseudo = modo.pseudo(id);
        if (!pseudo.isBlank()) {
            cy += p.parrafo(T.t("como.idea"), cx, cy, cw, tL, true, ui.tema.textoSuave, 1) + 6;
            float tCod = Math.min(21, ui.letra * 0.9f);
            float altoCod = pseudocodigo(p, pseudo, cx + 14, cy + 11, cw - 28, tCod, 0xE8E2C8, false);
            p.caja(cx, cy, cw, altoCod + 22, 10, 0x1B2430, 1);
            pseudocodigo(p, pseudo, cx + 14, cy + 11, cw - 28, tCod, 0xE8E2C8, true);
            cy += altoCod + 36;
        }
        cy = bloque(p, ui, T.t("como.ojo"), modo.nota().en(id), cx, cy, cw, tL, tT);
        cy = fichas(j, cx, cy, cw, tL);
        ui.finDesplazable(despl, cy - inicio + 10);
        botones(j, x, y, w, h);
    }

    /**
     * El pseudocódigo línea a línea, conservando la sangría (que dice qué va dentro de qué): cada línea partida si no
     * cabe, con su sangría. Devuelve el alto; si no {@code dibujar}, solo lo mide.
     */
    static float pseudocodigo(Pincel p, String s, float x, float y, float w, float tam, int color, boolean dibujar) {
        float yy = y, paso = tam * 0.6f;
        for (String linea : s.split("\n", -1)) {
            int n = 0;
            while (n < linea.length() && linea.charAt(n) == ' ') n++;
            String resto = linea.substring(n);
            float dx = Math.min(w * 0.5f, n * paso);
            if (resto.isEmpty()) {
                yy += p.altoDeLinea(tam);
                continue;
            }
            yy += dibujar ? p.parrafoDeCodigo(resto, x + dx, yy, w - dx, tam, false, color, 1)
                    : p.altoDeParrafoDeCodigo(resto, w - dx, tam, false);
        }
        return yy - y;
    }

    private static float bloque(Pincel p, Ui ui, String etiqueta, String texto, float x, float y, float w, float tL, float tT) {
        y += p.parrafo(etiqueta, x, y, w, tL, true, ui.tema.textoSuave, 1) + 2;
        return y + p.parrafo(texto, x, y, w, tT, ui.tema.texto, 1) + 16;
    }

    /** Las fichas de sus mecánicas, como botones que abren el índice en esa ficha. */
    private float fichas(Juego j, float x, float y, float w, float tL) {
        if (modo.mecanicas().isEmpty()) return y;
        Pincel p = j.pincel;
        Ui ui = j.ui;
        y += p.parrafo(T.t("como.mecanicas"), x, y, w, tL, true, ui.tema.textoSuave, 1) + 6;
        float bx = x, bh = Math.max(40, ui.letra * 1.8f);
        for (String f : modo.mecanicas()) {
            Concepto c = Concepto.de(f);
            if (c == null) continue;
            String t = c.titulo.en(T.idioma);
            float bw = Math.min(w, p.ancho(t, Math.min(ui.letra, 21), true) + 34);
            if (bx + bw > x + w) {
                bx = x;
                y += bh + 8;
            }
            if (ui.boton(t, bx, y, bw, bh, Ui.Estilo.PLANO, true, null)) {
                cerrar();
                j.ir(new Mecanicas(f));
            }
            bx += bw + 10;
        }
        return y + bh + 12;
    }

    private void botones(Juego j, float x, float y, float w, float h) {
        Ui ui = j.ui;
        float by = y + h - 70, bw = Math.min(320, (w - 76) / 2);
        boolean seguir = ui.boton(T.t("como.seguir"), x + w - 28 - bw, by, bw, 52, Ui.Estilo.PRINCIPAL, true, null);
        boolean todas = !"mecanicas".equals(modo.id())
                && ui.boton(T.t("como.todas"), x + 28, by, bw, 52, Ui.Estilo.NORMAL, true, T.t("como.todas_ayuda"));
        ui.reservarEscape();
        if (seguir || j.entrada.tecla(Input.Keys.ESCAPE) || j.entrada.tecla(Input.Keys.ENTER)) {
            cerrar();
            j.guardar();
        } else if (todas) {
            cerrar();
            j.guardar();
            j.ir(new Mecanicas());
        }
    }

    // ================================================================ leer el archivo

    private static Map<String, Modo> leer() {
        Map<String, Modo> m = new LinkedHashMap<>();
        try (InputStream in = ComoSeJuega.class.getResourceAsStream("/datos/como_se_juega.txt")) {
            if (in == null) return m;
            BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            String id = null, bloque = null, linea;
            Map<String, String> v = new LinkedHashMap<>();
            Map<String, List<String>> bloques = new LinkedHashMap<>();
            while ((linea = r.readLine()) != null) {
                if (linea.startsWith("#")) continue;
                if (linea.startsWith("=== ")) {
                    if (id != null) m.put(id, modo(id, v, bloques));
                    id = linea.substring(4).strip();
                    v = new LinkedHashMap<>();
                    bloques = new LinkedHashMap<>();
                    bloque = null;
                } else if (linea.startsWith("    ") && bloque != null) {
                    bloques.get(bloque).add(linea.substring(4).stripTrailing());
                } else if (!linea.isBlank() && linea.contains(":")) {
                    int i = linea.indexOf(':');
                    String clave = linea.substring(0, i).strip(), valor = linea.substring(i + 1).strip();
                    if (valor.isEmpty() && clave.startsWith("pseudo.")) {
                        bloque = clave;
                        bloques.put(clave, new ArrayList<>());
                    } else {
                        bloque = null;
                        v.put(clave, valor);
                    }
                }
            }
            if (id != null) m.put(id, modo(id, v, bloques));
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo leer res/datos/como_se_juega.txt", e);
        }
        return m;
    }

    private static Modo modo(String id, Map<String, String> v, Map<String, List<String>> bloques) {
        Map<Idioma, String> pseudo = new EnumMap<>(Idioma.class);
        for (Idioma i : Idioma.values()) {
            pseudo.put(i, String.join("\n", bloques.getOrDefault("pseudo." + i.codigo(), List.of())).stripTrailing());
        }
        return new Modo(id, tri(v, "titulo"), tri(v, "objetivo"), tri(v, "gana"), tri(v, "nota"), lista(v.get("metodos")),
                lista(v.get("mecanicas")), pseudo);
    }

    private static Trilingue tri(Map<String, String> v, String clave) {
        return new Trilingue(v.getOrDefault(clave + ".es", ""), v.getOrDefault(clave + ".de", ""), v.getOrDefault(clave + ".en", ""));
    }

    private static List<String> lista(String s) {
        List<String> l = new ArrayList<>();
        if (s == null) return l;
        for (String x : s.split(",")) if (!x.isBlank()) l.add(x.strip());
        return l;
    }
}
