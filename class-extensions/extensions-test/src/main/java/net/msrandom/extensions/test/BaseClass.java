package net.msrandom.extensions.test;

import java.io.File;
import java.util.function.IntSupplier;
import java.util.function.IntToLongFunction;

import net.msrandom.extensions.annotations.ImplementedByExtension;

public class BaseClass {
    public static final String FILE_SEPARATOR = File.separator;

    @ImplementedByExtension
    private BaseClass(int x, int y) {
        throw new AssertionError();
    }

    @ImplementedByExtension
    public int area() {
        throw new AssertionError();
    }

    @ImplementedByExtension
    public IntSupplier areaFunction() {
        throw new AssertionError();
    }

    @ImplementedByExtension
    public IntToLongFunction volumeByDepth() {
        throw new AssertionError();
    }

    @ImplementedByExtension
    public static BaseClass of(int x, int y) {
        throw new AssertionError();
    }
}
