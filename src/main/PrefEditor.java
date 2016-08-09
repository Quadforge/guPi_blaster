package main;
import javax.swing.JFrame;
import javax.swing.JButton;
import java.awt.Desktop;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;

public class PrefEditor extends JFrame{

	private GUI parent;
	private String prefsFileLoc;
	private JButton openFile;
	private JButton apply;
	
	public PrefEditor(String floc)
	{
		super("Edit Preferences");
		
		prefsFileLoc = floc;
		
		openFile = new JButton("Open Prefs File");
		openFile.addActionListener(e -> openPrefsFile(prefsFileLoc));
		
		apply = new JButton("Apply");
		apply.addActionListener(e -> applyPrefs());
		
		setLayout(new GridBagLayout());
		
		GridBagConstraints c = new GridBagConstraints();
		
		add(openFile, c);
		
		c.gridx = 1;
		add(apply, c);
	}
	
	public void openPrefsFile(String path)
	{
		File file = new File(path);
		try
		{
			Desktop.getDesktop().edit(file);
		}
		catch (IOException e)
		{
			System.out.println(e);
		}
	}
	
	public void applyPrefs()
	{
		parent.readPrefs(prefsFileLoc);
	}
	
	public void setParent(GUI p)
	{
		parent = p;
	}
}
