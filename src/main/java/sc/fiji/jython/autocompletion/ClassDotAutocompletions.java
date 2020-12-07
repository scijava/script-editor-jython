package sc.fiji.jython.autocompletion;

import java.util.ArrayList;
import java.util.List;

public class ClassDotAutocompletions extends DefVarDotAutocompletions {
	final List<String> superclassNames,       // List of superclasses
	                   dotAutocompletions; // List of class methods and fields
	
	public ClassDotAutocompletions(final String fnName, List<String> superclassNames, final List<String> argumentNames, final List<String> dotAutocompletions) {
		super(fnName, null, argumentNames);
		this.superclassNames = superclassNames;
		this.dotAutocompletions = dotAutocompletions;
	}
	
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
	
	@Override
	public String toString() {
		return "ClassDotAutocompletions: " + this.fnName + "(" + String.join(", ", this.argumentNames) + ")" + " -- " + String.join(", ", this.get());
	}
}