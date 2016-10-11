package inspire;

import java.util.Objects;

/**
 * This class stores information about a single client - with fields for a human-readable identifier {@code name} and
 * the host name of the client {@code hostName}.
 */
class Person {
    private String name;
    private String hostName;

    /**
     * @param name Human-readable identifier for the client
     * @param hostName The client's host name
     */
    Person(String name, String hostName) {
        this.name = name;
        this.hostName = hostName;
    }

    /**
     * @return Human-readable identifier for the client
     */
    String getName() {
        return name;
    }

    /**
     * @return The client's host name
     */
    String getHostName() {
        return hostName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Person person = (Person) o;
        return Objects.equals(hostName, person.hostName);
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
