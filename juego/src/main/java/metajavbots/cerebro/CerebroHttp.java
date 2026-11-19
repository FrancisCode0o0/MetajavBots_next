package metajavbots.cerebro;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

/**
 * CEREBRO HTTP — Habla con un modelo por HTTP. Tres formas: la de Anthropic
 * (Messages API), la «compatible con OpenAI» (/v1/chat/completions, que
 * también entiende KoboldCpp y muchos otros) y la propia de KoboldCpp
 * (/api/v1/generate) como último recurso. La clave va en la cabecera y
 * nunca se escribe en ningún registro.
 */
public final class CerebroHttp implements Cerebro {

    /** Cómo se habla con el modelo: la API de Anthropic, la de Gemini (Google), la compatible con OpenAI o la propia de KoboldCpp. */
    public enum Forma { ANTHROPIC, GEMINI, OPENAI, KOBOLD }

    private static final HttpClient CLIENTE = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();

    private final Forma forma;
    private final String direccion, clave, modelo, nombre;
    /** Lo que puede tardar una respuesta (un modelo local en un portátil, bastante). */
    public Duration espera = Duration.ofSeconds(300);

    public CerebroHttp(Forma forma, String direccion, String clave, String modelo, String nombre) {
        this.forma = forma;
        this.direccion = direccion.endsWith("/") ? direccion.substring(0, direccion.length() - 1) : direccion;
        this.clave = clave == null ? "" : clave.strip();
        this.modelo = modelo;
        this.nombre = nombre;
    }

    @Override
    public String nombre() {
        return nombre;
    }

    @Override
    public String preguntar(String sistema, List<Mensaje> conversacion, int maximoDePalabras) throws Exception {
        int fichas = Math.max(64, (int) (maximoDePalabras * 1.8));
        switch (forma) {
            case ANTHROPIC: return anthropic(sistema, conversacion, fichas);
            case GEMINI: return gemini(sistema, conversacion, fichas);
            case KOBOLD: return kobold(sistema, conversacion, fichas);
            default:
                try {
                    String r = openai(sistema, conversacion, fichas);
                    // Un modelo local que se ha quedado pensando sin contestar: otra vez, por el camino de KoboldCpp (sin pensar).
                    if (r.isBlank() && esLocal()) return kobold(sistema, conversacion, fichas);
                    return r;
                } catch (Exception e) {
                    if (esLocal()) return kobold(sistema, conversacion, fichas);
                    throw e;
                }
        }
    }

    private boolean esLocal() {
        return direccion.contains("localhost") || direccion.contains("127.0.0.1");
    }

    private String anthropic(String sistema, List<Mensaje> conv, int fichas) throws Exception {
        StringBuilder b = new StringBuilder("{\"model\":").append(Json.texto(modelo)).append(",\"max_tokens\":").append(fichas)
                .append(",\"system\":").append(Json.texto(sistema)).append(",\"messages\":[");
        for (int i = 0; i < conv.size(); i++) {
            if (i > 0) b.append(',');
            b.append("{\"role\":").append(Json.texto(conv.get(i).rol)).append(",\"content\":").append(Json.texto(conv.get(i).texto)).append('}');
        }
        b.append("]}");
        HttpRequest.Builder r = HttpRequest.newBuilder(URI.create(direccion + "/v1/messages")).timeout(espera)
                .header("content-type", "application/json").header("x-api-key", clave).header("anthropic-version", "2023-06-01")
                .POST(HttpRequest.BodyPublishers.ofString(b.toString(), StandardCharsets.UTF_8));
        JsonValue v = enviar(r.build());
        StringBuilder texto = new StringBuilder();
        JsonValue contenido = v.get("content");
        if (contenido != null) for (JsonValue c = contenido.child; c != null; c = c.next) if ("text".equals(c.getString("type",
                ""))) texto.append(c.getString("text", ""));
        return texto.toString().strip();
    }

    /**
     * Gemini (Google AI Studio): generateContent con la clave en la cabecera x-goog-api-key. El asistente se llama
     * «model»; a los «flash» se les pide que no piensen antes (gastaría las fichas en pensar y no contestaría).
     */
    private String gemini(String sistema, List<Mensaje> conv, int fichas) throws Exception {
        try {
            return gemini(sistema, conv, fichas, modelo.contains("flash"));
        } catch (IllegalStateException e) {
            // Hay modelos que no admiten «no pensar» (contestan 400): otra vez, sin pedirlo.
            if (modelo.contains("flash") && String.valueOf(e.getMessage()).startsWith("HTTP 400")) return gemini(sistema, conv, fichas, false);
            throw e;
        }
    }

    private String gemini(String sistema, List<Mensaje> conv, int fichas, boolean sinPensar) throws Exception {
        StringBuilder b = new StringBuilder("{\"systemInstruction\":{\"parts\":[{\"text\":").append(Json.texto(sistema)).append("}]},\"contents\":[");
        boolean primero = true;
        for (Mensaje m : conv) {
            if (!primero) b.append(',');
            primero = false;
            b.append("{\"role\":").append(Json.texto(m.rol.equals("assistant") ? "model" : "user")).append(",\"parts\":[{\"text\":")
                    .append(Json.texto(m.texto)).append("}]}");
        }
        b.append("],\"generationConfig\":{\"maxOutputTokens\":").append(fichas).append(",\"temperature\":0.8");
        if (sinPensar) b.append(",\"thinkingConfig\":{\"thinkingBudget\":0}");
        b.append("}}");
        HttpRequest r = HttpRequest.newBuilder(URI.create(direccion + "/v1beta/models/" + modelo + ":generateContent")).timeout(espera)
                .header("content-type", "application/json").header("x-goog-api-key", clave)
                .POST(HttpRequest.BodyPublishers.ofString(b.toString(), StandardCharsets.UTF_8)).build();
        JsonValue v = enviar(r);
        JsonValue cand = v.get("candidates");
        if (cand == null || cand.child == null) throw new IllegalStateException("respuesta sin candidates");
        JsonValue contenido = cand.child.get("content");
        JsonValue partes = contenido == null ? null : contenido.get("parts");
        StringBuilder texto = new StringBuilder();
        if (partes != null) for (JsonValue p = partes.child; p != null; p = p.next) texto.append(p.getString("text", ""));
        return limpiar(texto.toString());
    }

    private String openai(String sistema, List<Mensaje> conv, int fichas) throws Exception {
        StringBuilder b = new StringBuilder("{");
        if (modelo != null && !modelo.isEmpty()) b.append("\"model\":").append(Json.texto(modelo)).append(',');
        b.append("\"max_tokens\":").append(fichas).append(",\"temperature\":0.8,\"messages\":[{\"role\":\"system\",\"content\":")
                .append(Json.texto(sistema)).append('}');
        for (Mensaje m : conv) b.append(",{\"role\":").append(Json.texto(m.rol)).append(",\"content\":").append(Json.texto(m.texto)).append('}');
        b.append(']');
        // Qwen3.5 y otros «piensan» antes de contestar (minutos en un portátil): en el local se les pide que no.
        if (esLocal()) b.append(",\"chat_template_kwargs\":{\"enable_thinking\":false}");
        b.append('}');
        HttpRequest.Builder r = HttpRequest.newBuilder(URI.create(direccion + "/v1/chat/completions")).timeout(espera)
                .header("content-type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(b.toString(), StandardCharsets.UTF_8));
        if (!clave.isEmpty()) r.header("Authorization", "Bearer " + clave);
        JsonValue v = enviar(r.build());
        JsonValue choices = v.get("choices");
        if (choices == null || choices.child == null) throw new IllegalStateException("respuesta sin choices");
        JsonValue msg = choices.child.get("message");
        return limpiar(msg != null ? msg.getString("content", "") : choices.child.getString("text", ""));
    }

    private String kobold(String sistema, List<Mensaje> conv, int fichas) throws Exception {
        // KoboldCpp sin chat: se escribe todo como un solo texto con el formato de chat de Qwen (ChatML).
        StringBuilder p = new StringBuilder("<|im_start|>system\n").append(sistema).append("<|im_end|>\n");
        for (Mensaje m : conv) p.append("<|im_start|>").append(m.rol).append('\n').append(m.texto).append("<|im_end|>\n");
        // El pensamiento ya cerrado: así Qwen3.5 y los que «piensan» contestan directamente (si no, se quedan sin palabras pensando).
        p.append("<|im_start|>assistant\n<think>\n\n</think>\n\n");
        String cuerpo = "{\"prompt\":" + Json.texto(p.toString()) + ",\"max_length\":" + fichas
                + ",\"temperature\":0.8,\"stop_sequence\":[\"<|im_end|>\",\"<|im_start|>\"]}";
        HttpRequest r = HttpRequest.newBuilder(URI.create(direccion + "/api/v1/generate")).timeout(espera)
                .header("content-type", "application/json").POST(HttpRequest.BodyPublishers.ofString(cuerpo, StandardCharsets.UTF_8)).build();
        JsonValue v = enviar(r);
        JsonValue res = v.get("results");
        return limpiar(res != null && res.child != null ? res.child.getString("text", "") : "");
    }

    private JsonValue enviar(HttpRequest r) throws Exception {
        HttpResponse<String> res = CLIENTE.send(r, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (res.statusCode() / 100 != 2) {
            String cuerpo = res.body() == null ? "" : res.body();
            throw new IllegalStateException("HTTP " + res.statusCode() + ": " + (cuerpo.length() > 300 ? cuerpo.substring(0, 300) : cuerpo));
        }
        return new JsonReader().parse(res.body());
    }

    /** Quita lo que algunos modelos piensan en voz alta (<think>…</think>) y los restos de formato. */
    static String limpiar(String s) {
        if (s == null) return "";
        s = s.replaceAll("(?s)<think>.*?</think>", "");
        // Un pensamiento que no acabó (se quedó sin palabras): no hay respuesta.
        int i = s.indexOf("<think>");
        if (i >= 0) s = s.substring(0, i);
        s = s.replace("<|im_end|>", "").replace("<|im_start|>", "");
        return s.strip();
    }

    /** ¿Responde? (para el botón «Probar» de los ajustes). */
    public boolean responde() {
        try {
            String r = preguntar("Answer with one word.", "Say OK.", 5);
            return r != null && !r.isEmpty();
        } catch (Exception e) {
            return false;
        }
    }
}
