package inspire;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * The view for the server in the MVC design.
 * This class observes a {@code ServerModel} instance
 * and responds to state changes by updating the
 * GUI accordingly.
 *
 * @author Abhinav Baid, Atishay Jain
 * @version 1.0
 * @see ServerModel
 * @see ServerController
 * @since 20-12-2016
 */
class ServerView extends Observable {
    /**
     * Notifies click of start server button
     */
    static final char START_SERVER = '0';
    /**
     * Notifies change of downloads folder
     */
    static final char DOWNLOADS_FOLDER = '1';
    /**
     * Format for log history timestamping
     */
    private static final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("h:mm a");

    /**
     * Root panel for holding all GUI elements
     */
    private JPanel rootPanel;
    /**
     * Start server button
     */
    private JButton startServerButton;
    /**
     * Change downloads folder button
     */
    private JButton downloadsFolderButton;
    /**
     * Client list table for name -> host name
     */
    private JTable clientListTable;
    /**
     * Status of the application
     */
    private JLabel statusBar;
    /**
     * Log of the application activity
     */
    private JTextArea logHistory;
    /**
     * Clear log history
     */
    private JButton clearLogButton;

    /**
     * Initialise GUI for the application
     */
    ServerView() {
        // Initialise action listeners for buttons
        startServerButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setChanged();
                notifyObservers(String.valueOf(START_SERVER));
            }
        });
        downloadsFolderButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setCurrentDirectory(new File(System.getProperty("java.io.tmpdir")));
                fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                int result = fileChooser.showOpenDialog(rootPanel);
                if (result == JFileChooser.APPROVE_OPTION) {
                    setChanged();
                    notifyObservers(String.valueOf(DOWNLOADS_FOLDER) + " "
                            + fileChooser.getSelectedFile().getAbsolutePath());
                }
            }
        });
        clearLogButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                logHistory.setText("");
            }
        });
        // Initialise client list table and status bar
        clientListTable.setModel(new CustomTableModel(new ArrayList<>()));
        statusBar.setText("");
        // Display the GUI frame
        JFrame frame = new JFrame("Server");
        frame.setContentPane(rootPanel);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }

    /**
     * Used to get a client list file
     *
     * @param clientListFile File in which previously chosen client list
     *                       file path is stored
     * @return List of clients
     */
    List<Person> getClientList(String clientListFile) {
        File file = new File(clientListFile);
        List<Person> clientList = new ArrayList<>();
        int result = JOptionPane.NO_OPTION;
        // A client list file was selected previously
        if (file.exists()) {
            try {
                Scanner sc = new Scanner(file);
                clientListFile = sc.nextLine();
                sc.close();
                result = JOptionPane.showConfirmDialog(null, "Use '" + clientListFile + "' as the client list file?");
                if (result == JOptionPane.YES_OPTION) {
                    Scanner scanner = new Scanner(new File(clientListFile));
                    while (scanner.hasNextLine()) {
                        StringTokenizer st = new StringTokenizer(scanner.nextLine());
                        String name = st.nextToken();
                        String hostName = st.nextToken();
                        clientList.add(new Person(name, hostName));
                    }
                    scanner.close();
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
        // Choose a new client list file
        if (!file.exists() || result != JOptionPane.YES_OPTION) {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setCurrentDirectory(new File(System.getProperty("user.home")));
            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            fileChooser.setDialogTitle("Choose client list file");
            result = fileChooser.showOpenDialog(rootPanel);
            if (result == JFileChooser.APPROVE_OPTION) {
                try {
                    File newClientListFile = fileChooser.getSelectedFile();
                    Scanner scanner = new Scanner(newClientListFile);
                    PrintStream printStream = new PrintStream(file);
                    printStream.println(newClientListFile.getAbsolutePath());
                    while (scanner.hasNextLine()) {
                        StringTokenizer st = new StringTokenizer(scanner.nextLine());
                        String name = st.nextToken();
                        String hostName = st.nextToken();
                        clientList.add(new Person(name, hostName));
                    }
                    printStream.close();
                    scanner.close();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
        return clientList;
    }

    /**
     * Sets the {@code serverModel} parameter of the {@code ServerView} class
     *
     * @param serverModel New {@code ServerModel} instance
     */
    void setServerModel(ServerModel serverModel) {
        setStatus("Server started");
        clientListTable.setModel(new CustomTableModel(serverModel.getClientList()));
        serverModel.addObserver(new Observer() {
            @Override
            public void update(Observable observable, Object o) {
                String action = (String) o;
                switch (action.charAt(0)) {
                    case ServerModel.FILE_RECEIVE_STARTED:
                        setStatus("Receiving " + action.substring(2));
                        break;
                    case ServerModel.FILE_RECEIVE_FINISHED:
                        setStatus("Received " + action.substring(2));
                        break;
                    case ServerModel.FILES_RECEIVED:
                        setStatus("Received all files from '" + action.substring(2) + "'");
                        break;
                    case ServerModel.FILE_SEND_STARTED:
                        setStatus("Transferring " + action.substring(2));
                        break;
                    case ServerModel.FILE_SEND_FINISHED:
                        setStatus("Transferred " + action.substring(2));
                        break;
                    case ServerModel.FILES_SENT:
                        setStatus("Transferred all files to '" + action.substring(2) + "'");
                        break;
                    case ServerModel.CLIENT_CONNECTED:
                        setStatus("Connected to client '" + action.substring(2) + "'");
                        break;
                }
            }
        });
    }

    /**
     * Sets status of the application
     *
     * @param status Status of the application
     */
    private void setStatus(String status) {
        statusBar.setText(status);
        logHistory.append(simpleDateFormat.format(new Date()) + "   " + status + "\n");
    }

    /**
     * Displays a message
     *
     * @param message Message to be displayed
     */
    void showMessage(String message) {
        JOptionPane.showMessageDialog(rootPanel, message);
    }
}