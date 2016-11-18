package inspire;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.*;

public class ClientView extends Observable {
    static final char START_CLIENT = '0';
    static final char GET_LIST = '1';
    static final char DOWNLOADS_FOLDER = '2';
    static final char SEND = '3';
    static final char DISCONNECTED_CLIENT = '4';
    private static final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("h:mm a");

    private JButton startClientButton;
    private JButton getListButton;
    private JTable clientListTable;
    private JButton chooseFileButton;
    private JButton sendButton;
    private JPanel rootPanel;
    private JButton downloadsFolderButton;
    private JList fileList;
    private JButton deleteFilesButton;
    private JLabel statusBar;
    private JTextArea logHistory;
    private JButton clearLogButton;

    ClientView() {
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
        clientListTable.setModel(new CustomTableModel(new ArrayList<Person>()));
        fileList.setModel(new DefaultListModel());
        statusBar.setText("");
        JFrame frame = new JFrame("Client");
        frame.setContentPane(rootPanel);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }

    static String getServerHostName(String serverHostNameFile) {
        File file = new File(serverHostNameFile);
        String serverHostName = "";
        int result = JOptionPane.NO_OPTION;
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

    static List<String> getAutoServerList(String autoServerListFile) {
        File file = new File(autoServerListFile);
        List<String> autoServerNameList = new ArrayList<>();
        int result = JOptionPane.NO_OPTION;
        if (file.exists()) {
            result = JOptionPane.showConfirmDialog(null, "Use '" + autoServerListFile
                    + "' as the automatic server host names file?");
        }
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
                    String hostName;
                    while (scanner.hasNextLine()) {
                        hostName = scanner.nextLine();
                        printStream.println(hostName);
                        autoServerNameList.add(hostName);
                    }
                    printStream.close();
                    scanner.close();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
        } else {
            try {
                Scanner scanner = new Scanner(file);
                String hostName;
                while (scanner.hasNextLine()) {
                    hostName = scanner.nextLine();
                    autoServerNameList.add(hostName);
                }
                scanner.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
        return autoServerNameList;
    }

    int[] getReceiverIndices() {
        return clientListTable.getSelectedRows();
    }

    List<File> getSelectedFiles() {
        List<File> selectedFiles = new ArrayList<>();
        ListModel listModel = fileList.getModel();
        int size = listModel.getSize();
        for (int i = 0; i < size; i++) {
            selectedFiles.add(new File((String) listModel.getElementAt(i)));
        }
        return selectedFiles;
    }

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

    private void setStatus(String status) {
        statusBar.setText(status);
        logHistory.append(simpleDateFormat.format(new Date()) + "   " + status + "\n");
    }

    void showMessage(String message) {
        JOptionPane.showMessageDialog(rootPanel, message);
    }
}