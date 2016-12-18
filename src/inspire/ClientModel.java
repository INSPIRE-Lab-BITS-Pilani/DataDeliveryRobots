package inspire;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.*;

/**
 * The actual client class which is responsible for performing
 * all the functions of the application. It forms the model part
 * of the MVC design and is unaware of the existence of any
 * controller or view.
 *
 * @author Abhinav Baid, Atishay Jain
 * @version 1.0
 * @see ClientView
 * @see ClientController
 * @since 20-12-2016
 */
class ClientModel extends Observable implements Runnable {
    /**
     * Notifies receiving of client list from the server
     */
    static final char LIST_CHANGED = '0';
    /**
     * Notifies disconnection from the server
     */
    static final char DISCONNECTED = '1';
    /**
     * Notifies start of a transfer sequence
     */
    static final char TRANSFER_STARTED = '2';
    /**
     * Notifies start of a file receive
     */
    static final char FILE_RECEIVE_STARTED = '3';
    /**
     * Notifies end of a file receive
     */
    static final char FILE_RECEIVE_FINISHED = '4';
    /**
     * Notifies end of a receive sequence
     */
    static final char FILES_RECEIVED = '5';
    /**
     * Notifies start of a file transfer
     */
    static final char FILE_SEND_STARTED = '6';
    /**
     * Notifies end of a file transfer
     */
    static final char FILE_SEND_FINISHED = '7';
    /**
     * Notifies end of a transfer sequence
     */
    static final char FILES_SENT = '8';
    /**
     * Port to connect to a server
     */
    private static final int controlPort = 9000;
    /**
     * Port to transfer/receive files
     */
    private static final int dataPort = 9600;
    /**
     * Stores client list sent by the server
     */
    private List<Person> clientList;
    /**
     * Folder to store received files
     */
    private String downloadsFolder;
    /**
     * Self host name
     */
    private String myHostName;
    /**
     * Host name of the server
     */
    private String serverHostName;
    /**
     * The {@code MiniClient} instance which is
     * running to receive files from the server.
     */
    private Thread miniClientThread;
    /**
     * Reference to socket's output stream
     */
    private PrintWriter printWriter;
    /**
     * Reference to socket's input stream
     */
    private BufferedReader bufferedReader;

    /**
     * Constructs a client connected to a server
     *
     * @param serverHostName Host name of the server to which client is connected
     * @throws IOException If the connection cannot be made
     */
    ClientModel(String serverHostName) throws IOException {
        // Initialise client parameters
        try {
            this.clientList = new ArrayList<>();
            this.downloadsFolder = System.getProperty("user.home") + "/Downloads";
            this.myHostName = InetAddress.getLocalHost().getHostName();
            this.serverHostName = serverHostName;
            this.miniClientThread = null;
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        // Connect to the server via a socket
        Socket socket = new Socket(serverHostName, controlPort);
        // Initialise input and output streams
        bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        printWriter = new PrintWriter(socket.getOutputStream());
        // A new thread to accept files from the server
        Thread clientThread = new Thread(this);
        clientThread.start();
        // A new thread to read client list sent by the server
        Thread listReaderThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    while (true) {
                        String string = bufferedReader.readLine();
                        if (string != null && string.startsWith("SIZE")) {
                            clientList.clear();
                            StringTokenizer stringTokenizer = new StringTokenizer(string);
                            stringTokenizer.nextToken();
                            int size = Integer.parseInt(stringTokenizer.nextToken());
                            for (int i = 0; i < size; i++) {
                                String clientName = bufferedReader.readLine();
                                String clientHostName = bufferedReader.readLine();
                                clientList.add(new Person(clientName, clientHostName));
                            }
                            setChanged();
                            notifyObservers(String.valueOf(LIST_CHANGED));
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        listReaderThread.start();
    }

    /**
     * Used to get the list of clients returned by the server
     *
     * @return The list of clients
     */
    public List<Person> getClientList() {
        return clientList;
    }

    /**
     * Used to get the host name of the server to which
     * connection has been made
     *
     * @return The host name of the server connected to
     */
    String getServerHostName() {
        return serverHostName;
    }

    /**
     * Used to set the downloads folder for receiving files
     *
     * @param downloadsFolder The path of the folder where received files will be stored
     */
    void setDownloadsFolder(String downloadsFolder) {
        this.downloadsFolder = downloadsFolder;
    }

    /**
     * Used to request list of clients from the server
     */
    void getList() {
        printWriter.println("getlist");
        printWriter.flush();
    }

    /**
     * Sends files to the selected receivers
     *
     * @param fileList        List of files to be sent
     * @param receiverIndices Indices of receivers in the {@code clientList}
     *                        to which files are to be transferred
     */
    void send(List<File> fileList, int[] receiverIndices) {
        try {
            // Construct the list of receivers for this operation
            List<String> receiverList = new ArrayList<>();
            for (int i : receiverIndices) {
                receiverList.add(clientList.get(i).getHostName());
            }
            // Connect to the appropriate server port
            ServerSocket serverSocket = new ServerSocket(dataPort + clientList.indexOf(new Person(null, myHostName))
                    + 1);
            Socket socket = serverSocket.accept();
            setChanged();
            notifyObservers(String.valueOf(TRANSFER_STARTED));
            // A new thread to transfer the files to the server
            MiniServer miniServer = new MiniServer(socket, fileList, receiverList, serverSocket, false);
            Thread miniServerThread = new Thread(miniServer);
            // Observer transfer for progress monitoring
            miniServer.addObserver(new Observer() {
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
                            setChanged();
                            notifyObservers(String.valueOf(FILES_SENT) + " " + action.substring(2));
                            break;
                    }
                }
            });
            miniServerThread.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Spawn a client thread
     */
    @Override
    public void run() {
        while (true) {
            try {
                Socket socket = new Socket(serverHostName, dataPort);
                MiniClient miniClient = new MiniClient(socket, downloadsFolder);
                miniClientThread = new Thread(miniClient);
                // Monitor thread for messages
                miniClient.addObserver(new Observer() {
                    @Override
                    public void update(Observable observable, Object o) {
                        String action = (String) o;
                        switch (action.charAt(0)) {
                            case MiniClient.FILE_RECEIVE_STARTED:
                                setChanged();
                                notifyObservers(String.valueOf(ClientModel.FILE_RECEIVE_STARTED) + " "
                                        + action.substring(2));
                                break;
                            case MiniClient.FILE_RECEIVE_FINISHED:
                                setChanged();
                                notifyObservers(String.valueOf(ClientModel.FILE_RECEIVE_FINISHED) + " "
                                        + action.substring(2));
                                break;
                            case MiniClient.FILES_RECEIVED:
                                setChanged();
                                notifyObservers(String.valueOf(ClientModel.FILES_RECEIVED));
                                break;
                        }
                    }
                });
                miniClientThread.start();
                Thread.sleep(4000);
            } catch (IOException e) {
                // Client has been disconnected from the server
                setChanged();
                notifyObservers(String.valueOf(DISCONNECTED));
                clientList.clear();
                setChanged();
                notifyObservers(String.valueOf(LIST_CHANGED));
                break;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}