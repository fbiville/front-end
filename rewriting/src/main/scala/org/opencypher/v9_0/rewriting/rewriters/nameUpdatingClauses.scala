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

import org.opencypher.v9_0.ast.{Create, CreateUnique, Merge}
import org.opencypher.v9_0.expressions.Expression
import org.opencypher.v9_0.util.attribution.{Attributes, SameId}
import org.opencypher.v9_0.util.{Rewriter, bottomUp}

case class nameUpdatingClauses(attributes: Attributes) extends Rewriter {

  def apply(that: AnyRef): AnyRef = instance(that)

  private val findingRewriter: Rewriter = Rewriter.lift {
    case createUnique@CreateUnique(pattern) =>
      val rewrittenPattern = pattern.endoRewrite(nameAllPatternElements(attributes))
      createUnique.copy(pattern = rewrittenPattern)(createUnique.position)(SameId(createUnique.id))

    case create@Create(pattern) =>
      val rewrittenPattern = pattern.endoRewrite(nameAllPatternElements(attributes))
      create.copy(pattern = rewrittenPattern)(create.position)(SameId(create.id))

    case merge@Merge(pattern, _, _) =>
      val rewrittenPattern = pattern.endoRewrite(nameAllPatternElements(attributes))
      merge.copy(pattern = rewrittenPattern)(merge.position)(SameId(merge.id))
  }

  private val instance = bottomUp(findingRewriter, _.isInstanceOf[Expression])
}
