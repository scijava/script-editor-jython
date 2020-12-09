package sc.fiji.jython.autocompletion;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.fife.ui.autocomplete.BasicCompletion;
import org.fife.ui.autocomplete.Completion;
import org.fife.ui.autocomplete.CompletionProvider;
import org.python.indexer.types.NModuleType;
import org.scijava.ui.swing.script.autocompletion.AutoCompletionListener;
import org.scijava.ui.swing.script.autocompletion.JythonAutocompletionProvider;

public class JythonAutoCompletions implements AutoCompletionListener
{
	static {
		// Register as listener for jython autocompletions
		JythonAutocompletionProvider.addAutoCompletionListener(new JythonAutoCompletions());
	}
	
	static private final Pattern assign = Pattern.compile("^([ \\t]*)(([a-zA-Z_][a-zA-Z0-9_ \\t,]*)[ \\t]+=[ \\t]+(.*))$"),
						         nameToken = Pattern.compile("^(.*?[ \\t]+|)([a-zA-Z_][a-zA-Z0-9_]+)$"),
						         dotNameToken = Pattern.compile("^(.*?[ \\t]+|)([a-zA-Z0-9_\\.\\[\\](){}]+)\\.([a-zA-Z0-9_]*)$"),
						     	 endingCode = Pattern.compile("^([ \\t]*)[^#]*?(.*?)[ \\t]*:[ \\t]*(#.*|)[\\n]*$"),
						     	 sysPathAppend = Pattern.compile("sys.path.append[ \\t]*[(][ \\t]*['\"](.*?)['\"][ \\t]*[)]"), // fragile to line breaks in e.g. .append
						    	 importStatement = Pattern.compile("^((from[ \\t]+([a-zA-Z0-9._]+)[ \\t]+|[ \\t]*)import[ \\t]+)([a-zA-Z0-9_., \\t]*)$");

	
	public JythonAutoCompletions() {}
	
	@Override
	public List<Completion> completionsFor(final CompletionProvider provider, String codeWithoutLastLine, final String lastLine, final String alreadyEnteredText) {
		
		// Replacing of text will start at crop, given the already entered text that is considered for replacement
		final int crop = lastLine.length() - alreadyEnteredText.length();
		
		// Query the lastLine to find out what needs autocompletion
		
		// Preconditions 1: can't expand when ending with any of: "()[]{},; "
		final char lastChar = lastLine.charAt(lastLine.length() -1);
		if (0 == lastLine.length() || "()[]{},; ".indexOf(lastChar) > -1)
			return Collections.emptyList();
		
		// Preconditions 2: codeWithoutLastLine has to be valid
		// Analyze last line of codeWithoutLastLine: if it ends with a ':', must add a "pass" to make it a valid code block
		// so that the ParserFacade can work
		if (codeWithoutLastLine.endsWith("\n")) {
			JythonScriptParser.print("codeWithoutLastLine ends with line break");
			final int priorLineBreak = codeWithoutLastLine.lastIndexOf('\n', codeWithoutLastLine.length() - 2);
			final String endingLine = codeWithoutLastLine.substring(priorLineBreak + 1);
			final Matcher me = endingCode.matcher(endingLine);
			if (me.find()) {
				codeWithoutLastLine = codeWithoutLastLine.substring(0, priorLineBreak + 1) + me.group(1) + me.group(2) + ": pass";
				JythonScriptParser.print("changed code to: \n" + codeWithoutLastLine + "\n###");
			}
		}
		
		// Check if there are any additions to the sys.path to search for custom modules
		try {
			final Matcher mpath = sysPathAppend.matcher(codeWithoutLastLine);
			while (mpath.find()) {
				final File path = new File(mpath.group(1));
				if (path.exists() && path.isDirectory() && !Scope.indexer.getLoadPath().stream().filter(s -> path.equals(new File(s))).findFirst().isPresent())
					Scope.indexer.addPath(path.getAbsolutePath());
			}
			JythonScriptParser.print("PYTHONPATH:\n" + String.join("\n", Scope.indexer.getLoadPath()));
		} catch (Exception e) {
			System.out.println("Failed to add path from sys.path.append expression.");
			e.printStackTrace();
		}
		
		// Situations to autocomplete:
		// 0) a python module import
		// 1) a plain name: delimited with space (or none) to the left, and without parentheses.
		// 2) a method or field: none or some text after a period.
		
		final Matcher m4 = importStatement.matcher(lastLine);
		if (m4.find()) {
			JythonScriptParser.print("JythonAutoCompletions matched importStatement for: '" + lastLine + "'");
			// A python package file has two forms:
			// 1. module.py  --> must remove the ".py"
			// 2. module/__init__.py  --> it's enough to use the directory name for loading
			// And can be nested in subdirectories
			
			String g3 = m4.group(3) == null ? "" : m4.group(3), // package name, if any
				   g4 = m4.group(4) == null ? "" : m4.group(4); // class name or function if any. The empty string will list all
			
			if (g3.length() > 0) {
				// With package name: load the module and search its table of members
				try {
					NModuleType mod = Scope.loadPythonModule(g3);
					// Can be null, or can be loaded successfully and yet contain nothing if e.g. its __init__.py is empty
					if (null == mod || mod.getTable().isEmpty()) {
						// Module may be a folder without an __init__.py
						// Pretend the "from" part was fused with the "import" part
						g4 = g3 + "." + g4;
						g3 = "";
					} else {
						final String g3f = g3,
								     g4f = g4;
						return mod.getTable().keySet().stream().filter(s -> s.startsWith(g4f))
								.map(name -> new BasicCompletion(provider, "from " + g3f + " import " + name, null, "Python package"))
								.collect(Collectors.toList());
					}
				} catch (Exception e) {
					System.out.println("Can't find module " + g3);
					System.out.println(e.getMessage());
				}
			}
			
			if (0 == g3.length()) {
				// No separate package name: eg. "import os", or "import os.path" (which is wrong and has to be corrected)
				final int lastDot = g4.lastIndexOf('.');
				final String pkgName = -1 == lastDot ? "" : g4.substring(0, lastDot);
				final String seed = g4.substring(lastDot + 1);
				
				// Search builtin modules, e.g datetime, os, csv, array, ...
				List<Completion> ac = null;
				try {
					final String g4f = g4;
					ac = Scope.indexer.getBindings().keySet().stream()
						.filter(s -> s.startsWith(g4f))
						.map(new Function<String, BasicCompletion>() {
							@Override
							public BasicCompletion apply(final String s) {
								final int lastDot = s.lastIndexOf('.');
								final String imSt = -1 == lastDot ? "import " + s : "from " + s.substring(0, lastDot) + " import " + s.substring(lastDot + 1);
								return new BasicCompletion(provider, imSt, null, "Python module");
							}
						}).collect(Collectors.toList());
				} catch (Exception e) {
					System.out.println("Not a builtin module: '" + g4 + "'");
				}
				
				if (!ac.isEmpty())
					return ac;
				
				// If not, then search for files from sys.path
				return Scope.indexer.getLoadPath().stream()
						.map(s -> new File(s + pkgName.replace(".", "/")))
						.filter(f -> f.exists() && f.isDirectory())
						.map(File::list).flatMap(Stream::of)
						//.peek(s -> System.out.println("all: " + s))
						.filter(name -> name.startsWith(seed) && !name.endsWith(".class") && !name.endsWith("~")) // no temp or compiled files
						//.peek(s -> System.out.println("filtered: " + s))
						.map(name -> new BasicCompletion(provider,
								name.endsWith(".py") ? // module that can be loaded
										(-1 == lastDot ? "" : "from " + pkgName + " ") + "import " + name.substring(0, name.length() - 3)
										: "from " + (-1 == lastDot ? "" : pkgName + ".") + name + " import ",
								null, "Python package"))
						.collect(Collectors.toList());
			}
		}

		final Matcher m1 = nameToken.matcher(lastLine);
		if (m1.find())
			return JythonScriptParser.parseAST(codeWithoutLastLine).getLast().findStartsWith(m1.group(2)).stream()
					.map(s -> new BasicCompletion(provider, (lastLine + s.substring(m1.group(2).length())).substring(crop)))
					.collect(Collectors.toList());
		
		final Matcher m2 = dotNameToken.matcher(lastLine);
		if (m2.find()) {
			final String seed = m2.group(3); // can be empty
			// Expand fields and methods of previous class
			// Assume code is correct up to the dot
			// Python has multiple assignment: find out the class of the last left var
			final String code,
            			 varName;
			final Matcher m3 = assign.matcher(lastLine);
			if (m3.find()) {
				// An assignment, e.g. "ip1, ip2 = imp1.getProcessor(), imp2."
				final String[] assignment = lastLine.split("=");
				final String[] names = assignment[0].split(",");
				varName = names[names.length -1].trim();
				code = codeWithoutLastLine  + lastLine.substring(0, lastLine.length() -1 - seed.length()); // without the ending dot and the seed
			} else {
				// Not an assignment, i.e.  "imp.getImage()." or "imp."
				// Find first non-whitespace char
				int start = 0;
				while (Character.isWhitespace(lastLine.charAt(start++)));
				--start;
				varName = "____GRAB____"; // an injected var to capture the returned class
				code = codeWithoutLastLine + lastLine.substring(0, start) + varName + " = " + lastLine.substring(start, lastLine.length() - 1 - seed.length());
			}
			final DotAutocompletions da = JythonScriptParser.parseAST(code).getLast().find(varName, DotAutocompletions.EMPTY);
			return da.get().stream()
					.filter(s -> s.startsWith(seed))
					.map(s -> new BasicCompletion(provider, lastLine.substring(crop) + s.substring(seed.length()), null, da.getClassname()))
					.collect(Collectors.toList());
		}
		
		return Collections.emptyList();
	}
}
