package inspire;

import javax.swing.*;
import java.io.DataInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * The "actual" client class; it is responsible for receiving files sent by the MiniServer instances. It is utilized by
 * both the Server and Client classes.
 */
class MiniClient implements Runnable {
    private DataInputStream dis;
    private Map<String, Queue<String>> clientFileListMap;
    private String downloadsFolder;

    /**
     * @param sc                the socket from which the received files should be read
     * @param clientFileListMap map from client host names to the names of files that need to be transferred to them
     * @param downloadsFolder   the folder in which the received files should be stored
     */
    MiniClient(Socket sc, Map<String, Queue<String>> clientFileListMap, String downloadsFolder) {
        this.clientFileListMap = clientFileListMap;
        this.downloadsFolder = downloadsFolder;
        try {
            dis = new DataInputStream(sc.getInputStream());
            Thread t = new Thread(this);
            t.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            // The number of receiver clients (applicable to Server only)
            int receiverSize = dis.readInt();
            StringBuilder sb;
            // Map from client host names to the file names that need to be sent to them (applicable to Server only)
            Map<String, Queue<String>> receiverFileList = new HashMap<>();
            if (clientFileListMap != null && receiverSize != 0) {
                for (int i = 0; i < receiverSize; i++) {
                    // Length of the receiver client host name
                    int receiverNameLen = dis.readInt();
                    sb = new StringBuilder();
                    for (int j = 0; j < receiverNameLen; j++) {
                        sb.append(dis.readChar());
                    }
                    // The actual client name
                    String receiverName = sb.toString();
                    receiverFileList.put(receiverName, clientFileListMap.containsKey(receiverName)
                            ? clientFileListMap.get(receiverName) : new ConcurrentLinkedQueue<String>());
                }
            }
            // Number of files to receive
            int numberOfFiles = dis.readInt();
            for (int i = 0; i < numberOfFiles; i++) {
                // Length of the file name
                int filenameLen = dis.readInt();
                sb = new StringBuilder();
                for (int j = 0; j < filenameLen; j++) {
                    sb.append(dis.readChar());
                }
                // The actual file name
                String actualFileName = sb.toString();
                if (clientFileListMap != null && receiverSize != 0) {
                    for (Queue<String> fileList : receiverFileList.values()) {
                        fileList.add(actualFileName);
                    }
                }
                // Size of the file
                long size = dis.readLong();
                // Buffer to store part of the file
                byte[] b = new byte[1024 * 1024];
                // Bytes read so far
                long count = 0;
                // Stream to write the file
                FileOutputStream fos = new FileOutputStream(downloadsFolder + "/" + actualFileName);
                while (true) {
                    // Number of bytes read
                    int r = dis.read(b, 0, (size - count) > 1024 * 1024 ? 1024 * 1024 : (int) (size - count));
                    fos.write(b, 0, r);
                    count += r;
                    if (count == size) {
                        break;
                    }
                }
                fos.close();
            }
            if (clientFileListMap == null) {
                // Client
                JOptionPane.showMessageDialog(null, "Received the files");
            } else {
                // Server
                clientFileListMap.putAll(receiverFileList);
            }
            dis.close();
        } catch (IOException e) {
            // Do nothing.
        }
    }
}