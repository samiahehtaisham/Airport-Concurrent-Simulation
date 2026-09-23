class Gate {

    private final String name;
    private Flight assignedFlight;

    Gate(String name) {
        this.name = name;
    }

    String getName() {
        return name;
    }

    Flight getAssignedFlight() {
        return assignedFlight;
    }

    void setAssignedFlight(Flight assignedFlight) {
        this.assignedFlight = assignedFlight;
    }

    boolean isFree() {
        return assignedFlight == null;
    }
}