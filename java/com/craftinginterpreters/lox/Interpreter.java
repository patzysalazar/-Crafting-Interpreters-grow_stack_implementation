//> Evaluating Expressions interpreter-class
package com.craftinginterpreters.lox;
//> Statements and State import-list
//interpreter - using depth and index
//> Functions import-array-list
import java.util.ArrayList;
//< Functions import-array-list
//> Resolving and Binding import-hash-map
import java.util.HashMap;
//< Resolving and Binding import-hash-map
import java.util.List;
//< Statements and State import-list
//> Resolving and Binding import-map
import java.util.Map;
//< Resolving and Binding import-map

/* Evaluating Expressions interpreter-class < Statements and State interpreter
class Interpreter implements Expr.Visitor<Object> {
*/
//> Statements and State interpreter
class Interpreter implements Expr.Visitor<Object>,
                             Stmt.Visitor<Void> {
//< Statements and State interpreter
/* Statements and State environment-field < Functions global-environment
  private Environment environment = new Environment();
*/
//> Functions global-environment
  final Environment globals = new Environment();
  private Environment environment = globals;
//< Functions global-environment
//> Resolving and Binding locals-field
  private final Map<Expr, Integer> locals = new HashMap<>();
  //keep both separate
  private final Map<Expr, Integer> indexes = new HashMap<>();//stored indexes within the scope, added map to since resolver needs to remember both
  private final Map<Stmt.Var, Integer> declareVar = new HashMap<>();
  private final Map<Stmt.Function, Integer> declareFnct = new HashMap<>();

    //< Resolving and Binding locals-field
//> Statements and State environment-field
  private static Object uninitializedVar = new Object();
//< Statements and State environment-field
//> Functions interpreter-constructor
  Interpreter() {
    globals.define("clock", new LoxCallable() {
      @Override
      public int arity() { return 0; }

      @Override
      public Object call(Interpreter interpreter,
                         List<Object> arguments) {
        return (double)System.currentTimeMillis() / 1000.0;
      }

      @Override
      public String toString() { return "<native fn>"; }
    });
  }
  
//< Functions interpreter-constructor
/* Evaluating Expressions interpret < Statements and State interpret
  void interpret(Expr expression) { // [void]
    try {
      Object value = evaluate(expression);
      System.out.println(stringify(value));
    } catch (RuntimeError error) {
      Lox.runtimeError(error);
    }
  }
*/

  public static class BreakExept extends RuntimeException{}
//> Statements and State interpret
  void interpret(List<Stmt> statements) {
    try {
      for (Stmt statement : statements) {
        execute(statement);
      }
    } catch (RuntimeError error) {
      Lox.runtimeError(error);
    }
  }
//< Statements and State interpret
//> evaluate
public Object evaluate(Expr expr) {
    return expr.accept(this);
  }
//< evaluate
//> Statements and State execute
  private void execute(Stmt stmt) {
    stmt.accept(this);
  }
//< Statements and State execute
//> Resolving and Binding resolve
  void resolve(Expr expr, int depth, int index) {
    locals.put(expr, depth);
    indexes.put(expr, index);//added
  }
  void resolve(Stmt.Var statement, int index){
    declareVar.put(statement,index);
  }
  void resolve(Stmt.Function stmnt, int index){
        declareFnct.put(stmnt, index);
  }


    //< Resolving and Binding resolve
//> Statements and State execute-block
  void executeBlock(List<Stmt> statements,
                    Environment environment) {
    Environment previous = this.environment;
    try {
      this.environment = environment;

      for (Stmt statement : statements) {
        execute(statement);
      }
    } finally {
      this.environment = previous;
    }
  }
//< Statements and State execute-block
//> Statements and State visit-block
  @Override
  public Void visitBlockStmt(Stmt.Block stmt) {
    executeBlock(stmt.statements, new Environment(environment));
    return null;
  }
//< Statements and State visit-block
//> Classes interpreter-visit-class
  @Override
  public Void visitClassStmt(Stmt.Class stmt) {
//> Inheritance interpret-superclass
    Object superclass = null;
    if (stmt.superclass != null) {
      superclass = evaluate(stmt.superclass);
      if (!(superclass instanceof LoxClass)) {
        throw new RuntimeError(stmt.superclass.name,
            "Superclass must be a class.");
      }
    }

//< Inheritance interpret-superclass
    environment.define(stmt.name.lexeme, null);
//> Inheritance begin-superclass-environment

    if (stmt.superclass != null) {
      environment = new Environment(environment);
      environment.define(0, superclass);
    }
//< Inheritance begin-superclass-environment
//> interpret-methods

    Map<String, LoxFunction> methods = new HashMap<>();
    Map<String, LoxFunction> statMethods = new HashMap<>();
//new stat method map to use like methods
    for (Stmt.Function method : stmt.methods) {
/* Classes interpret-methods < Classes interpreter-method-initializer
      LoxFunction function = new LoxFunction(method, environment);
*/
//> interpreter-method-initializer
      LoxFunction function = new LoxFunction(method.name.lexeme, method.funct,environment,
          method.name.lexeme.equals("init"), method.stat, method.funct.get);//determines if method s a getter
//< interpreter-method-initializer
      //methods.put(method.name.lexeme, function);
        if(method.stat){//add if else stmnt to know which method to call
          statMethods.put(method.name.lexeme, function);//called on the class itself
        }
        else{
          methods.put(method.name.lexeme, function);//called on object
        }

    }

/* Classes interpret-methods < Inheritance interpreter-construct-class
    LoxClass klass = new LoxClass(stmt.name.lexeme, methods);
*/
//> Inheritance interpreter-construct-class
    LoxClass klass = new LoxClass(stmt.name.lexeme,
        (LoxClass)superclass, methods, statMethods);
//> end-superclass-environment

    if (superclass != null) {
      environment = environment.enclosing;
    }
//< end-superclass-environment

//< Inheritance interpreter-construct-class
//< interpret-methods
/* Classes interpreter-visit-class < Classes interpret-methods
    LoxClass klass = new LoxClass(stmt.name.lexeme);
*/
    environment.assign(stmt.name, klass);
    return null;
  }
//< Classes interpreter-visit-class
//> Statements and State visit-expression-stmt
  @Override
  public Void visitExpressionStmt(Stmt.Expression stmt) {
    evaluate(stmt.expression);
    return null;

  }
@Override
  public Void visitBreakStmt(Stmt.Break stmt){
    throw new BreakExept();
  }

//< Statements and State visit-expression-stmt
//> Functions visit-function
  @Override
  public Void visitFunctionStmt(Stmt.Function stmt) {
/* Functions visit-function < Functions visit-closure

*/
/* Functions visit-closure < Classes construct-function
    LoxFunction function = new LoxFunction(stmt, environment);
*/
//> Classes construct-function
    LoxFunction function = new LoxFunction(stmt.name.lexeme, stmt.funct, environment, false, stmt.stat, stmt.funct.get);
//< Classes construct-function
      if(declareFnct.get(stmt) == null){//global fcn
          environment.define(stmt.name.lexeme, function);//store by name
      }
        else{//local fcn
          environment.define(declareFnct.get(stmt), function);//store by index
      }
      return null;
  }
//< Functions visit-function
//> Control Flow visit-if
  @Override
  public Void visitIfStmt(Stmt.If stmt) {
    if (isTruthy(evaluate(stmt.condition))) {
      execute(stmt.thenBranch);
    } else if (stmt.elseBranch != null) {
      execute(stmt.elseBranch);
    }
    return null;
  }
//< Control Flow visit-if
//> Statements and State visit-print
  @Override
  public Void visitPrintStmt(Stmt.Print stmt) {
    Object value = evaluate(stmt.expression);
    System.out.println(stringify(value));
    return null;
  }
//< Statements and State visit-print
//> Functions visit-return
  @Override
  public Void visitReturnStmt(Stmt.Return stmt) {
    Object value = null;
    if (stmt.value != null) value = evaluate(stmt.value);

    throw new Return(value);
  }
//< Functions visit-return
//> Statements and State visit-var
  @Override
  public Void visitVarStmt(Stmt.Var stmt) {

    Object value = uninitializedVar;
    if (stmt.initializer != null) {
      value = evaluate(stmt.initializer);
    }
    if(declareVar.get(stmt) == null){
        environment.define(stmt.name.lexeme, value);

    }
    else {
        environment.define(declareVar.get(stmt), value);
    }
    return null;
  }
//< Statements and State visit-var
//> Control Flow visit-while
  @Override
  public Void visitWhileStmt(Stmt.While stmt) {
    try {//while loop - if true then execute what is in the while loop, else do not do anything and do not execute loop
        while (isTruthy(evaluate(stmt.condition))) {
            execute(stmt.body);
        }
    }   catch (BreakExept exept){

    }
    return null;
  }
//< Control Flow visit-while
//> Statements and State visit-assign
  @Override
  public Object visitAssignExpr(Expr.Assign expr) {
    Object value = evaluate(expr.value);
/* Statements and State visit-assign < Resolving and Binding resolved-assign
    environment.assign(expr.name, value);
*/
//> Resolving and Binding resolved-assign
//changes value of existing variable but now with index and distance instead of name
    Integer distance = locals.get(expr);
    Integer index = indexes.get(expr);
    if (distance != null) {
      environment.assignAt(distance,index, value);
    } else {//if global var use name
      globals.assign(expr.name, value);
    }

//< Resolving and Binding resolved-assign
    return value;
  }
//< Statements and State visit-assign
//> visit-binary
  @Override
  public Object visitBinaryExpr(Expr.Binary expr) {
      if (expr.operator.type == TokenType.COMMA){
          evaluate(expr.left);//ignore left but evaluate
          return evaluate(expr.right);//evaluate right and return it
      }
    Object left = evaluate(expr.left);
    Object right = evaluate(expr.right); // [left]

    switch (expr.operator.type) {
//> binary-equality
      case BANG_EQUAL: return !isEqual(left, right);
      case EQUAL_EQUAL: return isEqual(left, right);
//< binary-equality
//> binary-comparison
      case GREATER:
//> check-greater-operand
        checkNumberOperands(expr.operator, left, right);
//< check-greater-operand
        return (double)left > (double)right;
      case GREATER_EQUAL:
//> check-greater-equal-operand
        checkNumberOperands(expr.operator, left, right);
//< check-greater-equal-operand
        return (double)left >= (double)right;
      case LESS:
//> check-less-operand
        checkNumberOperands(expr.operator, left, right);
//< check-less-operand
        return (double)left < (double)right;
      case LESS_EQUAL:
//> check-less-equal-operand
        checkNumberOperands(expr.operator, left, right);
//< check-less-equal-operand
        return (double)left <= (double)right;
//< binary-comparison
      case MINUS:
//> check-minus-operand
        checkNumberOperands(expr.operator, left, right);
//< check-minus-operand
        return (double)left - (double)right;
//> binary-plus
// ________________________________________________________________________________
      case PLUS:
        if (left instanceof Double && right instanceof Double) {
          return (double)left + (double)right;
        } // [plus]

        if (left instanceof String && right instanceof String) {
          return (String)left + (String)right;
        }
        if (left instanceof Double && right instanceof String) {
            String strng = stringify(left);//turn Double in left to tring
            return strng + (String)right; //return result of double and string to fully be string
          } // [plus]
        if(left instanceof String && right instanceof Double){
            String STRNG = stringify(right);//turn double in right to string
            return (String)left + STRNG;
//___________________________________________________________________________________________________
        }


/* Evaluating Expressions binary-plus < Evaluating Expressions string-wrong-type
        break;
*/
//> string-wrong-type
        throw new RuntimeError(expr.operator,
            "Operands must be two numbers or two strings.");
//< string-wrong-type
//< binary-plus
      case SLASH:
//> check-slash-operand
        checkNumberOperands(expr.operator, left, right);
//< check-slash-operand
        return (double)left / (double)right;
      case STAR:
//> check-star-operand
        checkNumberOperands(expr.operator, left, right);
//< check-star-operand
        return (double)left * (double)right;
        case PERCENT:
        //For Modulo
        return (double)left % (double)right;
    }

    // Unreachable.
    return null;
  }

    @Override//conditional method
    public Object visitConditionalExpr(Expr.Conditional expr){
        Object condition = evaluate(expr.condition);//evaluates what the expression produces
//stored in variable condition
        if(isTruthy(condition)){//checks if condition is true in lox
            return evaluate(expr.trueValue);

        }
        else{
            return evaluate(expr.falseValue);
        }
    }

//< visit-binary
//> Functions visit-call
  @Override
  public Object visitCallExpr(Expr.Call expr) {
    Object callee = evaluate(expr.callee);

    List<Object> arguments = new ArrayList<>();
    for (Expr argument : expr.arguments) { // [in-order]
      arguments.add(evaluate(argument));
    }

//> check-is-callable
    if (!(callee instanceof LoxCallable)) {
      throw new RuntimeError(expr.paren,
          "Can only call functions and classes.");
    }

//< check-is-callable
    LoxCallable function = (LoxCallable)callee;
//> check-arity
    if (arguments.size() != function.arity()) {
      throw new RuntimeError(expr.paren, "Expected " +
          function.arity() + " arguments but got " +
          arguments.size() + ".");
    }

//< check-arity
    return function.call(this, arguments);
  }
//< Functions visit-call
//> Classes interpreter-visit-get
  @Override
  public Object visitGetExpr(Expr.Get expr) {
    Object object = evaluate(expr.object);
    if (object instanceof LoxInstance) {
       Object val = ((LoxInstance)object).get(expr.name);//gets name
       if(val instanceof LoxFunction){//if Loxfunction
           if(((LoxFunction)val).get){//if loxfunction is getter
              return  ((LoxFunction)val).call(this, new ArrayList<>());//call the fcn
           }

       }
       return val;
      //return ((LoxInstance) object).get(expr.name);
    } else if (object instanceof LoxClass){//added implementation of static methods called from LoxClass
      return ((LoxClass) object).get(expr.name);
        
    }
    else {

        throw new RuntimeError(expr.name,
                "Only instances have properties.");
    }

  }
//< Classes interpreter-visit-get
//> visit-grouping
  @Override
  public Object visitGroupingExpr(Expr.Grouping expr) {
    return evaluate(expr.expression);
  }
//< visit-grouping
//> visit-literal
  @Override
  public Object visitLiteralExpr(Expr.Literal expr) {
    return expr.value;
  }
  @Override
  public Object visitFunctExpr(Expr.Funct expr){
    return new LoxFunction(null, expr, environment, false, false, expr.get);//expects get now
//not stat
  }

//< visit-literal
//> Control Flow visit-logical
  @Override
  public Object visitLogicalExpr(Expr.Logical expr) {
    Object left = evaluate(expr.left);

    if (expr.operator.type == TokenType.OR) {
      if (isTruthy(left)) return left;
    } else {
      if (!isTruthy(left)) return left;
    }

    return evaluate(expr.right);
  }
//< Control Flow visit-logical
//> Classes interpreter-visit-set
  @Override
  public Object visitSetExpr(Expr.Set expr) {
    Object object = evaluate(expr.object);

    if (!(object instanceof LoxInstance)) { // [order]
      throw new RuntimeError(expr.name,
                             "Only instances have fields.");
    }

    Object value = evaluate(expr.value);
    ((LoxInstance)object).set(expr.name, value);
    return value;
  }
//< Classes interpreter-visit-set
//> Inheritance interpreter-visit-super
  @Override
  public Object visitSuperExpr(Expr.Super expr) {
    int distance = locals.get(expr);
    LoxClass superclass = (LoxClass)environment.getAt(
        distance, 0);
//> super-find-this

    LoxInstance object = (LoxInstance)environment.getAt(
        distance - 1, 0);
//< super-find-this
//> super-find-method

    LoxFunction method = superclass.findMethod(object, expr.method.lexeme);//lox obj
//> super-no-method

    if (method == null) {
      throw new RuntimeError(expr.method,
          "Undefined property '" + expr.method.lexeme + "'.");
    }

//< super-no-method
    return method;
//< super-find-method
  }
//< Inheritance interpreter-visit-super
//> Classes interpreter-visit-this
  @Override
  public Object visitThisExpr(Expr.This expr) {
    return lookUpVariable(expr.keyword, expr);
  }
//< Classes interpreter-visit-this
//> visit-unary
  @Override
  public Object visitUnaryExpr(Expr.Unary expr) {
    Object right = evaluate(expr.right);

    switch (expr.operator.type) {
//> unary-bang
      case BANG:
        return !isTruthy(right);
//< unary-bang
      case MINUS:
//> check-unary-operand
        checkNumberOperand(expr.operator, right);
//< check-unary-operand
        return -(double)right;
    }

    // Unreachable.
    return null;
  }
//< visit-unary
//> Statements and State visit-variable
  @Override
  public Object visitVariableExpr(Expr.Variable expr) {
/* Statements and State visit-variable < Resolving and Binding call-look-up-variable
    return environment.get(expr.name);
    //current environment object retrieves var value to use for comparison to initialized value, if no value then error
*/  if(lookUpVariable(expr.name, expr) == uninitializedVar){//check values
    //where that value is stored
    //throw runtime error if the variable is not initialized
      throw new RuntimeError(expr.name, "You must initialize a variable before using");

      }
//> Resolving and Binding call-look-up-variable
      //else return the initialized value
    else {
      return lookUpVariable(expr.name, expr);//how lox find local var value when used
      }
//< Resolving and Binding call-look-up-variable
  }
//> Resolving and Binding look-up-variable
  private Object lookUpVariable(Token name, Expr expr) {
    Integer distance = locals.get(expr);
    if (distance != null) {
      return environment.getAt(distance, indexes.get(expr));
    } else {
      return globals.get(name);
    }
  }
//< Resolving and Binding look-up-variable
//< Statements and State visit-variable
//> check-operand
  private void checkNumberOperand(Token operator, Object operand) {
    if (operand instanceof Double) return;
    throw new RuntimeError(operator, "Operand must be a number.");
  }
//< check-operand
//> check-operands
  private void checkNumberOperands(Token operator,
                                   Object left, Object right) {
    if (left instanceof Double && right instanceof Double) return;
    // [operand]
    throw new RuntimeError(operator, "Operands must be numbers.");
  }
//< check-operands
//> is-truthy
  private boolean isTruthy(Object object) {
    if (object == null) return false;
    if (object instanceof Boolean) return (boolean)object;
    return true;
  }
//< is-truthy
//> is-equal
  private boolean isEqual(Object a, Object b) {
    if (a == null && b == null) return true;
    if (a == null) return false;

    return a.equals(b);
  }
//< is-equal
//> stringify
  private String stringify(Object object) {
    if (object == null) return "nil";

    if (object instanceof Double) {
      String text = object.toString();
      if (text.endsWith(".0")) {
        text = text.substring(0, text.length() - 2);
      }
      return text;
    }

    return object.toString();
  }
//< stringify
}
