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
            int filenameLen = dis.readInt();
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < filenameLen; i++) {
                sb.append(dis.readChar());
            }
            String filename = sb.toString();
            String actualFileName = filename;
            Map<String, Queue<String>> receiverFileList = new HashMap<>();
            if (clientFileListMap != null) {
                for (int i = 0; i < filenameLen; i++) {
                    int receiverNameLen = dis.readInt();
                    sb = new StringBuilder();
                    for (int j = 0; j < receiverNameLen; j++) {
                        sb.append(dis.readChar());
                    }
                    String receiverName = sb.toString();
                    receiverFileList.put(receiverName, clientFileListMap.containsKey(receiverName)
                            ? clientFileListMap.get(receiverName) : new ConcurrentLinkedQueue<String>());
                }
                filenameLen = dis.readInt();
                sb = new StringBuilder();
                for (int i = 0; i < filenameLen; i++) {
                    sb.append(dis.readChar());
                }
                actualFileName = sb.toString();
                for (Queue<String> fileList : receiverFileList.values()) {
                    fileList.add(actualFileName);
                }
            }
            long size = dis.readLong();
            byte[] b = new byte[1024 * 1024];
            long count = 0;
            FileOutputStream fos = new FileOutputStream(downloadsFolder + "/" + actualFileName);
            while (true) {
                int r = dis.read(b, 0, 1024 * 1024);
                fos.write(b, 0, r);
                count += r;
                if (count == size) {
                    break;
                }
            }
            fos.close();
            if (clientFileListMap == null) {
                JOptionPane.showMessageDialog(null, "Received " + actualFileName);
            } else {
                clientFileListMap.putAll(receiverFileList);
            }
            dis.close();
        } catch (IOException e) {
            // Do nothing.
        }
    }
}