package de.tsl2.nano.serviceaccess.test;

import static de.tsl2.nano.service.util.finder.Finder.all;
import static de.tsl2.nano.service.util.finder.Finder.and;
import static de.tsl2.nano.service.util.finder.Finder.between;
import static de.tsl2.nano.service.util.finder.Finder.example;
import static de.tsl2.nano.service.util.finder.Finder.expression;
import static de.tsl2.nano.service.util.finder.Finder.holder;
import static de.tsl2.nano.service.util.finder.Finder.inSelection;
import static de.tsl2.nano.service.util.finder.Finder.member;
import static de.tsl2.nano.service.util.finder.Finder.not;
import static de.tsl2.nano.service.util.finder.Finder.or;
import static de.tsl2.nano.service.util.finder.Finder.orderBy;
import static de.tsl2.nano.service.util.finder.Finder.union;

import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.ScheduleExpression;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.junit.Before;
import org.junit.Test;

import de.tsl2.nano.action.IAction;
import de.tsl2.nano.bean.BeanProxy;
import de.tsl2.nano.collection.ListSet;
import de.tsl2.nano.execution.Profiler;
import de.tsl2.nano.service.feature.FeatureFactory;
import de.tsl2.nano.service.schedule.IJobScheduleService;
import de.tsl2.nano.service.util.ServiceUtil;
import de.tsl2.nano.service.util.finder.Finder;
import de.tsl2.nano.serviceaccess.ServiceFactory;
import de.tsl2.nano.serviceaccess.aas.ConsoleCallbackHandler;
import de.tsl2.nano.serviceaccess.aas.module.AbstractLoginModule;
import de.tsl2.nano.serviceaccess.aas.principal.AbstractPrincipalAction;
import de.tsl2.nano.util.DateUtil;

/*
 * Copyright © 2002-2008 Thomas Schneider
 * Alle Rechte vorbehalten.
 * Weiterverbreitung, Benutzung, Vervielfältigung oder Offenlegung,
 * auch auszugsweise, nur mit Genehmigung.
 *
 */

/**
 * tests all functions of service access and aas.<br>
 * The tests are only startable in a project context, having a project.properties and a login context. a jndi.properties
 * file on root of the classpath must reference an initialcontext factory class, that is inside the classpath! e.g.:
 * SerialInitContextFactory of suns glassfish (appserv-rt.jar)
 * 
 * Please look at {@link #testAuthorization()} to configure your test launch.
 * 
 * @author TS
 */
public class ServiceAccessTest {
    @Before
    public void setUp() {
        // System.setProperty(ExternPluginClassloader.KEY_APPLICATION_BUNDLE,
        // "testclient");
        // Thread.currentThread().setContextClassLoader(
        // new ExternPluginClassloader());
        ServiceFactory.createInstance(ServiceAccessTest.class.getClassLoader(), ServiceFactory.NO_JNDI);

        //works only in development-environment with bin-dir!
        final URL jaasUrl = this.getClass().getClassLoader().getResource("jaas-login.config");
        System.setProperty("java.security.auth.login.config", jaasUrl.toString());
        final URL policyUrl = this.getClass().getClassLoader().getResource("logincontext.policy");
        System.setProperty("java.security.manager", "");
        System.setProperty("java.security.policy", policyUrl.toString());
    }

    @Test
    public void testServiceAccess() throws Exception {
        // we have no server - so create at least one test service
        final Map<String, Object> initialServices = new Hashtable<String, Object>();
        final ITestService service = (ITestService) BeanProxy.createBeanImplementation(ITestService.class,
            null,null,
            this.getClass().getClassLoader());
        initialServices.put(ITestService.class.getName(), service);
        ServiceFactory.instance().setInitialServices(initialServices);
        final Collection<?> findTestObjects = ServiceFactory.instance()
            .getService(ITestService.class)
            .findTestObjects();
        // TODO: fill the proxy service with teams.
        log(findTestObjects);
    }

    /**
     * This test attempts to authenticate a user and reports whether or not the authentication was successful.
     * 
     * <pre>
     * -Djava.security.debug=all
     * -Djava.security.manager
     * -Djava.security.policy=file:./bin/logincontext.policy
     * -Djava.security.auth.login.config=jaas-login.config
     * </pre>
     * 
     * <p>
     * This LoginModule only recognizes one user: testUser testUser's password is: testPassword
     * 
     * @see AbstractLoginModule
     * @throws Exception
     */
    @Test
    public void testAuthentication() throws Exception {
        log("SecurityManager: " + System.getSecurityManager());
        final String user[] = new String[] { "wrongUser", "testUser", "testUser" };
        final String password[] = new String[] { "testPassword", "wrongPassword", "testPassword" };
        // Obtain a LoginContext, needed for authentication. Tell it
        // to use the LoginModule implementation specified by the
        // entry named "Sample" in the JAAS login configuration
        // file and to also use the specified CallbackHandler.
        LoginContext lc = null;
        try {
            lc = new LoginContext("LoginJaas", new ConsoleCallbackHandler());
        } catch (final LoginException le) {
            fail("Cannot create LoginContext. " + le.getMessage());
        } catch (final SecurityException se) {
            fail("Cannot create LoginContext. " + se.getMessage());
        }

        // the user has 3 attempts to authenticate successfully
        int i;
        for (i = 0; i < 3; i++) {
            try {
                System.setProperty(AbstractLoginModule.PROP_USER, user[i]);
                System.setProperty(AbstractLoginModule.PROP_PASSWORD, password[i]);
                // attempt authentication
                lc.login();

                // if we return with no exception, authentication succeeded
                break;

            } catch (final LoginException le) {

                System.err.println("Authentication failed:");
                System.err.println("  " + le.getMessage());
                try {
                    Thread.currentThread();
                    Thread.sleep(3000);
                } catch (final Exception e) {
                    // ignore
                }

            }
        }

        // did they fail three times?
        if (i == 3) {
            fail("Sorry - no login");
        }

        log("Authentication succeeded!");
    }

    @Test
    public void testAuthorization() throws Exception {
        testAuthentication();

        final IAction<?> actionExit = new AbstractPrincipalAction<Object>("action.exit", "Exit", "Exit") {
            @Override
            public Object action() throws Exception {
                log("..starting the empty action...");
                return null;
            }
        };
        final IAction<?> actionAny = new AbstractPrincipalAction<Object>("action.any", "...", "...") {
            @Override
            public Object action() throws Exception {
                log("..starting any action...");
                return null;
            }
        };
        // start the authorized action (see logincontext.policy)
        actionExit.activate();
        // start the action without permission
        try {
            actionAny.activate();
            fail(actionAny.getId() + " should be without permission!");
        } catch (final SecurityException e) {
            // Ok
        }
    }

    @Test
    public void testFeatureFactory() {
        //initialize it
        FeatureFactory.createInstance(null, this.getClass().getClassLoader(), null, null, null, false);
        if (FeatureFactory.instance().isEnabled(ITestService.class)) {
            fail("isEnabled() should return false! No TestServicImpl.class available!");
        }
        if (FeatureFactory.instance().getImpl(IAction.class).isDefault()) {
            fail("the empty feature-proxy implementation should return false");
        }

        //initialize it: mustImplement = true
        FeatureFactory.createInstance(null, this.getClass().getClassLoader(), null, null, null, true);
        if (FeatureFactory.instance().isEnabled(ITestService.class)) {
            fail("isEnabled() should return false! No TestServicImpl.class available!");
        }
        try {
            FeatureFactory.instance().getImpl(IAction.class).isDefault();
            fail("mustImplement ist true --> An exception should be thrown!");
        } catch (final Exception e) {
            //--> ok
        }
    }

    protected void log(Object message) {
        System.out.println(message);
    }

    protected Object fail(String message) {
        throw new RuntimeException(message);
    }

    @Test
    public void testGenericService() {
        /*
         * we prepare an own EntityManager - to check the queries of GenericService
         */
//        final EntityManager em = (EntityManager) BeanProxy.createBeanImplementation(EntityManager.class,
//            null,
//            this.getClass().getClassLoader());

//        GenericServiceBean serviceBean = new GenericServiceBean() {
//            @Override
//            protected void checkContextSecurity() {
//                entityManager = em;
//                super.checkContextSecurity();
//            }
//
//        };

        /*
         * now we can test! 
         * all calls will throw an exception, because no query will be created
         */
//        String shouldBe = "select s from ITestService";
//        try {
//            serviceBean.findAll(ITestService.class);
//        } catch (Exception e) {
//            Assert.assertEquals(shouldBe, BeanProxy.getLastInvokationArgs(em, "createQuery"));
//        }
    }

    @Test
    public void testQuery() {
        Person p1 = new Person("test", DateUtil.getDate(1970, 1, 1), new Address("Berlinerstr.1",
            "Frankfurt"));
        Person p2 = new Person("test", DateUtil.getDate(1979, 12, 31), new Address("Berlinerstr.1",
            "Frankfurt"));
        Team team = new Team("team1", new ListSet(p1, p2));
        Collection<Object> parameter = new LinkedList<Object>();
        LinkedList<Class<Object>> lazyRelations = new LinkedList<Class<Object>>();
        Profiler.si().starting(Finder.class, "createQuery");
        String qStr = Finder.createQuery(parameter,
            lazyRelations,
            all(Person.class),
            and(expression(Person.class, " 1 = 1 ", false, null)),
            between(p1, p2),
            or(example(p1)),
            or(member(team, Person.class, "player")),
            not(expression(Person.class, " myfield = myVariable ", false, null)),
            and(inSelection(Person.class, "name", Arrays.asList("test, test1"))),
            union(Person.class),
            holder(p1, Team.class, "player"),
            orderBy(Person.class, "+name", "-birthday"));
        Profiler.si().ending(Finder.class, "createQuery");
        log(ServiceUtil.getLogInfo(new StringBuffer(qStr), parameter));
        
        //no result checking available
    }

    @Test
    public void testScheduling() {
        //define a process
        final List<Runnable> callbacks = Arrays.asList((Runnable) new Runnable() {
            @Override
            public void run() {
                System.out.println("JobScheduleService started this at " + new Date());
            }

        });

        //define a timer
        final ScheduleExpression se = new ScheduleExpression();
        se.dayOfWeek(1);
        se.hour(22);
        //start the schedule
        final IJobScheduleService service = ServiceFactory.instance().getService(IJobScheduleService.class);
        service.createJob("testjob", se, true, callbacks);
    }

    /*
     * Some Test beans
     */
    @Entity
    public class Person {
        @Id
        String name;
        @Column
        Date birthday;
        @Column
        Address address;

        /**
         * constructor
         * 
         * @param name
         * @param birthday
         * @param addresses
         */
        public Person(String name, Date birthday, Address address) {
            super();
            this.name = name;
            this.birthday = birthday;
            this.address = address;
        }

        /**
         * @return Returns the name.
         */
        public String getName() {
            return name;
        }

        /**
         * @param name The name to set.
         */
        public void setName(String name) {
            this.name = name;
        }

        /**
         * @return Returns the birthday.
         */
        public Date getBirthday() {
            return birthday;
        }

        /**
         * @param birthday The birthday to set.
         */
        public void setBirthday(Date birthday) {
            this.birthday = birthday;
        }

        /**
         * @return Returns the addresses.
         */
        public Address getAddress() {
            return address;
        }

        /**
         * @param addresses The addresses to set.
         */
        public void setAddress(Address address) {
            this.address = address;
        }

    }

    @Entity
    public class Address {
        @Id
        String street;
        @Column
        String city;

        /**
         * constructor
         * 
         * @param street
         * @param city
         */
        public Address(String street, String city) {
            super();
            this.street = street;
            this.city = city;
        }

        /**
         * @return Returns the street.
         */
        public String getStreet() {
            return street;
        }

        /**
         * @param street The street to set.
         */
        public void setStreet(String street) {
            this.street = street;
        }

        /**
         * @return Returns the city.
         */
        public String getCity() {
            return city;
        }

        /**
         * @param city The city to set.
         */
        public void setCity(String city) {
            this.city = city;
        }
    }
    @Entity
    public class Team {
        @Id
        String name;
        @OneToMany
        Set<Person> player;
        /**
         * constructor
         * @param name
         * @param player
         */
        public Team(String name, Set<Person> player) {
            super();
            this.name = name;
            this.player = player;
        }
        /**
         * @return Returns the name.
         */
        public String getName() {
            return name;
        }
        /**
         * @param name The name to set.
         */
        public void setName(String name) {
            this.name = name;
        }
        /**
         * @return Returns the player.
         */
        public Set<Person> getPlayer() {
            return player;
        }
        /**
         * @param player The player to set.
         */
        public void setPlayer(Set<Person> player) {
            this.player = player;
        }
    }
}
