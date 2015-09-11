import gremlin.scala._
import org.apache.tinkerpop.gremlin.structure.io.IoCore.gryo
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__
import scala.collection.JavaConversions._
import org.scalatest.{ Matchers, WordSpec }

class MovieLensSpec extends WordSpec with Matchers {

  val g: ScalaGraph[TinkerGraph] = {
    val graph = TinkerGraph.open
    graph.io(gryo()).readGraph("src/test/resources/movie-lens.kryo")
    graph.asScala
  }

  "Get the vertex and edge counts for the graph" in {
    g.V.count.head shouldBe 9625
    g.E.count.head shouldBe 969719
  }

  "For each vertex, emit its label, then group and count each distinct label" in {
    val groupCount =
      g.V.label.groupCount.head
    groupCount.get("occupation") shouldBe 21
    groupCount.get("movie") shouldBe 3546
    groupCount.get("person") shouldBe 6040
    groupCount.get("genre") shouldBe 18
  }

  "For each rated-edge, emit its stars property value and compute the average value" in {
    val meanStars =
      g.E.hasLabel("rated")
        .values("stars").mean
        .head
    "%.2f".format(meanStars) shouldBe "3.57"
  }

  "Get the maximum number of movies a single user rated" in {
    val max =
      g.V.hasLabel("person")
      .flatMap(_.outE("rated").count)
      .max
      .head
    max shouldBe 2161
  }

  "What year was the oldest movie made?" in {
    val min =
      g.V.hasLabel("movie")
        .values[Integer]("year")
        .min
        .head
    min shouldBe 1919
  }

  "For each vertex that is labeled 'genre', emit the name property value of that vertex" in {
    val categories =
      g.V.hasLabel("genre")
        .values[String]("name")
        .toSet

    categories should contain("Animation")
    categories should contain("Thriller")
    categories should have size 18
  }

  "For each genre vertex, emit a map of its name and the number of movies it represents" in {
    val genreMovieCounts =
      g.V.hasLabel("genre")
        .as("a", "b")
        .select("a","b")
        .by("name")
        .by(__.inE("hasGenre").count)
        .toList

    genreMovieCounts should have size 18
    assertGenreMovieCount("Animation", 99)
    assertGenreMovieCount("Drama", 1405)

    def assertGenreMovieCount(genre: String, count: Int) = {
      val map = genreMovieCounts.find(_.get("a") == genre).get
      map.get("b") shouldBe count
    }
  }
}
