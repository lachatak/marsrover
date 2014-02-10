package org.kaloz.excercise

import scala.util.parsing.combinator.JavaTokenParsers

class ParserCombinator extends App{


}

class BasicParser extends JavaTokenParsers {
  def integer = decimalNumber ^^ {
    case x => Integer(x.toInt)
  }
  def addition = integer ~ "+" ~ integer ^^ {
    case left~plus~right => Addition(left,right)
  }

  def subtraction = integer ~ "-" ~ integer ^^ {
    case left~sub~right => Subtraction(left,right)
  }


  def multiplication = integer ~ "*" ~ integer ^^ {
    case left~mult~right => Multiplication(left,right)
  }

  def division = integer ~ "/" ~ integer ^^ {
    case left~div~right => Division(left,right)
  }

  def operation = (addition | subtraction | multiplication | division)

  def eval(input:String) = {
    parseAll(operation, input) match {
      case Success(result, _) => evalTerm(result)
      case _ =>
    }
  }

  def evalTerm(term:Term) = {
    term match {
      case Integer(x:Int) => x
//      case Addition(x,y) => evalTerm(x) + evalTerm(y)
//      case Subtraction(x,y) =>  evalTerm(x) - evalTerm(y)
//      case Multiplication(x,y) => evalTerm(x) * evalTerm(y)
//      case Division(x,y) => evalTerm(x) / evalTerm(y)
    }
  }
}

abstract class Term
case class Integer(value:Int) extends Term
case class Addition(left:Term,right:Term) extends Term
case class Subtraction(left:Term,right:Term) extends Term
case class Multiplication(left:Term,right:Term) extends Term
case class Division(left:Term,right:Term) extends Term
