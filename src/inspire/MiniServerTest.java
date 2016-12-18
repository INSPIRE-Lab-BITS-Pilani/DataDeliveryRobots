package inspire;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

import static inspire.MiniClient.*;
import static inspire.MiniClientTest.getBytesFromBAOS;
import static inspire.MiniClientTest.getBytesFromString;
import static inspire.MiniServer.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test for {@link MiniServer} class
 *
 * @author Abhinav Baid, Atishay Jain
 * @version 1.0
 * @since 20-12-2016
 */
public class MiniServerTest implements Observer {
    private String[] receivers;
    private String[] fileNames;
    private String[] fileData;
    private String fileLocation;
    private ByteArrayOutputStream baos;
    private MiniServer miniServer;
    private Thread miniServerThread;
    private List<Object> args;

    @Before
    public void setUp() throws Exception {
        receivers = new String[]{"Robo1", "Robo2", "Robo3"};
        fileNames = new String[]{"file1.txt", "file2.txt", "file3.txt"};
        fileData = new String[]{"This is file 1", "This is file 2", "This is file 3"};
        fileLocation = System.getProperty("java.io.tmpdir") + "/" + "__MiniServerTestDir__";
        new File(fileLocation).mkdir();
        List<File> fileList = new ArrayList<>();
        for (int i = 0; i < fileNames.length; i++) {
            String fileName = fileNames[i];
            File f = new File(fileLocation + "/" + fileName);
            fileList.add(f);
            FileOutputStream fos = new FileOutputStream(f);
            String fileDataStr = fileData[i];
            byte[] arr = getBytesFromString(fileDataStr);
            fos.write(arr);
            fos.close();
        }
        final Socket socket = mock(Socket.class);
        baos = new ByteArrayOutputStream();
        when(socket.getOutputStream()).thenReturn(baos);
        final InetAddress inetAddress = InetAddress.getLocalHost();
        when(socket.getInetAddress()).thenReturn(inetAddress);
        final ServerSocket serverSocket = mock(ServerSocket.class);
        miniServer = new MiniServer(socket, fileList, Arrays.asList(receivers), serverSocket, true);
        miniServer.addObserver(this);
        miniServerThread = new Thread(miniServer);
        args = new ArrayList<>();
    }

    @After
    public void tearDown() throws Exception {
        miniServer.deleteObserver(this);
        new File(fileLocation).delete();
    }

    @Override
    public void update(Observable observable, Object o) {
        assert observable == miniServer || observable.getClass() == MiniClient.class;
        args.add(o);
    }

    @Test
    public void testMiniServer() throws Exception {
        miniServerThread.start();
        while (true) {
            if (miniServerThread.getState() == Thread.State.TERMINATED) {
                int k = 0;
                for (String fileName : fileNames) {
                    Assert.assertEquals(args.get(k++), String.valueOf(FILE_SEND_STARTED) + " " + fileName);
                    Assert.assertEquals(args.get(k++), String.valueOf(FILE_SEND_FINISHED) + " " + fileName);
                }
                Assert.assertEquals(args.get(k), String.valueOf(FILES_SENT) + " "
                        + InetAddress.getLocalHost().getHostName());
                final Socket socket = mock(Socket.class);
                final InputStream inputStream = new ByteArrayInputStream(getBytesFromBAOS(baos));
                when(socket.getInputStream()).thenReturn(inputStream);
                String downloadsFolder = System.getProperty("java.io.tmpdir");
                MiniClient miniClient = new MiniClient(socket, downloadsFolder);
                miniClient.addObserver(this);
                Thread miniClientThread = new Thread(miniClient);
                args.clear();
                miniClientThread.start();
                while (true) {
                    if (miniClientThread.getState() == Thread.State.TERMINATED) {
                        k = 0;
                        for (String receiver : receivers) {
                            Assert.assertEquals(args.get(k++), String.valueOf(RECEIVER_ADDED) + " " + receiver);
                        }
                        for (int i = 0; i < fileNames.length; i++) {
                            String fileName = fileNames[i];
                            Assert.assertEquals(args.get(k++), String.valueOf(FILE_RECEIVE_STARTED) + " " + fileName);
                            Assert.assertEquals(args.get(k++), String.valueOf(FILE_RECEIVE_FINISHED) + " " + fileName);
                            Scanner sc = new Scanner(new File(downloadsFolder + "/" + fileName));
                            StringBuilder sb = new StringBuilder();
                            while (sc.hasNextLine()) {
                                sb.append(sc.nextLine());
                            }
                            sc.close();
                            Assert.assertEquals(sb.toString(), fileData[i]);
                        }
                        Assert.assertEquals(args.get(k), String.valueOf(FILES_RECEIVED));
                        break;
                    }
                }
                miniClient.deleteObserver(this);
                for (String fileName : fileNames) {
                    new File(downloadsFolder + "/" + fileName).delete();
                }
                break;
            }
        }
    }
}