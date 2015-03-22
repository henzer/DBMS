import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;


public class EvalVisitor extends DDLGrammarBaseVisitor{
	String baseDir="databases/";
	String databaseFileName="databases.json";
	public EvalVisitor(){
		File filep = new File(baseDir+databaseFileName);
		 
		// if file doesnt exists, then create it
		if (!filep.exists()) {
			System.out.println("No Existe");
			JSONObject registro= new JSONObject();
			registro.put("databases", new JSONArray());
			createFile(baseDir+databaseFileName,registro+"");
		}
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
			createDirectory(baseDir+"/"+name);
			//crear el archivo master.json
			JSONObject master = new JSONObject();
			master.put("tables", new JSONArray());
			createFile(baseDir+"/"+name+"/master.json",master+"");
		}
		else{
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
		boolean success = (new File(baseDir+dir)).mkdirs();
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
					file.close();
					file.flush();
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
		File path = new File(baseDir+dir);
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
						newO.put("length", (Integer)actual.get("length"));
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
	public void changeDirectoryName(String name,String newName){
		File dir=new File(baseDir+name);
		File newdir= new File(dir.getParent()+newName);
		dir.renameTo(newdir);
	}
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
