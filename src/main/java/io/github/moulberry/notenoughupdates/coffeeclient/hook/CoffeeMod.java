package io.github.moulberry.notenoughupdates.coffeeclient.hook;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks the main class of a CoffeeClient mod.
 * Equivalent to Forge's {@code @Mod} annotation.
 *
 * <p>
 * The annotated class is discovered via bytecode scanning when
 * the mod JAR is loaded. It will be instantiated during the NEU
 * constructor phase. Use {@link EventHandler} on methods to
 * receive lifecycle events.
 * </p>
 *
 * <pre>
 * {
 *     &#64;code
 *     &#64;CoffeeMod(name = "MyMod", version = "1.0.0")
 *     public class MyMod {
 *         @CoffeeMod.EventHandler
 *         public void onPreInit(CLPreInitEvent event) {
 *             // ...
 *         }
 *     }
 * }
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface CoffeeMod {

    /** Display name of the mod. */
    String name();

    /** Mod version string. */
    String version() default "1.0.0";

    /**
     * Marks a method as a CoffeeLoader lifecycle event handler.
     * The method must accept exactly one parameter whose type is
     * one of the {@code CL*Event} classes.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @interface EventHandler {
    }
}
