package org.prowl.pirotator.hardware.leds;

import java.util.Timer;
import java.util.TimerTask;

import org.prowl.pirotator.eventbus.ServerBus;
import org.prowl.pirotator.eventbus.events.RotateRequest;
import org.prowl.pirotator.hardware.Hardware;

import com.google.common.eventbus.Subscribe;
import com.pi4j.io.gpio.GpioPinDigitalOutput;

/**
 * Simple class to allow the changing of status leds on the front panel
 */
public class StatusLeds {

   private GpioPinDigitalOutput gpioFault;
   private GpioPinDigitalOutput gpioNetwork;
   private GpioPinDigitalOutput gpioMoving;
   private GpioPinDigitalOutput gpioGPS;
   
   private RotateRequest currentRequest;

   public StatusLeds() {
      init();
   }

   public void init() {

      gpioFault = Hardware.INSTANCE.getGpioFault();
      gpioNetwork = Hardware.INSTANCE.getGpioNetwork();
      gpioMoving = Hardware.INSTANCE.getGpioMoving();
      gpioGPS = Hardware.INSTANCE.getGpioGPS();
      ServerBus.INSTANCE.register(this);

      Timer timer = new Timer();
      timer.schedule(new TimerTask() { public void run() {
         RotateRequest testRequest = currentRequest;
         // Led on if there's a request and it's not expired.
         gpioNetwork.setState(testRequest != null && !testRequest.isExpired());  
      }}, 1000, 1000);

   }


   @Subscribe
   public void setNetwork(RotateRequest request) {
      currentRequest = request;
   }
  

   public void pulseGPS(long time) {
      gpioGPS.pulse(time);
   }

 
   public void setMoving(boolean on) {
      gpioMoving.setState(on);
   }

   public void setFaultBlink(boolean shouldBlink, long blinkRate) {
      if (shouldBlink) {
         gpioFault.blink(blinkRate);
      } else {
         gpioFault.low();
      }
   }

}
