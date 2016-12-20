import nts.ALinkedList;
import nts.AList;
import nts.AnArrayList;
import nts.MyList;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.math.BigInteger;
import java.util.*;

import static java.util.stream.IntStream.range;
import static java.util.stream.IntStream.rangeClosed;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * Create a list with SIZE elements, reverse, filter and sum the elements in a BigInteger.
 *
 * Very rough comparison test, just to catch severe performance issues.
 *
 * A proper benchmark would be implemented using JMH.
 */
@Ignore("too slow")
public class ComparisonTest {

    private final static int SIZE = 10_000_000;
    private final static int RUNS = 10;
    private final static BigInteger EXPECTED = BigInteger.valueOf(2941178529410L);
    private final static Integer[] elements = rangeClosed(0, SIZE).boxed().toArray(Integer[]::new);

    @BeforeClass
    public static void warmUp(){
        ComparisonTest test = new ComparisonTest();
        test.anArrayList();
        test.aLinkedList();
    }

    @Test
    public void myList() {
        run(()-> test(MyList.of(elements)));
    }

    @Test
    public void myListSmallerChunks() {
        run(()-> test(MyList.chunkOf(SIZE/10, elements)));
    }

    @Test
    public void anArrayList() {
        run(()-> test(AnArrayList.of(elements)));
    }

    @Test
    public void anArrayListParallel() {
        run(()-> test(AnArrayList.parallelOf(elements)));
    }

    @Test
    public void aLinkedList() {
        run(()-> test(ALinkedList.of(elements)));
    }

    @Test
    public void javaArrayList() {
        run(()-> test(new ArrayList<>(Arrays.asList(elements))));
    }

    @Test
    public void javaLinkedList() {
        run(()-> test(new LinkedList<>(Arrays.asList(elements))));
    }

    private void test(AList<Integer> list) {
        list = list.reverse();
        assertThat(list.head(), is(SIZE));
        list = list.filter(k -> k % 17 == 0);
        BigInteger sum = list.foldLeft(BigInteger.ZERO, (a, x) -> a.add(BigInteger.valueOf(x)));
        assertThat(sum, is(EXPECTED));
    }

    private void test(List<Integer> list) {
        Collections.reverse(list);
        assertThat(list.get(0), is(SIZE));
        BigInteger sum = list.stream().filter(k -> k % 17 == 0).map(BigInteger::valueOf).reduce(BigInteger::add).orElseThrow(Error::new);
        assertThat(sum, is(EXPECTED));
    }

    private void run(Runnable f) {
        range(0, RUNS).forEach(k -> f.run());
    }
}
