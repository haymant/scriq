package net.lizhao.scriq;

import net.lizhao.scriq.eval.Evaluator;
import net.lizhao.scriq.eval.Value;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;


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
    void testReturn() {
        String code = "return 1.1";
        Evaluator eval = new Evaluator();
        var val = eval.eval(getTree(code), new HashMap<String, Value>());
        assert (val.equals(1.1));
    }

    @Test
    void testAssign() {
        String code = "i=1.2\nreturn i";
        Evaluator eval = new Evaluator();
        var val = eval.eval(getTree(code), new HashMap<String, Value>());
        assert (val.equals(1.2));
    }

    @Test
    void testIf() {
        String code = "if 1.3==1.3:\n  return 1.3\n";
        Evaluator eval = new Evaluator();
        var val = eval.eval(getTree(code), new HashMap<String, Value>());
        assert (val.equals(1.3));
    }

    @Test
    void testIfElse() {
        String code = "if 1==2:\n  return 1\nelse:\n  return 2.0\n";
        Evaluator eval = new Evaluator();
        var val = eval.eval(getTree(code), new HashMap<String, Value>());
        assert (val.equals(2.0));
    }

    @Test
    void testIfElif() {
        String code = "if 1==2:\n  return 1\nelif 2==2:\n  return 3\nelse:\n  return 2\n";
        Evaluator eval = new Evaluator();
        var val = eval.eval(getTree(code), new HashMap<String, Value>());
        assert (val.equals(3));
    }

    @Test
    void testIfAndComp() {
        String code = "if True and 1==2:\n  return 3\nelse:\n  return 4";
        Evaluator eval = new Evaluator();
        var val = eval.eval(getTree(code), new HashMap<String, Value>());
        assert (val.equals(4));
    }

    @Test
    void testIfOrComp() {
        String code = "if True or 1==2:\n  return 5\nelse:\n  return 4";
        Evaluator eval = new Evaluator();
        var val = eval.eval(getTree(code), new HashMap<String, Value>());
        assert (val.equals(5));
    }

    @Test
    void testFuncCall() {
        String code = "i=6.1\nPV(i,2)\nreturn i";
        DemoFunc eval = new DemoFunc();
        var val = eval.eval(getTree(code), new HashMap<String, Value>());
        assert (val.equals(6.1));
    }

    @Test
    void testFuncCall1() {
        String code = "i=7\nj=PV(i,2)\nreturn j";
        DemoFunc eval = new DemoFunc();
        var val = eval.eval(getTree(code), new HashMap<String, Value>());
        assert (val.equals(7));
    }

    @Test
    void testFuncData() {
        String code = "j=PV(8,1)\ni=9.1\nPV(i,2)\nreturn i";
        DemoFunc eval = new DemoFunc();
        Map<Integer, Object> posMap = new HashMap<Integer, Object>();
        var val = eval.eval(getTree(code), new HashMap<String, Value>(), posMap);
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
    void testString() {
        String code = "j=\"test\"\n\nreturn j";
        Evaluator eval = new Evaluator();
        var val = eval.eval(getTree(code), new HashMap<String, Value>());
        assert (val.equals("test"));
    }

    @Test
    void testStringAdd() {
        String code = "j=\"test_\" + 1\n\nreturn j";
        Evaluator eval = new Evaluator();
        var val = eval.eval(getTree(code), new HashMap<String, Value>());
        assert (val.equals("test_1"));
    }

    @Test
    void testStringAdd1() {
        String code = "j=\"test_\" + \"2\"\n\nreturn j";
        Evaluator eval = new Evaluator();
        var val = eval.eval(getTree(code), new HashMap<String, Value>());
        assert (val.equals("test_2"));
    }

    @Test
    void testWhile() {
        String code = "i=10\nwhile i>3:\n  i=i-1\nreturn i";
        Evaluator eval = new Evaluator();
        var val = eval.eval(getTree(code), new HashMap<String, Value>());
        assert (val.equals(3));
    }

}
