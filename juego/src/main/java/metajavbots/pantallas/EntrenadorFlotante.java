package metajavbots.pantallas;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import com.badlogic.gdx.Input;

import metajavbots.Juego;
import metajavbots.cerebro.Cerebro;
import metajavbots.cerebro.CerebroDelJuego;
import metajavbots.pantalla.CampoDeTexto;
import metajavbots.pantalla.Pantalla;
import metajavbots.pantalla.Pincel;
import metajavbots.pantalla.Ui;
import metajavbots.pantalla.VentanaDeCodigo;
import metajavbots.progreso.Jugador;
import metajavbots.textos.Idioma;
import metajavbots.textos.T;
import metajavbots.tutor.CabezaDeEntrenador;
import metajavbots.tutor.Concepto;
import metajavbots.tutor.ConsejosDelJuego;
import metajavbots.tutor.ConsejosDelTaller;
import metajavbots.tutor.Entrenador;

/**
 * ENTRENADOR FLOTANTE — En cualquier pantalla (salvo la portada Inicio),
 * permanece recogido en la esquina inferior derecha con una discreta flechita
 * («◀») que invita a desplegarlo. Al pulsarla aparece su cabecita completa (con
 * «▶» para volver a recogerlo), y al hacer clic en la cabecita se abre su
 * ventana de consulta («Pregúntale a …»), donde responde dudas de programación
 * (Java, POO, errores) y de cualquier funcionalidad del juego (sin IA y con IA).
 *
 * Con recurrencia moderada, asoma en la esquina con un cartelito de consejos
 * pegado a él y botón «×» para cerrarlo; tras cerrarlo a mano tarda más en
 * volver y, si no estaba fijado abierto, vuelve a meterse en la esquina.
 */
public final class EntrenadorFlotante {

    // ================================================================ frecuencia y enfriamiento del cartelito
    /** Segundos antes de que salga el primer consejo automático al iniciar la sesión. */
    public static final float ESPERA_INICIAL_TIP = 60f;
    /** Segundos de enfriamiento normal entre consejos espontáneos (4 minutos). */
    public static final float ESPERA_ENTRE_TIPS = 240f;
    /** Segundos de enfriamiento ampliado cuando el jugador cierra el cartelito con «×» o «Gracias» (7 minutos). */
    public static final float ESPERA_TRAS_CERRAR_MANUAL = 420f;
    /** Segundos que permanece visible un consejo automático antes de recogerse solo. */
    public static final float DURACION_VISIBLE_TIP = 16f;

    /** Márgenes y tamaño del botón «×» en la esquina superior derecha del cartelito de consejos. */
    public static final float MARGEN_TEXTO_IZQ = GeometriaEntrenador.MARGEN_TEXTO_IZQ;
    public static final float BOTON_CERRAR_TAM = GeometriaEntrenador.BOTON_CERRAR_TAM;
    /** Margen vertical mínimo entre el cartelito y los bordes superior e inferior de la pantalla. */
    public static final float MARGEN_VERTICAL_TIP = GeometriaEntrenador.MARGEN_VERTICAL_TIP;

    /** Geometría de la pestaña recogida en la esquina inferior derecha: flecha «◀», cabecita medio asomada e insignia «?». */
    public record CajaRecogida(float x, float y, float w, float h, float flechaX, float flechaY,
            float cabezaX, float cabezaY, float radio, float ayudaX, float ayudaY, float ayudaRadio) {
        /** Indica si un punto cae sobre la insignia circular «?» situada encima de la cabecita. */
        public boolean enAyuda(float px, float py) {
            float dx = px - ayudaX;
            float dy = py - ayudaY;
            return dx * dx + dy * dy <= ayudaRadio * ayudaRadio;
        }

        /** Indica si un punto de pantalla cae dentro de la pestaña recogida (flecha, cabecita medio asomada o insignia «?»). */
        public boolean contiene(float px, float py) {
            return (px >= x && px < x + w && py >= y && py < y + h) || enAyuda(px, py);
        }

        /** Altura total vertical aproximada de la cabeza dibujada (de los rasgos superiores a la barbilla). */
        public float altoTotalCabeza() {
            return radio * 2.15f;
        }

        /** Altura vertical de la cabeza que sobresale visiblemente dentro de la pantalla en estado recogido. */
        public float altoVisibleCabeza(float pantallaAlto) {
            float arriba = Math.max(0f, cabezaY - radio * 1.25f);
            float abajo = Math.min(pantallaAlto, cabezaY + radio * 0.90f);
            return Math.max(0f, abajo - arriba);
        }
    }

    /** Geometría del cartelito de consejos y de su botón «×», compartida entre clics y dibujo. */
    public record CajaTip(float x, float y, float w, float h, float anchoTexto, float cerrarX, float cerrarY, float cerrarTam) {
        /** Indica si un punto de pantalla cae dentro del cartelito. */
        public boolean contiene(float px, float py) {
            return px >= x && px < x + w && py >= y && py < y + h;
        }

        /** Indica si un punto de pantalla cae dentro del botón «×». */
        public boolean enCerrar(float px, float py) {
            return px >= cerrarX && px < cerrarX + cerrarTam && py >= cerrarY && py < cerrarY + cerrarTam;
        }
    }

    /** Geometría de la pestaña desplegada con la cabecita del entrenador y la franja «▶» para recogerlo. */
    public record CajaPestana(float x, float y, float w, float h, float cabezaX, float cabezaY, float radio,
            float insigniaX, float insigniaY, float recogerX, float recogerY, float recogerW, float recogerH) {
        /** Indica si un punto de pantalla cae dentro de la pestaña desplegada. */
        public boolean contiene(float px, float py) {
            return px >= x && px < x + w && py >= y && py < y + h;
        }

        /** Indica si un punto cae sobre la flecha «▶» para recoger al entrenador. */
        public boolean enRecoger(float px, float py) {
            return px >= recogerX && px < recogerX + recogerW && py >= recogerY && py < recogerY + recogerH;
        }

        /** Indica si un punto cae sobre la zona de la cabecita (abre la ventana de consulta). */
        public boolean enCabeza(float px, float py) {
            return contiene(px, py) && !enRecoger(px, py);
        }
    }

    private boolean abierto;
    /** Si el jugador desplegó manualmente al entrenador con la flechita «◀». */
    private boolean desplegadoFijo;
    private String ultimoEntrenadorId;
    private final CampoDeTexto pregunta = new CampoDeTexto();
    private String ultimaPregunta = "";
    private List<Concepto> encontrados = new ArrayList<>();
    private Concepto elegido;
    private int nivel = 1;
    private final Ui.Desplazable despLista = new Ui.Desplazable(), despTexto = new Ui.Desplazable(), despTip = new Ui.Desplazable();
    private float t, hablando, dt;
    private float asomada;
    private CerebroDelJuego.Peticion peticion;
    private String respuestaIa;
    private Cerebro cerebro;
    private VentanaDeCodigo.Senalado error;
    private final List<Integer> pasosDelError = new ArrayList<>();
    private final PlanificadorDeConsejos planificador;

    /** Crea el entrenador flotante con selección aleatoria estándar para consejos de jugabilidad. */
    public EntrenadorFlotante() {
        this(new Random());
    }

    /** Crea el entrenador flotante con un generador aleatorio dado (útil para pruebas deterministas). */
    public EntrenadorFlotante(Random azar) {
        this.planificador = new PlanificadorDeConsejos(azar);
    }

    /** Indica si la ventana completa de consulta está abierta. */
    public boolean abierto() {
        return abierto;
    }

    /** Indica si la cabecita está visible (porque el jugador la desplegó o porque asoma con un consejo). */
    public boolean desplegado() {
        return desplegadoFijo || planificador.tipActual() != null;
    }

    /** Indica si el jugador fijó desplegado al entrenador pulsando la flechita «◀». */
    public boolean desplegadoFijo() {
        return desplegadoFijo;
    }

    /** Despliega la cabecita del entrenador en la esquina inferior derecha. */
    public void desplegar() {
        desplegadoFijo = true;
    }

    /** Vuelve a meter al entrenador en la esquina inferior derecha con su flechita «◀». */
    public void recoger() {
        desplegadoFijo = false;
        asomada = 0f;
    }

    /** Abre la ventana de consulta (con un concepto ya elegido, si se da su id). */
    public void abrir(Juego j, String concepto) {
        abierto = true;
        pregunta.enfocado = true;
        error = null;
        if (planificador.tipActual() != null) {
            planificador.cerrarNormal();
            despTip.alPrincipio();
        }
        sincronizarEntrenador(j.jugador);
        cerebro = j.jugador == null ? null : CerebroDelJuego.tutor(j.jugador);
        String tema = j.pantalla() == null ? null : j.pantalla().temaDeJava();
        Concepto delTema = null;
        for (Concepto c : Concepto.todos()) if (c.tema.equals(tema) && (delTema == null || c.nivel < delTema.nivel)) delTema = c;
        if (concepto != null && Concepto.de(concepto) != null) elegir(Concepto.de(concepto));
        else if (delTema != null) elegir(delTema);
        else if (elegido == null && !Concepto.todos().isEmpty()) elegir(Concepto.todos().get(0));
    }

    /** Abre la consulta con un error del editor: lo explica paso a paso y muestra el concepto detrás. */
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

    /** Escribe una pregunta (como si la tecleara el jugador) y elige lo primero que encuentra. */
    public void preguntar(String texto) {
        pregunta.ponerTexto(texto);
        ultimaPregunta = texto;
        error = null;
        encontrados = Concepto.buscar(texto, T.idioma, 8);
        if (!encontrados.isEmpty()) elegir(encontrados.get(0));
    }

    /** Pide un paso del error que explica (0 pelea, 1 pseudocódigo, 2 ejemplo, 3 cómo arreglarlo). */
    public void pedirPasoDelError(int paso) {
        if (error != null && !pasosDelError.contains(paso)) pasosDelError.add(paso);
    }

    /** Dispara el próximo consejo de inmediato (útil para capturas y pruebas). */
    public void tipAhora() {
        planificador.tipAhora();
    }

    /** El consejo que está mostrando el cartelito ahora mismo (o null si está oculto). */
    public ConsejosDelTaller.Tip tipActual() {
        return planificador.tipActual();
    }

    /** Segundos que faltan para el próximo consejo automático. */
    public float esperaDelTip() {
        return planificador.esperaDelTip();
    }

    /** Cierra el cartelito manualmente («×» o «Gracias») aplicando enfriamiento largo y recogiendo si no estaba fijado. */
    public void cerrarTipManualmente() {
        planificador.cerrarManual();
        despTip.alPrincipio();
        if (!desplegadoFijo) asomada = 0f;
    }

    /** Avanza el temporizador de tips del taller (sobrecarga de compatibilidad para pruebas). */
    public boolean avanzarTemporizadorDeTip(String codigo, float dt) {
        boolean nuevo = planificador.avanzar(null, codigo, dt, false);
        if (nuevo) despTip.alPrincipio();
        return nuevo;
    }

    /** Avanza el temporizador de consejos en cualquier pantalla, adaptado al progreso del jugador. */
    public boolean avanzarTemporizadorDeTip(Jugador jugador, String codigo, float dt) {
        boolean nuevo = planificador.avanzar(jugador, codigo, dt, true);
        if (nuevo) despTip.alPrincipio();
        return nuevo;
    }

    /** Indica si el entrenador flotante aplica en esta pantalla (nunca sin partida ni en Inicio). */
    public static boolean debeMostrarse(Jugador jugador, Pantalla pantalla) {
        return jugador != null && !(pantalla instanceof Inicio);
    }

    /** Devuelve el entrenador activo del jugador y limpia cualquier estado o caché obsoleta al cambiar de entrenador. */
    public Entrenador sincronizarEntrenador(Jugador jugador) {
        Entrenador actual = Entrenador.de(jugador == null ? null : jugador.entrenador);
        if (ultimoEntrenadorId != null && !ultimoEntrenadorId.equals(actual.id)) {
            respuestaIa = null;
            peticion = null;
            hablando = 0f;
            planificador.alCambiarEntrenador();
            despTip.alPrincipio();
            if (abierto && jugador != null) cerebro = CerebroDelJuego.tutor(jugador);
        }
        ultimoEntrenadorId = actual.id;
        return actual;
    }

    /** Id del entrenador con el que está sincronizado actualmente el entrenador flotante. */
    public String entrenadorSincronizadoId() {
        return ultimoEntrenadorId;
    }

    /** Calcula la pestaña discreta recogida en la esquina inferior derecha con su flechita «◀». */
    public static CajaRecogida cajaRecogida(float pantallaAncho, float pantallaAlto, float tamanoLetra) {
        return GeometriaEntrenador.cajaRecogida(pantallaAncho, pantallaAlto, tamanoLetra);
    }

    /** Calcula la pestaña desplegada con la cabecita del entrenador en la esquina inferior derecha. */
    public static CajaPestana cajaPestana(float pantallaAncho, float pantallaAlto, float asomada, float tamanoLetra) {
        return GeometriaEntrenador.cajaPestana(pantallaAncho, pantallaAlto, asomada, tamanoLetra);
    }

    /** Calcula la caja del cartelito pegado a la cabecita desplegada con tamaño de letra estándar (18). */
    public static CajaTip cajaTip(float pantallaAncho, float pantallaAlto, float altoTexto) {
        return GeometriaEntrenador.cajaTip(pantallaAncho, pantallaAlto, altoTexto, 18f);
    }

    /** Calcula la caja del cartelito pegado a la cabecita desplegada para un tamaño de letra dado. */
    public static CajaTip cajaTip(float pantallaAncho, float pantallaAlto, float altoTexto, float tamanoLetra) {
        return GeometriaEntrenador.cajaTip(pantallaAncho, pantallaAlto, altoTexto, tamanoLetra);
    }

    /** Cierra la ventana completa de consulta. */
    public void cerrar() {
        abierto = false;
        pregunta.enfocado = false;
    }

    private void elegir(Concepto c) {
        elegido = c;
        nivel = Math.max(1, Math.min(Concepto.NIVEL_MAX, c.nivel));
        hablando = 2.5f;
        respuestaIa = null;
        peticion = null;
        despTexto.alPrincipio();
    }

    private void decir(Juego j, String texto) {
        hablando = Math.min(6, 0.6f + texto.length() * 0.045f);
        j.voz.decir(texto.replace("\n", " "), j.jugador.idioma, Entrenador.de(j.jugador.entrenador).timbre);
    }

    private static boolean hospital(Juego j) {
        return j.pantalla() != null && j.pantalla().entrenadorEnElHospital();
    }

    /** Al principio del fotograma: bloquea entradas si está abierta o si el ratón está encima; gestiona F2. */
    public void antes(Juego j) {
        if (!debeMostrarse(j.jugador, j.pantalla())) return;
        sincronizarEntrenador(j.jugador);
        VentanaDeCodigo.Senalado s = j.ui.errorSenaladoAntes;
        if (!abierto && s != null && s.pedido() && !hospital(j)) abrirConError(j, s);
        else if (j.entrada.tecla(Input.Keys.F2)) {
            if (abierto) {
                cerrar();
                if (j.voz != null) j.voz.detener();
            } else if (s != null && !hospital(j)) abrirConError(j, s);
            else abrir(j, null);
        }
        if (abierto || ratonEncima(j)) j.entrada.bloqueada = true;
    }

    private boolean ratonEncima(Juego j) {
        if (!debeMostrarse(j.jugador, j.pantalla()) || !j.jugador.entrenadorActivo) return false;
        Pincel p = j.pincel;
        float letra = j.ui == null ? 18f : j.ui.letra;
        if (!desplegado()) {
            return cajaRecogida(p.ancho, p.alto, letra).contiene(j.entrada.ratonX, j.entrada.ratonY);
        }
        CajaPestana actual = cajaPestana(p.ancho, p.alto, asomada, letra);
        if (actual.contiene(j.entrada.ratonX, j.entrada.ratonY)) return true;
        ConsejosDelTaller.Tip tip = planificador.tipActual();
        if (tip != null && !hospital(j)) {
            String texto = ConsejosDelJuego.textoDelTip(tip, j.jugador.idioma);
            CajaTip c = GeometriaEntrenador.cajaTip(p, j.ui, texto);
            return j.entrada.dentro(c.x, c.y, c.w, c.h);
        }
        return false;
    }

    /** Al final del fotograma: la esquina (recogida o desplegada con consejo) o la consulta completa. */
    public void dibujar(Juego j, float dt) {
        if (!debeMostrarse(j.jugador, j.pantalla())) return;
        j.entrada.bloqueada = false;
        this.dt = dt;
        t += dt;
        hablando = Math.max(0, hablando - dt);
        Entrenador en = sincronizarEntrenador(j.jugador);
        if (!abierto) {
            if (!j.jugador.entrenadorActivo) return;
            actualizarConsejoEspontaneo(j, dt);
            esquina(j, en);
            dibujarCartelConsejo(j);
            return;
        }
        Ui ui = j.ui;
        ui.reservarEscape();
        if (j.entrada.tecla(Input.Keys.ESCAPE)) {
            if (pregunta.enfocado) pregunta.enfocado = false;
            else {
                cerrar();
                if (j.voz != null) j.voz.detener();
                return;
            }
        }
        consulta(j, en);
    }

    /** Esquina inferior derecha: recogida con flechita «◀» o desplegada con cabecita completa y «▶». */
    private void esquina(Juego j, Entrenador en) {
        if (!desplegado()) {
            dibujarRecogida(j, en);
            return;
        }
        dibujarDesplegada(j, en);
    }

    private void dibujarRecogida(Juego j, Entrenador en) {
        Pincel p = j.pincel;
        Ui ui = j.ui;
        boolean enfermo = hospital(j);
        CajaRecogida r = cajaRecogida(p.ancho, p.alto, ui.letra);
        boolean encima = r.contiene(j.entrada.ratonX, j.entrada.ratonY);
        p.caja(r.x + 2, r.y + 3, r.w, r.h, 12, 0x000000, 0.28f);
        p.caja(r.x, r.y, r.w, r.h, 12, enfermo ? 0xE8ECEF : (encima ? ui.tema.panelAlto : ui.tema.panel), 0.94f);
        p.recortar(r.x + 1f, r.y + 1f, r.w - 2f, r.h - 1f);
        CabezaDeEntrenador.dibujar(p, en, r.cabezaX, r.cabezaY, r.radio, false, t);
        if (enfermo) {
            p.rect(r.cabezaX - r.radio, r.cabezaY - r.radio * 0.55f, r.radio * 2, r.radio * 0.3f, 0xFFFFFF, 0.95f);
        }
        p.sinRecorte();
        p.bordeRedondeado(r.x, r.y, r.w, r.h, 12, enfermo ? 0xFF4D4D : ui.tema.acento, encima ? 1f : 0.85f, 1.8f);
        float fx = r.flechaX;
        float fy = r.flechaY;
        float s = 5.5f;
        p.triangulo(fx - s, fy, fx + s * 0.7f, fy - s, fx + s * 0.7f, fy + s, enfermo ? 0xFF4D4D : ui.tema.acento, 1f);
        p.circulo(r.ayudaX + 1.5f, r.ayudaY + 2f, r.ayudaRadio, 0x000000, 0.34f);
        p.circulo(r.ayudaX, r.ayudaY, r.ayudaRadio, enfermo ? 0xFFFFFF : (encima ? 0xFFE066 : 0xFFD23F), 1f);
        p.aro(r.ayudaX, r.ayudaY, r.ayudaRadio, 1.8f, enfermo ? 0xFF4D4D : 0x1A1F2B, 0.95f);
        float tamAyuda = Math.max(12f, r.ayudaRadio * 1.35f);
        p.textoCentrado("?", r.ayudaX, r.ayudaY - tamAyuda * 0.62f, tamAyuda, true, enfermo ? 0xCC1E1E : 0x141820, 1f);
        if (encima) {
            ui.avisar(en.nombre.en(j.jugador.idioma) + " — " + T.t("entrenador.desplegar"));
        }
        boolean clicEnAyuda = !j.entrada.bloqueada && j.entrada.recienPulsado && !j.entrada.clicUsado
                && r.enAyuda(j.entrada.ratonX, j.entrada.ratonY);
        if (clicEnAyuda) j.entrada.clicUsado = true;
        if (j.entrada.clic(r.x, r.y, r.w, r.h) || clicEnAyuda) {
            if (ui.sonidoDeClic != null) ui.sonidoDeClic.run();
            desplegar();
        }
    }

    private void dibujarDesplegada(Juego j, Entrenador en) {
        Pincel p = j.pincel;
        Ui ui = j.ui;
        boolean enfermo = hospital(j);
        boolean conConsejo = planificador.tipActual() != null;
        CajaPestana actual = cajaPestana(p.ancho, p.alto, conConsejo ? 1f : asomada, ui.letra);
        boolean encima = actual.contiene(j.entrada.ratonX, j.entrada.ratonY);
        if (conConsejo) asomada = 1f;
        else asomada += (encima ? 1 - asomada : -asomada) * Math.min(1, dt * 12);
        CajaPestana c = cajaPestana(p.ancho, p.alto, asomada, ui.letra);
        p.caja(c.x + 2, c.y + 3, c.w, c.h, 14, 0x000000, 0.28f);
        p.caja(c.x, c.y, c.w, c.h, 14, enfermo ? 0xE8ECEF : ui.tema.panelAlto, 0.94f);
        p.bordeRedondeado(c.x, c.y, c.w, c.h, 14, enfermo ? 0xFF4D4D : ui.tema.acento, 0.95f, 1.8f);
        CabezaDeEntrenador.dibujar(p, en, c.cabezaX, c.cabezaY, c.radio, hablando > 0, t);
        if (enfermo) {
            p.rect(c.cabezaX - c.radio, c.cabezaY - c.radio * 0.55f, c.radio * 2, c.radio * 0.3f, 0xFFFFFF, 0.95f);
            p.textoCentrado("+", c.insigniaX, c.insigniaY, 18, true, 0xFF4D4D, 1);
        } else {
            p.textoCentrado("?", c.insigniaX, c.insigniaY, 16, true, 0xFFD23F, 1);
        }
        boolean enRecoger = c.enRecoger(j.entrada.ratonX, j.entrada.ratonY);
        p.linea(c.recogerX, c.y + 8, c.recogerX, c.y + c.h - 8, 1f, ui.tema.textoApagado, 0.35f);
        float rx = c.recogerX + c.recogerW * 0.50f;
        float ry = c.recogerY + c.recogerH * 0.50f;
        p.triangulo(rx + 5f, ry, rx - 4f, ry - 6f, rx - 4f, ry + 6f, enRecoger ? ui.tema.acento : ui.tema.textoSuave, 1f);
        if (enRecoger) {
            ui.avisar(T.t("entrenador.recoger"));
        } else if (encima) {
            ui.avisar(en.nombre.en(j.jugador.idioma) + " — " + (enfermo ? T.t("entrenador.hospital.ayuda") : T.t("entrenador.pestana")));
        }
        if (j.entrada.clic(c.recogerX, c.recogerY, c.recogerW, c.recogerH)) {
            if (ui.sonidoDeClic != null) ui.sonidoDeClic.run();
            recoger();
            if (planificador.tipActual() != null) {
                cerrarTipManualmente();
                if (j.voz != null) j.voz.detener();
            }
            return;
        }
        if (j.entrada.clic(c.x, c.y, c.w - c.recogerW, c.h)) abrir(j, null);
    }

    // ================================================================ consejos espontáneos

    private void actualizarConsejoEspontaneo(Juego j, float dt) {
        if (hospital(j)) return;
        boolean habiaTip = planificador.tipActual() != null;
        String codigo = j.pantalla() == null ? null : j.pantalla().codigoParaConsejos();
        if (avanzarTemporizadorDeTip(j.jugador, codigo, dt)) {
            ConsejosDelTaller.Tip nuevo = planificador.tipActual();
            if (nuevo != null) {
                asomada = 1f;
                decir(j, ConsejosDelJuego.textoDelTip(nuevo, j.jugador.idioma));
            }
        } else if (habiaTip && planificador.tipActual() == null && !desplegadoFijo) {
            asomada = 0f;
        }
    }

    private void dibujarCartelConsejo(Juego j) {
        ConsejosDelTaller.Tip tip = planificador.tipActual();
        if (tip == null || hospital(j)) return;
        Pincel p = j.pincel;
        Ui ui = j.ui;
        String texto = ConsejosDelJuego.textoDelTip(tip, j.jugador.idioma);
        CajaTip c = GeometriaEntrenador.cajaTip(p, ui, texto);
        CajaPestana pestana = cajaPestana(p.ancho, p.alto, asomada, ui.letra);
        float tl = GeometriaEntrenador.tamanoLetraTip(p, ui, texto, c.anchoTexto);
        float aparece = Math.min(1, planificador.tiempoDelTip() * 4);
        p.caja(c.x + 3, c.y + 4, c.w, c.h, 12, 0x000000, 0.3f * aparece);
        p.caja(c.x, c.y, c.w, c.h, 12, 0xF5F3EA, 0.97f * aparece);
        float picoY = Math.max(c.y + 22f, Math.min(c.y + c.h - 22f, pestana.cabezaY));
        p.triangulo(c.x + c.w, picoY - 10, c.x + c.w, picoY + 10, c.x + c.w + 12, picoY, 0xF5F3EA, 0.97f * aparece);
        float altoTextoDisp = Math.max(BOTON_CERRAR_TAM, c.h - GeometriaEntrenador.RESERVA_VERTICAL_TIP);
        float altoParrafo = p.altoDeParrafo(texto, c.anchoTexto, tl);
        if (altoParrafo > altoTextoDisp) {
            float anchoDesp = Math.max(60f, c.anchoTexto - 14f);
            float d = ui.desplazable(despTip, c.x + MARGEN_TEXTO_IZQ, c.y + 10, c.anchoTexto, altoTextoDisp);
            float altoReal = p.parrafo(texto, c.x + MARGEN_TEXTO_IZQ, c.y + 10 - d, anchoDesp, tl, 0x1E1E1E, aparece);
            ui.finDesplazable(despTip, altoReal + 4f);
        } else {
            p.parrafo(texto, c.x + MARGEN_TEXTO_IZQ, c.y + 10, c.anchoTexto, tl, 0x1E1E1E, aparece);
        }
        if (botonCerrarTip(j, c, aparece)) {
            cerrarTipManualmente();
            if (j.voz != null) j.voz.detener();
            return;
        }
        botonesDelCartel(j, c, tip);
    }

    private void botonesDelCartel(Juego j, CajaTip c, ConsejosDelTaller.Tip tip) {
        Ui ui = j.ui;
        float by = c.y + c.h - 50;
        float bw = (c.w - 32) / 3;
        if (ui.boton(T.t("entrenador.tip.explica"), c.x + 8, by, bw, 40, Ui.Estilo.PLANO)) {
            String concepto = tip.concepto();
            planificador.cerrarNormal();
            despTip.alPrincipio();
            abrir(j, concepto);
            return;
        }
        if (ui.boton(T.t("entrenador.tip.otro"), c.x + 16 + bw, by, bw, 40, Ui.Estilo.PLANO)) {
            String codigo = j.pantalla() == null ? null : j.pantalla().codigoParaConsejos();
            ConsejosDelTaller.Tip otro = planificador.siguiente(j.jugador, codigo);
            despTip.alPrincipio();
            if (otro != null) decir(j, ConsejosDelJuego.textoDelTip(otro, j.jugador.idioma));
            return;
        }
        if (ui.boton(T.t("entrenador.tip.cerrar"), c.x + 24 + 2 * bw, by, bw, 40, Ui.Estilo.PLANO)) {
            cerrarTipManualmente();
            if (j.voz != null) j.voz.detener();
        }
    }

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

    // ================================================================ la consulta

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
            if (j.voz != null) j.voz.detener();
            return;
        }
        if (hospital(j)) {
            Hospital.dibujarEnConsulta(j, en, x, y, w, h, t);
            return;
        }
        float lw = Math.min(420, w * 0.34f);
        String quitar = j.jugador.entrenadorActivo ? T.t("entrenador.desactivar") : T.t("ajustes.entrenador");
        float qw = Math.min(Math.max(210f, w - lw - 110f), Math.min(340f, p.ancho(quitar, ui.letra * 0.78f, false) + 34f));
        float qx = x + w - 70 - qw;
        float anchoTitulo = Math.max(120f, Math.min(qx - (x + 112f), lw - 104f));
        CabezaDeEntrenador.dibujar(p, en, x + 56, y + 62, 34, hablando > 0, t);
        String titulo = T.f("entrenador.titulo", en.nombre.en(idioma));
        p.texto(ui.recortarTexto(titulo, anchoTitulo, ui.letra * 1.05f, true), x + 104, y + 28, ui.letra * 1.05f, true, ui.tema.texto, 1);
        p.texto(ui.recortarTexto(en.papel.en(idioma), anchoTitulo, ui.letra * 0.75f, false), x + 104, y + 28 + ui.letra * 1.45f,
                ui.letra * 0.75f, false, ui.tema.textoSuave, 1);
        if (ui.boton(quitar, qx, y + 12, qw, 42, Ui.Estilo.PLANO)) {
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
            preguntarALaIa(j, elegido, idioma, en);
        }
        float altoAyuda = ui.letra * 1.1f + 68;
        lista(j, x + 16, fy + 58, lw - 16, y + h - fy - 58 - altoAyuda, idioma);
        barraDeAyuda(j, x + 16, y + h - 58, lw - 16);
        explicacion(j, x + lw + 20, y + 70, w - lw - 40, h - 86, idioma, en);
    }

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
            float ocupado = dibujarPestanasNiveles(ui, x, y, w);
            y += ocupado;
            h -= ocupado;
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

    /** Dibuja las 6 pestañas (programación 1-5 + El juego 6) en dos filas de 3 para que nunca se recorten. */
    private float dibujarPestanasNiveles(Ui ui, float x, float y, float w) {
        String[] niveles = T.t("entrenador.niveles").split(";");
        int corte = Math.min(3, niveles.length);
        String[] fila1 = Arrays.copyOfRange(niveles, 0, corte);
        int sel1 = nivel <= corte ? nivel - 1 : -1;
        int e1 = ui.pestanas(fila1, sel1, x, y, w, 36);
        if (e1 >= 0 && e1 != sel1) {
            nivel = e1 + 1;
            despLista.alPrincipio();
        }
        if (niveles.length <= corte) return 44f;
        String[] fila2 = Arrays.copyOfRange(niveles, corte, niveles.length);
        int sel2 = nivel > corte ? nivel - corte - 1 : -1;
        int e2 = ui.pestanas(fila2, sel2, x, y + 40, w, 36);
        if (e2 >= 0 && e2 != sel2) {
            nivel = corte + e2 + 1;
            despLista.alPrincipio();
        }
        return 84f;
    }

    private void explicacion(Juego j, float x, float y, float w, float h, Idioma idioma, Entrenador en) {
        Ui ui = j.ui;
        Pincel p = j.pincel;
        Concepto c = elegido;
        if (c == null) return;
        float abajo = 56;
        float d = ui.desplazable(despTexto, x, y, w, h - abajo);
        float yy = y - d;
        if (error != null) {
            yy = PanelErrorConsulta.explicarError(j, error, pasosDelError, c, x, yy, w, idioma, texto -> decir(j, texto)) + ui.letra;
        }
        p.texto(c.titulo.en(idioma), x, yy, ui.letra * 1.5f, true, ui.tema.acento, 1);
        yy += ui.letra * 2.2f;
        for (String par : c.parrafos(idioma)) yy += p.parrafo(par, x, yy, w - 20, ui.letra, ui.tema.texto, 1) + ui.letra * 0.6f;
        yy = PanelErrorConsulta.bloqueDeCodigo(j, T.t("entrenador.pseudo"), c.pseudo(idioma), false, x, yy, w);
        yy = PanelErrorConsulta.bloqueDeCodigo(j, T.t("entrenador.ejemplo"), c.ejemplo(idioma), true, x, yy, w);
        yy = respuestaDeLaIa(j, x, yy, w);
        ui.finDesplazable(despTexto, yy + d - y);
        botones(j, c, x, y + h - abajo + 8, w, idioma, en);
    }

    // ================================================================ la IA

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
        int fijos = 2;
        if (c.guia > 0) {
            textos.add(T.t("guia.abrir"));
            fijos = 3;
        }
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
        Arrays.fill(estilos, Ui.Estilo.PLANO);
        Arrays.fill(activos, true);
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

    private void preguntarALaIa(Juego j, Concepto c, Idioma idioma, Entrenador en) {
        peticion = ConsultaIaEntrenador.lanzar(cerebro, j, c, error, ultimaPregunta, idioma, en);
        respuestaIa = null;
    }

    /** El código con su número delante de cada línea (como mucho «max» líneas). */
    public static String conNumeros(String codigo, int max) {
        return ConsultaIaEntrenador.conNumeros(codigo, max);
    }

    /** El contexto de una pantalla con editor: qué es, el código numerado y los errores marcados. */
    public static String contextoDeCodigo(String que, VentanaDeCodigo editor) {
        return ConsultaIaEntrenador.contextoDeCodigo(que, editor);
    }
}
