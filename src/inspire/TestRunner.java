package inspire;

import org.junit.runner.JUnitCore;
import ru.yandex.qatools.allure.junit.AllureRunListener;

public class TestRunner {
    public static void main(String[] args) {
        JUnitCore runner = new JUnitCore();
        runner.addListener(new AllureRunListener());
        runner.run(CustomTableModelTest.class, MiniClientTest.class, MiniServerTest.class, PersonTest.class);
    }
}
