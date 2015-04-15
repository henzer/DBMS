import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Set;
import java.util.Stack;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;

import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;


public class EvalVisitor extends DDLGrammarBaseVisitor<Tipo>{
	private String baseDir="databases/";
	private String databaseFileName="databases.json";
	private TablaTipos tablaTipos;
	private static JSONObject currentDataBase;
	private static String databaseName;
	private String owner="";
	private JSONArray currentConstraints=null;
	private JSONArray currentColumns=null;
	private HashMap<String, JSONObject> memoria;
	private HashMap<String, JSONObject> memoriaRef;
	private JSONObject currentDataFile=null;
	private boolean isCheck=false;
	private SimpleDateFormat formatoFecha;
	private long starttime=0;
	private boolean tableID=false;
	private ArrayList<Criterion> criterios;
	private boolean verbose;
	public static JTextArea consola;
	
	
	
	public EvalVisitor(){
		
		File filep = new File(baseDir+databaseFileName);
		 
		// if file doesnt exists, then create it
		if (!filep.exists()) {
			System.out.println("No Existe");
			JSONObject registro= new JSONObject();
			registro.put("databases", new JSONArray());
			createFile(baseDir+databaseFileName,registro+"");
		}
		tablaTipos = new TablaTipos();
		memoria = new HashMap<String, JSONObject>();
		memoriaRef = new HashMap<String, JSONObject>();
		
		//Se inicializa el comprobador de tipos
		formatoFecha = new SimpleDateFormat("yyyy-MM-dd");
		formatoFecha.setLenient(false);
		
		//Se agregan los tipos de datos primitivos a la tabla de tipos.
		tablaTipos.agregar("INT", 11);
		tablaTipos.agregar("FLOAT", 0);
		tablaTipos.agregar("DATE", 10);
		tablaTipos.agregar("CHAR", 0);
		tablaTipos.agregar("BOOLEAN", 1);
	}

	public boolean isVerbose() {
		return verbose;
		
	}
	public void setVerbose(boolean verbose) {
		this.verbose = verbose;
	}

	@Override public Tipo visitAlterTableRename(@NotNull DDLGrammarParser.AlterTableRenameContext ctx) { 
		//verificar la base de datos actual
		print("Checking that a database was selected");
		if(currentDataBase==null){
			return new Tipo("error","No database Selected");
		}
		currentConstraints=(JSONArray)currentDataBase.get("constraints");
		//verificar la existencia de la tabla
		print("Checking table "+ctx.ID(0).getText()+" existence");
		JSONArray tables=(JSONArray)currentDataBase.get("tables");
		int index=-1;
		for(int i=0;i<tables.size();i++){
			JSONObject current=(JSONObject)tables.get(i);
			if(ctx.ID(0).getText().equals((String)current.get("name"))){
				index=i;
				break;
			}
		}
		if(index==-1){
			return new Tipo("error","Table "+ctx.ID(0).getText()+" does not exist in "+databaseName);
		}
		//verificar la disponibilidad del nombre
		print("Checking table name availability");
		if(!checkTableName(ctx.ID(1).getText(),currentDataBase)){
			return new Tipo("error","Name "+ctx.ID(1).getText()+" is not available");
		}
		print("Changing directories");
		//cambiar las referencias en constraints
		for(int i=0;i<currentConstraints.size();i++){
			JSONObject current=(JSONObject)currentConstraints.get(i);
			if(ctx.ID(0).getText().equals((String)current.get("owner"))){
				current.remove("owner");
				current.put("owner", ctx.ID(1).getText());
			}
			else{
				JSONObject fk=(JSONObject)current.get("foreignKey");
				if(fk!=null){
					if(ctx.ID(0).getText().equals((String)current.get("table"))){
						fk.remove("table");
						fk.put("table", ctx.ID(1).getText());
					}
				}
			}
		}
		//escribir los archivos y realizar los cambios de nombre
		
		JSONObject newTable=new JSONObject();
		newTable.put("name", ctx.ID(1));
		JSONObject tabla=(JSONObject)tables.get(index);
		newTable.put("columns", (JSONArray)tabla.get("columns"));
		tables.remove(index);
		tables.add(newTable);
		createFile(baseDir+databaseName+"/master.json",currentDataBase+"");
		changeDirectoryName(databaseName+"/"+ctx.ID(0).getText()+".json",ctx.ID(1).getText());
		return new Tipo("void","Table name changed succesfully");
	}
	
	@Override public Tipo visitDropDatabase(@NotNull DDLGrammarParser.DropDatabaseContext ctx) {  
		String nombre = ctx.ID().getText();
		print("Awaiting confirmation");
		int opc = JOptionPane.showConfirmDialog(null, "¿Esta seguro que desea eliminar: " + nombre + "?", "Eliminar", JOptionPane.YES_NO_OPTION);
		if (opc==0){
			try {
				dropDatabase(nombre);
				return new Tipo("void", "Ha sido eliminada correctamente.");
			} catch (Exception e) {
				e.printStackTrace();
				return new Tipo("error", e.getMessage());
			}
		}else{
			return new Tipo("void", "Operacion cancelada.");
		}
	}
	
	@Override public Tipo visitConstraintDecl3(@NotNull DDLGrammarParser.ConstraintDecl3Context ctx) { 
		isCheck=true;
		Tipo res=  visit(ctx.expression());
		if(res.getTipo().equals("error")){
			return res;
		}
		try {
			JSONArray entries=(JSONArray)currentDataFile.get("entries");
			for(int i=0;i<entries.size();i++){
				if(!validar(res.getResultado(), (JSONObject) entries.get(i), true)){
					return new Tipo("error","Tuple "+entries.get(i)+" does not pass constraint");
				}
			}
			addCheck(currentConstraints, owner, ctx.ID().getText(), res.getResultado());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			return new Tipo("error",e.getMessage());
		}
		isCheck=false;
		return new Tipo("void"); 
	}
	
	@Override public Tipo visitTipoFloat(@NotNull DDLGrammarParser.TipoFloatContext ctx) { 
		return new Tipo("FLOAT"); 
	}
	
	@Override public Tipo visitLiteralFloat(@NotNull DDLGrammarParser.LiteralFloatContext ctx) { 
		ArrayList<String>currentExpression=new ArrayList<String>();
		currentExpression.add(ctx.getText());
		return new Tipo("FLOAT",currentExpression); 
	}
	
	@Override public Tipo visitShowTables(@NotNull DDLGrammarParser.ShowTablesContext ctx) { 
		//verificar la base de datos actual
		print("Checking that a database was selected");
		if(currentDataBase==null){
			return new Tipo("error","No database Selected");
		}
		String res="";
		JSONArray tables=(JSONArray)currentDataBase.get("tables");
		for(int i=0;i<tables.size();i++){
			JSONObject currentT=(JSONObject)tables.get(i);
			res+=(String)currentT.get("name")+"\n";
		}
		return new Tipo("void","Tables:\n"+res);
	}
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation returns the result of calling
	 * {@link #visitChildren} on {@code ctx}.</p>
	 */
	@Override public Tipo visitInt_literal(@NotNull DDLGrammarParser.Int_literalContext ctx) { return visitChildren(ctx); }
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation returns the result of calling
	 * {@link #visitChildren} on {@code ctx}.</p>
	 */
	@Override public Tipo visitEqE(@NotNull DDLGrammarParser.EqEContext ctx) { return visitChildren(ctx); }
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation returns the result of calling
	 * {@link #visitChildren} on {@code ctx}.</p>
	 */
	@Override public Tipo visitCond_op1(@NotNull DDLGrammarParser.Cond_op1Context ctx) { return visitChildren(ctx); }
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation returns the result of calling
	 * {@link #visitChildren} on {@code ctx}.</p>
	 */
	@Override public Tipo visitUniFactorFactor(@NotNull DDLGrammarParser.UniFactorFactorContext ctx) { 
		return visit(ctx.factor()); 
		}
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation returns the result of calling
	 * {@link #visitChildren} on {@code ctx}.</p>
	 */
	@Override public Tipo visitUniFactorNot(@NotNull DDLGrammarParser.UniFactorNotContext ctx) { 
		Tipo res =   visit(ctx.factor());
		if(res==null){
			ArrayList<String> nueva=new ArrayList<String>();
			nueva.add(isCheck+"");
			return new Tipo("BOOL",nueva);
		}
		if(!res.getTipo().equals("BOOL")){
			return new Tipo("error","Operator NOT requires BOOL expressions");
		}
		res.getResultado().add("NOT");
		return res; 
	}
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation returns the result of calling
	 * {@link #visitChildren} on {@code ctx}.</p>
	 */
	@Override public Tipo visitChar_literal(@NotNull DDLGrammarParser.Char_literalContext ctx) { return visitChildren(ctx); }
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation returns the result of calling
	 * {@link #visitChildren} on {@code ctx}.</p>
	 */
	@Override public Tipo visitAlterDatabase(@NotNull DDLGrammarParser.AlterDatabaseContext ctx) {
		try {
			alterDatabase(ctx.ID(0).getText(), ctx.ID(1).getText());
			if(databaseName.equals(ctx.ID(0))){
				databaseName=ctx.ID(1).getText();
			}
			return new Tipo("void", "Database name changed succesfully.");
		} catch (Exception e) {
			e.printStackTrace();
			return new Tipo("error", e.getMessage());
		}

	}
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation returns the result of calling
	 * {@link #visitChildren} on {@code ctx}.</p>
	 */
	@Override public Tipo visitRekB(@NotNull DDLGrammarParser.RekBContext ctx) { return visitChildren(ctx); }
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation returns the result of calling
	 * {@link #visitChildren} on {@code ctx}.</p>
	 */
	@Override public Tipo visitCond_op2(@NotNull DDLGrammarParser.Cond_op2Context ctx) { return visitChildren(ctx); }
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation returns the result of calling
	 * {@link #visitChildren} on {@code ctx}.</p>
	 */
	@Override public Tipo visitUseDatabase(@NotNull DDLGrammarParser.UseDatabaseContext ctx) { 
		try {
			useDatabase(ctx.ID().getText());
			//Se carga la metadata de la base de datos que se está utilizando.
			currentDataBase = readJSON(baseDir+databaseName+"/master.json");
			
			return new Tipo("void", "Using database "+ctx.ID().getText());
		} catch (Exception e) {
			e.printStackTrace();
			
			return new Tipo("error", e.getMessage()); 
		} 
	}
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation returns the result of calling
	 * {@link #visitChildren} on {@code ctx}.</p>
	 */
	@Override public Tipo visitRelLE(@NotNull DDLGrammarParser.RelLEContext ctx) { return visitChildren(ctx); }
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation returns the result of calling
	 * {@link #visitChildren} on {@code ctx}.</p>
	 */
	@Override public Tipo visitFactorLiteral(@NotNull DDLGrammarParser.FactorLiteralContext ctx) { 
		return visit(ctx.literal());
	}
	
	@Override public Tipo visitFactorNull(@NotNull DDLGrammarParser.FactorNullContext ctx) { 
		//debe retornar un tipo null
		return null;
	}
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation returns the result of calling
	 * {@link #visitChildren} on {@code ctx}.</p>
	 */
	@Override public Tipo visitCreateDatabase(@NotNull DDLGrammarParser.CreateDatabaseContext ctx) { 
		try {
			createDatabase(ctx.ID().getText());
			return new Tipo("void", "Base de datos creada exitosamente.");
		} catch (Exception e) {
			e.printStackTrace();
			return new Tipo("error", e.getMessage()); 
		}
	}

	@Override public Tipo visitLiteralChar(@NotNull DDLGrammarParser.LiteralCharContext ctx) { 
		ArrayList<String> currentExpression=new ArrayList<String>();
		currentExpression.add(ctx.getText());
		Tipo res= new Tipo("CHAR");
		res.setResultado(currentExpression);
		res.setLength(ctx.getText().length());
		return res;
	}
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation returns the result of calling
	 * {@link #visitChildren} on {@code ctx}.</p>
	 */
	@Override public Tipo visitExpr31(@NotNull DDLGrammarParser.Expr31Context ctx) { 
		Tipo res1=  visit(ctx.expr3());
		Tipo res2=  visit(ctx.unifactor());
		
		//checkeo de null
		if(res1==null&&res2==null){
			ArrayList<String> nuevo =new ArrayList<String>();
			nuevo.add(isCheck+"");
			return new Tipo("BOOL",nuevo);
		}
		else if(res1==null){
			if(!res2.getTipo().equals("error")){
				ArrayList<String> nuevo =new ArrayList<String>();
				nuevo.add(isCheck+"");
				return new Tipo("BOOL",nuevo);
			}
		}
		else if(res2==null){
			if(!res1.getTipo().equals("error")){
				ArrayList<String> nuevo =new ArrayList<String>();
				nuevo.add(isCheck+"");
				return new Tipo("BOOL",nuevo);
			}
		}
		//flujo normal
		if(res1.getTipo().equals("error")){
			return res1;
		}
		ArrayList<String> newExpr=new ArrayList<String>();
		newExpr.addAll(res1.getResultado());
		
		if(res2.getTipo().equals("error")){
			return res2;
		}
		newExpr.addAll(res2.getResultado());
		newExpr.add(ctx.rel_op().getText());
		if(!res1.getTipo().equals(res2.getTipo())){
			if (res1.getTipo().equals("INT")&&res2.getTipo().equals("FLOAT")){
				return new Tipo("BOOL",newExpr); 
			}
			else if(res1.getTipo().equals("FLOAT")&&res2.getTipo().equals("INT")){
				return new Tipo("BOOL",newExpr); 
			}
			return new Tipo("error","Incompatible types around "+ctx.rel_op().getText());
		}
		return new Tipo("BOOL",newExpr); 
	}
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation returns the result of calling
	 * {@link #visitChildren} on {@code ctx}.</p>
	 */
	@Override public Tipo visitLiteralInt(@NotNull DDLGrammarParser.LiteralIntContext ctx) {
		ArrayList<String> currentExpression=new ArrayList<String>();
		currentExpression.add(ctx.getText());
		return new Tipo("INT",currentExpression); 
	}
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation returns the result of calling
	 * {@link #visitChildren} on {@code ctx}.</p>
	 */
	@Override public Tipo visitExpr32(@NotNull DDLGrammarParser.Expr32Context ctx) { 
		return visit(ctx.unifactor()); 
	}
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation returns the result of calling
	 * {@link #visitChildren} on {@code ctx}.</p>
	 */
	@Override public Tipo visitCreateTable(@NotNull DDLGrammarParser.CreateTableContext ctx) { 
		tableID=false;
		print("Checking that a database was selected");
		if(currentDataBase==null){
			return new Tipo("error","No database selected");
		}
		currentDataBase = readJSON(baseDir+databaseName+"/master.json");
		owner=ctx.ID(0).getText();
		currentConstraints=(JSONArray)currentDataBase.get("constraints");
		print("Checking table name "+ctx.ID(0).getText()+" availability");
		if(!checkTableName(owner,currentDataBase)){
			return new Tipo("error","Table name "+owner+" not available in "+databaseName);
		}
		JSONObject newTable =new JSONObject();
		newTable.put("name", owner);
		JSONArray columns =new JSONArray();
		print("Checking that column names are unique");
		for(int i=0;i<ctx.tipo().size();i++){
			//revision de columna repetida
			for(int j=1;j<ctx.ID().size();j++){
				if(j!=i+1){
					if(ctx.ID(i+1).getText().equals(ctx.ID(j).getText())){
						return new Tipo("error","Duplicate column "+ctx.ID(i+1));
					}
				}
			}
			Tipo current=  visit(ctx.tipo(i));
			JSONObject newColumn = new JSONObject();
			newColumn.put("name", ctx.ID(i+1).getText());
			newColumn.put("type",current.getTipo());
			if(current.getTipo().equals("CHAR")){
				newColumn.put("length", current.getLength());
			}
			columns.add(newColumn);
		}
		newTable.put("columns", columns);
		currentColumns=columns;
		currentDataFile=new JSONObject();
		currentDataFile.put("entries", new JSONArray());
		for(int i=0;i<ctx.constraintDecl().size();i++){
			Tipo current=  visit(ctx.constraintDecl(i));
			if(current.getTipo().equals("error")){
				return current;
			}
		}
		//agregar uno al contador de tablas en el archivo databases
		JSONObject content = readJSON(baseDir+databaseFileName);
		JSONArray databases= (JSONArray)content.get("databases");
		for(int i=0;i<databases.size();i++){
			JSONObject current = (JSONObject)databases.get(i);
			if(databaseName.equals((String)current.get("name"))){
				int conttables=(int)(long)(current.get("length"))+1;
				databases.remove(i);
				JSONObject newdata=new JSONObject();
				newdata.put("name", databaseName);
				newdata.put("length", conttables);
				databases.add(newdata);
				createFile(baseDir+databaseFileName,content+"");
				break;
			}
		}
		print("Creating directories");
		//creacion y alteracion de archivos
		JSONArray tables = (JSONArray) currentDataBase.get("tables");
		tables.add(newTable);
		createFile(baseDir+databaseName+"/master.json",currentDataBase+"");
		createFile(baseDir+databaseName+"/"+owner+".json",currentDataFile+"");
		return new Tipo("void","Table "+owner+" created succesfully in "+databaseName);
	}
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation returns the result of calling
	 * {@link #visitChildren} on {@code ctx}.</p>
	 */
	@Override public Tipo visitTipoInt(@NotNull DDLGrammarParser.TipoIntContext ctx) { 
		return new Tipo("INT"); 
	}
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation returns the result of calling
	 * {@link #visitChildren} on {@code ctx}.</p>
	 */
	@Override public Tipo visitAlterTableAccion(@NotNull DDLGrammarParser.AlterTableAccionContext ctx) { 
		tableID=false;
		//revisar que se haya seleccionado una base de datos
		print("Checking that a database was selected");
		if(currentDataBase==null){
			return new Tipo("error","No database selected");
		}
		currentDataBase = readJSON(baseDir+databaseName+"/master.json");
		//inicializacion
		currentConstraints=(JSONArray)currentDataBase.get("constraints");
		//obtener columnas y verificacion de la existencia de la tabla
		JSONArray tables = (JSONArray)currentDataBase.get("tables");
		JSONArray columns=null;
		for(int i=0;i<tables.size();i++){
			JSONObject currentC= (JSONObject)tables.get(i);
			if(ctx.ID().getText().equals((String)currentC.get("name"))){
				columns=(JSONArray)currentC.get("columns");
			}
		}
		if(columns==null){
			return new Tipo("error","Table "+ctx.ID().getText()+" does not exist in "+databaseName);
		}
		currentColumns=columns;
		owner=ctx.ID().getText();
		currentDataFile=readJSON(baseDir+databaseName+"/"+owner+".json");
		for(int i=0;i<ctx.accion().size();i++){
			Tipo res=  visit(ctx.accion(i));
			if(res.getTipo().equals("error")){
				return res;
			}
		}
		//escribir master.json
		createFile(baseDir+databaseName+"/"+owner+".json",currentDataFile+"");
		createFile(baseDir+databaseName+"/master.json",currentDataBase+"");
		return new Tipo("void","Changes to "+owner+" in "+databaseName+" done succesfully");
	}
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation returns the result of calling
	 * {@link #visitChildren} on {@code ctx}.</p>
	 */
	@Override public Tipo visitEqNE(@NotNull DDLGrammarParser.EqNEContext ctx) { return visitChildren(ctx); }
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation returns the result of calling
	 * {@link #visitChildren} on {@code ctx}.</p>
	 */
	@Override public Tipo visitShowColumnsFrom(@NotNull DDLGrammarParser.ShowColumnsFromContext ctx) { 
		//verificar la base de datos actual
		print("Checking that a database was selected");
		if(currentDataBase==null){
			return new Tipo("error","No database Selected");
		}
		//verificar la existencia de la tabla
		JSONArray tables=(JSONArray)currentDataBase.get("tables");
		JSONArray constraints=(JSONArray)currentDataBase.get("constraints");
		JSONArray columns=null;
		String columnas="";
		String constr="";
		for(int i=0;i<tables.size();i++){
			JSONObject currentT=(JSONObject)tables.get(i);
			if(ctx.ID().getText().equals((String)currentT.get("name"))){
				columns=(JSONArray)currentT.get("columns");
			}
		}
		if(columns==null){
			return new Tipo("error","Table "+ctx.ID().getText()+" does not exist in "+databaseName);
		}
		//escribir columnas
		for(int i=0;i<columns.size();i++){
			JSONObject currentC=(JSONObject)columns.get(i);
			columnas+="name: "+(String)currentC.get("name")+" type: "+(String)currentC.get("type")+"\n";
		}
		//escribir constraints
		for(int i=0;i<constraints.size();i++){
			JSONObject currentC=(JSONObject)constraints.get(i);
			//revisando si es primary key
			if(currentC.containsKey("primaryKey")){
				JSONArray elements=(JSONArray)currentC.get("primaryKey");
				constr+="Primary Key: "+currentC.get("name")+" columns:"+elements+"\n";
			}
			else if(currentC.containsKey("foreignKey")){
				JSONObject fk=(JSONObject)currentC.get("foreignKey");
				constr="Foreign Key: "+currentC.get("name")+" local columns:"+(JSONArray)currentC.get("columns")+" references: "+currentC.get("table")+" referenced columns:"+(JSONArray)currentC.get("references");				
			}
			else if(currentC.containsKey("check")){
				constr="Check: "+currentC.get("name")+" Postfix expression: "+(JSONArray)currentC.get("check");
			}
		}
		return new Tipo("void","Columns\n"+columnas+"Constraints\n"+constr);
	}
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation returns the result of calling
	 * {@link #visitChildren} on {@code ctx}.</p>
	 */
	@Override public Tipo visitTipoDate(@NotNull DDLGrammarParser.TipoDateContext ctx) { 
		return new Tipo("DATE"); 
	}
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation returns the result of calling
	 * {@link #visitChildren} on {@code ctx}.</p>
	 */
	@Override public Tipo visitFactorExpression(@NotNull DDLGrammarParser.FactorExpressionContext ctx) { 
		return visit(ctx.expression()); 
	}
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation returns the result of calling
	 * {@link #visitChildren} on {@code ctx}.</p>
	 */
	@Override public Tipo visitRoot(@NotNull DDLGrammarParser.RootContext ctx) { 
		String mensaje = "";
		Tipo t=new Tipo("void");
		for(DDLGrammarParser.StatementContext statement: ctx.statement()){
			t = visit(statement);
			if(t.isError())return t;
			mensaje+= t.getMensaje() + "\n";
		}
		t.setMensaje(mensaje);
		return t;
	}
	
	@Override public Tipo visitStatement(@NotNull DDLGrammarParser.StatementContext ctx) { 
		Tipo res=visitChildren(ctx);
		return res; 
	}
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation returns the result of calling
	 * {@link #visitChildren} on {@code ctx}.</p>
	 */
	@Override public Tipo visitFloat_literal(@NotNull DDLGrammarParser.Float_literalContext ctx) { return visitChildren(ctx); }
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation returns the result of calling
	 * {@link #visitChildren} on {@code ctx}.</p>
	 */
	@Override public Tipo visitExpr22(@NotNull DDLGrammarParser.Expr22Context ctx) { 
		return visit(ctx.expr3()); 
	}
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation returns the result of calling
	 * {@link #visitChildren} on {@code ctx}.</p>
	 */
	@Override public Tipo visitDate_literal(@NotNull DDLGrammarParser.Date_literalContext ctx) { return visitChildren(ctx); }
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation returns the result of calling
	 * {@link #visitChildren} on {@code ctx}.</p>
	 */
	@Override public Tipo visitExpr21(@NotNull DDLGrammarParser.Expr21Context ctx) { 
		Tipo res1=  visit(ctx.expr2());
		Tipo res2=  visit(ctx.expr3());
		//checkeo de null
		if(res1==null&&res2==null){
			ArrayList<String> nuevo =new ArrayList<String>();
			nuevo.add(isCheck+"");
			return new Tipo("BOOL",nuevo);
		}
		else if(res1==null){
			if(!res2.getTipo().equals("error")){
				ArrayList<String> nuevo =new ArrayList<String>();
				nuevo.add(isCheck+"");
				return new Tipo("BOOL",nuevo);
			}
		}
		else if(res2==null){
			if(!res1.getTipo().equals("error")){
				ArrayList<String> nuevo =new ArrayList<String>();
				nuevo.add(isCheck+"");
				return new Tipo("BOOL",nuevo);
			}
		}
		//flujo normal
		if(res1.getTipo().equals("error")){
			return res1;
		}
		ArrayList<String> newExpr=new ArrayList<String>();
		newExpr.addAll(res1.getResultado());
		if(res2.getTipo().equals("error")){
			return res2;
		}
		newExpr.addAll(res2.getResultado());
		newExpr.add(ctx.eq_op().getText());
		if(!res1.getTipo().equals(res2.getTipo())){
			if (res1.getTipo().equals("INT")&&res2.getTipo().equals("FLOAT")){
				return new Tipo("BOOL",newExpr); 
			}
			else if(res1.getTipo().equals("FLOAT")&&res2.getTipo().equals("INT")){
				return new Tipo("BOOL",newExpr); 
			}
			return new Tipo("error","Incompatible types around "+ctx.eq_op().getText());
		}
		return new Tipo("BOOL",newExpr); 
	}
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation returns the result of calling
	 * {@link #visitChildren} on {@code ctx}.</p>
	 */
	@Override public Tipo visitDropTable(@NotNull DDLGrammarParser.DropTableContext ctx) { 
		//revisar si se ha seleccionado una base de datos
		print("Checking that a database was selected");
		if(currentDataBase==null){
			return new Tipo("error","No database selected");
		}
		currentDataBase = readJSON(baseDir+databaseName+"/master.json");
		print("Checking existence of table "+ctx.ID().getText());
		//revisar si existe la tabla
		JSONArray tables = (JSONArray)currentDataBase.get("tables");
		JSONArray constraints=(JSONArray)currentDataBase.get("constraints");
		int index=-1;
		for(int i=0;i<tables.size();i++){
			JSONObject currentC=(JSONObject)tables.get(i);
			if(ctx.ID().getText().equals((String)currentC.get("name"))){
				index=i;
			}
		}
		if(index==-1){
			return new Tipo("error","Table "+ctx.ID().getText()+" does not exist in "+databaseName);
		}
		//revisar que la tabla no sea referenciada
		print("Checking table references");
		for(int i=0;i<constraints.size();i++){
			JSONObject currentC=(JSONObject)constraints.get(i);
			if(currentC.get("foreignKey")!=null){
				JSONObject foreignKey=(JSONObject)currentC.get("foreignKey");
				if(ctx.ID().getText().equals((String)foreignKey.get("table"))){
					return new Tipo("error","Table "+ctx.ID().getText()+" cannot be deleted... is referenced in "+currentC.get("name"));
				}
			}
		}
		//borrar la tabla y sus constraints
		tables.remove(index);
		for(int i=constraints.size()-1;i>=0;i--){
			JSONObject currentC=(JSONObject)constraints.get(i);
			if(ctx.ID().getText().equals((String)currentC.get("owner"))){
				constraints.remove(i);
			}
		}
		JSONObject content = readJSON(baseDir+databaseFileName);
		JSONArray databases= (JSONArray)content.get("databases");
		for(int i=0;i<databases.size();i++){
			JSONObject current = (JSONObject)databases.get(i);
			if(databaseName.equals((String)current.get("name"))){
				int conttables=(int)(long)(current.get("length"))-1;
				databases.remove(i);
				JSONObject newdata=new JSONObject();
				newdata.put("name", databaseName);
				newdata.put("length", conttables);
				databases.add(newdata);
				createFile(baseDir+databaseFileName,content+"");
				break;
			}
		}
		print("Deleting directories");
		//escribir el archivo master.json y borrar el archivo de la tabla
		createFile(baseDir+databaseName+"/master.json",currentDataBase+"");
		eraseDirectory(baseDir+databaseName+"/"+ctx.ID().getText()+".json");
		return new Tipo("void","Table "+ctx.ID().getText()+" dropped succesfully");
	}
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation returns the result of calling
	 * {@link #visitChildren} on {@code ctx}.</p>
	 */
	@Override public Tipo visitConstraintDecl1(@NotNull DDLGrammarParser.ConstraintDecl1Context ctx) { 
		ArrayList<String> ids=new ArrayList<String>();
		JSONArray columnsToCheck=new JSONArray();
		for(int i=1;i<ctx.ID().size();i++){
			ids.add(ctx.ID(i).getText());
			columnsToCheck.add(ctx.ID(i).getText());
		}
		try {
			//checkear la unicidad de los valores
			JSONArray entries=(JSONArray)currentDataFile.get("entries");
			print("Checking if current data in table passes Constraint");
			for(int i=0;i<entries.size();i++){
				JSONObject tempTable = new JSONObject();
				JSONArray tempEntries =new JSONArray();
				tempEntries.addAll(entries.subList(0, i));
				tempEntries.addAll(entries.subList(i+1, entries.size()));
				//checkear la unicidad de la tabla sin el valor i
				tempTable.put("entries", tempEntries);
				if(!checkPrimaryKey(columnsToCheck, (JSONObject)entries.get(i), tempTable)){
					return new Tipo("error","Tuple "+(JSONObject)entries.get(i)+" is not unique");
				}
			}
			//agregar primary key
			addPrimaryKey(currentConstraints, currentColumns, owner, ctx.ID(0).getText(), ids);
			return new Tipo("void");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return new Tipo("error",e.getMessage());
		}
	}
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation returns the result of calling
	 * {@link #visitChildren} on {@code ctx}.</p>
	 */
	@Override public Tipo visitConstraintDecl2(@NotNull DDLGrammarParser.ConstraintDecl2Context ctx) {
		ArrayList<String> to = new ArrayList<String>();
		ArrayList<String> from=new ArrayList<String>();
		boolean references=false;
		String referenced="";
		for(int i=5;i<ctx.children.size();i++){
			String currentChild=ctx.children.get(i).getText();
			if(!(currentChild.equals("(")||currentChild.equals(")")||currentChild.equals(",")||currentChild.equals("REFERENCES"))){
				if(references){
					to.add(currentChild);
				}
				else{
					from.add(currentChild);
				}
			}
			else if(currentChild.equals("REFERENCES")){
				references=true;
				i++;
				referenced=ctx.children.get(i).getText();
			}
		}
		try {
			JSONArray fkLocal=new JSONArray();
			fkLocal.addAll(from);
			JSONArray fkRef=new JSONArray();
			fkRef.addAll(to);
			JSONObject relacionRef=readJSON(baseDir+databaseName+"/"+referenced+".json");
			JSONArray entries=(JSONArray)currentDataFile.get("entries");
			addForeignKey(currentConstraints, currentColumns, owner, ctx.ID(0).getText(), from, referenced, to);
			print("Checking if current data in table passes constraint");
			for(int i=0;i<entries.size();i++){
				if(!this.checkForeignKey(fkLocal, fkRef, (JSONObject) entries.get(i), relacionRef)){
					currentConstraints.remove(currentConstraints.size()-1);
					return new Tipo("error","Tuple "+entries.get(i)+" does not pass constraint");
				}
				
			}
			return new Tipo("void");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return new Tipo("error",e.getMessage());
		}
	}
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation returns the result of calling
	 * {@link #visitChildren} on {@code ctx}.</p>
	 */
	@Override public Tipo visitShowDatabases(@NotNull DDLGrammarParser.ShowDatabasesContext ctx) { 
		return new Tipo ("void",showDatabases());
	}
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation returns the result of calling
	 * {@link #visitChildren} on {@code ctx}.</p>
	 */
	@Override public Tipo visitRelL(@NotNull DDLGrammarParser.RelLContext ctx) { return visitChildren(ctx); }
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation returns the result of calling
	 * {@link #visitChildren} on {@code ctx}.</p>
	 */
	@Override public Tipo visitRelBE(@NotNull DDLGrammarParser.RelBEContext ctx) { return visitChildren(ctx); }
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation returns the result of calling
	 * {@link #visitChildren} on {@code ctx}.</p>
	 */
	@Override public Tipo visitExpression2(@NotNull DDLGrammarParser.Expression2Context ctx) { 
		Tipo res=visit(ctx.expr1());
		ArrayList<String> newExpr=res.getResultado();
		if(!res.getTipo().equals("error")){
			for(int i=0;i<newExpr.size();i++){
				System.out.println(newExpr.get(i));
			}
		}
		return res;
	}
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation returns the result of calling
	 * {@link #visitChildren} on {@code ctx}.</p>
	 */
	@Override public Tipo visitLiteralDate(@NotNull DDLGrammarParser.LiteralDateContext ctx) { 
		ArrayList<String> currentExpression=new ArrayList<String>();
		currentExpression.add(ctx.getText());
		return new Tipo("DATE",currentExpression); 
	}
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation returns the result of calling
	 * {@link #visitChildren} on {@code ctx}.</p>
	 */
	@Override public Tipo visitExpression1(@NotNull DDLGrammarParser.Expression1Context ctx) { 
		Tipo res1=  visit(ctx.expression());
		if(res1.isError())return res1;
		
		ArrayList<String> newExpr=new ArrayList<String>();
		newExpr.addAll(res1.getResultado());
		Tipo res2=  visit(ctx.expr1());
		
		if(res2.isError())return res2;
		
		newExpr.addAll(res2.getResultado());
		newExpr.add(ctx.cond_op2().getText());
		String resultado="";
		for(int i=0;i<newExpr.size();i++){
			System.out.println(newExpr.get(i));
		}
		if((!res1.getTipo().equals("BOOL"))||(!res2.getTipo().equals("BOOL"))){
			return new Tipo("error","Incompatible types around "+ctx.cond_op2().getText());
		}
		return new Tipo("BOOL",newExpr); 
	}
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation returns the result of calling
	 * {@link #visitChildren} on {@code ctx}.</p>
	 */
	@Override public Tipo visitAccion1(@NotNull DDLGrammarParser.Accion1Context ctx) { 
		//revisar que la columna a ingresar no tenga nombre repetido
		print("Checking availability of column names");
		for(int i=0;i<currentColumns.size();i++){
			JSONObject currentC=(JSONObject)currentColumns.get(i);
			String name=(String)currentC.get("name");
			if(ctx.ID().getText().equals(name)){
				return new Tipo("error","Column "+ctx.ID().getText()+" already exists");
			}
		}
		//traer el tipo
		Tipo actual=  visit(ctx.tipo());
		JSONObject newColumn=new JSONObject();
		newColumn.put("name", ctx.ID().getText());
		newColumn.put("type", actual.getTipo());
		if(actual.getTipo().equals("CHAR")){
			newColumn.put("length", actual.getLength());
		}
		//revisar que las constraints no tengan error
		currentColumns.add(newColumn);
		print("Checking constraints");
		for(int i=0;i<ctx.constraintDecl().size();i++){
			Tipo currentT=  visit(ctx.constraintDecl(i));
			if(currentT.getTipo().equals("error")){
				return currentT;
			}
		}
		JSONArray entries=(JSONArray)currentDataFile.get("entries");
		for(int i=0;i<entries.size();i++){
			JSONObject current=(JSONObject)entries.get(i);
			current.put(ctx.ID().getText(), null);
		}
		currentColumns.add(newColumn);

		return new Tipo("void");
	}
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation returns the result of calling
	 * {@link #visitChildren} on {@code ctx}.</p>
	 */
	@Override public Tipo visitAccion2(@NotNull DDLGrammarParser.Accion2Context ctx) { 
		return visit(ctx.constraintDecl());
	}
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation returns the result of calling
	 * {@link #visitChildren} on {@code ctx}.</p>
	 */
	@Override public Tipo visitTipoChar(@NotNull DDLGrammarParser.TipoCharContext ctx) { 
		Tipo res = new Tipo("CHAR");
		res.setLength(Integer.parseInt(ctx.NUM().getText()));
		return res;
	}
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation returns the result of calling
	 * {@link #visitChildren} on {@code ctx}.</p>
	 */
	@Override public Tipo visitAccion3(@NotNull DDLGrammarParser.Accion3Context ctx) { 
		//revisar que exista la columna
		int index=-1;
		print("Checking column existence");
		for(int i=0;i<currentColumns.size();i++){
			JSONObject current = (JSONObject)currentColumns.get(i);
			if(ctx.ID().getText().equals((String)current.get("name"))){
				index=i;
				break;
			}
		}
		if(index==-1){
			return new Tipo("error","Column "+ctx.ID().getText()+" does not exist in "+databaseName);
		}
		//revisar que no sea mencionado en constraints
		print("Checking references in Constraints");
		for(int i=0;i<currentConstraints.size();i++){
			 JSONObject currentC=(JSONObject)currentConstraints.get(i);
			 //revisar primary key
			 if(currentC.get("primaryKey")!=null){
				 JSONArray primaryKey = (JSONArray)currentC.get("primaryKey");
				 for(int j=0;j<primaryKey.size();j++){
					 if(ctx.ID().getText().equals((String)primaryKey.get(j))&&owner.equals((String)currentC.get("owner"))){
						 return new Tipo("error","Column "+ctx.ID().getText()+" used in constraint "+currentC.get("name"));
					 }
				 }
			 }
			 //revisar foreign key
			 else if(currentC.get("foreignKey")!=null){
				 JSONObject foreignKey=(JSONObject)currentC.get("foreignKey");
				 //revisar en columnas locales
				 if(owner.equals((String)currentC.get("owner"))){
					 JSONArray columnsL=(JSONArray)foreignKey.get("columns");
					 for(int j=0;j<columnsL.size();j++){
						 if(ctx.ID().getText().equals((String)columnsL.get(j))){
							 return new Tipo("error","Column "+ctx.ID().getText()+" used in constraint "+currentC.get("name"));
						 }
					 }
				 }
				 //revisar en referencias
				 else if(owner.equals((String)foreignKey.get("table"))){
					 JSONArray columnsR=(JSONArray)foreignKey.get("references");
					 for(int j=0;j<columnsR.size();j++){
						 if(ctx.ID().getText().equals((String)columnsR.get(j))){
							 return new Tipo("error","Column "+ctx.ID().getText()+" used in constraint "+currentC.get("name"));
						 }
					 }
				 }
			 }
			 //revisar check
			 else if(currentC.get("check")!=null){
				 JSONArray check=(JSONArray)currentC.get("check");
				 for(int j=0;j<check.size();j++){
					 if(ctx.ID().getText().equals((String)check.get(j))&&owner.equals((String)currentC.get("owner"))){
						 return new Tipo("error","Column "+ctx.ID().getText()+" used in constraint "+currentC.get("name"));
					 }
				 }
			 }
		}
		//eliminacion de campos de los registros de la tabla correspondiente
		print("Eliminating column data");
		currentColumns.remove(index);
		JSONArray entries=(JSONArray)currentDataFile.get("entries");
		for(int i=0;i<entries.size();i++){
			JSONObject current=(JSONObject)entries.get(i);
			current.remove(ctx.ID().getText());
		}
		return new Tipo("void","Column "+ctx.ID().getText()+" dropped succesfully");		
	}
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation returns the result of calling
	 * {@link #visitChildren} on {@code ctx}.</p>
	 */
	@Override public Tipo visitExpr11(@NotNull DDLGrammarParser.Expr11Context ctx) { 
		Tipo res1=  visit(ctx.expr1());
		Tipo res2=  visit(ctx.expr2());
		//checkeo de null
		if(res1==null&&res2==null){
			ArrayList<String> nuevo =new ArrayList<String>();
			nuevo.add(isCheck+"");
			return new Tipo("BOOL",nuevo);
		}
		else if(res1==null){
			if(!res2.getTipo().equals("error")){
				ArrayList<String> nuevo =new ArrayList<String>();
				nuevo.add(isCheck+"");
				return new Tipo("BOOL",nuevo);
			}
		}
		else if(res2==null){
			if(!res1.getTipo().equals("error")){
				ArrayList<String> nuevo =new ArrayList<String>();
				nuevo.add(isCheck+"");
				return new Tipo("BOOL",nuevo);
			}
		}
		//flujo normal
		if(res1.getTipo().equals("error")){
			return res1;
		}
		ArrayList<String> newExpr=new ArrayList<String>();
		newExpr.addAll(res1.getResultado());
		if(res2.getTipo().equals("error")){
			return res2;
		}
		newExpr.addAll(res2.getResultado());
		newExpr.add(ctx.cond_op1().getText());
		if((!res1.getTipo().equals("BOOL"))||(!res2.getTipo().equals("BOOL"))){
			return new Tipo("error","Incompatible types around "+ctx.cond_op1().getText());
		}
		return new Tipo("BOOL",newExpr);  
	}
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation returns the result of calling
	 * {@link #visitChildren} on {@code ctx}.</p>
	 */
	@Override public Tipo visitAccion4(@NotNull DDLGrammarParser.Accion4Context ctx) { 
		//buscar la constraint
		print("Checking Constraint name existence");
		for(int i=0;i<currentConstraints.size();i++){
			JSONObject currentC=(JSONObject)currentConstraints.get(i);
			if(ctx.ID().getText().equals((String)currentC.get("name"))){
				currentConstraints.remove(i);
				return new Tipo("void");
			}
		}
		return new Tipo("error","Constraint "+ctx.ID().getText()+" does not exist");
	}
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation returns the result of calling
	 * {@link #visitChildren} on {@code ctx}.</p>
	 */
	@Override public Tipo visitExpr12(@NotNull DDLGrammarParser.Expr12Context ctx) { 
		return visit(ctx.expr2()); 
	}
	
	@Override public Tipo visitFactorID(@NotNull DDLGrammarParser.FactorIDContext ctx) { 
		ArrayList<String> currentExpression=new ArrayList<String>();
		String alias=ctx.getText();
		//revisar si se permiten tableid
		if(!tableID){
			if(ctx.TABLEID()!=null){
				return new Tipo("error","ID.ID is only allowed in Select");
			}
		}
		
		Tipo resultado=null;
		//revisar si existe el nombre explicito
		for(int i=0;i<currentColumns.size();i++){
			JSONObject current=(JSONObject)currentColumns.get(i);
			String columnName=(String)current.get("name");
			if(alias.equals(columnName)){
				currentExpression.add(alias);
				resultado=new Tipo((String)current.get("type"),currentExpression);
				if("CHAR".equals((String)current.get("type"))){
					resultado.setLength(Integer.parseInt(current.get("length").toString()));
				}
				return resultado;
			}
			
		}
		//revisar que este contenido en id.id
		currentExpression=new ArrayList<String>();
		int count=0;
		String newName="";
		for(int i=0;i<currentColumns.size();i++){
			JSONObject current=(JSONObject)currentColumns.get(i);
			String columnName=(String)current.get("name");
			String checking=columnName.substring(columnName.indexOf(".")+1);
			if(alias.equals(checking)){
				resultado=new Tipo((String)current.get("type"),currentExpression);
				if("CHAR".equals((String)current.get("type"))){
					resultado.setLength(Integer.parseInt(current.get("length").toString()));
				}
				newName=columnName;
				count++;
			}
			
		}
		if(count==0){
			return new Tipo("error","Column "+ctx.ID()+" does not exist in relation");
		}
		else if(count==1){
			currentExpression.add(newName);
			return resultado;
		}
		else{
			return new Tipo("error","Ambiguos reference for "+alias);
		}
	}
	
	//Metodos para el DML*******************************************************************
	@Override public Tipo visitDmlInsert(@NotNull DDLGrammarParser.DmlInsertContext ctx) {
		start();
		if(currentDataBase==null){
			return new Tipo("error", "ERROR.-Se debe seleccionar una base de datos.");	
		}
		memoria = new HashMap<String, JSONObject>();
		currentDataBase = readJSON(baseDir+databaseName+"/master.json");
		int contador = 0;
		for(DDLGrammarParser.InsertContext insert : ctx.insert()){
			Tipo t = visit(insert);
			if (t.isError()) return t;
			contador++;
		}
		
		for(String key: memoria.keySet()){
			createFile(baseDir+databaseName+"/"+key+".json",memoria.get(key).toJSONString());
		}
		
		return new Tipo("void", "Succesfully inserted "+ contador+" entries in "+end());
		
	}
	@Override public Tipo visitDmlUpdate(@NotNull DDLGrammarParser.DmlUpdateContext ctx) {
		if(currentDataBase==null){
			return new Tipo("error", "ERROR.-Se debe seleccionar una base de datos.");	
		}
		memoria = new HashMap<String, JSONObject>();
		Tipo t=new Tipo("void");
		for(DDLGrammarParser.UpdateContext update : ctx.update()){
			t = visit(update);
			if (t.isError()) return t;
		}
		
		for(String key: memoria.keySet()){
			createFile(baseDir+databaseName+"/"+key+".json",memoria.get(key).toJSONString());
		}
		return t;
	}
	@Override public Tipo visitDmlDelete(@NotNull DDLGrammarParser.DmlDeleteContext ctx) {
		if(currentDataBase==null){
			return new Tipo("error", "ERROR.-Se debe seleccionar una base de datos.");	
		}
		memoria = new HashMap<String, JSONObject>();
		Tipo t=new Tipo("void");
		for(DDLGrammarParser.DeleteContext delete : ctx.delete()){
			t = visit(delete);
			if (t.isError()) return t;
		}
		
		for(String key: memoria.keySet()){
			createFile(baseDir+databaseName+"/"+key+".json",memoria.get(key).toJSONString());
		}
		
		return t;
	}
	
	@Override public Tipo visitDmlSelect(@NotNull DDLGrammarParser.DmlSelectContext ctx) { 
		Tipo resultado=null;
		if(currentDataBase==null){
			return new Tipo("error", "ERROR.-Se debe seleccionar una base de datos.");	
		}
		for(int i=0;i<ctx.select().size();i++){
			resultado=visit(ctx.select(i));
			if(resultado.isError()){
				return resultado;
			}
		}
		return resultado;
	}
	
	@Override public Tipo visitInsert(@NotNull DDLGrammarParser.InsertContext ctx) {
		tableID=false;
		if(verbose)print("Comprobando que se haya elegido una BD");
		if(currentDataBase==null){
			return new Tipo("error", "ERROR.-Se debe seleccionar una base de datos.");	
		}

		String tabla = ctx.ID(0).getText();
		
		if(verbose)print("Comprobando que exista la tabla");
		//Verfica que exista la tabla en la base de datos actual.
		JSONObject currentTable= getTable(tabla);
		if(currentTable==null){
			return new Tipo("error", "ERROR.-Table name "+tabla+" not available");
		}
		
		//Se guarda la relacion en memoria para optimizar el manejador.
		JSONObject relacion = getRelationFromMemory(tabla);
		
		
		JSONArray columns = (JSONArray) currentTable.get("columns");
		//Se crea el array a insertar
		JSONObject nueva = new JSONObject(); 
		
		
		int idSize = ctx.ID().size()-1;

		
		if(idSize==0){
			if(columns.size()!=ctx.literal().size()){
				return new Tipo("error", "ERROR.-No coincide el numero de valores del INSERT con las columnas de la tabla " + tabla);
			}
			int limite = ctx.literal().size();
			if(verbose)print("Comprobando tipos.");
			for(int i=0; i<limite; i++){
				//Se obtiene el valor a insertar
				String value = ctx.literal(i).getText();
				//Se obtienen los dos tipos para poder compararlos
				Tipo t1 = visit(ctx.literal(i));
				JSONObject t2 =(JSONObject)columns.get(i);
				
				try {
					//Se intenta castear ambos valores.
					
					value = castTypes(t2, t1, value);
					nueva.put(((JSONObject)columns.get(i)).get("name"), value);
				} catch (Exception e) {
					e.printStackTrace();
					return new Tipo("error", e.getMessage());
				}
			}
			
		}else{
			if(idSize!=ctx.literal().size()){
				return new Tipo("error", "ERROR.-No coincide el numero de valores del INSERT con las columnas especificadas en la tabla " + tabla);
			}
			int lim1 = columns.size();
			for(int i=0; i<lim1; i++){
				nueva.put(((JSONObject)columns.get(i)).get("name"), null);
			}
			if(verbose)print("Comprobando tipos.");
			for(int i=0; i<idSize; i++){
				String idCol = ctx.ID(i+1).getText();
				String value = ctx.literal(i).getText();
				JSONObject column = getColumn(columns, idCol);
				
				if(column==null){
					return new Tipo("error", "ERROR.-No existe la columna " + idCol + " en la tabla " + tabla);
				}
				
				Tipo t1 = visit(ctx.literal(i));
				JSONObject t2 = column;
				
				try {
					//Se intenta castear ambos valores.
					value = castTypes(t2, t1, value);
					nueva.put(((JSONObject)columns.get(i)).get("name"), value);
				} catch (Exception e) {
					e.printStackTrace();
					return new Tipo("error", e.getMessage());
				}
				nueva.put(idCol, value);
			}

		}
		
		
		//Intenta insertar. Se verifican las restricciones.
		try {
			insert(nueva, relacion, tabla);
		} catch (Exception e) {
			return new Tipo("error", e.getMessage());
		}
		
		return new Tipo("void", "Se ha insertado con éxito.");
		
	}
	
	@Override public Tipo visitUpdate(@NotNull DDLGrammarParser.UpdateContext ctx) {
		start();
		//Se extrae el nombre de la tabla 
		String tabla = ctx.ID(0).getText();
		//Verfica que exista la tabla en la base de datos actual.
		JSONObject currentTable= getTable(tabla);
		if(currentTable==null){
			return new Tipo("error", "ERROR.-Table name "+tabla+" not available in database " + databaseName);
		}
		
		currentColumns = (JSONArray)currentTable.get("columns");
		//Verifica que contenga al expression, de no ser así, no lo visita y no calcula la expression.
		boolean sinWhere = false;
		ArrayList<String> expr = null;
		if(ctx.expression()!=null){
			Tipo t = visit(ctx.expression());
			if(t.isError())return t;
			expr = t.getResultado();
		}else{
			sinWhere = true;
		}
		
		//Se guarda la relacion en memoria para optimizar el manejador.
		JSONObject relacion = getRelationFromMemory(tabla);
		//Se extraen los registros de la tabla a modificar
		JSONArray entries = (JSONArray)relacion.get("entries");
		int size = entries.size();
		
		//Se busca las restricciones
		JSONArray restricciones = (JSONArray)currentDataBase.get("constraints");
		int sizeRestrict = restricciones.size();
		JSONArray listFKRef = new JSONArray();
		JSONArray listFKLocal = new JSONArray();
		JSONObject PK = null;
		HashMap<String, JSONObject> misRelaciones = new HashMap();	
		
		for(int j=0; j<sizeRestrict; j++){
			JSONObject constr = (JSONObject) restricciones.get(j);
			if(constr.get("owner").equals(tabla) && constr.get("primaryKey")!=null){
				PK = constr;
			}
			if(constr.get("owner").equals(tabla) && constr.get("foreignKey")!=null){
				JSONObject foreignKey = (JSONObject)constr.get("foreignKey");
				listFKLocal.add(constr);
				String table = foreignKey.get("table").toString();
				misRelaciones.put(table, readJSON(baseDir+databaseName+"/"+table+".json"));
			}
			
			JSONObject foreignKey = (JSONObject)constr.get("foreignKey");
			if(foreignKey!=null && foreignKey.get("table").toString().equals(tabla)){
				listFKRef.add(constr);
				String table = constr.get("owner").toString();
				misRelaciones.put(table, readJSON(baseDir+databaseName+"/"+table+".json"));
			}
		}
		
		//Se realiza la comparacion de tipos
		LinkedHashMap<String, String> values = new LinkedHashMap<String, String>();
		int i = 1;
		for(DDLGrammarParser.LiteralContext literal: ctx.literal()){
			String id = ctx.ID(i).getText();
			String value = literal.getText();
			Tipo t = visit(literal);
			if(t.isError())return t;
			JSONObject col = getColumn(currentColumns, id);
			try{
				values.put(id, castTypes(col, t, value));
			}catch(Exception ex){
				return new Tipo("error", ex.getMessage());
			}
			i++;
		}
		
		int limValues = values.size();
		int sizeListFKRef = listFKRef.size();
		int contador = 0;
		for(int j= 0; j<size; j++){
			JSONObject tupla = (JSONObject)entries.get(j);
			if(sinWhere || validar(expr, tupla, false)){

				//Restricciones delete
				boolean bandera;
				for(int l= 0; l<sizeListFKRef; l++){
					bandera = true;
					JSONArray fkLocal = (JSONArray)((JSONObject)((JSONObject)listFKRef.get(l)).get("foreignKey")).get("references");
					for(String key: values.keySet()){
						if(fkLocal.contains(key)){
							bandera = false;
							break;
						}
					}
					if(bandera)continue;
					JSONArray fkRef = (JSONArray)((JSONObject)((JSONObject)listFKRef.get(l)).get("foreignKey")).get("columns");
					String tablaRef = ((JSONObject)listFKRef.get(l)).get("owner").toString();
					if(checkForeignKey(fkLocal, fkRef, tupla, misRelaciones.get(tablaRef)))
						return new Tipo("error", "ERROR.-No puede modificarse la tupla " + tupla + ".\nDETAIL: Porque viola la LLAVE FORANEA: " + ((JSONObject)listFKRef.get(j)).get("name"));
				}
				
				entries.remove(tupla);
				for(String key: values.keySet()){
					tupla.put(key, values.get(key));
				}
				
				//Restricciones Insert
				try {
					insert(tupla, relacion, tabla);
					i--;
					size--;
					contador++;
				} catch (Exception e) {
					return new Tipo("error", e.getMessage());
				}
				
			}
			
		}
		
		return new Tipo("void", "Modfied " + contador + " entries in "+end());
		
		
	}
	
	@Override public Tipo visitDelete(@NotNull DDLGrammarParser.DeleteContext ctx) {
		tableID=false;
		start();
		if(currentDataBase==null){
			return new Tipo("error", "ERROR.-Se debe seleccionar una base de datos.");	
		}
		//Se extrae el nombre de la tabla 
		String tabla = ctx.ID().getText();
		//Verfica que exista la tabla en la base de datos actual.
		JSONObject currentTable= getTable(tabla);
		if(currentTable==null){
			return new Tipo("error", "ERROR.-Table name "+tabla+" not available in database " + databaseName);
		}
		
		currentColumns = (JSONArray)currentTable.get("columns");
		boolean sinWhere = false;
		ArrayList<String> expr = null;
		if(ctx.expression()!=null){
			Tipo t = visit(ctx.expression());
			if(t.isError())return t;
			expr = t.getResultado();
		}else{
			sinWhere = true;
		}
		
		//Se guarda la relacion en memoria para optimizar el manejador.
		JSONObject relacion = getRelationFromMemory(tabla);
		
		JSONArray entries = (JSONArray)relacion.get("entries");
		int size = entries.size();
		
		//ScriptEngineManager manager = new ScriptEngineManager();
	    //ScriptEngine engine = manager.getEngineByName("js"); 
		//Se busca las restricciones
		JSONArray restricciones = (JSONArray)currentDataBase.get("constraints");
		int sizeRestrict = restricciones.size();
		JSONArray misRestricciones = new JSONArray();
		HashMap<String, JSONObject> misRelaciones = new HashMap();	
		
		for(int j=0; j<sizeRestrict; j++){
			JSONObject constr = (JSONObject) restricciones.get(j);
			JSONObject foreignKey = (JSONObject)constr.get("foreignKey");
			if(foreignKey!=null && foreignKey.get("table").toString().equals(tabla)){
				misRestricciones.add(constr);
				String table = constr.get("owner").toString();
				misRelaciones.put(table, readJSON(baseDir+databaseName+"/"+table+".json"));
			}
		}
		int limRes = misRestricciones.size();
		int contador = 0;
		
		for(int i = 0; i<size; i++){
			JSONObject tupla = (JSONObject)entries.get(i);
			if (sinWhere || validar(expr, tupla, false)){
				System.out.println("Se eliminará: " + tupla);
				if(limRes==0){
					entries.remove(i);
					size--;
					i--;
					contador++;
				}
				for(int j= 0; j<misRestricciones.size(); j++){
					JSONArray fkLocal = (JSONArray)((JSONObject)((JSONObject)misRestricciones.get(j)).get("foreignKey")).get("references");
					JSONArray fkRef = (JSONArray)((JSONObject)((JSONObject)misRestricciones.get(j)).get("foreignKey")).get("columns");
					String tablaRef = ((JSONObject)misRestricciones.get(j)).get("owner").toString();
					if(!checkForeignKey(fkLocal, fkRef, tupla, misRelaciones.get(tablaRef))){
						entries.remove(i);
						size--;
						i--;
						contador++;
					}else{
						return new Tipo("error", "ERROR.-No puede eliminarse la tupla " + tupla + ".\nDETAIL: Porque viola la LLAVE FORANEA: " + ((JSONObject)misRestricciones.get(j)).get("name"));
					}
				}
			}
		}
		
		return new Tipo("void", "Deleted " + contador + " entries in "+end());
		
	}
	
	@Override public Tipo visitSelect(@NotNull DDLGrammarParser.SelectContext ctx) { 
		tableID=true;
		start();
		//checkeo de from
		Tipo t1 = visit(ctx.from());
		if(t1.isError())return t1;
		//checko de part_select
		Tipo campos=visit(ctx.part_select());
		if(campos.isError()){
			return campos;
		}
		for(int i=0;i<campos.getResultado().size();i++){
			System.out.println(campos.getResultado().get(i));
		}
		//checko de where
		Tipo expression=null;
		if(ctx.where()!=null){
			expression =visit(ctx.where());
			if(expression.isError()){
				return expression;
			}
		}
		ArrayList<String> tablas =t1.getResultado();
		//creacion de tuplas y chequeo de tuplas
		JSONArray entries = new JSONArray();
		for(int i=0;i<tablas.size();i++){
			String cName=tablas.get(i);
			if(i==0){
				JSONObject currentT=getRelationFromMemory(tablas.get(i));
				JSONArray data=(JSONArray)currentT.get("entries");
				//sirve para agregar tabla. a cada elemento de la tupla
				for(int j=0;j<data.size();j++){
					JSONObject tuple = generarTupla(new JSONObject(),(JSONObject)data.get(j),cName);
					if(ctx.where()==null || validar(expression.getResultado(), tuple, false)){
						entries.add(tuple);
					}
				}
			}
			else{
				//informacion para poder hacer el producto
				JSONObject currentT=getRelationFromMemory(tablas.get(i));
				JSONArray data=(JSONArray)currentT.get("entries");
				JSONArray temp=new JSONArray();
				for(int j=0;j<entries.size();j++){
					for(int k=0;k<data.size();k++){
						JSONObject resultado=generarTupla((JSONObject)entries.get(j),(JSONObject)data.get(k),cName);
						System.out.println(resultado);
						if(i!=tablas.size()-1){
							temp.add(resultado);
						}
						else{
							//evaluacion de where
							if(expression!=null){
								if(validar(expression.getResultado(), resultado, false)){
									temp.add(resultado);
								}
							}
							else{
								temp.add(resultado);
							}
						}
						
					}
				}
				entries=temp;
			}
		}
		//preparandose para el retorno
		if(ctx.order_by()!=null){
			Tipo order=visit(ctx.order_by());
			if(order.isError())return order;
			entries.sort(new JSONComparator(criterios));
		}
		//filtrado eliminacion de columnas innecesarias
		ArrayList<String> requested=campos.getResultado();
		if(requested.size()!=0){
			for(int w=currentColumns.size()-1;w>=0;w--){
				JSONObject current=(JSONObject)currentColumns.get(w);
				boolean found=false;
				for(int z=0;z<requested.size();z++){
					if(requested.get(z).equals(current.get("name"))){
						found=true;
					}
				}
				if(!found){
					currentColumns.remove(w);
				}
			}
		}
		JSONObject resultados=new JSONObject();
		resultados.put("headers", currentColumns.clone());
		resultados.put("entries", entries);
		Tipo returnValue=new Tipo("select","Fetched "+entries.size()+" in "+end());
		returnValue.setRelacion(resultados);;
		return returnValue;
	
	}
	@Override public Tipo visitPart_select(@NotNull DDLGrammarParser.Part_selectContext ctx) { 
		if(ctx.getText().equals("*")){
			return new Tipo("all",new ArrayList<String>());
		}
		else{
			ArrayList<String> resultado=new ArrayList<String>();
			for(int i=0;i<ctx.children.size();i++){
				String alias=ctx.children.get(i).getText();
				if(!alias.equals(",")){
					int count=0;
					//revisar si existe el nombre explicito
					for(int j=0;j<currentColumns.size();j++){
						JSONObject current=(JSONObject)currentColumns.get(j);
						String columnName=(String)current.get("name");
						if(alias.equals(columnName)){
							resultado.add(alias);
							count++;
							break;
						}
						
					}
					//revisar que este contenido en id.id
					String newName="";
					if(count==0){
						for(int j=0;j<currentColumns.size();j++){
							JSONObject current=(JSONObject)currentColumns.get(j);
							String columnName=(String)current.get("name");
							String checking=columnName.substring(columnName.indexOf(".")+1);
							if(alias.equals(checking)){
								newName=columnName;
								count++;
							}
							
						}
						if(count==0){
							return new Tipo("error","Column "+ctx.ID()+" does not exist in relation");
						}
						else if(count==1){
							resultado.add(newName);
						}
						else{
							return new Tipo("error","Ambiguos reference for "+alias);
						}
					}
				}
			}
			return new Tipo("void",resultado);
		}
		
	}
	@Override public Tipo visitFrom(@NotNull DDLGrammarParser.FromContext ctx) { 
		currentColumns=new JSONArray();
		JSONArray tablas=(JSONArray)currentDataBase.get("tables");
		ArrayList<String> resultado=new ArrayList<String>();
		
		//revisar la existencia de las tablas
		for(int i=0;i<ctx.ID().size();i++){
			String actual=ctx.ID(i).getText();
			resultado.add(actual);
			boolean found=false;
			for(int j=0;j<tablas.size();j++){
				JSONObject tablaActual=(JSONObject)tablas.get(j);
				if(actual.equals((String)tablaActual.get("name"))){
					JSONArray columnas = (JSONArray)tablaActual.get("columns");
					found=true;
					for(int k=0;k<columnas.size();k++){
						JSONObject currentC=(JSONObject)columnas.get(k);
						JSONObject nuevo = new JSONObject();
						nuevo.put("name", ctx.ID(i).getText()+"."+currentC.get("name"));
						nuevo.put("type", currentC.get("type"));
						if(currentC.containsKey("length")){
							nuevo.put("length", Integer.parseInt(currentC.get("length")+""));
						}
						currentColumns.add(nuevo);
					}
					break;
				}				
			}
			if(!found){
				Tipo res=new Tipo("error","Table name does not exist "+ctx.ID(i).getText());
				return res;
			}
		}
		return new Tipo ("void",resultado);
	}
	@Override public Tipo visitWhere(@NotNull DDLGrammarParser.WhereContext ctx) { return visitChildren(ctx);}
	@Override public Tipo visitOrder_by(@NotNull DDLGrammarParser.Order_byContext ctx) {
		criterios = new ArrayList<Criterion>();
		Tipo t = visitChildren(ctx);
		return t;
		
	}
	@Override public Tipo visitCriterion(@NotNull DDLGrammarParser.CriterionContext ctx) {
		//Comprobar que exista el TABLA.ID
		System.out.println("currentColumns: " + currentColumns);
		if(ctx.TABLEID()!=null){
			String id = ctx.TABLEID().getText();
			
			for(int i= 0; i<currentColumns.size();i++){
				JSONObject current=(JSONObject)currentColumns.get(i);
				if(!current.get("name").equals(id))
					continue;
				int order = -1;
				if(ctx.ASC()!=null)
					order = 1;
				criterios.add(new Criterion(id, order, current.get("type").toString()));
				return new Tipo("void");
			}
			return new Tipo("error", "Column "+ctx.ID()+" does not exist in relation");
			
		}else{
			String alias = ctx.ID().getText();
			//revisar que este contenido en id.id
			int count=0;
			String newName="";
			String type="";
			for(int j=0;j<currentColumns.size();j++){
				JSONObject current=(JSONObject)currentColumns.get(j);
				String columnName=(String)current.get("name");
				String checking=columnName.substring(columnName.indexOf(".")+1);
				if(alias.equals(checking)){
					newName=columnName;
					type = current.get("type").toString();
					count++;
				}
				
			}
			if(count==0){
				return new Tipo("error","Column "+ctx.ID()+" does not exist in relation");
			}
			else if(count==1){
				int order = -1;
				if(ctx.ASC()!=null)
					order = 1;
				criterios.add(new Criterion(newName, order, type));
				return new Tipo("void");
			}
			else{
				return new Tipo("error","Ambiguos reference for "+alias);
			}
		}
	}

	
	
	
	//Metodos para archivos********************************************************************************************************
	
	//metodos para creacion de base de datos--------------------------------------------------------------------------
	//creacion de directorios
	public void createDatabase(String name)throws Exception{
		JSONObject content=readJSON(baseDir+databaseFileName);
		print("Checking name availability");
		if(checkDatabaseName(name,content)){
			print("Creating Directories");
			JSONArray databases=(JSONArray)content.get("databases");
			JSONObject element=new JSONObject();
			element.put("name", name);
			element.put("length",0);
			databases.add(element);
			//escribir el archivo databases.json con el nombre agregado
			createFile(baseDir+databaseFileName,content+"");
			//crear el directorio
			createDirectory(baseDir+name);
			//crear el archivo master.json
			JSONObject master = new JSONObject();
			master.put("tables", new JSONArray());
			master.put("constraints", new JSONArray());
			createFile(baseDir+name+"/master.json",master+"");
		}else{
			throw new Exception("Database with name "+name+" already exists");
		}
	}
	//revision de si existe la base de datos
	public boolean checkDatabaseName(String name,JSONObject content){
		JSONArray databases=(JSONArray)content.get("databases");
		for(int i=0;i<databases.size();i++){
			String nameActual=(String)((JSONObject)databases.get(i)).get("name");
			if(nameActual.equals(name)){
				return false;
			}
		}
		return true;
	}
	//creacion de directorio
	public void createDirectory(String dir){
		boolean success = (new File(dir)).mkdirs();
		if (!success) {
		    // Directory creation failed
		}
	}
	//creacion de archivos tambien sobrescribe
	public void createFile(String dir,String content){
		FileWriter file=null;
		try {
			file = new FileWriter(dir,false);
			file.write(content);
		} catch (IOException e) {
			e.printStackTrace();
		}
		finally{
			if(file!=null){
				try {
					file.flush();
					file.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
	
	//metodos para eliminacion de bases de datos -----------------------------------------------------------------
	public void dropDatabase(String name)throws Exception{
		JSONObject content=readJSON(baseDir+databaseFileName);
		print("Checking table "+name+" existence");
		if(!checkDatabaseName(name,content)){
			JSONArray databases=(JSONArray)content.get("databases");
			for(int i=0;i<databases.size();i++){
				JSONObject actual=(JSONObject)databases.get(i);
				if(name.equals((String)actual.get("name"))){
					databases.remove(i);
					break;
				}
			}
			print("Deleting directories");
			//escritura del archivo sin la base de datos senhalada
			createFile(baseDir+databaseFileName,content+"");
			//eliminacion del directorio
			eraseDirectory(baseDir+name);
		}
		else{
			throw new Exception("Database "+name+" does not exist");
		}
		
	}
	
	//eliminacion de directorios
	public void eraseDirectory(String dir){
		File path = new File(dir.toUpperCase());
		if( path.exists() ) {
			File[] files = path.listFiles();
			if(files==null){
			}
			else{
			    for(int i=0; i<files.length; i++) {
			    	if(files[i].isDirectory()) {
			    		eraseDirectory(files[i].getAbsolutePath());
			    	}
			    	else {
			    		files[i].delete();
			    	}
			    }
			}
		}
		else{
			System.out.println("path does not exist"+path);
		}
		    path.delete();
	}
	//metodos para alteracion de bases de datos-------------------------------------------------------------------------------
	public void alterDatabase(String name,String newname)throws Exception{
		JSONObject content = readJSON(baseDir+databaseFileName);
		print("Checking table "+name+" existence");
		if(!checkDatabaseName(name,content)){
			print("Checking name availability");
			if(checkDatabaseName(newname,content)){
				//cambio del nombre del directorio
				print("Changing directories");
				changeDirectoryName(name,newname);
				//cambio del nombre en el archivo.json
				JSONArray databases = (JSONArray)content.get("databases");
				for (int i=0;i<databases.size();i++){
					JSONObject actual=(JSONObject)databases.get(i);
					if(name.equals((String)actual.get("name"))){
						databases.remove(i);
						JSONObject newO=new JSONObject();
						newO.put("name", newname);
						newO.put("length", Integer.parseInt(actual.get("length").toString()));
						databases.add(newO);
						break;
					}
				}
				createFile(baseDir+databaseFileName,content+"");
			}
			else{
				throw new Exception("Database with name "+newname+" already exists");
			}
		}
		else{
			throw new Exception("Database "+name+" does not exist");
		}
	}
	//cambia el nombre de un directorio o archivo
	public void changeDirectoryName(String name,String newName){
		File dir=new File(baseDir+name);
		File newdir= new File(dir.getParent()+"/"+newName);
		dir.renameTo(newdir);
	}
	//show databases *devuelve un String temporalmente
	public String showDatabases(){
		String res="";
		JSONObject content=readJSON(baseDir+databaseFileName);
		JSONArray databases=(JSONArray)content.get("databases");
		print("Loading databases");
		for(int i=0;i<databases.size();i++){
			JSONObject actual=(JSONObject)databases.get(i);
			res+="nombre: "+actual.get("name")+"-tablas: "+actual.get("length")+"\n";
		}
		return res;
	}
	//useDatabase tira una excepcion 
	public void useDatabase(String nombre)throws Exception{
		JSONArray databases=(JSONArray)(readJSON(baseDir+databaseFileName).get("databases"));
		for(int i=0;i<databases.size();i++){
			JSONObject actual=(JSONObject)databases.get(i);
			print("Checking table "+nombre+" existence");
			if(nombre.equals((String)actual.get("name"))){
				databaseName=nombre;
				return;
			}
			
		}
		throw new Exception("Database "+nombre+" does not exist");
	}
	//metodos para manejo de tablas*************************************************
	//agrega una columna a un objeto JSONArray
	public JSONArray addColumn(JSONArray columnas,String nombreColumna,String tipo)throws Exception{
		for(int i=0;i<columnas.size();i++){
			JSONObject currentC=(JSONObject)columnas.get(i);
			if(nombreColumna.equals((String)currentC.get("name"))){
				throw new Exception("Column "+nombreColumna+" already exists");
			}
		}
		JSONObject nuevo=new JSONObject();
		nuevo.put("name", nombreColumna);
		nuevo.put("type", tipo);
		columnas.add(nuevo);
		return columnas;
	}
	//agrega una primary key antes de llamar a este metodo es necesario verificar la existencia de de la tabla owner
	public JSONArray addPrimaryKey(JSONArray constraints,JSONArray columnas,String owner,String nombreC,ArrayList<String> nombresID)throws Exception{
		//revisar que no exista constraint de nombreC
		print("Checking Constraint name "+nombreC+" availability");
		print("Checking Primary Key uniqueness");
		for(int i=0;i<constraints.size();i++){
			JSONObject currentC=(JSONObject)constraints.get(i);
			if(nombreC.equals((String)currentC.get("name"))){
				throw new Exception("Constraint with name "+nombreC+" already exists");
			}
			if(owner.equals((String)currentC.get("owner"))&&(currentC.get("primaryKey")!=null)){
				throw new Exception("Primary key for table "+owner+" already exists");
			}
		}
		print("Checking existence of local column names");
		JSONArray arrayID = new JSONArray();
		for(int j=0;j<nombresID.size();j++){
			boolean found=false;
			for(int i=0;i<columnas.size();i++){
				JSONObject cActual=(JSONObject)columnas.get(i);
				if(nombresID.get(j).equals((String)cActual.get("name"))){
					found=true;
				}
				
			}
			if(!found){
				throw new Exception("Column "+nombresID.get(j)+" does not exist in "+owner);
			}
			arrayID.add(nombresID.get(j));
		}
		JSONObject nuevo = new JSONObject();
		nuevo.put("owner", owner);
		nuevo.put("name", nombreC);
		nuevo.put("primaryKey", arrayID);
		constraints.add(nuevo);
		return constraints;
	}
	//agrega una foreign key antes de llamar a este metodo es necesario verificar la existencia de la tabla owner(si no es una tabla nueva)
	public JSONArray addForeignKey(JSONArray constraints,JSONArray columnas,String owner,String nombreC,ArrayList<String> fromC,String references,ArrayList<String> toC)throws Exception{
		//revisar que no exista una constraint nombre C
		print("Checking Constraint name "+nombreC+" availability");
		for(int i=0;i<constraints.size();i++){
			JSONObject currentC=(JSONObject)constraints.get(i);
			if(nombreC.equals((String)currentC.get("name"))){
				throw new Exception("Constraint with name "+nombreC+" already exists");
			}
		}
		//revisar que las columnas en fromc existan
		for(int j=0;j<fromC.size();j++){
			boolean found=false;
			for(int i=0;i<columnas.size();i++){
				JSONObject cActual=(JSONObject)columnas.get(i);
				if(fromC.get(j).equals((String)cActual.get("name"))){
					found=true;
				}
				
			}
			if(!found){
				throw new Exception("Column "+fromC.get(j)+" does not exist in "+owner);
			}
		}
		//revisar que exista la tabla a la que se le hace referencia
		print("Checking table "+references+" existence");
		JSONArray tables=(JSONArray)readJSON(baseDir+databaseName+"/master.json").get("tables");
		JSONArray referencedTable=null;
		boolean foundT=false;
		for(int i=0;i<tables.size();i++){
			JSONObject current=(JSONObject)tables.get(i);
			if(references.equals((String)current.get("name"))){
				foundT=true;
				referencedTable=(JSONArray)current.get("columns");
				break;
			}
		}
		
		if(!foundT){
			throw new Exception("Table "+references+" not found");
		}
		//ver que no exista nombre de constraint repetido
		print("Checking existence of local and referenced column names");
		if(fromC.size()!=toC.size()){
			throw new Exception("Size of fields do not match ");
		}
		boolean foundPK=false;
		for(int i=0;i<constraints.size();i++){
			JSONObject currentC=(JSONObject)constraints.get(i);
			if(nombreC.equals((String)currentC.get("name"))){
				throw new Exception("Constraint with name "+nombreC+" already exists");
			}
			else{
				//la constraint debe ser foreign key
				JSONObject currentFK=(JSONObject)currentC.get("foreignKey");
				if(currentFK!=null){
					JSONArray columnsD =(JSONArray)currentFK.get("columns");
					//revisar que cada columna solo haga regerencia a otra columna, no a varias
					for (int j=0;i<fromC.size();j++){
						for(int k=0;k<columnsD.size();k++){
							if(fromC.get(j).equals((String)columnsD.get(k))){
								throw new Exception("The column "+columnsD.get(j)+" already has a foreign key");
							}
						}
					}
				}
			}
			print("Checking existence of primary key in "+references);
			//revisar que exista la primary key que contiene a todas las columnas de la foreign key
			if(references.equals((String)currentC.get("owner"))&&currentC.get("primaryKey")!=null){
				JSONArray keys=(JSONArray)currentC.get("primaryKey");
				for(int k=0;k<toC.size();k++){
					boolean foundC=false;
					
					for(int x=0;x<keys.size();x++){
						if(toC.get(k).equals((String)keys.get(x))){
							foundC=true;
							break;
						}
					}
					if(!foundC){
						String combination=toC.get(0);
						for(int y=1;y<toC.size();y++){
							combination+=","+toC.get(y);
						}
						throw new Exception("Table "+references+" has no primary key "+combination);
					}
				}
				foundPK=true;
			}
		}
		if(!foundPK){
			String combination=toC.get(0);
			for(int y=1;y<toC.size();y++){
				combination+=","+toC.get(y);
			}
			throw new Exception("Table "+references+" has no primary key "+combination);
		}
		//revisar que sean del mismo tipo las columnas
		print("Checking type of local and referenced columns");
		for(int i=0;i<fromC.size();i++){
			Tipo tipof=null;
			//encontrar el tipo del elemento i de from c
			for(int j=0;j<columnas.size();j++){
				JSONObject current=(JSONObject)columnas.get(j);
				String actual =(String)current.get("name");
				if(fromC.get(i).equals(actual)){
					tipof=new Tipo((String)current.get("type"));
					if(tipof.getTipo().equals("CHAR")){
						tipof.setLength((int)(long)current.get("length"));
					}
				}
			}
			
			Tipo tipot=null;
			//encontrar el tipo del elemento i de to c
			for(int j=0;j<referencedTable.size();j++){
				JSONObject current=(JSONObject)referencedTable.get(j);
				String actual =(String)current.get("name");
				if(toC.get(i).equals(actual)){
					tipot=new Tipo((String)current.get("type"));
					if(tipot.getTipo().equals("CHAR")){
						tipot.setLength((int)(long)current.get("length"));
					}
				}
			}
			if(!tipof.getTipo().equals(tipot.getTipo())){
				throw new Exception("Column "+fromC.get(i)+" and colummn "+toC.get(i)+" do not have the same type");
			}
			else{
				if(tipof.getTipo().equals("CHAR")){
					if(tipof.getLength()!=tipot.getLength()){
						throw new Exception("Column "+fromC.get(i)+" and colummn "+toC.get(i)+" do not have the same type");
					}
				}
			}
		}
		//creacion de la constraint
		JSONObject nuevo = new JSONObject();
		nuevo.put("owner", owner);
		nuevo.put("name", nombreC);
		JSONObject key=new JSONObject();
		key.put("table", references);
		JSONArray columnasTabla= new JSONArray();
		JSONArray columnasRef=new JSONArray();
		for(int i=0;i<fromC.size();i++){
			columnasTabla.add(fromC.get(i));
			columnasRef.add(toC.get(i));
		}
		key.put("columns", columnasTabla);
		key.put("references", columnasRef);
		nuevo.put("foreignKey", key);
		constraints.add(nuevo);
		return constraints;
	}
	
		//expression debe estar en post fix y el checkeo de tipos se hace en los visitor, la existencia de las columnas se hace en visitor
	public JSONArray addCheck(JSONArray constraints,String owner,String nombreC, ArrayList<String> expression) throws Exception{
		print("Checking Constraint name "+nombreC+" availability");
		for(int i=0;i<constraints.size();i++){
			JSONObject currentC=(JSONObject)constraints.get(i);
			if(nombreC.equals((String)currentC.get("name"))){
				throw new Exception("Constraint with name "+nombreC+" already exists");
			}
		}
		JSONObject nuevo=new JSONObject();
		nuevo.put("owner",owner);
		nuevo.put("name",nombreC);
		JSONArray expr=new JSONArray();
		print("Checking expression");
		for(int i=0;i<expression.size();i++){
			expr.add(expression.get(i));
		}
		nuevo.put("check", expr);
		constraints.add(nuevo);
		return constraints;
	}
	
	//cambia el nombre de la tabla de parametro es necesario pasar el JSON Object que corresponde al master.json de la base de datos utilizada
	public boolean checkTableName(String name,JSONObject master){
		JSONArray tables=(JSONArray)master.get("tables");
		for(int i=0;i<tables.size();i++){
			JSONObject current = (JSONObject)tables.get(i);
			if(name.equals((String)current.get("name"))){
				return false;
			}
		}
		return true;
	}
	//renombra la tabla de nombre name en la base de datos currentDatabase
	public void renameTable(String name,String newName)throws Exception{
		JSONObject master=readJSON(baseDir+databaseName+"/master.json");
		if(!checkTableName(newName,master)){
			throw new Exception("Table name "+newName+" not available");
		}
		JSONArray tables=(JSONArray)master.get("tables");
		JSONArray constraints=(JSONArray)master.get("constraints");
		boolean found=false;
		//renombrar el campo en el listado de nombres
		for(int i=0;i<tables.size();i++){
			JSONObject currentT=(JSONObject)tables.get(i);
			if(name.equals((String)currentT.get("name"))){
				tables.remove(i);
				JSONObject nuevo=new JSONObject();
				nuevo.put("name",newName);
				nuevo.put("length", (String)currentT.get("length"));
				tables.add(nuevo);
			}
		}
		//renombrar todos las referencias en los constraints
		for(int i=0;i<constraints.size();i++){
			JSONObject currentC=(JSONObject)constraints.get(i);
			JSONObject nuevo=new JSONObject();
			boolean change=false;
			if(name.equals((String)currentC.get("owner"))){
				change=true;
				nuevo.put("owner",newName);
				nuevo.put("name", (String)currentC.get("name"));
			}
			else{
				nuevo=currentC;
			}
			JSONObject fk=(JSONObject)currentC.get("foreignKey");
			if(fk!=null){
				JSONObject newFK=new JSONObject();
				if(name.equals((String)fk.get("tables"))){
					change=true;
					newFK.put("columns", (JSONArray)fk.get("columns"));
					newFK.put("references", (JSONArray)fk.get("references"));
					newFK.put("table", newName);
					nuevo.remove("foreignKey");
					nuevo.put("foreignKey", fk);
				}
			}
			if(change){
				constraints.remove(i);
				constraints.add(nuevo);
			}
		}
	}
	
	//
	//lectura de jsons *******************************
	//lectura de archivos databases.json
	public JSONObject readJSON(String dir){
		JSONObject result=null;
		JSONParser parser = new JSONParser();
		Object obj;
		FileReader archivo=null;
		try {
			archivo=new FileReader(dir);
			obj = parser.parse(archivo);
			result = (JSONObject) obj;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		}
		finally{
			if(archivo!=null){
				try {
					archivo.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		return result;
	}
	
	//############ METODOS DML #######################################

	//Metodo para obtener una determinada tabla de la base de datos actual.
	public JSONObject getTable(String name){
		JSONArray tablas = (JSONArray) currentDataBase.get("tables");
		JSONObject currentTable=null;
		boolean encontrado = false;
		for(int i = 0; i<tablas.size(); i++){
			JSONObject current = (JSONObject) tablas.get(i);
			if(name.equals(current.get("name").toString())){
				currentTable = current;
				break;
			}
		}
		return currentTable;
	}
	
	public JSONObject getColumn(JSONArray columns, String name){
		Iterator<JSONObject> it = columns.iterator();
		while(it.hasNext()){
			JSONObject e = it.next();
			if(e.get("name").toString().equals(name)){
				return e;
			}
		}
		return null;
	}
	
	
	public boolean checkPrimaryKey(JSONArray pk, JSONObject values, JSONObject table){
		JSONArray entries = (JSONArray)table.get("entries");
		int lim1 = entries.size();
		int lim2 = pk.size();

		for(int i=0; i<lim1; i++){
			boolean encontrado = true;
			for(int j=0; j<lim2; j++){
				String key = pk.get(j).toString();
				if(!((JSONObject)entries.get(i)).get(key).equals(values.get(key))){
					encontrado = false;
					break;
				}
			}
			if(encontrado)return false;
		}
		return true;
		
	}
	
	public boolean checkForeignKey(JSONArray fkLocal, JSONArray fkRef, JSONObject values, JSONObject table){
		JSONArray entries = (JSONArray)table.get("entries");
		int lim1 = entries.size();
		int lim2 = fkLocal.size();

		for(int i=0; i<lim1; i++){
			boolean encontrado = true;
			for(int j=0; j<lim2; j++){
				String keyLocal = fkLocal.get(j).toString();
				String keyRef = fkRef.get(j).toString();
				
				if(!((JSONObject)entries.get(i)).get(keyRef).equals(values.get(keyLocal))){
					encontrado = false;
					break;
				}
			}
			if(encontrado)return true;
		}
		return false;
	}
	//null value se ingresa como false si se quiere que las expresiones que encuentren un null den como resulatdo un false
	public boolean validar (ArrayList<String> input,JSONObject tuple,boolean nullValue){
		Stack<String> temp=new Stack<String>();
		for(int i=0;i<input.size();i++){
			String actual = input.get(i);
			System.out.println(temp+" var"+actual);
			//revisar si es operador
			//operador and
			if(actual.equals("AND")){
				if(temp.peek()==null||temp.get(temp.size()-2)==null){
					temp.pop();
					temp.pop();
					temp.push(nullValue+"");
					continue;
				}
				boolean var1=Boolean.parseBoolean(temp.pop());
				boolean var2=Boolean.parseBoolean(temp.pop());
				temp.push((var1&&var2)+"");
			}
			//operador or
			else if(actual.equals("OR")){
				if(temp.peek()==null||temp.get(temp.size()-2)==null){
					temp.pop();
					temp.pop();
					temp.push(nullValue+"");
					continue;
				}
				boolean var1=Boolean.parseBoolean(temp.pop());
				boolean var2=Boolean.parseBoolean(temp.pop());
				temp.push((var1||var2)+"");
			}
			else if(actual.equals("NOT")){
				if(temp.peek()==null){
					temp.pop();
					temp.push(nullValue+"");
					continue;
				}
				boolean var=Boolean.parseBoolean(temp.pop());
				temp.push(!var+"");
			}
			else if(actual.equals(">")){
				if(temp.peek()==null||temp.get(temp.size()-2)==null){
					temp.pop();
					temp.pop();
					temp.push(nullValue+"");
					continue;
				}
				String var1=temp.pop();
				String var2=temp.pop();
				//evaluo alreves por que estan saliendo de la pila
				int res=compareTo(var2,var1);
				temp.push((res>0)+"");
			}
			else if(actual.equals("<")){
				if(temp.peek()==null||temp.get(temp.size()-2)==null){
					temp.pop();
					temp.pop();
					temp.push(nullValue+"");
					continue;
				}
				String var1=temp.pop();
				String var2=temp.pop();
				//evaluo alreves por que estan saliendo de la pila
				int res=compareTo(var2,var1);
				temp.push((res<0)+"");
			}
			else if(actual.equals("=")){
				if(temp.peek()==null||temp.get(temp.size()-2)==null){
					temp.pop();
					temp.pop();
					temp.push(nullValue+"");
					continue;
				}
				String var1=temp.pop();
				String var2=temp.pop();
				//evaluo alreves por que estan saliendo de la pila
				int res=compareTo(var2,var1);
				temp.push((res==0)+"");
			}
			else if(actual.equals("<>")){
				if(temp.peek()==null||temp.get(temp.size()-2)==null){
					temp.pop();
					temp.pop();
					temp.push(nullValue+"");
					continue;
				}
				String var1=temp.pop();
				String var2=temp.pop();
				//evaluo alreves por que estan saliendo de la pila
				int res=compareTo(var2,var1);
				temp.push((res!=0)+"");
			}
			else if(actual.equals("<")){
				if(temp.peek()==null||temp.get(temp.size()-2)==null){
					temp.pop();
					temp.pop();
					temp.push(nullValue+"");
					continue;
				}
				String var1=temp.pop();
				String var2=temp.pop();
				//evaluo alreves por que estan saliendo de la pila
				int res=compareTo(var2,var1);
				temp.push((res<0)+"");
			}
			else if(actual.equals(">=")){
				if(temp.peek()==null||temp.get(temp.size()-2)==null){
					temp.pop();
					temp.pop();
					temp.push(nullValue+"");
					continue;
				}
				String var1=temp.pop();
				String var2=temp.pop();
				//evaluo alreves por que estan saliendo de la pila
				int res=compareTo(var2,var1);
				temp.push((res>=0)+"");
			}
			else if(actual.equals("<=")){
				if(temp.peek()==null||temp.get(temp.size()-2)==null){
					temp.pop();
					temp.pop();
					temp.push(nullValue+"");
					continue;
				}
				String var1=temp.pop();
				String var2=temp.pop();
				//evaluo alreves por que estan saliendo de la pila
				int res=compareTo(var2,var1);
				temp.push((res<=0)+"");
			}
			else{
				String var=actual;
				String regex="[a-zA-Z](\\w)*(.[a-zA-Z](\\w)*)?";
				//revisar si es id
				if(var.matches(regex)){
					temp.push((String)tuple.get(actual));
				}
				else{
					temp.push(var);
				}
			}
		}
		System.out.println(temp);
		if(!temp.empty()){
			String value=temp.pop();
			return value.equals("true");
		}
		return true;

	}
	//si el value1<value2 devuelve negativo, si value1>value2 devuelvo positivo, si son iguales 0
	public int compareTo(String value1,String value2){
		//ver char y date
		if(value1.startsWith("'")||(value1.indexOf("-")>0)){
			return value1.compareTo(value2);
		}
		else{
			float var1=Float.parseFloat(value1);
			float var2=Float.parseFloat(value2);
			if(var1<var2){
				return -1;
			}
			else if(var2<var1){
				return 1;
			}
			else{
				return 0;
			}
		}
	}
	

	//Obtiene una relacion a partir del nombre enviado como parametro. Si esta en memoría solo la retorna y si no, la carga y luego la retorna.
	public JSONObject getRelationFromMemory(String name){
		JSONObject relacion;
		if(memoria.containsKey(name)){
			relacion = memoria.get(name);
		}else{
			relacion=readJSON(baseDir+databaseName+"/"+name+".json");
			memoria.put(name, relacion);
		}
		return relacion;
	}
	
	//Metodo que intenta castear los tipos pasados como parametros, de no ser compatibles
	public String castTypes(JSONObject column, Tipo literal, String value) throws Exception{
		String t1 = column.get("type").toString();
		String t2 = literal.getTipo();
		if(t1.equals(t2)){
			if(t1.equals("CHAR")){
				int l1 = Integer.parseInt(column.get("length").toString());
				int l2 = literal.getLength()-2;
				if(l1>=l2){
					return value;
				}else{
					throw new Exception("ERROR.-El CHAR "+value+"tiene un tamaño muy grande.");
				}
			}else if(t1.equals("DATE")){
				try{
					formatoFecha.parse(value);
				}catch(Exception ex){
					ex.printStackTrace();
					throw new Exception("ERROR.-La fecha "+value+" no es una fecha valida.");
				}
			}
			return value;
		}else{
			if (t1.equals("INT") && t2.equals("FLOAT")){
				int val = (int)Float.parseFloat(value);
				return val+"";
			}
			if(t1.equals("FLOAT") && t2.equals("INT")){
				float val = Integer.parseInt(value);
				return  val + "";
			}
			throw new Exception("ERROR.-El valor " + value + " no coincide con el tipo de la columna: " + column.get("name"));
		}
	}
	
	public void insert(JSONObject nueva, JSONObject relacion, String tabla) throws Exception{
		JSONArray restricciones = (JSONArray)currentDataBase.get("constraints");
		for(int i=0; i<restricciones.size(); i++){
			JSONObject constr = (JSONObject) restricciones.get(i);
			if(constr.get("owner").toString().equals(tabla)){
				if(constr.containsKey("primaryKey")){
					if(verbose)print("Validando PRIMARY KEYS.");
				
					JSONArray pk = (JSONArray) constr.get("primaryKey");
					if(!checkPrimaryKey(pk, nueva, relacion))
						throw new Exception("ERROR.-Se esta violando la llave primaria: " + constr.get("name"));
				}else if(constr.containsKey("foreignKey")){
					if(verbose)print("Validando FOREIGN KEYS.");
					
					JSONObject fkObj = (JSONObject)constr.get("foreignKey");
					JSONArray fkLocal = (JSONArray)fkObj.get("columns");
					JSONArray fkRef = (JSONArray)fkObj.get("references");
					String tableRef = fkObj.get("table").toString();
					
					JSONObject relacionRef;
					if(memoria.containsKey(tableRef)){
						relacionRef = memoria.get(tableRef);
					}else{
						relacionRef=readJSON(baseDir+databaseName+"/"+tableRef+".json");
						memoriaRef.put(tableRef, relacionRef);
					}
					if(!checkForeignKey(fkLocal, fkRef, nueva, relacionRef))
						throw new Exception("ERROR.-Se esta violando la llave foranea: " + constr.get("name"));
					
				}else{
					if(verbose)print("Validando CHECK");
					JSONArray expression = (JSONArray) constr.get("check");
					ArrayList<String> expr = new ArrayList<String>();
					for(int k=0; k<expression.size(); k++){
						expr.add(expression.get(k).toString());
					}
					if(!validar(expr, nueva, false)){
						throw new Exception("ERROR.-Se esta violando la condicion de CHECK: " + constr.get("name"));
					}
				}
			}
		}
		if(verbose)print("Insertando...");
		((JSONArray)relacion.get("entries")).add(nueva);
	}
	//table1 contiene todas las entradas
	public JSONObject generarTupla(JSONObject table1,JSONObject table2,String name2){
		JSONObject result= new JSONObject();
		result.putAll(table1);
		Set<String> keys=table2.keySet();
		Iterator iter=keys.iterator();
		while(iter.hasNext()){
			String keyA=(String)iter.next();
			result.put(name2+"."+keyA, table2.get(keyA));
		}
		
		return result;
	}
	//tomar el tiempo
	public void start(){
		starttime=System.nanoTime();
	}
	public String end(){
		return ((System.nanoTime()-starttime)/1000000)+"ms";
	}
	
	//Imprimir en consola
	public void print(String message){
		consola.setText(consola.getText() + message+ "\n");
	}
}



