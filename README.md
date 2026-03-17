# LL(1) Parser Design and Implementation

## Team Members

- Add member 1 name and roll number
- Add member 2 name and roll number

## Programming Language

Java

## Project Overview

This project implements an LL(1) parser assignment in Java.

It supports:

- Reading a CFG from a text file
- Left factoring
- Direct and indirect left recursion removal
- FIRST set computation
- FOLLOW set computation
- LL(1) parsing table construction
- Stack-based predictive parsing
- Error detection and panic-mode recovery
- Parse tree generation for accepted strings

## Folder Structure

```text
LL-1-Parser/
├── build.bat
├── docs/
├── input/
│   ├── grammar1.txt
│   ├── grammar2.txt
│   ├── grammar3.txt
│   ├── grammar4.txt
│   ├── input_valid.txt
│   ├── input_errors.txt
│   └── input_edge_cases.txt
├── out/
├── output/
└── src/
    ├── ErrorHandler.java
    ├── FirstFollow.java
    ├── Grammar.java
    ├── GrammarTransformer.java
    ├── Main.java
    ├── Parser.java
    ├── ParsingTable.java
    ├── Stack.java
    └── Tree.java
```

## Compilation Instructions

Open PowerShell in the project folder and run:

```powershell
build.bat
```

For a one-click demo on Windows, you can also double-click `demo.bat`.

Equivalent manual command:

```powershell
javac -d out src\*.java
```

## Execution Instructions

### Part 1 Only

```powershell
java -cp out Main input/grammar1.txt output
```

### Part 2 With Input Strings

```powershell
java -cp out Main input/grammar2.txt output input/input_valid.txt input/input_errors.txt input/input_edge_cases.txt
```

### One-Click Demo

```powershell
demo.bat
```

This builds the project and runs the expression-grammar demo using `input_valid.txt` and `input_errors.txt`.

### General Format

```powershell
java -cp out Main <grammar-file> <output-folder> [input-file-1] [input-file-2] ...
```

If input files are provided and the transformed grammar is LL(1), the program also writes parsing traces and parse trees.

## Grammar File Format

Each grammar rule must appear on a separate line in this format:

```text
NonTerminal -> production1 | production2 | production3
```

Example:

```text
Expr -> Expr + Term | Term
Term -> Term * Factor | Factor
Factor -> ( Expr ) | id
```

Rules:

- Non-terminals must start with an uppercase letter
- Multi-character non-terminals are supported
- Symbols on the right-hand side must be space-separated
- Epsilon can be written as `epsilon` or `@`
- Epsilon must appear alone in an alternative

## Input String File Format

- Each line contains one input string
- Tokens must be separated by spaces
- Tokens must belong to the grammar terminals

Example:

```text
id + id * id
( id + id ) * id
id * id + id
```

## Sample Grammar Files

- `input/grammar1.txt`: simple grammar with epsilon production
- `input/grammar2.txt`: expression grammar used for predictive parsing traces
- `input/grammar3.txt`: statement grammar that requires left factoring and still reports an LL(1) conflict because of dangling else
- `input/grammar4.txt`: grammar with indirect left recursion

## Sample Input Files

- `input/input_valid.txt`: valid input strings for the expression grammar
- `input/input_errors.txt`: invalid strings to demonstrate syntax error handling and recovery
- `input/input_edge_cases.txt`: empty input, missing symbols, repeated operators, and nested expressions

These sample input files are intended for `input/grammar2.txt`.

## Output Files

The program writes these files in the chosen output folder:

- `grammar_transformed.txt`
- `first_follow_sets.txt`
- `parsing_table.txt`
- `parsing_trace1.txt`
- `parsing_trace2.txt`
- `parse_trees.txt`

Additional parsing trace files may be produced if more input files are supplied.

## Known Limitations

- Parsing is only performed when the transformed grammar is LL(1)
- Input tokens must already be space-separated
- Parse trees are generated only for accepted strings
- The parser uses panic-mode recovery and may continue after errors, but recovered lines are not treated as clean accepted strings for tree output