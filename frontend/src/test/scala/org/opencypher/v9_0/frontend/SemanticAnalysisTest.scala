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
package org.opencypher.v9_0.frontend

import org.opencypher.v9_0.ast.AstConstructionTestSupport
import org.opencypher.v9_0.ast.semantics.SemanticErrorDef
import org.opencypher.v9_0.util.symbols._
import org.opencypher.v9_0.util.test_helpers.CypherFunSuite
import org.opencypher.v9_0.frontend.phases._
import org.opencypher.v9_0.util.attribution.{Attributes, IdGen, SequentialIdGen}

class SemanticAnalysisTest extends CypherFunSuite with AstConstructionTestSupport {

  // This test invokes SemanticAnalysis twice because that's what the production pipeline does
  val pipeline: Transformer[BaseContext, BaseState, BaseState] =
    Parsing(Attributes(new SequentialIdGen())) andThen SemanticAnalysis(warn = true) andThen SemanticAnalysis(warn = false)

  test("can inject starting semantic state") {
    val query = "RETURN name AS name"
    val startState = initStartState(query, Map("name" -> CTString))

    pipeline.transform(startState, ErrorCollectingContext)

    ErrorCollectingContext.errors shouldBe empty
  }

  test("can inject starting semantic state for larger query") {
    val query = "MATCH (n:Label {name: name}) WHERE n.age > age RETURN n.name AS name"

    val startState = initStartState(query, Map("name" -> CTString, "age" -> CTInteger))

    pipeline.transform(startState, ErrorCollectingContext)

    ErrorCollectingContext.errors shouldBe empty
  }

  private def initStartState(query: String, initialFields: Map[String, CypherType]) =
    InitialState(query, None, NoPlannerName, initialFields)
}

object ErrorCollectingContext extends BaseContext {

  var errors: Seq[SemanticErrorDef] = Seq.empty

  override def tracer = CompilationPhaseTracer.NO_TRACING
  override def notificationLogger = devNullLogger
  override def exceptionCreator = ???
  override def monitors = ???
  override def errorHandler = (errs: Seq[SemanticErrorDef]) =>
    errors = errs
  override val astIdGen: IdGen = new SequentialIdGen()
}

object NoPlannerName extends PlannerName {
  override def name = "no planner"
  override def toTextOutput = "no planner"
  override def version = "no version"
}
