package inspire;

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
    public String getName() {
        return name;
    }

    /**
     * @return the client's host name
     */
    public String getHostName() {
        return hostName;
    }

    @Override
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
