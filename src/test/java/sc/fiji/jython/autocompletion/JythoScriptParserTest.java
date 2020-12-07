package sc.fiji.jython.autocompletion;

import sc.fiji.jython.autocompletion.JythonScriptParser;

public class JythoScriptParserTest {
	
	static public String testCode = String.join("\n",
			"from os import path",
			"from ij.IJ import getImage", // example of static import of a function (i.e. using the java class like a namespace, and static methods like functions): should not have any expansions
			"from ij import IJ, ImageJ as IJA, VirtualStack, ImagePlus",
			"from ij.process import ByteProcessor",
			"grey8 = IJ.getImage().GRAY8", // static field but should work
			"pixels = IJ.getImage().getProcessor().getPixels()",
			"imp = IJ.getImage()",
			"ip = imp.getProcessor()",
			"width, height = imp.getWidth(), imp.getHeight()",
			"imp2 = imp",
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
	
	
	static public final void main(String[] args) {
		try {
			JythonScriptParser.DEBUG = true;
			JythonScriptParser.parseAST(testCode).print("");
		} catch (Exception e) {
			e.printStackTrace();
			if (null != e.getCause())
				e.getCause().printStackTrace();
		}
	}
}
