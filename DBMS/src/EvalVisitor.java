import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import org.antlr.v4.runtime.misc.NotNull;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;


public class EvalVisitor extends DDLGrammarBaseVisitor<Tipo>{
	private String baseDir="databases/";
	private String databaseFileName="databases.json";
	private TablaTipos tablaTipos;
	private static String currentDatabase="";
	private String owner="";
	private JSONArray currentConstraints=null;
	private JSONArray currentColumns=null;
	public EvalVisitor(){
		if(!currentDatabase.equals("")){
			System.out.println(currentDatabase);
		}
		File filep = new File(baseDir+databaseFileName);
		 
		// if file doesnt exists, then create it
		if (!filep.exists()) {
			System.out.println("No Existe");
			JSONObject registro= new JSONObject();
			registro.put("databases", new JSONArray());
			createFile(baseDir+databaseFileName,registro+"");
		}
		tablaTipos = new TablaTipos();
		
		//Se agregan los tipos de datos primitivos a la tabla de tipos.
		tablaTipos.agregar("INT", 11);
		tablaTipos.agregar("FLOAT", 0);
		tablaTipos.agregar("DATE", 10);
		tablaTipos.agregar("CHAR", 0);
		tablaTipos.agregar("BOOLEAN", 1);
	}
	
	@Override public Tipo visitAlterTableRename(@NotNull DDLGrammarParser.AlterTableRenameContext ctx) { return visitChildren(ctx); }
	
	@Override public Tipo visitDropDatabase(@NotNull DDLGrammarParser.DropDatabaseContext ctx) { 
		try {
			dropDatabase(ctx.ID().getText());
			return new Tipo("void", "Database "+ctx.ID().getText()+" eliminated succesfully");
		} catch (Exception e) {
			e.printStackTrace();
			return new Tipo("error", e.getMessage()); 
		} 
	}
	
	@Override public Tipo visitConstraintDecl3(@NotNull DDLGrammarParser.ConstraintDecl3Context ctx) { 
		Tipo res=visit(ctx.expression());
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
	
	@Override public Tipo visitShowTables(@NotNull DDLGrammarParser.ShowTablesContext ctx) { return visitChildren(ctx); }
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
		Tipo res = visit(ctx.factor());
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
			alterDatabase(ctx.ID(0).getText(),ctx.ID(1).getText());
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
		Tipo res1=visit(ctx.expr3());
		if(res1.getTipo().equals("error")){
			return res1;
		}
		ArrayList<String> newExpr=new ArrayList<String>();
		newExpr.addAll(res1.getResultado());
		Tipo res2=visit(ctx.unifactor());
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
		owner=ctx.ID(0).getText();
		JSONObject master=readJSON(baseDir+currentDatabase+"/master.json");
		currentConstraints=(JSONArray)master.get("constraints");
		if(!checkTableName(owner,master)){
			return new Tipo("error","Table name "+owner+" not available in "+currentDatabase);
		}
		JSONObject newTable =new JSONObject();
		newTable.put("name", owner);
		JSONArray columns =new JSONArray();
		for(int i=0;i<ctx.tipo().size();i++){
			Tipo current=visit(ctx.tipo(i));
			JSONObject newColumn = new JSONObject();
			newColumn.put("name", ctx.ID(i+1).getText());
			newColumn.put("type",current.getTipo());
			if(current.getTipo().equals("CHAR")){
				newColumn.put("length", current.getLength());
			}
			columns.add(newColumn);
			
		}
		System.out.println(columns);
		newTable.put("columns", columns);
		currentColumns=columns;
		for(int i=0;i<ctx.constraintDecl().size();i++){
			Tipo current=visit(ctx.constraintDecl(i));
			if(current.getTipo().equals("error")){
				return current;
			}
		}
		JSONArray tables = (JSONArray) master.get("tables");
		tables.add(newTable);
		createFile(baseDir+currentDatabase+"/master.json",master+"");
		JSONObject dataFile=new JSONObject();
		JSONArray entries=new JSONArray();
		dataFile.put("registros",entries);
		createFile(baseDir+currentDatabase+"/"+owner+".json",master+"");
		return new Tipo("void","Table "+owner+" created succesfully in "+currentDatabase);
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
	@Override public Tipo visitAlterTableAccion(@NotNull DDLGrammarParser.AlterTableAccionContext ctx) { return visitChildren(ctx); }
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
	@Override public Tipo visitShowColumnsFrom(@NotNull DDLGrammarParser.ShowColumnsFromContext ctx) { return visitChildren(ctx); }
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
		Tipo res1=visit(ctx.expr2());
		if(res1.getTipo().equals("error")){
			return res1;
		}
		ArrayList<String> newExpr=new ArrayList<String>();
		newExpr.addAll(res1.getResultado());
		Tipo res2=visit(ctx.expr3());
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
	@Override public Tipo visitDropTable(@NotNull DDLGrammarParser.DropTableContext ctx) { return visitChildren(ctx); }
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
		Tipo res1=visit(ctx.expression());
		if(res1.getTipo().equals("error")){
			return res1;
		}
		ArrayList<String> newExpr=new ArrayList<String>();
		newExpr.addAll(res1.getResultado());
		Tipo res2=visit(ctx.expr1());
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
	@Override public Tipo visitAccion1(@NotNull DDLGrammarParser.Accion1Context ctx) { return visitChildren(ctx); }
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation returns the result of calling
	 * {@link #visitChildren} on {@code ctx}.</p>
	 */
	@Override public Tipo visitAccion2(@NotNull DDLGrammarParser.Accion2Context ctx) { return visitChildren(ctx); }
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
	@Override public Tipo visitAccion3(@NotNull DDLGrammarParser.Accion3Context ctx) { return visitChildren(ctx); }
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation returns the result of calling
	 * {@link #visitChildren} on {@code ctx}.</p>
	 */
	@Override public Tipo visitExpr11(@NotNull DDLGrammarParser.Expr11Context ctx) { 
		Tipo res1=visit(ctx.expr1());
		if(res1.getTipo().equals("error")){
			return res1;
		}
		ArrayList<String> newExpr=new ArrayList<String>();
		newExpr.addAll(res1.getResultado());
		Tipo res2=visit(ctx.expr2());
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
	@Override public Tipo visitAccion4(@NotNull DDLGrammarParser.Accion4Context ctx) { return visitChildren(ctx); }
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
						newO.put("length", actual.get("length"));
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
				currentDatabase=nombre;
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
		}
		//revisar que exista la base de datos a la que se le hace referencia, y que cada campo exista en esa base de datos
		JSONArray tables=(JSONArray)readJSON(baseDir+currentDatabase+"/master.json").get("tables");
		boolean foundT=false;
		for(int i=0;i<tables.size();i++){
			JSONObject current=(JSONObject)tables.get(i);
			if(references.equals((String)current.get("name"))){
				foundT=true;
				JSONArray referencedColumns=(JSONArray)current.get("columns");
				for(int k=0;k<toC.size();k++){
					boolean foundC=false;
					for(int j=0;j<referencedColumns.size();j++){
						JSONObject column=(JSONObject)referencedColumns.get(j);
						if(toC.get(k).equals((String)column.get("name"))){
							foundC=true;
						}
					}
					if(!foundC){
						throw new Exception("Column "+toC.get(k)+" not found in "+(String)current.get("name"));
					}
				}
				
				break;
				
			}
		}
		if(!foundT){
			throw new Exception("Database "+references+" not found");
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
		JSONObject master=readJSON(baseDir+currentDatabase+"/master.json");
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
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return result;
	}
}
