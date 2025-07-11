package org.myjtools.jexten.test;

import static org.myjtools.jexten.Priority.*;
import java.util.stream.Stream;
import org.myjtools.jexten.Priority;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

class TestPriority {

    @Test
    void prioritiesAreSortedBeingHighestFirst() {
        assertThat(Stream.of(Priority.values()).sorted())
            .containsExactly(HIGHEST, HIGHER, NORMAL, LOWER, LOWEST);
    }
}
