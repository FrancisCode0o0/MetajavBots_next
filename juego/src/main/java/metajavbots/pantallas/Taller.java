package metajavbots.pantallas;

import java.util.ArrayList;
import java.util.List;

import metajavbots.Juego;
import metajavbots.codigo.Compilador;
import metajavbots.codigo.RobotDelJugador;
import metajavbots.combate.Combatiente;
import metajavbots.pantalla.CampoDeTexto;
import metajavbots.pantalla.Pantalla;
import metajavbots.pantalla.Pincel;
import metajavbots.pantalla.Ui;
import metajavbots.pantalla.VentanaDeCodigo;
import metajavbots.pantalla.VistaDeArena;
import metajavbots.piezas.CatalogoDePiezas;
import metajavbots.piezas.Modelo;
import metajavbots.piezas.Montaje;
import metajavbots.piezas.Pieza;
import metajavbots.progreso.Jugador;
import metajavbots.progreso.Progreso;
import metajavbots.textos.T;

/**
 * TALLER — Donde se programa y se monta el robot: el editor de código con su
 * barra (compilar, formatear, buscar, la API al lado, exportar el .java),
 * las piezas de cada hueco y el modelo. Aquí también se crean los robots
 * que se van ganando (uno en Noob, hasta cuatro en Master).
 */
public final class Taller implements Pantalla {

    private final VentanaDeCodigo editor = new VentanaDeCodigo();
    private final PanelDeApi api = new PanelDeApi();
    private boolean conApi = true;
    private int pestana;
    private int robotCargado = -1;
    private volatile Compilador.Resultado resultado;
    private volatile boolean compilando;
    private String aviso = "";
    private float t;
    private VistaDeArena vista;
    private final CampoDeTexto nombreNuevo = new CampoDeTexto();
    private int modeloNuevo;
    private boolean creando;
    private final RobotDelJugador probador = new RobotDelJugador();
    private final Charla charla = new Charla();

    @Override
    public void alEntrar(Juego j) {
        vista = new VistaDeArena(j.pincel);
        editor.palabras = Palabras.api(j.jugador.idioma);
        editor.tamano = j.jugador.tamanoDeLetra;
        editor.alCompilar = () -> compilar(j);
        editor.alPedirAyuda = palabra -> {
            conApi = true;
            api.buscar(palabra);
        };
        cargar(j);
    }

    @Override
    public void alSalir(Juego j) {
        guardarCodigo(j);
    }

    private void cargar(Juego j) {
        Jugador.Robot r = j.jugador.robot();
        if (r == null) return;
        robotCargado = j.jugador.robotElegido;
        editor.ponerTexto(r.codigo);
    }

    private void guardarCodigo(Juego j) {
        Jugador.Robot r = j.jugador.robot();
        if (r != null && robotCargado == j.jugador.robotElegido) r.codigo = editor.doc.texto();
        j.guardar();
    }

    private void compilar(Juego j) {
        if (compilando) return;
        if (!Compilador.hayCompilador()) {
            aviso = T.t("taller.sin_jdk");
            return;
        }
        guardarCodigo(j);
        compilando = true;
        j.sonidos.tocar("compilar", 0.5f);
        String codigo = editor.doc.texto();
        Thread h = new Thread(() -> {
            resultado = probador.compilar(codigo, j.jugador.idioma.locale());
            compilando = false;
        }, "compilar");
        h.setDaemon(true);
        h.start();
    }

    @Override
    public void fotograma(Juego j, float dt) {
        t += dt;
        Pincel p = j.pincel;
        Ui ui = j.ui;
        Jugador jug = j.jugador;
        if (resultado != null) {
            Compilador.Resultado r = resultado;
            resultado = null;
            editor.marcas.clear();
            for (Compilador.Fallo f : r.errores) {
                editor.marcas.add(new VentanaDeCodigo.Marca((int) f.linea, (int) f.columna, Explicador.explicar(f, jug.etiqueta, jug.idioma), f.grave));
            }
            aviso = r.bien ? T.f("taller.compilado_ok", r.milisegundos) : T.f("pelea.errores", r.graves().size());
            j.sonidos.tocar(r.bien ? "bien" : "mal", 0.6f);
        }
        Comun.fondo(j, t);
        float arriba = Comun.cabecera(j, T.t("menu.taller"), true);
        // A la izquierda: los robots de la partida.
        float iz = 20, ancho = 300, y = arriba + 16;
        ui.panel(iz, y, ancho, p.alto - y - 20, T.t("taller.robots"));
        float ry = y + 60;
        int maximo = Progreso.robotsQuePuedeTener(jug);
        for (int i = 0; i < jug.robots.size(); i++) {
            Jugador.Robot r = jug.robots.get(i);
            if (ui.boton(r.nombre, iz + 14, ry, ancho - 28, 46, i == jug.robotElegido ? Ui.Estilo.ELEGIDO : Ui.Estilo.PLANO)) {
                guardarCodigo(j);
                jug.robotElegido = i;
                cargar(j);
                creando = false;
            }
            ry += 54;
        }
        if (jug.robots.size() < maximo) {
            if (ui.boton("+ " + T.t("taller.nuevo_robot"), iz + 14, ry, ancho - 28, 46, Ui.Estilo.PRINCIPAL)) creando = true;
            ry += 54;
        } else if (jug.robots.size() < 4) {
            p.parrafo(T.f("taller.mas_robots", jug.etiqueta.robotsPermitidos(), Progreso.VICTORIAS_POR_ROBOT[Math.min(3, jug.robots.size())]),
                    iz + 18, ry, ancho - 36, ui.letra * 0.75f, ui.tema.textoApagado, 1);
            ry += 80;
        }
        Jugador.Robot r = jug.robot();
        if (r != null) {
            vista.robotSuelto(p, r.montaje(), iz + ancho / 2, p.alto - 70, 2.6f, t, 0, true);
        }
        float x = iz + ancho + 20, w = p.ancho - x - 20;
        if (creando) {
            nuevoRobot(j, x, y, w, p.alto - y - 20);
            return;
        }
        String[] pest = {T.t("taller.codigo"), T.t("taller.piezas"), T.t("taller.modelo"), T.t("taller.charla")};
        int antes = pestana;
        pestana = ui.pestanas(pest, pestana, x, y, 720, 46);
        if (antes == 0 && pestana != 0) guardarCodigo(j);
        float cy = y + 56, ch = p.alto - cy - 20;
        if (r == null) return;
        if (pestana == 1) {
            Combatiente c = new Combatiente(r.nombre, r.montaje(), null);
            piezas(j, c, x, cy, w, ch);
            r.ponerPiezas(c.montaje);
            return;
        }
        if (pestana == 2) {
            modelos(j, r, x, cy, w, ch);
            return;
        }
        if (pestana == 3) {
            charla.dibujar(j, r, editor.doc.texto(), x, cy, w, ch);
            return;
        }
        // La barra del código: cada botón del ancho de su texto; si no caben (alemán, letra grande), en dos filas.
        String[] textos = {compilando ? T.t("taller.compilando") : T.t("taller.compilar"), T.t("taller.formatear"), T.t("taller.buscar"),
            T.t("taller.api"), T.t("taller.exportar"), T.t("taller.plantilla"), T.t("taller.rival"), "⌨ " + T.t("taller.atajos")};
        Ui.Estilo[] estilos = {Ui.Estilo.PRINCIPAL, Ui.Estilo.PLANO, Ui.Estilo.PLANO, conApi ? Ui.Estilo.ELEGIDO : Ui.Estilo.PLANO, Ui.Estilo.PLANO,
            Ui.Estilo.PLANO, Ui.Estilo.PLANO, Ui.Estilo.PLANO};
        boolean[] activos = {!compilando, true, true, true, true, true, true, true};
        String[] ayudas = {T.t("taller.compilar_ayuda"), T.t("taller.formatear_ayuda"), "Ctrl+F · Ctrl+H", T.t("taller.api_ayuda"),
            T.t("taller.exportar_ayuda"), T.t("taller.plantilla_ayuda"), T.t("taller.rival_ayuda"), T.t("taller.atajos_ayuda")};
        switch (ui.botonera(textos, estilos, activos, ayudas, x, cy, w, 44)) {
            case 0: compilar(j); break;
            case 1: editor.doc.formatear(); break;
            case 2: editor.abrirBusqueda(false); break;
            case 3: conApi = !conApi; break;
            case 4:
                guardarCodigo(j);
                try {
                    aviso = T.f("taller.exportado", Exportar.exportar(r, jug.nombre));
                } catch (Exception e) {
                    aviso = e.toString();
                }
                break;
            case 5: editor.doc.cambiarTodo(Comun.plantilla(jug.idioma)); break;
            case 6: j.ir(new CodigoDelRival()); break;
            default: break;
        }
        // Debajo del editor, la pista (o lo que pasó al compilar), con su sitio: no pisa la última línea del código.
        String pie = aviso.isEmpty() ? T.t("taller.pista") : aviso;
        float tPie = ui.letra * 0.8f, altoPie = p.altoDeParrafo(pie, w, tPie) + 10;
        float ey = cy + ui.altoDeBotonera + 10, eh = p.alto - 20 - altoPie - ey;
        float ew = conApi ? w - 440 : w;
        editor.dibujar(ui, x, ey, ew, eh);
        if (conApi) api.dibujar(ui, jug.idioma, editor, x + ew + 12, ey, w - ew - 12, eh);
        p.parrafo(pie, x, p.alto - 14 - altoPie, w, tPie, aviso.isEmpty() ? ui.tema.textoApagado : ui.tema.textoSuave, 1);
    }

    private void nuevoRobot(Juego j, float x, float y, float w, float h) {
        Pincel p = j.pincel;
        Ui ui = j.ui;
        Jugador jug = j.jugador;
        ui.panel(x, y, w, h, T.t("taller.nuevo_robot"));
        List<Modelo> ms = Progreso.modelosDisponibles(jug);
        float cw = Math.min(200, (w - 40) / Math.max(1, ms.size()));
        for (int i = 0; i < ms.size(); i++) {
            float cx = x + 20 + i * cw;
            boolean es = i == modeloNuevo;
            if (es) p.caja(cx, y + 60, cw - 8, 300, 10, ui.tema.panelAlto, 1);
            vista.robotSuelto(p, new Montaje(ms.get(i)), cx + cw / 2 - 4, y + 300, 1.8f, t + i, 0, true);
            p.textoCentrado(ms.get(i).nombre(T.idioma), cx + cw / 2 - 4, y + 320, ui.letra * 0.8f, true, ui.tema.texto, 1);
            if (ui.e.clic(cx, y + 60, cw - 8, 300)) modeloNuevo = i;
        }
        p.texto(T.t("bienvenida.nombre_robot"), x + 20, y + 380, ui.letra, false, ui.tema.textoSuave, 1);
        boolean intro = nombreNuevo.dibujar(ui, x + 20, y + 410, 360, 50, ms.get(Math.min(modeloNuevo, ms.size() - 1)).nombre(T.idioma));
        if (ui.boton(T.t("taller.crear"), x + 400, y + 410, 180, 50, Ui.Estilo.PRINCIPAL) || intro) {
            Modelo m = ms.get(Math.min(modeloNuevo, ms.size() - 1));
            Jugador.Robot r = new Jugador.Robot(nombreNuevo.texto().isBlank() ? m.nombre(T.idioma) : nombreNuevo.texto().strip(), m.id);
            r.codigo = Comun.plantilla(jug.idioma);
            jug.robots.add(r);
            jug.robotElegido = jug.robots.size() - 1;
            cargar(j);
            creando = false;
            j.guardar();
            j.sonidos.tocar("subida", 0.5f);
        }
        // Esc cierra este panel (no se va del taller).
        ui.reservarEscape();
        if (ui.boton(T.t("comun.cancelar"), x + 600, y + 410, 160, 50, Ui.Estilo.PLANO) || ui.e.tecla(com.badlogic.gdx.Input.Keys.ESCAPE)) creando = false;
    }

    private void modelos(Juego j, Jugador.Robot r, float x, float y, float w, float h) {
        Pincel p = j.pincel;
        Ui ui = j.ui;
        p.parrafo(T.t("taller.modelo_ayuda"), x, y, w, ui.letra * 0.85f, ui.tema.textoSuave, 1);
        List<Modelo> todos = Modelo.todos();
        int porFila = 8;
        float cw = w / porFila, chh = (h - 60) / 2;
        for (int i = 0; i < todos.size(); i++) {
            Modelo m = todos.get(i);
            float cx = x + (i % porFila) * cw, cy = y + 50 + (i / porFila) * chh;
            boolean disponible = Progreso.modeloDisponible(j.jugador, m), es = m.id.equals(r.modelo);
            p.caja(cx + 4, cy, cw - 8, chh - 10, 10, es ? ui.tema.panelAlto : ui.tema.panel, disponible ? 1 : 0.5f);
            if (es) p.bordeRedondeado(cx + 4, cy, cw - 8, chh - 10, 10, ui.tema.acento, 1, 2.5f);
            if (disponible) vista.robotSuelto(p, new Montaje(m), cx + cw / 2, cy + chh - 90, 1.6f, t + i, 0, true);
            else p.textoCentrado("?", cx + cw / 2, cy + chh / 2 - 40, 60, true, ui.tema.textoApagado, 1);
            p.textoCentrado(disponible ? m.nombre(T.idioma) : desbloqueo(m.desbloqueo), cx + cw / 2, cy + chh - 74, ui.letra * 0.75f, true,
                    disponible ? ui.tema.texto : ui.tema.textoApagado, 1);
            if (disponible) p.textoCentrado(m.naturaleza.nombre(T.idioma), cx + cw / 2, cy + chh - 48, ui.letra * 0.7f, false, m.naturaleza.color(), 1);
            if (disponible && ui.e.dentro(cx, cy, cw, chh)) ui.avisar(m.lema.en(T.idioma));
            if (disponible && !es && ui.e.clic(cx, cy, cw, chh)) {
                r.modelo = m.id;
                r.piezas.clear();
                j.sonidos.tocar("servo", 0.6f);
                j.guardar();
            }
        }
    }

    static String desbloqueo(String regla) {
        StringBuilder b = new StringBuilder();
        for (String parte : regla.split("\\+")) {
            int i = parte.indexOf(':');
            String que = i < 0 ? parte : parte.substring(0, i), v = i < 0 ? "" : parte.substring(i + 1);
            if (b.length() > 0) b.append(" + ");
            switch (que) {
                case "victorias": b.append(T.f("desbloqueo.victorias", v)); break;
                case "nivel": b.append(metajavbots.progreso.Etiqueta.de(v).nombre); break;
                case "jefe": b.append(T.f("desbloqueo.jefe", v)); break;
                case "reto": b.append(T.f("desbloqueo.reto", v)); break;
                case "pruebas": b.append(T.f("desbloqueo.pruebas", v)); break;
                case "desafio": b.append(T.t("desbloqueo.desafio")); break;
                default: b.append(parte); break;
            }
        }
        return b.toString();
    }

    /** Las piezas de cada hueco, con ◄ ► entre las que se pueden poner, y sus cifras. Lo usan el taller y la pausa entre rondas. */
    public static void piezas(Juego j, Combatiente c, float x, float y, float w, float h) {
        Pincel p = j.pincel;
        Ui ui = j.ui;
        Jugador jug = j.jugador;
        Montaje m = c.montaje;
        CatalogoDePiezas cat = CatalogoDePiezas.delJuego();
        float fila = Math.min(96, (h - 120) / Montaje.Hueco.values().length);
        float yy = y;
        for (Montaje.Hueco hueco : Montaje.Hueco.values()) {
            List<Pieza> opciones = new ArrayList<>();
            for (Pieza pz : cat.de(hueco.ranura)) if (Progreso.piezaUsable(jug, pz, m.modelo)) opciones.add(pz);
            Pieza actual = m.pieza(hueco);
            if (hueco == Montaje.Hueco.NATURALEZA) opciones.add(0, null);
            int i = opciones.indexOf(actual);
            p.caja(x, yy, w * 0.62f, fila - 8, 10, ui.tema.panel, 1);
            p.texto(T.t("hueco." + hueco.name().toLowerCase()), x + 16, yy + 10, ui.letra * 0.8f, false, ui.tema.textoSuave, 1);
            String nombre = actual == null ? T.t("taller.sin_pieza") : actual.nombre(T.idioma);
            p.texto(nombre, x + 16, yy + 34, ui.letra * 1.05f, true, actual == null ? ui.tema.textoApagado : ui.tema.texto, 1);
            if (actual != null) {
                p.texto(cifras(actual), x + 16, yy + 62, ui.letra * 0.7f, false, ui.tema.textoSuave, 1);
                p.circulo(x + w * 0.62f - 150, yy + 22, 7, actual.naturaleza.color(), 1);
                p.texto(actual.naturaleza.nombre(T.idioma), x + w * 0.62f - 138, yy + 12, ui.letra * 0.7f, false, ui.tema.textoSuave, 1);
            }
            if (ui.boton("◄", x + w * 0.62f - 110, yy + 30, 46, 40, Ui.Estilo.PLANO, opciones.size() > 1, null)) {
                int k = (i - 1 + opciones.size()) % opciones.size();
                m.poner(hueco, opciones.get(k));
                j.sonidos.tocar("servo", 0.4f);
            }
            if (ui.boton("►", x + w * 0.62f - 58, yy + 30, 46, 40, Ui.Estilo.PLANO, opciones.size() > 1, null)) {
                int k = (i + 1) % opciones.size();
                m.poner(hueco, opciones.get(k));
                j.sonidos.tocar("servo", 0.4f);
            }
            yy += fila;
        }
        // A la derecha: las cifras del robot montado.
        Montaje.Cifras cf = m.cifras();
        float cx = x + w * 0.62f + 20, cw = w * 0.38f - 20;
        float cy = ui.panel(cx, y, cw, h, T.t("taller.cifras"));
        String[] l = {
                T.f("cifras.blindaje", Math.round(cf.blindajeTotal())),
                T.f("cifras.velocidad", String.format(java.util.Locale.ROOT, "%.1f", cf.velocidad)),
                T.f("cifras.giro", String.format(java.util.Locale.ROOT, "%.1f", cf.giro)),
                T.f("cifras.pasos", Math.round(cf.pasos)),
                T.f("cifras.energia", Math.round(cf.energiaMax), String.format(java.util.Locale.ROOT, "%.2f", cf.recarga)),
                T.f("cifras.peso", Math.round(m.peso())),
                T.f("cifras.naturaleza", m.modelo.naturaleza.nombre(T.idioma), Math.round(m.refuerzo(m.modelo.naturaleza.refuerza) * 100)),
                cf.vuela ? T.t("cifras.vuela") : ""};
        for (String s : l) {
            if (s.isEmpty()) continue;
            p.texto(s, cx + 18, cy, ui.letra * 0.88f, false, ui.tema.texto, 1);
            cy += ui.letra * 1.6f;
        }
        cy += 10;
        p.parrafo(T.t("taller.piezas_ayuda"), cx + 18, cy, cw - 36, ui.letra * 0.75f, ui.tema.textoApagado, 1);
    }

    static String cifras(Pieza p) {
        StringBuilder b = new StringBuilder();
        String[] claves = {"dano", "alcance", "enfriamiento", "energia", "radar", "cono", "velocidad", "giro", "pasos", "energiaMax", "blindaje", "peso",
                "refuerzo"};
        for (String k : claves) {
            double v = p.valor(k);
            if (v == 0) continue;
            if (b.length() > 0) b.append(" · ");
            b.append(T.t("valor." + k)).append(' ').append(v == Math.rint(v) ? Long.toString(Math.round(v)) : String.format(java.util.Locale.ROOT, "%.2f", v));
        }
        String arma = p.ranura == Pieza.Ranura.BRAZO ? p.arma().name() : p.ranura == Pieza.Ranura.CABEZA ? p.especial().name()
                : p.ranura == Pieza.Ranura.PIERNAS ? p.locomocion().name() : "";
        return (arma.isEmpty() || arma.equals("NINGUNO") ? "" : arma + "  ") + b;
    }
}
