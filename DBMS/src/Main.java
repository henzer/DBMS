import javax.swing.UIManager;


public class Main {
	public static void main(String args[]){
		try
		{
		   UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		   IDE ide = new IDE();
		   ide.setVisible(true);
		}
		catch (Exception e)
		{
		   e.printStackTrace();
		}
	}
}