/*******************************************************************************
 * Copyright (c) 2007, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.codenvy.ide.ext.java.jdt.internal.corext.fix;

import com.codenvy.ide.ext.java.jdt.codeassistant.api.IProblemLocation;
import com.codenvy.ide.ext.java.jdt.core.compiler.IProblem;
import com.codenvy.ide.ext.java.jdt.core.dom.ASTNode;
import com.codenvy.ide.ext.java.jdt.core.dom.CompilationUnit;
import com.codenvy.ide.ext.java.jdt.text.Document;
import com.codenvy.ide.runtime.CoreException;

import java.util.HashSet;
import java.util.Map;

public class UnimplementedCodeCleanUp extends AbstractMultiFix {

    public static final String MAKE_TYPE_ABSTRACT = "cleanup.make_type_abstract_if_missing_method"; //$NON-NLS-1$

    public UnimplementedCodeCleanUp() {
        super();
    }

    public UnimplementedCodeCleanUp(Map<String, String> settings) {
        super(settings);
    }

    /** {@inheritDoc} */
    @Override
    public String[] getStepDescriptions() {
        if (isEnabled(CleanUpConstants.ADD_MISSING_METHODES))
            return new String[]{MultiFixMessages.INSTANCE.UnimplementedCodeCleanUp_AddUnimplementedMethods_description()};

        if (isEnabled(MAKE_TYPE_ABSTRACT))
            return new String[]{MultiFixMessages.INSTANCE.UnimplementedCodeCleanUp_MakeAbstract_description()};

        return null;
    }

    /** {@inheritDoc} */
    @Override
    public String getPreview() {
        StringBuffer buf = new StringBuffer();

        if (isEnabled(MAKE_TYPE_ABSTRACT)) {
            buf.append("public abstract class Face implements IFace {\n"); //$NON-NLS-1$
        } else {
            buf.append("public class Face implements IFace {\n"); //$NON-NLS-1$
        }
        if (isEnabled(CleanUpConstants.ADD_MISSING_METHODES)) {
            boolean createComments = true;
            //TODO
            //            Boolean.valueOf(PreferenceConstants.getPreference(PreferenceConstants.CODEGEN_ADD_COMMENTS, null))
            //               .booleanValue();
            if (createComments)
                buf.append(indent(getOverridingMethodComment(), "    ")); //$NON-NLS-1$

            buf.append("    @Override\n"); //$NON-NLS-1$
            buf.append("    public void method() {\n"); //$NON-NLS-1$
            buf.append(indent(getMethodBody(), "        ")); //$NON-NLS-1$
            buf.append("    }\n"); //$NON-NLS-1$
        }
        buf.append("}\n"); //$NON-NLS-1$

        return buf.toString();
    }

    /** {@inheritDoc} */
    @Override
    public CleanUpRequirements getRequirements() {
        if (!isEnabled(CleanUpConstants.ADD_MISSING_METHODES) && !isEnabled(MAKE_TYPE_ABSTRACT))
            return super.getRequirements();

        return new CleanUpRequirements(true, false, false, null);
    }

    /** {@inheritDoc} */
    @Override
    protected ICleanUpFix createFix(CompilationUnit unit, Document document) throws CoreException {
        IProblemLocation[] problemLocations = convertProblems(unit.getProblems());
        problemLocations =
                filter(problemLocations, new int[]{IProblem.AbstractMethodMustBeImplemented,
                                                   IProblem.EnumConstantMustImplementAbstractMethod});

        return UnimplementedCodeFix.createCleanUp(unit, isEnabled(CleanUpConstants.ADD_MISSING_METHODES),
                                                  isEnabled(MAKE_TYPE_ABSTRACT), problemLocations, document);
    }

    /** {@inheritDoc} */
    @Override
    protected ICleanUpFix createFix(CompilationUnit unit, Document document, IProblemLocation[] problems)
            throws CoreException {
        IProblemLocation[] problemLocations =
                filter(problems, new int[]{IProblem.AbstractMethodMustBeImplemented,
                                           IProblem.EnumConstantMustImplementAbstractMethod});
        return UnimplementedCodeFix.createCleanUp(unit, isEnabled(CleanUpConstants.ADD_MISSING_METHODES),
                                                  isEnabled(MAKE_TYPE_ABSTRACT), problemLocations, document);
    }

    /** {@inheritDoc} */
    public boolean canFix(IProblemLocation problem) {
        int id = problem.getProblemId();
        if (id == IProblem.AbstractMethodMustBeImplemented || id == IProblem.EnumConstantMustImplementAbstractMethod)
            return isEnabled(CleanUpConstants.ADD_MISSING_METHODES) || isEnabled(MAKE_TYPE_ABSTRACT);

        return false;
    }

    /** {@inheritDoc} */
    @Override
    public int computeNumberOfFixes(CompilationUnit compilationUnit) {
        if (!isEnabled(CleanUpConstants.ADD_MISSING_METHODES) && !isEnabled(MAKE_TYPE_ABSTRACT))
            return 0;

        IProblemLocation[] locations =
                filter(convertProblems(compilationUnit.getProblems()), new int[]{IProblem.AbstractMethodMustBeImplemented,
                                                                                 IProblem.EnumConstantMustImplementAbstractMethod});

        HashSet<ASTNode> types = new HashSet<ASTNode>();
        for (int i = 0; i < locations.length; i++) {
            ASTNode type = UnimplementedCodeFix.getSelectedTypeNode(compilationUnit, locations[i]);
            if (type != null) {
                types.add(type);
            }
        }

        return types.size();
    }

    private String getOverridingMethodComment() {
        //      String templateName = CodeTemplateContextType.OVERRIDECOMMENT_ID;
        //
        //      Template template = getCodeTemplate(templateName);
        //      if (template == null)
        //         return ""; //$NON-NLS-1$
        //
        //      CodeTemplateContext context = new CodeTemplateContext(template.getContextTypeId(), null, "\n"); //$NON-NLS-1$
        //
        //      context.setVariable(CodeTemplateContextType.FILENAME, "Face.java"); //$NON-NLS-1$
        //      context.setVariable(CodeTemplateContextType.PACKAGENAME, "test"); //$NON-NLS-1$
        //      context.setVariable(CodeTemplateContextType.PROJECTNAME, "TestProject"); //$NON-NLS-1$
        //      context.setVariable(CodeTemplateContextType.ENCLOSING_TYPE, "Face"); //$NON-NLS-1$
        //      context.setVariable(CodeTemplateContextType.ENCLOSING_METHOD, "method"); //$NON-NLS-1$
        //      context.setVariable(CodeTemplateContextType.RETURN_TYPE, "void"); //$NON-NLS-1$
        //      context.setVariable(CodeTemplateContextType.SEE_TO_OVERRIDDEN_TAG, "test.IFace#foo()"); //$NON-NLS-1$
        //
        //      return evaluateTemplate(template, context);
        //TODO
        return "Comment";
    }

    private String getMethodBody() {
        //      String templateName = CodeTemplateContextType.METHODSTUB_ID;
        //      Template template = getCodeTemplate(templateName);
        //      if (template == null)
        //         return ""; //$NON-NLS-1$
        //
        //      CodeTemplateContext context = new CodeTemplateContext(template.getContextTypeId(), null, "\n"); //$NON-NLS-1$
        //      context.setVariable(CodeTemplateContextType.ENCLOSING_METHOD, "method"); //$NON-NLS-1$
        //      context.setVariable(CodeTemplateContextType.ENCLOSING_TYPE, "Face"); //$NON-NLS-1$
        //      context.setVariable(CodeTemplateContextType.BODY_STATEMENT, ""); //$NON-NLS-1$
        //      return evaluateTemplate(template, context);
        //TODO
        return "{}";
    }

//   private static Template getCodeTemplate(String id)
//   {
//      //TODO
//      return JavaExtension.get().getTemplateStore().getTemplates(id)[0];
//   }

    //   private String evaluateTemplate(Template template, CodeTemplateContext context)
    //   {
    //      TemplateBuffer buffer;
    //      try
    //      {
    //         buffer = context.evaluate(template);
    //      }
    //      catch (BadLocationException e)
    //      {
    //         //TODO
    //         e.printStackTrace();
    //         //JavaPlugin.log(e);
    //         return ""; //$NON-NLS-1$
    //      }
    //      catch (TemplateException e)
    //      {
    //         //TODO
    //         e.printStackTrace();
    ////         JavaPlugin.log(e);
    //         return ""; //$NON-NLS-1$
    //      }
    //      if (buffer == null)
    //         return ""; //$NON-NLS-1$
    //
    //      return buffer.getString();
    //   }

    private String indent(String code, String indent) {
        if (code.length() == 0)
            return code;

        StringBuffer buf = new StringBuffer();
        buf.append(indent);
        char[] codeArray = code.toCharArray();
        for (int i = 0; i < codeArray.length; i++) {
            buf.append(codeArray[i]);
            if (codeArray[i] == '\n')
                buf.append(indent);
        }
        buf.append("\n"); //$NON-NLS-1$

        return buf.toString();
    }

}
