//> Parsing Expressions parser
package com.craftinginterpreters.lox;

//> Statements and State parser-imports
import java.util.ArrayList;
//< Statements and State parser-imports
//> Control Flow import-arrays
import java.util.Arrays;
//< Control Flow import-arrays
import java.util.List;

import static com.craftinginterpreters.lox.TokenType.*;
//parser to see valid syntax
class Parser {
//> parse-error
  public static class ParseError extends RuntimeException {}

//< parse-error
  private final List<Token> tokens;
  private int current = 0;
  public boolean ifError = true;
  private int loops = 0;

  Parser(List<Token> tokens) {
    this.tokens = tokens;
  }
/* Parsing Expressions parse < Statements and State parse
  Expr parse() {
    try {
      return expression();
    } catch (ParseError error) {
      return null;
    }
  }
*/
//> Statements and State parse
  List<Stmt> parse() {
    List<Stmt> statements = new ArrayList<>();
    while (!isAtEnd()) {
/* Statements and State parse < Statements and State parse-declaration
      statements.add(statement());
*/
//> parse-declaration
      statements.add(declaration());
//< parse-declaration
    }

    return statements; // [parse-error-handling]
  }
//< Statements and State parse
//> expression
//---------------------------------------------------------------------------------------------
  private Expr expression() {
/* Parsing Expressions expression < Statements and State expression
    return equality();
*/
//> Statements and State expression
    //return assignment();
      return assignment();
      //return comma();//comma → equality ( "," equality )* ; -------> expression → comma ;
//Starting point lowest precedence under expression(not method), so lowest overall
//< Statements and State expression
  }
  Expr parseExpression(){
      return assignment();//returns one expression

  }
  private Expr comma(){
      Expr expr = assignment();// gets the first expression

      //checks toke comma if there
      while (check(COMMA)){//COMMA is Token already taken into consideration, scanner took the character and turned it into a token
        Token operator = peek();// takes comma and sets as operator
        advance();// mode forward
        Expr Rightexpr = assignment();//gets the second expression
          //building the new expression
        expr = new Expr.Binary(expr, operator,Rightexpr);
        //expression = left comma right
          // expression = ((left comma right), right)
        // loop - each iteration of loop - new complete expression becomes the left side each time after comma
      }
      return expr;//left associativity in comma preference - returns the whole expression
  }


//-------------------------------------------------------------------------------------------
//< expression
//> Statements and State declaration
  private Stmt declaration() {
    try {
//> Classes match-class
      if (match(CLASS)) return classDeclaration();
//< Classes match-class
//> Functions match-fun
      if (check(FUN) && checkNextToken(IDENTIFIER)){
       consume(FUN, null) ;
       return function("function", false);
        }
//< Functions match-fun
      if (match(VAR)) return varDeclaration();

      return statement();
    } catch (ParseError error) {
      synchronize();
      return null;
    }
  }
//< Statements and State declaration
//> Classes parse-class-declaration
  private Stmt classDeclaration() {
    Token name = consume(IDENTIFIER, "Expect class name.");
//> Inheritance parse-superclass

    Expr.Variable superclass = null;
    if (match(LESS)) {
      consume(IDENTIFIER, "Expect superclass name.");
      superclass = new Expr.Variable(previous());
    }

//< Inheritance parse-superclass
    consume(LEFT_BRACE, "Expect '{' before class body.");

    List<Stmt.Function> methods = new ArrayList<>();
    while (!check(RIGHT_BRACE) && !isAtEnd()) {
    if(match(CLASS) ){
      methods.add(function("method", true));
      }
      else{
      methods.add(function("method", false));
    }

    }

    consume(RIGHT_BRACE, "Expect '}' after class body.");

/* Classes parse-class-declaration < Inheritance construct-class-ast
    return new Stmt.Class(name, methods);
*/
//> Inheritance construct-class-ast
    return new Stmt.Class(name, superclass, methods);
//< Inheritance construct-class-ast
  }
//< Classes parse-class-declaration
//> Statements and State parse-statement
  private Stmt statement() {
//> Control Flow match-for
    if (match(FOR)) return forStatement();
//< Control Flow match-for
//> Control Flow match-if
    if (match(IF)) return ifStatement();
//< Control Flow match-if
    if (match(PRINT)) return printStatement();
//> Functions match-return
    if (match(RETURN)) return returnStatement();
//< Functions match-return
//> Control Flow match-while
    if (match(WHILE)) return whileStatement();
//< Control Flow match-while
//> parse-block
    if (match(LEFT_BRACE)) return new Stmt.Block(block());
    if (match(BREAK)) return breakStatement();//if the new token is break then create new break statement// calls Stmt.Break(); with ; checking
//< parse-block

    return expressionStatement();
  }
//< Statements and State parse-statement
//> Control Flow for-statement
  private Stmt forStatement() {
    consume(LEFT_PAREN, "Expect '(' after 'for'.");

/* Control Flow for-statement < Control Flow for-initializer
    // More here...
*/
//> for-initializer
    Stmt initializer;
    if (match(SEMICOLON)) {
      initializer = null;
    } else if (match(VAR)) {
      initializer = varDeclaration();
    } else {
      initializer = expressionStatement();
    }
//< for-initializer
//> for-condition

    Expr condition = null;
    if (!check(SEMICOLON)) {
      condition = expression();
    }
    consume(SEMICOLON, "Expect ';' after loop condition.");
//< for-condition
//> for-increment

    Expr increment = null;
    if (!check(RIGHT_PAREN)) {
      increment = expression();
    }
    consume(RIGHT_PAREN, "Expect ')' after for clauses.");
//< for-increment
//> for-body
    loops++;//same loop check added in forStatement
    Stmt body = statement();
    loops--;
//> for-desugar-increment
    if (increment != null) {
      body = new Stmt.Block(
          Arrays.asList(
              body,
              new Stmt.Expression(increment)));
    }

//< for-desugar-increment
//> for-desugar-condition
    if (condition == null) condition = new Expr.Literal(true);
    body = new Stmt.While(condition, body);

//< for-desugar-condition
//> for-desugar-initializer
    if (initializer != null) {
      body = new Stmt.Block(Arrays.asList(initializer, body));
    }

//< for-desugar-initializer
    return body;
//< for-body
  }
//< Control Flow for-statement
//> Control Flow if-statement
  private Stmt ifStatement() {
    consume(LEFT_PAREN, "Expect '(' after 'if'.");
    Expr condition = expression();
    consume(RIGHT_PAREN, "Expect ')' after if condition."); // [parens]

    Stmt thenBranch = statement();
    Stmt elseBranch = null;
    if (match(ELSE)) {
      elseBranch = statement();
    }

    return new Stmt.If(condition, thenBranch, elseBranch);
  }
//< Control Flow if-statement
//> Statements and State parse-print-statement
  private Stmt printStatement() {
    Expr value = expression();
    consume(SEMICOLON, "Expect ';' after value.");
    return new Stmt.Print(value);
  }
//< Statements and State parse-print-statement
//> Functions parse-return-statement
  private Stmt returnStatement() {
    Token keyword = previous();
    Expr value = null;
    if (!check(SEMICOLON)) {
      value = expression();
    }

    consume(SEMICOLON, "Expect ';' after return value.");
    return new Stmt.Return(keyword, value);
  }

  private Stmt breakStatement() {
      consume(SEMICOLON, "Expect ';' after the Break");//sees if break is followed by semicolon then creates new object
      //return new Stmt.Break();//returns to parser

      if (loops == 0) {//syntax error
          error(peek(), "cannot have break outside of the loop");
      } else {//allows break
          return new Stmt.Break();

      }


      return null;
  }

//< Functions parse-return-statement
//> Statements and State parse-var-declaration
  private Stmt varDeclaration() {
    Token name = consume(IDENTIFIER, "Expect variable name.");

    Expr initializer = null;
    if (match(EQUAL)) {
      initializer = expression();
    }

    consume(SEMICOLON, "Expect ';' after variable declaration.");
    return new Stmt.Var(name, initializer);
  }
//< Statements and State parse-var-declaration
//> Control Flow while-statement
  private Stmt whileStatement() {
    consume(LEFT_PAREN, "Expect '(' after 'while'.");
    Expr condition = expression();
    consume(RIGHT_PAREN, "Expect ')' after condition.");
    //Stmt body = statement();

    loops++;
    Stmt body = statement();//parse code inside the loop
    loops --;
    return new Stmt.While(condition, body);
  }
//< Control Flow while-statement
//> Statements and State parse-expression-statement
  private Stmt expressionStatement() {
    Expr expr = expression();
    consume(SEMICOLON, "Expect ';' after expression.");
    return new Stmt.Expression(expr);
  }
//< Statements and State parse-expression-statement
//> Functions parse-function
  private Stmt.Function function(String kind, boolean stat){//method static or not? bool
    Token name = consume(IDENTIFIER, "Expect '(' "+ kind +" fun.");
    return new Stmt.Function(name, functBody(kind), stat);
  }
  private Expr.Funct AnonymousFunction(String kind){
    //Token name = consume(IDENTIFIER, "Expect '(' "+ kind +" fun.");
    return functBody(kind);
  }


  private Expr.Funct functBody(String kind) {
    //Token name = consume(IDENTIFIER, "Expect " + kind + " name.");
//> parse-parameters
    List<Token> parameters = new ArrayList<>();
    boolean get;//boolean getter added to identify and determine it is a getter

    if(check(LEFT_PAREN)) {// if there is a parenthesis parse parameters
        get = false;
        //here getter is false because it is not a getter and has parenthesis and parameters, so do no look at this logic
        consume(LEFT_PAREN, "Expect '(' " + kind + " fun.");
        //List<Token> parameters = new ArrayList<>();
        if (!check(RIGHT_PAREN)) {
            do {
                if (parameters.size() >= 255) {
                    error(peek(), "Can't have more than 255 parameters.");
                }

                parameters.add(
                        consume(IDENTIFIER, "Expect parameter name."));
            } while (match(COMMA));
        }
        consume(RIGHT_PAREN, "Expect ')' after parameters.");
//< parse-parameters
//> parse-body

        }
        else{
            get = true;// if no parenthesis and parameters then this is a getter
        }// if it does not have parenthesis go straight to {}body
        consume(LEFT_BRACE, "Expect '{' before " + kind + " body.");
        List<Stmt> body = block();
        return new Expr.Funct(parameters, body, get);
//< parse-body

  }
//< Functions parse-function
//> Statements and State block
  private List<Stmt> block() {
    List<Stmt> statements = new ArrayList<>();

    while (!check(RIGHT_BRACE) && !isAtEnd()) {
      statements.add(declaration());
    }

    consume(RIGHT_BRACE, "Expect '}' after block.");
    return statements;
  }
//< Statements and State block
//> Statements and State parse-assignment
  private Expr assignment() {
/* Statements and State parse-assignment < Control Flow or-in-assignment
    Expr expr = equality();
*/
//> Control Flow or-in-assignment
    //Expr expr = or();
    Expr expr = conditional();
//< Control Flow or-in-assignment

    if (match(EQUAL, PLUS_EQUAL, MINUS_EQUAL, STAR_EQUAL, SLASH_EQUAL)) {
      Token equals = previous();
      Expr value = assignment();

      if (expr instanceof Expr.Variable && equals.type ==PLUS_EQUAL) {// checks if variable and is plus equal
        Token name = ((Expr.Variable)expr).name;//getting variable name
        Token NewTokenObj = new Token(PLUS, "+", null, equals.line);//new token object that treats plus equal as plus when reading the logic in program
        value = new Expr.Binary(expr,NewTokenObj, value);
        return new Expr.Assign(name, value);//assignment that uses the new binary operation change
//> Classes assign-set
      }
      else if (expr instanceof Expr.Variable && equals.type ==MINUS_EQUAL) {
        Token name = ((Expr.Variable)expr).name;
        Token NewTokenObj = new Token(MINUS, "-", null, equals.line);
        value = new Expr.Binary(expr,NewTokenObj, value);
        return new Expr.Assign(name, value);
      }
      else if (expr instanceof Expr.Variable && equals.type ==STAR_EQUAL) {
        Token name = ((Expr.Variable)expr).name;
        Token NewTokenObj = new Token(STAR, "*", null, equals.line);
        value = new Expr.Binary(expr,NewTokenObj, value);
        return new Expr.Assign(name, value);
      }
      else if (expr instanceof Expr.Variable && equals.type ==SLASH_EQUAL) {
        Token name = ((Expr.Variable)expr).name;
        Token NewTokenObj = new Token(SLASH, "/", null, equals.line);
        value = new Expr.Binary(expr,NewTokenObj, value);
        return new Expr.Assign(name, value);
      }

      else if (expr instanceof Expr.Get) {
        Expr.Get get = (Expr.Get)expr;
        return new Expr.Set(get.object, get.name, value);
//< Classes assign-set
      }

      error(equals, "Invalid assignment target."); // [no-throw]
    }

    return expr;
  }

  //Conditional - between assignment and or
    //?: if/else inside expression
    //condition ? value if true: value if false
    //right associative if nested
  private Expr conditional(){
    Expr expr = equality();//gets condition left side
    if (match(TokenType.QUESTION_MARK)){//Added ? to tokentype and is now a token// is next token a question mark
    Expr TrueValue = expression();// after the ? and between the next variable
        if(!check(TokenType.COLON)){//if not colon type error
            error(peek(), "The ':' is missing");
        }
        else{
            advance();
        }
    Expr FalseValue = conditional();//parses expression after :
    expr = new Expr.Conditional(expr,TrueValue, FalseValue);//object conatains all 3 to then evaluate mathematically in other parts of program
    }
    return expr;//condition sent back to parser
  }

//< Statements and State parse-assignment
//> Control Flow or
  private Expr or() {
    Expr expr = and();

    while (match(OR)) {
      Token operator = previous();
      Expr right = and();
      expr = new Expr.Logical(expr, operator, right);
    }

    return expr;
  }
//< Control Flow or
//> Control Flow and
  private Expr and() {
    Expr expr = equality();

    while (match(AND)) {
      Token operator = previous();
      Expr right = equality();
      expr = new Expr.Logical(expr, operator, right);
    }

    return expr;
  }
//< Control Flow and
//> equality
//----------------------------------------------------------------------------------------------------------
  private Expr equality() {
    Expr expr;

    if(match(BANG_EQUAL, EQUAL_EQUAL)) {
        //looks at == or !=
      Token operator = previous();//set previous token as operator, but since there is nothing for this error it will spit an error
      error(operator, "The left hand operand is missing");
      //discard that operator
      Expr discard = comparison();
      //discard as temp variable that was parsed, will not need or use
        //THE SAME LOGIC WILL BE USED FOR THE OTHER METHODS AS WELL
      return null;
    } else {
      expr = comparison();
    }


    while (match(BANG_EQUAL, EQUAL_EQUAL)) {
      Token operator = previous();
      Expr right = comparison();
      expr = new Expr.Binary(expr, operator, right);
    }

    return expr;
  }
//< equality
//> comparison
  private Expr comparison() {
    Expr expr;
    if(match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
      Token operator = previous();
      error(operator, "The left hand operand is missing");

      Expr discard =  term();
      return null;
    } else {
      expr = term();
    }


    while (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
      Token operator = previous();
      Expr right = term();
      expr = new Expr.Binary(expr, operator, right);
    }

    return expr;
  }
//< comparison
//> term
  private Expr term() {
    Expr expr;
    if(match(PLUS)) {//leave out MINUS due to negative numbers being accepted in expression
      Token operator = previous();
      error(operator, "The left hand operand is missing");
      Expr discard = factor();
      return null;
    } else {
      expr = factor();
    }


    while (match(MINUS, PLUS)) {
      Token operator = previous();
      Expr right = factor();
      expr = new Expr.Binary(expr, operator, right);
    }

    return expr;
  }
//< term
//> factor
  private Expr factor() {
    Expr expr;
    if(match(SLASH, STAR, PERCENT)){
      Token operator = previous();
      error(operator, "The left hand operand is missing");
      Expr discard = unary();
      return null;
    } else {
      expr = unary();
    }

    while (match(SLASH, STAR, PERCENT)) {
      Token operator = previous();
      Expr right = unary();
      expr = new Expr.Binary(expr, operator, right);
    }

    return expr;
  }
  //----------------------------------------------------------------------------------------------
//< factor
//> unary
  private Expr unary() {
    if (match(BANG, MINUS)) {
      Token operator = previous();
      Expr right = unary();
      return new Expr.Unary(operator, right);
    }

/* Parsing Expressions unary < Functions unary-call
    return primary();
*/
//> Functions unary-call
    return call();
//< Functions unary-call
  }
//< unary
//> Functions finish-call
  private Expr finishCall(Expr callee) {
    List<Expr> arguments = new ArrayList<>();
    if (!check(RIGHT_PAREN)) {
      do {
//> check-max-arity
        if (arguments.size() >= 255) {
          error(peek(), "Can't have more than 255 arguments.");
        }
        //< check-max-arity
        arguments.add(equality());
        //arguments.add(expression());

      } while (match(COMMA));
    }

    Token paren = consume(RIGHT_PAREN,
                          "Expect ')' after arguments.");

    return new Expr.Call(callee, paren, arguments);
  }
//< Functions finish-call
//> Functions call
  private Expr call() {
    Expr expr = primary();

    while (true) { // [while-true]
      if (match(LEFT_PAREN)) {
        expr = finishCall(expr);
//> Classes parse-property
      } else if (match(DOT)) {
        Token name = consume(IDENTIFIER,
            "Expect property name after '.'.");
        expr = new Expr.Get(expr, name);
//< Classes parse-property
      } else {
        break;
      }
    }

    return expr;
  }
//< Functions call
//> primary
  private Expr primary() {
    if (match(FALSE)) return new Expr.Literal(false);
    if (match(TRUE)) return new Expr.Literal(true);
    if (match(NIL)) return new Expr.Literal(null);

    if (match(NUMBER, STRING)) {
      return new Expr.Literal(previous().literal);
    }
//> Inheritance parse-super

    if (match(SUPER)) {
      Token keyword = previous();
      consume(DOT, "Expect '.' after 'super'.");
      Token method = consume(IDENTIFIER,
          "Expect superclass method name.");
      return new Expr.Super(keyword, method);
    }
//< Inheritance parse-super
//> Classes parse-this

    if (match(THIS)) return new Expr.This(previous());
//< Classes parse-this
//> Statements and State parse-identifier

    if (match(IDENTIFIER)) {
      return new Expr.Variable(previous());
    }
//< Statements and State parse-identifier

    if (match(LEFT_PAREN)) {
      Expr expr = expression();
      consume(RIGHT_PAREN, "Expect ')' after expression.");
      return new Expr.Grouping(expr);
    }

    if(match(FUN))return AnonymousFunction("function");
//> primary-error

    throw error(peek(), "Expect expression.");
//< primary-error
  }
//< primary

  private boolean checkNextToken(TokenType tokenType){
   int NextToken = current +1;//next index
   if((NextToken >= tokens.size()) || isAtEnd()) return false;//checks if token exists

   return tokens.get(NextToken).type == tokenType;
  }

//> match
  private boolean match(TokenType... types) {
    for (TokenType type : types) {
      if (check(type)) {
        advance();
        return true;
      }
    }

    return false;
  }
//< match
//> consume
  private Token consume(TokenType type, String message) {
    if (check(type)) return advance();

    throw error(peek(), message);
  }
//< consume
//> check
  private boolean check(TokenType type) {
    if (isAtEnd()) return false;
    return peek().type == type;
  }
//< check
//> advance
  private Token advance() {
    if (!isAtEnd()) current++;
    return previous();
  }
//< advance
//> utils
  private boolean isAtEnd() {
    return peek().type == EOF;
  }

  private Token peek() {
    return tokens.get(current);
  }

  private Token previous() {
    return tokens.get(current - 1);
  }
//< utils
//> error
  private ParseError error(Token token, String message) {
    if (ifError){Lox.error(token, message);}

    return new ParseError();
  }
//< error
//> synchronize
  private void synchronize() {
    advance();

    while (!isAtEnd()) {
      if (previous().type == SEMICOLON) return;

      switch (peek().type) {
        case CLASS:
        case FUN:
        case VAR:
        case FOR:
        case IF:
        case WHILE:
        case PRINT:
        case RETURN:
          return;
      }

      advance();
    }
  }
//< synchronize
}
