package org.prowl.pirotator.controllers.macdoppler;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.HashMap;
import java.util.Locale;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.prowl.pirotator.controllers.Controller;
import org.prowl.pirotator.eventbus.ServerBus;
import org.prowl.pirotator.eventbus.events.RotateRequest;

/**
 * A Macdoppler compatible UDP receiver that listens out for UDP packets to
 * control the rotator.
 * 
 * Sample data in packet: [AzEl Rotor Report:Azimuth:350.00, Elevation:0.00]
 */
public class MacDoppler implements Controller {

   private Log           LOG          = LogFactory.getLog("MacDoppler");

   private boolean       running;
   private ReceiveThread receiveThread;

   private int           DEFAULT_PORT = 9932;
   private int           listenPort   = DEFAULT_PORT;

   public MacDoppler(HierarchicalConfiguration config) {
      listenPort = config.getInt("listenPort", DEFAULT_PORT);
   }
   
   public void start() {
      init();
   }
   
   public void stop() {
      
   }
   
   public String getName() {
      return "MacDoppler";
   }


   public void init() {
      running = true;
      receiveThread = new ReceiveThread();
      receiveThread.start();
   }

   private class ReceiveThread extends Thread {

      private DatagramSocket udpListener;

      public ReceiveThread() {
         super("MacDoppler compatible UDP broadcast receiver");
      }

      public void run() {

         LOG.info("Listener started");
         while (running) {
            // Avoid churning CPU if network is disabled, just wait for link and retry.
            try {
               Thread.sleep(100);
            } catch (InterruptedException e) {
            }

            try {
               udpListener = new DatagramSocket(listenPort); // Macdoppler port 9932 for broadcasts

               byte[] buffer = new byte[2048];
               DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

               while (running) {
                  udpListener.receive(packet);
                  byte[] payload = packet.getData();
                  String stringData = new String(payload, 0, packet.getLength(), "ISO-8859-1");

                  System.out.println(stringData);
                  if (stringData.contains("[AzEl Rotor Report:")) {
                     String dataOnly = stringData.substring(stringData.indexOf("[AzEl Rotor Report:") + 19, stringData.lastIndexOf(']'));
                     processPacket(dataOnly);
                  }

               }

            } catch (Throwable e) {
               LOG.error(e.getMessage(), e);
            }
         }
      }

      /**
       * Process an azel packet like: [AzEl Rotor Report:Azimuth:350.00,
       * Elevation:0.00, SatName:ABC-1234]
       * 
       * @param data the data packet
       */
      public void processPacket(String data) {
         String[] parameters = data.split(","); // Split up into data:value
         HashMap<String, String> map = new HashMap<>();
         for (String s : parameters) {

            String[] pairs = s.trim().split(":");
            String name = pairs[0].trim().toLowerCase(Locale.ENGLISH);
            String value = pairs[1].trim();
            map.put(name, value);
         }
         RotateRequest request = new RotateRequest(Double.parseDouble(map.get("elevation")), Double.parseDouble(map.get("azimuth")), map.get("satname"));
         ServerBus.INSTANCE.post(request);
      }

   }

}