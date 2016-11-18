package inspire;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Observable;

/**
 * The "actual" server class; it is responsible for sending files to the MiniClient instances. It is utilized by both
 * the Server and Client classes.
 */
public class MiniServer extends Observable implements Runnable {
    static final char FILE_SEND_STARTED = '0';
    static final char FILE_SEND_FINISHED = '1';
    static final char FILES_SENT = '2';
    /**
     * The thread created from this {@code MiniServer} instance. It is used in the Server class (once the thread
     * terminates, the corresponding files can be removed from the {@code clientFileListMap} and deleted if possible).
     */
    private Socket socket;
    private boolean deleteFiles;
    private List<File> fileList;
    private List<String> receiverList;
    private ServerSocket serverSocket;

    /**
     * @param socket       the socket to which the files to be sent should be written
     * @param fileList     list of files selected for sending
     * @param receiverList list of clients to which {@code fileList} should be sent
     * @param serverSocket the server socket created for sending (in the Client case, this should be closed after
     *                     file transfer is complete)
     */
    public MiniServer(Socket socket, List<File> fileList, List<String> receiverList, ServerSocket serverSocket,
           boolean deleteFiles) {
        this.socket = socket;
        this.fileList = fileList;
        this.receiverList = receiverList;
        this.serverSocket = serverSocket;
        this.deleteFiles = deleteFiles;
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
            DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
            if (receiverList != null) {
                // Client
                dataOutputStream.writeInt(receiverList.size());
                for (String receiverHostName : receiverList) {
                    // Host name of the receiver
                    dataOutputStream.writeInt(receiverHostName.length());
                    dataOutputStream.writeChars(receiverHostName);
                }
            } else {
                // Server
                dataOutputStream.writeInt(0);
            }
            dataOutputStream.writeInt(fileList.size());
            for (File file : fileList) {
                // Stream to read the file
                FileInputStream fileInputStream = new FileInputStream(file);
                // Size of the file
                setChanged();
                notifyObservers(String.valueOf(FILE_SEND_STARTED) + " " + file.getName());
                long size = file.length();
                dataOutputStream.writeInt(file.getName().length());
                dataOutputStream.writeChars(file.getName());
                dataOutputStream.writeLong(size);
                dataOutputStream.flush();
                // Buffer to store part of the file
                byte[] buffer = new byte[1024 * 1024];
                // Bytes read so far
                long count = 0;
                while (true) {
                    // Number of bytes read
                    int bytesRead = fileInputStream.read(buffer, 0, 1024 * 1024);
                    dataOutputStream.write(buffer, 0, bytesRead);
                    dataOutputStream.flush();
                    count += bytesRead;
                    if (count == size) {
                        break;
                    }
                }
                fileInputStream.close();
                setChanged();
                notifyObservers(String.valueOf(FILE_SEND_FINISHED) + " " + file.getName());
                if (deleteFiles) {
                    // Server
                    file.delete();
                }
            }
            if (serverSocket != null) {
                // Client
                serverSocket.close();
            }
            dataOutputStream.close();
            socket.close();
            setChanged();
            notifyObservers(String.valueOf(FILES_SENT) + " " + getHostName(socket));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}