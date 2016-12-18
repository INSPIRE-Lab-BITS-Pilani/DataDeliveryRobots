package inspire;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.qatools.allure.annotations.Attachment;

import java.io.*;
import java.net.Socket;
import java.util.*;

import static inspire.MiniClient.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test for {@link MiniClient} class
 *
 * @author Abhinav Baid, Atishay Jain
 * @version 1.0
 * @since 20-12-2016
 */
public class MiniClientTest implements Observer {
    private String[] receivers;
    private String[] fileNames;
    private String[] fileData;
    private String downloadsFolder;
    private MiniClient miniClient;
    private Thread miniClientThread;
    private List<Object> args;

    @Attachment
    static byte[] getBytesFromBAOS(ByteArrayOutputStream baos) {
        return baos.toByteArray();
    }

    @Attachment
    static byte[] getBytesFromString(String fileDataStr) {
        return fileDataStr.getBytes();
    }

    @Before
    public void setUp() throws Exception {
        receivers = new String[]{"Robo1", "Robo2", "Robo3"};
        fileNames = new String[]{"file1.txt", "file2.txt", "file3.txt"};
        fileData = new String[]{"This is file 1", "This is file 2", "This is file 3"};
        final Socket socket = mock(Socket.class);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        dos.writeInt(receivers.length);
        for (String receiver : receivers) {
            dos.writeInt(receiver.length());
            dos.writeChars(receiver);
        }
        dos.writeInt(fileNames.length);
        for (int i = 0; i < fileNames.length; i++) {
            String fileName = fileNames[i];
            dos.writeInt(fileName.length());
            dos.writeChars(fileName);
            String fileDataStr = fileData[i];
            byte[] arr = getBytesFromString(fileDataStr);
            dos.writeLong(arr.length);
            dos.write(arr);
        }
        dos.flush();
        final InputStream inputStream = new ByteArrayInputStream(getBytesFromBAOS(baos));
        when(socket.getInputStream()).thenReturn(inputStream);
        downloadsFolder = System.getProperty("java.io.tmpdir");
        miniClient = new MiniClient(socket, downloadsFolder);
        miniClient.addObserver(this);
        miniClientThread = new Thread(miniClient);
        args = new ArrayList<>();
    }

    @After
    public void tearDown() throws Exception {
        miniClient.deleteObserver(this);
        for (String fileName : fileNames) {
            new File(downloadsFolder + "/" + fileName).delete();
        }
    }

    @Override
    public void update(Observable observable, Object o) {
        assert observable == miniClient;
        args.add(o);
    }

    @Test
    public void testMiniClient() throws Exception {
        miniClientThread.start();
        while (true) {
            if (miniClientThread.getState() == Thread.State.TERMINATED) {
                int k = 0;
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
    }
}