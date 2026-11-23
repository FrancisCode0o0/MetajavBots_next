package metajavbots.piezas;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import metajavbots.api.PiezaPropia;
import metajavbots.api.Puente;
import metajavbots.api.TopeDeCalculo;
import metajavbots.codigo.Cargador;
import metajavbots.codigo.Compilador;
import metajavbots.codigo.Vigilante;
import metajavbots.textos.Idioma;

/**
 * DISEÑADOR DE PIEZA — Compila bajo las barandillas de seguridad de
 * {@link Vigilante} y {@link Cargador} una clase escrita en Java por el
 * jugador que implementa {@link PiezaPropia} ({@code EigenesTeil} en alemán,
 * {@code CustomPart} en inglés), comprueba límites anti-abuso y un presupuesto
 * máximo de 100 puntos de equilibrio y construye una {@link Pieza} real lista
 * para guardarse y montarse en el robot.
 */
public final class DisenadorDePieza {

    /** Presupuesto máximo permitido para cualquier pieza diseñada por el jugador. */
    public static final double MAX_PRESUPUESTO = 100.0;
    /** Límite de pasos de cálculo al instanciar y leer atributos de la pieza. */
    private static final long TOPE_PASOS = 25_000L;

    /** Resultado de compilar y validar una pieza escrita por el jugador. */
    public static final class Resultado {
        public final boolean valida;
        public final Pieza pieza;
        public final double presupuesto;
        public final Compilador.Resultado compilacion;
        public final List<String> errores;

        Resultado(boolean valida, Pieza pieza, double presupuesto, Compilador.Resultado compilacion, List<String> errores) {
            this.valida = valida;
            this.pieza = pieza;
            this.presupuesto = presupuesto;
            this.compilacion = compilacion;
            this.errores = Collections.unmodifiableList(errores);
        }
    }

    private DisenadorDePieza() {
    }

    /** Compila, verifica con Vigilante/Cargador y valida el equilibrio de una pieza escrita por el jugador. */
    public static Resultado compilarYValidar(String codigo, Idioma idioma) {
        Idioma idm = idioma == null ? Idioma.ES : idioma;
        List<String> errores = new ArrayList<>();
        if (codigo == null || codigo.isBlank()) {
            errores.add(idm.elegir("El código de la pieza está vacío.", "Der Code des Teils ist leer.", "The part code is empty."));
            return new Resultado(false, null, 0, null, errores);
        }
        Compilador.Resultado res = Compilador.compilar(codigo, true, Vigilante.Modo.ROBOT, idm.locale());
        if (!res.bien) {
            for (Compilador.Fallo f : res.graves()) errores.add(f.toString());
            return new Resultado(false, null, 0, res, errores);
        }
        PiezaPropia instancia = instanciar(res, idm, errores);
        if (instancia == null) return new Resultado(false, null, 0, res, errores);
        return validarInstancia(instancia, res, idm, errores);
    }

    private static PiezaPropia instanciar(Compilador.Resultado res, Idioma idm, List<String> errores) {
        Cargador cargador = new Cargador(res.clases, Vigilante.Modo.ROBOT);
        Class<?> clasePieza = null;
        for (String nombre : res.clases.keySet()) {
            if (nombre.contains("$")) continue;
            try {
                Class<?> c = cargador.loadClass(nombre);
                if (PiezaPropia.class.isAssignableFrom(c) && !c.isInterface()
                        && !java.lang.reflect.Modifier.isAbstract(c.getModifiers())) {
                    clasePieza = c;
                    break;
                }
            } catch (ClassNotFoundException | LinkageError e) {
                errores.add(e.getMessage());
                return null;
            }
        }
        if (clasePieza == null) {
            errores.add(idm.elegir(
                    "Tu clase debe implementar PiezaPropia (public class MiPieza implements PiezaPropia).",
                    "Deine Klasse muss EigenesTeil (oder PiezaPropia) implementieren.",
                    "Your class must implement CustomPart (or PiezaPropia)."));
            return null;
        }
        Puente.contar(TOPE_PASOS, 200_000_000L);
        try {
            return (PiezaPropia) clasePieza.getDeclaredConstructor().newInstance();
        } catch (TopeDeCalculo e) {
            errores.add(idm.elegir("Bucle infinito o demasiado lento al crear la pieza.",
                    "Endlosschleife oder zu langsam beim Erstellen des Teils.",
                    "Infinite loop or too slow while creating the part."));
            return null;
        } catch (ReflectiveOperationException | RuntimeException e) {
            Throwable causa = e.getCause() != null ? e.getCause() : e;
            errores.add(idm.elegir("Error al crear la pieza: ", "Fehler beim Erstellen des Teils: ",
                    "Error creating the part: ") + causa.getMessage());
            return null;
        } finally {
            Puente.dejarDeContar();
        }
    }

    private static Resultado validarInstancia(PiezaPropia pp, Compilador.Resultado res, Idioma idm, List<String> errores) {
        Puente.contar(TOPE_PASOS, 200_000_000L);
        try {
            String nombre = pp.nombre() == null ? "" : pp.nombre().strip();
            if (nombre.isEmpty() || nombre.length() > 28) {
                errores.add(idm.elegir("El nombre debe tener entre 1 y 28 caracteres.",
                        "Der Name muss zwischen 1 und 28 Zeichen lang sein.",
                        "The name must be between 1 and 28 characters."));
            }
            Pieza.Ranura ranura = parsearRanura(pp.ranura());
            if (ranura == null) {
                errores.add(idm.elegir("Ranura no válida. Usa CABEZA, BRAZO, PIERNAS o NUCLEO.",
                        "Ungültiger Platz. Nutze KOPF, ARM, BEINE oder KERN.",
                        "Invalid slot. Use HEAD, ARM, LEGS, or CORE."));
                return new Resultado(false, null, 0, res, errores);
            }
            Naturaleza nat = Naturaleza.de(pp.naturaleza());
            Map<String, Double> valores = new LinkedHashMap<>();
            Map<String, String> textos = new LinkedHashMap<>();
            double presupuesto = evaluarRanura(pp, ranura, valores, textos, idm, errores);
            presupuesto = Math.round(presupuesto * 10.0) / 10.0;
            if (presupuesto > MAX_PRESUPUESTO) {
                errores.add(idm.elegir(
                        "Presupuesto excedido: " + presupuesto + " / " + (int) MAX_PRESUPUESTO + " puntos. Baja atributos o sube el peso.",
                        "Budget überschritten: " + presupuesto + " / " + (int) MAX_PRESUPUESTO + " Punkte. Senke Werte oder erhöhe das Gewicht.",
                        "Budget exceeded: " + presupuesto + " / " + (int) MAX_PRESUPUESTO + " points. Lower stats or increase weight."));
            }
            if (!errores.isEmpty()) return new Resultado(false, null, presupuesto, res, errores);
            String id = idDeNombre(nombre);
            Pieza pieza = Pieza.personalizada(id, ranura, nombre, nat, valores, textos);
            return new Resultado(true, pieza, presupuesto, res, errores);
        } catch (TopeDeCalculo e) {
            errores.add(idm.elegir("Demasiados pasos de cálculo al leer los atributos de la pieza.",
                    "Zu viele Rechenschritte beim Auslesen der Teilwerte.",
                    "Too many computation steps while reading part stats."));
            return new Resultado(false, null, 0, res, errores);
        } catch (RuntimeException e) {
            errores.add(idm.elegir("Error al leer los atributos: ", "Fehler beim Lesen der Werte: ",
                    "Error reading stats: ") + e.getMessage());
            return new Resultado(false, null, 0, res, errores);
        } finally {
            Puente.dejarDeContar();
        }
    }

    private static double evaluarRanura(PiezaPropia pp, Pieza.Ranura ranura, Map<String, Double> val,
                                        Map<String, String> txt, Idioma idm, List<String> err) {
        switch (ranura) {
            case BRAZO: return evaluarBrazo(pp, val, txt, idm, err);
            case CABEZA: return evaluarCabeza(pp, val, txt, idm, err);
            case PIERNAS: return evaluarPiernas(pp, val, txt, idm, err);
            default: return evaluarNucleo(pp, val, txt, idm, err);
        }
    }

    private static double evaluarBrazo(PiezaPropia pp, Map<String, Double> val, Map<String, String> txt, Idioma idm, List<String> err) {
        Pieza.Arma arma = Pieza.enumDe(Pieza.Arma.class, pp.estilo(), Pieza.Arma.NINGUNA);
        if (arma == Pieza.Arma.NINGUNA) {
            err.add(idm.elegir("Estilo de brazo no válido (LASER, BALA, MISIL, CUERPO, LLAMA, ESCUDO o REPARAR).",
                    "Ungültiger Arm-Stil (LASER, BALA, MISIL, CUERPO, LLAMA, ESCUDO oder REPARAR).",
                    "Invalid arm style (LASER, BALA, MISIL, CUERPO, LLAMA, ESCUDO, or REPARAR)."));
            arma = Pieza.Arma.LASER;
        }
        double dano = comprobar(pp.dano(), 0, 16, "dano", idm, err);
        double alcance = comprobar(pp.alcance(), 45, 420, "alcance", idm, err);
        double enf = comprobar(pp.enfriamiento(), 8, 60, "enfriamiento", idm, err);
        double ener = comprobar(pp.energia(), 4, 30, "energia", idm, err);
        double blin = comprobar(pp.blindaje(), 0, 28, "blindaje", idm, err);
        double peso = comprobar(pp.peso(), 4, 16, "peso", idm, err);
        txt.put("arma", arma.name());
        val.put("dano", redondear(dano));
        val.put("alcance", redondear(alcance));
        val.put("enfriamiento", redondear(enf));
        val.put("energia", redondear(ener));
        val.put("blindaje", redondear(blin));
        val.put("peso", redondear(peso));
        if (arma == Pieza.Arma.ESCUDO) val.put("bloqueo", 0.55);
        if (arma == Pieza.Arma.REPARAR) val.put("cura", Math.min(12, Math.max(4, dano)));
        double pts = dano * 4.0 + alcance * 0.09 + (32.0 - Math.min(32.0, enf)) * 1.4
                + (18.0 - Math.min(18.0, ener)) * 1.2 + blin * 0.9 - (peso - 4.0) * 1.6;
        return Math.max(0, pts);
    }

    private static double evaluarCabeza(PiezaPropia pp, Map<String, Double> val, Map<String, String> txt, Idioma idm, List<String> err) {
        Pieza.Especial esp = Pieza.enumDe(Pieza.Especial.class, pp.estilo(), Pieza.Especial.ESCUDO);
        double blin = comprobar(pp.blindaje(), 0, 34, "blindaje", idm, err);
        double radar = comprobar(pp.radar(), 180, 520, "radar", idm, err);
        double cono = comprobar(pp.cono(), 25, 90, "cono", idm, err);
        double ener = comprobar(pp.energia(), 10, 45, "energia", idm, err);
        double peso = comprobar(pp.peso(), 4, 14, "peso", idm, err);
        txt.put("especial", esp.name());
        val.put("blindaje", redondear(blin));
        val.put("radar", redondear(radar));
        val.put("cono", redondear(cono));
        val.put("coste", redondear(ener));
        val.put("duracion", 55.0);
        val.put("enfriamiento", 140.0);
        val.put("peso", redondear(peso));
        double bonoEsp = esp == Pieza.Especial.NINGUNO ? 0 : 12.0;
        double pts = blin * 1.5 + (radar - 180.0) * 0.12 + (cono - 25.0) * 0.45
                + (35.0 - Math.min(35.0, ener)) * 0.7 + bonoEsp - (peso - 4.0) * 1.8;
        return Math.max(0, pts);
    }

    private static double evaluarPiernas(PiezaPropia pp, Map<String, Double> val, Map<String, String> txt, Idioma idm, List<String> err) {
        Pieza.Locomocion loc = Pieza.enumDe(Pieza.Locomocion.class, pp.estilo(), Pieza.Locomocion.BIPEDO);
        double vel = comprobar(pp.velocidad(), 2.2, 5.6, "velocidad", idm, err);
        double giro = comprobar(pp.giro(), 3.0, 8.5, "giro", idm, err);
        double acel = comprobar(pp.aceleracion(), 0.25, 0.75, "aceleracion", idm, err);
        double carga = comprobar(pp.carga(), 20, 80, "carga", idm, err);
        double blin = comprobar(pp.blindaje(), 0, 34, "blindaje", idm, err);
        double peso = comprobar(pp.peso(), 5, 16, "peso", idm, err);
        txt.put("locomocion", loc.name());
        val.put("velocidad", redondear(vel));
        val.put("giro", redondear(giro));
        val.put("aceleracion", redondear(acel));
        val.put("carga", redondear(carga));
        val.put("blindaje", redondear(blin));
        val.put("peso", redondear(peso));
        double costeVuelo = loc == Pieza.Locomocion.VUELO ? 14.0 : 0.0;
        double pts = (vel - 2.2) * 14.0 + (giro - 3.0) * 4.5 + (acel - 0.25) * 28.0
                + (carga - 20.0) * 0.5 + blin * 1.0 + costeVuelo - (peso - 5.0) * 1.6;
        return Math.max(0, pts);
    }

    private static double evaluarNucleo(PiezaPropia pp, Map<String, Double> val, Map<String, String> txt, Idioma idm, List<String> err) {
        double pasos = comprobar(pp.pasos(), 1500, 5500, "pasos", idm, err);
        double eMax = comprobar(pp.energiaMax(), 70, 140, "energiaMax", idm, err);
        double rec = comprobar(pp.recarga(), 0.25, 0.60, "recarga", idm, err);
        double blin = comprobar(pp.blindaje(), 0, 24, "blindaje", idm, err);
        double peso = comprobar(pp.peso(), 3, 12, "peso", idm, err);
        val.put("pasos", redondear(pasos));
        val.put("energiaMax", redondear(eMax));
        val.put("recarga", redondear(rec));
        val.put("blindaje", redondear(blin));
        val.put("peso", redondear(peso));
        double pts = (pasos - 1500.0) * 0.011 + (eMax - 70.0) * 0.55 + (rec - 0.25) * 85.0
                + blin * 1.2 - (peso - 3.0) * 1.8;
        return Math.max(0, pts);
    }

    private static double comprobar(double valor, double min, double max, String campo, Idioma idm, List<String> err) {
        if (!Double.isFinite(valor) || valor < min || valor > max) {
            err.add(idm.elegir(
                    "«" + campo + "» debe estar entre " + fmt(min) + " y " + fmt(max) + " (salió " + valor + ").",
                    "„" + campo + "“ muss zwischen " + fmt(min) + " und " + fmt(max) + " liegen (war " + valor + ").",
                    "\"" + campo + "\" must be between " + fmt(min) + " and " + fmt(max) + " (got " + valor + ")."));
            return Math.max(min, Math.min(max, Double.isFinite(valor) ? valor : min));
        }
        return valor;
    }

    /** Convierte un texto de ranura en cualquiera de los tres idiomas a {@link Pieza.Ranura}. */
    public static Pieza.Ranura parsearRanura(String s) {
        if (s == null) return null;
        String k = s.strip().toUpperCase(Locale.ROOT).replace("Ú", "U");
        switch (k) {
            case "CABEZA": case "KOPF": case "HEAD": return Pieza.Ranura.CABEZA;
            case "BRAZO": case "ARM": return Pieza.Ranura.BRAZO;
            case "PIERNAS": case "BEINE": case "LEGS": return Pieza.Ranura.PIERNAS;
            case "NUCLEO": case "KERN": case "CORE": return Pieza.Ranura.NUCLEO;
            default: return null;
        }
    }

    /** Identificador estable y seguro para archivo a partir del nombre visible de la pieza. */
    public static String idDeNombre(String nombre) {
        String slug = nombre.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_").replaceAll("^_|_$", "");
        if (slug.isEmpty()) slug = "pieza";
        if (slug.length() > 28) slug = slug.substring(0, 28);
        return "propia_" + slug;
    }

    /** Plantilla trilingüe lista para compilar y montar según la ranura elegida. */
    public static String plantilla(Idioma idioma, Pieza.Ranura ranura) {
        Idioma idm = idioma == null ? Idioma.ES : idioma;
        Pieza.Ranura r = ranura == null ? Pieza.Ranura.BRAZO : ranura;
        if (idm == Idioma.DE) return plantillaDe(r);
        if (idm == Idioma.EN) return plantillaEn(r);
        return plantillaEs(r);
    }

    private static String plantillaEs(Pieza.Ranura r) {
        String cuerpo;
        switch (r) {
            case CABEZA:
                cuerpo = "    @Override public String nombre() { return \"Visor propio\"; }\n"
                        + "    @Override public String ranura() { return \"CABEZA\"; }\n"
                        + "    @Override public String naturaleza() { return \"FOCO\"; }\n"
                        + "    @Override public String estilo() { return \"ESCUDO\"; } // ESCUDO, TURBO, REPARAR, CAMUFLAJE, PULSO, SOBRECARGA\n"
                        + "    @Override public double blindaje() { return 18; }  // 0..34\n"
                        + "    @Override public double radar() { return 420; }    // 180..520\n"
                        + "    @Override public double cono() { return 60; }      // 25..90\n"
                        + "    @Override public double energia() { return 24; }   // 10..45\n"
                        + "    @Override public double peso() { return 7; }       // 4..14\n";
                break;
            case PIERNAS:
                cuerpo = "    @Override public String nombre() { return \"Zancada propia\"; }\n"
                        + "    @Override public String ranura() { return \"PIERNAS\"; }\n"
                        + "    @Override public String naturaleza() { return \"RAYO\"; }\n"
                        + "    @Override public String estilo() { return \"RUEDAS\"; } // BIPEDO, ORUGAS, RUEDAS, FLOTANTE, VUELO, PATAS\n"
                        + "    @Override public double velocidad() { return 4.4; }    // 2.2..5.6\n"
                        + "    @Override public double giro() { return 6.5; }         // 3.0..8.5\n"
                        + "    @Override public double aceleracion() { return 0.55; } // 0.25..0.75\n"
                        + "    @Override public double carga() { return 50; }         // 20..80\n"
                        + "    @Override public double blindaje() { return 14; }      // 0..34\n"
                        + "    @Override public double peso() { return 8; }           // 5..16\n";
                break;
            case NUCLEO:
                cuerpo = "    @Override public String nombre() { return \"Núcleo propio\"; }\n"
                        + "    @Override public String ranura() { return \"NUCLEO\"; }\n"
                        + "    @Override public String naturaleza() { return \"INGENIO\"; }\n"
                        + "    @Override public double pasos() { return 3800; }     // 1500..5500\n"
                        + "    @Override public double energiaMax() { return 115; } // 70..140\n"
                        + "    @Override public double recarga() { return 0.46; }   // 0.25..0.60\n"
                        + "    @Override public double blindaje() { return 12; }    // 0..24\n"
                        + "    @Override public double peso() { return 6; }         // 3..12\n";
                break;
            default:
                cuerpo = "    @Override public String nombre() { return \"Cañón propio\"; }\n"
                        + "    @Override public String ranura() { return \"BRAZO\"; }\n"
                        + "    @Override public String naturaleza() { return \"ACERO\"; }\n"
                        + "    @Override public String estilo() { return \"LASER\"; } // LASER, BALA, MISIL, CUERPO, LLAMA, ESCUDO, REPARAR\n"
                        + "    @Override public double dano() { return 10; }          // 0..16\n"
                        + "    @Override public double alcance() { return 260; }      // 45..420\n"
                        + "    @Override public double enfriamiento() { return 18; }  // 8..60\n"
                        + "    @Override public double energia() { return 12; }       // 4..30\n"
                        + "    @Override public double blindaje() { return 10; }      // 0..28\n"
                        + "    @Override public double peso() { return 8; }           // 4..16\n";
                break;
        }
        return "import metajavbots.api.PiezaPropia;\n\n"
                + "/** Diseña tu propia pieza respetando el presupuesto máximo de 100 puntos. */\n"
                + "public class MiPieza implements PiezaPropia {\n" + cuerpo + "}\n";
    }

    private static String plantillaDe(Pieza.Ranura r) {
        String cuerpo;
        switch (r) {
            case CABEZA:
                cuerpo = "    @Override public String name() { return \"Mein Visier\"; }\n"
                        + "    @Override public String platz() { return \"KOPF\"; }\n"
                        + "    @Override public String natur() { return \"FOCO\"; }\n"
                        + "    @Override public String stil() { return \"ESCUDO\"; } // ESCUDO, TURBO, REPARAR, CAMUFLAJE, PULSO, SOBRECARGA\n"
                        + "    @Override public double panzerung() { return 18; } // 0..34\n"
                        + "    @Override public double radar() { return 420; }    // 180..520\n"
                        + "    @Override public double kegel() { return 60; }     // 25..90\n"
                        + "    @Override public double energie() { return 24; }   // 10..45\n"
                        + "    @Override public double gewicht() { return 7; }    // 4..14\n";
                break;
            case PIERNAS:
                cuerpo = "    @Override public String name() { return \"Mein Antrieb\"; }\n"
                        + "    @Override public String platz() { return \"BEINE\"; }\n"
                        + "    @Override public String natur() { return \"RAYO\"; }\n"
                        + "    @Override public String stil() { return \"RUEDAS\"; } // BIPEDO, ORUGAS, RUEDAS, FLOTANTE, VUELO, PATAS\n"
                        + "    @Override public double tempo() { return 4.4; }         // 2.2..5.6\n"
                        + "    @Override public double drehung() { return 6.5; }       // 3.0..8.5\n"
                        + "    @Override public double beschleunigung() { return 0.55; } // 0.25..0.75\n"
                        + "    @Override public double traglast() { return 34; }       // 20..46\n"
                        + "    @Override public double panzerung() { return 14; }      // 0..34\n"
                        + "    @Override public double gewicht() { return 8; }         // 5..16\n";
                break;
            case NUCLEO:
                cuerpo = "    @Override public String name() { return \"Mein Kern\"; }\n"
                        + "    @Override public String platz() { return \"KERN\"; }\n"
                        + "    @Override public String natur() { return \"INGENIO\"; }\n"
                        + "    @Override public double schritte() { return 3800; }   // 1500..5500\n"
                        + "    @Override public double maxEnergie() { return 115; }  // 70..140\n"
                        + "    @Override public double aufladung() { return 0.46; }  // 0.25..0.60\n"
                        + "    @Override public double panzerung() { return 12; }    // 0..24\n"
                        + "    @Override public double gewicht() { return 6; }       // 3..12\n";
                break;
            default:
                cuerpo = "    @Override public String name() { return \"Meine Kanone\"; }\n"
                        + "    @Override public String platz() { return \"ARM\"; }\n"
                        + "    @Override public String natur() { return \"ACERO\"; }\n"
                        + "    @Override public String stil() { return \"LASER\"; } // LASER, BALA, MISIL, CUERPO, LLAMA, ESCUDO, REPARAR\n"
                        + "    @Override public double schaden() { return 10; }     // 0..16\n"
                        + "    @Override public double reichweite() { return 260; } // 45..420\n"
                        + "    @Override public double abklingzeit() { return 18; } // 8..60\n"
                        + "    @Override public double energie() { return 12; }     // 4..30\n"
                        + "    @Override public double panzerung() { return 10; }   // 0..28\n"
                        + "    @Override public double gewicht() { return 8; }      // 4..16\n";
                break;
        }
        return "import metajavbots.api.EigenesTeil;\n\n"
                + "/** Entwirf dein eigenes Roboterteil innerhalb des Budgets von 100 Punkten. */\n"
                + "public class MeinTeil implements EigenesTeil {\n" + cuerpo + "}\n";
    }

    private static String plantillaEn(Pieza.Ranura r) {
        String cuerpo;
        switch (r) {
            case CABEZA:
                cuerpo = "    @Override public String name() { return \"Custom Visor\"; }\n"
                        + "    @Override public String slot() { return \"HEAD\"; }\n"
                        + "    @Override public String nature() { return \"FOCO\"; }\n"
                        + "    @Override public String style() { return \"ESCUDO\"; } // ESCUDO, TURBO, REPARAR, CAMUFLAJE, PULSO, SOBRECARGA\n"
                        + "    @Override public double armor() { return 18; }   // 0..34\n"
                        + "    @Override public double radar() { return 420; }  // 180..520\n"
                        + "    @Override public double cone() { return 60; }    // 25..90\n"
                        + "    @Override public double energy() { return 24; }  // 10..45\n"
                        + "    @Override public double weight() { return 7; }   // 4..14\n";
                break;
            case PIERNAS:
                cuerpo = "    @Override public String name() { return \"Custom Stride\"; }\n"
                        + "    @Override public String slot() { return \"LEGS\"; }\n"
                        + "    @Override public String nature() { return \"RAYO\"; }\n"
                        + "    @Override public String style() { return \"RUEDAS\"; } // BIPEDO, ORUGAS, RUEDAS, FLOTANTE, VUELO, PATAS\n"
                        + "    @Override public double speed() { return 4.4; }        // 2.2..5.6\n"
                        + "    @Override public double turn() { return 6.5; }         // 3.0..8.5\n"
                        + "    @Override public double acceleration() { return 0.55; } // 0.25..0.75\n"
                        + "    @Override public double capacity() { return 34; }      // 20..46\n"
                        + "    @Override public double armor() { return 14; }         // 0..34\n"
                        + "    @Override public double weight() { return 8; }         // 5..16\n";
                break;
            case NUCLEO:
                cuerpo = "    @Override public String name() { return \"Custom Core\"; }\n"
                        + "    @Override public String slot() { return \"CORE\"; }\n"
                        + "    @Override public String nature() { return \"INGENIO\"; }\n"
                        + "    @Override public double steps() { return 3800; }     // 1500..5500\n"
                        + "    @Override public double maxEnergy() { return 115; }  // 70..140\n"
                        + "    @Override public double recharge() { return 0.46; }  // 0.25..0.60\n"
                        + "    @Override public double armor() { return 12; }       // 0..24\n"
                        + "    @Override public double weight() { return 6; }       // 3..12\n";
                break;
            default:
                cuerpo = "    @Override public String name() { return \"Custom Cannon\"; }\n"
                        + "    @Override public String slot() { return \"ARM\"; }\n"
                        + "    @Override public String nature() { return \"ACERO\"; }\n"
                        + "    @Override public String style() { return \"LASER\"; } // LASER, BALA, MISIL, CUERPO, LLAMA, ESCUDO, REPARAR\n"
                        + "    @Override public double damage() { return 10; }    // 0..16\n"
                        + "    @Override public double range() { return 260; }    // 45..420\n"
                        + "    @Override public double cooldown() { return 18; }  // 8..60\n"
                        + "    @Override public double energy() { return 12; }    // 4..30\n"
                        + "    @Override public double armor() { return 10; }     // 0..28\n"
                        + "    @Override public double weight() { return 8; }     // 4..16\n";
                break;
        }
        return "import metajavbots.api.CustomPart;\n\n"
                + "/** Design your own robot part within the 100-point balance budget. */\n"
                + "public class MyPart implements CustomPart {\n" + cuerpo + "}\n";
    }

    private static double redondear(double x) {
        return Math.round(x * 100.0) / 100.0;
    }

    private static String fmt(double v) {
        return v == Math.rint(v) ? Long.toString(Math.round(v)) : String.format(Locale.ROOT, "%.2f", v);
    }
}
