import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Tree {
    private final Node root;

    public Tree(String rootSymbol) {
        this.root = new Node(rootSymbol);
    }

    public Node getRoot() {
        return root;
    }

    // Keep a single parse-tree format for submission output.
    public String toAsciiTreeString() {
        StringBuilder builder = new StringBuilder();
        renderAsciiTree(root, builder, "", true);
        return builder.toString();
    }

    private void renderAsciiTree(Node node, StringBuilder builder, String prefix, boolean isLast) {
        builder.append(prefix);
        builder.append(isLast ? "`-- " : "|-- ");
        builder.append(node.getSymbol()).append(System.lineSeparator());

        List<Node> children = node.getChildren();
        for (int i = 0; i < children.size(); i++) {
            boolean last = (i == children.size() - 1);
            String extension = isLast ? "    " : "|   ";
            renderAsciiTree(children.get(i), builder, prefix + extension, last);
        }
    }

    public static class Node {
        private final String symbol;
        private final List<Node> children;

        public Node(String symbol) {
            this.symbol = symbol;
            this.children = new ArrayList<>();
        }

        public String getSymbol() {
            return symbol;
        }

        public List<Node> getChildren() {
            return Collections.unmodifiableList(children);
        }

        public Node addChild(String childSymbol) {
            Node child = new Node(childSymbol);
            children.add(child);
            return child;
        }

        public void addChild(Node child) {
            children.add(child);
        }
    }
}