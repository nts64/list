import nts.ABasicList;
import nts.AList;
import nts.AnArrayList;
import nts.MyList;
import org.junit.Test;

import static java.util.stream.IntStream.range;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

public class MyListTest {

    @Test
    public void chunkOf() {
        Integer[] elements = range(0, 10).boxed().toArray(Integer[]::new);
        AList<Integer> myList = MyList.chunkOf(3, elements);
        assertThat(myList.reverse().toString(), is("(9, 8, 7, 6, 5, 4, 3, 2, 1, 0)"));
        assertThat(myList.filter(k -> k > 5).toString(), is("(6, 7, 8, 9)"));
        assertThat(myList.filter(k -> k > 5).reverse().toString(), is("(9, 8, 7, 6)"));

        myList = MyList.chunkOf(4, new Integer[4]);
        myList = MyList.create(0, myList);
        assertThat(myList.size(), is(5L));
    }

    @Test
    public void createFromAnArrayList() {
        AList<Object> tail = AnArrayList.of(new Object[MyList.DEFAULT_CHUNK_SIZE + 1]);
        Object head = "head";
        AList list = MyList.create(head, tail);
        assertThat(list.head(), is(head));
        assertThat(list.tail().head(), is(nullValue()));

        tail = AnArrayList.of("a");
        list = MyList.create(head, tail);
        assertThat(list.reverse().toString(), is("(a, head)"));
    }

    @Test
    public void createFromAList() {
        AList<Object> tail = ABasicList.of(new Object[10]);
        Object head = "head";
        AList list = MyList.create(head, tail);
        assertThat(list.head(), is(head));
        assertThat(list.tail().head(), is(nullValue()));
        assertThat(list.reverse().head(), is(nullValue()));
    }
}
