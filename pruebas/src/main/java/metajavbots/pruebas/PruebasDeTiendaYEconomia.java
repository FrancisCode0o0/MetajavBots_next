package metajavbots.pruebas;

import static metajavbots.pruebas.Pruebas.caso;
import static metajavbots.pruebas.Pruebas.cierto;
import static metajavbots.pruebas.Pruebas.falso;
import static metajavbots.pruebas.Pruebas.igual;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import metajavbots.api.Metajavbot;
import metajavbots.codigo.Compilador;
import metajavbots.combate.Aparatos;
import metajavbots.combate.Armas;
import metajavbots.combate.Combatiente;
import metajavbots.combate.Cuerpo;
import metajavbots.combate.DificultadCarrera;
import metajavbots.combate.Pelea;
import metajavbots.ejercicios.Comprobador;
import metajavbots.ejercicios.Ejercicio;
import metajavbots.ejercicios.RetoDePieza;
import metajavbots.piezas.CatalogoDePiezas;
import metajavbots.piezas.Modelo;
import metajavbots.piezas.Montaje;
import metajavbots.piezas.Pieza;
import metajavbots.progreso.Etiqueta;
import metajavbots.progreso.Jugador;
import metajavbots.progreso.Progreso;
import metajavbots.textos.Idioma;
import metajavbots.textos.T;
import metajavbots.tutor.Concepto;
import metajavbots.tutor.ConocimientoDelEntrenador;

/**
 * Pruebas deterministas del catálogo ampliado de la Tienda (armas de energía/distancia,
 * escudos temporales con recarga lenta y protección tipificada, espadas láser y armas
 * contundentes por ciclos), la curva económica por rangos y la paridad trilingüe.
 */
final class PruebasDeTiendaYEconomia {

    private static final String[] NUEVAS_DISTANCIA = {
        "micro_haz", "blaster_iones", "canon_solar", "canon_electromagnetico"
    };
    private static final String[] NUEVOS_ESCUDOS = {
        "escudo_ligero", "escudo_reflector", "escudo_choque", "escudo_egida"
    };
    private static final String[] NUEVAS_CICLO = {
        "sable_fotonico", "maza_impacto", "sable_termico", "ariete_cinetico"
    };

    private PruebasDeTiendaYEconomia() {
    }

    static void apuntar() {
        apuntarCatalogoYRangos();
        apuntarCompensacionesDistanciaYEscudos();
        apuntarCompensacionesCiclos();
        apuntarCurvaEconomica();
        apuntarParidadTrilingueYEntrenador();
    }

    private static void apuntarCatalogoYRangos() {
        caso("tienda economía: nuevas familias por rangos sin colisiones con armas ni aparatos", () -> {
            CatalogoDePiezas cat = CatalogoDePiezas.delJuego();
            cierto(cat.avisos().isEmpty(), "sin avisos en piezas.txt: " + cat.avisos());

            Set<String> nombresAparatos = new HashSet<>();
            for (Aparatos.Tipo tipo : Aparatos.Tipo.values()) {
                nombresAparatos.add(tipo.id());
                nombresAparatos.add(tipo.nombre.toLowerCase(Locale.ROOT));
            }

            Set<String> idsNuevos = new HashSet<>();
            Etiqueta.Categoria[] rangos = {
                Etiqueta.Categoria.NOOB, Etiqueta.Categoria.JUNIOR,
                Etiqueta.Categoria.SENIOR, Etiqueta.Categoria.MASTER
            };
            String[][] grupos = {NUEVAS_DISTANCIA, NUEVOS_ESCUDOS, NUEVAS_CICLO};
            for (String[] grupo : grupos) {
                for (int i = 0; i < grupo.length; i++) {
                    Pieza p = cat.pieza(grupo[i]);
                    cierto(p != null, "existe pieza nueva: " + grupo[i]);
                    cierto(p.comprable(), "comprable en tienda: " + p.id);
                    igual(rangos[i], p.rangoTienda, "rango correcto para " + p.id);
                    falso(p.id.contains("martillo"), "no usa nombre martillo: " + p.id);
                    falso(p.nombre(Idioma.ES).toLowerCase(Locale.ROOT).contains("martillo"), "sin martillo en ES: " + p.id);
                    falso(nombresAparatos.contains(p.id), "no colisiona con id de aparato: " + p.id);
                    falso(nombresAparatos.contains(p.nombre(Idioma.ES).toLowerCase(Locale.ROOT)), "no colisiona con nombre de aparato: " + p.id);
                    idsNuevos.add(p.id);
                }
            }

            Set<String> nombresExistentes = new HashSet<>();
            for (Pieza p : cat.todas()) {
                if (!idsNuevos.contains(p.id)) nombresExistentes.add(p.nombre(Idioma.ES).toLowerCase(Locale.ROOT));
            }
            for (String id : idsNuevos) {
                Pieza p = cat.pieza(id);
                falso(nombresExistentes.contains(p.nombre(Idioma.ES).toLowerCase(Locale.ROOT)),
                        "nombre único respecto al catálogo previo: " + p.nombre(Idioma.ES));
            }

            // Desbloqueo del rango Especial y activación con reto de código.
            Jugador j = new Jugador("Especialista");
            j.etiqueta = Etiqueta.SENIOR_4;
            j.sumarMonedas(200);
            Pieza especial = cat.pieza("canon_electromagnetico");
            falso(Progreso.rangoDesbloqueadoEnTienda(j, Etiqueta.Categoria.MASTER), "Especial bloqueado sin jefe ni Master");
            igual(Progreso.CompraResultado.RANGO_INSUFICIENTE, Progreso.comprarPieza(j, especial), "bloquea compra Especial");
            j.jefesVencidos.add("herencia");
            cierto(Progreso.rangoDesbloqueadoEnTienda(j, Etiqueta.Categoria.MASTER), "Especial desbloqueado tras jefe");
            igual(Progreso.CompraResultado.COMPRADA, Progreso.comprarPieza(j, especial), "permite comprar Especial");
            cierto(Progreso.piezaPendiente(j, especial), "pendiente de reto de código");

            if (Compilador.hayCompilador()) {
                for (String id : idsNuevos) {
                    Ejercicio reto = RetoDePieza.para(cat.pieza(id));
                    Comprobador.Informe sol = Comprobador.comprobar(reto, reto.codigoSolucion(), Locale.ROOT);
                    cierto(sol.todoBien(), "reto de activación pasa en " + id + ": " + sol.errores);
                }
            }
        });
    }

    private static void apuntarCompensacionesDistanciaYEscudos() {
        caso("tienda economía: potencia vs carga en armas a distancia y energía", () -> {
            CatalogoDePiezas cat = CatalogoDePiezas.delJuego();
            for (int i = 1; i < NUEVAS_DISTANCIA.length; i++) {
                Pieza ligera = cat.pieza(NUEVAS_DISTANCIA[i - 1]);
                Pieza pesada = cat.pieza(NUEVAS_DISTANCIA[i]);
                cierto(pesada.valor("dano") > ligera.valor("dano"),
                        pesada.id + " debe pegar más que " + ligera.id);
                cierto(pesada.valor("enfriamiento") > ligera.valor("enfriamiento"),
                        pesada.id + " debe enfriar más lento que " + ligera.id);
                cierto(pesada.valor("energia") > ligera.valor("energia"),
                        pesada.id + " debe gastar más energía que " + ligera.id);
            }
        });

        caso("tienda economía: escudos temporales con protección por tipo y recarga lenta", () -> {
            CatalogoDePiezas cat = CatalogoDePiezas.delJuego();
            for (int i = 1; i < NUEVOS_ESCUDOS.length; i++) {
                Pieza menor = cat.pieza(NUEVOS_ESCUDOS[i - 1]);
                Pieza mayor = cat.pieza(NUEVOS_ESCUDOS[i]);
                cierto(mayor.valor("bloqueo") > menor.valor("bloqueo"),
                        mayor.id + " bloquea más que " + menor.id);
                cierto(mayor.valor("enfriamiento") > menor.valor("enfriamiento"),
                        mayor.id + " recarga más lento que " + menor.id);
            }

            // Efecto real por tipo de protección: antiproyectil vs antichoque vs integral.
            Pieza refl = cat.pieza("escudo_reflector");
            Pieza choq = cat.pieza("escudo_choque");
            Pieza egid = cat.pieza("escudo_egida");
            igual(Armas.TipoProteccion.PROYECTIL, Armas.tipoProteccion(refl), "reflector es PROYECTIL");
            igual(Armas.TipoProteccion.IMPACTO, Armas.tipoProteccion(choq), "choque es IMPACTO");
            igual(Armas.TipoProteccion.INTEGRAL, Armas.tipoProteccion(egid), "egida es INTEGRAL");

            double bReflLaser = Armas.bloqueoEfectivo(refl.valor("bloqueo"), Armas.tipoProteccion(refl), "laser", false);
            double bReflCuerpo = Armas.bloqueoEfectivo(refl.valor("bloqueo"), Armas.tipoProteccion(refl), "cuerpo", true);
            cierto(bReflLaser > bReflCuerpo * 2.0, "reflector protege mucho más contra láser que contra cuerpo");

            double bChoqCuerpo = Armas.bloqueoEfectivo(choq.valor("bloqueo"), Armas.tipoProteccion(choq), "cuerpo", true);
            double bChoqLaser = Armas.bloqueoEfectivo(choq.valor("bloqueo"), Armas.tipoProteccion(choq), "laser", false);
            cierto(bChoqCuerpo > bChoqLaser * 2.0, "escudo de choque protege mucho más contra impacto que contra láser");

            // Combate real determinista: activar escudo temporal reduce daño y luego entra en recarga lenta.
            double danoSinEscudo = simularDanoLaser(null);
            double danoConReflector = simularDanoLaser("escudo_reflector");
            cierto(danoConReflector < danoSinEscudo * 0.55,
                    "reflector activo reduce claramente el daño láser (" + danoConReflector + " vs " + danoSinEscudo + ")");
        });
    }

    private static void apuntarCompensacionesCiclos() {
        caso("tienda economía: espadas láser y contundentes por ciclos y daño a obstáculos", () -> {
            CatalogoDePiezas cat = CatalogoDePiezas.delJuego();
            Pieza sable = cat.pieza("sable_fotonico");
            Pieza ariete = cat.pieza("ariete_cinetico");
            cierto(Cuerpo.cargasDePieza(sable) > Cuerpo.cargasDePieza(ariete), "sable ligero tiene más golpes por ciclo que ariete");
            cierto(ariete.valor("dano") > sable.valor("dano"), "ariete pega más fuerte que sable ligero");
            cierto(ariete.valor("enfriamientoCiclo") > sable.valor("enfriamientoCiclo"), "ariete exige enfriamiento de ciclo más largo");

            Montaje m = new Montaje(Modelo.de("cobalto"));
            m.poner(Montaje.Hueco.IZQUIERDA, ariete);
            Montaje.Cifras cf = m.cifras();
            double danoRobot = Armas.danoDeArma(m, cf, ariete, 1.0);
            double danoObs = Armas.danoAObstaculo(m, cf, ariete, 1.0);
            cierto(danoObs > danoRobot && danoObs <= Armas.MAX_DANO_EFECTIVO,
                    "ariete potencia destrucción de obstáculos dentro de límites: " + danoObs + " vs " + danoRobot);

            // Simulación real del ciclo de cargas y enfriamiento largo en Pelea.
            Montaje mSable = new Montaje(Modelo.de("cobalto"));
            mSable.poner(Montaje.Hueco.IZQUIERDA, sable);
            Combatiente a = new Combatiente("Sable", mSable, () -> new RobotAtacanteIzquierda());
            Combatiente b = new Combatiente("Diana", new Montaje(Modelo.de("cobalto")), () -> new RobotQuieto());
            Pelea p = new Pelea(a, b, 42L);
            p.rondas = 1;
            p.empezarRonda();
            Cuerpo ca = p.cuerpo(0);
            igual(4, ca.cargasMaximas(Cuerpo.IZQUIERDA), "sable fotónico tiene 4 cargas por ciclo");
            igual(4, ca.cargasRestantes(Cuerpo.IZQUIERDA), "empieza con 4 cargas");

            int enfriamientoCorto = 0;
            int enfriamientoLargo = 0;
            boolean entroEnCiclo = false;
            boolean recargoCiclo = false;
            for (int t = 0; t < 160; t++) {
                p.avanzar();
                int enf = ca.enfriamiento(Cuerpo.IZQUIERDA);
                int carg = ca.cargasRestantes(Cuerpo.IZQUIERDA);
                if (carg > 0 && enf > 0) enfriamientoCorto = Math.max(enfriamientoCorto, enf);
                if (carg == 0 && enf > 0) {
                    entroEnCiclo = true;
                    enfriamientoLargo = Math.max(enfriamientoLargo, enf);
                }
                if (entroEnCiclo && carg == 4 && enf == 0) {
                    recargoCiclo = true;
                    break;
                }
            }
            cierto(entroEnCiclo, "agotó las 4 cargas y entró en enfriamiento de ciclo");
            cierto(enfriamientoLargo > enfriamientoCorto * 3,
                    "enfriamiento de ciclo mucho mayor que el corto (" + enfriamientoLargo + " vs " + enfriamientoCorto + ")");
            cierto(recargoCiclo, "tras acabar el enfriamiento largo, recarga las 4 cargas del ciclo");
        });
    }

    private static void apuntarCurvaEconomica() {
        caso("tienda economía: precios medios crecientes por rango y proporción jugable frente a ingresos", () -> {
            CatalogoDePiezas cat = CatalogoDePiezas.delJuego();
            double mediaNoob = mediaDePrecios(cat.comprables(Etiqueta.Categoria.NOOB));
            double mediaJunior = mediaDePrecios(cat.comprables(Etiqueta.Categoria.JUNIOR));
            double mediaSenior = mediaDePrecios(cat.comprables(Etiqueta.Categoria.SENIOR));
            double mediaEspecial = mediaDePrecios(cat.comprables(Etiqueta.Categoria.MASTER));

            cierto(mediaNoob > 0 && mediaJunior > 0 && mediaSenior > 0 && mediaEspecial > 0, "todos los rangos tienen piezas");
            cierto(mediaNoob < mediaJunior && mediaJunior < mediaSenior && mediaSenior < mediaEspecial,
                    String.format(Locale.ROOT, "crecimiento estricto: Noob=%.2f < Junior=%.2f < Senior=%.2f < Especial=%.2f",
                            mediaNoob, mediaJunior, mediaSenior, mediaEspecial));

            // Proporción jugable: múltiples partidas por pieza respecto a ingresos de cada rango.
            double ingNoob = Progreso.monedasPorVictoria(Etiqueta.NOOB_1);
            double ingJunior = Progreso.monedasPorVictoria(Etiqueta.JUNIOR_1);
            double ingSenior = Progreso.monedasPorVictoria(Etiqueta.SENIOR_1);
            double ingEspecial = Math.max(Progreso.monedasPorVictoria(Etiqueta.MASTER),
                    2.5 * DificultadCarrera.ESPECIAL.multiplicadorRecompensa(2));

            cierto(mediaNoob / ingNoob >= 2.0 && mediaNoob / ingNoob <= 6.0, "Noob alcanzable en varias partidas: " + mediaNoob / ingNoob);
            cierto(mediaJunior / ingJunior >= 4.0 && mediaJunior / ingJunior <= 10.0, "Junior exigente y jugable: " + mediaJunior / ingJunior);
            cierto(mediaSenior / ingSenior >= 6.0 && mediaSenior / ingSenior <= 16.0, "Senior exigente: " + mediaSenior / ingSenior);
            cierto(mediaEspecial / ingEspecial >= 6.0 && mediaEspecial / ingEspecial <= 20.0, "Especial exigente: " + mediaEspecial / ingEspecial);
        });
    }

    private static void apuntarParidadTrilingueYEntrenador() {
        caso("tienda economía: paridad trilingüe en piezas, ui.txt y conocimiento del entrenador", () -> {
            CatalogoDePiezas cat = CatalogoDePiezas.delJuego();
            String[][] grupos = {NUEVAS_DISTANCIA, NUEVOS_ESCUDOS, NUEVAS_CICLO};
            for (String[] grupo : grupos) {
                for (String id : grupo) {
                    Pieza p = cat.pieza(id);
                    for (Idioma idi : Idioma.values()) {
                        String nom = p.nombre(idi);
                        cierto(nom != null && !nom.isBlank(), id + " tiene nombre en " + idi);
                    }
                    falso(p.nombre(Idioma.ES).equals(p.nombre(Idioma.DE)), id + " ES != DE");
                    falso(p.nombre(Idioma.ES).equals(p.nombre(Idioma.EN)), id + " ES != EN");
                }
            }

            String[] clavesUi = {
                "tienda.rango.especial", "tienda.rango_especial_bloqueado", "valor.cargas",
                "valor.enfriamientoCiclo", "valor.danoObstaculo", "valor.proteccion.general",
                "valor.proteccion.proyectil", "valor.proteccion.impacto", "valor.proteccion.integral",
                "desbloqueo.tienda"
            };
            for (String k : clavesUi) {
                for (Idioma idi : Idioma.values()) {
                    String txt = T.en(k, idi);
                    cierto(txt != null && !txt.isBlank() && !txt.equals(k), "ui.txt tiene " + k + " en " + idi);
                }
            }

            ConocimientoDelEntrenador.Entrada tienda = ConocimientoDelEntrenador.de("tienda_y_monedas");
            ConocimientoDelEntrenador.Entrada modelos = ConocimientoDelEntrenador.de("modelos_y_armas");
            cierto(tienda != null && modelos != null, "conceptos del entrenador presentes");
            cierto(tienda.resumenIa().contains("temporal shields") && tienda.resumenIa().contains("Special"),
                    "resumen.ia de tienda_y_monedas actualizado");
            cierto(modelos.resumenIa().contains("temporal shields") && modelos.resumenIa().contains("cycle"),
                    "resumen.ia de modelos_y_armas actualizado");
            Concepto cTienda = Concepto.de("tienda_y_monedas");
            Concepto cModelos = Concepto.de("modelos_y_armas");
            cierto(cTienda != null && cModelos != null, "conceptos offline presentes");
            for (Idioma idi : Idioma.values()) {
                cierto(String.join(" ", cTienda.parrafos(idi)).length() > 200, "tienda_y_monedas completo en " + idi);
                cierto(String.join(" ", cModelos.parrafos(idi)).length() > 200, "modelos_y_armas completo en " + idi);
            }
        });
    }

    private static double mediaDePrecios(List<Pieza> piezas) {
        if (piezas.isEmpty()) return 0;
        double suma = 0;
        for (Pieza p : piezas) suma += p.precio;
        return suma / piezas.size();
    }

    private static double simularDanoLaser(String idEscudo) {
        CatalogoDePiezas cat = CatalogoDePiezas.delJuego();
        Montaje mAtac = new Montaje(Modelo.de("vigia"));
        mAtac.poner(Montaje.Hueco.IZQUIERDA, cat.pieza("canon_solar"));
        Montaje mDef = new Montaje(Modelo.de("cobalto"));
        if (idEscudo != null) mDef.poner(Montaje.Hueco.IZQUIERDA, cat.pieza(idEscudo));
        Combatiente atac = new Combatiente("Atacante", mAtac, () -> new RobotDispara());
        Combatiente def = new Combatiente("Defensor", mDef, () -> idEscudo != null ? new RobotEscudo() : new RobotQuieto());
        Pelea p = new Pelea(atac, def, 99L);
        p.rondas = 1;
        p.empezarRonda();
        p.cuerpo(0).x = 300;
        p.cuerpo(0).y = 350;
        p.cuerpo(0).mira = 0;
        p.cuerpo(1).x = 550;
        p.cuerpo(1).y = 350;
        p.cuerpo(1).mira = 180;
        for (int t = 0; t < 20; t++) p.avanzar();
        return def.danoTotalRecibido;
    }

    private static final class RobotQuieto extends Metajavbot {
        @Override
        public void cadaTurno() {
        }
    }

    private static final class RobotAtacanteIzquierda extends Metajavbot {
        @Override
        public void cadaTurno() {
            if (listoIzquierda()) atacarIzquierda();
        }
    }

    private static final class RobotDispara extends Metajavbot {
        private int turnos;

        @Override
        public void cadaTurno() {
            if (turnos++ >= 2 && listoIzquierda()) atacarIzquierda();
        }
    }

    private static final class RobotEscudo extends Metajavbot {
        private int turnos;

        @Override
        public void cadaTurno() {
            if (turnos++ == 0 && listoIzquierda()) atacarIzquierda();
        }
    }
}
