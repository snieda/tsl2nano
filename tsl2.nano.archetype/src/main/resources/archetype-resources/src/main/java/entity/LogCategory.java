#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.entity;

import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Lob;

import de.tsl2.nano.bean.annotation.Attributes;
import de.tsl2.nano.bean.annotation.Presentable;
import de.tsl2.nano.bean.annotation.ValueExpression;
import de.tsl2.nano.service.util.IPersistable;

@Entity
@ValueExpression("{name}")
@Presentable(icon="icons/cascade.png", iconFromField="picture")
@Attributes(names= {"name", "description", "picture"})
public class LogCategory implements IPersistable<String> {
	private static final long serialVersionUID = 1L;
	private String id;
	private String name;
	private String description;
	private byte[] picture;
	
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
}
