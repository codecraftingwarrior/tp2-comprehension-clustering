package org.analysis;

import org.analysis.cli.AbstractCLI;
import org.analysis.cli.processor.IntegerInputProcessor;
import org.analysis.core.Analyzer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.Set;

public class MainCLI extends AbstractCLI {

    private static Analyzer analyzer;
    private static Set<String> validChoices = new LinkedHashSet<>();

    public MainCLI() {
    }

    public static void main(String[] args) {
        init();
    }

    private static void init() {
        try {
            System.setProperty("org.graphstream.ui", "swing");
            System.out.printf("URL absolue de votre projet [%s] >>  ", Analyzer.getDefaultProjectDirPath());
            String projectPath = inputReader.readLine();
            Path path = Paths.get(projectPath);

            if (!Files.exists(path)) {
                System.err.println("Le dossier est introuvable");
                return;
            }
            analyzer = Analyzer.getInstance(projectPath);

            for (int i = 0; i <= 14; i++)
                validChoices.add(String.valueOf(i));

            MainCLI mainCLI = new MainCLI();
            mainCLI.run();
        } catch (InterruptedException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void mainMenu() {
        StringBuilder stringBuilder = new StringBuilder();
        System.out.println();
        System.out.print("---------------------------------\n");
        stringBuilder.append(String.format("%s. QUIT", QUIT));
        stringBuilder.append("\n---------------------------------");

        System.out.println(stringBuilder);
    }

    @Override
    protected void processUserInput(String userInput) throws IOException {

        if (userInput.equals(QUIT)) {
            System.out.println("À très bientôt");
            return;
        }

        if(!validChoices.contains(userInput)) {
            System.out.println("Valeur saisie incorrect merci de réssayer");
            System.out.println();
            return;
        }

    }
}
