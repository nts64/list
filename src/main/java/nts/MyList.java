package nts;

import java.util.Arrays;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * AList implementation, that is composed of chunks or sub-lists.
 *
 * @param <T> The element type.
 */
public class MyList<T> implements AList<T> {
    /**
     * The default size of the sub-lists or chunks.
     */
    public static final int DEFAULT_CHUNK_SIZE = 1_000_000;

    private final AList<AList<T>> chunks;
    private final int chunkSize;

    private MyList(AList<AList<T>> chunks) {
        this(chunks, DEFAULT_CHUNK_SIZE);
    }

    private MyList(AList<AList<T>> chunks, int chunkSize) {
        assert !chunks.isEmpty();

        this.chunks = chunks;
        this.chunkSize = chunkSize;
    }

    @Override
    public T head() {
        return chunks.head().head();
    }

    @Override
    public AList<T> tail() {
        AList<T> headChunk = chunks.head();
        AList<T> headChunkTail = headChunk.tail();
        if (headChunkTail.isEmpty()) {
            if (chunks.tail().isEmpty()) {
                return AList.empty();
            }
            return new MyList<>(chunks.tail());
        }
        return new MyList<>(AnArrayList.create(headChunkTail, chunks.tail()));
    }

    @Override
    public MyList<T> reverse() {
        AList<AList<T>> reversedChunks = chunks.map(AList::reverse).reverse();
        return new MyList<>(reversedChunks);
    }

    @Override
    public AList<T> filter(Predicate<? super T> predicate) {
        AList<AList<T>> filteredChunks = chunks.map(chunk -> chunk.filter(predicate)).filter(chunks -> !chunks.isEmpty());
        return filteredChunks.isEmpty()? AList.empty() : new MyList<>(filteredChunks);
    }

    @Override
    public <R> MyList<R> map(Function<? super T, ? extends R> function) {
        AList<AList<R>> mappedChunks = chunks.map(chunk -> chunk.map(function));
        return new MyList<>(mappedChunks);
    }

    @Override
    public <A> A foldLeft(A initial, BiFunction<? super A, ? super T, ? extends A> function) {
        return chunks.foldLeft(initial, (a, chunk) -> chunk.foldLeft(a, function));
    }

    @Override
    public long size() {
        return chunks.foldLeft(0L, (size, chunk) -> size + chunk.size());
    }

    @Override
    public String toString() {
        return asString();
    }

    @Override
    public AList.Factory factory() {
        return MyList::create;
    }

    public static <T> MyList<T> create(T head, AList<T> tail) {
        if (tail.isEmpty()) {
            AList<T> headChunk = AnArrayList.create(head, AList.empty());
            return new MyList<>(AnArrayList.create(headChunk, AList.empty()));
        }
        if (tail instanceof AnArrayList) {
            if (tail.size() >= DEFAULT_CHUNK_SIZE) {
                AnArrayList<T> headChunk = AnArrayList.create(head, AList.empty());
                AList<AList<T>> tailChunks = AnArrayList.create(tail, AList.empty());
                AList<AList<T>> chunks = AnArrayList.create(headChunk, tailChunks);
                return new MyList<>(chunks);
            }
            AnArrayList<T> headChunk = AnArrayList.create(head, tail);
            return new MyList<>(AnArrayList.create(headChunk, AList.empty()));
        }
        if (tail instanceof MyList) {
            MyList<T> tailList = (MyList<T>) tail;
            if (tailList.chunks.head().size() >= tailList.chunkSize) {
                AnArrayList<T> headChunk = AnArrayList.create(head, AList.empty());
                AList<AList<T>> chunks = AnArrayList.create(headChunk, tailList.chunks);
                return new MyList<>(chunks, tailList.chunkSize);
            }
            AnArrayList<T> headChunk = AnArrayList.create(head, tailList.chunks.head());
            return new MyList<>(AnArrayList.create(headChunk, tailList.chunks.tail()), tailList.chunkSize);
        }
        MyList<T> result = create(head, AList.empty());
        while (!tail.isEmpty()) {
            result = create(tail.head(), result);
            tail = tail.tail();
        }
        return result.reverse();
    }

    @SafeVarargs
    public static <T> AList<T> of(T... elements) {
        if (elements.length == 0) return AList.empty();
        return new MyList<>(AnArrayList.create(AnArrayList.of(elements), AList.empty()));
    }

    public static <T> AList<T> chunkOf(int chunkSize, T[] elements) {
        if (chunkSize < 1) throw new IllegalArgumentException("chunkSize = " + chunkSize);
        if (elements.length == 0) return AList.empty();
        int start = 0;
        int end = chunkSize > elements.length ? elements.length : chunkSize;
        AList<AList<T>> chunks = AList.empty();
        while (start < elements.length) {
            T[] chunkArray = Arrays.copyOfRange(elements, start, end);
            chunks = AnArrayList.create(AnArrayList.of(chunkArray), chunks);
            start = end;
            end = start + chunkSize;
            if (end > elements.length) end = elements.length;
        }
        return new MyList<>(chunks.reverse(), chunkSize);
    }
}
