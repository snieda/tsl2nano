# tsl2.nano.aspects providing coverup/mocking, loggin, profiling through aspectj

NOTE: NOT WORKING YET!

* Problem, setting a proxy to field types with that interface
* annotated covering does not work in cause of wrong this_ instance in pjp. we have a workaround setting a member holding this instance. this will not work on all situations (different threads, aspects in one method etc...)

## Features

* AbstractAspect can log/mock/profile any java aspect, configured by aop.xml
* AspectCover provides implementation/annotation based covers: mocking / enhancing /transforming / simple logging

## Details

* You are able to analyse any java project with the AbstractAspect. To do so, you only have to configure your pointcuts in the file *META-INF/aop.xml*
    * there are three pointcuts pre-configured: logging, mocking, profiling
* On Tests etc. , you may use the AspectCover, providing annotations like *@CoverBefore* *@CoverBody* *@CoverAfter* to do argument transformation or other extensions.
    * you can provide your own replacement implenentation, or something to be done before or/and after.
    * your implementatino may be your own static class with a cover annotation (s.a.), or inside the calling instance (e.g. in your test class)

## Examples

### Analysing an Application where you don't have the sources

If you put the file *META-INF/aop.xml* inside your classpath with content:

    <aspectj>
        <aspects>
            <aspect name="de.tsl2.nano.aspect.AspectCover"/>
            <!-- define a concrete aspect inline -->

            <concrete-aspect name="de.tsl2.nano.aspect.ConcreteAspectCover"
                extends="de.tsl2.nano.aspect.AbstractAspect">
                <pointcut name="trace" expression="execution(public * XXXX(..))"/>
                <pointcut name="profile" expression="execution(public * YYYY(..))"/>
                <pointcut name="mock" expression="execution(public * ZZZZ(..))"/>
            </concrete-aspect>
            <weaver options="-verbose -showWeaveInfo">
                <include within="my.package.path.*"/>
                <dump within="my.package.path.*" />
            </weaver>
        </aspects>
    </aspectj>

* all public methods inside *my.package.path* with name XXXX will be traced
* all public methods inside *my.package.path* with name YYYY will be profiled (time+mem)
* all public methods inside *my.package.path* with name ZZZZ will be mocked


### Using an own class with static methods that are annotated

having an annotated method like:

    @Cover(up = true, before = CoverStatic.class, body = CoverStatic.class, after = CoverStatic.class) 
    public boolean allowed() {
        return super.allowed();
    }

the framework will search for the following methods:

    interface CoverStatic {

        @CoverBefore
        static Object allowedCoverBefore(CoverArgs t) {
            ((Account1) t.target).before = "where";
            return null;
        }
        @CoverBody
        static boolean allowed(CoverArgs t) {
            ((Account1) t.target).body = "are";
            return false;
        }
        @CoverAfter
        static Object allowedCoverAfter(CoverArgs t) {
            ((Account1) t.target).after = "you";
            return null;
        }
    }

## Implementing the methods inside the caller class (like the Test class)

having an annotated method like:

        @Cover(up = true)
        public boolean allowed() {
            return false;
        }

the framework will search for the following methods in the caller class:

    @CoverBefore
    Object allowedCoverBefore(CoverArgs cargs) {
        before = "where";
        return null;
    }
    @CoverBody
    boolean allowed(CoverArgs cargs) {
        body = "are";
        return false;
    }
    @CoverAfter
    Object allowedCoverAfter(CoverArgs cargs) {
        after = "you";
        return null;
    }

the test or caller class may look like:

    public class AspectCoverAnnotationTest {
        @Cover // <-- important to internally call the aspect on field get/set to know the caller class!
        Account account = new Account2();

        String before, body, after;

        @Test
        public void testCoverBefore() {
            assertFalse(account.allowed());
            assertEquals("where", before);
        }

        @Test
        public void testCoverBody() {
            assertFalse(account.allowed());
            assertEquals("are", body);
        }

        @Test
        public void testCoverAfter() {
            assertFalse(account.allowed());
            assertEquals("you", after);
        }

    ...
    }

### Covering/Mocking all methods of a field

NOTE: does not work!

having an annotated field like:

    @Cover(up = true)
    IAccount iaccount = new Account(); //NOT WORKING YET: Proxy will not be set!!!

so all methods of that field will be mocked through a java proxy (working on the interface IAccount)
