package org.prowl.pirotator.eventbus.events;

public class RotateRequest extends BaseEvent {

   private Double elevation;
   private Double azimuth;

   public RotateRequest(Double elevation, Double azimuth) {
      this.elevation = elevation;
      this.azimuth = azimuth;
   }

   public Double getElevation() {
      return elevation;
   }

   public Double getAzimuth() {
      return azimuth;
   }

}