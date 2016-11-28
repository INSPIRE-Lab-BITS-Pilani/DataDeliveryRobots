package inspire;

import java.io.IOException;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

public class ClientController {
    private static ClientModel clientModel;
    private static ClientView clientView;

    public static void main(String[] args) {
        clientView = new ClientView();
        clientView.addObserver(new Observer() {
            @Override
            public void update(Observable observable, Object o) {
                String action = (String) o;
                switch (action.charAt(0)) {
                    case ClientView.START_CLIENT:
                        startClient();
                        break;
                    case ClientView.GET_LIST:
                        if (clientModel == null) {
                            clientView.showMessage("Start the client first!!");
                        } else {
                            clientModel.getList();
                        }
                        break;
                    case ClientView.DOWNLOADS_FOLDER:
                        clientModel.setDownloadsFolder(action.substring(2));
                        break;
                    case ClientView.SEND:
                        clientModel.send(clientView.getSelectedFiles(), clientView.getReceiverIndices());
                        break;
                    case ClientView.DISCONNECTED_CLIENT:
                        clientModel = null;
                        clientView.setClientModel(null);
                        break;
                }
            }
        });
        autoServerConnector();
    }

    private static void startClient() {
        if (clientModel == null) {
            String serverHostName = ClientView.getServerHostName(System.getProperty("java.io.tmpdir") + "/"
                    + "__ServerHostName__.txt");
            if (serverHostName != null && serverHostName.length() > 0) {
                try {
                    clientModel = new ClientModel(serverHostName);
                    clientView.setClientModel(clientModel);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } else {
            clientView.showMessage("You are already connected to '" + clientModel.getServerHostName() + "'!!");
        }
    }

    private static void autoServerConnector() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                List<String> autoServerNameList;
                autoServerNameList = ClientView.getAutoServerList(System.getProperty("java.io.tmpdir") + "/"
                        + "__AutoServerHostNames__.txt");
                while (true) {
                    try {
                        if (clientModel == null) {
                            for (String hostName : autoServerNameList) {
                                try {
                                    clientModel = new ClientModel(hostName);
                                    clientView.setClientModel(clientModel);
                                    break;
                                } catch (IOException e) {
                                }
                            }
                        }
                        Thread.sleep(4000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        thread.start();
    }
}