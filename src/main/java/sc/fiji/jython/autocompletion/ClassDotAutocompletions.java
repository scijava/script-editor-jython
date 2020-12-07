package sc.fiji.jython.autocompletion;

import java.util.List;

public class ClassDotAutocompletions implements DotAutocompletions {
	// List of class methods and fields
	final List<String> dotAutocompletions;
	
	public ClassDotAutocompletions(final List<String> dotAutocompletions) {
		this.dotAutocompletions = dotAutocompletions;
	}
	
	public List<String> get() {
		return this.dotAutocompletions;
	}
	
	@Override
	public String toString() {
		return "ClassDotAutocompletions: " + String.join(", ", this.dotAutocompletions);
	}
}