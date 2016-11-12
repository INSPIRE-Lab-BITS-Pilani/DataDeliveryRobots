package inspire;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ServerModel extends Observable implements Runnable {
    public static final int controlPort = 9000;
    public static final int dataPort = 9600;
    public static final char FILE_RECEIVE_STARTED = '0';
    public static final char FILE_RECEIVE_FINISHED = '1';
    public static final char FILES_RECEIVED = '2';
    public static final char FILE_SEND_STARTED = '3';
    public static final char FILE_SEND_FINISHED = '4';
    public static final char FILES_SENT = '5';
    public static final char CLIENT_CONNECTED = '6';

    private List<Person> clientList;
    private Map<String, Set<File>> clientFileMap;
    private String downloadsFolder;
    private ServerSocket serverSocket;
    private ServerSocket miniServerSocket;

    public ServerModel(List<Person> clientList, String downloadsFolder) {
        try {
            this.clientList = clientList;
            this.clientFileMap = new ConcurrentHashMap<>();
            this.downloadsFolder = downloadsFolder;
            this.serverSocket = new ServerSocket(controlPort);
            Thread serverThread = new Thread(this);
            serverThread.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<Person> getClientList() {
        return clientList;
    }

    public void setDownloadsFolder(String downloadsFolder) {
        this.downloadsFolder = downloadsFolder;
    }

    private String getHostName(Socket socket) throws UnknownHostException {
        String hostName = socket.getInetAddress().getHostName();
        if (hostName.equals("localhost")) {
            hostName = InetAddress.getLocalHost().getHostName();
        }
        return hostName;
    }

    @Override
    public void run() {
        try {
            miniServerSocket = new ServerSocket(dataPort);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                setChanged();
                notifyObservers(String.valueOf(CLIENT_CONNECTED) + " " + getHostName(clientSocket));
                clientFileMap.put(getHostName(clientSocket), Collections.synchronizedSet(new HashSet<>()));
                Thread listPasser = new Thread(new ListPasser(
                        new BufferedReader(new InputStreamReader(clientSocket.getInputStream())),
                        new PrintWriter(clientSocket.getOutputStream())
                ));
                listPasser.start();
                Thread clientHandler = new Thread(new ClientHandler(clientSocket));
                clientHandler.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private class ClientHandler implements Runnable {
        private Socket clientSocket;
        private String hostName;

        ClientHandler(Socket clientSocket) {
            try {
                this.clientSocket = clientSocket;
                this.hostName = getHostName(this.clientSocket);
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            while (true) {
                try {
                    Socket socket = new Socket(hostName, dataPort + clientList.indexOf(new Person(null, hostName)) + 1);
                    List<String> receiverList = new ArrayList<>();
                    List<File> fileList = new ArrayList<>();
                    MiniClient miniClient = new MiniClient(socket, downloadsFolder);
                    Thread miniClientThread = new Thread(miniClient);
                    miniClient.addObserver(new Observer() {
                        @Override
                        public void update(Observable observable, Object o) {
                            String action = (String) o;
                            switch (action.charAt(0)) {
                                case MiniClient.RECEIVER_ADDED:
                                    receiverList.add(action.substring(2));
                                    break;
                                case MiniClient.FILE_RECEIVE_STARTED:
                                    setChanged();
                                    notifyObservers(String.valueOf(FILE_RECEIVE_STARTED) + " " + action.substring(2));
                                    break;
                                case MiniClient.FILE_RECEIVE_FINISHED:
                                    fileList.add(new File(downloadsFolder + "/" + action.substring(2)));
                                    setChanged();
                                    notifyObservers(String.valueOf(FILE_RECEIVE_FINISHED) + " " + action.substring(2));
                                    break;
                                case MiniClient.FILES_RECEIVED:
                                    for (String hostName : receiverList) {
                                        clientFileMap.get(hostName).addAll(fileList);
                                    }
                                    send(new HashSet<>(receiverList), fileList);
                                    setChanged();
                                    notifyObservers(String.valueOf(FILES_RECEIVED) + " " + hostName);
                                    break;
                            }
                        }
                    });
                    miniClientThread.start();
                    Thread.sleep(4000);
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
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
                                MiniServer miniserver = new MiniServer(socket, fileList, null, null, true);
                                Thread miniServerThread = new Thread(miniserver);
                                miniserver.addObserver(new Observer() {
                                    @Override
                                    public void update(Observable observable, Object o) {
                                        String action = (String) o;
                                        switch (action.charAt(0)) {
                                            case MiniServer.FILE_SEND_STARTED:
                                                setChanged();
                                                notifyObservers(String.valueOf(FILE_SEND_STARTED) + " " + action.substring(2));
                                                break;
                                            case MiniServer.FILE_SEND_FINISHED:
                                                setChanged();
                                                notifyObservers(String.valueOf(FILE_SEND_FINISHED) + " " + action.substring(2));
                                                break;
                                            case MiniServer.FILES_SENT:
                                                clientFileMap.get(hostName).removeAll(fileList);
                                                receiverList.remove(action.substring(2));
                                                setChanged();
                                                notifyObservers(String.valueOf(FILES_SENT) + " " + action.substring(2));
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
            while (true) {
                try {
                    String string = bufferedReader.readLine();
                    if (string != null && string.startsWith("getlist")) {
                        printWriter.println("SIZE " + clientList.size());
                        for (Person client : clientList) {
                            printWriter.println(client.getName());
                            printWriter.println(client.getHostName());
                        }
                        printWriter.flush();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
