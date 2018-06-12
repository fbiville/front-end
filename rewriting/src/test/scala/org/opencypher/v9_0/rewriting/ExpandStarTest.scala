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

import org.opencypher.v9_0.ast._
import org.opencypher.v9_0.ast.semantics.{SemanticState, SyntaxExceptionCreator}
import org.opencypher.v9_0.rewriting.rewriters.{expandStar, normalizeReturnClauses, normalizeWithClauses}
import org.opencypher.v9_0.util.attribution.Attributes
import org.opencypher.v9_0.util.inSequence
import org.opencypher.v9_0.util.test_helpers.CypherFunSuite

class ExpandStarTest extends CypherFunSuite with AstConstructionTestSupport {

  import org.opencypher.v9_0.parser.ParserFixture.parser

  test("rewrites * in return") {
    assertRewrite(
      "match (n) return *",
      "match (n) return n")

    assertRewrite(
      "match (n),(c) return *",
      "match (n),(c) return c,n")

    assertRewrite(
      "match (n)-->(c) return *",
      "match (n)-->(c) return c,n")

    assertRewrite(
      "match (n)-[r]->(c) return *",
      "match (n)-[r]->(c) return c,n,r")

    assertRewrite(
      "create (n) return *",
      "create (n) return n")

    assertRewrite(
      "match p = shortestPath((a)-[r*]->(x)) return *",
      "match p = shortestPath((a)-[r*]->(x)) return a,p,r,x")

    assertRewrite(
      "match p=(a:Start)-->(b) return *",
      "match p=(a:Start)-->(b) return a, b, p")
  }

  test("rewrites * in with") {
    assertRewrite(
      "match (n) with * return n",
      "match (n) with n return n")

    assertRewrite(
      "match (n),(c) with * return n",
      "match (n),(c) with c,n return n")

    assertRewrite(
      "match (n)-->(c) with * return n",
      "match (n)-->(c) with c,n return n")

    assertRewrite(
      "match (n)-[r]->(c) with * return n",
      "match (n)-[r]->(c) with c,n,r return n")

    assertRewrite(
      "match (n)-[r]->(c) with *, r.pi as x return n",
      "match (n)-[r]->(c) with c, n, r, r.pi as x return n")

    assertRewrite(
      "create (n) with * return n",
      "create (n) with n return n")

    assertRewrite(
      "match p = shortestPath((a)-[r*]->(x)) with * return p",
      "match p = shortestPath((a)-[r*]->(x)) with a,p,r,x return p")
  }

  test("symbol shadowing should be taken into account") {
    assertRewrite(
      "match a,x,y with a match (b) return *",
      "match a,x,y with a match (b) return a, b")
  }

  test("expands _PRAGMA WITHOUT") {
    assertRewrite(
      "MATCH a,x,y _PRAGMA WITHOUT a MATCH b RETURN *",
      "MATCH a,x,y WITH x, y MATCH b RETURN b, x, y")
  }

  test("keeps listed items during expand") {
    assertRewrite(
      "MATCH (n) WITH *, 1 AS b RETURN *",
      "MATCH (n) WITH n, 1 AS b RETURN b, n"
    )
  }

  private def assertRewrite(originalQuery: String, expectedQuery: String) {
    val attributes = Attributes(idGen)
    val original = prepRewrite(originalQuery, attributes)
    val expected = prepRewrite(expectedQuery, attributes)

    val checkResult = original.semanticCheck(SemanticState.clean)
    val rewriter = expandStar(checkResult.state, attributes)

    val result = original.rewrite(rewriter)
    assert(result === expected)
  }

  private def prepRewrite(q: String, attributes: Attributes, multipleGraphs: Boolean = false) = {
    val mkException = new SyntaxExceptionCreator(q, Some(pos))
    val rewriter = if (multipleGraphs)
      inSequence(normalizeReturnClauses(mkException, attributes), normalizeWithClauses(mkException, attributes))
    else
      inSequence(normalizeReturnClauses(mkException, attributes), normalizeWithClauses(mkException, attributes))
    parser.parse(q).endoRewrite(rewriter)
  }
}
