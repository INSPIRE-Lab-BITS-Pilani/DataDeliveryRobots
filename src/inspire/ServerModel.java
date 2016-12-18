package inspire;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The actual server class which is responsible for performing
 * all the functions of the application. It forms the model part
 * of the MVC design and is unaware of the existence of any
 * controller or view.
 *
 * @author Abhinav Baid, Atishay Jain
 * @version 1.0
 * @see ServerView
 * @see ServerController
 * @since 20-12-2016
 */
class ServerModel extends Observable implements Runnable {
    /**
     * Notifies start of a file receive
     */
    static final char FILE_RECEIVE_STARTED = '0';
    /**
     * Notifies end of a file receive
     */
    static final char FILE_RECEIVE_FINISHED = '1';
    /**
     * Notifies end of a receive sequence
     */
    static final char FILES_RECEIVED = '2';
    /**
     * Notifies start of a file transfer
     */
    static final char FILE_SEND_STARTED = '3';
    /**
     * Notifies end of a file transfer
     */
    static final char FILE_SEND_FINISHED = '4';
    /**
     * Notifies end of a transfer sequence
     */
    static final char FILES_SENT = '5';
    /**
     * Notifies connection to a client
     */
    static final char CLIENT_CONNECTED = '6';
    /**
     * Port to connect to a client
     */
    private static final int controlPort = 9000;
    /**
     * Port to transfer/receive files
     */
    private static final int dataPort = 9600;
    /**
     * List of clients
     */
    private List<Person> clientList;
    /**
     * Map from client host name to set of pending files
     */
    private Map<String, Set<File>> clientFileMap;
    /**
     * Folder to temporarily store received files
     */
    private String downloadsFolder;
    /**
     * Server socket for {@code controlPort}
     */
    private ServerSocket serverSocket;
    /**
     * Server socket for {@code dataPort}
     */
    private ServerSocket miniServerSocket;

    /**
     * Starts a server
     *
     * @param clientList      List of clients
     * @param downloadsFolder Folder to temporarily store received files
     */
    ServerModel(List<Person> clientList, String downloadsFolder) {
        // Initialise server parameters
        try {
            this.clientList = clientList;
            this.clientFileMap = new ConcurrentHashMap<>();
            for (Person client : clientList) {
                clientFileMap.put(client.getHostName(), Collections.synchronizedSet(new HashSet<>()));
            }
            this.downloadsFolder = downloadsFolder;
            this.serverSocket = new ServerSocket(controlPort);
            Thread serverThread = new Thread(this);
            serverThread.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Get client list
     *
     * @return Client list
     */
    public List<Person> getClientList() {
        return clientList;
    }

    /**
     * Set folder to temporarily store files
     *
     * @param downloadsFolder Folder to temporarily store files
     */
    void setDownloadsFolder(String downloadsFolder) {
        this.downloadsFolder = downloadsFolder;
    }

    /**
     * Returns the host name of the other side of the socket
     *
     * @param socket Socket to get the host name of
     * @return Host name of the socket's other side
     * @throws UnknownHostException If socket host name is not found
     */
    private String getHostName(Socket socket) throws UnknownHostException {
        String hostName = socket.getInetAddress().getHostName();
        if (hostName.equals("localhost")) {
            hostName = InetAddress.getLocalHost().getHostName();
        }
        return hostName;
    }

    /**
     * Spawn a server thread
     */
    @Override
    public void run() {
        try {
            miniServerSocket = new ServerSocket(dataPort);
            // Keep trying to accept client connections
            while (true) {
                Socket clientSocket = serverSocket.accept();
                setChanged();
                notifyObservers(String.valueOf(CLIENT_CONNECTED) + " " + getHostName(clientSocket));
                // Thread to pass client list, if requested
                Thread listPasser = new Thread(new ListPasser(
                        new BufferedReader(new InputStreamReader(clientSocket.getInputStream())),
                        new PrintWriter(clientSocket.getOutputStream())
                ));
                listPasser.start();
                // Thread to receive files from the client
                Thread clientHandler = new Thread(new ClientHandler(clientSocket));
                clientHandler.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * This class handles a single client after its connection
     * has been accepted.
     *
     * @author Abhinav Baid, Atishay Jain
     * @version 1.0
     * @since 20-12-2016
     */
    private class ClientHandler implements Runnable {
        /**
         * Client socket after accepting connection
         */
        private Socket clientSocket;
        /**
         * Host name of the client
         */
        private String hostName;

        /**
         * Initialise client handler
         *
         * @param clientSocket Client socket
         */
        ClientHandler(Socket clientSocket) {
            try {
                this.clientSocket = clientSocket;
                this.hostName = getHostName(this.clientSocket);
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
        }

        /**
         * Spawn a thread for handling the particular client requests
         */
        @Override
        public void run() {
            while (true) {
                try {
                    // Try to connect to the client
                    Socket socket = new Socket(hostName, dataPort + clientList.indexOf(new Person(null, hostName)) + 1);
                    // If accepted, the client wants to send files
                    final List<String> receiverList = new ArrayList<>();
                    final List<File> fileList = new ArrayList<>();
                    // A thread to receive the files
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
                                    // Start sending the files to the receivers
                                    // mentioned by the client
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
                    miniClientThread.join();
                } catch (IOException | InterruptedException e) {
                    try {
                        Thread.sleep(4000);
                    } catch (InterruptedException e1) {
                        e1.printStackTrace();
                    }
                }
            }
        }

        /**
         * Send files to the receivers
         *
         * @param receiverList List of receivers
         * @param fileList     List of files to be transferred
         */
        private void send(final Set<String> receiverList, final List<File> fileList) {
            Thread handler = new Thread(new Runnable() {
                @Override
                public void run() {
                    // Till no more receivers are left
                    while (receiverList.size() != 0) {
                        try {
                            // Accept client and see if it is one of the receivers
                            Socket socket = miniServerSocket.accept();
                            final String hostName = getHostName(socket);
                            // Client is one of the receivers
                            if (receiverList.contains(hostName)) {
                                // A thread to send the files to the receiver
                                MiniServer miniserver = new MiniServer(socket, fileList, null, null, true);
                                Thread miniServerThread = new Thread(miniserver);
                                miniserver.addObserver(new Observer() {
                                    @Override
                                    public void update(Observable observable, Object o) {
                                        String action = (String) o;
                                        switch (action.charAt(0)) {
                                            case MiniServer.FILE_SEND_STARTED:
                                                setChanged();
                                                notifyObservers(String.valueOf(FILE_SEND_STARTED) + " "
                                                        + action.substring(2));
                                                break;
                                            case MiniServer.FILE_SEND_FINISHED:
                                                setChanged();
                                                notifyObservers(String.valueOf(FILE_SEND_FINISHED) + " "
                                                        + action.substring(2));
                                                break;
                                            case MiniServer.FILES_SENT:
                                                clientFileMap.get(hostName).removeAll(fileList);
                                                setChanged();
                                                notifyObservers(String.valueOf(FILES_SENT) + " "
                                                        + action.substring(2));
                                                break;
                                        }
                                    }
                                });
                                receiverList.remove(hostName);
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

    /**
     * Class to pass the client list to a client if it requests
     *
     * @author Abhinav Baid, Atishay Jain
     * @version 1.0
     * @since 20-12-2016
     */
    private class ListPasser implements Runnable {
        /**
         * Input stream of the socket
         */
        private final BufferedReader bufferedReader;
        /**
         * Output stream of the socket
         */
        private final PrintWriter printWriter;

        /**
         * Create a new list passing thread instance
         *
         * @param bufferedReader Input stream of the socket
         * @param printWriter    Output stream of the socket
         */
        ListPasser(BufferedReader bufferedReader, PrintWriter printWriter) {
            this.bufferedReader = bufferedReader;
            this.printWriter = printWriter;
        }

        /**
         * Spawn a thread for passing client list if requested
         */
        @Override
        public void run() {
            while (true) {
                try {
                    // Client list requested
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