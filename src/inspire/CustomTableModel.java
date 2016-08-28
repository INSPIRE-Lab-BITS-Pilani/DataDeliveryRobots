package inspire;

import javax.swing.table.AbstractTableModel;
import java.util.List;

class CustomTableModel extends AbstractTableModel {
    private String[] columns = {"Name: IP Address"};
    private List<Person> clientList;

    CustomTableModel(List<Person> clientList) {
        this.clientList = clientList;
    }

    public String getColumnName(int c) {
        return columns[c];
    }

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