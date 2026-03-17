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

    public String toIndentedString() {
        StringBuilder builder = new StringBuilder();
        render(root, builder, 0);
        return builder.toString();
    }

    private void render(Node node, StringBuilder builder, int depth) {
        builder.append("  ".repeat(depth))
            .append(node.getSymbol())
            .append(System.lineSeparator());

        for (Node child : node.getChildren()) {
            render(child, builder, depth + 1);
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