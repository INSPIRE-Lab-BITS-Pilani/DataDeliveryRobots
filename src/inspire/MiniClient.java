package inspire;

import java.io.DataInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Observable;

/**
 * The "actual" client class; it is responsible for receiving files sent by the MiniServer instances. It is utilized by
 * both the Server and Client classes.
 */
public class MiniClient extends Observable implements Runnable {
    public static final char FILE_RECEIVE_STARTED = '0';
    public static final char FILE_RECEIVE_FINISHED = '1';
    public static final char FILES_RECEIVED = '2';
    public static final char RECEIVER_ADDED = '3';

    private Socket socket;
    private String downloadsFolder;

    /**
     * @param socket          the socket from which the received files should be read
     * @param downloadsFolder the folder in which the received files should be stored
     */
    public MiniClient(Socket socket, String downloadsFolder) {
        this.socket = socket;
        this.downloadsFolder = downloadsFolder;
    }

    @Override
    public void run() {
        try {
            // The number of receiver clients (applicable to Server only)
            DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
            int receiverSize = dataInputStream.readInt();
            StringBuilder sb;
            // Map from client host names to the file names that need to be sent to them (applicable to Server only)
            if (receiverSize != 0) {
                for (int i = 0; i < receiverSize; i++) {
                    // Length of the receiver client host name
                    int receiverNameLength = dataInputStream.readInt();
                    sb = new StringBuilder();
                    for (int j = 0; j < receiverNameLength; j++) {
                        sb.append(dataInputStream.readChar());
                    }
                    // The actual client name
                    String receiverName = sb.toString();
                    setChanged();
                    notifyObservers(String.valueOf(RECEIVER_ADDED) + " " + receiverName);
                }
            }
            // Number of files to receive
            int numberOfFiles = dataInputStream.readInt();
            for (int i = 0; i < numberOfFiles; i++) {
                // Length of the file name
                int filenameLength = dataInputStream.readInt();
                sb = new StringBuilder();
                for (int j = 0; j < filenameLength; j++) {
                    sb.append(dataInputStream.readChar());
                }
                // The actual file name
                String fileName = sb.toString();
                setChanged();
                notifyObservers(String.valueOf(FILE_RECEIVE_STARTED) + " " + fileName);
                // Size of the file
                long size = dataInputStream.readLong();
                // Buffer to store part of the file
                byte[] buffer = new byte[1024 * 1024];
                // Bytes read so far
                long count = 0;
                // Stream to write the file
                FileOutputStream fileOutputStream = new FileOutputStream(downloadsFolder + "/" + fileName);
                while (true) {
                    // Number of bytes read
                    int bytesRead = dataInputStream.read(buffer, 0, ((int) (size - count)) > 1024 * 1024 ? 1024 * 1024 : (int) (size - count));
                    fileOutputStream.write(buffer, 0, bytesRead);
                    count += bytesRead;
                    if (count == size) {
                        break;
                    }
                }
                fileOutputStream.close();
                setChanged();
                notifyObservers(String.valueOf(FILE_RECEIVE_FINISHED) + " " + fileName);
            }
            dataInputStream.close();
            socket.close();
            setChanged();
            notifyObservers(String.valueOf(FILES_RECEIVED));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}