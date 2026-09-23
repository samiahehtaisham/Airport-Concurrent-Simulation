class GateReleased extends AtcMessage {

    final Flight flight;

    GateReleased(Flight flight) {
        this.flight = flight;
    }
}