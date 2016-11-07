package inspire;

import javax.swing.*;
import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.*;

public class ClientModel extends Observable implements Runnable {
    public static final char LIST_CHANGED = '0';
    public static final char CONNECTED = '1';
    public static final char DISCONNECTED = '2';
    public static final char TRANSFER_STARTED = '3';
    public static final char FILE_RECEIVE_STARTED = '4';
    public static final char FILE_RECEIVE_FINISHED = '5';
    public static final char FILES_RECEIVED = '6';

    private List<Person> clientList;
    private String downloadsFolder;
    private String myHostName;
    private String serverHostName;
    private BufferedReader bufferedReader;
    private PrintWriter printWriter;
    private Thread clientThread;
    private Thread listReaderThread;
    private List<Thread> miniServerThreads;
    private Thread miniClientThread;

    public ClientModel(String serverHostName) throws Exception {
        this.miniClientThread = null;
        this.miniServerThreads = new ArrayList<>();
        this.clientList = new ArrayList<>();
        this.downloadsFolder = System.getProperty("user.home") + "/Downloads";
        try {
            this.myHostName = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        this.serverHostName = serverHostName;

        Socket socket = new Socket(serverHostName, 9000);
        bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        printWriter = new PrintWriter(socket.getOutputStream());
        clientThread = new Thread(this);
        clientThread.start();
        listReaderThread = new Thread(new Runnable() {
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
                    JOptionPane.showMessageDialog(null, "Main Server is not running");
                    e.printStackTrace();
                }
            }
        });
        listReaderThread.start();
    }

    public String getServerHostName() {
        return serverHostName;
    }

    public List<Person> getClientList() {
        return clientList;
    }

    public void getList() {
        printWriter.println("getlist");
        printWriter.flush();
    }

    public void send(List<File> selectedFiles, int[] selectedPeopleIndices) {
        List<String> selectedPeople = new ArrayList<>();
        for (int i : selectedPeopleIndices) {
            selectedPeople.add(clientList.get(i).getHostName());
        }
        try {
            ServerSocket serverSocket = new ServerSocket(9600 + clientList.indexOf(new Person(null, myHostName)) + 1);
            setChanged();
            notifyObservers(String.valueOf(TRANSFER_STARTED));
            Socket socket = serverSocket.accept();
            Thread miniServer = new Thread(new MiniServer(socket, selectedFiles, selectedPeople, serverSocket));
            miniServer.start();
            miniServerThreads.add(miniServer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setDownloadsFolder(String downloadsFolder) {
        this.downloadsFolder = downloadsFolder;
    }

    @Override
    public void run() {
        setChanged();
        notifyObservers(String.valueOf(CONNECTED) + " " + serverHostName);
        while (true) {
            try {
                if (miniClientThread == null || !miniClientThread.isAlive()) {
                    Socket socket = new Socket(serverHostName, 9600);
                    MiniClient miniClient = new MiniClient(socket, null, downloadsFolder);
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
