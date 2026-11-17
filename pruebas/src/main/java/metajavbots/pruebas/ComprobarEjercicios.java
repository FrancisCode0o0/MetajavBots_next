package metajavbots.pruebas;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import metajavbots.codigo.Compilador;
import metajavbots.ejercicios.Comprobador;
import metajavbots.ejercicios.Ejercicio;
import metajavbots.ejercicios.Tema;

/**
 * COMPROBAR EJERCICIOS — Comprueba archivos de ejercicios sueltos, sin Gradle:
 *   java -cp <clases del juego y de las pruebas>;res metajavbots.pruebas.ComprobarEjercicios res/ejercicios/05_bucles.txt …
 * Dice, de cada ejercicio, si está bien escrito, si su solución pasa todos sus
 * casos y si su plantilla no los pasa. Sale con 0 si todo está bien.
 */
public final class ComprobarEjercicios {

    private ComprobarEjercicios() {
    }

    public static void main(String[] args) throws Exception {
        int malos = 0, total = 0;
        Set<String> ids = new HashSet<>();
        for (String a : args) {
            List<Ejercicio> lista;
            try (InputStream in = Files.newInputStream(Path.of(a))) {
                lista = Ejercicio.leer(in, Path.of(a).getFileName().toString());
            }
            for (Ejercicio e : lista) {
                total++;
                List<String> mal = new ArrayList<>();
                if (!ids.add(e.id)) mal.add("id repetido");
                if (!e.id.matches("[a-z0-9_]+")) mal.add("el id va en minúsculas, cifras y _");
                if (Tema.de(e.tema) == null) mal.add("tema que no existe: " + e.tema);
                for (String s : new String[]{e.titulo.es, e.titulo.de, e.titulo.en, e.enunciado.es, e.enunciado.de, e.enunciado.en}) {
                    if (s.isEmpty()) {
                        mal.add("falta un título o un enunciado en algún idioma");
                        break;
                    }
                }
                if (e.casos.size() < 3) mal.add("menos de tres casos");
                if (e.tipo != Ejercicio.Tipo.CLASE && e.firma.isEmpty()) mal.add("sin firma");
                if (e.java <= Compilador.versionDeJava()) {
                    Comprobador.Informe bien = Comprobador.comprobar(e, e.codigoSolucion(), Locale.ROOT);
                    if (!bien.todoBien()) {
                        StringBuilder b = new StringBuilder("la solución no pasa:");
                        for (Compilador.Fallo f : bien.errores) if (f.grave) b.append("\n      error ").append(f.linea).append(": ").append(f.mensaje);
                        for (Comprobador.Resultado r : bien.casos) {
                            if (!r.bien) b.append("\n      ").append(r.caso).append("   (salió ").append(r.obtenido).append(")");
                        }
                        mal.add(b.toString());
                    }
                    Comprobador.Informe plantilla = Comprobador.comprobar(e, e.codigoInicial(), Locale.ROOT);
                    if (plantilla.todoBien()) mal.add("la plantilla ya pasa todos los casos");
                    if (e.tipo != Ejercicio.Tipo.CLASE && !plantilla.compila()) {
                        mal.add("la plantilla no compila: " + plantilla.errores);
                    }
                }
                if (mal.isEmpty()) {
                    System.out.println("  bien  " + e.id);
                } else {
                    malos++;
                    System.out.println("  MAL   " + e.id + " (" + e.archivo + ")");
                    for (String m : mal) System.out.println("      " + m);
                }
            }
        }
        System.out.println(total + " ejercicios, " + malos + " mal");
        System.exit(malos == 0 ? 0 : 1);
    }
}
