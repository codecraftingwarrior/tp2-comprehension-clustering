package org.analysis.visitor;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.PackageDeclaration;

import java.util.HashSet;

public class PackageVisitor extends ASTVisitor {

    //HashSet pour ne pas ajouter de doublons
    HashSet<String> packages = new HashSet<String>();

    public boolean visit(PackageDeclaration node) {
        packages.add(node.toString());
        return super.visit(node);
    }

    public HashSet<String> getPackages() {
        return packages;
    }
}
