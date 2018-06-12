/*
 * Copyright © 2002-2018 Neo4j Sweden AB (http://neo4j.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.opencypher.v9_0.ast

import org.opencypher.v9_0.expressions.{Parameter, _}
import org.opencypher.v9_0.util.attribution.SequentialIdGen
import org.opencypher.v9_0.util.symbols._
import org.opencypher.v9_0.util.test_helpers.CypherFunSuite

class ConstantExpressionTest extends CypherFunSuite {
  private implicit val idGen = new SequentialIdGen()

  test("tests") {
    assertIsConstant(SignedDecimalIntegerLiteral("42")(null))
    assertIsConstant(Parameter("42", CTAny)(null))
    assertIsConstant(ListLiteral(Seq(SignedDecimalIntegerLiteral("42")(null)))(null))
  }

  private def assertIsConstant(e: Expression) = {
    val unapply = ConstantExpression.unapply(e)
    if (unapply.isEmpty) fail(s"$e should be considered constant")
  }
}
