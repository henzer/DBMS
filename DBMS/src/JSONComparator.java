import java.util.ArrayList;
import java.util.Comparator;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;


public class JSONComparator implements Comparator<JSONObject>{

	private ArrayList<Criterion> criterios;
	public JSONComparator(ArrayList<Criterion> criterios){
		this.criterios = criterios;
	}
	
	
	@Override
	public int compare(JSONObject a, JSONObject b) {
		int size = criterios.size();
		for(Criterion c: criterios){
			String valA = a.get(c.getColumn()).toString();
			String valB = b.get(c.getColumn()).toString();
			if(valA.compareTo(valB)>0){
				return 1*c.getOrder();
			}else if(valA.compareTo(valB)>0){
				return (-1)*c.getOrder();
			}
		}
		return 0;
	}

}
