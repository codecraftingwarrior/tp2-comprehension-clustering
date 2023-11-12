package org.analysis;

import org.analysis.cli.AbstractCLI;
import org.analysis.core.Analyzer;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.Set;

public class MainCLI extends AbstractCLI {

    private static Analyzer analyzer;
    // Ensemble pour stocker les choix valides de l'utilisateur
    private static final Set<String> validChoices = new LinkedHashSet<>();

    public MainCLI() { }

    public static void main(String[] args) {
        init();
    }

    // Initialisation de l'application
    private static void init() {
        try {
            // Configuration de la bibliothèque graphstream pour l'interface utilisateur
            System.setProperty("org.graphstream.ui", "swing");
            // Demande à l'utilisateur de fournir le chemin du projet à analyser
            System.out.printf("URL absolue de votre projet [%s] >>  ", Analyzer.getDefaultProjectDirPath());
            String projectPath = inputReader.readLine();
            Path path = Paths.get(projectPath);

            // Vérification de l'existence du chemin saisi par l'utilisateur
            if (!Files.exists(path)) {
                System.err.println("Le dossier est introuvable");
                return;
            }

            analyzer = Analyzer.getInstance(projectPath);

            // Remplissage de l'ensemble des choix valides
            for (int i = 0; i <= 5; i++) validChoices.add(String.valueOf(i));

            // Création et exécution de l'interface CLI
            MainCLI mainCLI = new MainCLI();
            mainCLI.run();
        } catch (InterruptedException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    // Affichage du menu principal
    @Override
    protected void mainMenu() {
        StringBuilder stringBuilder = new StringBuilder();
        System.out.println();
        System.out.print("---------------------------------\n");
        stringBuilder.append(String.format("%s. QUIT", QUIT));
        stringBuilder.append("\n1. Couplage entre deux classe A et B.");
        stringBuilder.append("\n2. Générer le graphe de couplage pondéré.");
        stringBuilder.append("\n3. Visualiser le graphe d'appel.");
        stringBuilder.append("\n4. Clustering - Identification des clusters.");
        stringBuilder.append("\n5. Identification des groupes de classes couplées (Modules / Service /...).");
        stringBuilder.append("\n---------------------------------");

        System.out.println(stringBuilder);
    }

    // Traite l'entrée utilisateur
    @Override
    protected void processUserInput(String userInput) throws IOException {

        if (userInput.equals(QUIT)) {
            System.out.println("À très bientôt !");
            return;
        }

        if(!validChoices.contains(userInput)) {
            System.out.println("Valeur saisie incorrect, merci de réssayer !");
            System.out.println();
            return;
        }

        switch (userInput) {
            case "1":
                handleChoice1();
                break;

            case "2":
                analyzer.buildWeightedCouplingGraph();
                break;

            case "3":
                analyzer.buildAndShowCallGraph();
                break;

            case "4":
                analyzer.buildClusters();
                break;
            case "5":
                analyzer.identifyModules();
                break;
        }
    }

    // Gestion du choix 1 du menu principal
    private void handleChoice1() throws IOException {
        System.out.print("Nom de la classe A : ");
        String classNameA = inputReader.readLine();

        System.out.print("Nom de la classe B : ");
        String classNameB = inputReader.readLine();

        double couplingWeight = analyzer.calculateCouplingMetric(classNameA, classNameB);

        System.out.printf("Le couplage entre %s et %s vaut %f (%.2f%%). %n", classNameA, classNameB, couplingWeight, couplingWeight*100);
    }
}