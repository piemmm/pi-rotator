package org.prowl.pirotator.ui.ansi.parser;

import java.util.Arrays;

public enum MonitorLevel {

   NONE,
   ANNOUNCE,
   APRS,
   ALL;
   
   public static MonitorLevel findByName(final String name) {
      return Arrays.stream(values()).filter(value -> value.name().equalsIgnoreCase(name)).findFirst().orElse(null);
   }

}
