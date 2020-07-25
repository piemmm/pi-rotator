package org.prowl.pirotator.utils;
/**
 * Averages a number of points
 * 
 * @author ihawkins
 */
public class EWMAFilter {
 
   private double alpha;
   private double oldValue;
   private boolean firstRun = true;
   
   public EWMAFilter(float alpha) {
      this.alpha = alpha;
    }

   public synchronized float addPoint(float value) {
      if (Float.isInfinite(value)) {
         throw new RuntimeException("Attempt to add infinity to Totaliser function");
      }
      if (Float.isNaN(value)) {
         throw new RuntimeException("Attempt to add invalid value to Totaliser function");
      }
      
      if (firstRun) {
         firstRun = false;
         oldValue = value;
         return value;
      }
      
      double newValue = oldValue+alpha*(value - oldValue);
      oldValue = newValue;
      return (float)newValue;
      
   }

    
}
 