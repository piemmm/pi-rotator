package org.prowl.pirotator.hardware.adc;

import java.io.IOException;
import java.util.concurrent.Semaphore;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.prowl.pirotator.hardware.Hardware;
import org.prowl.pirotator.utils.EWMAFilter;

import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.spi.SpiDevice;

/**
 * Read values from an MCP3008 ADC
 */
public class MCP3008 {

   private Log                  LOG     = LogFactory.getLog("MCP3008");

   private SpiDevice            spi     = Hardware.INSTANCE.getSPI();
   private Semaphore            spiLock = Hardware.INSTANCE.getSPILock();

   private GpioPinDigitalOutput gpioSS;

   public MCP3008() {
      init();
   }

   public void init() {
      gpioSS = Hardware.INSTANCE.getGpioSS0();
   }

   /**
    * Enable the CS for the device
    */
   private void enableSS() {
      gpioSS.high();
   }

   /**
    * Disable the CS for the device
    */
   private void disableSS() {
      gpioSS.low();
   }

   public float readADCChannel(int channel) {
      float value = (float) (450d / 900d) * ((((readRegister((8 + channel) << 4)[1]) & 0xFF) << 8) + ((readRegister((8 + channel) << 4)[2]) & 0xFF));
      return value;
   }

   /**
    * Read a register from the device
    * 
    * @param addr
    * @return
    */
   private byte[] readRegister(int addr) {
      byte[] result = null;
      try {
         spiLock.acquire();
         byte spibuf[] = new byte[3];
         spibuf[0] = 1;
         spibuf[1] = (byte) (addr);
         spibuf[2] = 0x00;
         disableSS();
         try {
            result = spi.write(spibuf);
         } catch (IOException e) {
            LOG.error(e.getMessage(), e);
         }
         enableSS();
         spiLock.release();
      } catch (InterruptedException e) {
      }
      return result;
   }

}