package tsl2.nano.cursus.persistence;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;

import de.tsl2.nano.bean.annotation.Attributes;
import de.tsl2.nano.bean.annotation.ValueExpression;
import de.tsl2.nano.service.util.IPersistable;
import tsl2.nano.cursus.Grex;

@Entity
@ValueExpression(expression="{genRes}")
@Attributes(names= {"genRes", "validObjectIDs"})
public class EGrex extends Grex<Object, Object> implements IPersistable<String> {
	private static final long serialVersionUID = 1L;

	String id;

	public EGrex() {
	}

	public EGrex(Class<Object> type, String path, Object... objectIDs) {
		super(type, path, objectIDs);
	}

	public EGrex(ERes genericRes, Object... objectIDs) {
		super(genericRes, objectIDs);
	}
	
	@Id
	@GeneratedValue
	@Override
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}

	@OneToOne @JoinColumn
	public ERes getGenRes() {
		return (ERes) genRes;
	}
	public void setGenRes(ERes genRes) {
		this.genRes = genRes;
	}
	public HashSet<String> getValidObjectIDs() {
		return (HashSet<String>) (HashSet)validObjectIDs;
	}
	public void setValidObjectIDs(Set<Object> validObjectIDs) {
		this.validObjectIDs = validObjectIDs;
	}
}
