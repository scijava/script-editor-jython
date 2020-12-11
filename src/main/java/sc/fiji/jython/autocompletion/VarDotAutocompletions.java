package sc.fiji.jython.autocompletion;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class VarDotAutocompletions implements DotAutocompletions {
	String className;
	public VarDotAutocompletions(final String className) {
		this.className = className;
	}
	@Override
	public String getClassname() {
		return this.className;
	}
	@Override
	public List<String> get() {
		final List<String> ac = new ArrayList<>();
		if (null != this.className) {
			try {
				final Class<?> c = Class.forName(this.className);
				for (final Field f: c.getFields())
					ac.add(f.getName());
				for (final Method m: c.getMethods())
					ac.add(m.getName()); // TODO could do a parameter-driven autocompletion
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return ac;
	}
	@Override
	public String toString() {
		return "VarDotAutocompletions: " + this.className;
	}
}