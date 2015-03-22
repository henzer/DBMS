import java.io.File;
import java.io.IOException;
import java.util.Scanner;

import javax.swing.UIManager;


public class Main {
	public static void main(String args[]) throws IOException{
		//
		File currentDirectory = new File(new File(".").getAbsolutePath());
		System.out.println(currentDirectory.getCanonicalPath());
		System.out.println(currentDirectory.getAbsolutePath());
		Scanner scanner = new Scanner(System.in);
		System.out.println("Enter the path of directory to rename.");
		String dirPath = scanner.nextLine();
		File dir = new File(dirPath);
		if (!dir.isDirectory()) {
		System.err.println("There is no directory @ given path");
		System.exit(0);
		}
		 
		System.out
		.println("Enter new name of directory(Only Name and Not Path).");
		String newDirName = scanner.nextLine();
		 
		File newDir = new File(dir.getParent() + "/" + newDirName);
		dir.renameTo(newDir);
		 
		System.out.println("Done");
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