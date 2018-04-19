package jsy.student

import jsy.lab6.Lab6Like
import jsy.lab6.ast._
import jsy.util.DoWith

object Lab6 extends jsy.util.JsyApplication with Lab6Like {

  /*
   * CSCI 3155: Lab 6
   * Michael Gohde
   *
   * Partner: <Your Partner's Name>
   * Collaborators: <Any Collaborators>
   */

  /*
   * Fill in the appropriate portions above by replacing things delimited
   * by '<'... '>'.
   *
   * Replace the '???' expression with your code in each function.
   *
   * Do not make other modifications to this template, such as
   * - adding "extends App" or "extends Application" to your Lab object,
   * - adding a "main" method, and
   * - leaving any failing asserts.
   *
   * Your lab will not be graded if it does not compile.
   *
   * This template compiles without error. Before you submit comment out any
   * code that does not compile or causes a failing assert. Simply put in a
   * '???' as needed to get something that compiles without error. The '???'
   * is a Scala expression that throws the exception scala.NotImplementedError.
   */

  /*** Exercises with Continuations ***/

  def foldLeftAndThen[A,B](t: Tree)(z: A)(f: (A,Int) => A)(sc: A => B): B = {
    def loop(acc: A, t: Tree)(sc: A => B): B = t match {
      case Empty => sc(acc) //This base case is returning list(). f(acc, 0) here is returning list(0)
      case Node(l, v, r) => loop(f(acc, v), l)( a => loop(a, r)(sc))//loop(f(acc, v), l)(loop(f(acc, r))(sc))
      //case Node(l, v, r) => sc(loop(f(loop(acc, l)( loop(FUCK YEAH, BABY) ), v), r))
    }
    loop(z, t)(sc)
  }

  def dfs[A](t: Tree)(f: Int => Boolean)(sc: List[Int] => A)(fc: () => A): A = {
    def loop(path: List[Int], t: Tree)(fc: () => A): A = t match {
      case Empty => sc(path)
        //If this is part of the path:
      case Node(l, v, r) => if(f(v)) sc(v::path) else fc()//loop(v::path, l)(fc) else sc(path) //( () => loop(v::path, r)(fc)) else sc(path)
      //case Node(l, v, r) => if(f(v)) fc(t::loop(l)(fc)) else fc()
    }
    loop(Nil, t)(fc)
  }

  /*** Regular Expression Parsing ***/

  /* We define a recursive decent parser for regular expressions in
   * REParser.
   * 
   * The REParserLike trait derives from Parsers in the Scala library to make
   * use of it's handing of input (Input) and parsing results (ParseResult).
   * 
   * The Parsers trait is actually a general purpose combinator parser library,
   * which we won't use directly.
   *
   * Grammar. You will want to write a BNF grammar here from your write-up
   * as the basis for your implementation.
   *
   *   re ::= union
   *
   *   union ::= intersect unions
   *   unions ::= epsilon | '|' intersect unions
   *
   *   intersect ::= ???
   *   concat ::= ???
   *   not ::= ???
   *   star ::= ???
   *   atom ::= ???
   * 
   */
  object REParser extends REParserLike {
    /* The following items are the relevant pieces inherited from Parsers
     * 
     * type Input = Reader[Char]
     * sealed abstract class ParseResult[T] {
     *   val next: Input
     *   def map[U](f: T => U): ParseResult[U]
     * }
     * case class Success[T](result: T, next: Input) extends ParseResult[T]
     * case class Failure(next: Input) extends ParseResult[Nothing]
     */

    def re(next: Input): ParseResult[RegExpr] = union(next)

    def union(next: Input): ParseResult[RegExpr] = intersect(next) match {
      case Success(r, next) => {
        def unions(acc: RegExpr, next: Input): ParseResult[RegExpr] =
          if (next.atEnd) Success(acc, next)
          else (next.first, next.rest) match {
            case ('|', next) => intersect(next) match {
              case Success(r, next) => unions(RUnion(acc, r), next)
              case _ => Failure("expected intersect", next)
            }
            case _ => Success(acc, next)
          }
        unions(r, next)
      }
      case _ => Failure("expected intersect", next)
    }

    def intersect(next: Input): ParseResult[RegExpr] = concat(next) match {
      case Success(r, next) => {
        def intersections(acc: RegExpr, next: Input): ParseResult[RegExpr] =
          if (next.atEnd) Success(acc, next)
          else (next.first, next.rest) match {
            case ('&', next) => intersect(next) match {
              case Success(r, next) => intersections(RIntersect(acc, r), next)
              case _ => Failure("expected concat", next)
            }
            case _ => Success(acc, next)
          }
        intersections(r, next)
      }
      case _ => Failure("expected concat", next)
    }

    def concat(next: Input): ParseResult[RegExpr] = not(next) match {
      case Success(r, next) => {}
      case _ => Failure("expected neg", next)
    }

    def not(next: Input): ParseResult[RegExpr] = star(next) match {
        //Just pass on the star:
      case Success(r, next) => Success(r, next)

        //Perform the not operation:
      case Failure(_, next) => (next.first, next.rest) match { //In this case, we need to transform a tilde into a not operator:
        case ('~', rest) => not(rest) match {
          case Success(r, next) => Success(RNeg(r), next)
        }
        case _ => Failure("expected neg", next)
      }
    }

    def star(next: Input): ParseResult[RegExpr] = ???

    /* This set is useful to check if a Char is/is not a regular expression
       meta-language character.  Use delimiters.contains(c) for a Char c. */
    val delimiters = Set('|', '&', '~', '*', '+', '?', '!', '#', '.', '(', ')')

    def atom(next: Input): ParseResult[RegExpr] = ???
  }


  /***  Regular Expression Matching ***/

  /** Tests whether a prefix of chars matches the regular expression re with a continuation for the suffix.
    *
    * @param re a regular expression
    * @param chars a sequence of characters
    * @param sc the success continuation
    * @return if there is a prefix match, then sc is called with the remainder of chars that has yet to be matched. That is, the success continuation sc captures “what to do next if a prefix of chars successfully matches re; if a failure to match is discovered, then false is returned directly.
    */
  def test(re: RegExpr, chars: List[Char])(sc: List[Char] => Boolean): Boolean = (re, chars) match {
    /* Basic Operators */
    case (RNoString, s) => s match {
      case Nil => true
      case _ => false
    } //Empty set
    case (REmptyString, s) => s match {
      case Nil => true
      case _ => false
    }
    case (RSingle(_), Nil) => false
    case (RSingle(c1), c2 :: t) => if(c1==c2) sc(t) else false
    case (RConcat(re1, re2), _) => test(re1, chars) {ch => test(re2, ch)(sc)}
    case (RUnion(re1, re2), _) => test(re1, chars)(sc) || test(re2, chars)(sc)
    case (RStar(re1), _) => test(re1, chars)(sc) || sc(chars)

    /* Extended Operators */
    case (RAnyChar, Nil) => false
    case (RAnyChar, _ :: t) => ???
    case (RPlus(re1), _) => ???
    case (ROption(re1), _) => ???

    /***** Extra Credit Cases *****/
    case (RIntersect(re1, re2), _) => ???
    case (RNeg(re1), _) => ???
  }

  //TODO: Come back to this. The continuation function needs to be a call on test once more.
  def retest(re: RegExpr, s: String): Boolean = test(re, s.toList) { chars => {val str=chars.mkString; s==chars} }


  /*******************************/
  /*** JavaScripty Interpreter ***/
  /*******************************/

  /* This part is optional for fun and extra credit.
   *
   * Note from Michael: You fellas have an interesting definition of "fun" :)
   *
   * If you want your own complete JavaScripty interpreter, you can copy your
   * Lab 5 interpreter here and extend it for the Lab 6 constructs.
   */

  /*** Type Inference ***/

  def typeof(env: TEnv, e: Expr): Typ = ???

  /*** Step ***/

  def substitute(e: Expr, v: Expr, x: String): Expr = ???
  def step(e: Expr): DoWith[Mem,Expr] = ???

  /*** Lower ***/

  def lower(e: Expr): Expr = e

}