import java.awt.BorderLayout;
import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.JScrollPane;
import java.awt.FlowLayout;
import javax.swing.JTabbedPane;
import javax.swing.BoxLayout;
import javax.swing.JTextArea;
import java.awt.Dimension;
import javax.swing.JMenuBar;
import javax.swing.JButton;
import javax.swing.ImageIcon;


public class IDE extends JFrame {

	private JPanel contentPane;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					IDE frame = new IDE();
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the frame.
	 */
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
		
		JTextArea textArea = new JTextArea();
		scrollPane.setViewportView(textArea);
		
		JMenuBar menuBar = new JMenuBar();
		scrollPane.setColumnHeaderView(menuBar);
		
		JButton btnEjecutar = new JButton("Ejecutar");
		btnEjecutar.setIcon(new ImageIcon(IDE.class.getResource("/com/sun/javafx/webkit/prism/resources/mediaPlayDisabled.png")));
		menuBar.add(btnEjecutar);
		
		JButton btnAbrir = new JButton("Abrir");
		btnAbrir.setIcon(new ImageIcon(IDE.class.getResource("/com/sun/java/swing/plaf/windows/icons/NewFolder.gif")));
		menuBar.add(btnAbrir);
		
		JScrollPane scrollPane_1 = new JScrollPane();
		tabbedPane.addTab("DML", null, scrollPane_1, null);
		
		JTextArea textArea_1 = new JTextArea();
		scrollPane_1.setViewportView(textArea_1);
		
		
		JScrollPane scrollPane_2 = new JScrollPane();
		scrollPane_2.setPreferredSize(new Dimension(2, 100));
		contentPane.add(scrollPane_2, BorderLayout.SOUTH);
		
		JTextArea textArea_2 = new JTextArea();
		scrollPane_2.setViewportView(textArea_2);
		
	}
}
