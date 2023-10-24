package org.analysis.clustering;

import org.analysis.cli.processor.DoubleInputProcessor;
import org.analysis.core.Analyzer;
import org.graphstream.graph.Element;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class ModuleClusterer {

    private final Analyzer analyzer;

    private List<String> candidates;

    private List<Cluster> clusters;

    private Set<Cluster> dendro;


    public ModuleClusterer(Analyzer analyzer) {
        this.analyzer = analyzer;

        candidates = analyzer
                .getJavaFileNames()
                .stream()
                .map(c -> c.substring(0, c.lastIndexOf(".")))
                .collect(Collectors.toList());

        this.clusters = new ArrayList<>();

        for (String classe : candidates) {
            Cluster cluster = new Cluster(classe);
            clusters.add(cluster);
        }
    }

    public ModuleClusterer buildClusters() throws IOException {
        // get the minimum coupling average value (CP)
        DoubleInputProcessor processor = new DoubleInputProcessor("Valeur de CP : ");
        double minimumCouplingValue = processor.process();

        System.out.println("Démmarage du processus de Clustering ...........");
        System.out.println("Clustering en cours ...........");

        dendro = new LinkedHashSet<>();


        while (clusters.size() > 1) {
            double bestMetric = -1.0;
            Cluster cluster1 = null;
            Cluster cluster2 = null;

            for (int i = 0; i < clusters.size(); i++) {
                for (int j = i + 1; j < clusters.size(); j++) {
                    Cluster c1 = clusters.get(i);
                    Cluster c2 = clusters.get(j);

                    double metric = analyzer.calculateCouplingMetric(c1, c2);

                    if (metric > bestMetric) {
                        bestMetric = metric;
                        cluster1 = c1;
                        cluster2 = c2;
                    }
                }
            }

            cluster1.merge(cluster2);
            clusters.remove(cluster2);
            dendro.add(cluster1);

        }

        return this;

    }

    public List<Cluster> getClusters() {
        return clusters;
    }

    public Set<Cluster> getDendro() {
        return dendro;
    }
}
