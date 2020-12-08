package sc.fiji.jython.autocompletion;

import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.fife.ui.autocomplete.BasicCompletion;
import org.fife.ui.autocomplete.Completion;
import org.fife.ui.autocomplete.CompletionProvider;
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
						     	 endingCode = Pattern.compile("^([ \\t]*)[^#]*?(.*?)[ \\t]*:[ \\t]*(#.*|)[\\n]*$");
	
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
			String endingLine = codeWithoutLastLine.substring(priorLineBreak);
			final Matcher me = endingCode.matcher(endingLine);
			if (me.find()) {
				codeWithoutLastLine = codeWithoutLastLine.substring(0, priorLineBreak + 1) + me.group(1) + me.group(2) + ": pass";
				JythonScriptParser.print("changed code to: \n" + codeWithoutLastLine + "\n###");
			}
		}
		
		// Two situations to autocomplete:
		// 1) a plain name: delimited with space (or none) to the left, and without parentheses.
		// 2) a method or field: none or some text after a period.

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
