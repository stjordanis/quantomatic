package quanto.data.test

import quanto.data._
import quanto.util.json._
import org.scalatest._
import java.awt.Color

class TheorySpec extends FlatSpec {
  behavior of "A theory"

  val rgValueDesc = Theory.ValueDesc(
    typ = Vector(Theory.ValueType.String),
    latexConstants = true,
    validateWithCore = true
  )

  val hValueDesc = Theory.ValueDesc(
    typ = Vector(Theory.ValueType.Empty),
    latexConstants = false,
    validateWithCore = false
  )

  val rStyleDesc = Theory.VertexStyleDesc(
    shape = Theory.VertexShape.Circle,
    strokeColor = Color.BLACK,
    fillColor = Color.RED,
    labelPosition = Theory.VertexLabelPosition.Below,
    labelForegroundColor = Color.BLACK,
    labelBackgroundColor = None
  )

  val gStyleDesc = rStyleDesc.copy(fillColor = Color.GREEN)

  val hStyleDesc = rStyleDesc.copy(
    shape = Theory.VertexShape.Rectangle,
    fillColor = Color.YELLOW,
    labelPosition = Theory.VertexLabelPosition.Center
  )

  val thy = Theory(
    name = "Test Theory",
    coreName = "test_theory",
    vertexTypes = Map(
      "red" -> Theory.VertexDesc(
        value = rgValueDesc,
        style = rStyleDesc,
        defaultData = JsonObject("type"->"red", "label"->"0", "value" -> JsonObject("pretty"->"0"))
      ),
      "green" -> Theory.VertexDesc(
        value = rgValueDesc,
        style = gStyleDesc,
        defaultData = JsonObject("type"->"green", "label"->"0", "value" -> JsonObject("pretty"->"0"))
      ),
      "hadamard" -> Theory.VertexDesc(
        value = hValueDesc,
        style = hStyleDesc,
        defaultData = JsonObject("type"->"hadamard")
      )
    ),
    defaultVertexType = "red"
  )

  val thyJson = Json.parse(
    """
      |{
      |  "name" : "Test Theory",
      |  "core_name" : "test_theory",
      |  "vertex_types" : {
      |    "red" : {
      |      "value" : {
      |        "type" : "string",
      |        "latex_constants" : true,
      |        "validate_with_core" : true
      |      },
      |      "style" : {
      |        "label" : {
      |          "position" : "below",
      |          "fg_color" : [ 0.0, 0.0, 0.0 ]
      |        },
      |        "stroke_color" : [ 0.0, 0.0, 0.0 ],
      |        "fill_color" : [ 1.0, 0.0, 0.0 ],
      |        "shape" : "circle",
      |        "stroke_width" : 1
      |      },
      |      "default_data" : {
      |        "type" : "red",
      |        "label" : "0",
      |        "value" : {
      |          "pretty" : "0"
      |        }
      |      }
      |    },
      |    "green" : {
      |      "value" : {
      |        "type" : "string",
      |        "latex_constants" : true,
      |        "validate_with_core" : true
      |      },
      |      "style" : {
      |        "label" : {
      |          "position" : "below",
      |          "fg_color" : [ 0.0, 0.0, 0.0 ]
      |        },
      |        "stroke_color" : [ 0.0, 0.0, 0.0 ],
      |        "fill_color" : [ 0.0, 1.0, 0.0 ],
      |        "shape" : "circle",
      |        "stroke_width" : 1
      |      },
      |      "default_data" : {
      |        "type" : "green",
      |        "label" : "0",
      |        "value" : {
      |          "pretty" : "0"
      |        }
      |      }
      |    },
      |    "hadamard" : {
      |      "value" : {
      |        "type" : "empty",
      |        "latex_constants" : false,
      |        "validate_with_core" : false
      |      },
      |      "style" : {
      |        "label" : {
      |          "position" : "center",
      |          "fg_color" : [ 0.0, 0.0, 0.0 ]
      |        },
      |        "stroke_color" : [ 0.0, 0.0, 0.0 ],
      |        "fill_color" : [ 1.0, 1.0, 0.0 ],
      |        "shape" : "rectangle",
      |        "stroke_width" : 1
      |      },
      |      "default_data" : {
      |        "type" : "hadamard"
      |      }
      |    }
      |  },
      |  "default_vertex_type" : "red",
      |  "default_edge_type" : "plain",
      |  "edge_types" : {
      |    "plain" : {
      |      "value" : {
      |        "type" : "empty",
      |        "latex_constants" : false,
      |        "validate_with_core" : false
      |      },
      |      "style" : {
      |        "stroke_color" : [ 0.0, 0.0, 0.0 ],
      |        "stroke_width" : 1,
      |        "label" : {
      |          "position" : "auto",
      |          "fg_color" : [ 0.0, 0.0, 0.0 ]
      |        }
      |      },
      |      "default_data" : {
      |        "type" : "plain"
      |      }
      |    }
      |  }
      |}
    """.stripMargin)

  it should "save to JSON" in {
    var compiled = Theory.toJson(thy)
    var expected = thyJson
    assert(compiled == expected)
  }

  it should "load from JSON" in {
    var loaded : Theory = Theory.fromJson(thyJson)
    assert(loaded.vertexTypes === thy.vertexTypes)
    print(loaded.vertexTypes)
    assert(loaded === thy)
  }

  behavior of "mixing theories"

  it should "mix with itself" in {
    assert(thy.mixin(thy, None) == thy)
  }

  val plain = Theory.fromFile("plain")
  val rg = Theory.fromFile("red_green")

  it should "mix with others" in {
    val mixedPlainRG = plain.mixin(rg, Some("plain with red_green"))
    assert(mixedPlainRG.vertexTypes.keySet == Set("var", "hadamard", "Z", "X"))
  }

  it should "mix with fragments" in {
    assert(plain.mixin(newVertexTypes = rg.vertexTypes.filter(_._1 == "Z")).vertexTypes.keySet == Set("var", "Z"))
  }
}
