import java.util.ArrayList;

import org.json.simple.JSONObject;
public class Tipo{
	private String tipo;
	private int length;
	private String mensaje;
	private JSONObject relacion;
	
	private ArrayList<String> resultado;
	
	public Tipo(String tipo) {
		super();
		this.tipo = tipo;
	}
	
	public Tipo(String tipo, String mensaje) {
		super();
		this.tipo = tipo;
		this.mensaje = mensaje;
	}
	public Tipo(String tipo,ArrayList<String>resultado) {
		super();
		this.tipo = tipo;
		this.resultado=resultado;
	}
	public String getTipo() {
		return tipo;
	}
	public void setTipo(String tipo) {
		this.tipo = tipo;
	}
	public int getLength() {
		return length;
	}
	public void setLength(int length) {
		this.length = length;
	}
	
	public String getMensaje() {
		return mensaje;
	}

	public void setMensaje(String mensaje) {
		this.mensaje = mensaje;
	}

	public Tipo(String tipo, int length) {
		super();
		this.tipo = tipo;
		this.length = length;
	}
	@Override
	public String toString() {
		return "[" + tipo + ":" + length + "]";
	}
	public boolean isError(){
		return (tipo.equals("error"));
	}
	public void setResultado(ArrayList<String> resultado){
		this.resultado=resultado;
	}
	public void addResultado(ArrayList<String> resultado){
		this.resultado.addAll(resultado);
	}
	public ArrayList<String> getResultado(){
		return resultado;
	}

	public JSONObject getRelacion() {
		return relacion;
	}

	public void setRelacion(JSONObject relacion) {
		this.relacion = relacion;
	}
	
	
	
}
