package org.analysis.core;

import org.analysis.clustering.Cluster;
import org.analysis.clustering.ModuleClusterer;
import org.analysis.visitor.*;
import org.apache.commons.io.FileUtils;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.*;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.SingleGraph;
import org.graphstream.ui.layout.springbox.implementations.LinLog;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

// Classe utilisé pour analyser le projet
public class Analyzer {

    // Chemins pour le projet et les sources Java, ainsi que le chemin JRE pour l'analyse
    private static String projectPath;
    private static String projectSourcePath;
    private static final String jrePath = "/System/Library/Frameworks/JavaVM.framework/";

    // Compteurs pour les classes et méthodes
    private Integer classCount = null, methodCount = null;

    // Structures de données pour maintenir les comptages par classe et par méthode
    private final Map<String, Integer> methodCountByClass = new LinkedHashMap<>();
    private final Map<String, Integer> attributeCountByClass = new LinkedHashMap<>();
    private final Map<String, Integer> LOCountByMethod = new LinkedHashMap<>();

    // Graphes pour représenter les appels et le couplage entre les classes
    private final SingleGraph callGraph = new SingleGraph("Call Graph");
    private final SingleGraph weightedCouplingGraph = new SingleGraph("Coupling Graph");

    // Listes pour stocker les fichiers Java et leurs noms
    private List<File> javaFiles = new ArrayList<>();
    private List<String> javaFileNames = new ArrayList<>();

    // Instance unique de Analyzer pour le modèle Singleton
    private static Analyzer instance = null;

    // Instance de ModuleClusterer pour l'identification des clusters
    private static ModuleClusterer clusterer;

    // Constructeur privé pour empêcher l'instanciation directe
    private Analyzer(String projectUrl) {
        projectPath = projectUrl.isEmpty() ? getDefaultProjectDirPath() : projectUrl;
        projectSourcePath = projectPath + "/src";
        final File folder = new File(projectSourcePath);
        javaFiles = listJavaFilesForFolder(folder);
        javaFileNames = javaFiles
                .stream()
                .map(File::getName)
                .collect(Collectors.toList());
    }

    public static Analyzer getInstance(String projectPath) {
        if (instance == null) instance = new Analyzer(projectPath);

        clusterer = new ModuleClusterer(instance);
        return instance;
    }

    public static Analyzer getInstance() {
        if (instance == null) throw new RuntimeException("Aucun singleton existant");

        return instance;
    }

    // Retourne le graphe d'appel
    public SingleGraph getCallGraph() { return callGraph; }

    // Retourne la liste des noms de fichiers Java
    public List<String> getJavaFileNames() { return javaFileNames; }

    // Retourne la liste des fichiers Java
    public List<File> getJavaFiles() { return javaFiles; }

    // Liste les fichiers Java dans un dossier
    private @NotNull ArrayList<File> listJavaFilesForFolder(final @NotNull File folder) {
        ArrayList<File> javaFiles = new ArrayList<>();

        for (File fileEntry : Objects.requireNonNull(folder.listFiles())) {
            if (fileEntry.isDirectory()) {
                javaFiles.addAll(listJavaFilesForFolder(fileEntry));
            } else if (fileEntry.getName().contains(".java")) {
                javaFiles.add(fileEntry);
            }
        }
        return javaFiles;
    }

    // Parse les sources Java pour créer l'AST
    private CompilationUnit parse(char[] classSource) {
        ASTParser parser = ASTParser.newParser(AST.JLS4); // java +1.6
        parser.setResolveBindings(true);
        parser.setKind(ASTParser.K_COMPILATION_UNIT);

        parser.setBindingsRecovery(true);

        Map options = JavaCore.getOptions();
        parser.setCompilerOptions(options);

        parser.setUnitName("");

        String[] sources = {projectSourcePath};
        String[] classpath = {jrePath};

        parser.setEnvironment(classpath, sources, new String[]{"UTF-8"}, true);
        parser.setSource(classSource);

        return (CompilationUnit) parser.createAST(null);
    }

    // Construit et affiche le graphe d'appel
    public void buildAndShowCallGraph() throws IOException {

        buildCallGraph();

        String css = "text-alignment: at-right; text-padding: 3px, 2px; text-background-mode: rounded-box; text-background-color: #EB2; text-color: #222;";

        for (Node node : this.callGraph.nodes().collect(Collectors.toList())) {
            //node.setAttribute("ui.style", "shape:circle; fill-color: cyan;size: 30px; text-alignment: center;");
            node.setAttribute("ui.style", css);
            node.setAttribute("ui.label", node.getId());
            if (!node.neighborNodes().findAny().isPresent())
                node.setAttribute("ui.hide");

        }

        for (Edge edge : this.callGraph.edges().collect(Collectors.toList())) {
            edge.setAttribute("layout.weight", 20.0);
        }

        callGraph.setAttribute("ui.quality");
        callGraph.setAttribute("ui.style", "padding: 40px;");

        LinLog layout = new LinLog();
        layout.setStabilizationLimit(0.001); // Valeur de stabilisation
        layout.setQuality(1.0); // Qualité de la disposition
        callGraph.display().enableAutoLayout(layout);

    }

    // Construit le graphe d'appel à partir des sources Java
    public void buildCallGraph() throws IOException {
        for (File fileEntry : javaFiles) {
            String content = FileUtils.readFileToString(fileEntry, StandardCharsets.UTF_8);
            CompilationUnit ast = parse(content.toCharArray());

            MethodDeclarationVisitor visitor = new MethodDeclarationVisitor();
            ast.accept(visitor);

            for (MethodDeclaration method : visitor.getMethodDeclarations()) {

                String fullMethodName = getFullMethodName(method);
                if (callGraph.nodes().noneMatch(n -> n.getId().equals(fullMethodName)))
                    this.callGraph.addNode(fullMethodName);

                MethodInvocationVisitor miVisitor = new MethodInvocationVisitor();
                method.accept(miVisitor);

                if (!miVisitor.getMethodInvocations().isEmpty()) {
                    for (MethodInvocation mi : miVisitor.getMethodInvocations()) {
                        String invokedMethodName = getFullMethodName(mi);
                        Pattern regex = Pattern.compile("^(\\w+)\\.(\\w+)$");
                        Matcher matcher = regex.matcher(invokedMethodName);
                        if (callGraph.nodes().noneMatch(n -> n.getId().equals(invokedMethodName)) && matcher.matches() && javaFileNames.contains(matcher.group(1) + ".java")) {
                            this.callGraph.addNode(invokedMethodName);

                            String edgeID = fullMethodName + "-" + invokedMethodName;
                            if (callGraph.edges().noneMatch(e -> e.getId().equals(edgeID)))
                                this.callGraph.addEdge(edgeID, fullMethodName, invokedMethodName, true);
                        }
                    }
                }
            }
        }
    }

    // Retourne le nom complet d'une méthode invoquée
    private String getFullMethodName(MethodInvocation mi) {
        if (mi.getExpression() != null)
            if (mi.getExpression().resolveTypeBinding() != null)
                return mi.getExpression().resolveTypeBinding().getName() + "." + mi.getName().toString();

        if (mi.resolveMethodBinding() != null)
            return mi.resolveMethodBinding().getDeclaringClass().getName() + "." + mi.getName().toString();

        if (mi.resolveTypeBinding() != null)
            return mi.resolveTypeBinding().getDeclaringClass().getName() + "." + mi.getName().toString();

        return mi.getName().toString();
    }

    // Retourne le nom complet d'une déclaration de méthode
    private String getFullMethodName(MethodDeclaration method) {
        String className = method.resolveBinding().getDeclaringClass().getName();

        return className + "." + method.getName().toString();
    }

    // Retourne le chemin par défaut du projet
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

    // Calcule la métrique de couplage entre deux classes spécifiques
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

    // Calcule la métrique de couplage entre deux clusters
    public float calculateCouplingMetric(Cluster cluster1, Cluster cluster2) throws IOException {
        float result = 0.0f;

        for (String classNameA : cluster1.getClasses())
            for (String classNameB : cluster2.getClasses())
                result += calculateCouplingMetric(classNameA, classNameB);

        return result;
    }

    // Construit le graphe de couplage pondéré
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

    // Construit des clusters à partir des métriques de couplage
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

    // Identifie les modules à partir des clusters
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