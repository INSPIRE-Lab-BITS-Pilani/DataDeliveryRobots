package inspire;

import org.junit.runner.JUnitCore;
import ru.yandex.qatools.allure.junit.AllureRunListener;

/**
 * Test for entire project
 *
 * @author Abhinav Baid, Atishay Jain
 * @version 1.0
 * @since 20-12-2016
 */
class TestRunner {
    public static void main(String[] args) {
        JUnitCore runner = new JUnitCore();
        runner.addListener(new AllureRunListener());
        runner.run(CustomTableModelTest.class, MiniClientTest.class, MiniServerTest.class, PersonTest.class);
    }
}