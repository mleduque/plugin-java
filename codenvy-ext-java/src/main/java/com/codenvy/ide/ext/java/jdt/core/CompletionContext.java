/*******************************************************************************
 * Copyright (c) 2005, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.codenvy.ide.ext.java.jdt.core;

/**
 * Completion context.
 * <p/>
 * Represent the context in which the completion occurs.
 *
 * @noinstantiate This class is not intended to be instantiated by clients.
 * @see CompletionRequestor#acceptContext(CompletionContext)
 * @since 3.1
 */
public class CompletionContext {

    /**
     * The completed token is the first token of a member declaration.<br>
     * e.g.
     * <p/>
     * <pre>
     * public class X {
     *   Foo| // completion occurs at |
     * }
     * </pre>
     *
     * @see #getTokenLocation()
     * @since 3.4
     */
    public static final int TL_MEMBER_START = 1;

    /**
     * The completed token is the first token of a statement.<br>
     * e.g.
     * <p/>
     * <pre>
     * public class X
     * {
     *    public void bar() {
     *     Foo| // completion occurs at |
     *   }
     * }
     * </pre>
     *
     * @see #getTokenLocation()
     * @since 3.4
     */
    public static final int TL_STATEMENT_START = 2;

    /**
     * The completion token is unknown.
     *
     * @since 3.2
     */
    public static final int TOKEN_KIND_UNKNOWN = 0;

    /**
     * The completion token is a name.
     *
     * @since 3.2
     */
    public static final int TOKEN_KIND_NAME = 1;

    /**
     * The completion token is a string literal. The string literal ends quote can be not present the source. <code>"foo"</code> or
     * <code>"foo</code>.
     *
     * @since 3.2
     */

    public static final int TOKEN_KIND_STRING_LITERAL = 2;

    /**
     * Tell user whether completion takes place in a javadoc comment or not.
     *
     * @return boolean true if completion takes place in a javadoc comment, false otherwise.
     * @since 3.2
     */
    public boolean isInJavadoc() {
        return false; // default overridden by concrete implementation
    }

    /**
     * Tell user whether completion takes place in text area of a javadoc comment or not.
     *
     * @return boolean true if completion takes place in a text area of a javadoc comment, false otherwise.
     * @since 3.2
     */
    public boolean isInJavadocText() {
        return false; // default overridden by concrete implementation
    }

    /**
     * Tell user whether completion takes place in a formal reference of a javadoc tag or not. Tags with formal reference are:
     * <ul>
     * <li>&#64;see</li>
     * <li>&#64;throws</li>
     * <li>&#64;exception</li>
     * <li>{&#64;link Object}</li>
     * <li>{&#64;linkplain Object}</li>
     * <li>{&#64;value} when compiler compliance is set at leats to 1.5</li>
     * </ul>
     *
     * @return boolean true if completion takes place in formal reference of a javadoc tag, false otherwise.
     * @since 3.2
     */
    public boolean isInJavadocFormalReference() {
        return false; // default overridden by concrete implementation
    }

    /**
     * Returns whether this completion context is an extended context. Some methods of this context can be used only if this
     * context is an extended context but an extended context consumes more memory.
     *
     * @return <code>true</code> if this completion context is an extended context.
     * @since 3.4
     */
    public boolean isExtended() {
        return false; // default overridden by concrete implementation
    }

    /**
     * Return signatures of expected types of a potential completion proposal at the completion position.
     * <p/>
     * It's not mandatory to a completion proposal to respect this expectation.
     *
     * @return signatures expected types of a potential completion proposal at the completion position or <code>null</code> if
     *         there is no expected types.
     * @see Signature
     */
    public char[][] getExpectedTypesSignatures() {
        return null; // default overridden by concrete implementation
    }

    /**
     * Return keys of expected types of a potential completion proposal at the completion position.
     * <p/>
     * It's not mandatory to a completion proposal to respect this expectation.
     *
     * @return keys of expected types of a potential completion proposal at the completion position or <code>null</code> if there
     *         is no expected types.
     * @see org.eclipse.jdt.core.dom.ASTParser#createASTs(ICompilationUnit[], String[], org.eclipse.jdt.core.dom.ASTRequestor,
     *      org.eclipse.core.runtime.IProgressMonitor)
     */
    public char[][] getExpectedTypesKeys() {
        return null; // default overridden by concrete implementation
    }

    /**
     * Returns the completed token. This token is either the identifier or Java language keyword or the string literal under,
     * immediately preceding, the original request offset. If the original request offset is not within or immediately after an
     * identifier or keyword or a string literal then the returned value is <code>null</code>.
     *
     * @return completed token or <code>null</code>
     * @since 3.2
     */
    public char[] getToken() {
        return null; // default overridden by concrete implementation
    }

    /**
     * Returns the kind of completion token being proposed.
     * <p>
     * The set of different kinds of completion token is expected to change over time. It is strongly recommended that clients do
     * <b>not</b> assume that the kind is one of the ones they know about, and code defensively for the possibility of unexpected
     * future growth.
     * </p>
     *
     * @return the kind; one of the kind constants declared on this class whose name starts with <code>TOKEN_KIND</code>, or
     *         possibly a kind unknown to the caller
     * @since 3.2
     */
    public int getTokenKind() {
        return -1; // default overridden by concrete implementation
    }

    /**
     * Returns the location of completion token being proposed. The returned location is a bit mask which can contain some values
     * of the constants declared on this class whose name starts with <code>TL</code>, or possibly values unknown to the caller.
     * <p/>
     * <p>
     * The set of different location values is expected to change over time. It is strongly recommended that clients do <b>not</b>
     * assume that the location contains only known value, and code defensively for the possibility of unexpected future growth.
     * </p>
     *
     * @return the location
     * @since 3.4
     */
    public int getTokenLocation() {
        return -1; // default overridden by concrete implementation
    }

    /**
     * Returns the character index of the start of the subrange in the source file buffer containing the relevant token being
     * completed. This token is either the identifier or Java language keyword under, or immediately preceding, the original
     * request offset. If the original request offset is not within or immediately after an identifier or keyword, then the
     * position returned is original request offset and the token range is empty.
     *
     * @return character index of token start position (inclusive)
     * @since 3.2
     */
    public int getTokenStart() {
        return -1; // default overridden by concrete implementation
    }

    /**
     * Returns the character index of the end (exclusive) of the subrange in the source file buffer containing the relevant token.
     * When there is no relevant token, the range is empty ( <code>getTokenEnd() == getTokenStart() - 1</code>).
     *
     * @return character index of token end position (exclusive)
     * @since 3.2
     */
    // TODO (david) https://bugs.eclipse.org/bugs/show_bug.cgi?id=132558
    public int getTokenEnd() {
        return -1; // default overridden by concrete implementation
    }

    /**
     * Returns the offset position in the source file buffer after which code assist is requested.
     *
     * @return offset position in the source file buffer
     * @since 3.2
     */
    public int getOffset() {
        return -1; // default overridden by concrete implementation
    }

}
