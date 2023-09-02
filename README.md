# ScriQ Scripting Language

## Get Started

Add the dependency to your project, e.g., for a Maven project.
```xml
    <dependency>
      <groupId>net.lizhao</groupId>
      <artifactId>scriq</artifactId>
      <version>0.0.4</version>
    </dependency>
```

## Evaluate ScriQ scripts

ScriQ is a tiny Python variant. This is an example to evaluate ScriQ scripts.
```java
    @Test
    void testIfOrComp() {
        String code = "if True or 1==2:\n  return 3\nelse:\n  return 4";
        Evaluator eval = new Evaluator();
        var val = eval.eval(getTree(code), new HashMap<String, Value>());
        assert (val.equals(3));
    }
```

ScriQs allow the app to extend functions, instead of support function def in script.

1. Derive a class from *Evaluator*, and add public function, e.g., 
```java
public class DemoFunc extends Evaluator {

    public Value PV(Value i) {
        return new Value(i);
    }
}
```

2. The script then can use the function:

```python
i = 10
j = PV(i)
return j
```

## Retrospect Evaluation

ScriQ can record the arguments used calling a function, by giving the function start index in
the script string.

```java
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
```

## View of Grammar Tree

Serializer like Jackson ObjectMapper could be use to stringify the treeMap into json string.

```java
    @Test
    void testTree() {
        String code = "j=PV(33,1)\ni=11\nPV(i,2)\nreturn i";
        Map<String, Object> treeMap = new HashMap<String, Object>();
        Evaluator.genTree(getTree(code), treeMap);
        assert (treeMap.size()>0);
    }
```