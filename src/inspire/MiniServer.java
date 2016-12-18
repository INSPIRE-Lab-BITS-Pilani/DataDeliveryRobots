package inspire;

import ru.yandex.qatools.allure.annotations.Attachment;

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
 * The actual server class which is responsible for
 * transferring files to the {@code MiniClient} instances.
 * It is utilized by both the Server and Client.
 *
 * @author Abhinav Baid, Atishay Jain
 * @version 1.0
 * @see MiniClient
 * @since 20-12-2016
 */
class MiniServer extends Observable implements Runnable {
    /**
     * Notify start of file transfer
     */
    static final char FILE_SEND_STARTED = '0';
    /**
     * Notify end of file transfer
     */
    static final char FILE_SEND_FINISHED = '1';
    /**
     * Notify completion of file transfers
     */
    static final char FILES_SENT = '2';
    /**
     * Socket to transfer files via
     */
    private final Socket socket;
    /**
     * Flag to indicate whether to delete files after transfer
     */
    private final boolean deleteFiles;
    /**
     * List of files to transfer
     */
    private final List<File> fileList;
    /**
     * List of receivers to transfer with the file list (Only for Client)
     */
    private final List<String> receiverList;
    /**
     * Server socket ({@code null} in case of Server)
     */
    private final ServerSocket serverSocket;

    /**
     * Construct an instance with appropriate parameters
     *
     * @param socket       The socket to which the files to be sent should be written
     * @param fileList     List of files selected for sending
     * @param receiverList List of clients to which {@code fileList} should be sent
     * @param serverSocket The server socket created for sending (in the Client case,
     *                     this should be closed after file transfer is complete)
     * @param deleteFiles  Flag to know if files are to be deleted after
     *                     transfer (true in case of Server)
     */
    public MiniServer(Socket socket, List<File> fileList, List<String> receiverList, ServerSocket serverSocket,
                      boolean deleteFiles) {
        this.socket = socket;
        this.fileList = fileList;
        this.receiverList = receiverList;
        this.serverSocket = serverSocket;
        this.deleteFiles = deleteFiles;
    }

    /**
     * Returns the host name of the other side of the socket
     *
     * @param socket Socket to get the host name of
     * @return Host name of the socket's other side
     * @throws UnknownHostException If socket host name is not found
     */
    @Attachment
    private String getHostName(Socket socket) throws UnknownHostException {
        String hostName = socket.getInetAddress().getHostName();
        if (hostName.equals("localhost")) {
            hostName = InetAddress.getLocalHost().getHostName();
        }
        return hostName;
    }

    /**
     * Spawn a thread to transfer files
     */
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