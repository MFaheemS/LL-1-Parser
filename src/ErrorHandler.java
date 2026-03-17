import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ErrorHandler {
    private final ArrayList<ParseError> errors;

    public ErrorHandler() {
        this.errors = new ArrayList<>();
    }

    public void add(ParseError error) {
        errors.add(error);
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public int size() {
        return errors.size();
    }

    public List<ParseError> getErrors() {
        return Collections.unmodifiableList(errors);
    }

    public String formatErrors() {
        StringBuilder builder = new StringBuilder();
        for (ParseError error : errors) {
            builder.append(error.format()).append(System.lineSeparator());
        }
        return builder.toString();
    }

    public static ParseError unexpectedToken(int lineNumber, int columnNumber, String found, String expected) {
        return new ParseError(
            lineNumber,
            columnNumber,
            found,
            expected,
            "Unexpected symbol"
        );
    }

    public static ParseError missingSymbol(int lineNumber, int columnNumber, String expected, String found) {
        return new ParseError(
            lineNumber,
            columnNumber,
            found,
            expected,
            "Missing symbol"
        );
    }

    public static ParseError synchronize(int lineNumber, int columnNumber, String nonTerminal, String found) {
        return new ParseError(
            lineNumber,
            columnNumber,
            found,
            nonTerminal,
            "Synchronizing by popping non-terminal"
        );
    }

    public static ParseError emptyTableEntry(int lineNumber, int columnNumber, String nonTerminal, String found) {
        return new ParseError(
            lineNumber,
            columnNumber,
            found,
            nonTerminal,
            "No parsing-table entry"
        );
    }

    public static ParseError prematureEnd(int lineNumber, int columnNumber, String expected) {
        return new ParseError(
            lineNumber,
            columnNumber,
            FirstFollow.END_MARKER,
            expected,
            "Premature end of input"
        );
    }

    public record ParseError(int lineNumber, int columnNumber, String found, String expected, String message) {
        public String format() {
            return "Line " + lineNumber
                + ", Column " + columnNumber
                + ": " + message
                + " | Expected: " + expected
                + " | Found: " + found;
        }
    }
}