package org.prowl.pirotator.rotator;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.prowl.pirotator.PiRotator;
import org.prowl.pirotator.eventbus.ServerBus;
import org.prowl.pirotator.eventbus.events.RotateRequest;
import org.prowl.pirotator.hardware.Hardware;
import org.prowl.pirotator.hardware.adc.MCP3008;
import org.prowl.pirotator.utils.EWMAFilter;

import com.google.common.eventbus.Subscribe;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.wiringpi.Gpio;

/**
 * Main Rotator logic class
 *
 */
public class Rotator {

   private Log                 LOG          = LogFactory.getLog("Rotator");

   private static final double MAX_OFFSET   = 1.7;

   private RotateRequest       currentRequest;
   private RotateThread        rotateThread;

   private float               azimuth      = 0;
   private float               elevation    = 0;

   private double              maxElevation = 180d;
   private double              maxAzimuth   = 450d;

   private double              maxADCValue  = 900d;

   private EWMAFilter          aEmF         = new EWMAFilter(0.04f);
   private EWMAFilter          eEmF         = new EWMAFilter(0.04f);

   public Rotator(HierarchicalConfiguration config) {

      maxElevation = config.getDouble("maxElevationDegrees", 180);
      maxAzimuth = config.getDouble("maxAzimuthDegrees", 450);
      maxADCValue = config.getDouble("maxADCReading", 900);
      
      init();
   }

   public void init() {
      rotateThread = new RotateThread();
      rotateThread.start();

      Thread thread = new Thread() {
         public void run() {
            while (true) {
               try {
                  Thread.sleep(10);
               } catch (InterruptedException e) {
               }

               float ele = (float) (maxElevation / maxADCValue) * PiRotator.INSTANCE.getMCP().readADCChannel(1);
               float azi = (float) (maxAzimuth / maxADCValue) * PiRotator.INSTANCE.getMCP().readADCChannel(0);

               float eleF = eEmF.addPoint(ele);
               float aziF = aEmF.addPoint(azi);

               elevation = eleF;
               azimuth = aziF;

            }
         }
      };

      thread.start();

      ServerBus.INSTANCE.register(this);

   }

   @Subscribe
   public void getRequest(RotateRequest request) {
      currentRequest = request;
   }

   /**
    * Get the current tracking sat name, or null for none.
    * 
    * @return
    */
   public String getCurrentTracking() {
      RotateRequest req = currentRequest;
      if (req == null || req.isExpired()) {
         return null;
      }

      return req.getName();
   }

   public class RotateThread extends Thread {

      private GpioPinDigitalOutput azCCW = Hardware.INSTANCE.getGpioAzCCW();
      private GpioPinDigitalOutput azCW  = Hardware.INSTANCE.getGpioAzCW();
      private GpioPinDigitalOutput elCCW = Hardware.INSTANCE.getGpioElCCW();
      private GpioPinDigitalOutput elCW  = Hardware.INSTANCE.getGpioElCW();

      public RotateThread() {
         super("Rotate thread");
      }

      public void run() {

         while (true) {
            try {
               if (currentRequest == null) {
                  Thread.sleep(50);
               } else {
                  Thread.sleep(150);
               }
            } catch (InterruptedException e) {
            }

            if (currentRequest == null) {
               stopRotating();
            } else if (currentRequest.isExpired()) {
               currentRequest = null;
               stopRotating();
            }

            if (currentRequest != null) {
               ensureRotated(currentRequest);
            }

         }

      }

      public void ensureRotated(RotateRequest request) {

         MCP3008 mcp = PiRotator.INSTANCE.getMCP();
         double reqAz = request.getAzimuth();
         double reqEl = request.getElevation();

         if (azimuth - reqAz > MAX_OFFSET) {
            azCCW.high();
            azCW.low();
         } else if (azimuth - reqAz < -MAX_OFFSET) {
            azCCW.low();
            azCW.high();
         } else {
            azCCW.low();
            azCW.low();
         }

         if (elevation - reqEl > MAX_OFFSET) {
            elCCW.low();
            elCW.high();
         } else if (elevation - reqEl < -MAX_OFFSET) {
            elCCW.high();
            elCW.low();
         } else {
            elCCW.low();
            elCW.low();
         }

      }

      public void stopRotating() {
         azCCW.low();
         azCW.low();
         elCCW.low();
         elCW.low();
      }

   }

   public double getAzimuth() {
      return azimuth;
   }

   public double getElevation() {
      return elevation;
   }

}