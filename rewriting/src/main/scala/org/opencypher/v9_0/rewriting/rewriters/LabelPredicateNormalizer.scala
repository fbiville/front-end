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

import org.opencypher.v9_0.expressions.{Expression, HasLabels, NodePattern}
import org.opencypher.v9_0.util.attribution.{Attributes, SameId}

// Rewriter that moves labels from the MATCH pattern to where predicates
// MATCH (a:A:B:C) => MATCH (a) WHERE a:A and a:B and a:C
case class LabelPredicateNormalizer(attributes: Attributes) extends MatchPredicateNormalizer {
  override val extract: PartialFunction[AnyRef, IndexedSeq[Expression]] = {
    case p@NodePattern(Some(id), labels, _, _) if labels.nonEmpty =>
      labels.map {
        l => HasLabels(id.copyId, Seq(l))(p.position)(attributes.copy(p.id))
      }.toIndexedSeq
  }

  override val replace: PartialFunction[AnyRef, AnyRef] = {
    case p@NodePattern(Some(id), labels, _, _) if labels.nonEmpty =>
      p.copy(variable = Some(id.copyId), labels = Seq.empty)(p.position)(SameId(p.id))
  }
}
