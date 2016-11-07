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
    public static final char START_CLIENT = '0';
    public static final char GET_LIST = '1';
    public static final char DOWNLOADS_FOLDER = '2';
    public static final char SEND = '3';
    public static final char DISCONNECTED_CLIENT = '4';
    public static final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("h:mm a");

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
    private CustomTableModel customTableModel;
    private ClientModel clientModel;

    public ClientView() {
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
                    notifyObservers(new String(String.valueOf(DOWNLOADS_FOLDER) + " " + fileChooser.getSelectedFile().getAbsolutePath()));
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
                        showMessage("Choose a file first!");
                    }
                } else {
                    showMessage("Select the receiver(s) first!");
                }
            }
        });
        customTableModel = new CustomTableModel(new ArrayList<>());
        clientListTable.setModel(customTableModel);
        clientModel = null;
        fileList.setModel(new DefaultListModel());
        statusBar.setText("");
        JFrame frame = new JFrame("Client");
        frame.setContentPane(rootPanel);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }

    public static String getServerHostName(String serverHostNameFile) {
        File file = new File(serverHostNameFile);
        String serverHostName = "";
        int result = JOptionPane.NO_OPTION;
        if (file.exists()) {
            try {
                Scanner sc = new Scanner(file);
                serverHostName = sc.nextLine();
                sc.close();
                result = JOptionPane.showConfirmDialog(null, "Use " + serverHostName + " as the server host name?");
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
        if (!file.exists() || result != JOptionPane.YES_OPTION) {
            serverHostName = JOptionPane.showInputDialog("Enter the host name of the server");
            if (serverHostName != null) {
                try {
                    PrintStream ps = new PrintStream(file);
                    ps.println(serverHostName);
                    ps.close();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
        return serverHostName;
    }

    public static List<String> getAutoServerList(String autoServerListFile) {
        File file = new File(autoServerListFile);
        List<String> autoServerNameList = new ArrayList<>();
        int result = JOptionPane.NO_OPTION;
        if (file.exists()) {
            result = JOptionPane.showConfirmDialog(null, "Use " + autoServerListFile + " as the automatic server host names file?");
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
                    Scanner sc = new Scanner(selectedFile);
                    PrintStream ps = new PrintStream(file);
                    String hostName;
                    while (sc.hasNextLine()) {
                        hostName = sc.nextLine();
                        ps.println(hostName);
                        autoServerNameList.add(hostName);
                    }
                    sc.close();
                    ps.close();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
        } else {
            try {
                Scanner sc = new Scanner(file);
                String hostName;
                while (sc.hasNextLine()) {
                    hostName = sc.nextLine();
                    autoServerNameList.add(hostName);
                }
                sc.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }

        return autoServerNameList;
    }

    public void setStatus(String status) {
        statusBar.setText(status);
        logHistory.append(simpleDateFormat.format(new Date()).toString() + "   " + status + "\n");
    }

    public void listChanged() {
        customTableModel = new CustomTableModel(clientModel.getClientList());
        clientListTable.setModel(customTableModel);
    }

    public void setClientModel(ClientModel clientModel) {
        if (clientModel == null) {
            this.clientModel = null;
            return;
        }
        this.clientModel = clientModel;
        clientModel.addObserver(new Observer() {
            @Override
            public void update(Observable observable, Object o) {
                String action = (String) o;
                switch (action.charAt(0)) {
                    case ClientModel.LIST_CHANGED:
                        listChanged();
                        break;
                    case ClientModel.CONNECTED:
                        //status bar
                        break;
                    case ClientModel.DISCONNECTED:
                        showMessage("Disconnected from " + clientModel.getServerHostName());
                        setChanged();
                        notifyObservers(String.valueOf(DISCONNECTED_CLIENT));
                        break;
                    case ClientModel.TRANSFER_STARTED:
                        fileList.setModel(new DefaultListModel());
                        //status bar
                        break;
                    case ClientModel.FILE_RECEIVE_STARTED:
                        setStatus("Receiving " + action.substring(2));
                        break;
                    case ClientModel.FILE_RECEIVE_FINISHED:
                        setStatus("Received " + action.substring(2));
                        break;
                    case ClientModel.FILES_RECEIVED:
                        setStatus("Files received successfully!!");
                        break;
                }
            }
        });
    }

    public int[] getSelectedPeopleIndices() {
        return clientListTable.getSelectedRows();
    }

    public List<File> getSelectedFiles() {
        List<File> selectedFiles = new ArrayList<>();
        ListModel listModel = fileList.getModel();
        int size = listModel.getSize();
        for (int i = 0; i < size; i++) {
            selectedFiles.add(new File((String) listModel.getElementAt(i)));
        }
        return selectedFiles;
    }

    public JPanel getRootPanel() {
        return rootPanel;
    }

    public void showMessage(String message) {
        JOptionPane.showMessageDialog(rootPanel, message);
    }
}