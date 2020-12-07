package sc.fiji.jython.autocompletion;

import java.util.Collections;
import java.util.List;

public class EmptyDotAutocompletions implements DotAutocompletions {
	@Override
	public List<String> get() {
		return Collections.emptyList();
	}
	
	@Override
	public String toString() {
		return "EMPTY";
	}
}
