package nts;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Generic immutable list interface.
 * <br/>
 * Provides methods to get the first (head) and the rest (tail) of the elements and a factory to create a new list from
 * a head element and a tail list.
 * Also provides the empty list implementation.
 *
 * Using those operations and the empty list allows us to recursively define a set of other functions - map, filter,
 * foldLeft, reverse and size.
 *
 * @param <T> the type of elements
 */
public interface AList<T> {

    /**
     * Get the first element. This method should return null if either the first element is null or the list is empty.
     * To test for emptiness, the {@link #isEmpty()} method should be used.
     *
     * @return The first element, <code>null</code> if list is empty.
     */
    T head();

    /**
     * @return The list of elements after the list.
     */
    AList<T> tail();

    /**
     * Provides a way to create instances of this list from a head element and a tail list.
     *
     * @return AListFactory for the list.
     */
    Factory factory();

    /**
     * This method should return false for all but the empty list.
     *
     * @return <code>true</code> if no elements in the list.
     */
    default boolean isEmpty() {
        return false;
    }

    /**
     * Get a list of the elements in this list that match the predicate, preserving the order.
     *
     * @param predicate Function to test each element.
     * @return AList of matching elements.
     */
    default AList<T> filter(Predicate<? super T> predicate){
        return predicate.test(head()) ? factory().create(head(), tail().filter(predicate)) : tail().filter(predicate);
    }

    /**
     * Get a list of elements, created by applying a mapping function to each element of this list.
     *
     * @param function A mapping function from the type of this list to the type of the new list.
     * @param <R> The result type.
     * @return A list of the mapped elements, in the same order.
     */
    default <R> AList<R> map(Function<? super T, ? extends R> function) {
        return factory().create(function.apply(head()), tail().map(function));
    }

    /**
     * Get the accumulated value of applying a function to each element, starting from the first.
     * The function return value is used as an argument when calling the function with the next element.
     * As the name suggests, the operation 'folds' the list into a single value, starting from the left (first element).
     * To get the semantics of a foldRight operation, reverse and then foldLeft.
     *
     * @param initial The initial value of the accumulator.
     * @param function A function accepting the current accumulator value and list element,
     *                 returning the next value of the accumulator.
     * @param <A> The type of the accumulated value.
     * @return The accumulated value, after applying the function to all elements.
     */
    default <A> A foldLeft(A initial, BiFunction<? super A, ? super T, ? extends A> function) {
        return tail().foldLeft(function.apply(initial, head()), function);
    }

    /**
     * @return A list of the elements in reverse order.
     */
    default AList<T> reverse(){
        return foldLeft(empty(), (a, x) -> factory().create(x, a));
    }

    /**
     * @return The list as a string.
     */
    default String asString() {
        StringBuilder sb = foldLeft(new StringBuilder("("), (a, x) -> a.append(x).append(", "));
        return sb.delete(sb.length() - 2, sb.length()).append(')').toString();
    }

    /**
     * @return The size of this list.
     */
    default long size() {
        return foldLeft(0, (count, e) -> count++);
    }

    /**
     * Utility method to type-case the empty list as it is an instance of <code>AList&lt;T&gt;</code> for any T.
     *
     * @param <T> The desired type.
     * @return The empty list.
     */
    @SuppressWarnings("unchecked")
    static <T> AList<T> empty() {
        return EMPTY;
    }

    /**
     * The empty list instance. Implementations of AList should always use it to represent an empty list.
     */
    AList EMPTY = new AList() {
        @Override
        public Object head() {
            return null;
        }

        @Override
        public AList tail() {
            return this;
        }

        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public AList reverse() {
            return this;
        }

        @Override
        public AList filter(Predicate predicate) {
            return this;
        }

        @Override
        public String asString() {
            return "()";
        }

        @Override
        public Factory factory() {
            return new Factory() {
                @Override
                public <E> AList<E> create(E head, AList<E> tail) {
                    if (tail != EMPTY) return tail.factory().create(head, tail);
                    throw new CreationFailed("Empty list and tail, cannot determine type");
                }
            };
        }

        @Override
        public AList map(Function function) {
            return this;
        }

        @Override
        public Object foldLeft(Object initial, BiFunction function) {
            return initial;
        }

        @Override
        public String toString() {
            return asString();
        }
    };

    /**
     * Thrown when a list cannot be created.
     */
    class CreationFailed extends RuntimeException {
        CreationFailed(String message) {
            super(message);
        }
    }

    /**
     * AList creation factory.
     */
    @FunctionalInterface
    interface Factory {
        /**
         * Create a new list from given head and tail.
         *
         * @param head The element to be the head of the new list.
         * @param tail The list to be the tail of the new list.
         * @param <T> The type of the elements.
         * @return The new list instance.
         * @throws AList.CreationFailed If list could not be created.
         */
        <T> AList<T> create(T head, AList<T> tail) throws AList.CreationFailed;
    }
}
