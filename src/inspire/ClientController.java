package inspire;

import java.io.IOException;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

/**
 * The controller for the client in the MVC design.
 * This class observes a {@code ClientView} instance
 * and responds to user actions by calling appropriate
 * methods. It is the entry point of the client.
 *
 * @author Abhinav Baid, Atishay Jain
 * @version 1.0
 * @see ClientModel
 * @see ClientView
 * @since 20-12-2016
 */
class ClientController {
    /**
     * The {@code ClientModel} instance for the application.
     */
    private static ClientModel clientModel;
    /**
     * The {@code ClientView} instance for the application.
     * It determines the GUI of the program.
     */
    private static ClientView clientView;

    public static void main(String[] args) {
        clientView = new ClientView();
        // Attach observer to the view
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
        // Start automatically trying connecting to servers
        autoServerConnector();
    }

    /**
     * This method gets a server host name from the user
     * and tries to connect to a server with that host name.
     */
    private static void startClient() {
        if (clientModel == null) {
            // Get server host name from the user
            String serverHostName = ClientView.getServerHostName(System.getProperty("java.io.tmpdir") + "/"
                    + "__ServerHostName__.txt");
            if (serverHostName != null && serverHostName.length() > 0) {
                try {
                    // Initialise a client instance
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

    /**
     * This method gets an auto server host names file from the user
     * and tries to connect to a server with a host name in that list.
     */
    private static void autoServerConnector() {
        // A new thread for auto connections
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                // Get auto server host name list file from the user
                List<String> autoServerNameList = autoServerNameList = ClientView.getAutoServerList(System.getProperty("java.io.tmpdir") + "/"
                        + "__AutoServerHostNames__.txt");
                // Keep trying to connect to servers
                while (true) {
                    try {
                        if (clientModel == null) {
                            for (String hostName : autoServerNameList) {
                                try {
                                    clientModel = new ClientModel(hostName);
                                    clientView.setClientModel(clientModel);
                                    break;
                                } catch (IOException e) {
                                    continue;
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