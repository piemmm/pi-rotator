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

/**
 * Main Rotator logic class
 *
 */
public class Rotator {

   private Log           LOG            = LogFactory.getLog("Rotator");

   private double        maxDelta       = 2;

   private RotateRequest currentRequest;
   private RotateThread  rotateThread;

   private float         azimuth        = 0;
   private float         elevation      = 0;

   private double        maxElevation   = 180d;
   private double        maxAzimuth     = 450d;

   private double        maxADCValue    = 900d;

   private boolean       parkingEnabled = false;
   private double        parkElevation  = -1;
   private double        parkAzimuth    = -1;
   private int           parkTimeout    = -1;
   private long          lastRequest    = 0;

   private EWMAFilter    aEmF           = new EWMAFilter(0.04f);
   private EWMAFilter    eEmF           = new EWMAFilter(0.04f);

   public Rotator(HierarchicalConfiguration config) {

      // ADC conversion and rotator setup
      maxElevation = config.getDouble("maxElevationDegrees", 180d);
      maxAzimuth = config.getDouble("maxAzimuthDegrees", 450d);
      maxADCValue = config.getDouble("maxADCReading", 900d);
      maxDelta = config.getDouble("maxDelta", 2d);

      // Parking setup
      HierarchicalConfiguration parkingConfig = config.configurationAt("parking");
      parkingEnabled = parkingConfig.getBoolean("enabled", false);
      parkElevation = parkingConfig.getDouble("elevation", -1);
      parkAzimuth = parkingConfig.getDouble("azimuth", -1);
      parkTimeout = parkingConfig.getInteger("inactivityTimeoutSeconds", -1);

      // Init the controller
      init();
   }

   public void init() {
      LOG.info("Rotator hardware controller starting");
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

         // Initial check if parking required.
         checkForParking();
         while (true) {
            try {
                  Thread.sleep(150);
            } catch (InterruptedException e) {
            }

            if (currentRequest == null) {
               checkForParking();
            } else if (currentRequest.isExpired()) {
               currentRequest = null;
               lastRequest = System.currentTimeMillis();
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

         if (azimuth - reqAz > maxDelta) {
            azCCW.high();
            azCW.low();
         } else if (azimuth - reqAz < -maxDelta) {
            azCCW.low();
            azCW.high();
         } else {
            azCCW.low();
            azCW.low();
         }

         if (elevation - reqEl > maxDelta) {
            elCCW.low();
            elCW.high();
         } else if (elevation - reqEl < -maxDelta) {
            elCCW.high();
            elCW.low();
         } else {
            elCCW.low();
            elCW.low();
         }

         PiRotator.INSTANCE.getStatus().setMoving(azCCW.isHigh() || azCW.isHigh() || elCCW.isHigh() || elCW.isHigh());

      }

      public void stopRotating() {
         azCCW.low();
         azCW.low();
         elCCW.low();
         elCW.low();
         PiRotator.INSTANCE.getStatus().setMoving(false);
      }

   }

   /**
    * Check to see if we need to park - if so then we send a park request.
    */
   public void checkForParking() {
      if (parkingEnabled && System.currentTimeMillis() > lastRequest + (parkTimeout * 1000)) {
         RotateRequest parkEvent = new RotateRequest(parkElevation, parkAzimuth, "<Parking>");
         ServerBus.INSTANCE.post(parkEvent);
      }
   }

   public double getAzimuth() {
      return azimuth;
   }

   public double getElevation() {
      return elevation;
   }

}