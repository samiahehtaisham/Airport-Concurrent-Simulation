class LandingRunwayCleared extends AtcMessage {

    final Flight flight;

    LandingRunwayCleared(Flight flight) {
        this.flight = flight;
    }
}