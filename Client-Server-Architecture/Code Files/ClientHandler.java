
import java.io.*;
import java.net.*;

public class ClientHandler implements Runnable {

    private final Socket socket;
    private final String clientName;
    private final DataInputStream in;
    private final DataOutputStream out;
    private final ServerGUI serverGUI;

    // Constructor
    public ClientHandler(Socket socket, String clientName, DataInputStream in, ServerGUI serverGUI) {
        this.socket = socket;
        this.clientName = clientName;
        this.in = in;
        this.serverGUI = serverGUI;

        DataOutputStream tempOut = null;
        try {
            tempOut = new DataOutputStream(socket.getOutputStream());  //outputstream for sending dsta to client
        } catch (IOException e) {
            serverGUI.displayMessage("Error setting up output stream for " + clientName); //log event (error setting up output stream)
        }
        this.out = tempOut;
    }

    /*this method reads the  clients requests and send back appropritae  responses
   it checks if the incoming message is a text message,a file or any bad request or malformed data
   */
    @Override
    public void run() {
        try {
            while (true) {
                 //recieve and process client requests
                String header = in.readUTF();
                  //define clear communication protocol(indicates simple text message)
                if (header.startsWith("MSG:")) {
                    String msg = header.substring(4).trim();  // Extract actual message
                    serverGUI.displayMessage(clientName + ": " + msg);

                    // Normalize message for processing
                    String normalizedMsg = msg.toLowerCase().trim();

                    // Respond according to type
                    String response = getAutoResponse(normalizedMsg);

                    if (response != null) {
                        out.writeUTF("Server: " + response);
                    } else if (isInvalidMessage(normalizedMsg)) {
                        out.writeUTF("Server: Invalid message. Too many special characters.");
                    } else if (isRandomNumber(normalizedMsg)) {
                        out.writeUTF("Server: Invalid message. Numbers seem meaningless.");
                    } else {
                        out.writeUTF("Server: Sorry, unknown command.");
                    }

                } else if (header.startsWith("FILE:")) {   //if the request is for a file
                    handleFileTransfer(header);
                } else {
                    serverGUI.displayMessage("Unknown command from " + clientName + ": " + header);
                    out.writeUTF("Server: Unknown command received.");  //unknown commands
                }
            }
        } catch (IOException e) {
            synchronized (System.out) {
                serverGUI.displayMessage(clientName + " disconnected."); //log event(disconnection)
                serverGUI.removeClient(clientName);
                try {
                    socket.close(); // gracefully closes client socket 
                } catch (IOException ignored) {}
            }
        }
    }

    // File transfer handling
    private void handleFileTransfer(String header) throws IOException {
        String[] parts = header.split(":");
        if (parts.length != 3) {
            serverGUI.displayMessage("Malformed FILE header from " + clientName);  //if the format is not accurate 
            return;
        }

        String fileName = parts[1];
        long fileLength = Long.parseLong(parts[2]);

        File file = new File("received_" + fileName);

        try (FileOutputStream fos = new FileOutputStream(file)) {  //write incoming file data
            byte[] buffer = new byte[4096];
            long remaining = fileLength;

            while (remaining > 0) {
                int read = in.read(buffer, 0, (int) Math.min(buffer.length, remaining));
                if (read == -1) break;
                fos.write(buffer, 0, read);  //write the data to file
                remaining -= read;           //remaining bytes count
            } 
        }

        serverGUI.displayMessage("Received file " + fileName + " from " + clientName);
    }

    // Predefined responses
    private String getAutoResponse(String msg) {
        return switch (msg) {
            case "hi" -> "hello!";
            case "hello" -> "hi!";
            case "hi there!" -> "Greetings! How can I assist you today?";
            case "how are you?" -> "I'm functioning optimally, thank you for asking.";
            case "can you help me" -> "Yes, sure! Please specify your request.";
            case "thanks", "thank you" -> "You're welcome! I am always here if you need further assistance.";
            case "what services do you offer?" -> "I can assist with various tasks. Kindly let me know your requirement.";
            case "can you share files with me?" -> "Yes, I can share files with you. Which file do you need?";
            case "i am facing an issue" -> "I am here to help. Please describe the issue in detail.";
            case "are you human" -> "I am an automated server assistant designed to assist you.";
            default -> null;
        };
    }

    // Check for invalid special character ratio
    private boolean isInvalidMessage(String msg) {
        if (msg.isEmpty()) return true;
        int validChars = 0;
        for (char c : msg.toCharArray()) {
            if (Character.isLetterOrDigit(c) || Character.isWhitespace(c)) {
                validChars++;
            }
        }
        double percentage = (double) validChars / msg.length();
        return percentage < 0.7;
    }

    // Check if message is just numbers
    private boolean isRandomNumber(String msg) {
        String noSpaces = msg.replaceAll("\\s+", "");
        return noSpaces.matches("\\d+");
    }
}
