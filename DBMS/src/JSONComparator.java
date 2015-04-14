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
			if(c.getType().equals("INT")){
				int A = Integer.parseInt(a.get(c.getColumn()).toString());
				int B = Integer.parseInt(b.get(c.getColumn()).toString());
				if(A>B){
					return 1*c.getOrder();
				}else if(B>A){
					return -1*c.getOrder();
				}
			}else if(c.getType().equals("FLOAT")){
				float A = Float.parseFloat(a.get(c.getColumn()).toString());
				float B = Float.parseFloat(b.get(c.getColumn()).toString());
				if(A>B){
					return 1*c.getOrder();
				}else if(B>A){
					return -1*c.getOrder();
				}
			}else{
				if(valA.compareTo(valB)>0){
					return 1*c.getOrder();
				}else if(valA.compareTo(valB)<0){
					return (-1)*c.getOrder();
				}
			}
		}
		return 0;
	}

}
