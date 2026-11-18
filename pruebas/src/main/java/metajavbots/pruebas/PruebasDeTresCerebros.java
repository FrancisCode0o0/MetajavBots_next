package metajavbots.pruebas;

import static metajavbots.pruebas.Pruebas.caso;
import static metajavbots.pruebas.Pruebas.cierto;
import static metajavbots.pruebas.Pruebas.igual;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import com.sun.net.httpserver.HttpServer;

import metajavbots.cerebro.CerebroDelJuego;
import metajavbots.cerebro.CerebroHttp;

/**
 * Los tres cerebros a la vez (bloque 2): tres servidores falsos en este ordenador, uno con la forma de Claude, otro
 * con la de Gemini y otro con la de OpenAI, que tardan un segundo cada uno en contestar. Se les pregunta a la vez y
 * tienen que contestar los tres, cada uno lo suyo, en mucho menos de tres segundos (es decir, a la vez, no en cola).
 */
final class PruebasDeTresCerebros {

    private PruebasDeTresCerebros() {
    }

    /** Un servidor que contesta con ese cuerpo tras un segundo, y apunta lo que le llegó. */
    private static HttpServer servidor(String ruta, String respuesta, List<String> recibido) throws IOException {
        HttpServer s = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        s.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());
        s.createContext("/", h -> {
            String cuerpo = new String(h.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            synchronized (recibido) {
                recibido.add(h.getRequestURI().getPath() + " " + h.getRequestHeaders().getFirst("x-goog-api-key") + " " + cuerpo);
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            byte[] b = respuesta.getBytes(StandardCharsets.UTF_8);
            h.getResponseHeaders().add("content-type", "application/json");
            h.sendResponseHeaders(h.getRequestURI().getPath().startsWith(ruta) ? 200 : 404, b.length);
            try (OutputStream o = h.getResponseBody()) {
                o.write(b);
            }
        });
        s.start();
        return s;
    }

    static void apuntar() {
        caso("cerebros: tres a la vez (Claude, Gemini y OpenAI), cada uno lo suyo y sin esperar en cola", () -> {
            List<String> recibido = new ArrayList<>();
            HttpServer claude = servidor("/v1/messages", "{\"content\":[{\"type\":\"text\",\"text\":\"soy Claude\"}]}", recibido);
            HttpServer gemini = servidor("/v1beta/models/", "{\"candidates\":[{\"content\":{\"role\":\"model\",\"parts\":[{\"text\":\"soy \"},"
                    + "{\"text\":\"Gemini\"}]}}]}", recibido);
            HttpServer openai = servidor("/v1/chat/completions", "{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":\"soy OpenAI\"}}]}",
                    recibido);
            try {
                CerebroHttp[] cs = {
                    new CerebroHttp(CerebroHttp.Forma.ANTHROPIC, "http://127.0.0.1:" + claude.getAddress().getPort(), "k1", "claude-x", "Claude"),
                    new CerebroHttp(CerebroHttp.Forma.GEMINI, "http://127.0.0.1:" + gemini.getAddress().getPort(), "k2", "gemini-flash-latest", "Gemini"),
                    new CerebroHttp(CerebroHttp.Forma.OPENAI, "http://127.0.0.1:" + openai.getAddress().getPort(), "k3", "gpt-x", "OpenAI"),
                };
                long t0 = System.nanoTime();
                List<CerebroDelJuego.Peticion> ps = new ArrayList<>();
                for (CerebroHttp c : cs) ps.add(CerebroDelJuego.Peticion.lanzar(c, "Eres un robot.", CerebroDelJuego.uno("¿Quién eres?"), 20));
                while (ps.stream().anyMatch(p -> !p.lista) && System.nanoTime() - t0 < 10_000_000_000L) Thread.sleep(20);
                double segundos = (System.nanoTime() - t0) / 1e9;
                igual("soy Claude", ps.get(0).respuesta, "Claude: " + ps.get(0).error);
                igual("soy Gemini", ps.get(1).respuesta, "Gemini: " + ps.get(1).error);
                igual("soy OpenAI", ps.get(2).respuesta, "OpenAI: " + ps.get(2).error);
                cierto(segundos < 2.2, "a la vez, no en cola: " + segundos + " s");
                // Gemini: la clave en su cabecera, el sistema aparte y el asistente se llama «model».
                String aGemini = recibido.stream().filter(r -> r.startsWith("/v1beta/models/gemini-flash-latest:generateContent")).findFirst().orElse("");
                cierto(aGemini.contains(" k2 ") && aGemini.contains("\"systemInstruction\"") && aGemini.contains("\"role\":\"user\""), aGemini);
            } finally {
                claude.stop(0);
                gemini.stop(0);
                openai.stop(0);
            }
        });
    }
}
