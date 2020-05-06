package de.tsl2.nano.math.vector;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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

    @Test
    public void testRectangle() {
    	Rectangle r = new Rectangle(-5, -5, 10, 10);
    	assertTrue(r.contains(-5, -5));
    	assertTrue(r.contains(0, 0));
    	assertTrue(r.contains(5, 5));

    	assertFalse(r.contains(6, 5));
    	
    	r.checkDimension(1, 1);
    	
    	try {
    		r.checkDimension(1,1,1);
    		fail("wrong dimension");
    	} catch (Exception ex) {
    		//ok, wrong dimension
    	}
    }
    
    @Test
    public void testVector() {
    	Vector v = new Vector(1,2,3,4,5);
    	assertEquals(5, v.dimension());

    	v.add(new Vector(1,1,1,1,1));
    	assertEquals(new Vector(2,3,4,5,6), v);
    	
    	assertEquals(Math.sqrt(4+4), new Vector(2,2).len(), 0.001);
    	assertEquals(Float.NaN, new Vector(2,2).angle(new Vector(1,1)), 0.001);
    	assertEquals(4, new Vector(2,2).multiply(new Vector(1,1)), 0.001);
    }
    
    @Test
    public void testVector3D() {
    	Vector3D v = new Vector3D(2, 2, 2);
    	Vector vv = v.clone(1);
    	
    	v.rotX((float) (2 * Math.PI));
    	v.rotY((float) (2 * Math.PI));
    	v.rotZ((float) (2 * Math.PI));
    	for (int i = 0; i < v.dimension(); i++) {
        	assertEquals(vv.x[i], v.x[i], 0.0001); //in cause of rounding errors
		}

    	v.rotX((float) (1));
    	assertNotEquals(vv, v);
    }
}
