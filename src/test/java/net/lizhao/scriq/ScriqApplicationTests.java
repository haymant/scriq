package net.lizhao.scriq;

import net.lizhao.scriq.eval.Evaluator;
import net.lizhao.scriq.eval.Value;
import net.lizhao.scriq.py.Python3Lexer;
import net.lizhao.scriq.py.Python3Parser;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

class ScriqApplicationTests {


    private static Evaluator eval = new Evaluator();


    private ParseTree getTree(String code) {
        CharStream is = CharStreams.fromString(code);
        Python3Lexer lexer = new Python3Lexer(is);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        Python3Parser parser = new Python3Parser(tokens);
        ParseTree tree = parser.source(); // parse
        return tree;
    }


    @Test
    void testReturn() throws ExecutionException, InterruptedException {
        String code = "return 1.1";
        Evaluator eval = new Evaluator();
        var val = eval.eval(getTree(code), new HashMap<String, Value>());
        assert (val.equals(1.1));
    }

    @Test
    void testAssign() throws ExecutionException, InterruptedException {
        String code = "i=1.2\nreturn i";
        Evaluator eval = new Evaluator();
        var val = eval.eval(getTree(code), new HashMap<String, Value>());
        assert (val.equals(1.2));
    }

    @Test
    void testIf() throws ExecutionException, InterruptedException {
        String code = "if 1.3==1.3:\n  return 1.3\n";
        Evaluator eval = new Evaluator();
        var val = eval.eval(getTree(code), new HashMap<String, Value>());
        assert (val.equals(1.3));
    }

    @Test
    void testIfElse() throws ExecutionException, InterruptedException {
        String code = "if 1==2:\n  return 1\nelse:\n  return 2.0\n";
        Evaluator eval = new Evaluator();
        var val = eval.eval(getTree(code), new HashMap<String, Value>());
        assert (val.equals(2.0));
    }

    @Test
    void testIfElif() throws ExecutionException, InterruptedException {
        String code = "if 1==2:\n  return 1\nelif 2==2:\n  return 3\nelse:\n  return 2\n";
        Evaluator eval = new Evaluator();
        var val = eval.eval(getTree(code), new HashMap<String, Value>());
        assert (val.equals(3));
    }

    @Test
    void testIfAndComp() throws ExecutionException, InterruptedException {
        String code = "if True and 1==2:\n  return 3\nelse:\n  return 4";
        Evaluator eval = new Evaluator();
        var val = eval.eval(getTree(code), new HashMap<String, Value>());
        assert (val.equals(4));
    }

    @Test
    void testIfOrComp() throws ExecutionException, InterruptedException {
        String code = "if True or 1==2:\n  return 5\nelse:\n  return 4";
        Evaluator eval = new Evaluator();
        var val = eval.eval(getTree(code), new HashMap<String, Value>());
        assert (val.equals(5));
    }

    @Test
    void testFuncCall() throws ExecutionException, InterruptedException {
        String code = "i=6.1\nPV(i,2)\nreturn i";
        DemoFunc eval = new DemoFunc();
        var val = eval.eval(getTree(code), new HashMap<String, Value>());
        assert (val.equals(6.1));
    }

    @Test
    void testFuncCall1() throws ExecutionException, InterruptedException {
        String code = "i=7\nj=PV(i,2)\nreturn j";
        DemoFunc eval = new DemoFunc();
        var val = eval.eval(getTree(code), new HashMap<String, Value>());
        assert (val.equals(7));
    }

    @Test
    void testFuncData() throws ExecutionException, InterruptedException {
        String code = "j=PV(8,1)\ni=9.1\nPV(i,2)\nreturn i";
        DemoFunc eval = new DemoFunc();
        Map<Integer, Object> posMap = new HashMap<Integer, Object>();
        var val = eval.eval(getTree(code), new HashMap<String, Value>(), posMap, false);
        assert (val.equals(9.1));
        assert (((Value[])posMap.get(2))[0].equals(8));
        assert (((Value[])posMap.get(2))[1].equals(1));
        assert (((Value[])posMap.get(16))[0].equals(9.1));
        assert (((Value[])posMap.get(16))[1].equals(2));
    }

    @Test
    void testTree() {
        String code = "j=PV(10,1)\ni=11\nPV(i,2)\nreturn i";
        Map<String, Object> treeMap = new HashMap<String, Object>();
        Evaluator.genTree(getTree(code), treeMap);
        assert (treeMap.size()>0);
    }

    @Test
    void testString() throws ExecutionException, InterruptedException {
        String code = "j=\"test\"\n\nreturn j";
        Evaluator eval = new Evaluator();
        var val = eval.eval(getTree(code), new HashMap<String, Value>());
        assert (val.equals("test"));
    }

    @Test
    void testStringAdd() throws ExecutionException, InterruptedException {
        String code = "j=\"test_\" + 1\n\nreturn j";
        Evaluator eval = new Evaluator();
        var val = eval.eval(getTree(code), new HashMap<String, Value>());
        assert (val.equals("test_1.0"));
    }

    @Test
    void testStringAdd1() throws ExecutionException, InterruptedException {
        String code = "j=\"test_\" + \"2\"\n\nreturn j";
        Evaluator eval = new Evaluator();
        var val = eval.eval(getTree(code), new HashMap<String, Value>());
        assert (val.equals("test_2"));
    }

    @Test
    void testWhile() throws ExecutionException, InterruptedException {
        String code = "i=10\nwhile i>3:\n  i=i-1\nreturn i";
        Evaluator eval = new Evaluator();
        var val = eval.eval(getTree(code), new HashMap<String, Value>());
        assert (val.equals(3));
    }

    @Test
    void testWhileBreak() throws ExecutionException, InterruptedException {
        String code = "i=10\nwhile i>3:\n  i=i-1\n  if i==5:\n    break\nreturn i";
        Evaluator eval = new Evaluator();
        var val = eval.eval(getTree(code), new HashMap<String, Value>());
        assert (val.equals(5));
    }

    @Test
    void testEnv() throws ExecutionException, InterruptedException {
        String code = "return i+j";
        Evaluator eval = new Evaluator();
        var map = new HashMap<String, Value>();
        map.put("i", new Value(2.1));
        map.put("j", new Value(2.43));
        var val = eval.eval(getTree(code), map);
        assert (val.equals(4.53));
    }

    @Test
    void testPresetFuncArgs() throws ExecutionException, InterruptedException {
        String code = "j=PV(10,1)\nreturn j";
        Map<Integer, Object> treeMap = new HashMap<Integer, Object>();
        treeMap.put(2, new Value[]{new Value(50), new Value(0)});
        DemoFunc eval = new DemoFunc();
        var val = eval.eval(getTree(code), new HashMap<String, Value>(), treeMap, false);
        assert (val.equals(50));
    }

    @Test
    void testMultkIdx() throws ExecutionException, InterruptedException {
        String code = """
                a = [1, 2, 3]
                return a[1]
                """;
        Evaluator eval = new Evaluator();
        var val = eval.eval(getTree(code), new HashMap<String, Value>());
        assert (val.equals(2));
    }

    @Test
    void testMultkIdx1() throws ExecutionException, InterruptedException {
        String code = """
                a = [[[1, 2, 3], [4, 5, 6]], [[7, 8, 9],[10, 11, 12]],[[13, 14, 15],[16, 17, 18]]]
                return a[0,1,2]
                """;
        Evaluator eval = new Evaluator();
        var val = eval.eval(getTree(code), new HashMap<String, Value>());
        assert (val.equals(6));
    }

    @Test
    void testMultik() throws ExecutionException, InterruptedException {
        String code = """
                a = [[[1, 2, 3], [4, 5, 6]], [[7, 8, 9],[10, 11, 12]],[[13, 14, 15],[16, 17, 18]]]
                b = a[1] + a[0] - a[2] * a[1] / a[2]
                c = b[1] * b[1]
                return c .* c
                """;
        Evaluator eval = new Evaluator();
        var val = eval.eval(getTree(code), new HashMap<String, Value>());
        assert (val.equals(2177));
    }
    @Test
    void testMultikSlice() throws ExecutionException, InterruptedException {
        String code = """
                a = [[[1, 2, 3], [4, 5, 6]], [[7, 8, 9],[10, 11, 12]],[[13, 14, 15],[16, 17, 18]]]
                b =  a[1:, :, :1] 
                c = b[0,0]
                return c .* c
                """;
        Evaluator eval = new Evaluator();
        var val = eval.eval(getTree(code), new HashMap<String, Value>());
        assert (val.equals(113));
    }

    @Test
    void testFuture() throws ExecutionException, InterruptedException {
        String code = "j=getIntAsync()+getIntAsync()\nreturn j";
        Map<Integer, Object> treeMap = new HashMap<Integer, Object>();
        DemoFunc eval = new DemoFunc();
        long start = System.currentTimeMillis();
        var val = eval.eval(getTree(code), new HashMap<String, Value>());
        long end = System.currentTimeMillis();
        assert(end-start>=500);
        assert (val.equals(4));
    }

    @Test
    void testFutureAsync() throws ExecutionException, InterruptedException {
        String code = "j=getIntAsync()+getIntAsync()+getIntAsync()\nreturn j";
        Map<Integer, Object> treeMap = new HashMap<Integer, Object>();
        DemoFunc eval = new DemoFunc();
        var val = eval.evalAsync(getTree(code), new HashMap<String, Value>());
        assert(val.isFuture());
        assert (val.asFuture().get().equals(6));
    }
}
