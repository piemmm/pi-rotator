package org.prowl.pirotator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.prowl.pirotator.config.Config;
import org.prowl.pirotator.hardware.adc.MCP3008;
import org.prowl.pirotator.ui.UI;
import org.prowl.pirotator.ui.hardware.Status;

/**
 * PiRotator starting class
 * 
 * Loads the configuration and starts the node
 */
public enum PiRotator {

   INSTANCE;

   static {
      // Set our default log format before any logger is created
      System.setProperty("java.util.logging.SimpleFormatter.format", "%1$tF %1$tT %4$s %2$s %5$s%6$s%n");
   }

   public static final String NAME           = "PiRotator";
   public static final String VERSION        = "0.02";
   public static final long   BUILD          = 2020021401;
   public static final String VERSION_STRING = NAME + " v" + VERSION;
   public static final String INFO_TEXT      = "   prowl.org";
   private static final Log   LOG            = LogFactory.getLog("PiRotator");

   private Config             configuration;
   private Status             status;
   private UI                 ui;
   private String             myCall;
   private MCP3008            mcp3008;

   PiRotator() {
   }

   public void startup() {

      try {
         // Load configuration and initialise everything needed.
         configuration = new Config();
         
         // Init hardware
         mcp3008 = new MCP3008();
         
         // Init status objects
         status = new Status();

         // Init User interfaces
         ui = new UI(configuration.getConfig("ui"));

         // Start node services
         ui.start();

         // All done
         Thread t = new Thread() {
            public void run() {
               while (true) {
                  try {
                     Thread.sleep(1000);

                  } catch (InterruptedException e) {
                     e.printStackTrace();
                  }
               }
            }
         };
         t.start();

      } catch (Throwable e) {
         LOG.error(e.getMessage(), e);
         System.exit(1);
      }
   }

   public Config getConfiguration() {
      return configuration;
   }

   public Status getStatus() {
      return status;
   }

   public static void main(String[] args) {
      INSTANCE.startup();
   }

   public String getMyCall() {
      return myCall;
   }

}
