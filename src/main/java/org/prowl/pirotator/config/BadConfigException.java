package org.prowl.pirotator.config;

import java.io.IOException;

public class BadConfigException extends IOException {

   public BadConfigException(String message) {
      super(message);
   }
}
