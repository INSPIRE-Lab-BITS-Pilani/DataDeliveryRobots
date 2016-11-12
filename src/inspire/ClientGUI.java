package inspire;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.StringTokenizer;

/**
 * The top-level client class including GUI elements.
 */
public class ClientGUI {
    private JButton startClientButton;
    private JButton getListButton;
    private JTable clientListTable;
    private JButton chooseFileButton;
    private JButton sendButton;
    private JPanel rootPanel;
    private JButton downloadsFolderButton;
    private JList fileList;
    private JButton deleteFilesButton;

    /**
     * The list of clients.
     */
    private List<Person> clientList;
    /**
     * The current {@link Client} instance.
     */
    private Client cl;
    /**
     * {@code TableModel} implementation to view {@code clientList}.
     */
    private CustomTableModel tm;
    /**
     * List of files selected for sending.
     */
    private List<File> selectedFiles;
    /**
     * The folder in which the received files should be stored.
     */
    private String downloadsFolder;
    /**
     * Host name of the machine on which the client instance {@code cl} is running.
     */
    private String mHostName;

    /**
     * @param autoServerHostNames host names of servers to which the client should automatically connect if the server
     *                            is online and the client isn't connected to any other server
     */
    private ClientGUI(String autoServerHostNames) {
        clientList = new ArrayList<>();
        selectedFiles = new ArrayList<>();
        tm = new CustomTableModel(clientList);
        downloadsFolder = System.getProperty("user.home") + "/Downloads";
        fileList.setModel(new DefaultListModel());

        // Contains host names of servers to which the client should connect automatically
        File f = new File(autoServerHostNames);
        try {
            Scanner sc = new Scanner(f);
            List<String> hostNames = new ArrayList<>();
            while (sc.hasNextLine()) {
                String hName = sc.nextLine();
                if (!hName.isEmpty()) {
                    hostNames.add(hName);
                }
            }
            new Thread(new AutoServerConnector(hostNames)).start();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        // Start client
        startClientButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (cl == null) {
                    // Contains the host name of the previously selected server, if it exists
                    File f = new File(System.getProperty("java.io.tmpdir") + "/" + "__ServerHostName__.txt");
                    // The host name of the selected server
                    String ip = null;
                    int result = JOptionPane.NO_OPTION;
                    if (f.exists()) {
                        // A server was connected to previously
                        try {
                            Scanner sc = new Scanner(f);
                            ip = sc.nextLine();
                            sc.close();
                            result = JOptionPane.showConfirmDialog(rootPanel, "Use " + ip + " as the server host name?");
                        } catch (FileNotFoundException e1) {
                            e1.printStackTrace();
                        }
                    }
                    if (!f.exists() || result != JOptionPane.YES_OPTION) {
                        // The first time the client is being started or the client wants to connect to different server
                        ip = JOptionPane.showInputDialog("Enter the host name of the server");
                        if (ip != null) {
                            try {
                                PrintStream ps = new PrintStream(f);
                                ps.println(ip);
                                ps.close();
                            } catch (FileNotFoundException e1) {
                                e1.printStackTrace();
                            }
                        }
                    }
                    if (ip != null) {
                        try {
                            cl = new Client(ip);
                        } catch (IOException e1) {
                            JOptionPane.showMessageDialog(rootPanel, "Cannot connect to server");
                            e1.printStackTrace();
                        }
                    }
                } else {
                    JOptionPane.showMessageDialog(rootPanel, "You are already connected to " + cl.hostName);
                }
            }
        });
        // Get list of clients from the server
        getListButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (cl != null) {
                    cl.pw.println("getlist");
                    cl.pw.flush();
                } else {
                    JOptionPane.showMessageDialog(null, "Start the client first!");
                }
            }
        });
        clientListTable.setModel(tm);
        // Add a file to the list of files to send
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
                    selectedFiles.add(selectedFile);
                }
            }
        });
        // Send the selected list of files to the selected receivers
        sendButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int x = clientListTable.getSelectedRowCount();
                if (x != 0) {
                    if (selectedFiles.size() != 0) {
                        List<Person> selectedPeople = new ArrayList<>();
                        for (int n : clientListTable.getSelectedRows()) {
                            selectedPeople.add(clientList.get(n));
                        }
                        int result = JOptionPane.showConfirmDialog(null, "Send selected files to "
                                + selectedPeople + "?");
                        if (result == JOptionPane.YES_OPTION) {
                            try {
                                ServerSocket sersock = new ServerSocket(9600 + clientList.indexOf(new Person(null,
                                        mHostName)) + 1);
                                JOptionPane.showMessageDialog(null, "Transfer of selected files started");
                                Socket sc = sersock.accept();
                                //new Thread(new MiniServer(sc, selectedFiles, selectedPeople, sersock)).start();
                                selectedFiles = new ArrayList<>();
                                fileList.setModel(new DefaultListModel());
                            } catch (IOException e1) {
                                e1.printStackTrace();
                            }
                        }
                    } else {
                        JOptionPane.showMessageDialog(null, "Choose a file first!");
                    }
                } else {
                    JOptionPane.showMessageDialog(null, "Select the receiver(s) first!");
                }
            }
        });
        // Change the downloads folder
        downloadsFolderButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setCurrentDirectory(new File(downloadsFolder));
                fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                int result = fileChooser.showOpenDialog(rootPanel);
                if (result == JFileChooser.APPROVE_OPTION) {
                    downloadsFolder = fileChooser.getSelectedFile().getAbsolutePath();
                }
            }
        });
        // Delete the selected files from the list of files to send
        deleteFilesButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                DefaultListModel model = (DefaultListModel) fileList.getModel();
                int[] indices = fileList.getSelectedIndices();
                for (int i = indices.length - 1; i >= 0; i--) {
                    model.remove(indices[i]);
                    selectedFiles.remove(indices[i]);
                }
                fileList.setModel(model);
            }
        });
    }

    /**
     * @param args the program arguments (ignored)
     */
    public static void main(String[] args) {
        // Contains the host names of servers to automatically connect to, if it exists
        File f = new File(System.getProperty("java.io.tmpdir") + "/" + "__AutoServerHostNames__.txt");
        // Name (with absolute path) of the file containing host names of servers to automatically connect to
        String ip = null;
        int result = JOptionPane.NO_OPTION;
        if (f.exists()) {
            // A file was chosen previously
            try {
                Scanner sc = new Scanner(f);
                ip = sc.nextLine();
                sc.close();
                result = JOptionPane.showConfirmDialog(null, "Use " + ip + " as the automatic server host names file?");
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
        if (!f.exists() || result != JOptionPane.YES_OPTION) {
            // The first time the client is being started or a different file needs to be selected
            ip = null;
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setCurrentDirectory(new File(System.getProperty("user.home")));
            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            fileChooser.setDialogTitle("Choose automatic server host names file");
            result = fileChooser.showOpenDialog(null);
            if (result == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();
                ip = selectedFile.getAbsolutePath();
                try {
                    PrintStream ps = new PrintStream(f);
                    ps.println(ip);
                    ps.close();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
        if (ip != null) {
            JFrame frame = new JFrame("ClientGUI");
            frame.setContentPane(new ClientGUI(ip).rootPanel);
            frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            frame.pack();
            frame.setVisible(true);
        }
    }

    /**
     * The class responsible for reading the list of clients from the server and for reading files.
     */
    private class Client implements Runnable {
        private String hostName;
        /**
         * Used to read the list of clients sent by the server.
         */
        private BufferedReader brn;
        /**
         * Used to write a request to the server for the list of clients.
         */
        private PrintWriter pw;

        /**
         * @param ip the host name of the server
         * @throws IOException if one of the socket related operations fails
         */
        public Client(String ip) throws IOException {
            Socket sock = new Socket(ip, 9000);
            hostName = ip;
            mHostName = InetAddress.getLocalHost().getHostName();
            brn = new BufferedReader(new InputStreamReader(sock.getInputStream()));
            pw = new PrintWriter(sock.getOutputStream());
            Thread t = new Thread(this);
            t.start();
        }

        @Override
        public void run() {
            JOptionPane.showMessageDialog(rootPanel, "Connected to " + hostName);
            // Spawn a new thread for reading the list of files
            new ListReader(brn);
            while (true) {
                try {
                    // Socket for accepting files from the server
                    Socket sc = new Socket(hostName, 9600);
                    // Spawn a new thread for reading the files
                    new Thread(new MiniClient(sc, downloadsFolder)).start();
                    // To avoid memory- and network-hogging
                    Thread.sleep(4000);
                } catch (IOException e) {
                    JOptionPane.showMessageDialog(rootPanel, "Disconnected from " + hostName);
                    // Nullify the client instance and clear the list of clients
                    cl = null;
                    clientList.clear();
                    tm.fireTableDataChanged();
                    break;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * This class is responsible for reading the list of clients from the server.
     */
    private class ListReader implements Runnable {
        private BufferedReader brn;

        /**
         * @param brn used to read the list of clients sent by the server
         */
        ListReader(BufferedReader brn) {
            this.brn = brn;
            Thread t = new Thread(this);
            t.start();
        }

        @Override
        public void run() {
            try {
                while (true) {
                    String s = brn.readLine();
                    // If the server has sent the list of clients...
                    if (s != null && s.startsWith("SIZE")) {
                        // Clear the current list;
                        clientList.clear();
                        // Read the size of the list; and
                        StringTokenizer st = new StringTokenizer(s);
                        st.nextToken();
                        int size = Integer.parseInt(st.nextToken());
                        // Read the list of clients ;)
                        for (int i = 0; i < size; i++) {
                            String clientName = brn.readLine();
                            String clientHostName = brn.readLine();
                            clientList.add(new Person(clientName, clientHostName));
                            tm.fireTableDataChanged();
                        }
                    }
                }
            } catch (IOException e) {
                JOptionPane.showMessageDialog(null, "Main Server is not running");
                e.printStackTrace();
            }
        }
    }

    /**
     * This class is used to connect to some servers automatically, if the client is not connected already and the
     * server is online.
     */
    private class AutoServerConnector implements Runnable {
        private List<String> hostNames;

        /**
         * @param hostNames list of host names of servers to which the client should connect automatically
         */
        AutoServerConnector(List<String> hostNames) {
            this.hostNames = hostNames;
        }

        @Override
        public void run() {
            while (true) {
                // If the client is not connected already...
                if (cl == null) {
                    for (String hostName : hostNames) {
                        // Connect to the server, if possible
                        try {
                            cl = new Client(hostName);
                            break;
                        } catch (IOException e) {
                            // Do nothing.
                        }
                    }
                }
                try {
                    // To avoid memory- and network-hogging
                    Thread.sleep(4000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
