package org.prowl.pirotator.hardware.adc;

import java.io.IOException;
import java.util.concurrent.Semaphore;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.prowl.pirotator.hardware.Hardware;
import org.prowl.pirotator.utils.EWMAFilter;

import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.spi.SpiDevice;

public class MCP3008 {

   private Log                  LOG       = LogFactory.getLog("MCP3008");

   private SpiDevice            spi       = Hardware.INSTANCE.getSPI();
   private Semaphore            spiLock   = Hardware.INSTANCE.getSPILock();

   private GpioPinDigitalOutput gpioSS;

   private EWMAFilter           aEmF      = new EWMAFilter(0.05f);
   private EWMAFilter           eEmF      = new EWMAFilter(0.05f);

   private double               azimuth   = 0;
   private double               elevation = 0;

   public MCP3008() {
      init();
   }

   public void init() {
      gpioSS = Hardware.INSTANCE.getGpioSS0();

      Thread thread = new Thread() {
         public void run() {
            LOG.info("ADC monitor starting");
            while (true) {
               try {
                  Thread.sleep(10);
               } catch (InterruptedException e) {
               }

               float ele = (float) (180d / 900d) * ((((readRegister(9 << 4)[1]) & 0xFF) << 8) + ((readRegister(9 << 4)[2]) & 0xFF));
               float azi = (float) (450d / 900d) * ((((readRegister(8 << 4)[1]) & 0xFF) << 8) + ((readRegister(8 << 4)[2]) & 0xFF));

               float eleF = eEmF.addPoint(ele);
               float aziF = aEmF.addPoint(azi);

               elevation = eleF;

               azimuth = aziF;

            }
         }
      };

      thread.start();

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
            // res = result[1] & 0xFF;
         } catch (IOException e) {
            LOG.error(e.getMessage(), e);
         }
         enableSS();
         spiLock.release();
      } catch (InterruptedException e) {
      }
      return result;
   }

   /**
    * Write to a register on the device
    * 
    * @param addr
    * @param value
    */
   private void writeRegister(int addr, int value) {
      try {
         spiLock.acquire();
         byte spibuf[] = new byte[2];
         spibuf[0] = (byte) (addr | 0x80); // | For SPI, 0x80 MSB set == write, clear = read.
         spibuf[1] = (byte) value;
         disableSS();
         try {
            spi.write(spibuf);
         } catch (IOException e) {
            LOG.error(e.getMessage(), e);
         }
         enableSS();
         spiLock.release();
      } catch (InterruptedException e) {
      }

   }

   public double getAzimuth() {
      return azimuth;
   }

   public double getElevation() {
      return elevation;
   }

}