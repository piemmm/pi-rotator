package org.prowl.pirotator.ui.ansi;

import java.io.IOException;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.prowl.pirotator.ui.UIService;

import com.googlecode.lanterna.terminal.ansi.TelnetTerminal;
import com.googlecode.lanterna.terminal.ansi.TelnetTerminalServer;

/**
 * ANSI UI implementation using lanterna
 */
public class ANSIServer implements UIService {

   private static final Log          LOG = LogFactory.getLog("ANSIServer");

   private boolean stop;
   
   private TelnetTerminalServer server;
   
   private HierarchicalConfiguration config;


   public ANSIServer(HierarchicalConfiguration config) {
     
   }
   
   public void start() {
      try {
         server = new TelnetTerminalServer(23);
      
         while (!stop) {
            TelnetTerminal terminal = server.acceptConnection();
            ANSIClient client = new ANSIClient(terminal);
            client.start();
         }
         
         
      } catch(IOException e) { 
         LOG.error(e.getMessage(),e);
      }
   }
   
   public void stop() {
      stop = true;
   }
   
   public String getName() {
      return getClass().getName();
   }
}
