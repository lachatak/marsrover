package org.kaloz.excercise.marsrover

import org.specs2.mutable.Specification
import org.specs2.specification.AllExpectations
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner

@RunWith(classOf[JUnitRunner])
class MarsExpeditionConfigurationParserTest extends Specification with AllExpectations {

  "A MarsExpeditionConfigurationParser" should {
    "extract expedition condiguration" in {

      val input = """5 5
                    |1 2 N
                    |LMLMLMLMM
                    |3 3 E
                    |MMRMMRMRRM""".stripMargin

      import Action._
      import Facing._

      val parser = new MarsExpeditionConfigurationParser()
      val result = parser.parseAll(parser.marsExpeditionConfiguration, input) match {
        case parser.Success(mec, _) => mec
        case other => other
      }

      result mustEqual MarsExpeditionConfiguration(PlateauConfiguration(5, 5), List(RoverConfiguration(RoverPosition(1, 2, N), List(L, M, L, M, L, M, L, M, M)), RoverConfiguration(RoverPosition(3, 3, E), List(M, M, R, M, M, R, M, R, R, M))))
    }
  }
}
