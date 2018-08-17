package tsl2.nano.cursus.persistence;

import java.lang.Thread.UncaughtExceptionHandler;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.tsl2.nano.bean.BeanContainer;
import de.tsl2.nano.bean.annotation.Action;
import de.tsl2.nano.bean.annotation.Attributes;
import de.tsl2.nano.bean.annotation.Presentable;
import de.tsl2.nano.bean.annotation.ValueExpression;
import de.tsl2.nano.bean.def.SStatus;
import de.tsl2.nano.core.exception.Message;
import de.tsl2.nano.core.messaging.IListener;
import de.tsl2.nano.core.util.ConcurrentUtil;
import de.tsl2.nano.core.util.Util;
import de.tsl2.nano.service.util.IPersistable;
import tsl2.nano.cursus.EProcessLog;
import tsl2.nano.cursus.IConsilium;
import tsl2.nano.cursus.Processor;
import tsl2.nano.cursus.effectus.Effectree;

@Entity
@ValueExpression(expression="{startedAt}: {startPeriod}-{endPeriod} (Items: {items}, Status: {status})")
@Attributes(names= {"startPeriod", "endPeriod", "startedAt", "items", "status", "log"})
@Presentable(label="ΔProcess", icon="icons/go.png", enabled=false)
public class EProcess implements IPersistable<String>, IListener<Object> {
	private static final long serialVersionUID = 1L;
	private static final Log LOG = LogFactory.getLog(EProcess.class);
	
	transient Processor prc = new Processor();
	String id;
	
	Date startedAt;
	Date endedAt;
	Status status = Status.CREATED;
	
	Date startPeriod;
	Date EndPeriod;
	
	long items = -1;
	
	List<EProcessLog> log;
	private transient EProcess current;
	
	enum Status {CREATED, RUNNING, RUNNING_WITH_FAILURES, FINISHED_SUCCESS, FAILED, FINISHED_WITH_FAILURES, CANCELED};
	
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
	@Presentable(enabled=false, nesting=true)
	public List<EProcessLog> getLog() {
		if (log == null)
			log = new LinkedList<>();
		return log;
	}
	protected void setLog(List<EProcessLog> log) {
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
	@Presentable(enabled=false)
	public Date getEndedAt() {
		return endedAt;
	}
	public void setEndedAt(Date endedAt) {
		this.endedAt = endedAt;
	}
	@Enumerated(EnumType.STRING)
	@Presentable(enabled=false)
	public Status getStatus() {
		return status;
	}
	public void setStatus(Status status) {
		this.status = status;
	}
	@Presentable(enabled=false)
	public long getItems() {
		return items;
	}
	public void setItems(long items) {
		this.items = items;
	}
	@Action(argNames= {"grex"})
	@Presentable(icon="icons/go.png")
	public List<EProcessLog> actionStart(EGrex grex) {
		loadEffectree(grex);
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
					LOG.error(e.toString(), e);
					Message.send(e);
					setEndedAt(new Date());
					setStatus(Status.FAILED);
					try { //if that failes, too, no error may be shown, so we should catch that
						current = BeanContainer.instance().save(EProcess.this);
					} catch (Throwable e1) {
						LOG.error("", e1);
					}
				}
			}
		});
		ConcurrentUtil.sleep(2000);
		return current != null ? current.getLog() : null;
	}

	protected void loadEffectree(EGrex grex) {
		if (Effectree.instance().isEmpty()) {
			Collection<EGrexEffectus> grexEffectus = BeanContainer.instance().getBeans(EGrexEffectus.class, 0, -1);
			for (EGrexEffectus ge : grexEffectus) {
				ERes res = ge.getEffectus().getRes();
				Effectree.instance().addEffects(ge.getGrex().getGenRes(),
						Effectree.effect(res.getType(), res.getPath(), ERuleEffectus.class, null));
			}
		}
	}

	@Presentable(icon="icons/stop.png")
	public List<EProcessLog> actionStop() {
		prc.stop();
		ConcurrentUtil.sleep(1000);
		return current != null ? current.getLog() : null;
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
		current = BeanContainer.instance().save(this);
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
			EConsilium consilium = (EConsilium) event;
			EProcessLog log = new EProcessLog(this, new Timestamp(System.currentTimeMillis()), consilium);
			getLog().add(log);
			if (!consilium.getStatus().equals(IConsilium.Status.INACTIVE)) {
				log.setStatus(new SStatus(SStatus.OK, consilium.getStatus().name(), null));
				BeanContainer.instance().save(consilium);
			} else {
				log.setStatus(new SStatus(SStatus.OK, "STARTED", null));
			}
		} else if (event instanceof Exception) {
			setStatus(Status.RUNNING_WITH_FAILURES);
			getLog().get(getLog().size()-1).setStatus(new de.tsl2.nano.bean.def.SStatus((Exception)event));
		} else if (event.equals(Processor.FINISHED)){
			setEndedAt(new Date());
			if (getStatus().equals(Status.RUNNING_WITH_FAILURES))
				setStatus(Status.FINISHED_WITH_FAILURES);
			else {
				if (getLog().size() > 0)
					getLog().get(getLog().size()-1).setStatus(new SStatus(SStatus.OK, "FINSISHED", null));
				setStatus(Status.FINISHED_SUCCESS);
			}
		}
		Message.send("processing (" + this + "): " + event.toString());
		current = BeanContainer.instance().save(this);
	}
	
	@Override
	public String toString() {
		return Util.toString(getClass(), startedAt, startPeriod, EndPeriod, status, items);
	}
}
