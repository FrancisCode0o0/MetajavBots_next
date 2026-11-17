package metajavbots.junit;

/** Una comprobación que no se cumple. */
public class AssertionFailedError extends AssertionError {

    private static final long serialVersionUID = 1L;

    public AssertionFailedError(String mensaje) {
        super(mensaje);
    }
}
