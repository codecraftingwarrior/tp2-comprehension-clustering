package org.analysis.cli;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public abstract class AbstractCLI {
    public static final String QUIT = "0";

    protected static BufferedReader inputReader = new BufferedReader(new InputStreamReader(System.in));

    protected abstract void mainMenu();

    protected abstract void processUserInput(String userInput) throws IOException;


    protected void run() throws InterruptedException, IOException {
        String input = "";
        do {
            mainMenu();
            System.out.print("> Choix : ");
            input = inputReader.readLine();
            processUserInput(input);
            Thread.sleep(1000);
        } while (!input.equals(QUIT));
    }
}
