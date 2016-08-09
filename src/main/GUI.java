package main;
import javax.swing.JFrame;
import javax.swing.JButton;
import javax.swing.JTextField;
import javax.swing.JPanel;	
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JTextArea;
import java.text.DecimalFormat;
import java.awt.Insets;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowAdapter;
import java.awt.Desktop;
import java.awt.Color;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.File;
import java.util.ArrayList;

public class GUI extends JFrame {
	
	static final int PIN = 0;
	static final String PATH = "/dev/servoblaster";
	static final String PREF_PATH = "prefs";
	static final DecimalFormat msDecForm = new DecimalFormat("0.0");
	
	//private JTextField pathName;
	private String logPath;
	private JTextField width;
	private JTextArea data;
	private FileReader prefReader;
	private BufferedReader buffPrefReader;
	private JFileChooser fc;
	private JButton openFile;
	private JButton plus;
	private JButton minus;
	private JButton powerToggle;
	private JButton startAuto;
	private JMenuBar menuBar;
	private JMenu fileMenu;
	private JMenuItem editPrefs;
	private FIFOWriter fifoWriter;
	private DataReader dataReader;
	private PrefEditor prefEditor;
	private ArrayList<String> prefs;
	private Color cgreen;
	private Color cred;
	private int r = 0;
    private int g = 253;
	//private JPanel 
	
	public GUI()
	{
		super("Test Bench GUI");
		
		prefs = new ArrayList<String>();
		
			//to make the colors 
		cgreen = new Color(0,253,0);
		cred = new Color(253,0,0);
		
		/*** MINUS BUTTON CREATION ***/
		minus = new JButton("-");
		minus.addActionListener(e -> lessPWM());
		minus.setEnabled(false);
		
		/*** PLUS BUTTON CREATION ***/
		plus = new JButton("+");
		plus.addActionListener(e-> morePWM());
		plus.setEnabled(false);
		
		/*** POWER BUTTON CREATION ***/
		powerToggle = new JButton("ON");
		powerToggle.setBackground(cgreen);
		powerToggle.addActionListener(e -> togglePower());
		
		/*** AUTO BUTTON CREATION ***/
		startAuto = new JButton("Start Auto Mode");
		startAuto.addActionListener(e -> startAutoCycle());
		startAuto.setEnabled(false);
		startAuto.setBackground(new Color(r,g,0));
		
		width = new JTextField();
		width.setEditable(false);
		//width.setText("1.0 ms (OFF)");
		setPWText(1.0, false, false);
		width.setHorizontalAlignment(SwingConstants.CENTER);
		
		logPath = "";
		
		fc = new JFileChooser();
		
		/*** OPEN FILE BUTTON CREATION ***/
		openFile = new JButton("Choose File");
		openFile.addActionListener(e -> startFileOpener());
		
		/*pathName = new JTextField();
		pathName.setEditable(false);
		pathName.setHorizontalAlignment(SwingConstants.CENTER);*/
		
		menuBar = new JMenuBar();
		fileMenu = new JMenu("File");
		menuBar.add(fileMenu);
		editPrefs = new JMenuItem("Edit Prefs");
		editPrefs.addActionListener(e -> {
			prefEditor = new PrefEditor(PREF_PATH);
			setEditorParent(prefEditor);
			prefEditor.setBounds(200, 200, 300, 300);
			prefEditor.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
			prefEditor.setVisible(true);
		});
		fileMenu.add(editPrefs);
		
		readPrefs(PREF_PATH);
		
		boolean isLogFile = logPath != "";
		while (!isLogFile)
		{
			try
			{
				JOptionPane.showMessageDialog(this, "Please select the log file directory.");
				startFileOpener();
				isLogFile = true;
			}
			catch(NullPointerException e)
			{
				System.out.println(e);
			}
		}
		
		fifoWriter = new FIFOWriter(this);
		//fifoWriter.setParent(this);
		fifoWriter.saveValue(PIN, 1.0);
		fifoWriter.addValue(PIN, 0.0);
		
		//System.out.println(logPath);
		dataReader = new DataReader(this);
		
		data = new JTextArea();
		data.setEditable(false);
		
		fifoWriter.initLogFile(logPath);
		fifoWriter.writeToFile(PATH, logPath, dataReader.outputsToLog());
		
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e)
			{
				exitProcedure();
				System.exit(0);
			}
		});
		
		setJMenuBar(menuBar);
		
		setLayout(new GridBagLayout());
		
		GridBagConstraints c = new GridBagConstraints();
		
		Insets inset = new Insets(1, 1, 1, 1);
		
		c.fill = GridBagConstraints.BOTH;
		
		c.weightx = 0.1;
		c.weighty = 1.0;
		c.insets = inset;
		
		c.gridx = 2;
		add(plus, c);
		
		c.weightx = 1.0;
		c.gridx = 1;
		add(width, c);
		
		c.weightx = 0.1;
		c.gridx = 0;
		add(minus, c);
		
		c.gridy = 1;
		add(startAuto, c);
		
		c.gridx = 1;
		add(data, c);
		//add(openFile, c);
		
		c.gridx = 2;
		//add(pathName, c);
		add(openFile, c);
		
		c.gridx = 0;
		c.gridy = 2;
		c.gridwidth = 3;
		add(powerToggle, c);
	}
	
	public void lessPWM()
	{
		if (fifoWriter.getValue(PIN) >= 1.09)
		{
			fifoWriter.addValue(PIN, (double)(fifoWriter.getValue(PIN) - 0.1));	
			fifoWriter.writeToFile(PATH, logPath, dataReader.outputsToLog());							
			//width.setText("" + valueToString(fifoWriter.getValue(PIN)) + " ms");
			setPWText(fifoWriter.getValue(PIN), true, false);
			
		}
		ensureBounds();
	}
	
	public void morePWM()
	{
		if ((int)(fifoWriter.getValue(PIN) * 10) / 10.0 <= 1.9)
		{
			fifoWriter.addValue(PIN, (double)(fifoWriter.getValue(PIN) + 0.1));	
			fifoWriter.writeToFile(PATH, logPath, dataReader.outputsToLog());							
			//width.setText("" + valueToString(fifoWriter.getValue(PIN)) + " ms");
			setPWText(fifoWriter.getValue(PIN), true, false);
		}
		ensureBounds();
	}
	
	public void togglePower()
	{
		fifoWriter.autoCycleStop();
		
	
		
		//System.out.println("Pin on? " + fifoWriter.pinOn(PIN));
		
		if (fifoWriter.pinOn(PIN))
		{
			powerToggle.setText("ON");
			powerToggle.setBackground(cgreen);
			fifoWriter.saveValue(PIN, 1.0);
			//width.setText("1.0 ms");
			setPWText(1.0, false, false);
			fifoWriter.addValue(PIN, 0.0);
			fifoWriter.writeToFile(PATH, logPath, dataReader.outputsToLog());
			//width.setText(width.getText() + " (OFF)");
			plus.setEnabled(false);
			minus.setEnabled(false);
			startAuto.setEnabled(false);
			r = 0;
			g = 253;
			startAuto.setBackground(new Color(r,g,0));
		}
		else
		{
			powerToggle.setText("OFF");
			powerToggle.setBackground(cred); 
			fifoWriter.addValue(PIN, fifoWriter.percentToValue(fifoWriter.getSavedValue().get(1)));
			fifoWriter.writeToFile(PATH, logPath, dataReader.outputsToLog());
			//width.setText("" + valueToString(fifoWriter.getValue(PIN)) + " ms");
			setPWText(fifoWriter.getValue(PIN), true, false);
			plus.setEnabled(true);
			minus.setEnabled(true);
			startAuto.setEnabled(true);
			ensureBounds();
		}
	}
	
	public void startAutoCycle()
	{
		fifoWriter.autoCycle(PIN, PATH);
		plus.setEnabled(false);
		minus.setEnabled(false);
		startAuto.setEnabled(false);
	}
	
	public void enableButtons()
	{
		minus.setEnabled(true);
		plus.setEnabled(true);
		openFile.setEnabled(true);
		startAuto.setEnabled(true);
		ensureBounds();
	}
	
	public void ensureBounds()
	{
		if ((int)(fifoWriter.getValue(PIN) + 0.99) <= 1){
			minus.setEnabled(false);
		}
		else{
			minus.setEnabled(true);
		}
		if ((int)(fifoWriter.getValue(PIN)) >= 2){
			plus.setEnabled(false);
		}
		else{
			plus.setEnabled(true);
		}
	}
	
	public String valueToString(double d)
	{
		return msDecForm.format(d);
	}
	
	public void exitProcedure()
	{
		fifoWriter.addValue(PIN, 0.0);
		fifoWriter.autoCycleStop();
		fifoWriter.writeToFile(PATH, logPath, dataReader.outputsToLog());
		dataReader.shutdown();
	}
	
	public boolean readPrefs(String prefPath)
	{
		try
		{
			prefReader = new FileReader(prefPath);
			buffPrefReader = new BufferedReader(prefReader);
			String s = buffPrefReader.readLine();
			while (s != null)
			{
				s = s.trim();
				//System.out.println(s);
				prefs.add(s);
				s = buffPrefReader.readLine();
			}
			if (prefs.get(0) != null)
			{
				parsePrefs();
				return true;
			}
		}
		catch (FileNotFoundException e)
		{
			System.out.println(e);
			JOptionPane.showMessageDialog(this, "No prefs file found!");
			return false;
		}
		catch (IOException e)
		{
			System.out.println(e);
			return false;
		}
		return false;
	}
	
	private void parsePrefs()
	{
		for(String s:prefs)
		{
			for (int i = 0; i < s.length(); i++)
			{
				if(s.charAt(i) == ':')
				{
					switch(s.substring(0, i))
					{
					case "logpath":	logPath = s.substring(i+1, s.length()).trim();
									openFile.setText(logPath);
									break;
					}
				}
			}
		}
	}
	
	public FIFOWriter getFIFOWriter()
	{
		return fifoWriter;
	}
	
	public void getLogData()
	{
		String data = dataReader.outputsToLog();
	}
	
	public void startFileOpener()
	{
		fc.showOpenDialog(this);
		logPath = fc.getSelectedFile().getAbsolutePath();
		openFile.setText(logPath);
		//fifoWriter.initLogFile(logPath);
	}
	
	public DataReader getDataReader()
	{
		return dataReader;
	}
	
	public void setPWText(double value, boolean on, boolean auto)
	{
		String text = "Pulse Width:\r\n" + valueToString(value) + " ms";
		if(!on)
			text += " (OFF)";
		if(auto)
			text += " (AUTO)";
		width.setText(text);
	}
	
	public String getLogPath()
	{
		return logPath;
	}
	
	public void setEditorParent(PrefEditor e)
	{
		e.setParent(this);
	}
	
	public int getR(){
		return r;
	}
	
	public int getG(){
		return g;
	}
	
	public void setG(int gValue){
		g = gValue;
	}
	
	public void setR(int rValue){
		r = rValue;
	}
	
	public void updateAutoColor(){
		startAuto.setBackground(new Color(r,g,0));
	}
	
	public void setGUIData(String str)
	{
		data.setText(str);
	}
}