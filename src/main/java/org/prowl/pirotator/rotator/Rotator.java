package org.prowl.pirotator.rotator;

import org.prowl.pirotator.PiRotator;
import org.prowl.pirotator.eventbus.ServerBus;
import org.prowl.pirotator.eventbus.events.RotateRequest;
import org.prowl.pirotator.hardware.Hardware;
import org.prowl.pirotator.hardware.adc.MCP3008;

import com.google.common.eventbus.Subscribe;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.wiringpi.Gpio;

/**
 * Main Rotator logic class
 *
 */
public enum Rotator {

   INSTANCE;

   private static final double MAX_OFFSET = 1.7;

   private RotateRequest       currentRequest;
   private RotateThread        rotateThread;

   private Rotator() {
      init();
   }

   public void init() {
      rotateThread = new RotateThread();
      rotateThread.start();
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
         double azimuth = mcp.getAzimuth();
         double elevation = mcp.getElevation();
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

}