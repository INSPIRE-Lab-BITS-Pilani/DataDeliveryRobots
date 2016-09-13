package inspire;

import javax.swing.*;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

class MiniServer implements Runnable {
    private Socket sc;
    private File selectedFile;
    private String receiverHostName;
    private ServerSocket serverSocket;
    Thread reqHandlerThread;

    MiniServer(Socket sc, File selectedFile, String receiverHostName, ServerSocket serverSocket) {
        this.sc = sc;
        this.selectedFile = selectedFile;
        this.receiverHostName = receiverHostName;
        this.serverSocket = serverSocket;
    }

    @Override
    public void run() {
        new MiniServerRequestHandler(sc);
    }

    private class MiniServerRequestHandler implements Runnable {
        DataOutputStream dos;

        MiniServerRequestHandler(Socket sc) {
            try {
                dos = new DataOutputStream(sc.getOutputStream());
                reqHandlerThread = new Thread(this);
                reqHandlerThread.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            try {
                FileInputStream fis = new FileInputStream(selectedFile);
                long size = selectedFile.length();
                if (receiverHostName != null) {
                    dos.writeInt(receiverHostName.length());
                    dos.writeChars(receiverHostName);
                }
                dos.writeInt(selectedFile.getName().length());
                dos.writeChars(selectedFile.getName());
                dos.writeLong(size);
                dos.flush();
                byte[] b = new byte[1024 * 1024];
                long count = 0;
                while (true) {
                    int r = fis.read(b, 0, 1024 * 1024);
                    dos.write(b, 0, r);
                    dos.flush();
                    count += r;
                    if (count == size) {
                        break;
                    }
                }
                fis.close();
                if (receiverHostName != null) {
                    JOptionPane.showMessageDialog(null, "Transfer of " + selectedFile + " completed");
                }
                if (serverSocket != null) {
                    serverSocket.close();
                }
                dos.close();
                sc.close();
            } catch (IOException e) {
                // Do nothing.
            }
        }
    }
}