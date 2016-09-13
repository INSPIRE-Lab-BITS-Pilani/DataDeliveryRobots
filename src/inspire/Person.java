package inspire;

import java.util.Objects;

class Person {
    private String name;
    private String hostName;

    Person(String name, String hostName) {
        this.name = name;
        this.hostName = hostName;
    }

    String getName() {
        return name;
    }

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
