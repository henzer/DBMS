
public class Criterion {
	private String column;
	private int order;
	
	
	public Criterion(String column, int order) {
		super();
		this.column = column;
		this.order = order;
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
	
}
