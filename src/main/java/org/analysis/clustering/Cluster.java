package org.analysis.clustering;

import java.util.ArrayList;
import java.util.List;

public class Cluster {
    private String name;
    private List<Cluster> children;

    private List<String> classes;

    public Cluster(String name, List<String> classes) {
        this.name = name;
        this.children = new ArrayList<>();
        this.classes = classes;
    }

    public Cluster(Cluster cluster1, Cluster cluster2) {
        this.name = "(" + cluster1.getName() + ", " + cluster2.getName() + ")";
        this.children = new ArrayList<>();
        this.classes = new ArrayList<>();
        this.children.add(cluster1);
        this.children.add(cluster2);
        this.classes.addAll(cluster1.getClasses());
        this.classes.addAll(cluster2.getClasses());

    }

    public String getName() {
        return name;
    }

    public List<Cluster> getChildren() {
        return children;
    }

    public List<String> getClasses() {
        return classes;
    }

    public List<String> getAllClassNames() {
        List<String> allNames = new ArrayList<>(this.classes);
        for (Cluster child : children) {
            allNames.addAll(child.getAllClassNames());
        }
        return allNames;
    }
}
