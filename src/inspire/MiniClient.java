package inspire;

import javax.swing.*;
import java.io.DataInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

class MiniClient implements Runnable {
    private DataInputStream dis;
    private Map<String, List<String>> clientFileListMap;
    private String downloadsFolder;

    MiniClient(String ip, Map<String, List<String>> clientFileListMap, String downloadsFolder, int port) {
        while (true) {
            try {
                Socket sc = new Socket(ip, port);
                this.clientFileListMap = clientFileListMap;
                this.downloadsFolder = downloadsFolder;
                dis = new DataInputStream(sc.getInputStream());
                Thread t = new Thread(this);
                t.start();
                break;
            } catch (IOException e) {
                // Do nothing.
            }
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
            List<String> fileList = null;
            if (clientFileListMap != null) {
                fileList = clientFileListMap.containsKey(filename)
                        ? clientFileListMap.get(filename) : new ArrayList<>();
                filenameLen = dis.readInt();
                sb = new StringBuilder();
                for (int i = 0; i < filenameLen; i++) {
                    sb.append(dis.readChar());
                }
                fileList.add(actualFileName = sb.toString());
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
                JOptionPane.showMessageDialog(null, "Received " + filename);
            } else {
                clientFileListMap.put(filename, fileList);
            }
        } catch (IOException e) {
            // Do nothing.
        }
    }
}