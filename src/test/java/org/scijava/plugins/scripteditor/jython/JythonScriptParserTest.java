/*-
 * #%L
 * Autocompletion for the jython language in the Script Editor
 * %%
 * Copyright (C) 2020 - 2022 SciJava developers.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */
package org.scijava.plugins.scripteditor.jython;

import org.scijava.plugins.scripteditor.jython.JythonScriptParser;

public class JythonScriptParserTest {
	
	static public String testCode = String.join("\n",
			"from os import path",
			"from ij.IJ import getImage", // example of static import of a function (i.e. using the java class like a namespace, and static methods like functions): should not have any expansions
			"from ij import IJ, ImageJ as IJA, VirtualStack, ImagePlus",
			"from ij.process import ByteProcessor",
			"import sys",
			"sys.path.append(\"/path/to/custom/modules/\")",
			"grey8 = IJ.getImage().GRAY8", // static field but should work
			"pixels = IJ.getImage().getProcessor().getPixels()",
			"imp = IJ.getImage()",
			"ip = imp.getProcessor()",
			"width, height = imp.getWidth(), imp.getHeight()",
			"imp2 = imp",
			"name = str(imp)", // test builtin function
			"class Volume(VirtualStack):",
			"  def __init__(self):",
			"    self.msg = 'hi'",
			"  def getProcessor(self, index):",
			"    return ByteProcessor(512, 512)",
			"  def getSize(self):",
			"    return 10",
			"def createImage(w, h):",
			"  imp = ImagePlus('new', ByteProcessor(w, h))",
			"  return imp",
			"def setRoi(an_imp):",
			"  ip = an_imp.getStack().getProcessor(3)", // unknowable: derives from an untyped argument
			"  pixels = ip.");
	
	static final String testCode2 = String.join("\n",
			"class Vol():",
			"  def do1(self):",
			"    self.a = 10",
			"    return self.a",
			"  def do2(self, num):",
			"    self.b = num",
			"    return self.c + self.b",
			""
			);
	
	static final String testCode3 = String.join("\n",
			"a = 10",
			"b = 20.0",
			"if a < b:",
			"  c = 0",
			"else:",
			"  c = 30",
			""
			);
	
	static final String testCode4 = String.join("\n",
			"ls = [1, 2, 3]",
			"def yielfFn(seq):",
			"  for elem in seq:",
			"    yield elem",
			"total = 0",
			"for o in yieldFn(ls):",
			"  total += o",
			"print total",
			""
			);
	
	static final String testCode5 = String.join("\n",
			"from __future__ import with_statement",
			"import sys",
			"lines = []",
			"with open('/tmp/file.txt') as f:",
			"  for line in f:",
			"    try:",
			"      line = line[:-1]",
			"      print line",
			"      lines.append(line)",
			"    except:",
			"      print sys.exc_info()",
			"    finally:",
			"      pass",
			""
			);
	
	static public final void main(String[] args) {
		try {
			final String code = testCode3;
			JythonScriptParser.DEBUG = true;
			final int lastLineBreak = code.lastIndexOf("\n");
			final String codeToParse = -1 == lastLineBreak ? code : code.substring(0, lastLineBreak);
			JythonScriptParser.parseAST(codeToParse).print("");
		} catch (Exception e) {
			e.printStackTrace();
			if (null != e.getCause())
				e.getCause().printStackTrace();
		}
	}
}
