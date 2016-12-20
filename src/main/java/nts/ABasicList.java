package nts;

/**
 * A very simple AList implementation, using recursion.
 * As the stack is limited, the list size is also quite limited.
 *
 * @param <T> The element type.
 */
public class ABasicList<T> implements AList<T> {
    private final T head;
    private final AList<T> tail;
    private final long size;

    public ABasicList(T head, AList<T> tail) {
        this.head = head;
        this.tail = tail;
        this.size = tail.size() + 1;
    }

    @Override
    public T head() {
        return head;
    }

    @Override
    public AList<T> tail() {
        return tail;
    }

    @Override
    public long size() {
        return size;
    }

    @Override
    public String toString() {
        return asString();
    }

    @Override
    public AList.Factory factory() {
        return ABasicList::new;
    }

    /**
     * Utility factory method for creating a list from given elements.
     *
     * @param elements The elements of the list.
     * @param <T> The type of elements.
     * @return The created list.
     */
    @SafeVarargs
    public static <T> AList<T> of(T... elements) {
        int c = elements.length;
        AList<T> list = AList.empty();
        while (c-- > 0) {
            list = new ABasicList<>(elements[c], list);
        }
        return list;
    }
}
