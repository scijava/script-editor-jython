package sc.fiji.jython.autocompletion;

import java.util.List;

public interface DotAutocompletions {
	
	static public final DotAutocompletions EMPTY = new EmptyDotAutocompletions();
	
	default public String getClassname() {
		return null;
	}
	public List<String> get();
}