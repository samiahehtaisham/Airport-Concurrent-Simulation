import java.util.concurrent.CompletableFuture;

class FinishRequest extends AtcMessage {

    final CompletableFuture<Void> result;

    FinishRequest(CompletableFuture<Void> result) {
        this.result = result;
    }
}