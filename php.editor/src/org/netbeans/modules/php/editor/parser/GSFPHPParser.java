/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2010 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 *
 * Contributor(s):
 *
 * Portions Copyrighted 2008 Sun Microsystems, Inc.
 */
package org.netbeans.modules.php.editor.parser;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java_cup.runtime.Symbol;
import javax.swing.event.ChangeListener;
import org.netbeans.modules.csl.api.OffsetRange;
import org.netbeans.modules.csl.spi.GsfUtilities;
import org.netbeans.modules.csl.spi.ParserResult;
import org.netbeans.modules.parsing.api.Snapshot;
import org.netbeans.modules.parsing.api.Task;
import org.netbeans.modules.parsing.spi.ParseException;
import org.netbeans.modules.parsing.spi.Parser;
import org.netbeans.modules.parsing.spi.SourceModificationEvent;
import org.netbeans.modules.php.api.util.FileUtils;
import org.netbeans.modules.php.editor.parser.astnodes.ASTError;
import org.netbeans.modules.php.editor.parser.astnodes.Comment;
import org.netbeans.modules.php.editor.parser.astnodes.NamespaceDeclaration;
import org.netbeans.modules.php.editor.parser.astnodes.Program;
import org.netbeans.modules.php.editor.parser.astnodes.Statement;
import org.netbeans.modules.php.project.api.PhpLanguageProperties;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.ChangeSupport;
import org.openide.util.WeakListeners;

/**
 *
 * @author Petr Pisl
 */
public class GSFPHPParser extends Parser implements PropertyChangeListener {
    private static final Logger LOGGER = Logger.getLogger(GSFPHPParser.class.getName());
    private static final boolean PARSE_BIG_FILES = Boolean.getBoolean("nb.php.parse.big.files"); //NOI18N
    private static final int BIG_FILE_SIZE = Integer.getInteger("nb.php.big.file.size", 5000000); //NOI18N
    private static final List<String> REGISTERED_PHP_EXTENSIONS = FileUtil.getMIMETypeExtensions(FileUtils.PHP_MIME_TYPE);
    private final ChangeSupport changeSupport = new ChangeSupport(this);
    private boolean shortTags = true;
    private boolean aspTags = false;
    private ParserResult result = null;
    private boolean projectPropertiesListenerAdded = false;

    @Override
    public Result getResult(Task task) throws ParseException {
        return result;
    }

    @Override
    public void cancel(CancelReason reason, SourceModificationEvent event) {
        super.cancel(reason, event);
        LOGGER.log(Level.FINE, "ParserTask cancel: {0}", reason.name());
    }

    @Override
    public void addChangeListener(ChangeListener changeListener) {
        changeSupport.addChangeListener(changeListener);
    }

    @Override
    public void removeChangeListener(ChangeListener changeListener) {
        changeSupport.removeChangeListener(changeListener);
    }

    @Override
    public void parse(Snapshot snapshot, Task task, SourceModificationEvent event) throws ParseException {
        if (snapshot == null) {
            return;
        }

        long startTime = System.currentTimeMillis();
        FileObject fileObject = snapshot.getSource().getFileObject();
        if (!PARSE_BIG_FILES && fileIsTooBig(fileObject)) {
            createEmptyResult(snapshot);
            LOGGER.log(
                    Level.INFO,
                    "Parsing of big file cancelled. Size: {0} Name: {1}",
                    new Object[] {fileObject.getSize(), FileUtil.getFileDisplayName(fileObject)});
        } else if (!isRegisteredPhpFile(fileObject)) {
            createEmptyResult(snapshot);
            LOGGER.log(
                    Level.FINE,
                    "Skipped file extension: {0}\nRegistered extensions: {1}",
                    new Object[] {fileObject.getExt(), REGISTERED_PHP_EXTENSIONS.toString()});
        } else {
            processParsing(fileObject, snapshot, event);
        }
        long endTime = System.currentTimeMillis();
        LOGGER.log(Level.FINE, "Parsing took: {0}ms source: {1}", new Object[]{endTime - startTime, System.identityHashCode(snapshot.getSource())}); //NOI18N
    }

    private static boolean fileIsTooBig(FileObject fileObject) {
        return fileObject != null && fileObject.getSize() > BIG_FILE_SIZE;
    }

    private static boolean isRegisteredPhpFile(FileObject fileObject) {
        return fileObject == null || fileObject.getExt().isEmpty() || REGISTERED_PHP_EXTENSIONS.contains(fileObject.getExt().toLowerCase());
    }

    private void createEmptyResult(Snapshot snapshot) {
        Program emptyProgram = new Program(0, 0, Collections.<Statement>emptyList(), Collections.<Comment>emptyList());
        result = new PHPParseResult(snapshot, emptyProgram);
    }

    private void processParsing(FileObject fileObject, Snapshot snapshot, SourceModificationEvent event) {
        PhpLanguageProperties languageProperties = PhpLanguageProperties.forFileObject(fileObject);
        if (!projectPropertiesListenerAdded) {
            PropertyChangeListener weakListener = WeakListeners.propertyChange(this, languageProperties);
            languageProperties.addPropertyChangeListener(weakListener);
            projectPropertiesListenerAdded = true;
        }
        shortTags = languageProperties.areShortTagsEnabled();
        aspTags = languageProperties.areAspTagsEnabled();
        try {
            int caretOffset = GsfUtilities.getLastKnownCaretOffset(snapshot, event);
            LOGGER.log(Level.FINE, "caretOffset: {0}", caretOffset); //NOI18N
            Context context = new Context(snapshot, caretOffset);
            result = parseBuffer(context, Sanitize.NONE, null);
        } catch (Exception exception) {
            LOGGER.log(Level.FINE, "Exception during parsing: {0}", exception);
            int end = snapshot.getText().toString().length();
            ASTError error = new ASTError(0, end);
            List<Statement> statements = new ArrayList<>();
            statements.add(error);
            Program emptyProgram = new Program(0, end, statements, Collections.<Comment>emptyList());
            result = new PHPParseResult(snapshot, emptyProgram);
        }
    }

    protected PHPParseResult parseBuffer(final Context context, final Sanitize sanitizing, PHP5ErrorHandler errorHandler) throws Exception {
        boolean sanitizedSource = false;
        if (errorHandler == null) {
            errorHandler = new PHP5ErrorHandlerImpl(context);
        }
        if (!isParsingWithoutSanitization(sanitizing)) {
            // don't add syntax errors catched by sanitizers to the error handler...that errors are not relevant for the user
            errorHandler.disableHandling();
            boolean ok = sanitizeSource(context, sanitizing, errorHandler);
            if (ok) {
                assert context.getSanitizedPart() != null;
                sanitizedSource = true;
            } else {
                // Try next trick
                return sanitize(context, sanitizing, errorHandler);
            }
        }

        PHPParseResult phpParserResult;
        // calling the php ast parser itself
        ASTPHP5Scanner scanner = new ASTPHP5Scanner(new StringReader(context.getSanitizedSource()), shortTags, aspTags);
        ASTPHP5Parser parser = new ASTPHP5Parser(scanner);

        parser.setErrorHandler(errorHandler);

        java_cup.runtime.Symbol rootSymbol = parser.parse();
        if (scanner.getCurlyBalance() != 0 && !sanitizedSource) {
            sanitizeSource(context, Sanitize.MISSING_CURLY, null);
            if (context.getSanitizedPart() != null) {
                context.setSourceHolder(new StringSourceHolder(context.getSanitizedSource()));
                scanner = new ASTPHP5Scanner(new StringReader(context.getBaseSource()), shortTags, aspTags);
                parser = new ASTPHP5Parser(scanner);
                rootSymbol = parser.parse();
            }
        }
        if (rootSymbol != null) {
            Program program = null;
            if (rootSymbol.value instanceof Program) {
                program = (Program) rootSymbol.value; // call the parser itself
                List<Statement> statements = program.getStatements();
                //do we need sanitization?
                boolean ok = true;
                for (Statement statement : statements) {
                    if (statement instanceof NamespaceDeclaration) {
                        NamespaceDeclaration ns = (NamespaceDeclaration) statement;
                        for (Statement st : ns.getBody().getStatements()) {
                            ok = isStatementOk(st, context);
                            if (!ok) {
                                break;
                            }
                        }
                        if (!ok) {
                            break;
                        }
                    } else {
                        ok = isStatementOk(statement, context);
                        if (!ok) {
                            break;
                        }
                    }
                }
                if (ok) {
                    phpParserResult = new PHPParseResult(context.getSnapshot(), program);
                } else {
                    phpParserResult = sanitize(context, sanitizing, errorHandler);
                }
            } else {
                LOGGER.log(Level.FINE, "The parser value is not a Program: {0}", rootSymbol.value);
                phpParserResult = sanitize(context, sanitizing, errorHandler);
            }
            if (isParsingWithoutSanitization(sanitizing)) {
                phpParserResult.setErrors(errorHandler.displaySyntaxErrors(program));
            }
        } else { // there was no rootElement
            phpParserResult = sanitize(context, sanitizing, errorHandler);
            phpParserResult.setErrors(errorHandler.displayFatalError());
        }

        return phpParserResult;
    }

    private static boolean isParsingWithoutSanitization(Sanitize sanitizing) {
        return (sanitizing == Sanitize.NONE) || (sanitizing == Sanitize.NEVER);
    }

    private boolean isStatementOk(final Statement statement, final Context context) throws IOException {
        boolean isStatementOk = true;
        if (statement instanceof ASTError) {
            // if there is an errot, try to sanitize only if there
            // is a class or function inside the error
            String errorCode = "<?php " + context.getSanitizedSource().substring(statement.getStartOffset(), statement.getEndOffset()) + "?>";
            ASTPHP5Scanner fcScanner = new ASTPHP5Scanner(new StringReader(errorCode), shortTags, aspTags);
            Symbol token = fcScanner.next_token();
            while (token.sym != ASTPHP5Symbols.EOF) {
                if (token.sym == ASTPHP5Symbols.T_CLASS || token.sym == ASTPHP5Symbols.T_FUNCTION || isRequireFunction(token)) {
                    isStatementOk = false;
                    break;
                }
                token = fcScanner.next_token();
            }
        }
        return isStatementOk;
    }

    private boolean sanitizeSource(Context context, Sanitize sanitizing, PHP5ErrorHandler errorHandler) {
        if (sanitizing == Sanitize.SYNTAX_ERROR_CURRENT) {
            List<PHP5ErrorHandler.SyntaxError> syntaxErrors = errorHandler.getSyntaxErrors();
            if (syntaxErrors.size() > 0) {
                PHP5ErrorHandler.SyntaxError error = syntaxErrors.get(0);
                String source = context.getBaseSource();
                int end = error.getCurrentToken().right;
                int start = error.getCurrentToken().left;
                String replace = source.substring(start, end);
                if ("}".equals(replace)) {
                    return false;
                }
                context.setSanitizedPart(new SanitizedPartImpl(new OffsetRange(start, end), Utils.getSpaces(end - start)));
                return true;
            }
        }
        if (sanitizing == Sanitize.SYNTAX_ERROR_PREVIOUS) {
            List<PHP5ErrorHandler.SyntaxError> syntaxErrors = errorHandler.getSyntaxErrors();
            if (syntaxErrors.size() > 0) {
                PHP5ErrorHandler.SyntaxError error = syntaxErrors.get(0);
                String source = context.getBaseSource();
                int end = error.getPreviousToken().right;
                int start = error.getPreviousToken().left;
                if (source.substring(start, end).equals("}")) {
                    return false;
                }
                context.setSanitizedPart(new SanitizedPartImpl(new OffsetRange(start, end), Utils.getSpaces(end - start)));
                return true;
            }
        }
        if (sanitizing == Sanitize.SYNTAX_ERROR_PREVIOUS_LINE) {
            List<PHP5ErrorHandler.SyntaxError> syntaxErrors = errorHandler.getSyntaxErrors();
            if (syntaxErrors.size() > 0) {
                PHP5ErrorHandler.SyntaxError error = syntaxErrors.get(0);
                String source = context.getBaseSource();

                int end = Utils.getRowEnd(source, error.getPreviousToken().right);
                int start = Utils.getRowStart(source, error.getPreviousToken().left);

                StringBuilder sb = new StringBuilder(end - start);
                for (int index = start; index < end; index++) {
                    if (source.charAt(index) == ' ' || source.charAt(index) == '}'
                            || source.charAt(index) == '\n' || source.charAt(index) == '\r') {
                        sb.append(source.charAt(index));
                    } else {
                        sb.append(' ');
                    }
                }

                context.setSanitizedPart(new SanitizedPartImpl(new OffsetRange(start, end), sb.toString()));
                return true;
            }
        }
        if (sanitizing == Sanitize.EDITED_LINE) {
            if (context.getCaretOffset() > 0) {
                String source = context.getBaseSource();
                int start = context.getCaretOffset() - 1;
                int end = context.getCaretOffset();
                // fix until new line or }
                char c = source.charAt(start);
                while (start > 0 && c != '\n' && c != '\r' && c != '{' && c != '}') {
                    c = source.charAt(--start);
                }
                start++;
                if (end < source.length()) {
                    c = source.charAt(end);
                    while (end < source.length() && c != '\n' && c != '\r' && c != '{' && c != '}') {
                        c = source.charAt(end++);
                    }
                }
                context.setSanitizedPart(new SanitizedPartImpl(new OffsetRange(start, end), Utils.getSpaces(end - start)));
                return true;
            }
        }
        if (sanitizing == Sanitize.MISSING_CURLY) {
            return sanitizeCurly(context);
        }
        if (sanitizing == Sanitize.SYNTAX_ERROR_BLOCK) {
            List<PHP5ErrorHandler.SyntaxError> syntaxErrors = errorHandler.getSyntaxErrors();
            if (syntaxErrors.size() > 0) {
                PHP5ErrorHandler.SyntaxError error = syntaxErrors.get(0);
                return sanitizeRemoveBlock(context, error.getCurrentToken().left);
            }
        }
        if (sanitizing == Sanitize.REQUIRE_FUNCTION_INCOMPLETE) {
            List<PHP5ErrorHandler.SyntaxError> syntaxErrors = errorHandler.getSyntaxErrors();
            if (syntaxErrors.size() > 0) {
                PHP5ErrorHandler.SyntaxError error = syntaxErrors.get(0);

                int start = Utils.getRowStart(context.getBaseSource(), error.getPreviousToken().left);
                int end = Utils.getRowEnd(context.getBaseSource(), error.getCurrentToken().left);

                return sanitizeRequireAndInclude(context, start, end);
            }
        }
        return false;
    }

    protected boolean sanitizeRequireAndInclude(Context context, int start, int end) {
        try {
            String source = context.getBaseSource();
            String shortOpenTag = "<?";
            String phpOpenDelimiter = shortOpenTag + "php ";
            String actualSource = phpOpenDelimiter + source.substring(start, end) + "?>";
            ASTPHP5Scanner scanner = new ASTPHP5Scanner(new StringReader(actualSource), shortTags, aspTags);
            char delimiter = '0';

            Symbol token = scanner.next_token();
            while (token.sym != ASTPHP5Symbols.EOF) {
                if (isRequireFunction(token)) {
                    boolean containsOpenParenthese = false;
                    int currentLeftOffset = token.right;

                    char c = actualSource.charAt(currentLeftOffset);
                    if (isStringDelimiter(c)) {
                        delimiter = c;
                    } else {
                        currentLeftOffset++;

                        if (Character.isWhitespace(c)) {
                            // fetch all following whitespaces
                            while (Character.isWhitespace(actualSource.charAt(currentLeftOffset))) {
                                currentLeftOffset++;
                            }

                            char cc = actualSource.charAt(currentLeftOffset);
                            if (isStringDelimiter(cc)) {
                                delimiter = cc;
                            } else if (cc == '(') {
                                containsOpenParenthese = true;
                                currentLeftOffset++;
                                delimiter = actualSource.charAt(currentLeftOffset);
                            }
                        } else if (c == '(') {
                            containsOpenParenthese = true;
                            delimiter = actualSource.charAt(currentLeftOffset);
                        }
                    }

                    if (isStringDelimiter(delimiter)) {
                        char expectedCloseDelimiter = actualSource.charAt(currentLeftOffset + 1);

                        boolean hasCloseDelimiter = false;
                        boolean hasCloseParenthese = false;
                        if (expectedCloseDelimiter == delimiter) {
                            hasCloseDelimiter = true;
                            currentLeftOffset++;

                            char expectedCloseParenthese = actualSource.charAt(currentLeftOffset + 1);
                            if (expectedCloseParenthese == ')') {
                                hasCloseParenthese = true;
                                currentLeftOffset++;
                            }
                        }

                        boolean canBeSanitized = true;
                        for (int i = 1; i <= numberOfSanitizedChars(containsOpenParenthese, hasCloseDelimiter, hasCloseParenthese); i++) {
                            if (!Character.isWhitespace(actualSource.charAt(currentLeftOffset + i))) {
                                canBeSanitized = false;
                                break;
                            }
                        }

                        if (canBeSanitized) {
                            int sanitizedChars = numberOfSanitizedChars(containsOpenParenthese, hasCloseDelimiter, hasCloseParenthese);
                            context.setSanitizedPart(new SanitizedPartImpl(
                                    new OffsetRange(start + currentLeftOffset - 1 - (phpOpenDelimiter.length() - shortOpenTag.length()),
                                            start + currentLeftOffset + sanitizedChars - phpOpenDelimiter.length() + 1),
                                    sanitizationString(delimiter, containsOpenParenthese, hasCloseDelimiter, hasCloseParenthese)));
                            return true;
                        } else {
                            break;
                        }
                    }
                }

                token = scanner.next_token();
            }
        } catch (IOException ex) {
            LOGGER.log(Level.INFO, "Exception during 'require' sanitization.", ex);
        }

        return false;
    }

    private boolean isRequireFunction(Symbol token) {
        return token.sym == ASTPHP5Symbols.T_REQUIRE || token.sym == ASTPHP5Symbols.T_REQUIRE_ONCE
                || token.sym == ASTPHP5Symbols.T_INCLUDE || token.sym == ASTPHP5Symbols.T_INCLUDE_ONCE;
    }

    private boolean isStringDelimiter(char c) {
        return c == '"' || c == '\'';
    }

    private String sanitizationString(char delimiter, boolean containsOpenParenthese, boolean containsCloseDelimiter, boolean containsCloseParenthese) {
        if (containsCloseDelimiter) {
            if (containsOpenParenthese) {
                if (containsCloseParenthese) {
                    return ";";
                } else {
                    return ");";
                }
            } else {
                return ";";
            }
        } else {
            if (containsOpenParenthese) {
                return delimiter + ");";
            } else {
                return delimiter + ";";
            }
        }
    }

    private int numberOfSanitizedChars(boolean containsOpenParenthese, boolean containsCloseDelimiter, boolean containsCloseParenthese) {
        int chars = 1;

        if (containsOpenParenthese) {
            chars += containsCloseParenthese ? 0 : 1;
        }

        chars += containsCloseDelimiter ? 0 : 1;

        return chars;
    }

    protected boolean sanitizeCurly(Context context) {
        String source = context.getBaseSource();
        ASTPHP5Scanner scanner = new ASTPHP5Scanner(new StringReader(source), shortTags, aspTags);
        //keep index of last ?>
        Symbol lastPHPToken = null;
        Symbol token = null;

        int bracketCounter = 0;
        int bracketClassCounter = 0;
        SanitizedPart classSanitizedPart = SanitizedPart.NONE;
        try {
            token = scanner.next_token();
            boolean inClass = false;
            int lastOpenInClass = -1;
            while (token.sym != ASTPHP5Symbols.EOF) {
                switch (token.sym) {
                    case ASTPHP5Symbols.T_CLASS:
                        if (!inClass) {
                            inClass = true;
                        } else {
                            char c;
                            int index = token.left;
                            // missing only the only one }
                            int max = bracketClassCounter;
                            for (int i = 0; i < max; i++) {
                                index--;
                                c = source.charAt(index);
                                while (index > lastOpenInClass && c != '}'
                                        && c != '\n' && c != '\r' && c != '\t' && c != ' ') {
                                    index--;
                                    c = source.charAt(index);
                                }
                                if (c != '}' && c != '{') {
                                    source = source.substring(0, index) + '}' + source.substring(index + 1);
                                    classSanitizedPart = createNewClassSanitizedPart(classSanitizedPart, source, index);
                                    bracketClassCounter--;
                                }
                            }

                            if (bracketClassCounter > 0) {
                                // try to add } at the beginig of line
                                index--;
                                c = source.charAt(index);
                                while (index > 0 && c != '}'
                                        && c != '\n' && c != '\r') {
                                    index--;
                                    c = source.charAt(index);
                                }
                                if (c == '}') {
                                    c = source.charAt(--index);
                                }
                                while (index < source.length() && bracketClassCounter > 0
                                        && (c == '\n' || c == '\r' || c == '\t' || c == ' ')) {
                                    source = source.substring(0, index) + '}' + source.substring(index + 1);
                                    classSanitizedPart = createNewClassSanitizedPart(classSanitizedPart, source, index);
                                    bracketClassCounter--;
                                    c = source.charAt(--index);
                                }
                            }
                            context.setSanitizedPart(classSanitizedPart);
                        }
                        break;
                    case ASTPHP5Symbols.T_CURLY_OPEN:
                    case ASTPHP5Symbols.T_CURLY_OPEN_WITH_DOLAR:
                        if (inClass) {
                            bracketClassCounter++;
                            lastOpenInClass = token.left;
                        } else {
                            bracketCounter++;
                        }
                        break;
                    case ASTPHP5Symbols.T_CURLY_CLOSE:
                        if (inClass) {
                            bracketClassCounter--;
                            if (bracketClassCounter == 0) {
                                inClass = false;
                            }
                        } else {
                            bracketCounter--;
                        }
                        break;
                    default:
                    // do nothing
                }
                if (token.sym != ASTPHP5Symbols.T_INLINE_HTML) {
                    lastPHPToken = token;
                }
                token = scanner.next_token();
            }
        } catch (IOException exception) {
            LOGGER.log(Level.INFO, "Exception during calculating missing }", exception);
        }
        int count = bracketCounter + bracketClassCounter;
        if (count > 0) {
            if (lastPHPToken != null) {
                String lastTokenText = source.substring(lastPHPToken.left, lastPHPToken.right).trim();
                if ("?>".equals(lastTokenText)) {   //NOI18N
                    context.setSanitizedPart(new SanitizedPartImpl(new OffsetRange(lastPHPToken.left, lastPHPToken.left), Utils.getRepeatingChars('}', count)));
                    return true;
                }
                if (token != null && token.sym == ASTPHP5Symbols.EOF) {
                    context.setSanitizedPart(new SanitizedPartImpl(new OffsetRange(token.left, token.left), Utils.getRepeatingChars('}', count)));
                    return true;
                }
            }
        }
        return false;
    }

    private SanitizedPart createNewClassSanitizedPart(SanitizedPart classSanitizedPart, String source, int index) {
        OffsetRange offsetRange = classSanitizedPart.getOffsetRange();
        int start = offsetRange.getStart();
        int end = offsetRange.getEnd();
        if (index <= start) {
            start = index;
        }
        if (index + 1 >= end) {
            end = index + 1;
        }
        return new SanitizedPartImpl(new OffsetRange(start, end), source.substring(start, end));
    }

    private boolean sanitizeRemoveBlock(Context context, int index) {
        String source = context.getBaseSource();
        ASTPHP5Scanner scanner = new ASTPHP5Scanner(new StringReader(source), shortTags, aspTags);
        Symbol token;
        int start = -1;
        int end = -1;
        try {
            token = scanner.next_token();
            while (token.sym != ASTPHP5Symbols.EOF && end == -1) {
                if (token.sym == ASTPHP5Symbols.T_CURLY_OPEN && token.left <= index) {
                    start = token.right;
                }
                if (token.sym == ASTPHP5Symbols.T_CURLY_CLOSE && token.left >= index) {
                    end = token.right - 1;
                }
                token = scanner.next_token();
            }
        } catch (IOException exception) {
            LOGGER.log(Level.INFO, "Exception during removing block", exception);   //NOI18N
        }
        if (start > -1 && start < end) {
            context.setSanitizedPart(new SanitizedPartImpl(new OffsetRange(start, end), Utils.getSpaces(end - start)));
            return true;
        }
        return false;
    }

    private PHPParseResult sanitize(final Context context, final Sanitize sanitizing, PHP5ErrorHandler errorHandler) throws Exception {
        switch (sanitizing) {
            case NONE:
            case MISSING_CURLY:
                return parseBuffer(context, Sanitize.REQUIRE_FUNCTION_INCOMPLETE, errorHandler);
            case REQUIRE_FUNCTION_INCOMPLETE:
                return parseBuffer(context, Sanitize.SYNTAX_ERROR_CURRENT, errorHandler);
            case SYNTAX_ERROR_CURRENT:
                // one more time
                return parseBuffer(context, Sanitize.SYNTAX_ERROR_PREVIOUS, errorHandler);
            case SYNTAX_ERROR_PREVIOUS:
                return parseBuffer(context, Sanitize.SYNTAX_ERROR_PREVIOUS_LINE, errorHandler);
            case SYNTAX_ERROR_PREVIOUS_LINE:
                return parseBuffer(context, Sanitize.EDITED_LINE, errorHandler);
            case EDITED_LINE:
                return parseBuffer(context, Sanitize.SYNTAX_ERROR_BLOCK, errorHandler);
            default:
                int end = context.getBaseSource().length();
                // add the ast error, some features can recognized that there is something wrong.
                // for example folding.
                ASTError error = new ASTError(0, end);
                List<Statement> statements = new ArrayList<>();
                statements.add(error);
                Program emptyProgram = new Program(0, end, statements, Collections.<Comment>emptyList());

                return new PHPParseResult(context.getSnapshot(), emptyProgram);
        }

    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (PhpLanguageProperties.PROP_PHP_VERSION.equals(evt.getPropertyName())) {
            forceReparsing();
        }
    }

    private void forceReparsing() {
        changeSupport.fireChange();
    }

    /**
     * Attempts to sanitize the input buffer.
     */
    public static enum Sanitize {

        /**
         * Only parse the current file accurately, don't try heuristics.
         */
        NEVER,
        /**
         * Perform no sanitization.
         */
        NONE,
        /**
         * Remove current error token.
         */
        SYNTAX_ERROR_CURRENT,
        /**
         * Remove token before error.
         */
        SYNTAX_ERROR_PREVIOUS,
        /**
         * remove line with error.
         */
        SYNTAX_ERROR_PREVIOUS_LINE,
        /**
         * try to delete the whole block, where is the error.
         */
        SYNTAX_ERROR_BLOCK,
        /**
         * Try to remove the trailing . or :: at the caret line.
         */
        EDITED_DOT,
        /**
         * Try to remove the trailing . or :: at the error position, or the
         * prior line, or the caret line.
         */
        ERROR_DOT,
        /**
         * Try to remove the initial "if" or "unless" on the block in case it's
         * not terminated.
         */
        BLOCK_START,
        /**
         * Try to cut out the error line.
         */
        ERROR_LINE,
        /**
         * Try to cut out the current edited line, if known.
         */
        EDITED_LINE,
        /**
         * Attempt to fix missing }.
         */
        MISSING_CURLY,
        /**
         * Try tu fix incomplete 'require("' function for FS code complete.
         */
        REQUIRE_FUNCTION_INCOMPLETE,
    }

    public static class Context {

        private final Snapshot snapshot;
        private final int caretOffset;
        private SourceHolder sourceHolder;
        private SanitizedPart sanitizedPart;

        public Context(Snapshot snapshot, int caretOffset) {
            this.snapshot = snapshot;
            this.caretOffset = caretOffset;
            this.sourceHolder = new SnapshotSourceHolder(snapshot);
        }

        @Override
        public String toString() {
            return "PHPParser.Context(" + snapshot.getSource().getFileObject() + ")"; // NOI18N
        }

        public Snapshot getSnapshot() {
            return snapshot;
        }

        private void setSourceHolder(SourceHolder sourceHolder) {
            this.sourceHolder = sourceHolder;
        }

        public String getBaseSource() {
            return sourceHolder.getText();
        }

        public int getCaretOffset() {
            return caretOffset;
        }

        public void setSanitizedPart(SanitizedPart sanitizedPart) {
            this.sanitizedPart = sanitizedPart;
        }

        public SanitizedPart getSanitizedPart() {
            return sanitizedPart;
        }

        public String getSanitizedSource() {
            StringBuilder sb = new StringBuilder();
            if (sanitizedPart == null) {
                sb.append(getBaseSource());
            } else {
                OffsetRange offsetRange = sanitizedPart.getOffsetRange();
                sb.append(getBaseSource().substring(0, offsetRange.getStart()))
                        .append(sanitizedPart.getText())
                        .append(getBaseSource().substring(offsetRange.getEnd()));
            }
            return sb.toString();
        }

    }

    public interface SanitizedPart {
        SanitizedPart NONE = new SanitizedPart() {

            @Override
            public OffsetRange getOffsetRange() {
                return OffsetRange.NONE;
            }

            @Override
            public String getText() {
                return "";
            }
        };

        OffsetRange getOffsetRange();
        String getText();

    }

    public static class SanitizedPartImpl implements SanitizedPart {
        private final OffsetRange offsetRange;
        private final String text;

        public SanitizedPartImpl(OffsetRange offsetRange, String text) {
            assert offsetRange != null;
            assert text != null;
            this.offsetRange = offsetRange;
            this.text = text;
        }

        @Override
        public OffsetRange getOffsetRange() {
            return offsetRange;
        }

        @Override
        public String getText() {
            return text;
        }

    }

    private interface SourceHolder {

        String getText();

    }

    private static class StringSourceHolder implements SourceHolder {
        private final String text;

        public StringSourceHolder(String text) {
            assert text != null;
            this.text = text;
        }

        @Override
        public String getText() {
            return text;
        }

    }

    private static class SnapshotSourceHolder implements SourceHolder {
        private final Snapshot snapshot;

        public SnapshotSourceHolder(Snapshot snapshot) {
            assert snapshot != null;
            this.snapshot = snapshot;
        }

        @Override
        public String getText() {
            return snapshot.getText().toString();
        }

    }

}
