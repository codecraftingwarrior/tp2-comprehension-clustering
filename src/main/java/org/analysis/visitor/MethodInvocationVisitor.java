package org.analysis.visitor;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;

import java.util.ArrayList;
import java.util.List;

public class MethodInvocationVisitor extends ASTVisitor {
    List<MethodInvocation> methodInvocations = new ArrayList<>();
    List<SuperMethodInvocation> superMethodInvocations = new ArrayList<>();


    public boolean visit(MethodInvocation node) {
        methodInvocations.add(node);
        super.visit(node);
        return false;
    }

    public boolean visit(SuperMethodInvocation node) {
        superMethodInvocations.add(node);
        super.visit(node);
        return false;
    }

    public List<MethodInvocation> getMethodInvocations() {
        return methodInvocations;
    }

    public List<SuperMethodInvocation> getSuperMethodInvocations() {
        return superMethodInvocations;
    }
}
