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

import org.opencypher.v9_0.ast.semantics.SemanticState
import org.opencypher.v9_0.ast.{Statement, UnaliasedReturnItem}
import org.opencypher.v9_0.expressions.NotEquals
import org.opencypher.v9_0.rewriting.RewriterStep._
import org.opencypher.v9_0.rewriting.conditions._
import org.opencypher.v9_0.rewriting.RewriterStep
import org.opencypher.v9_0.util.attribution.Attributes
import org.opencypher.v9_0.rewriting.rewriters.{replaceLiteralDynamicPropertyLookups, _}
import org.opencypher.v9_0.rewriting.{RewriterCondition, RewriterStepSequencer}

class ASTRewriter(rewriterSequencer: String => RewriterStepSequencer,
                  literalExtraction: LiteralExtraction,
                  getDegreeRewriting: Boolean) {

  def rewrite(queryText: String, statement: Statement, semanticState: SemanticState, attributes: Attributes): (Statement, Map[String, Any], Set[RewriterCondition]) = {

  val rewriters = Seq[RewriterStep](
      recordScopes(semanticState, attributes),
      desugarMapProjection(attributes),
      normalizeComparisons(attributes),
      enableCondition(noReferenceEqualityAmongVariables),
      enableCondition(containsNoNodesOfType[UnaliasedReturnItem]),
      enableCondition(noDuplicatesInReturnItems),
      expandStar(semanticState, attributes),
      enableCondition(containsNoReturnAll),
      foldConstants(attributes),
      nameMatchPatternElements(attributes),
      nameUpdatingClauses(attributes),
      enableCondition(noUnnamedPatternElementsInMatch),
      normalizeMatchPredicates(getDegreeRewriting, attributes),
      normalizeNotEquals(attributes),
      enableCondition(containsNoNodesOfType[NotEquals]),
      normalizeArgumentOrder,
      normalizeSargablePredicates,
      enableCondition(normalizedEqualsArguments),
      addUniquenessPredicates(attributes),
      replaceLiteralDynamicPropertyLookups,
      namePatternComprehensionPatternElements(attributes),
      enableCondition(noUnnamedPatternElementsInPatternComprehension),
      inlineNamedPathsInPatternComprehensions(attributes)
    )

    val contract = rewriterSequencer("ASTRewriter")(rewriters:_*)


    val rewrittenStatement = statement.endoRewrite(contract.rewriter)
    val (extractParameters, extractedParameters) = literalReplacement(rewrittenStatement, literalExtraction)

    (rewrittenStatement.endoRewrite(extractParameters), extractedParameters, contract.postConditions)
  }
}
