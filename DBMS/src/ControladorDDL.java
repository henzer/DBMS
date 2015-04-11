import java.io.File;
import java.util.Iterator;

import javax.swing.table.DefaultTableModel;
import javax.swing.tree.TreeModel;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.antlr.v4.runtime.tree.gui.TreeViewer;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;


public class ControladorDDL {
	private ANTLRInputStream input;
	private DDLGrammarLexer lexer;
	private CommonTokenStream tokens;
	private DDLGrammarParser parser;
	private ParseTree tree;
	private ParseTreeWalker walker;
	private TreeViewer vista;
	private boolean data;
	private static DefaultTableModel modelo;
	
	
	public ControladorDDL(){
		data = false;
		modelo = new DefaultTableModel();
	}
	
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
			
			JSONObject resultado = t.getRelacion();
			if(resultado!=null){
				crearModelo(resultado);
				data = true;
			}
			parser.reset();
			return t.getMensaje();
		}
		else{
			return e.getMessage();
		}
		
	}
	
	public void crearModelo(JSONObject resultado){
		JSONArray entries = (JSONArray)resultado.get("entries");
		JSONArray encabezado = (JSONArray)resultado.get("headers");
		
		modelo = new DefaultTableModel();
		int sizeH = encabezado.size();
		for(int i=0; i<sizeH; i++){
			JSONObject actual=(JSONObject)encabezado.get(i);
			modelo.addColumn(actual.get("name")+" - "+actual.get("type"));
			System.out.println(encabezado.get(i));
		}
		System.out.println(entries);
		Iterator<JSONObject> iterador = entries.iterator();
		while(iterador.hasNext()){
			JSONObject tupla = iterador.next();
			Object [] row = new Object[sizeH];
			for(int i=0; i<sizeH; i++){
				JSONObject actual=(JSONObject)encabezado.get(i);
				row[i] = tupla.get(actual.get("name"));
			}
			modelo.addRow(row);
		}
	}

	public boolean isData() {
		return data;
	}

	public void setData(boolean data) {
		this.data = data;
	}

	public static DefaultTableModel getModelo() {
		return modelo;
	}

	public static void setModelo(DefaultTableModel modelo) {
		ControladorDDL.modelo = modelo;
	}
	
	

}
