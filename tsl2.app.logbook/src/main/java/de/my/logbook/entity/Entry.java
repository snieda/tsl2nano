package de.my.logbook.entity;

import java.util.Date;

import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import de.tsl2.nano.bean.annotation.Attributes;
import de.tsl2.nano.bean.annotation.Constraint;
import de.tsl2.nano.bean.annotation.Presentable;
import de.tsl2.nano.bean.annotation.ValueExpression;
import de.tsl2.nano.core.util.DateUtil;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.core.util.Util;
import de.tsl2.nano.service.util.IPersistable;

@Entity
@ValueExpression("{date} {time}-{user}-{category}: {type} {value}")
@Presentable(icon="icons/compose.png", iconFromField="attachment")
@Attributes(names= {"date", "time", "user", "category", "type", "value", "comment", "attachment"})
public class Entry implements IPersistable<String> {
	private static final long serialVersionUID = 1L;
	private String id;
	private Date date;
	private Date time;
	private User user;
	private LogCategory category;
	private String comment;
	private ValueType type;
	private double value;
	private byte[] attachment;
	
		@Id
		@GeneratedValue
		@Override
		public String getId() {
			return id;
		}
		public void setId(String id) {
			this.id = id;
		}
		@Temporal(TemporalType.DATE)
		@Constraint(nullable=false)
		public Date getDate() {
			return date;
		}
		public void setDate(Date date) {
			this.date = date;
		}
		
		@Temporal(TemporalType.TIME)
		@Constraint(nullable=false)
		public Date getTime() {
			return time;
		}
		public void setTime(Date time) {
			this.time = time;
		}
		@ManyToOne @JoinColumn(nullable=false)
		public LogCategory getCategory() {
			return category;
		}
		public void setCategory(LogCategory category) {
			this.category = category;
		}
		@ManyToOne @JoinColumn(nullable=false)
		public ValueType getType() {
			return type;
		}
		public void setType(ValueType type) {
			this.type = type;
		}
		@Constraint(nullable=false, min="1")
		public double getValue() {
			return value;
		}
		public void setValue(double value) {
			this.value = value;
		}
		@Basic @Lob
		public byte[] getAttachment() {
			return attachment;
		}
		public void setAttachment(byte[] attachment) {
			this.attachment = attachment;
		}
		@ManyToOne @JoinColumn(nullable=false)
		public User getUser() {
			return user;
		}
		public void setUser(User user) {
			this.user = user;
		}
		public String getComment() {
			return comment;
		}
		public void setComment(String comment) {
			this.comment = comment;
		}
		
		@Override
		public String toString() {
			return StringUtil.toString(Util.toString(getClass(), DateUtil.getFormattedDate(date), DateUtil.getFormattedTime(time), type, value), 64);
		}
}
