package org.analysis.core;
import org.analysis.clustering.Cluster;
import org.analysis.clustering.ModuleClusterer;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.SingleGraph;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import spoon.Launcher;
import spoon.reflect.CtModel;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.visitor.filter.TypeFilter;
import spoon.reflect.code.CtInvocation;

public class Analyzer {

    private final SingleGraph callGraph = new SingleGraph("Call Graph");
    private final SingleGraph weightedCouplingGraph = new SingleGraph("Coupling Graph");
    private static Analyzer instance = null;
    private static ModuleClusterer clusterer;
    private CtModel model; // Modèle Spoon pour l'AST

    // Constructeur de l'analyseur qui initialise Spoon et construit le modèle AST
    private Analyzer(String projectUrl) {
        Launcher launcher = new Launcher();
        launcher.addInputResource(projectUrl);
        launcher.buildModel();
        model = launcher.getModel();
    }

    // Méthode pour calculer la métrique de couplage entre deux classes utilisant Spoon
    public double calculateCouplingMetric(String classNameA, String classNameB) throws IOException {
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
    }

    public void buildCallGraph() {
        // Utilisez le modèle pour trouver toutes les méthodes et leurs invocations
        for (CtMethod<?> method : this.model.getElements(new TypeFilter<>(CtMethod.class))) {
            String fullMethodName = getFullMethodName(method);

            // Ajoutez le nœud pour la méthode courante s'il n'existe pas déjà
            if (callGraph.getNode(fullMethodName) == null) {
                callGraph.addNode(fullMethodName);
            }

            // Pour chaque invocation dans la méthode
            for (CtInvocation<?> invocation : method.getElements(new TypeFilter<>(CtInvocation.class))) {
                CtExecutableReference<?> executable = invocation.getExecutable();
                if (executable.getDeclaringType() != null) {
                    String invokedMethodName = getFullMethodName(executable);

                    // Ajoutez le nœud pour la méthode invoquée s'il n'existe pas déjà
                    if (callGraph.getNode(invokedMethodName) == null) {
                        callGraph.addNode(invokedMethodName);
                    }

                    // Ajoutez l'arête entre la méthode courante et la méthode invoquée
                    String edgeID = fullMethodName + "-" + invokedMethodName;
                    if (callGraph.getEdge(edgeID) == null) {
                        callGraph.addEdge(edgeID, fullMethodName, invokedMethodName);
                    }
                }
            }
        }
    }

    private String getFullMethodName(CtMethod<?> method) {
        // Obtenez le nom de la classe déclarante
        String className = method.getDeclaringType().getSimpleName();
        // Obtenez le nom de la méthode
        String methodName = method.getSimpleName();
        // Concaténez-les pour obtenir un nom complet au format "NomClasse.nomMethod"
        return className + "." + methodName;
    }

    private String getFullMethodName(CtExecutableReference<?> executable) {
        // Obtenez le nom de la classe déclarante
        String className = executable.getDeclaringType().getSimpleName();
        // Obtenez le nom de la méthode
        String methodName = executable.getSimpleName();
        // Concaténez-les pour obtenir un nom complet au format "NomClasse.nomMethod"
        return className + "." + methodName;
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

    public float calculateCouplingMetric(Cluster cluster1, Cluster cluster2) throws IOException {
        float result = 0.0f;

        for (String classNameA : cluster1.getClasses())
            for (String classNameB : cluster2.getClasses())
                result += calculateCouplingMetric(classNameA, classNameB);

        return result;
    }

    public void buildWeightedCouplingGraph() throws IOException {
        for (String javaFileName : javaFileNames) {
            String outerClassName = javaFileName.substring(0, javaFileName.lastIndexOf("."));
            if (weightedCouplingGraph.nodes().noneMatch(n -> n.getId().equals(outerClassName)))
                weightedCouplingGraph.addNode(outerClassName);

            for (String innerJavaFileName : javaFileNames) {
                if (!javaFileName.equals(innerJavaFileName)) {
                    String innerClassName = innerJavaFileName.substring(0, innerJavaFileName.lastIndexOf("."));
                    double couplingMetric = calculateCouplingMetric(outerClassName, innerClassName);
                    if (couplingMetric > 0) {
                        if (weightedCouplingGraph.nodes().noneMatch(n -> n.getId().equals(innerClassName)))
                            weightedCouplingGraph.addNode(innerClassName);
                        if (weightedCouplingGraph.edges().noneMatch(n -> n.getId().equals(outerClassName + "->" + innerClassName)) && weightedCouplingGraph.edges().noneMatch(n -> n.getId().equals(innerClassName + "->" + outerClassName))) {
                            Edge e = weightedCouplingGraph.addEdge(outerClassName + "->" + innerClassName, outerClassName, innerClassName);
                            e.setAttribute("ui.label", String.format("%.3f", couplingMetric));
                        }

                    }

                }
            }
        }


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

    public void buildClusters() throws IOException {
        Set<Cluster> clusters;
        if (clusterer.getDendro() == null || clusterer.getDendro().isEmpty())
            clusters = clusterer
                    .buildClusters()
                    .getDendro();
        else
            clusters = clusterer.getDendro();


        int i = 0;
        for (Cluster cluster : clusters) {
            System.out.print("Cluster " + (++i) + " { " + (cluster.getAVGCoupling()) + " } [ ");
            cluster.getClasses().forEach(c -> System.out.print(c + " "));
            System.out.println("]");
        }
    }

    public void identifyModules() throws IOException {
        Set<Cluster> modules = clusterer.getIdentifiedModules();

        int i = 0;
        for (Cluster module : modules) {
            System.out.print("Module " + (++i) + " { " + (module.getAVGCoupling()) + " } [ ");
            module.getClasses().forEach(c -> System.out.print(c + " "));
            System.out.println("]");
        }


    }

}
