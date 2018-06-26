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

import org.opencypher.v9_0.frontend.phases.CompilationPhaseTracer.CompilationPhase.AST_REWRITE
import org.opencypher.v9_0.rewriting.rewriters._
import org.opencypher.v9_0.util.attribution.Attributes
import org.opencypher.v9_0.util.inSequence

case object PreparatoryRewriting extends Phase[BaseContext, BaseState, BaseState] {

  override def process(from: BaseState, context: BaseContext): BaseState = {
    val attributes = Attributes(context.astIdGen, from.positions())

    val rewrittenStatement = from.statement().endoRewrite(inSequence(
      normalizeReturnClauses(context.exceptionCreator, attributes),
      normalizeWithClauses(context.exceptionCreator, attributes),
      expandCallWhere(attributes),
      replaceAliasedFunctionInvocations,
      mergeInPredicates(attributes)))

    from.withStatement(rewrittenStatement)
  }

  override val phase = AST_REWRITE

  override val description = "rewrite the AST into a shape that semantic analysis can be performed on"

  override def postConditions: Set[Condition] = Set.empty
}
