import java.awt.BorderLayout;
import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.JScrollPane;

import java.awt.FlowLayout;

import javax.swing.JTabbedPane;
import javax.swing.BoxLayout;
import javax.swing.JTextArea;

import java.awt.Dimension;

import javax.swing.JFileChooser;
import javax.swing.JMenuBar;
import javax.swing.JButton;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JOptionPane;

import java.awt.Font;

import javax.swing.JTree;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeModel;

import java.awt.Component;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import javax.swing.SwingConstants;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.JTable;
import javax.swing.JCheckBox;


public class IDE extends JFrame {

	private JPanel contentPane;
	private JTree treeBD;
	private ControladorDDL controlDDL;
	private JTable table;
	
	public IDE() {
		controlDDL = new ControladorDDL();
		
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 716, 439);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(new BorderLayout(0, 0));
		
		JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP);
		contentPane.add(tabbedPane, BorderLayout.CENTER);
		
		JScrollPane scrollPane = new JScrollPane();
		tabbedPane.addTab("DDL", null, scrollPane, null);
		
		JTextArea txtDDL = new JTextArea();
		txtDDL.setTabSize(4);
		scrollPane.setViewportView(txtDDL);
		
		table = new JTable();

		JScrollPane spnData = new JScrollPane();
		spnData.setViewportView(table);
		tabbedPane.addTab("Data", null, spnData, null);
		
		JScrollPane scrollPane_1 = new JScrollPane();
		tabbedPane.addTab("Otros", null, scrollPane_1, null);
		
		
		JScrollPane scrollPane_2 = new JScrollPane();
		scrollPane_2.setPreferredSize(new Dimension(2, 150));
		contentPane.add(scrollPane_2, BorderLayout.SOUTH);
		
		JTextArea txtConsola = new JTextArea();
		scrollPane_2.setViewportView(txtConsola);

		JMenuBar menuBar = new JMenuBar();
		scrollPane_2.setColumnHeaderView(menuBar);
		
		JButton btnEjecutar = new JButton("Ejecutar");
		btnEjecutar.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if(tabbedPane.getSelectedIndex()==0){
					String mensaje = controlDDL.compilar(toUpperCase(txtDDL.getText()));
					txtConsola.setText(mensaje);
					
					if(controlDDL.isData()){
						table.setModel(controlDDL.getModelo());
						tabbedPane.setSelectedIndex(1);
						
					}
					
				}else{
					tabbedPane.setSelectedIndex(0);
					JOptionPane.showMessageDialog(null, "Debe estar en esta pestaña para poder COMPILAR");
				}
				
				//Se modifica el arbol
				FileNode f = new FileNode(System.getProperty("user.dir")+ "/databases/");
				TreeModel model = new FileTreeModel(f);
				treeBD.setModel(model);
				
			}
		});
		btnEjecutar.setIcon(new ImageIcon(IDE.class.getResource("/com/sun/javafx/webkit/prism/resources/mediaPlayDisabled.png")));
		menuBar.add(btnEjecutar);
		
		JButton btnAbrir = new JButton("Abrir");
		btnAbrir.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				txtDDL.setText(abrirArchivo());
			}
		});
		
		
		btnAbrir.setIcon(new ImageIcon(IDE.class.getResource("/com/sun/java/swing/plaf/windows/icons/NewFolder.gif")));
		menuBar.add(btnAbrir);
		
		JCheckBox chkVerbose = new JCheckBox("Verbose");
		menuBar.add(chkVerbose);
		
		JPanel panel = new JPanel();
		panel.setPreferredSize(new Dimension(150, 10));
		contentPane.add(panel, BorderLayout.EAST);
		panel.setLayout(new BorderLayout(0, 0));
		
		JLabel lblArbol = new JLabel("Bases de Datos");
		lblArbol.setHorizontalAlignment(SwingConstants.CENTER);
		lblArbol.setHorizontalTextPosition(SwingConstants.LEFT);
		lblArbol.setAlignmentX(Component.CENTER_ALIGNMENT);
		lblArbol.setFont(new Font("Tahoma", Font.PLAIN, 13));
		panel.add(lblArbol, BorderLayout.NORTH);
		
		JScrollPane scrollPane_3 = new JScrollPane();
		panel.add(scrollPane_3, BorderLayout.CENTER);
		
		treeBD = new JTree();
		FileNode f = new FileNode(System.getProperty("user.dir")+ "/databases/");
		TreeModel model = new FileTreeModel(f);
		treeBD.setModel(model);
		
		scrollPane_3.setViewportView(treeBD);
	}
	
	public String abrirArchivo() {
		String aux="";   
		String texto="";
		try
		{
			JFileChooser file=new JFileChooser();
			FileNameExtensionFilter filtroImagen=new FileNameExtensionFilter("Archivos de Texto","txt");
		    file.setFileFilter(filtroImagen);
			file.showOpenDialog(null);
			File abre=file.getSelectedFile(); 
			if(abre!=null)
			{     
				FileReader archivos=new FileReader(abre);
				BufferedReader lee=new BufferedReader(archivos);
				while((aux=lee.readLine())!=null)
				{
					texto+= aux+ "\n";
				}
				lee.close();
			}    
		}
		catch(IOException ex)
		{
			JOptionPane.showMessageDialog(null,ex+"" +
					"\nNo se ha encontrado el archivo",
					"ADVERTENCIA!!!",JOptionPane.WARNING_MESSAGE);
		}
		return texto;
		}
		public static String toUpperCase(String input){
			String res="";
			boolean isString=false;
			for(int i=0;i<input.length();i++){
				char actual= input.charAt(i);
				if(actual=='\''){
					isString=!isString;
					res+=actual;
				}
				else if(isString){
					res+=actual;
				}
				else{
					res+=(actual+"").toUpperCase();
				}
				
			}
			return res;
		}
}
