import javax.swing.*;                    //for creating gui components (frames,panels etc)
import javax.swing.border.EmptyBorder;   //for GUI layouts
import java.awt.*;                       //abstract window toolkit,works along eith swing for GUIS
import java.io.*;                        //input/output classes for reading and writing or i/o streams 
import java.net.Socket;                  //establish and managing network b/w clients and server
import java.text.SimpleDateFormat;       //for date and time 


public class ClientGUI {
    private JFrame Clientframe;
    private JPanel messagePanel;
    private JScrollPane scrollPane;
    private JTextField inputField;
    private JButton sendButton;
    private JButton sendFileButton;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private String clientName;

    public ClientGUI() {
        CreateGUI();
        connectToServer();
    }
/*this method creats an interactive GUI for client side provides options for sending messages and files and
    a message panel for viewing messages exchange b/w client and the server */
    private void CreateGUI() {
        Clientframe = new JFrame("Client");
        Clientframe.setSize(400, 600);
        Clientframe.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);     //the window exit ensures closing connection when 
        Clientframe.setLayout(new BorderLayout());

        JPanel headerPanel = new JPanel();
        headerPanel.setBackground(new Color(0, 128, 128));
        headerPanel.setPreferredSize(new Dimension(Clientframe.getWidth(), 60));
        headerPanel.setLayout(new BorderLayout());
        
        JLabel titleLabel = new JLabel("Client Chat", SwingConstants.CENTER);
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        headerPanel.add(titleLabel, BorderLayout.CENTER);
        
        Clientframe.add(headerPanel, BorderLayout.NORTH);

        messagePanel = new JPanel();
        messagePanel.setLayout(new BoxLayout(messagePanel, BoxLayout.Y_AXIS));
        messagePanel.setBackground(new Color(229, 221, 213));

        scrollPane = new JScrollPane(messagePanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(new Color(229, 221, 213));

        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BorderLayout());
        bottomPanel.setBackground(Color.WHITE);
        bottomPanel.setBorder(new EmptyBorder(5, 5, 5, 5));

        inputField = new JTextField();
        inputField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200)),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));

        sendButton = createWhatsAppButtons("Send");
        sendFileButton = createWhatsAppButtons("File");

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        buttonPanel.add(sendButton);
        buttonPanel.add(sendFileButton);
        buttonPanel.setOpaque(false);

        bottomPanel.add(inputField, BorderLayout.CENTER);
        bottomPanel.add(buttonPanel, BorderLayout.EAST);

        Clientframe.add(scrollPane, BorderLayout.CENTER);
        Clientframe.add(bottomPanel, BorderLayout.SOUTH);

        sendButton.addActionListener(e -> sendMessageToServer());
        inputField.addActionListener(e -> sendMessageToServer());
        sendFileButton.addActionListener(e -> sendFileToServer());

        Clientframe.setVisible(true);
    }

    private JButton createWhatsAppButtons(String text) {
        JButton button = new JButton(text);
        button.setBackground(new Color(0, 128, 128));
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return button;
    }
    
    /*this method also serves for providing interative gui to client
    it ensures the messages are appended to proper text areas and also provides scrolling features */
    private void appendMessage(String msg, boolean isSent) {
        JPanel messageContainer = new JPanel(new FlowLayout(isSent ? FlowLayout.RIGHT : FlowLayout.LEFT));
        messageContainer.setOpaque(false);
        messageContainer.setBorder(new EmptyBorder(5, 10, 5, 10));

        JPanel messageBubble = new JPanel(new BorderLayout());
        messageBubble.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        messageBubble.setMaximumSize(new Dimension(300, Integer.MAX_VALUE));

        if (isSent) {
            messageBubble.setBackground(new Color(220, 248, 198));
            messageBubble.setBorder(new EmptyBorder(8, 20, 8, 12));
        } else {
            messageBubble.setBackground(new Color(255, 255, 255));
            messageBubble.setBorder(new EmptyBorder(8, 12, 8, 20));
        }

        JLabel messageLabel = new JLabel("<html><div style='width: 250px;'>" + msg + "</div></html>");
        messageLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        JLabel timeLabel = new JLabel(new SimpleDateFormat("hh:mm a").format(new java.util.Date()));
        timeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        timeLabel.setForeground(new Color(100, 100, 100));
        timeLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.add(messageLabel, BorderLayout.CENTER);
        contentPanel.add(timeLabel, BorderLayout.SOUTH);
        contentPanel.setOpaque(false);

        messageBubble.add(contentPanel, BorderLayout.CENTER);
        messageContainer.add(messageBubble);

        JLabel senderLabel = new JLabel(isSent ? "You" : "Server");
        senderLabel.setFont(new Font("Segoe UI", Font.BOLD, 10));
        senderLabel.setForeground(new Color(100, 100, 100));
        senderLabel.setBorder(new EmptyBorder(2, 5, 0, 5));
        messageContainer.add(senderLabel);

        messagePanel.add(messageContainer);
        messagePanel.revalidate();
        messagePanel.repaint();

        SwingUtilities.invokeLater(() -> {
            scrollPane.getVerticalScrollBar().setValue(scrollPane.getVerticalScrollBar().getMaximum());
        });
    }
/* 1. this function establishes a connection to server via port and IP 
    sets the flow of input and output stream b/w client and server 
    and notifies the server with the clientname as soon as the connection is established*/
    private void connectToServer() {
        try {
            socket = new Socket("127.0.0.1", 1234);            
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());

            clientName = in.readUTF();
            appendMessage("Connected to server as " + clientName, true);     //successfull connection
            receiveMessagesFromServer();
        } catch (IOException e) {
            appendMessage("Connection error: " + e.getMessage(), true);      //handles timeouts and  connectivity errors
        }
    }
    
    /* this function determines sending text messages and commands  from client to the server
       also handles errors if any oocurs while sending messages*/
     private void sendMessageToServer() {
        String msg = inputField.getText().trim();
        if (msg.isEmpty()) 
            return;
        
        try {
            out.writeUTF("MSG:" + msg);
            appendMessage(msg, true);     //successful sending of message
            inputField.setText("");       //empty the inputfield after sending messages
        } catch (IOException e) {
            appendMessage("Error sending message: " + e.getMessage(), true);  //handles error if message sending fails 
        }
    }
     
     private void sendFileToServer() {
        JFileChooser chooser = new JFileChooser(); //open file chooser to select file
        int result = chooser.showOpenDialog(Clientframe);
        if (result != JFileChooser.APPROVE_OPTION) return;

        File file = chooser.getSelectedFile();
        if (!file.exists()) {
            appendMessage("File does not exist", true); //if file not found in file chooser
            return;
        }

        try {
            out.writeUTF("FILE:" + file.getName() + ":" + file.length());
            
            FileInputStream fis = new FileInputStream(file); //read file
            BufferedOutputStream bos = new BufferedOutputStream(socket.getOutputStream()); //send to server
            
            byte[] buffer = new byte[4096]; //buffer the bytes of file
            int count;
            while ((count = fis.read(buffer)) > 0) { //read  the file bytes  and write to output stream
                bos.write(buffer, 0, count);
            }
            bos.flush(); //ensure all data is sent
            fis.close(); //closes input 
            
            appendMessage("Sent file: " + file.getName(), true);
        } catch (IOException e) {
            appendMessage("Error sending file: " + e.getMessage(), true); //4.handling timeouts and connection errors

        }}
/*this function recieves the responses,data and files from the server 
     it checks if the incoming message is a text message or a file 
     if the message is a text its displayed on the client
     */
    private void receiveMessagesFromServer() {
        new Thread(() -> {
            try {
                while (true) {
                    String msg = in.readUTF();  
                    if (msg.startsWith("FILE:")) {            //checking if it is a file
                        String[] parts = msg.split(":");       
                        String fileName = parts[1];          
                        long fileLength = Long.parseLong(parts[2]);
                        receiveFileFromServer(fileName, fileLength);   //for recieving file the recievefilefromserver method is called
                    } else if (msg.startsWith("MSG:")) {               //checking if the message is text
                        appendMessage(msg.substring(4), false);
                    } else {
                        appendMessage(msg, false);    //handling other messages
                    }
                }
            } catch (IOException e) {
                appendMessage("Disconnected from server", true); // handlle timeouts and connection errors
            }
        }).start();
    }

   /*this function is for recieving files from server */

      private void receiveFileFromServer(String fileName, long fileLength) {
        try {
            File file = new File("received_" + fileName);
            FileOutputStream fos = new FileOutputStream(file);
            BufferedInputStream bis = new BufferedInputStream(socket.getInputStream());

            byte[] buffer = new byte[4096];
            int count;
            long totalRead = 0;
            while (totalRead < fileLength && (count = bis.read(buffer)) > 0) {
                fos.write(buffer, 0, count);
                totalRead += count;
            }
            fos.close();
            appendMessage("Received file: " + fileName, false);
        } catch (IOException e) {
            appendMessage("Error receiving file: " + e.getMessage(), false);
        }
    }



 



    public static void main(String[] args) {
        SwingUtilities.invokeLater(ClientGUI::new);
    }
}