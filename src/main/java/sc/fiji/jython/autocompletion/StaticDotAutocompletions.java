package sc.fiji.jython.autocompletion;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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
			String msg = "";
			try {
				// Check first if it's a python module
				final NModuleType module = Scope.loadPythonModule(this.className); // Scope.indexer.getBuiltinModule(this.className);
				System.out.println("module is: " + module);
				if (null != module) {
					ac.addAll(module.getTable().keySet());
					// Not need to remove: a file system watcher will do so when the module file is updated or deleted.
					// Scope.indexer.moduleTable.remove(this.className);
					return ac;
				} else {
					msg += "Not a python module: " + this.className;
				}
				// Or a java class:
				try {
					fieldsAndStaticMethodsInto(Class.forName(this.className), ac);
					return ac;
				} catch (ClassNotFoundException cnfe) {
					msg += "\nCannot find java class " + this.className;
				}
				// Or a static method of a java class
				try {
					final int idot = this.className.lastIndexOf('.');
					final String name = this.className.substring(idot + 1);
					final Class<?> c = Class.forName(this.className.substring(0, idot));
					// There could be more than one method, with more than one return type
					for (final Class<?> r: Arrays.asList(c.getMethods()).stream()
							.filter(m -> m.getName().equals(name))
							.map(m -> m.getReturnType())
							.distinct()
							.collect(Collectors.toList())) {
						fieldsAndMethodsInto(r, ac);
					}
					return ac;
				} catch (ClassNotFoundException cnfe) {
					msg += "\nCannot derive static method or field from " + this.className;
				}
				
				if (null != msg) {
					System.out.println(msg);
				}
			} catch (Exception e) {
				if (null != msg) System.out.println(msg);
				e.printStackTrace();
			}
		}
		return ac;
	}
	
	private void fieldsAndStaticMethodsInto(final Class<?> c, final List<String> ac) {
		for (final Field f: c.getDeclaredFields())
			if (Modifier.isStatic(f.getModifiers()))
				ac.add(f.getName());
		for (final Method m: c.getDeclaredMethods())
			if (Modifier.isStatic(m.getModifiers()))
				ac.add(m.getName() + "("); // TODO could do a parameter-driven autocompletion
	}
	
	private void fieldsAndMethodsInto(final Class<?> c, final List<String> ac) {
		for (final Field f: c.getDeclaredFields())
			if (!Modifier.isStatic(f.getModifiers()))
				ac.add(f.getName());
		for (final Method m: c.getDeclaredMethods())
			if (!Modifier.isStatic(m.getModifiers()))
				ac.add(m.getName() + "("); // TODO could do a parameter-driven autocompletion
	}
	
	@Override
	public String toString() {
		return "StaticDotAutocompletions: " + className + " -- " +String.join(", ", get());
	}
}