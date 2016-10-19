import java.io.*;
import java.net.*;
import java.util.*;
import java.lang.Thread;

class TCPClient {
public static void main(String[] args) {
  Client client = new Client("131.252.208.103", 6789);
}
}

class Client {
private static BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
private static ObjectOutputStream outToServer;
private static ObjectInputStream inFromServer;
private static Queue<TCPPacket> Inbox = new LinkedList<TCPPacket>();
private static String clientName = "UNNAME";

Client(String host, int port) {
  try {
    Socket clientSocket = new Socket(InetAddress.getByName(host), port);
    outToServer = new ObjectOutputStream(clientSocket.getOutputStream());
    inFromServer = new ObjectInputStream(clientSocket.getInputStream());
  } catch (IOException e) {
    System.out.println(e.getMessage());
    System.exit(1);
  }

  inbox();
  readMessage();
  sendMessage();
}

//This thread function will check if the server is still alive
private class isAlive extends Thread { 
  private Socket socket; 
  isAlive(Socket socket) {
    this.socket = socket; 
  }
  
  public void run() {
    while(true) {
      try{
        Thread.sleep(1000);
        if(socket.getInputStream().read() == -1)
          throw new IOException("Server crashed.");
      }catch(IOException e) {
        System.out.println(e.getMessage());
        System.exit(1);
        try {
          socket.close();
        } catch(IOException a) {System.out.println("Error in closing server socket.");}
      }catch(InterruptedException e) {
        System.out.println("Got in interruped");
        e.printStackTrace();
      }
    }
  }
}

public static void sendMessage() {
  while(true) {
    String sentence;
    try {
      sentence = input.readLine();
      String [] tokens = sentence.split(" ");

      TCPPacket toSend = null;
      String command, value, message = null;

      if(tokens.length == 1) {
        command = (tokens[0]);
        toSend = new TCPPacket(clientName, command);
        if(command.equals("/quit")) {
          toSend = new TCPPacket(clientName, command);
        }
      } else if(tokens.length == 2) {
        command = (tokens[0]);
        value = (tokens[1]);
        /* System.out.println(value);
        System.out.println(tokens[1]); */
        toSend = new TCPPacket(clientName, command, value);
      } else {
        command = (tokens[0]);
        value = (tokens[1]);
        message = sentence.substring(command.length() + value.length() + 2);
        toSend = new TCPPacket(clientName, command, value, message);
      }
      sendToServer(toSend);
    } catch (Exception e) { System.out.println("Invalid command. Type /help for mannual.");}
  }
}



private void inbox() {
  Thread storeMessage = new Thread() {
    public void run() {
      while(true) {
        try {
          Object obj = inFromServer.readObject();
          if(obj instanceof TCPPacket) {
            TCPPacket received = (TCPPacket) obj;
            Inbox.add(received);
          } 
          else { System.out.println("Soemtign wrong");}
        } catch(Exception e) { }
      }
    } 
  }; storeMessage.start();
}

private void readMessage() {
  Thread displayMessage = new Thread() {
    public void run() {
      while(true) {
        try{
          TCPPacket packet = Inbox.remove();
          String name = packet.getName().trim();
          String value = packet.getValue().trim(); 
          String command = packet.getCommand().trim();
          switch(command) {
            case "/ERR":
              System.out.println(name + ": " + value + '\n');
              break;
            case "/OK":
              System.out.println(name + ": " + value);
              if(value.contains("Logging off")) {
                System.out.println("You have sucessfully log out!");
                  System.exit(1);
                }
                break;
            }
          } catch(Exception e) {}
        }
      }
    }; displayMessage.start();
  }

  public static void sendToServer(TCPPacket packet) throws IOException {
    outToServer.writeObject(packet);
    outToServer.flush();
  }


// Encryption and Decrytion. Using Caesar cipher ==================================
  private static String caesar(String msg, int shift){
    String plaintext = "";
    int len = msg.length();
    for(int x = 0; x < len; x++){
      char c = (char)(msg.charAt(x) - shift);
      if (c > 'z')
        plaintext += (char)(msg.charAt(x) - (26-shift));
      else 
        plaintext += (char)(msg.charAt(x) - shift);
    }
    return plaintext;
  }
}
