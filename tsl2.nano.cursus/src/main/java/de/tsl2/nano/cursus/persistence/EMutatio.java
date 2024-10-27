package de.tsl2.nano.cursus.persistence;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;

import de.tsl2.nano.bean.annotation.Attributes;
import de.tsl2.nano.bean.annotation.Presentable;
import de.tsl2.nano.bean.annotation.ValueExpression;
import de.tsl2.nano.core.util.Util;
import de.tsl2.nano.cursus.Mutatio;
import de.tsl2.nano.service.util.IPersistable;

@Entity
@ValueExpression("{res}: {next}")
@Attributes(names = { "previous", "next", "res" })
@Presentable(label = "ΔMutatio", icon = "icons/compose.png")
public class EMutatio extends Mutatio<Object, Object> implements IPersistable<String> {
	private static final long serialVersionUID = 1L;

	String id;

	EExsecutio exsecutio;

	public EMutatio() {
	}

	public EMutatio(String next, ERes res) {
		super(next, res);
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

	public String getPrevious() {
		return Util.asString(getOld());
	}

	public void setPrevious(String previous) {
		this.previous = previous;
	}

	public String getNext() {
		return Util.asString(getNew());
	}

	public void setNext(String next) {
		this.next = next;
	}

	@OneToOne(mappedBy = "mutatio", cascade = CascadeType.ALL, orphanRemoval = true)
	public ERes getRes() {
		return (ERes) res;
	}

	public void setRes(ERes res) {
		if (res != null)
			res.setMutatio(this);
		this.res = res;
	}

	@ManyToOne
	@JoinColumn
	public EExsecutio getExsecutio() {
		return exsecutio;
	}

	public void setExsecutio(EExsecutio exsecutio) {
		this.exsecutio = exsecutio;
	}

}
