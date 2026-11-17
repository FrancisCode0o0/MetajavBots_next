package metajavbots.junit;

/** EXECUTABLE — Un trozo de código que puede lanzar algo (para assertThrows), como en JUnit 5. */
@FunctionalInterface
public interface Executable {
    void execute() throws Throwable;
}
