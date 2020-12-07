package sc.fiji.jython.autocompletion;

import java.util.Collections;
import java.util.List;

public class EmptyDotAutocompletions implements DotAutocompletions {

	static private EmptyDotAutocompletions instance = new EmptyDotAutocompletions();
	
	private EmptyDotAutocompletions() {}
	
	static public EmptyDotAutocompletions instance() {
		return instance;
	}
	
	@Override
	public List<String> get() {
		return Collections.emptyList();
	}
	
	@Override
	public String toString() {
		return "EMPTY";
	}
}
