import java.io.File;

import javax.swing.tree.TreeModel;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.antlr.v4.runtime.tree.gui.TreeViewer;


public class ControladorDDL {
	private ANTLRInputStream input;
	private DDLGrammarLexer lexer;
	private CommonTokenStream tokens;
	private DDLGrammarParser parser;
	private ParseTree tree;
	private ParseTreeWalker walker;
	private TreeViewer vista;
	
	public String compilar(String texto){
		input = new ANTLRInputStream(texto);
		lexer = new DDLGrammarLexer(input);
		tokens = new CommonTokenStream(lexer);
		parser = new DDLGrammarParser(tokens);
		
		ErrorListener e = new ErrorListener();
		
		//Se agrega el listener de Errores que implementa la interfaz ANTLRErrorListener al lexer y al parser.
		lexer.removeErrorListeners();
		lexer.addErrorListener(e);
		parser.removeErrorListeners();
		parser.addErrorListener(e);
		
		
		//Revision Lexica y Sintactica
		parser.statement();
		if(!e.isError()){
			parser.reset();
			//Revision Semantica
			EvalVisitor visitador = new EvalVisitor();
			Tipo t = (Tipo) visitador.visit(parser.statement());
			parser.reset();
			return t.getMensaje();
		}else{
			return e.getMessage();
		}
		
	}

}
