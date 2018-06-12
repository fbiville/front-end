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
package org.opencypher.v9_0.ast.semantics

import org.opencypher.v9_0.expressions.Variable
import org.opencypher.v9_0.util.DummyPosition
import org.opencypher.v9_0.util.attribution.SequentialIdGen
import org.opencypher.v9_0.util.symbols._

class VariableTest extends SemanticFunSuite {

  test("shouldDefineVariableDuringSemanticCheckWhenUndefined") {
    val position = DummyPosition(0)
    val variable = Variable("x")(position)(new SequentialIdGen())

    val result = SemanticExpressionCheck.simple(variable)(SemanticState.clean)
    result.errors should have size 1
    result.errors.head.position should equal(position)
    result.state.symbol("x").isDefined should equal(true)
    result.state.symbolTypes("x") should equal(CTAny.covariant)
  }
}
