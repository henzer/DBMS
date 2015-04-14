
public class Criterion {
	private String column;
	private int order;
	private String type;
	
	public Criterion(String column, int order, String type) {
		super();
		this.column = column;
		this.order = order;
		this.type = type;
	}
	
	public String getColumn() {
		return column;
	}
	public void setColumn(String column) {
		this.column = column;
	}
	public int getOrder() {
		return order;
	}
	public void setOrder(int order) {
		this.order = order;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}
	
	
}
