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

    public Cluster(Cluster c) {
        this.merge(c);
    }

    public Cluster merge(Cluster other) {
        this.classes.addAll(other.getClasses());
        return this;
    }

    public Set<String> getClasses() {
        return classes;
    }
}
