import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Parser {
    private final Grammar grammar;
    private final FirstFollow firstFollow;
    private final ParsingTable parsingTable;

    public Parser(Grammar grammar, FirstFollow firstFollow, ParsingTable parsingTable) {
        this.grammar = grammar;
        this.firstFollow = firstFollow;
        this.parsingTable = parsingTable;
    }

    public ParseFileResult parseInputFile(String inputFilePath) throws IOException {
        ensureLl1Grammar();

        List<String> rawLines = Files.readAllLines(Path.of(inputFilePath));
        List<LineParseResult> lineResults = new ArrayList<>();

        for (int index = 0; index < rawLines.size(); index++) {
            lineResults.add(parseLine(rawLines.get(index), index + 1));
        }

        return new ParseFileResult(inputFilePath, lineResults);
    }

    public static List<String> tokenize(String rawLine) {
        if (rawLine == null || rawLine.isBlank()) {
            return new ArrayList<>();
        }

        String[] parts = rawLine.trim().split("\\s+");
        ArrayList<String> tokens = new ArrayList<>();
        Collections.addAll(tokens, parts);
        return tokens;
    }

    public Grammar getGrammar() {
        return grammar;
    }

    public FirstFollow getFirstFollow() {
        return firstFollow;
    }

    public ParsingTable getParsingTable() {
        return parsingTable;
    }

    private LineParseResult parseLine(String rawLine, int lineNumber) {
        ArrayList<String> workingInput = new ArrayList<>(tokenize(rawLine));
        workingInput.add(FirstFollow.END_MARKER);

        Stack<String> stack = new Stack<>();
        Stack<Tree.Node> nodeStack = new Stack<>();
        Tree parseTree = new Tree(grammar.getStartSymbol());
        stack.push(FirstFollow.END_MARKER);
        stack.push(grammar.getStartSymbol());
        nodeStack.push(null);
        nodeStack.push(parseTree.getRoot());

        ArrayList<TraceStep> traceSteps = new ArrayList<>();
        ErrorHandler errorHandler = new ErrorHandler();
        boolean accepted = false;
        int inputIndex = 0;
        int stepNumber = 1;

        while (!stack.isEmpty()) {
            String top = stack.top();
            String current = workingInput.get(inputIndex);
            String stackSnapshot = renderStack(stack);
            String inputSnapshot = renderRemainingInput(workingInput, inputIndex);

            if (FirstFollow.END_MARKER.equals(top) && FirstFollow.END_MARKER.equals(current)) {
                String action = errorHandler.hasErrors() ? "Accept (after recovery)" : "Accept";
                traceSteps.add(new TraceStep(stepNumber, stackSnapshot, inputSnapshot, action));
                accepted = !errorHandler.hasErrors();
                break;
            }

            if (!grammar.hasNonTerminal(top)) {
                if (top.equals(current)) {
                    traceSteps.add(new TraceStep(stepNumber, stackSnapshot, inputSnapshot, "Match " + current));
                    stack.pop();
                    nodeStack.pop();
                    inputIndex++;
                } else {
                    if (FirstFollow.END_MARKER.equals(top)) {
                        errorHandler.add(ErrorHandler.unexpectedToken(lineNumber, inputIndex + 1, current, top));
                        traceSteps.add(
                            new TraceStep(stepNumber, stackSnapshot, inputSnapshot, "ERROR: Extra symbol " + current + ". Skipping " + current)
                        );
                        inputIndex++;
                    } else if (FirstFollow.END_MARKER.equals(current)) {
                        errorHandler.add(ErrorHandler.prematureEnd(lineNumber, inputIndex + 1, top));
                        traceSteps.add(
                            new TraceStep(stepNumber, stackSnapshot, inputSnapshot, "ERROR: Premature end of input. Missing " + top)
                        );
                        stack.pop();
                        nodeStack.pop();
                    } else {
                        errorHandler.add(ErrorHandler.missingSymbol(lineNumber, inputIndex + 1, top, current));
                        traceSteps.add(
                            new TraceStep(stepNumber, stackSnapshot, inputSnapshot, "ERROR: Missing " + top + ". Popping " + top)
                        );
                        stack.pop();
                        nodeStack.pop();
                    }
                }

                stepNumber++;
                continue;
            }

            List<String> production = parsingTable.getProduction(top, current);
            if (production == null) {
                String expected = expectedLookaheads(top);
                if (FirstFollow.END_MARKER.equals(current)) {
                    errorHandler.add(ErrorHandler.prematureEnd(lineNumber, inputIndex + 1, top));
                    traceSteps.add(
                        new TraceStep(
                            stepNumber,
                            stackSnapshot,
                            inputSnapshot,
                            "ERROR: Premature end of input while parsing " + top + ". Synchronizing by popping " + top
                        )
                    );
                    stack.pop();
                    nodeStack.pop();
                } else if (firstFollow.getFollowSets().get(top).contains(current)) {
                    errorHandler.add(ErrorHandler.synchronize(lineNumber, inputIndex + 1, top, current));
                    traceSteps.add(
                        new TraceStep(
                            stepNumber,
                            stackSnapshot,
                            inputSnapshot,
                            "ERROR: No rule for " + top + " on " + current + ". Synchronizing by popping " + top
                        )
                    );
                    stack.pop();
                    nodeStack.pop();
                } else {
                    errorHandler.add(ErrorHandler.unexpectedToken(lineNumber, inputIndex + 1, current, expected));
                    traceSteps.add(
                        new TraceStep(
                            stepNumber,
                            stackSnapshot,
                            inputSnapshot,
                            "ERROR: Unexpected " + current + ". Expected: " + expected + ". Skipping " + current
                        )
                    );
                    inputIndex++;
                }

                stepNumber++;
                continue;
            }

            traceSteps.add(new TraceStep(stepNumber, stackSnapshot, inputSnapshot, top + " -> " + Grammar.renderAlternative(production)));
            stack.pop();
            Tree.Node parentNode = nodeStack.pop();
            pushProduction(stack, nodeStack, parentNode, production);
            stepNumber++;
        }

        if (!accepted && !errorHandler.hasErrors()) {
            String expected = stack.isEmpty() ? FirstFollow.END_MARKER : stack.top();
            String found = workingInput.get(Math.min(inputIndex, workingInput.size() - 1));
            errorHandler.add(ErrorHandler.unexpectedToken(lineNumber, inputIndex + 1, found, expected));
        }

        return new LineParseResult(lineNumber, rawLine, traceSteps, errorHandler, accepted, accepted ? parseTree : null);
    }

    private void ensureLl1Grammar() {
        if (!parsingTable.isLl1()) {
            throw new IllegalStateException("Cannot parse input strings because the grammar is not LL(1).");
        }
    }

    private void pushProduction(Stack<String> stack, Stack<Tree.Node> nodeStack, Tree.Node parentNode, List<String> production) {
        if (parentNode == null) {
            return;
        }

        if (production.size() == 1 && Grammar.EPSILON.equals(production.get(0))) {
            parentNode.addChild(Grammar.EPSILON);
            return;
        }

        ArrayList<Tree.Node> childNodes = new ArrayList<>();
        for (String symbol : production) {
            childNodes.add(parentNode.addChild(symbol));
        }

        for (int index = production.size() - 1; index >= 0; index--) {
            stack.push(production.get(index));
            nodeStack.push(childNodes.get(index));
        }
    }

    private String renderStack(Stack<String> stack) {
        return String.join(" ", stack.toBottomToTopList());
    }

    private String renderRemainingInput(List<String> input, int inputIndex) {
        return String.join(" ", input.subList(inputIndex, input.size()));
    }

    private String expectedLookaheads(String nonTerminal) {
        List<String> expected = new ArrayList<>();
        for (String terminal : parsingTable.getTerminalsInOrder()) {
            if (parsingTable.hasEntry(nonTerminal, terminal)) {
                expected.add(terminal);
            }
        }

        if (expected.isEmpty()) {
            return nonTerminal;
        }

        return String.join(" or ", expected);
    }

    public record TraceStep(int stepNumber, String stackContents, String remainingInput, String action) {
    }

    public static class ParseFileResult {
        private final String inputFilePath;
        private final List<LineParseResult> lineResults;

        public ParseFileResult(String inputFilePath, List<LineParseResult> lineResults) {
            this.inputFilePath = inputFilePath;
            this.lineResults = lineResults;
        }

        public String formatTraceReport() {
            StringBuilder builder = new StringBuilder();
            builder.append("Input File: ").append(inputFilePath).append(System.lineSeparator()).append(System.lineSeparator());

            for (int index = 0; index < lineResults.size(); index++) {
                if (index > 0) {
                    builder.append(System.lineSeparator());
                    builder.append("=".repeat(110)).append(System.lineSeparator()).append(System.lineSeparator());
                }
                builder.append(lineResults.get(index).formatTrace());
            }

            return builder.toString();
        }

        public String formatAcceptedTrees() {
            StringBuilder builder = new StringBuilder();
            builder.append("Input File: ").append(inputFilePath).append(System.lineSeparator()).append(System.lineSeparator());

            boolean wroteTree = false;
            for (LineParseResult lineResult : lineResults) {
                if (!lineResult.isAccepted() || lineResult.getParseTree() == null) {
                    continue;
                }

                if (wroteTree) {
                    builder.append(System.lineSeparator());
                    builder.append("-".repeat(80)).append(System.lineSeparator()).append(System.lineSeparator());
                }

                builder.append("Input Line ").append(lineResult.getLineNumber()).append(": ");
                builder.append(lineResult.getSourceLine() == null || lineResult.getSourceLine().isBlank() ? "<empty>" : lineResult.getSourceLine().trim());
                builder.append(System.lineSeparator());
                builder.append(lineResult.getParseTree().toIndentedString());
                wroteTree = true;
            }

            if (!wroteTree) {
                builder.append("No accepted strings in this file.").append(System.lineSeparator());
            }

            return builder.toString();
        }
    }

    public static class LineParseResult {
        private final int lineNumber;
        private final String sourceLine;
        private final List<TraceStep> traceSteps;
        private final ErrorHandler errorHandler;
        private final boolean accepted;
        private final Tree parseTree;

        public LineParseResult(
            int lineNumber,
            String sourceLine,
            List<TraceStep> traceSteps,
            ErrorHandler errorHandler,
            boolean accepted,
            Tree parseTree
        ) {
            this.lineNumber = lineNumber;
            this.sourceLine = sourceLine;
            this.traceSteps = traceSteps;
            this.errorHandler = errorHandler;
            this.accepted = accepted;
            this.parseTree = parseTree;
        }

        public boolean isAccepted() {
            return accepted;
        }

        public int getLineNumber() {
            return lineNumber;
        }

        public String getSourceLine() {
            return sourceLine;
        }

        public Tree getParseTree() {
            return parseTree;
        }

        public String formatTrace() {
            StringBuilder builder = new StringBuilder();
            builder.append("Input Line ").append(lineNumber).append(": ");
            builder.append(sourceLine == null || sourceLine.isBlank() ? "<empty>" : sourceLine.trim());
            builder.append(System.lineSeparator());
            builder.append(String.format("%-6s| %-30s| %-30s| %s%n", "Step", "Stack", "Input", "Action"));
            builder.append("-".repeat(110)).append(System.lineSeparator());

            for (TraceStep traceStep : traceSteps) {
                builder.append(
                    String.format(
                        "%-6d| %-30s| %-30s| %s%n",
                        traceStep.stepNumber(),
                        traceStep.stackContents(),
                        traceStep.remainingInput(),
                        traceStep.action()
                    )
                );
            }

            builder.append(System.lineSeparator());
            if (accepted) {
                builder.append("Result: String accepted successfully!");
            } else if (errorHandler.hasErrors()) {
                builder.append("Result: Parsing completed with ").append(errorHandler.size()).append(" error(s).")
                    .append(System.lineSeparator());
                builder.append("Errors").append(System.lineSeparator());
                builder.append(errorHandler.formatErrors());
            } else {
                builder.append("Result: Parsing failed.");
            }

            return builder.toString();
        }
    }
}