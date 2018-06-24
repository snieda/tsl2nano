/**
 * 
 */
package tsl2.nano.cursus;

import java.util.Map;

/**
 * lets map an object to a simple key/value map
 * @author Tom
 */
public interface IMapable {
	Map<String, ? extends Object> toValueMap();
}
