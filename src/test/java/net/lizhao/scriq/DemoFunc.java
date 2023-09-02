package net.lizhao.scriq;

import net.lizhao.scriq.eval.Evaluator;
import net.lizhao.scriq.eval.Value;

public class DemoFunc extends Evaluator {

    public Value PV(Value i, Value j) {
        return new Value(i);
    }
}
