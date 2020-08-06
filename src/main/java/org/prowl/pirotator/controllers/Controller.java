package org.prowl.pirotator.controllers;

import java.io.IOException;

public interface Controller {
   
   /**
    * Start the controller
    * @throws IOException if an error occurs during startup
    */
   public void start() throws IOException;
   
   /**
    * Stop the controller
    * @throws IOException if an error occurs
    */
   public void stop() throws IOException;
   
   public String getName();
   
   
   
   
}