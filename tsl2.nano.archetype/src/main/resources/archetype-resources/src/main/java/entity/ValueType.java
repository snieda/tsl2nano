#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.entity;

import java.util.Collection;

import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import de.tsl2.nano.annotation.extension.With;
import de.tsl2.nano.bean.annotation.Attributes;
import de.tsl2.nano.bean.annotation.Presentable;
import de.tsl2.nano.bean.annotation.ValueExpression;
import de.tsl2.nano.h5.annotation.Controller;
import de.tsl2.nano.h5.annotation.ControllerAnnotationFactory;
import de.tsl2.nano.service.util.IPersistable;

@Entity
@ValueExpression("{name}/{unit}")
@Presentable(icon="icons/properties.png", iconFromField="picture")
@Attributes(names= {"name", "description", "unit", "picture", "entries"})
@With(ControllerAnnotationFactory.class) @Controller(baseType=ValueType.class, baseAttribute="entries", targetType=Entry.class, targetAttribute="type")
public class ValueType implements IPersistable<String> {
	private static final long serialVersionUID = 1L;
	private String id;
	private String name;
	private String description;
	private Unit unit;
	private byte[] picture;
	private Collection<Entry> entries; //only for the Controller-View!
	
	@Id
	@GeneratedValue
	@Override
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	@Basic @Lob
	public byte[] getPicture() {
		return picture;
	}
	public void setPicture(byte[] picture) {
		this.picture = picture;
	}
	@ManyToOne @JoinColumn
	public Unit getUnit() {
		return unit;
	}
	public void setUnit(Unit unit) {
		this.unit = unit;
	}
	@OneToMany
	@Presentable(visible=false)
	public Collection<Entry> getEntries() {
		return entries;
	}
	public void setEntries(Collection<Entry> entries) {
		this.entries = entries;
	}
	@Override
	public String toString() {
		return name + "/" + unit;
	}
}
