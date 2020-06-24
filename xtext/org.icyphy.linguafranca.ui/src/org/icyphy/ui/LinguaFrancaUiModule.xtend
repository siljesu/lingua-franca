/*
 * generated by Xtext 2.17.0
 */
package org.icyphy.ui

import org.eclipse.jface.text.BadLocationException
import org.eclipse.jface.text.DocumentCommand
import org.eclipse.jface.text.IDocument
import org.eclipse.jface.text.IRegion
import org.eclipse.xtend.lib.annotations.FinalFieldsConstructor
import org.eclipse.xtext.ui.editor.autoedit.AbstractEditStrategyProvider.IEditStrategyAcceptor
import org.eclipse.xtext.ui.editor.autoedit.CommandInfo
import org.eclipse.xtext.ui.editor.autoedit.CompoundMultiLineTerminalsEditStrategy
import org.eclipse.xtext.ui.editor.autoedit.DefaultAutoEditStrategyProvider
import org.eclipse.xtext.ui.editor.autoedit.MultiLineTerminalsEditStrategy
import org.eclipse.xtext.ui.editor.autoedit.SingleLineTerminalsStrategy

/**
 * Use this class to register components to be used within the Eclipse IDE.
 */
@FinalFieldsConstructor
class LinguaFrancaUiModule extends AbstractLinguaFrancaUiModule {
    
    def Class<? extends DefaultAutoEditStrategyProvider> bindAutoEditStrategy() {
        return LinguaFrancaAutoEdit
    }
    
    static class LinguaFrancaAutoEdit extends DefaultAutoEditStrategyProvider {
        // After a huge amount of experimentation with completely undocumented
        // xtext code, the following seems to provide auto completion for codeblock
        // delimiters for Lingua Franca.
        
        // Following needs to be a 
        protected CompoundLFMultiLineTerminalsEditStrategy.Factory compoundLFMultiLineTerminals;

        // The following from the base class completely messes up with codeblocks.
        // So we replace it below.
        override void configureCompoundBracesBlocks(IEditStrategyAcceptor acceptor) {
            // acceptor.accept(compoundMultiLineTerminals.newInstanceFor("{", "}").and("[", "]").and("(", ")"), IDocument.DEFAULT_CONTENT_TYPE);
            acceptor.accept(compoundLFMultiLineTerminals.newInstanceFor("{=", "=}")
                .and("{", "}")
                .and("(", ")")
                .and("[", "]"), IDocument.DEFAULT_CONTENT_TYPE);
        }
                
        /**
         * When encountering {= append =}.
         */
        protected def configureCodeBlock(IEditStrategyAcceptor acceptor) {
            acceptor.accept(new SingleLineTerminalsStrategy("{=", "=}", SingleLineTerminalsStrategy.DEFAULT) {
                    override void handleInsertLeftTerminal(IDocument document, DocumentCommand command)
                            throws BadLocationException {
                        if (command.text.length() > 0 && appliedText(document, command).endsWith(getLeftTerminal())
                                && isInsertClosingTerminal(document, command.offset + command.length)) {
                            val documentContent = getDocumentContent(document, command);
                            val opening = count(getLeftTerminal(), documentContent);
                            val closing = count(getRightTerminal(), documentContent);
                            val occurences = opening + closing;
                            if (occurences % 2 == 0) {
                                command.caretOffset = command.offset + command.text.length();
                                // Do not insert the right delimitter '=}' because there is already
                                // a '}' from the previous auto complete when the '{' was typed.
                                command.text = command.text + '=';
                                command.shiftsCaret = false;
                            }
                        }
                    }
    
                    override boolean isInsertClosingTerminal(IDocument doc, int offset) {
                        if (doc.getLength() <= offset) return true;
                        if (offset == 0) return false;
                        // xtend fails horribly with char literals, so we have to
                        // convert this to a string.
                        val charAtOffset = Character.toString(doc.getChar(offset));
                        val charBeforeOffset = Character.toString(doc.getChar(offset - 1));
                        val result = ((charAtOffset == '}') && charBeforeOffset == '{')
                        return result
                    }
                },
                IDocument.DEFAULT_CONTENT_TYPE
            );
        }
        /**
         * When hitting Return with a code block, move the =} to a newline properly indented.
         */
        protected def configureMultilineCodeBlock(IEditStrategyAcceptor acceptor) {
            acceptor.accept(new LFMultiLineTerminalsEditStrategy("(", ")", true), IDocument.DEFAULT_CONTENT_TYPE)
            acceptor.accept(new LFMultiLineTerminalsEditStrategy("(", ")", true), IDocument.DEFAULT_CONTENT_TYPE)
            acceptor.accept(new LFMultiLineTerminalsEditStrategy("[", "]", true), IDocument.DEFAULT_CONTENT_TYPE)
            acceptor.accept(new LFMultiLineTerminalsEditStrategy("{=", "=}", true), IDocument.DEFAULT_CONTENT_TYPE)
        }
        
        /** Specify these new acceptors. */
        override void configure(IEditStrategyAcceptor acceptor) {
            configureMultilineCodeBlock(acceptor)
            super.configure(acceptor)
            configureCodeBlock(acceptor)
        }
        
        static class LFMultiLineTerminalsEditStrategy extends MultiLineTerminalsEditStrategy {
            new(String leftTerminal, String rightTerminal, boolean nested) {
                super(leftTerminal, null, rightTerminal, nested)
            }

            override CommandInfo handleCursorInFirstLine(
                    IDocument document,
                    DocumentCommand command,
                    IRegion startTerminal,
                    IRegion stopTerminal) throws BadLocationException {
                // Create a modified command.
                val newC = new CommandInfo();
                // If this is handling delimiters { }, but the actual delimiters are {= =},
                // then do nothing.
                if (leftTerminal == "{") {
                    val start = document.get(startTerminal.offset, 2)
                    if (start == "{=") return newC
                }
                newC.isChange = true;
                newC.offset = command.offset;
                // Insert the Return character into the new command.
                newC.text += command.text;
                newC.cursorOffset = command.offset + newC.text.length();
                if (stopTerminal === null && atEndOfLineInput(document, command.offset)) {
                    newC.text += command.text + getRightTerminal();
                }
                if (stopTerminal !== null && stopTerminal.getOffset() >= command.offset
                        && util.isSameLine(document, stopTerminal.getOffset(), command.offset)) {
                    // Get the string between the delimiters
                    val string = document.get(
                        command.offset,
                        stopTerminal.getOffset() - command.offset
                    );
                    // Find the indentation needed.
                    val lineNumber = document.getLineOfOffset(command.offset) // Line number.
                    val lineStart = document.getLineOffset(lineNumber) // Offset of start of line.
                    val lineLength = document.getLineLength(lineNumber) // Length of the line.
                    val line = document.get(lineStart, lineLength)
                    // This assumes any existing indentation is spaces, not tabs.
                    val indentation = line.indexOf(line.trim())
                    // Indent by at least 4 spaces.
                    for (var i = 0; i < indentation / 4 + 1; i++) {
                        newC.text += "    "
                        newC.cursorOffset += 4
                    }
                    newC.text += string.trim();
                    newC.text += command.text;
                    for (var i = 0; i < indentation / 4; i++) {
                        newC.text += "    "
                    }
                    newC.length += string.trim().length();
                }
                return newC;
            }
        }
        
        static class CompoundLFMultiLineTerminalsEditStrategy extends CompoundMultiLineTerminalsEditStrategy {
            // FIXME: Do Something here!
        }
    }
}
