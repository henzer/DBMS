import java.util.HashMap;
public class Table extends HashMap<String, Column>{
	@Override
	public String toString() {
		String texto = "{";
		for(String key: this.keySet()){
			texto += "\"" + key + "\": " + this.get(key);
		}
		texto += "}";
		return texto;
	}
	
}
