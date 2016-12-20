import nts.*;
import org.junit.Test;

import java.math.BigInteger;

import static java.util.stream.IntStream.rangeClosed;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class AnArrayListTest {
    private final static int SIZE = 2000000;
    private final static Integer[] ELEMENTS = rangeClosed(1, SIZE).boxed().toArray(Integer[]::new);

    @Test
    public void slowMap() {
        int evenCount = AnArrayList.of(ELEMENTS)
                                   .reverse()
                                   .filter(i -> i % 2 == 0)
                                   .map(k -> BigInteger.valueOf(k).modPow(BigInteger.valueOf(k), BigInteger.TEN.pow(10)))
                                   .foldLeft(0, (a, k) -> a + 1);
        assertThat(evenCount, is(SIZE /2));
    }

    @Test
    public void slowMapInParallel() {
        int evenCount = AnArrayList.parallelOf(ELEMENTS)
                                   .reverse()
                                   .filter(i -> i % 2 == 0)
                                   .map(k -> BigInteger.valueOf(k).modPow(BigInteger.valueOf(k), BigInteger.TEN.pow(10)))
                                   .foldLeft(0, (a, k) -> a + 1);
        assertThat(evenCount, is(SIZE /2));
    }

    @Test
    public void testMixing() {
        AList<String> list = ALinkedList.of("").factory().create("a", ABasicList.of("b"));
        assertThat(list.toString(), is("(a, b)"));

        list = AnArrayList.create("c", list);
        assertThat(list.toString(), is("(c, a, b)"));

        list = ALinkedList.of("a");
        assertTrue(list.tail().isEmpty());
    }

    @Test
    public void testReversedCreate() {
        AList<Integer> list1 = AnArrayList.of(1, 2, 3);
        AList<Integer> list2 = list1.reverse().tail();
        for (int i = 4; i < 6; i++) {
            list2 = AnArrayList.create(i, list2);
        }
        assertThat(list1.toString(), is("(1, 2, 3)"));
        assertThat(list2.toString(), is("(5, 4, 2, 1)"));
    }

    @Test(expected = AList.CreationFailed.class)
    public void testCreationFail() {
        AList<Byte> biggy = AnArrayList.of(new Byte[AnArrayList.MAX_SIZE]);
        biggy.factory().create(null, biggy);
    }
}
