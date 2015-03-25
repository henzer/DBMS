import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;


public class TablaTipos extends HashMap<String, Tipo>{
	
	public TablaTipos(){
	}
	
	public void agregar(String tipo, int tamanio){
		this.put(tipo, new Tipo(tipo, tamanio));
	}
	
	
	public void agregar(Tipo t){
		this.put(t.getTipo(), t);
	}
	
	
	public Tipo getTipo(String tipo){
		if(this.containsKey(tipo)){
			return this.get(tipo);
		}else{
			return null;
		}
		
	}
	
	public boolean existe(String tipo){
		return this.containsKey(tipo);
	}
}
