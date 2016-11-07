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
    /**
     * The thread created from this {@code MiniServer} instance. It is used in the Server class (once the thread
     * terminates, the corresponding files can be removed from the {@code clientFileListMap} and deleted if possible).
     */
    Thread reqHandlerThread;
    private Socket sc;
    private List<File> selectedFiles;
    private List<Person> receivers;
    private ServerSocket serverSocket;

    /**
     * @param sc            the socket to which the files to be sent should be written
     * @param selectedFiles list of files selected for sending
     * @param receivers     list of clients to which {@code selectedFiles} should be sent
     * @param serverSocket  the server socket created for sending (in the Client case, this should be closed after
     *                      file transfer is complete)
     */
    public MiniServer(Socket sc, List<File> selectedFiles, List<Person> receivers, ServerSocket serverSocket) {
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
                    // Client
                    dos.writeInt(receivers.size());
                    for (Person receiver : receivers) {
                        // Host name of the receiver
                        String receiverHostName = receiver.getHostName();
                        dos.writeInt(receiverHostName.length());
                        dos.writeChars(receiverHostName);
                    }
                } else {
                    // Server
                    dos.writeInt(0);
                }
                dos.writeInt(selectedFiles.size());
                for (File selectedFile : selectedFiles) {
                    // Stream to read the file
                    FileInputStream fis = new FileInputStream(selectedFile);
                    // Size of the file
                    long size = selectedFile.length();
                    dos.writeInt(selectedFile.getName().length());
                    dos.writeChars(selectedFile.getName());
                    dos.writeLong(size);
                    dos.flush();
                    // Buffer to store part of the file
                    byte[] b = new byte[1024 * 1024];
                    // Bytes read so far
                    long count = 0;
                    while (true) {
                        // Number of bytes read
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
                    // Client
                    JOptionPane.showMessageDialog(null, "Transfer of " + selectedFiles + " completed");
                }
                if (serverSocket != null) {
                    // Client
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