import java.util.ArrayList;
import java.util.Collections;
import java.util.EmptyStackException;
import java.util.List;

public class Stack<T> {
    private final ArrayList<T> elements;

    public Stack() {
        this.elements = new ArrayList<>();
    }

    public void push(T value) {
        elements.add(value);
    }

    public T pop() {
        if (elements.isEmpty()) {
            throw new EmptyStackException();
        }
        return elements.remove(elements.size() - 1);
    }

    public T top() {
        if (elements.isEmpty()) {
            throw new EmptyStackException();
        }
        return elements.get(elements.size() - 1);
    }

    public boolean isEmpty() {
        return elements.isEmpty();
    }

    public int size() {
        return elements.size();
    }

    public void clear() {
        elements.clear();
    }

    public List<T> toBottomToTopList() {
        return Collections.unmodifiableList(new ArrayList<>(elements));
    }
}