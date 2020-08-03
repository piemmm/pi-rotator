package org.prowl.pirotator.ui.hardware;

import java.text.NumberFormat;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.prowl.pirotator.PiRotator;
import org.prowl.pirotator.eventbus.ServerBus;
import org.prowl.pirotator.hardware.lcd.US2066;
import org.prowl.pirotator.hardware.leds.StatusLeds;
import org.prowl.pirotator.rotator.Rotator;
import org.prowl.pirotator.utils.EWMAFilter;
import org.prowl.pirotator.utils.Tools;

public class Status {

   private static final Log LOG = LogFactory.getLog("Status");

   private US2066           lcd;
   private StatusLeds       leds;
   private Timer            tickerTimer;

   private EWMAFilter       rssi2m;
   private EWMAFilter       rssi70cm;

   private NumberFormat     nf;

   public Status() {
      init();
   }

   public void init() {

      rssi2m = new EWMAFilter(0.2f);
      rssi70cm = new EWMAFilter(0.2f);
      nf = NumberFormat.getInstance();
      nf.setMaximumFractionDigits(0);
      nf.setMinimumFractionDigits(0);
      nf.setMaximumIntegerDigits(3);
      nf.setMinimumIntegerDigits(3);
      try {
         lcd = new US2066();
         leds = new StatusLeds();
         lcd.writeText(PiRotator.VERSION_STRING, PiRotator.INFO_TEXT);

         start();
      } catch (UnsatisfiedLinkError e) {
         // Probably not running on pi
         LOG.error(e.getMessage(), e);
      }

   }

   public void start() {

      tickerTimer = new Timer();
      tickerTimer.schedule(new TimerTask() {
         private int screen = 0;

         public void run() {

            try {
               screen0();
            } catch (Throwable e) {
               LOG.debug(e.getMessage(), e);
            }

            screen++;

         }
      }, 2000, 50);

      // Register our interest in events.
      ServerBus.INSTANCE.register(this);

   }

   public void stop() {
      // Stop listening to events
      ServerBus.INSTANCE.unregister(this);
   }

   public void screen0() {

      String satName = StringUtils.defaultIfBlank(Rotator.INSTANCE.getCurrentTracking(), " -");

      String topString = "El: " + nf.format(PiRotator.INSTANCE.getMCP().getElevation()) +"   Az: " + nf.format(PiRotator.INSTANCE.getMCP().getAzimuth());
      String bottomString =  "Sat: " + satName;

      setText(topString.toString(), bottomString.toString());
   }

   public void setText(String line1, String line2) {
      lcd.writeText(line1, line2);
   }

   public void pulseGPS(long time) {
      leds.pulseGPS(time);
   }

   public void setMessageBlink(boolean shouldBlink, long blinkRate) {
      leds.setMessageBlink(shouldBlink, blinkRate);
   }

   public void setLink(boolean on) {
      leds.setLink(on);
   }

}
