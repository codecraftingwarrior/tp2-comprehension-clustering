package org.analysis.cli.processor;

import java.lang.reflect.Method;
import java.util.function.Predicate;

public class DoubleInputProcessor extends ComplexInputProcessor<Double>{

    public DoubleInputProcessor(String msg) {
        super(msg);
    }

    @Override
    protected Predicate<String> getValidator() {
        return str -> {
            try {
                Double.parseDouble(str);
                return true;
            } catch (NumberFormatException e) {
                return false;
            }
        };
    }

    @Override
    protected Method getParser() {
        try {
            return Double.class.getMethod("parseDouble", String.class);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }
}
