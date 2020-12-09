package sc.fiji.jython.autocompletion;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import org.python.indexer.types.NModuleType;

public class StaticDotAutocompletions implements DotAutocompletions
{	
	final String className;
	public StaticDotAutocompletions(final String className) {
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
				// Check first if it's a python module
				final NModuleType module = Scope.loadPythonModule(this.className); // Scope.indexer.getBuiltinModule(this.className);
				if (null != module) {
					ac.addAll(module.getTable().keySet());
					// Not need to remove: a file system watcher will do so when the module file is updated or deleted.
					// Scope.indexer.moduleTable.remove(this.className);
					return ac;
				}
				// Or a java class:
				final Class<?> c = Class.forName(this.className);
				for (final Field f: c.getDeclaredFields())
					if (Modifier.isStatic(f.getModifiers()))
						ac.add(f.getName());
				for (final Method m: c.getDeclaredMethods())
					if (Modifier.isStatic(m.getModifiers()))
						ac.add(m.getName()); // TODO could do a parameter-driven autocompletion
			} catch (ClassNotFoundException cnfe) {
				System.out.println("Cannot find java class or python module " + this.className);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return ac;
	}
	
	@Override
	public String toString() {
		return "StaticDotAutocompletions: " + className + " -- " +String.join(", ", get());
	}
}