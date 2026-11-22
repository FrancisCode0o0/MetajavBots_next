package metajavbots.pantallas;

import java.util.List;

import metajavbots.ejercicios.Ejercicio;
import metajavbots.pantalla.Pincel;
import metajavbots.pantalla.Ui;
import metajavbots.textos.Idioma;
import metajavbots.textos.T;

/**
 * PREVISUALIZACIÓN DE EJERCICIO — Escenario en estado de espera antes de pulsar
 * «Comprobar» (y ante error de compilación). Muestra el mismo fondo temático y
 * el mismo tipo de escena que luego animará {@link AnimacionDelCaso}, con el
 * robotito vivo esperando (respiración y parpadeo) para pasar sin cortes a la
 * ejecución real.
 */
public final class PrevisualizacionDeEjercicio {

    /** Estado visual del panel antes de tener una ejecución válida. */
    public enum Estado {
        /** Aún no se ha comprobado (o se ha reiniciado). */
        ESPERA,
        /** El comprobador está compilando y ejecutando el código. */
        COMPILANDO,
        /** El código tiene errores de compilación y el robotito no puede arrancar. */
        ERROR_COMPILACION
    }

    /** Descripción determinista del escenario de previsualización de un ejercicio. */
    public record Escenario(String escenaId, String formaId, String llamadaMuestra, String esperadoMuestra) {
    }

    private static final int CAJA = 0xC98B4A;
    private static final int AZUL = 0x3FA9F5;
    private static final int APAGADO = 0x6E8FAF;
    private static final int OJO_ESPERA = 0x5BD6FF;
    private static final int OJO_COMPILANDO = 0xFFD23F;
    private static final int VISERA = 0x111318;

    private PrevisualizacionDeEjercicio() {
    }

    /** Resuelve de forma determinista el escenario inicial de un ejercicio en español. */
    public static Escenario de(Ejercicio e) {
        return de(e, Idioma.ES);
    }

    /** Resuelve de forma determinista el escenario inicial de un ejercicio en el idioma indicado. */
    public static Escenario de(Ejercicio e, Idioma idioma) {
        if (e == null) {
            return new Escenario(EscenasDelRobotito.Escena.CLASIFICAR.name(), AnimacionDelCaso.Forma.PIZARRA.name(), "", "");
        }
        EscenasDelRobotito.Escena escena = escenaDe(e);
        List<Ejercicio.Caso> casos = e.casos(idioma == null ? Idioma.ES : idioma);
        String llamada = casos.isEmpty() ? "" : casos.get(0).llamada;
        String esperado = casos.isEmpty() ? "" : casos.get(0).esperado;
        AnimacionDelCaso.Forma forma = AnimacionDelCaso.forma(e, esperado);
        return new Escenario(escena.name(), forma.name(), llamada, esperado);
    }

    /** Escena temática correspondiente al ejercicio (idéntica a la de AnimacionDelCaso). */
    static EscenasDelRobotito.Escena escenaDe(Ejercicio e) {
        return EscenasDelRobotito.de(e.tema, e.enunciado.es + " " + e.titulo.es);
    }

    /** Dibuja el escenario en espera, compilando o detenido por error de compilación. */
    public static void dibujar(Ui ui, float x, float y, float w, float h, Ejercicio e, Estado estado, float reloj) {
        if (ui == null || ui.p == null || e == null || w < 40 || h < 60) return;
        Pincel p = ui.p;
        EscenasDelRobotito.Escena escena = escenaDe(e);
        List<Ejercicio.Caso> casos = e.casos(T.idioma);
        String esperado = casos.isEmpty() ? "" : casos.get(0).esperado;
        AnimacionDelCaso.Forma forma = AnimacionDelCaso.forma(e, esperado);
        p.caja(x, y, w, h, 10, ui.tema.fondo, 1);
        p.recortar(x, y, w, h);
        EscenasDelRobotito.fondo(p, escena, x, y, w, h, reloj);
        float suelo = y + h - 26;
        switch (forma) {
            case GIRO:
                esperaGiro(p, x, y + 32, w, h - 32, AnimacionDelCaso.numero(esperado), estado, reloj);
                break;
            case AVANCE:
                esperaAvance(p, x, suelo, w, AnimacionDelCaso.numero(esperado), estado, reloj);
                break;
            case CAJAS:
                esperaCajas(p, escena, x, suelo, w, AnimacionDelCaso.numero(esperado), estado, reloj);
                break;
            case PUERTAS:
                esperaPuertas(ui, x, suelo, w, h - 32, estado, reloj);
                break;
            case ESTANTERIA:
                esperaEstanteria(ui, x, suelo, w, AnimacionDelCaso.elementos(AnimacionDelCaso.dentro(esperado)), estado, reloj);
                break;
            case PAPEL:
                esperaEscribir(ui, x, y + 30, w, h - 30, true, estado, reloj);
                break;
            case ALARMA:
                esperaAlarma(p, x, suelo, w, h - 30, estado, reloj);
                break;
            default:
                esperaEscribir(ui, x, y + 30, w, h - 30, false, estado, reloj);
                break;
        }
        p.sinRecorte();
        dibujarCabecera(ui, x, y, w, h, estado);
    }

    /** Cabecera superior con etiqueta discreta y leyenda de estado sin solapar en letra 18 ni 26. */
    private static void dibujarCabecera(Ui ui, float x, float y, float w, float h, Estado estado) {
        Pincel p = ui.p;
        float tCab = Math.min(ui.letra * 0.78f, Math.max(15, h * 0.11f));
        String leyenda;
        int color;
        if (estado == Estado.ERROR_COMPILACION) {
            leyenda = "✘ " + T.t("animacion.error_compilacion");
            color = AnimacionDelCaso.ROJO;
        } else if (estado == Estado.COMPILANDO) {
            leyenda = "… " + T.t("ejercicio.comprobando");
            color = OJO_COMPILANDO;
        } else {
            leyenda = "▶ " + T.t("animacion.espera");
            color = ui.tema.textoSuave;
        }
        String etiqueta = T.t("animacion.vista_previa");
        float anchoEtq = p.ancho(etiqueta, tCab, true);
        float anchoLey = p.ancho(leyenda, tCab, false);
        if (anchoEtq + anchoLey + 40 <= w) {
            p.texto(etiqueta, x + 12, y + 7, tCab, true, ui.tema.texto, 0.9f);
            p.textoDerecha(leyenda, x + w - 12, y + 7, tCab, estado == Estado.ERROR_COMPILACION, color, 1);
        } else {
            p.texto(ui.recortarTexto(leyenda, w - 24, tCab, estado == Estado.ERROR_COMPILACION), x + 12, y + 7, tCab,
                    estado == Estado.ERROR_COMPILACION, color, 1);
        }
    }

    /** Robotito en postura viva de espera (respiración suave y parpadeo) o bloqueado por error de compilación. */
    private static void robotEnEspera(Pincel p, float x, float suelo, Estado estado, float reloj) {
        if (estado == Estado.ERROR_COMPILACION) {
            float temblor = (float) Math.sin(reloj * 18) * 1.2f;
            AnimacionDelCaso.robotito(p, x + temblor, suelo + 3, 0.05f, AnimacionDelCaso.ROJO, APAGADO);
            p.circulo(x + 28, suelo - 92, 11, AnimacionDelCaso.ROJO, 0.92f);
            p.textoCentrado("!", x + 28, suelo - 101, 16, true, 0xFFFFFF, 1);
            return;
        }
        float resp = (float) Math.sin(reloj * 2.6f) * 2.2f;
        float brazo = estado == Estado.COMPILANDO
                ? 0.45f + 0.35f * (float) Math.abs(Math.sin(reloj * 6))
                : 0.28f + 0.16f * (float) Math.sin(reloj * 2.1f);
        boolean parpadeo = (reloj % 3.6f) > 3.46f;
        int ojo = parpadeo ? VISERA : (estado == Estado.COMPILANDO ? OJO_COMPILANDO : OJO_ESPERA);
        AnimacionDelCaso.robotito(p, x, suelo - Math.max(0, resp), brazo, ojo, AZUL);
    }

    /** Pizarra verde o hoja de papel en blanco preparadas, con el robotito listo junto a ellas. */
    private static void esperaEscribir(Ui ui, float x, float y, float w, float h, boolean papel, Estado estado, float reloj) {
        Pincel p = ui.p;
        float suelo = y + h - 26;
        float bx = x + Math.min(148, w * 0.24f);
        float by = y + 14;
        float bh = Math.max(46, h - 54);
        if (papel) {
            float bw = Math.max(80, (w - (bx - x) - 36) * 0.62f);
            p.caja(bx, by, bw, bh, 4, 0xF2EEE3, 1);
            for (float ly = by + 18; ly < by + bh - 4; ly += 18) p.rect(bx + 6, ly, bw - 12, 1, 0xB9C6D6, 0.5f);
            float derW = Math.max(40, x + w - (bx + bw + 26));
            p.caja(bx + bw + 14, by, derW, bh, 4, 0xC9C5BA, 0.9f);
            if (((int) (reloj * 2) % 2) == 0 && estado != Estado.ERROR_COMPILACION) {
                p.rect(bx + 12, by + 10, 2, 14, 0x1E2A4A, 0.85f);
            }
        } else {
            float bw = Math.max(90, x + w - bx - 18);
            p.caja(bx, by, bw, bh, 6, 0x1F3A2E, 1);
            p.bordeRedondeado(bx, by, bw, bh, 6, 0x8C6A43, 1, 4);
            if (((int) (reloj * 2) % 2) == 0 && estado != Estado.ERROR_COMPILACION) {
                p.rect(bx + 14, by + 16, 10, 2, 0xF5F5F0, 0.8f);
            }
        }
        robotEnEspera(p, x + Math.min(72, w * 0.12f), suelo, estado, reloj);
    }

    /** Montón de cajas a la izquierda y ranuras vacías translúcidas a la derecha esperando a rellenarse. */
    private static void esperaCajas(Pincel p, EscenasDelRobotito.Escena escena, float x, float suelo, float w, long esperado,
            Estado estado, float reloj) {
        long valor = AccionesDelRobotito.valorDeCaja(esperado, 0);
        int nEsp = Math.max(3, Math.min(8, (int) (Math.abs(esperado) / valor)));
        float lado = Math.max(16, Math.min(26, (w - 200) / 10));
        float monton = x + 44;
        float x0 = x + Math.min(176, w * 0.32f);
        int porFila = Math.max(1, Math.min(6, (int) ((x + w - x0 - 20) / (lado + 4))));
        for (int k = 0; k < 3; k++) {
            EscenasDelRobotito.objeto(p, escena, monton - lado / 2 + (k % 2) * 3, suelo - (k + 1) * (lado + 1), lado, CAJA, CAJA, 1);
        }
        for (int k = 0; k < nEsp; k++) {
            float hx = x0 + (k % porFila) * (lado + 4);
            float hy = suelo - (k / porFila + 1) * (lado + 4);
            EscenasDelRobotito.objeto(p, escena, hx, hy, lado, 0x8A919C, CAJA, 0.28f);
        }
        robotEnEspera(p, monton + 56, suelo, estado, reloj);
    }

    /** Estantería con ranuras vacías numeradas esperando a ser llenadas u ordenadas. */
    private static void esperaEstanteria(Ui ui, float x, float suelo, float w, List<String> esperado, Estado estado, float reloj) {
        Pincel p = ui.p;
        float ey = suelo - 86;
        float cajon = x + 46;
        float izq = x + Math.min(134, w * 0.24f);
        p.rect(izq, ey, Math.max(80, x + w - izq - 18), 6, 0x6B4F33, 1);
        p.rect(izq + 4, ey + 6, 6, suelo - ey - 6, 0x5A4129, 1);
        p.rect(x + w - 26, ey + 6, 6, suelo - ey - 6, 0x5A4129, 1);
        p.caja(cajon - 28, suelo - 32, 56, 32, 4, 0x8C6A43, 1);
        int n = Math.max(3, Math.min(6, esperado.isEmpty() ? 4 : esperado.size()));
        float hueco = Math.min(96, Math.max(34, (x + w - izq - 36) / n));
        float t = Math.min(ui.letra * 0.68f, 15);
        for (int k = 0; k < n; k++) {
            float cx = izq + 14 + k * hueco;
            p.caja(cx, ey - 38, hueco - 8, 36, 5, 0x8A919C, 0.24f);
            p.textoCentrado("[" + k + "]", cx + (hueco - 8) / 2, ey - 28, t, false, ui.tema.textoApagado, 0.9f);
        }
        robotEnEspera(p, cajon + 44, suelo, estado, reloj);
    }

    /** Dos puertas true/false cerradas con el robotito frente a ellas esperando decisión. */
    private static void esperaPuertas(Ui ui, float x, float suelo, float w, float h, Estado estado, float reloj) {
        Pincel p = ui.p;
        float pw = Math.min(86, w * 0.18f);
        float ph = Math.max(54, Math.min(108, h - 66));
        float[] px = {x + w * 0.44f, x + w * 0.74f};
        String[] nombre = {"true", "false"};
        float t = Math.min(ui.letra * 0.85f, 18);
        for (int i = 0; i < 2; i++) {
            float dx = px[i] - pw / 2;
            float dy = suelo - ph;
            p.caja(dx, dy, pw, ph, 6, 0x8C6A43, 1);
            p.circulo(dx + pw - 12, dy + ph / 2, 4, 0xE8C547, 1);
            p.textoCentrado(nombre[i], px[i], dy - t * 1.25f, t, true, 0xFFF1C1, 1);
        }
        robotEnEspera(p, x + Math.min(76, w * 0.16f), suelo, estado, reloj);
    }

    /** Palanca de alarma en reposo y sirena apagada, con el robotito esperando junto a ella. */
    private static void esperaAlarma(Pincel p, float x, float suelo, float w, float h, Estado estado, float reloj) {
        float lx = x + w * 0.46f;
        float ly = suelo - 68;
        float sirena = x + w * 0.76f;
        p.caja(sirena - 22, Math.max(suelo - h + 46, suelo - 130), 44, 28, 8, 0x7A2A28, 1);
        p.caja(lx - 16, ly - 10, 32, 44, 4, 0x4A5260, 1);
        float giro = -0.6f;
        p.linea(lx, ly + 12, lx + (float) Math.sin(giro) * 34, ly + 12 - (float) Math.cos(giro) * 34, 5, 0xB8C0C8, 1);
        p.circulo(lx + (float) Math.sin(giro) * 34, ly + 12 - (float) Math.cos(giro) * 34, 7, AnimacionDelCaso.ROJO, 1);
        robotEnEspera(p, lx - 68, suelo, estado, reloj);
    }

    /** Arena circular con la torreta en 0° esperando la orden de giro. */
    private static void esperaGiro(Pincel p, float x, float y, float w, float h, long esperado, Estado estado, float reloj) {
        float r = Math.max(36, Math.min(w * 0.36f, h * 0.34f));
        float cx = x + w / 2;
        float cy = y + h * 0.5f;
        p.circulo(cx, cy, r + 5, 0x3A3F46, 1);
        p.circulo(cx, cy, r, 0x5B6470, 1);
        double ae = Math.toRadians(esperado);
        float mx = cx + (float) Math.cos(ae) * r * 0.75f;
        float my = cy - (float) Math.sin(ae) * r * 0.75f;
        p.circulo(mx, my, 14, 0xFFFFFF, 0.9f);
        p.circulo(mx, my, 9, AnimacionDelCaso.ROJO, 0.9f);
        p.circulo(mx, my, 4, 0xFFFFFF, 0.9f);
        double ag = estado == Estado.ERROR_COMPILACION ? 0 : Math.toRadians(Math.sin(reloj * 1.6f) * 6);
        p.circulo(cx, cy, 22, estado == Estado.ERROR_COMPILACION ? APAGADO : 0x2F80ED, 1);
        p.linea(cx, cy, cx + (float) Math.cos(ag) * 38, cy - (float) Math.sin(ag) * 38, 8, 0xBFD7E3, 1);
        p.circulo(cx, cy, 10, 0x10161C, 1);
    }

    /** Pista lateral con el robotito en la línea de salida y la bandera esperando. */
    private static void esperaAvance(Pincel p, float x, float suelo, float w, long esperado, Estado estado, float reloj) {
        p.rect(x + 12, suelo, w - 24, 4, 0x8A9199, 1);
        float x0 = x + w * 0.35f;
        float bx = x + w * (esperado >= 0 ? 0.72f : 0.18f);
        p.rect(x0 - 2, suelo - 10, 4, 14, 0xFFFFFF, 0.85f);
        p.rect(bx - 1, suelo - 54, 3, 54, 0xE8ECEF, 1);
        float ondea = (float) Math.sin(reloj * 4) * 3;
        p.triangulo(bx + 2, suelo - 54, bx + 2, suelo - 36, bx + 28 + ondea, suelo - 45, AnimacionDelCaso.ROJO, 1);
        robotEnEspera(p, x0, suelo, estado, reloj);
    }
}
