package net.msrandom.extensions.test.extensions;

import java.io.File;
import java.util.List;
import java.util.function.IntSupplier;
import java.util.function.IntToLongFunction;
import net.msrandom.extensions.annotations.ClassExtension;
import net.msrandom.extensions.annotations.ExtensionInjectedElement;
import net.msrandom.extensions.annotations.ImplementsBaseElement;
import net.msrandom.extensions.test.BaseClass;

@ClassExtension(BaseClass.class)
class ExtensionClass {
    @ExtensionInjectedElement
    public static final String PATH_SEPARATOR = File.pathSeparator;

    @ExtensionInjectedElement
    private final int x;

    @ExtensionInjectedElement
    private final int y;

    @ImplementsBaseElement
    private ExtensionClass(int x, int y) {
        this.x = x;
        this.y = y;
    }

    @ImplementsBaseElement
    public int area() {
        return x * y;
    }

    @ImplementsBaseElement
    public IntSupplier areaFunction() {
        return this::area;
    }

    @ImplementsBaseElement
    public IntToLongFunction volumeByDepth() {
        return z -> area() * z;
    }

    @ImplementsBaseElement
    public static ExtensionClass of(int x, int y) {
        return new ExtensionClass(x, y);
    }

    public static class X<T extends ExtensionClass> {
        public class Y<U extends X.Y> {

        }
    }
}
