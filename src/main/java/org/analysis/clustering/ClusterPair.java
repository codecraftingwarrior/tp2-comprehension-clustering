package org.analysis.clustering;

public class ClusterPair {
    private final Cluster cluster1;
    private final Cluster cluster2;

    public ClusterPair(Cluster cluster1, Cluster cluster2) {
        this.cluster1 = cluster1;
        this.cluster2 = cluster2;
    }

    public Cluster getCluster1() {
        return cluster1;
    }

    public Cluster getCluster2() {
        return cluster2;
    }
}
