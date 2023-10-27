package org.analysis.clustering;

import org.analysis.core.Analyzer;

import java.io.IOException;
import java.util.*;

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

    public double getAVGCoupling() throws IOException {
        Analyzer analyzer = Analyzer.getInstance();

        double coupling = 0.0;
        Object[] allClasses = classes.toArray();
        for (int i = 0; i < classes.size(); i++)
            for (int j = i + 1; j < classes.size(); j++)
                coupling += analyzer.calculateCouplingMetric((String)allClasses[i], (String)allClasses[j]);

        return coupling;
    }
}
