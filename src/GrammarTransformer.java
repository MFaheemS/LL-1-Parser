import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GrammarTransformer {
    public Grammar applyLeftFactoring(Grammar source) {
        Grammar transformed = source.copy();
        boolean changed;

        do {
            changed = false;
            List<String> nonTerminals = transformed.getNonTerminalsInOrder();
            for (String nonTerminal : nonTerminals) {
                if (factorNonTerminal(transformed, nonTerminal)) {
                    changed = true;
                    break;
                }
            }
        } while (changed);

        return transformed;
    }

    public Grammar removeLeftRecursion(Grammar source) {
        Grammar transformed = source.copy();
        List<String> orderedNonTerminals = new ArrayList<>(transformed.getNonTerminalsInOrder());

        for (int i = 0; i < orderedNonTerminals.size(); i++) {
            String current = orderedNonTerminals.get(i);
            List<List<String>> currentAlternatives = deepCopy(transformed.getAlternatives(current));

            for (int j = 0; j < i; j++) {
                String previous = orderedNonTerminals.get(j);
                currentAlternatives = substituteLeadingNonTerminal(currentAlternatives, previous, transformed.getAlternatives(previous));
            }

            transformed.setAlternatives(current, currentAlternatives);
            String generated = eliminateDirectLeftRecursion(transformed, current);
            if (generated != null) {
                orderedNonTerminals.add(generated);
            }
        }

        return transformed;
    }

    private boolean factorNonTerminal(Grammar grammar, String nonTerminal) {
        List<List<String>> alternatives = deepCopy(grammar.getAlternatives(nonTerminal));
        PrefixMatch bestMatch = findBestPrefixMatch(alternatives);
        if (bestMatch == null) {
            return false;
        }

        String generated = grammar.nextGeneratedNonTerminal(nonTerminal);
        List<List<String>> remainingAlternatives = new ArrayList<>();
        List<List<String>> factoredAlternatives = new ArrayList<>();
        Set<Integer> groupedIndexes = new LinkedHashSet<>(bestMatch.indexes());

        for (int i = 0; i < alternatives.size(); i++) {
            List<String> alternative = alternatives.get(i);
            if (!groupedIndexes.contains(i)) {
                remainingAlternatives.add(alternative);
                continue;
            }

            List<String> suffix = alternative.subList(bestMatch.prefix().size(), alternative.size());
            if (suffix.isEmpty()) {
                factoredAlternatives.add(List.of(Grammar.EPSILON));
            } else {
                factoredAlternatives.add(new ArrayList<>(suffix));
            }
        }

        List<String> newAlternative = new ArrayList<>(bestMatch.prefix());
        newAlternative.add(generated);
        remainingAlternatives.add(newAlternative);

        grammar.setAlternatives(nonTerminal, remainingAlternatives);
        grammar.setAlternatives(generated, factoredAlternatives);
        return true;
    }

    private PrefixMatch findBestPrefixMatch(List<List<String>> alternatives) {
        PrefixMatch best = null;

        for (int i = 0; i < alternatives.size(); i++) {
            for (int j = i + 1; j < alternatives.size(); j++) {
                List<String> prefix = commonPrefix(alternatives.get(i), alternatives.get(j));
                if (prefix.isEmpty()) {
                    continue;
                }

                List<Integer> indexes = new ArrayList<>();
                for (int candidate = 0; candidate < alternatives.size(); candidate++) {
                    if (startsWith(alternatives.get(candidate), prefix)) {
                        indexes.add(candidate);
                    }
                }

                if (indexes.size() < 2) {
                    continue;
                }

                if (best == null
                    || prefix.size() > best.prefix().size()
                    || (prefix.size() == best.prefix().size() && indexes.size() > best.indexes().size())) {
                    best = new PrefixMatch(prefix, indexes);
                }
            }
        }

        return best;
    }

    private List<List<String>> substituteLeadingNonTerminal(
        List<List<String>> alternatives,
        String targetLeadingSymbol,
        List<List<String>> replacementAlternatives
    ) {
        List<List<String>> expanded = new ArrayList<>();

        for (List<String> alternative : alternatives) {
            if (alternative.isEmpty() || !alternative.get(0).equals(targetLeadingSymbol)) {
                expanded.add(new ArrayList<>(alternative));
                continue;
            }

            List<String> suffix = alternative.subList(1, alternative.size());
            for (List<String> replacement : replacementAlternatives) {
                List<String> newAlternative = new ArrayList<>();
                if (!(replacement.size() == 1 && Grammar.EPSILON.equals(replacement.get(0)))) {
                    newAlternative.addAll(replacement);
                }
                newAlternative.addAll(suffix);
                if (newAlternative.isEmpty()) {
                    newAlternative.add(Grammar.EPSILON);
                }
                expanded.add(newAlternative);
            }
        }

        return deduplicate(expanded);
    }

    private String eliminateDirectLeftRecursion(Grammar grammar, String nonTerminal) {
        List<List<String>> alternatives = deepCopy(grammar.getAlternatives(nonTerminal));
        List<List<String>> recursive = new ArrayList<>();
        List<List<String>> nonRecursive = new ArrayList<>();

        for (List<String> alternative : alternatives) {
            if (!alternative.isEmpty() && alternative.get(0).equals(nonTerminal)) {
                recursive.add(new ArrayList<>(alternative.subList(1, alternative.size())));
            } else {
                nonRecursive.add(alternative);
            }
        }

        if (recursive.isEmpty()) {
            return null;
        }

        String generated = grammar.nextGeneratedNonTerminal(nonTerminal);
        List<List<String>> rewrittenOriginal = new ArrayList<>();
        List<List<String>> rewrittenGenerated = new ArrayList<>();

        if (nonRecursive.isEmpty()) {
            List<String> bridge = new ArrayList<>();
            bridge.add(generated);
            rewrittenOriginal.add(bridge);
        }

        for (List<String> beta : nonRecursive) {
            List<String> candidate = new ArrayList<>();
            if (!(beta.size() == 1 && Grammar.EPSILON.equals(beta.get(0)))) {
                candidate.addAll(beta);
            }
            candidate.add(generated);
            rewrittenOriginal.add(candidate);
        }

        for (List<String> alpha : recursive) {
            List<String> candidate = new ArrayList<>();
            if (!(alpha.size() == 1 && Grammar.EPSILON.equals(alpha.get(0)))) {
                candidate.addAll(alpha);
            }
            candidate.add(generated);
            rewrittenGenerated.add(candidate);
        }
        rewrittenGenerated.add(List.of(Grammar.EPSILON));

        grammar.setAlternatives(nonTerminal, deduplicate(rewrittenOriginal));
        grammar.setAlternatives(generated, deduplicate(rewrittenGenerated));
        return generated;
    }

    private List<String> commonPrefix(List<String> left, List<String> right) {
        List<String> prefix = new ArrayList<>();
        int limit = Math.min(left.size(), right.size());
        for (int i = 0; i < limit; i++) {
            if (!left.get(i).equals(right.get(i))) {
                break;
            }
            if (Grammar.EPSILON.equals(left.get(i))) {
                break;
            }
            prefix.add(left.get(i));
        }
        return prefix;
    }

    private boolean startsWith(List<String> alternative, List<String> prefix) {
        if (alternative.size() < prefix.size()) {
            return false;
        }
        for (int i = 0; i < prefix.size(); i++) {
            if (!alternative.get(i).equals(prefix.get(i))) {
                return false;
            }
        }
        return true;
    }

    private List<List<String>> deduplicate(List<List<String>> alternatives) {
        Set<String> seen = new HashSet<>();
        List<List<String>> unique = new ArrayList<>();
        for (List<String> alternative : alternatives) {
            String key = String.join("\u0001", alternative);
            if (seen.add(key)) {
                unique.add(alternative);
            }
        }
        return unique;
    }

    private List<List<String>> deepCopy(List<List<String>> source) {
        List<List<String>> copy = new ArrayList<>();
        for (List<String> alternative : source) {
            copy.add(new ArrayList<>(alternative));
        }
        return copy;
    }

    private record PrefixMatch(List<String> prefix, List<Integer> indexes) {
    }
}