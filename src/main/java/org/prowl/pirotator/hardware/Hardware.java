package org.prowl.pirotator.hardware;

import java.io.IOException;
import java.util.concurrent.Semaphore;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.prowl.pirotator.PiRotator;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalInput;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.Pin;
import com.pi4j.io.gpio.PinPullResistance;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.RaspiPin;
import com.pi4j.io.i2c.I2CDevice;
import com.pi4j.io.i2c.I2CFactory;
import com.pi4j.io.spi.SpiChannel;
import com.pi4j.io.spi.SpiDevice;
import com.pi4j.io.spi.SpiFactory;
import com.pi4j.io.spi.SpiMode;
import com.pi4j.system.SystemInfo;

/**
 * Single class to access the hardware
 */
public enum Hardware {

   INSTANCE;

   private final Log            LOG          = LogFactory.getLog("Hardware");

   private float                MAX_CPU_TEMP = 75f;

   private final Semaphore      spiLock      = new Semaphore(1, true);
   private SpiDevice            spi;

   private Pin                  dio0         = RaspiPin.GPIO_07;
   private Pin                  dio1         = RaspiPin.GPIO_03;

   private Pin                  ss0          = RaspiPin.GPIO_06;
   private Pin                  ss1          = RaspiPin.GPIO_02;

   private Pin                  reset        = RaspiPin.GPIO_00;

   private Pin                  el_cw        = RaspiPin.GPIO_27;
   private Pin                  el_ccw       = RaspiPin.GPIO_24;
   private Pin                  az_cw        = RaspiPin.GPIO_28;
   private Pin                  az_ccw       = RaspiPin.GPIO_29;

   private Pin                  fan          = RaspiPin.GPIO_25;

   private Pin                  fault        = RaspiPin.GPIO_23;
   private Pin                  network      = RaspiPin.GPIO_22;
   private Pin                  moving       = RaspiPin.GPIO_26;
   private Pin                  gps          = RaspiPin.GPIO_21;

   private GpioPinDigitalOutput gpioFault;
   private GpioPinDigitalOutput gpioNetwork;
   private GpioPinDigitalOutput gpioMoving;
   private GpioPinDigitalOutput gpioGPS;

   private GpioController       gpio;
   private GpioPinDigitalInput  gpioDio0;
   private GpioPinDigitalInput  gpioDio1;
   private GpioPinDigitalOutput gpioSS0;
   private GpioPinDigitalOutput gpioSS1;
   private GpioPinDigitalOutput gpioRst;
   private GpioPinDigitalOutput gpioFan;

   private GpioPinDigitalOutput gpioElCW;
   private GpioPinDigitalOutput gpioElCCW;
   private GpioPinDigitalOutput gpioAzCW;
   private GpioPinDigitalOutput gpioAzCCW;

   private Hardware() {
      try {
         spi = SpiFactory.getInstance(SpiChannel.CS0, 1_000_000, SpiMode.MODE_0);

         gpio = GpioFactory.getInstance();

         // Interrupt setup 0
         gpioDio0 = gpio.provisionDigitalInputPin(dio0, PinPullResistance.PULL_DOWN);
         gpioDio0.setShutdownOptions(true);

         // Interrupt setup 1
         gpioDio1 = gpio.provisionDigitalInputPin(dio1, PinPullResistance.PULL_DOWN);
         gpioDio1.setShutdownOptions(true);

         // Select pin 0
         gpioSS0 = gpio.provisionDigitalOutputPin(ss0, PinState.HIGH);
         gpioSS0.setShutdownOptions(true, PinState.HIGH);
         gpioSS0.high();

         // Select pin 1
         gpioSS1 = gpio.provisionDigitalOutputPin(ss1, PinState.HIGH);
         gpioSS1.setShutdownOptions(true, PinState.HIGH);
         gpioSS1.low();

         // Elevation Clockwise
         gpioElCW = gpio.provisionDigitalOutputPin(el_cw, PinState.LOW);
         gpioElCW.setShutdownOptions(true, PinState.LOW);
         gpioElCW.low();

         // Elevation Counter Clockwise
         gpioElCCW = gpio.provisionDigitalOutputPin(el_ccw, PinState.LOW);
         gpioElCCW.setShutdownOptions(true, PinState.LOW);
         gpioElCCW.low();

         // Azimuth Clockwise
         gpioAzCW = gpio.provisionDigitalOutputPin(az_cw, PinState.LOW);
         gpioAzCW.setShutdownOptions(true, PinState.LOW);
         gpioAzCW.low();

         // Azimuth Counter Clockwise
         gpioAzCCW = gpio.provisionDigitalOutputPin(az_ccw, PinState.LOW);
         gpioAzCCW.setShutdownOptions(true, PinState.LOW);
         gpioAzCCW.low();

         // Fan/Blower
         gpioFan = gpio.provisionDigitalOutputPin(fan, PinState.HIGH);
         gpioFan.setShutdownOptions(true, PinState.HIGH); // Fan always on if no thermal monitor running
         gpioFan.high();

         // Fault redLED - should be on if app is ever not running
         gpioFault = gpio.provisionDigitalOutputPin(fault, PinState.LOW);
         gpioFault.setShutdownOptions(false, PinState.HIGH);

         // Network led - active when an API sending rotate requests
         gpioNetwork = gpio.provisionDigitalOutputPin(network, PinState.LOW);
         gpioNetwork.setShutdownOptions(true, PinState.LOW);

         // GPS - flashes when searching, solid when 3d Fixed
         gpioGPS = gpio.provisionDigitalOutputPin(gps, PinState.LOW);
         gpioGPS.setShutdownOptions(true, PinState.LOW);

         // Moving LED - active when the rotator is being moved.
         gpioMoving = gpio.provisionDigitalOutputPin(moving, PinState.LOW);
         gpioMoving.setShutdownOptions(true, PinState.LOW);

         // Reset (default being reset until we're ready). This also is commoned with the
         // RF modules reset pin.
         gpioRst = gpio.provisionDigitalOutputPin(reset, PinState.HIGH);
         gpioRst.setShutdownOptions(true, PinState.LOW); // If the VM exits or something quits us, we make sure the SX modules can't
                                                         // transmit
         gpioRst.low();
         resetAll();

      } catch (IOException e) {
         LOG.error(e.getMessage(), e);
      }

   }

   public I2CDevice getI2CDevice(int addr) throws IOException, I2CFactory.UnsupportedBusNumberException {
      return I2CFactory.getInstance(1).getDevice(addr);
   }

   public final Semaphore getSPILock() {
      return spiLock;
   }

   public final SpiDevice getSPI() {
      return spi;
   }

   public GpioPinDigitalOutput getGpioSS0() {
      return gpioSS0;
   }

   public GpioPinDigitalOutput getGpioSS1() {
      return gpioSS1;
   }

   public void resetAll() {
      LOG.debug("Reset issued to devices");
      try {
         Thread.sleep(50);
      } catch (InterruptedException e) {
      }
      gpioRst.low();
      try {
         Thread.sleep(150);
      } catch (InterruptedException e) {
      }
      gpioRst.high();
      try {
         Thread.sleep(150);
      } catch (InterruptedException e) {
      }
   }

   public GpioPinDigitalOutput getGpioElCW() {
      return gpioElCW;
   }

   public GpioPinDigitalOutput getGpioElCCW() {
      return gpioElCCW;
   }

   public GpioPinDigitalOutput getGpioAzCW() {
      return gpioAzCW;
   }

   public GpioPinDigitalOutput getGpioAzCCW() {
      return gpioAzCCW;
   }

   public GpioPinDigitalOutput getFan() {
      return gpioFan;
   }

   public GpioPinDigitalOutput getGpioNetwork() {
      return gpioNetwork;
   }

   public GpioPinDigitalOutput getGpioMoving() {
      return gpioMoving;
   }

   public GpioPinDigitalOutput getGpioGPS() {
      return gpioGPS;
   }

   public GpioPinDigitalOutput getGpioFault() {
      return gpioFault;
   }

   
   
}
