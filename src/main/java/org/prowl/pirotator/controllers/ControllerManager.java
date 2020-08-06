package org.prowl.pirotator.controllers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.SubnodeConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ControllerManager {

   private static final Log     LOG         = LogFactory.getLog("ControllerManager");

   private SubnodeConfiguration configuration;

   private List<Controller>     controllers = new ArrayList<>();

   public ControllerManager(SubnodeConfiguration configuration) throws IOException {
      this.configuration = configuration;
      parseConfiguration();
   }

   /**
    * Parse the configuration and setup each controller
    */
   public void parseConfiguration() {
      // Get a list of controller from the config file
      List<HierarchicalConfiguration> cxs = configuration.configurationsAt("connector");

      // Go create and configure each one.
      for (HierarchicalConfiguration c : cxs) {
         String className = c.getString("type");
         try {
            Controller con = (Controller) Class.forName(className).getConstructor(HierarchicalConfiguration.class).newInstance(c);
            controllers.add(con);
            LOG.info("Added controller: " + className);
         } catch (Throwable e) {
            // Something blew up. Log it and carry on.
            LOG.error("Unable to add controller: " + className, e);
         }

      }

      // If there are no controllers configured then exit as there's little point in
      // continuing.
      if (controllers.size() == 0) {
         LOG.error("Not starting as no controllers have been configured");
         System.exit(1);
      }

   }

   public void start() {
      LOG.info("Starting controllers...");
      for (Controller controller : controllers) {
         try {
            LOG.info("Starting: " + controller.getName());
            controller.start();

         } catch (Throwable e) {
            LOG.error("Unable to start connector: " + controller.getName(), e);
         }
      }
   }

   
}