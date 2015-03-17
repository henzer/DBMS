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

import java.awt.Component;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import javax.swing.SwingConstants;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;


public class IDE extends JFrame {

	private JPanel contentPane;
	public IDE() {
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
		
		JScrollPane scrollPane_1 = new JScrollPane();
		tabbedPane.addTab("DML", null, scrollPane_1, null);
		
		JTextArea txtDML = new JTextArea();
		scrollPane_1.setViewportView(txtDML);
		
		
		JScrollPane scrollPane_2 = new JScrollPane();
		scrollPane_2.setPreferredSize(new Dimension(2, 150));
		contentPane.add(scrollPane_2, BorderLayout.SOUTH);
		
		JTextArea txtConsola = new JTextArea();
		scrollPane_2.setViewportView(txtConsola);

		JMenuBar menuBar = new JMenuBar();
		scrollPane_2.setColumnHeaderView(menuBar);
		
		JButton btnEjecutar = new JButton("Ejecutar");
		btnEjecutar.setIcon(new ImageIcon(IDE.class.getResource("/com/sun/javafx/webkit/prism/resources/mediaPlayDisabled.png")));
		menuBar.add(btnEjecutar);
		
		JButton btnAbrir = new JButton("Abrir");
		btnAbrir.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if(tabbedPane.getSelectedIndex()==0){
					txtDDL.setText(abrirArchivo());
				}else{
					txtDML.setText(abrirArchivo());
				}
			}
		});
		
		
		btnAbrir.setIcon(new ImageIcon(IDE.class.getResource("/com/sun/java/swing/plaf/windows/icons/NewFolder.gif")));
		menuBar.add(btnAbrir);
		
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
		
		JTree treeBD = new JTree();
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
	
}
