package inspire;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ServerModel extends Observable implements Runnable {
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
                Thread clientHandler = new Thread(new ClientHandler(clientSocket, miniServerSocket));
                clientHandler.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private class ClientHandler implements Runnable {
        private Socket clientSocket;
        private ServerSocket miniServerSocket;

        ClientHandler(Socket clientSocket, ServerSocket miniServerSocket) {
            this.clientSocket = clientSocket;
            this.miniServerSocket = miniServerSocket;
        }

        @Override
        public void run() {
            try {
                new ClientHandler.MiniServerHandler(serverSocket, threadMap);
                while (true) {
                    try {
                        // Get the host name of the client from its socket
                        String hostName = getHostName(clientSocket);
                        // The port number is unique to each client based on its position in the clientList
                        Socket sc = new Socket(hostName, 9600 + clientList.indexOf(new Person(null, hostName)) + 1);
                        // Spawn a new thread for receiving files from the client
                        new Thread(new MiniClient(sc, clientFileListMap, downloadsFolder)).start();
                        // To avoid memory- and network-hogging
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

            /**
             * @param serverSocket the socket through which the server transfers files to the client
             * @param threadMap    map from a MiniServer instance to a map from the host name of client to the files
             *                     transferred to it
             */
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
                        // Accept the client connection
                        Socket sc = serverSocket.accept();
                        // Get host name of the client
                        String hostName = getHostName(sc);
                        // If there are files that need to be transferred...
                        if (clientFileListMap.containsKey(hostName)) {
                            // List of files to be transferred to the client
                            List<File> tempFileList = new ArrayList<>();
                            // The MiniServer instance created for this transfer
                            MiniServer ms;
                            // See tempFileList
                            Queue<File> q = new LinkedList<>();
                            for (String filename : clientFileListMap.get(hostName)) {
                                File f = new File(downloadsFolder + "/" + filename);
                                tempFileList.add(f);
                                q.add(f);
                            }
                            // Spawn a new thread for transferring the files from the server to the client
                            Thread t = new Thread(ms = new MiniServer(sc, tempFileList, null, null));
                            t.start();
                            // Create a map from the client hostname to the list of files that need to be transferred
                            Map<String, Queue<File>> fileMap = Collections.singletonMap(hostName, q);
                            // Put an entry from the MiniServer instance to the map created above in threadMap
                            threadMap.put(ms, fileMap);
                            // To avoid memory- and network-hogging
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
