package inspire;

import ru.yandex.qatools.allure.annotations.Attachment;
import ru.yandex.qatools.allure.annotations.Step;

import java.util.Objects;

/**
 * This class stores information about a single client - with fields for a human-readable identifier {@code name} and
 * the host name of the client {@code hostName}.
 */
public class Person {
    private String name;
    private String hostName;

    /**
     * @param name     human-readable identifier for the client
     * @param hostName the client's host name
     */
    public Person(String name, String hostName) {
        this.name = name;
        this.hostName = hostName;
    }

    /**
     * @return human-readable identifier for the client
     */
    @Attachment
    public String getName() {
        return name;
    }

    /**
     * @return the client's host name
     */
    @Attachment
    public String getHostName() {
        return hostName;
    }

    @Override
    @Step("Is hostname of {0} equal to hostname of this object?")
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Person person = (Person) o;
        return hostName.equals(person.getHostName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(hostName);
    }

    @Override
    public String toString() {
        return name + ": " + hostName;
    }
}