import java.io.*;
import java.net.*;
import java.util.*;
import java.lang.*;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentHashMap;

public class server
{

   // AVAILABLE USERS, LAST HOUR, COMBINATIONS -- simple data structures (to be upgraded)
   public static ArrayList<String> availableUsers = new ArrayList<String>(); 
   public static ConcurrentHashMap<String, Long> usersLastHour = new ConcurrentHashMap<String, Long>(); 
   public static ArrayList<String[]> combinations = new ArrayList<String[]>(); 

   // KEEPS TRACK OF WHO IS BLOCKED FOR WHICH SPECIFIC PERSON and MESSAGES TO SEND
   public static ConcurrentHashMap<String, ConcurrentLinkedQueue<String>> blocked = new ConcurrentHashMap<String, ConcurrentLinkedQueue<String>>(); 
   public static ConcurrentHashMap<String, ConcurrentLinkedQueue<String>> messagesToSend = new ConcurrentHashMap<String, ConcurrentLinkedQueue<String>>();
   public static ConcurrentHashMap<String, Long> blackList = new ConcurrentHashMap<String, Long>(); 

   // EXTRA FUNCTIONALITY -- GROUPS OF USERS
   public static ConcurrentHashMap<String, ConcurrentLinkedQueue<String>> groups = new ConcurrentHashMap<String, ConcurrentLinkedQueue<String>>();

   // STORES SOCKETS FOR ALL USERS (IF THEY EXIST)
   public static ConcurrentHashMap<String, Socket> users = new ConcurrentHashMap<String, Socket>(); 

   public static final int BLOCK_TIME = 1000 * 60;

   // CLIENT THREAD CLASS
   private static class ClientThread implements Runnable {
 
      private Socket threadSocket; 
      private String clientUsername; 

      public ClientThread(Socket threadSock) {
         threadSocket = threadSock;  
      }

      // BROADCAST MESSAGE
      public void broadcast(String message) {
         // PRINT TO EVERY AVAILABLE USER
         for(String person : availableUsers) {
            Socket sendSock = users.get(person); 
            Thread client = new Thread(new SendingMultiple(sendSock, message)); 
            client.start(); 
         }
      }

      // PRIVATE MESSAGE
      public void message(String message, String user) {
         if(!blocked.get(user).contains(clientUsername)) {
            messagesToSend.get(user).add(message);
            messagesToSend.get(clientUsername).add("Your private message went through!"); 
         }
         else {
            // you are on the blacklist so the message will not go through
            messagesToSend.get(clientUsername).add("Sorry, you are blocked from the desired user");
         }
      }

      // WHOELSE
      public void whoelse() {
         String output = ""; 

         for(int i = 0; i < availableUsers.size(); i++){
            output += availableUsers.get(i) + " "; 
         }

         // ADD THE MESSAGE TO THE QUEUE FOR SENDING
         ConcurrentLinkedQueue<String> oldMsgQueue = messagesToSend.get(clientUsername);
         oldMsgQueue.add(output);
         messagesToSend.put(clientUsername, oldMsgQueue); 
      }

      // BLOCK USER
      public void blockUser(String name) {
         // UPDATE BLOCKED LIST TO INCLUDE THE USER MENTIONED
         ConcurrentLinkedQueue<String> oldQueue = blocked.get(clientUsername); 
         if(!oldQueue.contains(name)) {
            oldQueue.add(name); 
            blocked.put(clientUsername, oldQueue); 
            ConcurrentLinkedQueue<String> oldMsgQueue = messagesToSend.get(clientUsername);
            oldMsgQueue.add("NOTICE: YOU HAVE BLOCKED A USER!");
            oldMsgQueue.add("Blocked User: " + name);
            messagesToSend.put(clientUsername, oldMsgQueue); 
         }
         else {
            ConcurrentLinkedQueue<String> oldMsgQueue = messagesToSend.get(clientUsername);
            oldMsgQueue.add("NOTICE: The specific user is already blocked.");
            messagesToSend.put(clientUsername, oldMsgQueue); 
         }
      }

      // UNBLOCK USER
      public void unblockUser(String name) {
         // UPDATE THE BLOCKED LIST TO NOT INCLUDE THE USER MENTIONED
         ConcurrentLinkedQueue<String> oldQueue = blocked.get(clientUsername); 
         if(oldQueue.contains(name)) {
            oldQueue.remove(name); 
            blocked.put(clientUsername, oldQueue); 

            ConcurrentLinkedQueue<String> oldMsgQueue = messagesToSend.get(clientUsername);
            oldMsgQueue.add("NOTICE: YOU HAVE UNBLOCKED A USER SUCCESSFULLY!");
            messagesToSend.put(clientUsername, oldMsgQueue); 
         }
         else {
            ConcurrentLinkedQueue<String> oldMsgQueue = messagesToSend.get(clientUsername);
            oldMsgQueue.add("The user you tried to unblock is not already blocked.");
            messagesToSend.put(clientUsername, oldMsgQueue); 
         }
      }

      public void run(){
         // LIFESPAN OF CLIENT THREAD
         try {
            BufferedReader inFromClient = new BufferedReader(new InputStreamReader(threadSocket.getInputStream()));
            PrintWriter outToClient = new PrintWriter(new OutputStreamWriter(threadSocket.getOutputStream()), true); 
         
            threadMessage("ALERT: NEW CLIENT THREAD"); 
            boolean banned = greeting(inFromClient, outToClient, combinations); 
            if(banned) {
               // CLOSE THE CLIENT SOCKET
               threadSocket.close();
            }
            else {
               acceptCommand(inFromClient, outToClient, threadSocket);
            }
         }  catch (Exception e) {
            System.err.println("Caught Exception: " + e.getMessage()); 
         }
      }

      // THREAD FOR SENDING TO MULTIPLE CLIENTS
      private class SendingMultiple implements Runnable {
         private Socket sendSocket; 
         private String toSend; 

         public SendingMultiple(Socket sendSock, String message) {
            sendSocket = sendSock;
            toSend = message; 
         }

         public void run() {
            try {
               PrintWriter outbound = new PrintWriter(new OutputStreamWriter(sendSocket.getOutputStream()), true); 
               outbound.println(toSend);
            } catch (Exception e) {
               System.err.println("EXCEPTION: " + e.getMessage()); 
               e.printStackTrace();
            }
         }
      }

      // THREAD FOR SENDING MESSAGES
      private class SendingThread implements Runnable {
         PrintWriter outToClient;
         Socket sock;

         public SendingThread(PrintWriter out, Socket s){
            sock = s;
            try {
               outToClient = new PrintWriter(new OutputStreamWriter(sock.getOutputStream()), true);
            } catch(Exception e) {e.printStackTrace();}
            System.out.println("Sending to: " + sock);
         }

         public void run() {
            try {
               while(true) {
                  for(Map.Entry<String, ConcurrentLinkedQueue<String>> entry : messagesToSend.entrySet()) {
                     String key = entry.getKey(); 
                     ConcurrentLinkedQueue<String> messagesWaiting = entry.getValue();
                     if(availableUsers.contains(key)) {
                        while(!messagesWaiting.isEmpty()) {
                           String m = messagesWaiting.poll();
                           Socket sendSock = users.get(key);
                           Thread client = new Thread(new SendingMultiple(sendSock, m)); 
                           client.start(); 
                        }
                     }
                  }
                  Thread.sleep(1000);

               }
            } catch (Exception e) {
               System.err.println("EXCEPTION: " + e.getMessage()); 
               e.printStackTrace(); 
            }
         }
      }

      // THREAD FOR RECEIVING MESSAGES
      private class ReceivingThread implements Runnable {
         BufferedReader inFromClient;
         Socket sock; 

         public ReceivingThread(BufferedReader in, Socket s){
            sock = s; 
            inFromClient = in; 
         }

         public void run() {
            try {
               String command;

               while(true){
                  while((command = inFromClient.readLine()) != null) {
                     
                     String[] commandSeries = command.split(" ");

                     // keep track of COMMAND SET
                     String commandAlone = ""; String parameter1 = ""; String parameter2 = ""; 
                     String privMsg = ""; String broadMsg = ""; 
                     if(commandSeries.length > 0) {
                        commandAlone = commandSeries[0]; 
                        if(commandSeries.length > 1) {
                           parameter1 = commandSeries[1];
                           broadMsg += parameter1 + " "; 
                           if(commandSeries.length > 2) {

                              for(int i = 2; i < commandSeries.length; i++) {
                                 // the other parameters 2 to n
                                 privMsg += commandSeries[i] + " "; 
                                 broadMsg += commandSeries[i] + " "; 
                              }
                           }
                        }
                     }

                     System.err.println("COMMAND >> " + command);

                     if(commandAlone.equals("whoelse")) {
                        // WHOELSE
                        whoelse();  
                     }
                     else if(commandAlone.equals("addgroup")){
                        // ADD YOURSELF TO GROUP
                        String groupname = parameter1; 
                        addGroup(groupname);
                     }
                     else if(commandAlone.equals("offgroup")){
                        // REMOVE YOURSELF FROM GROUP
                        String groupname = parameter1; 
                        offGroup(groupname);   
                     }
                     else if(commandAlone.equals("groupmessage")){
                        // SEND A MESSAGE TO A GROUP YOUR IN
                        String message = privMsg; 
                        String groupname = parameter1; 
                        groupMessage(message, groupname);   
                     }
                     else if(commandAlone.equals("wholasthr")){
                        // WHOLASTHR
                        wholasthr();   
                     }
                     else if(commandAlone.equals("broadcast")) {
                        // broadcast message to EVERY CLIENT
                        broadcast(broadMsg); 
                     }
                     else if(commandAlone.equals("message")) {
                        // send private message to CLIENT SPECIFIED
                        String recipient = parameter1; 
                        message(privMsg, recipient); 

                     }
                     else if(commandAlone.equals("block")) {
                        // block SPECIFIC PERSON from sending you messages
                        String blockedUser = parameter1; 
                        blockUser(blockedUser); 

                     }
                     else if(commandAlone.equals("unblock")) {
                        // unblock SPECIFIC PERSON or throw error if not there
                        String unblockedUser = parameter1; 
                        unblockUser(unblockedUser); 
                     }
                     else if(commandAlone.equals("logout")) {
                        System.out.println("LOGOUT TRIGGERED.");
                        return; 
                     }
                     else {
                        // INVALID COMMAND
                        invalid(); 
                     }
                     messagesToSend.get(clientUsername).add("Enter a command: "); 
                  }
               }
               //deleteUser();

            } catch (Exception e) {
               System.err.println("EXCEPTION: " + e.getMessage()); 
               e.printStackTrace(); 
            }
         }
      }

      // ADD THE CLIENT TO A GROUP, HE/SHE SPECIFIES
      public void addGroup(String nameOfGroup) {
         if (groups.containsKey(nameOfGroup)) {
            // the group DOES exist
            groups.get(nameOfGroup).add(clientUsername); 
            messagesToSend.get(clientUsername).add("Successfully added to group!");
         }
         else {
            // the group DOES not exist yet
            ConcurrentLinkedQueue<String> userGroup = new ConcurrentLinkedQueue<String>();
            groups.put(nameOfGroup, userGroup);
            groups.get(nameOfGroup).add(clientUsername); 
            messagesToSend.get(clientUsername).add("Group created and successfully added!");
         }
      }

      // DELETE THE CLIENT FROM A GROUP, HE/SHE SPECIFIES
      public void offGroup(String nameOfGroup) {
         // first make sure that the group does exist!
         if (groups.containsKey(nameOfGroup)) {
            // the group does exist -- now make sure that the user is in the group
            if(groups.get(nameOfGroup).contains(clientUsername)) {
               // client is already in group -- delete him/her
               groups.get(nameOfGroup).remove(clientUsername); 
               messagesToSend.get(clientUsername).add("Successfully removed from group!");
            }
            else {
               // client is not already in group -- error
               messagesToSend.get(clientUsername).add("CANNOT REMOVE YOU. Not in group.");
            }
         }
         else {
            messagesToSend.get(clientUsername).add("CANNOT REMOVE YOU. The group does not exist.");
         }
      }

      // SEND A MESSAGE TO A GROUP CLIENT SPECIFIES, UNLESS NOT IN GROUP 
      public void groupMessage(String message, String nameOfGroup) {
         // first make sure that the group does exist!
         if (groups.containsKey(nameOfGroup)) {
            // now check to make sure the user is in the group!
            if(groups.get(nameOfGroup).contains(clientUsername)) {
               // broadcast message to rest of group
               ConcurrentLinkedQueue<String> listToSend = groups.get(nameOfGroup);

               for(String person : listToSend) {
                  Socket sendSock = users.get(person); 
                  Thread client = new Thread(new SendingMultiple(sendSock, message)); 
                  client.start(); 
               }
            }
            else {
               messagesToSend.get(clientUsername).add("You do not exist within this group.");
            }
         }
         else {
            // the group does not exist!
            messagesToSend.get(clientUsername).add("The group does not exist!");
         }
      }

      // DELETE USER FROM AVAILABLE LIST (CLEANING UP)
      public void deleteUser() {
         for(int i = 0; i < availableUsers.size(); i++) { 
               if(availableUsers.get(i).equals(clientUsername)) {
                     availableUsers.remove(i); 
                     System.out.println(clientUsername + " was removed.");
               }
         }
      }

      // SERVER-SIDE MENU (START LISTENER AND SENDER)
      public void acceptCommand(BufferedReader inFromClient, PrintWriter outToClient, Socket sock) throws Exception {
         Thread listener = new Thread(new ReceivingThread(inFromClient, sock)); 
         listener.start(); 
         Thread sender = new Thread(new SendingThread(outToClient, sock)); 
         sender.start(); 
      }

      // PRINT INVALID ERROR COMMAND MESSAGE
      public void invalid() {
         ConcurrentLinkedQueue<String> oldMsgQueue = messagesToSend.get(clientUsername);
         oldMsgQueue.add("WARNING: INVALID COMMAND. Enter another: ");
         messagesToSend.put(clientUsername, oldMsgQueue); 
      }

      // WHOLASTHR
      public void wholasthr() {
         // range checked for login (default one hour)

         final long LAST_HOUR = 1000 * 60;
         long currentTime = System.currentTimeMillis(); 

         String output = ""; 

         for(Map.Entry<String, Long> entry : usersLastHour.entrySet()) {
            String username = entry.getKey(); 
            Long userTime = entry.getValue(); 
            long usernameTime = userTime.longValue(); 

            if((currentTime - usernameTime) > LAST_HOUR) {
               // this user last logged in over an hour ago -- DELETE THEM FROM HASHMAP
               usersLastHour.remove(username); 
            }
            else {
               // DISPLAY THIS USER HAS ONE OF MANY WHO USED WITHIN LAST HOUR
               output += username + " "; 
            }
         }  
         messagesToSend.get(clientUsername).add(output);
      }

      public boolean greeting(BufferedReader inFromClient, PrintWriter outToClient, ArrayList<String[]> combinations) throws Exception {
      
         // USERNAME INPUT
         System.out.println("RECEIVED: " + inFromClient.readLine()); 

         boolean comboFound = false; 
         String username = ""; 
         int numFails = 0; 

         loops:
         while(numFails < 3 && !comboFound) {
            // USERNAME
            outToClient.println("ENTER USERNAME: "); 
            username = inFromClient.readLine();

            // PASSWORD
            outToClient.println("ENTER PASSWORD: "); 
            String password = inFromClient.readLine();

            // test the user against the black list
            if(blackList.containsKey(username)) {
               if((System.currentTimeMillis() - blackList.get(username).longValue()) < BLOCK_TIME) {
                  // disconnect!
                  break loops;  
               }
               else {
                  blackList.remove(username); 
               }
            }

            // SEARCH FOR USER/PASS COMBO
            for(String[] combo : combinations) {
               if(username.equals(combo[0])) {
                  if(password.equals(combo[1])) {
                     // the user is VALIDATED
                     // but only if he is not already ONLINE
                     if(!availableUsers.contains(username)) {
                        comboFound = true; 
                     }
                  }
               }
            }

            // AUTHENTICATION PROMPT
            if(comboFound) {
               outToClient.println("WELCOME! Enter a command: ");
               break; 
            }

            numFails++; 
         }

         if(comboFound) { 
            System.out.println(username + " = AUTHENTICATED.");

            // ADD TO HASHMAP AND AVAILABLE ARRAYLIST
            usersLastHour.put(username, System.currentTimeMillis()); 
            availableUsers.add(username); 
            clientUsername = username; // SET THE GLOBAL VARIABLE
            System.out.println("Client username instantiated to: " + clientUsername); 
            System.out.println("Adding socket: " + threadSocket);
            users.put(username, threadSocket); // ADD USER TO HASHMAP

            return false; 
         }
         else {
            // ENTER BLOCKING CODE FOR USER
            System.out.println("NOTICE: USER HAS BEEN BANNED"); 
            blackList.put(username, System.currentTimeMillis());  
            return true;
         }
      }
   }

   public static void main(String argv[]) throws Exception
   {

      long startTime = System.currentTimeMillis();

      // READ IN USERNAMES AND PASSWORDS
      BufferedReader in = new BufferedReader(new FileReader("user_pass.txt")); 
      while (in.ready()) { 
           String[] passCombo = in.readLine().split(" "); 
           combinations.add(passCombo); 
      }
      in.close();

      // LOOP THROUGH BLOCKED AND SET EVERYTHING TO AN EMPTY CONCURRENT LINKED QUEUE
      for(String[] passCombo : combinations) {
         String usernameTemp = passCombo[0]; 
         blocked.put(usernameTemp, new ConcurrentLinkedQueue<String>()); 
         messagesToSend.put(usernameTemp, new ConcurrentLinkedQueue<String>()); 
      }

      // INITALIZED THE USERS HASHMAP BASED ON COMBINATIONS --> DID NOT INITIALIZE!
      for(String[] passCombo : combinations) {
         String usernameTemp = passCombo[0]; 
         users.put(usernameTemp, new Socket()); 
      }
      
      
      int portNum = Integer.parseInt(argv[0]);
      ServerSocket welcomeSocket = new ServerSocket(portNum);
 
      // START ACCEPTING CLIENTS
      while(true)
      {
         Socket connectionSocket = welcomeSocket.accept();
         Thread client = new Thread(new ClientThread(connectionSocket)); 
         System.out.println("Obtained socket: " + connectionSocket);
         client.start(); 
      }
      
   }

   // define the THREAD MESSAGE
   public static void threadMessage(String message) {
      String threadName = Thread.currentThread().getName();
      System.out.format("%s: %s%n", threadName, message);
   }
   
}