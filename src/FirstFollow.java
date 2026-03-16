import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FirstFollow {
    public static final String END_MARKER = "$";

    private final Grammar grammar;
    private final LinkedHashMap<String, LinkedHashSet<String>> firstSets;
    private final LinkedHashMap<String, LinkedHashSet<String>> followSets;

    public FirstFollow(Grammar grammar) {
        this.grammar = grammar;
        this.firstSets = new LinkedHashMap<>();
        this.followSets = new LinkedHashMap<>();

        for (String nonTerminal : grammar.getNonTerminalsInOrder()) {
            firstSets.put(nonTerminal, new LinkedHashSet<>());
            followSets.put(nonTerminal, new LinkedHashSet<>());
        }
    }

    public void compute() {
        computeFirstSets();
        computeFollowSets();
    }

    public Map<String, LinkedHashSet<String>> getFirstSets() {
        return firstSets;
    }

    public Map<String, LinkedHashSet<String>> getFollowSets() {
        return followSets;
    }

    public LinkedHashSet<String> firstOfSequence(List<String> sequence) {
        LinkedHashSet<String> result = new LinkedHashSet<>();

        if (sequence.isEmpty()) {
            result.add(Grammar.EPSILON);
            return result;
        }

        boolean allNullable = true;
        for (String symbol : sequence) {
            LinkedHashSet<String> symbolFirst = firstOfSymbol(symbol);
            for (String token : symbolFirst) {
                if (!Grammar.EPSILON.equals(token)) {
                    result.add(token);
                }
            }

            if (!symbolFirst.contains(Grammar.EPSILON)) {
                allNullable = false;
                break;
            }
        }

        if (allNullable) {
            result.add(Grammar.EPSILON);
        }
        return result;
    }

    public String formatSets() {
        StringBuilder builder = new StringBuilder();
        builder.append("FIRST Sets").append(System.lineSeparator());
        for (String nonTerminal : grammar.getNonTerminalsInOrder()) {
            builder.append(String.format("%-18s = %s%n", "FIRST(" + nonTerminal + ")", formatSet(firstSets.get(nonTerminal))));
        }

        builder.append(System.lineSeparator());
        builder.append("FOLLOW Sets").append(System.lineSeparator());
        for (String nonTerminal : grammar.getNonTerminalsInOrder()) {
            builder.append(String.format("%-18s = %s%n", "FOLLOW(" + nonTerminal + ")", formatSet(followSets.get(nonTerminal))));
        }
        return builder.toString();
    }

    private void computeFirstSets() {
        boolean changed;
        do {
            changed = false;
            for (String nonTerminal : grammar.getNonTerminalsInOrder()) {
                for (List<String> alternative : grammar.getAlternatives(nonTerminal)) {
                    LinkedHashSet<String> firstOfAlternative = firstOfSequence(alternative);
                    if (firstSets.get(nonTerminal).addAll(firstOfAlternative)) {
                        changed = true;
                    }
                }
            }
        } while (changed);
    }

    private void computeFollowSets() {
        followSets.get(grammar.getStartSymbol()).add(END_MARKER);
        boolean changed;

        do {
            changed = false;
            for (String nonTerminal : grammar.getNonTerminalsInOrder()) {
                for (List<String> alternative : grammar.getAlternatives(nonTerminal)) {
                    for (int position = 0; position < alternative.size(); position++) {
                        String symbol = alternative.get(position);
                        if (!grammar.hasNonTerminal(symbol)) {
                            continue;
                        }

                        List<String> suffix = new ArrayList<>();
                        for (int index = position + 1; index < alternative.size(); index++) {
                            suffix.add(alternative.get(index));
                        }

                        LinkedHashSet<String> firstOfSuffix = firstOfSequence(suffix);
                        for (String token : firstOfSuffix) {
                            if (!Grammar.EPSILON.equals(token) && followSets.get(symbol).add(token)) {
                                changed = true;
                            }
                        }

                        if (suffix.isEmpty() || firstOfSuffix.contains(Grammar.EPSILON)) {
                            if (followSets.get(symbol).addAll(followSets.get(nonTerminal))) {
                                changed = true;
                            }
                        }
                    }
                }
            }
        } while (changed);
    }

    private LinkedHashSet<String> firstOfSymbol(String symbol) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        if (Grammar.EPSILON.equals(symbol)) {
            result.add(Grammar.EPSILON);
        } else if (!grammar.hasNonTerminal(symbol)) {
            result.add(symbol);
        } else {
            result.addAll(firstSets.get(symbol));
        }
        return result;
    }

    private String formatSet(Set<String> values) {
        return "{ " + String.join(", ", values) + " }";
    }
}