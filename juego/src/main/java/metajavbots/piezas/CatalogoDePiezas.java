package metajavbots.piezas;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import metajavbots.progreso.Etiqueta;
import metajavbots.textos.Trilingue;

/**
 * CATÁLOGO DE PIEZAS — Todas las piezas, leídas de res/datos/piezas.txt.
 * Una línea por pieza:
 *
 *   id | RANURA | nombre en español | en alemán | en inglés | clave=valor clave=valor …
 *
 * Con «serie=1-5» la línea da cinco piezas (Mk I … Mk V), cada una algo
 * mejor que la anterior y que se gana más tarde: así, con pocas líneas, hay
 * cientos de piezas. Añadir una pieza es añadir una línea (docs/PIEZAS.md).
 */
public final class CatalogoDePiezas {

    private static CatalogoDePiezas delJuego;

    private final Map<String, Pieza> porId = new LinkedHashMap<>();
    private final List<String> avisos = new ArrayList<>();

    /** Cuánto mejora cada valor por cada marca (Mk) de más: +10 % de daño, −4 % de enfriamiento… */
    private static final Map<String, Double> MEJORA_POR_MARCA = new LinkedHashMap<>();

    static {
        MEJORA_POR_MARCA.put("dano", 0.10);
        MEJORA_POR_MARCA.put("blindaje", 0.10);
        MEJORA_POR_MARCA.put("alcance", 0.03);
        MEJORA_POR_MARCA.put("enfriamiento", -0.04);
        MEJORA_POR_MARCA.put("energia", -0.03);
        MEJORA_POR_MARCA.put("velocidad", 0.04);
        MEJORA_POR_MARCA.put("giro", 0.03);
        MEJORA_POR_MARCA.put("radar", 0.04);
        MEJORA_POR_MARCA.put("cono", 0.02);
        MEJORA_POR_MARCA.put("pasos", 0.25);
        MEJORA_POR_MARCA.put("energiaMax", 0.08);
        MEJORA_POR_MARCA.put("recarga", 0.08);
        MEJORA_POR_MARCA.put("refuerzo", 0.20);
        MEJORA_POR_MARCA.put("duracion", 0.05);
        MEJORA_POR_MARCA.put("coste", -0.03);
        MEJORA_POR_MARCA.put("peso", 0.03);
        MEJORA_POR_MARCA.put("carga", 0.05);
    }

    private static final String[] ROMANOS = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};

    /** Las del juego (res/datos/piezas.txt), leídas una vez. */
    public static synchronized CatalogoDePiezas delJuego() {
        if (delJuego == null) {
            try (InputStream in = CatalogoDePiezas.class.getResourceAsStream("/datos/piezas.txt")) {
                if (in == null) throw new IllegalStateException("Falta res/datos/piezas.txt");
                delJuego = leer(in);
            } catch (IOException e) {
                throw new IllegalStateException("No se pudo leer res/datos/piezas.txt", e);
            }
        }
        return delJuego;
    }

    public static CatalogoDePiezas leer(InputStream in) throws IOException {
        CatalogoDePiezas c = new CatalogoDePiezas();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String linea;
            int n = 0;
            while ((linea = r.readLine()) != null) {
                n++;
                c.leerLinea(linea, n);
            }
        }
        return c;
    }

    public static CatalogoDePiezas deTexto(String texto) {
        CatalogoDePiezas c = new CatalogoDePiezas();
        String[] lineas = texto.split("\n");
        for (int i = 0; i < lineas.length; i++) c.leerLinea(lineas[i], i + 1);
        return c;
    }

    private void leerLinea(String linea, int n) {
        linea = linea.strip();
        if (linea.isEmpty() || linea.startsWith("#")) return;
        String[] partes = linea.split("\\|", -1);
        if (partes.length < 6) {
            avisos.add("línea " + n + ": faltan columnas");
            return;
        }
        String id = partes[0].strip();
        Pieza.Ranura ranura = Pieza.enumDe(Pieza.Ranura.class, partes[1], null);
        if (ranura == null) {
            avisos.add("línea " + n + ": ranura desconocida «" + partes[1].strip() + "»");
            return;
        }
        Trilingue nombre = new Trilingue(partes[2].strip(), partes[3].strip(), partes[4].strip());
        Map<String, Double> valores = new LinkedHashMap<>();
        Map<String, String> textos = new LinkedHashMap<>();
        for (String par : partes[5].strip().split("\\s+")) {
            if (par.isEmpty()) continue;
            int igual = par.indexOf('=');
            if (igual <= 0) {
                avisos.add("línea " + n + ": «" + par + "» no es clave=valor");
                continue;
            }
            String k = par.substring(0, igual), v = par.substring(igual + 1);
            try {
                valores.put(k, Double.parseDouble(v));
            } catch (NumberFormatException e) {
                textos.put(k, v);
            }
        }
        Naturaleza nat = Naturaleza.de(textos.remove("naturaleza"));
        Pieza.Rareza rareza = Pieza.enumDe(Pieza.Rareza.class, textos.remove("rareza"), Pieza.Rareza.COMUN);
        String desbloqueo = textos.getOrDefault("desbloqueo", "inicio");
        textos.remove("desbloqueo");
        Double precioObj = valores.remove("precio");
        double precioBase = precioObj == null ? 0 : Math.max(0, precioObj);
        String rangoTxt = textos.remove("rango");
        Etiqueta.Categoria rangoBase = rangoTxt == null ? null : Pieza.enumDe(Etiqueta.Categoria.class, rangoTxt, null);
        if (rangoTxt != null && rangoBase == null) avisos.add("línea " + n + ": rango desconocido «" + rangoTxt + "»");
        String serie = textos.remove("serie");
        int desde = 1, hasta = 1;
        if (serie != null) {
            String[] dh = serie.split("-");
            try {
                desde = Integer.parseInt(dh[0]);
                hasta = Integer.parseInt(dh[dh.length - 1]);
            } catch (NumberFormatException e) {
                avisos.add("línea " + n + ": serie «" + serie + "» no vale");
            }
        }
        for (int marca = desde; marca <= Math.min(hasta, ROMANOS.length - 1); marca++) {
            Map<String, Double> v = new LinkedHashMap<>();
            for (Map.Entry<String, Double> e : valores.entrySet()) {
                Double mejora = MEJORA_POR_MARCA.get(e.getKey());
                double x = e.getValue();
                if (mejora != null && marca > 1) x = x * (1 + mejora * (marca - 1));
                v.put(e.getKey(), redondear(x));
            }
            String idMarca = marca == 1 ? id : id + "_" + marca;
            Trilingue nom = serie == null ? nombre : new Trilingue(
                    nombre.es + " Mk " + ROMANOS[marca], nombre.de + " Mk " + ROMANOS[marca], nombre.en + " Mk " + ROMANOS[marca]);
            String desb = marca == 1 ? desbloqueo : desbloqueoDeMarca(desbloqueo, marca);
            double precioMarca = precioDeMarca(precioBase, desbloqueo, marca);
            Etiqueta.Categoria rangoMarca = precioMarca > 0 ? rangoDeMarca(rangoBase, marca) : null;
            if (porId.containsKey(idMarca)) avisos.add("línea " + n + ": «" + idMarca + "» repetida");
            porId.put(idMarca, new Pieza(idMarca, ranura, nom, nat, rareza, desb, marca, precioMarca, rangoMarca, v, textos));
        }
    }

    /** El precio en monedas sube con la marca (las piezas de inicio Mk I no se venden en la tienda). */
    static double precioDeMarca(double base, String desbloqueoBase, int marca) {
        if (base <= 0 || (marca == 1 && "inicio".equals(desbloqueoBase))) return 0;
        return Math.round(base * (1.0 + 0.5 * (marca - 1)) * 2.0) / 2.0;
    }

    /** El rango mínimo en tienda escala con las marcas más altas (Mk III-IV y Mk V). */
    static Etiqueta.Categoria rangoDeMarca(Etiqueta.Categoria base, int marca) {
        if (base == null) return null;
        if (marca >= 5) return Etiqueta.Categoria.SENIOR;
        if (marca >= 4 && base == Etiqueta.Categoria.JUNIOR) return Etiqueta.Categoria.SENIOR;
        if (marca >= 3 && base == Etiqueta.Categoria.NOOB) return Etiqueta.Categoria.JUNIOR;
        return base;
    }

    /** La Mk k de una serie se gana con más victorias que la anterior. */
    static String desbloqueoDeMarca(String base, int marca) {
        int extra = (marca - 1) * 5;
        if (base.equals("inicio")) return "victorias:" + (marca - 1) * 4;
        if (base.startsWith("victorias:") && !base.contains("+")) {
            try {
                return "victorias:" + (Integer.parseInt(base.substring(10)) + extra);
            } catch (NumberFormatException e) {
                return base;
            }
        }
        return base + "+victorias:" + extra;
    }

    private static double redondear(double x) {
        return Math.round(x * 100) / 100.0;
    }

    public Pieza pieza(String id) {
        return id == null ? null : porId.get(id.strip().toLowerCase(Locale.ROOT));
    }

    public List<Pieza> todas() {
        return Collections.unmodifiableList(new ArrayList<>(porId.values()));
    }

    public List<Pieza> de(Pieza.Ranura ranura) {
        List<Pieza> l = new ArrayList<>();
        for (Pieza p : porId.values()) if (p.ranura == ranura) l.add(p);
        return l;
    }

    /** Piezas comprables en la tienda (si rango es null, devuelve todas las comprables). */
    public List<Pieza> comprables(Etiqueta.Categoria rango) {
        List<Pieza> l = new ArrayList<>();
        for (Pieza p : porId.values()) {
            if (p.comprable() && (rango == null || p.rangoTienda == rango)) l.add(p);
        }
        return l;
    }

    public int cuantas() {
        return porId.size();
    }

    /** Lo que estaba mal escrito en el archivo (las pruebas exigen que esté vacío). */
    public List<String> avisos() {
        return Collections.unmodifiableList(avisos);
    }
}
