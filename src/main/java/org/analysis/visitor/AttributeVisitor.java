package org.analysis.visitor;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.FieldDeclaration;

import java.util.ArrayList;
import java.util.List;

public class AttributeVisitor extends ASTVisitor {

    List<FieldDeclaration> attributes = new ArrayList<>();

    public boolean visit(FieldDeclaration node) {
        attributes.add(node);
        return super.visit(node);
    }

    public List<FieldDeclaration> getAttributes() {
        return attributes;
    }
}
