package org.prowl.pirotator.hardware.adc;

import java.io.IOException;
import java.util.concurrent.Semaphore;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.prowl.pirotator.utils.Hardware;

import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.spi.SpiDevice;

public class MCP3008 {

   private Log                  LOG     = LogFactory.getLog("MCP3008");

   public SpiDevice             spi     = Hardware.INSTANCE.getSPI();
   private Semaphore            spiLock = Hardware.INSTANCE.getSPILock();

   private GpioPinDigitalOutput gpioSS;

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
                Thread.sleep(1000);
             } catch (InterruptedException e) {
             }
             
             
             System.out.println("Reg:" + readRegister(0)+"  "+readRegister(1)+"  "+readRegister(2)+"  "+readRegister(3));
             
//             gpioElCW.high();
//             gpioElCCW.high();
//             gpioAzCW.high();
//             gpioAzCCW.high();
//             try {
//                Thread.sleep(1000);
//             } catch (InterruptedException e) {
//             }
//             gpioElCW.low();
//             gpioElCCW.low();
//             gpioAzCW.low();
//             gpioAzCCW.low();
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
   private int readRegister(int addr) {
      int res = 0x00;
      try {
         spiLock.acquire();
         byte spibuf[] = new byte[2];
         spibuf[0] = (byte) (addr);
         spibuf[1] = 0x00;
         disableSS();
         try {
            byte[] result = spi.write(spibuf);
            res = result[1] & 0xFF;
         } catch (IOException e) {
            LOG.error(e.getMessage(), e);
         }
         enableSS();
         spiLock.release();
      } catch (InterruptedException e) {
      }
      return res;
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

}