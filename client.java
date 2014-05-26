import java.io.*;
import java.net.*;
import java.util.*; 

public class client
{

   private static Socket clientSocketFinal; 
   private static String usernameFinal; 
   public static boolean isClosed = false;

   // (to be implemented later) 
   public static final int TIME_OUT = 1000 * 60 * 30; 

   public static void menu(PrintWriter outToServer, BufferedReader inFromServer, BufferedReader inFromUser) throws Exception {
      Thread send = new Thread(new SendingThread(1, outToServer, inFromUser)); 
      Thread receive = new Thread(new ReceivingThread(2, inFromServer)); 

      send.start(); 
      receive.start(); 
   }

   // THREAD FOR SENDING MESSAGES
   private static class SendingThread implements Runnable {
      private int threadName;  
      PrintWriter outToServer; 
      BufferedReader inFromUser; 

      public SendingThread(int i, PrintWriter out, BufferedReader in){
         threadName = i; 
         outToServer = out; 
         inFromUser = in; 
      }

      public void run() {
         try {
            long currentTime = System.currentTimeMillis(); 
            clientSocketFinal.setSoTimeout(TIME_OUT); 
            while(true) { 

               String toSend = inFromUser.readLine();
               outToServer.println(toSend); 

               if(toSend.equals("logout")) {
                  System.out.println("LOGOUT TRIGGERED CLIENT SIDE."); 

                  // close the socket
                  closeSocket(clientSocketFinal, usernameFinal); 
                  isClosed = true; 
                  System.exit(1);
               }
            }
         } catch (Exception e) {
            System.err.println("EXCEPTION: " + e.getMessage()); 

         }
      }
   }

   // THREAD FOR RECEIVING MESSAGES
   private static class ReceivingThread implements Runnable {
      private int threadName; 
      BufferedReader inFromServer; 

      public ReceivingThread(int i, BufferedReader in){
         threadName = i; 
         inFromServer = in; 
      }

      public void run() {
         try {
            String toRead;
            while((toRead = inFromServer.readLine()) != null) {
               // String toRead = inFromServer.readLine();
               System.out.println("FROM SERVER >> " + toRead); 
            }
         } catch (Exception e) {
            System.err.println("EXCEPTION: " + e.getMessage()); 
            try {
               closeSocket(clientSocketFinal, usernameFinal);
            } catch(Exception f) {
               System.err.println("EXCEPTION: " + f.getMessage()); 
            }

            System.exit(1);
         }
      }
   }

   public static void main(String argv[]) throws Exception
   { 
      try {
         String ip_address = argv[0];
         int portNum = Integer.parseInt(argv[1]); 
         
         // CLIENT SOCKET
         Socket clientSocket = new Socket(ip_address, portNum);
         clientSocketFinal = clientSocket;  // GLOBAL VARIABLE CHANGED

         // I/O
         BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
         PrintWriter outToServer = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream()), true); 
         BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

         String username = greeting(outToServer, inFromServer, inFromUser);
         usernameFinal = username;  // GLOBAL VARIABLE CHANGED

         if(username != null)
            menu(outToServer, inFromServer, inFromUser); 

         // CLOSING THE INDIVIDUAL SOCKET
         // closeSocket(clientSocket, username); 
      } catch(Exception e) {
         System.err.println("Sorry, but your credentials did not match the server! Please try again."); 
      }
   }

   public static void closeSocket(Socket clientSocket, String username) throws Exception{
      clientSocket.close();
   }

   // SIMPLE CLASS DICTATING GREETING OF SOCKET
   public static String greeting(PrintWriter outToServer, BufferedReader inFromServer, BufferedReader inFromUser) throws Exception{
      // USERNAME AND PASSWORD
      outToServer.println("Client process connected."); 

      String successPrompt = "";
      String username = ""; 
      int numFails = 0; 

      System.out.print("FROM SERVER >> " + inFromServer.readLine() + " ");

      // what if the response from the server is the same initial prompt for username?
      while(successPrompt != null && !successPrompt.contains("WELCOME!") && numFails < 3) {
         // USERNAME
         username = inFromUser.readLine();
         outToServer.println(username);
         
         // PASSWORD
         System.out.print("FROM SERVER >> " + inFromServer.readLine() + " ");
         outToServer.println(inFromUser.readLine());

         // AUTHENTICATION RESPONSE
         successPrompt = inFromServer.readLine(); 
         System.out.print("FROM SERVER >> " + successPrompt + " "); 

         numFails++; 
      }

      // RETURN THE USERNAME IF SUCCESSFUL, NULL IF NOT
      if(numFails < 3) {   return username; }
      else {   return null; }
   }
}