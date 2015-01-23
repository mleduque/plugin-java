/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.codenvy.ide.ext.java.client.core.rewrite;

import com.codenvy.ide.ext.java.jdt.core.dom.AST;
import com.codenvy.ide.ext.java.jdt.core.dom.ASTNode;
import com.codenvy.ide.ext.java.jdt.core.dom.AbstractTypeDeclaration;
import com.codenvy.ide.ext.java.jdt.core.dom.AssertStatement;
import com.codenvy.ide.ext.java.jdt.core.dom.Assignment;
import com.codenvy.ide.ext.java.jdt.core.dom.Block;
import com.codenvy.ide.ext.java.jdt.core.dom.BooleanLiteral;
import com.codenvy.ide.ext.java.jdt.core.dom.BreakStatement;
import com.codenvy.ide.ext.java.jdt.core.dom.CatchClause;
import com.codenvy.ide.ext.java.jdt.core.dom.ClassInstanceCreation;
import com.codenvy.ide.ext.java.jdt.core.dom.CompilationUnit;
import com.codenvy.ide.ext.java.jdt.core.dom.ConstructorInvocation;
import com.codenvy.ide.ext.java.jdt.core.dom.ContinueStatement;
import com.codenvy.ide.ext.java.jdt.core.dom.DoStatement;
import com.codenvy.ide.ext.java.jdt.core.dom.Expression;
import com.codenvy.ide.ext.java.jdt.core.dom.ExpressionStatement;
import com.codenvy.ide.ext.java.jdt.core.dom.ForStatement;
import com.codenvy.ide.ext.java.jdt.core.dom.IfStatement;
import com.codenvy.ide.ext.java.jdt.core.dom.InfixExpression;
import com.codenvy.ide.ext.java.jdt.core.dom.LabeledStatement;
import com.codenvy.ide.ext.java.jdt.core.dom.MethodDeclaration;
import com.codenvy.ide.ext.java.jdt.core.dom.MethodInvocation;
import com.codenvy.ide.ext.java.jdt.core.dom.Modifier;
import com.codenvy.ide.ext.java.jdt.core.dom.Name;
import com.codenvy.ide.ext.java.jdt.core.dom.PrefixExpression;
import com.codenvy.ide.ext.java.jdt.core.dom.PrimitiveType;
import com.codenvy.ide.ext.java.jdt.core.dom.ReturnStatement;
import com.codenvy.ide.ext.java.jdt.core.dom.SimpleName;
import com.codenvy.ide.ext.java.jdt.core.dom.SimpleType;
import com.codenvy.ide.ext.java.jdt.core.dom.SingleVariableDeclaration;
import com.codenvy.ide.ext.java.jdt.core.dom.Statement;
import com.codenvy.ide.ext.java.jdt.core.dom.StringLiteral;
import com.codenvy.ide.ext.java.jdt.core.dom.SwitchCase;
import com.codenvy.ide.ext.java.jdt.core.dom.SwitchStatement;
import com.codenvy.ide.ext.java.jdt.core.dom.SynchronizedStatement;
import com.codenvy.ide.ext.java.jdt.core.dom.TryStatement;
import com.codenvy.ide.ext.java.jdt.core.dom.Type;
import com.codenvy.ide.ext.java.jdt.core.dom.TypeDeclaration;
import com.codenvy.ide.ext.java.jdt.core.dom.UnionType;
import com.codenvy.ide.ext.java.jdt.core.dom.VariableDeclarationExpression;
import com.codenvy.ide.ext.java.jdt.core.dom.VariableDeclarationFragment;
import com.codenvy.ide.ext.java.jdt.core.dom.VariableDeclarationStatement;
import com.codenvy.ide.ext.java.jdt.core.dom.WhileStatement;
import com.codenvy.ide.ext.java.jdt.core.dom.rewrite.ASTRewrite;
import com.codenvy.ide.ext.java.jdt.core.dom.rewrite.ListRewrite;
import com.codenvy.ide.ext.java.jdt.internal.compiler.env.ICompilationUnit;
import com.codenvy.ide.legacy.client.text.DocumentImpl;
import com.codenvy.ide.api.text.Document;
import com.codenvy.ide.api.text.edits.TextEdit;

import org.junit.Ignore;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertTrue;

public class ASTRewritingStatementsTest extends ASTRewritingTest {

    @Test
    public void testInsert1() throws Exception {
      /* foo(): append a return statement */
        StringBuffer buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class C {\n");
        buf.append("    public Object foo() {\n");
        buf.append("        if (this.equals(new Object())) {\n");
        buf.append("            toString();\n");
        buf.append("        }\n");
        buf.append("    }\n");
        buf.append("}\n");
        ICompilationUnit cu =
                new com.codenvy.ide.ext.java.jdt.compiler.batch.CompilationUnit(buf.toString().toCharArray(), "C.java", "");

        CompilationUnit astRoot = createAST(cu);
        ASTRewrite rewrite = ASTRewrite.create(astRoot.getAST());
        TypeDeclaration type = findTypeDeclaration(astRoot, "C");

        MethodDeclaration methodDecl = findMethodDeclaration(type, "foo");
        Block block = methodDecl.getBody();
        assertTrue("No block", block != null);

        ReturnStatement returnStatement = block.getAST().newReturnStatement();
        returnStatement.setExpression(block.getAST().newNullLiteral());
        rewrite.getListRewrite(block, Block.STATEMENTS_PROPERTY).insertLast(returnStatement, null);

        String preview = evaluateRewrite(cu, rewrite);

        buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class C {\n");
        buf.append("    public Object foo() {\n");
        buf.append("        if (this.equals(new Object())) {\n");
        buf.append("            toString();\n");
        buf.append("        }\n");
        buf.append("        return null;\n");
        buf.append("    }\n");
        buf.append("}\n");

        assertEqualString(preview, buf.toString());

    }

    @Test
    public void testInsert2() throws Exception {
      /* insert a statement before */
        StringBuffer buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class D {\n");
        buf.append("    public Object goo() {\n");
        buf.append("        Integer i= new Integer(3);\n");
        buf.append("    }\n");
        buf.append("    public void hoo(int p1, Object p2) {\n");
        buf.append("        return;\n");
        buf.append("    }\n");
        buf.append("}\n");
        ICompilationUnit cu =
                new com.codenvy.ide.ext.java.jdt.compiler.batch.CompilationUnit(buf.toString().toCharArray(), "D.java", "");

        CompilationUnit astRoot = createAST(cu);
        ASTRewrite rewrite = ASTRewrite.create(astRoot.getAST());
        TypeDeclaration type = findTypeDeclaration(astRoot, "D");

        MethodDeclaration methodDeclGoo = findMethodDeclaration(type, "goo");
        List bodyStatements = methodDeclGoo.getBody().statements();

        ASTNode copy = rewrite.createCopyTarget((ASTNode)bodyStatements.get(0));

        MethodDeclaration methodDecl = findMethodDeclaration(type, "hoo");
        Block block = methodDecl.getBody();
        assertTrue("No block", block != null);

        List statements = block.statements();
        assertTrue("No statements in block", !statements.isEmpty());
        assertTrue("No ReturnStatement", statements.get(0) instanceof ReturnStatement);

        rewrite.getListRewrite(block, Block.STATEMENTS_PROPERTY).insertFirst(copy, null);

        String preview = evaluateRewrite(cu, rewrite);

        buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class D {\n");
        buf.append("    public Object goo() {\n");
        buf.append("        Integer i= new Integer(3);\n");
        buf.append("    }\n");
        buf.append("    public void hoo(int p1, Object p2) {\n");
        buf.append("        Integer i= new Integer(3);\n");
        buf.append("        return;\n");
        buf.append("    }\n");
        buf.append("}\n");

        assertEqualString(preview, buf.toString());

    }

    @Test
    public void testInsert3() throws Exception {
        // add after comment
        StringBuffer buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public Object goo() {\n");
        buf.append("        i++; //comment\n");
        buf.append("        i++; //comment\n");
        buf.append("        return new Integer(3);\n");
        buf.append("    }\n");
        buf.append("}\n");
        ICompilationUnit cu =
                new com.codenvy.ide.ext.java.jdt.compiler.batch.CompilationUnit(buf.toString().toCharArray(), "E.java", "");

        CompilationUnit astRoot = createAST(cu);
        ASTRewrite rewrite = ASTRewrite.create(astRoot.getAST());
        TypeDeclaration type = findTypeDeclaration(astRoot, "E");
        AST ast = astRoot.getAST();

        MethodDeclaration methodDecl = findMethodDeclaration(type, "goo");
        Block block = methodDecl.getBody();
        assertTrue("No block", block != null);

        MethodInvocation invocation1 = ast.newMethodInvocation();
        invocation1.setName(ast.newSimpleName("foo"));
        ExpressionStatement statement1 = ast.newExpressionStatement(invocation1);

        MethodInvocation invocation2 = ast.newMethodInvocation();
        invocation2.setName(ast.newSimpleName("foo"));
        ExpressionStatement statement2 = ast.newExpressionStatement(invocation2);

        List statements = methodDecl.getBody().statements();
        ASTNode second = (ASTNode)statements.get(1);

        ListRewrite listRewrite = rewrite.getListRewrite(methodDecl.getBody(), Block.STATEMENTS_PROPERTY);
        listRewrite.remove(second, null);
        listRewrite.insertBefore(statement1, second, null);
        listRewrite.insertAfter(statement2, second, null);

        String preview = evaluateRewrite(cu, rewrite);

        buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public Object goo() {\n");
        buf.append("        i++; //comment\n");
        buf.append("        foo();\n");
        buf.append("        foo();\n");
        buf.append("        return new Integer(3);\n");
        buf.append("    }\n");
        buf.append("}\n");

        assertEqualString(preview, buf.toString());

    }

    @Test
    public void testRemove1() throws Exception {
      /* foo():  remove if... */
        StringBuffer buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class C {\n");
        buf.append("    public Object foo() {\n");
        buf.append("        if (this.equals(new Object())) {\n");
        buf.append("            toString();\n");
        buf.append("        }\n");
        buf.append("    }\n");
        buf.append("}\n");
        ICompilationUnit cu =
                new com.codenvy.ide.ext.java.jdt.compiler.batch.CompilationUnit(buf.toString().toCharArray(), "C.java", "");

        CompilationUnit astRoot = createAST(cu);
        ASTRewrite rewrite = ASTRewrite.create(astRoot.getAST());
        TypeDeclaration type = findTypeDeclaration(astRoot, "C");

        MethodDeclaration methodDecl = findMethodDeclaration(type, "foo");
        Block block = methodDecl.getBody();
        assertTrue("No block", block != null);

        List statements = block.statements();
        assertTrue("No statements in block", !statements.isEmpty());

        rewrite.remove((ASTNode)statements.get(0), null);

        String preview = evaluateRewrite(cu, rewrite);

        buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class C {\n");
        buf.append("    public Object foo() {\n");
        buf.append("    }\n");
        buf.append("}\n");

        assertEqualString(preview, buf.toString());

    }

    @Test
    public void testRemove2() throws Exception {
        StringBuffer buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class D {\n");
        buf.append("    public Object goo() {\n");
        buf.append("        return new Integer(3);\n");
        buf.append("    }\n");
        buf.append("    public void hoo(int p1, Object p2) {\n");
        buf.append("        return;\n");
        buf.append("    }\n");
        buf.append("}\n");
        ICompilationUnit cu =
                new com.codenvy.ide.ext.java.jdt.compiler.batch.CompilationUnit(buf.toString().toCharArray(), "D.java", "");

        CompilationUnit astRoot = createAST(cu);
        ASTRewrite rewrite = ASTRewrite.create(astRoot.getAST());
        TypeDeclaration type = findTypeDeclaration(astRoot, "D");

        MethodDeclaration methodDecl = findMethodDeclaration(type, "goo");
        Block block = methodDecl.getBody();
        assertTrue("No block", block != null);

        List statements = block.statements();
        assertTrue("No statements in block", !statements.isEmpty());
        assertTrue("No ReturnStatement", statements.get(0) instanceof ReturnStatement);

        ReturnStatement returnStatement = (ReturnStatement)statements.get(0);
        Expression expr = returnStatement.getExpression();
        rewrite.remove(expr, null);

        String preview = evaluateRewrite(cu, rewrite);

        buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class D {\n");
        buf.append("    public Object goo() {\n");
        buf.append("        return;\n");
        buf.append("    }\n");
        buf.append("    public void hoo(int p1, Object p2) {\n");
        buf.append("        return;\n");
        buf.append("    }\n");
        buf.append("}\n");

        assertEqualString(preview, buf.toString());

    }

    @Test
    public void testRemove3() throws Exception {
        // delete
        StringBuffer buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public Object goo() {\n");
        buf.append("        i++; //comment\n");
        buf.append("        i++; //comment\n");
        buf.append("        return new Integer(3);\n");
        buf.append("    }\n");
        buf.append("}\n");
        ICompilationUnit cu =
                new com.codenvy.ide.ext.java.jdt.compiler.batch.CompilationUnit(buf.toString().toCharArray(), "E.java", "");

        CompilationUnit astRoot = createAST(cu);
        ASTRewrite rewrite = ASTRewrite.create(astRoot.getAST());
        TypeDeclaration type = findTypeDeclaration(astRoot, "E");

        MethodDeclaration methodDecl = findMethodDeclaration(type, "goo");
        Block block = methodDecl.getBody();
        assertTrue("No block", block != null);

        List statements = methodDecl.getBody().statements();
        rewrite.remove((ASTNode)statements.get(0), null);
        rewrite.remove((ASTNode)statements.get(1), null);
        rewrite.remove((ASTNode)statements.get(2), null);

        String preview = evaluateRewrite(cu, rewrite);

        buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public Object goo() {\n");
        buf.append("    }\n");
        buf.append("}\n");

        assertEqualString(preview, buf.toString());

    }

    @Test
    // https://bugs.eclipse.org/bugs/show_bug.cgi?id=276938
    public void testRemoveStatement01() throws Exception {
        StringBuffer buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public void foo(int i) {\n");
        buf.append("        foo(0);\n");
        buf.append("        foo(1);\n");
        buf.append("        foo(2);\n");
        buf.append("    }\n");
        buf.append("}\n");
        ICompilationUnit cu =
                new com.codenvy.ide.ext.java.jdt.compiler.batch.CompilationUnit(buf.toString().toCharArray(), "E.java", "");

        CompilationUnit astRoot = createAST(cu);
        ASTRewrite rewrite = ASTRewrite.create(astRoot.getAST());

        assertTrue("Parse errors", (astRoot.getFlags() & ASTNode.MALFORMED) == 0);
        TypeDeclaration type = findTypeDeclaration(astRoot, "E");
        MethodDeclaration methodDecl = findMethodDeclaration(type, "foo");
        Block block = methodDecl.getBody();
        List blockStatements = block.statements();
        assertTrue("Number of statements not 3", blockStatements.size() == 3);
        {
            ListRewrite listRewrite = rewrite.getListRewrite(block, Block.STATEMENTS_PROPERTY);
            listRewrite.remove((ASTNode)blockStatements.get(1), null);
        }

        String preview = evaluateRewrite(cu, rewrite);

        buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public void foo(int i) {\n");
        buf.append("        foo(0);\n");
        buf.append("        foo(2);\n");
        buf.append("    }\n");
        buf.append("}\n");
        assertEqualString(preview, buf.toString());
    }

    @Test
    // https://bugs.eclipse.org/bugs/show_bug.cgi?id=276938
    public void testRemoveStatement02() throws Exception {
        StringBuffer buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public void foo(int i) {\n");
        buf.append("        foo(0);\n");
        buf.append("        foo(1);foo(2);\n");
        buf.append("    }\n");
        buf.append("}\n");
        ICompilationUnit cu =
                new com.codenvy.ide.ext.java.jdt.compiler.batch.CompilationUnit(buf.toString().toCharArray(), "E.java", "");

        CompilationUnit astRoot = createAST(cu);
        ASTRewrite rewrite = ASTRewrite.create(astRoot.getAST());

        assertTrue("Parse errors", (astRoot.getFlags() & ASTNode.MALFORMED) == 0);
        TypeDeclaration type = findTypeDeclaration(astRoot, "E");
        MethodDeclaration methodDecl = findMethodDeclaration(type, "foo");
        Block block = methodDecl.getBody();
        List blockStatements = block.statements();
        assertTrue("Number of statements not 3", blockStatements.size() == 3);
        {
            ListRewrite listRewrite = rewrite.getListRewrite(block, Block.STATEMENTS_PROPERTY);
            listRewrite.remove((ASTNode)blockStatements.get(1), null);
        }

        String preview = evaluateRewrite(cu, rewrite);

        buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public void foo(int i) {\n");
        buf.append("        foo(0);\n");
        buf.append("        foo(2);\n");
        buf.append("    }\n");
        buf.append("}\n");
        assertEqualString(preview, buf.toString());
    }

    @Test
    // https://bugs.eclipse.org/bugs/show_bug.cgi?id=276938
    public void testRemoveStatement03() throws Exception {
        StringBuffer buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public void foo(int i) {\n");
        buf.append("        foo(0);foo(1);\n");
        buf.append("        foo(2);\n");
        buf.append("    }\n");
        buf.append("}\n");
        ICompilationUnit cu =
                new com.codenvy.ide.ext.java.jdt.compiler.batch.CompilationUnit(buf.toString().toCharArray(), "E.java", "");

        CompilationUnit astRoot = createAST(cu);
        ASTRewrite rewrite = ASTRewrite.create(astRoot.getAST());

        assertTrue("Parse errors", (astRoot.getFlags() & ASTNode.MALFORMED) == 0);
        TypeDeclaration type = findTypeDeclaration(astRoot, "E");
        MethodDeclaration methodDecl = findMethodDeclaration(type, "foo");
        Block block = methodDecl.getBody();
        List blockStatements = block.statements();
        assertTrue("Number of statements not 3", blockStatements.size() == 3);
        {
            ListRewrite listRewrite = rewrite.getListRewrite(block, Block.STATEMENTS_PROPERTY);
            listRewrite.remove((ASTNode)blockStatements.get(1), null);
        }

        String preview = evaluateRewrite(cu, rewrite);

        buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public void foo(int i) {\n");
        buf.append("        foo(0);\n");
        buf.append("        foo(2);\n");
        buf.append("    }\n");
        buf.append("}\n");
        assertEqualString(preview, buf.toString());
    }

    @Test
    // https://bugs.eclipse.org/bugs/show_bug.cgi?id=276938
    public void testRemoveStatement04() throws Exception {
        StringBuffer buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public void foo(int i) {\n");
        buf.append("        foo(0);foo(1);foo(2);\n");
        buf.append("    }\n");
        buf.append("}\n");
        ICompilationUnit cu =
                new com.codenvy.ide.ext.java.jdt.compiler.batch.CompilationUnit(buf.toString().toCharArray(), "E.java", "");

        CompilationUnit astRoot = createAST(cu);
        ASTRewrite rewrite = ASTRewrite.create(astRoot.getAST());

        assertTrue("Parse errors", (astRoot.getFlags() & ASTNode.MALFORMED) == 0);
        TypeDeclaration type = findTypeDeclaration(astRoot, "E");
        MethodDeclaration methodDecl = findMethodDeclaration(type, "foo");
        Block block = methodDecl.getBody();
        List blockStatements = block.statements();
        assertTrue("Number of statements not 3", blockStatements.size() == 3);
        {
            ListRewrite listRewrite = rewrite.getListRewrite(block, Block.STATEMENTS_PROPERTY);
            listRewrite.remove((ASTNode)blockStatements.get(1), null);
        }

        String preview = evaluateRewrite(cu, rewrite);

        buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public void foo(int i) {\n");
        buf.append("        foo(0);foo(2);\n");
        buf.append("    }\n");
        buf.append("}\n");
        assertEqualString(preview, buf.toString());
    }

    @Test
    // https://bugs.eclipse.org/bugs/show_bug.cgi?id=276938
    public void testRemoveStatement05() throws Exception {
        StringBuffer buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public void foo(int i) {\n");
        buf.append("        foo(0);\n");
        buf.append("        foo(1);foo(2);\n");
        buf.append("        foo(3);\n");
        buf.append("    }\n");
        buf.append("}\n");
        ICompilationUnit cu =
                new com.codenvy.ide.ext.java.jdt.compiler.batch.CompilationUnit(buf.toString().toCharArray(), "E.java", "");

        CompilationUnit astRoot = createAST(cu);
        ASTRewrite rewrite = ASTRewrite.create(astRoot.getAST());

        assertTrue("Parse errors", (astRoot.getFlags() & ASTNode.MALFORMED) == 0);
        TypeDeclaration type = findTypeDeclaration(astRoot, "E");
        MethodDeclaration methodDecl = findMethodDeclaration(type, "foo");
        Block block = methodDecl.getBody();
        List blockStatements = block.statements();
        assertTrue("Number of statements not 4", blockStatements.size() == 4);
        {
            ListRewrite listRewrite = rewrite.getListRewrite(block, Block.STATEMENTS_PROPERTY);
            listRewrite.remove((ASTNode)blockStatements.get(2), null);
        }

        String preview = evaluateRewrite(cu, rewrite);

        buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public void foo(int i) {\n");
        buf.append("        foo(0);\n");
        buf.append("        foo(1);\n");
        buf.append("        foo(3);\n");
        buf.append("    }\n");
        buf.append("}\n");
        assertEqualString(preview, buf.toString());
    }

    @Test
    // https://bugs.eclipse.org/bugs/show_bug.cgi?id=276938
    public void testRemoveStatement06() throws Exception {
        StringBuffer buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public void foo(int i) {\n");
        buf.append("        foo(0);\n");
        buf.append("        foo(1);foo(2);\n");
        buf.append("        foo(3);\n");
        buf.append("    }\n");
        buf.append("}\n");
        ICompilationUnit cu =
                new com.codenvy.ide.ext.java.jdt.compiler.batch.CompilationUnit(buf.toString().toCharArray(), "E.java", "");

        CompilationUnit astRoot = createAST(cu);
        ASTRewrite rewrite = ASTRewrite.create(astRoot.getAST());

        assertTrue("Parse errors", (astRoot.getFlags() & ASTNode.MALFORMED) == 0);
        TypeDeclaration type = findTypeDeclaration(astRoot, "E");
        MethodDeclaration methodDecl = findMethodDeclaration(type, "foo");
        Block block = methodDecl.getBody();
        List blockStatements = block.statements();
        assertTrue("Number of statements not 4", blockStatements.size() == 4);
        {
            ListRewrite listRewrite = rewrite.getListRewrite(block, Block.STATEMENTS_PROPERTY);
            listRewrite.remove((ASTNode)blockStatements.get(1), null);
            listRewrite.remove((ASTNode)blockStatements.get(2), null);
        }

        String preview = evaluateRewrite(cu, rewrite);

        buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public void foo(int i) {\n");
        buf.append("        foo(0);\n");
        buf.append("        foo(3);\n");
        buf.append("    }\n");
        buf.append("}\n");
        assertEqualString(preview, buf.toString());
    }

    @Test
    // https://bugs.eclipse.org/bugs/show_bug.cgi?id=276938
    public void testRemoveStatement07() throws Exception {
        StringBuffer buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public void foo(int i) {\n");
        buf.append("        foo(0);\n");
        buf.append("        foo(1);foo(2);foo(3);\n");
        buf.append("        foo(4);\n");
        buf.append("    }\n");
        buf.append("}\n");
        ICompilationUnit cu =
                new com.codenvy.ide.ext.java.jdt.compiler.batch.CompilationUnit(buf.toString().toCharArray(), "E.java", "");

        CompilationUnit astRoot = createAST(cu);
        ASTRewrite rewrite = ASTRewrite.create(astRoot.getAST());

        assertTrue("Parse errors", (astRoot.getFlags() & ASTNode.MALFORMED) == 0);
        TypeDeclaration type = findTypeDeclaration(astRoot, "E");
        MethodDeclaration methodDecl = findMethodDeclaration(type, "foo");
        Block block = methodDecl.getBody();
        List blockStatements = block.statements();
        assertTrue("Number of statements not 5", blockStatements.size() == 5);
        {
            ListRewrite listRewrite = rewrite.getListRewrite(block, Block.STATEMENTS_PROPERTY);
            listRewrite.remove((ASTNode)blockStatements.get(2), null);
            listRewrite.remove((ASTNode)blockStatements.get(3), null);
        }

        String preview = evaluateRewrite(cu, rewrite);

        buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public void foo(int i) {\n");
        buf.append("        foo(0);\n");
        buf.append("        foo(1);\n");
        buf.append("        foo(4);\n");
        buf.append("    }\n");
        buf.append("}\n");
        assertEqualString(preview, buf.toString());
    }

    @Test
    // https://bugs.eclipse.org/bugs/show_bug.cgi?id=276938
    public void testRemoveStatement08() throws Exception {
        StringBuffer buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public void foo(int i) {\n");
        buf.append("        foo(0);\n");
        buf.append("        foo(1);foo(2);foo(3);\n");
        buf.append("        foo(4);\n");
        buf.append("    }\n");
        buf.append("}\n");
        ICompilationUnit cu =
                new com.codenvy.ide.ext.java.jdt.compiler.batch.CompilationUnit(buf.toString().toCharArray(), "E.java", "");

        CompilationUnit astRoot = createAST(cu);
        ASTRewrite rewrite = ASTRewrite.create(astRoot.getAST());

        assertTrue("Parse errors", (astRoot.getFlags() & ASTNode.MALFORMED) == 0);
        TypeDeclaration type = findTypeDeclaration(astRoot, "E");
        MethodDeclaration methodDecl = findMethodDeclaration(type, "foo");
        Block block = methodDecl.getBody();
        List blockStatements = block.statements();
        assertTrue("Number of statements not 5", blockStatements.size() == 5);
        {
            ListRewrite listRewrite = rewrite.getListRewrite(block, Block.STATEMENTS_PROPERTY);
            listRewrite.remove((ASTNode)blockStatements.get(2), null);
        }

        String preview = evaluateRewrite(cu, rewrite);

        buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public void foo(int i) {\n");
        buf.append("        foo(0);\n");
        buf.append("        foo(1);foo(3);\n");
        buf.append("        foo(4);\n");
        buf.append("    }\n");
        buf.append("}\n");
        assertEqualString(preview, buf.toString());
    }

    @Test
    // https://bugs.eclipse.org/bugs/show_bug.cgi?id=276938
    public void testRemoveStatement09() throws Exception {
        StringBuffer buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public void foo(int i) {\n");
        buf.append("        foo(0);\n");
        buf.append("        foo(1);foo(2);foo(3);\n");
        buf.append("        foo(4);\n");
        buf.append("    }\n");
        buf.append("}\n");
        ICompilationUnit cu =
                new com.codenvy.ide.ext.java.jdt.compiler.batch.CompilationUnit(buf.toString().toCharArray(), "E.java", "");

        CompilationUnit astRoot = createAST(cu);
        ASTRewrite rewrite = ASTRewrite.create(astRoot.getAST());

        assertTrue("Parse errors", (astRoot.getFlags() & ASTNode.MALFORMED) == 0);
        TypeDeclaration type = findTypeDeclaration(astRoot, "E");
        MethodDeclaration methodDecl = findMethodDeclaration(type, "foo");
        Block block = methodDecl.getBody();
        List blockStatements = block.statements();
        assertTrue("Number of statements not 5", blockStatements.size() == 5);
        {
            ListRewrite listRewrite = rewrite.getListRewrite(block, Block.STATEMENTS_PROPERTY);
            listRewrite.remove((ASTNode)blockStatements.get(1), null);
            listRewrite.remove((ASTNode)blockStatements.get(2), null);
        }

        String preview = evaluateRewrite(cu, rewrite);

        buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public void foo(int i) {\n");
        buf.append("        foo(0);\n");
        buf.append("        foo(3);\n");
        buf.append("        foo(4);\n");
        buf.append("    }\n");
        buf.append("}\n");
        assertEqualString(preview, buf.toString());
    }

    @Test
    // https://bugs.eclipse.org/bugs/show_bug.cgi?id=276938
    public void testRemoveStatement10() throws Exception {
        StringBuffer buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public void foo(int i) {\n");
        buf.append("        foo(0);\n");
        buf.append("        foo(1);foo(2);\n");
        buf.append("        foo(3);\n");
        buf.append("    }\n");
        buf.append("}\n");
        ICompilationUnit cu =
                new com.codenvy.ide.ext.java.jdt.compiler.batch.CompilationUnit(buf.toString().toCharArray(), "E.java", "");

        CompilationUnit astRoot = createAST(cu);
        ASTRewrite rewrite = ASTRewrite.create(astRoot.getAST());

        AST ast = astRoot.getAST();

        assertTrue("Parse errors", (astRoot.getFlags() & ASTNode.MALFORMED) == 0);
        TypeDeclaration type = findTypeDeclaration(astRoot, "E");
        MethodDeclaration methodDecl = findMethodDeclaration(type, "foo");
        Block block = methodDecl.getBody();
        List blockStatements = block.statements();
        assertTrue("Number of statements not 4", blockStatements.size() == 4);
        {
            ASTNode statement = (ASTNode)blockStatements.get(2);

            ListRewrite listRewrite = rewrite.getListRewrite(block, Block.STATEMENTS_PROPERTY);
            listRewrite.insertBefore(ast.newBreakStatement(), statement, null);
            listRewrite.remove(statement, null);
        }

        String preview = evaluateRewrite(cu, rewrite);

        buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public void foo(int i) {\n");
        buf.append("        foo(0);\n");
        buf.append("        foo(1);break;\n");
        buf.append("        foo(3);\n");
        buf.append("    }\n");
        buf.append("}\n");
        assertEqualString(preview, buf.toString());
    }

    @Test
    // https://bugs.eclipse.org/bugs/show_bug.cgi?id=276938
    public void testRemoveStatement11() throws Exception {
        StringBuffer buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public void foo(int i) {\n");
        buf.append("        foo(0);\n");
        buf.append("        foo(1);foo(2);\n");
        buf.append("        foo(3);\n");
        buf.append("    }\n");
        buf.append("}\n");
        ICompilationUnit cu =
                new com.codenvy.ide.ext.java.jdt.compiler.batch.CompilationUnit(buf.toString().toCharArray(), "E.java", "");

        CompilationUnit astRoot = createAST(cu);
        ASTRewrite rewrite = ASTRewrite.create(astRoot.getAST());

        AST ast = astRoot.getAST();

        assertTrue("Parse errors", (astRoot.getFlags() & ASTNode.MALFORMED) == 0);
        TypeDeclaration type = findTypeDeclaration(astRoot, "E");
        MethodDeclaration methodDecl = findMethodDeclaration(type, "foo");
        Block block = methodDecl.getBody();
        List blockStatements = block.statements();
        assertTrue("Number of statements not 4", blockStatements.size() == 4);
        {
            ASTNode statement = (ASTNode)blockStatements.get(2);

            ListRewrite listRewrite = rewrite.getListRewrite(block, Block.STATEMENTS_PROPERTY);
            listRewrite.insertAfter(ast.newBreakStatement(), statement, null);
            listRewrite.remove(statement, null);
        }

        String preview = evaluateRewrite(cu, rewrite);

        buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public void foo(int i) {\n");
        buf.append("        foo(0);\n");
        buf.append("        foo(1);\n");
        buf.append("        break;\n");
        buf.append("        foo(3);\n");
        buf.append("    }\n");
        buf.append("}\n");
        assertEqualString(preview, buf.toString());
    }

    @Test
    // https://bugs.eclipse.org/bugs/show_bug.cgi?id=276938
    public void testRemoveStatement12() throws Exception {
        StringBuffer buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public void foo(int i) {\n");
        buf.append("        foo(0);\n");
        buf.append("        foo(1);foo(2);\n");
        buf.append("        foo(3);\n");
        buf.append("    }\n");
        buf.append("}\n");
        ICompilationUnit cu =
                new com.codenvy.ide.ext.java.jdt.compiler.batch.CompilationUnit(buf.toString().toCharArray(), "E.java", "");

        CompilationUnit astRoot = createAST(cu);
        ASTRewrite rewrite = ASTRewrite.create(astRoot.getAST());

        AST ast = astRoot.getAST();

        assertTrue("Parse errors", (astRoot.getFlags() & ASTNode.MALFORMED) == 0);
        TypeDeclaration type = findTypeDeclaration(astRoot, "E");
        MethodDeclaration methodDecl = findMethodDeclaration(type, "foo");
        Block block = methodDecl.getBody();
        List blockStatements = block.statements();
        assertTrue("Number of statements not 4", blockStatements.size() == 4);
        {
            ListRewrite listRewrite = rewrite.getListRewrite(block, Block.STATEMENTS_PROPERTY);
            listRewrite.insertBefore(ast.newBreakStatement(), (ASTNode)blockStatements.get(3), null);
            listRewrite.remove((ASTNode)blockStatements.get(2), null);
        }

        String preview = evaluateRewrite(cu, rewrite);

        buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public void foo(int i) {\n");
        buf.append("        foo(0);\n");
        buf.append("        foo(1);\n");
        buf.append("        break;\n");
        buf.append("        foo(3);\n");
        buf.append("    }\n");
        buf.append("}\n");
        assertEqualString(preview, buf.toString());
    }

    @Test
    public void testReplace1() throws Exception {
      /* foo(): if.. -> return; */
        StringBuffer buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class C {\n");
        buf.append("    public Object foo() {\n");
        buf.append("        if (this.equals(new Object())) {\n");
        buf.append("            toString();\n");
        buf.append("        }\n");
        buf.append("    }\n");
        buf.append("}\n");
        ICompilationUnit cu =
                new com.codenvy.ide.ext.java.jdt.compiler.batch.CompilationUnit(buf.toString().toCharArray(), "C.java", "");

        CompilationUnit astRoot = createAST(cu);
        ASTRewrite rewrite = ASTRewrite.create(astRoot.getAST());
        TypeDeclaration type = findTypeDeclaration(astRoot, "C");

        MethodDeclaration methodDecl = findMethodDeclaration(type, "foo");
        Block block = methodDecl.getBody();
        assertTrue("No block", block != null);

        List statements = block.statements();
        assertTrue("No statements in block", !statements.isEmpty());

        ReturnStatement returnStatement = block.getAST().newReturnStatement();
        rewrite.replace((ASTNode)statements.get(0), returnStatement, null);

        String preview = evaluateRewrite(cu, rewrite);

        buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class C {\n");
        buf.append("    public Object foo() {\n");
        buf.append("        return;\n");
        buf.append("    }\n");
        buf.append("}\n");

        assertEqualString(preview, buf.toString());

    }

    @Test
    public void testReplace2() throws Exception {
      /* goo(): new Integer(3) -> 'null' */
        StringBuffer buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class D {\n");
        buf.append("    public Object goo() {\n");
        buf.append("        return new Integer(3);\n");
        buf.append("    }\n");
        buf.append("    public void hoo(int p1, Object p2) {\n");
        buf.append("        return;\n");
        buf.append("    }\n");
        buf.append("}\n");
        ICompilationUnit cu =
                new com.codenvy.ide.ext.java.jdt.compiler.batch.CompilationUnit(buf.toString().toCharArray(), "D.java", "");

        CompilationUnit astRoot = createAST(cu);
        ASTRewrite rewrite = ASTRewrite.create(astRoot.getAST());
        TypeDeclaration type = findTypeDeclaration(astRoot, "D");

        MethodDeclaration methodDecl = findMethodDeclaration(type, "goo");
        Block block = methodDecl.getBody();
        assertTrue("No block", block != null);

        List statements = block.statements();
        assertTrue("No statements in block", !statements.isEmpty());
        assertTrue("No ReturnStatement", statements.get(0) instanceof ReturnStatement);

        ReturnStatement returnStatement = (ReturnStatement)statements.get(0);
        Expression expr = returnStatement.getExpression();
        Expression modified = block.getAST().newNullLiteral();

        rewrite.replace(expr, modified, null);

        String preview = evaluateRewrite(cu, rewrite);

        buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class D {\n");
        buf.append("    public Object goo() {\n");
        buf.append("        return null;\n");
        buf.append("    }\n");
        buf.append("    public void hoo(int p1, Object p2) {\n");
        buf.append("        return;\n");
        buf.append("    }\n");
        buf.append("}\n");

        assertEqualString(preview, buf.toString());

    }

    @Test
    public void testBreakStatement() throws Exception {
        StringBuffer buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public void foo() {\n");
        buf.append("        break;\n");
        buf.append("        break label;\n");
        buf.append("        break label;\n");
        buf.append("    }\n");
        buf.append("}\n");
        ICompilationUnit cu =
                new com.codenvy.ide.ext.java.jdt.compiler.batch.CompilationUnit(buf.toString().toCharArray(), "E.java", "");

        CompilationUnit astRoot = createAST(cu);
        ASTRewrite rewrite = ASTRewrite.create(astRoot.getAST());

        AST ast = astRoot.getAST();

        assertTrue("Parse errors", (astRoot.getFlags() & ASTNode.MALFORMED) == 0);
        TypeDeclaration type = findTypeDeclaration(astRoot, "E");
        MethodDeclaration methodDecl = findMethodDeclaration(type, "foo");
        Block block = methodDecl.getBody();
        List statements = block.statements();
        assertTrue("Number of statements not 3", statements.size() == 3);
        { // insert label
            BreakStatement statement = (BreakStatement)statements.get(0);
            assertTrue("Has label", statement.getLabel() == null);

            SimpleName newLabel = ast.newSimpleName("label2");
            rewrite.set(statement, BreakStatement.LABEL_PROPERTY, newLabel, null);
        }
        { // replace label
            BreakStatement statement = (BreakStatement)statements.get(1);

            SimpleName label = statement.getLabel();
            assertTrue("Has no label", label != null);

            SimpleName newLabel = ast.newSimpleName("label2");

            rewrite.replace(label, newLabel, null);
        }
        { // remove label
            BreakStatement statement = (BreakStatement)statements.get(2);

            SimpleName label = statement.getLabel();
            assertTrue("Has no label", label != null);

            rewrite.remove(label, null);
        }

        String preview = evaluateRewrite(cu, rewrite);

        buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public void foo() {\n");
        buf.append("        break label2;\n");
        buf.append("        break label2;\n");
        buf.append("        break;\n");
        buf.append("    }\n");
        buf.append("}\n");
        assertEqualString(preview, buf.toString());

    }

    @Test
    public void testConstructorInvocation() throws Exception {
        StringBuffer buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public E(String e, String f) {\n");
        buf.append("        this();\n");
        buf.append("    }\n");
        buf.append("    public E() {\n");
        buf.append("        this(\"Hello\", true);\n");
        buf.append("    }\n");
        buf.append("}\n");
        ICompilationUnit cu =
                new com.codenvy.ide.ext.java.jdt.compiler.batch.CompilationUnit(buf.toString().toCharArray(), "E.java", "");

        CompilationUnit astRoot = createAST(cu);
        ASTRewrite rewrite = ASTRewrite.create(astRoot.getAST());

        AST ast = astRoot.getAST();

        assertTrue("Parse errors", (astRoot.getFlags() & ASTNode.MALFORMED) == 0);
        TypeDeclaration type = findTypeDeclaration(astRoot, "E");
        MethodDeclaration[] declarations = type.getMethods();
        assertTrue("Number of statements not 2", declarations.length == 2);

        { // add parameters
            Block block = declarations[0].getBody();
            List statements = block.statements();
            assertTrue("Number of statements not 1", statements.size() == 1);

            ConstructorInvocation invocation = (ConstructorInvocation)statements.get(0);

            StringLiteral stringLiteral1 = ast.newStringLiteral();
            stringLiteral1.setLiteralValue("Hello");

            StringLiteral stringLiteral2 = ast.newStringLiteral();
            stringLiteral2.setLiteralValue("World");

            ListRewrite listRewrite = rewrite.getListRewrite(invocation, ConstructorInvocation.ARGUMENTS_PROPERTY);
            listRewrite.insertLast(stringLiteral1, null);
            listRewrite.insertLast(stringLiteral2, null);

        }
        { //remove parameters
            Block block = declarations[1].getBody();
            List statements = block.statements();
            assertTrue("Number of statements not 1", statements.size() == 1);
            ConstructorInvocation invocation = (ConstructorInvocation)statements.get(0);

            List arguments = invocation.arguments();

            rewrite.remove((ASTNode)arguments.get(0), null);
            rewrite.remove((ASTNode)arguments.get(1), null);
        }
        String preview = evaluateRewrite(cu, rewrite);

        buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public E(String e, String f) {\n");
        buf.append("        this(\"Hello\", \"World\");\n");
        buf.append("    }\n");
        buf.append("    public E() {\n");
        buf.append("        this();\n");
        buf.append("    }\n");
        buf.append("}\n");
        assertEqualString(preview, buf.toString());

    }

    @Test
    public void testConstructorInvocation2() throws Exception {
        StringBuffer buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public <A, B>E(String e, String f) {\n");
        buf.append("        this();\n");
        buf.append("    }\n");
        buf.append("    public E() {\n");
        buf.append("        <String, String>this(\"Hello\", true);\n");
        buf.append("    }\n");
        buf.append("}\n");
        ICompilationUnit cu =
                new com.codenvy.ide.ext.java.jdt.compiler.batch.CompilationUnit(buf.toString().toCharArray(), "E.java", "");

        CompilationUnit astRoot = createAST3(cu);
        ASTRewrite rewrite = ASTRewrite.create(astRoot.getAST());

        AST ast = astRoot.getAST();

        assertTrue("Parse errors", (astRoot.getFlags() & ASTNode.MALFORMED) == 0);
        TypeDeclaration type = findTypeDeclaration(astRoot, "E");
        MethodDeclaration[] declarations = type.getMethods();
        assertTrue("Number of declarations not 2", declarations.length == 2);

        { // add type argument
            Block block = declarations[0].getBody();
            List statements = block.statements();
            assertTrue("Number of statements not 1", statements.size() == 1);

            Type newTypeArg = ast.newSimpleType(ast.newSimpleName("A"));
            ConstructorInvocation invocation = (ConstructorInvocation)statements.get(0);
            ListRewrite listRewrite = rewrite.getListRewrite(invocation, ConstructorInvocation.TYPE_ARGUMENTS_PROPERTY);
            listRewrite.insertLast(newTypeArg, null);
        }
        { //remove type argument
            Block block = declarations[1].getBody();
            List statements = block.statements();
            assertTrue("Number of statements not 1", statements.size() == 1);
            ConstructorInvocation invocation = (ConstructorInvocation)statements.get(0);

            List typeArguments = invocation.typeArguments();

            rewrite.remove((ASTNode)typeArguments.get(0), null);
            rewrite.remove((ASTNode)typeArguments.get(1), null);
        }
        String preview = evaluateRewrite(cu, rewrite);

        buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public <A, B>E(String e, String f) {\n");
        buf.append("        <A>this();\n");
        buf.append("    }\n");
        buf.append("    public E() {\n");
        buf.append("        this(\"Hello\", true);\n");
        buf.append("    }\n");
        buf.append("}\n");
        assertEqualString(preview, buf.toString());

    }

    @Test
    public void testContinueStatement() throws Exception {
        StringBuffer buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public void foo() {\n");
        buf.append("        continue;\n");
        buf.append("        continue label;\n");
        buf.append("        continue label;\n");
        buf.append("    }\n");
        buf.append("}\n");
        ICompilationUnit cu =
                new com.codenvy.ide.ext.java.jdt.compiler.batch.CompilationUnit(buf.toString().toCharArray(), "E.java", "");

        CompilationUnit astRoot = createAST(cu);
        ASTRewrite rewrite = ASTRewrite.create(astRoot.getAST());

        AST ast = astRoot.getAST();

        assertTrue("Parse errors", (astRoot.getFlags() & ASTNode.MALFORMED) == 0);
        TypeDeclaration type = findTypeDeclaration(astRoot, "E");
        MethodDeclaration methodDecl = findMethodDeclaration(type, "foo");
        Block block = methodDecl.getBody();
        List statements = block.statements();
        assertTrue("Number of statements not 3", statements.size() == 3);
        { // insert label
            ContinueStatement statement = (ContinueStatement)statements.get(0);
            assertTrue("Has label", statement.getLabel() == null);

            SimpleName newLabel = ast.newSimpleName("label2");

            rewrite.set(statement, ContinueStatement.LABEL_PROPERTY, newLabel, null);
        }
        { // replace label
            ContinueStatement statement = (ContinueStatement)statements.get(1);

            SimpleName label = statement.getLabel();
            assertTrue("Has no label", label != null);

            SimpleName newLabel = ast.newSimpleName("label2");

            rewrite.replace(label, newLabel, null);
        }
        { // remove label
            ContinueStatement statement = (ContinueStatement)statements.get(2);

            SimpleName label = statement.getLabel();
            assertTrue("Has no label", label != null);

            rewrite.remove(label, null);
        }

        String preview = evaluateRewrite(cu, rewrite);

        buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public void foo() {\n");
        buf.append("        continue label2;\n");
        buf.append("        continue label2;\n");
        buf.append("        continue;\n");
        buf.append("    }\n");
        buf.append("}\n");
        assertEqualString(preview, buf.toString());

    }

    @Test
    public void testDoStatement() throws Exception {
        StringBuffer buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public void foo() {\n");
        buf.append("        do {\n");
        buf.append("            System.beep();\n");
        buf.append("        } while (i == j);\n");
        buf.append("    }\n");
        buf.append("}\n");
        ICompilationUnit cu =
                new com.codenvy.ide.ext.java.jdt.compiler.batch.CompilationUnit(buf.toString().toCharArray(), "E.java", "");

        CompilationUnit astRoot = createAST(cu);
        ASTRewrite rewrite = ASTRewrite.create(astRoot.getAST());

        AST ast = astRoot.getAST();

        assertTrue("Parse errors", (astRoot.getFlags() & ASTNode.MALFORMED) == 0);
        TypeDeclaration type = findTypeDeclaration(astRoot, "E");
        MethodDeclaration methodDecl = findMethodDeclaration(type, "foo");
        Block block = methodDecl.getBody();
        List statements = block.statements();
        assertTrue("Number of statements not 1", statements.size() == 1);

        { // replace body and expression
            DoStatement doStatement = (DoStatement)statements.get(0);

            Block newBody = ast.newBlock();

            MethodInvocation invocation = ast.newMethodInvocation();
            invocation.setName(ast.newSimpleName("hoo"));
            invocation.arguments().add(ast.newNumberLiteral("11"));

            newBody.statements().add(ast.newExpressionStatement(invocation));

            rewrite.replace(doStatement.getBody(), newBody, null);

            BooleanLiteral literal = ast.newBooleanLiteral(true);
            rewrite.replace(doStatement.getExpression(), literal, null);
        }

        String preview = evaluateRewrite(cu, rewrite);

        buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public void foo() {\n");
        buf.append("        do {\n");
        buf.append("            hoo(11);\n");
        buf.append("        } while (true);\n");
        buf.append("    }\n");
        buf.append("}\n");
        assertEqualString(preview, buf.toString());

    }

    @Test
    public void testDoStatement1() throws Exception {
        StringBuffer buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public void foo() {\n");
        buf.append("        do {\n");
        buf.append("            foo();\n");
        buf.append("        } while (true);\n");
        buf.append("        do\n");
        buf.append("            foo();\n");
        buf.append("        while (true);\n");
        buf.append("        do {\n");
        buf.append("            foo();\n");
        buf.append("        } while (true);\n");
        buf.append("        do\n");
        buf.append("            foo();\n");
        buf.append("        while (true);\n");
        buf.append("    }\n");
        buf.append("}\n");
        ICompilationUnit cu =
                new com.codenvy.ide.ext.java.jdt.compiler.batch.CompilationUnit(buf.toString().toCharArray(), "E.java", "");

        CompilationUnit astRoot = createAST(cu);
        ASTRewrite rewrite = ASTRewrite.create(astRoot.getAST());
        AST ast = astRoot.getAST();

        TypeDeclaration type = findTypeDeclaration(astRoot, "E");
        MethodDeclaration methodDecl = findMethodDeclaration(type, "foo");
        Block block = methodDecl.getBody();
        assertTrue("Parse errors", (block.getFlags() & ASTNode.MALFORMED) == 0);

        List statements = block.statements();
        assertTrue("Number of statements not 4", statements.size() == 4);

        { // replace body block with statement
            DoStatement doStatement = (DoStatement)statements.get(0);

            TryStatement newTry = ast.newTryStatement();
            newTry.getBody().statements().add(ast.newReturnStatement());
            CatchClause newCatchClause = ast.newCatchClause();
            SingleVariableDeclaration varDecl = ast.newSingleVariableDeclaration();
            varDecl.setType(ast.newSimpleType(ast.newSimpleName("Exception")));
            varDecl.setName(ast.newSimpleName("e"));
            newCatchClause.setException(varDecl);
            newTry.catchClauses().add(newCatchClause);

            rewrite.replace(doStatement.getBody(), newTry, null);
        }
        { // replace body statement with block
            DoStatement doStatement = (DoStatement)statements.get(1);

            Block newBody = ast.newBlock();

            MethodInvocation invocation = ast.newMethodInvocation();
            invocation.setName(ast.newSimpleName("hoo"));
            invocation.arguments().add(ast.newNumberLiteral("11"));

            newBody.statements().add(ast.newExpressionStatement(invocation));

            rewrite.replace(doStatement.getBody(), newBody, null);

        }
        { // replace body block with block
            DoStatement doStatement = (DoStatement)statements.get(2);

            Block newBody = ast.newBlock();

            MethodInvocation invocation = ast.newMethodInvocation();
            invocation.setName(ast.newSimpleName("hoo"));
            invocation.arguments().add(ast.newNumberLiteral("11"));

            newBody.statements().add(ast.newExpressionStatement(invocation));

            rewrite.replace(doStatement.getBody(), newBody, null);

        }
        { // replace body statement with body
            DoStatement doStatement = (DoStatement)statements.get(3);

            TryStatement newTry = ast.newTryStatement();
            newTry.getBody().statements().add(ast.newReturnStatement());
            CatchClause newCatchClause = ast.newCatchClause();
            SingleVariableDeclaration varDecl = ast.newSingleVariableDeclaration();
            varDecl.setType(ast.newSimpleType(ast.newSimpleName("Exception")));
            varDecl.setName(ast.newSimpleName("e"));
            newCatchClause.setException(varDecl);
            newTry.catchClauses().add(newCatchClause);

            rewrite.replace(doStatement.getBody(), newTry, null);
        }

        String preview = evaluateRewrite(cu, rewrite);

        buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public void foo() {\n");
        buf.append("        do\n");
        buf.append("            try {\n");
        buf.append("                return;\n");
        buf.append("            } catch (Exception e) {\n");
        buf.append("            }\n");
        buf.append("        while (true);\n");
        buf.append("        do {\n");
        buf.append("            hoo(11);\n");
        buf.append("        } while (true);\n");
        buf.append("        do {\n");
        buf.append("            hoo(11);\n");
        buf.append("        } while (true);\n");
        buf.append("        do\n");
        buf.append("            try {\n");
        buf.append("                return;\n");
        buf.append("            } catch (Exception e) {\n");
        buf.append("            }\n");
        buf.append("        while (true);\n");
        buf.append("    }\n");
        buf.append("}\n");
        assertEqualString(preview, buf.toString());

    }

    @Test
    public void testDoStatement2() throws Exception {
        StringBuffer buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public void foo() {\n");
        buf.append("        do {\n");
        buf.append("            foo();\n");
        buf.append("        } while (true);\n");
        buf.append("    }\n");
        buf.append("}\n");
        ICompilationUnit cu =
                new com.codenvy.ide.ext.java.jdt.compiler.batch.CompilationUnit(buf.toString().toCharArray(), "E.java", "");

        CompilationUnit astRoot = createAST(AST.JLS4, cu, true);
        ASTRewrite rewrite = ASTRewrite.create(astRoot.getAST());
        AST ast = astRoot.getAST();

        TypeDeclaration type = findTypeDeclaration(astRoot, "E");
        MethodDeclaration methodDecl = findMethodDeclaration(type, "foo");
        Block block = methodDecl.getBody();
        assertTrue("Parse errors", (block.getFlags() & ASTNode.MALFORMED) == 0);

        List statements = block.statements();
        assertTrue("Number of statements not 1", statements.size() == 1);

        { // replace body statement with body
            DoStatement doStatement = (DoStatement)statements.get(0);

            TryStatement newTry = ast.newTryStatement();
            newTry.getBody().statements().add(ast.newReturnStatement());
            CatchClause newCatchClause = ast.newCatchClause();
            SingleVariableDeclaration varDecl = ast.newSingleVariableDeclaration();
            varDecl.setType(ast.newSimpleType(ast.newSimpleName("Exception")));
            varDecl.setName(ast.newSimpleName("e"));
            newCatchClause.setException(varDecl);
            newTry.catchClauses().add(newCatchClause);
            VariableDeclarationFragment fragment = ast.newVariableDeclarationFragment();
            fragment.setName(ast.newSimpleName("reader2"));
            fragment.setInitializer(ast.newNullLiteral());
            VariableDeclarationExpression resource = ast.newVariableDeclarationExpression(fragment);
            resource.setType(ast.newSimpleType(ast.newSimpleName("Reader")));

            rewrite.getListRewrite(newTry, TryStatement.RESOURCES_PROPERTY).insertLast(resource, null);

            rewrite.replace(doStatement.getBody(), newTry, null);
        }

        String preview = evaluateRewrite(cu, rewrite);

        buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public void foo() {\n");
        buf.append("        do\n");
        buf.append("            try (Reader reader2 = null) {\n");
        buf.append("                return;\n");
        buf.append("            } catch (Exception e) {\n");
        buf.append("            }\n");
        buf.append("        while (true);\n");
        buf.append("    }\n");
        buf.append("}\n");
        assertEqualString(preview, buf.toString());
    }

    @Test
    public void testExpressionStatement() throws Exception {
        StringBuffer buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public void foo() {\n");
        buf.append("        i= 0;\n");
        buf.append("    }\n");
        buf.append("}\n");
        ICompilationUnit cu =
                new com.codenvy.ide.ext.java.jdt.compiler.batch.CompilationUnit(buf.toString().toCharArray(), "E.java", "");

        CompilationUnit astRoot = createAST(cu);
        ASTRewrite rewrite = ASTRewrite.create(astRoot.getAST());

        AST ast = astRoot.getAST();

        TypeDeclaration type = findTypeDeclaration(astRoot, "E");
        MethodDeclaration methodDecl = findMethodDeclaration(type, "foo");
        Block block = methodDecl.getBody();
        assertTrue("Parse errors", (block.getFlags() & ASTNode.MALFORMED) == 0);

        List statements = block.statements();
        assertTrue("Number of statements not 1", statements.size() == 1);
        { // replace expression
            ExpressionStatement stmt = (ExpressionStatement)statements.get(0);

            Assignment assignment = (Assignment)stmt.getExpression();
            Expression placeholder = (Expression)rewrite.createCopyTarget(assignment);

            Assignment newExpression = ast.newAssignment();
            newExpression.setLeftHandSide(ast.newSimpleName("x"));
            newExpression.setRightHandSide(placeholder);
            newExpression.setOperator(Assignment.Operator.ASSIGN);

            rewrite.replace(stmt.getExpression(), newExpression, null);
        }

        String preview = evaluateRewrite(cu, rewrite);

        buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public void foo() {\n");
        buf.append("        x = i= 0;\n");
        buf.append("    }\n");
        buf.append("}\n");
        assertEqualString(preview, buf.toString());

    }

    @Test
    public void testForStatement() throws Exception {
        StringBuffer buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public void foo() {\n");
        buf.append("        for (int i= 0; i < len; i++) {\n");
        buf.append("        }\n");
        buf.append("        for (i= 0, j= 0; i < len; i++, j++) {\n");
        buf.append("        }\n");
        buf.append("        for (;;) {\n");
        buf.append("        }\n");
        buf.append("        for (;;) {\n");
        buf.append("        }\n");
        buf.append("        for (i= 0; i < len; i++) {\n");
        buf.append("        }\n");
        buf.append("    }\n");
        buf.append("}\n");
        ICompilationUnit cu =
                new com.codenvy.ide.ext.java.jdt.compiler.batch.CompilationUnit(buf.toString().toCharArray(), "E.java", "");

        CompilationUnit astRoot = createAST(cu);
        ASTRewrite rewrite = ASTRewrite.create(astRoot.getAST());
        AST ast = astRoot.getAST();

        TypeDeclaration type = findTypeDeclaration(astRoot, "E");
        MethodDeclaration methodDecl = findMethodDeclaration(type, "foo");
        Block block = methodDecl.getBody();
        assertTrue("Parse errors", (block.getFlags() & ASTNode.MALFORMED) == 0);

        List statements = block.statements();
        assertTrue("Number of statements not 5", statements.size() == 5);

        { // replace initializer, change expression, add updater, replace cody
            ForStatement forStatement = (ForStatement)statements.get(0);

            List initializers = forStatement.initializers();
            assertTrue("Number of initializers not 1", initializers.size() == 1);

            Assignment assignment = ast.newAssignment();
            assignment.setLeftHandSide(ast.newSimpleName("i"));
            assignment.setOperator(Assignment.Operator.ASSIGN);
            assignment.setRightHandSide(ast.newNumberLiteral("3"));

            rewrite.replace((ASTNode)initializers.get(0), assignment, null);

            Assignment assignment2 = ast.newAssignment();
            assignment2.setLeftHandSide(ast.newSimpleName("j"));
            assignment2.setOperator(Assignment.Operator.ASSIGN);
            assignment2.setRightHandSide(ast.newNumberLiteral("4"));

            rewrite.getListRewrite(forStatement, ForStatement.INITIALIZERS_PROPERTY).insertLast(assignment2, null);

            BooleanLiteral literal = ast.newBooleanLiteral(true);
            rewrite.replace(forStatement.getExpression(), literal, null);

            // add updater
            PrefixExpression prefixExpression = ast.newPrefixExpression();
            prefixExpression.setOperand(ast.newSimpleName("j"));
            prefixExpression.setOperator(PrefixExpression.Operator.INCREMENT);

            rewrite.getListRewrite(forStatement, ForStatement.UPDATERS_PROPERTY).insertLast(prefixExpression, null);

            // replace body
            Block newBody = ast.newBlock();

            MethodInvocation invocation = ast.newMethodInvocation();
            invocation.setName(ast.newSimpleName("hoo"));
            invocation.arguments().add(ast.newNumberLiteral("11"));

            newBody.statements().add(ast.newExpressionStatement(invocation));

            rewrite.replace(forStatement.getBody(), newBody, null);
        }
        { // remove initializers, expression and updaters
            ForStatement forStatement = (ForStatement)statements.get(1);

            List initializers = forStatement.initializers();
            assertTrue("Number of initializers not 2", initializers.size() == 2);

            rewrite.remove((ASTNode)initializers.get(0), null);
            rewrite.remove((ASTNode)initializers.get(1), null);

            rewrite.remove(forStatement.getExpression(), null);

            List updaters = forStatement.updaters();
            assertTrue("Number of initializers not 2", updaters.size() == 2);

            rewrite.remove((ASTNode)updaters.get(0), null);
            rewrite.remove((ASTNode)updaters.get(1), null);
        }
        { // insert updater
            ForStatement forStatement = (ForStatement)statements.get(2);

            PrefixExpression prefixExpression = ast.newPrefixExpression();
            prefixExpression.setOperand(ast.newSimpleName("j"));
            prefixExpression.setOperator(PrefixExpression.Operator.INCREMENT);

            rewrite.getListRewrite(forStatement, ForStatement.UPDATERS_PROPERTY).insertLast(prefixExpression, null);
        }

        { // insert updater & initializer
            ForStatement forStatement = (ForStatement)statements.get(3);

            Assignment assignment = ast.newAssignment();
            assignment.setLeftHandSide(ast.newSimpleName("j"));
            assignment.setOperator(Assignment.Operator.ASSIGN);
            assignment.setRightHandSide(ast.newNumberLiteral("3"));

            rewrite.getListRewrite(forStatement, ForStatement.INITIALIZERS_PROPERTY).insertLast(assignment, null);

            PrefixExpression prefixExpression = ast.newPrefixExpression();
            prefixExpression.setOperand(ast.newSimpleName("j"));
            prefixExpression.setOperator(PrefixExpression.Operator.INCREMENT);

            rewrite.getListRewrite(forStatement, ForStatement.UPDATERS_PROPERTY).insertLast(prefixExpression, null);
        }

        { // replace initializer: turn assignement to var decl
            ForStatement forStatement = (ForStatement)statements.get(4);

            Assignment assignment = (Assignment)forStatement.initializers().get(0);
            SimpleName leftHandSide = (SimpleName)assignment.getLeftHandSide();

            VariableDeclarationFragment varFragment = ast.newVariableDeclarationFragment();
            VariableDeclarationExpression varDecl = ast.newVariableDeclarationExpression(varFragment);
            varFragment.setName(ast.newSimpleName(leftHandSide.getIdentifier()));

            Expression placeholder = (Expression)rewrite.createCopyTarget(assignment.getRightHandSide());
            varFragment.setInitializer(placeholder);
            varDecl.setType(ast.newPrimitiveType(PrimitiveType.INT));

            rewrite.replace(assignment, varDecl, null);
        }

        String preview = evaluateRewrite(cu, rewrite);

        buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public void foo() {\n");
        buf.append("        for (i = 3, j = 4; true; i++, ++j) {\n");
        buf.append("            hoo(11);\n");
        buf.append("        }\n");
        buf.append("        for (;;) {\n");
        buf.append("        }\n");
        buf.append("        for (;;++j) {\n");
        buf.append("        }\n");
        buf.append("        for (j = 3;;++j) {\n");
        buf.append("        }\n");
        buf.append("        for (int i = 0; i < len; i++) {\n");
        buf.append("        }\n");
        buf.append("    }\n");
        buf.append("}\n");
        assertEqualString(preview, buf.toString());

    }

    @Test
    public void testForStatement1() throws Exception {
        StringBuffer buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public void foo() {\n");
        buf.append("        for (;;) {\n");
        buf.append("            foo();\n");
        buf.append("        }\n");
        buf.append("        for (;;)\n");
        buf.append("            foo();\n");
        buf.append("        for (;;) {\n");
        buf.append("            foo();\n");
        buf.append("        }\n");
        buf.append("        for (;;)\n");
        buf.append("            foo();\n");
        buf.append("    }\n");
        buf.append("}\n");
        ICompilationUnit cu =
                new com.codenvy.ide.ext.java.jdt.compiler.batch.CompilationUnit(buf.toString().toCharArray(), "E.java", "");

        CompilationUnit astRoot = createAST(cu);
        ASTRewrite rewrite = ASTRewrite.create(astRoot.getAST());
        AST ast = astRoot.getAST();

        TypeDeclaration type = findTypeDeclaration(astRoot, "E");
        MethodDeclaration methodDecl = findMethodDeclaration(type, "foo");
        Block block = methodDecl.getBody();
        assertTrue("Parse errors", (block.getFlags() & ASTNode.MALFORMED) == 0);

        List statements = block.statements();
        assertTrue("Number of statements not 4", statements.size() == 4);

        { // replace body block with statement
            ForStatement forStatement = (ForStatement)statements.get(0);

            TryStatement newTry = ast.newTryStatement();
            newTry.getBody().statements().add(ast.newReturnStatement());
            CatchClause newCatchClause = ast.newCatchClause();
            SingleVariableDeclaration varDecl = ast.newSingleVariableDeclaration();
            varDecl.setType(ast.newSimpleType(ast.newSimpleName("Exception")));
            varDecl.setName(ast.newSimpleName("e"));
            newCatchClause.setException(varDecl);
            newTry.catchClauses().add(newCatchClause);

            rewrite.replace(forStatement.getBody(), newTry, null);
        }
        { // replace body statement with block
            ForStatement forStatement = (ForStatement)statements.get(1);

            Block newBody = ast.newBlock();

            MethodInvocation invocation = ast.newMethodInvocation();
            invocation.setName(ast.newSimpleName("hoo"));
            invocation.arguments().add(ast.newNumberLiteral("11"));

            newBody.statements().add(ast.newExpressionStatement(invocation));

            rewrite.replace(forStatement.getBody(), newBody, null);

        }
        { // replace body block with block
            ForStatement forStatement = (ForStatement)statements.get(2);

            Block newBody = ast.newBlock();

            MethodInvocation invocation = ast.newMethodInvocation();
            invocation.setName(ast.newSimpleName("hoo"));
            invocation.arguments().add(ast.newNumberLiteral("11"));

            newBody.statements().add(ast.newExpressionStatement(invocation));

            rewrite.replace(forStatement.getBody(), newBody, null);

        }
        { // replace body statement with body
            ForStatement forStatement = (ForStatement)statements.get(3);

            TryStatement newTry = ast.newTryStatement();
            newTry.getBody().statements().add(ast.newReturnStatement());
            CatchClause newCatchClause = ast.newCatchClause();
            SingleVariableDeclaration varDecl = ast.newSingleVariableDeclaration();
            varDecl.setType(ast.newSimpleType(ast.newSimpleName("Exception")));
            varDecl.setName(ast.newSimpleName("e"));
            newCatchClause.setException(varDecl);
            newTry.catchClauses().add(newCatchClause);

            rewrite.replace(forStatement.getBody(), newTry, null);
        }

        String preview = evaluateRewrite(cu, rewrite);

        buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public void foo() {\n");
        buf.append("        for (;;)\n");
        buf.append("            try {\n");
        buf.append("                return;\n");
        buf.append("            } catch (Exception e) {\n");
        buf.append("            }\n");
        buf.append("        for (;;) {\n");
        buf.append("            hoo(11);\n");
        buf.append("        }\n");
        buf.append("        for (;;) {\n");
        buf.append("            hoo(11);\n");
        buf.append("        }\n");
        buf.append("        for (;;)\n");
        buf.append("            try {\n");
        buf.append("                return;\n");
        buf.append("            } catch (Exception e) {\n");
        buf.append("            }\n");
        buf.append("    }\n");
        buf.append("}\n");
        assertEqualString(preview, buf.toString());

    }

    @Test
    public void testForStatement2() throws Exception {
        StringBuffer buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public void foo() {\n");
        buf.append("        for (;;) {\n");
        buf.append("        }\n");
        buf.append("    }\n");
        buf.append("}\n");
        ICompilationUnit cu =
                new com.codenvy.ide.ext.java.jdt.compiler.batch.CompilationUnit(buf.toString().toCharArray(), "E.java", "");

        CompilationUnit astRoot = createAST(cu);
        ASTRewrite rewrite = ASTRewrite.create(astRoot.getAST());
        AST ast = astRoot.getAST();

        TypeDeclaration type = findTypeDeclaration(astRoot, "E");
        MethodDeclaration methodDecl = findMethodDeclaration(type, "foo");
        Block block = methodDecl.getBody();
        assertTrue("Parse errors", (block.getFlags() & ASTNode.MALFORMED) == 0);

        List statements = block.statements();
        assertTrue("Number of statements not 1", statements.size() == 1);

        { // replace for statement
            ForStatement forStatement = (ForStatement)statements.get(0);

            ForStatement newForStatement = ast.newForStatement();
            List initializers = newForStatement.initializers();

            Assignment init1 = ast.newAssignment();
            init1.setLeftHandSide(ast.newSimpleName("x"));
            init1.setRightHandSide(ast.newNumberLiteral("1"));
            initializers.add(init1);

            Assignment init2 = ast.newAssignment();
            init2.setLeftHandSide(ast.newSimpleName("y"));
            init2.setRightHandSide(ast.newNumberLiteral("10"));
            initializers.add(init2);

            InfixExpression expression = ast.newInfixExpression();
            expression.setOperator(InfixExpression.Operator.LESS);
            expression.setRightOperand(ast.newSimpleName("y"));
            expression.setLeftOperand(ast.newSimpleName("x"));
            newForStatement.setExpression(expression);

            List updaters = newForStatement.updaters();
            PrefixExpression upd1 = ast.newPrefixExpression();
            upd1.setOperator(PrefixExpression.Operator.INCREMENT);
            upd1.setOperand(ast.newSimpleName("x"));
            updaters.add(upd1);

            PrefixExpression upd2 = ast.newPrefixExpression();
            upd2.setOperator(PrefixExpression.Operator.DECREMENT);
            upd2.setOperand(ast.newSimpleName("y"));
            updaters.add(upd2);

            newForStatement.setBody(ast.newBlock());
            rewrite.replace(forStatement, newForStatement, null);
        }

        String preview = evaluateRewrite(cu, rewrite);

        buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public void foo() {\n");
        buf.append("        for (x = 1, y = 10; x < y; ++x, --y) {\n");
        buf.append("        }\n");
        buf.append("    }\n");
        buf.append("}\n");
        assertEqualString(preview, buf.toString());
    }

    @Test
    public void testIfStatement() throws Exception {
        StringBuffer buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public void foo() {\n");
        buf.append("        if (i == 0) {\n");
        buf.append("            System.beep();\n");
        buf.append("        } else {\n");
        buf.append("        }\n");
        buf.append("        if (i == 0) {\n");
        buf.append("            System.beep();\n");
        buf.append("        }\n");
        buf.append("    }\n");
        buf.append("}\n");
        ICompilationUnit cu =
                new com.codenvy.ide.ext.java.jdt.compiler.batch.CompilationUnit(buf.toString().toCharArray(), "E.java", "");

        CompilationUnit astRoot = createAST(cu);
        ASTRewrite rewrite = ASTRewrite.create(astRoot.getAST());

        AST ast = astRoot.getAST();

        assertTrue("Parse errors", (astRoot.getFlags() & ASTNode.MALFORMED) == 0);
        TypeDeclaration type = findTypeDeclaration(astRoot, "E");
        MethodDeclaration methodDecl = findMethodDeclaration(type, "foo");
        Block block = methodDecl.getBody();
        List statements = block.statements();
        assertTrue("Number of statements not 2", statements.size() == 2);

        { // replace expression body and then body, remove else body
            IfStatement ifStatement = (IfStatement)statements.get(0);

            BooleanLiteral literal = ast.newBooleanLiteral(true);
            rewrite.replace(ifStatement.getExpression(), literal, null);

            MethodInvocation invocation = ast.newMethodInvocation();
            invocation.setName(ast.newSimpleName("hoo"));
            invocation.arguments().add(ast.newNumberLiteral("11"));
            Block newBody = ast.newBlock();
            newBody.statements().add(ast.newExpressionStatement(invocation));

            rewrite.replace(ifStatement.getThenStatement(), newBody, null);

            rewrite.remove(ifStatement.getElseStatement(), null);
        }
        { // add else body
            IfStatement ifStatement = (IfStatement)statements.get(1);

            MethodInvocation invocation = ast.newMethodInvocation();
            invocation.setName(ast.newSimpleName("hoo"));
            invocation.arguments().add(ast.newNumberLiteral("11"));
            Block newBody = ast.newBlock();
            newBody.statements().add(ast.newExpressionStatement(invocation));

            rewrite.set(ifStatement, IfStatement.ELSE_STATEMENT_PROPERTY, newBody, null);
        }

        String preview = evaluateRewrite(cu, rewrite);

        buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public void foo() {\n");
        buf.append("        if (true) {\n");
        buf.append("            hoo(11);\n");
        buf.append("        }\n");
        buf.append("        if (i == 0) {\n");
        buf.append("            System.beep();\n");
        buf.append("        } else {\n");
        buf.append("            hoo(11);\n");
        buf.append("        }\n");
        buf.append("    }\n");
        buf.append("}\n");
        assertEqualString(preview, buf.toString());

    }

    @Test
    public void testIfStatement1() throws Exception {
        StringBuffer buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public void foo() {\n");
        buf.append("        if (i == 0) {\n");
        buf.append("            System.beep();\n");
        buf.append("        }\n");
        buf.append("        if (i == 0) {\n");
        buf.append("            System.beep();\n");
        buf.append("        } else {\n");
        buf.append("            hoo(11);\n");
        buf.append("        }\n");
        buf.append("        if (i == 0) {\n");
        buf.append("            System.beep();\n");
        buf.append("        } else\n");
        buf.append("            hoo(11);\n");
        buf.append("    }\n");
        buf.append("}\n");
        ICompilationUnit cu =
                new com.codenvy.ide.ext.java.jdt.compiler.batch.CompilationUnit(buf.toString().toCharArray(), "E.java", "");

        CompilationUnit astRoot = createAST(cu);
        ASTRewrite rewrite = ASTRewrite.create(astRoot.getAST());

        assertTrue("Parse errors", (astRoot.getFlags() & ASTNode.MALFORMED) == 0);
        TypeDeclaration type = findTypeDeclaration(astRoot, "E");
        MethodDeclaration methodDecl = findMethodDeclaration(type, "foo");
        Block block = methodDecl.getBody();
        List statements = block.statements();
        assertTrue("Number of statements not 5", statements.size() == 3);

        { // replace then block by statement
            IfStatement ifStatement = (IfStatement)statements.get(0);

            Block body = (Block)ifStatement.getThenStatement();
            ASTNode statement = (ASTNode)body.statements().get(0);

            ASTNode newBody = rewrite.createMoveTarget(statement);

            rewrite.replace(body, newBody, null);
        }
        { // replace then block by statement
            IfStatement ifStatement = (IfStatement)statements.get(1);

            Block body = (Block)ifStatement.getThenStatement();
            ASTNode statement = (ASTNode)body.statements().get(0);

            ASTNode newBody = rewrite.createMoveTarget(statement);

            rewrite.replace(body, newBody, null);
        }
        { // replace then block by statement
            IfStatement ifStatement = (IfStatement)statements.get(2);

            Block body = (Block)ifStatement.getThenStatement();
            ASTNode statement = (ASTNode)body.statements().get(0);

            ASTNode newBody = rewrite.createMoveTarget(statement);

            rewrite.replace(body, newBody, null);
        }

        String preview = evaluateRewrite(cu, rewrite);

        buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public void foo() {\n");
        buf.append("        if (i == 0)\n");
        buf.append("            System.beep();\n");
        buf.append("        if (i == 0)\n");
        buf.append("            System.beep();\n");
        buf.append("        else {\n");
        buf.append("            hoo(11);\n");
        buf.append("        }\n");
        buf.append("        if (i == 0)\n");
        buf.append("            System.beep();\n");
        buf.append("        else\n");
        buf.append("            hoo(11);\n");
        buf.append("    }\n");
        buf.append("}\n");
        assertEqualString(preview, buf.toString());

    }

    @Test
    public void testIfStatement2() throws Exception {
        StringBuffer buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public void foo() {\n");
        buf.append("        if (i == 0) {\n");
        buf.append("            System.beep();\n");
        buf.append("        }\n");
        buf.append("        if (i == 0) {\n");
        buf.append("            System.beep();\n");
        buf.append("        } else {\n");
        buf.append("            hoo(11);\n");
        buf.append("        }\n");
        buf.append("        if (i == 0) {\n");
        buf.append("            System.beep();\n");
        buf.append("        }\n");
        buf.append("        if (i == 0) {\n");
        buf.append("            System.beep();\n");
        buf.append("        } else\n");
        buf.append("            hoo(11);\n");
        buf.append("    }\n");
        buf.append("}\n");
        ICompilationUnit cu =
                new com.codenvy.ide.ext.java.jdt.compiler.batch.CompilationUnit(buf.toString().toCharArray(), "E.java", "");

        CompilationUnit astRoot = createAST(cu);
        ASTRewrite rewrite = ASTRewrite.create(astRoot.getAST());
        AST ast = astRoot.getAST();

        assertTrue("Parse errors", (astRoot.getFlags() & ASTNode.MALFORMED) == 0);
        TypeDeclaration type = findTypeDeclaration(astRoot, "E");
        MethodDeclaration methodDecl = findMethodDeclaration(type, "foo");
        Block block = methodDecl.getBody();
        List statements = block.statements();
        assertTrue("Number of statements not 4", statements.size() == 4);

        { // replace then block by statement, add else statement
            IfStatement ifStatement = (IfStatement)statements.get(0);

            Block body = (Block)ifStatement.getThenStatement();
            ASTNode statement = (ASTNode)body.statements().get(0);

            ASTNode newBody = rewrite.createMoveTarget(statement);

            rewrite.replace(body, newBody, null);

            Statement returnStatement = ast.newReturnStatement();
            rewrite.set(ifStatement, IfStatement.ELSE_STATEMENT_PROPERTY, returnStatement, null);

        }
        { // replace then block by statement, remove else statement
            IfStatement ifStatement = (IfStatement)statements.get(1);

            Block body = (Block)ifStatement.getThenStatement();
            ASTNode statement = (ASTNode)body.statements().get(0);

            ASTNode newBody = rewrite.createMoveTarget(statement);

            rewrite.replace(body, newBody, null);
            rewrite.remove(ifStatement.getElseStatement(), null);
        }
        { // replace then block by statement, add else statement
            IfStatement ifStatement = (IfStatement)statements.get(2);

            Block body = (Block)ifStatement.getThenStatement();
            ASTNode statement = (ASTNode)body.statements().get(0);

            ASTNode newBody = rewrite.createMoveTarget(statement);

            rewrite.replace(body, newBody, null);

            Block newElseBlock = ast.newBlock();
            Statement returnStatement = ast.newReturnStatement();
            newElseBlock.statements().add(returnStatement);

            rewrite.set(ifStatement, IfStatement.ELSE_STATEMENT_PROPERTY, newElseBlock, null);

        }
        { // replace then block by statement, remove else statement
            IfStatement ifStatement = (IfStatement)statements.get(3);

            Block body = (Block)ifStatement.getThenStatement();
            ASTNode statement = (ASTNode)body.statements().get(0);

            ASTNode newBody = rewrite.createMoveTarget(statement);

            rewrite.replace(body, newBody, null);
            rewrite.remove(ifStatement.getElseStatement(), null);
        }
        String preview = evaluateRewrite(cu, rewrite);

        buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public void foo() {\n");
        buf.append("        if (i == 0)\n");
        buf.append("            System.beep();\n");
        buf.append("        else\n");
        buf.append("            return;\n");
        buf.append("        if (i == 0)\n");
        buf.append("            System.beep();\n");
        buf.append("        if (i == 0)\n");
        buf.append("            System.beep();\n");
        buf.append("        else {\n");
        buf.append("            return;\n");
        buf.append("        }\n");
        buf.append("        if (i == 0)\n");
        buf.append("            System.beep();\n");
        buf.append("    }\n");
        buf.append("}\n");
        assertEqualString(preview, buf.toString());

    }

    @Test
    public void testIfStatement3() throws Exception {
        StringBuffer buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public void foo() {\n");
        buf.append("        if (i == 0)\n");
        buf.append("            System.beep();\n");
        buf.append("        if (i == 0)\n");
        buf.append("            System.beep();\n");
        buf.append("        else {\n");
        buf.append("            hoo(11);\n");
        buf.append("        }\n");
        buf.append("        if (i == 0)\n");
        buf.append("            System.beep();\n");
        buf.append("        else\n");
        buf.append("            hoo(11);\n");
        buf.append("    }\n");
        buf.append("}\n");
        ICompilationUnit cu =
                new com.codenvy.ide.ext.java.jdt.compiler.batch.CompilationUnit(buf.toString().toCharArray(), "E.java", "");

        CompilationUnit astRoot = createAST(cu);
        ASTRewrite rewrite = ASTRewrite.create(astRoot.getAST());
        AST ast = astRoot.getAST();

        assertTrue("Parse errors", (astRoot.getFlags() & ASTNode.MALFORMED) == 0);
        TypeDeclaration type = findTypeDeclaration(astRoot, "E");
        MethodDeclaration methodDecl = findMethodDeclaration(type, "foo");
        Block block = methodDecl.getBody();
        List statements = block.statements();
        assertTrue("Number of statements not 3", statements.size() == 3);

        { // replace then block by statement
            IfStatement ifStatement = (IfStatement)statements.get(0);

            ASTNode statement = ifStatement.getThenStatement();

            ASTNode placeholder = rewrite.createMoveTarget(statement);
            Block newBody = ast.newBlock();
            newBody.statements().add(placeholder);

            rewrite.replace(statement, newBody, null);
        }
        { // replace then block by statement
            IfStatement ifStatement = (IfStatement)statements.get(1);

            ASTNode statement = ifStatement.getThenStatement();

            ASTNode placeholder = rewrite.createMoveTarget(statement);
            Block newBody = ast.newBlock();
            newBody.statements().add(placeholder);

            rewrite.replace(statement, newBody, null);
        }
        { // replace then block by statement
            IfStatement ifStatement = (IfStatement)statements.get(2);

            ASTNode statement = ifStatement.getThenStatement();

            ASTNode placeholder = rewrite.createMoveTarget(statement);
            Block newBody = ast.newBlock();
            newBody.statements().add(placeholder);

            rewrite.replace(statement, newBody, null);
        }

        String preview = evaluateRewrite(cu, rewrite);

        buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public void foo() {\n");
        buf.append("        if (i == 0) {\n");
        buf.append("            System.beep();\n");
        buf.append("        }\n");
        buf.append("        if (i == 0) {\n");
        buf.append("            System.beep();\n");
        buf.append("        } else {\n");
        buf.append("            hoo(11);\n");
        buf.append("        }\n");
        buf.append("        if (i == 0) {\n");
        buf.append("            System.beep();\n");
        buf.append("        } else\n");
        buf.append("            hoo(11);\n");
        buf.append("    }\n");
        buf.append("}\n");
        assertEqualString(preview, buf.toString());

    }

    @Test
    public void testIfStatement4() throws Exception {
        StringBuffer buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public void foo() {\n");
        buf.append("        if (i == 0)\n");
        buf.append("            System.beep();\n");
        buf.append("        if (i == 0)\n");
        buf.append("            System.beep();\n");
        buf.append("        else {\n");
        buf.append("            hoo(11);\n");
        buf.append("        }\n");
        buf.append("        if (i == 0)\n");
        buf.append("            System.beep();\n");
        buf.append("        if (i == 0)\n");
        buf.append("            System.beep();\n");
        buf.append("        else\n");
        buf.append("            hoo(11);\n");
        buf.append("    }\n");
        buf.append("}\n");
        ICompilationUnit cu =
                new com.codenvy.ide.ext.java.jdt.compiler.batch.CompilationUnit(buf.toString().toCharArray(), "E.java", "");

        CompilationUnit astRoot = createAST(cu);
        ASTRewrite rewrite = ASTRewrite.create(astRoot.getAST());
        AST ast = astRoot.getAST();

        assertTrue("Parse errors", (astRoot.getFlags() & ASTNode.MALFORMED) == 0);
        TypeDeclaration type = findTypeDeclaration(astRoot, "E");
        MethodDeclaration methodDecl = findMethodDeclaration(type, "foo");
        Block block = methodDecl.getBody();
        List statements = block.statements();
        assertTrue("Number of statements not 4", statements.size() == 4);

        { // replace then statement by block , add else statement
            IfStatement ifStatement = (IfStatement)statements.get(0);

            ASTNode statement = ifStatement.getThenStatement();

            ASTNode placeholder = rewrite.createMoveTarget(statement);
            Block newBody = ast.newBlock();
            newBody.statements().add(placeholder);

            rewrite.replace(statement, newBody, null);

            Statement returnStatement = ast.newReturnStatement();
            rewrite.set(ifStatement, IfStatement.ELSE_STATEMENT_PROPERTY, returnStatement, null);

        }
        { // replace then statement by block, remove else statement
            IfStatement ifStatement = (IfStatement)statements.get(1);

            ASTNode statement = ifStatement.getThenStatement();

            ASTNode placeholder = rewrite.createMoveTarget(statement);
            Block newBody = ast.newBlock();
            newBody.statements().add(placeholder);

            rewrite.replace(statement, newBody, null);

            rewrite.remove(ifStatement.getElseStatement(), null);
        }
        { // replace then block by statement, add else statement
            IfStatement ifStatement = (IfStatement)statements.get(2);

            ASTNode statement = ifStatement.getThenStatement();

            ASTNode placeholder = rewrite.createMoveTarget(statement);
            Block newBody = ast.newBlock();
            newBody.statements().add(placeholder);

            rewrite.replace(statement, newBody, null);

            Block newElseBlock = ast.newBlock();
            Statement returnStatement = ast.newReturnStatement();
            newElseBlock.statements().add(returnStatement);

            rewrite.set(ifStatement, IfStatement.ELSE_STATEMENT_PROPERTY, newElseBlock, null);
        }
        { // replace then block by statement, remove else statement
            IfStatement ifStatement = (IfStatement)statements.get(3);

            ASTNode statement = ifStatement.getThenStatement();

            ASTNode placeholder = rewrite.createMoveTarget(statement);
            Block newBody = ast.newBlock();
            newBody.statements().add(placeholder);

            rewrite.replace(statement, newBody, null);

            rewrite.remove(ifStatement.getElseStatement(), null);
        }
        String preview = evaluateRewrite(cu, rewrite);

        buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public void foo() {\n");
        buf.append("        if (i == 0) {\n");
        buf.append("            System.beep();\n");
        buf.append("        } else\n");
        buf.append("            return;\n");
        buf.append("        if (i == 0) {\n");
        buf.append("            System.beep();\n");
        buf.append("        }\n");
        buf.append("        if (i == 0) {\n");
        buf.append("            System.beep();\n");
        buf.append("        } else {\n");
        buf.append("            return;\n");
        buf.append("        }\n");
        buf.append("        if (i == 0) {\n");
        buf.append("            System.beep();\n");
        buf.append("        }\n");
        buf.append("    }\n");
        buf.append("}\n");
        assertEqualString(preview, buf.toString());

    }

    @Test
    public void testIfStatement5() throws Exception {
        StringBuffer buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public void foo() {\n");
        buf.append("        if (i == 0)\n");
        buf.append("            System.beep();\n");
        buf.append("        if (i == 0)\n");
        buf.append("            System.beep();\n");
        buf.append("        else if (true) {\n");
        buf.append("            hoo(11);\n");
        buf.append("        }\n");
        buf.append("        if (i == 0)\n");
        buf.append("            System.beep();\n");
        buf.append("        if (i == 0)\n");
        buf.append("            System.beep();\n");
        buf.append("        else if (true)\n");
        buf.append("            hoo(11);\n");
        buf.append("    }\n");
        buf.append("}\n");
        ICompilationUnit cu =
                new com.codenvy.ide.ext.java.jdt.compiler.batch.CompilationUnit(buf.toString().toCharArray(), "E.java", "");

        CompilationUnit astRoot = createAST(cu);
        ASTRewrite rewrite = ASTRewrite.create(astRoot.getAST());
        AST ast = astRoot.getAST();

        assertTrue("Parse errors", (astRoot.getFlags() & ASTNode.MALFORMED) == 0);
        TypeDeclaration type = findTypeDeclaration(astRoot, "E");
        MethodDeclaration methodDecl = findMethodDeclaration(type, "foo");
        Block block = methodDecl.getBody();
        List statements = block.statements();
        assertTrue("Number of statements not 4", statements.size() == 4);

        { // replace then statement by block , add else statement
            IfStatement ifStatement = (IfStatement)statements.get(0);

            ASTNode statement = ifStatement.getThenStatement();

            ASTNode placeholder = rewrite.createMoveTarget(statement);
            Block newBody = ast.newBlock();
            newBody.statements().add(placeholder);

            rewrite.replace(statement, newBody, null);

            IfStatement newElseBlock = ast.newIfStatement();
            newElseBlock.setExpression(ast.newBooleanLiteral(true));

            Block newBody2 = ast.newBlock();
            Statement returnStatement = ast.newReturnStatement();
            newBody2.statements().add(returnStatement);

            newElseBlock.setThenStatement(newBody2);

            rewrite.set(ifStatement, IfStatement.ELSE_STATEMENT_PROPERTY, newElseBlock, null);
        }
        { // replace then statement by block, remove else statement
            IfStatement ifStatement = (IfStatement)statements.get(1);

            ASTNode statement = ifStatement.getThenStatement();

            ASTNode placeholder = rewrite.createMoveTarget(statement);
            Block newBody = ast.newBlock();
            newBody.statements().add(placeholder);

            rewrite.replace(statement, newBody, null);

            rewrite.remove(ifStatement.getElseStatement(), null);
        }
        { // replace then block by statement, add else statement
            IfStatement ifStatement = (IfStatement)statements.get(2);

            ASTNode statement = ifStatement.getThenStatement();

            ASTNode placeholder = rewrite.createMoveTarget(statement);
            Block newBody = ast.newBlock();
            newBody.statements().add(placeholder);

            rewrite.replace(statement, newBody, null);

            IfStatement newElseBlock = ast.newIfStatement();
            newElseBlock.setExpression(ast.newBooleanLiteral(true));

            Statement returnStatement = ast.newReturnStatement();

            newElseBlock.setThenStatement(returnStatement);
            rewrite.set(ifStatement, IfStatement.ELSE_STATEMENT_PROPERTY, newElseBlock, null);
        }
        { // replace then block by statement, remove else statement
            IfStatement ifStatement = (IfStatement)statements.get(3);

            ASTNode statement = ifStatement.getThenStatement();

            ASTNode placeholder = rewrite.createMoveTarget(statement);
            Block newBody = ast.newBlock();
            newBody.statements().add(placeholder);

            rewrite.replace(statement, newBody, null);

            rewrite.remove(ifStatement.getElseStatement(), null);
        }
        String preview = evaluateRewrite(cu, rewrite);

        buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public void foo() {\n");
        buf.append("        if (i == 0) {\n");
        buf.append("            System.beep();\n");
        buf.append("        } else if (true) {\n");
        buf.append("            return;\n");
        buf.append("        }\n");
        buf.append("        if (i == 0) {\n");
        buf.append("            System.beep();\n");
        buf.append("        }\n");
        buf.append("        if (i == 0) {\n");
        buf.append("            System.beep();\n");
        buf.append("        } else if (true)\n");
        buf.append("            return;\n");
        buf.append("        if (i == 0) {\n");
        buf.append("            System.beep();\n");
        buf.append("        }\n");
        buf.append("    }\n");
        buf.append("}\n");
        assertEqualString(preview, buf.toString());

    }

    @Test
    public void testIfStatement6() throws Exception {
        StringBuffer buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public void foo() {\n");
        buf.append("        if (i == 0)\n");
        buf.append("            System.beep();\n");
        buf.append("        if (i == 0) {\n");
        buf.append("            System.beep();\n");
        buf.append("        }\n");
        buf.append("    }\n");
        buf.append("}\n");
        ICompilationUnit cu =
                new com.codenvy.ide.ext.java.jdt.compiler.batch.CompilationUnit(buf.toString().toCharArray(), "E.java", "");

        CompilationUnit astRoot = createAST(cu);
        ASTRewrite rewrite = ASTRewrite.create(astRoot.getAST());
        AST ast = astRoot.getAST();

        assertTrue("Parse errors", (astRoot.getFlags() & ASTNode.MALFORMED) == 0);
        TypeDeclaration type = findTypeDeclaration(astRoot, "E");
        MethodDeclaration methodDecl = findMethodDeclaration(type, "foo");
        Block block = methodDecl.getBody();
        List statements = block.statements();
        assertTrue("Number of statements not 2", statements.size() == 2);

        { // replace then statement by a try statement
            IfStatement ifStatement = (IfStatement)statements.get(0);

            TryStatement newTry = ast.newTryStatement();
            newTry.getBody().statements().add(ast.newReturnStatement());
            CatchClause newCatchClause = ast.newCatchClause();
            SingleVariableDeclaration varDecl = ast.newSingleVariableDeclaration();
            varDecl.setType(ast.newSimpleType(ast.newSimpleName("Exception")));
            varDecl.setName(ast.newSimpleName("e"));
            newCatchClause.setException(varDecl);
            newTry.catchClauses().add(newCatchClause);

            rewrite.replace(ifStatement.getThenStatement(), newTry, null);
        }
        { // replace then statement by a try statement
            IfStatement ifStatement = (IfStatement)statements.get(1);

            TryStatement newTry = ast.newTryStatement();
            newTry.getBody().statements().add(ast.newReturnStatement());
            CatchClause newCatchClause = ast.newCatchClause();
            SingleVariableDeclaration varDecl = ast.newSingleVariableDeclaration();
            varDecl.setType(ast.newSimpleType(ast.newSimpleName("Exception")));
            varDecl.setName(ast.newSimpleName("e"));
            newCatchClause.setException(varDecl);
            newTry.catchClauses().add(newCatchClause);

            rewrite.replace(ifStatement.getThenStatement(), newTry, null);
        }
        String preview = evaluateRewrite(cu, rewrite);

        buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public void foo() {\n");
        buf.append("        if (i == 0)\n");
        buf.append("            try {\n");
        buf.append("                return;\n");
        buf.append("            } catch (Exception e) {\n");
        buf.append("            }\n");
        buf.append("        if (i == 0)\n");
        buf.append("            try {\n");
        buf.append("                return;\n");
        buf.append("            } catch (Exception e) {\n");
        buf.append("            }\n");
        buf.append("    }\n");
        buf.append("}\n");
        assertEqualString(preview, buf.toString());

    }

    @Test
    public void testIfStatement_bug48988() throws Exception {
        StringBuffer buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    void doit() {\n");
        buf.append("        int var;\n");
        buf.append("        if (true)\n");
        buf.append("            var = 17;\n");
        buf.append("        else if (var == 18)\n");
        buf.append("            if (1 < var && var < 17)\n");
        buf.append("                var = 1;\n");
        buf.append("            else\n");
        buf.append("                var++;\n");
        buf.append("        else\n");
        buf.append("            return;\n");
        buf.append("    }\n");
        buf.append("}\n");
        ICompilationUnit cu =
                new com.codenvy.ide.ext.java.jdt.compiler.batch.CompilationUnit(buf.toString().toCharArray(), "E.java", "");

        CompilationUnit astRoot = createAST(cu);
        ASTRewrite rewrite = ASTRewrite.create(astRoot.getAST());
        AST ast = astRoot.getAST();

        TypeDeclaration typeDecl = findTypeDeclaration(astRoot, "E");
        MethodDeclaration methDecl = findMethodDeclaration(typeDecl, "doit");
        IfStatement outerIf = (IfStatement)methDecl.getBody().statements().get(1);
        ASTNode node = ((IfStatement)outerIf.getElseStatement()).getThenStatement();

        assertTrue(node instanceof IfStatement);

        { // replace else statement by a block containing the old then statement
            IfStatement ifStatement = (IfStatement)node;
            ASTNode placeholder = rewrite.createMoveTarget(ifStatement);
            Block newBlock = ast.newBlock();
            newBlock.statements().add(placeholder);
            rewrite.replace(ifStatement, newBlock, null);
        }
        String preview = evaluateRewrite(cu, rewrite);

        buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    void doit() {\n");
        buf.append("        int var;\n");
        buf.append("        if (true)\n");
        buf.append("            var = 17;\n");
        buf.append("        else if (var == 18) {\n");
        buf.append("            if (1 < var && var < 17)\n");
        buf.append("                var = 1;\n");
        buf.append("            else\n");
        buf.append("                var++;\n");
        buf.append("        } else\n");
        buf.append("            return;\n");
        buf.append("    }\n");
        buf.append("}\n");
        assertEqualString(preview, buf.toString());

    }

    @Test
    public void testIfStatementReplaceElse1() throws Exception {
        StringBuffer buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public void foo() {\n");
        buf.append("        if (i == 0)\n");
        buf.append("            System.beep();\n");
        buf.append("        else\n");
        buf.append("            hoo(11);\n");
        buf.append("        if (i == 0) {\n");
        buf.append("            System.beep();\n");
        buf.append("        } else\n");
        buf.append("            hoo(11);\n");
        buf.append("        if (i == 0)\n");
        buf.append("            System.beep();\n");
        buf.append("        else {\n");
        buf.append("            hoo(11);\n");
        buf.append("        }\n");
        buf.append("        if (i == 0) {\n");
        buf.append("            System.beep();\n");
        buf.append("        } else {\n");
        buf.append("            hoo(11);\n");
        buf.append("        }\n");
        buf.append("    }\n");
        buf.append("}\n");
        ICompilationUnit cu =
                new com.codenvy.ide.ext.java.jdt.compiler.batch.CompilationUnit(buf.toString().toCharArray(), "E.java", "");

        CompilationUnit astRoot = createAST(cu);
        ASTRewrite rewrite = ASTRewrite.create(astRoot.getAST());
        AST ast = astRoot.getAST();

        assertTrue("Parse errors", (astRoot.getFlags() & ASTNode.MALFORMED) == 0);
        TypeDeclaration type = findTypeDeclaration(astRoot, "E");
        MethodDeclaration methodDecl = findMethodDeclaration(type, "foo");
        Block block = methodDecl.getBody();
        List statements = block.statements();
        assertTrue("Number of statements not 4", statements.size() == 4);

        { // replace then statement by block , replace else statement
            IfStatement ifStatement = (IfStatement)statements.get(0);

            Block newElseBlock = ast.newBlock();
            Statement returnStatement = ast.newReturnStatement();
            newElseBlock.statements().add(returnStatement);

            rewrite.replace(ifStatement.getElseStatement(), newElseBlock, null);

        }
        { // replace then statement by block, replace else statement
            IfStatement ifStatement = (IfStatement)statements.get(1);

            Block newElseBlock = ast.newBlock();
            Statement returnStatement = ast.newReturnStatement();
            newElseBlock.statements().add(returnStatement);

            rewrite.replace(ifStatement.getElseStatement(), newElseBlock, null);
        }
        { // replace then block by statement, replace else statement
            IfStatement ifStatement = (IfStatement)statements.get(2);

            Statement returnStatement = ast.newReturnStatement();
            rewrite.replace(ifStatement.getElseStatement(), returnStatement, null);

        }
        { // replace then block by statement, replace else statement
            IfStatement ifStatement = (IfStatement)statements.get(3);

            Statement returnStatement = ast.newReturnStatement();
            rewrite.replace(ifStatement.getElseStatement(), returnStatement, null);
        }
        String preview = evaluateRewrite(cu, rewrite);

        buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public void foo() {\n");
        buf.append("        if (i == 0)\n");
        buf.append("            System.beep();\n");
        buf.append("        else {\n");
        buf.append("            return;\n");
        buf.append("        }\n");
        buf.append("        if (i == 0) {\n");
        buf.append("            System.beep();\n");
        buf.append("        } else {\n");
        buf.append("            return;\n");
        buf.append("        }\n");
        buf.append("        if (i == 0)\n");
        buf.append("            System.beep();\n");
        buf.append("        else\n");
        buf.append("            return;\n");
        buf.append("        if (i == 0) {\n");
        buf.append("            System.beep();\n");
        buf.append("        } else\n");
        buf.append("            return;\n");
        buf.append("    }\n");
        buf.append("}\n");
        assertEqualString(preview, buf.toString());

    }

    @Test
    public void testIfStatementReplaceElse2() throws Exception {
        StringBuffer buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public void foo() {\n");
        buf.append("        if (i == 0)\n");
        buf.append("            System.beep();\n");
        buf.append("        else\n");
        buf.append("            hoo(11);\n");
        buf.append("        if (i == 0) {\n");
        buf.append("            System.beep();\n");
        buf.append("        } else\n");
        buf.append("            hoo(11);\n");
        buf.append("        if (i == 0)\n");
        buf.append("            System.beep();\n");
        buf.append("        else {\n");
        buf.append("            hoo(11);\n");
        buf.append("        }\n");
        buf.append("        if (i == 0) {\n");
        buf.append("            System.beep();\n");
        buf.append("        } else {\n");
        buf.append("            hoo(11);\n");
        buf.append("        }\n");
        buf.append("    }\n");
        buf.append("}\n");
        ICompilationUnit cu =
                new com.codenvy.ide.ext.java.jdt.compiler.batch.CompilationUnit(buf.toString().toCharArray(), "E.java", "");

        CompilationUnit astRoot = createAST(cu);
        ASTRewrite rewrite = ASTRewrite.create(astRoot.getAST());
        AST ast = astRoot.getAST();

        assertTrue("Parse errors", (astRoot.getFlags() & ASTNode.MALFORMED) == 0);
        TypeDeclaration type = findTypeDeclaration(astRoot, "E");
        MethodDeclaration methodDecl = findMethodDeclaration(type, "foo");
        Block block = methodDecl.getBody();
        List statements = block.statements();
        assertTrue("Number of statements not 4", statements.size() == 4);

        { // replace then statement by block , replace else statement
            IfStatement ifStatement = (IfStatement)statements.get(0);

            ASTNode statement = ifStatement.getThenStatement();

            Block newBody = ast.newBlock();
            ASTNode newStatement = rewrite.createMoveTarget(statement);
            newBody.statements().add(newStatement);

            rewrite.replace(statement, newBody, null);

            Block newElseBlock = ast.newBlock();
            Statement returnStatement = ast.newReturnStatement();
            newElseBlock.statements().add(returnStatement);

            rewrite.replace(ifStatement.getElseStatement(), newElseBlock, null);

        }
        { // replace then statement by block, replace else statement
            IfStatement ifStatement = (IfStatement)statements.get(1);

            Block body = (Block)ifStatement.getThenStatement();
            ASTNode statement = (ASTNode)body.statements().get(0);

            ASTNode newBody = rewrite.createMoveTarget(statement);

            rewrite.replace(body, newBody, null);

            Block newElseBlock = ast.newBlock();
            Statement returnStatement = ast.newReturnStatement();
            newElseBlock.statements().add(returnStatement);

            rewrite.replace(ifStatement.getElseStatement(), newElseBlock, null);
        }
        { // replace then block by statement, replace else statement
            IfStatement ifStatement = (IfStatement)statements.get(2);

            ASTNode statement = ifStatement.getThenStatement();

            Block newBody = ast.newBlock();
            ASTNode newStatement = rewrite.createMoveTarget(statement);
            newBody.statements().add(newStatement);

            rewrite.replace(statement, newBody, null);

            Statement returnStatement = ast.newReturnStatement();
            rewrite.replace(ifStatement.getElseStatement(), returnStatement, null);

        }
        { // replace then block by statement, replace else statement
            IfStatement ifStatement = (IfStatement)statements.get(3);

            Block body = (Block)ifStatement.getThenStatement();
            ASTNode statement = (ASTNode)body.statements().get(0);

            ASTNode newBody = rewrite.createMoveTarget(statement);

            rewrite.replace(body, newBody, null);

            Statement returnStatement = ast.newReturnStatement();
            rewrite.replace(ifStatement.getElseStatement(), returnStatement, null);
        }
        String preview = evaluateRewrite(cu, rewrite);

        buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public void foo() {\n");
        buf.append("        if (i == 0) {\n");
        buf.append("            System.beep();\n");
        buf.append("        } else {\n");
        buf.append("            return;\n");
        buf.append("        }\n");
        buf.append("        if (i == 0)\n");
        buf.append("            System.beep();\n");
        buf.append("        else {\n");
        buf.append("            return;\n");
        buf.append("        }\n");
        buf.append("        if (i == 0) {\n");
        buf.append("            System.beep();\n");
        buf.append("        } else\n");
        buf.append("            return;\n");
        buf.append("        if (i == 0)\n");
        buf.append("            System.beep();\n");
        buf.append("        else\n");
        buf.append("            return;\n");
        buf.append("    }\n");
        buf.append("}\n");
        assertEqualString(preview, buf.toString());

    }

    @Test
    public void testIfStatementReplaceElse3() throws Exception {
        StringBuffer buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public void foo() {\n");
        buf.append("        if (i == 0)\n");
        buf.append("            System.beep();\n");
        buf.append("        else\n");
        buf.append("            hoo(11);\n");
        buf.append("        if (i == 0) {\n");
        buf.append("            System.beep();\n");
        buf.append("        } else\n");
        buf.append("            hoo(11);\n");
        buf.append("        if (i == 0)\n");
        buf.append("            System.beep();\n");
        buf.append("        else {\n");
        buf.append("            hoo(11);\n");
        buf.append("        }\n");
        buf.append("        if (i == 0) {\n");
        buf.append("            System.beep();\n");
        buf.append("        } else {\n");
        buf.append("            hoo(11);\n");
        buf.append("        }\n");
        buf.append("    }\n");
        buf.append("}\n");
        ICompilationUnit cu =
                new com.codenvy.ide.ext.java.jdt.compiler.batch.CompilationUnit(buf.toString().toCharArray(), "E.java", "");

        CompilationUnit astRoot = createAST(cu);
        ASTRewrite rewrite = ASTRewrite.create(astRoot.getAST());
        AST ast = astRoot.getAST();

        assertTrue("Parse errors", (astRoot.getFlags() & ASTNode.MALFORMED) == 0);
        TypeDeclaration type = findTypeDeclaration(astRoot, "E");
        MethodDeclaration methodDecl = findMethodDeclaration(type, "foo");
        Block block = methodDecl.getBody();
        List statements = block.statements();
        assertTrue("Number of statements not 4", statements.size() == 4);

        { // replace then statement by block , replace else with if statement (no block)
            IfStatement ifStatement = (IfStatement)statements.get(0);

            ASTNode statement = ifStatement.getThenStatement();

            Block newBody = ast.newBlock();
            ASTNode newStatement = rewrite.createMoveTarget(statement);
            newBody.statements().add(newStatement);

            rewrite.replace(statement, newBody, null);

            IfStatement newElseBlock = ast.newIfStatement();
            newElseBlock.setExpression(ast.newBooleanLiteral(true));

            Statement newBody2 = (Statement)rewrite.createMoveTarget(ifStatement.getElseStatement());
            newElseBlock.setThenStatement(newBody2);

            rewrite.replace(ifStatement.getElseStatement(), newElseBlock, null);
        }
        { // replace then statement by block, replace else with if statement (no block)
            IfStatement ifStatement = (IfStatement)statements.get(1);

            Block body = (Block)ifStatement.getThenStatement();
            ASTNode statement = (ASTNode)body.statements().get(0);

            ASTNode newBody = rewrite.createMoveTarget(statement);

            rewrite.replace(body, newBody, null);

            IfStatement newElseBlock = ast.newIfStatement();
            newElseBlock.setExpression(ast.newBooleanLiteral(true));

            Statement newBody2 = (Statement)rewrite.createMoveTarget(ifStatement.getElseStatement());
            newElseBlock.setThenStatement(newBody2);

            rewrite.replace(ifStatement.getElseStatement(), newElseBlock, null);
        }
        { // replace then block by statement, replace else with if statement (block)
            IfStatement ifStatement = (IfStatement)statements.get(2);

            ASTNode statement = ifStatement.getThenStatement();

            Block newBody = ast.newBlock();
            ASTNode newStatement = rewrite.createMoveTarget(statement);
            newBody.statements().add(newStatement);

            rewrite.replace(statement, newBody, null);

            IfStatement newElseBlock = ast.newIfStatement();
            newElseBlock.setExpression(ast.newBooleanLiteral(true));

            Statement newBody2 = (Statement)rewrite.createMoveTarget(ifStatement.getElseStatement());
            newElseBlock.setThenStatement(newBody2);

            rewrite.replace(ifStatement.getElseStatement(), newElseBlock, null);

        }
        { // replace then block by statement, replace else with if statement (block)
            IfStatement ifStatement = (IfStatement)statements.get(3);

            Block body = (Block)ifStatement.getThenStatement();
            ASTNode statement = (ASTNode)body.statements().get(0);

            ASTNode newBody = rewrite.createMoveTarget(statement);

            rewrite.replace(body, newBody, null);

            IfStatement newElseBlock = ast.newIfStatement();
            newElseBlock.setExpression(ast.newBooleanLiteral(true));

            Statement newBody2 = (Statement)rewrite.createMoveTarget(ifStatement.getElseStatement());
            newElseBlock.setThenStatement(newBody2);

            rewrite.replace(ifStatement.getElseStatement(), newElseBlock, null);
        }
        String preview = evaluateRewrite(cu, rewrite);

        buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public void foo() {\n");
        buf.append("        if (i == 0) {\n");
        buf.append("            System.beep();\n");
        buf.append("        } else if (true)\n");
        buf.append("            hoo(11);\n");
        buf.append("        if (i == 0)\n");
        buf.append("            System.beep();\n");
        buf.append("        else if (true)\n");
        buf.append("            hoo(11);\n");
        buf.append("        if (i == 0) {\n");
        buf.append("            System.beep();\n");
        buf.append("        } else if (true) {\n");
        buf.append("            hoo(11);\n");
        buf.append("        }\n");
        buf.append("        if (i == 0)\n");
        buf.append("            System.beep();\n");
        buf.append("        else if (true) {\n");
        buf.append("            hoo(11);\n");
        buf.append("        }\n");
        buf.append("    }\n");
        buf.append("}\n");
        assertEqualString(preview, buf.toString());

    }

    @Test
    public void testIfStatementReplaceElse4() throws Exception {
        StringBuffer buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public void foo() {\n");
        buf.append("        if (i == 0)\n");
        buf.append("            System.beep();\n");
        buf.append("        else\n");
        buf.append("            hoo(11);\n");
        buf.append("        if (i == 0) {\n");
        buf.append("            System.beep();\n");
        buf.append("        } else\n");
        buf.append("            hoo(11);\n");
        buf.append("        if (i == 0)\n");
        buf.append("            System.beep();\n");
        buf.append("        else {\n");
        buf.append("            hoo(11);\n");
        buf.append("        }\n");
        buf.append("        if (i == 0) {\n");
        buf.append("            System.beep();\n");
        buf.append("        } else {\n");
        buf.append("            hoo(11);\n");
        buf.append("        }\n");
        buf.append("    }\n");
        buf.append("}\n");
        ICompilationUnit cu =
                new com.codenvy.ide.ext.java.jdt.compiler.batch.CompilationUnit(buf.toString().toCharArray(), "E.java", "");

        CompilationUnit astRoot = createAST(cu);
        ASTRewrite rewrite = ASTRewrite.create(astRoot.getAST());
        AST ast = astRoot.getAST();

        assertTrue("Parse errors", (astRoot.getFlags() & ASTNode.MALFORMED) == 0);
        TypeDeclaration type = findTypeDeclaration(astRoot, "E");
        MethodDeclaration methodDecl = findMethodDeclaration(type, "foo");
        Block block = methodDecl.getBody();
        List statements = block.statements();
        assertTrue("Number of statements not 4", statements.size() == 4);

        { // replace then statement by block , replace else with if statement (block)
            IfStatement ifStatement = (IfStatement)statements.get(0);

            ASTNode statement = ifStatement.getThenStatement();

            Block newBody = ast.newBlock();
            ASTNode newStatement = rewrite.createMoveTarget(statement);
            newBody.statements().add(newStatement);

            rewrite.replace(statement, newBody, null);

            IfStatement newElseBlock = ast.newIfStatement();
            newElseBlock.setExpression(ast.newBooleanLiteral(true));

            Block newBody2 = ast.newBlock();
            Statement returnStatement = ast.newReturnStatement();
            newBody2.statements().add(returnStatement);

            newElseBlock.setThenStatement(newBody2);

            rewrite.replace(ifStatement.getElseStatement(), newElseBlock, null);
        }
        { // replace then statement by block, replace else with if statement (block)
            IfStatement ifStatement = (IfStatement)statements.get(1);

            Block body = (Block)ifStatement.getThenStatement();
            ASTNode statement = (ASTNode)body.statements().get(0);

            ASTNode newBody = rewrite.createMoveTarget(statement);

            rewrite.replace(body, newBody, null);

            IfStatement newElseBlock = ast.newIfStatement();
            newElseBlock.setExpression(ast.newBooleanLiteral(true));

            Block newBody2 = ast.newBlock();
            Statement returnStatement = ast.newReturnStatement();
            newBody2.statements().add(returnStatement);

            newElseBlock.setThenStatement(newBody2);

            rewrite.replace(ifStatement.getElseStatement(), newElseBlock, null);
        }
        { // replace then block by statement, replace else with if statement (no block)
            IfStatement ifStatement = (IfStatement)statements.get(2);

            ASTNode statement = ifStatement.getThenStatement();

            Block newBody = ast.newBlock();
            ASTNode newStatement = rewrite.createMoveTarget(statement);
            newBody.statements().add(newStatement);

            rewrite.replace(statement, newBody, null);

            IfStatement newElseBlock = ast.newIfStatement();
            newElseBlock.setExpression(ast.newBooleanLiteral(true));

            Statement newBody2 = ast.newReturnStatement();
            newElseBlock.setThenStatement(newBody2);

            rewrite.replace(ifStatement.getElseStatement(), newElseBlock, null);

        }
        { // replace then block by statement, replace else with if statement (no block)
            IfStatement ifStatement = (IfStatement)statements.get(3);

            Block body = (Block)ifStatement.getThenStatement();
            ASTNode statement = (ASTNode)body.statements().get(0);

            ASTNode newBody = rewrite.createMoveTarget(statement);

            rewrite.replace(body, newBody, null);

            IfStatement newElseBlock = ast.newIfStatement();
            newElseBlock.setExpression(ast.newBooleanLiteral(true));

            Statement newBody2 = ast.newReturnStatement();
            newElseBlock.setThenStatement(newBody2);

            rewrite.replace(ifStatement.getElseStatement(), newElseBlock, null);
        }
        String preview = evaluateRewrite(cu, rewrite);

        buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public void foo() {\n");
        buf.append("        if (i == 0) {\n");
        buf.append("            System.beep();\n");
        buf.append("        } else if (true) {\n");
        buf.append("            return;\n");
        buf.append("        }\n");
        buf.append("        if (i == 0)\n");
        buf.append("            System.beep();\n");
        buf.append("        else if (true) {\n");
        buf.append("            return;\n");
        buf.append("        }\n");
        buf.append("        if (i == 0) {\n");
        buf.append("            System.beep();\n");
        buf.append("        } else if (true)\n");
        buf.append("            return;\n");
        buf.append("        if (i == 0)\n");
        buf.append("            System.beep();\n");
        buf.append("        else if (true)\n");
        buf.append("            return;\n");
        buf.append("    }\n");
        buf.append("}\n");
        assertEqualString(preview, buf.toString());

    }

    @Test
    public void testIfStatementReplaceElse5() throws Exception {
        StringBuffer buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public void foo(int i) {\n");
        buf.append("        if (i == 0) {\n");
        buf.append("            System.beep();\n");
        buf.append("        } else {\n");
        buf.append("            System.beep();\n");
        buf.append("            System.beep();\n");
        buf.append("        }\n");
        buf.append("        // comment\n");
        buf.append("    }\n");
        buf.append("}\n");
        ICompilationUnit cu =
                new com.codenvy.ide.ext.java.jdt.compiler.batch.CompilationUnit(buf.toString().toCharArray(), "E.java", "");

        CompilationUnit astRoot = createAST(cu);
        AST ast = astRoot.getAST();
        ASTRewrite rewrite = ASTRewrite.create(ast);

        assertTrue("Parse errors", (astRoot.getFlags() & ASTNode.MALFORMED) == 0);
        TypeDeclaration type = findTypeDeclaration(astRoot, "E");
        MethodDeclaration methodDecl = findMethodDeclaration(type, "foo");
        Block block = methodDecl.getBody();
        List statements = block.statements();
        assertTrue("Number of statements not 1", statements.size() == 1);

        {
            // replace then statement by block , replace else with if statement (block)
            IfStatement ifStatement = (IfStatement)statements.get(0);

            Statement thenStatement = ifStatement.getThenStatement();
            Statement elseStatement = ifStatement.getElseStatement();

            Statement newElseStatement = (Statement)rewrite.createMoveTarget(thenStatement);
            Statement newThenStatement = (Statement)rewrite.createMoveTarget(elseStatement);

            rewrite.set(ifStatement, IfStatement.THEN_STATEMENT_PROPERTY, newThenStatement, null);
            rewrite.set(ifStatement, IfStatement.ELSE_STATEMENT_PROPERTY, newElseStatement, null);
        }
        String preview = evaluateRewrite(cu, rewrite);

        buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public void foo(int i) {\n");
        buf.append("        if (i == 0) {\n");
        buf.append("            System.beep();\n");
        buf.append("            System.beep();\n");
        buf.append("        }\n");
        buf.append("        // comment\n");
        buf.append("        else {\n");
        buf.append("            System.beep();\n");
        buf.append("        }\n");
        buf.append("    }\n");
        buf.append("}\n");
        assertEqualString(preview, buf.toString());

    }

    @Test
    public void testLabeledStatement() throws Exception {
        StringBuffer buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public void foo() {\n");
        buf.append("        label: if (i == 0) {\n");
        buf.append("            System.beep();\n");
        buf.append("        }\n");
        buf.append("    }\n");
        buf.append("}\n");
        ICompilationUnit cu =
                new com.codenvy.ide.ext.java.jdt.compiler.batch.CompilationUnit(buf.toString().toCharArray(), "E.java", "");

        CompilationUnit astRoot = createAST(cu);
        ASTRewrite rewrite = ASTRewrite.create(astRoot.getAST());

        AST ast = astRoot.getAST();

        assertTrue("Parse errors", (astRoot.getFlags() & ASTNode.MALFORMED) == 0);
        TypeDeclaration type = findTypeDeclaration(astRoot, "E");
        MethodDeclaration methodDecl = findMethodDeclaration(type, "foo");
        Block block = methodDecl.getBody();
        List statements = block.statements();
        assertTrue("Number of statements not 1", statements.size() == 1);

        { // replace label and statement
            LabeledStatement labeledStatement = (LabeledStatement)statements.get(0);

            Name newLabel = ast.newSimpleName("newLabel");

            rewrite.replace(labeledStatement.getLabel(), newLabel, null);

            Assignment newExpression = ast.newAssignment();
            newExpression.setLeftHandSide(ast.newSimpleName("x"));
            newExpression.setRightHandSide(ast.newNumberLiteral("1"));
            newExpression.setOperator(Assignment.Operator.ASSIGN);

            Statement newStatement = ast.newExpressionStatement(newExpression);

            rewrite.replace(labeledStatement.getBody(), newStatement, null);
        }

        String preview = evaluateRewrite(cu, rewrite);

        buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public void foo() {\n");
        buf.append("        newLabel: x = 1;\n");
        buf.append("    }\n");
        buf.append("}\n");
        assertEqualString(preview, buf.toString());

    }

    @Test
    public void testReturnStatement() throws Exception {
        StringBuffer buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public void foo() {\n");
        buf.append("        return;\n");
        buf.append("        return 1;\n");
        buf.append("        return 1;\n");
        buf.append("        return 1 + 2;\n");
        buf.append("        return(1 + 2);\n");
        buf.append("        return/*com*/ 1;\n");
        buf.append("    }\n");
        buf.append("}\n");
        ICompilationUnit cu =
                new com.codenvy.ide.ext.java.jdt.compiler.batch.CompilationUnit(buf.toString().toCharArray(), "E.java", "");

        CompilationUnit astRoot = createAST(cu);
        ASTRewrite rewrite = ASTRewrite.create(astRoot.getAST());

        AST ast = astRoot.getAST();

        assertTrue("Parse errors", (astRoot.getFlags() & ASTNode.MALFORMED) == 0);
        TypeDeclaration type = findTypeDeclaration(astRoot, "E");
        MethodDeclaration methodDecl = findMethodDeclaration(type, "foo");
        Block block = methodDecl.getBody();
        List statements = block.statements();
        assertTrue("Number of statements not 6", statements.size() == 6);
        { // insert expression
            ReturnStatement statement = (ReturnStatement)statements.get(0);
            assertTrue("Has expression", statement.getExpression() == null);

            SimpleName newExpression = ast.newSimpleName("x");
            rewrite.set(statement, ReturnStatement.EXPRESSION_PROPERTY, newExpression, null);
        }
        { // replace expression
            ReturnStatement statement = (ReturnStatement)statements.get(1);

            Expression expression = statement.getExpression();
            assertTrue("Has no label", expression != null);

            SimpleName newExpression = ast.newSimpleName("x");

            rewrite.replace(expression, newExpression, null);
        }
        { // remove expression
            ReturnStatement statement = (ReturnStatement)statements.get(2);

            Expression expression = statement.getExpression();
            assertTrue("Has no label", expression != null);

            rewrite.remove(expression, null);
        }
        { // modify in expression (no change)
            ReturnStatement statement = (ReturnStatement)statements.get(3);

            InfixExpression expression = (InfixExpression)statement.getExpression();
            rewrite.replace(expression.getLeftOperand(), ast.newNumberLiteral("9"), null);
        }
        { // replace parentized expression (additional space needed)
            ReturnStatement statement = (ReturnStatement)statements.get(4);

            Expression expression = statement.getExpression();
            rewrite.replace(expression, ast.newNumberLiteral("9"), null);
        }
        { // replace expression with comment (additional space needed)
            ReturnStatement statement = (ReturnStatement)statements.get(5);

            Expression expression = statement.getExpression();
            rewrite.replace(expression, ast.newNumberLiteral("9"), null);
        }

        String preview = evaluateRewrite(cu, rewrite);

        buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public void foo() {\n");
        buf.append("        return x;\n");
        buf.append("        return x;\n");
        buf.append("        return;\n");
        buf.append("        return 9 + 2;\n");
        buf.append("        return 9;\n");
        buf.append("        return 9;\n");
        buf.append("    }\n");
        buf.append("}\n");
        assertEqualString(preview, buf.toString());

    }

    @Test
    public void testReturnStatement2() throws Exception {
        StringBuffer buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public void foo() {\n");
        buf.append("        return\"A\";\n");
        buf.append("        return\"A\"+\"B\";\n");
        buf.append("        return(1) * 2 + 3;\n");
        buf.append("    }\n");
        buf.append("}\n");
        ICompilationUnit cu =
                new com.codenvy.ide.ext.java.jdt.compiler.batch.CompilationUnit(buf.toString().toCharArray(), "E.java", "");

        CompilationUnit astRoot = createAST(cu);
        ASTRewrite rewrite = ASTRewrite.create(astRoot.getAST());

        AST ast = astRoot.getAST();

        assertTrue("Parse errors", (astRoot.getFlags() & ASTNode.MALFORMED) == 0);
        TypeDeclaration type = findTypeDeclaration(astRoot, "E");
        MethodDeclaration methodDecl = findMethodDeclaration(type, "foo");
        Block block = methodDecl.getBody();
        List statements = block.statements();
        assertTrue("Number of statements not 3", statements.size() == 3);
        { // replace expression "A" in return"A"
            ReturnStatement statement = (ReturnStatement)statements.get(0);

            Expression expression = statement.getExpression();
            assertTrue("Has no label", expression != null);

            SimpleName newExpression = ast.newSimpleName("x");

            rewrite.replace(expression, newExpression, null);
        }

        { // replace expression "A" in return"A"+"B"
            ReturnStatement statement = (ReturnStatement)statements.get(1);

            Expression expression = statement.getExpression();
            assertTrue("Has no expression", expression != null);
            assertTrue("Is not an InfixExpression", expression instanceof InfixExpression);
            Expression leftOperand = ((InfixExpression)expression).getLeftOperand();
            assertTrue("Has no leftOperand", leftOperand != null);

            SimpleName newExpression = ast.newSimpleName("x");

            rewrite.replace(leftOperand, newExpression, null);
        }

        { // replace expression (1) in return(1) * 2 + 3
            ReturnStatement statement = (ReturnStatement)statements.get(2);

            Expression expression = statement.getExpression();
            assertTrue("Has no expression", expression != null);
            assertTrue("Is not an InfixExpression", expression instanceof InfixExpression);
            Expression leftOperand = ((InfixExpression)expression).getLeftOperand();
            assertTrue("Has no leftOperand", leftOperand != null);
            assertTrue("Is not an InfixExpression", leftOperand instanceof InfixExpression);
            Expression leftOperand2 = ((InfixExpression)leftOperand).getLeftOperand();
            assertTrue("Has no leftOperand2", leftOperand2 != null);

            SimpleName newExpression = ast.newSimpleName("x");

            rewrite.replace(leftOperand2, newExpression, null);
        }

        String preview = evaluateRewrite(cu, rewrite);

        buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public void foo() {\n");
        buf.append("        return x;\n");
        buf.append("        return x+\"B\";\n");
        buf.append("        return x * 2 + 3;\n");
        buf.append("    }\n");
        buf.append("}\n");
        assertEqualString(preview, buf.toString());

    }

    @Test
    public void testAssertStatement() throws Exception {
        StringBuffer buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public void foo() {\n");
        buf.append("        assert(true);\n");
        buf.append("        assert/* comment*/true;\n");
        buf.append("        assert(true);\n");
        buf.append("        assert(true) : \"Hello\";\n");
        buf.append("        assert(true) : \"Hello\";\n");
        buf.append("    }\n");
        buf.append("}\n");
        ICompilationUnit cu =
                new com.codenvy.ide.ext.java.jdt.compiler.batch.CompilationUnit(buf.toString().toCharArray(), "E.java", "");

        CompilationUnit astRoot = createAST(cu);
        ASTRewrite rewrite = ASTRewrite.create(astRoot.getAST());

        AST ast = astRoot.getAST();

        assertTrue("Parse errors", (astRoot.getFlags() & ASTNode.MALFORMED) == 0);
        TypeDeclaration type = findTypeDeclaration(astRoot, "E");
        MethodDeclaration methodDecl = findMethodDeclaration(type, "foo");
        Block block = methodDecl.getBody();
        List statements = block.statements();
        assertTrue("Number of statements not 5", statements.size() == 5);
        { // replace expression
            AssertStatement statement = (AssertStatement)statements.get(0);

            SimpleName newExpression = ast.newSimpleName("x");
            rewrite.set(statement, AssertStatement.EXPRESSION_PROPERTY, newExpression, null);
        }
        { // replace expression
            AssertStatement statement = (AssertStatement)statements.get(1);

            SimpleName newExpression = ast.newSimpleName("x");
            rewrite.set(statement, AssertStatement.EXPRESSION_PROPERTY, newExpression, null);
        }
        { // insert message
            AssertStatement statement = (AssertStatement)statements.get(2);

            SimpleName newExpression = ast.newSimpleName("x");
            rewrite.set(statement, AssertStatement.MESSAGE_PROPERTY, newExpression, null);
        }
        { // replace message
            AssertStatement statement = (AssertStatement)statements.get(3);

            SimpleName newExpression = ast.newSimpleName("x");
            rewrite.set(statement, AssertStatement.MESSAGE_PROPERTY, newExpression, null);
        }
        { // remove message
            AssertStatement statement = (AssertStatement)statements.get(4);

            rewrite.set(statement, AssertStatement.MESSAGE_PROPERTY, null, null);
        }

        String preview = evaluateRewrite(cu, rewrite);

        buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public void foo() {\n");
        buf.append("        assert x;\n");
        buf.append("        assert x;\n");
        buf.append("        assert(true) : x;\n");
        buf.append("        assert(true) : x;\n");
        buf.append("        assert(true);\n");
        buf.append("    }\n");
        buf.append("}\n");
        assertEqualString(preview, buf.toString());

    }

    @Test
    @Ignore
    public void testSwitchStatement() throws Exception {
        StringBuffer buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public void foo(int i) {\n");
        buf.append("        switch (i) {\n");
        buf.append("        }\n");
        buf.append("        switch (i) {\n");
        buf.append("            case 1:\n");
        buf.append("                i= 1;\n");
        buf.append("                break;\n");
        buf.append("            case 2:\n");
        buf.append("                i= 2;\n");
        buf.append("                break;\n");
        buf.append("            default:\n");
        buf.append("                i= 3;\n");
        buf.append("        }\n");
        buf.append("    }\n");
        buf.append("}\n");
        ICompilationUnit cu =
                new com.codenvy.ide.ext.java.jdt.compiler.batch.CompilationUnit(buf.toString().toCharArray(), "E.java", "");

        CompilationUnit astRoot = createAST(cu);
        ASTRewrite rewrite = ASTRewrite.create(astRoot.getAST());

        AST ast = astRoot.getAST();

        assertTrue("Parse errors", (astRoot.getFlags() & ASTNode.MALFORMED) == 0);
        TypeDeclaration type = findTypeDeclaration(astRoot, "E");
        MethodDeclaration methodDecl = findMethodDeclaration(type, "foo");
        Block block = methodDecl.getBody();
        List blockStatements = block.statements();
        assertTrue("Number of statements not 2", blockStatements.size() == 2);
        { // insert statements, replace expression
            SwitchStatement switchStatement = (SwitchStatement)blockStatements.get(0);

            ASTNode expression = switchStatement.getExpression();
            SimpleName newExpression = ast.newSimpleName("x");
            rewrite.replace(expression, newExpression, null);

            List statements = switchStatement.statements();
            assertTrue("Number of statements not 0", statements.size() == 0);

            SwitchCase caseStatement1 = ast.newSwitchCase();
            caseStatement1.setExpression(ast.newNumberLiteral("1"));

            Statement statement1 = ast.newReturnStatement();

            SwitchCase caseStatement2 = ast.newSwitchCase(); // default
            caseStatement2.setExpression(null);

            ListRewrite listRewrite = rewrite.getListRewrite(switchStatement, SwitchStatement.STATEMENTS_PROPERTY);
            listRewrite.insertLast(caseStatement1, null);
            listRewrite.insertLast(statement1, null);
            listRewrite.insertLast(caseStatement2, null);
        }

        { // insert, remove, replace statements, change case statements
            SwitchStatement switchStatement = (SwitchStatement)blockStatements.get(1);

            List statements = switchStatement.statements();
            assertTrue("Number of statements not 8", statements.size() == 8);

            // remove statements

            rewrite.remove((ASTNode)statements.get(0), null);
            rewrite.remove((ASTNode)statements.get(1), null);
            rewrite.remove((ASTNode)statements.get(2), null);

            // change case statement
            SwitchCase caseStatement = (SwitchCase)statements.get(3);
            Expression newCaseExpression = ast.newNumberLiteral("10");
            rewrite.replace(caseStatement.getExpression(), newCaseExpression, null);

            ListRewrite listRewrite = rewrite.getListRewrite(switchStatement, SwitchStatement.STATEMENTS_PROPERTY);

            {
                // insert case statement
                SwitchCase caseStatement2 = ast.newSwitchCase();
                caseStatement2.setExpression(ast.newNumberLiteral("11"));
                listRewrite.insertFirst(caseStatement2, null);

                // insert statement
                Statement statement1 = ast.newReturnStatement();
                listRewrite.insertAfter(statement1, caseStatement2, null);
            }

            {
                // insert case statement
                SwitchCase caseStatement2 = ast.newSwitchCase();
                caseStatement2.setExpression(ast.newNumberLiteral("12"));
                listRewrite.insertLast(caseStatement2, null);

                // insert statement
                Statement statement1 = ast.newReturnStatement();
                listRewrite.insertAfter(statement1, caseStatement2, null);
            }

        }

        String preview = evaluateRewrite(cu, rewrite);

        buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public void foo(int i) {\n");
        buf.append("        switch (x) {\n");
        buf.append("            case 1:\n");
        buf.append("                return;\n");
        buf.append("            default:\n");
        buf.append("        }\n");
        buf.append("        switch (i) {\n");
        buf.append("            case 11:\n");
        buf.append("                return;\n");
        buf.append("            case 10:\n");
        buf.append("                i= 2;\n");
        buf.append("                break;\n");
        buf.append("            default:\n");
        buf.append("                i= 3;\n");
        buf.append("            case 12:\n");
        buf.append("                return;\n");
        buf.append("        }\n");
        buf.append("    }\n");
        buf.append("}\n");
        assertEqualString(preview, buf.toString());

    }

    @Test
    @Ignore
    public void testSwitchStatement2() throws Exception {
        StringBuffer buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public void foo(int i) {\n");
        buf.append("        switch (i) {\n");
        buf.append("        }\n");
        buf.append("        switch (i) {\n");
        buf.append("        case 1:\n");
        buf.append("            i= 1;\n");
        buf.append("            break;\n");
        buf.append("        case 2:\n");
        buf.append("            i= 2;\n");
        buf.append("            break;\n");
        buf.append("        default:\n");
        buf.append("            i= 3;\n");
        buf.append("        }\n");
        buf.append("    }\n");
        buf.append("}\n");
        ICompilationUnit cu =
                new com.codenvy.ide.ext.java.jdt.compiler.batch.CompilationUnit(buf.toString().toCharArray(), "E.java", "");

        CompilationUnit astRoot = createAST(cu);
        ASTRewrite rewrite = ASTRewrite.create(astRoot.getAST());

        AST ast = astRoot.getAST();

        assertTrue("Parse errors", (astRoot.getFlags() & ASTNode.MALFORMED) == 0);
        TypeDeclaration type = findTypeDeclaration(astRoot, "E");
        MethodDeclaration methodDecl = findMethodDeclaration(type, "foo");
        Block block = methodDecl.getBody();
        List blockStatements = block.statements();
        assertTrue("Number of statements not 2", blockStatements.size() == 2);
        { // insert statements, replace expression
            SwitchStatement switchStatement = (SwitchStatement)blockStatements.get(0);

            ASTNode expression = switchStatement.getExpression();
            SimpleName newExpression = ast.newSimpleName("x");
            rewrite.replace(expression, newExpression, null);

            List statements = switchStatement.statements();
            assertTrue("Number of statements not 0", statements.size() == 0);

            SwitchCase caseStatement1 = ast.newSwitchCase();
            caseStatement1.setExpression(ast.newNumberLiteral("1"));

            Statement statement1 = ast.newReturnStatement();

            SwitchCase caseStatement2 = ast.newSwitchCase(); // default
            caseStatement2.setExpression(null);

            ListRewrite listRewrite = rewrite.getListRewrite(switchStatement, SwitchStatement.STATEMENTS_PROPERTY);
            listRewrite.insertLast(caseStatement1, null);
            listRewrite.insertLast(statement1, null);
            listRewrite.insertLast(caseStatement2, null);
        }

        { // insert, remove, replace statements, change case statements
            SwitchStatement switchStatement = (SwitchStatement)blockStatements.get(1);

            List statements = switchStatement.statements();
            assertTrue("Number of statements not 8", statements.size() == 8);

            // remove statements

            rewrite.remove((ASTNode)statements.get(0), null);
            rewrite.remove((ASTNode)statements.get(1), null);
            rewrite.remove((ASTNode)statements.get(2), null);

            // change case statement
            SwitchCase caseStatement = (SwitchCase)statements.get(3);
            Expression newCaseExpression = ast.newNumberLiteral("10");
            rewrite.replace(caseStatement.getExpression(), newCaseExpression, null);

            ListRewrite listRewrite = rewrite.getListRewrite(switchStatement, SwitchStatement.STATEMENTS_PROPERTY);

            {
                // insert case statement
                SwitchCase caseStatement2 = ast.newSwitchCase();
                caseStatement2.setExpression(ast.newNumberLiteral("11"));
                listRewrite.insertFirst(caseStatement2, null);

                // insert statement
                Statement statement1 = ast.newReturnStatement();
                listRewrite.insertAfter(statement1, caseStatement2, null);
            }

            {
                // insert case statement
                SwitchCase caseStatement2 = ast.newSwitchCase();
                caseStatement2.setExpression(ast.newNumberLiteral("12"));
                listRewrite.insertLast(caseStatement2, null);

                // insert statement
                Statement statement1 = ast.newReturnStatement();
                listRewrite.insertAfter(statement1, caseStatement2, null);
            }

        }

        String preview = evaluateRewrite(cu, rewrite);

        buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public void foo(int i) {\n");
        buf.append("        switch (x) {\n");
        buf.append("        case 1:\n");
        buf.append("            return;\n");
        buf.append("        default:\n");
        buf.append("        }\n");
        buf.append("        switch (i) {\n");
        buf.append("        case 11:\n");
        buf.append("            return;\n");
        buf.append("        case 10:\n");
        buf.append("            i= 2;\n");
        buf.append("            break;\n");
        buf.append("        default:\n");
        buf.append("            i= 3;\n");
        buf.append("        case 12:\n");
        buf.append("            return;\n");
        buf.append("        }\n");
        buf.append("    }\n");
        buf.append("}\n");
        assertEqualString(preview, buf.toString());
    }

    @Test
    @Ignore
    public void testSwitchStatement3() throws Exception {
        StringBuffer buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public void foo(int i) {\n");
        buf.append("        switch (i) {\n");
        buf.append("        case 1:\n");
        buf.append("            i= 1;\n");
        buf.append("        case 2:\n");
        buf.append("            i= 2;\n");
        buf.append("            break;\n");
        buf.append("        }\n");
        buf.append("    }\n");
        buf.append("}\n");
        ICompilationUnit cu =
                new com.codenvy.ide.ext.java.jdt.compiler.batch.CompilationUnit(buf.toString().toCharArray(), "E.java", "");

        CompilationUnit astRoot = createAST(cu);
        ASTRewrite rewrite = ASTRewrite.create(astRoot.getAST());

        AST ast = astRoot.getAST();

        assertTrue("Parse errors", (astRoot.getFlags() & ASTNode.MALFORMED) == 0);
        TypeDeclaration type = findTypeDeclaration(astRoot, "E");
        MethodDeclaration methodDecl = findMethodDeclaration(type, "foo");
        Block block = methodDecl.getBody();
        List blockStatements = block.statements();
        assertTrue("Number of statements not 2", blockStatements.size() == 1);
        { // insert statements, replace expression
            SwitchStatement switchStatement = (SwitchStatement)blockStatements.get(0);

            List statements = switchStatement.statements();
            assertTrue("Number of statements not 0", statements.size() == 5);

            SwitchCase caseStatement = (SwitchCase)statements.get(2);

            BreakStatement breakStatement = ast.newBreakStatement();

            ListRewrite listRewrite = rewrite.getListRewrite(switchStatement, SwitchStatement.STATEMENTS_PROPERTY);
            listRewrite.insertBefore(breakStatement, caseStatement, null);
        }

        String preview = evaluateRewrite(cu, rewrite);

        buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public void foo(int i) {\n");
        buf.append("        switch (i) {\n");
        buf.append("        case 1:\n");
        buf.append("            i= 1;\n");
        buf.append("            break;\n");
        buf.append("        case 2:\n");
        buf.append("            i= 2;\n");
        buf.append("            break;\n");
        buf.append("        }\n");
        buf.append("    }\n");
        buf.append("}\n");
        assertEqualString(preview, buf.toString());
    }

    /*
    * https://bugs.eclipse.org/bugs/show_bug.cgi?id=246627
    * Insert a statement before an unchanged statement (and preceded by an unchanged statement)
    */
    @Test
    @Ignore
    public void testSwitchStatement5() throws Exception {
        StringBuffer buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public void foo(int i) {\n");
        buf.append("        switch (i) {\n");
        buf.append("        case 1:\n");
        buf.append("            i= 1;\n");
        buf.append("        case 2:\n");
        buf.append("            break;\n");
        buf.append("        }\n");
        buf.append("    }\n");
        buf.append("}\n");
        ICompilationUnit cu =
                new com.codenvy.ide.ext.java.jdt.compiler.batch.CompilationUnit(buf.toString().toCharArray(), "E.java", "");

        CompilationUnit astRoot = createAST(cu);
        ASTRewrite rewrite = ASTRewrite.create(astRoot.getAST());

        AST ast = astRoot.getAST();

        assertTrue("Parse errors", (astRoot.getFlags() & ASTNode.MALFORMED) == 0);
        TypeDeclaration type = findTypeDeclaration(astRoot, "E");
        MethodDeclaration methodDecl = findMethodDeclaration(type, "foo");
        Block block = methodDecl.getBody();
        List blockStatements = block.statements();
        assertTrue("Number of statements not 1", blockStatements.size() == 1);
        { // insert statements, replace expression
            SwitchStatement switchStatement = (SwitchStatement)blockStatements.get(0);

            List statements = switchStatement.statements();
            assertTrue("Number of statements not 4", statements.size() == 4);

            SwitchCase caseStatement = (SwitchCase)statements.get(2); // case 2:

            ListRewrite listRewrite = rewrite.getListRewrite(switchStatement, SwitchStatement.STATEMENTS_PROPERTY);
            listRewrite.insertBefore(ast.newBreakStatement(), caseStatement, null);
        }

        String preview = evaluateRewrite(cu, rewrite);

        buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public void foo(int i) {\n");
        buf.append("        switch (i) {\n");
        buf.append("        case 1:\n");
        buf.append("            i= 1;\n");
        buf.append("            break;\n");
        buf.append("        case 2:\n");
        buf.append("            break;\n");
        buf.append("        }\n");
        buf.append("    }\n");
        buf.append("}\n");
        assertEqualString(preview, buf.toString());
    }

    /*
    * https://bugs.eclipse.org/bugs/show_bug.cgi?id=246627
    * Insert a statement after an unchanged statement (and preceded by an unchanged statement)
    */
    @Test
    @Ignore
    public void testSwitchStatement6() throws Exception {
        StringBuffer buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public void foo(int i) {\n");
        buf.append("        switch (i) {\n");
        buf.append("        case 1:\n");
        buf.append("            i= 1;\n");
        buf.append("        case 2:\n");
        buf.append("            break;\n");
        buf.append("        }\n");
        buf.append("    }\n");
        buf.append("}\n");
        ICompilationUnit cu =
                new com.codenvy.ide.ext.java.jdt.compiler.batch.CompilationUnit(buf.toString().toCharArray(), "E.java", "");

        CompilationUnit astRoot = createAST(cu);
        ASTRewrite rewrite = ASTRewrite.create(astRoot.getAST());

        AST ast = astRoot.getAST();

        assertTrue("Parse errors", (astRoot.getFlags() & ASTNode.MALFORMED) == 0);
        TypeDeclaration type = findTypeDeclaration(astRoot, "E");
        MethodDeclaration methodDecl = findMethodDeclaration(type, "foo");
        Block block = methodDecl.getBody();
        List blockStatements = block.statements();
        assertTrue("Number of statements not 1", blockStatements.size() == 1);
        { // insert statements, replace expression
            SwitchStatement switchStatement = (SwitchStatement)blockStatements.get(0);

            List statements = switchStatement.statements();
            assertTrue("Number of statements not 4", statements.size() == 4);

            ExpressionStatement assignment = (ExpressionStatement)statements.get(1); // i= 1;

            ListRewrite listRewrite = rewrite.getListRewrite(switchStatement, SwitchStatement.STATEMENTS_PROPERTY);
            listRewrite.insertAfter(ast.newBreakStatement(), assignment, null);
        }

        String preview = evaluateRewrite(cu, rewrite);

        buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public void foo(int i) {\n");
        buf.append("        switch (i) {\n");
        buf.append("        case 1:\n");
        buf.append("            i= 1;\n");
        buf.append("            break;\n");
        buf.append("        case 2:\n");
        buf.append("            break;\n");
        buf.append("        }\n");
        buf.append("    }\n");
        buf.append("}\n");
        assertEqualString(preview, buf.toString());
    }

    /*
    * https://bugs.eclipse.org/bugs/show_bug.cgi?id=246627
    * Replace a statement preceded by an unchanged statement)
    */
    @Test
    public void testSwitchStatement7() throws Exception {
        StringBuffer buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public void foo(int i) {\n");
        buf.append("        switch (i) {\n");
        buf.append("        case 1:\n");
        buf.append("            i= 1;\n");
        buf.append("        case 3:\n");
        buf.append("            break;\n");
        buf.append("        }\n");
        buf.append("    }\n");
        buf.append("}\n");
        ICompilationUnit cu =
                new com.codenvy.ide.ext.java.jdt.compiler.batch.CompilationUnit(buf.toString().toCharArray(), "E.java", "");

        CompilationUnit astRoot = createAST(cu);
        ASTRewrite rewrite = ASTRewrite.create(astRoot.getAST());

        AST ast = astRoot.getAST();

        assertTrue("Parse errors", (astRoot.getFlags() & ASTNode.MALFORMED) == 0);
        TypeDeclaration type = findTypeDeclaration(astRoot, "E");
        MethodDeclaration methodDecl = findMethodDeclaration(type, "foo");
        Block block = methodDecl.getBody();
        List blockStatements = block.statements();
        assertTrue("Number of statements not 1", blockStatements.size() == 1);
        { // insert statements, replace expression
            SwitchStatement switchStatement = (SwitchStatement)blockStatements.get(0);

            List statements = switchStatement.statements();
            assertTrue("Number of statements not 4", statements.size() == 4);

            ExpressionStatement assignment = (ExpressionStatement)statements.get(1); // i= 1;:

            SwitchCase switchCase = ast.newSwitchCase();
            switchCase.setExpression(ast.newNumberLiteral("2"));

            ListRewrite listRewrite = rewrite.getListRewrite(switchStatement, SwitchStatement.STATEMENTS_PROPERTY);
            listRewrite.replace(assignment, switchCase, null);
        }

        String preview = evaluateRewrite(cu, rewrite);

        buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public void foo(int i) {\n");
        buf.append("        switch (i) {\n");
        buf.append("        case 1:\n");
        buf.append("        case 2:\n");
        buf.append("        case 3:\n");
        buf.append("            break;\n");
        buf.append("        }\n");
        buf.append("    }\n");
        buf.append("}\n");
        assertEqualString(preview, buf.toString());
    }

    /*
    * https://bugs.eclipse.org/bugs/show_bug.cgi?id=246627
    * Remove a statement preceded by an unchanged statement)
    */
    @Test
    public void testSwitchStatement8() throws Exception {
        StringBuffer buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public void foo(int i) {\n");
        buf.append("        switch (i) {\n");
        buf.append("        case 1:\n");
        buf.append("            i= 1;\n");
        buf.append("        case 2:\n");
        buf.append("            break;\n");
        buf.append("        }\n");
        buf.append("    }\n");
        buf.append("}\n");
        ICompilationUnit cu =
                new com.codenvy.ide.ext.java.jdt.compiler.batch.CompilationUnit(buf.toString().toCharArray(), "E.java", "");

        CompilationUnit astRoot = createAST(cu);
        ASTRewrite rewrite = ASTRewrite.create(astRoot.getAST());

        assertTrue("Parse errors", (astRoot.getFlags() & ASTNode.MALFORMED) == 0);
        TypeDeclaration type = findTypeDeclaration(astRoot, "E");
        MethodDeclaration methodDecl = findMethodDeclaration(type, "foo");
        Block block = methodDecl.getBody();
        List blockStatements = block.statements();
        assertTrue("Number of statements not 1", blockStatements.size() == 1);
        { // insert statements, replace expression
            SwitchStatement switchStatement = (SwitchStatement)blockStatements.get(0);

            List statements = switchStatement.statements();
            assertTrue("Number of statements not 4", statements.size() == 4);

            ExpressionStatement assignment = (ExpressionStatement)statements.get(1); // i= 1;:

            ListRewrite listRewrite = rewrite.getListRewrite(switchStatement, SwitchStatement.STATEMENTS_PROPERTY);
            listRewrite.remove(assignment, null);
        }

        String preview = evaluateRewrite(cu, rewrite);

        buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public void foo(int i) {\n");
        buf.append("        switch (i) {\n");
        buf.append("        case 1:\n");
        buf.append("        case 2:\n");
        buf.append("            break;\n");
        buf.append("        }\n");
        buf.append("    }\n");
        buf.append("}\n");
        assertEqualString(preview, buf.toString());
    }

    /*
    * https://bugs.eclipse.org/bugs/show_bug.cgi?id=246627
    * Remove a statement followed by an inserted statement)
    */
    @Test
    public void testSwitchStatement9() throws Exception {
        StringBuffer buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public void foo(int i) {\n");
        buf.append("        switch (i) {\n");
        buf.append("        case 1:\n");
        buf.append("            i= 1;\n");
        buf.append("        case 3:\n");
        buf.append("            break;\n");
        buf.append("        }\n");
        buf.append("    }\n");
        buf.append("}\n");
        ICompilationUnit cu =
                new com.codenvy.ide.ext.java.jdt.compiler.batch.CompilationUnit(buf.toString().toCharArray(), "E.java", "");

        CompilationUnit astRoot = createAST(cu);
        ASTRewrite rewrite = ASTRewrite.create(astRoot.getAST());

        AST ast = astRoot.getAST();

        assertTrue("Parse errors", (astRoot.getFlags() & ASTNode.MALFORMED) == 0);
        TypeDeclaration type = findTypeDeclaration(astRoot, "E");
        MethodDeclaration methodDecl = findMethodDeclaration(type, "foo");
        Block block = methodDecl.getBody();
        List blockStatements = block.statements();
        assertTrue("Number of statements not 1", blockStatements.size() == 1);
        { // insert statements, replace expression
            SwitchStatement switchStatement = (SwitchStatement)blockStatements.get(0);

            List statements = switchStatement.statements();
            assertTrue("Number of statements not 4", statements.size() == 4);

            ExpressionStatement assignment = (ExpressionStatement)statements.get(1); // i= 1;

            SwitchCase switchCase = ast.newSwitchCase();
            switchCase.setExpression(ast.newNumberLiteral("2"));

            ListRewrite listRewrite = rewrite.getListRewrite(switchStatement, SwitchStatement.STATEMENTS_PROPERTY);
            listRewrite.remove(assignment, null);
            listRewrite.insertAfter(switchCase, assignment, null);
        }

        String preview = evaluateRewrite(cu, rewrite);

        buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public void foo(int i) {\n");
        buf.append("        switch (i) {\n");
        buf.append("        case 1:\n");
        buf.append("        case 2:\n");
        buf.append("        case 3:\n");
        buf.append("            break;\n");
        buf.append("        }\n");
        buf.append("    }\n");
        buf.append("}\n");
        assertEqualString(preview, buf.toString());
    }

    /*
    * https://bugs.eclipse.org/bugs/show_bug.cgi?id=246627
    * Remove a statement preceded by an inserted statement)
    */
    @Test
    @Ignore
    public void testSwitchStatement10() throws Exception {
        StringBuffer buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public void foo(int i) {\n");
        buf.append("        switch (i) {\n");
        buf.append("        case 1:\n");
        buf.append("            i= 1;\n");
        buf.append("        case 2:\n");
        buf.append("            break;\n");
        buf.append("        }\n");
        buf.append("    }\n");
        buf.append("}\n");
        ICompilationUnit cu =
                new com.codenvy.ide.ext.java.jdt.compiler.batch.CompilationUnit(buf.toString().toCharArray(), "E.java", "");

        CompilationUnit astRoot = createAST(cu);
        ASTRewrite rewrite = ASTRewrite.create(astRoot.getAST());

        AST ast = astRoot.getAST();

        assertTrue("Parse errors", (astRoot.getFlags() & ASTNode.MALFORMED) == 0);
        TypeDeclaration type = findTypeDeclaration(astRoot, "E");
        MethodDeclaration methodDecl = findMethodDeclaration(type, "foo");
        Block block = methodDecl.getBody();
        List blockStatements = block.statements();
        assertTrue("Number of statements not 1", blockStatements.size() == 1);
        { // insert statements, replace expression
            SwitchStatement switchStatement = (SwitchStatement)blockStatements.get(0);

            List statements = switchStatement.statements();
            assertTrue("Number of statements not 4", statements.size() == 4);

            ExpressionStatement assignment = (ExpressionStatement)statements.get(1); // i= 1;:

            ListRewrite listRewrite = rewrite.getListRewrite(switchStatement, SwitchStatement.STATEMENTS_PROPERTY);
            listRewrite.insertBefore(ast.newBreakStatement(), assignment, null);
            listRewrite.remove(assignment, null);
        }

        String preview = evaluateRewrite(cu, rewrite);

        buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public void foo(int i) {\n");
        buf.append("        switch (i) {\n");
        buf.append("        case 1:\n");
        buf.append("            break;\n");
        buf.append("        case 2:\n");
        buf.append("            break;\n");
        buf.append("        }\n");
        buf.append("    }\n");
        buf.append("}\n");
        assertEqualString(preview, buf.toString());
    }

    //https://bugs.eclipse.org/bugs/show_bug.cgi?id=276938
    @Test
    public void testSwitchStatement11() throws Exception {
        StringBuffer buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public void foo(int i) {\n");
        buf.append("        switch(4){\n");
        buf.append("           case 4:break;break;\n");
        buf.append("            default:System.out.println(\"Not 4\");\n");
        buf.append("        }\n");
        buf.append("    }\n");
        buf.append("}\n");
        ICompilationUnit cu =
                new com.codenvy.ide.ext.java.jdt.compiler.batch.CompilationUnit(buf.toString().toCharArray(), "E.java", "");

        CompilationUnit astRoot = createAST(cu);
        ASTRewrite rewrite = ASTRewrite.create(astRoot.getAST());

        assertTrue("Parse errors", (astRoot.getFlags() & ASTNode.MALFORMED) == 0);
        TypeDeclaration type = findTypeDeclaration(astRoot, "E");
        MethodDeclaration methodDecl = findMethodDeclaration(type, "foo");
        Block block = methodDecl.getBody();
        List blockStatements = block.statements();
        assertTrue("Number of statements not 1", blockStatements.size() == 1);
        { // insert statements, replace expression
            SwitchStatement switchStatement = (SwitchStatement)blockStatements.get(0);

            List statements = switchStatement.statements();
            assertTrue("Number of statements not 5", statements.size() == 5);

            BreakStatement breakStatement = (BreakStatement)statements.get(2); // break;:

            ListRewrite listRewrite = rewrite.getListRewrite(switchStatement, SwitchStatement.STATEMENTS_PROPERTY);
            listRewrite.remove(breakStatement, null);
        }

        String preview = evaluateRewrite(cu, rewrite);

        buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public void foo(int i) {\n");
        buf.append("        switch(4){\n");
        buf.append("           case 4:break;\n");
        buf.append("            default:System.out.println(\"Not 4\");\n");
        buf.append("        }\n");
        buf.append("    }\n");
        buf.append("}\n");
        assertEqualString(preview, buf.toString());
    }

    //https://bugs.eclipse.org/bugs/show_bug.cgi?id=276938
    @Test
    public void testSwitchStatement12() throws Exception {
        StringBuffer buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public void foo(int i) {\n");
        buf.append("        switch(4){\n");
        buf.append("           case 4:break;break;default:System.out.println(\"Not 4\");\n");
        buf.append("        }\n");
        buf.append("    }\n");
        buf.append("}\n");
        ICompilationUnit cu =
                new com.codenvy.ide.ext.java.jdt.compiler.batch.CompilationUnit(buf.toString().toCharArray(), "E.java", "");

        CompilationUnit astRoot = createAST(cu);
        ASTRewrite rewrite = ASTRewrite.create(astRoot.getAST());

        assertTrue("Parse errors", (astRoot.getFlags() & ASTNode.MALFORMED) == 0);
        TypeDeclaration type = findTypeDeclaration(astRoot, "E");
        MethodDeclaration methodDecl = findMethodDeclaration(type, "foo");
        Block block = methodDecl.getBody();
        List blockStatements = block.statements();
        assertTrue("Number of statements not 1", blockStatements.size() == 1);
        { // insert statements, replace expression
            SwitchStatement switchStatement = (SwitchStatement)blockStatements.get(0);

            List statements = switchStatement.statements();
            assertTrue("Number of statements not 5", statements.size() == 5);

            BreakStatement breakStatement = (BreakStatement)statements.get(2); // break;:

            ListRewrite listRewrite = rewrite.getListRewrite(switchStatement, SwitchStatement.STATEMENTS_PROPERTY);
            listRewrite.remove(breakStatement, null);
        }

        String preview = evaluateRewrite(cu, rewrite);

        buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public void foo(int i) {\n");
        buf.append("        switch(4){\n");
        buf.append("           case 4:break;default:System.out.println(\"Not 4\");\n");
        buf.append("        }\n");
        buf.append("    }\n");
        buf.append("}\n");
        assertEqualString(preview, buf.toString());
    }

    //https://bugs.eclipse.org/bugs/show_bug.cgi?id=290730
    @Test
    @Ignore
    public void testSwitchStatement13() throws Exception {
        StringBuffer buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public void foo(int i) {\n");
        buf.append("        switch(4){\n");
        buf.append("            case 4:break;default:System.out.println(\"Not 4\");\n");
        buf.append("        }\n");
        buf.append("    }\n");
        buf.append("}\n");
        ICompilationUnit cu =
                new com.codenvy.ide.ext.java.jdt.compiler.batch.CompilationUnit(buf.toString().toCharArray(), "E.java", "");

        CompilationUnit astRoot = createAST(cu);

        ASTRewrite rewrite = ASTRewrite.create(astRoot.getAST());
        List types = astRoot.types();
        List bodyDeclarations = ((AbstractTypeDeclaration)types.get(0)).bodyDeclarations();
        MethodDeclaration methodDeclaration = (MethodDeclaration)bodyDeclarations.get(0);
        List statements = methodDeclaration.getBody().statements();
        SwitchStatement swtch = (SwitchStatement)statements.get(0);
        String toAppend = "case 5:\nSystem.out.println(\"This is 5\");break;";
        ListRewrite lrw = rewrite.getListRewrite(swtch, SwitchStatement.STATEMENTS_PROPERTY);
        ASTNode placeHolder = rewrite.createStringPlaceholder(toAppend, ASTNode.BLOCK);
        lrw.insertLast(placeHolder, null);

        Document document1 = new DocumentImpl(String.valueOf(cu.getContents()));
        TextEdit res = rewrite.rewriteAST(document1, null);
        res.apply(document1);
        String preview = document1.get();

        buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public void foo(int i) {\n");
        buf.append("        switch(4){\n");
        buf.append("            case 4:break;default:System.out.println(\"Not 4\");\n");
        buf.append("         case 5:\n");
        buf.append("         System.out.println(\"This is 5\");break;\n");
        buf.append("        }\n");
        buf.append("    }\n");
        buf.append("}\n");
        assertEqualString(preview, buf.toString());
    }

    @Test
    public void testSynchronizedStatement() throws Exception {
        StringBuffer buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public void foo() {\n");
        buf.append("        synchronized(this) {\n");
        buf.append("            System.beep();\n");
        buf.append("        }\n");
        buf.append("    }\n");
        buf.append("}\n");
        ICompilationUnit cu =
                new com.codenvy.ide.ext.java.jdt.compiler.batch.CompilationUnit(buf.toString().toCharArray(), "E.java", "");

        CompilationUnit astRoot = createAST(cu);
        ASTRewrite rewrite = ASTRewrite.create(astRoot.getAST());

        AST ast = astRoot.getAST();

        assertTrue("Parse errors", (astRoot.getFlags() & ASTNode.MALFORMED) == 0);
        TypeDeclaration type = findTypeDeclaration(astRoot, "E");
        MethodDeclaration methodDecl = findMethodDeclaration(type, "foo");
        Block block = methodDecl.getBody();
        List statements = block.statements();
        assertTrue("Number of statements not 1", statements.size() == 1);

        { // replace expression and body
            SynchronizedStatement statement = (SynchronizedStatement)statements.get(0);
            ASTNode newExpression = ast.newSimpleName("obj");
            rewrite.replace(statement.getExpression(), newExpression, null);

            Block newBody = ast.newBlock();

            Assignment assign = ast.newAssignment();
            assign.setLeftHandSide(ast.newSimpleName("x"));
            assign.setRightHandSide(ast.newNumberLiteral("1"));
            assign.setOperator(Assignment.Operator.ASSIGN);

            newBody.statements().add(ast.newExpressionStatement(assign));

            rewrite.replace(statement.getBody(), newBody, null);
        }

        String preview = evaluateRewrite(cu, rewrite);

        buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public void foo() {\n");
        buf.append("        synchronized(obj) {\n");
        buf.append("            x = 1;\n");
        buf.append("        }\n");
        buf.append("    }\n");
        buf.append("}\n");
        assertEqualString(preview, buf.toString());

    }

    @Test
    public void testTryStatement() throws Exception {
        StringBuffer buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public void foo(int i) {\n");
        buf.append("        try {\n");
        buf.append("        } finally {\n");
        buf.append("        }\n");
        buf.append("        try {\n");
        buf.append("        } catch (IOException e) {\n");
        buf.append("        } finally {\n");
        buf.append("        }\n");
        buf.append("        try {\n");
        buf.append("        } catch (IOException e) {\n");
        buf.append("        }\n");
        buf.append("        try {\n");
        buf.append("        } catch (IOException e) {\n");
        buf.append("        }\n");
        buf.append("    }\n");
        buf.append("}\n");
        ICompilationUnit cu =
                new com.codenvy.ide.ext.java.jdt.compiler.batch.CompilationUnit(buf.toString().toCharArray(), "E.java", "");

        CompilationUnit astRoot = createAST(cu);
        ASTRewrite rewrite = ASTRewrite.create(astRoot.getAST());

        AST ast = astRoot.getAST();

        assertTrue("Parse errors", (astRoot.getFlags() & ASTNode.MALFORMED) == 0);
        TypeDeclaration type = findTypeDeclaration(astRoot, "E");
        MethodDeclaration methodDecl = findMethodDeclaration(type, "foo");
        Block block = methodDecl.getBody();
        List blockStatements = block.statements();
        assertTrue("Number of statements not 4", blockStatements.size() == 4);
        { // add catch, replace finally
            TryStatement tryStatement = (TryStatement)blockStatements.get(0);

            CatchClause catchClause = ast.newCatchClause();
            SingleVariableDeclaration decl = ast.newSingleVariableDeclaration();
            decl.setType(ast.newSimpleType(ast.newSimpleName("IOException")));
            decl.setName(ast.newSimpleName("e"));
            catchClause.setException(decl);

            rewrite.getListRewrite(tryStatement, TryStatement.CATCH_CLAUSES_PROPERTY).insertLast(catchClause, null);

            Block body = ast.newBlock();
            body.statements().add(ast.newReturnStatement());

            rewrite.replace(tryStatement.getFinally(), body, null);
        }
        { // replace catch, remove finally
            TryStatement tryStatement = (TryStatement)blockStatements.get(1);

            List catchClauses = tryStatement.catchClauses();

            CatchClause catchClause = ast.newCatchClause();
            SingleVariableDeclaration decl = ast.newSingleVariableDeclaration();
            decl.setType(ast.newSimpleType(ast.newSimpleName("Exception")));
            decl.setName(ast.newSimpleName("x"));
            catchClause.setException(decl);

            rewrite.replace((ASTNode)catchClauses.get(0), catchClause, null);

            rewrite.remove(tryStatement.getFinally(), null);
        }
        { // remove catch, add finally
            TryStatement tryStatement = (TryStatement)blockStatements.get(2);

            List catchClauses = tryStatement.catchClauses();
            rewrite.remove((ASTNode)catchClauses.get(0), null);

            Block body = ast.newBlock();
            body.statements().add(ast.newReturnStatement());

            rewrite.set(tryStatement, TryStatement.FINALLY_PROPERTY, body, null);
        }
        { // insert catch before and after existing
            TryStatement tryStatement = (TryStatement)blockStatements.get(3);

            CatchClause catchClause1 = ast.newCatchClause();
            SingleVariableDeclaration decl1 = ast.newSingleVariableDeclaration();
            decl1.setType(ast.newSimpleType(ast.newSimpleName("ParseException")));
            decl1.setName(ast.newSimpleName("e"));
            catchClause1.setException(decl1);

            rewrite.getListRewrite(tryStatement, TryStatement.CATCH_CLAUSES_PROPERTY).insertFirst(catchClause1, null);

            CatchClause catchClause2 = ast.newCatchClause();
            SingleVariableDeclaration decl2 = ast.newSingleVariableDeclaration();
            decl2.setType(ast.newSimpleType(ast.newSimpleName("FooException")));
            decl2.setName(ast.newSimpleName("e"));
            catchClause2.setException(decl2);

            rewrite.getListRewrite(tryStatement, TryStatement.CATCH_CLAUSES_PROPERTY).insertLast(catchClause2, null);
        }

        String preview = evaluateRewrite(cu, rewrite);

        buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public void foo(int i) {\n");
        buf.append("        try {\n");
        buf.append("        } catch (IOException e) {\n");
        buf.append("        } finally {\n");
        buf.append("            return;\n");
        buf.append("        }\n");
        buf.append("        try {\n");
        buf.append("        } catch (Exception x) {\n");
        buf.append("        }\n");
        buf.append("        try {\n");
        buf.append("        } finally {\n");
        buf.append("            return;\n");
        buf.append("        }\n");
        buf.append("        try {\n");
        buf.append("        } catch (ParseException e) {\n");
        buf.append("        } catch (IOException e) {\n");
        buf.append("        } catch (FooException e) {\n");
        buf.append("        }\n");
        buf.append("    }\n");
        buf.append("}\n");
        assertEqualString(preview, buf.toString());
    }

    @Test
    public void testTryStatement2() throws Exception {
        StringBuffer buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public void foo(int i) {\n");
        buf.append("        try {\n");
        buf.append("            throw new IOException();\n");
        buf.append("        } catch (IOException e) {\n");
        buf.append("        }\n");
        buf.append("    }\n");
        buf.append("}\n");
        ICompilationUnit cu =
                new com.codenvy.ide.ext.java.jdt.compiler.batch.CompilationUnit(buf.toString().toCharArray(), "E.java", "");

        CompilationUnit astRoot = createAST(AST.JLS4, cu, false);
        AST ast = astRoot.getAST();
        ASTRewrite rewrite = ASTRewrite.create(ast);

        assertTrue("Parse errors", (astRoot.getFlags() & ASTNode.MALFORMED) == 0);
        TypeDeclaration type = findTypeDeclaration(astRoot, "E");
        MethodDeclaration methodDecl = findMethodDeclaration(type, "foo");
        Block block = methodDecl.getBody();
        List blockStatements = block.statements();
        assertTrue("Number of statements not 1", blockStatements.size() == 1);
        { // replace catch exception type with a union type
            TryStatement tryStatement = (TryStatement)blockStatements.get(0);

            List catchClauses = tryStatement.catchClauses();

            CatchClause catchClause = (CatchClause)catchClauses.get(0);
            SingleVariableDeclaration exception = catchClause.getException();
            UnionType unionType = ast.newUnionType();
            unionType.types().add(ast.newSimpleType(ast.newSimpleName("IOException")));
            unionType.types().add(ast.newSimpleType(ast.newSimpleName("Exception")));
            rewrite.set(exception, SingleVariableDeclaration.TYPE_PROPERTY, unionType, null);
        }

        String preview = evaluateRewrite(cu, rewrite);

        buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public void foo(int i) {\n");
        buf.append("        try {\n");
        buf.append("            throw new IOException();\n");
        buf.append("        } catch (IOException | Exception e) {\n");
        buf.append("        }\n");
        buf.append("    }\n");
        buf.append("}\n");
        assertEqualString(preview, buf.toString());
    }

    @Test
    public void testTryStatement3() throws Exception {
        StringBuffer buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public void foo(int i) {\n");
        buf.append("        try {\n");
        buf.append("            throw new IOException();\n");
        buf.append("        } catch (IOException | Exception e) {\n");
        buf.append("        }\n");
        buf.append("    }\n");
        buf.append("}\n");
        ICompilationUnit cu =
                new com.codenvy.ide.ext.java.jdt.compiler.batch.CompilationUnit(buf.toString().toCharArray(), "E.java", "");

        CompilationUnit astRoot = createAST(AST.JLS4, cu, false);
        AST ast = astRoot.getAST();
        ASTRewrite rewrite = ASTRewrite.create(ast);

        assertTrue("Parse errors", (astRoot.getFlags() & ASTNode.MALFORMED) == 0);
        TypeDeclaration type = findTypeDeclaration(astRoot, "E");
        MethodDeclaration methodDecl = findMethodDeclaration(type, "foo");
        Block block = methodDecl.getBody();
        List blockStatements = block.statements();
        assertTrue("Number of statements not 1", blockStatements.size() == 1);
        { // replace catch exception type with a union type
            TryStatement tryStatement = (TryStatement)blockStatements.get(0);

            List catchClauses = tryStatement.catchClauses();

            CatchClause catchClause = (CatchClause)catchClauses.get(0);
            SingleVariableDeclaration exception = catchClause.getException();
            UnionType unionType = (UnionType)exception.getType();

            SimpleType exceptionType = (SimpleType)unionType.types().get(0);
            rewrite.getListRewrite(unionType, UnionType.TYPES_PROPERTY).replace(exceptionType,
                                                                                ast.newSimpleType(
                                                                                        ast.newSimpleName("FileNotFoundException")), null);
        }

        String preview = evaluateRewrite(cu, rewrite);

        buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public void foo(int i) {\n");
        buf.append("        try {\n");
        buf.append("            throw new IOException();\n");
        buf.append("        } catch (FileNotFoundException | Exception e) {\n");
        buf.append("        }\n");
        buf.append("    }\n");
        buf.append("}\n");
        assertEqualString(preview, buf.toString());
    }

    @Test
    public void testTryStatement4() throws Exception {
        StringBuffer buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public void foo(int i) {\n");
        buf.append("        try {\n");
        buf.append("            throw new IOException();\n");
        buf.append("        } catch (IOException | Exception e) {\n");
        buf.append("        } finally {\n");
        buf.append("        }\n");
        buf.append("    }\n");
        buf.append("}\n");
        ICompilationUnit cu =
                new com.codenvy.ide.ext.java.jdt.compiler.batch.CompilationUnit(buf.toString().toCharArray(), "E.java", "");

        CompilationUnit astRoot = createAST(AST.JLS4, cu, false);
        ASTRewrite rewrite = ASTRewrite.create(astRoot.getAST());

        assertTrue("Parse errors", (astRoot.getFlags() & ASTNode.MALFORMED) == 0);
        TypeDeclaration type = findTypeDeclaration(astRoot, "E");
        MethodDeclaration methodDecl = findMethodDeclaration(type, "foo");
        Block block = methodDecl.getBody();
        List blockStatements = block.statements();
        assertTrue("Number of statements not 1", blockStatements.size() == 1);
        { // remove finally
            TryStatement tryStatement = (TryStatement)blockStatements.get(0);

            rewrite.remove(tryStatement.getFinally(), null);
        }

        String preview = evaluateRewrite(cu, rewrite);

        buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public void foo(int i) {\n");
        buf.append("        try {\n");
        buf.append("            throw new IOException();\n");
        buf.append("        } catch (IOException | Exception e) {\n");
        buf.append("        }\n");
        buf.append("    }\n");
        buf.append("}\n");
        assertEqualString(preview, buf.toString());
    }

    @Test
    public void testTryStatementWithResources() throws Exception {
        StringBuffer buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public void foo(int i) {\n");
        buf.append("        try (Reader reader = null) {\n");
        buf.append("        } finally {\n");
        buf.append("        }\n");
        buf.append("        try (Reader reader = null) {\n");
        buf.append("        } catch (IOException e) {\n");
        buf.append("        } finally {\n");
        buf.append("        }\n");
        buf.append("    }\n");
        buf.append("}\n");
        ICompilationUnit cu =
                new com.codenvy.ide.ext.java.jdt.compiler.batch.CompilationUnit(buf.toString().toCharArray(), "E.java", "");

        CompilationUnit astRoot = createAST(AST.JLS4, cu, false);
        AST ast = astRoot.getAST();
        ASTRewrite rewrite = ASTRewrite.create(ast);

        assertTrue("Parse errors", (astRoot.getFlags() & ASTNode.MALFORMED) == 0);
        TypeDeclaration type = findTypeDeclaration(astRoot, "E");
        MethodDeclaration methodDecl = findMethodDeclaration(type, "foo");
        Block block = methodDecl.getBody();
        List blockStatements = block.statements();
        assertTrue("Number of statements not 2", blockStatements.size() == 2);
        { // add catch, replace finally
            TryStatement tryStatement = (TryStatement)blockStatements.get(0);

            CatchClause catchClause = ast.newCatchClause();
            SingleVariableDeclaration decl = ast.newSingleVariableDeclaration();
            decl.setType(ast.newSimpleType(ast.newSimpleName("IOException")));
            decl.setName(ast.newSimpleName("e"));
            catchClause.setException(decl);

            rewrite.getListRewrite(tryStatement, TryStatement.CATCH_CLAUSES_PROPERTY).insertLast(catchClause, null);

            Block body = ast.newBlock();
            body.statements().add(ast.newReturnStatement());

            rewrite.replace(tryStatement.getFinally(), body, null);
        }
        { // add resource
            TryStatement tryStatement = (TryStatement)blockStatements.get(0);
            VariableDeclarationFragment fragment = ast.newVariableDeclarationFragment();
            fragment.setName(ast.newSimpleName("reader2"));
            fragment.setInitializer(ast.newNullLiteral());
            VariableDeclarationExpression resource = ast.newVariableDeclarationExpression(fragment);
            resource.setType(ast.newSimpleType(ast.newSimpleName("Reader")));

            rewrite.getListRewrite(tryStatement, TryStatement.RESOURCES_PROPERTY).insertLast(resource, null);
        }
        { // replace catch, remove finally
            TryStatement tryStatement = (TryStatement)blockStatements.get(1);

            List catchClauses = tryStatement.catchClauses();

            CatchClause catchClause = ast.newCatchClause();
            SingleVariableDeclaration decl = ast.newSingleVariableDeclaration();
            decl.setType(ast.newSimpleType(ast.newSimpleName("Exception")));
            decl.setName(ast.newSimpleName("x"));
            catchClause.setException(decl);

            rewrite.replace((ASTNode)catchClauses.get(0), catchClause, null);

            rewrite.remove(tryStatement.getFinally(), null);
        }

        String preview = evaluateRewrite(cu, rewrite);

        buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public void foo(int i) {\n");
        buf.append("        try (Reader reader = null;\n");
        buf.append("                Reader reader2 = null) {\n");
        buf.append("        } catch (IOException e) {\n");
        buf.append("        } finally {\n");
        buf.append("            return;\n");
        buf.append("        }\n");
        buf.append("        try (Reader reader = null) {\n");
        buf.append("        } catch (Exception x) {\n");
        buf.append("        }\n");
        buf.append("    }\n");
        buf.append("}\n");
        assertEqualString(preview, buf.toString());
    }

    @Test
    public void testTryStatementWithResources2() throws Exception {
        StringBuffer buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public void foo(int i) {\n");
        buf.append("        try (Reader reader = null) {\n");
        buf.append("        } catch (IOException e) {\n");
        buf.append("        } finally {\n");
        buf.append("        }\n");
        buf.append("    }\n");
        buf.append("}\n");
        ICompilationUnit cu =
                new com.codenvy.ide.ext.java.jdt.compiler.batch.CompilationUnit(buf.toString().toCharArray(), "E.java", "");

        CompilationUnit astRoot = createAST(AST.JLS4, cu, false);
        AST ast = astRoot.getAST();
        ASTRewrite rewrite = ASTRewrite.create(ast);

        assertTrue("Parse errors", (astRoot.getFlags() & ASTNode.MALFORMED) == 0);
        TypeDeclaration type = findTypeDeclaration(astRoot, "E");
        MethodDeclaration methodDecl = findMethodDeclaration(type, "foo");
        Block block = methodDecl.getBody();
        List blockStatements = block.statements();
        assertTrue("Number of statements not 1", blockStatements.size() == 1);
        { // rename resource
            TryStatement tryStatement = (TryStatement)blockStatements.get(0);

            VariableDeclarationExpression resource = (VariableDeclarationExpression)tryStatement.resources().get(0);
            VariableDeclarationFragment fragment = (VariableDeclarationFragment)resource.fragments().get(0);

            rewrite.set(fragment, VariableDeclarationFragment.NAME_PROPERTY, ast.newSimpleName("r1"), null);
        }

        String preview = evaluateRewrite(cu, rewrite);

        buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public void foo(int i) {\n");
        buf.append("        try (Reader r1 = null) {\n");
        buf.append("        } catch (IOException e) {\n");
        buf.append("        } finally {\n");
        buf.append("        }\n");
        buf.append("    }\n");
        buf.append("}\n");
        assertEqualString(preview, buf.toString());
    }

    /** https://bugs.eclipse.org/bugs/show_bug.cgi?id=351170 */
    @Test
    public void testTryStatementWithResources3() throws Exception {
        StringBuffer buf = new StringBuffer();
        buf.append("package test0017;\n");
        buf.append("\n");
        buf.append("public class X {\n");
        buf.append("   void foo() {\n");
        buf.append("      FileReader reader1 = new FileReader(\"file1\");\n");
        buf.append("      try {\n");
        buf.append("         int ch;\n");
        buf.append("         while ((ch = reader1.read()) != -1) {\n");
        buf.append("            System.out.println(ch);\n");
        buf.append("         }\n");
        buf.append("      } finally {\n");
        buf.append("      }\n");
        buf.append("   }\n");
        buf.append("}");

        ICompilationUnit cu =
                new com.codenvy.ide.ext.java.jdt.compiler.batch.CompilationUnit(buf.toString().toCharArray(), "X.java", "");
        CompilationUnit astRoot = createAST(AST.JLS4, cu, true, true);
        AST ast = astRoot.getAST();
        ASTRewrite rewrite = ASTRewrite.create(ast);

        Block block = ((MethodDeclaration)((TypeDeclaration)astRoot.types().get(0)).bodyDeclarations().get(0)).getBody();
        List statements = block.statements();
        Statement statement = (Statement)statements.get(1);
        assertTrue(statement instanceof TryStatement);

        TryStatement tryStatement = (TryStatement)statement;

        VariableDeclarationStatement variableDeclarationStatement = (VariableDeclarationStatement)statements.get(0);
        VariableDeclarationFragment fragment =
                (VariableDeclarationFragment)variableDeclarationStatement.fragments().get(0);
        VariableDeclarationExpression newVariableDeclarationExpression =
                ast.newVariableDeclarationExpression((VariableDeclarationFragment)rewrite.createCopyTarget(fragment));
        newVariableDeclarationExpression.setType((Type)rewrite.createCopyTarget(variableDeclarationStatement.getType()));

        ListRewrite listRewrite = rewrite.getListRewrite(tryStatement, TryStatement.RESOURCES_PROPERTY);
        listRewrite.insertLast(newVariableDeclarationExpression, null);
        rewrite.remove(variableDeclarationStatement, null);

        Document document1 = new DocumentImpl(String.valueOf(cu.getContents()));
        TextEdit res = rewrite.rewriteAST(document1, null);
        res.apply(document1);
        String preview = document1.get();

        buf = new StringBuffer();
        buf.append("package test0017;\n");
        buf.append("\n");
        buf.append("public class X {\n");
        buf.append("   void foo() {\n");
        buf.append("      try (FileReader reader1 = new FileReader(\"file1\")) {\n");
        buf.append("         int ch;\n");
        buf.append("         while ((ch = reader1.read()) != -1) {\n");
        buf.append("            System.out.println(ch);\n");
        buf.append("         }\n");
        buf.append("      } finally {\n");
        buf.append("      }\n");
        buf.append("   }\n");
        buf.append("}");
        assertEqualString(preview, buf.toString());
    }

    /** https://bugs.eclipse.org/bugs/show_bug.cgi?id=351170 */
    @Test
    public void testTryStatementWithResources4() throws Exception {
        StringBuffer buf = new StringBuffer();
        buf.append("package test0017;\n");
        buf.append("\n");
        buf.append("public class X {\n");
        buf.append("   void foo() {\n");
        buf.append("      try (FileReader reader1 = new FileReader(\"file1\")) {\n");
        buf.append("         int ch;\n");
        buf.append("         while ((ch = reader1.read()) != -1) {\n");
        buf.append("            System.out.println(ch);\n");
        buf.append("         }\n");
        buf.append("      } finally {\n");
        buf.append("      }\n");
        buf.append("   }\n");
        buf.append("}");

        ICompilationUnit cu =
                new com.codenvy.ide.ext.java.jdt.compiler.batch.CompilationUnit(buf.toString().toCharArray(), "X.java", "");
        CompilationUnit astRoot = createAST(AST.JLS4, cu, true, true);
        AST ast = astRoot.getAST();
        ASTRewrite rewrite = ASTRewrite.create(ast);

        Block block = ((MethodDeclaration)((TypeDeclaration)astRoot.types().get(0)).bodyDeclarations().get(0)).getBody();
        List statements = block.statements();
        Statement statement = (Statement)statements.get(0);
        assertTrue(statement instanceof TryStatement);

        TryStatement tryStatement = (TryStatement)statement;

        VariableDeclarationFragment fragment = ast.newVariableDeclarationFragment();
        fragment.setExtraDimensions(0);
        fragment.setName(ast.newSimpleName("reader2"));
        ClassInstanceCreation classInstanceCreation = ast.newClassInstanceCreation();
        classInstanceCreation.setType(ast.newSimpleType(ast.newSimpleName("FileReader")));
        StringLiteral literal = ast.newStringLiteral();
        literal.setLiteralValue("file2");
        classInstanceCreation.arguments().add(literal);
        fragment.setInitializer(classInstanceCreation);
        VariableDeclarationExpression newVariableDeclarationExpression = ast.newVariableDeclarationExpression(fragment);
        newVariableDeclarationExpression.setType(ast.newSimpleType(ast.newSimpleName("FileReader")));

        ListRewrite listRewrite = rewrite.getListRewrite(tryStatement, TryStatement.RESOURCES_PROPERTY);
        listRewrite.insertLast(newVariableDeclarationExpression, null);

        Document document1 = new DocumentImpl(String.valueOf(cu.getContents()));
        TextEdit res = rewrite.rewriteAST(document1, null);
        res.apply(document1);
        String preview = document1.get();

        buf = new StringBuffer();
        buf.append("package test0017;\n");
        buf.append("\n");
        buf.append("public class X {\n");
        buf.append("   void foo() {\n");
        buf.append("      try (FileReader reader1 = new FileReader(\"file1\");\n");
        buf.append("            FileReader reader2 = new FileReader(\"file2\")) {\n");
        buf.append("         int ch;\n");
        buf.append("         while ((ch = reader1.read()) != -1) {\n");
        buf.append("            System.out.println(ch);\n");
        buf.append("         }\n");
        buf.append("      } finally {\n");
        buf.append("      }\n");
        buf.append("   }\n");
        buf.append("}");
        assertEqualString(preview, buf.toString());
    }

    /** https://bugs.eclipse.org/bugs/show_bug.cgi?id=351170 */
    @Test
    public void testTryStatementWithResources5() throws Exception {
        StringBuffer buf = new StringBuffer();
        buf.append("package test0017;\n");
        buf.append("\n");
        buf.append("public class X {\n");
        buf.append("   void foo() {\n");
        buf.append("      try (FileReader reader1 = new FileReader(\"file1\");) {\n");
        buf.append("         int ch;\n");
        buf.append("         while ((ch = reader1.read()) != -1) {\n");
        buf.append("            System.out.println(ch);\n");
        buf.append("         }\n");
        buf.append("      } finally {\n");
        buf.append("      }\n");
        buf.append("   }\n");
        buf.append("}");

        ICompilationUnit cu =
                new com.codenvy.ide.ext.java.jdt.compiler.batch.CompilationUnit(buf.toString().toCharArray(), "X.java", "");
        CompilationUnit astRoot = createAST(AST.JLS4, cu, true, true);
        AST ast = astRoot.getAST();
        ASTRewrite rewrite = ASTRewrite.create(ast);

        Block block = ((MethodDeclaration)((TypeDeclaration)astRoot.types().get(0)).bodyDeclarations().get(0)).getBody();
        List statements = block.statements();
        Statement statement = (Statement)statements.get(0);
        assertTrue(statement instanceof TryStatement);

        TryStatement tryStatement = (TryStatement)statement;

        VariableDeclarationFragment fragment = ast.newVariableDeclarationFragment();
        fragment.setExtraDimensions(0);
        fragment.setName(ast.newSimpleName("reader2"));
        ClassInstanceCreation classInstanceCreation = ast.newClassInstanceCreation();
        classInstanceCreation.setType(ast.newSimpleType(ast.newSimpleName("FileReader")));
        StringLiteral literal = ast.newStringLiteral();
        literal.setLiteralValue("file2");
        classInstanceCreation.arguments().add(literal);
        fragment.setInitializer(classInstanceCreation);
        VariableDeclarationExpression newVariableDeclarationExpression = ast.newVariableDeclarationExpression(fragment);
        newVariableDeclarationExpression.setType(ast.newSimpleType(ast.newSimpleName("FileReader")));

        ListRewrite listRewrite = rewrite.getListRewrite(tryStatement, TryStatement.RESOURCES_PROPERTY);
        listRewrite.insertLast(newVariableDeclarationExpression, null);

        Document document1 = new DocumentImpl(String.valueOf(cu.getContents()));
        TextEdit res = rewrite.rewriteAST(document1, null);
        res.apply(document1);
        String preview = document1.get();

        buf = new StringBuffer();
        buf.append("package test0017;\n");
        buf.append("\n");
        buf.append("public class X {\n");
        buf.append("   void foo() {\n");
        buf.append("      try (FileReader reader1 = new FileReader(\"file1\");\n");
        buf.append("            FileReader reader2 = new FileReader(\"file2\");) {\n");
        buf.append("         int ch;\n");
        buf.append("         while ((ch = reader1.read()) != -1) {\n");
        buf.append("            System.out.println(ch);\n");
        buf.append("         }\n");
        buf.append("      } finally {\n");
        buf.append("      }\n");
        buf.append("   }\n");
        buf.append("}");
        assertEqualString(preview, buf.toString());
    }

    @Test
    public void testVariableDeclarationStatement() throws Exception {
        StringBuffer buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class A {\n");
        buf.append("    public void foo() {\n");
        buf.append("        int i1= 1;\n");
        buf.append("        int i2= 1, k2= 2, n2= 3;\n");
        buf.append("        final int i3= 1, k3= 2, n3= 3;\n");
        buf.append("    }\n");
        buf.append("}\n");
        ICompilationUnit cu =
                new com.codenvy.ide.ext.java.jdt.compiler.batch.CompilationUnit(buf.toString().toCharArray(), "A.java", "");

        CompilationUnit astRoot = createAST(cu);
        ASTRewrite rewrite = ASTRewrite.create(astRoot.getAST());

        AST ast = astRoot.getAST();

        assertTrue("Parse errors", (astRoot.getFlags() & ASTNode.MALFORMED) == 0);
        TypeDeclaration type = findTypeDeclaration(astRoot, "A");

        MethodDeclaration methodDecl = findMethodDeclaration(type, "foo");
        Block block = methodDecl.getBody();
        assertTrue("Parse errors", (block.getFlags() & ASTNode.MALFORMED) == 0);

        List statements = block.statements();
        assertTrue("Number of statements not 3", statements.size() == 3);
        { // add modifier, change type, add fragment
            VariableDeclarationStatement decl = (VariableDeclarationStatement)statements.get(0);

            // add modifier
            int newModifiers = Modifier.FINAL;
            rewrite.set(decl, VariableDeclarationStatement.MODIFIERS_PROPERTY, new Integer(newModifiers), null);

            PrimitiveType newType = ast.newPrimitiveType(PrimitiveType.BOOLEAN);
            rewrite.replace(decl.getType(), newType, null);

            VariableDeclarationFragment frag = ast.newVariableDeclarationFragment();
            frag.setName(ast.newSimpleName("k1"));
            frag.setInitializer(null);

            rewrite.getListRewrite(decl, VariableDeclarationStatement.FRAGMENTS_PROPERTY).insertLast(frag, null);
        }
        { // add modifiers, remove first two fragments, replace last
            VariableDeclarationStatement decl = (VariableDeclarationStatement)statements.get(1);

            // add modifier
            int newModifiers = Modifier.FINAL;
            rewrite.set(decl, VariableDeclarationStatement.MODIFIERS_PROPERTY, new Integer(newModifiers), null);

            List fragments = decl.fragments();
            assertTrue("Number of fragments not 3", fragments.size() == 3);

            rewrite.remove((ASTNode)fragments.get(0), null);
            rewrite.remove((ASTNode)fragments.get(1), null);

            VariableDeclarationFragment frag = ast.newVariableDeclarationFragment();
            frag.setName(ast.newSimpleName("k2"));
            frag.setInitializer(null);

            rewrite.replace((ASTNode)fragments.get(2), frag, null);
        }
        { // remove modifiers
            VariableDeclarationStatement decl = (VariableDeclarationStatement)statements.get(2);

            // add modifiers
            int newModifiers = 0;
            rewrite.set(decl, VariableDeclarationStatement.MODIFIERS_PROPERTY, new Integer(newModifiers), null);
        }

        String preview = evaluateRewrite(cu, rewrite);

        buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class A {\n");
        buf.append("    public void foo() {\n");
        buf.append("        final boolean i1= 1, k1;\n");
        buf.append("        final int k2;\n");
        buf.append("        int i3= 1, k3= 2, n3= 3;\n");
        buf.append("    }\n");
        buf.append("}\n");

        assertEqualString(preview, buf.toString());

    }

    @Test
    public void testWhileStatement() throws Exception {
        StringBuffer buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public void foo() {\n");
        buf.append("        while (i == j) {\n");
        buf.append("            System.beep();\n");
        buf.append("        }\n");
        buf.append("    }\n");
        buf.append("}\n");
        ICompilationUnit cu =
                new com.codenvy.ide.ext.java.jdt.compiler.batch.CompilationUnit(buf.toString().toCharArray(), "E.java", "");

        CompilationUnit astRoot = createAST(cu);
        ASTRewrite rewrite = ASTRewrite.create(astRoot.getAST());

        AST ast = astRoot.getAST();

        assertTrue("Parse errors", (astRoot.getFlags() & ASTNode.MALFORMED) == 0);
        TypeDeclaration type = findTypeDeclaration(astRoot, "E");
        MethodDeclaration methodDecl = findMethodDeclaration(type, "foo");
        Block block = methodDecl.getBody();
        List statements = block.statements();
        assertTrue("Number of statements not 1", statements.size() == 1);

        { // replace expression and body
            WhileStatement whileStatement = (WhileStatement)statements.get(0);

            BooleanLiteral literal = ast.newBooleanLiteral(true);
            rewrite.replace(whileStatement.getExpression(), literal, null);

            Block newBody = ast.newBlock();

            MethodInvocation invocation = ast.newMethodInvocation();
            invocation.setName(ast.newSimpleName("hoo"));
            invocation.arguments().add(ast.newNumberLiteral("11"));

            newBody.statements().add(ast.newExpressionStatement(invocation));

            rewrite.replace(whileStatement.getBody(), newBody, null);
        }

        String preview = evaluateRewrite(cu, rewrite);

        buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public void foo() {\n");
        buf.append("        while (true) {\n");
        buf.append("            hoo(11);\n");
        buf.append("        }\n");
        buf.append("    }\n");
        buf.append("}\n");
        assertEqualString(preview, buf.toString());

    }

    @Test
    public void testWhileStatement1() throws Exception {
        StringBuffer buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public void foo() {\n");
        buf.append("        while (true) {\n");
        buf.append("            foo();\n");
        buf.append("        }\n");
        buf.append("        while (true)\n");
        buf.append("            foo();\n");
        buf.append("        while (true) {\n");
        buf.append("            foo();\n");
        buf.append("        }\n");
        buf.append("        while (true)\n");
        buf.append("            foo();\n");
        buf.append("    }\n");
        buf.append("}\n");
        ICompilationUnit cu =
                new com.codenvy.ide.ext.java.jdt.compiler.batch.CompilationUnit(buf.toString().toCharArray(), "E.java", "");

        CompilationUnit astRoot = createAST(cu);
        ASTRewrite rewrite = ASTRewrite.create(astRoot.getAST());
        AST ast = astRoot.getAST();

        TypeDeclaration type = findTypeDeclaration(astRoot, "E");
        MethodDeclaration methodDecl = findMethodDeclaration(type, "foo");
        Block block = methodDecl.getBody();
        assertTrue("Parse errors", (block.getFlags() & ASTNode.MALFORMED) == 0);

        List statements = block.statements();
        assertTrue("Number of statements not 4", statements.size() == 4);

        { // replace body block with statement
            WhileStatement whileStatement = (WhileStatement)statements.get(0);

            TryStatement newTry = ast.newTryStatement();
            newTry.getBody().statements().add(ast.newReturnStatement());
            CatchClause newCatchClause = ast.newCatchClause();
            SingleVariableDeclaration varDecl = ast.newSingleVariableDeclaration();
            varDecl.setType(ast.newSimpleType(ast.newSimpleName("Exception")));
            varDecl.setName(ast.newSimpleName("e"));
            newCatchClause.setException(varDecl);
            newTry.catchClauses().add(newCatchClause);

            rewrite.replace(whileStatement.getBody(), newTry, null);
        }
        { // replace body statement with block
            WhileStatement whileStatement = (WhileStatement)statements.get(1);

            Block newBody = ast.newBlock();

            MethodInvocation invocation = ast.newMethodInvocation();
            invocation.setName(ast.newSimpleName("hoo"));
            invocation.arguments().add(ast.newNumberLiteral("11"));

            newBody.statements().add(ast.newExpressionStatement(invocation));

            rewrite.replace(whileStatement.getBody(), newBody, null);

        }
        { // replace body block with block
            WhileStatement whileStatement = (WhileStatement)statements.get(2);

            Block newBody = ast.newBlock();

            MethodInvocation invocation = ast.newMethodInvocation();
            invocation.setName(ast.newSimpleName("hoo"));
            invocation.arguments().add(ast.newNumberLiteral("11"));

            newBody.statements().add(ast.newExpressionStatement(invocation));

            rewrite.replace(whileStatement.getBody(), newBody, null);

        }
        { // replace body statement with body
            WhileStatement whileStatement = (WhileStatement)statements.get(3);

            TryStatement newTry = ast.newTryStatement();
            newTry.getBody().statements().add(ast.newReturnStatement());
            CatchClause newCatchClause = ast.newCatchClause();
            SingleVariableDeclaration varDecl = ast.newSingleVariableDeclaration();
            varDecl.setType(ast.newSimpleType(ast.newSimpleName("Exception")));
            varDecl.setName(ast.newSimpleName("e"));
            newCatchClause.setException(varDecl);
            newTry.catchClauses().add(newCatchClause);

            rewrite.replace(whileStatement.getBody(), newTry, null);
        }

        String preview = evaluateRewrite(cu, rewrite);

        buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public void foo() {\n");
        buf.append("        while (true)\n");
        buf.append("            try {\n");
        buf.append("                return;\n");
        buf.append("            } catch (Exception e) {\n");
        buf.append("            }\n");
        buf.append("        while (true) {\n");
        buf.append("            hoo(11);\n");
        buf.append("        }\n");
        buf.append("        while (true) {\n");
        buf.append("            hoo(11);\n");
        buf.append("        }\n");
        buf.append("        while (true)\n");
        buf.append("            try {\n");
        buf.append("                return;\n");
        buf.append("            } catch (Exception e) {\n");
        buf.append("            }\n");
        buf.append("    }\n");
        buf.append("}\n");
        assertEqualString(preview, buf.toString());

    }

    @Test
    public void testInsertCode() throws Exception {
        StringBuffer buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public void foo() {\n");
        buf.append("        while (i == j) {\n");
        buf.append("            System.beep();\n");
        buf.append("        }\n");
        buf.append("    }\n");
        buf.append("}\n");
        ICompilationUnit cu =
                new com.codenvy.ide.ext.java.jdt.compiler.batch.CompilationUnit(buf.toString().toCharArray(), "E.java", "");

        CompilationUnit astRoot = createAST(cu);
        ASTRewrite rewrite = ASTRewrite.create(astRoot.getAST());

        assertTrue("Parse errors", (astRoot.getFlags() & ASTNode.MALFORMED) == 0);
        TypeDeclaration type = findTypeDeclaration(astRoot, "E");
        MethodDeclaration methodDecl = findMethodDeclaration(type, "foo");
        Block block = methodDecl.getBody();
        List statements = block.statements();
        assertTrue("Number of statements not 1", statements.size() == 1);

        { // replace while statement with comment, insert new statement
            WhileStatement whileStatement = (WhileStatement)statements.get(0);
            String comment = "//hello";
            ASTNode placeHolder = rewrite.createStringPlaceholder(comment, ASTNode.RETURN_STATEMENT);

            rewrite.replace(whileStatement, placeHolder, null);

            StringBuffer buf1 = new StringBuffer();
            buf1.append("if (i == 3) {\n");
            buf1.append("    System.beep();\n");
            buf1.append("}");

            ASTNode placeHolder2 = rewrite.createStringPlaceholder(buf1.toString(), ASTNode.IF_STATEMENT);
            rewrite.getListRewrite(block, Block.STATEMENTS_PROPERTY).insertLast(placeHolder2, null);
        }

        String preview = evaluateRewrite(cu, rewrite);

        buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public void foo() {\n");
        buf.append("        //hello\n");
        buf.append("        if (i == 3) {\n");
        buf.append("            System.beep();\n");
        buf.append("        }\n");
        buf.append("    }\n");
        buf.append("}\n");
        assertEqualString(preview, buf.toString());

    }

    @Test
    public void testInsertComment() throws Exception {
        StringBuffer buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public void foo() {\n");
        buf.append("        while (i == j) {\n");
        buf.append("            System.beep();\n");
        buf.append("        }\n");
        buf.append("    }\n");
        buf.append("}\n");
        ICompilationUnit cu =
                new com.codenvy.ide.ext.java.jdt.compiler.batch.CompilationUnit(buf.toString().toCharArray(), "E.java", "");

        CompilationUnit astRoot = createAST(cu);
        ASTRewrite rewrite = ASTRewrite.create(astRoot.getAST());

        // Get while statement block
        TypeDeclaration typeDecl = (TypeDeclaration)astRoot.types().get(0);
        MethodDeclaration methodDecl = typeDecl.getMethods()[0];
        Block block = methodDecl.getBody();
        List statements = block.statements();
        WhileStatement whileStatement = (WhileStatement)statements.get(0);
        Statement whileBlock = whileStatement.getBody();

        // replace while statement with comment, insert new statement
        StringBuffer comment = new StringBuffer();
        comment.append("/*\n");
        comment.append(" * Here's the block comment I want to insert :-)\n");
        comment.append(" */");
        ASTNode placeHolder = rewrite.createStringPlaceholder(comment.toString(), ASTNode.RETURN_STATEMENT);
        ListRewrite list = rewrite.getListRewrite(whileBlock, Block.STATEMENTS_PROPERTY);
        list.insertFirst(placeHolder, null);

        // Get new code
        String preview = evaluateRewrite(cu, rewrite);

        buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public void foo() {\n");
        buf.append("        while (i == j) {\n");
        buf.append("            /*\n");
        buf.append("             * Here's the block comment I want to insert :-)\n");
        buf.append("             */\n");
        buf.append("            System.beep();\n");
        buf.append("        }\n");
        buf.append("    }\n");
        buf.append("}\n");
        assertEqualString(preview, buf.toString());

    }

}
