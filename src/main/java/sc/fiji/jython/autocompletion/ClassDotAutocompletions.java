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
	
	/** Make this be cda plus its own dotAutocompletions. */
	public void mutateIntoPlus(final ClassDotAutocompletions cda) 
	{
		this.className = cda.className;
		this.fnName = cda.fnName;
		this.superclassNames.clear();
		this.superclassNames.addAll(cda.superclassNames);
		this.dotAutocompletions.addAll(cda.dotAutocompletions);
		this.argumentNames.clear();
		this.argumentNames.addAll(cda.argumentNames);
		this.scope = cda.scope;
	}
	
	@Override
	public String toString() {
		return "ClassDotAutocompletions: " + this.fnName + "(" + String.join(", ", this.argumentNames) + ")" + " -- " + String.join(", ", this.get());
	}
}