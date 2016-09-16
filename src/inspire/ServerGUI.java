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

public class ServerGUI {
    private JPanel rootPanel;
    private JButton startServerButton;
    private JButton downloadsFolderButton;
    private JTable connectedTable;

    private List<Person> clientList;
    private CustomTableModel tm;
    private String downloadsFolder;
    private Map<String, Queue<String>> clientFileListMap;

    private ServerGUI() {
        clientList = new ArrayList<>();
        tm = new CustomTableModel(clientList);
        downloadsFolder = System.getProperty("java.io.tmpdir");
        clientFileListMap = new HashMap<>();

        startServerButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    JFileChooser fileChooser = new JFileChooser();
                    fileChooser.setCurrentDirectory(new File(System.getProperty("user.home")));
                    fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
                    fileChooser.setDialogTitle("Choose client list file");
                    int result = fileChooser.showOpenDialog(rootPanel);
                    if (result == JFileChooser.APPROVE_OPTION) {
                        File clientListFile = fileChooser.getSelectedFile();
                        ServerGUI.this.populateClientList(clientListFile);
                        new Server();
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
                Map<MiniServer, Map<String, File>> threadMap = new ConcurrentHashMap<>();
                new MiniServerHandler(serverSocket, threadMap);
                while (true) {
                    for (Iterator<Map.Entry<MiniServer, Map<String, File>>> it = threadMap.entrySet().iterator();
                         it.hasNext(); ) {
                        Map.Entry<MiniServer, Map<String, File>> entry = it.next();
                        Thread thread = entry.getKey().reqHandlerThread;
                        if (thread != null && thread.getState() == Thread.State.TERMINATED) {
                            for (String sockHostName : entry.getValue().keySet()) {
                                clientFileListMap.get(sockHostName).remove(entry.getValue().get(sockHostName).getName());
                                entry.getValue().get(sockHostName).delete();
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
            Map<MiniServer, Map<String, File>> threadMap;

            MiniServerHandler(ServerSocket serverSocket, Map<MiniServer, Map<String, File>> threadMap) {
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
                            for (String filename : clientFileListMap.get(hostName)) {
                                File f = new File(downloadsFolder + "/" + filename);
                                MiniServer ms;
                                Thread t = new Thread(ms = new MiniServer(sc, f, null, null));
                                t.start();
                                Map<String, File> fileMap = Collections.singletonMap(hostName, f);
                                threadMap.put(ms, fileMap);
                            }
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
