import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

class Flight implements Runnable {
    
    private final AirportSimulation simulation;
    private final int number;
    private final boolean emergency;
    private final int passengersArriving;
    private final int passengersBoarding;

    private Gate gate;
    private boolean holdingAnnouncementShown = false;

    Flight(
            AirportSimulation simulation,
            int number,
            boolean emergency,
            int passengersArriving,
            int passengersBoarding
    ) {
        this.simulation = simulation;
        this.number = number;
        this.emergency = emergency;
        this.passengersArriving = passengersArriving;
        this.passengersBoarding = passengersBoarding;
    }

    int getNumber() {
        return number;
    }

    boolean isEmergency() {
        return emergency;
    }

    Gate getGate() {
        return gate;
    }

    void setGate(Gate gate) {
        this.gate = gate;
    }

    @Override
    public void run() {

        try {
            if (emergency) {
                AirportSimulation.log(
                        "Emergency landing request from Plane " + number
                );
            } else {
                AirportSimulation.log(
                        "Landing request from Plane " + number
                );
            }

            gate = simulation.atc.requestLanding(this, emergency);

            AirportSimulation.log(
                    "Landing clearance received for Plane "
                            + number
                            + " -> Gate "
                            + gate.getName()
            );

            AirportSimulation.pause(180);

            AirportSimulation.log(
                    "Plane " + number + " landed on runway."
            );

            AirportSimulation.pause(180);

            AirportSimulation.log(
                    "Plane " + number
                            + " reached Gate "
                            + gate.getName()
            );

            simulation.atc.landingRunwayCleared(this);

            if (number <= 2) {
                simulation.firstTwoPlanesDocked.countDown();
            }

            if (number <= 2) {
                simulation.emergencyDocked.await();
            }

            if (emergency) {
                simulation.emergencyDocked.countDown();
            }

            runGroundOperations();

            AirportSimulation.log(
                    "Plane " + number + " requests departure."
            );

            simulation.atc.requestDeparture(this);

            AirportSimulation.log(
                    "Plane " + number
                            + " undocking from Gate "
                            + gate.getName()
            );

            AirportSimulation.pause(180);

            simulation.atc.releaseGate(this);

            AirportSimulation.log(
                    "Plane " + number
                            + " reached runway for departure."
            );

            AirportSimulation.pause(180);

            simulation.atc.departureCompleted(this);

            AirportSimulation.log(
                    "Plane " + number + " has left the airport."
            );

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();

            AirportSimulation.log(
                    "Plane " + number + " interrupted."
            );
        }
    }

    private void runGroundOperations() {

        List<Future<?>> tasks = new ArrayList<>();

        tasks.add(
                simulation.passengerWorkers.submit(() -> {

                    AirportSimulation.log(
                            "Plane " + number
                                    + ": passengers disembarking ("
                                    + passengersArriving
                                    + ")."
                    );

                    AirportSimulation.pause(240);

                    AirportSimulation.log(
                            "Plane " + number
                                    + ": passengers boarding ("
                                    + passengersBoarding
                                    + ")."
                    );

                    AirportSimulation.pause(260);

                    simulation.statistics
                            .addPassengersBoarded(passengersBoarding);
                })
        );

        tasks.add(
                simulation.groundWorkers.submit(() -> {

                    AirportSimulation.log(
                            "Plane " + number
                                    + ": refilling supplies."
                    );

                    AirportSimulation.pause(420);
                })
        );

        tasks.add(
                simulation.groundWorkers.submit(() -> {

                    AirportSimulation.log(
                            "Plane " + number
                                    + ": cleaning aircraft."
                    );

                    AirportSimulation.pause(380);
                })
        );

        tasks.add(
                simulation.fuelTruck.submit(() -> {

                    AirportSimulation.log(
                            "Plane " + number
                                    + ": refuelling."
                    );

                    AirportSimulation.pause(520);
                })
        );

        for (Future<?> task : tasks) {
            AirportSimulation.get(task);
        }

        AirportSimulation.log(
                "Plane " + number
                        + ": all ground operations completed."
        );
    }

    boolean hasHoldingAnnouncement() {
    return holdingAnnouncementShown;
}

    void markHoldingAnnouncement() {
    holdingAnnouncementShown = true;
    }
}
