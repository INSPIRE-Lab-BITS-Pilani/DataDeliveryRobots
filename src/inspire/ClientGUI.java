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

    private List<Person> clientList;
    private Client cl;
    private CustomTableModel tm;
    private File selectedFile;
    private List<File> selectedFiles;
    private String downloadsFolder;
    private String mHostName;

    private ClientGUI(String autoServerHostNames) {
        clientList = new ArrayList<>();
        selectedFiles = new ArrayList<>();
        tm = new CustomTableModel(clientList);
        downloadsFolder = System.getProperty("user.home") + "/Downloads";
        fileList.setModel(new DefaultListModel());

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

        startClientButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (cl == null) {
                    File f = new File(System.getProperty("java.io.tmpdir") + "/" + "__ServerHostName__.txt");
                    String ip = null;
                    int result = JOptionPane.NO_OPTION;
                    if (f.exists()) {
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
        chooseFileButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setCurrentDirectory(new File(System.getProperty("user.home")));
                fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
                int result = fileChooser.showOpenDialog(rootPanel);
                if (result == JFileChooser.APPROVE_OPTION) {
                    selectedFile = fileChooser.getSelectedFile();
                    DefaultListModel model = (DefaultListModel) fileList.getModel();
                    model.addElement(selectedFile.getAbsolutePath());
                    fileList.setModel(model);
                    selectedFiles.add(selectedFile);
                }
            }
        });
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
                                new Thread(new MiniServer(sc, selectedFiles, selectedPeople, sersock)).start();
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

    public static void main(String[] args) {
        File f = new File(System.getProperty("java.io.tmpdir") + "/" + "__AutoServerHostNames__.txt");
        String ip = null;
        int result = JOptionPane.NO_OPTION;
        if (f.exists()) {
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

    private class Client implements Runnable {
        private String hostName;
        private BufferedReader brn;
        private PrintWriter pw;

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
            new ListReader(brn);
            while (true) {
                try {
                    Socket sc = new Socket(hostName, 9600);
                    new MiniClient(sc, null, downloadsFolder);
                    Thread.sleep(4000);
                } catch (IOException e) {
                    JOptionPane.showMessageDialog(rootPanel, "Disconnected from " + hostName);
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

    private class ListReader implements Runnable {
        private BufferedReader brn;

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
                    if (s != null && s.startsWith("SIZE")) {
                        clientList.clear();
                        StringTokenizer st = new StringTokenizer(s);
                        st.nextToken();
                        int size = Integer.parseInt(st.nextToken());
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

    private class AutoServerConnector implements Runnable {
        private List<String> hostNames;

        AutoServerConnector(List<String> hostNames) {
            this.hostNames = hostNames;
        }

        @Override
        public void run() {
            while (true) {
                if (cl == null) {
                    for (String hostName : hostNames) {
                        try {
                            cl = new Client(hostName);
                            break;
                        } catch (IOException e) {
                            // Do nothing.
                        }
                    }
                }
                try {
                    Thread.sleep(4000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
