import java.util.concurrent.CompletableFuture;

class DepartureRequest extends AtcMessage {

    final Flight flight;
    final CompletableFuture<Void> result;
    final long sequence;

    DepartureRequest(
            Flight flight,
            CompletableFuture<Void> result,
            long sequence
    ) {
        this.flight = flight;
        this.result = result;
        this.sequence = sequence;
    }
}