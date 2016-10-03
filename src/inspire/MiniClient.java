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

class MiniClient implements Runnable {
    private DataInputStream dis;
    private Map<String, Queue<String>> clientFileListMap;
    private String downloadsFolder;

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
            int receiverSize = dis.readInt();
            StringBuilder sb;
            Map<String, Queue<String>> receiverFileList = new HashMap<>();
            if (clientFileListMap != null && receiverSize != 0) {
                for (int i = 0; i < receiverSize; i++) {
                    int receiverNameLen = dis.readInt();
                    sb = new StringBuilder();
                    for (int j = 0; j < receiverNameLen; j++) {
                        sb.append(dis.readChar());
                    }
                    String receiverName = sb.toString();
                    receiverFileList.put(receiverName, clientFileListMap.containsKey(receiverName)
                            ? clientFileListMap.get(receiverName) : new ConcurrentLinkedQueue<String>());
                }
            }
            int numberOfFiles = dis.readInt();
            for (int i = 0; i < numberOfFiles; i++) {
                int filenameLen = dis.readInt();
                sb = new StringBuilder();
                for (int j = 0; j < filenameLen; j++) {
                    sb.append(dis.readChar());
                }
                String actualFileName = sb.toString();
                if (clientFileListMap != null && receiverSize != 0) {
                    for (Queue<String> fileList : receiverFileList.values()) {
                        fileList.add(actualFileName);
                    }
                }
                long size = dis.readLong();
                byte[] b = new byte[1024 * 1024];
                long count = 0;
                FileOutputStream fos = new FileOutputStream(downloadsFolder + "/" + actualFileName);
                while (true) {
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
                JOptionPane.showMessageDialog(null, "Received the files");
            } else {
                clientFileListMap.putAll(receiverFileList);
            }
            dis.close();
        } catch (IOException e) {
            // Do nothing.
        }
    }
}