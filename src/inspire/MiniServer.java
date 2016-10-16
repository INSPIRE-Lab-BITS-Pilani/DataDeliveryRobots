package inspire;

import javax.swing.*;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;

/**
 * The "actual" server class; it is responsible for sending files to the MiniClient instances. It is utilized by both
 * the Server and Client classes.
 */
class MiniServer implements Runnable {
    private Socket sc;
    private List<File> selectedFiles;
    private List<Person> receivers;
    private ServerSocket serverSocket;
    Thread reqHandlerThread;

    /**
     * @param sc the socket to which the files to be sent should be written
     * @param selectedFiles list of files selected for sending
     * @param receivers list of clients to which {@code selectedFiles} should be sent
     * @param serverSocket the server socket created for sending (in the Client case, this should be closed after
     *                     file transfer is complete)
     */
    MiniServer(Socket sc, List<File> selectedFiles, List<Person> receivers, ServerSocket serverSocket) {
        this.sc = sc;
        this.selectedFiles = selectedFiles;
        this.receivers = receivers;
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
                if (receivers != null) {
                    dos.writeInt(receivers.size());
                    for (Person receiver : receivers) {
                        String receiverHostName = receiver.getHostName();
                        dos.writeInt(receiverHostName.length());
                        dos.writeChars(receiverHostName);
                    }
                } else {
                    dos.writeInt(0);
                }
                dos.writeInt(selectedFiles.size());
                for (int i = 0; i < selectedFiles.size(); i++) {
                    File selectedFile = selectedFiles.get(i);
                    FileInputStream fis = new FileInputStream(selectedFile);
                    long size = selectedFile.length();
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
                }
                if (receivers != null) {
                    JOptionPane.showMessageDialog(null, "Transfer of " + selectedFiles + " completed");
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