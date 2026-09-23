class DepartureCompleted extends AtcMessage {

    final Flight flight;

    DepartureCompleted(Flight flight) {
        this.flight = flight;
    }
}