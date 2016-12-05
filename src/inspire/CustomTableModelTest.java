package inspire;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class CustomTableModelTest {
    private List<Person> clientList;
    private CustomTableModel ctm;

    @Before
    public void setUp() throws Exception {
        clientList = new ArrayList<>();
        clientList.add(new Person("test1", "0.0.0.0"));
        clientList.add(new Person("test2", "0.0.0.1"));
        clientList.add(new Person("test3", "0.0.0.2"));
        clientList.add(new Person("test4", "0.0.0.3"));
        ctm = new CustomTableModel(clientList);
    }

    @Test
    public void getColumnName() throws Exception {
        Assert.assertEquals(ctm.getColumnName(0), "Name: Host Name");
    }

    @Test
    public void getColumnClass() throws Exception {
        Assert.assertEquals(ctm.getColumnClass(0), String.class);
    }

    @Test
    public void getRowCount() throws Exception {
        Assert.assertEquals(ctm.getRowCount(), clientList.size());
    }

    @Test
    public void getColumnCount() throws Exception {
        Assert.assertEquals(ctm.getColumnCount(), 1);
    }

    @Test
    public void getValueAt() throws Exception {
        for (int i = 0; i < clientList.size(); i++) {
            Assert.assertEquals(ctm.getValueAt(i, 0), clientList.get(i));
        }
    }
}