import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

class AirTrafficController extends Thread {

    private final AirportSimulation simulation;

    private final BlockingQueue<AtcMessage> mailbox = new LinkedBlockingQueue<>();

    private final Gate gateA = new Gate("Gate A");
    private final Gate gateB = new Gate("Gate B");
    private final Gate gateC = new Gate("Gate C");

    private final List<LandingRequest> waitingLandings = new ArrayList<>();
    private final List<DepartureRequest> waitingDepartures = new ArrayList<>();

    private boolean runwayBusy = false;
    private Flight runwayFlight = null;
    private boolean runwayUsedForLanding = false;

    private int planesOnGround = 0;

    // Gate C is kept available for the emergency plane.
    private boolean emergencyGateReserved = true;

    private final AtomicLong nextRequestSequence = new AtomicLong(1);

    private boolean finished = false;

    AirTrafficController(AirportSimulation simulation) {
        super("ATC-Manager");
        this.simulation = simulation;
    }

    // ---------------------------------------------------------
    // Requests from planes
    // ---------------------------------------------------------

    Gate requestLanding(Flight flight, boolean emergency) {

        CompletableFuture<Gate> result = new CompletableFuture<>();

        try {
            mailbox.put(
                new LandingRequest(
                    flight,
                    emergency,
                    result,
                    nextRequestSequence.getAndIncrement()
                )
            );

            // Wait until ATC actually gives landing clearance.
            return result.get();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(
                "Landing request interrupted", e
            );

        } catch (ExecutionException e) {
            throw new IllegalStateException(
                "Landing request failed", e
            );
        }
    }

    void landingRunwayCleared(Flight flight) {

        try {
            mailbox.put(new LandingRunwayCleared(flight));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(
                "Could not notify ATC that runway is clear", e
            );
        }
    }

    void requestDeparture(Flight flight) {

        CompletableFuture<Void> result = new CompletableFuture<>();

        try {
            mailbox.put(
                new DepartureRequest(
                    flight,
                    result,
                    nextRequestSequence.getAndIncrement()
                )
            );

            // Wait until ATC gives departure clearance.
            result.get();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(
                "Departure request interrupted", e
            );

        } catch (ExecutionException e) {
            throw new IllegalStateException(
                "Departure request failed", e
            );
        }
    }

    void releaseGate(Flight flight) {

        try {
            mailbox.put(new GateReleased(flight));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(
                "Could not notify ATC that gate was released", e
            );
        }
    }

    void departureCompleted(Flight flight) {

        try {
            mailbox.put(new DepartureCompleted(flight));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(
                "Could not notify ATC that departure completed", e
            );
        }
    }

    void finishAndReport() {

        CompletableFuture<Void> result = new CompletableFuture<>();

        try {
            mailbox.put(new FinishRequest(result));
            result.get();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(
                "ATC finish request interrupted", e
            );

        } catch (ExecutionException e) {
            throw new IllegalStateException(
                "ATC finish request failed", e
            );
        }
    }

    // ---------------------------------------------------------
    // ATC thread
    // ---------------------------------------------------------

    @Override
    public void run() {

        while (!finished) {

            try {

                AtcMessage message = mailbox.take();

                if (message instanceof LandingRequest) {

                    LandingRequest request =
                        (LandingRequest) message;

                    waitingLandings.add(request);

                    log(
                        "Received landing request from Plane "
                        + request.flight.getNumber()
                        + (request.emergency
                            ? " [EMERGENCY]"
                            : "")
                    );

                    /*
                     * Planes 3 and 4 are used to create the
                     * required congestion scenario.
                     */
                    int number = request.flight.getNumber();

                    if (number == 3 || number == 4) {
                        simulation.firstTwoHoldingRequestsReceived
                            .countDown();
                    }

                } else if (message instanceof LandingRunwayCleared) {

                    LandingRunwayCleared cleared =
                        (LandingRunwayCleared) message;

                    if (runwayBusy
                            && runwayFlight == cleared.flight
                            && runwayUsedForLanding) {

                        runwayBusy = false;
                        runwayFlight = null;
                        runwayUsedForLanding = false;

                        log(
                            "Runway cleared after landing of Plane "
                            + cleared.flight.getNumber()
                        );
                    }

                } else if (message instanceof DepartureRequest) {

                    DepartureRequest request =
                        (DepartureRequest) message;

                    waitingDepartures.add(request);

                    log(
                        "Received departure request from Plane "
                        + request.flight.getNumber()
                    );

                } else if (message instanceof GateReleased) {

                    GateReleased released =
                        (GateReleased) message;

                    releaseAssignedGate(released.flight);

                } else if (message instanceof DepartureCompleted) {

                    DepartureCompleted completed =
                        (DepartureCompleted) message;

                    if (runwayBusy
                            && runwayFlight == completed.flight
                            && !runwayUsedForLanding) {

                        runwayBusy = false;
                        runwayFlight = null;

                        planesOnGround--;

                        simulation.statistics.planeServed();

                        log(
                            "Plane "
                            + completed.flight.getNumber()
                            + " completed departure. "
                            + "Planes on ground: "
                            + planesOnGround
                        );
                    }

                } else if (message instanceof FinishRequest) {

                    reportSanityChecksAndStatistics();

                    FinishRequest finish =
                        (FinishRequest) message;

                    finish.result.complete(null);

                    finished = true;
                }

                if (!(message instanceof FinishRequest)) {
                    dispatchNextRunwayOperation();
                    announceHoldingPlanes();
                }

            } catch (InterruptedException e) {

                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    // ---------------------------------------------------------
    // Runway dispatching
    // ---------------------------------------------------------

    private void dispatchNextRunwayOperation() {

        if (runwayBusy) {
            return;
        }

        /*
         * Landing requests have priority over departures.
         * Emergency landing requests have priority over
         * normal landing requests.
         */
        LandingRequest landing = findGrantableLanding();

        if (landing != null) {

            Gate gate = selectGate(landing.emergency);

            if (gate == null) {
                return;
            }

            waitingLandings.remove(landing);

            gate.setAssignedFlight(landing.flight);

            planesOnGround++;

            runwayBusy = true;
            runwayFlight = landing.flight;
            runwayUsedForLanding = true;

            if (landing.emergency) {
                emergencyGateReserved = false;
            }

            long waitingTime =
                System.currentTimeMillis()
                - landing.requestTimeMillis;

            simulation.statistics.recordWaitingTime(
                waitingTime
            );

            log(
                "Landing clearance granted to Plane "
                + landing.flight.getNumber()
                + " -> "
                + gate.getName()
                + " | Wait: "
                + waitingTime
                + " ms"
            );

            landing.result.complete(gate);

            return;
        }

        /*
         * If no landing can be granted, check whether
         * a plane is waiting to depart.
         */
        if (!waitingDepartures.isEmpty()) {

            DepartureRequest departure =
                waitingDepartures.remove(0);

            runwayBusy = true;
            runwayFlight = departure.flight;
            runwayUsedForLanding = false;

            log(
                "Departure clearance granted to Plane "
                + departure.flight.getNumber()
            );

            departure.result.complete(null);
        }
    }

    // ---------------------------------------------------------
    // Landing selection
    // ---------------------------------------------------------

    private LandingRequest findGrantableLanding() {

        if (planesOnGround >= 3) {
            return null;
        }

        if (waitingLandings.isEmpty()) {
            return null;
        }

        /*
         * Emergency requests are always considered first.
         * Otherwise requests are handled according to
         * their original sequence.
         */
        waitingLandings.sort(
            Comparator
                .comparing(
                    (LandingRequest request)
                        -> !request.emergency
                )
                .thenComparingLong(
                    request -> request.sequence
                )
        );

        for (LandingRequest request : waitingLandings) {

            if (selectGate(request.emergency) != null) {
                return request;
            }
        }

        return null;
    }

    // ---------------------------------------------------------
    // Gate selection
    // ---------------------------------------------------------

    private Gate selectGate(boolean emergency) {

        /*
         * Emergency aircraft must use Gate C.
         */
        if (emergency) {

            if (gateC.isFree()) {
                return gateC;
            }

            return null;
        }

        /*
         * Keep Gate C available for the emergency aircraft
         * until the emergency landing happens.
         */
        if (emergencyGateReserved) {

            if (gateA.isFree()) {
                return gateA;
            }

            if (gateB.isFree()) {
                return gateB;
            }

            return null;
        }

        /*
         * After the emergency aircraft has been assigned,
         * Gate C can be used normally.
         */
        if (gateA.isFree()) {
            return gateA;
        }

        if (gateB.isFree()) {
            return gateB;
        }

        if (gateC.isFree()) {
            return gateC;
        }

        return null;
    }

    // ---------------------------------------------------------
    // Gate release
    // ---------------------------------------------------------

    private void releaseAssignedGate(Flight flight) {

        Gate[] gates = {
            gateA,
            gateB,
            gateC
        };

        for (Gate gate : gates) {

            if (gate.getAssignedFlight() == flight) {

                gate.setAssignedFlight(null);

                log(
                    gate.getName()
                    + " released by Plane "
                    + flight.getNumber()
                );

                return;
            }
        }
    }

    // ---------------------------------------------------------
    // Holding announcements
    // ---------------------------------------------------------

    private void announceHoldingPlanes() {

    for (LandingRequest request : waitingLandings) {

        if (planesOnGround >= 3
                || selectGate(request.emergency) == null) {

            if (!request.flight.hasHoldingAnnouncement()) {

                log(
                    "Plane "
                    + request.flight.getNumber()
                    + " is holding because airport capacity "
                    + "is currently unavailable."
                );

                request.flight.markHoldingAnnouncement();
            }
        }
    }
}

    // ---------------------------------------------------------
    // Final checks and statistics
    // ---------------------------------------------------------

    private void reportSanityChecksAndStatistics() {

        boolean allGatesEmpty =
            gateA.isFree()
            && gateB.isFree()
            && gateC.isFree();

        boolean runwayClear = !runwayBusy;
        boolean groundClear = planesOnGround == 0;

        log("----------------------------------------");
        log("FINAL AIRPORT CHECKS");
        log("All gates empty: " + allGatesEmpty);
        log("Runway clear: " + runwayClear);
        log("Airport grounds clear: " + groundClear);

        AirportStatistics.Snapshot snapshot =
            simulation.statistics.snapshot();

        log("----------------------------------------");
        log("FINAL STATISTICS");
        log("Planes served: "
            + snapshot.planesServed);

        log("Passengers boarded: "
            + snapshot.passengersBoarded);

        log("Minimum waiting time: "
            + snapshot.minimumWaitMillis
            + " ms");

        log("Average waiting time: "
            + String.format(
                "%.1f",
                snapshot.averageWaitMillis
            )
            + " ms");

        log("Maximum waiting time: "
            + snapshot.maximumWaitMillis
            + " ms");

        log("----------------------------------------");
    }

    // ---------------------------------------------------------
    // Logging
    // ---------------------------------------------------------

    private void log(String message) {

        System.out.printf(
            "%-18s | %s%n",
            Thread.currentThread().getName(),
            message
        );
    }
}