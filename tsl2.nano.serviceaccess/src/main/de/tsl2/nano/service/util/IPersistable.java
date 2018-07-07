/**
 * 
 */
package de.tsl2.nano.service.util;

import java.io.Serializable;

import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

/**
 * Provide jpa entities with a generic id.
 * 
 * @author Tom
 */
@MappedSuperclass
public interface IPersistable<ID> extends Serializable {
	@Id
	@GeneratedValue
	ID getId();
	void setId(ID id);
}
