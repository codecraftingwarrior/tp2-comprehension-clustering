package org.analysis.cli.processor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.function.Predicate;

public abstract class ComplexInputProcessor<T> {
    protected final String message;
    protected BufferedReader inputReader = new BufferedReader(new InputStreamReader(System.in));
    private final Method parser;
    private final Predicate<String> validator;
    protected T parameter;

    public ComplexInputProcessor(String msg) {
        message = msg;
        parser = getParser();
        validator = getValidator();
    }

    protected abstract Predicate<String> getValidator();

    protected abstract Method getParser();

    public T process() throws IOException {
        System.out.print(message);
        String userInput = inputReader.readLine();

        while (!validator.test(userInput)) {
            System.err.println("Invalide, veuillez réessayer");
            System.out.println();
            System.out.println(message);
            userInput = inputReader.readLine();
        }

        try {
            parameter = (T) parser.invoke(null, userInput);
        } catch (InvocationTargetException | IllegalAccessException | SecurityException | IllegalArgumentException e) {
            throw new RuntimeException(e);
        }

        return parameter;
    }
}
