package metajavbots.pantallas;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.badlogic.gdx.Input;

import metajavbots.Juego;
import metajavbots.cerebro.Cerebro;
import metajavbots.cerebro.CerebroDelJuego;
import metajavbots.pantalla.CampoDeTexto;
import metajavbots.pantalla.Pincel;
import metajavbots.pantalla.Ui;
import metajavbots.pantalla.VentanaDeCodigo;
import metajavbots.progreso.Jugador;
import metajavbots.textos.Idioma;
import metajavbots.textos.T;
import metajavbots.tutor.CabezaDeEntrenador;
import metajavbots.tutor.Concepto;
import metajavbots.tutor.ConsejosDelTaller;
import metajavbots.tutor.Entrenador;

/**
 * ENTRENADOR FLOTANTE — En cualquier pantalla, la cabecita del entrenador
 * asoma por el borde derecho («Pregúntame», o F2). Al pulsarla se abre su
 * consulta: se le pregunta lo que sea («¿qué es una variable?», «hilos
 * virtuales», «Vererbung») y explica el concepto desde cero, con una
 * metáfora de peleas, pseudocódigo y un ejemplo del propio juego, con las
 * explicaciones escritas de serie (res/conceptos, sin IA); a la izquierda,
 * todos los conceptos por nivel, de lo más básico a lo de senior. Con
 * cerebro de IA, además, contesta cualquier pregunta con sus palabras,
 * mirando el código y la situación del jugador (Pantalla.contextoParaElEntrenador).
 *
 * Los errores del editor: con el ratón encima de uno (o el cursor en su
 * línea), F2 o un clic en el punto rojo del margen abre al entrenador con
 * ese error, y lo explica paso a paso a elección del jugador: con una pelea,
 * en pseudocódigo, con un ejemplo y, según la barra «Dame menos / Dame más»,
 * con una pregunta para pensar o con la pista de cómo arreglarlo (nunca la
 * solución entera). En el taller da tips mientras se programa
 * (tutor/ConsejosDelTaller); en la pelea aconseja contra el enemigo marcado
 * (EntrenadorEnLaPelea); contra los jefes está en el hospital (Hospital).
 * Se desactiva en Ajustes o con su botón (F2 lo abre igual).
 * Mientras está abierta, lo de debajo no recibe ni ratón ni teclado.
 */
public final class EntrenadorFlotante {

    // ================================================================ frecuencia y enfriamiento del bocadillo de tips
    /** Segundos antes de que salga el primer tip automático al entrar al taller (antes: 20 s). */
    public static final float ESPERA_INICIAL_TIP = 75f;
    /** Segundos de enfriamiento entre tips automáticos tras caducar solos o abrir «Explícamelo» (antes: 45 s). */
    public static final float ESPERA_ENTRE_TIPS = 180f;
    /** Segundos de enfriamiento cuando el jugador cierra el bocadillo a mano con «×» o «Gracias»/«Danke». */
    public static final float ESPERA_TRAS_CERRAR_MANUAL = 300f;
    /** Segundos que permanece visible un tip automático antes de ocultarse solo. */
    public static final float DURACION_VISIBLE_TIP = 16f;

    /** Márgenes y tamaño del botón «×» en la esquina superior derecha del bocadillo de tips. */
    public static final float MARGEN_TEXTO_IZQ = 12f;
    public static final float BOTON_CERRAR_TAM = 24f;
    private static final float BOTON_CERRAR_MARGEN = 6f;
    private static final float MARGEN_TEXTO_DER = BOTON_CERRAR_TAM + BOTON_CERRAR_MARGEN + 12f;

    /** Geometría del bocadillo de tips y de su botón «×», compartida entre el bloqueo de clic y el dibujo. */
    public record CajaTip(float x, float y, float w, float h, float anchoTexto, float cerrarX, float cerrarY, float cerrarTam) {
        public boolean contiene(float px, float py) {
            return px >= x && px < x + w && py >= y && py < y + h;
        }

        public boolean enCerrar(float px, float py) {
            return px >= cerrarX && px < cerrarX + cerrarTam && py >= cerrarY && py < cerrarY + cerrarTam;
        }
    }

    private boolean abierto;
    private final CampoDeTexto pregunta = new CampoDeTexto();
    private String ultimaPregunta = "";
    private List<Concepto> encontrados = new ArrayList<>();
    private Concepto elegido;
    private int nivel = 1;
    private final Ui.Desplazable despLista = new Ui.Desplazable(), despTexto = new Ui.Desplazable();
    private float t, hablando, dt;
    /** Cuánto ha salido la pestaña (0, recogida en el borde; 1, entera). */
    private float asomada;
    private CerebroDelJuego.Peticion peticion;
    private String respuestaIa;
    private Cerebro cerebro;
    /** El error del editor que explica (o null), y lo que el jugador ya ha pedido de él, en orden. */
    private VentanaDeCodigo.Senalado error;
    private final List<Integer> pasosDelError = new ArrayList<>();
    /** El tip del taller que enseña ahora (o null), cuánto lleva a la vista y cuánto falta para el siguiente. */
    private ConsejosDelTaller.Tip tip;
    private float tiempoDelTip, esperaDelTip = ESPERA_INICIAL_TIP;
    private final Set<String> tipsDados = new HashSet<>();
    private int tipGeneral;

    public boolean abierto() {
        return abierto;
    }

    /** Lo abre (con un concepto ya elegido, si se da su id). */
    public void abrir(Juego j, String concepto) {
        abierto = true;
        pregunta.enfocado = true;
        error = null;
        if (tip != null) cerrarTip(ESPERA_ENTRE_TIPS);
        cerebro = j.jugador == null ? null : CerebroDelJuego.tutor(j.jugador);
        String tema = j.pantalla() == null ? null : j.pantalla().temaDeJava();
        Concepto delTema = null;
        for (Concepto c : Concepto.todos()) if (c.tema.equals(tema) && (delTema == null || c.nivel < delTema.nivel)) delTema = c;
        if (concepto != null && Concepto.de(concepto) != null) elegir(Concepto.de(concepto));
        else if (delTema != null) elegir(delTema);
        else if (elegido == null && !Concepto.todos().isEmpty()) elegir(Concepto.todos().get(0));
    }

    /** Lo abre con un error del editor: lo explica paso a paso, y el concepto que hay detrás, debajo. */
    public void abrirConError(Juego j, VentanaDeCodigo.Senalado s) {
        abrir(j, null);
        pregunta.ponerTexto("");
        ultimaPregunta = "";
        encontrados = new ArrayList<>();
        error = s;
        pasosDelError.clear();
        pregunta.enfocado = false;
        Concepto c = Concepto.delError(s.marca().codigo);
        if (c != null) elegir(c);
        decir(j, s.marca().texto.split("\n\njavac:")[0]);
    }

    /** Escribe una pregunta (como si la tecleara el jugador; para las capturas y las pruebas) y elige lo primero que encuentra. */
    public void preguntar(String texto) {
        pregunta.ponerTexto(texto);
        ultimaPregunta = texto;
        error = null;
        encontrados = Concepto.buscar(texto, T.idioma, 8);
        if (!encontrados.isEmpty()) elegir(encontrados.get(0));
    }

    /** Pide un paso del error que explica (0 con una pelea, 1 pseudocódigo, 2 ejemplo, 3 cómo arreglarlo; para las capturas). */
    public void pedirPasoDelError(int paso) {
        if (error != null && !pasosDelError.contains(paso)) pasosDelError.add(paso);
    }

    /** El próximo tip del taller, ya (para las capturas). */
    public void tipAhora() {
        esperaDelTip = 0;
    }

    /** El tip que está mostrando el bocadillo ahora mismo (o null si está oculto). */
    public ConsejosDelTaller.Tip tipActual() {
        return tip;
    }

    /** Segundos que faltan para el próximo tip automático. */
    public float esperaDelTip() {
        return esperaDelTip;
    }

    /** Cierra el bocadillo de tips manualmente («×» o «Gracias»/«Danke») aplicando el enfriamiento largo. */
    public void cerrarTipManualmente() {
        cerrarTip(ESPERA_TRAS_CERRAR_MANUAL);
    }

    private void cerrarTip(float enfriamiento) {
        tip = null;
        tiempoDelTip = 0;
        esperaDelTip = Math.max(esperaDelTip, enfriamiento);
    }

    /**
     * Avanza el temporizador de tips automáticos del taller.
     * Devuelve true si acaba de aparecer un tip nuevo en este paso.
     */
    public boolean avanzarTemporizadorDeTip(String codigo, float dt) {
        if (codigo == null) {
            if (tip != null) cerrarTip(ESPERA_ENTRE_TIPS);
            return false;
        }
        if (tip == null) {
            esperaDelTip -= dt;
            if (esperaDelTip > 0) return false;
            tip = siguienteTip(codigo);
            tiempoDelTip = dt;
            return tip != null;
        }
        tiempoDelTip += dt;
        if (tiempoDelTip > DURACION_VISIBLE_TIP) {
            cerrarTip(ESPERA_ENTRE_TIPS);
        }
        return false;
    }

    /** Calcula la caja del bocadillo y la posición del botón «×» a partir del tamaño de pantalla y la altura del texto. */
    public static CajaTip cajaTip(float pantallaAncho, float pantallaAlto, float altoTexto) {
        float w = Math.min(420, pantallaAncho * 0.36f);
        float anchoTexto = Math.max(80f, w - MARGEN_TEXTO_IZQ - MARGEN_TEXTO_DER);
        float h = Math.max(altoTexto, BOTON_CERRAR_TAM) + 70f;
        float x = pantallaAncho - 80f - w;
        float y = pantallaAlto * 0.56f - h / 2f;
        float cerrarX = x + w - BOTON_CERRAR_MARGEN - BOTON_CERRAR_TAM;
        float cerrarY = y + BOTON_CERRAR_MARGEN;
        return new CajaTip(x, y, w, h, anchoTexto, cerrarX, cerrarY, BOTON_CERRAR_TAM);
    }

    private CajaTip cajaTip(Pincel p, Ui ui, String texto) {
        float w = Math.min(420, p.ancho * 0.36f);
        float tl = ui.letra * 0.8f;
        float anchoTexto = Math.max(80f, w - MARGEN_TEXTO_IZQ - MARGEN_TEXTO_DER);
        float th = p.altoDeParrafo(texto, anchoTexto, tl);
        return cajaTip(p.ancho, p.alto, th);
    }

    public void cerrar() {
        abierto = false;
        pregunta.enfocado = false;
    }

    private void elegir(Concepto c) {
        elegido = c;
        nivel = Math.max(1, Math.min(5, c.nivel));
        hablando = 2.5f;
        respuestaIa = null;
        peticion = null;
        despTexto.alPrincipio();
    }

    /** Dice algo con su voz (la de su Timbre) y mueve la boca un rato. */
    private void decir(Juego j, String texto) {
        hablando = Math.min(6, 0.6f + texto.length() * 0.045f);
        j.voz.decir(texto.replace("\n", " "), j.jugador.idioma, Entrenador.de(j.jugador.entrenador).timbre);
    }

    private static boolean hospital(Juego j) {
        return j.pantalla() != null && j.pantalla().entrenadorEnElHospital();
    }

    /** Al principio del fotograma: abierta, lo de debajo no se entera de nada; F2 la abre o la cierra (con el error que se mira, si hay). */
    public void antes(Juego j) {
        if (j.jugador == null) return;
        VentanaDeCodigo.Senalado s = j.ui.errorSenaladoAntes;
        if (!abierto && s != null && s.pedido() && !hospital(j)) abrirConError(j, s);
        else if (j.entrada.tecla(Input.Keys.F2)) {
            if (abierto) cerrar();
            else if (s != null && !hospital(j)) abrirConError(j, s);
            else abrir(j, null);
        }
        if (abierto || ratonEncima(j)) j.entrada.bloqueada = true;
    }

    private boolean ratonEncima(Juego j) {
        if (j.jugador == null || !j.jugador.entrenadorActivo) return false;
        Pincel p = j.pincel;
        float r = 20, cy = p.alto * 0.56f;
        if (j.entrada.dentro(p.ancho - (asomada > 0.5f ? r * 2 + 50 : 38), cy - r - 14, r * 2 + 50, r * 2 + 28)) return true;
        if (tip != null && j.pantalla() != null && j.pantalla().codigoParaConsejos() != null) {
            CajaTip c = cajaTip(p, j.ui, T.t("entrenador.tip." + tip.id()));
            return j.entrada.dentro(c.x, c.y, c.w, c.h);
        }
        return false;
    }

    /** Al final del fotograma: la pestaña con la cabecita y el tip (cerrada) o la consulta entera (abierta). */
    public void dibujar(Juego j, float dt) {
        if (j.jugador == null) return;
        j.entrada.bloqueada = false;
        this.dt = dt;
        t += dt;
        hablando = Math.max(0, hablando - dt);
        Entrenador en = Entrenador.de(j.jugador.entrenador);
        if (!abierto) {
            if (!j.jugador.entrenadorActivo) return;
            pestana(j, en);
            tipDelTaller(j, en, dt);
            return;
        }
        Ui ui = j.ui;
        ui.reservarEscape();
        if (j.entrada.tecla(Input.Keys.ESCAPE)) {
            if (pregunta.enfocado) pregunta.enfocado = false;
            else {
                cerrar();
                return;
            }
        }
        consulta(j, en);
    }

    /** Cerrada: la cabecita asoma por el borde derecho y se mece (vendada si está en el hospital); al pasar el ratón, «Pregúntame (F2)». */
    private void pestana(Juego j, Entrenador en) {
        Pincel p = j.pincel;
        Ui ui = j.ui;
        boolean enfermo = hospital(j);
        // Recogida, solo asoma media cabeza por el borde (no tapa lo de la pantalla); con el ratón encima, sale entera.
        float r = 20, cy = p.alto * 0.56f;
        boolean encima = j.entrada.dentro(p.ancho - (asomada > 0.5f ? r * 2 + 50 : 38), cy - r - 14, r * 2 + 50, r * 2 + 28);
        asomada += (encima || tip != null ? 1 - asomada : -asomada) * Math.min(1, dt * 12);
        float cx = p.ancho - 26 + (1 - asomada) * 24, sale = asomada * 10;
        p.caja(cx - r - 12 - sale, cy - r - 14, r * 2 + 40, r * 2 + 28, 14, enfermo ? 0xE8ECEF : ui.tema.panelAlto, 0.92f);
        p.bordeRedondeado(cx - r - 12 - sale, cy - r - 14, r * 2 + 40, r * 2 + 28, 14, enfermo ? 0xFF4D4D : ui.tema.acento, 0.9f, 1.5f);
        CabezaDeEntrenador.dibujar(p, en, cx - sale, cy, r, false, t);
        if (enfermo) {
            p.rect(cx - sale - r, cy - r * 0.55f, r * 2, r * 0.3f, 0xFFFFFF, 0.95f);
            p.textoCentrado("+", cx - sale + r * 0.9f, cy - r - 11, 18, true, 0xFF4D4D, 1);
        } else {
            p.textoCentrado("?", cx - sale + r * 0.9f, cy - r - 11, 16, true, 0xFFD23F, 1);
        }
        if (encima) ui.avisar(enfermo ? T.t("entrenador.hospital.ayuda") : T.t("entrenador.pestana"));
        if (j.entrada.clic(cx - r - 12 - sale, cy - r - 14, r * 2 + 40, r * 2 + 28)) abrir(j, null);
    }

    // ================================================================ los tips del taller

    /** Mientras se programa un robot: de vez en cuando, un tip en una viñeta junto a la cabecita. */
    private void tipDelTaller(Juego j, Entrenador en, float dt) {
        String codigo = j.pantalla() == null ? null : j.pantalla().codigoParaConsejos();
        if (avanzarTemporizadorDeTip(codigo, dt)) {
            decir(j, T.t("entrenador.tip." + tip.id()));
        }
        if (tip == null) return;
        Pincel p = j.pincel;
        Ui ui = j.ui;
        String texto = T.t("entrenador.tip." + tip.id());
        float tl = ui.letra * 0.8f;
        CajaTip c = cajaTip(p, ui, texto);
        float aparece = Math.min(1, tiempoDelTip * 4);
        p.caja(c.x + 3, c.y + 4, c.w, c.h, 12, 0x000000, 0.3f * aparece);
        p.caja(c.x, c.y, c.w, c.h, 12, 0xF5F3EA, 0.97f * aparece);
        p.triangulo(c.x + c.w, c.y + c.h / 2 - 10, c.x + c.w, c.y + c.h / 2 + 10, c.x + c.w + 14, c.y + c.h / 2, 0xF5F3EA, 0.97f * aparece);
        p.parrafo(texto, c.x + MARGEN_TEXTO_IZQ, c.y + 10, c.anchoTexto, tl, 0x1E1E1E, aparece);
        if (botonCerrarTip(j, c, aparece)) {
            cerrarTipManualmente();
            return;
        }
        float by = c.y + c.h - 50, bw = (c.w - 32) / 3;
        if (ui.boton(T.t("entrenador.tip.explica"), c.x + 8, by, bw, 40, Ui.Estilo.PLANO)) {
            String concepto = tip.concepto();
            cerrarTip(ESPERA_ENTRE_TIPS);
            abrir(j, concepto);
            return;
        }
        if (ui.boton(T.t("entrenador.tip.otro"), c.x + 16 + bw, by, bw, 40, Ui.Estilo.PLANO)) {
            tip = siguienteTip(codigo);
            tiempoDelTip = 0;
            if (tip != null) decir(j, T.t("entrenador.tip." + tip.id()));
            return;
        }
        if (ui.boton(T.t("entrenador.tip.cerrar"), c.x + 24 + 2 * bw, by, bw, 40, Ui.Estilo.PLANO)) {
            cerrarTipManualmente();
        }
    }

    /** Botón pequeño «×» en la esquina superior derecha del bocadillo: cierra el tip y consume el clic. */
    private boolean botonCerrarTip(Juego j, CajaTip c, float aparece) {
        Pincel p = j.pincel;
        Ui ui = j.ui;
        boolean encima = j.entrada.dentro(c.cerrarX, c.cerrarY, c.cerrarTam, c.cerrarTam);
        boolean apretado = encima && j.entrada.pulsado;
        if (encima) {
            p.caja(c.cerrarX, c.cerrarY, c.cerrarTam, c.cerrarTam, 6, apretado ? 0xCFC8B4 : 0xE2DCCB, 0.95f * aparece);
            ui.avisar(T.t("entrenador.tip.cerrar_x"));
        }
        float tam = Math.min(18f, ui.letra * 0.9f);
        float desplaza = apretado ? 1f : 0f;
        p.textoCentrado("×", c.cerrarX + c.cerrarTam / 2f, c.cerrarY + desplaza + (c.cerrarTam - tam * 1.15f) / 2f, tam, true,
                encima ? 0x1E1E1E : 0x625D52, aparece);
        if (j.entrada.clic(c.cerrarX, c.cerrarY, c.cerrarTam, c.cerrarTam)) {
            if (ui.sonidoDeClic != null) ui.sonidoDeClic.run();
            return true;
        }
        return false;
    }

    /** El primer tip del código que aún no ha dado; si ya los dio todos, uno de los generales (por turnos). */
    private ConsejosDelTaller.Tip siguienteTip(String codigo) {
        List<ConsejosDelTaller.Tip> l = ConsejosDelTaller.de(codigo);
        List<ConsejosDelTaller.Tip> generales = new ArrayList<>();
        for (ConsejosDelTaller.Tip x : l) {
            if (x.id().startsWith("general_")) generales.add(x);
            else if (tipsDados.add(x.id())) return x;
        }
        return generales.isEmpty() ? null : generales.get(tipGeneral++ % generales.size());
    }

    // ================================================================ la consulta

    /** Abierta: a la izquierda, el entrenador, la pregunta y la lista; a la derecha, la explicación (o el hospital). */
    private void consulta(Juego j, Entrenador en) {
        Pincel p = j.pincel;
        Ui ui = j.ui;
        Idioma idioma = j.jugador.idioma;
        p.rect(0, 0, p.ancho, p.alto, 0x000000, 0.6f);
        float w = Math.min(1400, p.ancho - 60), h = p.alto - 60, x = (p.ancho - w) / 2, y = 30;
        p.caja(x + 4, y + 6, w, h, 16, 0x000000, 0.4f);
        p.caja(x, y, w, h, 16, ui.tema.panelAlto, 1);
        p.bordeRedondeado(x, y, w, h, 16, ui.tema.acento, 1, 2);
        if (ui.boton("✕", x + w - 58, y + 12, 46, 42, Ui.Estilo.PLANO)) {
            cerrar();
            return;
        }
        if (hospital(j)) {
            enElHospital(j, en, x, y, w, h);
            return;
        }
        float lw = Math.min(380, w * 0.32f);
        // El entrenador, que habla un rato al explicar.
        CabezaDeEntrenador.dibujar(p, en, x + 56, y + 62, 34, hablando > 0, t);
        p.texto(T.f("entrenador.titulo", en.nombre.en(idioma)), x + 104, y + 30, ui.letra * 1.1f, true, ui.tema.texto, 1);
        p.texto(ui.recortarTexto(en.papel.en(idioma), lw - 110, ui.letra * 0.75f, false), x + 104, y + 30 + ui.letra * 1.5f, ui.letra * 0.75f, false,
                ui.tema.textoSuave, 1);
        // Desactivarlo (y volver a activarlo) sin ir a Ajustes.
        String quitar = j.jugador.entrenadorActivo ? T.t("entrenador.desactivar") : T.t("ajustes.entrenador");
        float qw = Math.min(320, p.ancho(quitar, ui.letra * 0.75f, false) + 30);
        if (ui.boton(quitar, x + w - 70 - qw, y + 12, qw, 42, Ui.Estilo.PLANO)) {
            j.jugador.entrenadorActivo = !j.jugador.entrenadorActivo;
            if (!j.jugador.entrenadorActivo) {
                decir(j, T.t("entrenador.desactivado"));
                cerrar();
            }
            j.guardar();
            return;
        }
        float fy = y + 118;
        boolean intro = pregunta.dibujar(ui, x + 16, fy, lw - 16, 46, T.t("entrenador.pregunta"));
        if (!pregunta.texto().equals(ultimaPregunta)) {
            ultimaPregunta = pregunta.texto();
            encontrados = ultimaPregunta.isBlank() ? new ArrayList<>() : Concepto.buscar(ultimaPregunta, idioma, 8);
            despLista.alPrincipio();
        }
        if (intro && !encontrados.isEmpty()) {
            error = null;
            elegir(encontrados.get(0));
        } else if (intro && !ultimaPregunta.isBlank() && cerebro != null && peticion == null) {
            // Nada escrito de serie que se le parezca: con IA, se lo pregunta a ella.
            preguntarALaIa(j, elegido, idioma, en);
        }
        lista(j, x + 16, fy + 58, lw - 16, y + h - fy - 124, idioma);
        barraDeAyuda(j, x + 16, y + h - 58, lw - 16);
        explicacion(j, x + lw + 20, y + 70, w - lw - 40, h - 86, idioma, en);
    }

    /** «− Dame menos» y «+ Dame más» (la misma barra del tutor del taller): regula cuánto ayuda. */
    private void barraDeAyuda(Juego j, float x, float y, float w) {
        Ui ui = j.ui;
        Jugador jug = j.jugador;
        float mitad = (w - 8) / 2;
        if (ui.boton(T.t("tutor.menos"), x, y, mitad, 42, Ui.Estilo.PLANO, jug.ayudaDelTutor > 0, T.t("tutor.menos_ayuda"))) {
            jug.ayudaDelTutor = Math.max(0, jug.ayudaDelTutor - 0.2f);
            decir(j, T.t("tutor.vale_menos"));
        }
        if (ui.boton(T.t("tutor.mas"), x + mitad + 8, y, mitad, 42, Ui.Estilo.PLANO, jug.ayudaDelTutor < 1, T.t("tutor.mas_ayuda"))) {
            jug.ayudaDelTutor = Math.min(1, jug.ayudaDelTutor + 0.2f);
            decir(j, T.t("tutor.vale_mas"));
        }
        j.pincel.texto(T.f("entrenador.ayuda", Math.round(PanelDelTutor.ayuda(jug, 0) * 10)), x, y - ui.letra * 1.1f, ui.letra * 0.7f, false,
                ui.tema.textoApagado, 1);
    }

    /** Lo que se parece a la pregunta; sin pregunta, todos los conceptos de un nivel (con pestañas del 1 al 5). */
    private void lista(Juego j, float x, float y, float w, float h, Idioma idioma) {
        Pincel p = j.pincel;
        Ui ui = j.ui;
        List<Concepto> l;
        if (!ultimaPregunta.isBlank()) {
            l = encontrados;
            if (l.isEmpty()) {
                p.parrafo(T.t("entrenador.no_se"), x + 4, y + 6, w - 8, ui.letra * 0.85f, ui.tema.textoSuave, 1);
                return;
            }
        } else {
            String[] niveles = T.t("entrenador.niveles").split(";");
            int elegida = ui.pestanas(niveles, nivel - 1, x, y, w, 40);
            if (elegida != nivel - 1) {
                nivel = elegida + 1;
                despLista.alPrincipio();
            }
            y += 50;
            h -= 50;
            l = new ArrayList<>();
            for (Concepto c : Concepto.todos()) if (c.nivel == nivel) l.add(c);
        }
        float d = ui.desplazable(despLista, x, y, w, h);
        float yy = y - d, alto = ui.letra * 1.9f;
        for (Concepto c : l) {
            boolean este = c == elegido;
            if (yy + alto > y - 4 && yy < y + h + 4
                    && ui.boton(c.titulo.en(idioma), x, yy, w - 14, alto - 4, este ? Ui.Estilo.ELEGIDO : Ui.Estilo.PLANO)) {
                error = null;
                elegir(c);
            }
            yy += alto;
        }
        ui.finDesplazable(despLista, yy + d - y);
    }

    /** El error (si lo hay), el concepto elegido (título, párrafos, pseudocódigo, ejemplo), la IA y los botones. */
    private void explicacion(Juego j, float x, float y, float w, float h, Idioma idioma, Entrenador en) {
        Ui ui = j.ui;
        Pincel p = j.pincel;
        Concepto c = elegido;
        if (c == null) return;
        float abajo = 56;
        float d = ui.desplazable(despTexto, x, y, w, h - abajo);
        float yy = y - d;
        if (error != null) yy = explicarError(j, c, x, yy, w, idioma) + ui.letra;
        p.texto(c.titulo.en(idioma), x, yy, ui.letra * 1.5f, true, ui.tema.acento, 1);
        yy += ui.letra * 2.2f;
        for (String par : c.parrafos(idioma)) yy += p.parrafo(par, x, yy, w - 20, ui.letra, ui.tema.texto, 1) + ui.letra * 0.6f;
        yy = bloqueDeCodigo(j, T.t("entrenador.pseudo"), c.pseudo(idioma), false, x, yy, w);
        yy = bloqueDeCodigo(j, T.t("entrenador.ejemplo"), c.ejemplo(idioma), true, x, yy, w);
        yy = respuestaDeLaIa(j, x, yy, w);
        ui.finDesplazable(despTexto, yy + d - y);
        botones(j, c, x, y + h - abajo + 8, w, idioma, en);
    }

    /** Un recuadro de código (Java, con los comentarios en gris) o de pseudocódigo, con su rótulo encima. Devuelve dónde acaba. */
    private float bloqueDeCodigo(Juego j, String rotulo, String texto, boolean java, float x, float yy, float w) {
        if (texto == null || texto.isBlank()) return yy;
        Pincel p = j.pincel;
        Ui ui = j.ui;
        p.texto(rotulo, x, yy, ui.letra * 0.85f, true, ui.tema.textoSuave, 1);
        yy += ui.letra * 1.4f;
        String[] lineas = texto.split("\n");
        float tl = ui.letra * 0.85f, alto = p.altoDeLinea(tl);
        p.caja(x, yy, w - 20, lineas.length * alto + 16, 8, java ? 0x0E1014 : 0x1B2430, 1);
        for (int i = 0; i < lineas.length; i++) {
            String s = lineas[i];
            int com = java ? s.indexOf("//") : -1;
            float lx = x + 12, ly = yy + 8 + i * alto;
            if (com >= 0) {
                String antes = s.substring(0, com);
                p.texto(antes, lx, ly, tl, false, ui.tema.cadena, 1);
                lx += p.ancho(antes, tl, false);
                s = s.substring(com);
            }
            p.texto(ui.recortarTexto(s, x + w - 32 - lx, tl, false), lx, ly, tl, false, com >= 0 ? 0x7F8C8D : java ? ui.tema.cadena : 0xE8E2C8, 1);
        }
        return yy + lineas.length * alto + 28;
    }

    // ================================================================ los errores del editor

    /** Los pasos que se pueden pedir de un error: con una pelea, en pseudocódigo, con un ejemplo y cómo arreglarlo. */
    private static final String[] PASOS = {"entrenador.error.pelea", "entrenador.error.pseudo", "entrenador.error.ejemplo", "entrenador.error.arreglo"};

    /**
     * El error del editor: la línea (con el subrayado), lo que dice el editor a su nivel, el concepto de detrás y
     * los pasos que el jugador vaya pidiendo, cada uno debajo del anterior (y el entrenador lo dice en voz alta).
     */
    private float explicarError(Juego j, Concepto c, float x, float yy, float w, Idioma idioma) {
        Pincel p = j.pincel;
        Ui ui = j.ui;
        VentanaDeCodigo.Marca m = error.marca();
        p.texto(T.f("entrenador.error.titulo", m.linea), x, yy, ui.letra, true, ui.tema.mal, 1);
        yy += ui.letra * 1.6f;
        // La línea tal cual, con la ola roja debajo de donde marca javac.
        float tl = ui.letra * 0.9f, letra = p.anchoDeLetra(tl), alto = p.altoDeLinea(tl);
        String linea = error.linea() == null ? "" : error.linea().replace("\t", "    ");
        int sangria = linea.length() - linea.stripLeading().length();
        String visible = linea.strip();
        p.caja(x, yy, w - 20, alto + 22, 8, 0x0E1014, 1);
        p.texto(ui.recortarTexto(m.linea + "  " + visible, w - 44, tl, false), x + 12, yy + 8, tl, false, ui.tema.cadena, 1);
        int col = Math.max(0, m.columna - 1 - sangria) + (m.linea + "  ").length();
        float ox = x + 12 + col * letra;
        for (int i = 0; i < 5 && ox + i * letra * 0.5f < x + w - 30; i++) {
            float a = ox + i * letra * 0.5f;
            p.linea(a, yy + alto + 12, a + letra * 0.25f, yy + alto + 8 + (i % 2) * 6, 2, ui.tema.mal, 1);
        }
        yy += alto + 34;
        String dice = m.texto.split("\n\njavac:")[0];
        yy += p.parrafo(dice, x, yy, w - 20, ui.letra, ui.tema.texto, 1) + ui.letra * 0.5f;
        if (m.mensaje != null && !m.mensaje.isBlank())
            yy += p.parrafo("javac: " + m.mensaje, x, yy, w - 20, ui.letra * 0.8f, ui.tema.textoApagado, 1) + ui.letra * 0.6f;
        yy += p.parrafo(T.f("entrenador.error.concepto", c.titulo.en(idioma)), x, yy, w - 20, ui.letra * 0.9f, ui.tema.textoSuave, 1) + 8;
        // Los pasos, de botón en botón: el jugador decide qué quiere ver.
        float bw = (w - 20 - 24) / 4;
        for (int i = 0; i < PASOS.length; i++) {
            boolean visto = pasosDelError.contains(i);
            if (ui.boton(T.t(PASOS[i]), x + i * (bw + 8), yy, bw, 44, visto ? Ui.Estilo.ELEGIDO : Ui.Estilo.NORMAL) && !visto) {
                pasosDelError.add(i);
                decir(j, textoDelPaso(j, i, c, idioma));
            }
        }
        yy += 58;
        for (int i : pasosDelError) {
            p.texto(T.t(PASOS[i]), x, yy, ui.letra * 0.85f, true, ui.tema.acento, 1);
            yy += ui.letra * 1.4f;
            if (i == 1) yy = bloqueDeCodigo(j, "", c.pseudo(idioma), false, x, yy - ui.letra * 1.4f, w);
            else if (i == 2) yy = bloqueDeCodigo(j, "", c.ejemplo(idioma), true, x, yy - ui.letra * 1.4f, w);
            else yy += p.parrafo(textoDelPaso(j, i, c, idioma), x, yy, w - 20, ui.letra, ui.tema.texto, 1) + ui.letra * 0.8f;
        }
        p.linea(x, yy, x + w - 20, yy, 1, ui.tema.textoApagado, 0.5f);
        return yy + 12;
    }

    /**
     * Lo que dice cada paso: la pelea es la metáfora del concepto (su segundo párrafo); el arreglo, según la
     * ayuda: con poca, la pregunta para pensar (lo de nivel junior, más escueto); con más, la pista con ejemplo
     * (lo de nivel noob). Nunca el código arreglado entero.
     */
    private String textoDelPaso(Juego j, int paso, Concepto c, Idioma idioma) {
        VentanaDeCodigo.Marca m = error.marca();
        switch (paso) {
            case 0: {
                List<String> ps = c.parrafos(idioma);
                return ps.size() > 1 ? ps.get(1) : ps.isEmpty() ? "" : ps.get(0);
            }
            case 1:
                return c.pseudo(idioma);
            case 2:
                return T.t("entrenador.ejemplo");
            default: {
                boolean poca = PanelDelTutor.ayuda(j.jugador, 0) < 0.45f;
                String s = Explicador.enNivel(m.codigo, m.mensaje, m.linea, poca ? "junior" : "noob", idioma);
                if (s == null) return T.t("entrenador.error.sin_pista");
                return poca ? T.t("entrenador.error.piensa") + " " + s : s;
            }
        }
    }

    // ================================================================ el hospital

    /** Contra un jefe: la camilla, el parte médico con la causa absurda de esta vez y que necesita que ganes. */
    private void enElHospital(Juego j, Entrenador en, float x, float y, float w, float h) {
        Pincel p = j.pincel;
        Ui ui = j.ui;
        Idioma idioma = j.jugador.idioma;
        p.texto(T.f("entrenador.hospital.titulo", en.nombre.en(idioma)), x + 30, y + 26, ui.letra * 1.4f, true, ui.tema.mal, 1);
        float ch = Math.min(h * 0.6f, 420);
        Hospital.camilla(p, en, x + 30, y + 80, w - 60, ch, t);
        float ty = y + 90 + ch;
        String causa = Hospital.causa(j.pantalla().hashCode());
        ty += p.parrafo(T.f("entrenador.hospital.parte", causa), x + 30, ty, w - 60, ui.letra * 1.1f, true, ui.tema.texto, 1) + 12;
        p.parrafo(T.t("entrenador.hospital.pide"), x + 30, ty, w - 60, ui.letra, ui.tema.textoSuave, 1);
    }

    // ================================================================ la IA

    /** Lo que contestó la IA (o «pensando…»), debajo de la explicación de serie. */
    private float respuestaDeLaIa(Juego j, float x, float yy, float w) {
        Pincel p = j.pincel;
        Ui ui = j.ui;
        if (peticion != null && peticion.lista) {
            respuestaIa = peticion.respuesta != null ? peticion.respuesta : T.f("cerebro.error", peticion.error);
            peticion = null;
            decir(j, respuestaIa);
        }
        if (peticion != null) {
            yy += p.parrafo(T.t("cerebro.pensando"), x, yy, w - 20, ui.letra * 0.9f, ui.tema.textoApagado, 1) + 10;
        } else if (respuestaIa != null) {
            p.texto(T.t("entrenador.ia_dice"), x, yy, ui.letra * 0.85f, true, ui.tema.textoSuave, 1);
            yy += ui.letra * 1.4f;
            yy += p.parrafo(respuestaIa, x, yy, w - 20, ui.letra, ui.tema.texto, 1) + 10;
        }
        return yy;
    }

    private void botones(Juego j, Concepto c, float x, float y, float w, Idioma idioma, Entrenador en) {
        Ui ui = j.ui;
        List<String> textos = new ArrayList<>();
        List<Concepto> relacionados = new ArrayList<>();
        textos.add("♪ " + T.t("entrenador.leer"));
        textos.add(T.t("entrenador.ia"));
        // Lo del juego y los cerebros de IA se enseña además con dibujos.
        int fijos = 2;
        if (c.guia > 0) {
            textos.add(T.t("guia.abrir"));
            fijos = 3;
        }
        // Los relacionados, solo los que caben en una fila (la botonera saltaría de línea y se saldría del recuadro).
        float ocupado = 0;
        for (String s : textos) ocupado += j.pincel.ancho(s, ui.letra, true) + 48;
        for (String id : c.ver) {
            Concepto r = Concepto.de(id);
            if (r == null || relacionados.size() >= 3) continue;
            float bw = j.pincel.ancho("→ " + r.titulo.en(idioma), ui.letra, true) + 48;
            if (ocupado + bw > w - 20) break;
            ocupado += bw;
            relacionados.add(r);
            textos.add("→ " + r.titulo.en(idioma));
        }
        Ui.Estilo[] estilos = new Ui.Estilo[textos.size()];
        boolean[] activos = new boolean[textos.size()];
        String[] ayudas = new String[textos.size()];
        java.util.Arrays.fill(estilos, Ui.Estilo.PLANO);
        java.util.Arrays.fill(activos, true);
        activos[1] = cerebro != null && peticion == null;
        ayudas[1] = cerebro == null ? T.t("cerebro.sin") : null;
        int pulsado = ui.botonera(textos.toArray(new String[0]), estilos, activos, ayudas, x, y, w - 20, 42);
        if (pulsado == 0) {
            decir(j, String.join(" ", c.parrafos(idioma)));
        } else if (pulsado == 1) {
            preguntarALaIa(j, c, idioma, en);
        } else if (pulsado == 2 && c.guia > 0) {
            cerrar();
            j.ir(new GuiaDeCerebros(c.guia - 1));
        } else if (pulsado >= fijos) {
            error = null;
            elegir(relacionados.get(pulsado - fijos));
        }
    }

    /**
     * Con IA: contesta la pregunta del jugador (o explica el concepto, o el error) con sus palabras, mirando lo
     * que tiene delante (el código con números de línea, los errores, cómo va la pelea), con pseudocódigo,
     * trocitos de Java y metáforas de peleas, y nunca la solución entera; cuánto ayuda, según la barra.
     */
    private void preguntarALaIa(Juego j, Concepto c, Idioma idioma, Entrenador en) {
        String q = ultimaPregunta.isBlank() ? c == null ? "" : c.titulo.en(idioma) : ultimaPregunta;
        int ayuda = Math.round(PanelDelTutor.ayuda(j.jugador, 0) * 10);
        String sistema = "You are " + en.nombre.en + ", " + en.estilo + ", the coach in MetajavBots, a game where you program fighting robots in Java "
                + "(class MiRobot extends Metajavbot; API: cadaTurno, alVerEnemigo, avanzar, girar, apuntarHacia, atacar, energia, enemigosVistos, memoria). "
                + "Answer any question about Java, object orientation or the game, to someone who may know nothing about programming: plain words, "
                + "a fight metaphor with robots, LOTS of pseudocode and small Java snippets (Java standard library and the robot API). "
                + "If the player's code or fight is given, look at it and talk about THEIR code and situation (line numbers, what happens, why they lose). "
                + "Coach for real: hints, guiding questions and small pieces; NEVER the complete solution nor the whole program, even if asked. "
                + "Help level " + ayuda + "/10: at low levels only a guiding question and a small hint; at high levels pseudocode and a short snippet. "
                + "Answer in " + idioma.name() + " (" + idioma.locale().getDisplayLanguage(java.util.Locale.ENGLISH) + "), at most 200 words, no markdown.";
        StringBuilder contexto = new StringBuilder();
        if (c != null) contexto.append("Built-in explanation of «").append(c.titulo.en).append("»: ").append(String.join(" ", c.parrafos(Idioma.EN)))
                .append("\n\n");
        if (error != null) contexto.append("The editor marks line ").append(error.marca().linea).append(": ").append(error.linea()).append("\njavac: ")
                .append(error.marca().mensaje).append("\n\n");
        String pantalla = j.pantalla() == null ? null : j.pantalla().contextoParaElEntrenador();
        if (pantalla != null) contexto.append(pantalla).append("\n\n");
        else if (error != null) contexto.append("The player's code:\n").append(conNumeros(error.codigo(), 160)).append("\n\n");
        contexto.append("The player asks: ").append(q.isBlank() ? "explain this error" : q);
        peticion = CerebroDelJuego.Peticion.lanzar(cerebro, sistema, CerebroDelJuego.uno(contexto.toString()), 280);
        respuestaIa = null;
    }

    /** El código con su número delante de cada línea (como mucho «max» líneas), para que la IA hable de líneas concretas. */
    public static String conNumeros(String codigo, int max) {
        if (codigo == null) return "";
        String[] l = codigo.split("\n", -1);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < l.length && i < max; i++) sb.append(i + 1).append(": ").append(l[i]).append('\n');
        if (l.length > max) sb.append("…\n");
        return sb.toString();
    }

    /** El contexto de una pantalla con editor: qué es, el código numerado y los errores marcados. */
    public static String contextoDeCodigo(String que, VentanaDeCodigo editor) {
        StringBuilder sb = new StringBuilder(que).append("\nThe player's code:\n").append(conNumeros(editor.doc.texto(), 160));
        if (!editor.marcas.isEmpty()) {
            sb.append("Errors marked by the editor:\n");
            for (VentanaDeCodigo.Marca m : editor.marcas) {
                sb.append("line ").append(m.linea).append(": ").append(m.mensaje != null ? m.mensaje : m.texto).append('\n');
            }
        }
        return sb.toString();
    }
}
