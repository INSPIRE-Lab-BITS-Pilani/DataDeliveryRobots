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
    private JPanel rootPanel;
    private JButton startServerButton;
    private JButton downloadsFolderButton;
    private JTable connectedTable;

    private List<Person> clientList;
    private CustomTableModel tm;
    private String downloadsFolder;
    private final Map<String, Queue<String>> clientFileListMap;

    private ServerGUI() {
        clientList = new ArrayList<>();
        tm = new CustomTableModel(clientList);
        downloadsFolder = System.getProperty("java.io.tmpdir");
        clientFileListMap = new HashMap<>();

        startServerButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    File f = new File(System.getProperty("java.io.tmpdir") + "/" + "__ClientListFile__.txt");
                    File clientListFile;
                    int result = JOptionPane.NO_OPTION;
                    if (f.exists()) {
                        Scanner sc = new Scanner(f);
                        clientListFile = new File(sc.nextLine());
                        sc.close();
                        result = JOptionPane.showConfirmDialog(null, "Use " + clientListFile + " as the client list" +
                                " file?");
                        if (result == JOptionPane.YES_OPTION) {
                            ServerGUI.this.populateClientList(clientListFile);
                            new Server();
                        }
                    }
                    if (!f.exists() || result != JOptionPane.YES_OPTION) {
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
     * @param args the program arguments (ignored)
     */
    public static void main(String[] args) {
        JFrame frame = new JFrame("ServerGUI");
        frame.setContentPane(new ServerGUI().rootPanel);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }

    private class Server implements Runnable {
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
                ServerSocket miniServerSocket = new ServerSocket(9600);
                while (true) {
                    Socket clientSocket = serverSocket.accept();
                    new Thread(new WorkerRunnable(clientSocket, miniServerSocket)).start();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private class WorkerRunnable implements Runnable {
        private Socket clientSocket;
        private ServerSocket serverSocket;

        WorkerRunnable(Socket clientSocket, ServerSocket miniServerSocket) {
            this.clientSocket = clientSocket;
            this.serverSocket = miniServerSocket;
        }

        @Override
        public void run() {
            try {
                BufferedReader brn = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                PrintWriter pw = new PrintWriter(clientSocket.getOutputStream());

                new ListPasser(brn, pw);
                Map<MiniServer, Map<String, Queue<File>>> threadMap = new ConcurrentHashMap<>();
                new MiniServerHandler(serverSocket, threadMap);
                while (true) {
                    for (Iterator<Map.Entry<MiniServer, Map<String, Queue<File>>>> it = threadMap.entrySet().iterator();
                         it.hasNext(); ) {
                        Map.Entry<MiniServer, Map<String, Queue<File>>> entry = it.next();
                        Thread thread = entry.getKey().reqHandlerThread;
                        if (thread != null && thread.getState() == Thread.State.TERMINATED) {
                            for (String sockHostName : entry.getValue().keySet()) {
                                for (File f : entry.getValue().get(sockHostName)) {
                                    clientFileListMap.get(sockHostName).remove(f.getName());
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
                                synchronized (clientFileListMap) {
                                    clientFileListMap.remove(sockHostName);
                                }
                            }
                            it.remove();
                        }
                    }
                    try {
                        String hostName = getHostName(clientSocket);
                        Socket sc = new Socket(hostName, 9600 + clientList.indexOf(new Person(null, hostName)) + 1);
                        new MiniClient(sc, clientFileListMap, downloadsFolder);
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
                        Socket sc = serverSocket.accept();
                        String hostName = getHostName(sc);
                        if (clientFileListMap.containsKey(hostName)) {
                            List<File> tempFileList = new ArrayList<>();
                            MiniServer ms;
                            Queue<File> q = new LinkedList<>();
                            for (String filename : clientFileListMap.get(hostName)) {
                                File f = new File(downloadsFolder + "/" + filename);
                                tempFileList.add(f);
                                q.add(f);
                            }
                            Thread t = new Thread(ms = new MiniServer(sc, tempFileList, null, null));
                            t.start();
                            Map<String, Queue<File>> fileMap = Collections.singletonMap(hostName, q);
                            threadMap.put(ms, fileMap);
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

    private String getHostName(Socket sc) throws UnknownHostException {
        String hostName = sc.getInetAddress().getHostName();
        if (hostName.equals("localhost")) {
            hostName = InetAddress.getLocalHost().getHostName();
        }
        return hostName;
    }

    private class ListPasser implements Runnable {
        private BufferedReader brn;
        private PrintWriter pw;

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
                    if (s != null && s.startsWith("getlist")) {
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
