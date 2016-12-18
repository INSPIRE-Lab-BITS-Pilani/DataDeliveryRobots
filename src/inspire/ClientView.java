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
 * The view for the client in the MVC design.
 * This class observes a {@code ClientModel} instance
 * and responds to state changes by updating the
 * GUI accordingly.
 *
 * @author Abhinav Baid, Atishay Jain
 * @version 1.0
 * @see ClientModel
 * @see ClientController
 * @since 20-12-2016
 */
class ClientView extends Observable {
    /**
     * Notifies click of start client button
     */
    static final char START_CLIENT = '0';
    /**
     * Notifies click of get list button
     */
    static final char GET_LIST = '1';
    /**
     * Notifies change of downloads folder
     */
    static final char DOWNLOADS_FOLDER = '2';
    /**
     * Notifies a transfer request
     */
    static final char SEND = '3';
    /**
     * Notifies disconnection from the server
     */
    static final char DISCONNECTED_CLIENT = '4';
    /**
     * Format for log history timestamping
     */
    private static final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("h:mm a");

    /**
     * Start client button
     */
    private JButton startClientButton;
    /**
     * Get list button
     */
    private JButton getListButton;
    /**
     * Client list table for name -> host name
     */
    private JTable clientListTable;
    /**
     * Choose file button
     */
    private JButton chooseFileButton;
    /**
     * Send button for starting transfer
     */
    private JButton sendButton;
    /**
     * Root panel for holding all GUI elements
     */
    private JPanel rootPanel;
    /**
     * Change downloads folder button
     */
    private JButton downloadsFolderButton;
    /**
     * List of files chosen till now
     */
    private JList fileList;
    /**
     * Delete files button
     */
    private JButton deleteFilesButton;
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
    ClientView() {
        // Initialise action listeners for buttons
        startClientButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                setChanged();
                notifyObservers(String.valueOf(START_CLIENT));
            }
        });
        getListButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                setChanged();
                notifyObservers(String.valueOf(GET_LIST));
            }
        });
        chooseFileButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setCurrentDirectory(new File(System.getProperty("user.home")));
                fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
                int result = fileChooser.showOpenDialog(rootPanel);
                if (result == JFileChooser.APPROVE_OPTION) {
                    File selectedFile = fileChooser.getSelectedFile();
                    DefaultListModel model = (DefaultListModel) fileList.getModel();
                    model.addElement(selectedFile.getAbsolutePath());
                    fileList.setModel(model);
                }
            }
        });
        downloadsFolderButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setCurrentDirectory(new File(System.getProperty("user.home") + "/Downloads"));
                fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                int result = fileChooser.showOpenDialog(rootPanel);
                if (result == JFileChooser.APPROVE_OPTION) {
                    setChanged();
                    notifyObservers(String.valueOf(DOWNLOADS_FOLDER) + " "
                            + fileChooser.getSelectedFile().getAbsolutePath());
                }
            }
        });
        deleteFilesButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                DefaultListModel model = (DefaultListModel) fileList.getModel();
                int[] indices = fileList.getSelectedIndices();
                for (int i = indices.length - 1; i >= 0; i--) {
                    model.remove(indices[i]);
                }
                fileList.setModel(model);
            }
        });
        sendButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (clientListTable.getSelectedRowCount() != 0) {
                    if (fileList.getModel().getSize() != 0) {
                        int result = JOptionPane.showConfirmDialog(null, "Send the files to the selected clients?");
                        if (result == JOptionPane.YES_OPTION) {
                            setChanged();
                            notifyObservers(String.valueOf(SEND));
                        }
                    } else {
                        showMessage("Choose a file first!!");
                    }
                } else {
                    showMessage("Select the receiver(s) first!!");
                }
            }
        });
        clearLogButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                logHistory.setText("");
            }
        });
        // Initialise client list table, file list and status bar
        clientListTable.setModel(new CustomTableModel(new ArrayList<>()));
        fileList.setModel(new DefaultListModel());
        statusBar.setText("");
        // Display the GUI frame
        JFrame frame = new JFrame("Client");
        frame.setContentPane(rootPanel);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }

    /**
     * Used to get a server host name from the user to connect
     *
     * @param serverHostNameFile The name of file where previous host name
     *                           information may be found
     * @return A server host name
     */
    static String getServerHostName(String serverHostNameFile) {
        File file = new File(serverHostNameFile);
        String serverHostName = "";
        int result = JOptionPane.NO_OPTION;
        // Host name previously chosen
        if (file.exists()) {
            try {
                Scanner scanner = new Scanner(file);
                serverHostName = scanner.nextLine();
                scanner.close();
                result = JOptionPane.showConfirmDialog(null, "Use '" + serverHostName + "' as the server host name?");
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
        // Choose a new server host name
        if (!file.exists() || result != JOptionPane.YES_OPTION) {
            serverHostName = JOptionPane.showInputDialog("Enter the host name of the server");
            if (serverHostName != null) {
                try {
                    PrintStream printStream = new PrintStream(file);
                    printStream.println(serverHostName);
                    printStream.close();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
        return serverHostName;
    }

    /**
     * Used to get a list of server host names
     *
     * @param autoServerListFile File in which previous host name
     *                           list file information will be found
     * @return A list of host names to try connecting to
     */
    static List<String> getAutoServerList(String autoServerListFile) {
        File file = new File(autoServerListFile);
        List<String> autoServerNameList = new ArrayList<>();
        int result = JOptionPane.NO_OPTION;
        // Auto server file choosen previously
        if (file.exists()) {
            try {
                Scanner scanner = new Scanner(file);
                autoServerListFile = scanner.nextLine();
                scanner.close();
                result = JOptionPane.showConfirmDialog(null, "Use '" + autoServerListFile
                        + "' as the automatic server host names file?");
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
        // Choose a new file
        if (!file.exists() || result != JOptionPane.YES_OPTION) {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setCurrentDirectory(new File(System.getProperty("user.home")));
            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            fileChooser.setDialogTitle("Choose automatic server host names file");
            result = fileChooser.showOpenDialog(null);
            if (result == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();
                try {
                    Scanner scanner = new Scanner(selectedFile);
                    PrintStream printStream = new PrintStream(file);
                    printStream.println(selectedFile.getAbsolutePath());
                    String hostName;
                    while (scanner.hasNextLine()) {
                        hostName = scanner.nextLine();
                        if (!hostName.isEmpty()) {
                            autoServerNameList.add(hostName);
                        }
                    }
                    printStream.close();
                    scanner.close();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
        } else {
            try {
                Scanner scanner = new Scanner(new File(autoServerListFile));
                String hostName;
                while (scanner.hasNextLine()) {
                    hostName = scanner.nextLine();
                    if (!hostName.isEmpty()) {
                        autoServerNameList.add(hostName);
                    }
                }
                scanner.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
        return autoServerNameList;
    }

    /**
     * Returns indices of selected receivers
     *
     * @return Indices of selected receivers
     */
    int[] getReceiverIndices() {
        return clientListTable.getSelectedRows();
    }

    /**
     * Returns selected file list
     *
     * @return Selected file list
     */
    List<File> getSelectedFiles() {
        List<File> selectedFiles = new ArrayList<>();
        ListModel listModel = fileList.getModel();
        int size = listModel.getSize();
        for (int i = 0; i < size; i++) {
            selectedFiles.add(new File((String) listModel.getElementAt(i)));
        }
        return selectedFiles;
    }

    /**
     * Sets the {@code clientModel} parameter of the {@code ClientView} class
     *
     * @param clientModel New {@code ClientModel} instance
     */
    void setClientModel(final ClientModel clientModel) {
        if (clientModel == null) {
            return;
        }
        setStatus("Connected to '" + clientModel.getServerHostName() + "'");
        clientModel.addObserver(new Observer() {
            @Override
            public void update(Observable observable, Object o) {
                String action = (String) o;
                switch (action.charAt(0)) {
                    case ClientModel.LIST_CHANGED:
                        clientListTable.setModel(new CustomTableModel(clientModel.getClientList()));
                        break;
                    case ClientModel.DISCONNECTED:
                        setStatus("Disconnected from '" + clientModel.getServerHostName() + "'");
                        setChanged();
                        notifyObservers(String.valueOf(DISCONNECTED_CLIENT));
                        break;
                    case ClientModel.TRANSFER_STARTED:
                        fileList.setModel(new DefaultListModel());
                        break;
                    case ClientModel.FILE_RECEIVE_STARTED:
                        setStatus("Receiving " + action.substring(2));
                        break;
                    case ClientModel.FILE_RECEIVE_FINISHED:
                        setStatus("Received " + action.substring(2));
                        break;
                    case ClientModel.FILES_RECEIVED:
                        setStatus("Received all files from '" + clientModel.getServerHostName() + "'");
                        break;
                    case ClientModel.FILE_SEND_STARTED:
                        setStatus("Transferring " + action.substring(2));
                        break;
                    case ClientModel.FILE_SEND_FINISHED:
                        setStatus("Transferred " + action.substring(2));
                        break;
                    case ClientModel.FILES_SENT:
                        setStatus("Transferred all files to '" + clientModel.getServerHostName() + "'");
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