package sc.fiji.jython.autocompletion;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public interface DotAutocompletions {
	
	static public final DotAutocompletions EMPTY = new EmptyDotAutocompletions();
	
	default public String getClassname() {
		return null;
	}
	
	public List<String> get();
	
	static public List<String> getPublicFieldsAndMethods(final String className) {
		final List<String> ac = new ArrayList<>();
		if (null != className) {
			try {
				final Class<?> c = Class.forName(className);
				for (final Field f: c.getFields())
					ac.add(f.getName());
				for (final Method m: c.getMethods())
					ac.add(m.getName()); // TODO could do a parameter-driven autocompletion
			} catch (Exception e) {
				System.out.println("Could not load class " + className + " :: " + e.getMessage());
			}
		}
		return ac;
	}
}