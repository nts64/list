package nts;

import java.util.Arrays;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * As the name suggests, this is AList implementation that uses an array to hold the elements.
 * It is still effectively immutable, as the array is not exposed.
 * Unlike java.util.ArrayList, it does not provide an operation to get an element by index. The only extension to the
 * AList interface is a method to get a parallel version of this list, which uses the default fork/join pool to execute
 * map and filter concurrently, as they don't depend on the order of invocation.
 * The reverse operation does not actually reverse the elements in the array, it just switches the logical order.
 * The size is limited to the {@link #MAX_SIZE}.
 *
 * @param <T>
 */
@SuppressWarnings("unchecked")
public class AnArrayList<T> implements AList<T> {

    public static final int MAX_SIZE = 1 << 24;

    private final boolean parallel;
    private final Object[] elements;
    private final int headIndex; //inclusive
    private final int endIndex; //exclusive, last element is elements[endIndex - 1]
    private final boolean reversed;

    private AnArrayList(Object[] elements, boolean reversed, boolean parallel) {
        this(elements, 0, elements.length, reversed, parallel);
    }

    private AnArrayList(Object[] elements, int headIndex, int endIndex, boolean reversed, boolean parallel) {
        assert headIndex >= 0;
        assert endIndex <= elements.length;

        if (elements.length > MAX_SIZE) throw new AList.CreationFailed("Too big");
        this.elements = elements;
        this.headIndex = headIndex;
        this.endIndex = endIndex;
        this.reversed = reversed;
        this.parallel = parallel;
    }

    @Override
    public T head() {
        int index = reversed ? endIndex - 1 : headIndex;
        return (T) nullOut(elements[index]);
    }

    /**
     * This returns a new list object, pointing to the part of the array after the head (or before head if reversed).
     * Here we might get to the point where a very short list is holding a reference to a huge array.
     * If there are no more elements, the empty list is returned.
     *
     * @return a list of all elements after head.
     */
    @Override
    public AList<T> tail() {
        if (headIndex + 1 == endIndex) return AList.empty();
        if (reversed) {
            return new AnArrayList<>(elements, headIndex, endIndex - 1, true, parallel);
        }
        return new AnArrayList<>(elements, headIndex + 1, endIndex, false, parallel);
    }

    @Override
    public AnArrayList<T> reverse() {
        return new AnArrayList<>(elements, headIndex, endIndex, !reversed, parallel);
    }

    @Override
    public AList<T> filter(Predicate<? super T> predicate) {
        Object[] filtered = stream().filter(predicate).map(AnArrayList::nullIn).toArray();
        return filtered.length == 0 ? AList.empty() : new AnArrayList<>(filtered, reversed, parallel);
    }

    @Override
    public <R> AnArrayList<R> map(Function<? super T, ? extends R> function) {
        return new AnArrayList<>(stream().map(function).map(AnArrayList::nullIn).toArray(), reversed, parallel);
    }

    @Override
    public <A> A foldLeft(A initial, BiFunction<? super A, ? super T, ? extends A> function) {
        A accumulator = initial;
        if (reversed) {
            for (int i = endIndex - 1; i >= headIndex; i--) {
                accumulator = function.apply(accumulator, nullOut(elements[i]));
            }
            return accumulator;
        }
        for (int i = headIndex; i < endIndex; i++) {
            accumulator = function.apply(accumulator, nullOut(elements[i]));
        }
        return accumulator;
    }

    @Override
    public String toString() {
        return asString();
    }

    @Override
    public AList.Factory factory() {
        return AnArrayList::create;
    }

    private Stream<T> stream() {
        Stream<T> stream = Arrays.stream(elements, headIndex, endIndex).map(AnArrayList::nullOut);
        if (parallel) {
            stream = stream.parallel();
        }
        return stream;
    }

    /**
     * Create AnArrayList from head element and tail list.
     * If the tail is AnArrayList, its array can be re-used (provided there is free space).
     * Otherwise the tail's elements array is copied into a bigger array. This is potentially very slow,
     * especially if another thread is copying it, as the monitor on the array object is needed.
     * If the tail is another AList implementation, it's elements are added one by one to a new AnArrayList.
     *
     * @param head The first element in the new list.
     * @param tail The list of elements after the first.
     * @param <T> The type of the elements.
     * @return A new AnArrayList containing all the elements.
     */
    public static <T> AnArrayList<T> create(T head, AList<T> tail) {
        if (tail.isEmpty()) {
            return new AnArrayList<>(new Object[]{nullIn(head)}, false, false);
        }
        if (tail instanceof AnArrayList) {
            AnArrayList tailList = (AnArrayList) tail;
            if (tailList.reversed) {
                return insertAtEnd(head, tailList);
            }
            return insertAtStart(head, tailList);
        }
        return toAnArrayList(head, tail);
    }

    private static <T> AnArrayList<T> insertAtEnd(T head, AnArrayList<T> tail) {
        Object[] elements = tail.elements;
        int headIndex = tail.headIndex;
        int endIndex = tail.endIndex;
        //we need exclusive access to the array here
        synchronized (tail.elements) {
            if (endIndex == tail.elements.length || tail.elements[endIndex] != null) {//at array end or slot already used
                //copy the array portion into a new array with added free space
                elements = new Object[tail.newSize()];
                System.arraycopy(tail.elements, headIndex, elements, 0, tail.intSize());
                headIndex = 0;
                endIndex = tail.intSize();
            }
            elements[endIndex] = nullIn(head);
        }
        return new AnArrayList<>(elements, headIndex, endIndex + 1, true, tail.parallel);
    }

    private static <T> AnArrayList<T> insertAtStart(T head, AnArrayList<T> tail) {
        Object[] elements = tail.elements;
        int headIndex = tail.headIndex;
        int endIndex = tail.endIndex;
        //again we need exclusive access to the array here
        synchronized (tail.elements) {
            if (headIndex == 0 || tail.elements[headIndex - 1] != null) { //at array start or slot already used
                int increment = tail.newSize();
                elements = new Object[increment];
                System.arraycopy(tail.elements, headIndex, elements, increment - tail.intSize(), tail.intSize());
                headIndex = increment - tail.intSize();
                endIndex = elements.length;
            }
            elements[headIndex - 1] = nullIn(head);
        }
        return new AnArrayList<>(elements, headIndex - 1, endIndex, false, tail.parallel);
    }

    private int newSize() {
        if (size() == MAX_SIZE) throw new CreationFailed("Too big");
        long newSize = size()*6/5 + 10;
        if (newSize > MAX_SIZE) return MAX_SIZE;
        return (int)newSize;
    }

    private int intSize() {
        return endIndex - headIndex;
    }

    public long size() {
        return intSize();
    }

    public static <T> AList<T> of(T... elements) {
        if (elements.length == 0) return AList.empty();
        return new AnArrayList<>(Arrays.stream(elements).map(AnArrayList::nullIn).toArray(), false, false);
    }

    public static <T> AList<T> parallelOf(T... elements) {
        if (elements.length == 0) return AList.empty();
        return new AnArrayList<>(Arrays.stream(elements).map(AnArrayList::nullIn).toArray(), false, true);
    }

    private static <T> AnArrayList<T> toAnArrayList(T head, AList<T> list) {
        AnArrayList<T> result = create(head, AList.empty());
        while (!list.isEmpty()) {
            result = create(list.head(), result);
            list = list.tail();
        }
        return result.reverse();
    }

    private static Object nullIn(Object o) {
        return (o == null) ? NULL : o;
    }

    private static <T> T nullOut(Object o) {
        assert o != null;
        return o == NULL ? null : (T) o;
    }

    /*
     * used to distinguish between a free slot in the array and a null element in the list
     */
    private final static Object NULL = new Object();
}
