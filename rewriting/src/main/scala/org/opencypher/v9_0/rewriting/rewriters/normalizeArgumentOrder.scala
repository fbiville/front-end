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
package org.opencypher.v9_0.rewriting.rewriters

import org.opencypher.v9_0.expressions.{InequalityExpression, _}
import org.opencypher.v9_0.util.attribution.SameId
import org.opencypher.v9_0.util.{Rewriter, topDown}

case object normalizeArgumentOrder extends Rewriter {

  override def apply(that: AnyRef): AnyRef = instance(that)

  private val instance: Rewriter = topDown(Rewriter.lift {

    // move id(n) on equals to the left
    case predicate @ Equals(func: FunctionInvocation, _) if func.function == functions.Id =>
      predicate.selfThis

    case predicate @ Equals(lhs, rhs: FunctionInvocation) if rhs.function == functions.Id =>
      predicate.copy(lhs = rhs, rhs = lhs)(predicate.position)(SameId(predicate.id))

    // move n.prop on equals to the left
    case predicate @ Equals(Property(_, _), _) =>
      predicate.selfThis

    case predicate @ Equals(lhs, rhs @ Property(_, _)) =>
      predicate.copy(lhs = rhs.selfThis, rhs = lhs)(predicate.position)(SameId(predicate.id))

    case inequality: InequalityExpression =>
      val lhsIsProperty = inequality.lhs.isInstanceOf[Property]
      val rhsIsProperty = inequality.rhs.isInstanceOf[Property]
      if (!lhsIsProperty && rhsIsProperty) {
        inequality.swapped
      } else {
        inequality
      }
  })
}
