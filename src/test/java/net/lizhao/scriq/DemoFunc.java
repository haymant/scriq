package net.lizhao.scriq;

import net.lizhao.scriq.eval.Evaluator;
import net.lizhao.scriq.eval.Value;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

public class DemoFunc extends Evaluator {

    public Value PV(Value i, Value j) {
        return new Value(i);
    }

    public CompletableFuture<Value> getIntAsync() throws InterruptedException {
        CompletableFuture<Value> completableFuture = new CompletableFuture<>();

        Executors.newCachedThreadPool().submit(() -> {
            Thread.sleep(500);
            completableFuture.complete(new Value(new BigDecimal(2.0)));
            return null;
        });

        return completableFuture;
    }
}
