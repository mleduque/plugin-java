/*******************************************************************************
 * Copyright (c) 2005, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.codenvy.ide.ext.java.jdt.codeassistant;

import com.codenvy.ide.ext.java.jdt.Images;
import com.codenvy.ide.ext.java.jdt.codeassistant.api.JavaCompletionProposal;
import com.codenvy.ide.ext.java.jdt.codeassistant.ui.StyledString;
import com.codenvy.ide.ext.java.jdt.core.CompletionProposal;
import com.codenvy.ide.ext.java.jdt.core.IJavaElement;
import com.codenvy.ide.ext.java.jdt.core.JavaCore;
import com.codenvy.ide.ext.java.jdt.core.compiler.CharOperation;
import com.codenvy.ide.ext.java.jdt.core.dom.CompilationUnit;
import com.codenvy.ide.ext.java.jdt.text.DefaultPositionUpdater;
import com.codenvy.ide.ext.java.jdt.text.Document;
import com.codenvy.ide.ext.java.jdt.text.PositionUpdater;
import com.codenvy.ide.runtime.Assert;
import com.codenvy.ide.api.text.BadLocationException;
import com.codenvy.ide.api.text.BadPositionCategoryException;
import com.codenvy.ide.api.text.Position;
import com.codenvy.ide.api.text.Region;
import com.codenvy.ide.api.text.RegionImpl;


/** @since 3.2 */
public abstract class AbstractJavaCompletionProposal implements JavaCompletionProposal {

    /** A class to simplify tracking a reference position in a document. */
    static final class ReferenceTracker {

        /** The reference position category name. */
        private static final String CATEGORY = "reference_position"; //$NON-NLS-1$

        /** The position updater of the reference position. */
        private final PositionUpdater fPositionUpdater = new DefaultPositionUpdater(CATEGORY);

        /** The reference position. */
        private final Position fPosition = new Position(0);

        /**
         * Called before document changes occur. It must be followed by a call to postReplace().
         *
         * @param document
         *         the document on which to track the reference position.
         * @param offset
         *         the offset
         * @throws BadLocationException
         *         if the offset describes an invalid range in this document
         */
        public void preReplace(Document document, int offset) throws BadLocationException {
            fPosition.setOffset(offset);
            try {
                document.addPositionCategory(CATEGORY);
                document.addPositionUpdater(fPositionUpdater);
                document.addPosition(CATEGORY, fPosition);

            } catch (BadPositionCategoryException e) {
                // should not happen
//                Log.error(getClass(), e);
                //TODO log error
            }
        }

        /**
         * Called after the document changed occurred. It must be preceded by a call to preReplace().
         *
         * @param document
         *         the document on which to track the reference position.
         * @return offset after the replace
         */
        public int postReplace(Document document) {
            try {
                document.removePosition(CATEGORY, fPosition);
                document.removePositionUpdater(fPositionUpdater);
                document.removePositionCategory(CATEGORY);

            } catch (BadPositionCategoryException e) {
                // should not happen
//                Log.error(getClass(), e);
                //TODO log error
            }
            return fPosition.getOffset();
        }
    }

    // protected static final class ExitPolicy implements IExitPolicy {
    //
    // final char fExitCharacter;
    // private final IDocument fDocument;
    //
    // public ExitPolicy(char exitCharacter, IDocument document) {
    // fExitCharacter= exitCharacter;
    // fDocument= document;
    // }
    //
    // /*
    // * @see
    // org.eclipse.jdt.internal.ui.text.link.LinkedPositionUI.ExitPolicy#doExit(org.eclipse.jdt.internal.ui.text.link.LinkedPositionManager,
    // org.eclipse.swt.events.VerifyEvent, int, int)
    // */
    // public ExitFlags doExit(LinkedModeModel environment, VerifyEvent event, int offset, int length) {
    //
    // if (event.character == fExitCharacter) {
    // if (environment.anyPositionContains(offset))
    // return new ExitFlags(ILinkedModeListener.UPDATE_CARET, false);
    // else
    // return new ExitFlags(ILinkedModeListener.UPDATE_CARET, true);
    // }
    //
    // switch (event.character) {
    // case ';':
    // return new ExitFlags(ILinkedModeListener.NONE, true);
    // case SWT.CR:
    // // when entering an anonymous class as a parameter, we don't want
    // // to jump after the parenthesis when return is pressed
    // if (offset > 0) {
    // try {
    // if (fDocument.getChar(offset - 1) == '{')
    // return new ExitFlags(ILinkedModeListener.EXIT_ALL, true);
    // } catch (BadLocationException e) {
    // }
    // }
    // return null;
    // default:
    // return null;
    // }
    // }
    //
    // }

    private StyledString fDisplayString;

    private String fReplacementString;

    private int fReplacementOffset;

    private int fReplacementLength;

    private int fCursorPosition;

    private Images fImage;

//   private ContextInformation fContextInformation;

    private ProposalInfo fProposalInfo;

    private char[] fTriggerCharacters;

    private String fSortString;

    private int fRelevance;

    private boolean fIsInJavadoc;

    private CompilationUnit compilationUnit;

    private boolean fToggleEating;

    /** The invocation context of this completion proposal. Can be <code>null</code>. */
    protected final JavaContentAssistInvocationContext fInvocationContext;

    /** Cache to store last validation state. */
    private boolean fIsValidated = true;

    protected AbstractJavaCompletionProposal() {
        fInvocationContext = null;
    }

    protected AbstractJavaCompletionProposal(JavaContentAssistInvocationContext context) {
        fInvocationContext = context;
        compilationUnit = context.getCompilationUnit();
        // TODO set configurable
        fToggleEating = true;
    }

    /*
     * @see ICompletionProposalExtension#getTriggerCharacters()
     */
    public char[] getTriggerCharacters() {
        return fTriggerCharacters;
    }

    /**
     * Sets the trigger characters.
     *
     * @param triggerCharacters
     *         The set of characters which can trigger the application of this completion proposal
     */
    public void setTriggerCharacters(char[] triggerCharacters) {
        fTriggerCharacters = triggerCharacters;
    }

    /**
     * Sets the proposal info.
     *
     * @param proposalInfo
     *         The additional information associated with this proposal or <code>null</code>
     */
    public void setProposalInfo(ProposalInfo proposalInfo) {
        fProposalInfo = proposalInfo;
    }

    /**
     * Returns the additional proposal info, or <code>null</code> if none exists.
     *
     * @return the additional proposal info, or <code>null</code> if none exists
     */
    protected ProposalInfo getProposalInfo() {
        return fProposalInfo;
    }

    /**
     * Sets the cursor position relative to the insertion offset. By default this is the length of the completion string (Cursor
     * positioned after the completion)
     *
     * @param cursorPosition
     *         The cursorPosition to set
     */
    public void setCursorPosition(int cursorPosition) {
        Assert.isTrue(cursorPosition >= 0);
        fCursorPosition = cursorPosition;
    }

    public int getCursorPosition() {
        return fCursorPosition;
    }

    /** {@inheritDoc} */
    public final void apply(Document document) {
        // not used any longer
        apply(document, (char)0, getReplacementOffset() + getReplacementLength());
    }

    /** {@inheritDoc} */
    public void apply(Document document, char trigger, int offset) {
        int newLength = fInvocationContext.getInvocationOffset() - getReplacementOffset();
        if ((insertCompletion() ^ fToggleEating) && newLength >= 0)
            setReplacementLength(newLength);

        if (isSupportingRequiredProposals()) {
            CompletionProposal coreProposal = ((MemberProposalInfo)getProposalInfo()).fProposal;
            CompletionProposal[] requiredProposals = coreProposal.getRequiredProposals();
            for (int i = 0; requiredProposals != null && i < requiredProposals.length; i++) {
                int oldLen = document.getLength();
                if (requiredProposals[i].getKind() == CompletionProposal.TYPE_REF) {
                    LazyJavaCompletionProposal proposal =
                            createRequiredTypeCompletionProposal(requiredProposals[i], fInvocationContext);
                    proposal.apply(document);
                    setReplacementOffset(getReplacementOffset() + document.getLength() - oldLen);
                } else if (requiredProposals[i].getKind() == CompletionProposal.TYPE_IMPORT) {
                    ImportCompletionProposal proposal =
                            new ImportCompletionProposal(document, compilationUnit, requiredProposals[i], fInvocationContext,
                                                         coreProposal.getKind());
                    proposal.setReplacementOffset(getReplacementOffset());
                    proposal.apply(document);
                    setReplacementOffset(getReplacementOffset() + document.getLength() - oldLen);
                } else if (requiredProposals[i].getKind() == CompletionProposal.METHOD_IMPORT) {
                    ImportCompletionProposal proposal =
                            new ImportCompletionProposal(document, compilationUnit, requiredProposals[i], fInvocationContext,
                                                         coreProposal.getKind());
                    proposal.setReplacementOffset(getReplacementOffset());
                    proposal.apply(document);
                    setReplacementOffset(getReplacementOffset() + document.getLength() - oldLen);
                } else if (requiredProposals[i].getKind() == CompletionProposal.FIELD_IMPORT) {
                    ImportCompletionProposal proposal =
                            new ImportCompletionProposal(document, compilationUnit, requiredProposals[i], fInvocationContext,
                                                         coreProposal.getKind());
                    proposal.setReplacementOffset(getReplacementOffset());
                    proposal.apply(document);
                    setReplacementOffset(getReplacementOffset() + document.getLength() - oldLen);
                } else {
               /*
                * In 3.3 we only support the above required proposals, see CompletionProposal#getRequiredProposals()
                */
                    Assert.isTrue(false);
                }
            }
        }

        try {
            boolean isSmartTrigger = isSmartTrigger(trigger);

            String replacement;
            if (isSmartTrigger || trigger == (char)0) {
                replacement = getReplacementString();
            } else {
                StringBuffer buffer = new StringBuffer(getReplacementString());

                // fix for PR #5533. Assumes that no eating takes place.
                if ((getCursorPosition() > 0 && getCursorPosition() <= buffer.length() && buffer
                                                                                                  .charAt(getCursorPosition() - 1) !=
                                                                                          trigger)) {
                    buffer.insert(getCursorPosition(), trigger);
                    setCursorPosition(getCursorPosition() + 1);
                }

                replacement = buffer.toString();
                setReplacementString(replacement);
            }

            // reference position just at the end of the document change.
            int referenceOffset = getReplacementOffset() + getReplacementLength();
            final ReferenceTracker referenceTracker = new ReferenceTracker();
            referenceTracker.preReplace(document, referenceOffset);

            replace(document, getReplacementOffset(), getReplacementLength(), replacement);

            referenceOffset = referenceTracker.postReplace(document);
            setReplacementOffset(referenceOffset - (replacement == null ? 0 : replacement.length()));

            // // PR 47097
            // if (isSmartTrigger)
            // handleSmartTrigger(document, trigger, referenceOffset);

        } catch (BadLocationException x) {
            // ignore
        }
    }

    /**
     * Creates the required type proposal.
     *
     * @param completionProposal
     *         the core completion proposal
     * @param invocationContext
     *         invocation context
     * @return the required type completion proposal
     * @since 3.5
     */
    protected LazyJavaCompletionProposal createRequiredTypeCompletionProposal(CompletionProposal completionProposal,
                                                                              JavaContentAssistInvocationContext invocationContext) {
        // if (PreferenceConstants.getPreferenceStore().getBoolean(PreferenceConstants.CODEASSIST_FILL_ARGUMENT_NAMES))
        return (LazyJavaCompletionProposal)new FillArgumentNamesCompletionProposalCollector(invocationContext)
                .createJavaCompletionProposal(completionProposal);
        // else
        // return new LazyJavaTypeCompletionProposal(completionProposal, invocationContext);
    }

    private boolean isSmartTrigger(char trigger) {
        // return trigger == ';' &&
        // JavaPlugin.getDefault().getCombinedPreferenceStore().getBoolean(PreferenceConstants.EDITOR_SMART_SEMICOLON)
        // || trigger == '{' &&
        // JavaPlugin.getDefault().getCombinedPreferenceStore().getBoolean(PreferenceConstants.EDITOR_SMART_OPENING_BRACE);
        // TODO
        return false;
    }

    // private void handleSmartTrigger(IDocument document, char trigger, int referenceOffset) throws BadLocationException {
    // DocumentCommand cmd= new DocumentCommand() {
    // };
    //
    // cmd.offset= referenceOffset;
    // cmd.length= 0;
    // cmd.text= Character.toString(trigger);
    // cmd.doit= true;
    // cmd.shiftsCaret= true;
    // cmd.caretOffset= getReplacementOffset() + getCursorPosition();
    //
    // SmartSemicolonAutoEditStrategy strategy= new SmartSemicolonAutoEditStrategy(IJavaPartitions.JAVA_PARTITIONING);
    // strategy.customizeDocumentCommand(document, cmd);
    //
    // replace(document, cmd.offset, cmd.length, cmd.text);
    // setCursorPosition(cmd.caretOffset - getReplacementOffset() + cmd.text.length());
    // }

    protected final void replace(Document document, int offset, int length, String string) throws BadLocationException {
        if (!document.get(offset, length).equals(string))
            document.replace(offset, length, string);
    }

    // /*
    // * @see org.eclipse.jface.text.contentassist.ICompletionProposalExtension1#apply(org.eclipse.jface.text.ITextViewer, char,
    // int, int)
    // */
    // public void apply(ITextViewer viewer, char trigger, int stateMask, int offset) {
    //
    // IDocument document= viewer.getDocument();
    // if (fTextViewer == null)
    // fTextViewer= viewer;
    //
    // // see https://bugs.eclipse.org/bugs/show_bug.cgi?id=96059
    // // don't apply the proposal if for some reason we're not valid any longer
    // if (!isInJavadoc() && !validate(document, offset, null)) {
    // setCursorPosition(offset);
    // if (trigger != '\0') {
    // try {
    // document.replace(offset, 0, String.valueOf(trigger));
    // setCursorPosition(getCursorPosition() + 1);
    // if (trigger == '(' && autocloseBrackets()) {
    //                  document.replace(getReplacementOffset() + getCursorPosition(), 0, ")"); //$NON-NLS-1$
    // setUpLinkedMode(document, ')');
    // }
    // } catch (BadLocationException x) {
    // // ignore
    // }
    // }
    // return;
    // }
    //
    // // don't eat if not in preferences, XOR with Ctrl
    // // but: if there is a selection, replace it!
    // Point selection= viewer.getSelectedRange();
    // fToggleEating= (stateMask & SWT.CTRL) != 0;
    // int newLength= selection.x + selection.y - getReplacementOffset();
    // if ((insertCompletion() ^ fToggleEating) && newLength >= 0)
    // setReplacementLength(newLength);
    //
    // apply(document, trigger, offset);
    // fToggleEating= false;
    // }

    /**
     * Tells whether the user toggled the insert mode by pressing the 'Ctrl' key.
     *
     * @return <code>true</code> if the insert mode is toggled, <code>false</code> otherwise
     * @since 3.5
     */
    protected boolean isInsertModeToggled() {
        return fToggleEating;
    }

    /**
     * Returns <code>true</code> if the proposal is within javadoc, <code>false</code> otherwise.
     *
     * @return <code>true</code> if the proposal is within javadoc, <code>false</code> otherwise
     */
    protected boolean isInJavadoc() {
        return fIsInJavadoc;
    }

    /**
     * Sets the javadoc attribute.
     *
     * @param isInJavadoc
     *         <code>true</code> if the proposal is within javadoc
     */
    protected void setInJavadoc(boolean isInJavadoc) {
        fIsInJavadoc = isInJavadoc;
    }

    /*
     * @see ICompletionProposal#getSelection
     */
    public Region getSelection(Document document) {
        if (!fIsValidated)
            return null;
        return new RegionImpl(getReplacementOffset() + getCursorPosition(), 0);
    }

//   /*
//    * @see ICompletionProposal#getContextInformation()
//    */
//   public ContextInformation getContextInformation()
//   {
//      return fContextInformation;
//   }

//   /**
//    * Sets the context information.
//    * 
//    * @param contextInformation The context information associated with this proposal
//    */
//   public void setContextInformation(ContextInformation contextInformation)
//   {
//      fContextInformation = contextInformation;
//   }

    /*
     * @see ICompletionProposal#getDisplayString()
     */
    public String getDisplayString() {
        if (fDisplayString != null)
            return fDisplayString.getString();
        return ""; //$NON-NLS-1$
    }

//    /*
//     * @see ICompletionProposal#getAdditionalProposalInfo()
//     */
//    public Widget getAdditionalProposalInfo() {
//        if (getProposalInfo() != null) {
//            return getProposalInfo().getInfo();
//        }
//        return null;
//    }

    /*
     * @see ICompletionProposalExtension#getContextInformationPosition()
     */
    public int getContextInformationPosition() {
//      if (getContextInformation() == null)
//         return getReplacementOffset() - 1;
        return getReplacementOffset() + getCursorPosition();
    }

    /**
     * Gets the replacement offset.
     *
     * @return Returns a int
     */
    public int getReplacementOffset() {
        return fReplacementOffset;
    }

    /**
     * Sets the replacement offset.
     *
     * @param replacementOffset
     *         The replacement offset to set
     */
    public void setReplacementOffset(int replacementOffset) {
        Assert.isTrue(replacementOffset >= 0);
        fReplacementOffset = replacementOffset;
    }

    /*
     * @see org.eclipse.jface.text.contentassist.ICompletionProposalExtension3#getCompletionOffset()
     */
    public int getPrefixCompletionStart(Document document, int completionOffset) {
        return getReplacementOffset();
    }

    /**
     * Gets the replacement length.
     *
     * @return Returns a int
     */
    public int getReplacementLength() {
        return fReplacementLength;
    }

    /**
     * Sets the replacement length.
     *
     * @param replacementLength
     *         The replacementLength to set
     */
    public void setReplacementLength(int replacementLength) {
        Assert.isTrue(replacementLength >= 0);
        fReplacementLength = replacementLength;
    }

    /**
     * Gets the replacement string.
     *
     * @return Returns a String
     */
    public String getReplacementString() {
        return fReplacementString;
    }

    /**
     * Sets the replacement string.
     *
     * @param replacementString
     *         The replacement string to set
     */
    public void setReplacementString(String replacementString) {
        Assert.isNotNull(replacementString);
        fReplacementString = replacementString;
    }

    /*
     * @see org.eclipse.jface.text.contentassist.ICompletionProposalExtension3#getReplacementText()
     */
    public CharSequence getPrefixCompletionText(Document document, int completionOffset) {
        if (!isCamelCaseMatching())
            return getReplacementString();

        String prefix = getPrefix(document, completionOffset);
        return getCamelCaseCompound(prefix, getReplacementString());
    }

    /*
     * @see ICompletionProposal#getImage()
     */
    public Images getImage() {
        return fImage;
    }

    /**
     * Sets the image.
     *
     * @param image
     *         The image to set
     */
    public void setImage(Images image) {
        fImage = image;
    }

    /*
     * @see ICompletionProposalExtension#isValidFor(IDocument, int)
     */
    public boolean isValidFor(Document document, int offset) {
        // return validate(document, offset, null);
        // TODO
        return true;
    }

    // /*
    // * @see org.eclipse.jface.text.contentassist.ICompletionProposalExtension2#validate(org.eclipse.jface.text.IDocument, int,
    // org.eclipse.jface.text.DocumentEvent)
    // */
    // public boolean validate(IDocument document, int offset, DocumentEvent event) {
    //
    // if (!isOffsetValid(offset))
    // return fIsValidated= false;
    //
    // fIsValidated= isValidPrefix(getPrefix(document, offset));
    //
    // if (fIsValidated && event != null) {
    // // adapt replacement range to document change
    // int delta= (event.fText == null ? 0 : event.fText.length()) - event.fLength;
    // final int newLength= Math.max(getReplacementLength() + delta, 0);
    // setReplacementLength(newLength);
    // }
    //
    // return fIsValidated;
    // }

    /**
     * Checks whether the given offset is valid for this proposal.
     *
     * @param offset
     *         the caret offset
     * @return <code>true</code> if the offset is valid for this proposal
     * @since 3.5
     */
    protected boolean isOffsetValid(int offset) {
        return getReplacementOffset() <= offset;
    }

    // /**
    // * Checks whether <code>prefix</code> is a valid prefix for this proposal. Usually, while code
    // * completion is in progress, the user types and edits the prefix in the document in order to
    // * filter the proposal list. From {@link #validate(IDocument, int, DocumentEvent) }, the
    // * current prefix in the document is extracted and this method is called to find out whether the
    // * proposal is still valid.
    // * <p>
    // * The default implementation checks if <code>prefix</code> is a prefix of the proposal's
    // * {@link #getDisplayString() display string} using the {@link #isPrefix(String, String) }
    // * method.
    // * </p>
    // *
    // * @param prefix the current prefix in the document
    // * @return <code>true</code> if <code>prefix</code> is a valid prefix of this proposal
    // */
    // protected boolean isValidPrefix(String prefix) {
    // /*
    // * See http://dev.eclipse.org/bugs/show_bug.cgi?id=17667
    // * why we do not use the replacement string.
    // * String word= fReplacementString;
    // *
    // * Besides that bug we also use the display string
    // * for performance reasons, as computing the
    // * replacement string can be expensive.
    // */
    // return isPrefix(prefix, TextProcessor.deprocess(getDisplayString()));
    // }

    /**
     * Gets the proposal's relevance.
     *
     * @return Returns a int
     */
    public int getRelevance() {
        return fRelevance;
    }

    /**
     * Sets the proposal's relevance.
     *
     * @param relevance
     *         The relevance to set
     */
    public void setRelevance(int relevance) {
        fRelevance = relevance;
    }

    /**
     * Returns the text in <code>document</code> from {@link #getReplacementOffset()} to <code>offset</code>. Returns the empty
     * string if <code>offset</code> is before the replacement offset or if an exception occurs when accessing the document.
     *
     * @param document
     *         the document
     * @param offset
     *         the offset
     * @return the prefix
     */
    protected String getPrefix(Document document, int offset) {
        try {
            int length = offset - getReplacementOffset();
            if (length > 0)
                return document.get(getReplacementOffset(), length);
        } catch (BadLocationException x) {
        }
        return ""; //$NON-NLS-1$
    }

    /**
     * Case insensitive comparison of <code>prefix</code> with the start of <code>string</code>.
     *
     * @param prefix
     *         the prefix
     * @param string
     *         the string to look for the prefix
     * @return <code>true</code> if the string begins with the given prefix and <code>false</code> if <code>prefix</code> is longer
     *         than <code>string</code> or the string doesn't start with the given prefix
     * @since 3.2
     */
    protected boolean isPrefix(String prefix, String string) {
        if (prefix == null || string == null || prefix.length() > string.length())
            return false;
        String start = string.substring(0, prefix.length());
        return start.equalsIgnoreCase(prefix) || isCamelCaseMatching()
                                                 && CharOperation.camelCaseMatch(prefix.toCharArray(), string.toCharArray());
    }

    /**
     * Matches <code>prefix</code> against <code>string</code> and replaces the matched region by prefix. Case is preserved as much
     * as possible. This method returns <code>string</code> if camel case completion is disabled. Examples when camel case
     * completion is enabled:
     * <ul>
     * <li>getCamelCompound("NuPo", "NullPointerException") -> "NuPointerException"</li>
     * <li>getCamelCompound("NuPoE", "NullPointerException") -> "NuPoException"</li>
     * <li>getCamelCompound("hasCod", "hashCode") -> "hasCode"</li>
     * </ul>
     *
     * @param prefix
     *         the prefix to match against
     * @param string
     *         the string to match
     * @return a compound of prefix and any postfix taken from <code>string</code>
     * @since 3.2
     */
    protected final String getCamelCaseCompound(String prefix, String string) {
        if (prefix.length() > string.length())
            return string;

        // a normal prefix - no camel case logic at all
        String start = string.substring(0, prefix.length());
        if (start.equalsIgnoreCase(prefix))
            return string;

        final char[] patternChars = prefix.toCharArray();
        final char[] stringChars = string.toCharArray();

        for (int i = 1; i <= stringChars.length; i++)
            if (CharOperation.camelCaseMatch(patternChars, 0, patternChars.length, stringChars, 0, i))
                return prefix + string.substring(i);

        // Not a camel case match at all.
        // This should not happen -> stay with the default behavior
        return string;
    }

    /**
     * Returns true if camel case matching is enabled.
     *
     * @return <code>true</code> if camel case matching is enabled
     * @since 3.2
     */
    protected boolean isCamelCaseMatching() {
        String value = JavaCore.getOption(JavaCore.CODEASSIST_CAMEL_CASE_MATCH);
        return JavaCore.ENABLED.equals(value);
    }

    protected static boolean insertCompletion() {
        // IPreferenceStore preference= JavaPlugin.getDefault().getPreferenceStore();
        // return preference.getBoolean(PreferenceConstants.CODEASSIST_INSERT_COMPLETION);
        return true;
    }

    // private static Color getForegroundColor() {
    // IPreferenceStore preference= JavaPlugin.getDefault().getPreferenceStore();
    // RGB rgb= PreferenceConverter.getColor(preference, PreferenceConstants.CODEASSIST_REPLACEMENT_FOREGROUND);
    // JavaTextTools textTools= JavaPlugin.getDefault().getJavaTextTools();
    // return textTools.getColorManager().getColor(rgb);
    // }
    //
    // private static Color getBackgroundColor() {
    // IPreferenceStore preference= JavaPlugin.getDefault().getPreferenceStore();
    // RGB rgb= PreferenceConverter.getColor(preference, PreferenceConstants.CODEASSIST_REPLACEMENT_BACKGROUND);
    // JavaTextTools textTools= JavaPlugin.getDefault().getJavaTextTools();
    // return textTools.getColorManager().getColor(rgb);
    // }

    // private void repairPresentation(ITextViewer viewer) {
    // if (fRememberedStyleRange != null) {
    // if (viewer instanceof ITextViewerExtension2) {
    // // attempts to reduce the redraw area
    // ITextViewerExtension2 viewer2= (ITextViewerExtension2)viewer;
    // viewer2.invalidateTextPresentation(fRememberedStyleRange.start, fRememberedStyleRange.length);
    // } else
    // viewer.invalidateTextPresentation();
    // }
    // }

    // private void updateStyle(ITextViewer viewer) {
    // StyledText text= viewer.getTextWidget();
    // int widgetOffset= getWidgetOffset(viewer, fRememberedStyleRange.start);
    // StyleRange range= new StyleRange(fRememberedStyleRange);
    // range.start= widgetOffset;
    // range.length= fRememberedStyleRange.length;
    // StyleRange currentRange= text.getStyleRangeAtOffset(widgetOffset);
    // if (currentRange != null) {
    // range.strikeout= currentRange.strikeout;
    // range.underline= currentRange.underline;
    // range.fontStyle= currentRange.fontStyle;
    // }
    //
    // // http://dev.eclipse.org/bugs/show_bug.cgi?id=34754
    // try {
    // text.setStyleRange(range);
    // } catch (IllegalArgumentException x) {
    // // catching exception as offset + length might be outside of the text widget
    // fRememberedStyleRange= null;
    // }
    // }

    // /**
    // * Convert a document offset to the corresponding widget offset.
    // *
    // * @param viewer the text viewer
    // * @param documentOffset the document offset
    // * @return widget offset
    // * @since 3.6
    // */
    // private int getWidgetOffset(ITextViewer viewer, int documentOffset) {
    // if (viewer instanceof ITextViewerExtension5) {
    // ITextViewerExtension5 extension= (ITextViewerExtension5)viewer;
    // return extension.modelOffset2WidgetOffset(documentOffset);
    // }
    // IRegion visible= viewer.getVisibleRegion();
    // int widgetOffset= documentOffset - visible.getOffset();
    // if (widgetOffset > visible.getLength()) {
    // return -1;
    // }
    // return widgetOffset;
    // }

    // /**
    // * Creates a style range for the text viewer.
    // *
    // * @param viewer the text viewer
    // * @return the new style range for the text viewer or <code>null</code>
    // * @since 3.6
    // */
    // private StyleRange createStyleRange(ITextViewer viewer) {
    // StyledText text= viewer.getTextWidget();
    // if (text == null || text.isDisposed())
    // return null;
    //
    // int widgetCaret= text.getCaretOffset();
    //
    // int modelCaret= 0;
    // if (viewer instanceof ITextViewerExtension5) {
    // ITextViewerExtension5 extension= (ITextViewerExtension5) viewer;
    // modelCaret= extension.widgetOffset2ModelOffset(widgetCaret);
    // } else {
    // IRegion visibleRegion= viewer.getVisibleRegion();
    // modelCaret= widgetCaret + visibleRegion.getOffset();
    // }
    //
    // if (modelCaret >= getReplacementOffset() + getReplacementLength())
    // return null;
    //
    // int length= getReplacementOffset() + getReplacementLength() - modelCaret;
    //
    // Color foreground= getForegroundColor();
    // Color background= getBackgroundColor();
    //
    // return new StyleRange(modelCaret, length, foreground, background);
    // }

    // /*
    // * @see org.eclipse.jface.text.contentassist.ICompletionProposalExtension2#selected(ITextViewer, boolean)
    // */
    // public void selected(final ITextViewer viewer, boolean smartToggle) {
    // repairPresentation(viewer);
    // fRememberedStyleRange= null;
    //
    // if (!insertCompletion() ^ smartToggle) {
    // StyleRange range= createStyleRange(viewer);
    // if (range == null)
    // return;
    //
    // fRememberedStyleRange= range;
    //
    // if (viewer instanceof ITextViewerExtension4) {
    // if (fTextPresentationListener == null) {
    // fTextPresentationListener= new ITextPresentationListener() {
    // /* (non-Javadoc)
    // * @see org.eclipse.jface.text.ITextPresentationListener#applyTextPresentation(org.eclipse.jface.text.TextPresentation)
    // */
    // public void applyTextPresentation(TextPresentation textPresentation) {
    // fRememberedStyleRange= createStyleRange(viewer);
    // if (fRememberedStyleRange != null)
    // textPresentation.mergeStyleRange(fRememberedStyleRange);
    // }
    // };
    // ((ITextViewerExtension4)viewer).addTextPresentationListener(fTextPresentationListener);
    // }
    // repairPresentation(viewer);
    // } else
    // updateStyle(viewer);
    // }
    // }

    // /*
    // * @see org.eclipse.jface.text.contentassist.ICompletionProposalExtension2#unselected(ITextViewer)
    // */
    // public void unselected(ITextViewer viewer) {
    // if (fTextPresentationListener != null) {
    // ((ITextViewerExtension4)viewer).removeTextPresentationListener(fTextPresentationListener);
    // fTextPresentationListener= null;
    // }
    // repairPresentation(viewer);
    // fRememberedStyleRange= null;
    // }

    // /*
    // * @see org.eclipse.jface.text.contentassist.ICompletionProposalExtension3#getInformationControlCreator()
    // */
    // public IInformationControlCreator getInformationControlCreator() {
    // Shell shell= JavaPlugin.getActiveWorkbenchShell();
    // if (shell == null || !BrowserInformationControl.isAvailable(shell))
    // return null;
    //
    // if (fCreator == null) {
    // /*
    // * FIXME: Take control creators (and link handling) out of JavadocHover,
    // * see https://bugs.eclipse.org/bugs/show_bug.cgi?id=232024
    // */
    // JavadocHover.PresenterControlCreator presenterControlCreator= new JavadocHover.PresenterControlCreator(getSite());
    // fCreator= new JavadocHover.HoverControlCreator(presenterControlCreator, true);
    // }
    // return fCreator;
    // }
    //
    // private IWorkbenchSite getSite() {
    // IWorkbenchPage page= JavaPlugin.getActivePage();
    // if (page != null) {
    // IWorkbenchPart part= page.getActivePart();
    // if (part != null)
    // return part.getSite();
    // }
    // return null;
    // }

    public String getSortString() {
        return fSortString;
    }

    protected void setSortString(String string) {
        fSortString = string;
    }

    // protected ITextViewer getTextViewer() {
    // return fTextViewer;
    // }

    protected boolean isToggleEating() {
        return fToggleEating;
    }

    /**
     * Sets up a simple linked mode at {@link #getCursorPosition()} and an exit policy that will exit the mode when
     * <code>closingCharacter</code> is typed and an exit position at <code>getCursorPosition() + 1</code>.
     *
     * @param document
     *         the document
     * @param closingCharacter
     *         the exit character
     */
    protected void setUpLinkedMode(Document document, char closingCharacter) {
        // TODO
        // if (getTextViewer() != null && autocloseBrackets()) {
        // int offset= getReplacementOffset() + getCursorPosition();
        // int exit= getReplacementOffset() + getReplacementString().length();
        // try {
        // LinkedPositionGroup group= new LinkedPositionGroup();
        // group.addPosition(new LinkedPosition(document, offset, 0, LinkedPositionGroup.NO_STOP));
        //
        // LinkedModeModel model= new LinkedModeModel();
        // model.addGroup(group);
        // model.forceInstall();
        //
        // LinkedModeUI ui= new EditorLinkedModeUI(model, getTextViewer());
        // ui.setSimpleMode(true);
        // ui.setExitPolicy(new ExitPolicy(closingCharacter, document));
        // ui.setExitPosition(getTextViewer(), exit, 0, Integer.MAX_VALUE);
        // ui.setCyclingMode(LinkedModeUI.CYCLE_NEVER);
        // ui.enter();
        // } catch (BadLocationException x) {
        // JavaPlugin.log(x);
        // }
        // }
    }

    protected boolean autocloseBrackets() {
        // IPreferenceStore preferenceStore= JavaPlugin.getDefault().getPreferenceStore();
        // return preferenceStore.getBoolean(PreferenceConstants.EDITOR_CLOSE_BRACKETS);
        return true;
    }

    protected void setDisplayString(String string) {
        fDisplayString = new StyledString(string);
    }

    /*
     * @see org.eclipse.jface.text.contentassist.ICompletionProposalExtension6#getStyledDisplayString()
     * @since 3.4
     */
    public StyledString getStyledDisplayString() {
        return fDisplayString;
    }

    public void setStyledDisplayString(StyledString text) {
        fDisplayString = text;
    }

    /*
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return getDisplayString();
    }

    /**
     * Returns the java element proposed by the receiver, possibly <code>null</code>.
     *
     * @return the java element proposed by the receiver, possibly <code>null</code>
     */
    public IJavaElement getJavaElement() {
        if (getProposalInfo() != null)
            return getProposalInfo().getJavaElement();
        return null;
    }

    /**
     * Tells whether required proposals are supported by this proposal.
     *
     * @return <code>true</code> if required proposals are supported by this proposal
     * @see CompletionProposal#getRequiredProposals()
     * @since 3.3
     */
    protected boolean isSupportingRequiredProposals() {
        if (fInvocationContext == null)
            return false;

        ProposalInfo proposalInfo = getProposalInfo();
        if (!(proposalInfo instanceof MemberProposalInfo || proposalInfo instanceof AnonymousTypeProposalInfo))
            return false;

        CompletionProposal proposal = ((MemberProposalInfo)proposalInfo).fProposal;
        return proposal != null
               && (proposal.getKind() == CompletionProposal.METHOD_REF //
                   || proposal.getKind() == CompletionProposal.FIELD_REF //
                   || proposal.getKind() == CompletionProposal.TYPE_REF //
                   || proposal.getKind() == CompletionProposal.CONSTRUCTOR_INVOCATION //
                   || proposal.getKind() == CompletionProposal.ANONYMOUS_CLASS_CONSTRUCTOR_INVOCATION);
    }

}
