package org.prowl.pirotator.eventbus.events;

public class RotateRequest extends BaseEvent {

   private long MAX_AGE = 120000; // 120 seconds.
   
   private Double elevation;
   private Double azimuth;
   private String name;
   private long created;
   
   public RotateRequest(Double elevation, Double azimuth, String name) {
      this.elevation = elevation;
      this.azimuth = azimuth;
      this.name = name;
      this.created = System.currentTimeMillis();
   }

   public Double getElevation() {
      return elevation;
   }

   public Double getAzimuth() {
      return azimuth;
   }

   public String getName() {
      return name;
   }

   public boolean isExpired() {
      if (created > System.currentTimeMillis() - MAX_AGE) 
         return false;
      return true;
   }
}