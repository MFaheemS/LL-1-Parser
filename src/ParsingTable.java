import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ParsingTable {
    private final Grammar grammar;
    private final FirstFollow firstFollow;
    private final LinkedHashMap<String, LinkedHashMap<String, List<String>>> table;
    private final List<String> terminals;
    private final List<String> conflicts;
    private boolean ll1;

    public ParsingTable(Grammar grammar, FirstFollow firstFollow) {
        this.grammar = grammar;
        this.firstFollow = firstFollow;
        this.table = new LinkedHashMap<>();
        this.terminals = new ArrayList<>(grammar.getTerminals());
        this.terminals.add(FirstFollow.END_MARKER);
        this.conflicts = new ArrayList<>();
        this.ll1 = true;

        for (String nonTerminal : grammar.getNonTerminalsInOrder()) {
            LinkedHashMap<String, List<String>> row = new LinkedHashMap<>();
            for (String terminal : terminals) {
                row.put(terminal, null);
            }
            table.put(nonTerminal, row);
        }
    }

    public void build() {
        for (String nonTerminal : grammar.getNonTerminalsInOrder()) {
            for (List<String> alternative : grammar.getAlternatives(nonTerminal)) {
                LinkedHashSet<String> first = firstFollow.firstOfSequence(alternative);
                for (String terminal : first) {
                    if (!Grammar.EPSILON.equals(terminal)) {
                        place(nonTerminal, terminal, alternative);
                    }
                }

                if (first.contains(Grammar.EPSILON)) {
                    for (String followSymbol : firstFollow.getFollowSets().get(nonTerminal)) {
                        place(nonTerminal, followSymbol, alternative);
                    }
                }
            }
        }
    }

    public boolean isLl1() {
        return ll1;
    }

    public Map<String, LinkedHashMap<String, List<String>>> getTable() {
        return Collections.unmodifiableMap(table);
    }

    public List<String> getConflicts() {
        return Collections.unmodifiableList(conflicts);
    }

    public String formatTable() {
        StringBuilder builder = new StringBuilder();
        builder.append("LL(1) Parsing Table").append(System.lineSeparator());
        builder.append(String.format("%-18s", "NonTerminal"));
        for (String terminal : terminals) {
            builder.append(String.format("| %-24s", terminal));
        }
        builder.append(System.lineSeparator());

        int width = 18 + terminals.size() * 27;
        builder.append("-".repeat(Math.max(18, width))).append(System.lineSeparator());

        for (String nonTerminal : grammar.getNonTerminalsInOrder()) {
            builder.append(String.format("%-18s", nonTerminal));
            for (String terminal : terminals) {
                List<String> production = table.get(nonTerminal).get(terminal);
                String rendered = production == null ? "" : nonTerminal + " -> " + Grammar.renderAlternative(production);
                builder.append(String.format("| %-24s", rendered));
            }
            builder.append(System.lineSeparator());
        }

        builder.append(System.lineSeparator());
        builder.append("Grammar status: ").append(ll1 ? "LL(1)" : "Not LL(1)").append(System.lineSeparator());
        if (!conflicts.isEmpty()) {
            builder.append("Conflicts:").append(System.lineSeparator());
            for (String conflict : conflicts) {
                builder.append("- ").append(conflict).append(System.lineSeparator());
            }
        }

        return builder.toString();
    }

    private void place(String nonTerminal, String terminal, List<String> alternative) {
        List<String> existing = table.get(nonTerminal).get(terminal);
        if (existing != null && !existing.equals(alternative)) {
            ll1 = false;
            conflicts.add(
                "Conflict at M[" + nonTerminal + ", " + terminal + "]: "
                    + Grammar.renderAlternative(existing) + " vs " + Grammar.renderAlternative(alternative)
            );
            return;
        }
        table.get(nonTerminal).put(terminal, new ArrayList<>(alternative));
    }
}