package inspire;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.*;

public class ServerModel extends Observable implements Runnable {
    public static final char FILE_RECEIVE_STARTED = '0';
    public static final char FILE_RECEIVE_FINISHED = '1';
    public static final char FILES_RECEIVED = '2';

    private List<Person> clientList;
    private String downloadsFolder;
    private ServerSocket serverSocket;
    private Thread serverThread;

    public ServerModel(List<Person> clientList) throws Exception {
        this.clientList = clientList;
        downloadsFolder = System.getProperty("java.io.tmpdir");
        serverSocket = new ServerSocket(9000);
        serverThread = new Thread(this);
        serverThread.start();
    }

    public void setDownloadsFolder(String downloadsFolder) {
        this.downloadsFolder = downloadsFolder;
    }

    public List<Person> getClientList() {
        return clientList;
    }

    private String getHostName(Socket sc) throws UnknownHostException {
        String hostName = sc.getInetAddress().getHostName();
        if (hostName.equals("localhost")) {
            hostName = InetAddress.getLocalHost().getHostName();
        }
        return hostName;
    }

    @Override
    public void run() {
        try {
            ServerSocket miniServerSocket = new ServerSocket(9600);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                Thread listPasser = new Thread(new ListPasser(
                        new BufferedReader(new InputStreamReader(clientSocket.getInputStream())),
                        new PrintWriter(clientSocket.getOutputStream())
                ));
                listPasser.start();
                Thread clientHandler = new Thread(new ClientHandler(clientSocket, miniServerSocket, downloadsFolder));
                clientHandler.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private class ClientHandler implements Runnable {
        private Socket clientSocket;
        private ServerSocket miniServerSocket;
        private String downloadsFolder;

        ClientHandler(Socket clientSocket, ServerSocket miniServerSocket, String downloadsFolder) {
            this.clientSocket = clientSocket;
            this.miniServerSocket = miniServerSocket;
            this.downloadsFolder = downloadsFolder;
        }

        private void send(Set<String> receiverList, List<File> fileList) {
            Thread handler = new Thread(new Runnable() {
                @Override
                public void run() {
                    while (receiverList.size() != 0) {
                        try {
                            Socket socket = miniServerSocket.accept();
                            String hostName = getHostName(socket);
                            if (receiverList.contains(hostName)) {
                                MiniServer miniserver = new MiniServer(socket, fileList, null, null);
                                Thread miniServerThread = new Thread(miniserver);
                                miniserver.addObserver(new Observer() {
                                    @Override
                                    public void update(Observable observable, Object o) {
                                        String action = (String) o;
                                        switch (action.charAt(0)) {
                                            case MiniServer.FILE_SEND_STARTED:
                                                break;
                                            case MiniServer.FILE_SEND_FINISHED:
                                                break;
                                            case MiniServer.FILES_SENT:
                                                receiverList.remove(action.substring(2));
                                                break;
                                        }
                                    }
                                });
                                miniServerThread.start();
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
            handler.start();
        }

        @Override
        public void run() {
            while (true) {
                try {
                    String hostName = getHostName(clientSocket);
                    Socket socket = new Socket(hostName, 9600 + clientList.indexOf(new Person(null, hostName)) + 1);
                    List<String> receiverList = new ArrayList<>();
                    List<File> fileList = new ArrayList<>();
                    MiniClient miniClient = new MiniClient(socket, receiverList, downloadsFolder);
                    Thread miniClientThread = new Thread(miniClient);
                    miniClient.addObserver(new Observer() {
                        @Override
                        public void update(Observable observable, Object o) {
                            String action = (String) o;
                            switch (action.charAt(0)) {
                                case MiniClient.FILE_RECEIVE_STARTED:
                                    setChanged();
                                    notifyObservers(String.valueOf(ServerModel.FILE_RECEIVE_STARTED) + " " + action.substring(2));
                                    break;
                                case MiniClient.FILE_RECEIVE_FINISHED:
                                    fileList.add(new File(downloadsFolder + "/" + action.substring(2)));
                                    setChanged();
                                    notifyObservers(String.valueOf(ServerModel.FILE_RECEIVE_FINISHED) + " " + action.substring(2));
                                    break;
                                case MiniClient.FILES_RECEIVED:
                                    send(new HashSet<String>(receiverList), fileList);
                                    setChanged();
                                    notifyObservers(String.valueOf(ServerModel.FILES_RECEIVED) + " " + hostName);
                                    break;
                            }
                        }
                    });
                    miniClientThread.start();
                    Thread.sleep(4000);
                } catch (IOException e) {

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private class ListPasser implements Runnable {
        private BufferedReader bufferedReader;
        private PrintWriter printWriter;

        ListPasser(BufferedReader bufferedReader, PrintWriter printWriter) {
            this.bufferedReader = bufferedReader;
            this.printWriter = printWriter;
        }

        @Override
        public void run() {
            try {
                while (true) {
                    String string = bufferedReader.readLine();
                    if (string != null && string.startsWith("getlist")) {
                        printWriter.println("SIZE " + clientList.size());
                        for (Person client : clientList) {
                            printWriter.println(client.getName());
                            printWriter.println(client.getHostName());
                        }
                        printWriter.flush();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
