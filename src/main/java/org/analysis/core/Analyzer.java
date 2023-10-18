package org.analysis.core;

import org.analysis.visitor.*;
import org.apache.commons.io.FileUtils;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.*;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.SingleGraph;
import org.graphstream.ui.layout.springbox.implementations.LinLog;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public class Analyzer {
    private static String projectPath;

    private static String projectSourcePath;
    private static final String jrePath = "/System/Library/Frameworks/JavaVM.framework/";
    private Integer classCount = null, methodCount = null;

    private final Map<String, Integer> methodCountByClass = new LinkedHashMap<>();

    private final Map<String, Integer> attributeCountByClass = new LinkedHashMap<>();

    private final Map<String, Integer> LOCountByMethod = new LinkedHashMap<>();

    private final SingleGraph callGraph = new SingleGraph("Call Graph");
    private final SingleGraph couplingGraph = new SingleGraph("Coupling Graph");

    List<File> javaFiles = new ArrayList<>();

    private static Analyzer instance = null;

    private Analyzer(String projectUrl) {
        projectPath = projectUrl.isEmpty() ? getDefaultProjectDirPath() : projectUrl;
        projectSourcePath = projectPath + "/src";
        final File folder = new File(projectSourcePath);
        javaFiles = listJavaFilesForFolder(folder);
    }

    public static Analyzer getInstance(String projectPath) {
        if (instance == null)
            instance = new Analyzer(projectPath);

        return instance;
    }

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

    // Création de l'AST
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

        return (CompilationUnit) parser.createAST(null); // create and parse
    }

    public int getClassCount() throws IOException {
        int classCounter = 0;
        for (File fileEntry : javaFiles) {
            String content = FileUtils.readFileToString(fileEntry, StandardCharsets.UTF_8);
            CompilationUnit ast = parse(content.toCharArray());

            TypeDeclarationVisitor visitor = new TypeDeclarationVisitor();
            ast.accept(visitor);

            classCounter += visitor.getClasses().size();
        }
        this.classCount = classCounter;
        return classCounter;
    }

    public int getLineCount() {
        int totalLines = 0;

        for (File file : javaFiles)
            totalLines += countLines(file);

        return totalLines;
    }

    private static int countLines(File file) {
        int lines = 0;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            while (reader.readLine() != null)
                lines++;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return lines;
    }

    public int getMethodCount() throws IOException {
        int methodCounter = 0;
        for (File fileEntry : javaFiles) {
            String content = FileUtils.readFileToString(fileEntry, StandardCharsets.UTF_8);
            CompilationUnit ast = parse(content.toCharArray());

            MethodDeclarationVisitor visitor = new MethodDeclarationVisitor();
            ast.accept(visitor);

            for (MethodDeclaration ignored : visitor.getMethodDeclarations())
                methodCounter++;
        }
        this.methodCount = methodCounter;
        return methodCounter;
    }

    public int getPackageCount() throws IOException {
        PackageVisitor packageVisitor = new PackageVisitor();

        for (File fileEntry : javaFiles) {
            String content = FileUtils.readFileToString(fileEntry, StandardCharsets.UTF_8);
            CompilationUnit ast = parse(content.toCharArray());

            ast.accept(packageVisitor);
        }
        return packageVisitor.getPackages().size();
    }

    public int getAverageMethodCountPerClass() throws IOException {
        return (this.methodCount == null ? getMethodCount() : this.methodCount) / (this.classCount == null ? getClassCount() : this.classCount);
    }

    public int getAverageLOCPerMethod() throws IOException {
        MethodDeclarationVisitor mdVisitor = new MethodDeclarationVisitor();
        int totalLineMethod = 0;
        for (File fileEntry : javaFiles) {
            String content = FileUtils.readFileToString(fileEntry, StandardCharsets.UTF_8);
            CompilationUnit ast = parse(content.toCharArray());

            ast.accept(mdVisitor);
            totalLineMethod += mdVisitor.getNumberLineMethod();
        }

        return totalLineMethod / (this.methodCount == null ? getMethodCount() : this.methodCount);
    }

    public int getAverageAttributeCountPerClass() throws IOException {
        AttributeVisitor attributeVisitor = new AttributeVisitor();

        for (File fileEntry : javaFiles) {
            String content = FileUtils.readFileToString(fileEntry, StandardCharsets.UTF_8);
            CompilationUnit ast = parse(content.toCharArray());

            ast.accept(attributeVisitor);
        }

        return (attributeVisitor.getAttributes().size()) / (this.classCount == null ? getClassCount() : this.classCount);
    }

    public void show10PercentClassWithHighestNumberOfMethods() throws IOException {


        countMethodPerClass();

        LinkedHashMap<String, Integer> sortedMap = getSortedMap(methodCountByClass);
        int count = (10 * sortedMap.size()) / 100;
        if (count < 1)
            count = 1;

        System.out.printf("\n10%% des classes (%d au total) avec le plus grand nombre de méthodes : %n", sortedMap.size());
        sortedMap
                .entrySet()
                .stream()
                .limit(count)
                .forEach((entry) -> System.out.printf("\t- %s -> %d %n", entry.getKey(), entry.getValue()));
    }

    private void countMethodPerClass() throws IOException {
        for (File fileEntry : javaFiles) {
            String content = FileUtils.readFileToString(fileEntry, StandardCharsets.UTF_8);
            CompilationUnit ast = parse(content.toCharArray());

            TypeDeclarationVisitor tdVisitor = new TypeDeclarationVisitor();

            ast.accept(tdVisitor);

            for (TypeDeclaration td : tdVisitor.getClasses())
                methodCountByClass.put(td.getName().toString(), (int) td.getMethods().length);
        }
    }

    public void show10PercentClassWithHighestNumberOfAttributes() throws IOException {

        countAttrinutePerClass();

        LinkedHashMap<String, Integer> sortedMap = getSortedMap(attributeCountByClass);

        int count = (10 * sortedMap.size()) / 100;
        if (count < 1)
            count = 1;

        System.out.printf("\n10%% des classes (%d au total) avec le plus grand nombre d'attributs : %n", sortedMap.size());
        sortedMap
                .entrySet()
                .stream()
                .limit(count)
                .forEach((entry) -> System.out.printf("\t- %s -> %d %n", entry.getKey(), entry.getValue()));

    }

    private void countAttrinutePerClass() throws IOException {
        for (File fileEntry : javaFiles) {
            String content = FileUtils.readFileToString(fileEntry, StandardCharsets.UTF_8);
            CompilationUnit ast = parse(content.toCharArray());

            TypeDeclarationVisitor tdVisitor = new TypeDeclarationVisitor();

            ast.accept(tdVisitor);

            for (TypeDeclaration td : tdVisitor.getClasses()) {
                attributeCountByClass.put(td.getName().toString(), td.getFields().length);
            }
        }
    }

    public void countLOCByMethod() throws IOException {

        for (File fileEntry : javaFiles) {
            String content = FileUtils.readFileToString(fileEntry, StandardCharsets.UTF_8);
            CompilationUnit ast = parse(content.toCharArray());

            ASTVisitor locCountVisitor = new ASTVisitor() {
                @Override
                public boolean visit(MethodDeclaration methode) {
                    int startLine = ast.getLineNumber(methode.getStartPosition());
                    int endLine = ast.getLineNumber(methode.getStartPosition() + methode.getLength());
                    int loc = endLine - startLine + 1;
                    LOCountByMethod.put(methode.getName().toString(), loc);
                    return true;
                }
            };

            ast.accept(locCountVisitor);
        }
    }

    public void show10PercentMethodsWithHighestLOC() throws IOException {
        System.out.println("\n10% des methodes avec le plus grands nombre de ligne de code : ");
        countLOCByMethod();
        int round = Math.round(10f * (this.methodCount == null ? getMethodCount() : this.methodCount) / 100);
        if (round < 1)
            round = 1;

        getSortedMap(LOCountByMethod)
                .entrySet()
                .stream()
                .limit(round)
                .forEach(entry -> System.out.println("\t- " + entry.getKey() + " -> " + entry.getValue() + " LOC"));
    }

    public void showClassIn2PreviousCategories() throws IOException {
        if (attributeCountByClass.size() == 0)
            countAttrinutePerClass();
        if (methodCountByClass.size() == 0)
            countMethodPerClass();

        System.out.println("\nLes classes qui font en même temps partie des deux catégories précédentes : ");
        getSortedMap(methodCountByClass)
                .entrySet()
                .stream()
                .limit((10L * methodCountByClass.size()) / 100)
                .forEach(entry -> {
                    if (getSortedMap(attributeCountByClass).containsKey(entry.getKey()))
                        System.out.printf("\t- %s %n", entry.getKey());
                });
    }

    public void showClassWithMoreThanXMethods(int x) throws IOException {
        if (methodCountByClass.size() == 0)
            countMethodPerClass();

        System.out.printf("Les classes qui ont plus de %d méthodes : %n", x);
        for (Map.Entry<String, Integer> entry : methodCountByClass.entrySet())
            if (entry.getValue() > x)
                System.out.println("\t- " + entry.getKey());
    }

    private static LinkedHashMap<String, Integer> getSortedMap(Map<String, Integer> map) {
        LinkedHashMap<String, Integer> sortedMap = map.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (oldValue, newValue) -> oldValue, LinkedHashMap::new));
        return sortedMap;
    }

    public int getMaximumNumberOfParameter() throws IOException {
        int highestNumberOfParameter = 0;
        for (File fileEntry : javaFiles) {
            String content = FileUtils.readFileToString(fileEntry, StandardCharsets.UTF_8);
            CompilationUnit ast = parse(content.toCharArray());

            MethodDeclarationVisitor visitor = new MethodDeclarationVisitor();
            ast.accept(visitor);
            for (MethodDeclaration method : visitor.getMethodDeclarations())
                if (method.parameters().size() > highestNumberOfParameter)
                    highestNumberOfParameter = method.parameters().size();
        }

        return highestNumberOfParameter;
    }

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

    private void buildCallGraph() throws IOException {
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
                        if (callGraph.nodes().noneMatch(n -> n.getId().equals(invokedMethodName)))
                            this.callGraph.addNode(invokedMethodName);

                        String edgeID = fullMethodName + "-" + invokedMethodName;
                        if (callGraph.edges().noneMatch(e -> e.getId().equals(edgeID)))
                            this.callGraph.addEdge(edgeID, fullMethodName, invokedMethodName, true);
                    }
                }

            }

        }
    }

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

    private String getFullMethodName(MethodDeclaration method) {
        String className = method.resolveBinding().getDeclaringClass().getName();

        return className + "." + method.getName().toString();
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

    public float calculateCouplingMetric(String classNameA, String classNameB) throws IOException {
        float couplingCounter = 0;

        buildCallGraph();

        float totalCoupling = callGraph.edges().count();

        for(Edge e: callGraph.edges().collect(Collectors.toList()))
            if(e.getNode0().getId().startsWith(classNameA + ".") && e.getNode1().getId().startsWith(classNameB + "."))
                couplingCounter++;

        return couplingCounter / totalCoupling;
    }

    public void buildWeightedCouplingGraph() throws IOException{

        for (File fileEntry : javaFiles) {
            //Création de l'AST
            String content = FileUtils.readFileToString(fileEntry, StandardCharsets.UTF_8);
            CompilationUnit ast = parse(content.toCharArray());

            //Visite des classes
            TypeDeclarationVisitor visitor = new TypeDeclarationVisitor();
            ast.accept(visitor);

            //Création de tous les noeuds
            this.couplingGraph.addNode(visitor.getClasses().get(0).getName().toString());
        }

        int totalNode = this.couplingGraph.getNodeCount();

        //Création des arrêtes pondérées
        for (Node node1 : this.callGraph.nodes().collect(Collectors.toList())){
            for (Node node2 : this.callGraph.nodes().collect(Collectors.toList())){
                this.callGraph.addEdge("concatenation", node1, node2);
            }
        }

    }

}
