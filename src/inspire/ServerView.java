package inspire;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.*;

public class ServerView extends Observable {
    static final char START_SERVER = '0';
    static final char DOWNLOADS_FOLDER = '1';
    private static final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("h:mm a");

    private JPanel rootPanel;
    private JButton startServerButton;
    private JButton downloadsFolderButton;
    private JTable connectedTable;
    private JTextArea logHistory;
    private JLabel statusBar;
    private JButton clearLogButton;

    ServerView() {
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
        clearLogButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                logHistory.setText("");
            }
        });
        connectedTable.setModel(new CustomTableModel(new ArrayList<Person>()));
        statusBar.setText("");
        JFrame frame = new JFrame("Server");
        frame.setContentPane(rootPanel);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }

    List<Person> getClientList(String clientListFile) {
        File file = new File(clientListFile);
        List<Person> clientList = new ArrayList<>();
        int result = JOptionPane.NO_OPTION;
        if (file.exists()) {
            result = JOptionPane.showConfirmDialog(null, "Use '" + clientListFile + "' as the client list file?");
            if (result == JOptionPane.YES_OPTION) {
                try {
                    Scanner scanner = new Scanner(file);
                    while (scanner.hasNextLine()) {
                        StringTokenizer st = new StringTokenizer(scanner.nextLine());
                        String name = st.nextToken();
                        String hostName = st.nextToken();
                        clientList.add(new Person(name, hostName));
                    }
                    scanner.close();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
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
                    while (scanner.hasNextLine()) {
                        StringTokenizer st = new StringTokenizer(scanner.nextLine());
                        String name = st.nextToken();
                        String hostName = st.nextToken();
                        printStream.println(name + " " + hostName);
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

    void setServerModel(ServerModel serverModel) {
        setStatus("Server started");
        connectedTable.setModel(new CustomTableModel(serverModel.getClientList()));
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

    private void setStatus(String status) {
        statusBar.setText(status);
        logHistory.append(simpleDateFormat.format(new Date()) + "   " + status + "\n");
    }

    void showMessage(String message) {
        JOptionPane.showMessageDialog(rootPanel, message);
    }
}