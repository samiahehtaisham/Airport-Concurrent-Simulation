import java.util.concurrent.CompletableFuture;

class LandingRequest extends AtcMessage {

    final Flight flight;
    final boolean emergency;
    final CompletableFuture<Gate> result;
    final long sequence;
    final long requestTimeMillis;

    LandingRequest(
            Flight flight,
            boolean emergency,
            CompletableFuture<Gate> result,
            long sequence
    ) {
        this.flight = flight;
        this.emergency = emergency;
        this.result = result;
        this.sequence = sequence;
        this.requestTimeMillis = System.currentTimeMillis();
    }
}