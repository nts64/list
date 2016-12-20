This is a description of the process I went through, while doing the assignment. It also answers the additional questions about complexity, thread safety and scaling.

Language of choice was Java 8, as I am most familiar with it.

1.
 From past experience, I knew that the ability to decompose a list into head and tail and to create a new list from an element (head) and a list (tail), along with the 'special' empty list are sufficient to implement a linked list and all the operations required. This is reflected in the interface AList.

2.
 Starting with the simplest thing that works, I created a basic recursive implementation called ABasicList, that has line-long methods (later moved to AList as default methods) and passes most of the basic tests (ListTest.java). Except the test with a larger list, which of course fails with a stack overflow error.

3.
 Next step was to redefine the recursive methods using loops and the result was ALinkedList. This now works for any size, but generates a lot of memory overhead and can't be made very fast. Complexity of map/filter/fold/reverse is linear, but map and filter are doing an extra reverse at the end, compared to the basic list.
 Both those implementations are immutable (thus thread-safe) and operations have linear complexity.
 At this point I also modified the ListTest to accept the list class as a parameter, so I can easily run the same tests for each implementation.

4.
 Next step was to use an array as the element storage (AnArrayList.java). This allows for some improvements, but also has some disadvantages.
 The complexity of map/filter is still linear, but the execution can be made faster with a fork/join approach. I felt implementing this approach was beyond the scope of the assignment, so I just used parallel streams.
 The reverse operation is now O(1) and the foldLeft is still sequential, as it is type asymmetrical and can't be executed in parallel. Well, unless a combiner function can be provided, like in j.u.stream.Stream.reduce(U, BiFunction<...>, BinaryOperator<U>), but this was not part of the assignment.
 The list is still thread-safe, but as the storage array can be shared, some synchronization was needed for the create operation. The need to resize/copy the array can also make the create operation slow, especially for large arrays.

     Advantages:
      * parallel map and filter operations
      * reverse operation is just flipping a boolean
      * less memory overhead

     Disadvantages:
      * adding elements can be slow, as the array may have to be copied in a synchronized block.
      * the size is limited to Java maximum array size (max_int - 2)
      * null values and unused array slots have to be distinguished, requiring some additional logic

5.
 My last step (MyList.java) was to use an array list of array lists as the storage (I called them chunks). This solves the size problem, while preserving the thread-safety and complexity of operations. With the provided factory methods, a large list can also be constructed much more efficiently.

Note on scaling:
  MyList can be used as starting point when implementing a list spread over many nodes. Each chunk can be mapped to a node, using a consistent hashing algorithm on the index of the chunk. The fork/join algorithm would then spread the tasks over nodes instead of threads. So the chunk implementation could be a proxy, passing the function to another node and getting the results back.
I would make map, filter and reverse lazy, so a chain of operations can all be done remotely, e.g. list.map(m).filter(f).reverse().head() would actually send the functions and receive the result in the head() method. I'd also add a reduce() operation, similar to the one from j.u.s.Stream above, so it could be run in parallel.

Notes on possible performance improvements:
  In Java there is no equivalent of the C-style 'array of structs'. But there is another way to achieve similar performance with ObjectLayout ( http://objectlayout.github.io/ObjectLayout/ ), though I think the necessary JVM optimisations are only available in Zing (The JVM provided by Azul Systems).
  The StructuredArray class could replace the Object[] used in my implementation and provide an optimised memory layout. This in turn would allow us to take advantage of modern CPU architecture (caches, pre-fetchers...) and avoid 'pointer chasing'.
  The lists being immutable can cause the generation of a lot of 'garbage' (short-lived objects that need to be GC-ed) and lead to long pauses, as the garbage collector struggles to catch up. Zing can avoid this problem, with its pause-less garbage collector (C4) and the ability to handle very big allocation rates.
