
### Multithreaded Chatroom 

I built a chatroom using TCP/IP programming in Java.

#### Features

* ```whoelse```: see who else is currently in the chatroom
* ```wholasthr```: view who has been on chatroom in within certain time ago (default = one hour)
* ```broadcast```: sends message to all online clients
* ```blocking``` and ```unblocking``` users: you can unblock or block specific users on the network
* ```logout```: you can log out of the system
* multiple clients: every new client that connects to the chatroom runs in a separate thread on the server
* constant sending and receiving: sending and receiving are separate threads on both the client and the server
* private messaging: you can send a message to a specific user if you are not blocked. if they are offline, when they come back online they will be able to see the message.

#### Compile & Run 

I included the Makefile in my current directory, so you can just run ```make``` to compile the two required Java files which are ```client.java``` and ```server.java```. To clean all of your ```.class``` files at the end, you can just type in ```make clean```. 

* to run the server, type ```java server <portname>```
* to run a client, type ```java client <ip address> <portnumber>```

#### Groups

* ```addgroup <groupname>```: adds yourself to a group and creates the group if first member
* ```offgroup <groupname>```: removes yourself from a group
* ```groupmessage <groupname> <message>```: allows you to send a group message, if you are a member
