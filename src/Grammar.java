import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class Grammar {
    public static final String EPSILON = "epsilon";

    private final LinkedHashMap<String, List<List<String>>> productions;
    private final LinkedHashSet<String> nonTerminals;
    private final LinkedHashSet<String> terminals;
    private String startSymbol;

    public Grammar() {
        this.productions = new LinkedHashMap<>();
        this.nonTerminals = new LinkedHashSet<>();
        this.terminals = new LinkedHashSet<>();
    }

    public static Grammar fromFile(String filePath) throws IOException {
        List<String> lines = Files.readAllLines(Path.of(filePath));
        Grammar grammar = new Grammar();
        grammar.load(lines);
        return grammar;
    }

    public Grammar copy() {
        Grammar clone = new Grammar();
        clone.startSymbol = this.startSymbol;

        for (String nonTerminal : this.getNonTerminalsInOrder()) {
            List<List<String>> alternatives = this.productions.get(nonTerminal);
            List<List<String>> alternativesCopy = new ArrayList<>();
            for (List<String> alternative : alternatives) {
                alternativesCopy.add(new ArrayList<>(alternative));
            }
            clone.productions.put(nonTerminal, alternativesCopy);
            clone.nonTerminals.add(nonTerminal);
        }

        clone.rebuildTerminals();
        return clone;
    }

    public void addProduction(String nonTerminal, List<String> alternative) {
        validateNonTerminal(nonTerminal);
        List<String> normalizedAlternative = normalizeAlternative(alternative);

        if (startSymbol == null) {
            startSymbol = nonTerminal;
        }

        productions.computeIfAbsent(nonTerminal, key -> new ArrayList<>());
        nonTerminals.add(nonTerminal);
        productions.get(nonTerminal).add(normalizedAlternative);
        rebuildTerminals();
    }

    public void setAlternatives(String nonTerminal, List<List<String>> alternatives) {
        validateNonTerminal(nonTerminal);

        List<List<String>> normalizedAlternatives = new ArrayList<>();
        for (List<String> alternative : alternatives) {
            normalizedAlternatives.add(normalizeAlternative(alternative));
        }

        if (startSymbol == null) {
            startSymbol = nonTerminal;
        }

        productions.put(nonTerminal, normalizedAlternatives);
        nonTerminals.add(nonTerminal);
        rebuildTerminals();
    }

    public boolean hasNonTerminal(String symbol) {
        return nonTerminals.contains(symbol);
    }

    public String getStartSymbol() {
        return startSymbol;
    }

    public List<String> getNonTerminalsInOrder() {
        return new ArrayList<>(productions.keySet());
    }

    public Set<String> getTerminals() {
        return Collections.unmodifiableSet(terminals);
    }

    public Map<String, List<List<String>>> getProductions() {
        LinkedHashMap<String, List<List<String>>> view = new LinkedHashMap<>();
        for (Map.Entry<String, List<List<String>>> entry : productions.entrySet()) {
            List<List<String>> alternatives = new ArrayList<>();
            for (List<String> alternative : entry.getValue()) {
                alternatives.add(Collections.unmodifiableList(alternative));
            }
            view.put(entry.getKey(), Collections.unmodifiableList(alternatives));
        }
        return Collections.unmodifiableMap(view);
    }

    public List<List<String>> getAlternatives(String nonTerminal) {
        List<List<String>> alternatives = productions.get(nonTerminal);
        if (alternatives == null) {
            return Collections.emptyList();
        }

        List<List<String>> copy = new ArrayList<>();
        for (List<String> alternative : alternatives) {
            copy.add(Collections.unmodifiableList(alternative));
        }
        return Collections.unmodifiableList(copy);
    }

    public String nextGeneratedNonTerminal(String base) {
        String candidate = base + "Prime";
        if (!nonTerminals.contains(candidate)) {
            return candidate;
        }

        int suffix = 2;
        while (nonTerminals.contains(candidate + suffix)) {
            suffix++;
        }
        return candidate + suffix;
    }

    public String toFormattedString() {
        StringBuilder builder = new StringBuilder();
        boolean firstRule = true;

        for (Map.Entry<String, List<List<String>>> entry : productions.entrySet()) {
            if (!firstRule) {
                builder.append(System.lineSeparator());
            }
            firstRule = false;
            builder.append(entry.getKey()).append(" -> ");

            List<String> renderedAlternatives = new ArrayList<>();
            for (List<String> alternative : entry.getValue()) {
                renderedAlternatives.add(renderAlternative(alternative));
            }
            builder.append(String.join(" | ", renderedAlternatives));
        }

        return builder.toString();
    }

    public static boolean isEpsilon(String symbol) {
        return EPSILON.equals(symbol) || "@".equals(symbol);
    }

    public static boolean isNonTerminalName(String symbol) {
        return symbol != null
            && !symbol.isBlank()
            && Character.isUpperCase(symbol.charAt(0));
    }

    public static String renderAlternative(List<String> alternative) {
        if (alternative.size() == 1 && EPSILON.equals(alternative.get(0))) {
            return EPSILON;
        }
        return String.join(" ", alternative);
    }

    private void load(List<String> lines) {
        List<RuleDraft> drafts = new ArrayList<>();

        for (int index = 0; index < lines.size(); index++) {
            String rawLine = lines.get(index).trim();
            if (rawLine.isEmpty()) {
                continue;
            }

            String[] parts = rawLine.split("->", 2);
            if (parts.length != 2) {
                throw new IllegalArgumentException("Invalid production at line " + (index + 1) + ": missing '->'.");
            }

            String left = parts[0].trim();
            String right = parts[1].trim();

            validateNonTerminal(left);
            if (right.isEmpty()) {
                throw new IllegalArgumentException("Invalid production at line " + (index + 1) + ": missing RHS.");
            }

            if (startSymbol == null) {
                startSymbol = left;
            }
            nonTerminals.add(left);
            drafts.add(new RuleDraft(left, right, index + 1));
        }

        for (RuleDraft draft : drafts) {
            List<List<String>> alternatives = parseAlternatives(draft.rightSide(), draft.lineNumber());
            productions.computeIfAbsent(draft.leftSide(), key -> new ArrayList<>());
            productions.get(draft.leftSide()).addAll(alternatives);
        }

        rebuildTerminals();
    }

    private List<List<String>> parseAlternatives(String rightSide, int lineNumber) {
        List<List<String>> alternatives = new ArrayList<>();
        String[] rawAlternatives = rightSide.split("\\|");

        for (String rawAlternative : rawAlternatives) {
            String trimmed = rawAlternative.trim();
            if (trimmed.isEmpty()) {
                throw new IllegalArgumentException("Invalid production at line " + lineNumber + ": empty alternative.");
            }

            List<String> tokens = new ArrayList<>(Arrays.asList(trimmed.split("\\s+")));
            alternatives.add(normalizeAlternative(tokens));
        }
        return alternatives;
    }

    private List<String> normalizeAlternative(List<String> alternative) {
        Objects.requireNonNull(alternative, "alternative cannot be null");
        if (alternative.isEmpty()) {
            throw new IllegalArgumentException("Production alternative cannot be empty.");
        }

        List<String> normalized = new ArrayList<>();
        for (String token : alternative) {
            if (token == null || token.isBlank()) {
                continue;
            }
            if (isEpsilon(token)) {
                normalized.add(EPSILON);
            } else {
                normalized.add(token.trim());
            }
        }

        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Production alternative cannot be empty.");
        }

        if (normalized.size() > 1 && normalized.contains(EPSILON)) {
            throw new IllegalArgumentException("Epsilon must appear alone in an alternative.");
        }

        return normalized;
    }

    private void rebuildTerminals() {
        terminals.clear();
        for (List<List<String>> alternatives : productions.values()) {
            for (List<String> alternative : alternatives) {
                for (String symbol : alternative) {
                    if (!EPSILON.equals(symbol) && !nonTerminals.contains(symbol)) {
                        terminals.add(symbol);
                    }
                }
            }
        }
    }

    private void validateNonTerminal(String nonTerminal) {
        if (!isNonTerminalName(nonTerminal)) {
            throw new IllegalArgumentException(
                "Invalid non-terminal '" + nonTerminal + "'. Non-terminals must start with an uppercase letter."
            );
        }
    }

    private record RuleDraft(String leftSide, String rightSide, int lineNumber) {
    }
}