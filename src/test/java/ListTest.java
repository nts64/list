import nts.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.lang.reflect.InvocationTargetException;
import java.util.function.BiFunction;
import java.util.function.Function;

import static java.util.Arrays.asList;
import static java.util.stream.IntStream.rangeClosed;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.*;
import static org.junit.Assume.assumeFalse;

/**
 * Run the same tests against all three AList implementations.
 * The largeList test fails with StackOverflowError for ABasicList, due to recursion being used.
 */
@SuppressWarnings("unchecked")
@RunWith(Parameterized.class)
public class ListTest {

    private Class<? extends AList> listClass;

    @Parameters(name = "{0}")
    public static Iterable<Class<? extends AList>> data() {
        return asList(
                ABasicList.class,
                ALinkedList.class,
                AnArrayList.class,
                MyList.class
        );
    }

    public ListTest(Class<? extends AList> listClass) {
        this.listClass = listClass;
    }

    @Test
    public void headAndTail() {
        AList<String> list = listOf("a", "b", "c");
        assertThat(list.head(), is("a"));
        assertThat(list.tail().head(), is("b"));
        assertThat(list.tail().tail().head(), is("c"));
        assertTrue(list.tail().tail().tail().isEmpty());
        assertThat(list.reverse().head(), is("c"));
        assertThat(list.reverse().tail().head(), is("b"));
    }

    @Test(expected = AList.CreationFailed.class)
    public void create() {
        AList<String> list = listOf("a");
        list = create("b", list);
        list = create("c", list);
        assertThat(list.toString(), is("(c, b, a)"));
        assertThat(list.reverse().toString(), is("(a, b, c)"));

        list = create("d", list.reverse());
        assertThat(list.toString(), is("(d, a, b, c)"));

        list = create(null, listOf((String) null));
        assertThat(list.head(), is(nullValue()));
        assertThat(list.toString(), is("(null, null)"));
        AList<String> list2 = create("A", list.tail());
        assertThat(list2.toString(), is("(A, null)"));

        list = AList.empty().factory().create("a", listOf("b"));
        assertThat(list.toString(), is("(a, b)"));

        //can't determine the correct type, should throw CreationFailed
        AList.empty().factory().create("", AList.empty());
    }

    @Test
    public void reverse() {
        AList<String> list = listOf("a", "b", "c");
        AList<String> reversed = list.reverse();
        assertThat(reversed.toString(), is("(c, b, a)"));
        assertThat(reversed.reverse().toString(), is("(a, b, c)"));
    }

    @Test
    public void filter() {
        AList<String> list = listOf("a", "bb", "c");
        AList<String> filtered = list.filter(x -> x.length() == 1);
        assertThat(filtered.asString(), is("(a, c)"));

        assertTrue(listOf(1, 2, 3).filter((Object k) -> k.equals((short)0)).isEmpty());
    }

    @Test
    public void map() {
        AList<String> list = listOf("foo", "bar", "baz");
        AList<String> mapped = list.map(String::toUpperCase);
        assertThat(mapped.toString(), is("(FOO, BAR, BAZ)"));

        list = listOf();
        assertTrue(list.map(String::toUpperCase).isEmpty());

        Function<Object, Integer> function = Object::hashCode;
        AList<Number> hashed = listOf("a").map(function);
        assertThat(hashed.head(), is("a".hashCode()));
    }

    @Test
    public void foldLeft() {
        AList<String> list = listOf("a", "bb", "ccc");
        int lengthSum = list.foldLeft(0, (a, x) -> a + x.length());
        assertThat(lengthSum, is(6));
        String listConcat = list.foldLeft("", String::concat);
        assertThat(listConcat, is("abbccc"));

        //just making sure it compiles with contravariant params and covariant return type
        BiFunction<Object, Object, Integer> function = (a, x) -> a.hashCode() / x.hashCode();
        Number n = list.foldLeft((Number)0, function);
        assertThat(n, is(0));
    }

    @Test
    public void empty() {
        AList<Object> list = listOf();
        assertTrue(list.isEmpty());
        assertThat(list.head(), is(nullValue()));
        assertTrue(list.tail().isEmpty());
        assertThat(list.toString(), is("()"));
        assertTrue(list.filter(e -> true).isEmpty());
        assertTrue(list.map(e -> e).isEmpty());
        assertTrue(list.reverse().isEmpty());
        assertThat(list.foldLeft(0, (a, e) -> a + 1), is(0));
        assertThat(list.size(), is(0L));
        assertFalse(listOf(list).isEmpty());
    }

    @Test
    public void largeList() {
        assumeFalse(listClass == ABasicList.class);
        int max = 200000;
        int evenCount = listOf(rangeClosed(1, max).boxed().toArray(Integer[]::new))
                              .reverse()
                              .filter(i -> i % 2 == 0)
                              .map(k -> (long) k)
                              .foldLeft(0, (a, k) -> a + 1);
        assertThat(evenCount, is(max/2));
    }

    private <T> AList<T> listOf(T... items) {
        Function creator = o -> {
            try {
                return listClass.getDeclaredMethod("of", Object[].class).invoke(null, o);
            } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                throw new AssertionError(e);
            }
        };
        return (AList<T>) creator.apply(items);
    }

    private <T> AList<T> create(T head, AList<T> tail) {
        return tail.factory().create(head, tail);
    }
}

