import java.io.*;


public class TCPPacket implements Serializable {
  private static final long serialVersionUID = 6529685098267757690L;
  private String name;
  private String command;
  private String value;
  private String message;

  TCPPacket(String name, String command, String value, String message) {
    this.name = name;
    this.command = command;
    this.value = value;
    this.message = message;
  }

  TCPPacket(String name, String command, String value) {
    this.name = name;
    this.command = command;
    this.value = value;
    this.message = null;
  }

  TCPPacket(String name, String command) {
    this.name = name;
    this.command = command;
    this.value = null;
    this.message = null;
  }

  public String getMsg() {
    return message;
  }

  public String getName() {
    return name;
  }

  public String getValue() {
    return value;
  }

  public String getCommand() {
    return command;
  }

  public void display() {
    System.out.println(command + ", " + value + ", " + message);
  } 
}
