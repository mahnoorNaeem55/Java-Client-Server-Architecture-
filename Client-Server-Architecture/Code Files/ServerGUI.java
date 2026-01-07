import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.Map;

/*creation of a UI interface for server side */
public class ServerGUI {
    private final JFrame Serverframe;
    private final JPanel messagePanel;
    private final JScrollPane ChatscrollPane;
    private final JTextField targetField;
    private final JButton sendFileButton;
    private ServerSocket serverSocket;
    private final Map<String, DataOutputStream> clientMap = new HashMap<>();
    private final Map<String, Socket> clientSockets = new HashMap<>();
    private int clientCounter = 0;

    public ServerGUI() {
        Serverframe = new JFrame("Server");
        Serverframe.setSize(400, 600);
        Serverframe.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        Serverframe.setLayout(new BorderLayout());

        
        JPanel headerPanel = new JPanel();
        headerPanel.setBackground(new Color(0, 128, 128));
        headerPanel.setPreferredSize(new Dimension(Serverframe.getWidth(), 60));
        headerPanel.setLayout(new BorderLayout());
        
        JLabel titleLabel = new JLabel("Server Chat", SwingConstants.CENTER);
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        headerPanel.add(titleLabel, BorderLayout.CENTER);
        
        Serverframe.add(headerPanel, BorderLayout.NORTH);

        // panel for displaying message
        messagePanel = new JPanel();
        messagePanel.setLayout(new BoxLayout(messagePanel, BoxLayout.Y_AXIS));
        messagePanel.setBackground(new Color(229, 221, 213));
         
        //scrolling window feature
        ChatscrollPane = new JScrollPane(messagePanel);
        ChatscrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        ChatscrollPane.setBorder(null);
        ChatscrollPane.getViewport().setBackground(new Color(229, 221, 213));

       
        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BorderLayout());
        bottomPanel.setBackground(Color.WHITE);
        bottomPanel.setBorder(new EmptyBorder(5, 5, 5, 5));

        JPanel inputWrapper = new JPanel(new BorderLayout());
        inputWrapper.setBackground(Color.WHITE);
        
         //choose the client whom to send 
        targetField = new JTextField("", 10);
        targetField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200)),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));

        JPanel targetPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        targetPanel.add(new JLabel("To:"));
        targetPanel.add(targetField);
        targetPanel.setOpaque(false);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        sendFileButton = createWhatsAppButton("Send File");
        buttonPanel.add(sendFileButton);
        buttonPanel.setOpaque(false);

        inputWrapper.add(targetPanel, BorderLayout.WEST);
        inputWrapper.add(buttonPanel, BorderLayout.EAST);

        bottomPanel.add(inputWrapper, BorderLayout.CENTER);

        Serverframe.add(ChatscrollPane, BorderLayout.CENTER);
        Serverframe.add(bottomPanel, BorderLayout.SOUTH);

        sendFileButton.addActionListener(e -> InitiateFileSend());

        Serverframe.setVisible(true);
        startServer();
    }
 //creating buttons
    private JButton createWhatsAppButton(String text) {
        JButton button = new JButton(text);
        button.setBackground(new Color(0, 128, 128));
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return button;
    }
/* it ensures the messages are appended to proper text areas and also provides scrolling features */
    private void AppendMessage(String msg, boolean isServerMessage) {
        JPanel messageContainer = new JPanel(new FlowLayout(isServerMessage ? FlowLayout.LEFT : FlowLayout.RIGHT));
        messageContainer.setOpaque(false);
        messageContainer.setBorder(new EmptyBorder(5, 10, 5, 10));

        JPanel messageBubble = new JPanel(new BorderLayout());
        messageBubble.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        messageBubble.setMaximumSize(new Dimension(300, Integer.MAX_VALUE));

        if (isServerMessage) {
            messageBubble.setBackground(new Color(220, 248, 198));
            messageBubble.setBorder(new EmptyBorder(8, 12, 8, 20));
        } else {
            messageBubble.setBackground(new Color(255, 255, 255));
            messageBubble.setBorder(new EmptyBorder(8, 20, 8, 12));
        }

        JLabel messageLabel = new JLabel("<html><div style='width: 250px;'>" + msg + "</div></html>");
        messageLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        JLabel timeLabel = new JLabel(new java.text.SimpleDateFormat("hh:mm a").format(new java.util.Date()));
        timeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        timeLabel.setForeground(new Color(100, 100, 100));
        timeLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.add(messageLabel, BorderLayout.CENTER);
        contentPanel.add(timeLabel, BorderLayout.SOUTH);
        contentPanel.setOpaque(false);

        messageBubble.add(contentPanel, BorderLayout.CENTER);
        messageContainer.add(messageBubble);

        JLabel senderLabel = new JLabel(isServerMessage ? "Server" : "Client");
        senderLabel.setFont(new Font("Segoe UI", Font.BOLD, 10));
        senderLabel.setForeground(new Color(100, 100, 100));
        senderLabel.setBorder(new EmptyBorder(2, 5, 0, 5));
        messageContainer.add(senderLabel);

        messagePanel.add(messageContainer);
        messagePanel.revalidate();
        messagePanel.repaint();

        SwingUtilities.invokeLater(() -> {
            ChatscrollPane.getVerticalScrollBar().setValue(ChatscrollPane.getVerticalScrollBar().getMaximum());
        });  
    }
  
    public void displayMessage(String msg) {
        AppendMessage(msg, false);
    }
   /*bind to a specific port+ip and listen for incoming connections */
    private void startServer() {
        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(1234, 0, InetAddress.getByName("127.0.0.1"));
                AppendMessage("Server started on port 1234", true);
                
                 //accepts multiple clients (used threading here)
                while (true) {
                    Socket socket = serverSocket.accept();
                    clientCounter++;
                    String clientName = "Client " + clientCounter; //assigns id to client 
                    
                   //recieve and process client requests
                    DataInputStream in = new DataInputStream(socket.getInputStream());
                    DataOutputStream out = new DataOutputStream(socket.getOutputStream());

                    out.writeUTF(clientName);
                    //manages client sessions (tracking active clients)
                    synchronized (clientMap) {
                        clientMap.put(clientName, out);
                        clientSockets.put(clientName, socket);
                    }

                    AppendMessage(clientName + " connected", true);
                    new Thread(new ClientHandler(socket, clientName, in, this)).start();
                }
                //graceful closure of server, notyfing all the connected clients 
            } catch (IOException e) {
                broadcastingToClients("SERVER_SHUTDOWN:The server has been disconnected. Please reconnect."); 
                AppendMessage("Server Error: " + e.getMessage(), true);
            }
        }).start();
    }
//this method is for sending files to clients
    private void InitiateFileSend() {
        String target = targetField.getText().trim();

        JFileChooser chooser = new JFileChooser();    //choose file from file chooser
        int result = chooser.showOpenDialog(Serverframe);
        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File file = chooser.getSelectedFile(); //if file dont exist
        if (!file.exists()) {
            AppendMessage("File does not exist", true);
            return;
        }

        if (target.isEmpty()) {   //choosing target whom to send file
            synchronized (clientMap) {
                for (String client : clientMap.keySet()) {
                    sendFileToClient(client, file);
                }
            }
        } else {
            sendFileToClient(target, file);
        }
    }
 //this method is responsiblr for actual file transfer to client 
    private void sendFileToClient(String target, File file) { 
        synchronized (clientMap) {
            DataOutputStream out = clientMap.get(target);
            Socket socket = clientSockets.get(target);

            if (out == null || socket == null) {
                AppendMessage("Client " + target + " not found", true); 
                return;
            }

            try {
                out.writeUTF("FILE:" + file.getName() + ":" + file.length());
                out.flush();

                FileInputStream fis = new FileInputStream(file);
                BufferedOutputStream bos = new BufferedOutputStream(socket.getOutputStream());

                byte[] buffer = new byte[4096];
                int count;
                while ((count = fis.read(buffer)) > 0) {
                    bos.write(buffer, 0, count);
                }
                bos.flush();
                fis.close();

                AppendMessage("Sent file: " + file.getName(), true);
            } catch (IOException ex) {
                AppendMessage("Error sending file to " + target + ": " + ex.getMessage(), true);
            }
        }
    }
/*this method is for notifying all the clients if the server disconnects and if to share file with all the connected clients */
    public void broadcastingToClients(String msg) {  
        synchronized (clientMap) {
            for (DataOutputStream out : clientMap.values()) {
                try {
                    out.writeUTF(msg);
                } catch (IOException ignored) {}
            }
        }
    }
     //log event (disconnection):remove the client that disconnects 
    public void removeClient(String clientName) {
        synchronized (clientMap) {
            clientMap.remove(clientName);
            clientSockets.remove(clientName);
        }
        AppendMessage(clientName + " disconnected", true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ServerGUI::new);
    }
}