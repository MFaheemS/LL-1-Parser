# LL(1) Parser - README

## Team Members with Roll Numbers
- Member 1: Faheem - 23i-0728
- Member 2: Humayun - 23i-0832

## Programming Language Used
Java

## Compilation Instructions (Java)
From project root, run:

```powershell
build.bat
```

Equivalent manual command:

```powershell
javac -d out src\*.java
```

## Execution Instructions with Examples
General format:

```powershell
java -cp out Main <grammar-file> <output-folder> [input-file-1] [input-file-2] ...
```

Examples:

```powershell
java -cp out Main input/grammar1.txt output input/input_grammar1_simple_valid_invalid_missing_extra_empty.txt
```

```powershell
java -cp out Main input/grammar2.txt output input/input_grammar2_expression_valid_invalid_missing_extra_empty_error_recovery.txt
```

```powershell
java -cp out Main input/grammar3.txt output input/input_grammar3_statement_left_factoring_valid_invalid_missing_extra.txt
```

```powershell
java -cp out Main input/grammar4.txt output input/input_grammar4_indirect_left_recursion_valid_invalid_missing_extra.txt
```

Or run the demo script:

```powershell
demo.bat
```

## Input File Format Specification
- Grammar files are plain text files in input/.
- Input string files are plain text files in input/.
- One logical unit per line.
- Blank lines are allowed in input string files.

## Grammar File Format
Each production must be written on one line:

```text
NonTerminal -> alternative1 | alternative2 | alternative3
```

Rules:
- Non-terminals start with an uppercase letter.
- Symbols on the right-hand side are space-separated.
- Epsilon can be written as epsilon or @.
- Epsilon must appear alone in an alternative.

Example:

```text
Expr -> Expr + Term | Term
Term -> Term * Factor | Factor
Factor -> ( Expr ) | id
```

## Input String File Format
- One input string per line.
- Tokens must be separated by spaces.
- Tokens must belong to the grammar terminals.

Example:

```text
id + id * id
( id + id ) * id
id * id + id
```

## Sample Grammar and Input Files Explanation
- input/grammar1.txt: simple grammar with epsilon production.
- input/grammar2.txt: expression grammar (requires left recursion removal).
- input/grammar3.txt: statement grammar (requires left factoring).
- input/grammar4.txt: indirect left recursion grammar.

- input/input_grammar1_simple_valid_invalid_missing_extra_empty.txt: 7 strings for Grammar 1 (valid + invalid + missing/extra + empty case).
- input/input_grammar2_expression_valid_invalid_missing_extra_empty_error_recovery.txt: 11 strings for Grammar 2 (valid + syntax errors + missing/extra symbols + empty case + error recovery).
- input/input_grammar3_statement_left_factoring_valid_invalid_missing_extra.txt: 9 strings for Grammar 3 (valid + invalid + missing/extra symbols).
- input/input_grammar4_indirect_left_recursion_valid_invalid_missing_extra.txt: 10 strings for Grammar 4 indirect left recursion coverage (valid + invalid + missing/extra).

Coverage notes:
- At least 3 different grammars tested (4 included).
- At least 5 input strings per grammar.
- Both valid and invalid strings included.
- Left recursion removal coverage via grammar2.
- Left factoring coverage via grammar3.
- Indirect left recursion test cases included via grammar4/input_grammar4_indirect_left_recursion_valid_invalid_missing_extra.
- Error recovery demonstrated with malformed lines in grammar1 and grammar2 test files.

Output files generated in output/:
- grammar_transformed.txt
- first_follow_sets.txt
- parsing_table.txt
- parsing_trace1.txt
- parsing_trace2.txt
- parse_trees.txt (ASCII parse trees for accepted strings)

## Known Limitations (if any)
- Parsing runs only if the transformed grammar is LL(1).
- Input tokens must be pre-tokenized (space-separated).
- Parse trees are generated only for accepted strings.
- Panic-mode recovery may continue after errors, but recovered lines are not treated as accepted.
