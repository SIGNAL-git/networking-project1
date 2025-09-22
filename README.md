## networking-project1

Project I did for my Networking class in college. Simple file transfer between a client and server.



### How to use:

1. Start up your command terminal
2. Navigate to the bin folder
3. Set up the server, cache, and client connections in that order using these commands:
4. To set up the server, type: java server.java \<server port> <tcp/udp>
5. To set up the cache, type: java cache.java \<cache port> \<server ip> \<server port> <tcp/udp>
6. To set up the client, type: java client.java \<server ip> \<server port> \<cache ip> \<cache port> <tcp/udp>



All further control is done by the client.



### The controls are as follows:

#### put <file\_name>
* &nbsp;	This puts a file from the client\_fl folder to the server\_fl folder
#### get <file\_name>
* &nbsp;	This requests a file from the cache\_fl folder to put on the client\_fl folder
* &nbsp;	If cache\_fl does not have the file, then it requests a file from the server\_fl folder, which is then sent to the client\_fl folder
* &nbsp;	If server\_fl does not have the file, the command fails
#### quit
* &nbsp;	This closes all sockets and automatically exits the program
