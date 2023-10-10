package org.analysis.cli.processor;

import java.lang.reflect.Method;
import java.util.function.Predicate;

public class IntegerInputProcessor extends ComplexInputProcessor<Integer> {

    public IntegerInputProcessor(String msg) {
        super(msg);
    }

    @Override
    protected Predicate<String> getValidator() {
        return str -> {
            try {
                Integer.parseInt(str);
                return true;
            } catch (NumberFormatException e) {
                return false;
            }
        };
    }

    @Override
    protected Method getParser() {
        try {
            return Integer.class.getMethod("parseInt", String.class);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }
}
