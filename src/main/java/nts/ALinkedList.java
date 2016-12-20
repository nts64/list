package nts;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Extends ABasicList to provide a more useful AList implementation, by replacing recursive method implementations
 * with iteration. This makes the filter and map operations slower, as they have to reverse the result, but removes
 * the limit on the size.
 *
 * @param <T> The element type
 */
public class ALinkedList<T> extends ABasicList<T> {

    private ALinkedList(T head, AList<T> tail) {
        super(head, tail);
    }

    @Override
    public AList<T> filter(Predicate<? super T> predicate) {
        AList<T> list = this;
        AList<T> result = AList.empty();
        while (!list.isEmpty()) {
            T head = list.head();
            if (predicate.test(head)) {
                result = factory().create(head, result);
            }
            list = list.tail();
        }
        return result.reverse();
    }

    @Override
    public <R> AList<R> map(Function<? super T, ? extends R> function) {
        AList<T> list = this;
        AList<R> result = AList.empty();
        while (!list.isEmpty()) {
            R mapped = function.apply(list.head());
            result = factory().create(mapped, result);
            list = list.tail();
        }
        return result.reverse();
    }

    @Override
    public <A> A foldLeft(A initial, BiFunction<? super A, ? super T, ? extends A> function) {
        A accumulator = initial;
        AList<T> list = this;
        while (!list.isEmpty()) {
            accumulator = function.apply(accumulator, list.head());
            list = list.tail();
        }
        return accumulator;
    }

    @Override
    public AList.Factory factory() {
        return ALinkedList::new;
    }

    @SafeVarargs
    public static <T> AList<T> of(T... elements) {
        int c = elements.length;
        AList<T> list = AList.empty();
        while (c-- > 0) {
            list = new ALinkedList<>(elements[c], list);
        }
        return list;
    }
}
