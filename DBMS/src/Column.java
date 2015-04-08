import org.json.simple.JSONArray;


public class Column{
	private String type;
	private int length;
	private JSONArray entries;
	
	public Column(String type, int length, JSONArray entries) {
		super();
		this.type = type;
		this.length = length;
		this.entries = entries;
	}

	@Override
	public String toString() {
		return "{\"type\":" + type + ", \"length\":" + length + ", \"entries\":"
				+ entries + "}";
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public int getLength() {
		return length;
	}

	public void setLength(int length) {
		this.length = length;
	}

	public JSONArray getEntries() {
		return entries;
	}

	public void setEntries(JSONArray entries) {
		this.entries = entries;
	}
	
	
	
	
	
}
