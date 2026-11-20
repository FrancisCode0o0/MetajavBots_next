package metajavbots.pantallas;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import metajavbots.codigo.Paquetes;
import metajavbots.textos.Idioma;

/**
 * FICHAS DE PAQUETES — Resuelve la ficha práctica trilingüe (descripción breve,
 * dónde se usa y ejemplo en pseudocódigo de 2 a 5 líneas) de cualquier método o
 * campo listado en {@link PanelDePaquetes}, aprovechando {@link Referencia} para
 * los miembros de {@code metajavbots.api}.
 */
final class FichasDePaquetes {

    private FichasDePaquetes() {
    }

    static PanelDePaquetes.FichaMiembro resolver(Paquetes.Componente c, String lineaMiembro, Idioma idioma) {
        Idioma id = idioma == null ? Idioma.ES : idioma;
        String linea = lineaMiembro == null ? "" : lineaMiembro.strip();
        int sep = linea.indexOf(" — ");
        String firma = sep > 0 ? linea.substring(0, sep).strip() : linea;
        String resumen = sep > 0 ? linea.substring(sep + 3).strip() : (c == null ? "" : c.descripcion(id).strip());
        if (firma.isEmpty()) firma = c == null ? "miembro" : c.nombre(id);

        if (c != null && "metajavbots.api".equals(c.paquete)) {
            PanelDePaquetes.FichaMiembro api = resolverApi(c, firma, resumen, id);
            if (api != null) return api;
        }
        String desc = descripcionGeneral(c, firma, resumen, id);
        String donde = dondeGeneral(c, firma, id);
        String pseudo = pseudocodigoGeneral(c, firma, id);
        return new PanelDePaquetes.FichaMiembro(firma, desc, donde, pseudo);
    }

    private static PanelDePaquetes.FichaMiembro resolverApi(Paquetes.Componente c, String firma, String resumen, Idioma id) {
        if ("TopeDeCalculo".equals(c.id)) {
            String desc = capitalizarPunto(resumen.isEmpty() ? c.descripcion(id) : resumen);
            String donde = id.elegir(
                    "Lo lanza el motor si un bucle en cadaTurno() u otro evento supera el límite de pasos; evita bucles infinitos.",
                    "Wird von der Engine ausgelöst, wenn eine Schleife in jedenZug() das Schrittlimit sprengt; vermeide Endlosschleifen.",
                    "Thrown by the engine if a loop in onTick() or an event exceeds the step budget; avoid infinite loops.");
            String pseudo = id.elegir(
                    "CADA TURNO:\n    REPETIR como máximo 10 veces:\n        hacer cálculo corto\n    // sin bucles infinitos",
                    "JEDEN ZUG:\n    WIEDERHOLE höchstens 10-mal:\n        kurze Berechnung ausführen\n    // keine Endlosschleifen",
                    "EVERY TICK:\n    REPEAT at most 10 times:\n        run short calculation\n    // no infinite loops");
            return new PanelDePaquetes.FichaMiembro(firma, desc, donde, pseudo);
        }
        String prefijo = prefijoApi(c.id);
        if (prefijo == null) return null;
        List<String> nombres = extraerMetodos(firma);
        List<Referencia.Entrada> encontradas = new ArrayList<>();
        for (String nom : nombres) {
            Referencia.Entrada e = buscarEnReferencia(prefijo, nom, id);
            if (e != null && !encontradas.contains(e)) encontradas.add(e);
        }
        if (encontradas.isEmpty()) return null;

        Referencia.Entrada principal = encontradas.get(0);
        String desc = encontradas.size() == 1
                ? principal.texto(id).strip()
                : combinarDescripciones(resumen, encontradas, id);
        String donde = principal.donde(id).strip();
        String pseudo = elegirEjemploApi(encontradas, id);
        return new PanelDePaquetes.FichaMiembro(firma, capitalizarPunto(desc), donde, ajustarLineas(pseudo, id, c.nombre(id), firma));
    }

    private static String prefijoApi(String idComp) {
        if ("Metajavbot".equals(idComp)) return "api.";
        if ("Enemigo".equals(idComp)) return "Enemigo.";
        if ("Golpe".equals(idComp)) return "Golpe.";
        return null;
    }

    private static Referencia.Entrada buscarEnReferencia(String prefijo, String metodo, Idioma id) {
        String alias = aliasApi(metodo);
        for (Referencia.Entrada e : Referencia.todas()) {
            if (!e.id.startsWith(prefijo)) continue;
            if (e.id.equals(prefijo + metodo) || e.id.equals(prefijo + alias)) return e;
            String f = e.firma(id);
            if (contieneLlamada(f, metodo)) return e;
        }
        return null;
    }

    private static String aliasApi(String metodo) {
        if ("usarEspecial".equals(metodo) || "spezial".equals(metodo) || "useSpecial".equals(metodo)) return "especial";
        return metodo;
    }

    private static boolean contieneLlamada(String firmaRef, String metodo) {
        int idx = firmaRef.indexOf(metodo + "(");
        if (idx < 0) return false;
        return idx == 0 || !Character.isJavaIdentifierPart(firmaRef.charAt(idx - 1));
    }

    private static String combinarDescripciones(String resumen, List<Referencia.Entrada> encontradas, Idioma id) {
        String base = resumen.isEmpty() ? "" : capitalizarPunto(resumen) + " ";
        String detalle = encontradas.get(0).texto(id).strip();
        return base.isEmpty() ? detalle : base + detalle;
    }

    private static String elegirEjemploApi(List<Referencia.Entrada> encontradas, Idioma id) {
        for (Referencia.Entrada e : encontradas) {
            String ej = e.ejemplo(id).strip();
            int n = contarLineas(ej);
            if (n >= 2 && n <= 5) return ej;
        }
        return encontradas.get(0).ejemplo(id).strip();
    }

    private static String descripcionGeneral(Paquetes.Componente c, String firma, String resumen, Idioma id) {
        String limpio = resumen.isEmpty() ? (c == null ? firma : c.descripcion(id).strip()) : resumen;
        String base = capitalizarPunto(limpio);
        if (c == null) return base;
        if (limpio.length() >= 38) return base;
        String comp = c.nombre(id);
        return id.elegir(
                base + " Miembro de " + comp + " (" + c.paquete + ").",
                base + " Element von " + comp + " (" + c.paquete + ").",
                base + " Member of " + comp + " (" + c.paquete + ").");
    }

    private static String dondeGeneral(Paquetes.Componente c, String firma, Idioma id) {
        if (c == null) {
            return id.elegir(
                    "Úsalo dentro de cadaTurno(), alVerEnemigo() o un método auxiliar de tu robot.",
                    "Verwende es in jedenZug(), beiSicht() oder einer Hilfsmethode deines Roboters.",
                    "Use it inside onTick(), onSeeEnemy(), or a helper method in your robot.");
        }
        if ("metajavbots.junit".equals(c.paquete)) {
            if (c.tipo == Paquetes.Tipo.ANOTACION) {
                return id.elegir(
                        "Colócalo justo encima de un método de prueba en el Laboratorio o en ejercicios de tests (no dentro de un robot de combate).",
                        "Direkt über einer Testmethode im Labor oder in Testübungen platzieren (nicht in einem Kampfroboter).",
                        "Place it right above a test method in the Laboratory or testing exercises (not inside a combat robot).");
            }
            return id.elegir(
                    "Llámalo dentro de un método marcado con @Test en el Laboratorio o en los ejercicios de pruebas unitarias.",
                    "Innerhalb einer mit @Test markierten Methode im Labor oder in Unit-Test-Übungen aufrufen.",
                    "Call it inside a @Test method in the Laboratory or in unit-testing exercises.");
        }
        if (c.soloEjercicio) {
            return id.elegir(
                    "Solo disponible en ejercicios y Laboratorio (la caja de arena bloquea la concurrencia en robots). Úsalo en métodos de ejercicio.",
                    "Nur in Übungen und im Labor erlaubt (die Sandbox sperrt Nebenläufigkeit in Robotern). Nutze es in Übungsmethoden.",
                    "Available only in exercises and the Laboratory (the sandbox blocks concurrency in robots). Use it in exercise methods.");
        }
        if (c.tipo == Paquetes.Tipo.EXCEPCION || c.tipo == Paquetes.Tipo.ERROR) {
            return id.elegir(
                    "Úsalo dentro de un bloque try/catch en cadaTurno(), en un método auxiliar o al validar datos en ejercicios.",
                    "In einem try/catch-Block in jedenZug(), einer Hilfsmethode oder bei der Datenprüfung in Übungen verwenden.",
                    "Use it inside a try/catch block in onTick(), a helper method, or when validating inputs in exercises.");
        }
        if (c.tipo == Paquetes.Tipo.ANOTACION || firma.startsWith("@")) {
            return id.elegir(
                    "Colócalo encima de la clase o del método correspondiente (por ejemplo, encima de cadaTurno() o alVerEnemigo()) en tu clase.",
                    "Direkt über der Klasse oder der passenden Methode (z. B. über jedenZug() oder beiSicht()) im Klassenrumpf platzieren.",
                    "Place it directly above the class or target method (for example, above onTick() or onSeeEnemy()) in your class body.");
        }
        if (c.tipo == Paquetes.Tipo.ENUM) {
            return id.elegir(
                    "Úsalo en comparaciones (if / switch) dentro de cadaTurno(), alVerEnemigo() o como campo en el cuerpo de tu clase.",
                    "In Vergleichen (if / switch) in jedenZug(), beiSicht() oder als Feld im Rumpf deiner Klasse verwenden.",
                    "Use it in comparisons (if / switch) inside onTick(), onSeeEnemy(), or as a field in your class body.");
        }
        if (esConstanteOCampo(firma)) {
            return id.elegir(
                    "Úsalo en cálculos y condiciones dentro de cadaTurno(), alVerEnemigo() o como valor inicial de un campo en tu clase.",
                    "In Berechnungen und Bedingungen in jedenZug(), beiSicht() oder als Startwert eines Felds im Klassenrumpf verwenden.",
                    "Use it in calculations and conditions inside onTick(), onSeeEnemy(), or as an initial field value in your class body.");
        }
        return id.elegir(
                "Úsalo en cadaTurno(), alEmpezar(), alVerEnemigo() o métodos auxiliares de tu robot (y sobre campos de clase si guardas estado).",
                "In jedenZug(), beimStart(), beiSicht() oder Hilfsmethoden deines Roboters nutzen (und auf Klassenfeldern für Zustand).",
                "Use it inside onTick(), onStart(), onSeeEnemy(), or helper methods of your robot (and on class fields when keeping state).");
    }

    private static String pseudocodigoGeneral(Paquetes.Componente c, String firma, Idioma id) {
        String primero = primerElemento(firma);
        String comp = c == null ? "Objeto" : c.nombre(id);
        if (c != null && "metajavbots.junit".equals(c.paquete)) {
            if (c.tipo == Paquetes.Tipo.ANOTACION) {
                return id.elegir(
                        "EN clase de pruebas:\n    " + primero + "\n    MÉTODO comprobarCaso():\n        verificar resultado esperado",
                        "IN Testklasse:\n    " + primero + "\n    METHODE prüfeFall():\n        erwartetes Ergebnis prüfen",
                        "IN test class:\n    " + primero + "\n    METHOD checkCase():\n        verify expected result");
            }
            return id.elegir(
                    "EN prueba @Test:\n    obtenido ← calcularValor()\n    " + primero + "  // comprueba el resultado",
                    "IN @Test-Methode:\n    istWert ← berechneWert()\n    " + primero + "  // prüft das Ergebnis",
                    "IN @Test method:\n    actual ← computeValue()\n    " + primero + "  // checks the result");
        }
        if (c != null && (c.tipo == Paquetes.Tipo.EXCEPCION || c.tipo == Paquetes.Tipo.ERROR)) {
            return id.elegir(
                    "INTENTAR:\n    procesar dato del turno\nSI ocurre " + comp + " e:\n    registrar e." + nombreSimple(primero),
                    "VERSUCHEN:\n    Zugdaten verarbeiten\nBEI " + comp + " e:\n    e." + nombreSimple(primero) + " ausgeben",
                    "TRY:\n    process tick data\nON " + comp + " e:\n    log e." + nombreSimple(primero));
        }
        if (c != null && (c.tipo == Paquetes.Tipo.ANOTACION || firma.startsWith("@"))) {
            return id.elegir(
                    "EN cuerpo de tu clase:\n    " + primero + "\n    CADA TURNO:\n        ejecutar lógica del robot",
                    "IM Klassenrumpf:\n    " + primero + "\n    JEDEN ZUG:\n        Roboterlogik ausführen",
                    "IN your class body:\n    " + primero + "\n    EVERY TICK:\n        run robot logic");
        }
        if (esConstanteOCampo(firma)) {
            return id.elegir(
                    "EN cadaTurno():\n    referencia ← " + primero + "\n    SI valor > referencia:\n        ajustar estrategia",
                    "IN jedenZug():\n    grenze ← " + primero + "\n    WENN wert > grenze:\n        Strategie anpassen",
                    "IN onTick():\n    limit ← " + primero + "\n    IF value > limit:\n        adjust strategy");
        }
        if (c != null && c.soloEjercicio) {
            return id.elegir(
                    "EN método del ejercicio:\n    usar " + primero + " de " + comp + "\n    devolver resultado comprobado",
                    "IN Übungsmethode:\n    " + primero + " von " + comp + " verwenden\n    geprüftes Ergebnis zurückgeben",
                    "IN exercise method:\n    use " + primero + " from " + comp + "\n    return verified result");
        }
        return id.elegir(
                "EN cadaTurno() o alVerEnemigo():\n    dato ← " + comp + ": " + primero
                        + "\n    SI dato sirve para decidir:\n        actuar con el resultado",
                "IN jedenZug() oder beiSicht():\n    wert ← " + comp + ": " + primero
                        + "\n    WENN wert für Entscheidung passt:\n        mit dem Ergebnis handeln",
                "IN onTick() or onSeeEnemy():\n    value ← " + comp + ": " + primero
                        + "\n    IF value helps decide:\n        act on the result");
    }

    private static String ajustarLineas(String texto, Idioma id, String comp, String firma) {
        String s = texto == null ? "" : texto.strip();
        if (s.isEmpty()) return pseudocodigoGeneral(null, firma, id);
        String[] lineas = s.split("\n");
        if (lineas.length > 5) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 5; i++) {
                if (i > 0) sb.append('\n');
                sb.append(lineas[i]);
            }
            return sb.toString();
        }
        if (lineas.length == 1) {
            String cabecera = id.elegir("EN cadaTurno():", "IN jedenZug():", "IN onTick():");
            return cabecera + "\n    " + s;
        }
        return s;
    }

    private static int contarLineas(String s) {
        return s == null || s.isEmpty() ? 0 : s.split("\n").length;
    }

    private static List<String> extraerMetodos(String firma) {
        List<String> l = new ArrayList<>();
        for (String parte : firma.split("/")) {
            int p = parte.indexOf('(');
            if (p <= 0) continue;
            String antes = parte.substring(0, p).strip();
            int esp = Math.max(antes.lastIndexOf(' '), antes.lastIndexOf('.'));
            String nom = esp >= 0 ? antes.substring(esp + 1).strip() : antes;
            if (!nom.isEmpty() && Character.isJavaIdentifierStart(nom.charAt(0))) l.add(nom);
        }
        return l;
    }

    private static boolean esConstanteOCampo(String firma) {
        return !firma.contains("(") && !firma.startsWith("@");
    }

    private static String primerElemento(String firma) {
        int slash = firma.indexOf(" / ");
        String primero = slash > 0 ? firma.substring(0, slash).strip() : firma.strip();
        int p = primero.indexOf('(');
        if (p > 0) {
            String antes = primero.substring(0, p).strip();
            int esp = antes.lastIndexOf(' ');
            if (esp >= 0) return antes.substring(esp + 1) + primero.substring(p);
        }
        return primero;
    }

    private static String nombreSimple(String expr) {
        int p = expr.indexOf('(');
        if (p > 0) return expr;
        return expr.contains(".") ? expr.substring(expr.lastIndexOf('.') + 1) : expr;
    }

    private static String capitalizarPunto(String s) {
        if (s == null || s.isBlank()) return "";
        String t = s.strip();
        String cap = t.substring(0, 1).toUpperCase(Locale.ROOT) + t.substring(1);
        char fin = cap.charAt(cap.length() - 1);
        return fin == '.' || fin == '!' || fin == '?' ? cap : cap + ".";
    }
}
