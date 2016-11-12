package inspire;

import java.util.List;
import java.util.Observable;
import java.util.Observer;

public class ServerController {
    private static ServerModel serverModel;
    private static ServerView serverView;

    public static void main(String[] args) {
        serverView = new ServerView();
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

    public static void startServer() {
        if (serverModel == null) {
            List<Person> clientList = serverView.getClientList(
                    System.getProperty("java.io.tmpdir") + "/" + "__ClientListFile__.txt"
            );
            if (clientList.size() > 0) {
                serverModel = new ServerModel(clientList, System.getProperty("java.io.tmpdir"));
                serverView.setServerModel(serverModel);
            }
        } else {
            serverView.showMessage("Server is already running!!");
        }
    }
}
