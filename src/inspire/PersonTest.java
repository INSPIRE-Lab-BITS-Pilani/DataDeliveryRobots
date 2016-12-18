package inspire;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Test for {@link Person} class
 *
 * @author Abhinav Baid, Atishay Jain
 * @version 1.0
 * @since 20-12-2016
 */
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