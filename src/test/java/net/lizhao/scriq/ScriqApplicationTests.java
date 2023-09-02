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
        String code = "return 11";
        Evaluator eval = new Evaluator();
        var val = eval.eval(getTree(code), new HashMap<String, Value>());
        assert (val.equals(11));
    }

    @Test
    void testAssign() {
        String code = "i=20\nreturn i";
        Evaluator eval = new Evaluator();
        var val = eval.eval(getTree(code), new HashMap<String, Value>());
        assert (val.equals(20));
    }

    @Test
    void testIf() {
        String code = "if 1==1:\n  return 1\n";
        Evaluator eval = new Evaluator();
        var val = eval.eval(getTree(code), new HashMap<String, Value>());
        assert (val.equals(1));
    }

    @Test
    void testIfElse() {
        String code = "if 1==2:\n  return 1\nelse:\n  return 2\n";
        Evaluator eval = new Evaluator();
        var val = eval.eval(getTree(code), new HashMap<String, Value>());
        assert (val.equals(2));
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
        String code = "if True or 1==2:\n  return 3\nelse:\n  return 4";
        Evaluator eval = new Evaluator();
        var val = eval.eval(getTree(code), new HashMap<String, Value>());
        assert (val.equals(3));
    }

    @Test
    void testFuncCall() {
        String code = "i=11\nPV(i,2)\nreturn i";
        DemoFunc eval = new DemoFunc();
        var val = eval.eval(getTree(code), new HashMap<String, Value>());
        assert (val.equals(11));
    }

    @Test
    void testFuncCall1() {
        String code = "i=11\nj=PV(i,2)\nreturn j";
        DemoFunc eval = new DemoFunc();
        var val = eval.eval(getTree(code), new HashMap<String, Value>());
        assert (val.equals(11));
    }

    @Test
    void testFuncData() {
        String code = "j=PV(33,1)\ni=11\nPV(i,2)\nreturn i";
        DemoFunc eval = new DemoFunc();
        Map<Integer, Object> posMap = new HashMap<Integer, Object>();
        var val = eval.eval(getTree(code), new HashMap<String, Value>(), posMap);
        assert (val.equals(11));
        assert (((Value[])posMap.get(2))[0].equals(33));
        assert (((Value[])posMap.get(2))[1].equals(1));
        assert (((Value[])posMap.get(16))[0].equals(11));
        assert (((Value[])posMap.get(16))[1].equals(2));
    }

    @Test
    void testTree() {
        String code = "j=PV(33,1)\ni=11\nPV(i,2)\nreturn i";
        Map<String, Object> treeMap = new HashMap<String, Object>();
        Evaluator.genTree(getTree(code), treeMap);
        assert (treeMap.size()>0);
    }

}
