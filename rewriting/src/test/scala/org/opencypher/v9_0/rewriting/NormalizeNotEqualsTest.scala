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
package org.opencypher.v9_0.rewriting

import org.opencypher.v9_0.ast.SequentialIds
import org.opencypher.v9_0.expressions.{Equals, Not, NotEquals, _}
import org.opencypher.v9_0.rewriting.rewriters.normalizeNotEquals
import org.opencypher.v9_0.util.DummyPosition
import org.opencypher.v9_0.util.attribution.Attributes
import org.opencypher.v9_0.util.test_helpers.CypherFunSuite


class NormalizeNotEqualsTest extends CypherFunSuite with SequentialIds {

  val pos = DummyPosition(0)
  val lhs: Expression = StringLiteral("42")(pos)
  val rhs: Expression = StringLiteral("42")(pos)

  test("notEquals  iff  not(equals)") {
    val notEquals = NotEquals(lhs, rhs)(pos)
    val output = notEquals.rewrite(normalizeNotEquals(Attributes(idGen)))
    val expected: Expression = Not(Equals(lhs, rhs)(pos))(pos)
    output should equal(expected)
  }

  test("should do nothing on other expressions") {
    val output = lhs.rewrite(normalizeNotEquals(Attributes(idGen)))
    output should equal(lhs)
  }
}
