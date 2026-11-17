package metajavbots.cerebro;

/** JSON — Lo justo para escribir las peticiones (textos con comillas y saltos bien escapados). */
final class Json {

    private Json() {
    }

    static String texto(String s) {
        StringBuilder b = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"': b.append("\\\""); break;
                case '\\': b.append("\\\\"); break;
                case '\n': b.append("\\n"); break;
                case '\r': b.append("\\r"); break;
                case '\t': b.append("\\t"); break;
                default:
                    if (c < 0x20) b.append(String.format("\\u%04x", (int) c));
                    else b.append(c);
                    break;
            }
        }
        return b.append('"').toString();
    }
}
