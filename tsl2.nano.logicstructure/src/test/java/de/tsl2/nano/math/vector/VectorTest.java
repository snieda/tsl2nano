package de.tsl2.nano.math.vector;

import static junit.framework.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class VectorTest {

    @Test
    public void testFloatStream() {
        List<Vector> vlist = Arrays.asList(new Vector(1, 2, 3), new Vector(4, 5, 6), new Vector(7, 8, 9));
        List<Vector> list = Vector.fromStream(Vector.toStream(vlist));
        assertTrue(Arrays.equals(vlist.toArray(), list.toArray()));
    }

}
