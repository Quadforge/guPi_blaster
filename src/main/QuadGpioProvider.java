package main;

import com.pi4j.gpio.extension.ads.ADS1015GpioProvider;
import com.pi4j.io.gpio.GpioProvider;
import com.pi4j.io.i2c.I2CBus;
import com.pi4j.io.i2c.I2CFactory.UnsupportedBusNumberException;
import com.pi4j.io.gpio.Pin;
import java.io.IOException;

public class QuadGpioProvider extends ADS1015GpioProvider{
    
    public QuadGpioProvider(int busNumber, int address) throws UnsupportedBusNumberException, IOException{
        super(busNumber, address);
    }
    
    public QuadGpioProvider(I2CBus bus, int address) throws IOException{
        super(bus, address);
    }
    
    @Override 
    public double getImmediateValue(Pin pin) throws IOException{
        // Start with default values
        int config = ADS1x15_REG_CONFIG_CQUE_NONE    | // Disable the comparator (default val)
                     ADS1x15_REG_CONFIG_CLAT_NONLAT  | // Non-latching (default val)
                     ADS1x15_REG_CONFIG_CPOL_ACTVLOW | // Alert/Rdy active low   (default val)
                     ADS1x15_REG_CONFIG_CMODE_TRAD   | // Traditional comparator (default val)
                     ADS1x15_REG_CONFIG_DR_1600SPS   | // 1600 samples per second (default)
                     ADS1x15_REG_CONFIG_MODE_SINGLE;   // Single-shot mode (default)

        // Set PGA/voltage range
        config |= pga[pin.getAddress()].getConfigValue();  // +/- 6.144V range (limited to VDD +0.3V max!)

        // Set single-ended input channel
        switch (pin.getAddress())
        {
          case (0):
            config |= ADS1x15_REG_CONFIG_MUX_DIFF_0_1;
            break;
          case (1):
            config |= ADS1x15_REG_CONFIG_MUX_DIFF_0_3;
            break;
          case (2):
            config |= ADS1x15_REG_CONFIG_MUX_DIFF_1_3;
            break;
          case (3):
            config |= ADS1x15_REG_CONFIG_MUX_DIFF_2_3;
            break;
        }

        // Set 'start single-conversion' bit
        config |= ADS1x15_REG_CONFIG_OS_SINGLE;

        // Write config register to the ADC
        writeRegister(ADS1x15_REG_POINTER_CONFIG, config);

        // Wait for the conversion to complete
        try{
            if(conversionDelay > 0){
                Thread.sleep(conversionDelay);
            }
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }

        // read the conversion results
        int value = readRegister(ADS1x15_REG_POINTER_CONVERT);

        getPinCache(pin).setAnalogValue(value);
        return value;
    }
    
    public int readRegisterConfigReg() throws IOException{
        try{
            return super.readRegister(0x01);
        }
        catch(IOException e){
            System.out.println(e);
            return 0;
        }
    }
    
    
    
}