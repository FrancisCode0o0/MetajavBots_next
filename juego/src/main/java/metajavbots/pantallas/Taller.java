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
    private final PanelDePaquetes paquetes = new PanelDePaquetes();
    private final BancoDePruebas banco = new BancoDePruebas();
    private boolean conApi = true;
    private boolean conApiAntes = true;
    private boolean enBanco;
    private boolean abrirBancoAlEntrar;
    private int pestana;
    private int pestanaAnterior;
    private int robotCargado = -1;
    private volatile Compilador.Resultado resultado;
    private volatile String codigoEnCompilacion = "";
    private volatile boolean compilando;
    private String aviso = "";
    private float t;
    private VistaDeArena vista;
    private final CampoDeTexto nombreNuevo = new CampoDeTexto();
    private int modeloNuevo;
    private boolean creando;
    private final RobotDelJugador probador = new RobotDelJugador();
    private final Charla charla = new Charla();
    private final PanelDelTutor tutor = new PanelDelTutor();
    /** Lo que ocupa la lista de robots (el tutor va debajo). */
    private float robotsAlto = 140;

    /** Plantilla inicial del taller para el idioma y modelo elegidos (incluye su arquetipo y habilidad propia). */
    public static String plantillaInicial(metajavbots.textos.Idioma idioma, Modelo modelo) {
        return Comun.plantilla(idioma, modelo);
    }

    /** Abre el taller directamente con el banco de pruebas rápido activo (para las capturas). */
    public static Taller conBanco() {
        Taller taller = new Taller();
        taller.abrirBancoAlEntrar = true;
        return taller;
    }

    /** Abre el taller directamente en la pestaña de piezas (para las capturas del peso frente a la carga). */
    public static Taller enPiezas() {
        Taller taller = new Taller();
        taller.pestana = 1;
        taller.pestanaAnterior = 1;
        return taller;
    }

    /** Abre el taller en la pestaña de piezas avisando de que el robot pesa demasiado para pelear (Juego.ir lo usa). */
    public static Taller porSobrepeso() {
        Taller taller = new Taller();
        taller.pestana = 1;
        taller.pestanaAnterior = 1;
        taller.aviso = T.t("peso.no_puede_pelear");
        return taller;
    }

    /** Abre el taller directamente en la pestaña de documentación Javadoc (para las capturas). */
    public static Taller enPaquetes() {
        Taller taller = new Taller();
        taller.pestanaAnterior = 0;
        taller.pestana = 4;
        return taller;
    }

    /** Índice de la pestaña activa (0 código, 1 piezas, 2 modelo, 3 charla, 4 documentación). */
    public int pestanaActual() {
        return pestana;
    }

    /** Pestaña del taller en la que estaba el jugador antes de abrir la documentación. */
    public int pestanaAnterior() {
        return pestanaAnterior;
    }

    /** Cambia a una pestaña del taller recordando la anterior si se entra en documentación. */
    public void irAPestana(int nueva) {
        if (nueva == 4) {
            abrirDocumentacion();
        } else if (nueva >= 0 && nueva < 4) {
            pestana = nueva;
            pestanaAnterior = nueva;
        }
    }

    /** Abre la vista de documentación recordando en qué pestaña del taller estaba el jugador. */
    public void abrirDocumentacion() {
        if (pestana != 4) pestanaAnterior = pestana;
        pestana = 4;
    }

    /**
     * Retrocede un nivel dentro de la documentación; si ya estaba en el índice, vuelve a la pestaña anterior del taller
     * sin sacar al jugador del Taller. Devuelve true si la acción se atendió dentro de la documentación.
     */
    public boolean retrocederEnDocumentacion() {
        if (pestana != 4) return false;
        if (!paquetes.retroceder()) pestana = pestanaAnterior;
        return true;
    }

    /** Acceso al panel de documentación (útil para pruebas automatizadas). */
    public PanelDePaquetes panelDePaquetes() {
        return paquetes;
    }

    @Override
    public void alEntrar(Juego j) {
        vista = new VistaDeArena(j.pincel);
        editor.palabras = Palabras.api(j.jugador.idioma);
        // Como un IDE: la documentación al pasar el ratón y los errores mientras se escribe (se compila sin cargar el robot).
        editor.documentacion = w -> Referencia.de(w, j.jugador.idioma);
        editor.revisarAlEscribir = codigo -> {
            if (!Compilador.hayCompilador()) return null;
            List<VentanaDeCodigo.Marca> l = new ArrayList<>();
            for (Compilador.Fallo f : Compilador.compilar(codigo, false, metajavbots.codigo.Vigilante.Modo.ROBOT, j.jugador.idioma.locale()).errores) {
                l.add(Explicador.marca(f, j.jugador));
            }
            return l;
        };
        editor.tamano = j.jugador.tamanoDeLetra;
        editor.alCompilar = () -> compilar(j);
        editor.alPedirAyuda = palabra -> {
            if (enBanco) cerrarBanco(false);
            conApi = true;
            api.buscar(palabra);
        };
        cargar(j);
        if (abrirBancoAlEntrar) {
            abrirBancoAlEntrar = false;
            conApiAntes = conApi;
            conApi = false;
            enBanco = true;
            probador.cargarDesdeJugador(j.jugador.robot(), j.jugador.idioma.locale(), true);
            banco.reiniciar(j.jugador, j.jugador.robot(), probador, j.pincel);
            for (int i = 0; i < 15; i++) banco.avanzarTiempo(0.1f);
        }
    }

    @Override
    public String contextoParaElEntrenador() {
        if (pestana == 4) return paquetes.contextoParaIa();
        return EntrenadorFlotante.contextoDeCodigo("The player is in the workshop, programming their fighting robot.", editor);
    }

    @Override
    public String codigoParaConsejos() {
        return editor.doc.texto();
    }

    @Override
    public void alSalir(Juego j) {
        cerrarBanco(false);
        paquetes.desenfocar(j.ui);
        guardarCodigo(j);
    }

    private void cargar(Juego j) {
        Jugador.Robot r = j.jugador.robot();
        if (r == null) return;
        robotCargado = j.jugador.robotElegido;
        editor.ponerTexto(r.codigo);
        probador.cargarDesdeJugador(r, j.jugador.idioma.locale(), enBanco);
    }

    private void guardarCodigo(Juego j) {
        Jugador.Robot r = j.jugador.robot();
        if (r != null && robotCargado == j.jugador.robotElegido) r.codigo = editor.doc.texto();
        j.guardar();
    }

    private void compilar(Juego j) {
        lanzarCompilacion(j, true);
    }

    private void lanzarCompilacion(Juego j, boolean guardar) {
        if (compilando) return;
        if (!Compilador.hayCompilador()) {
            aviso = T.t("taller.sin_jdk");
            return;
        }
        if (guardar) guardarCodigo(j);
        compilando = true;
        j.sonidos.tocar("compilar", 0.5f);
        String codigo = editor.doc.texto();
        codigoEnCompilacion = codigo;
        boolean conTraza = enBanco;
        Thread h = new Thread(() -> {
            resultado = probador.compilar(codigo, j.jugador.idioma.locale(), conTraza);
            compilando = false;
        }, "compilar");
        h.setDaemon(true);
        h.start();
    }

    private void alternarBanco(Juego j) {
        if (enBanco) {
            cerrarBanco(true);
            return;
        }
        conApiAntes = conApi;
        conApi = false;
        enBanco = true;
        probador.cargarDesdeJugador(j.jugador.robot(), j.jugador.idioma.locale(), true);
        banco.reiniciar(j.jugador, j.jugador.robot(), probador, j.pincel);
    }

    private void cerrarBanco(boolean restaurarApi) {
        enBanco = false;
        banco.limpiar();
        editor.lineasActivas.clear();
        editor.lineaActiva = 0;
        if (restaurarApi) conApi = conApiAntes;
    }

    @Override
    public void fotograma(Juego j, float dt) {
        t += dt;
        Pincel p = j.pincel;
        Ui ui = j.ui;
        Jugador jug = j.jugador;
        atenderCompilacion(j);
        Comun.fondo(j, t);
        float arriba = Comun.cabecera(j, T.t("menu.taller"), true);
        // A la izquierda: los robots de la partida.
        float iz = 20, ancho = 300, y = arriba + 16;
        robotsAlto = robots(j, iz, y, ancho);
        Jugador.Robot r = jug.robot();
        // El modo tutor, con su entrenador, entre la lista de robots y el robot (que queda abajo, más pequeño).
        float alturaTutor = p.alto - 160 - (y + robotsAlto);
        if (alturaTutor > 260) tutor.dibujar(j, editor, iz + 12, y + robotsAlto, ancho - 24, alturaTutor, dt);
        if (r != null) {
            vista.robotSuelto(p, r.montaje(), iz + ancho / 2, p.alto - 34, alturaTutor > 260 ? 1.5f : 2.6f, t, 0, true);
        }
        float x = iz + ancho + 20, w = p.ancho - x - 20;
        if (creando) {
            nuevoRobot(j, x, y, w, p.alto - y - 20);
            return;
        }
        String[] pest = {T.t("taller.codigo"), T.t("taller.piezas"), T.t("taller.modelo"), T.t("taller.charla")};
        float anchoDoc = Math.min(250, Math.max(200, w * 0.26f));
        float anchoPest = Math.max(480, Math.min(700, w - anchoDoc - 4));
        int antes = pestana;
        int elegida = ui.pestanas(pest, pestana, x, y, anchoPest, 46);
        if (elegida != pestana) {
            pestana = elegida;
            if (pestana < 4) pestanaAnterior = pestana;
        }
        if (pestanaPaquetes(ui, x + anchoPest + 4, y, Math.min(anchoDoc, w - anchoPest - 4), 46, pestana == 4)) {
            abrirDocumentacion();
        }
        if (antes == 0 && pestana != 0) {
            guardarCodigo(j);
            if (enBanco) cerrarBanco(true);
        }
        if (antes == 4 && pestana != 4) paquetes.desenfocar(ui);
        float cy = y + 56, ch = p.alto - cy - 20;
        if (r == null) return;
        if (pestana == 1) {
            Combatiente c = new Combatiente(r.nombre, jug.montajeDe(r), null);
            piezas(j, c, x, cy, w, ch, true);
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
        if (pestana == 4) {
            if (paquetes.dibujar(ui, jug.idioma, x, cy, w, ch)) {
                paquetes.desenfocar(ui);
                pestana = pestanaAnterior;
            }
            return;
        }
        pestanaCodigo(j, r, x, cy, w, dt);
    }

    private static boolean pestanaPaquetes(Ui ui, float x, float y, float w, float h, boolean elegida) {
        Pincel p = ui.p;
        boolean encima = ui.e.dentro(x, y, w, h);
        int fondo = elegida ? 0xCC8E5C : encima ? 0xC28454 : 0xB87A4B;
        p.rect(x, y, w, h, fondo, 1);
        if (elegida) p.rect(x, y + h - 3, w, 3, ui.tema.acento, 1);
        String txt = T.t("taller.paquetes");
        float t = ui.cabe(txt, w - 16, Math.min(ui.letra, h * 0.5f), true);
        p.textoCentrado(ui.recortarTexto(txt, w - 16, t, true), x + w / 2, y + (h - t * 1.15f) / 2, t, true, 0x14161A, 1);
        if (ui.e.clic(x, y, w, h)) {
            if (ui.sonidoDeClic != null) ui.sonidoDeClic.run();
            return true;
        }
        return false;
    }

    private void pestanaCodigo(Juego j, Jugador.Robot r, float x, float cy, float w, float dt) {
        Pincel p = j.pincel;
        Ui ui = j.ui;
        Jugador jug = j.jugador;
        // La barra del código: cada botón del ancho de su texto; si no caben (alemán, letra grande), en dos filas.
        String[] textos = {compilando ? T.t("taller.compilando") : T.t("taller.compilar"), T.t("taller.probar_aqui"), T.t("taller.formatear"),
            T.t("taller.buscar"), T.t("taller.api"), T.t("taller.exportar"), T.t("taller.plantilla"), T.t("taller.rival"), "⌨ " + T.t("taller.atajos")};
        Ui.Estilo[] estilos = {Ui.Estilo.COMPILAR, Ui.Estilo.DESTACADO, Ui.Estilo.PLANO, Ui.Estilo.PLANO,
            conApi && !enBanco ? Ui.Estilo.ELEGIDO : Ui.Estilo.PLANO, Ui.Estilo.PLANO, Ui.Estilo.PLANO, Ui.Estilo.PLANO, Ui.Estilo.PLANO};
        boolean[] activos = {!compilando, true, true, true, true, true, true, true, true};
        String[] ayudas = {T.t("taller.compilar_ayuda"), T.t("taller.probar_aqui_ayuda"), T.t("taller.formatear_ayuda"), "Ctrl+F · Ctrl+H",
            T.t("taller.api_ayuda"), T.t("taller.exportar_ayuda"), T.t("taller.plantilla_ayuda"), T.t("taller.rival_ayuda"), T.t("taller.atajos_ayuda")};
        switch (ui.botonera(textos, estilos, activos, ayudas, x, cy, w, 44)) {
            case 0: compilar(j); break;
            case 1: alternarBanco(j); break;
            case 2: editor.doc.formatear(); break;
            case 3: editor.abrirBusqueda(false); break;
            case 4:
                if (enBanco) {
                    cerrarBanco(false);
                    conApi = true;
                } else {
                    conApi = !conApi;
                }
                break;
            case 5:
                guardarCodigo(j);
                try {
                    aviso = T.f("taller.exportado", Exportar.exportar(r, jug.nombre));
                } catch (Exception e) {
                    aviso = e.toString();
                }
                break;
            case 6: editor.doc.cambiarTodo(plantillaInicial(jug.idioma, Modelo.de(r.modelo))); break;
            case 7: j.ir(new CodigoDelRival()); break;
            default: break;
        }
        // Recordatorio visible si el robot nunca se compiló o tiene cambios pendientes sin compilar.
        String recordatorio = AvisoDeCompilacion.recordatorioEnTaller(r, editor.doc.texto(), jug.idioma);
        float ey = cy + ui.altoDeBotonera + 10;
        if (!recordatorio.isEmpty()) {
            float tRec = Math.min(18f, ui.letra * 0.82f);
            float hRec = p.altoDeParrafo("⚠ " + recordatorio, w - 28f, tRec, true) + 12f;
            p.caja(x, ey, w, hRec, 8f, ui.tema.panelAlto, 0.96f);
            p.bordeRedondeado(x, ey, w, hRec, 8f, r.tieneCodigoCompilado() ? ui.tema.aviso : Ui.COMPILAR_FONDO, 1f, 1.5f);
            p.parrafo("⚠ " + recordatorio, x + 14f, ey + 6f, w - 28f, tRec, true,
                    r.tieneCodigoCompilado() ? ui.tema.aviso : Ui.COMPILAR_TEXTO, 1f);
            ey += hRec + 8f;
        }
        // Debajo del editor, la pista (o lo que pasó al compilar), con su sitio: no pisa la última línea del código.
        String pie = aviso.isEmpty() ? T.t("taller.pista") : aviso;
        float tPie = ui.letra * 0.8f, altoPie = p.altoDeParrafo(pie, w, tPie) + 10;
        float eh = Math.max(140f, p.alto - 34 - altoPie - ey);
        float anchoDerecho = enBanco ? Math.min(540, Math.max(400, w * 0.44f)) : conApi ? 440 : 0;
        float ew = anchoDerecho > 0 ? w - anchoDerecho : w;
        if (enBanco) banco.marcarLineas(editor);
        editor.dibujar(ui, x, ey, ew, eh);
        if (enBanco) {
            if (banco.dibujar(j, r, probador, compilando, x + ew + 12, ey, w - ew - 12, eh, dt)) cerrarBanco(true);
        } else if (conApi) {
            api.dibujar(ui, jug.idioma, editor, x + ew + 12, ey, w - ew - 12, eh);
        }
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
            r.codigo = plantillaInicial(jug.idioma, m);
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
        float bwBio = Math.min(260, p.ancho("✎ " + T.t("biografias.editar"), ui.letra * 0.85f, true) + 34);
        p.parrafo(T.t("taller.modelo_ayuda"), x, y, w - bwBio - 16, ui.letra * 0.85f, ui.tema.textoSuave, 1);
        if (ui.boton("✎ " + T.t("biografias.editar"), x + w - bwBio, y, bwBio, 40, Ui.Estilo.NORMAL, true, T.t("biografias.editar_ayuda"))) {
            j.ir(new PantallaBiografias(false, r.modelo));
        }
        List<Modelo> todos = Modelo.todos();
        int porFila = 8, filas = (todos.size() + porFila - 1) / porFila;
        float cw = w / porFila, chh = (h - 60) / filas;
        float talla = Math.min(1.6f, chh / 190);
        for (int i = 0; i < todos.size(); i++) {
            Modelo m = todos.get(i);
            float cx = x + (i % porFila) * cw, cy = y + 50 + (i / porFila) * chh;
            boolean disponible = Progreso.modeloDisponible(j.jugador, m), es = m.id.equals(r.modelo);
            p.caja(cx + 4, cy, cw - 8, chh - 10, 10, es ? ui.tema.panelAlto : ui.tema.panel, disponible ? 1 : 0.5f);
            if (es) p.bordeRedondeado(cx + 4, cy, cw - 8, chh - 10, 10, ui.tema.acento, 1, 2.5f);
            if (disponible) vista.robotSuelto(p, new Montaje(m), cx + cw / 2, cy + chh - 80, talla, t + i, 0, true);
            else p.textoCentrado("?", cx + cw / 2, cy + chh / 2 - 40, 60, true, ui.tema.textoApagado, 1);
            String nombre = disponible ? m.nombre(T.idioma) : desbloqueo(m.desbloqueo);
            float tn = ui.cabe(nombre, cw - 16, ui.letra * 0.75f, true);
            p.textoCentrado(ui.recortarTexto(nombre, cw - 16, tn, true), cx + cw / 2, cy + chh - 68, tn, true,
                    disponible ? ui.tema.texto : ui.tema.textoApagado, 1);
            if (disponible) p.textoCentrado(m.naturaleza.nombre(T.idioma), cx + cw / 2, cy + chh - 42, ui.letra * 0.7f, false, m.naturaleza.color(), 1);
            if (disponible && ui.e.dentro(cx, cy, cw, chh)) {
                ui.avisar(m.lema.en(T.idioma) + "\n\n" + metajavbots.tutor.Biografias.deRobot(m.id, T.idioma));
            }
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
                case "tienda": b.append(T.t("desbloqueo.tienda")); break;
                default: b.append(parte); break;
            }
        }
        return b.toString();
    }

    /** Las piezas de cada hueco, con ◄ ► entre las que se pueden poner, y sus cifras. Lo usan el taller y la pausa entre rondas. */
    public static void piezas(Juego j, Combatiente c, float x, float y, float w, float h) {
        piezas(j, c, x, y, w, h, false);
    }

    /** Igual que {@link #piezas(Juego, Combatiente, float, float, float, float)}; si {@code permitirActivar}, muestra botón para activar pendientes. */
    public static void piezas(Juego j, Combatiente c, float x, float y, float w, float h, boolean permitirActivar) {
        Pincel p = j.pincel;
        Ui ui = j.ui;
        Jugador jug = j.jugador;
        Montaje m = c.montaje;
        CatalogoDePiezas cat = CatalogoDePiezas.delJuego();
        float fila = Math.min(96, (h - 120) / Montaje.Hueco.values().length);
        float yy = y;
        Montaje previa = null;
        String quePrevia = "";
        for (Montaje.Hueco hueco : Montaje.Hueco.values()) {
            List<Pieza> opciones = new ArrayList<>();
            for (Pieza pz : cat.de(hueco.ranura)) {
                if (Progreso.piezaUsable(jug, pz, m.modelo)) {
                    opciones.add(metajavbots.ejercicios.RetoDePieza.aplicarAjustes(pz, jug.ajustesDePiezas.get(pz.id)));
                }
            }
            for (Pieza propia : jug.piezasPropias()) {
                if (propia.ranura == hueco.ranura) opciones.add(propia);
            }
            Pieza actual = m.pieza(hueco);
            if (hueco == Montaje.Hueco.NATURALEZA) opciones.add(0, null);
            // Entre rondas no se puede montar lo que deja al robot sin poder pelear (en el Taller sí, y se explica).
            if (!permitirActivar) opciones.removeIf(o -> o != actual && !conPieza(m, hueco, o).puedePelear());
            int i = indiceDePieza(opciones, actual);
            // Al pasar el ratón por ◄ ►: cómo quedaría el robot con la pieza de al lado.
            if (previa == null && opciones.size() > 1) {
                int k = ui.e.dentro(x + w * 0.62f - 110, yy + 30, 46, 40) ? (i - 1 + opciones.size()) % opciones.size()
                        : ui.e.dentro(x + w * 0.62f - 58, yy + 30, 46, 40) ? (i + 1) % opciones.size() : -1;
                if (k >= 0) {
                    previa = conPieza(m, hueco, opciones.get(k));
                    quePrevia = opciones.get(k) == null ? T.t("taller.sin_pieza") : opciones.get(k).nombre(T.idioma);
                }
            }
            p.caja(x, yy, w * 0.62f, fila - 8, 10, ui.tema.panel, 1);
            p.texto(T.t("hueco." + hueco.name().toLowerCase()), x + 16, yy + 10, ui.letra * 0.8f, false, ui.tema.textoSuave, 1);
            String nombre = actual == null ? T.t("taller.sin_pieza") : actual.nombre(T.idioma);
            boolean propia = actual != null && m.esArmaCaracteristica(actual);
            p.texto(propia ? "★ " + nombre : nombre, x + 16, yy + 34, ui.letra * 1.05f, true, actual == null ? ui.tema.textoApagado : ui.tema.texto, 1);
            if (actual != null) {
                String detalle = cifras(actual) + (propia ? "  ·  " + T.f("taller.sinergia_arma", Math.round(Montaje.BONO_ARMA_CARACTERISTICA * 100)) : "");
                p.texto(ui.recortarTexto(detalle, w * 0.62f - 140, ui.letra * 0.7f, false), x + 16, yy + 62, ui.letra * 0.7f, false, ui.tema.textoSuave, 1);
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
        dibujarPanelCifras(j, m, previa, quePrevia, x + w * 0.62f + 20, y, w * 0.38f - 20, h, permitirActivar);
    }

    /** Una copia del montaje con esa pieza en ese hueco (para ver cómo quedaría antes de montarla). */
    static Montaje conPieza(Montaje m, Montaje.Hueco hueco, Pieza p) {
        Montaje c = m.copia();
        c.poner(hueco, p);
        return c;
    }

    private static int indiceDePieza(List<Pieza> opciones, Pieza actual) {
        if (actual == null) return opciones.indexOf(null);
        for (int i = 0; i < opciones.size(); i++) {
            Pieza p = opciones.get(i);
            if (p != null && p.id.equals(actual.id)) return i;
        }
        return -1;
    }

    private static void dibujarPanelCifras(Juego j, Montaje m, Montaje previa, String quePrevia, float cx, float y, float cw, float h,
                                           boolean permitirActivar) {
        Pincel p = j.pincel;
        Ui ui = j.ui;
        Montaje.Cifras cf = m.cifras();
        float cy = ui.panel(cx, y, cw, h, T.t("taller.cifras"));
        // Peso frente a carga, movilidad y terrenos (docs/EQUILIBRIO.md); y, al pasar por ◄ ►, cómo quedaría.
        cy += PanelDePeso.dibujar(j, m, previa, quePrevia, cx + 18, cy, cw - 36) + 6;
        String[] l = {
                T.f("cifras.blindaje", Math.round(cf.blindajeTotal())),
                T.f("cifras.pasos", Math.round(cf.pasos)),
                T.f("cifras.energia", Math.round(cf.energiaMax), String.format(java.util.Locale.ROOT, "%.2f", cf.recarga)),
                T.f("cifras.naturaleza", m.modelo.naturaleza.nombre(T.idioma), Math.round(m.refuerzo(m.modelo.naturaleza.refuerza) * 100)),
                m.armasCaracteristicasMontadas() > 0 ? T.f("cifras.sinergia", m.armasCaracteristicasMontadas(),
                        Math.round(Montaje.BONO_ARMA_CARACTERISTICA * 100)) : "",
                cf.vuela ? T.t("cifras.vuela") : ""};
        for (String s : l) {
            if (s.isEmpty() || cy + ui.letra * 1.2f > y + h - 8) continue;
            cy += p.parrafo(s, cx + 18, cy, cw - 36, ui.letra * 0.82f, ui.tema.texto, 1) + ui.letra * 0.35f;
        }
        if (permitirActivar && cy + 46 <= y + h - 12) {
            cy += 6;
            if (ui.boton("⚙ " + T.t("taller.disenar_pieza"), cx + 18, cy, cw - 36, 40, Ui.Estilo.DESTACADO, true,
                    T.t("taller.disenar_pieza_ayuda"))) {
                j.ir(new PantallaDisenarPieza());
            }
            cy += 48;
        }
        List<Pieza> pendientes = permitirActivar ? Progreso.piezasPendientes(j.jugador) : List.of();
        if (!pendientes.isEmpty()) {
            cy += 8;
            p.texto(T.t("taller.pendientes"), cx + 18, cy, ui.letra * 0.85f, true, ui.tema.aviso, 1);
            cy += ui.letra * 1.4f;
            for (Pieza pz : pendientes) {
                if (cy + 40 > y + h - 12) break;
                if (ui.boton("⚡ " + pz.nombre(T.idioma), cx + 18, cy, cw - 36, 36, Ui.Estilo.PRINCIPAL, true, T.t("tienda.activar_ayuda"))) {
                    j.ir(new PantallaActivarPieza(pz));
                }
                cy += 42;
            }
            return;
        }
        cy += 10;
        if (cy + ui.letra * 2 < y + h) p.parrafo(T.t("taller.piezas_ayuda"), cx + 18, cy, cw - 36, ui.letra * 0.75f, ui.tema.textoApagado, 1);
    }

    static String cifras(Pieza p) {
        StringBuilder b = new StringBuilder();
        String prot = p.texto("proteccion");
        if (!prot.isEmpty()) {
            b.append(T.t("valor.proteccion." + prot.toLowerCase(java.util.Locale.ROOT)));
        }
        String[] claves = {"dano", "alcance", "bloqueo", "duracion", "cargas", "enfriamiento", "enfriamientoCiclo",
                "danoObstaculo", "antiaereo", "potencia", "energia", "cura", "radar", "cono", "velocidad", "giro", "carga", "pasos",
                "energiaMax", "blindaje", "peso", "refuerzo"};
        for (String k : claves) {
            double v = p.valor(k);
            if (v == 0) continue;
            if (b.length() > 0) b.append(" · ");
            if (k.equals("bloqueo")) {
                b.append(T.t("valor.bloqueo")).append(' ').append(Math.round(v * 100)).append('%');
            } else if (k.equals("danoObstaculo") || k.equals("antiaereo")) {
                b.append(T.t("valor." + k)).append(" ×").append(String.format(java.util.Locale.ROOT, "%.2f", v));
            } else if (k.equals("potencia")) {
                b.append(T.t("valor.potencia"));
            } else {
                String num = v == Math.rint(v) ? Long.toString(Math.round(v)) : String.format(java.util.Locale.ROOT, "%.2f", v);
                b.append(T.t("valor." + k)).append(' ').append(num);
            }
        }
        String arma = p.ranura == Pieza.Ranura.BRAZO ? p.arma().name() : p.ranura == Pieza.Ranura.CABEZA ? p.especial().name()
                : p.ranura == Pieza.Ranura.PIERNAS ? p.locomocion().name() : "";
        return (arma.isEmpty() || arma.equals("NINGUNO") ? "" : arma + "  ") + b;
    }

    /** Lo que dijo el compilador: los errores al editor (explicados según el nivel) y el aviso de abajo. */
    private void atenderCompilacion(Juego j) {
        Jugador jug = j.jugador;
        if (resultado != null) {
            Compilador.Resultado r = resultado;
            resultado = null;
            editor.marcas.clear();
            for (Compilador.Fallo f : r.errores) {
                editor.marcas.add(Explicador.marca(f, jug));
            }
            if (r.bien) {
                Jugador.Robot rob = jug.robot();
                if (rob != null) {
                    rob.codigo = codigoEnCompilacion;
                    rob.registrarCompilacionValida(codigoEnCompilacion);
                }
                jug.avisoCompilarVisto = true;
                j.guardar();
            }
            aviso = r.bien ? T.f("taller.compilado_ok", r.milisegundos) : T.f("pelea.errores", r.graves().size());
            tutor.compilado(r.bien);
            if (r.bien && enBanco) banco.reiniciar(jug, jug.robot(), probador, j.pincel);
            j.sonidos.tocar(r.bien ? "bien" : "mal", 0.6f);
        }
    }

    /** A la izquierda: los robots de la partida (para elegir uno), el botón de uno nuevo o cuántas victorias faltan. */
    private float robots(Juego j, float iz, float y, float ancho) {
        Pincel p = j.pincel;
        Ui ui = j.ui;
        Jugador jug = j.jugador;
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
                if (enBanco) {
                    banco.limpiar();
                    banco.reiniciar(jug, jug.robot(), probador, j.pincel);
                }
            }
            ry += 54;
        }
        if (jug.robots.size() < maximo) {
            if (ui.boton("+ " + T.t("taller.nuevo_robot"), iz + 14, ry, ancho - 28, 46, Ui.Estilo.PRINCIPAL)) {
                if (enBanco) cerrarBanco(true);
                creando = true;
            }
            ry += 54;
        } else if (jug.robots.size() < 4) {
            ry += p.parrafo(T.f("taller.mas_robots", jug.etiqueta.robotsPermitidos(), Progreso.VICTORIAS_POR_ROBOT[Math.min(3, jug.robots.size())]),
                    iz + 18, ry, ancho - 36, ui.letra * 0.75f, ui.tema.textoApagado, 1) + 12;
        }
        return ry - y + 10;
    }
}
