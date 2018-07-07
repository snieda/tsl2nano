package tsl2.nano.cursus;


import static org.junit.Assert.assertEquals;
import static tsl2.nano.cursus.effectus.Effectree.*;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import de.tsl2.nano.core.util.DateUtil;
import de.tsl2.nano.core.util.ENVTestPreparation;
import tsl2.nano.cursus.effectus.Effectree;
import tsl2.nano.cursus.effectus.IncEffectus;

/**
 * Test-Implementation for a complete Action-based Data-System.
 * Advantage:
 *  - less data to store
 *  - complete change history 
 * Disadvantage:
 */
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
    public void testContract() throws Exception {
    	Effectree.instance().addEffects(Contract.class, "contract.end", effect(Contract.class, "contract.value", IncEffectus.class));
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
    	//TODO: why this effect will not be found?
//    	assertEquals(5d, origin.value);
    	assertEquals(2, origin.accounts.size());
    	
    	
    }
}
class TRes extends Res<Contract, Object> {
	public TRes(String type, String objectid, String path) {
		super(type, objectid, path);
	}

	@Override
	protected Contract materialize(String description) {
		return CursusTest.origin;
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