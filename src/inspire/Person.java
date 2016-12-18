package inspire;

import ru.yandex.qatools.allure.annotations.Attachment;
import ru.yandex.qatools.allure.annotations.Step;

import java.util.Objects;

/**
 * This class stores information about a single client,
 * with fields for a human-readable identifier {@code name} and
 * the host name of the client {@code hostName}.
 *
 * @author Abhinav Baid, Atishay Jain
 * @version 1.0
 * @since 20-12-2016
 */
class Person {
    /**
     * Name of the client
     */
    private final String name;
    /**
     * Host name of the client
     */
    private final String hostName;

    /**
     * Initialises a new client (person)
     * @param name     human-readable identifier for the client
     * @param hostName the client's host name
     */
    public Person(String name, String hostName) {
        this.name = name;
        this.hostName = hostName;
    }

    /**
     * Get client name
     *
     * @return human-readable identifier for the client
     */
    @Attachment
    public String getName() {
        return name;
    }

    /**
     * Get client host name
     *
     * @return the client's host name
     */
    @Attachment
    public String getHostName() {
        return hostName;
    }

    /**
     * Compares two person objects by their host name
     *
     * @param o Object to be compared
     * @return {@code true} if equal, {@code false} if not
     */
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

    /**
     * Get object hash code
     *
     * @return Object hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(hostName);
    }

    /**
     * Get string representation
     *
     * @return String representation
     */
    @Override
    public String toString() {
        return name + ": " + hostName;
    }
}