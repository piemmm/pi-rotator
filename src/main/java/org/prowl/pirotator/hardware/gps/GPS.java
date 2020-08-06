package org.prowl.pirotator.hardware.gps;

import java.io.IOException;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.prowl.pirotator.PiRotator;

import com.pi4j.io.serial.Baud;
import com.pi4j.io.serial.DataBits;
import com.pi4j.io.serial.FlowControl;
import com.pi4j.io.serial.Parity;
import com.pi4j.io.serial.Serial;
import com.pi4j.io.serial.SerialConfig;
import com.pi4j.io.serial.SerialFactory;
import com.pi4j.io.serial.SerialPort;
import com.pi4j.io.serial.StopBits;

import net.sf.marineapi.nmea.io.SentenceReader;
import net.sf.marineapi.nmea.parser.SentenceFactory;
import net.sf.marineapi.nmea.util.Date;
import net.sf.marineapi.nmea.util.GpsFixStatus;
import net.sf.marineapi.nmea.util.Position;
import net.sf.marineapi.nmea.util.Time;
import net.sf.marineapi.provider.HeadingProvider;
import net.sf.marineapi.provider.PositionProvider;
import net.sf.marineapi.provider.SatelliteInfoProvider;
import net.sf.marineapi.provider.event.HeadingEvent;
import net.sf.marineapi.provider.event.HeadingListener;
import net.sf.marineapi.provider.event.PositionEvent;
import net.sf.marineapi.provider.event.ProviderListener;
import net.sf.marineapi.provider.event.SatelliteInfoEvent;
import net.sf.marineapi.provider.event.SatelliteInfoListener;

public class GPS {

   private static final Log          LOG = LogFactory.getLog("GPS");

   private HierarchicalConfiguration config;
   private Serial                    serial;
   private SentenceFactory           sf;
   private static Position           currentPosition;
   private static Double             currentCourse;
   private static Double             currentHeading;
   private static Double             currentSpeed;
   private static Time               currentTime;
   private static Date               currentDate;

   public GPS(HierarchicalConfiguration config) {
      this.config = config;
      sf = SentenceFactory.getInstance();
   }

   public void start() throws IOException {

      serial = SerialFactory.createInstance();

      // create serial config object
      SerialConfig config = new SerialConfig();

      try {

         config.device(SerialPort.getDefaultPort())
               .baud(Baud._9600)
               .dataBits(DataBits._8)
               .parity(Parity.NONE)
               .stopBits(StopBits._1)
               .flowControl(FlowControl.NONE);

         // open the default serial device/port with the configuration settings
         serial.open(config);

      } catch (Throwable e) {
         // Try 3B+
         try {
            config.device("/dev/ttyS0")
                  .baud(Baud._9600)
                  .dataBits(DataBits._8)
                  .parity(Parity.NONE)
                  .stopBits(StopBits._1)
                  .flowControl(FlowControl.NONE);
            serial.open(config);
         } catch (Throwable ex) {
            // Rethrow as IOE
            throw new IOException(e);
         }

      }

      SentenceReader reader = new SentenceReader(serial.getInputStream());

      HeadingProvider provider = new HeadingProvider(reader);
      provider.addListener(new HeadingListener() {

         @Override
         public void providerUpdate(HeadingEvent evt) {
            currentHeading = evt.getHeading();
         }
      });

      PositionProvider pprovider = new PositionProvider(reader);
      pprovider.addListener(new ProviderListener<PositionEvent>() {

         @Override
         public void providerUpdate(PositionEvent evt) {
            currentPosition = evt.getPosition();
            currentTime = evt.getTime();
            currentCourse = evt.getCourse();
            currentSpeed = evt.getSpeed();
            currentDate = evt.getDate();
         }
      });

      SatelliteInfoProvider sprovider = new SatelliteInfoProvider(reader);
      sprovider.addListener(new SatelliteInfoListener() {

         @Override
         public void providerUpdate(SatelliteInfoEvent evt) {
            GpsFixStatus status = evt.getGpsFixStatus();
            if (status == GpsFixStatus.GPS_NA || status == GpsFixStatus.GPS_2D) {
               // Pulse GPS led until locked
               PiRotator.INSTANCE.getStatus().pulseGPS(150);
               currentPosition = null;
            } else if (status == GpsFixStatus.GPS_3D) {
               // LED on all the time
               PiRotator.INSTANCE.getStatus().pulseGPS(1500);
            }
         }

      });

      reader.start();

   }

   public void stop() {

   }

   /**
    * Returns the current known position, or null if no fix
    * 
    * @return
    */
   public static Position getCurrentPosition() {
      return currentPosition;
   }

   public static Time getCurrentTime() {
      return currentTime;
   }

   public static Double getCurrentCourse() {
      return currentCourse;
   }

   public static Double getCurrentHeading() {
      return currentHeading;
   }

   public static Double getCurrentSpeed() {
      return currentSpeed;
   }

   public static Date getCurrentDate() {
      return currentDate;
   }

   public String getName() {
      return getClass().getSimpleName();
   }

}