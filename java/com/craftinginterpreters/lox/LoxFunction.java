//> Functions lox-function
package com.craftinginterpreters.lox;

import java.util.List;

class LoxFunction implements LoxCallable {
  private final Expr.Funct declaration;

  //private final Expr.Function declaration;
//> closure-field
  private final Environment closure;
  
//< closure-field
/* Functions lox-function < Functions closure-constructor
  LoxFunction(Stmt.Function declaration) {
*/
/* Functions closure-constructor < Classes is-initializer-field
  LoxFunction(Stmt.Function declaration, Environment closure) {
*/
//> Classes is-initializer-field
  private final boolean isInitializer;
  private final String name;
  private final boolean stat;//added stat field, added stat to constructor, stored value
  public final boolean get;

// added bool to know if fcn is static
  LoxFunction(String name, Expr.Funct declaration, Environment closure,
              boolean isInitializer, boolean stat, boolean get) {
    this.name = name;
    this.isInitializer = isInitializer;

//< Classes is-initializer-field
//> closure-constructor
    this.closure = closure;
//< closure-constructor
    this.declaration = declaration;
    this.stat = stat;
    this.get = get;
  }
//> Classes bind-instance
  LoxFunction bind(LoxInstance instance, LoxFunction innerMethod) {//connects outer method to obj
    Environment environment = new Environment(closure);
    environment.define(0, instance);//this
    environment.define(1, innerMethod);//inner// make changes to resolver
/* Classes bind-instance < Classes lox-function-bind-with-initializer
    return new LoxFunction(declaration, environment);
*/
//> lox-function-bind-with-initializer
    return new LoxFunction(name, declaration, environment,
                           isInitializer, stat, get);
//< lox-function-bind-with-initializer
  }
//< Classes bind-instance
//> function-to-string
  @Override
  public String toString() {
      if (name != null) {
          return "<fn " + name + ">";
      } else {
          return "<fn>";
      }
  }
//< function-to-string
//> function-arity
  @Override
  public int arity() {
    return declaration.param.size();
  }
//< function-arity
//> function-call
  @Override
  public Object call(Interpreter interpreter,
                     List<Object> arguments) {
/* Functions function-call < Functions call-closure
    Environment environment = new Environment(interpreter.globals);
*/
//> call-closure
    Environment environment = new Environment(closure);
//< call-closure
    for (int i = 0; i < declaration.param.size(); i++) {
      environment.define(i,
          arguments.get(i));
    }

/* Functions function-call < Functions catch-return
    interpreter.executeBlock(declaration.body, environment);
*/
//> catch-return
    try {
      interpreter.executeBlock(declaration.body, environment);
    } catch (Return returnValue) {
//> Classes early-return-this
      if (isInitializer) return closure.getAt(0, 0);

//< Classes early-return-this
      return returnValue.value;
    }
//< catch-return
//> Classes return-this

    if (isInitializer) return closure.getAt(0, 0);//index 0 and distance 0
//< Classes return-this
    return null;
  }
//< function-call
}
