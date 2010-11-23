package com.jetbrains.python.codeInsight.intentions;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.TextComponentAccessor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import com.jetbrains.python.PyBundle;
import com.jetbrains.python.PyTokenSeparatorGenerator;
import com.jetbrains.python.PyTokenTypes;
import com.jetbrains.python.psi.*;
import org.jetbrains.annotations.NotNull;
import sun.tools.tree.Statement;

import javax.swing.*;
import java.util.List;

/**
 * User: catherine
 */
public class ReplaceListComprehensionWithForIntention implements IntentionAction {
  @NotNull
  public String getText() {
    return PyBundle.message("INTN.replace.list.comprehensions.with.for");
  }

  @NotNull
  public String getFamilyName() {
    return PyBundle.message("INTN.replace.list.comprehensions.with.for");
  }

  public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
    PyListCompExpression expression =
      PsiTreeUtil.getTopmostParentOfType(file.findElementAt(editor.getCaretModel().getOffset()), PyListCompExpression.class);
    if (expression == null) {
      return false;
    }
    
    PsiElement parent = expression.getParent();
    if (parent instanceof PyAssignmentStatement || parent instanceof PyPrintStatement) {
      return true;
    }
    return false;
  }

  public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
    PyListCompExpression expression =
      PsiTreeUtil.getTopmostParentOfType(file.findElementAt(editor.getCaretModel().getOffset()), PyListCompExpression.class);
    if (expression == null) {
      return;
    }
    PsiElement parent = expression.getParent();
    PyElementGenerator elementGenerator = PyElementGenerator.getInstance(project);

    if (parent instanceof PyAssignmentStatement) {
      PsiElement leftExpr = ((PyAssignmentStatement)parent).getLeftHandSideExpression();
      PyAssignmentStatement initAssignment = elementGenerator.createFromText(LanguageLevel.getDefault(), PyAssignmentStatement.class,
                                                                         leftExpr.getText() + " = []");
      PsiElement lineBreak = elementGenerator.createFromText(LanguageLevel.getDefault(), PsiWhiteSpace.class, "\n");
      initAssignment.add(lineBreak);

      PyStatement result = elementGenerator.createFromText(LanguageLevel.forElement(expression), PyStatement.class,
                                                          leftExpr.getText() + ".append("+ getResult(expression).getText() +")");

      PyForStatement forStatement = createForLoop (expression, elementGenerator, result);
      initAssignment.add(forStatement);
      parent.replace(initAssignment);

    }
    else if (parent instanceof PyPrintStatement) {
      PyStatement result = elementGenerator.createFromText(LanguageLevel.forElement(expression), PyStatement.class,
                                                          "print " + "(" + getResult(expression).getText() +")");
      PyForStatement forStatement = createForLoop (expression, elementGenerator, result);
      parent.replace(forStatement);
    }
  }

  private static PyForStatement createForLoop(PyListCompExpression expression, PyElementGenerator elementGenerator, PyStatement result) {
    List <ComprhForComponent> forComps = expression.getForComponents();

    if (forComps.size() != 0) {
      ComprhForComponent forComponent = forComps.get(0);
      PyForStatement forStatement = elementGenerator.createFromText(LanguageLevel.getDefault(), PyForStatement.class,
                             "for " + forComponent.getIteratorVariable().getText()  + " in " +
                             forComponent.getIteratedList().getText() + ":\n  a+1");

      List<ComprhIfComponent> ifComps = expression.getIfComponents();
      if (ifComps.size() != 0) {
        addIfComponents(forStatement, ifComps, elementGenerator);
      }


      if (expression.getResultExpression() instanceof PyListCompExpression) {
        addForComponents(forStatement, (PyListCompExpression)expression.getResultExpression(), elementGenerator, result);
      }
      else {
        PyStatement stat = forStatement.getForPart().getStatementList().getStatements()[0];
        while (stat instanceof PyIfStatement) {
          stat = ((PyIfStatement)stat).getIfPart().getStatementList().getStatements()[0];
        }
        stat.replace(result);
      }
      return forStatement;
    }
    return null;
  }

  private static void addIfComponents(PyForStatement forStatement,
                               List <ComprhIfComponent> ifComps,
                               PyElementGenerator elementGenerator) {
    PyStatementList pyStatementList = forStatement.getForPart().getStatementList();
    for (ComprhIfComponent ifComp : ifComps) {
      PyIfStatement ifStat = elementGenerator.createFromText(LanguageLevel.getDefault(), PyIfStatement.class,
                           "if " + ifComp.getTest().getText() + ":\n  a+1");
      pyStatementList.getStatements()[0].replace(ifStat);
      pyStatementList = ((PyIfStatement)pyStatementList.getStatements()[0]).getIfPart().getStatementList();
    }
  }

  private static PyElement getResult(PyListCompExpression expression) {
    PyElement result = expression.getResultExpression();
    if (result instanceof PyListCompExpression) {
      return getResult((PyListCompExpression)result);
    }
    return result;
  }


  private static void addForComponents(PyElement statement, PyListCompExpression expression, PyElementGenerator elementGenerator, PsiElement result) {
    PyStatement pyStatement = ((PyForStatement)statement).getForPart().getStatementList().getStatements()[0];
    while (pyStatement instanceof PyIfStatement) {
      pyStatement = ((PyIfStatement)pyStatement).getIfPart().getStatementList().getStatements()[0];
    }

    List <ComprhForComponent> forComps = expression.getForComponents();
    if ( forComps.size() != 0) {
      ComprhForComponent comp = forComps.get(0);

      PyForStatement pyForStatement = elementGenerator.createFromText(LanguageLevel.getDefault(), PyForStatement.class,
                             "for " + comp.getIteratorVariable().getText()  + " in "+ comp.getIteratedList().getText() + ":\n  a+1");

      List<ComprhIfComponent> ifComps = expression.getIfComponents();
      if (ifComps.size() != 0) {
        addIfComponents(pyForStatement, ifComps, elementGenerator);
      }

      if (expression.getResultExpression() instanceof PyListCompExpression) {
        addForComponents(pyForStatement, (PyListCompExpression)expression.getResultExpression(), elementGenerator, result);
        pyStatement.replace(pyForStatement);
      }
      else {
        PyStatement stat = pyForStatement.getForPart().getStatementList().getStatements()[0];
        while (stat instanceof PyIfStatement) {
          stat = ((PyIfStatement)stat).getIfPart().getStatementList().getStatements()[0];
        }
        stat.replace(result);
      }
      pyStatement.replace(pyForStatement);
    }
  }

  public boolean startInWriteAction() {
    return true;
  }
}