package metajavbots.pruebas;

import java.lang.management.ManagementFactory;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import com.sun.management.ThreadMXBean;

import metajavbots.combate.Arena;
import metajavbots.combate.Combatiente;
import metajavbots.combate.Pelea;
import metajavbots.combate.Suceso;
import metajavbots.piezas.CatalogoDePiezas;
import metajavbots.piezas.Modelo;
import metajavbots.piezas.Montaje;
import metajavbots.piezas.Pieza;
import metajavbots.rivales.Rivales;

/**
 * PRUEBAS DE OPTIMIZACIÓN — Red de seguridad antes y después de optimizar:
 * 1) Peleas de referencia con semillas fijas (modelos, armas/escudos de Tienda,
 *    aparatos, arena con trampas/obstáculos destructibles, Senior con presupuesto
 *    y robots en paralelo) que deben producir resultados idénticos.
 * 2) Tanda fija de peleas simuladas con medición de tiempo y sin hilos colgados.
 * 3) Vigilancia de asignaciones en el bucle de simulación.
 */
final class PruebasDeOptimizacion {

    private PruebasDeOptimizacion() {
    }

    static void apuntar() {
        Pruebas.caso("optimización: peleas de referencia deterministas (estándar, tienda, aparatos, trampas, Senior)",
                PruebasDeOptimizacion::peleasDeReferencia);
        Pruebas.caso("optimización: pelea Senior en paralelo cierra sus hilos al terminar y al rendirse",
                PruebasDeOptimizacion::hilosParalelosLimpios);
        Pruebas.caso("optimización: tanda fija de 20 peleas simuladas sin ventana (rendimiento)",
                PruebasDeOptimizacion::tandaDePeleasSimuladas);
        Pruebas.caso("optimización: recursos liberados (Voz perezosa, Sonidos, TextoEnriquecido cacheado, LectorRes)",
                PruebasDeOptimizacion::recursosYUtilidadesComunes);
    }

    private static void peleasDeReferencia() {
        StringBuilder firma = new StringBuilder();
        firma.append("E1=").append(resumenPelea(peleaEstandar(101L, 0, 5, 4, "HANGAR", false, false))).append('\n');
        firma.append("E2=").append(resumenPelea(peleaEstandar(202L, 3, 12, 6, "CHATARRERIA", true, false))).append('\n');
        firma.append("E3=").append(resumenPelea(peleaConTiendaYEscudos(303L))).append('\n');
        firma.append("E4=").append(resumenPelea(peleaEstandar(404L, 16, 19, 4, "TEJADO", true, true))).append('\n');
        firma.append("E5=").append(resumenPelea(peleaRoyale(505L))).append('\n');
        String esperada = REFERENCIA_ESPERADA;
        if (!esperada.isEmpty()) {
            Pruebas.igual(esperada, firma.toString(), "huella de peleas de referencia");
        } else {
            System.out.println("=== REFERENCIA ===\n" + firma + "=== FIN ===");
            Pruebas.cierto(firma.length() > 100, "huella generada");
        }
    }

    /**
     * Huella exacta de las peleas de referencia. Regenerada (23-nov-2026) tras el equilibrio de peso, carga y locomoción:
     * cambian las cifras de movilidad, los choques, el retroceso, los datos de piezas y la IA del rival a propósito
     * (docs/OPTIMIZACION.md, «Peleas de referencia regeneradas»; docs/EQUILIBRIO.md).
     */
    private static final String REFERENCIA_ESPERADA = ""
            + "E1=g=0,r=4|c0:738.76/1123.54;256.0:817;141.9:290;228.2:554;112.7:319"
            + "|c1:1123.54/725.66;294.4:817;250.4:290;284.4:554;294.4:319|ev=724#218299eef856bb5\n"
            + "E2=g=0,r=6|c0:531.33/1185.29;67.0:182;165.3:1200;95.4:288;77.6:324;44.7:238;81.3:278"
            + "|c1:1234.84/468.23;223.6:182;151.7:1200;266.7:288;225.9:324;214.1:238;152.9:278|ev=1619#507f54685e60318\n"
            + "E3=g=0,r=4|c0:498.99/1028.57;104.0:418;193.5:1200;84.8:1054;116.6:245"
            + "|c1:1034.18/296.48;292.2:418;231.3:1200;224.9:1054;285.8:245|ev=904#8e7c18d7ee431e6f\n"
            + "E4=g=1,r=4|c0:909.14/352.89;352.4:400;221.6:335;186.2:1200;148.9:601"
            + "|c1:384.81/801.02;6.9:400;59.5:335;19.3:1200;299.1:601|ev=710#532598675ea8e1f\n"
            + "E5=g=4,r=2|c0:839.30/585.75;359.7:457;479.6:1200|c1:665.36/965.97;347.0:457;318.4:1200"
            + "|c2:673.54/676.97;569.4:457;104.1:1200|c3:587.50/392.41;296.5:457;291.0:1200"
            + "|c4:227.93/143.41;153.1:457;74.8:1200|c5:402.15/573.53;252.9:457;149.3:1200|ev=1467#863fc308ae1adad7\n";

    private static Pelea peleaEstandar(long semilla, int mA, int mB, int rondas, String escenario,
                                       boolean aparatos, boolean senior) {
        List<Modelo> ms = Modelo.todos();
        Random r = new Random(semilla);
        Combatiente a = Rivales.crear(ms.get(mA % ms.size()), 0.7, 0.7, r);
        Combatiente b = Rivales.crear(ms.get(mB % ms.size()), 0.7, 0.7, r);
        Pelea p = new Pelea(a, b, semilla);
        p.rondas = rondas;
        p.arena = Arena.de(escenario, p.ancho, p.alto);
        p.aparatos.activos = aparatos;
        if (senior) {
            p.presupuestoNanosPorTurno = 5_000_000L;
            p.presupuestoBytesPorTurno = 16_384L;
        }
        p.jugarEntera();
        return p;
    }

    private static Pelea peleaConTiendaYEscudos(long semilla) {
        List<Modelo> ms = Modelo.todos();
        CatalogoDePiezas cat = CatalogoDePiezas.delJuego();
        Random r = new Random(semilla);
        Combatiente a = Rivales.crear(ms.get(1), 0.75, 0.75, r);
        Combatiente b = Rivales.crear(ms.get(8), 0.75, 0.75, r);
        ponerSiExiste(a.montaje, Montaje.Hueco.DERECHA, cat.pieza("blaster_iones"));
        ponerSiExiste(a.montaje, Montaje.Hueco.IZQUIERDA, cat.pieza("escudo_reflector"));
        ponerSiExiste(b.montaje, Montaje.Hueco.DERECHA, cat.pieza("sable_fotonico"));
        ponerSiExiste(b.montaje, Montaje.Hueco.IZQUIERDA, cat.pieza("escudo_choque"));
        Pelea p = new Pelea(a, b, semilla);
        p.rondas = 4;
        p.aparatos.activos = true;
        p.jugarEntera();
        return p;
    }

    private static Pelea peleaRoyale(long semilla) {
        List<Modelo> ms = Modelo.todos();
        Random r = new Random(semilla);
        Combatiente[] cs = new Combatiente[6];
        int[] eq = new int[6];
        for (int i = 0; i < 6; i++) {
            cs[i] = Rivales.crear(ms.get((i * 3) % ms.size()), 0.65, 0.65, r);
            eq[i] = i;
        }
        Pelea p = new Pelea(cs, eq, semilla);
        p.rondas = 2;
        p.jugarEntera();
        return p;
    }

    private static void ponerSiExiste(Montaje m, Montaje.Hueco h, Pieza p) {
        if (p != null) m.poner(h, p);
    }

    private static String resumenPelea(Pelea p) {
        StringBuilder sb = new StringBuilder();
        sb.append("g=").append(p.ganador()).append(",r=").append(p.ronda());
        for (int i = 0; i < p.combatientes.length; i++) {
            Combatiente c = p.combatientes[i];
            sb.append("|c").append(i).append(':')
                    .append(String.format(Locale.ROOT, "%.2f/%.2f", c.danoTotalRecibido, c.danoTotalHecho));
            for (Combatiente.Informe inf : c.informes) {
                sb.append(';').append(String.format(Locale.ROOT, "%.1f:%d", inf.danoRecibido, inf.turnos));
            }
        }
        long hashEv = 17L;
        int nEv = 0;
        for (Suceso s : p.historia()) {
            if (s.tipo == Suceso.Tipo.TOPE && "tiempo".equals(s.texto)) continue;
            hashEv = hashEv * 31L + s.tipo.ordinal();
            hashEv = hashEv * 31L + s.quien;
            hashEv = hashEv * 31L + s.turno;
            hashEv = hashEv * 31L + Math.round(s.valor * 10.0);
            nEv++;
        }
        sb.append("|ev=").append(nEv).append('#').append(Long.toHexString(hashEv));
        return sb.toString();
    }

    private static void hilosParalelosLimpios() throws InterruptedException {
        List<Modelo> ms = Modelo.todos();
        Random r = new Random(777L);
        Combatiente[] cs = {
            Rivales.crear(ms.get(0), 0.7, 0.7, r),
            Rivales.crear(ms.get(1), 0.7, 0.7, r),
            Rivales.crear(ms.get(2), 0.7, 0.7, r),
            Rivales.crear(ms.get(3), 0.7, 0.7, r)
        };
        Pelea p = new Pelea(cs, new int[]{0, 0, 1, 1}, 777L);
        p.rondas = 2;
        p.pensarEnParalelo = true;
        p.jugarEntera();
        Pruebas.cierto(p.estado() == Pelea.Estado.TERMINADA, "la pelea en paralelo debe terminar");
        esperarHilosParalelos();
        Pruebas.igual(0, contarHilosParalelos(), "ningún hilo metajavbots-paralelo-* vivo al terminar");

        Pelea p2 = new Pelea(cs, new int[]{0, 0, 1, 1}, 778L);
        p2.rondas = 2;
        p2.pensarEnParalelo = true;
        p2.empezarRonda();
        for (int i = 0; i < 15; i++) p2.avanzar();
        p2.rendirse(0);
        esperarHilosParalelos();
        Pruebas.igual(0, contarHilosParalelos(), "ningún hilo metajavbots-paralelo-* vivo al rendirse");
    }

    private static void esperarHilosParalelos() throws InterruptedException {
        for (int i = 0; i < 20 && contarHilosParalelos() > 0; i++) Thread.sleep(25);
    }

    private static int contarHilosParalelos() {
        int vivos = 0;
        for (Thread t : Thread.getAllStackTraces().keySet()) {
            if (t.isAlive() && t.getName().startsWith("metajavbots-paralelo-")) vivos++;
        }
        return vivos;
    }

    private static void tandaDePeleasSimuladas() {
        java.lang.management.ThreadMXBean base = ManagementFactory.getThreadMXBean();
        ThreadMXBean mx = base instanceof ThreadMXBean ? (ThreadMXBean) base : null;
        if (mx != null && mx.isThreadAllocatedMemorySupported()) mx.setThreadAllocatedMemoryEnabled(true);
        List<Modelo> ms = Modelo.todos();
        long t0 = System.nanoTime();
        long b0 = mx != null ? mx.getCurrentThreadAllocatedBytes() : 0;
        int turnosTotales = 0;
        for (int i = 0; i < 20; i++) {
            Random r = new Random(1000L + i);
            Combatiente a = Rivales.crear(ms.get(i % ms.size()), 0.65, 0.65, r);
            Combatiente b = Rivales.crear(ms.get((i + 7) % ms.size()), 0.65, 0.65, r);
            Pelea p = new Pelea(a, b, 1000L + i);
            p.rondas = 4;
            p.aparatos.activos = (i % 2 == 0);
            p.arena = Arena.de(i % 3 == 0 ? "CHATARRERIA" : "HANGAR", p.ancho, p.alto);
            p.jugarEntera();
            for (Combatiente.Informe inf : a.informes) turnosTotales += inf.turnos;
        }
        long b1 = mx != null ? mx.getCurrentThreadAllocatedBytes() : 0;
        long msElapsed = (System.nanoTime() - t0) / 1_000_000L;
        long bytesPorTurno = turnosTotales > 0 ? (b1 - b0) / turnosTotales : 0;
        System.out.printf(Locale.ROOT,
                "            tanda 20 peleas: %d ms (%d turnos, %d B/turno)%n", msElapsed, turnosTotales, bytesPorTurno);
        Pruebas.cierto(turnosTotales > 0, "la tanda simuló turnos");
        if (mx != null) {
            Pruebas.cierto(bytesPorTurno < 3800,
                    "asignaciones por turno por debajo del umbral optimizado (< 3800 B/turno): " + bytesPorTurno);
        }
    }

    private static void recursosYUtilidadesComunes() throws Exception {
        // 1) Voz no crea hilo hasta usarse y libera limpiamente.
        metajavbots.sonido.Voz voz = new metajavbots.sonido.Voz();
        voz.activa = false;
        voz.liberar();
        // 2) Sonidos sin audio libera sin fallos ni fugas.
        metajavbots.sonido.Sonidos son = new metajavbots.sonido.Sonidos();
        son.tocar("clic", 0.5f);
        son.liberar();
        // 3) TextoEnriquecido cachea aTrozos y devuelve la misma instancia sin reasignar.
        var t1 = metajavbots.pantalla.TextoEnriquecido.aTrozos("Pulsa [[Compilar]] ahora", false);
        var t2 = metajavbots.pantalla.TextoEnriquecido.aTrozos("Pulsa [[Compilar]] ahora", false);
        Pruebas.cierto(t1 == t2, "TextoEnriquecido.aTrozos debe reutilizar la lista cacheada");
        // 4) Banco.deTema y Banco.todos reutilizan listas cacheadas sin copiar cada fotograma.
        var b = metajavbots.ejercicios.Banco.delJuego();
        Pruebas.cierto(b.deTema("01") == b.deTema("01"), "Banco.deTema debe reutilizar su lista inmutable");
        Pruebas.cierto(b.todos() == b.todos(), "Banco.todos debe reutilizar su lista inmutable");
        // 5) LectorRes unificado: trilingüe, números, normalización sin tildes y secciones.
        var tr = metajavbots.textos.LectorRes.trilingue("Hola | Hallo | Hello", true);
        Pruebas.igual("Hola", tr.es, "LectorRes es");
        Pruebas.igual("Hallo", tr.de, "LectorRes de");
        Pruebas.igual("Hello", tr.en, "LectorRes en");
        Pruebas.igual(42, metajavbots.textos.LectorRes.entero("42", 0), "LectorRes entero");
        Pruebas.igual(7, metajavbots.textos.LectorRes.entero("x", 7), "LectorRes entero defecto");
        Pruebas.igual("strasse o", metajavbots.textos.LectorRes.sinTildes("Straße Ó"), "LectorRes sinTildes");
    }
}
