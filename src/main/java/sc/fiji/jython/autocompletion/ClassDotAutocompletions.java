package sc.fiji.jython.autocompletion;

import java.util.ArrayList;
import java.util.List;

public class ClassDotAutocompletions extends DefVarDotAutocompletions {
	final List<String> superclassNames,       // List of superclasses
	                   dotAutocompletions; // List of class methods and fields
	
	public ClassDotAutocompletions(final String fnName, List<String> superclassNames,
			final List<String> argumentNames, final List<String> dotAutocompletions, final Scope class_scope) {
		super(fnName, null, argumentNames, class_scope);
		this.superclassNames = superclassNames;
		this.dotAutocompletions = dotAutocompletions;
	}
	
	@Override
	public List<String> get() {
		final List<String> ac = new ArrayList<>(this.dotAutocompletions);
		for (final String className: this.superclassNames) {
			List<String> fm = DotAutocompletions.getPublicFieldsAndMethods(className);
			if (fm.isEmpty()) {
				// Maybe it's a python class
				// get them all via the org.python.indexer.Indexer and Builtins classes, see StaticDotAutocompletions
				System.out.println("Don't know yet how to handle class " + className);
			}
			ac.addAll(fm);
		}
		return ac;
	}
	
	public void put(final String name) {
		if (this.dotAutocompletions.contains(name)) return; // list search OK: very low N
		this.dotAutocompletions.add(name);
	}
	
	/** Return a duplicate with everything the same (same object ref) except for the dotAutocompletions list which is a copy
	 *  into which the provided vars are added. */
	public ClassDotAutocompletions plus(final List<String> vars) {
		final List<String> da = new ArrayList<String>(vars);
		da.addAll(this.dotAutocompletions);
		return new ClassDotAutocompletions(this.fnName, this.superclassNames, this.argumentNames, da, this.scope);
	}
	
	@Override
	public String toString() {
		return "ClassDotAutocompletions: " + this.fnName + "(" + String.join(", ", this.argumentNames) + ")" + " -- " + String.join(", ", this.get());
	}
}