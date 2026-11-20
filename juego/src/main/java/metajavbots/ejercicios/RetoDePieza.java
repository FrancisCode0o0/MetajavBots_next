package metajavbots.ejercicios;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import metajavbots.combate.Armas;
import metajavbots.combate.Cuerpo;
import metajavbots.piezas.Pieza;
import metajavbots.textos.Idioma;

/**
 * RETO DE PIEZA — Genera un reto corto de programación (en es, de y en) ligado
 * a la mecánica de cada pieza comprada en la Tienda, y define los parámetros
 * básicos que el jugador puede ajustar dentro de límites anti-abuso.
 */
public final class RetoDePieza {

    /** Parámetro básico personalizable de una pieza con su rango seguro. */
    public static final class Parametro {
        public final String clave;
        public final double base, min, max, paso;

        public Parametro(String clave, double base, double min, double max, double paso) {
            this.clave = clave;
            this.base = redondear(base);
            this.min = redondear(Math.min(min, max));
            this.max = redondear(Math.max(min, max));
            this.paso = paso;
        }

        /** Acota cualquier valor al rango permitido [min, max]; rechaza NaN e infinitos. */
        public double acotar(double valor) {
            if (!Double.isFinite(valor)) return base;
            return redondear(Math.max(min, Math.min(max, valor)));
        }
    }

    private RetoDePieza() {
    }

    /** Parámetros básicos ajustables para esta pieza, ya acotados por equilibrio. */
    public static List<Parametro> parametros(Pieza p) {
        List<Parametro> l = new ArrayList<>();
        if (p == null) return l;
        switch (p.ranura) {
            case BRAZO:
                parametrosDeBrazo(p, l);
                break;
            case CABEZA:
                anadir(l, "radar", p.valor("radar", 500), 150, 1100, 0.12, 20);
                anadir(l, "cono", p.valor("cono", 60), Armas.MIN_CONO, Armas.MAX_CONO, 0.15, 5);
                break;
            case PIERNAS:
                anadir(l, "velocidad", p.valor("velocidad", 5), Cuerpo.MIN_VELOCIDAD, Cuerpo.MAX_VELOCIDAD, 0.12, 0.25);
                anadir(l, "giro", p.valor("giro", 6), Cuerpo.MIN_GIRO, Cuerpo.MAX_GIRO, 0.12, 0.5);
                break;
            case NUCLEO:
                anadir(l, "recarga", p.valor("recarga", 0.45), Cuerpo.MIN_RECARGA, Cuerpo.MAX_RECARGA, 0.15, 0.05);
                anadir(l, "energiaMax", p.valor("energiaMax", 100), Cuerpo.MIN_ENERGIA_MAX, Cuerpo.MAX_ENERGIA_MAX, 0.12, 5);
                break;
            case NATURALEZA:
                anadir(l, "refuerzo", p.valor("refuerzo", 0.06), 0.01, 0.25, 0.15, 0.01);
                break;
            default:
                break;
        }
        return l;
    }

    private static void parametrosDeBrazo(Pieza p, List<Parametro> l) {
        switch (p.arma()) {
            case ESCUDO:
                anadir(l, "bloqueo", p.valor("bloqueo", 0.6), Armas.MIN_BLOQUEO, Armas.MAX_BLOQUEO, 0.12, 0.05);
                anadir(l, "duracion", p.valor("duracion", 45), Armas.MIN_DURACION, Armas.MAX_DURACION, 0.15, 5);
                break;
            case REPARAR:
                anadir(l, "cura", p.valor("cura", 12), Armas.MIN_CURA, Armas.MAX_CURA, 0.15, 1);
                anadir(l, "enfriamiento", p.valor("enfriamiento", 150), Cuerpo.MIN_ENFRIAMIENTO, Cuerpo.MAX_ENFRIAMIENTO, 0.15, 5);
                break;
            case LLAMA:
                anadir(l, "dano", p.valor("dano", 1.1), 0.5, Armas.MAX_DANO, 0.15, 0.1);
                anadir(l, "duracion", p.valor("duracion", 14), Armas.MIN_DURACION, Armas.MAX_DURACION, 0.15, 1);
                break;
            default:
                anadir(l, "dano", p.valor("dano", 5), 0.5, Armas.MAX_DANO, 0.15, 0.5);
                anadir(l, "enfriamiento", p.valor("enfriamiento", 20), Cuerpo.MIN_ENFRIAMIENTO, Cuerpo.MAX_ENFRIAMIENTO, 0.15, 1);
                break;
        }
    }

    private static void anadir(List<Parametro> l, String clave, double base, double limMin, double limMax, double margen, double paso) {
        double segura = Math.max(limMin, Math.min(limMax, Double.isFinite(base) ? base : limMin));
        double min = Math.max(limMin, segura * (1.0 - margen));
        double max = Math.min(limMax, segura * (1.0 + margen));
        if (paso >= 1) {
            min = Math.max(limMin, Math.floor(min));
            max = Math.min(limMax, Math.ceil(max));
        }
        l.add(new Parametro(clave, segura, min, max, paso));
    }

    /** Filtra y acota los ajustes propuestos por el jugador a los límites anti-abuso de esta pieza. */
    public static Map<String, Double> acotarAjustes(Pieza p, Map<String, Double> propuestos) {
        Map<String, Double> resultado = new LinkedHashMap<>();
        if (p == null || propuestos == null || propuestos.isEmpty()) return resultado;
        Map<String, Parametro> permitidos = new LinkedHashMap<>();
        for (Parametro par : parametros(p)) permitidos.put(par.clave, par);
        for (Map.Entry<String, Double> e : propuestos.entrySet()) {
            Parametro par = permitidos.get(e.getKey());
            if (par != null && e.getValue() != null) resultado.put(par.clave, par.acotar(e.getValue()));
        }
        return resultado;
    }

    /** Devuelve la pieza con sus ajustes ya validados y acotados dentro de los límites de equilibrio. */
    public static Pieza aplicarAjustes(Pieza p, Map<String, Double> propuestos) {
        if (p == null) return null;
        Map<String, Double> seguros = acotarAjustes(p, propuestos);
        return seguros.isEmpty() ? p : p.conValores(seguros);
    }

    /** Genera un ejercicio breve en es, de y en ligado a la mecánica de la pieza. */
    public static Ejercicio para(Pieza p) {
        if (p == null) throw new IllegalArgumentException("pieza nula");
        Map<String, String> v = new LinkedHashMap<>();
        v.put("id", "activar_" + p.id);
        v.put("tema", "04");
        v.put("dificultad", Integer.toString(Math.max(1, Math.min(3, p.marca))));
        v.put("java", "8");
        v.put("tipo", "metodo");
        v.put("titulo.es", "Activar: " + p.nombre(Idioma.ES));
        v.put("titulo.de", "Aktivieren: " + p.nombre(Idioma.DE));
        v.put("titulo.en", "Activate: " + p.nombre(Idioma.EN));
        switch (p.ranura) {
            case BRAZO:
                llenarRetoDeBrazo(p, v);
                break;
            case CABEZA:
                llenarRetoCabeza(p, v);
                break;
            case PIERNAS:
                llenarRetoPiernas(p, v);
                break;
            case NUCLEO:
                llenarRetoNucleo(p, v);
                break;
            case NATURALEZA:
                llenarRetoNaturaleza(p, v);
                break;
            default:
                llenarRetoDistancia(p, v);
                break;
        }
        return new Ejercicio(v, "tienda");
    }

    private static void llenarRetoDeBrazo(Pieza p, Map<String, String> v) {
        switch (p.arma()) {
            case CUERPO:
                llenarRetoCuerpo(p, v);
                break;
            case ESCUDO:
                llenarRetoEscudo(p, v);
                break;
            case REPARAR:
                llenarRetoReparar(p, v);
                break;
            case LLAMA:
                llenarRetoLlama(p, v);
                break;
            default:
                llenarRetoDistancia(p, v);
                break;
        }
    }

    private static void llenarRetoDistancia(Pieza p, Map<String, String> v) {
        double maxDano = redondear(Math.min(Armas.MAX_DANO, Math.max(1, p.valor("dano", 5) * 1.15)));
        String maxStr = num(maxDano);
        String baseStr = num(p.valor("dano", 5));
        v.put("enunciado.es", "Calibra «" + p.nombre(Idioma.ES) + "» antes de montarla. Devuelve danoBase * factor acotado entre 1.0 y "
                + maxStr + " (si danoBase <= 0 o factor <= 0, devuelve 0.0).");
        v.put("enunciado.de", "Kalibriere «" + p.nombre(Idioma.DE) + "» vor dem Einbau. Gib danoBase * factor begrenzt zwischen 1.0 und "
                + maxStr + " zurück (bei danoBase <= 0 oder factor <= 0 gib 0.0 zurück).");
        v.put("enunciado.en", "Calibrate \"" + p.nombre(Idioma.EN) + "\" before equipping. Return danoBase * factor clamped between 1.0 and "
                + maxStr + " (if danoBase <= 0 or factor <= 0, return 0.0).");
        v.put("pista.es", "Comprueba primero si danoBase <= 0 || factor <= 0; si no, usa Math.max(1.0, Math.min(" + maxStr + ", danoBase * factor)).");
        v.put("pista.de", "Prüfe zuerst danoBase <= 0 || factor <= 0; sonst nutze Math.max(1.0, Math.min(" + maxStr + ", danoBase * factor)).");
        v.put("pista.en", "Check danoBase <= 0 || factor <= 0 first; otherwise use Math.max(1.0, Math.min(" + maxStr + ", danoBase * factor)).");
        v.put("firma", "static double calibrarDisparo(double danoBase, double factor)");
        v.put("plantilla", "return 0.0;\n");
        v.put("solucion", "if (danoBase <= 0 || factor <= 0) return 0.0;\n"
                + "return Math.max(1.0, Math.min(" + maxStr + ", danoBase * factor));\n");
        v.put("casos", "calibrarDisparo(" + baseStr + ", 1.0) => " + num(Math.max(1.0, Math.min(maxDano, p.valor("dano", 5)))) + "\n"
                + "calibrarDisparo(" + baseStr + ", 5.0) => " + maxStr + "\n"
                + "calibrarDisparo(0.2, 1.0) => 1.0\n"
                + "calibrarDisparo(-4.0, 1.0) => 0.0\n");
    }

    private static void llenarRetoCuerpo(Pieza p, Map<String, String> v) {
        double alcance = redondear(Armas.alcanceSeguro(p.valor("alcance", 55)));
        double maxDano = redondear(Math.min(Armas.MAX_DANO, Math.max(1, p.valor("dano", 10) * 1.15)));
        v.put("enunciado.es", "Activa «" + p.nombre(Idioma.ES) + "» (alcance " + num(alcance)
                + "). Si distancia <= " + num(alcance) + " y dano > 0, devuelve Math.min(dano, " + num(maxDano) + "); si no, 0.0.");
        v.put("enunciado.de", "Aktiviere «" + p.nombre(Idioma.DE) + "» (Reichweite " + num(alcance)
                + "). Wenn distancia <= " + num(alcance) + " und dano > 0, gib Math.min(dano, " + num(maxDano) + ") zurück; sonst 0.0.");
        v.put("enunciado.en", "Activate \"" + p.nombre(Idioma.EN) + "\" (range " + num(alcance)
                + "). If distancia <= " + num(alcance) + " and dano > 0, return Math.min(dano, " + num(maxDano) + "); otherwise 0.0.");
        v.put("pista.es", "Devuelve 0.0 cuando el rival esté fuera de alcance o el daño no sea positivo.");
        v.put("pista.de", "Gib 0.0 zurück, wenn der Gegner außer Reichweite ist oder der Schaden nicht positiv ist.");
        v.put("pista.en", "Return 0.0 when the enemy is out of range or damage is not positive.");
        v.put("firma", "static double golpeSeguro(double distancia, double dano)");
        v.put("plantilla", "return 0.0;\n");
        v.put("solucion", "if (distancia > " + num(alcance) + " || dano <= 0) return 0.0;\nreturn Math.min(" + num(maxDano) + ", dano);\n");
        v.put("casos", "golpeSeguro(" + num(alcance / 2) + ", " + num(Math.min(maxDano, p.valor("dano", 10))) + ") => "
                + num(Math.min(maxDano, p.valor("dano", 10))) + "\n"
                + "golpeSeguro(" + num(alcance) + ", 999.0) => " + num(maxDano) + "\n"
                + "golpeSeguro(" + num(alcance + 25) + ", 10.0) => 0.0\n"
                + "golpeSeguro(10.0, -5.0) => 0.0\n");
    }

    private static void llenarRetoEscudo(Pieza p, Map<String, String> v) {
        double maxBloq = Armas.MAX_BLOQUEO;
        v.put("enunciado.es", "Programa el escudo «" + p.nombre(Idioma.ES) + "»: acota bloqueo entre 0.1 y " + num(maxBloq)
                + " y devuelve el daño restante dano * (1.0 - bloqueoAcotado) (0.0 si dano <= 0).");
        v.put("enunciado.de", "Programmiere den Schild «" + p.nombre(Idioma.DE) + "»: begrenze bloqueo zwischen 0.1 und " + num(maxBloq)
                + " und gib den Restschaden dano * (1.0 - bloqueoAcotado) zurück (0.0 bei dano <= 0).");
        v.put("enunciado.en", "Program shield \"" + p.nombre(Idioma.EN) + "\": clamp bloqueo between 0.1 and " + num(maxBloq)
                + " and return remaining damage dano * (1.0 - clampedBlock) (0.0 if dano <= 0).");
        v.put("pista.es", "Usa double b = Math.max(0.1, Math.min(" + num(maxBloq) + ", bloqueo)); y devuelve dano * (1.0 - b).");
        v.put("pista.de", "Nutze double b = Math.max(0.1, Math.min(" + num(maxBloq) + ", bloqueo)); und gib dano * (1.0 - b) zurück.");
        v.put("pista.en", "Use double b = Math.max(0.1, Math.min(" + num(maxBloq) + ", bloqueo)); and return dano * (1.0 - b).");
        v.put("firma", "static double danoTrasEscudo(double dano, double bloqueo)");
        v.put("plantilla", "return dano;\n");
        v.put("solucion", "if (dano <= 0) return 0.0;\ndouble b = Math.max(0.1, Math.min(" + num(maxBloq) + ", bloqueo));\nreturn dano * (1.0 - b);\n");
        v.put("casos", "danoTrasEscudo(100.0, 0.5) => 50.0\n"
                + "danoTrasEscudo(100.0, 1.5) => 15.0\n"
                + "danoTrasEscudo(100.0, -0.2) => 90.0\n"
                + "danoTrasEscudo(-10.0, 0.5) => 0.0\n");
    }

    private static void llenarRetoReparar(Pieza p, Map<String, String> v) {
        double maxCura = redondear(Math.min(Armas.MAX_CURA, Math.max(1, p.valor("cura", 12) * 1.15)));
        v.put("enunciado.es", "Activa «" + p.nombre(Idioma.ES) + "»: devuelve la nueva vida tras sumar cura acotada a [0, "
                + num(maxCura) + "], sin superar vidaMax.");
        v.put("enunciado.de", "Aktiviere «" + p.nombre(Idioma.DE) + "»: gib die neue Lebenspunktezahl nach Addition von cura in [0, "
                + num(maxCura) + "] zurück, maximal vidaMax.");
        v.put("enunciado.en", "Activate \"" + p.nombre(Idioma.EN) + "\": return the new health after adding cura clamped to [0, "
                + num(maxCura) + "], never exceeding vidaMax.");
        v.put("pista.es", "Acota cura con Math.max(0.0, Math.min(" + num(maxCura) + ", cura)) y luego Math.min(vidaMax, vida + c).");
        v.put("pista.de", "Begrenze cura mit Math.max(0.0, Math.min(" + num(maxCura) + ", cura)) und dann Math.min(vidaMax, vida + c).");
        v.put("pista.en", "Clamp cura with Math.max(0.0, Math.min(" + num(maxCura) + ", cura)) then Math.min(vidaMax, vida + c).");
        v.put("firma", "static double repararSeguro(double vida, double vidaMax, double cura)");
        v.put("plantilla", "return vida;\n");
        v.put("solucion", "double c = Math.max(0.0, Math.min(" + num(maxCura) + ", cura));\nreturn Math.min(vidaMax, vida + c);\n");
        v.put("casos", "repararSeguro(50.0, 100.0, 10.0) => 60.0\n"
                + "repararSeguro(50.0, 100.0, 999.0) => " + num(50.0 + maxCura) + "\n"
                + "repararSeguro(95.0, 100.0, 10.0) => 100.0\n"
                + "repararSeguro(50.0, 100.0, -5.0) => 50.0\n");
    }

    private static void llenarRetoLlama(Pieza p, Map<String, String> v) {
        int maxDur = Math.min(Armas.MAX_DURACION, (int) Math.ceil(p.valor("duracion", 14) * 1.15));
        double dano = redondear(Armas.danoSeguro(p.valor("dano", 1.1)));
        v.put("enunciado.es", "Regula «" + p.nombre(Idioma.ES) + "»: acota duracion a [5, " + maxDur
                + "] turnos y devuelve el daño total duracionAcotada * " + num(dano) + ".");
        v.put("enunciado.de", "Regle «" + p.nombre(Idioma.DE) + "»: begrenze duracion auf [5, " + maxDur
                + "] Züge und gib den Gesamtschaden duracionAcotada * " + num(dano) + " zurück.");
        v.put("enunciado.en", "Regulate \"" + p.nombre(Idioma.EN) + "\": clamp duracion to [5, " + maxDur
                + "] ticks and return total damage clampedDuration * " + num(dano) + ".");
        v.put("pista.es", "Usa int d = Math.max(5, Math.min(" + maxDur + ", duracion)); y multiplica por " + num(dano) + ".");
        v.put("pista.de", "Nutze int d = Math.max(5, Math.min(" + maxDur + ", duracion)); und multipliziere mit " + num(dano) + ".");
        v.put("pista.en", "Use int d = Math.max(5, Math.min(" + maxDur + ", duracion)); and multiply by " + num(dano) + ".");
        v.put("firma", "static double danoLlama(int duracion)");
        v.put("plantilla", "return 0.0;\n");
        v.put("solucion", "int d = Math.max(5, Math.min(" + maxDur + ", duracion));\nreturn d * " + num(dano) + ";\n");
        v.put("casos", "danoLlama(10) => " + num(10 * dano) + "\n"
                + "danoLlama(2) => " + num(5 * dano) + "\n"
                + "danoLlama(999) => " + num(maxDur * dano) + "\n"
                + "danoLlama(-3) => " + num(5 * dano) + "\n");
    }

    private static void llenarRetoCabeza(Pieza p, Map<String, String> v) {
        double radar = redondear(Math.max(150, Math.min(1100, p.valor("radar", 500))));
        double medioCono = redondear(Armas.conoSeguro(p.valor("cono", 60)) / 2.0);
        v.put("enunciado.es", "Calibra el radar de «" + p.nombre(Idioma.ES) + "»: devuelve true si distancia <= "
                + num(radar) + " y Math.abs(angulo) <= " + num(medioCono) + " (y distancia >= 0).");
        v.put("enunciado.de", "Kalibriere das Radar von «" + p.nombre(Idioma.DE) + "»: gib true zurück, wenn distancia <= "
                + num(radar) + " und Math.abs(angulo) <= " + num(medioCono) + " (und distancia >= 0).");
        v.put("enunciado.en", "Calibrate \"" + p.nombre(Idioma.EN) + "\" radar: return true if distancia <= "
                + num(radar) + " and Math.abs(angulo) <= " + num(medioCono) + " (and distancia >= 0).");
        v.put("pista.es", "Combina tres condiciones con &&: distancia >= 0, distancia <= " + num(radar) + " y Math.abs(angulo) <= " + num(medioCono) + ".");
        v.put("pista.de", "Verknüpfe drei Bedingungen mit &&: distancia >= 0, distancia <= " + num(radar) + " und Math.abs(angulo) <= " + num(medioCono) + ".");
        v.put("pista.en", "Combine three conditions with &&: distancia >= 0, distancia <= " + num(radar) + " and Math.abs(angulo) <= " + num(medioCono) + ".");
        v.put("firma", "static boolean detectaObjetivo(double distancia, double angulo)");
        v.put("plantilla", "return false;\n");
        v.put("solucion", "return distancia >= 0 && distancia <= " + num(radar) + " && Math.abs(angulo) <= " + num(medioCono) + ";\n");
        v.put("casos", "detectaObjetivo(100.0, 0.0) => true\n"
                + "detectaObjetivo(" + num(radar) + ", -" + num(medioCono) + ") => true\n"
                + "detectaObjetivo(" + num(radar + 50) + ", 0.0) => false\n"
                + "detectaObjetivo(100.0, " + num(medioCono + 15) + ") => false\n");
    }

    private static void llenarRetoPiernas(Pieza p, Map<String, String> v) {
        double velMax = redondear(Math.min(Cuerpo.MAX_VELOCIDAD, p.valor("velocidad", 5) * 1.12));
        v.put("enunciado.es", "Sincroniza «" + p.nombre(Idioma.ES) + "»: acota velocidad a [1.0, " + num(velMax)
                + "] y, si sobrecargado es true, multiplícala por 0.7.");
        v.put("enunciado.de", "Synchronisiere «" + p.nombre(Idioma.DE) + "»: begrenze velocidad auf [1.0, " + num(velMax)
                + "] und multipliziere sie bei sobrecargado == true mit 0.7.");
        v.put("enunciado.en", "Sync \"" + p.nombre(Idioma.EN) + "\": clamp velocidad to [1.0, " + num(velMax)
                + "] and, if sobrecargado is true, multiply it by 0.7.");
        v.put("pista.es", "Primero acota v con Math.max(1.0, Math.min(" + num(velMax) + ", velocidad)); luego aplica el factor 0.7 si sobrecargado.");
        v.put("pista.de", "Begrenze zuerst v mit Math.max(1.0, Math.min(" + num(velMax) + ", velocidad)); wende dann 0.7 bei sobrecargado an.");
        v.put("pista.en", "First clamp v with Math.max(1.0, Math.min(" + num(velMax) + ", velocidad)); then apply 0.7 if sobrecargado.");
        v.put("firma", "static double velocidadSegura(double velocidad, boolean sobrecargado)");
        v.put("plantilla", "return 0.0;\n");
        v.put("solucion", "double v = Math.max(1.0, Math.min(" + num(velMax) + ", velocidad));\nreturn sobrecargado ? v * 0.7 : v;\n");
        v.put("casos", "velocidadSegura(3.0, false) => 3.0\n"
                + "velocidadSegura(3.0, true) => 2.1\n"
                + "velocidadSegura(99.0, false) => " + num(velMax) + "\n"
                + "velocidadSegura(-5.0, false) => 1.0\n");
    }

    private static void llenarRetoNucleo(Pieza p, Map<String, String> v) {
        double maxRec = redondear(Math.min(Cuerpo.MAX_RECARGA, Math.max(Cuerpo.MIN_RECARGA, p.valor("recarga", 0.45) * 1.15)));
        double maxE = redondear(Cuerpo.energiaMaxSegura(p.valor("energiaMax", 100)));
        v.put("enunciado.es", "Estabiliza «" + p.nombre(Idioma.ES) + "»: acota recarga a [0.1, " + num(maxRec)
                + "] y devuelve Math.min(" + num(maxE) + ", energiaActual + recargaAcotada).");
        v.put("enunciado.de", "Stabilisiere «" + p.nombre(Idioma.DE) + "»: begrenze recarga auf [0.1, " + num(maxRec)
                + "] und gib Math.min(" + num(maxE) + ", energiaActual + recargaAcotada) zurück.");
        v.put("enunciado.en", "Stabilize \"" + p.nombre(Idioma.EN) + "\": clamp recarga to [0.1, " + num(maxRec)
                + "] and return Math.min(" + num(maxE) + ", energiaActual + clampedRecharge).");
        v.put("pista.es", "Usa double r = Math.max(0.1, Math.min(" + num(maxRec) + ", recarga)); y suma sin pasar de " + num(maxE) + ".");
        v.put("pista.de", "Nutze double r = Math.max(0.1, Math.min(" + num(maxRec) + ", recarga)); und bleib maximal bei " + num(maxE) + ".");
        v.put("pista.en", "Use double r = Math.max(0.1, Math.min(" + num(maxRec) + ", recarga)); and cap at " + num(maxE) + ".");
        v.put("firma", "static double siguienteEnergia(double energiaActual, double recarga)");
        v.put("plantilla", "return energiaActual;\n");
        v.put("solucion", "double r = Math.max(0.1, Math.min(" + num(maxRec) + ", recarga));\nreturn Math.min(" + num(maxE) + ", energiaActual + r);\n");
        v.put("casos", "siguienteEnergia(20.0, 0.3) => " + num(20.0 + Math.min(maxRec, 0.3)) + "\n"
                + "siguienteEnergia(20.0, 0.0) => 20.1\n"
                + "siguienteEnergia(20.0, 99.0) => " + num(20.0 + maxRec) + "\n"
                + "siguienteEnergia(" + num(maxE) + ", 0.3) => " + num(maxE) + "\n");
    }

    private static void llenarRetoNaturaleza(Pieza p, Map<String, String> v) {
        v.put("enunciado.es", "Sintoniza «" + p.nombre(Idioma.ES) + "»: acota refuerzo a [0.01, 0.25]. "
                + "Si mismaNaturaleza es true, devuélvelo entero; si no, la mitad.");
        v.put("enunciado.de", "Stimme «" + p.nombre(Idioma.DE) + "» ab: begrenze refuerzo auf [0.01, 0.25]. "
                + "Bei mismaNaturaleza == true gib den vollen Wert zurück, sonst die Hälfte.");
        v.put("enunciado.en", "Tune \"" + p.nombre(Idioma.EN) + "\": clamp refuerzo to [0.01, 0.25]. "
                + "If mismaNaturaleza is true, return it in full; otherwise return half.");
        v.put("pista.es", "Primero acota r = Math.max(0.01, Math.min(0.25, refuerzo)); luego devuelve mismaNaturaleza ? r : r * 0.5.");
        v.put("pista.de", "Begrenze zuerst r = Math.max(0.01, Math.min(0.25, refuerzo)); gib dann mismaNaturaleza ? r : r * 0.5 zurück.");
        v.put("pista.en", "First clamp r = Math.max(0.01, Math.min(0.25, refuerzo)); then return mismaNaturaleza ? r : r * 0.5.");
        v.put("firma", "static double refuerzoSeguro(double refuerzo, boolean mismaNaturaleza)");
        v.put("plantilla", "return 0.0;\n");
        v.put("solucion", "double r = Math.max(0.01, Math.min(0.25, refuerzo));\nreturn mismaNaturaleza ? r : r * 0.5;\n");
        v.put("casos", "refuerzoSeguro(0.10, true) => 0.10\n"
                + "refuerzoSeguro(0.10, false) => 0.05\n"
                + "refuerzoSeguro(2.0, true) => 0.25\n"
                + "refuerzoSeguro(-1.0, false) => 0.005\n");
    }

    private static double redondear(double x) {
        return Math.round(x * 100.0) / 100.0;
    }

    private static String num(double x) {
        double r = redondear(x);
        if (r == Math.rint(r)) return String.format(Locale.ROOT, "%.1f", r);
        return String.format(Locale.ROOT, "%.2f", r);
    }
}
