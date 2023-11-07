package controllers

import java.io.InputStreamReader
import com.github.tototoshi.csv.CSVReader
import javax.inject._
import play.api._
import play.api.mvc._
import scala.util.Using

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class MainController @Inject()(val controllerComponents: ControllerComponents) extends BaseController {


  private final val filePath = "/data/tinyset.csv"

  //given the time constraints im just hoping that the CSV reader is getting cleaned up by the garbage collector,
  //normally i'd verify that an object this large goes away after the initial load
  lazy val loaded: List[RelevantFields] = {
    Using.Manager { use =>
      val inputStream = use(this.getClass.getResourceAsStream(filePath))
      val reader = use(CSVReader.open(new InputStreamReader(inputStream)))
      val relevantFields: List[RelevantFields] =
        reader.allWithHeaders()
          .flatMap(RelevantFields.fromDict(_))
      relevantFields
    }.getOrElse(List())
  }

  // case classes with many fields are hard to work with (writing a function to auto-load one without enumerating all the fields would probably be impossible without reflection for example)
  // it really pains me to have to write all of these fields _three_ times like this but here we are
  case class RelevantFields(
                             addr_state: String,
                             loan_amnt: String,
                             grade: String
                           )

  object RelevantFields {
    def fromDict(dict: Map[String, String]): Option[RelevantFields] = {
      (dict.get("addr_state"),
        dict.get("loan_amnt"),
        dict.get("grade")
      ) match {
        case (Some(state), Some(loanAmt), Some(grade)) => Some(RelevantFields(state, loanAmt, grade))
        case _ => None
      }
    }

    val fromNameFunc: String =>
      RelevantFields => String =
      RelevantFields.fromName _

    def fromName(field: String)(data: RelevantFields) = {
      field match {
        case "addr_state" => data.addr_state
        case "loan_amnt" => data.loan_amnt
        case "grade" => data.grade
      }
    }

    //this whitelists the numerical fields, the categorical fields default to any fields not in the explicitely-numerical list
    val numericalFields: List[String] = List(
      "loan_amnt"
    )
  }

  def index = Action {
    val relevantFields = loaded.mkString("\n")
    Ok(s"${relevantFields}")
  }

    def byState = Action {
      val groupedByState = loaded.groupBy(_.addr_state).map(group => (group._1, group._2.map(_.loan_amnt).map(s => s.toInt).sum / group._2.size))
      val sorted = groupedByState.toList.sortBy(x => x._2).reverse //default sort here is ascending
      val prettySorted = sorted.map{ case (state, amt) => s"$state: $amt" }
      Ok(s"${prettySorted.mkString("\n")}")
    }

  //currently the only aggregation available here is an average
    def AggregateByField(aggregationField: String, aggregatedField: String) = Action {
      //start by checking the field we want to group on is categorical and the aggregated field is numerical
      if(RelevantFields.numericalFields.contains(aggregatedField) && !RelevantFields.numericalFields.contains(aggregationField)) {
        val aggregationFunc = RelevantFields.fromNameFunc(aggregationField)
        val aggregatedFunc = RelevantFields.fromNameFunc(aggregatedField)
        //we're doing int math here so everything is rounded
        val groupedByField = loaded.groupBy(aggregationFunc).map(group => (group._1, group._2.map(aggregatedFunc).map(s => s.toInt).sum / group._2.size))
        val sorted = groupedByField.toList.sortBy(x => x._2).reverse //gonna default to descending sort for numerical fields
        val prettySorted = sorted.map { case (field, avg) => s"$field: $avg" }
        Ok(s"${prettySorted.mkString("\n")}")
      }
      else BadRequest("invalid fields selected")
    }

    def statsByField(selectedField: String) = Action {
      if(RelevantFields.numericalFields.contains(selectedField)) {
        val selectFunc = RelevantFields.fromNameFunc(selectedField)
        val selected = loaded.map(selectFunc).map(_.toInt)

        val mean = selected.sum / selected.size

        //this doesn't take into account a shared mode
        val groupedByField = selected.groupBy(identity).map(x => (x._1, x._2.size))
        val mode = groupedByField.toList.maxBy(_._2)._1

        val min = selected.min
        val max = selected.max

        //if I had more time I'd import a real numerics library and do std deviation and more detailed stats
        val output = s"Mean: $mean\nMode: $mode\nMinimum: $min\nMaximum: $max"
        Ok(output)
      }
      else BadRequest("invalid fields selected")
    }
}
