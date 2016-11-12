package inspire;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.*;

public class ClientModel extends Observable implements Runnable {
    public static final int controlPort = 9000;
    public static final int dataPort = 9600;
    public static final char LIST_CHANGED = '0';
    public static final char DISCONNECTED = '1';
    public static final char TRANSFER_STARTED = '2';
    public static final char FILE_RECEIVE_STARTED = '3';
    public static final char FILE_RECEIVE_FINISHED = '4';
    public static final char FILES_RECEIVED = '5';
    public static final char FILE_SEND_STARTED = '6';
    public static final char FILE_SEND_FINISHED = '7';
    public static final char FILES_SENT = '8';

    private List<Person> clientList;
    private String downloadsFolder;
    private String myHostName;
    private String serverHostName;
    private Thread miniClientThread;
    private PrintWriter printWriter;
    private BufferedReader bufferedReader;

    public ClientModel(String serverHostName) throws IOException {
        try {
            this.clientList = new ArrayList<>();
            this.downloadsFolder = System.getProperty("user.home") + "/Downloads";
            this.myHostName = InetAddress.getLocalHost().getHostName();
            this.serverHostName = serverHostName;
            this.miniClientThread = null;
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        Socket socket = new Socket(serverHostName, controlPort);
        bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        printWriter = new PrintWriter(socket.getOutputStream());
        Thread clientThread = new Thread(this);
        clientThread.start();
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

    public List<Person> getClientList() {
        return clientList;
    }

    public String getServerHostName() {
        return serverHostName;
    }

    public void setDownloadsFolder(String downloadsFolder) {
        this.downloadsFolder = downloadsFolder;
    }

    public void getList() {
        printWriter.println("getlist");
        printWriter.flush();
    }

    public void send(List<File> fileList, int[] receiverIndices) {
        try {
            List<String> receiverList = new ArrayList<>();
            for (int i : receiverIndices) {
                receiverList.add(clientList.get(i).getHostName());
            }
            ServerSocket serverSocket = new ServerSocket(dataPort + clientList.indexOf(new Person(null, myHostName)) + 1);
            Socket socket = serverSocket.accept();
            setChanged();
            notifyObservers(String.valueOf(TRANSFER_STARTED));
            MiniServer miniServer = new MiniServer(socket, fileList, receiverList, serverSocket, false);
            Thread miniServerThread = new Thread(miniServer);
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

    @Override
    public void run() {
        while (true) {
            try {
                if (miniClientThread == null || !miniClientThread.isAlive()) {
                    Socket socket = new Socket(serverHostName, dataPort);
                    MiniClient miniClient = new MiniClient(socket, downloadsFolder);
                    miniClientThread = new Thread(miniClient);
                    miniClient.addObserver(new Observer() {
                        @Override
                        public void update(Observable observable, Object o) {
                            String action = (String) o;
                            switch (action.charAt(0)) {
                                case MiniClient.FILE_RECEIVE_STARTED:
                                    setChanged();
                                    notifyObservers(String.valueOf(ClientModel.FILE_RECEIVE_STARTED) + " " + action.substring(2));
                                    break;
                                case MiniClient.FILE_RECEIVE_FINISHED:
                                    setChanged();
                                    notifyObservers(String.valueOf(ClientModel.FILE_RECEIVE_FINISHED) + " " + action.substring(2));
                                    break;
                                case MiniClient.FILES_RECEIVED:
                                    setChanged();
                                    notifyObservers(String.valueOf(ClientModel.FILES_RECEIVED));
                                    break;
                            }
                        }
                    });
                    miniClientThread.start();
                }
                Thread.sleep(4000);
            } catch (IOException e) {
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
