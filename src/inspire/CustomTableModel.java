package inspire;

import ru.yandex.qatools.allure.annotations.Attachment;
import ru.yandex.qatools.allure.annotations.Step;
import javax.swing.table.AbstractTableModel;
import java.util.List;

/**
 * This class provides an implementation of the {@code TableModel} interface tailored to view the list of clients
 * {@code clientList}.
 */
public class CustomTableModel extends AbstractTableModel {
    private String[] columns = {"Name: Host Name"};
    private List<Person> clientList;

    /**
     * @param clientList the list of clients
     */
    public CustomTableModel(List<Person> clientList) {
        this.clientList = clientList;
    }

    @Override
    @Attachment
    public String getColumnName(int c) {
        return columns[c];
    }

    @Override
    @Step("Get class of the {0}th column.")
    public Class getColumnClass(int i) {
        return columns[i].getClass();
    }

    /**
     * @return the number of rows in the model
     */
    @Override
    @Step("Get number of rows.")
    public int getRowCount() {
        return clientList.size();
    }

    /**
     * @return the number of columns in the model
     */
    @Override
    @Step("Get number of columns.")
    public int getColumnCount() {
        return columns.length;
    }

    /**
     * @param rowIndex    the row whose value is to be queried
     * @param columnIndex the column whose value is to be queried
     * @return the value Object at the specified cell
     */
    @Override
    @Step("Get value at ({0}, {1}).")
    public Object getValueAt(int rowIndex, int columnIndex) {
        return clientList.get(rowIndex);
    }
}