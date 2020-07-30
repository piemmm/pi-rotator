package org.prowl.pirotator.rotator;

import org.prowl.pirotator.eventbus.ServerBus;
import org.prowl.pirotator.eventbus.events.RotateRequest;

import com.google.common.eventbus.Subscribe;

/**
 * Main Rotator logic class
 *
 */
public enum Rotator {
   
   INSTANCE;
   
   private Rotator() {
      ServerBus.INSTANCE.register(this);
   }
   
   @Subscribe
   public void getRequest(RotateRequest request) {
      
   }
   
  
}