package org.prowl.pirotator.ui.ansi.parser;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

import org.apache.commons.lang.StringUtils;
import org.prowl.pirotator.eventbus.ServerBus;
import org.prowl.pirotator.utils.Tools;
import org.prowl.pirotator.PiRotator;

import com.google.common.eventbus.Subscribe;

public class CommandParser {

   public static final String PROMPT           = ":";
   public static final String UNKNOWN_COMMAND  = "Unknown command!";
   public static final String INCORRECT_ARGS   = "Incorrect number of arguments for command!";
   public static final String INVALID_ARGUMENT = "Invalid data supplied for command!";
   public static final String PORT_UNSUPPORTED = "Operation unsupported on this port";

   private ScreenWriter       screen;

   private Mode               mode             = Mode.CMD;                                    // Default to command mode.
   private int                port             = 0;                                           // Default radio port
   private MonitorLevel       monitorLevel     = MonitorLevel.NONE;                           // Level of monitoring

   public CommandParser(ScreenWriter screen) {
      this.screen = screen;
      ServerBus.INSTANCE.register(this);
   }

   public void parse(String c) {

      if (mode == Mode.CMD) {
         String[] arguments = c.split(" ");

         if (c.length() > 0) {
            Command command = Command.findByName(arguments[0].toUpperCase(Locale.ENGLISH));
            if (command != null) {
               doCommand(command, arguments);
            } else {
               unknownCommand();
            }

         }
         screen.write(getPrompt());
      }
   }

   /**
    * Do the command.
    * 
    * @param command
    * @param arguments
    */
   public void doCommand(Command command, String[] arguments) {
      switch (command) {
         case HELP:
            showHelp(arguments);
            break;
         
         case MON:
         case MONITOR:
            monitor(arguments);
            break;
         case BYE:
         case END:
         case LOGOFF:
         case LOGOUT:
         case EXIT:
         case QUIT:
            logout();
            break;
        
            
         default:
            unknownCommand();
      }
   }
    
   

   public void logout() {
      screen.terminate();
   }
   
   public void monitor(String[] arguments) {
      if (arguments.length != 2) {
         write(INCORRECT_ARGS);
         return;
      }

      MonitorLevel newLevel = MonitorLevel.findByName(arguments[1]);

      if (newLevel == null) {
         write(INVALID_ARGUMENT);
         return;
      }

      write("Monitor level changed to: " + newLevel.name());
      monitorLevel = newLevel;
   }

   public void showHelp(String[] arguments) {
      screen.write("No help yet");
      // screen.write(getFile("help.txt"));
   }

//
//   public void showHeard() {
//
//      SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss");
//      MHeard heard = PiRotator.INSTANCE.getStatistics().getHeard();
//      List<Node> nodes = heard.listHeard();
//      if (nodes.size() == 0) {
//         write("No nodes heard");
//      } else {
//         write("Callsign       Last Heard               RSSI");
//         write("--------------------------------------------");
//         for (Node node : nodes) {
//            write(StringUtils.rightPad(node.getCallsign(),15) + StringUtils.rightPad(sdf.format(node.getLastHeard()), 24) + StringUtils.rightPad("-" + node.getRSSI() + " dBm",10));
//         }
//      }
//   }
 
 
   public void unknownCommand() {
      screen.write(UNKNOWN_COMMAND);
   }

   public String getPrompt() {
      return mode.name().toLowerCase() + PROMPT;
   }

   public void write(String s) {
      screen.write(s);
   }
 
 
   

   public void stop() {
      ServerBus.INSTANCE.unregister(this);
   }
}