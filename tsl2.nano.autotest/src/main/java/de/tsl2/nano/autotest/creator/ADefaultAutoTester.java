package de.tsl2.nano.autotest.creator;

import static org.mockito.Mockito.mock;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.mockito.MockitoAnnotations;

import de.tsl2.nano.core.util.Util;

public class ADefaultAutoTester {
    public static final String KEY_AUTOTEST_INITMOCKITO = "tsl2nano.autotest.initmockito";
    public static final String DEFAULT_MOCK_CLASSNAMES[] = {
            "javax.persistence.Inject",
            "javax.persistence.EntityManager",
            "javax.persistence.PersistenceContext",
            "javax.annotation.Resource",
            "javax.ejb.EJB",
            "jakarta.persistence.Inject",
            "jakarta.persistence.EntityManager",
            "jakarta.persistence.PersistenceContext",
            "javax.ws.rs.client.Client"
    };

    @Before
    public void setUp() {
        if (Boolean.getBoolean(System.getProperty(KEY_AUTOTEST_INITMOCKITO, "true"))) {
            provideDefaultMocks(this);
        }
    }

    public Map<Class, Object> provideDefaultMocks(Object testInstance) {
        MockitoAnnotations.initMocks(this);

        Map<Class, Object> mocks = new HashMap<>();
        String mockClassNames[] = System
                .getProperty("tsl2.nano.auotest.defaultmocks", Arrays.toString(DEFAULT_MOCK_CLASSNAMES)).split(",");
        for (int i = 0; i < mockClassNames.length; i++) {
            final String name = mockClassNames[i];
            Class mockCls = Util.trY(() -> Thread.currentThread().getContextClassLoader().loadClass(name), false);
            if (mockCls != null) {
                mocks.put(mockCls, initMockObject(mock(mockCls)));
            }
        }
        return mocks;
    }

    // TODO use function overloading to avoid if-else blocks
    // with mockito we have a problem on generalizing providing results
    // perhaps it's better to use own proxying
    public <T> T initMockObject(T mockObject) {
        if (mockObject.getClass().isAssignableFrom(getClass() /* TODO: which class without dependency???*/)) {
            // when(mock)
        }
        return mockObject;
    }
}
