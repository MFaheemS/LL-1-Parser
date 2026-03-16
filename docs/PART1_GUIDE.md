# LL(1) Parser Assignment Part 1 Guide

## What This Project Does

This Java project implements only **Part 1** of the assignment.

It performs these steps:

1. Reads a CFG from a text file.
2. Stores productions in a reusable grammar structure.
3. Applies **left factoring**.
4. Removes **indirect and direct left recursion**.
5. Computes **FIRST** sets.
6. Computes **FOLLOW** sets.
7. Builds the **LL(1) parsing table**.
8. Reports whether the grammar is LL(1) or not.

It does **not** do Part 2 parsing, stack tracing, error recovery during parsing, or parse tree generation.

## Folder Structure

```text
CC 2/
├── docs/
│   └── PART1_GUIDE.md
├── input/
│   ├── grammar1.txt
│   ├── grammar2.txt
│   ├── grammar3.txt
│   └── grammar4.txt
├── output/
│   ├── first_follow_sets.txt
│   ├── grammar_transformed.txt
│   └── parsing_table.txt
├── out/
│   └── compiled .class files
└── src/
    ├── FirstFollow.java
    ├── Grammar.java
    ├── GrammarTransformer.java
    ├── Main.java
    └── ParsingTable.java
```

## How To Compile

Open PowerShell in the project folder and run:

```powershell
javac -d out src\*.java
```

### What This Does

- `javac` compiles the Java files.
- `-d out` puts the compiled `.class` files inside the `out` folder.

If compilation succeeds, there will be no error message.

## How To Run

### Default Run

```powershell
java -cp out Main input/grammar1.txt output
```

### Format

```powershell
java -cp out Main <grammar-file> <output-folder>
```

### Examples

```powershell
java -cp out Main input/grammar1.txt output
java -cp out Main input/grammar2.txt output
java -cp out Main input/grammar3.txt output
java -cp out Main input/grammar4.txt output
```

### What Happens When You Run It

The program:

1. Reads the grammar file.
2. Prints the original grammar.
3. Prints the grammar after left factoring.
4. Prints the grammar after left recursion removal.
5. Prints FIRST sets.
6. Prints FOLLOW sets.
7. Prints the LL(1) parsing table.
8. Writes the main outputs into the `output` folder.

## Grammar Input Format

Each production must be on one line.

### Required format

```text
NonTerminal -> production1 | production2 | production3
```

### Examples

```text
Expr -> Expr + Term | Term
Term -> Term * Factor | Factor
Factor -> ( Expr ) | id
```

### Important Notes

- Non-terminals should start with an uppercase letter.
- Multi-character non-terminals are supported, for example `Expr`, `TermPrime`, `Stmt`.
- Terminals are stored exactly as written, for example `id`, `if`, `then`, `+`, `*`, `(`, `)`.
- Symbols in the right-hand side should be separated by spaces.
- Epsilon can be written as `epsilon` or `@`.
- Epsilon must appear alone in an alternative.

## Output Files

### `output/grammar_transformed.txt`

This file contains:

- Original grammar
- Grammar after left factoring
- Grammar after left recursion removal

### `output/first_follow_sets.txt`

This file contains:

- FIRST set of every non-terminal
- FOLLOW set of every non-terminal

### `output/parsing_table.txt`

This file contains:

- The LL(1) parsing table
- Grammar status: `LL(1)` or `Not LL(1)`
- Any table conflicts

## File-by-File Explanation

## `src/Main.java`

This is the entry point of the program.

### Responsibilities

- Reads command-line arguments.
- Loads the input grammar file.
- Calls left factoring.
- Calls left recursion removal.
- Calls FIRST/FOLLOW computation.
- Calls parsing table construction.
- Prints results to the console.
- Writes results to output text files.

### Flow inside `main`

1. Load grammar using `Grammar.fromFile(...)`
2. Create `GrammarTransformer`
3. Apply left factoring
4. Remove left recursion
5. Create `FirstFollow`
6. Compute FIRST and FOLLOW
7. Create `ParsingTable`
8. Build the table
9. Save output files

This file controls the full Part 1 pipeline.

## `src/Grammar.java`

This file defines the grammar data structure and input handling.

### Responsibilities

- Reads grammar lines from file.
- Validates rule format.
- Stores productions.
- Stores non-terminals.
- Detects terminals automatically.
- Keeps track of the start symbol.
- Supports copying the grammar for transformations.
- Prints grammar in a readable format.

### Important Data Stored

- `productions`: maps each non-terminal to its list of alternatives
- `nonTerminals`: ordered set of grammar non-terminals
- `terminals`: ordered set of grammar terminals
- `startSymbol`: first non-terminal read from the file

### Important Methods

- `fromFile(...)`: reads the grammar file and builds the grammar object
- `addProduction(...)`: adds a new production
- `setAlternatives(...)`: replaces all alternatives of one non-terminal
- `getAlternatives(...)`: returns alternatives for a non-terminal
- `getNonTerminalsInOrder()`: preserves grammar order
- `nextGeneratedNonTerminal(...)`: creates names like `ExprPrime`, `ExprPrime2`
- `toFormattedString()`: prints the grammar cleanly

### Why This File Matters

Every other file depends on this class. It is the base representation of the grammar used in all later steps.

## `src/GrammarTransformer.java`

This file performs the grammar transformations required to make the grammar more suitable for LL(1) parsing.

### Responsibilities

- Applies left factoring
- Removes indirect left recursion
- Removes direct left recursion

### Left Factoring Logic

The method `applyLeftFactoring(...)` repeatedly checks each non-terminal for alternatives with common prefixes.

Example:

```text
Stmt -> if Cond then Stmt | if Cond then Stmt else Stmt | a
```

becomes:

```text
Stmt -> a | if Cond then Stmt StmtPrime
StmtPrime -> epsilon | else Stmt
```

It does this by:

1. Finding the best common prefix.
2. Creating a new generated non-terminal.
3. Moving the suffixes into that new non-terminal.
4. Repeating until no more factoring is needed.

### Left Recursion Removal Logic

The method `removeLeftRecursion(...)` follows the standard algorithm:

1. Process non-terminals in order.
2. For each `Ai`, replace productions that begin with an earlier non-terminal `Aj`.
3. After substitution, remove direct left recursion from `Ai`.

### Direct Left Recursion Example

```text
Expr -> Expr + Term | Term
```

becomes:

```text
Expr -> Term ExprPrime
ExprPrime -> + Term ExprPrime | epsilon
```

### Important Methods

- `applyLeftFactoring(...)`
- `removeLeftRecursion(...)`
- `factorNonTerminal(...)`
- `substituteLeadingNonTerminal(...)`
- `eliminateDirectLeftRecursion(...)`

### Why This File Matters

FIRST, FOLLOW, and LL(1) table construction should be done on the transformed grammar, not the raw left-recursive one.

## `src/FirstFollow.java`

This file computes FIRST and FOLLOW sets.

### Responsibilities

- Computes FIRST for each non-terminal
- Computes FOLLOW for each non-terminal
- Computes FIRST of a full RHS sequence
- Formats the sets for output

### FIRST Computation

The method `computeFirstSets()` uses repeated passes until no new symbols are added.

For each production `A -> Y1 Y2 ... Yn`:

1. Add `FIRST(Y1) - epsilon`
2. If `Y1` can derive epsilon, continue to `Y2`
3. Continue until a symbol cannot derive epsilon
4. If all symbols can derive epsilon, add epsilon to `FIRST(A)`

### FOLLOW Computation

The method `computeFollowSets()` also uses repeated passes.

Rules used:

1. Put `$` in FOLLOW(start symbol)
2. For `A -> alpha B beta`, add `FIRST(beta) - epsilon` to FOLLOW(B)
3. If `beta` can derive epsilon, add FOLLOW(A) to FOLLOW(B)
4. If `B` is at the end, add FOLLOW(A) to FOLLOW(B)

### Important Methods

- `compute()`
- `computeFirstSets()`
- `computeFollowSets()`
- `firstOfSequence(...)`
- `formatSets()`

### Why This File Matters

The parsing table depends completely on correct FIRST and FOLLOW computation.

## `src/ParsingTable.java`

This file builds the LL(1) predictive parsing table.

### Responsibilities

- Creates the matrix of non-terminals vs terminals
- Inserts productions into the correct cells
- Detects conflicts
- Reports whether the grammar is LL(1)
- Formats the table for output

### Table Construction Logic

For every production `A -> alpha`:

1. For each terminal `a` in `FIRST(alpha) - epsilon`, place `A -> alpha` in `M[A, a]`
2. If epsilon is in `FIRST(alpha)`, then for each `b` in `FOLLOW(A)`, place `A -> alpha` in `M[A, b]`

### Conflict Detection

If a table cell already contains a different production, that means the grammar is not LL(1).

Example conflict message:

```text
Conflict at M[StmtPrime, else]: epsilon vs else Stmt
```

### Important Methods

- `build()`
- `place(...)`
- `formatTable()`
- `isLl1()`

### Why This File Matters

This is the final result of Part 1. It tells you whether the grammar can be parsed using an LL(1) predictive parser.

## Input File Explanation

## `input/grammar1.txt`

Expression grammar with direct left recursion.

Purpose:

- Tests direct left recursion removal
- Tests FIRST/FOLLOW on expressions
- Produces a valid LL(1) transformed grammar

## `input/grammar2.txt`

Simple grammar with epsilon.

Purpose:

- Tests epsilon handling
- Tests FIRST/FOLLOW correctness

## `input/grammar3.txt`

Statement grammar with common prefix.

Purpose:

- Tests left factoring
- Shows a grammar that still becomes non-LL(1) because of dangling else style ambiguity

## `input/grammar4.txt`

Grammar with indirect left recursion.

Purpose:

- Tests substitution of earlier non-terminals
- Tests direct + indirect left recursion handling
- Shows a grammar that may still not be LL(1)

## Typical Demo Explanation

If your instructor asks what happens internally, explain it in this order:

1. The grammar is read and stored as ordered productions.
2. Left factoring removes common prefixes.
3. Left recursion removal rewrites recursive productions using a generated prime non-terminal.
4. FIRST sets are computed by fixed-point iteration.
5. FOLLOW sets are computed by fixed-point iteration.
6. The LL(1) table is filled using FIRST and FOLLOW.
7. If any table cell gets two different productions, the grammar is not LL(1).

## How To Explain The Program In Simple Words

You can describe the project like this:

> This program reads a grammar from a file, transforms it into a form suitable for predictive parsing, computes FIRST and FOLLOW sets, and then builds the LL(1) parsing table. If any table entry has more than one production, the grammar is reported as not LL(1).

## Current Limitations

- This project implements only Part 1.
- It does not parse input strings.
- It does not generate parse trees.
- It expects the grammar file to use spaces between symbols on the right-hand side.
- It assumes non-terminals begin with uppercase letters.

## Quick Run Checklist

1. Open PowerShell in the project folder.
2. Run `javac -d out src\*.java`
3. Run `java -cp out Main input/grammar1.txt output`
4. Check the console output.
5. Check the generated files inside `output`.

## Suggested Submission Use

You can use this project as your Part 1 implementation and adapt this guide into:

- `README.md`
- report explanation sections
- viva preparation notes
