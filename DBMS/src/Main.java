import java.io.File;
import java.io.IOException;
import java.util.Scanner;

import javax.swing.UIManager;


public class Main {
	public static void main(String args[]) throws IOException{
		try{
		   UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		   IDE ide = new IDE();
		   ide.setVisible(true);
		}
		catch (Exception e){
		   e.printStackTrace();
		}
		//alliasdfjaksdfasdjfja;sdfj;aldfj;lskdjf;alsdfjaksdjfajsdl;fjk;sdf
		
	}
}