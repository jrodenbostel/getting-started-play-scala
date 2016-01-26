import controllers.{routes, Widgets}
import org.junit.runner.RunWith
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner
import play.api.libs.json.{JsArray, Json}
import play.api.mvc.{Result, _}
import play.api.test.Helpers._
import play.api.test.{WithApplication, _}
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.commands.LastError
import reactivemongo.bson.BSONDocument
import repos.WidgetRepoImpl

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by justin on 1/6/16.
  */
@RunWith(classOf[JUnitRunner])
class ApplicationSpec extends Specification with Results with Mockito {

  val mockRecipeRepo = mock[WidgetRepoImpl]
  val reactiveMongoApi = mock[ReactiveMongoApi]
  val documentId = "56a0ddb6c70000c700344254"
  val lastRequestStatus = new LastError(true, None, None, None, 0, None, false, None, None, false, None, None)

  val widgetOne = Json.obj(
        "name" -> "Widget One",
        "description" -> "My first widget",
        "author" -> "Justin"
      )

  val posts = List(
    widgetOne,
    Json.obj(
      "name" -> "Widget Two: The Return",
      "description" -> "My second widget",
      "author" -> "Justin"
    ))

  class TestController() extends Widgets(reactiveMongoApi) {
    override def widgetRepo: WidgetRepoImpl = mockRecipeRepo
  }

  val controller = new TestController()

  "Application" should {

    "send 404 on a bad request" in {
      new WithApplication() {
        route(FakeRequest(GET, "/boum")) must beSome.which(status(_) == NOT_FOUND)
      }
    }

    "Recipes#delete" should {
      "remove recipe" in {
        mockRecipeRepo.remove(any[BSONDocument])(any[ExecutionContext]) returns Future(lastRequestStatus)

        val result: Future[Result] = controller.delete(documentId).apply(FakeRequest())

        status(result) must be equalTo ACCEPTED
        there was one(mockRecipeRepo).remove(any[BSONDocument])(any[ExecutionContext])
      }
    }

    "Recipes#list" should {
      "list recipes" in {
        mockRecipeRepo.find()(any[ExecutionContext]) returns Future(posts)

        val result: Future[Result] = controller.index().apply(FakeRequest())

        contentAsJson(result) must be equalTo JsArray(posts)
        there was one(mockRecipeRepo).find()(any[ExecutionContext])
      }
    }

    "Recipes#read" should {
      "read recipe" in {
        mockRecipeRepo.select(any[String])(any[ExecutionContext]) returns Future(Option(widgetOne))

        val result: Future[Result] = controller.read(documentId).apply(FakeRequest())

        contentAsJson(result) must be equalTo widgetOne
        there was one(mockRecipeRepo).select(any[String])(any[ExecutionContext])
      }
    }

    "Recipes#create" should {
      "create recipe" in {
        mockRecipeRepo.save(any[BSONDocument])(any[ExecutionContext]) returns Future(lastRequestStatus)

        val request = FakeRequest().withBody(widgetOne)
        val result: Future[Result] = controller.create()(request)

        status(result) must be equalTo CREATED
        there was one(mockRecipeRepo).save(any[BSONDocument])(any[ExecutionContext])
      }
    }

    "Recipes#update" should {
      "update recipe" in {
        mockRecipeRepo.update(any[BSONDocument], any[BSONDocument])(any[ExecutionContext]) returns Future(lastRequestStatus)

        val request = FakeRequest().withBody(widgetOne)
        val result: Future[Result] = controller.update(documentId)(request)

        status(result) must be equalTo ACCEPTED
        there was one(mockRecipeRepo).update(any[BSONDocument], any[BSONDocument])(any[ExecutionContext])
      }
    }
  }
}
