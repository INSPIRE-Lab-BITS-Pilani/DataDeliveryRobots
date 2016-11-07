package inspire;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The top-level server class including GUI elements.
 */
public class ServerGUI {
    /**
     * Map from client host names to the names of files that need to be transferred to them.
     */
    private final Map<String, Queue<String>> clientFileListMap;
    private JPanel rootPanel;
    private JButton startServerButton;
    private JButton downloadsFolderButton;
    private JTable connectedTable;
    /**
     * The list of clients.
     */
    private List<Person> clientList;
    /**
     * {@code TableModel} implementation to view {@code clientList}.
     */
    private CustomTableModel tm;
    /**
     * The folder in which the received files should be stored.
     */
    private String downloadsFolder;

    private ServerGUI() {
        clientList = new ArrayList<>();
        tm = new CustomTableModel(clientList);
        downloadsFolder = System.getProperty("java.io.tmpdir");
        clientFileListMap = new HashMap<>();

        // Start server
        startServerButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    // Contains the name of the previously selected clientListFile, if it exists
                    File f = new File(System.getProperty("java.io.tmpdir") + "/" + "__ClientListFile__.txt");
                    // Contains the list of clients
                    File clientListFile;
                    int result = JOptionPane.NO_OPTION;
                    if (f.exists()) {
                        // A clientListFile was selected previously
                        Scanner sc = new Scanner(f);
                        clientListFile = new File(sc.nextLine());
                        sc.close();
                        result = JOptionPane.showConfirmDialog(null, "Use " + clientListFile + " as the client list file?");
                        if (result == JOptionPane.YES_OPTION) {
                            ServerGUI.this.populateClientList(clientListFile);
                            new Server();
                        }
                    }
                    if (!f.exists() || result != JOptionPane.YES_OPTION) {
                        // This is the first time the server is being started or the clientListFile has changed
                        JFileChooser fileChooser = new JFileChooser();
                        fileChooser.setCurrentDirectory(new File(System.getProperty("user.home")));
                        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
                        fileChooser.setDialogTitle("Choose client list file");
                        result = fileChooser.showOpenDialog(rootPanel);
                        if (result == JFileChooser.APPROVE_OPTION) {
                            clientListFile = fileChooser.getSelectedFile();
                            PrintStream ps = new PrintStream(f);
                            ps.println(clientListFile.getAbsolutePath());
                            ps.close();
                            ServerGUI.this.populateClientList(clientListFile);
                            new Server();
                        }
                    }
                } catch (FileNotFoundException e1) {
                    e1.printStackTrace();
                }
            }
        });
        connectedTable.setModel(tm);
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
    }

    /**
     * @param args the program arguments (ignored)
     */
    public static void main(String[] args) {
        JFrame frame = new JFrame("ServerGUI");
        frame.setContentPane(new ServerGUI().rootPanel);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }

    /**
     * @param clientListFile file containing the list of clients (each line in the file represents a unique client, with
     *                       a human-readable identifier and the host name separated by spaces)
     * @throws FileNotFoundException if {@code clientListFile} is not found
     */
    private void populateClientList(File clientListFile) throws FileNotFoundException {
        Scanner sc = new Scanner(clientListFile);
        while (sc.hasNextLine()) {
            StringTokenizer st = new StringTokenizer(sc.nextLine());
            String name = st.nextToken();
            String hostName = st.nextToken();
            clientList.add(new Person(name, hostName));
        }
        tm.fireTableDataChanged();
    }

    /**
     * @param sc the socket bound to the machine whose host name should be returned
     * @return the actual host name of the machine to which the socket is bound (localhost is replaced by the canonical
     * host name of the machine)
     * @throws UnknownHostException if {@link InetAddress#getLocalHost()} fails
     */
    private String getHostName(Socket sc) throws UnknownHostException {
        String hostName = sc.getInetAddress().getHostName();
        if (hostName.equals("localhost")) {
            hostName = InetAddress.getLocalHost().getHostName();
        }
        return hostName;
    }

    /**
     * The multi-threaded Server application class. It delegates each client connection to its own separate thread.
     */
    private class Server implements Runnable {
        /**
         * The socket to which the clients must connect.
         */
        private ServerSocket serverSocket;

        public Server() {
            try {
                serverSocket = new ServerSocket(9000);
                Thread t = new Thread(this);
                t.start();
            } catch (IOException e) {
                JOptionPane.showMessageDialog(null, "Cannot start the server");
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            try {
                // The socket on the server side to which clients connect for getting files from the server
                ServerSocket miniServerSocket = new ServerSocket(9600);
                while (true) {
                    Socket clientSocket = serverSocket.accept();
                    // Spawn a new thread for each client connection
                    new Thread(new WorkerRunnable(clientSocket, miniServerSocket)).start();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * The class handling one client connection. It is responsible for sending files to and receiving files from that
     * client.
     */
    private class WorkerRunnable implements Runnable {
        private Socket clientSocket;
        private ServerSocket serverSocket;

        /**
         * @param clientSocket     the socket through which the client transfers files to the server
         * @param miniServerSocket the socket through which the server transfers files to the client
         */
        WorkerRunnable(Socket clientSocket, ServerSocket miniServerSocket) {
            this.clientSocket = clientSocket;
            this.serverSocket = miniServerSocket;
        }

        @Override
        public void run() {
            try {
                // Used to read whether the client requires the list of clients to be sent
                BufferedReader brn = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                // Used to write the list of clients, if requested
                PrintWriter pw = new PrintWriter(clientSocket.getOutputStream());

                // Spawn a new thread which checks whether the client has requested the list of clients and sends them
                new ListPasser(brn, pw);
                // Map from a MiniServer instance to a map from the host name of client to the files transferred to it
                Map<MiniServer, Map<String, Queue<File>>> threadMap = new ConcurrentHashMap<>();
                // Spawn a new thread for handling file transfers to the client
                new MiniServerHandler(serverSocket, threadMap);
                while (true) {
                    for (Iterator<Map.Entry<MiniServer, Map<String, Queue<File>>>> it = threadMap.entrySet().iterator();
                         it.hasNext(); ) {
                        Map.Entry<MiniServer, Map<String, Queue<File>>> entry = it.next();
                        Thread thread = entry.getKey().reqHandlerThread;
                        // If the thread has completed execution...
                        if (thread != null && thread.getState() == Thread.State.TERMINATED) {
                            for (String sockHostName : entry.getValue().keySet()) {
                                for (File f : entry.getValue().get(sockHostName)) {
                                    // Remove the file from the list of files to be sent to the client;
                                    clientFileListMap.get(sockHostName).remove(f.getName());
                                    // Delete the file, if possible; and
                                    boolean flag = true;
                                    for (Queue<String> q : clientFileListMap.values()) {
                                        if (q.contains(f.getName())) {
                                            flag = false;
                                            break;
                                        }
                                    }
                                    if (flag) {
                                        f.delete();
                                    }
                                }
                                // Remove the client from the set of clients that still need to be sent files
                                synchronized (clientFileListMap) {
                                    clientFileListMap.remove(sockHostName);
                                }
                            }
                            // Also, the entry can be removed from threadMap
                            it.remove();
                        }
                    }
                    try {
                        // Get the host name of the client from its socket
                        String hostName = getHostName(clientSocket);
                        // The port number is unique to each client based on its position in the clientList
                        Socket sc = new Socket(hostName, 9600 + clientList.indexOf(new Person(null, hostName)) + 1);
                        // Spawn a new thread for receiving files from the client
                        //new Thread(new MiniClient(sc, clientFileListMap, downloadsFolder)).start();
                        // To avoid memory- and network-hogging
                        Thread.sleep(4000);
                    } catch (IOException e) {
                        // Do nothing.
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private class MiniServerHandler implements Runnable {
            ServerSocket serverSocket;
            Map<MiniServer, Map<String, Queue<File>>> threadMap;

            /**
             * @param serverSocket the socket through which the server transfers files to the client
             * @param threadMap    map from a MiniServer instance to a map from the host name of client to the files
             *                     transferred to it
             */
            MiniServerHandler(ServerSocket serverSocket, Map<MiniServer, Map<String, Queue<File>>> threadMap) {
                this.serverSocket = serverSocket;
                this.threadMap = threadMap;
                Thread t = new Thread(this);
                t.start();
            }

            @Override
            public void run() {
                try {
                    while (true) {
                        // Accept the client connection
                        Socket sc = serverSocket.accept();
                        // Get host name of the client
                        String hostName = getHostName(sc);
                        // If there are files that need to be transferred...
                        if (clientFileListMap.containsKey(hostName)) {
                            // List of files to be transferred to the client
                            List<File> tempFileList = new ArrayList<>();
                            // The MiniServer instance created for this transfer
                            MiniServer ms;
                            // See tempFileList
                            Queue<File> q = new LinkedList<>();
                            for (String filename : clientFileListMap.get(hostName)) {
                                File f = new File(downloadsFolder + "/" + filename);
                                tempFileList.add(f);
                                q.add(f);
                            }
                            // Spawn a new thread for transferring the files from the server to the client
                            Thread t = new Thread(ms = new MiniServer(sc, tempFileList, null, null));
                            t.start();
                            // Create a map from the client hostname to the list of files that need to be transferred
                            Map<String, Queue<File>> fileMap = Collections.singletonMap(hostName, q);
                            // Put an entry from the MiniServer instance to the map created above in threadMap
                            threadMap.put(ms, fileMap);
                            // To avoid memory- and network-hogging
                            Thread.sleep(4000);
                        } else {
                            sc.close();
                        }
                    }
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * This class is responsible for passing the list of clients from the server to a requesting client.
     */
    private class ListPasser implements Runnable {
        private BufferedReader brn;
        private PrintWriter pw;

        /**
         * @param brn used to read whether the client requires the list of clients to be sent
         * @param pw  used to write the list of clients, if requested
         */
        ListPasser(BufferedReader brn, PrintWriter pw) {
            this.brn = brn;
            this.pw = pw;
            Thread t = new Thread(this);
            t.start();
        }

        @Override
        public void run() {
            try {
                while (true) {
                    String s = brn.readLine();
                    // If the client has requested the list...
                    if (s != null && s.startsWith("getlist")) {
                        // Send the list ;)
                        pw.println("SIZE " + clientList.size());
                        for (Person client : clientList) {
                            pw.println(client.getName());
                            pw.println(client.getHostName());
                        }
                        pw.flush();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
