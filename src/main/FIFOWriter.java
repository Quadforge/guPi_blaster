package main;
import java.io.FileWriter;
import java.io.RandomAccessFile;
import java.io.IOException;
import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Date;

public class FIFOWriter {
    
    private FileWriter writer;
    private ArrayList<Integer> pins;
    private ArrayList<Double> values;
    private ArrayList<Double> savedValue;
    private ArrayList<Double[]> autoCyclePoints;
    private Timer autoTimer;
    private int autoCycleLooper;
    private int autoCyclePin;
    private String autoCyclePath;
    private GUI parent;
    
    
    public DecimalFormat decForm;
    
    public FIFOWriter(GUI p)
    {
        parent = p;
    	decForm = new DecimalFormat("####");
    	pins = new ArrayList<Integer>();
    	values = new ArrayList<Double>();
    	savedValue = new ArrayList<Double>();
    	savedValue.add(0.0);
    	savedValue.add(0.0);
    	autoTimer = new Timer();
    	autoCyclePoints = new ArrayList<Double[]>();
    	
    	for(int i = 0; i <= 10; i ++)
    	{
    		autoCyclePoints.add(new Double[]{1.0+(i / 10.0), (double)i});
    	}
    }
    
    public void addValue(int p, double v)
    {
    	for (int i = 0; i < pins.size(); i++)
    	{
    		if(p == pins.get(i))
    		{
    			values.set(i, valueToPercent(v));
    			return;
    		}
    	}
    	pins.add(p);
    	values.add(valueToPercent(v));
    }
    
    public String getAllValues()
    {
    	String retString = "";
    	for (int i = 0; i < pins.size(); i++)
    	{
    		retString += pins.get(i) + "=" + decForm.format(values.get(i)) + "us";  //"" + pins.get(i) + "=" + decForm.format(values.get(i));
    	}
    	return retString;
    }
    
    public double getValue(int p)
    {
    	for (int i = 0; i < pins.size(); i++)
    	{
    		if (pins.get(i) == p)
    			return percentToValue(values.get(i));
    	}
    	return 0.0f;
    }
    
    public void writeToFile(String path, String logPath, String data)
    {
        File file = new File(path);
        //file.seek(-1);
        File log = new File(logPath);
        try
        {
            writer = new FileWriter(file, true);
            writer.write(getAllValues() + "\n");
            writer.flush();
            writer.close();
        }
        catch (IOException e)
        {
            System.out.println(e);
        }
        
       logAll(data, logPath);
    }
    
    /*public void writeToFile(File file, boolean append)
    {
        try
        {
            writer = new FileWriter(file, append);
            writer.write(getAllValues());
            writer.flush();
            writer.close();
        }
        catch (IOException e)
        {
            System.out.println(e);
        }
    }*/
    
    public boolean pinOn(int p)
    {
    	for (int i = 0; i < pins.size(); i++)
    	{
    		if(pins.get(i) == p)
    		{
    		    //System.out.println("Pin: " + pins.get(i) + " Value: " + values.get(i));
    			return(values.get(i) != 0.0);
    		}
    	}
    	return false;
    }
    
    public double percentToValue(double per) // NO LONGER PECENTAGE, JUST WHAT IT SAYS IN SERVO BLASTER
    {
        /*if (per != 0.0)
    	    return (per - 20) / 880;
    	else
    	    return 0.0;*/
    	//return per;
    	return per / 1000.0;
    }					// THE GUI SHOULD NEVER SEE THE PERCENTAGE, ONLY MILLISECONDS
    
    public double valueToPercent(double val)
    {
        /*if(val != 0.0)
    	    return val * 880 + 20;	// INVERSE OF FUNCTION ABOVE
    	else
    	    return 0.0;*/
    	//return val;
    	return val * 1000.0;
    }
    
    public void saveValue(int pin)
    {
    	savedValue.set(0, (double)pin);
    	savedValue.set(1, values.get(getIndex(pin)));
    }
    
    public void saveValue(int pin, double value)
    {
    	savedValue.set(0, (double)pin);
    	savedValue.set(1, valueToPercent(value));
    }
    
    public ArrayList<Double> getSavedValue()
    {
    	return savedValue;
    }
    
    public int getIndex(int pin)
    {
    	for (int i = 0; i < pins.size(); i ++)
    	{
    		if (pins.get(i) == pin)
    			return i;
    	}
    	throw new IllegalArgumentException("Pin " + pin + " not found.");
    }
    
    public void autoCycle(int p, String path)
    {
    	autoCyclePin = p;
    	autoCyclePath = path;
    	parent.setR(253);
    	parent.setG(0);
    	parent.updateAutoColor();
    	
    	for(autoCycleLooper = 0; autoCycleLooper < autoCyclePoints.size(); autoCycleLooper ++)
    	{
    		System.out.println(autoCycleLooper);
    		autoTimer.schedule(new TimerTask(){
    			int i = autoCycleLooper;
    			public void run()
    			{
    				addValue(autoCyclePin, autoCyclePoints.get(i)[0]);
    				writeToFile(autoCyclePath, parent.getLogPath(), parent.getDataReader().outputsToLog());
    				
    				parent.setR(parent.getR() - 23);
    	            parent.setG(parent.getG() + 23);
    	            parent.updateAutoColor();
    				//System.out.println("Value Added");
    				//parent.width.setText("" + parent.msDecForm.format(autoCyclePoints.get(i)[0]) + " ms (AUTO)");
    				parent.setPWText(autoCyclePoints.get(i)[0], true, true);
    				if (i == autoCyclePoints.size() - 1)
    				{
    					autoCycleStop();
    				}
    			}
    		},
    		(long)(autoCyclePoints.get(autoCycleLooper)[1] * 1000));
    	}
    }
    
    public void initLogFile(String path)
    {
    	File file = new File(path);
    	try
    	{
    		FileWriter writer = new FileWriter(file, true);
    		writer.write("**********LOG FOR SESSION STARTING " + new Date().toString() + "**********\r\n" + "Pulse Width (ms),Voltage (V),Current (A),Power (W),Force (N),Lift (g),Temperature (C),Time\r\n");
    		writer.flush();
    		writer.close();
    	}
    	catch(IOException e)
    	{
    		System.out.println(e);
    	}
    }
    
    public void writeToLogOnly(String toWrite, String logPath)
    {
        File file = new File(logPath);
        try
        {
            FileWriter writer = new FileWriter(file, true);
            writer.write(toWrite + "\r\n");// + getAllValues() + "\r\n");
            writer.flush();
            writer.close();
        }
        catch (IOException e)
        {
            System.out.println(e);
        }
    }
    
    public void logAll(String readerVals, String logPath)
    {
        File file = new File(logPath);
        try
        {
            FileWriter writer = new FileWriter(file, true);
            writer.write(parent.msDecForm.format(getValue(parent.PIN)) + "," + readerVals + new Date().toString() + "\r\n");
            writer.flush();
            writer.close();
        }
        catch (IOException e)
        {
            System.out.println(e);
        }
    }
    
    public void autoCycleStop()
    {
    	autoTimer.cancel();
    	autoTimer = new Timer();
    	//System.out.println("Timer Stopped");
    	//parent.width.setText("" + decForm.format(getValue(parent.PIN)) + " ms");
    	parent.setPWText(getValue(parent.PIN), true, false);
    	parent.enableButtons();
    }
    
}