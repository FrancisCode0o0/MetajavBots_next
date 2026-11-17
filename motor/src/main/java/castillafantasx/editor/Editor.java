package castillafantasx.editor;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.Input.Keys;

import castillafantasx.LaPartida;
import castillafantasx.Pantalla;
import castillafantasx.entrada.Teclas;
import castillafantasx.f1.CatalogoF1;
import castillafantasx.f1.Herramienta;
import castillafantasx.f1.MenuComodo;
import castillafantasx.f1.PanelF1;

/**
 * EDITOR — El modo editor (docs/EDITOR.md): el mundo se sigue viendo, en
 * pausa, y arriba hay un selector que despliega la rueda de herramientas.
 * La herramienta elegida queda flotando arriba, como una ficha, con su panel
 * de opciones; Esc, la × o llevar la ficha hacia arriba la quitan.
 *
 * Aquí está lo que se elige y lo que hace cada tecla y cada gesto del
 * puntero; lo pinta interfaz.DibujoEditor con las mismas medidas.
 */
public final class Editor {

    /** Una casilla de la rueda: una categoría, las favoritas, los recientes, una herramienta o una de sus opciones. */
    public static final class Casilla {
        public final String nombre, icono, ayuda;
        public final int color;
        /** Lo que se abre al entrar: una Categoria, FAVORITAS, RECIENTES o el id de una entrada del buscador. */
        public final Object destino;
        /** Sus tareas pendientes (el globito rojo). */
        public final int pendientes;

        Casilla(String nombre, String icono, String ayuda, int color, Object destino, int pendientes) {
            this.nombre = nombre;
            this.icono = icono;
            this.ayuda = ayuda;
            this.color = color;
            this.destino = destino;
            this.pendientes = pendientes;
        }
    }

    public static final String FAVORITAS = "favoritas", RECIENTES = "recientes";

    // --- Medidas de lo que se pinta arriba, en píxeles de la imagen del juego ---
    public static final int ALTO_DE_LA_BARRA = 20, ANCHO_DEL_SELECTOR = 104, ANCHO_DE_LA_FICHA = 150, BOTON = 20;
    public static final int EJE_DE_LA_RUEDA = 80 - (int) Rueda.RADIO, FONDO_DE_LA_RUEDA = 140;      // la y del eje y hasta dónde llega su zona
    public static final int ANCHO_DEL_PANEL = 330, FILAS_DEL_PANEL = 9, ANCHO_DE_LAS_FLECHAS = 30;
    /** El sitio del ↺ (volver a lo de fábrica) de una opción cambiada, a la izquierda de sus flechas. */
    public static final int ANCHO_DEL_DE_FABRICA = 16;
    // TODO-FRAN: cuánto hay que subir la ficha para quitarla y cuánto dura la pulsación larga.
    public static final int SUBIDA_PARA_QUITAR = 12, FRAMES_DE_PULSACION_LARGA = 40;
    private static final int FRAMES_DE_AVISO = 240;

    /** ¿Lleva esta versión el editor? Solo la de desarrollo: la de publicar se hace sin res/editor/ (docs/EDITOR.md). */
    public static final boolean DISPONIBLE = Editor.class.getResource("/editor/codigo.txt") != null;

    /** Lo del PC (abrir programas, recompilar): lo pone el lanzador de escritorio; sin él, no hace nada. */
    public static Sistema sistema = new Sistema.Ninguno();

    public final LaPartida partida;
    /** El modo editor está puesto (aunque se haya abierto otra pantalla desde él). */
    public boolean abierto;

    // La rueda
    public boolean ruedaAbierta;
    /** Lo que enseña la rueda: null, las categorías; una Categoria, FAVORITAS o RECIENTES, lo de dentro. */
    public Object nivel;
    public final Rueda rueda = new Rueda();
    private int casillaPinchada = -1;

    // La herramienta en uso (la ficha y su panel)
    public HerramientaDeEditor enUso;
    public int opcion;
    public final Panel panel = new Panel("opciones", 0, 0, ANCHO_DEL_PANEL, 100);
    private boolean fichaPinchada;
    private float yAlPincharLaFicha;
    private int framesConLaFichaPinchada;

    // ------------------------------------------------------------------
    // El menú del clic derecho y la mesa acercada (encargo ORDEN_ERGONOMIA_2)
    // ------------------------------------------------------------------

    /** El menú del clic derecho que está abierto, o null. */
    public MenuContextual menuAbierto;

    /** El menú de lo que hay en el mundo, cuando la herramienta puesta no tiene uno propio (o no hay ninguna). Lo pone el juego. */
    public interface MenuDelMundo {
        MenuContextual en(Editor editor, float x, float y);
    }

    public static MenuDelMundo menuDelMundo;

    /** ¿Abre el clic derecho sobre el mundo el menú de lo que hay? Con «no», hace lo de antes (Taller › Cómo se maneja; editor.txt: menu_derecho). */
    public boolean menuConElDerecho = true;
    /** ¿Abre la pulsación larga sobre el mundo ese menú? (editor.txt: menu_pulsando). */
    public boolean menuConPulsacionLarga = true;

    /** Guarda en editor.txt cómo se maneja (Taller › Cómo se maneja). */
    public void guardarManejo() {
        menu.notas.put("menu_derecho", menuConElDerecho ? "si" : "no");
        menu.notas.put("menu_pulsando", menuConPulsacionLarga ? "si" : "no");
        menu.guardar();
    }

    /** Lo que puede temblar el dedo (en píxeles de la imagen) en una pulsación larga sin que cuente como arrastrar. */
    public static final int TEMBLOR_DE_LA_PULSACION = 4;
    // La pulsación sobre el mundo: dónde empezó, cuántos frames dura (-1: no hay) y si se ha movido.
    private float xAlPincharElMundo, yAlPincharElMundo;
    private int framesConElMundoPinchado = -1;
    private boolean pinchadoSinMover, pinchadoSinHerramienta;
    // Arrastrar la vista de una mesa acercada (con el botón central o con la mano).
    private boolean moviendoLaMesa;
    private float xDeLaMesa, yDeLaMesa;

    /** Abre ese menú junto a ese punto de la imagen (si tiene algo que ofrecer). */
    public void abrirMenu(MenuContextual m, float x, float y) {
        if (m == null) return;
        List<Opcion> filas = m.opciones(this);
        if (filas.isEmpty()) return;
        m.colocar(x, y, filas.size());
        m.elegida = 0;
        m.ajustar(filas);
        menuAbierto = m;
    }

    public void cerrarMenu() {
        menuAbierto = null;
    }

    /** La mesa de la herramienta puesta, si trabaja sobre una y se ve (sin la rueda ni el buscador); o null. */
    public Mesa mesaALaVista() {
        return enUso == null || ruedaAbierta || menu.buscando() ? null : enUso.mesa(this);
    }

    /** La presión del lápiz (de 0 a 1); con el ratón o el dedo, 1. */
    public float presion() {
        try {
            float p = com.badlogic.gdx.Gdx.input == null ? 1 : com.badlogic.gdx.Gdx.input.getPressure();
            return p > 0.05f && p < 1 ? p : 1;
        } catch (RuntimeException sinLapiz) {
            return 1;
        }
    }

    /** El buscador, las favoritas y los recientes (lo cómodo de F1). */
    public final MenuComodo menu = new MenuComodo("editor.txt", castillafantasx.Contenido.delJuego().favoritasDelEditor());
    public int resultadoElegido;
    public final Historial historial = new Historial();
    public final Vigia vigia = new Vigia();

    /** Un aviso corto (guardado, recargado, deshecho…) y si es de algo que ha ido mal. */
    public String aviso;
    public boolean avisoMalo;
    private int framesDeAviso;
    /** La línea de ayuda de lo que señala el puntero, o null. */
    public String ayudaSenalada;

    public Editor(LaPartida partida) {
        this.partida = partida;
    }

    // ------------------------------------------------------------------
    // Abrir y cerrar
    // ------------------------------------------------------------------

    /** Entra en el modo editor: la pantalla limpia, con el selector arriba. */
    public void abrir() {
        abierto = true;
        menuAbierto = null;
        ruedaAbierta = false;
        nivel = null;
        menu.abrir();
        panel.deNota(menu.notas.get("panel_" + panel.id));
        menuConElDerecho = !"no".equals(menu.notas.get("menu_derecho"));
        menuConPulsacionLarga = !"no".equals(menu.notas.get("menu_pulsando"));
        colocarPanel(false);
    }

    /** Sale del modo editor. */
    public void cerrar() {
        menuAbierto = null;
        quitarHerramienta();
        abierto = false;
        ruedaAbierta = false;
        menu.abrir();
    }

    public void avisar(String texto, boolean malo) {
        aviso = texto;
        avisoMalo = malo;
        framesDeAviso = FRAMES_DE_AVISO;
    }

    // ------------------------------------------------------------------
    // Lo que hay para elegir
    // ------------------------------------------------------------------

    /** Todo lo que encuentra el buscador: cada herramienta y cada opción de las hechas con filas de F1. */
    public List<MenuComodo.Entrada> entradas() {
        List<MenuComodo.Entrada> lista = new ArrayList<>();
        for (HerramientaDeEditor h : CatalogoDelEditor.todas()) {
            lista.add(new MenuComodo.Entrada(h.id, h.nombre, h.categoria.nombre, h.icono, lista.size(), PanelF1.normalizar(h.ayuda) + " " + h.palabras));
            if (!(h instanceof GrupoDeF1)) continue;
            List<Opcion> opciones = h.opciones(this);
            if (opciones.size() < 2) continue;
            for (Opcion o : opciones) {
                lista.add(new MenuComodo.Entrada(h.id + "/" + o.id, o.nombre, h.nombre, o.icono, lista.size(), PanelF1.normalizar(o.texto)));
            }
        }
        return lista;
    }

    /** La herramienta de una entrada del buscador ("horas" o "horas/hora"), o null. */
    public static HerramientaDeEditor herramientaDe(String idDeEntrada) {
        int barra = idDeEntrada.indexOf('/');
        return CatalogoDelEditor.deId(barra < 0 ? idDeEntrada : idDeEntrada.substring(0, barra));
    }

    /** Las casillas que enseña la rueda en el nivel de ahora. */
    public List<Casilla> casillas() {
        List<Casilla> lista = new ArrayList<>();
        if (nivel == null) {
            for (Categoria c : Categoria.values()) {
                int pendientes = 0;
                for (HerramientaDeEditor h : CatalogoDelEditor.de(c)) pendientes += h.pendientes();
                lista.add(new Casilla(c.nombre, c.icono, c.ayuda, c.color(), c, pendientes));
            }
            lista.add(new Casilla("Favoritas", "favorita", "Tus herramientas favoritas (Ctrl+D pone o quita la elegida; Ctrl+1…9 las usa).", Estilo.acento, FAVORITAS, 0));
            lista.add(new Casilla("Recientes", "recientes", "Las últimas herramientas que has usado.", Estilo.textoSuave, RECIENTES, 0));
        } else if (nivel instanceof Categoria) {
            for (HerramientaDeEditor h : CatalogoDelEditor.de((Categoria) nivel)) {
                if (h == CatalogoDelEditor.OTRAS_DE_F1 && h.opciones(this).isEmpty()) continue;
                lista.add(new Casilla(h.nombre, h.icono, h.ayuda, h.categoria.color(), h.id, h.pendientes()));
            }
        } else {
            List<MenuComodo.Entrada> todas = entradas();
            for (MenuComodo.Entrada e : MenuComodo.deLista(nivel == FAVORITAS ? menu.favoritas : menu.recientes, todas)) {
                HerramientaDeEditor h = herramientaDe(e.id);
                lista.add(new Casilla(e.nombre, e.icono, h.nombre.equals(e.nombre) ? h.ayuda : h.nombre + ": " + h.ayuda, h.categoria.color(), e.id, 0));
            }
        }
        return lista;
    }

    /** Las opciones de la herramienta en uso (vacío si no hay ninguna). */
    public List<Opcion> opciones() {
        return enUso == null ? new ArrayList<>() : enUso.opciones(this);
    }

    /** Los resultados del buscador, las mejores primero. */
    public List<MenuComodo.Entrada> resultados() {
        return menu.resultados(entradas(), null);
    }

    // ------------------------------------------------------------------
    // La rueda
    // ------------------------------------------------------------------

    /** Despliega la rueda, siempre en la misma postura: las categorías, con la primera abajo. */
    public void abrirRueda() {
        ruedaAbierta = true;
        nivel = null;
        rueda.poner(casillas().size(), enUso != null ? enUso.categoria.ordinal() : 0);
        menu.abrir();
    }

    public void cerrarRueda() {
        ruedaAbierta = false;
        nivel = null;
    }

    /** Entra en la casilla 'cual' de la rueda: una categoría enseña lo suyo; una herramienta se pone. */
    public void entrar(int cual) {
        List<Casilla> casillas = casillas();
        if (cual < 0 || cual >= casillas.size()) return;
        Object destino = casillas.get(cual).destino;
        if (nivel == null) {
            nivel = destino;
            int dentro = casillas().size();
            if (dentro == 0) {
                nivel = null;
                avisar(destino == FAVORITAS ? "Aún no hay favoritas: Ctrl+D pone la herramienta elegida." : "Aún no has usado ninguna herramienta.", false);
                return;
            }
            rueda.poner(dentro, 0);
        } else {
            usarEntrada((String) destino);
        }
    }

    /** Vuelve de las herramientas a las categorías. */
    private void subirDeNivel() {
        Object antes = nivel;
        nivel = null;
        List<Casilla> casillas = casillas();
        int donde = 0;
        for (int i = 0; i < casillas.size(); i++) {
            if (casillas.get(i).destino == antes) donde = i;
        }
        rueda.poner(casillas.size(), donde);
    }

    // ------------------------------------------------------------------
    // Poner y quitar una herramienta
    // ------------------------------------------------------------------

    /** Usa una entrada del buscador, de las favoritas o de la rueda: pone su herramienta y, si es una opción, la acciona. */
    public void usarEntrada(String idDeEntrada) {
        HerramientaDeEditor h = herramientaDe(idDeEntrada);
        if (h == null) return;
        MenuComodo.Entrada entrada = MenuComodo.deId(entradas(), idDeEntrada);
        menu.abrir();
        menu.usada(entrada);
        cerrarRueda();
        int barra = idDeEntrada.indexOf('/');
        if (barra < 0) {
            poner(h);
            return;
        }
        // Una opción suelta: se pone su herramienta con esa fila elegida y se acciona, como en F1.
        if (enUso != h) ponerSinAccionar(h);
        List<Opcion> opciones = opciones();
        for (int i = 0; i < opciones.size(); i++) {
            if (!opciones.get(i).id.equals(idDeEntrada.substring(barra + 1))) continue;
            opcion = i;
            opciones.get(i).hacer(this, 0);
        }
    }

    /** Pone una herramienta: queda flotando arriba con su panel (o hace lo suyo, si es de las que abren otra pantalla). */
    public void poner(HerramientaDeEditor h) {
        menuAbierto = null;
        quitarHerramienta();
        cerrarRueda();
        if (!h.alElegir(this)) return;
        enUso = h;
        opcion = 0;
        opcionDelPuntero = false;
        listaDeOpciones.alPrincipio();
        colocarPanel(true);
    }

    private void ponerSinAccionar(HerramientaDeEditor h) {
        quitarHerramienta();
        enUso = h;
        opcion = 0;
        opcionDelPuntero = false;
        listaDeOpciones.alPrincipio();
        colocarPanel(true);
    }

    /** Quita la herramienta en uso: la pantalla queda limpia. */
    public void quitarHerramienta() {
        if (enUso == null) return;
        menuAbierto = null;          // el menú de lo de la herramienta se va con ella
        HerramientaDeEditor h = enUso;
        enUso = null;
        fichaPinchada = false;
        h.alQuitar(this);
    }

    /** El panel toma el alto de las opciones de ahora; si 'abierto', además se despliega. */
    private void colocarPanel(boolean desplegar) {
        if (desplegar) panel.plegado = false;
        if (panel.x == 0 && panel.y == 0) {
            panel.x = Math.round((Pantalla.ancho - panel.ancho) / 2);
            panel.y = ALTO_DE_LA_BARRA + 4;
        }
        if (!panel.grande) {
            boolean conLienzo = enUso != null && enUso.lienzo(this) != null;
            int filas = Math.max(1, Math.min(FILAS_DEL_PANEL, opciones().size()));
            panel.ancho = conLienzo ? Math.min(Pantalla.ancho - 2 * Estilo.MARGEN, 520) : ANCHO_DEL_PANEL;
            panel.alto = conLienzo ? Math.min(Pantalla.alto - ALTO_DE_LA_BARRA - 2 * Estilo.MARGEN, 400)
                    : Panel.ALTO_DEL_TITULO + filas * Estilo.RENGLON + 2 * Estilo.RENGLON + 6;
        }
        panel.encajar();
    }

    /** Cuántas filas de opciones caben en el panel. */
    public int filasQueCaben() {
        return Math.max(1, (int) ((panel.alto - Panel.ALTO_DEL_TITULO - 2 * Estilo.RENGLON - 6) / Estilo.RENGLON));
    }

    /** La lista de opciones del panel: solo se mueve con el teclado, la rueda o su barra, no al señalar (ListaQuieta). */
    public final castillafantasx.interfaz.ListaQuieta listaDeOpciones = new castillafantasx.interfaz.ListaQuieta();
    /** ¿La opción elegida la ha puesto el puntero al señalarla? Entonces la lista no se mueve. */
    private boolean opcionDelPuntero;

    /** La primera opción que se ve en el panel. */
    public int primeraOpcion() {
        return listaDeOpciones.primera(opcion, opciones().size(), filasQueCaben(), opcionDelPuntero);
    }

    // ------------------------------------------------------------------
    // Las filas de F1, desde el editor
    // ------------------------------------------------------------------

    /**
     * Acciona la fila de F1 de esa herramienta (paso 0: Intro; -1 y 1: ← y →),
     * igual que en F1. Si el cambio tiene vuelta atrás (un valor que sube y
     * baja, un sí o no), queda apuntado en el historial.
     */
    public void usarDeF1(String idDeF1, int paso) {
        Herramienta h = CatalogoF1.deId(partida.herramientasDeF1(), idDeF1);
        if (h == null) return;
        String antes = partida.opcionesDeDepuracion().get(h.fila);
        // La hora no vuelve justo a la de antes con la flecha contraria: se guarda tal cual estaba.
        castillafantasx.tiempo.Reloj reloj = partida.reloj();
        double horaDeAntes = reloj.hora;
        int diaDeAntes = reloj.dia;
        partida.usarFilaDeF1(h.fila, paso == 0 ? Keys.ENTER : paso > 0 ? Keys.RIGHT : Keys.LEFT);
        Herramienta ahora = CatalogoF1.deId(partida.herramientasDeF1(), idDeF1);
        String despues = ahora == null ? antes : partida.opcionesDeDepuracion().get(ahora.fila);
        if (despues.equals(antes) || !abierto) return;
        boolean siONo = paso == 0 && (antes.endsWith(": sí") || antes.endsWith(": no"));
        if (paso == 0 && !siONo) return;        // un Intro que no es un interruptor (sanar, dar…) no tiene vuelta atrás
        int vuelta = -paso;
        if (idDeF1.equals("hora")) {
            historial.apuntar(h.nombre + ": " + PanelF1.valorCorto(despues), () -> {
                reloj.hora = horaDeAntes;
                reloj.dia = diaDeAntes;
            }, () -> usarSinApuntar(idDeF1, paso));
            return;
        }
        historial.apuntar(h.nombre + ": " + PanelF1.valorCorto(despues), () -> usarSinApuntar(idDeF1, vuelta), () -> usarSinApuntar(idDeF1, paso));
    }

    private void usarSinApuntar(String idDeF1, int paso) {
        Herramienta h = CatalogoF1.deId(partida.herramientasDeF1(), idDeF1);
        if (h != null) partida.usarFilaDeF1(h.fila, paso == 0 ? Keys.ENTER : paso > 0 ? Keys.RIGHT : Keys.LEFT);
    }

    // ------------------------------------------------------------------
    // Teclas
    // ------------------------------------------------------------------

    private boolean control() {
        return partida.entrada().mantenida(Keys.CONTROL_LEFT) || partida.entrada().mantenida(Keys.CONTROL_RIGHT);
    }

    /** Una tecla (o un botón del ratón, o la rueda) con el editor puesto. */
    public void tecla(int tecla) {
        if (tecla < Teclas.RATON || tecla >= Teclas.RUEDA_ARRIBA) opcionDelPuntero = false;      // el teclado y la rueda mueven la lista
        if (tecla >= Teclas.RATON) {
            raton(tecla);
            return;
        }
        if (menuAbierto != null) {
            if (control() && teclaConControl(tecla)) return;
            if (teclaDelMenu(tecla)) return;
            cerrarMenu();           // una tecla que no es del menú (Supr, F1…) lo cierra y hace lo suyo
        }
        if (control() && teclaDeLaMesa(tecla)) return;
        if (control() && teclaConControl(tecla)) return;
        boolean libre = !ruedaAbierta && !menu.buscando();
        // Mayús+F10 (o la tecla de menú): el menú del clic derecho de lo elegido, junto al panel.
        if (((tecla == Keys.F10 && mayus()) || tecla == Keys.MENU) && enUso != null && libre) {
            MenuContextual m = enUso.menuDeLoElegido(this);
            if (m == null) avisar("No hay nada elegido: pincha algo (o el clic derecho encima).", false);
            else abrirMenu(m, panel.x + panel.ancho, panel.y + Panel.ALTO_DEL_TITULO);
            return;
        }
        // Ctrl+Retroceso: la opción elegida, a lo de fábrica (como su ↺).
        if (control() && tecla == Keys.BACKSPACE && libre && enUso != null) {
            List<Opcion> lista = opciones();
            if (opcion < lista.size() && !lista.get(opcion).volverAFabrica(this)) avisar("Esta opción ya está como de fábrica.", false);
            return;
        }
        if (enUso != null && libre && enUso.tecla(this, tecla)) return;
        if (tecla == Keys.ESCAPE && buscadorEsperando) {
            buscadorEsperando = false;      // la lupa tocada: Esc la deja
            return;
        }
        if (tecla == Keys.ESCAPE) {
            retroceder();
            return;
        }
        if (tecla == Keys.F1) {
            partida.volverAF1DesdeElEditor();
            return;
        }
        if (menu.buscando()) {
            teclaDelBuscador(tecla);
            return;
        }
        if (tecla == Keys.TAB) {
            if (ruedaAbierta) cerrarRueda();
            else abrirRueda();
            return;
        }
        // Las letras escriben en el buscador (salvo que la herramienta puesta se las quede).
        char letra = letraDe(tecla);
        if (letra != 0 && !control() && !(enUso != null && libre && enUso.escribe(this))) {
            menu.escribir(letra);
            resultadoElegido = 0;
            buscadorEsperando = false;
            return;
        }
        boolean intro = tecla == Keys.ENTER || tecla == Keys.NUMPAD_ENTER || tecla == Keys.SPACE;
        if (ruedaAbierta) {
            if (tecla == Keys.LEFT) rueda.girar(-1);
            else if (tecla == Keys.RIGHT) rueda.girar(1);
            else if (intro || tecla == Keys.DOWN) entrar(rueda.elegida);
            else if (tecla == Keys.UP || tecla == Keys.BACKSPACE) retroceder();
            return;
        }
        if (enUso == null) {
            if (intro || tecla == Keys.DOWN) abrirRueda();
            return;
        }
        List<Opcion> opciones = opciones();
        if (opciones.isEmpty()) return;
        opcion = Math.max(0, Math.min(opciones.size() - 1, opcion));
        if (opciones.get(opcion).esSeccion) opcion = siguienteOpcion(opciones, opcion, 1);
        if (tecla == Keys.UP) opcion = siguienteOpcion(opciones, opcion, -1);
        else if (tecla == Keys.DOWN) opcion = siguienteOpcion(opciones, opcion, 1);
        // AvPág y RePág, una página; Inicio y Fin, la primera y la última (Mayús+Inicio es de la vista del mundo).
        else if (tecla == Keys.PAGE_DOWN) opcion = deSalto(opciones, Math.min(opciones.size() - 1, opcion + filasQueCaben()), 1);
        else if (tecla == Keys.PAGE_UP) opcion = deSalto(opciones, Math.max(0, opcion - filasQueCaben()), -1);
        else if (tecla == Keys.HOME) opcion = deSalto(opciones, 0, 1);
        else if (tecla == Keys.END) opcion = deSalto(opciones, opciones.size() - 1, -1);
        else if (tecla == Keys.LEFT) opciones.get(opcion).hacer(this, -1);
        else if (tecla == Keys.RIGHT) opciones.get(opcion).hacer(this, 1);
        else if (intro) opciones.get(opcion).hacer(this, 0);
    }

    /** La siguiente (o la anterior) opción que no es el título de un grupo, dando la vuelta. */
    private static int siguienteOpcion(List<Opcion> opciones, int desde, int paso) {
        int n = opciones.size();
        for (int i = 1; i <= n; i++) {
            int cual = Math.floorMod(desde + paso * i, n);
            if (!opciones.get(cual).esSeccion) return cual;
        }
        return desde;
    }

    /** Esa opción o, si es un título, la de al lado (hacia 'paso') que no lo es, sin dar la vuelta. */
    private static int deSalto(List<Opcion> opciones, int cual, int paso) {
        for (int i = cual; i >= 0 && i < opciones.size(); i += paso) {
            if (!opciones.get(i).esSeccion) return i;
        }
        return siguienteOpcion(opciones, cual, -paso);
    }

    private boolean mayus() {
        return partida.entrada().mantenida(Keys.SHIFT_LEFT) || partida.entrada().mantenida(Keys.SHIFT_RIGHT);
    }

    /** Con una mesa a la vista: Ctrl + y Ctrl − la acercan y alejan, Ctrl+0 la enseña entera y Ctrl + flechas mueven su vista. */
    private boolean teclaDeLaMesa(int tecla) {
        Mesa m = mesaALaVista();
        if (m == null) return false;
        float cx = m.x + m.lienzo.ancho * m.veces / 2f, cy = m.y + m.lienzo.alto * m.veces / 2f;
        int paso = Estilo.RENGLON * 2;
        if (tecla == Keys.PLUS || tecla == Keys.EQUALS || tecla == Keys.NUMPAD_ADD) m.acercar(1, cx, cy);
        else if (tecla == Keys.MINUS || tecla == Keys.NUMPAD_SUBTRACT) m.acercar(-1, cx, cy);
        else if (tecla == Keys.NUM_0 || tecla == Keys.NUMPAD_0) m.encajar();
        else if (m.lupa > 1 && (tecla == Keys.LEFT || tecla == Keys.RIGHT)) m.desplazar(tecla == Keys.LEFT ? paso : -paso, 0);
        else if (m.lupa > 1 && (tecla == Keys.UP || tecla == Keys.DOWN)) m.desplazar(0, tecla == Keys.UP ? paso : -paso);
        else return false;
        return true;
    }

    /** Una tecla con el menú del clic derecho abierto: ↑ ↓ eligen, ← → cambian, Intro usa, Esc cierra, y las letras buscan. Devuelve false si no es del menú. */
    private boolean teclaDelMenu(int tecla) {
        MenuContextual m = menuAbierto;
        List<Opcion> filas = m.opciones(this);
        if (filas.isEmpty()) {
            cerrarMenu();
            return false;
        }
        m.ajustar(filas);
        Opcion o = filas.get(m.elegida);
        if (tecla == Keys.ESCAPE) {
            if (m.buscado.isEmpty()) cerrarMenu();
            else m.buscado = "";
        } else if (tecla == Keys.BACKSPACE) {
            if (m.buscado.isEmpty()) cerrarMenu();
            else m.buscado = m.buscado.substring(0, m.buscado.length() - 1);
        } else if (tecla == Keys.UP || tecla == Keys.DOWN) {
            m.mover(filas, tecla == Keys.UP ? -1 : 1);
        } else if (tecla == Keys.HOME || tecla == Keys.END || tecla == Keys.PAGE_UP || tecla == Keys.PAGE_DOWN) {
            boolean arriba = tecla == Keys.HOME || tecla == Keys.PAGE_UP;
            m.elegida = arriba ? filas.size() - 1 : 0;
            m.mover(filas, arriba ? 1 : -1);
        } else if (tecla == Keys.LEFT || tecla == Keys.RIGHT) {
            if (o.conValor) o.hacer(this, tecla == Keys.LEFT ? -1 : 1);
        } else if (tecla == Keys.ENTER || tecla == Keys.NUMPAD_ENTER || tecla == Keys.SPACE) {
            usarDelMenu(o, 0);
        } else if (tecla == Keys.TAB) {
            m.mover(filas, 1);
        } else {
            char letra = letraDe(tecla);
            if (letra == 0 || control()) return false;
            m.buscado += letra;
        }
        if (menuAbierto != null) menuAbierto.ajustar(menuAbierto.opciones(this));
        return true;
    }

    /** Usa una fila del menú: lo que se hace de una vez lo cierra; lo que tiene valor lo deja abierto, para seguir cambiándolo. */
    private void usarDelMenu(Opcion o, int paso) {
        if (o.esSeccion || (menuAbierto != null && !menuAbierto.casa(o))) return;
        if (paso == 0 && !o.conValor) cerrarMenu();
        o.hacer(this, paso);
    }

    /** Un clic dentro del menú abierto: la × lo cierra; las flechas de una fila con valor lo cambian; el resto de la fila la usa. */
    private void pincharElMenu(MenuContextual m, List<Opcion> filas, float x, float y) {
        if (m.enLaEquis(x, y)) {
            cerrarMenu();
            return;
        }
        int fila = m.filaEn(x, y, filas.size());
        if (fila < 0 || !m.seElige(filas.get(fila))) return;
        m.elegida = fila;
        m.conElPuntero = true;
        Opcion o = filas.get(fila);
        float flechas = m.x + MenuContextual.ANCHO - ANCHO_DE_LAS_FLECHAS;
        if (o.conValor && x >= flechas) o.hacer(this, x < flechas + ANCHO_DE_LAS_FLECHAS / 2f ? -1 : 1);
        else usarDelMenu(o, 0);
    }

    /** Ctrl+Z deshace, Ctrl+Y rehace, Ctrl+D marca la favorita y Ctrl+1…9 usa una. Devuelve true si era una de estas. */
    private boolean teclaConControl(int tecla) {
        if (tecla == Keys.Z) {
            deshacer();
        } else if (tecla == Keys.Y) {
            rehacer();
        } else if (tecla == Keys.D) {
            MenuComodo.Entrada e = MenuComodo.deId(entradas(), idElegido());
            if (e != null) avisar(menu.cambiarFavorita(e) ? "«" + e.nombre + "», a las favoritas." : "«" + e.nombre + "» ya no es favorita.", false);
        } else if (tecla >= Keys.NUM_1 && tecla <= Keys.NUM_9) {
            List<MenuComodo.Entrada> favoritas = MenuComodo.deLista(menu.favoritas, entradas());
            if (tecla - Keys.NUM_1 < favoritas.size()) usarEntrada(favoritas.get(tecla - Keys.NUM_1).id);
        } else {
            return false;
        }
        return true;
    }

    public void deshacer() {
        String nombre = historial.deshacer();
        avisar(nombre == null ? "No hay nada que deshacer." : "Deshecho: " + nombre, false);
    }

    public void rehacer() {
        String nombre = historial.rehacer();
        avisar(nombre == null ? "No hay nada que rehacer." : "Rehecho: " + nombre, false);
    }

    /** El id de la entrada elegida ahora: el resultado del buscador, la casilla de la rueda o la opción de la herramienta puesta. */
    public String idElegido() {
        if (menu.buscando()) {
            List<MenuComodo.Entrada> casan = resultados();
            return casan.isEmpty() ? null : casan.get(Math.min(resultadoElegido, casan.size() - 1)).id;
        }
        if (ruedaAbierta) {
            List<Casilla> casillas = casillas();
            Object destino = casillas.isEmpty() ? null : casillas.get(rueda.elegida).destino;
            return destino instanceof String && nivel != null ? (String) destino : null;
        }
        if (enUso == null) return null;
        List<Opcion> opciones = opciones();
        boolean suelta = enUso instanceof GrupoDeF1 && opciones.size() >= 2 && opcion < opciones.size();
        return suelta ? enUso.id + "/" + opciones.get(opcion).id : enUso.id;
    }

    /** Esc o el botón derecho: deshace el último paso (vaciar el buscador, subir de nivel, cerrar la rueda, quitar la herramienta, salir). */
    public void retroceder() {
        if (menu.buscando()) menu.abrir();
        else if (ruedaAbierta && nivel != null) subirDeNivel();
        else if (ruedaAbierta) cerrarRueda();
        else if (enUso != null) quitarHerramienta();
        else partida.salirDelEditor();
    }

    private void teclaDelBuscador(int tecla) {
        List<MenuComodo.Entrada> casan = resultados();
        resultadoElegido = casan.isEmpty() ? 0 : Math.min(resultadoElegido, casan.size() - 1);
        if (tecla == Keys.BACKSPACE) {
            menu.borrar();
            resultadoElegido = 0;
        } else if (tecla == Keys.UP && !casan.isEmpty()) {
            resultadoElegido = (resultadoElegido + casan.size() - 1) % casan.size();
        } else if (tecla == Keys.DOWN && !casan.isEmpty()) {
            resultadoElegido = (resultadoElegido + 1) % casan.size();
        } else if (tecla == Keys.TAB && !casan.isEmpty()) {
            if (!menu.completar(casan.get(resultadoElegido))) resultadoElegido = (resultadoElegido + 1) % casan.size();
        } else if ((tecla == Keys.ENTER || tecla == Keys.NUMPAD_ENTER) && !casan.isEmpty()) {
            usarEntrada(casan.get(resultadoElegido).id);
        } else {
            char letra = tecla == Keys.SPACE ? ' ' : letraDe(tecla);
            if (letra != 0) {
                menu.escribir(letra);
                resultadoElegido = 0;
            }
        }
    }

    /** La letra que escribe esa tecla en el buscador (a–z y 0–9), o 0 si no escribe. */
    static char letraDe(int tecla) {
        if (tecla >= Keys.A && tecla <= Keys.Z) return (char) ('a' + tecla - Keys.A);
        if (tecla >= Keys.NUM_0 && tecla <= Keys.NUM_9) return (char) ('0' + tecla - Keys.NUM_0);
        return 0;
    }

    /** Una letra tal cual sale del teclado (con mayúsculas, tildes y signos): para las herramientas que escriben. */
    public void letra(char letra) {
        if (enUso != null && !ruedaAbierta && !menu.buscando() && enUso.escribe(this)) enUso.letra(this, letra);
    }

    // ------------------------------------------------------------------
    // El puntero (ratón, lápiz o dedo): todo se hace también solo con él
    // ------------------------------------------------------------------

    // Dónde está cada cosa de la barra de arriba.
    public static float xDelSelector() {
        return Math.round(Pantalla.ancho / 2f - ANCHO_DEL_SELECTOR / 2f);
    }

    /** Los botones de la derecha, contando desde el borde: 0 salir, 1 rehacer, 2 deshacer, 3 buscar. */
    /** Los botones de la derecha de la barra: salir, rehacer, deshacer, favoritas y la lupa del buscador. */
    public static final int BOTONES = 5;

    public static float xDelBoton(int cual) {
        return Pantalla.ancho - (cual + 1) * (BOTON + 2) - 2;
    }

    private static boolean enLaBarra(float y) {
        return y >= 0 && y < ALTO_DE_LA_BARRA;
    }

    /** El botón de la derecha que hay en ese punto, o -1. */
    public static int botonEn(float x, float y) {
        if (!enLaBarra(y)) return -1;
        for (int i = 0; i < BOTONES; i++) {
            if (x >= xDelBoton(i) && x < xDelBoton(i) + BOTON) return i;
        }
        return -1;
    }

    public boolean enElSelector(float x, float y) {
        return enLaBarra(y) && x >= xDelSelector() && x < xDelSelector() + ANCHO_DEL_SELECTOR;
    }

    /** La ficha de la herramienta en uso va a la izquierda del selector. */
    public static float xDeLaFicha() {
        return xDelSelector() - ANCHO_DE_LA_FICHA - 4;
    }

    public boolean enLaFicha(float x, float y) {
        return enUso != null && enLaBarra(y) && x >= xDeLaFicha() && x < xDeLaFicha() + ANCHO_DE_LA_FICHA;
    }

    /** La × de la ficha: su extremo derecho. */
    public boolean enLaEquisDeLaFicha(float x, float y) {
        return enLaFicha(x, y) && x >= xDeLaFicha() + ANCHO_DE_LA_FICHA - BOTON;
    }

    /** La fila de opciones del panel que hay en ese punto, o -1. */
    public int opcionEn(float x, float y) {
        if (enUso == null || panel.plegado || enUso.lienzo(this) != null || !panel.dentro(x, y)) return -1;
        int fila = (int) Math.floor((y - panel.y - Panel.ALTO_DEL_TITULO - 2) / Estilo.RENGLON);
        int cual = primeraOpcion() + fila;
        return fila >= 0 && fila < filasQueCaben() && cual < opciones().size() ? cual : -1;
    }

    /** Dónde empieza la lista de resultados del buscador y lo que mide cada fila. */
    public static final int Y_DE_RESULTADOS = ALTO_DE_LA_BARRA + 22, ANCHO_DE_RESULTADOS = 300, RENGLON_DE_RESULTADOS = 15;
    /** El buscador va arriba a la izquierda, bajo la barra: pequeño, sin tapar la rueda ni el mundo del centro (ORDEN_ERGONOMIA). */
    public static final int X_DE_RESULTADOS = Estilo.MARGEN;
    /** Se ha tocado la lupa de la barra: el buscador espera lo que se escriba (en la tablet, con el teclado en pantalla). */
    public boolean buscadorEsperando;

    public int resultadoEn(float x, float y) {
        float x0 = X_DE_RESULTADOS;
        if (!menu.buscando() || x < x0 || x >= x0 + ANCHO_DE_RESULTADOS || y < Y_DE_RESULTADOS) return -1;
        int fila = (int) ((y - Y_DE_RESULTADOS) / RENGLON_DE_RESULTADOS);
        return fila < resultados().size() ? fila : -1;
    }

    private void raton(int tecla) {
        float x = partida.ratonX(), y = partida.ratonY();
        if (menuAbierto != null) {
            MenuContextual m = menuAbierto;
            List<Opcion> filas = m.opciones(this);
            if (tecla == Teclas.RUEDA_ARRIBA || tecla == Teclas.RUEDA_ABAJO) {
                m.mover(filas, tecla == Teclas.RUEDA_ABAJO ? 1 : -1);
                return;
            }
            boolean dentro = m.dentro(x, y, filas.size());
            if (tecla == Teclas.RATON_IZQUIERDO && dentro) {
                pincharElMenu(m, filas, x, y);
                return;
            }
            cerrarMenu();
            if (dentro) return;
            // Fuera del menú, el clic lo cierra y sigue haciendo lo suyo (poner, elegir, otro clic derecho).
        }
        if (tecla == Teclas.RATON_CENTRAL) {
            Mesa m = mesaALaVista();
            if (m != null && m.dentro(x, y)) empezarAMoverLaMesa(x, y);
            return;
        }
        if (tecla == Teclas.RUEDA_ARRIBA || tecla == Teclas.RUEDA_ABAJO) {
            int paso = tecla == Teclas.RUEDA_ABAJO ? 1 : -1;
            Mesa mesa = control() ? mesaALaVista() : null;
            if (mesa != null && mesa.dentro(x, y)) {
                mesa.acercar(-paso, x, y);         // Ctrl + la rueda, sobre la mesa: acerca o aleja donde está el puntero
                return;
            }
            if (menu.buscando()) teclaDelBuscador(paso > 0 ? Keys.DOWN : Keys.UP);
            else if (ruedaAbierta) rueda.girar(paso);
            else if (enUso != null && !enUso.rueda(this, paso)) tecla(paso > 0 ? Keys.DOWN : Keys.UP);
            return;
        }
        if (tecla == Teclas.RATON_DERECHO) {
            // Sobre el mundo, el botón derecho abre el menú de lo que hay (el de la herramienta o el del mundo, ORDEN_ERGONOMIA_2);
            // si no hay menú, es de la herramienta (coger lo que hay, cancelar); y si no lo quiere, vuelve atrás.
            boolean sobreElMundo = !ruedaAbierta && !menu.buscando() && !(enUso != null && panel.dentro(x, y)) && y > ALTO_DE_LA_BARRA;
            if (sobreElMundo && !menuConElDerecho) {
                if (enUso != null && enUso.alPincharConElDerecho(this, x, y)) return;
            } else if (sobreElMundo) {
                MenuContextual m = enUso != null ? enUso.menuContextual(this, x, y) : null;
                if (m != null) {
                    abrirMenu(m, x, y);
                    return;
                }
                if (enUso != null && enUso.alPincharConElDerecho(this, x, y)) return;
                Mesa mesa = mesaALaVista();
                m = menuDelMundo != null && (mesa == null || !mesa.dentro(x, y)) ? menuDelMundo.en(this, x, y) : null;
                if (m != null) {
                    abrirMenu(m, x, y);
                    return;
                }
            }
            retroceder();
            return;
        }
        if (tecla != Teclas.RATON_IZQUIERDO) return;
        int boton = botonEn(x, y);
        if (boton == 0) {
            partida.salirDelEditor();
        } else if (boton == 1) {
            rehacer();
        } else if (boton == 2) {
            deshacer();
        } else if (boton == 3) {
            // La estrella: con un solo puntero, enseña las favoritas (lo que más se busca).
            abrirRueda();
            entrar(Categoria.values().length);
        } else if (boton == 4) {
            // La lupa: el buscador espera lo que se escriba (en la tablet sale el teclado).
            buscadorEsperando = !buscadorEsperando;
            try {
                if (com.badlogic.gdx.Gdx.input != null) com.badlogic.gdx.Gdx.input.setOnscreenKeyboardVisible(buscadorEsperando);
            } catch (RuntimeException sinTeclado) {
                // En el PC (o sin ventana) basta con escribir.
            }
        } else if (enLaEquisDeLaFicha(x, y)) {
            quitarHerramienta();
        } else if (enLaFicha(x, y)) {
            fichaPinchada = true;
            yAlPincharLaFicha = y;
            framesConLaFichaPinchada = 0;
        } else if (enElSelector(x, y)) {
            if (ruedaAbierta) cerrarRueda();
            else abrirRueda();
        } else if (menu.buscando()) {
            int fila = resultadoEn(x, y);
            if (fila >= 0) usarEntrada(resultados().get(fila).id);
            else menu.abrir();
        } else if (ruedaAbierta) {
            if (y >= FONDO_DE_LA_RUEDA) {
                cerrarRueda();      // un clic fuera la cierra
                return;
            }
            casillaPinchada = rueda.en(x, y, Pantalla.ancho / 2f, EJE_DE_LA_RUEDA);
            rueda.pinchar(x);
        } else if (enUso != null && panel.dentro(x, y)) {
            if (y < panel.y + Panel.ALTO_DEL_TITULO && enUso.alPincharElTitulo(this, x, y)) return;
            if (panel.pinchar(x, y)) return;
            if (enUso.lienzo(this) != null) {
                enUso.alPincharElPanel(this, x, y);
                return;
            }
            int fila = opcionEn(x, y);
            if (fila < 0 || opciones().get(fila).esSeccion) return;
            opcion = fila;
            opcionDelPuntero = true;
            Opcion o = opciones().get(fila);
            // Las dos flechas del final de una fila con valor: ◄ baja y ► sube; el resto de la fila es Intro.
            float flechas = panel.x + panel.ancho - ANCHO_DE_LAS_FLECHAS;
            // Su ↺, a la izquierda de las flechas: la vuelve a lo de fábrica.
            if (o.deFabrica != null && o.cambiada && x >= flechas - ANCHO_DEL_DE_FABRICA && x < flechas) {
                o.volverAFabrica(this);
                return;
            }
            if (o.flecha != null && x >= flechas) o.hacer(this, o.abierta() ? -1 : 1);      // la flechita: abre o cierra
            else if (o.conValor && x >= flechas) o.hacer(this, x < flechas + ANCHO_DE_LAS_FLECHAS / 2f ? -1 : 1);
            else o.hacer(this, 0);
        } else if (enUso != null && pincharLaMesa(x, y)) {
            return;
        } else if (enUso != null) {
            mundoPinchado = enUso.alPincharElMundo(this, x, y);
            empezarLaPulsacion(x, y, false);
        } else if (y > ALTO_DE_LA_BARRA) {
            empezarLaPulsacion(x, y, true);
        }
    }

    /** Empieza a contar una pulsación sobre el mundo: si dura sin moverse, abre el menú de lo que hay (el clic derecho del dedo). */
    private void empezarLaPulsacion(float x, float y, boolean sinHerramienta) {
        xAlPincharElMundo = x;
        yAlPincharElMundo = y;
        framesConElMundoPinchado = 0;
        pinchadoSinMover = true;
        pinchadoSinHerramienta = sinHerramienta;
    }

    /** Los botones y las muestras de color de la mesa, y la mano. Devuelve true si el clic era de ellos. */
    private boolean pincharLaMesa(float x, float y) {
        Mesa m = mesaALaVista();
        if (m == null) return false;
        int boton = m.botonEn(x, y);
        if (boton >= 0) {
            m.pulsar(boton);
            return true;
        }
        int muestra = m.muestraEn(x, y);
        if (muestra >= 0) {
            enUso.alElegirMuestra(this, muestra);
            return true;
        }
        if (m.conLaMano && m.dentro(x, y)) {
            empezarAMoverLaMesa(x, y);
            return true;
        }
        return false;
    }

    private void empezarAMoverLaMesa(float x, float y) {
        moviendoLaMesa = true;
        xDeLaMesa = x;
        yDeLaMesa = y;
    }

    /** Se ha pinchado el mundo y el botón sigue apretado: lo que se mueva el puntero es arrastrar (una pincelada, mover una pieza). */
    private boolean mundoPinchado;

    /** Se suelta una tecla o un botón. */
    public void soltar(int tecla) {
        if (moviendoLaMesa && (tecla == Teclas.RATON_CENTRAL || tecla == Teclas.RATON_IZQUIERDO)) {
            moviendoLaMesa = false;
            return;
        }
        if (tecla != Teclas.RATON_IZQUIERDO) return;
        framesConElMundoPinchado = -1;
        if (mundoPinchado) {
            mundoPinchado = false;
            if (enUso != null) enUso.alSoltarElMundo(this, partida.ratonX(), partida.ratonY());
        }
        if (rueda.arrastrando()) {
            boolean girada = rueda.soltar();
            if (!girada && casillaPinchada >= 0 && ruedaAbierta) entrar(casillaPinchada);
            casillaPinchada = -1;
        }
        if (fichaPinchada) {
            // Un toque corto en la ficha pliega o despliega su panel.
            fichaPinchada = false;
            panel.plegado = !panel.plegado;
        }
        if (panel.soltar()) recordarPanel();
    }

    private void recordarPanel() {
        menu.notas.put("panel_" + panel.id, panel.nota());
        menu.guardar();
    }

    /** El puntero está en ese punto de la imagen del juego. */
    public void ratonEn(float x, float y) {
        if (moviendoLaMesa) {
            Mesa m = mesaALaVista();
            if (m != null) m.desplazar(x - xDeLaMesa, y - yDeLaMesa);
            xDeLaMesa = x;
            yDeLaMesa = y;
            return;
        }
        if (framesConElMundoPinchado >= 0 && Math.hypot(x - xAlPincharElMundo, y - yAlPincharElMundo) > TEMBLOR_DE_LA_PULSACION) pinchadoSinMover = false;
        if (menuAbierto != null) {
            // Señalar una fila la elige (sin mover la lista).
            List<Opcion> filas = menuAbierto.opciones(this);
            int fila = menuAbierto.filaEn(x, y, filas.size());
            if (fila >= 0 && menuAbierto.seElige(filas.get(fila))) {
                menuAbierto.elegida = fila;
                menuAbierto.conElPuntero = true;
            }
            ayudaSenalada = menuAbierto.enLaEquis(x, y) ? "Cerrar el menú (Esc, clic derecho o un clic fuera)" : null;
            return;
        }
        rueda.arrastrar(x);
        panel.arrastrar(x, y);
        if (mundoPinchado && enUso != null) enUso.alArrastrarElMundo(this, x, y);
        if (fichaPinchada && yAlPincharLaFicha - y >= SUBIDA_PARA_QUITAR) quitarHerramienta();     // la ficha, hacia arriba: fuera
        senalar(x, y);
    }

    /** Lo que señala el puntero queda elegido y da su línea de ayuda. */
    private void senalar(float x, float y) {
        ayudaSenalada = null;
        int boton = botonEn(x, y);
        Mesa mesa = mesaALaVista();
        int deLaMesa = mesa == null ? -1 : mesa.botonEn(x, y);
        if (deLaMesa >= 0) {
            ayudaSenalada = Mesa.AYUDA_DE_LOS_BOTONES[deLaMesa];
        } else if (mesa != null && mesa.muestraEn(x, y) >= 0) {
            ayudaSenalada = "Un color de hace poco: tócalo para pintar con él";
        } else if (boton >= 0) {
            ayudaSenalada = new String[] { "Salir del editor (Esc)", "Rehacer (Ctrl+Y)", "Deshacer (Ctrl+Z)", "Favoritas", "Buscar (o escribe sin más)" }[boton];
        } else if (enLaEquisDeLaFicha(x, y)) {
            ayudaSenalada = "Quitar la herramienta (Esc; o lleva la ficha hacia arriba)";
        } else if (enLaFicha(x, y)) {
            ayudaSenalada = enUso.ayuda;
        } else if (enElSelector(x, y)) {
            ayudaSenalada = "La rueda de herramientas (Tab). Escribe para buscar una.";
        } else if (menu.buscando()) {
            int fila = resultadoEn(x, y);
            if (fila >= 0) resultadoElegido = fila;
        } else if (ruedaAbierta) {
            int casilla = rueda.arrastrando() ? -1 : rueda.en(x, y, Pantalla.ancho / 2f, EJE_DE_LA_RUEDA);
            if (casilla >= 0) ayudaSenalada = casillas().get(casilla).nombre + ": " + casillas().get(casilla).ayuda;
        } else {
            int fila = opcionEn(x, y);
            if (fila >= 0 && !opciones().get(fila).esSeccion) {
                opcion = fila;
                opcionDelPuntero = true;
                Opcion o = opciones().get(fila);
                float flechas = panel.x + panel.ancho - ANCHO_DE_LAS_FLECHAS;
                if (o.deFabrica != null && o.cambiada && x >= flechas - ANCHO_DEL_DE_FABRICA && x < flechas) ayudaSenalada = "Volver a lo de fábrica (Ctrl+Retroceso; se puede deshacer)";
            }
        }
    }

    // ------------------------------------------------------------------
    // Cada frame
    // ------------------------------------------------------------------

    public void actualizar() {
        rueda.actualizar();
        if (framesDeAviso > 0 && --framesDeAviso == 0) aviso = null;
        if (fichaPinchada && ++framesConLaFichaPinchada >= FRAMES_DE_PULSACION_LARGA) quitarHerramienta();     // la pulsación larga
        // La pulsación larga sobre el mundo, sin moverse: el menú de lo que hay (lo que el clic derecho, para el dedo y el lápiz).
        if (framesConElMundoPinchado >= 0 && ++framesConElMundoPinchado == FRAMES_DE_PULSACION_LARGA && pinchadoSinMover && menuAbierto == null && menuConPulsacionLarga) {
            float x = xAlPincharElMundo, y = yAlPincharElMundo;
            MenuContextual m = pinchadoSinHerramienta ? (menuDelMundo == null || enUso != null ? null : menuDelMundo.en(this, x, y))
                    : enUso == null ? null : enUso.menuContextual(this, x, y);
            if (m != null) {
                if (mundoPinchado) {
                    mundoPinchado = false;
                    enUso.alSoltarElMundo(this, x, y);
                }
                framesConElMundoPinchado = -1;
                abrirMenu(m, x, y);
            }
        }
        if (enUso != null) {
            enUso.actualizar(this);
            if (enUso != null && !panel.grande && enUso.lienzo(this) == null) colocarPanel(false);
        }
    }
}
