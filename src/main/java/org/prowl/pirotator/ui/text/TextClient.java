package org.prowl.pirotator.ui.text;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.ansi.TelnetTerminal;

public class TextClient extends Thread {
   
   private static final Log          LOG = LogFactory.getLog("TextClient");

   private TelnetTerminal terminal;
   private TerminalScreen screen;
    
   public TextClient(TelnetTerminal terminal) {
      this.terminal = terminal;
   }
   
   public void start() {
      try {

         screen = new TerminalScreen(terminal);
         
         screen.startScreen();
         terminal.clearScreen();
 
           
      
      } catch(Throwable e) {
         LOG.error(e.getMessage(), e);
      }
      
      try { screen.close(); } catch(Throwable e) { }
   }
   
 
   
}