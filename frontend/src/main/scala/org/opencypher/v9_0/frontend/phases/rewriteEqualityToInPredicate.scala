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
package org.opencypher.v9_0.frontend.phases

import org.opencypher.v9_0.expressions.{In, Variable, _}
import org.opencypher.v9_0.util.attribution.Attributes
import org.opencypher.v9_0.util.{Rewriter, bottomUp}

/*
TODO: This should implement Rewriter instead
 */
case object rewriteEqualityToInPredicate extends StatementRewriter {

  override def description: String = "normalize equality predicates into IN comparisons"


  override def instance(in: BaseState, ctx: BaseContext): Rewriter = {
    val attributes = Attributes(ctx.astIdGen, in.positions())
    bottomUp(Rewriter.lift {
      // id(a) = value => id(a) IN [value]
      case predicate@Equals(func@FunctionInvocation(_, _, _, IndexedSeq(idExpr)), idValueExpr)
        if func.function == functions.Id =>
        In(func.selfThis, ListLiteral(Seq(idValueExpr))(idValueExpr.position)(attributes.copy(idValueExpr.id)))(predicate.position)(attributes.copy(predicate.id))

      // Equality between two property lookups should not be rewritten
      case predicate@Equals(_:Property, _:Property) =>
        predicate.selfThis

      // a.prop = value => a.prop IN [value]
      case predicate@Equals(prop@Property(id: Variable, propKeyName), idValueExpr) =>
        In(prop.selfThis, ListLiteral(Seq(idValueExpr))(idValueExpr.position)(attributes.copy(idValueExpr.id)))(predicate.position)(attributes.copy(predicate.id))
    })
  }

  override def postConditions: Set[Condition] = Set.empty
}
