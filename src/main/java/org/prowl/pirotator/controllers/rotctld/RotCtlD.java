package org.prowl.pirotator.controllers.rotctld;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.prowl.pirotator.PiRotator;
import org.prowl.pirotator.eventbus.ServerBus;
import org.prowl.pirotator.eventbus.events.RotateRequest;
import org.prowl.pirotator.rotator.Rotator;


/**
 * Rotctld compatible controller - this still isn't working quite right, needs more investigation!
 */
public class RotCtlD {

   private Log          LOG    = LogFactory.getLog("RotCrlD");

   private ClientThread client = null;
   private TCPListener  tcpListener;

   public RotCtlD() {

   }
   
   public void start() {
      init();
   }
   
   public void stop() {
      
   }


   public void init() {
      tcpListener = new TCPListener();
      tcpListener.start();
   }

   /**
    * Server socket thread, handles incoming connections.
    */
   public class TCPListener extends Thread {

      public TCPListener() {
         super("RotCtlD tcp listener");
      }

      public void run() {
         while (true) {

            // No need for fast connections, if network goes down then don't chew CPU cycles
            // trying to create a socket.
            try {
               Thread.sleep(100);
            } catch (Throwable e) {
            }

            // Create a server socket
            try (ServerSocket incoming = new ServerSocket(4533);) {

               while (true) {
                  Socket socket = incoming.accept();
                  if (client != null) {
                     client.disconnect(); // Disconnect old clients as we only allow 1 thing to control the rotator at a
                                          // time
                  }
                  client = new ClientThread(socket);
                  client.start();
               }

            } catch (Throwable e) {
               LOG.error(e.getMessage(), e);
            }
         }
      }

   }

   /**
    * Client thread - handles the client once connected.
    */
   public class ClientThread extends Thread {

      private Socket         socket;
      private BufferedReader in;
      private BufferedWriter out;

      public ClientThread(Socket socket) {
         super("RotCtlD: Connected to: " + socket.getInetAddress());
         this.socket = socket;
         LOG.info("Connected to:" + socket.getInetAddress());
      }

      public void run() {
         try {

            in = new BufferedReader(new InputStreamReader(new BufferedInputStream(socket.getInputStream(), 1024)));
            out = new BufferedWriter(new OutputStreamWriter(new BufferedOutputStream(socket.getOutputStream())));
            while (!socket.isClosed()) {

               String command = in.readLine();

               LOG.info("command:" + command);

               if (command.equals("p")) {
                  getPos();
               } else if (command.startsWith("P")) {
                  setPos(command);
               }

            }
         } catch (IOException e) {
            LOG.info("Disconnected: " + socket.getInetAddress() + " - " + e.getMessage());
         } catch (Throwable e) {
            LOG.error(e.getMessage(), e);
         }
      }

      public void write(String s) throws IOException {
         out.write(s + "\n");
         out.flush();
      }

      public void getPos() throws IOException {
         write(""+((int)PiRotator.INSTANCE.getRotator().getAzimuth()));
         write(""+((int)PiRotator.INSTANCE.getRotator().getElevation()));
      }

      public void setPos(String command) throws IOException {
         String[] parts = command.replaceAll("  +", " ").split(" ");
         Double azimuth = Double.parseDouble(parts[1]);
         Double elevation = Double.parseDouble(parts[2]);

         // Send the request off to our rotator
         RotateRequest request = new RotateRequest(elevation, azimuth, "(RotCtlD)");
         ServerBus.INSTANCE.post(request);
         
         write("RPRT 0");
         
      }

      public void disconnect() {
         try {
            socket.close();
         } catch (Throwable e) {
         }
      }

   }
}