multithreaded-chatroom
======================

Description: 
--------------

I built a chatroom using TCP/IP programming in JAVA. Some of the following features it supports includes: 

```whoelse```: you can see who else is currently in the chatroom

multiple clients: every new client that connects to the chatroom runs in a separate thread on the server

constant sending and receiving: sending and receiving are separate threads on both the client and the server to handle requests and responses

```wholasthr```: you can see who has been on in the last specified amount of time (which defaults in this case to one hour)

private messaging: you can send a message to one specific user as long as you have not been blocked by them. note that you can even send them a message if they are offline. if that is the case, when they come back online they will be able to see the message.

`broadcast`: you can send a message to every single person currently online

`blocking` and `unblocking` users: you can unblock or block specific users on the network

`logout`: you can log out of the system

How to compile and run the code: 
------------------------

I included the Makefile in my current directory, so you can just run "make" to compile the two required Java files which are "client.java" and "server.java". To clean all of you .class files at the end, you can just type in "make clean". 

To run the server, type ```java server <portname>```
To run a client, type ```java client <ip address> <portnumber>```

Development environment: 
------------------------

I used Sublime Text 2 to write the code and used the terminals on my mac for testing. 

Additional functionalities: 
---------------------------

Add yourself to a group: 

addgroup <groupname>

adds yourself to a group and creates that group if it has not already been created!

Remove yourself from a group: 

offgroup <groupname>

removes yourself from a group or throws an error if the group does not exist or if you are not in the group

groupmessage <groupname> <message>

allows you to send the designated to a group! does not send a message if you are not in the group
