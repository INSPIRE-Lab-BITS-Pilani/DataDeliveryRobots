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

    @Override
    public int getRowCount() {
        return clientList.size();
    }

    @Override
    public int getColumnCount() {
        return columns.length;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        return clientList.get(rowIndex);
    }
}