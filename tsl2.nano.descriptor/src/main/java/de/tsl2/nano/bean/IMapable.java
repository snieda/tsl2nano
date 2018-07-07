/**
 * 
 */
package de.tsl2.nano.bean;

import java.util.Map;

/**
 * UNUSED YET! <p/>
 * lets map an object to a simple key/value map
 * @author Tom
 */
public interface IMapable {
	Map<String, ? extends Object> toValueMap();
}
