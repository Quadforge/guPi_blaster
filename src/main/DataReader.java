package main;
import java.io.IOException;
import java.text.DecimalFormat;
import com.pi4j.gpio.extension.ads.ADS1015GpioProvider;
import com.pi4j.gpio.extension.ads.ADS1015Pin;
import com.pi4j.gpio.extension.ads.ADS1x15GpioProvider.ProgrammableGainAmplifierValue;
import com.pi4j.io.gpio.GpioProvider;
import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinAnalogInput;
import com.pi4j.io.gpio.event.GpioPinAnalogValueChangeEvent;
import com.pi4j.io.gpio.event.GpioPinListenerAnalog;
import com.pi4j.io.i2c.I2CBus;
import com.pi4j.io.i2c.I2CFactory.UnsupportedBusNumberException;
import com.pi4j.wiringpi.Spi;

public class DataReader{
    
    
    static final DecimalFormat df = new DecimalFormat("#.##");
    static final DecimalFormat pdf = new DecimalFormat("###.#");
    
    private FIFOWriter writer;
    private GUI parent;
    private GpioController gpio;
    private QuadGpioProvider gpioProviderVoltAmp;
    private QuadGpioProvider gpioProviderForce;
    private GpioPinAnalogInput[] myInputs;
    private MAX31855 thermocouple;
    private int[] rawTemperatureData = new int[2];
    
    public DataReader(GUI p)
    {
        
        setParent(p);
        
        writer = parent.getFIFOWriter();
        
        writer.writeToLogOnly("<---ADS1015 Reader Started--->", parent.getLogPath());
        
        gpio = GpioFactory.getInstance();
        
        thermocouple = new MAX31855(Spi.CHANNEL_0);
        
        int fd = Spi.wiringPiSPISetup(0, 10000000);
        
        try
        {
            gpioProviderVoltAmp = new QuadGpioProvider(I2CBus.BUS_1, ADS1015GpioProvider.ADS1015_ADDRESS_0x48);
        }
        catch (IOException e)
        {
            System.out.println(e);
        }
        catch(UnsupportedBusNumberException e){
            System.out.println(e);
        }
        
        try
        {
            gpioProviderForce = new QuadGpioProvider(I2CBus.BUS_1, ADS1015GpioProvider.ADS1015_ADDRESS_0x49);
        }
        catch (IOException e)
        {
            System.out.println(e);
        }
        catch(UnsupportedBusNumberException e){
            System.out.println(e);
        }
        
        myInputs = new GpioPinAnalogInput[]{
                gpio.provisionAnalogInputPin(gpioProviderVoltAmp, ADS1015Pin.INPUT_A0, "Voltage"),
                //gpio.provisionAnalogInputPin(gpioProvider, ADS1015Pin.INPUT_A1, "Current"),
                //gpio.provisionAnalogInputPin(gpioProvider, ADS1015Pin.INPUT_A2, "MyAnalogInput-A2"),
                gpio.provisionAnalogInputPin(gpioProviderVoltAmp, ADS1015Pin.INPUT_A3, "Current"),
                gpio.provisionAnalogInputPin(gpioProviderForce, ADS1015Pin.INPUT_A0, "Force"),
            };
            
        gpioProviderVoltAmp.setProgrammableGainAmplifier(ProgrammableGainAmplifierValue.PGA_4_096V, ADS1015Pin.INPUT_A0);
        
        gpioProviderForce.setProgrammableGainAmplifier(ProgrammableGainAmplifierValue.PGA_4_096V, ADS1015Pin.INPUT_A0);
        
        gpioProviderVoltAmp.setEventThreshold(500, ADS1015Pin.ALL);
        
        gpioProviderForce.setEventThreshold(500, ADS1015Pin.ALL);
        
        gpioProviderVoltAmp.setMonitorInterval(100);
        
        gpioProviderForce.setMonitorInterval(100);
        
        GpioPinListenerAnalog listener = new GpioPinListenerAnalog()
        {
            @Override
            public void handleGpioPinAnalogValueChangeEvent(GpioPinAnalogValueChangeEvent event)
            {
                
                // RAW value
                /*double value = event.getValue();

                // percentage
                double percent =  ((value * 100) / ADS1015GpioProvider.ADS1015_RANGE_MAX_VALUE);
                
                // approximate voltage ( *scaled based on PGA setting )
                double voltage = gpioProvider.getProgrammableGainAmplifier(event.getPin()).getVoltage() * (percent/100);*/

                // display output
                writer.logAll(outputsToLog(), parent.getLogPath());
                
                //thermocouple.readRaw(rawTemperatureData);
                //if(thermocouple.readRaw(rawTemperatureData) == 0){
                //    System.out.println("No faults");
                //}
               //System.out.println("Temp: " + thermocouple.getThermocoupleTemperature(rawTemperatureData[1]));
                //System.out.println("Internal Temp " + thermocouple.getThermocoupleTemperature(rawTemperatureData[0]));
                
                //writer.writeToLogOnly("\r (" + event.getPin().getName() +") : VOLTS=" + df.format(voltage) + "  | PERCENT=" + pdf.format(percent) + "% | RAW=" + value + "       ", parent.logPath);
            }
        };
        
        myInputs[0].addListener(listener);
        myInputs[1].addListener(listener);
        myInputs[2].addListener(listener);
        //myInputs[3].addListener(listener);
    }
    
    public String outputsToLog()
    {
        String retStr = "";
        String guiStr = "";
        double power = 0.0;
        for(GpioPinAnalogInput pin:myInputs)
        {
            
            double value = pin.getValue();
            double percent = ((value * 100) / ADS1015GpioProvider.ADS1015_RANGE_MAX_VALUE);
            double voltage = gpioProviderVoltAmp.getProgrammableGainAmplifier(pin).getVoltage() * (percent/100);
            if(pin.getName() == "Voltage")
            {
                guiStr += "Voltage: " + df.format(voltage) + "V\r\n";
                retStr += df.format(voltage) + ",";
                power = voltage;
            }
            if(pin.getName() == "Current")
            {
                guiStr += "Current: " + df.format(voltToCurrent(voltage)) + "A\r\n";
                power *= voltToCurrent(voltage);
                guiStr += "Power: " + df.format(power) + "W\r\n";
                retStr += df.format(voltToCurrent(voltage)) + "," + df.format(power) + ",";
            }
            if(pin.getName() == "Force")
            {
                guiStr += "Force: " + df.format(voltToForce(voltage)) + "N\r\n";
                retStr += df.format(voltToForce(voltage)) + ",";
                double lift = voltToForce(voltage) * 101.97;
                guiStr += "Lift: " + df.format(lift) + "g\r\n";
                retStr += df.format(lift) + ",";
            }
            //retStr += "(" + pin.getName() +") : VOLTS=" + df.format(voltage) + "  | PERCENT=" + pdf.format(percent) + "% | RAW=" + value + ",";
            System.out.println("Pin read and added.");
        }
        thermocouple.readRaw(rawTemperatureData);
        guiStr += "Temperature: " + thermocouple.getThermocoupleTemperature(rawTemperatureData[1]) + "C";
        parent.setGUIData(guiStr);
        retStr += thermocouple.getThermocoupleTemperature(rawTemperatureData[1]) + ",";
        return retStr;
    }
    
    public double voltToCurrent(double voltage)
    {
        return (voltage - 0.5) * (1.0 / 0.133);
    }
    
    public double voltToForce(double voltage)
    {
        System.out.println("Force Raw: " + voltage);
        // NEED TO UNSWITCH WIRES TO REMOVE NEGATIVE
        return (-6.125 * ( -voltage)) + 12.25;
    }
    
    public void shutdown()
    {
        gpio.shutdown();
    }
    
    private void setParent(GUI p)
    {
        parent = p;
    }
}