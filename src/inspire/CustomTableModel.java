package inspire;

import ru.yandex.qatools.allure.annotations.Attachment;
import ru.yandex.qatools.allure.annotations.Step;

import javax.swing.table.AbstractTableModel;
import java.util.List;

/**
 * This class provides an implementation of the {@code TableModel}
 * interface tailored to view the list of clients {@code clientList}.
 *
 * @author Abhinav Baid, Atishay Jain
 * @version 1.0
 * @since 20-12-2016
 */
class CustomTableModel extends AbstractTableModel {
    /**
     * Array of column headings of the table
     */
    private final String[] columns = {"Name: Host Name"};
    /**
     * List of clients
     */
    private final List<Person> clientList;

    /**
     * Construct the table from a client list
     *
     * @param clientList The list of clients
     */
    public CustomTableModel(List<Person> clientList) {
        this.clientList = clientList;
    }

    /**
     * Returns column name
     *
     * @param c Index of column
     * @return Name of the column
     */
    @Override
    @Attachment
    public String getColumnName(int c) {
        return columns[c];
    }

    /**
     * Returns class of column indexed by {@code i}
     *
     * @param i Index of column
     * @return Class of the column
     */
    @Override
    @Step("Get class of the {0}th column.")
    public Class getColumnClass(int i) {
        return columns[i].getClass();
    }

    /**
     * Returns number of clients
     *
     * @return The number of rows in the model
     */
    @Override
    @Step("Get number of rows.")
    public int getRowCount() {
        return clientList.size();
    }

    /**
     * Returns number of columns
     *
     * @return The number of columns in the model
     */
    @Override
    @Step("Get number of columns.")
    public int getColumnCount() {
        return columns.length;
    }

    /**
     * Returns value at ({@code rowIndex}, {@code columnIndex})
     *
     * @param rowIndex    The row whose value is to be queried
     * @param columnIndex The column whose value is to be queried
     * @return The value Object at the specified cell
     */
    @Override
    @Step("Get value at ({0}, {1}).")
    public Object getValueAt(int rowIndex, int columnIndex) {
        return clientList.get(rowIndex);
    }
}