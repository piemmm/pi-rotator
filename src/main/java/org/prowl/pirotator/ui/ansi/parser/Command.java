package org.prowl.pirotator.ui.ansi.parser;

import java.util.Arrays;

public enum Command {

   HELP, // Help text
   BYE, // Logout (close connection)
   QUIT, // Logout (close connection)
   EXIT, // Logout (close connection)
   END, // Logout (close connection)
   LOGOUT, // Logout (close connection)
   LOGOFF, // Logout (close connection)

   BAUD, // Change baud
   DEVIATION, // Change deviation
   AFC, // Change AFC filter
   DEMOD, // Change demod filter
   
   
   PORT, // Change Port: 'port 1'
   PORTS, // List ports
   HEARD, // List heard stations
   MON, MONITOR, // Monitor a port
   PING; // Perform a ping

   public static Command findByName(final String name) {
      return Arrays.stream(values()).filter(value -> value.name().equals(name)).findFirst().orElse(null);
   }

}
