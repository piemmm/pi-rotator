package org.prowl.pirotator.ui.text;

import java.io.IOException;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.prowl.pirotator.ui.UIService;

import com.googlecode.lanterna.terminal.ansi.TelnetTerminal;
import com.googlecode.lanterna.terminal.ansi.TelnetTerminalServer;

public class TextServer implements UIService {
   
      private static final Log          LOG = LogFactory.getLog("TextServer");

      private boolean stop;
      
      private TelnetTerminalServer server;
      
      private HierarchicalConfiguration config;


      public TextServer(HierarchicalConfiguration config) {
        
      }
      
      public void start() {
         try {
            server = new TelnetTerminalServer(23);
         
            while (!stop) {
               TelnetTerminal terminal = server.acceptConnection();
               TextClient client = new TextClient(terminal);
               client.start();
            }
            
            
         } catch(IOException e) { 
            
         }
      }
      
      public void stop() {
         stop = true;
      }
      
      public String getName() {
         return getClass().getName();
      }
}
