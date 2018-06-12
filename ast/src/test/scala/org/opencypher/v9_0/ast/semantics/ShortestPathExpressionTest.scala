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

import org.opencypher.v9_0.expressions
import org.opencypher.v9_0.expressions._
import org.opencypher.v9_0.util.attribution.{IdGen, SequentialIdGen}
import org.opencypher.v9_0.util.symbols.{TypeSpec, _}

class ShortestPathExpressionTest extends SemanticFunSuite {
  private implicit val idGen: IdGen = new SequentialIdGen()

  test("should get correct types for shortestPath") {
    // Given
    val (exp, state) = makeShortestPathExpression(true)

    // When
    val result = SemanticExpressionCheck.simple(exp)(state)

    // Then
    result.errors shouldBe empty
    types(exp)(result.state) should equal(TypeSpec.exact(CTPath))
  }

  test("should get correct types for allShortestPath") {
    // Given
    val (exp, state) = makeShortestPathExpression(false)

    // When
    val result =  SemanticExpressionCheck.simple(exp)(state)

    // Then
    result.errors shouldBe empty
    types(exp)(result.state) should equal(TypeSpec.exact(CTList(CTPath)))
  }

  private def makeShortestPathExpression(single: Boolean): (ShortestPathExpression, SemanticState) = {
    val state = Seq("n", "k").foldLeft(SemanticState.clean) { (acc, n) =>
      acc.specifyType(variable(n), TypeSpec.exact(CTNode)).right.get
    }
    val pattern = chain(node(Some(variable("n"))), relationship(None), node(Some(variable("k"))))
    (ShortestPathExpression(ShortestPaths(pattern, single)(pos)), state)
  }
  private def chain(left: PatternElement, rel: RelationshipPattern, right: NodePattern): RelationshipChain = {
    expressions.RelationshipChain(left, rel, right)(pos)
  }

  private def relationship(id: Option[Variable]): RelationshipPattern = {
    RelationshipPattern(id, Seq.empty, None, None, SemanticDirection.OUTGOING)(pos)
  }

  private def node(id: Option[Variable]): NodePattern = {
    expressions.NodePattern(id, Seq.empty, None)(pos)
  }

}
