package sc.fiji.jython.autocompletion;

import java.util.List;

public interface DotAutocompletions {
	default public String getClassname() {
		return null;
	}
	public List<String> get();
}