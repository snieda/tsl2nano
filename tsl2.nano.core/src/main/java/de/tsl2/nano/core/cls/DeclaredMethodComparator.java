package de.tsl2.nano.core.cls;

import java.lang.reflect.Method;
import java.util.Comparator;

/**
 * If a method is overwritten and declares another return-type, the overwritten
 * method with the new return-type should come first! Used by BeanClass to evaluate all methods.
 * 
 * @author Tom
 */
public class DeclaredMethodComparator implements Comparator<Method> {

	@Override
	public int compare(Method m1, Method m2) {
		if (m1.equals(m2))
			return 0;
		if (m1.getDeclaringClass().equals(m2.getDeclaringClass())) {
			if (m1.getName().equals(m2.getName())) {
				if (!m1.getReturnType().equals(m2.getReturnType()))
					return m2.getReturnType().isAssignableFrom(m1.getReturnType()) ? -1
							: 1;
				else
					return compareName(m1, m2);
			} else
				return compareName(m1, m2);
		}
		if (!m1.getName().equals(m2.getName()))
			return compareName(m1, m2);
		// only on hierarchy...
		boolean m1TopOfm2 = m2.getDeclaringClass().isAssignableFrom(m1.getDeclaringClass());
		// TODO: why it throws a 'java.lang.IllegalArgumentException: Comparison method
		// violates its general contract!'
		return m1TopOfm2 ? -1 
				: 1;
	}

	private int compareName(Method m1, Method m2) {
		return m1.getName().compareTo(m2.getName());
	}

}
