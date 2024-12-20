package de.tsl2.nano.cursus;

import static de.tsl2.nano.cursus.effectus.Effectree.effect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import de.tsl2.nano.core.util.CollectionUtil;
import de.tsl2.nano.core.util.ConcurrentUtil;
import de.tsl2.nano.core.util.DateUtil;
import de.tsl2.nano.core.util.ENVTestPreparation;
import de.tsl2.nano.cursus.IConsilium.Priority;
import de.tsl2.nano.cursus.effectus.Effectree;
import de.tsl2.nano.cursus.effectus.IncEffectus;

/**
 * Test-Implementation for a complete Action-based Data-System.
 * Advantage:
 *  - less data to store
 *  - complete change history 
 * Disadvantage:
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class CursusTest implements Serializable /* only for inner-classes...*/ {
	Contract origin;

	@BeforeClass
	public static void setUp() {
		ENVTestPreparation.setUp("cursus", false);
	}

	@AfterClass
	public static void tearDown() {
		ENVTestPreparation.tearDown();
	}

	@Before
	public void setUpTest() {
		origin = new Contract();
		ConcurrentUtil.setCurrent(origin);
	}

	@Test
	public void testContract() throws Exception {
		IConsilium[] consilii = process(createConsilii());

		assertEquals(
				DateUtil.getDate((String) ((Exsecutio) consilii[0].getExsecutios().iterator().next()).mutatio.getNew()),
				origin.end);
		assertEquals(Long.valueOf((String) ((Exsecutio) consilii[1].getExsecutios().iterator().next()).mutatio.getNew())
				.longValue(), ((List<Account>) origin.accounts).get(1).saldo);
		assertEquals(IConsilium.Status.INACTIVE, consilii[2].getStatus());
		assertEquals(5d, origin.value);
		assertEquals(2, origin.accounts.size());
	}

	@Test
	public void testObsidio() throws Exception {
		Timer blockTime = new Timer(DateUtil.getYesterday(), DateUtil.getToday());
		IConsilium[] consilii = createConsilii();

		Object consiliumID = consilii[0].getName();
		Consilium obsidio = new Consilium("BLOCKER", blockTime, Priority.HIGHEST,
				new Obsidio("BLOCKEREX", consiliumID, blockTime, new Grex(Contract.class, "contract.end", "1")));
		process(CollectionUtil.concat(consilii, new IConsilium[] { obsidio }));
		assertEquals(null, origin.end);
	}

	@Test
	public void testGrex() throws Exception {
		Grex<Contract, Object> grex = new Grex<>(Contract.class, "end", "1");
		Set<? extends Res<Contract, Object>> parts = grex.createParts();
		assertEquals(1, parts.size());
		assertEquals(new Res(Contract.class, "1", "end"), parts.iterator().next());

		try {
			grex.findParts(null);
			fail("BeanContainer should not be initialized");
		} catch (Throwable e) {
			assertTrue(e.getMessage().startsWith("beancontainer not initialized"));
		}

	}

	private IConsilium[] process(IConsilium[] consilii) {
		Effectree.instance().addEffects(Contract.class, "contract.end",
				effect(Contract.class, "contract.value", TIncEffectus.class, 5));
		System.out.println(Effectree.instance().toString());
		Date lastYear = DateUtil.add(DateUtil.getToday(), Calendar.YEAR, -1);
		new Processor().run(DateUtil.getStartOfYear(lastYear), DateUtil.getToday(), consilii);
		return consilii;
	}

	private Consilium[] createConsilii() {
		int curYear = DateUtil.getFieldOfDate(DateUtil.getToday(), Calendar.YEAR);
		Consilium consilii[] = {
				new Consilium("end", new Timer(DateUtil.getYesterday(), null), Consilium.Priority.HIGHEST,
						new Exsecutio(Action.CHANGE_END.name(),
								new Mutatio(DateUtil.toLocaleFormat("2019-01-01"),
										new TRes(Contract.class.getName(), "1", "contract.end")),
								null)),
				new Consilium("saldo", new Timer(DateUtil.getToday(), null), Consilium.Priority.NORMAL,
						new Exsecutio(Action.CHANGE_SALDO.name(),
								new Mutatio("2000",
										new TRes(Contract.class.getName(), "1",
												"contract.accounts[?type=CAPACITY].saldo")),
								null)),
				new Consilium("future", new Timer(DateUtil.getTomorrow(), null), Consilium.Priority.NORMAL,
						new Exsecutio(Action.CHANGE_SALDO.name(),
								new Mutatio("XXXX",
										new TRes(Contract.class.getName(), "1",
												"contract.accounts[type=CAPACITY].saldo")),
								null)),
				new Consilium("auto",
						new Timer(DateUtil.getDate(curYear - 1, 01, 1), DateUtil.getDate(curYear, 12, 1),
								Calendar.MONTH, 1),
						Consilium.Priority.LOW,
						new Exsecutio(Action.CHANGE_SALDO.name(),
								new Mutatio("2001",
										new TRes(Contract.class.getName(), "1",
												"contract.accounts[?type=PAYMENT].saldo")),
								null)),
		};
		return consilii;
	}
}

//Mock
class TRes extends Res<Contract, Object> {
	public TRes() {
		super();
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
		return ConcurrentUtil.getCurrent(Contract.class);
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

enum Action {
	CHANGE_END,
	CHANGE_STATUS,
	CHANGE_SALDO;
}

/*
 * try to simulate a simple insurance organization
 */
class Contract implements Serializable {
	String id;
	Tarif tarif;
	Date start;
	Date end;
	Number value;

	Partner partner;

	enum Tarif {
		LifeInsurance, DisabilityInsurance
	}

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

class Partner implements Serializable {
	String id;
	String name;
}

class Account implements Serializable {
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

	enum AccountType {
		PAYMENT, COMMISSION, CAPACITY
	}
}