package de.my.logbook.entity;

import java.text.DateFormat;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import de.tsl2.nano.bean.annotation.Attributes;
import de.tsl2.nano.bean.annotation.Presentable;
import de.tsl2.nano.bean.annotation.ValueExpression;
import de.tsl2.nano.h5.websocket.chat.Chat;
import de.tsl2.nano.service.util.IPersistable;

@Entity
@ValueExpression("{sent} {from}: {message}")
@Presentable(icon="icons/e-mail.png")
@Attributes(names= {"sent", "from", "to", "message", "attachment"})
@Chat(receiver="to", message="message", attachment="attachment")
public class ChatMessage implements IPersistable<String> {
	private static final long serialVersionUID = 1L;
	private String id;
	private Date sent;
	private String from;
	private String to;
	private String message;
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
	@Temporal(TemporalType.TIMESTAMP)
	public Date getSent() {
		return sent;
	}
	public void setSent(Date sent) {
		this.sent = sent;
	}
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
	@Column(name="sfrom", length=64)
	public String getFrom() {
		return from;
	}
	public void setFrom(String from) {
		this.from = from;
	}
	@Column(name="sto", length=64)
	public String getTo() {
		return to;
	}
	public void setTo(String to) {
		this.to = to;
	}
	public byte[] getAttachment() {
		return attachment;
	}
	public void setAttachment(byte[] attachment) {
		this.attachment = attachment;
	}
	@Override
	public String toString() {
		return (sent != null ? DateFormat.getDateInstance().format(sent) : "----") + " " + from + ": " + message;
	}
}
