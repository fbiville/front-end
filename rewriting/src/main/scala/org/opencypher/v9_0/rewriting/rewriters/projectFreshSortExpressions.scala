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

import org.opencypher.v9_0.ast._
import org.opencypher.v9_0.expressions.Expression
import org.opencypher.v9_0.util.attribution.{Attributes, SameId}
import org.opencypher.v9_0.util.{Rewriter, bottomUp}

/**
 * This rewriter ensures that WITH clauses containing a ORDER BY or WHERE are split, such that the ORDER BY or WHERE does not
 * refer to any newly introduced variable.
 *
 * This is required due to constraints in the planner. Note that this structure is invalid for semantic checking, which requires
 * that ORDER BY and WHERE _only refer to variables introduced in the associated WITH_.
 *
 * Additionally, it splits RETURN clauses containing ORDER BY. This would typically be done earlier during normalizeReturnClauses, however
 * "RETURN * ORDER BY" is not handled at that stage, due to lacking variable information. If expandStar has already been run, then this
 * will now work as expected.
 */
case class projectFreshSortExpressions(attributes: Attributes) extends Rewriter {

  override def apply(that: AnyRef): AnyRef = instance(that)

  private val clauseRewriter: Clause => Seq[Clause] = {
    case clause@With(_, _, None, _, _, None) =>
      Seq(clause.selfThis)

    case clause@With(_, ri: ReturnItems, orderBy, skip, limit, where) =>
      val allAliases = ri.aliases
      val passedThroughAliases = ri.passedThrough
      val evaluatedAliases = allAliases -- passedThroughAliases

      if (evaluatedAliases.isEmpty) {
        Seq(clause.selfThis)
      } else {
        val nonItemDependencies = orderBy.map(_.dependencies).getOrElse(Set.empty) ++
            skip.map(_.dependencies).getOrElse(Set.empty) ++
            limit.map(_.dependencies).getOrElse(Set.empty) ++
            where.map(_.dependencies).getOrElse(Set.empty)
        val dependenciesFromPreviousScope = nonItemDependencies -- allAliases

        val passedItems = dependenciesFromPreviousScope.map(v => AliasedReturnItem(v)(attributes.copy(v.id)))
        val outputItems = allAliases.toIndexedSeq.map(v => AliasedReturnItem(v)(attributes.copy(v.id)))

        val result = Seq(
          clause.copy(returnItems = ri.mapItems(originalItems => originalItems ++ passedItems), orderBy = None, skip = None, limit = None, where = None)(clause.position)(attributes.copy(clause.id)),
          clause.copy(distinct = false, returnItems = ri.mapItems(_ => outputItems))(clause.position)(attributes.copy(clause.id))
        )
        result
      }

    case clause =>
      Seq(clause)
  }

  private val rewriter = Rewriter.lift {
    case query@SingleQuery(clauses) =>
      query.copy(clauses = clauses.flatMap(clauseRewriter))(query.position)(SameId(query.id))
  }

  private val instance: Rewriter = bottomUp(rewriter, _.isInstanceOf[Expression])
}
