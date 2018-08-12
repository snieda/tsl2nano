package tsl2.nano.cursus.persistence;

import java.lang.Thread.UncaughtExceptionHandler;
import java.sql.Timestamp;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import de.tsl2.nano.bean.BeanContainer;
import de.tsl2.nano.bean.annotation.Action;
import de.tsl2.nano.bean.annotation.Attributes;
import de.tsl2.nano.bean.annotation.Presentable;
import de.tsl2.nano.bean.annotation.ValueExpression;
import de.tsl2.nano.core.exception.Message;
import de.tsl2.nano.core.messaging.IListener;
import de.tsl2.nano.core.util.ConcurrentUtil;
import de.tsl2.nano.core.util.Util;
import de.tsl2.nano.service.util.IPersistable;
import tsl2.nano.cursus.EProcessLog;
import tsl2.nano.cursus.IConsilium;
import tsl2.nano.cursus.Processor;

@Entity
@ValueExpression(expression="{startedAt}: {startPeriod}-{endPeriod} ({items})")
@Attributes(names= {"startPeriod", "endPeriod", "startedAt"})
@Presentable(label="ΔProcess", icon="icons/go.png", enabled=false)
public class EProcess implements IPersistable<String>, IListener<Object> {
	private static final long serialVersionUID = 1L;
	transient Processor prc = new Processor();
	String id;
	
	Date startedAt;
	Date endedAt;
	Status status = Status.CREATED;
	
	Date startPeriod;
	Date EndPeriod;
	
	long items;
	
	List<EProcessLog> log;
	
	enum Status {CREATED, RUNNING, RUNNING_WITH_FAILURES, FINISHED_SUCCESS, FAILED, FINISHED_WITH_FAILURES};
	
	public EProcess() {
		prc.getEventController().addListener(this, Object.class);
	}
	
	public EProcess(Date startPeriod, Date endPeriod) {
		this(null, startPeriod, endPeriod, 0);
	}
	
	public EProcess(Timestamp startedAt, Date startPeriod, Date endPeriod, long items) {
		this();
		this.startedAt = startedAt;
		this.startPeriod = startPeriod;
		EndPeriod = endPeriod;
		this.items = items;
	}


	@Id @GeneratedValue
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	
	@Temporal(TemporalType.DATE)
	public Date getStartPeriod() {
		return startPeriod;
	}
	public void setStartPeriod(Date startPeriod) {
		this.startPeriod = startPeriod;
	}
	@Temporal(TemporalType.DATE)
	public Date getEndPeriod() {
		return EndPeriod;
	}
	public void setEndPeriod(Date endPeriod) {
		EndPeriod = endPeriod;
	}
	@OneToMany(mappedBy="process", cascade=CascadeType.ALL, orphanRemoval=true)
	public List<EProcessLog> getLog() {
		if (log == null)
			log = new LinkedList<>();
		return log;
	}
	public void setLog(List<EProcessLog> log) {
		this.log = log;
	}
	@Temporal(TemporalType.TIMESTAMP)
	@Presentable(enabled=false)
	public Date getStartedAt() {
		return startedAt;
	}
	public void setStartedAt(Date startedAt) {
		this.startedAt = startedAt;
	}
	public Date getEndedAt() {
		return endedAt;
	}
	public void setEndedAt(Date endedAt) {
		this.endedAt = endedAt;
	}
	public Status getStatus() {
		return status;
	}
	public void setStatus(Status status) {
		this.status = status;
	}
	public long getItems() {
		return items;
	}
	public void setItems(long items) {
		this.items = items;
	}
	@Action(argNames= {"grex"})
	@Presentable(icon="icons/go.png")
	public void actionStart(EGrex grex) {
		Set<IConsilium> consilii = new LinkedHashSet<>();
		Set<ERes> parts = grex.createParts();
		checkAndSave(parts);
		for (ERes res : parts) {
			consilii.addAll(res.getConsilii());
		}
		if (consilii.isEmpty())
			throw new IllegalStateException("For given Grex " + grex + " no bound consilii were found! Nothing to do!");
		final UncaughtExceptionHandler uncaughtExceptionHandler = Thread.currentThread().getUncaughtExceptionHandler();
		ConcurrentUtil.startDaemon(toString(), new Runnable() {
			@Override
			public void run() {
				try {
					Thread.currentThread().setUncaughtExceptionHandler(uncaughtExceptionHandler);
					prc.run(getStartPeriod(), getEndPeriod(), consilii.toArray(new EConsilium[0]));
				} catch (Throwable e) {
					e.printStackTrace();
					Message.send(uncaughtExceptionHandler, e);
					setEndedAt(new Date());
					setStatus(Status.FAILED);
					BeanContainer.instance().save(this);
				}
			}
		});
	}
	@Action(argNames={"res"})
	@Presentable(icon="icons/blocked.png")
	public void actionDeactivate(ERes res) {
		checkAndSave();
		prc.deactivate(new HashSet<>(res.getConsilii()), getStartPeriod(), getEndPeriod());
	}
	@Action(argNames= {"lastActiveConsilium"})
	@Presentable(icon="icons/reload.png")
	public void actionResetTo(EConsilium lastActiveConsilium) {
		checkAndSave();
		prc.resetTo(lastActiveConsilium.followers(), lastActiveConsilium);
	}

	protected void checkAndSave() {
		if (getStartedAt() != null) {
			throw new IllegalStateException("This process was already started! Please define a new one!");
		}
		setStartedAt(new Date());
		setStatus(Status.RUNNING);
		BeanContainer.instance().save(this);
	}
	protected void checkAndSave(Set<ERes> parts) {
		if (Util.isEmpty(parts))
			throw new IllegalArgumentException("no objectIDs defined -> nothing to do!");
		checkAndSave();
	}

	@Override
	public void handleEvent(Object event) {
		if (event instanceof Integer) {
			setItems(((Integer)event).longValue());
		} else if (event instanceof EConsilium) {
			EProcessLog log = new EProcessLog(this, new Timestamp(System.currentTimeMillis()), (EConsilium) event);
			getLog().add(log);
		} else if (event instanceof Exception) {
			setStatus(Status.RUNNING_WITH_FAILURES);
			getLog().get(getLog().size()-1).setStatus(new de.tsl2.nano.bean.def.Status((Exception)event));
		} else if (event.equals(Processor.FINISHED)){
			if (getStatus().equals(Status.RUNNING_WITH_FAILURES))
				setStatus(Status.FINISHED_WITH_FAILURES);
			else
				setStatus(Status.FINISHED_SUCCESS);
		}
		Message.send("processing (" + this + "): " + event.toString());
		BeanContainer.instance().save(this);
	}
	
	@Override
	public String toString() {
		return Util.toString(getClass(), startedAt, startPeriod, EndPeriod, status, items);
	}
}
