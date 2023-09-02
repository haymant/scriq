package net.lizhao.scriq.eval;


import net.lizhao.scriq.Python3BaseVisitor;
import net.lizhao.scriq.Python3Parser;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNodeImpl;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.*;

/**
 * The instances of classes that implement script language evaluator
 */
public class Evaluator extends Python3BaseVisitor<Value> {

    @Override public Value visitAssignment_stmt(Python3Parser.Assignment_stmtContext ctx) {
        String id = ctx.NAME().getText();  // id is left-hand side of '='
        Value val = visit(ctx.expr());   // compute value of expression on right
        if (val.value instanceof  Value) {
            memory.put(id, (Value)val.value);           // store it in our memory
        } else {
            memory.put(id, val);           // store it in our memory
        }

        return val;
    }


    @Override public Value visitNameAtom(Python3Parser.NameAtomContext ctx) {
        String name = ctx.getText();
        Value val = memory.get(name);
        if(val == null) {
            throw new RuntimeException("no such variable: " + val);
        }
        return val;
    }

    @Override public Value visitStringAtom(Python3Parser.StringAtomContext ctx) {
        String str = ctx.getText();
        // strip quotes
        str = str.substring(1, str.length() - 1).replace("\"\"", "\"");
        return new Value(str);
    }

    @Override public Value visitNumberAtom(Python3Parser.NumberAtomContext ctx) {
        return new Value(new BigDecimal(ctx.getText()));
    }

    @Override public Value visitBooleanAtom(Python3Parser.BooleanAtomContext ctx) {
        return new Value(Boolean.valueOf(ctx.getText()));
    }

    @Override public Value visitNilAtom(Python3Parser.NilAtomContext ctx) {
        return new Value(null);
    }

    @Override public Value visitParExpr(Python3Parser.ParExprContext ctx) {
        return visit(ctx.expr());
    }

    @Override public Value visitPowExpr(Python3Parser.PowExprContext ctx) {
        Value left = visit(ctx.expr(0));
        Value right = visit(ctx.expr(1));
        return new Value(left.asBigDecimal().pow(right.asBigDecimal().intValue()));
    }

    @Override
    public Value visitUnaryMinusExpr(Python3Parser.UnaryMinusExprContext ctx) {
        Value value = visit(ctx.expr());
        return new Value(value.asBigDecimal().negate());
    }

    @Override
    public Value visitNotExpr(Python3Parser.NotExprContext ctx) {
        Value value = visit(ctx.expr());
        return new Value(!value.asBoolean());
    }

    @Override
    public Value visitMultiplicationExpr(@NotNull Python3Parser.MultiplicationExprContext ctx) {

        Value left = visit(ctx.expr(0));
        Value right = visit(ctx.expr(1));

        switch (ctx.op.getType()) {
            case Python3Parser.MUL:
                return new Value(left.asBigDecimal().multiply(right.asBigDecimal()));
            case Python3Parser.DIV:
                return new Value(left.asBigDecimal().divide(right.asBigDecimal(),MathContext.DECIMAL128));
            case Python3Parser.MOD:
                return new Value(left.asBigDecimal().intValue() % right.asBigDecimal().intValue());
            default:
                throw new RuntimeException("unknown operator: " + Python3Parser.tokenNames[ctx.op.getType()]);
        }
    }

    @Override
    public Value visitAdditiveExpr(@NotNull Python3Parser.AdditiveExprContext ctx) {

        Value left = visit(ctx.expr(0));
        Value right = visit(ctx.expr(1));

        switch (ctx.op.getType()) {
            case Python3Parser.ADD:
                return left.isBigDecimal() && right.isBigDecimal() ?
                        new Value(left.asBigDecimal().add(right.asBigDecimal())) :
                        new Value(left.asString() + right.asString());
            case Python3Parser.SUB:
                return new Value(left.asBigDecimal().subtract(right.asBigDecimal()));
            default:
                throw new RuntimeException("unknown operator: " + Python3Parser.tokenNames[ctx.op.getType()]);
        }
    }

    @Override
    public Value visitRelationalExpr(@NotNull Python3Parser.RelationalExprContext ctx) {

        Value left = this.visit(ctx.expr(0));
        Value right = this.visit(ctx.expr(1));

        var comp = left.asBigDecimal().compareTo(right.asBigDecimal());
        switch (ctx.op.getType()) {
            case Python3Parser.LT:
                return new Value(comp<0);
            case Python3Parser.LTEQ:
                return new Value(!(comp>0));
            case Python3Parser.GT:
                return new Value(comp>0);
            case Python3Parser.GTEQ:
                return new Value(!(comp<0));
            default:
                throw new RuntimeException("unknown operator: " + Python3Parser.tokenNames[ctx.op.getType()]);
        }
    }

    final BigDecimal epsilon = new BigDecimal(00000000001);
    @Override
    public Value visitEqualityExpr(@NotNull Python3Parser.EqualityExprContext ctx) {

        Value left = visit(ctx.expr(0));
        Value right = visit(ctx.expr(1));

        switch (ctx.op.getType()) {
            case Python3Parser.EQ:
                return left.isBigDecimal() && right.isBigDecimal() ?
                        new Value(left.asBigDecimal().subtract(right.asBigDecimal()).abs().compareTo(epsilon) < 0) :
                        new Value(left.equals(right));
            case Python3Parser.NEQ:
                return left.isBigDecimal() && right.isBigDecimal() ?
                        new Value(left.asBigDecimal().subtract(right.asBigDecimal()).abs().compareTo(epsilon) >= 0) :
                        new Value(!left.equals(right));
            default:
                throw new RuntimeException("unknown operator: " + Python3Parser.tokenNames[ctx.op.getType()]);
        }
    }

    @Override
    public Value visitAndExpr(Python3Parser.AndExprContext ctx) {
        Value left = visit(ctx.expr(0));
        Value right = visit(ctx.expr(1));
        return new Value(left.asBoolean() && right.asBoolean());
    }

    @Override
    public Value visitOrExpr(Python3Parser.OrExprContext ctx) {
        Value left = visit(ctx.expr(0));
        Value right = visit(ctx.expr(1));
        return new Value(left.asBoolean() || right.asBoolean());
    }

    // log override
    @Override
    public Value visitPrint_stmt(Python3Parser.Print_stmtContext ctx) {
        Value value = visit(ctx.expr());
        System.out.println(value);
        return value;
    }

    @Override
    public Value visitIf_stmt(Python3Parser.If_stmtContext ctx) {

        List<Python3Parser.Test_blockContext> tests =  ctx.test_block();

        boolean evaluatedBlock = false;

        for(Python3Parser.Test_blockContext test : tests) {

            Value evaluated = visit(test.expr());

            if(evaluated.asBoolean()) {
                evaluatedBlock = true;
                // evaluate this block whose expr==true
                this.visit(test.block());
                break;
            }
        }

        if(!evaluatedBlock && ctx.block() != null) {
            // evaluate the else-stat_block (if present == not null)
            this.visit(ctx.block());
        }

        return Value.VOID;
    }

    @Override
    public Value visitWhile_stmt(Python3Parser.While_stmtContext ctx) {

        Value value = visit(ctx.expr());

        while(value.asBoolean()) {

            // evaluate the code block
            visit(ctx.block());

            // evaluate the expression
            value = visit(ctx.expr());
        }

        return Value.VOID;
    }

    private Value ret;
    @Override public Value visitReturn_stmt(Python3Parser.Return_stmtContext ctx) {
        ret = visit(ctx.expr());
        return ret;
    }

    @Override public Value visitFuncCall(Python3Parser.FuncCallContext ctx) {
        int nArgs = ctx.arguments()==null?0:ctx.arguments().expr().size();
        Class[] cArgs = new Class[nArgs];
        Arrays.fill(cArgs, Value.class);
        Method mtd = null;
        try {
            mtd = this.getClass().getMethod(ctx.NAME().getText(), cArgs);
        } catch (NoSuchMethodException e){
            throw new RuntimeException(e);
        }

        Value[] vArgs = new Value[nArgs];
        if (nArgs>0) {
            for(int idx=0;idx<nArgs; idx++) {
                vArgs[idx] = visit(ctx.arguments().expr(idx));
            }
            if (funcPos != null) {
                funcPos.put(ctx.getStart().getStartIndex(), vArgs);
            }
        }
        try {
            return (Value)mtd.invoke(this, vArgs);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Override public Value visitFuncExpr(Python3Parser.FuncExprContext ctx) {
        return visitChildren(ctx);
    }

    @Override public Value visit(ParseTree tree) {
        var v = super.visit(tree);
        if (ret != null) {
            return ret;
        } else {
            return v;
        }
    }

    private Map<String, Value> memory = new HashMap<String, Value>();
    public Value eval(ParseTree tree, Map<String, Value> mem) {
        this.memory = mem;
        return visit(tree);
    }

    private Map<Integer, Object> funcPos = null;
    public Value eval(ParseTree tree, Map<String, Value> mem, Map<Integer, Object> posMap) {
        this.memory = mem;
        this.funcPos = posMap;
        return visit(tree);
    }

    public static void genTree(ParseTree tree, Map<String, Object> treeMap) {
        if (tree instanceof TerminalNodeImpl) {
            Token token = ((TerminalNodeImpl)tree).getSymbol();
            treeMap.put("type", token.getType());
            treeMap.put("type", token.getText());
        } else {
            List<Map<String, Object>> children = new ArrayList<>();
            String name = tree.getClass().getSimpleName().replaceAll("context$", "");
            treeMap.put(Character.toLowerCase(name.charAt(0)) + name.substring(1), children);
            if (tree instanceof ParserRuleContext) {
                treeMap.put("sLine", ((ParserRuleContext)tree).getStart().getLine());
                treeMap.put("sPos", ((ParserRuleContext)tree).getStart().getCharPositionInLine());
                treeMap.put("eLine", ((ParserRuleContext)tree).getStop().getLine());
                treeMap.put("ePos", ((ParserRuleContext)tree).getStop().getCharPositionInLine());
            }
            for (int i=0; i< tree.getChildCount(); i++) {
                Map<String, Object> nested = new LinkedHashMap<>();
                children.add(nested);
                genTree(tree.getChild(i), nested);
            }
        }
    }
}
