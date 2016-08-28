package inspire;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

public class ClientGUI {
    private JButton startClientButton;
    private JButton getListButton;
    private JTable clientListTable;
    private JButton chooseFileButton;
    private JButton sendButton;
    private JTextArea filePath;
    private JPanel rootPanel;
    private JButton downloadsFolderButton;

    private String ip;
    private List<Person> clientList;
    private Client cl;
    private CustomTableModel tm;
    private File selectedFile;
    private String downloadsFolder;
    private String mIp;

    private ClientGUI(String ip) {
        this.ip = ip;
        clientList = new ArrayList<>();
        tm = new CustomTableModel(clientList);
        downloadsFolder = System.getProperty("user.home") + "/Downloads";

        startClientButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cl = new Client(ClientGUI.this.ip);
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
                    filePath.append(selectedFile.getAbsolutePath() + "\n");
                }
            }
        });
        sendButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int n = clientListTable.getSelectedRow();
                if (n != -1) {
                    if (selectedFile != null) {
                        int result = JOptionPane.showConfirmDialog(null, "Send " + selectedFile + " to "
                                + clientList.get(n) + "?");
                        if (result == JOptionPane.YES_OPTION) {
                            try {
                                ServerSocket sersock = new ServerSocket(9600 + clientList.indexOf(new Person(null,
                                        mIp)) + 1);
                                JOptionPane.showMessageDialog(null, "Transfer of " + selectedFile + " started");
                                Socket sc = sersock.accept();
                                new Thread(new MiniServer(sc, selectedFile, clientList.get(n).getIp(),
                                        sersock)).start();
                            } catch (IOException e1) {
                                e1.printStackTrace();
                            }
                        }
                    } else {
                        JOptionPane.showMessageDialog(null, "Choose a file first!");
                    }
                } else {
                    JOptionPane.showMessageDialog(null, "Select the receiver first!");
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
                    downloadsFolder = fileChooser.getSelectedFile().getAbsolutePath();
                }
            }
        });
    }

    public static void main(String[] args) {
        String ip = JOptionPane.showInputDialog("Enter the IP address of the server");
        if (ip != null) {
            JFrame frame = new JFrame("ClientGUI");
            frame.setContentPane(new ClientGUI(ip).rootPanel);
            frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            frame.pack();
            frame.setVisible(true);
        }
    }

    private class Client implements Runnable {
        private BufferedReader brn;
        private PrintWriter pw;

        public Client(String ip) {
            try {
                Socket sock = new Socket(ip, 9000);
                mIp = sock.getLocalAddress().getHostAddress();
                brn = new BufferedReader(new InputStreamReader(sock.getInputStream()));
                pw = new PrintWriter(sock.getOutputStream());
                Thread t = new Thread(this);
                t.start();
            } catch (IOException e) {
                JOptionPane.showMessageDialog(null, "Cannot connect to server");
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            new ListReader(brn);
            while (true) {
                try {
                    Socket sc = new Socket(ip, 9600);
                    new MiniClient(sc, null, downloadsFolder);
                    Thread.sleep(4000);
                } catch (IOException e) {
                    // Do nothing.
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
                            String clientIp = brn.readLine();
                            clientList.add(new Person(clientName, clientIp));
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
}
