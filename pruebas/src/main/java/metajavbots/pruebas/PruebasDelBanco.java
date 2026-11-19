package metajavbots.pruebas;

import metajavbots.codigo.Compilador;
import metajavbots.codigo.RobotDelJugador;
import metajavbots.combate.Aparatos;
import metajavbots.combate.Arena;
import metajavbots.combate.Cuerpo;
import metajavbots.combate.Pelea;
import metajavbots.pantalla.VentanaDeCodigo;
import metajavbots.pantallas.BancoDePruebas;
import metajavbots.pantallas.Comun;
import metajavbots.progreso.Jugador;
import metajavbots.textos.Idioma;
import metajavbots.textos.T;

/**
 * PRUEBAS DEL BANCO — Comprueba los cuatro modos del banco de pruebas rápido del
 * taller, la pausa, el paso a paso, el reinicio, la limpieza y las traducciones.
 */
public final class PruebasDelBanco {

    private PruebasDelBanco() {
    }

    private static Jugador jugadorDePrueba(String nombre) {
        Jugador jug = new Jugador(nombre);
        Jugador.Robot r = new Jugador.Robot("Cobalto", "cobalto");
        r.codigo = Comun.plantilla(Idioma.ES);
        jug.robots.add(r);
        return jug;
    }

    public static void apuntar() {
        Pruebas.caso("banco: los cuatro modos usan combate/ sin desgaste ni progreso", PruebasDelBanco::probarCuatroModos);
        Pruebas.caso("banco: pausa, paso a paso, reinicio y limpieza", PruebasDelBanco::probarControles);
        Pruebas.caso("banco: robotitos rojo y azul se alternan al azar entre jugador y muñeco", PruebasDelBanco::probarColoresRobotitos);
        Pruebas.caso("banco: textos en es, de y en sin claves faltantes", PruebasDelBanco::probarTextos);
    }

    private static void probarCuatroModos() {
        Jugador jug = jugadorDePrueba("PruebaBanco");
        int historialAntes = jug.historial.size();
        long expAntes = jug.experiencia;
        Jugador.Robot r = jug.robot();
        RobotDelJugador probador = new RobotDelJugador();
        if (Compilador.hayCompilador()) {
            Compilador.Resultado res = probador.compilar(r.codigo, Idioma.ES.locale(), true);
            Pruebas.cierto(res.bien, "la plantilla debe compilar con traza");
            Pruebas.cierto(probador.conTraza(), "debe quedar marcado con traza");
        }
        BancoDePruebas banco = new BancoDePruebas();

        // 1. Quieto
        banco.modo(BancoDePruebas.Modo.QUIETO);
        banco.reiniciar(r, probador, null);
        Pelea p = banco.pelea();
        Pruebas.igual(1, p.rondas, "una sola ronda sin desgaste");
        Pruebas.igual(0.0, p.desgaste(), "sin desgaste");
        Pruebas.cierto(p.arena == Arena.NINGUNA, "sin obstáculos en modo quieto");
        Pruebas.falso(p.aparatos.activos, "sin aparatos en modo quieto");
        Cuerpo m0 = p.cuerpo(1);
        double x0 = m0.x, y0 = m0.y;
        for (int i = 0; i < 20; i++) banco.avanzarTiempo(0.05f);
        Pruebas.cierto(Math.hypot(m0.x - x0, m0.y - y0) < 1.0, "el muñeco quieto no se desplaza solo");
        if (Compilador.hayCompilador()) {
            VentanaDeCodigo editor = new VentanaDeCodigo();
            banco.marcarLineas(editor);
            Pruebas.cierto(!editor.lineasActivas.isEmpty() && editor.lineaActiva > 0, "debe resaltar las líneas ejecutadas");
        }

        // 2. En movimiento
        banco.modo(BancoDePruebas.Modo.MOVIMIENTO);
        banco.reiniciar(r, probador, null);
        p = banco.pelea();
        Cuerpo m1 = p.cuerpo(1);
        double mx0 = m1.x, my0 = m1.y;
        for (int i = 0; i < 30; i++) banco.avanzarTiempo(0.05f);
        Pruebas.cierto(Math.hypot(m1.x - mx0, m1.y - my0) > 20.0, "el muñeco en movimiento debe desplazarse");

        // 3. Con obstáculo
        banco.modo(BancoDePruebas.Modo.OBSTACULO);
        banco.reiniciar(r, probador, null);
        p = banco.pelea();
        Pruebas.cierto(!p.arena.trampas().isEmpty(), "el modo con obstáculo añade trampas/objetos de arena");

        // 4. Con aparato
        banco.modo(BancoDePruebas.Modo.APARATO);
        banco.reiniciar(r, probador, null);
        p = banco.pelea();
        Pruebas.cierto(p.aparatos.activos, "el modo con aparato activa los aparatos");
        for (int i = 0; i < Aparatos.SALE + 5; i++) banco.paso();
        Pruebas.cierto(!p.aparatos.todos().isEmpty(), "tras Aparatos.SALE turnos debe aparecer un aparato");

        // Sin afectar historial ni experiencia del jugador
        Pruebas.igual(historialAntes, jug.historial.size(), "no altera el historial");
        Pruebas.igual(expAntes, jug.experiencia, "no altera la experiencia");
        banco.limpiar();
        Pruebas.falso(banco.activo(), "limpiar deja el banco inactivo");
    }

    private static void probarControles() {
        Jugador jug = jugadorDePrueba("PruebaPausa");
        BancoDePruebas banco = new BancoDePruebas();
        banco.reiniciar(jug.robot(), null, null);
        banco.alternarPausa();
        Pruebas.cierto(banco.pausado(), "debe quedar en pausa");
        int t0 = banco.pelea().turno();
        banco.avanzarTiempo(0.5f);
        Pruebas.igual(t0, banco.pelea().turno(), "en pausa no avanza con el tiempo");
        banco.paso();
        Pruebas.igual(t0 + 1, banco.pelea().turno(), "paso a paso avanza exactamente un turno");
        Pruebas.cierto(banco.pausado(), "paso a paso mantiene la pausa");
        banco.reiniciar(jug.robot(), null, null);
        Pruebas.igual(0, banco.pelea().turno(), "reiniciar vuelve al turno 0");
        Pruebas.falso(banco.pausado(), "reiniciar quita la pausa");
        banco.masLento();
        Pruebas.cierto(banco.velocidad() < 1f, "masLento reduce la velocidad");
        banco.avanzarTiempo(0.1f);
        int turnosLento = banco.pelea().turno();
        banco.masRapido();
        banco.masRapido();
        Pruebas.cierto(banco.velocidad() > 1f, "masRapido sube la velocidad");
        banco.reiniciar(jug.robot(), null, null);
        banco.avanzarTiempo(0.1f);
        Pruebas.cierto(banco.pelea().turno() > turnosLento, "más velocidad avanza más turnos en el mismo tiempo");
        banco.limpiar();
        Pruebas.cierto(banco.pelea() == null, "limpiar libera la pelea");
    }

    private static void probarColoresRobotitos() {
        Jugador jug = jugadorDePrueba("PruebaColores");
        BancoDePruebas banco = new BancoDePruebas();
        boolean vioAzul = false;
        boolean vioRojo = false;
        for (int i = 0; i < 40; i++) {
            banco.reiniciar(jug.robot(), null, null);
            int cj = banco.colorJugador();
            int cm = banco.colorMuneco();
            Pruebas.cierto(cj != cm, "jugador y muñeco deben tener colores distintos");
            Pruebas.cierto(cj == BancoDePruebas.COLOR_AZUL || cj == BancoDePruebas.COLOR_ROJO, "el jugador es azul o rojo");
            Pruebas.cierto(cm == BancoDePruebas.COLOR_AZUL || cm == BancoDePruebas.COLOR_ROJO, "el muñeco es azul o rojo");
            if (cj == BancoDePruebas.COLOR_AZUL) vioAzul = true;
            if (cj == BancoDePruebas.COLOR_ROJO) vioRojo = true;
        }
        Pruebas.cierto(vioAzul && vioRojo, "el reparto entre rojo y azul debe alternar aleatoriamente");
    }

    private static void probarTextos() {
        String[] claves = {"taller.probar_aqui", "taller.probar_aqui_ayuda", "banco.titulo", "banco.quieto",
            "banco.movimiento", "banco.obstaculo", "banco.aparato", "banco.reiniciar", "banco.pausa",
            "banco.seguir", "banco.paso", "banco.cerrar", "banco.muneco", "banco.turno", "banco.en_pausa",
            "banco.terminada", "banco.compilando", "banco.sin_compilar", "banco.pista", "banco.aparato_prox",
            "banco.mas_lento", "banco.mas_rapido", "banco.velocidad_ayuda"};
        Idioma antes = T.idioma;
        try {
            for (Idioma id : Idioma.values()) {
                T.idioma = id;
                for (String c : claves) {
                    String v = T.t(c);
                    Pruebas.cierto(v != null && !v.isBlank() && !v.equals(c), "falta traducción " + c + " en " + id);
                }
            }
        } finally {
            T.idioma = antes;
        }
    }
}
