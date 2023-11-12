package org.analysis.core;

import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.Edge;
import org.graphstream.graph.implementations.SingleGraph;
import org.graphstream.ui.layout.springbox.implementations.LinLog;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import spoon.Launcher;
import spoon.reflect.CtModel;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtType;
import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.visitor.filter.TypeFilter;
import spoon.reflect.code.CtInvocation;

public class Analyzer {

    private final Graph callGraph = new SingleGraph("Call Graph");
    private final Graph weightedCouplingGraph = new SingleGraph("Coupling Graph");
    private CtModel model; // Modèle Spoon pour l'AST

    private List<String> allTypes;
    private static String projectPath;
    private static String projectSourcePath;
    private static Analyzer instance = null;

    // Constructeur de l'analyseur qui initialise Spoon et construit le modèle AST
    private Analyzer(String projectUrl) {
        projectPath = projectUrl.isEmpty() ? getDefaultProjectDirPath() : projectUrl;
        projectSourcePath = projectPath + "/src";

        Launcher launcher = new Launcher();
        launcher.addInputResource(projectSourcePath);
        launcher.getEnvironment().setAutoImports(true);
        launcher.getEnvironment().setNoClasspath(true);
        launcher.buildModel();

        this.model = launcher.getModel();

        allTypes = this
                .model
                .getAllTypes()
                .stream()
                .map(CtType::getSimpleName)
                .collect(Collectors.toList());
    }

    public static Analyzer getInstance(String projectUrl) {
        if (instance == null) {
            instance = new Analyzer(projectUrl);
            return instance;
        }

        return instance;
    }

    // Méthode pour calculer la métrique de couplage entre deux classes utilisant Spoon
   /* public double calculateCouplingMetric(String classNameA, String classNameB) throws IOException {
        CtClass<?> classA = (CtClass<?>) model.getAllTypes().stream()
                .filter(type -> type instanceof CtClass<?> && type.getSimpleName().equals(classNameA))
                .findFirst()
                .orElse(null);

        CtClass<?> classB = (CtClass<?>) model.getAllTypes().stream()
                .filter(type -> type instanceof CtClass<?> && type.getSimpleName().equals(classNameB))
                .findFirst()
                .orElse(null);

        if (classA == null || classB == null) {
            throw new IllegalArgumentException("One of the classes cannot be found in the model.");
        }

        long totalRelations = model.getAllTypes().stream()
                .flatMap(type -> type.getMethods().stream())
                .flatMap(method -> method.getElements(new TypeFilter<>(CtInvocation.class)).stream())
                .count();

        long relationsAB = classA.getMethods().stream()
                .flatMap(method -> method.getElements(new TypeFilter<>(CtInvocation.class)).stream())
                .map(CtInvocation::getExecutable)
                .filter(executable -> executable.getDeclaringType() != null)
                .filter(executable -> executable.getDeclaringType().getSimpleName().equals(classNameB))
                .count();

        long relationsBA = classB.getMethods().stream()
                .flatMap(method -> method.getElements(new TypeFilter<>(CtInvocation.class)).stream())
                .map(CtInvocation::getExecutable)
                .filter(executable -> executable.getDeclaringType() != null)
                .filter(executable -> executable.getDeclaringType().getSimpleName().equals(classNameA))
                .count();

        return (double) (relationsAB + relationsBA) / totalRelations;
    }*/

    public double calculateCouplingMetric(String classNameA, String classNameB) throws IOException {
        int couplingCounter = 0;

        if (!this.callGraph.nodes().findAny().isPresent())
            buildCallGraph();

        float totalCoupling = callGraph.edges().count();

        for (Edge e : callGraph.edges().collect(Collectors.toList()))
            if (
                    (e.getNode0().getId().startsWith(classNameA + ".")
                            && e.getNode1().getId().startsWith(classNameB + "."))
                            ||
                            (e.getNode0().getId().startsWith(classNameB + ".")
                                    && e.getNode1().getId().startsWith(classNameA + ".")))
                couplingCounter++;

        return couplingCounter / totalCoupling;
    }

    // Méthode pour construire et afficher le graphe d'appel basé sur les informations fournies par Spoon
    public void buildAndShowCallGraph() {

        buildCallGraph();

        // Applique les styles CSS aux nœuds et aux arêtes
        setGraphStyle();

        // Affiche le graphe avec un layout automatique
        callGraph.display().enableAutoLayout(new LinLog());
    }

    private void buildCallGraph() {
        // Construis le graphe d'appel en utilisant Spoon
        for (CtMethod<?> method : model.getElements(new TypeFilter<>(CtMethod.class))) {

            for (CtInvocation<?> invocation : method.getElements(new TypeFilter<>(CtInvocation.class))) {
                CtExecutableReference<?> executable = invocation.getExecutable();
                if (executable.getDeclaringType() != null && allTypes.contains(executable.getDeclaringType().getSimpleName())) {
                    String invokedMethodName = executable.getDeclaringType().getSimpleName() + "." + executable.getSimpleName();
                    Node invokedNode = null;
                    if (callGraph.getNode(invokedMethodName) == null) {
                        invokedNode = callGraph.addNode(invokedMethodName);
                        invokedNode.setAttribute("ui.label", invokedMethodName);

                    }

                    String methodID = method.getDeclaringType().getSimpleName() + "." + method.getSimpleName();
                    Node methodNode = null;
                    if (callGraph.getNode(methodID) == null) {
                        methodNode = callGraph.addNode(methodID);
                        methodNode.setAttribute("ui.label", methodID);
                    }

                    if (callGraph.getEdge(methodID + "->" + invokedMethodName) == null)
                        callGraph.addEdge(methodID + "->" + invokedMethodName, methodID, invokedMethodName, true);
                }
            }
        }
    }

    public static String getDefaultProjectDirPath() {
        String projectPath = System.getProperty("user.dir");

        File projetFile = new File(projectPath);

        File parentDirectory = projetFile.getParentFile();

        if (parentDirectory != null) {

            File topicDirectory = new File(parentDirectory, "topic");

            if (topicDirectory.exists() && topicDirectory.isDirectory()) {
                return topicDirectory.getAbsolutePath();
            } else {
                System.err.println("Le répertoire 'topic' n'existe pas dans le répertoire parent.");
                System.exit(0);
                return "";
            }
        } else {
            System.err.println("Le répertoire parent n'existe pas.");
            System.exit(0);
            return "";
        }
    }

    public void buildWeightedCouplingGraph() throws IOException {
        // Parcours toutes les classes du modèle Spoon
        List<CtClass<?>> allClasses = model.getElements(new TypeFilter<>(CtClass.class));

        // Calcule la métrique de couplage pour chaque paire de classes
        for (CtClass<?> classA : allClasses) {
            for (CtClass<?> classB : allClasses) {
                if (!classA.equals(classB)) {
                    String classNameA = classA.getQualifiedName();
                    String classNameB = classB.getQualifiedName();

                    // Utilise la méthode existante pour calculer la métrique de couplage entre deux classes
                    float couplingMetric = (float) calculateCouplingMetric(classA.getSimpleName(), classB.getSimpleName());

                    if (couplingMetric > 0) {
                        // Ajoute une arête pondérée au graphe pour chaque couple de classes avec une métrique de couplage positive
                        if (weightedCouplingGraph.getNode(classNameA) == null) {
                            Node aNode = weightedCouplingGraph.addNode(classNameA);
                            aNode.setAttribute("ui.label", classNameA);
                        }

                        if (weightedCouplingGraph.getNode(classNameB) == null) {
                            Node bNode = weightedCouplingGraph.addNode(classNameB);
                            bNode.setAttribute("ui.label", classNameB);
                        }

                        String edgeId = classNameA + "->" + classNameB;
                        String edgeIdSym = classNameB + "->" + classNameA;

                        Edge e = weightedCouplingGraph.getEdge(edgeId);
                        Edge eSym = weightedCouplingGraph.getEdge(edgeIdSym);

                        if (e == null && eSym == null) {
                            e = weightedCouplingGraph.addEdge(edgeId, classNameA, classNameB);
                            e.setAttribute("ui.label", String.format("%.3f", couplingMetric));
                        }
                    }
                }
            }
        }

        // Configuration du style CSS pour les nœuds et les arêtes
        String css = "text-alignment: at-right; text-padding: 3px, 2px; text-background-mode: rounded-box; text-background-color: #EB2; text-color: #222;";

        for (
                Node node : this.weightedCouplingGraph.nodes().

                collect(Collectors.toList())) {
            //node.setAttribute("ui.style", "shape:circle; fill-color: cyan;size: 30px; text-alignment: center;");
            node.setAttribute("ui.style", css);
            node.setAttribute("ui.label", node.getId());
            if (!node.neighborNodes().findAny().isPresent())
                node.setAttribute("ui.hide");
        }

        for (
                Edge edge : this.weightedCouplingGraph.edges().

                collect(Collectors.toList())) {
            edge.setAttribute("layout.weight", 20.0);
        }

        weightedCouplingGraph.setAttribute("ui.quality");
        weightedCouplingGraph.setAttribute("ui.style", "padding: 10px;");

        weightedCouplingGraph.display();
    }

    // Méthode pour configurer le style du graphe avec CSS
    private void setGraphStyle() {
        String css = "text-alignment: at-right; text-padding: 3px, 2px; text-background-mode: rounded-box; text-background-color: #EB2; text-color: #222;";
        for (Node node : callGraph.nodes().collect(Collectors.toSet())) {
            node.setAttribute("ui.style", css);
            node.setAttribute("ui.label", node.getId());
        }

        for (Edge edge : callGraph.edges().collect(Collectors.toSet())) {
            edge.setAttribute("layout.weight", 20.0);
            edge.setAttribute("ui.label", String.format("%.3f", edge.getNumber("weight")));
        }

        callGraph.setAttribute("ui.quality");
        callGraph.setAttribute("ui.antialias");
    }

}