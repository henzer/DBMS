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
		parser.statement().inspect(parser);
		return "mensaje";
	}

}
