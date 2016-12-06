package inspire;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class PersonTest {
    private String name;
    private String hostName;
    private Person p;

    @Before
    public void setUp() throws Exception {
        name = "test";
        hostName = "localhost";
        p = new Person(name, hostName);
    }

    @Test
    public void getName() throws Exception {
        Assert.assertEquals(p.getName(), name);
    }

    @Test
    public void getHostName() throws Exception {
        Assert.assertEquals(p.getHostName(), hostName);
    }

    @Test
    public void equals() throws Exception {
        Assert.assertTrue(p.equals(new Person(null, hostName)));
    }
}