package org.prowl.pirotator.hardware.blower;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.prowl.pirotator.hardware.Hardware;

import com.pi4j.system.SystemInfo;

public class Blower {

   private Log    LOG          = LogFactory.getLog("Blower");

   private float  MAX_CPU_TEMP = 75f;

   private Thread thermalMonitor;

   public Blower() {

   }
 
   /**
    * Simple fan controller for making sure the pi cpu doesn't get too close to
    * it's thermal limits.
    */
   public synchronized void makeThermalMonitor() {

      if (thermalMonitor != null) {
         return;
      }

      thermalMonitor = new Thread() {
         public void run() {
            LOG.info("Thermal monitor starting");
            while (true) {
               try {
                  Thread.sleep(10000);
               } catch (InterruptedException e) {
               }
               try {
                  float currentTemp = SystemInfo.getCpuTemperature();
                  LOG.debug("CPU thermals:" + currentTemp);
                  if (currentTemp > MAX_CPU_TEMP) {
                     Hardware.INSTANCE.getFan().high();
                  } else if (currentTemp < MAX_CPU_TEMP - 5) {
                     Hardware.INSTANCE.getFan().low();
                  }
               } catch (UnsupportedOperationException e) {
                  LOG.warn("CPU Does not support temperature measurement");
                  break;
               } catch (InterruptedException e) {
               } catch (Throwable e) {
                  e.printStackTrace();
               }
            }
            LOG.info("Thermal monitor exiting");
         }
      };

      thermalMonitor.start();

   }

}
