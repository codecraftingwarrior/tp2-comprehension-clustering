package org.analysis.clustering;

import org.analysis.core.Analyzer;
import java.util.ArrayList;
import java.util.List;

public class ModuleGenerator {
    private Dendrogram dendrogram;
    private double couplingThreshold; // Le seuil CP de couplage
    private List<String> allTypes;
    private Analyzer analyzer;

    public ModuleGenerator(Dendrogram dendrogram, List<String> allTypes, double couplingThreshold, Analyzer analyzer) {
        this.dendrogram = dendrogram;
        this.allTypes = allTypes;
        this.couplingThreshold = couplingThreshold;
        this.analyzer = analyzer;
    }

    public List<Cluster> getIdentifiedModules() {
        List<Cluster> modules = new ArrayList<>();
        int totalClasses = allTypes.size();

        System.out.println();
        System.out.println("Identification des modules en cours ...... ");

        identifyModulesRecursively(dendrogram.getRootCluster(), modules, totalClasses);

        return modules;
    }


    private void identifyModulesRecursively(Cluster cluster, List<Cluster> modules, int totalClasses) {
        if (!cluster.getClasses().isEmpty() && modules.size() <= totalClasses / 2) {
            double averageCoupling = calculateAverageCoupling(cluster);

            // Si la moyenne du couplage est supérieure au seuil, c'est un module
            if (averageCoupling > couplingThreshold) {
                modules.add(cluster);
            } else {
                // Sinon, explorez récursivement les enfants du cluster
                for (Cluster child : cluster.getChildren()) {
                    identifyModulesRecursively(child, modules, totalClasses);
                }
            }
        }
    }

    private double calculateAverageCoupling(Cluster cluster) {
        List<String> classNames = cluster.getClasses();
        double totalCoupling = 0.0;

        for (int i = 0; i < classNames.size(); i++) {
            for (int j = i + 1; j < classNames.size(); j++) {
                String classA = classNames.get(i);
                String classB = classNames.get(j);

                totalCoupling += analyzer.calculateCouplingMetric(classA, classB);
            }
        }

        return (cluster.getClasses().size() > 0) ? totalCoupling / cluster.getClasses().size() : 0;
    }

}
