package inspire;

import java.io.DataInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.List;
import java.util.Observable;

/**
 * The "actual" client class; it is responsible for receiving files sent by the MiniServer instances. It is utilized by
 * both the Server and Client classes.
 */
class MiniClient extends Observable implements Runnable {
    public static final char FILE_RECEIVE_STARTED = '0';
    public static final char FILE_RECEIVE_FINISHED = '1';
    public static final char FILES_RECEIVED = '2';

    private DataInputStream dataInputStream;
    private List<String> receiverList;
    private String downloadsFolder;

    /**
     * @param socket            the socket from which the received files should be read
     * @param receiverList map from client host names to the names of files that need to be transferred to them
     * @param downloadsFolder   the folder in which the received files should be stored
     */
    public MiniClient(Socket socket, List<String> receiverList, String downloadsFolder) {
        this.receiverList = receiverList;
        this.downloadsFolder = downloadsFolder;
        try {
            dataInputStream = new DataInputStream(socket.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            // The number of receiver clients (applicable to Server only)
            int receiverSize = dataInputStream.readInt();
            StringBuilder sb;
            // Map from client host names to the file names that need to be sent to them (applicable to Server only)
            if (receiverList != null && receiverSize != 0) {
                for (int i = 0; i < receiverSize; i++) {
                    // Length of the receiver client host name
                    int receiverNameLen = dataInputStream.readInt();
                    sb = new StringBuilder();
                    for (int j = 0; j < receiverNameLen; j++) {
                        sb.append(dataInputStream.readChar());
                    }
                    // The actual client name
                    String receiverName = sb.toString();
                    receiverList.add(receiverName);
                }
            }
            // Number of files to receive
            int numberOfFiles = dataInputStream.readInt();
            for (int i = 0; i < numberOfFiles; i++) {
                // Length of the file name
                int filenameLen = dataInputStream.readInt();
                sb = new StringBuilder();
                for (int j = 0; j < filenameLen; j++) {
                    sb.append(dataInputStream.readChar());
                }
                // The actual file name
                String actualFileName = sb.toString();
                setChanged();
                notifyObservers(new String(String.valueOf(FILE_RECEIVE_STARTED) + " " + actualFileName));
                // Size of the file
                long size = dataInputStream.readLong();
                // Buffer to store part of the file
                byte[] b = new byte[1024 * 1024];
                // Bytes read so far
                long count = 0;
                // Stream to write the file
                FileOutputStream fos = new FileOutputStream(downloadsFolder + "/" + actualFileName);
                while (true) {
                    // Number of bytes read
                    int r = dataInputStream.read(b, 0, (size - count) > 1024 * 1024 ? 1024 * 1024 : (int) (size - count));
                    fos.write(b, 0, r);
                    count += r;
                    if (count == size) {
                        break;
                    }
                }
                fos.close();
                setChanged();
                notifyObservers(new String(String.valueOf(FILE_RECEIVE_FINISHED) + " " + actualFileName));
            }
            if (receiverList == null) {
                // Client
            } else {
                // Server
            }
            dataInputStream.close();
            setChanged();
            notifyObservers(String.valueOf(FILES_RECEIVED));
        } catch (IOException e) {
            // Do nothing.
        }
    }
}