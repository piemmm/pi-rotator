package org.prowl.pirotator.ui;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.SubnodeConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class UI {

   private static final Log     LOG        = LogFactory.getLog("UI");

   private SubnodeConfiguration configuration;

   private List<UIService>      interfaces = new ArrayList<>();

   public UI(SubnodeConfiguration configuration) {
      this.configuration = configuration;
      parseConfiguration();
   }

   public void parseConfiguration() {

      // Get a list of user interfaces from the config file
      List<HierarchicalConfiguration> uis = configuration.configurationsAt("interface");

      // Go create and configure each one.
      for (HierarchicalConfiguration c : uis) {
         String className = c.getString("type");
         try {
            UIService con = (UIService) Class.forName(className).getConstructor(HierarchicalConfiguration.class).newInstance(c);
            interfaces.add(con);
            LOG.info("Added interface: " + className);
         } catch (Throwable e) {
            // Something blew up. Log it and carry on.
            LOG.error("Unable to add interface: " + className, e);
         }

      }
   }

   public void start() {
      LOG.info("Starting user interfaces...");
      for (UIService ui : interfaces) {
         try {
            LOG.info("Starting: " + ui.getName());
            ui.start();

         } catch (Throwable e) {
            LOG.error("Unable to start ui: " + ui.getName(), e);
         }
      }
   }
}