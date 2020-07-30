package org.prowl.pirotator.hardware.leds;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.Pin;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.RaspiPin;

/**
 * Simple class to allow the changing of status leds on the front panel
 */
public class StatusLeds {

   private GpioController       gpio;
   private Pin                  message = RaspiPin.GPIO_23;
   private Pin                  link    = RaspiPin.GPIO_22;
   private Pin                  gps     = RaspiPin.GPIO_26;
   private Pin                  trx     = RaspiPin.GPIO_21;

   private GpioPinDigitalOutput gpioMessage;
   private GpioPinDigitalOutput gpioLink;
   private GpioPinDigitalOutput gpioGPS;
   private GpioPinDigitalOutput gpioTRX;

   public StatusLeds() {
      init();
   }

   public void init() {

      gpio = GpioFactory.getInstance();

      gpioMessage = gpio.provisionDigitalOutputPin(message, PinState.LOW);
      gpioMessage.setShutdownOptions(true, PinState.LOW);

      gpioLink = gpio.provisionDigitalOutputPin(link, PinState.LOW);
      gpioLink.setShutdownOptions(true, PinState.LOW);

      gpioGPS = gpio.provisionDigitalOutputPin(gps, PinState.LOW);
      gpioGPS.setShutdownOptions(true, PinState.LOW);

      gpioTRX = gpio.provisionDigitalOutputPin(trx, PinState.LOW);
      gpioTRX.setShutdownOptions(true, PinState.LOW);

   }

   public void setLink(boolean on) {
      if (on) {
         gpioLink.high();
      } else {
         gpioLink.low();
      }
   }

   public void pulseGPS(long time) {
      gpioGPS.pulse(time);
   }

   public void pulseTRX(long time) {
      gpioTRX.pulse(time);
   }

   public void setMessageBlink(boolean shouldBlink, long blinkRate) {
      if (shouldBlink) {
         gpioMessage.blink(blinkRate);
      } else {
         gpioMessage.low();
      }
   }

}
