import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Main {
    public static void main(String[] args) {
        String grammarPath = args.length > 0 ? args[0] : "input/grammar1.txt";
        String outputDirectory = args.length > 1 ? args[1] : "output";

        try {
            Path outputDir = Path.of(outputDirectory);
            Files.createDirectories(outputDir);

            Grammar originalGrammar = Grammar.fromFile(grammarPath);
            GrammarTransformer transformer = new GrammarTransformer();

            Grammar factoredGrammar = transformer.applyLeftFactoring(originalGrammar);
            Grammar transformedGrammar = transformer.removeLeftRecursion(factoredGrammar);

            FirstFollow firstFollow = new FirstFollow(transformedGrammar);
            firstFollow.compute();

            ParsingTable parsingTable = new ParsingTable(transformedGrammar, firstFollow);
            parsingTable.build();

            String transformedText = "Original Grammar" + System.lineSeparator()
                + originalGrammar.toFormattedString() + System.lineSeparator() + System.lineSeparator()
                + "After Left Factoring" + System.lineSeparator()
                + factoredGrammar.toFormattedString() + System.lineSeparator() + System.lineSeparator()
                + "After Left Recursion Removal" + System.lineSeparator()
                + transformedGrammar.toFormattedString() + System.lineSeparator();

            writeFile(outputDir.resolve("grammar_transformed.txt"), transformedText);
            writeFile(outputDir.resolve("first_follow_sets.txt"), firstFollow.formatSets());
            writeFile(outputDir.resolve("parsing_table.txt"), parsingTable.formatTable());

            if (args.length > 2) {
                if (!parsingTable.isLl1()) {
                    System.out.println("Skipping Part 2 parsing because the transformed grammar is not LL(1).");
                } else {
                    Parser parser = new Parser(transformedGrammar, firstFollow, parsingTable);
                    int traceNumber = 1;
                    StringBuilder acceptedTrees = new StringBuilder();

                    for (int index = 2; index < args.length; index++) {
                        Parser.ParseFileResult parseFileResult = parser.parseInputFile(args[index]);
                        String traceReport = parseFileResult.formatTraceReport();
                        writeFile(outputDir.resolve("parsing_trace" + traceNumber + ".txt"), traceReport);
                        System.out.println(traceReport);

                        if (acceptedTrees.length() > 0) {
                            acceptedTrees.append(System.lineSeparator());
                            acceptedTrees.append("=".repeat(100)).append(System.lineSeparator()).append(System.lineSeparator());
                        }
                        acceptedTrees.append(parseFileResult.formatAcceptedTrees());
                        traceNumber++;
                    }

                    writeFile(outputDir.resolve("parse_trees.txt"), acceptedTrees.toString());
                }
            }

            System.out.println("Grammar loaded from: " + grammarPath);
            System.out.println();
            System.out.println(transformedText);
            System.out.println(firstFollow.formatSets());
            System.out.println(parsingTable.formatTable());
        } catch (Exception exception) {
            System.err.println("Failed to process grammar: " + exception.getMessage());
        }
    }

    private static void writeFile(Path path, String content) throws IOException {
        Files.writeString(path, content);
    }
}