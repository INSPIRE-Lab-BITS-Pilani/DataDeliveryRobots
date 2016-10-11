package inspire;

import javax.swing.table.AbstractTableModel;
import java.util.List;

/**
 * This class provides an implementation of the {@code TableModel} interface tailored to view the list of clients
 * {@code clientList}.
 */
class CustomTableModel extends AbstractTableModel {
    private String[] columns = {"Name: Host Name"};
    private List<Person> clientList;

    /**
     * @param clientList the list of clients
     */
    CustomTableModel(List<Person> clientList) {
        this.clientList = clientList;
    }

    @Override
    public String getColumnName(int c) {
        return columns[c];
    }

    @Override
    public Class getColumnClass(int i) {
        return columns[i].getClass();
    }

    /**
     * @return the number of rows in the model
     */
    @Override
    public int getRowCount() {
        return clientList.size();
    }

    /**
     * @return the number of columns in the model
     */
    @Override
    public int getColumnCount() {
        return columns.length;
    }

    /**
     * @param rowIndex the row whose value is to be queried
     * @param columnIndex the column whose value is to be queried
     * @return the value Object at the specified cell
     */
    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        return clientList.get(rowIndex);
    }
}