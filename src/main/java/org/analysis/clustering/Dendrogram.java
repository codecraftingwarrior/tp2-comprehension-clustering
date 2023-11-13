package org.analysis.clustering;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Dendrogram {

    private Cluster rootCluster;
    private Map<Cluster, List<ClusterPair>> history;

    public Dendrogram() {
        this.history = new LinkedHashMap<>();
    }

    public void addMergeStep(Cluster cluster1, Cluster cluster2, Cluster merged) {
        history
                .computeIfAbsent(merged, k -> new ArrayList<>())
                .add(new ClusterPair(cluster1, cluster2));
    }

    public void setRootCluster(Cluster rootCluster) {
        this.rootCluster = rootCluster;
    }

    public Cluster getRootCluster() {
        return rootCluster;
    }

    public void print() {
        if (rootCluster == null)
            throw new RuntimeException("The root cluster was not initialized");

        System.out.println("Clustering Tree : ");
        System.out.println(rootCluster.getName());
    }
}
