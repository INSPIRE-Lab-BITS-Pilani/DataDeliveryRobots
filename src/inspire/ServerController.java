package inspire;

import java.util.List;
import java.util.Observable;
import java.util.Observer;

/**
 * The controller for the server in the MVC design.
 * This class observes a {@code ServerView} instance
 * and responds to user actions by calling appropriate
 * methods. It is the entry point of the server.
 *
 * @author Abhinav Baid, Atishay Jain
 * @version 1.0
 * @see ServerModel
 * @see ServerView
 * @since 20-12-2016
 */
class ServerController {
    /**
     * The {@code ServerModel} instance for the application.
     */
    private static ServerModel serverModel;
    /**
     * The {@code ServerView} instance for the application.
     * It determines the GUI of the program.
     */
    private static ServerView serverView;

    public static void main(String[] args) {
        serverView = new ServerView();
        // Attach observer to the view
        serverView.addObserver(new Observer() {
            @Override
            public void update(Observable observable, Object o) {
                String action = (String) o;
                switch (action.charAt(0)) {
                    case ServerView.START_SERVER:
                        startServer();
                        break;
                    case ServerView.DOWNLOADS_FOLDER:
                        serverModel.setDownloadsFolder(action.substring(2));
                        break;
                }
            }
        });
    }

    /**
     * This method gets a client list file from the user
     * and starts the server instance.
     */
    private static void startServer() {
        if (serverModel == null) {
            // Get client list file from the user
            List<Person> clientList = serverView.getClientList(
                    System.getProperty("java.io.tmpdir") + "/" + "__ClientListFile__.txt");
            if (clientList.size() > 0) {
                serverModel = new ServerModel(clientList, System.getProperty("java.io.tmpdir"));
                serverView.setServerModel(serverModel);
            }
        } else {
            serverView.showMessage("Server is already running!!");
        }
    }
}