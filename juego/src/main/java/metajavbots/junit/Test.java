package metajavbots.junit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * TEST — Marca un método de prueba, como @Test de JUnit 5. Las pruebas del
 * laboratorio son JUnit de verdad en todo menos el paquete: cambiando el
 * import a org.junit.jupiter.api, funcionan con JUnit 5.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Test {
}
