package org.analysis.clustering;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class Cluster {
    private Set<String> classes;

    public Cluster(String className) {
        this.classes = new LinkedHashSet<>();
        this.classes.add(className);
    }

    public Cluster() {
        this.classes = new LinkedHashSet<>();
    }

    public Cluster(Cluster cluster1, Cluster cluster2) {
        this.classes = new LinkedHashSet<>();
        this.classes.addAll(cluster1.getClasses());
        this.classes.addAll(cluster2.getClasses());
    }

    public Cluster(Cluster c) {
        this.merge(c);
    }

    public void merge(Cluster other) {
        this.classes.addAll(other.getClasses());
    }

    public Set<String> getClasses() {
        return classes;
    }

    public static Cluster empty() {
        return new Cluster();
    }
}
