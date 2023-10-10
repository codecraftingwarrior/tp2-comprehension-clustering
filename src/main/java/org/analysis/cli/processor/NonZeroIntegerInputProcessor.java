package org.analysis.cli.processor;

import java.util.function.Predicate;

public class NonZeroIntegerInputProcessor extends IntegerInputProcessor {

    public NonZeroIntegerInputProcessor(String msg) {
        super(msg);
    }

    @Override
    protected Predicate<String> getValidator() {
        return str -> {
            try {
                return Integer.parseInt(str) != 0;
            } catch (NumberFormatException e) {
                throw new RuntimeException(e);
            }
        };
    }
}
