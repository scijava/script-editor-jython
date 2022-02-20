package org.scijava.plugins.scripteditor.jython;

public class JythonDev
{
	static public int SILENT = 0,
					  MESSAGES = 1,
					  ERRORS = 2;
	
	/** Adjust value to see debugging information, including:
	 * SILENT: nothing is printed at all.
	 * MESSAGES: prints light messages indicating some internals and the kinds of errors.
	 * ERRORS: prints everything including exceptions.
	 */
	static public int debug = MESSAGES;
	
	static public final void print(Object s) {
		if (debug >= MESSAGES) System.out.println(s);
	}
	
	static public final void printError(Throwable e) {
		if (debug >= ERRORS) e.printStackTrace();
	}
	
	static public final void printTrace(Object s) {
		if (debug >= ERRORS) System.out.println(s);
	}
	
	static public final void print(Object s, Throwable e) {
		if (debug >= ERRORS) {
			System.out.println(s);
			e.printStackTrace();
		} else if (debug >= MESSAGES) {
			System.out.println(s.toString() + " :: " + e.getMessage());
		}
	}

}
