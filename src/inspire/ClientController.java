package inspire;

import java.io.File;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

public class ClientController {
    private Thread autoServerConnectorThread;
    private ClientModel clientModel;
    private ClientView clientView;

    public ClientController() {
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
                            clientView.showMessage("Start the client first!");
                        } else {
                            clientModel.getList();
                        }
                        break;
                    case ClientView.DOWNLOADS_FOLDER:
                        clientModel.setDownloadsFolder(action.substring(2));
                        break;
                    case ClientView.SEND:
                        List<File> selectedFiles = clientView.getSelectedFiles();
                        clientModel.send(selectedFiles, clientView.getSelectedPeopleIndices());
                        break;
                    case ClientView.DISCONNECTED_CLIENT:
                        clientModel = null;
                        clientView.setClientModel(null);
                        break;
                }
            }
        });
        autoServerConnectorThread = autoServerConnector();
    }

    public static void main(String[] args) {
        new ClientController();
    }

    private void startClient() {
        if (clientModel == null) {
            String serverHostName = ClientView.getServerHostName(System.getProperty("java.io.tmpdir") + "/" + "__ServerHostName__.txt");
            try {
                clientModel = new ClientModel(serverHostName);
                clientView.setClientModel(clientModel);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            clientView.showMessage("You are already connected to " + clientModel.getServerHostName());
        }
    }

    private Thread autoServerConnector() {
        List<String> autoServerNameList = ClientView.getAutoServerList(System.getProperty("java.io.tmpdir") + "/" + "__AutoServerHostNames__.txt");
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    if (clientModel == null) {
                        for (String hostName : autoServerNameList) {
                            try {
                                clientModel = new ClientModel(hostName);
                                clientView.setClientModel(clientModel);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            break;
                        }
                    }
                    try {
                        Thread.sleep(4000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        thread.start();
        return thread;
    }
}