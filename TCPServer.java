import java.io.*;
import java.net.*;
import java.util.*;
import java.lang.Thread;

class TCPServer{
  private static Map<String,ArrayList<Client>> List = new HashMap<String,ArrayList<Client>>();
  private static Queue<TCPPacket> packets = new LinkedList<TCPPacket>();
  private static Map<Integer, Client> conList = new HashMap<Integer, Client> (); 

  public static void main(String[] args) throws IOException, InterruptedException {
    ServerSocket welcomeSocket = new ServerSocket(6789);
    Thread newClient = new Thread() {
      public void run() {
        int count = 0;
        while(true) {
          try {
            Socket connectionSocket = welcomeSocket.accept();
            Client client = new Client(connectionSocket, ++count);
            conList.put(count, client);
            client.start();
            System.out.println("A new client has connected to the server!");
          } catch (IOException e) { 
            System.out.println(e.getMessage());
          }
        }
      }
    }; newClient.start();



    while(true) {
      try {
        TCPPacket packet = packets.remove();
        String command = packet.getCommand().trim();
          
        if(!(command.equals("/setName")) && conList.get(packet.getID()).getClientName() == null) {
          isNameSet(packet);
          continue;
        }

        switch(command) {
          case "/help":
            help(packet);
            break;
          case "/quit":
            quit(packet);
            break;
          case "/setName":
            setName(packet);
            break;
          case "/pm":
            privateMessage(packet);
            break;
          case "/listOnline":
            if(packet.getValue() != null)
              listOnlineChannel(packet);
            else
              listOnline(packet);
            break;
          case "/create":
            createChannel(packet);
            break;
          case "/join":
            joinChannel(packet);
            break;
          case "/listChannel":
            listChannel(packet);
            break;
          case "/leave":
            leave(packet);
            break;
          case "/msg":
            msg(packet);
            break;
        }
      }catch(Exception e) {}
    }
  } 

//====================== messageHandler functions =============//
  private static void isNameSet(TCPPacket packet) {
    TCPPacket send = new TCPPacket("Server", "/ERR", 
          "Please set a name first by using command /setName <name>.");
    send.setID(packet.getID());
    sendToOne(send);
  }

  private static void help(TCPPacket packet) {
    TCPPacket send = new TCPPacket("Server",  "/ERR"
                    , "\n/help - to show commands" +
        "\n/setName - to set a username" + 
        "\n/quit - to terminate" +
        "\n/listOnline - show online username" +
        "\n/pm - private message");
    send.setID(packet.getID());
    sendToOne(send);
  }

  private static void quit(TCPPacket packet) {
    try {
      TCPPacket send = new TCPPacket("Server" 
            ,"/OK" , "Logging off..");
      send.setID(packet.getID());
      sendToOne(send);
      Client fromClient = conList.get(packet.getID());
      String clientName = fromClient.getClientName();
      fromClient.close(); 
      conList.remove(packet.getID());
      send = new TCPPacket("Server", "/OK", clientName + " has logged off");
      sendToAll(send);
    } catch(IOException e ) { System.out.println(e.getMessage()); }
  }

  private static void setName(TCPPacket packet) {
    TCPPacket send = null;
    if(isNameValid(packet)) {
      Client fromClient = conList.get(packet.getID());
      fromClient.setClientName(packet.getValue());
      send = new TCPPacket("Server", "/OK", "Welcome! You have successfully connected to server!");
    } else {
      send = new TCPPacket("Server", "/ERR", "Name is already taken. Please set another name.");
    }
    send.setID(packet.getID());
    sendToOne(send);
  }

  private static boolean isNameValid(TCPPacket packet) {
    String name = packet.getValue().trim();
    for(Map.Entry<Integer, Client> entry : conList.entrySet()) {
      String clientName = entry.getValue().getClientName();
      if(name.equals(clientName)) {
        return false;
      } 
    }
    return true;
  }

  private static void privateMessage(TCPPacket packet) {
    Client fromClient = conList.get(packet.getID());
    String fromName = fromClient.getClientName();
    String toName = packet.getValue().trim();
    String message = packet.getMsg();

    System.out.println(toName + " " + message);

    for(Map.Entry<Integer, Client> entry : conList.entrySet()) {
      Client toClient = entry.getValue();
      if(toClient.getClientName().equals(toName)) {
        TCPPacket send = new TCPPacket(fromName, "/OK", message); 
        send.setID(toClient.getClientID());
        sendToOne(send);
        send.setID(fromClient.getClientID());
        sendToOne(send);
      }
    }
  }

  private static void listOnline(TCPPacket packet) {
    StringBuilder list = new StringBuilder(); 
    for(Map.Entry<Integer, Client> entry : conList.entrySet()) {
      String name = entry.getValue().getClientName();
      if(name != null) {
        list.append('\n' + name);
      }
    }
    TCPPacket send = (new TCPPacket("Server", "/OK", "\n" + list.toString()));
    send.setID(packet.getID());
    sendToOne(send);
  } 

  private static void listOnlineChannel(TCPPacket packet) {
    String channel = packet.getValue().trim();
    StringBuilder str = new StringBuilder();
    for(Map.Entry<String, ArrayList<Client>> entry : List.entrySet()) {
      if(entry.getKey().equals(channel)) {
        for(Client c : entry.getValue())
          str.append(c.getClientName() + '\n');
      }
    }
    TCPPacket send = new TCPPacket("Server", "/OK", str.toString());
    send.setID(packet.getID());
    sendToOne(send);
  }

  private static void createChannel(TCPPacket packet) {
    String channel = packet.getValue().trim();
    TCPPacket send = null;
    for(Map.Entry<String, ArrayList<Client>> entry : List.entrySet()) {
      String existedChannel = entry.getKey();
      if(channel.equals(existedChannel)) {
        send = new TCPPacket("Server", "/ERR", channel + " is already created.");
        send.setID(packet.getID());
        sendToOne(send);
        return;
      }
    }
    ArrayList<Client> clientList = new ArrayList<Client>();
    List.put(channel, clientList);
    send = new TCPPacket("Server", "/OK", channel + " is created.");
    send.setID(packet.getID());
    sendToOne(send);
  }

  private static void joinChannel(TCPPacket packet) {
    String channel = packet.getValue().trim();
    TCPPacket send = null;
    for(Map.Entry<String, ArrayList<Client>> entry : List.entrySet()) {
      String existedChannel = entry.getKey();
      if(channel.equals(existedChannel)) {
        ArrayList<Client> clientList = List.remove(channel);
        clientList.add(conList.get(packet.getID()));
        List.put(channel, clientList); 
        send = new TCPPacket("Server", "/OK", "sucessfuly joined " + channel);
        send.setID(packet.getID());
        sendToOne(send);
        return;
      }
    }
    send = new TCPPacket("Server", "/ERR", channel + " doesn't exist!");
    send.setID(packet.getID());
    sendToOne(send);
  }

  private static void listChannel(TCPPacket packet) {
    StringBuilder str = new StringBuilder();
    for(Map.Entry<String, ArrayList<Client>> entry : List.entrySet()) {
      str.append(entry.getKey() + '\n');
    }
    TCPPacket send = new TCPPacket("Server", "/ERR", str.toString());
    send.setID(packet.getID());
    sendToOne(send);
  }

  private static void leave(TCPPacket packet) {
    String channel = packet.getValue().trim();
    Client client = conList.get(packet.getID());
    TCPPacket send = null;
    ArrayList<Client> clientList = List.remove(channel);
    clientList.remove(client);
    List.put(channel, clientList);
    send = new TCPPacket("Server", "/OK", client.getClientName() +  " has left the channel " + channel);
    send.setID(packet.getID());
    sendToAll(send);
  }

  private static void msg(TCPPacket packet) {
    String channel = packet.getValue().trim();
    String fromName = conList.get(packet.getID()).getClientName();
    String message = packet.getMsg().trim();

    ArrayList<Client> clientList = List.get(channel);
    TCPPacket send = new TCPPacket(fromName + "(" + channel+")", "/OK", message);
    for(Client c : clientList) {
      send.setID(c.getClientID());
      sendToOne(send);
    }
  }
    
//====================== heartbeat ============================//

  private static class isAlive extends Thread {
    private Socket socket; 
    isAlive(Socket socket) {
      this.socket = socket; 
    }

    public void run() {
      while(true) {
        try {
          Thread.sleep(1000);
          if(socket.getInputStream().read() == -1)
            throw new IOException("A Client disconnected");
        }catch(IOException e) {
          System.out.println("A client left");
          try {
            socket.close();
          } catch(IOException a) { System.out.print("Error in closing connection.");}
          break;
        }catch(InterruptedException e) {
          System.out.println("Got interrupped");
          e.printStackTrace();
        }
      }
    }
  }

//====================== Sending to Client ====================//
  private static void sendToAll(TCPPacket packet) {
    for(Map.Entry<Integer, Client> entry : conList.entrySet()) {
      Client toClient = entry.getValue();
      if(toClient.getClientName() != null) 
        toClient.send(packet);
    }
  }

  private static void sendToOne(TCPPacket packet) {
    conList.get(packet.getID()).send(packet);
  }

//===================== Single Client ========================//
  private static class Client extends Thread { 
    private Socket socket;
    private String clientName = null;
    private int id;
    private ObjectInputStream inFromClientSocket;
    private ObjectOutputStream outToClientSocket;

    Client (Socket socket, int id) throws IOException {
      this.socket = socket;
      this.id = id;
      inFromClientSocket = new ObjectInputStream(this.socket.getInputStream());
      outToClientSocket = new ObjectOutputStream(this.socket.getOutputStream()); 
      isAlive heartbeat = new isAlive(socket);
    } 

    public void run() {
      while(true) {
        try {
          Object obj = inFromClientSocket.readObject();
          if(obj instanceof TCPPacket) {
            TCPPacket packet = (TCPPacket) obj;
            packet.setID(id);
            packets.add(packet);
            System.out.println("You got mail.");
          }
        }catch(Exception e) { } 
      }
    }

    private void send(Object obj) {
      try {
        outToClientSocket.writeObject(obj);
        outToClientSocket.flush();
      } catch(IOException e) { e.printStackTrace(); }
    }

    private int getClientID() {
      return id;
    }

    private void setClientName(String n) {
      clientName = n;
    }

    private String getClientName() {
      return clientName;
    }

    private void close() throws IOException {
      this.socket.close();
    }
  }
  // Encryption and Decrytion. Using Caesar cipher ================================== 
  public static String caesar(String msg, int shift){ 
    String encrypted = ""; 
    int len = msg.length(); 
    for(int x = 0; x < len; x++){ 
      char c = (char)(msg.charAt(x) + shift); 
      if (c > 'z') 
        encrypted += (char)(msg.charAt(x) - (26-shift)); 
      else  
        encrypted += (char)(msg.charAt(x) + shift); 
    } 
    return encrypted; 
  } 
}
