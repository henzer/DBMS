import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import javax.swing.JOptionPane;

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
	private JSONObject currentDataFile=null;
	
	
	
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
		
		//Se agregan los tipos de datos primitivos a la tabla de tipos.
		tablaTipos.agregar("INT", 11);
		tablaTipos.agregar("FLOAT", 0);
		tablaTipos.agregar("DATE", 10);
		tablaTipos.agregar("CHAR", 0);
		tablaTipos.agregar("BOOLEAN", 1);
	}
	
	@Override public Tipo visitAlterTableRename(@NotNull DDLGrammarParser.AlterTableRenameContext ctx) { 
		//verificar la base de datos actual
		if(currentDataBase==null){
			return new Tipo("error","No database Selected");
		}
		//verificar la existencia de la tabla
		JSONArray tables=(JSONArray)currentDataBase.get("tables");
		int index=-1;
		int length=-1;
		for(int i=0;i<tables.size();i++){
			JSONObject current=(JSONObject)tables.get(i);
			if(ctx.ID(0).getText().equals((String)current.get("name"))){
				index=i;
				length=(int)(long)current.get("length");
				break;
			}
		}
		if(index==-1){
			return new Tipo("error","Table "+ctx.ID(0).getText()+" does not exist in "+databaseName);
		}
		//verificar la disponibilidad del nombre
		if(!checkTableName(ctx.ID(1).getText(),currentDataBase)){
			return new Tipo("error","Name "+ctx.ID(1).getText()+" is not available");
		}
		//escribir los archivos y realizar los cambios de nombre
		
		JSONObject newTable=new JSONObject();
		newTable.put("name", ctx.ID(1));
		newTable.put("length",length );
		tables.remove(index);
		tables.add(newTable);
		createFile(baseDir+databaseName+"/master.json",currentDataBase+"");
		changeDirectoryName(databaseName+"/"+ctx.ID(0).getText()+".json",ctx.ID(1).getText());
		return new Tipo("void","Table name changed succesfully");
	}
	
	@Override public Tipo visitDropDatabase(@NotNull DDLGrammarParser.DropDatabaseContext ctx) {  
		String nombre = ctx.ID().getText();
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
		Tipo res=  visit(ctx.expression());
		if(res.getTipo().equals("error")){
			return res;
		}
		addCheck(currentConstraints, owner, ctx.ID().getText(), res.getResultado());
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
		if(res1.getTipo().equals("error")){
			return res1;
		}
		ArrayList<String> newExpr=new ArrayList<String>();
		newExpr.addAll(res1.getResultado());
		Tipo res2=  visit(ctx.unifactor());
		if(res2.getTipo().equals("error")){
			return res2;
		}
		newExpr.addAll(res2.getResultado());
		newExpr.add(ctx.rel_op().getText());
		if(!res1.getTipo().equals(res2.getTipo())){
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
		if(currentDataBase==null){
			return new Tipo("error","No database selected");
		}
		owner=ctx.ID(0).getText();
		currentConstraints=(JSONArray)currentDataBase.get("constraints");
		if(!checkTableName(owner,currentDataBase)){
			return new Tipo("error","Table name "+owner+" not available in "+databaseName);
		}
		JSONObject newTable =new JSONObject();
		newTable.put("name", owner);
		JSONArray columns =new JSONArray();
		JSONObject dataFile=new JSONObject();
		for(int i=0;i<ctx.tipo().size();i++){
			JSONObject nuevo=new JSONObject();
			dataFile.put(ctx.ID(i+1), nuevo);
			Tipo current=  visit(ctx.tipo(i));
			nuevo.put("type", current.getTipo());
			JSONObject newColumn = new JSONObject();
			newColumn.put("name", ctx.ID(i+1).getText());
			newColumn.put("type",current.getTipo());
			if(current.getTipo().equals("CHAR")){
				newColumn.put("length", current.getLength());
				nuevo.put("length", current.getLength());
			}
			nuevo.put("entries", new JSONArray());
			columns.add(newColumn);
		}
		System.out.println(columns);
		newTable.put("columns", columns);
		currentColumns=columns;
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
		//creacion y alteracion de archivos
		JSONArray tables = (JSONArray) currentDataBase.get("tables");
		tables.add(newTable);
		createFile(baseDir+databaseName+"/master.json",currentDataBase+"");
		createFile(baseDir+databaseName+"/"+owner+".json",dataFile+"");
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
		//revisar que se haya seleccionado una base de datos
		if(currentDataBase==null){
			return new Tipo("error","No database selected");
		}
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
			if(ctx.ID().getText().equals((String)currentC.get("owner"))){
				constr+=currentC+"\n";
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
	@Override public Tipo visitStatement(@NotNull DDLGrammarParser.StatementContext ctx) { return visitChildren(ctx); }
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
		if(res1.getTipo().equals("error")){
			return res1;
		}
		ArrayList<String> newExpr=new ArrayList<String>();
		newExpr.addAll(res1.getResultado());
		Tipo res2=  visit(ctx.expr3());
		if(res2.getTipo().equals("error")){
			return res2;
		}
		newExpr.addAll(res2.getResultado());
		newExpr.add(ctx.eq_op().getText());
		if(!res1.getTipo().equals(res2.getTipo())){
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
		if(currentDataBase==null){
			return new Tipo("error","No database selected");
		}
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
		for(int i=1;i<ctx.ID().size();i++){
			ids.add(ctx.ID(i).getText());
		}
		try {
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
			addForeignKey(currentConstraints, currentColumns, owner, ctx.ID(0).getText(), from, referenced, to);
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
	@Override public Tipo visitExpression2(@NotNull DDLGrammarParser.Expression2Context ctx) { return visitChildren(ctx); }
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
		if(res1.getTipo().equals("error")){
			return res1;
		}
		ArrayList<String> newExpr=new ArrayList<String>();
		newExpr.addAll(res1.getResultado());
		Tipo res2=  visit(ctx.expr1());
		if(res2.getTipo().equals("error")){
			return res2;
		}
		newExpr.addAll(res2.getResultado());
		newExpr.add(ctx.cond_op2().getText());
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
		String name="";
		 for(int i=0;i<currentColumns.size();i++){
			 JSONObject currentC=(JSONObject)currentColumns.get(i);
			 name=(String)currentC.get("name");
			 if(ctx.ID().getText().equals(name)){
				 return new Tipo("error","Column "+ctx.ID().getText()+" already exists");
			 }
			 
		 }
		 JSONObject column=(JSONObject)currentDataFile.get(name);
		 JSONArray elements=(JSONArray)column.get("entries");
		 JSONObject nColumn=new JSONObject();
		 JSONArray nEntries=new JSONArray();
		 for(int i=0;i<elements.size();i++){
			 nEntries.add(null);
		 }
		 nColumn.put("entries", nEntries);
		 Tipo actual=  visit(ctx.tipo());
		 JSONObject newColumn=new JSONObject();
		 newColumn.put("name", ctx.ID().getText());
		 newColumn.put("type", actual.getTipo());
		 nColumn.put("type", actual.getTipo());
		 if(actual.getTipo().equals("CHAR")){
			 newColumn.put("length", actual.getLength());
			 nColumn.put("length", actual.getLength());
		 }
		 
		 for(int i=0;i<ctx.constraintDecl().size();i++){
			 Tipo currentT=  visit(ctx.constraintDecl(i));
			 if(currentT.getTipo().equals("error")){
				 return currentT;
			 }
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
		if(ctx.NUM()!=null){
			res.setLength(Integer.parseInt(ctx.NUM().getText()));
		}
		else{
			res.setLength(Integer.parseInt(ctx.UNUM().getText()));
		}
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
		for(int i=0;i<currentColumns.size();i++){
			if(ctx.ID().getText().equals((String)currentColumns.get(i))){
				index=i;
				break;
			}
		}
		if(index==-1){
			return new Tipo("error","Column "+ctx.ID().getText()+" does not exist in "+databaseName);
		}
		//revisar que no sea mencionado en constraints
		for(int i=0;i<currentConstraints.size();i++){
			 JSONObject currentC=(JSONObject)currentConstraints.get(i);
			 //revisar primary key
			 if(currentC.get("primaryKey")!=null){
				 JSONArray primaryKey = (JSONArray)currentC.get("primaryKey");
				 for(int j=0;j<primaryKey.size();j++){
					 if(ctx.ID().getText().equals((String)primaryKey.get(i))){
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
					 if(ctx.ID().getText().equals((String)check.get(j))){
						 return new Tipo("error","Column "+ctx.ID().getText()+" used in constraint "+currentC.get("name"));
					 }
				 }
			 }
		}
		//eliminacion de campos de los registros de la tabla correspondiente
		currentColumns.remove(index);
		currentDataFile.remove(ctx.ID().getText());
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
		if(res1.getTipo().equals("error")){
			return res1;
		}
		ArrayList<String> newExpr=new ArrayList<String>();
		newExpr.addAll(res1.getResultado());
		Tipo res2=  visit(ctx.expr2());
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
		currentExpression.add(ctx.getText());
		for(int i=0;i<currentColumns.size();i++){
			JSONObject current=(JSONObject)currentColumns.get(i);
			if(ctx.ID().getText().equals((String)current.get("name"))){
				if("CHAR".equals((String)current.get("type"))){
					Tipo resultado=new Tipo((String)current.get("type")); 
					resultado.setLength(Integer.parseInt((String)current.get("length")));
					return resultado;
				}
				return new Tipo((String)current.get("type"),currentExpression);
			}
		}
		return new Tipo("error","Column "+ctx.ID()+" does not exist in "+owner); 
	}
	
	//Metodos para el DML*******************************************************************
	@Override public Tipo visitDmlInsert(@NotNull DDLGrammarParser.DmlInsertContext ctx) {
		memoria = new HashMap<String, JSONObject>();
		System.out.println("hola");
		
		int contador = 0;
		for(DDLGrammarParser.InsertContext insert : ctx.insert()){
			Tipo t = visit(insert);
			if (t.isError()) return t;
			contador++;
		}
		
		for(String key: memoria.keySet()){
			createFile(baseDir+databaseName+"/"+key+".json",memoria.get(key).toJSONString());
		}
		
		return new Tipo("void", "Se ha insertado "+ contador+" registros con éxito.");
		
	}
	@Override public Tipo visitDmlUpdate(@NotNull DDLGrammarParser.DmlUpdateContext ctx) { return visitChildren(ctx); }
	@Override public Tipo visitDmlDelete(@NotNull DDLGrammarParser.DmlDeleteContext ctx) {
		memoria = new HashMap<String, JSONObject>();

		int contador = 0;
		for(DDLGrammarParser.DeleteContext delete : ctx.delete()){
			Tipo t = visit(delete);
			if (t.isError()) return t;
			contador++;
		}
		
		for(String key: memoria.keySet()){
			createFile(baseDir+databaseName+"/"+key+".json",memoria.get(key).toJSONString());
		}
		
		return new Tipo("void", "Se han eliminado "+ contador+" registros con éxito.");
	}
	
	@Override public Tipo visitDmlSelect(@NotNull DDLGrammarParser.DmlSelectContext ctx) { return visitChildren(ctx); }
	
	@Override public Tipo visitInsert(@NotNull DDLGrammarParser.InsertContext ctx) {
		if(currentDataBase==null){
			return new Tipo("error", "ERROR.-Se debe seleccionar una base de datos.");	
		}

		String tabla = ctx.ID(0).getText();
		
		//Verfica que exista la tabla en la base de datos actual.
		JSONObject currentTable= getTable(tabla);
		if(currentTable==null){
			return new Tipo("error", "ERROR.-Table name "+tabla+" not available");
		}
		
		//Se guarda la relacion en memoria para optimizar el manejador.
		JSONObject relacion;
		if(memoria.containsKey(tabla)){
			relacion = memoria.get(tabla);
		}else{
			relacion=readJSON(baseDir+databaseName+"/"+tabla+".json");
			memoria.put(tabla, relacion);
		}
		
		
		JSONArray columns = (JSONArray) currentTable.get("columns");
		//Se crea el array a insertar
		JSONObject nueva = new JSONObject(); 
		
		
		int idSize = ctx.ID().size()-1;

		
		if(idSize==0){
			if(columns.size()!=ctx.literal().size()){
				return new Tipo("error", "ERROR.-No coincide el numero de valores del INSERT con las columnas de la tabla " + tabla);
			}
			int limite = ctx.literal().size();
			for(int i=0; i<limite; i++){
				String value = ctx.literal(i).getText();
				
				Tipo t1 = visit(ctx.literal(i));
				String t2 = ((JSONObject)columns.get(i)).get("type").toString();
				if(!t1.getTipo().equals(t2)){
					if(!(t2.equals("FLOAT") && t1.getTipo().equals("INT")))
						return new Tipo("error", "ERROR.-No coincide el tipo de la columna " + ((JSONObject)columns.get(i)).get("name").toString() + " con el tipo ingresado.");
					else
						value = value + ".0";
				}else if (t1.getTipo().equals("CHAR")){
					int length = Integer.parseInt(((JSONObject)columns.get(i)).get("length").toString());
					if(length<t1.getLength()){
						return new Tipo("error", "ERROR.-La longitud del CHAR, supera lo soportado por la columna: " + ((JSONObject)columns.get(i)).get("name").toString() + "(" + length +")");
					}
				}
				nueva.put(((JSONObject)columns.get(i)).get("name"), value);
			}
			
		}else{
			if(idSize!=ctx.literal().size()){
				return new Tipo("error", "ERROR.-No coincide el numero de valores del INSERT con las columnas especificadas en la tabla " + tabla);
			}
			int lim1 = columns.size();
			for(int i=0; i<lim1; i++){
				nueva.put(((JSONObject)columns.get(i)).get("name"), null);
			}

			for(int i=0; i<idSize; i++){
				String idCol = ctx.ID(i+1).getText();
				String value = ctx.literal(i).getText();
				JSONObject column = getColumn(columns, idCol);
				
				if(column==null){
					return new Tipo("error", "ERROR.-No existe la columna " + idCol + " en la tabla " + tabla);
				}
				
				String t1 = column.get("type").toString();
				Tipo t2 = visit(ctx.literal(i));
				if(t2.isError())return t2;
				
				
				if(!t1.equals(t2.getTipo())){
					return new Tipo("error", "ERROR.-No coincide el tipo de la columna " + idCol + " con el tipo ingresado.");
				}else if (t1.equals("CHAR")){
					int length = Integer.parseInt(column.get("length").toString());
					if(length<t2.getLength()){
						return new Tipo("error", "ERROR.-La longitud del CHAR, supera lo soportado por la columna: " + idCol + "(" + length +")");
					}
				}
				nueva.put(idCol, value);
			}

		}
		
		//CHEQUEA PRIMARY KEY
		//Se verifican las restricciones.
		JSONArray restricciones = (JSONArray)currentDataBase.get("constraints");
		for(int i=0; i<restricciones.size(); i++){
			JSONObject constr = (JSONObject) restricciones.get(i);
			if(constr.containsKey("primaryKey")){
				JSONArray pk = (JSONArray) constr.get("primaryKey");
				if(!checkPrimaryKey(pk, nueva, relacion))
					return new Tipo("error", "ERROR.-Ya existe una tupla con esa llave");
			}else if(constr.containsKey("foreignKey")){
				
			}else{
				
			}
		}
		
		
		((JSONArray)relacion.get("entries")).add(nueva);
		
		return new Tipo("void", "Se ha insertado con éxito.");
		
	}
	
	@Override public Tipo visitUpdate(@NotNull DDLGrammarParser.UpdateContext ctx) { return visitChildren(ctx); }
	@Override public Tipo visitDelete(@NotNull DDLGrammarParser.DeleteContext ctx) { return visitChildren(ctx); }
	@Override public Tipo visitSelect(@NotNull DDLGrammarParser.SelectContext ctx) { return visitChildren(ctx);}
	

	//Metodos para archivos********************************************************************************************************
	
	//metodos para creacion de base de datos--------------------------------------------------------------------------
	//creacion de directorios
	public void createDatabase(String name)throws Exception{
		JSONObject content=readJSON(baseDir+databaseFileName);
		if(checkDatabaseName(name,content)){
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
			System.out.println(dir);
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
		if(!checkDatabaseName(name,content)){
			JSONArray databases=(JSONArray)content.get("databases");
			for(int i=0;i<databases.size();i++){
				JSONObject actual=(JSONObject)databases.get(i);
				if(name.equals((String)actual.get("name"))){
					databases.remove(i);
					break;
				}
			}
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
		    for(int i=0; i<files.length; i++) {
		    	if(files[i].isDirectory()) {
		    		eraseDirectory(files[i].getAbsolutePath());
		    	}
		    	else {
		    		files[i].delete();
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
		if(!checkDatabaseName(name,content)){
			if(checkDatabaseName(newname,content)){
				//cambio del nombre del directorio
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
		for(int i=0;i<constraints.size();i++){
			JSONObject currentC=(JSONObject)constraints.get(i);
			if(nombreC.equals((String)currentC.get("nombreC"))){
				throw new Exception("Constraint with name "+nombreC+" already exists");
			}
			if(owner.equals((String)currentC.get("owner"))&&(currentC.get("primaryKey")!=null)){
				throw new Exception("Primary key for table "+owner+" already exists");
			}
		}
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
		//revisar que exista la tabla a la que se le hace referencia
		JSONArray tables=(JSONArray)readJSON(baseDir+databaseName+"/master.json").get("tables");
		boolean foundT=false;
		for(int i=0;i<tables.size();i++){
			JSONObject current=(JSONObject)tables.get(i);
			if(references.equals((String)current.get("name"))){
				foundT=true;
				break;
			}
		}
		
		if(!foundT){
			throw new Exception("Database "+references+" not found");
		}
		//ver que no exista nombre de constraint repetido
		if(fromC.size()!=toC.size()){
			throw new Exception("Size of fields do not match ");
		}
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
			//revisar que exista la primary key que contiene a todas las columnas de la foreign key
			if(references.equals((String)currentC.get("owner"))&&currentC.get("primaryKey")!=null){
				JSONArray keys=(JSONArray)currentC.get("primaryKey");
				for(int k=0;k<toC.size();k++){
					boolean foundC=false;
					
					for(int x=0;x<keys.size();x++){
						if(toC.get(k).equals((String)keys.get(x))){
							foundC=true;
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
		key.put("columnas", columnasTabla);
		key.put("references", columnasRef);
		nuevo.put("foreignKey", key);
		return constraints;
	}
	
		//expression debe estar en post fix y el checkeo de tipos se hace en los visitor, la existencia de las columnas se hace en visitor
		public JSONArray addCheck(JSONArray constraints,String owner,String nombreC, ArrayList<String> expression){
			JSONObject nuevo=new JSONObject();
			nuevo.put("owner",owner);
			nuevo.put("name",nombreC);
			JSONArray expr=new JSONArray();
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
		try {
			obj = parser.parse(new FileReader(dir));
			result = (JSONObject) obj;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
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

}



