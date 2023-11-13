package org.analysis.clustering;

import org.analysis.core.Analyzer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HierarchicalClusterer {

    private Analyzer analyzer;

    public HierarchicalClusterer(Analyzer analyzer) {
        this.analyzer = analyzer;
    }

    public Dendrogram doClusteringFor(List<String> classNames) {
        // Initialiser chaque classe comme un cluster
        List<Cluster> clusters = new ArrayList<>();
        for (String className : classNames) {
            clusters.add(new Cluster(className, Collections.singletonList(className)));
        }

        Dendrogram dendrogram = new Dendrogram();

        while (clusters.size() > 1) {
            ClusterPair mostCoupledPair = findMostCoupledClusters(clusters);

            if (mostCoupledPair == null) break;

            Cluster cluster1 = mostCoupledPair.getCluster1();
            Cluster cluster2 = mostCoupledPair.getCluster2();

            // Fusionner ces clusters
            Cluster mergedCluster = new Cluster(cluster1, cluster2);
            clusters.remove(cluster1);
            clusters.remove(cluster2);
            clusters.add(mergedCluster);

            // Ajouter l'étape de fusion au dendrogramme
            dendrogram.addMergeStep(cluster1, cluster2, mergedCluster);
        }

        dendrogram.setRootCluster(clusters.get(clusters.size() - 1));
        return dendrogram;
    }

    private ClusterPair findMostCoupledClusters(List<Cluster> clusters) {
        double maxCoupling = Double.MIN_VALUE;
        ClusterPair mostCoupledPair = null;

        for (int i = 0; i < clusters.size(); i++) {
            for (int j = i + 1; j < clusters.size(); j++) {
                Cluster cluster1 = clusters.get(i);
                Cluster cluster2 = clusters.get(j);
                double coupling = analyzer.calculateCouplingMetric(cluster1, cluster2);

                if (coupling > maxCoupling) {
                    maxCoupling = coupling;
                    mostCoupledPair = new ClusterPair(cluster1, cluster2);
                }
            }
        }

        return mostCoupledPair;
    }
}
