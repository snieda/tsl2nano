package tsl2.nano.cursus;


import static org.junit.Assert.assertEquals;
import static tsl2.nano.cursus.effectus.Effectree.effect;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import de.tsl2.nano.bean.def.Bean;
import de.tsl2.nano.bean.def.BeanDefinition;
import de.tsl2.nano.core.cls.PrivateAccessor;
import de.tsl2.nano.core.util.DateUtil;
import de.tsl2.nano.core.util.ENVTestPreparation;
import tsl2.nano.cursus.effectus.Effectree;
import tsl2.nano.cursus.effectus.IncEffectus;
import tsl2.nano.cursus.persistence.EConsilium;
import tsl2.nano.cursus.persistence.EExsecutio;
import tsl2.nano.cursus.persistence.EMutatio;
import tsl2.nano.cursus.persistence.ERes;
import tsl2.nano.cursus.persistence.ERuleEffectus;
import tsl2.nano.cursus.persistence.ETimer;

/**
 * Test-Implementation for a complete Action-based Data-System.
 * Advantage:
 *  - less data to store
 *  - complete change history 
 * Disadvantage:
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class CursusTest {
	static Contract origin = new Contract();

    @BeforeClass
    public static void setUp() {
    	ENVTestPreparation.setUp("cursus", false);
    }

    @AfterClass
    public static void tearDown() {
    	ENVTestPreparation.tearDown();
    }

    @Test
    public void testEntityAttributeAccessability() throws Exception {
    	accessAttributes(new EConsilium(), 9);
    	accessAttributes(new EExsecutio(), 6);
    	accessAttributes(new EMutatio(), 7);
    	accessAttributes(new ERes(), 7);
    	accessAttributes(new ETimer(), 7);
    	//TODO: warum wird hier EMutatio angezogen? accessAttributes(new ERuleEffectus(), 7);
    }

	private void accessAttributes(Object instance, int attrCount) {
		Bean<?> bean = Bean.getBean(instance);
    	assertEquals(attrCount, bean.getAttributes(true).size());
		bean.getAttributes(true).stream().forEach(a -> a.setValue(instance , a.getValue(instance)));
	}
    
    @Test
    public void testContract() throws Exception {
    	Effectree.instance().addEffects(Contract.class, "contract.end", effect(Contract.class, "contract.value", TIncEffectus.class, 5));
    	System.out.println(Effectree.instance().toString());
    	Consilium consilii[] = {
    		new Consilium("end", new Timer(DateUtil.getYesterday(), null), Consilium.Priority.HIGHEST, 
    				new Exsecutio(Action.CHANGE_END.name(), 
    				new Mutatio("01/01/2019", new TRes(Contract.class.getName(), "1", "contract.end")), null)),
    		new Consilium("saldo", new Timer(DateUtil.getToday(), null), Consilium.Priority.NORMAL, 
    				new Exsecutio(Action.CHANGE_SALDO.name(),
    				new Mutatio("2000", new TRes(Contract.class.getName(), "1", "contract.accounts[?type=CAPACITY].saldo")), null)),
    		new Consilium("future", new Timer(DateUtil.getTomorrow(), null), Consilium.Priority.NORMAL, 
    				new Exsecutio(Action.CHANGE_SALDO.name(), 
    				new Mutatio("XXXX", new TRes(Contract.class.getName(), "1", "contract.accounts[type=CAPACITY].saldo")), null)),
    		new Consilium("auto", new Timer(DateUtil.getDate(2018, 01, 1), DateUtil.getDate(2018, 12, 1), Calendar.MONTH, 1), Consilium.Priority.LOW, 
    				new Exsecutio(Action.CHANGE_SALDO.name(), 
    				new Mutatio("2001", new TRes(Contract.class.getName(), "1", "contract.accounts[?type=PAYMENT].saldo")), null)),
    	};
    	new Processor().run(DateUtil.getStartOfYear(DateUtil.getToday()), DateUtil.getToday(), consilii);
    	
    	assertEquals(new Date((String)((Exsecutio)consilii[0].getExsecutios().iterator().next()).mutatio.getNew()), origin.end);
    	assertEquals(Long.valueOf((String)((Exsecutio)consilii[1].getExsecutios().iterator().next()).mutatio.getNew()).longValue(), ((List<Account>)origin.accounts).get(1).saldo);
    	assertEquals(Consilium.Status.INACTIVE, consilii[2].status);
    	assertEquals(5d, origin.value);
    	assertEquals(2, origin.accounts.size());
    }
}
//Mock
class TRes extends Res<Contract, Object> {
	public TRes() {
		super();
		// TODO Auto-generated constructor stub
	}
	public TRes(String type, String objectid, String path) {
		super(type, objectid, path);
	}
	@Override
	protected Class<Contract> type(String description) {
		return Contract.class;
	}
	@Override
	protected Contract materialize(String description) {
		return CursusTest.origin;
	}	
}

//Mock
class TIncRes extends TRes {
	public TIncRes(String type, String objectid, String path) {
		this.type = type;
		this.objectid = objectid;
		this.path = path;
		setDescription(PREFIX_REFERENCE + type + PREFIX_ID + objectid + PREFIX_PATH + path);
	}
	@Override
	protected void checkDescription(String description) {
		//checks cannot be done on non-public test-classes
	}
}

//Mock
class TIncEffectus extends IncEffectus {
	public TIncEffectus() {
	}
	public TIncEffectus(Res res, Number increase) {
		super(new TIncRes(res.getType().toString(), res.getObjectid().toString(), res.getPath()), increase);
	}
}
enum Action{
	CHANGE_END, 
	CHANGE_STATUS, 
	CHANGE_SALDO;
}
/*
 * try to simulate a simple insurance organization
 */
class Contract {
	String id;
	Tarif tarif;
	Date start;
	Date end;
	Number value;
	
	Partner partner;
	enum Tarif {LifeInsurance, DisabilityInsurance}
	Set<Account> accounts;
	
	public Date getEnd() {
		return end;
	}
	public void setEnd(Date end) {
		this.end = end;
	}
	public Tarif getTarif() {
		return tarif;
	}
	public void setTarif(Tarif tarif) {
		this.tarif = tarif;
	}
	public Partner getPartner() {
		return partner;
	}
	public void setPartner(Partner partner) {
		this.partner = partner;
	}
	public Set<Account> getAccounts() {
		return accounts;
	}
	public void setAccounts(Set<Account> accounts) {
		this.accounts = accounts;
	}
	public Number getValue() {
		return value;
	}
	public void setValue(Number value) {
		this.value = value;
	}
}

class Partner {
	String id;
	String name;
}
class Account {
	AccountType type;
	long saldo;
	public AccountType getType() {
		return type;
	}
	public void setType(AccountType type) {
		this.type = type;
	}
	public long getSaldo() {
		return saldo;
	}
	public void setSaldo(long saldo) {
		this.saldo = saldo;
	}
	enum AccountType {PAYMENT, COMMISSION, CAPACITY}
}