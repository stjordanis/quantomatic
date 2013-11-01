package quanto.gui

import graphview.GraphView
import quanto.data._
import swing._
import swing.event._
import javax.swing.ImageIcon
import quanto.util.swing.ToolBar
import quanto.util.{SockJson,SockJsonError,SockJsonErrorType}

class GraphEditPanel(theory: Theory, val readOnly: Boolean = false) extends BorderPanel {
  // GUI components
  val graphView = new GraphView(theory) {
    drawGrid = true
    dynamicResize = true
    focusable = true
  }

  val graphDocument = new GraphDocument(graphView)
  def graph = graphDocument.graph
  def graph_=(g: Graph) { graphDocument.graph = g }

  // alias for graph_=, used in java code
  def setGraph(g: Graph) { graph_=(g) }


  val VertexTypeLabel  = new Label("Vertex Type:  ") { xAlignment = Alignment.Right; enabled = false }
  val VertexTypeSelect = new ComboBox("<wire>" +: theory.vertexTypes.keys.toSeq) { enabled = false }
  val EdgeTypeLabel    = new Label("Edge Type:  ") { xAlignment = Alignment.Right; enabled = false }
  val EdgeTypeSelect   = new ComboBox(theory.edgeTypes.keys.toSeq) { enabled = false }
  val EdgeDirected     = new CheckBox("directed") { selected = true; enabled = false }

  // Bottom panel
  object BottomPanel extends GridPanel(1,5) {
    contents += (VertexTypeLabel, VertexTypeSelect)
    contents += (EdgeTypeLabel, EdgeTypeSelect, EdgeDirected)
  }

  val graphEditController = new GraphEditController(graphView, readOnly) {
    undoStack            = graphDocument.undoStack
    vertexTypeSelect     = VertexTypeSelect
    edgeTypeSelect       = EdgeTypeSelect
    edgeDirectedCheckBox = EdgeDirected
  }

  val GraphViewScrollPane = new ScrollPane(graphView)

  trait ToolButton { var tool: MouseState = SelectTool() }

  val SelectButton = new ToggleButton() with ToolButton {
    icon = new ImageIcon(GraphEditor.getClass.getResource("select-rectangular.png"), "Select")
    selected = true
    tool = SelectTool()
  }

  val AddVertexButton = new ToggleButton() with ToolButton {
    icon = new ImageIcon(GraphEditor.getClass.getResource("draw-ellipse.png"), "Add Vertex")
    tool = AddVertexTool()
  }

  val AddEdgeButton = new ToggleButton() with ToolButton {
    icon = new ImageIcon(GraphEditor.getClass.getResource("draw-path.png"), "Add Edge")
    tool = AddEdgeTool()
  }

  val AddBangBoxButton = new ToggleButton() with ToolButton {
    icon = new ImageIcon(GraphEditor.getClass.getResource("draw-bang.png"), "Add Bang Box")
    tool = AddBangBoxTool()
  }

  val ReLayoutButton = new Button("Re-Layout")
  val ConnectButton = new Button("Connect")
  val PrevButton = new Button("Backtrack")
  val NextButton = new Button("Next")
  val DisconnectButton = new Button("Finish")

   def setEvalButtonStatus (con : Boolean, discon : Boolean, prev : Boolean, next : Boolean) {
     PrevButton.enabled_=(prev);
     NextButton.enabled_=(next);
     ConnectButton.enabled_=(con);
     DisconnectButton.enabled_=(discon);

   }

  setEvalButtonStatus (true, false, false, false);

   private def error(action: String, reason: String) {
    Dialog.showMessage(
      title = "Info",
      message = action + " (" + reason + ") ")
  }

  val GraphToolGroup = new ButtonGroup(SelectButton,
                                       AddVertexButton, 
                                       AddEdgeButton,
                                       AddBangBoxButton,
                                       ReLayoutButton,
                                       ConnectButton,
                                       DisconnectButton,
                                       NextButton,
                                       PrevButton
                                      )

  val MainToolBar = new ToolBar {
    contents += (SelectButton, AddVertexButton, AddEdgeButton, AddBangBoxButton);
    addSeparator();
    contents += (ReLayoutButton)
    addSeparator();
    contents += (ConnectButton, DisconnectButton, PrevButton, NextButton)
  }


  if (!readOnly) {
    add(MainToolBar, BorderPanel.Position.North)
    add(BottomPanel, BorderPanel.Position.South)
  }

  add(GraphViewScrollPane, BorderPanel.Position.Center)


  listenTo(GraphViewScrollPane, graphDocument)
  GraphToolGroup.buttons.foreach(listenTo(_))
  reactions += {
    case UIElementResized(GraphViewScrollPane) => graphView.repaint()
    case ButtonClicked(t: ToolButton) =>
      graphEditController.mouseState = t.tool
      t.tool match {
        case SelectTool() =>
          VertexTypeLabel.enabled = false
          VertexTypeSelect.enabled = false
          EdgeTypeLabel.enabled = false
          EdgeTypeSelect.enabled = false
          EdgeDirected.enabled = false
        case AddVertexTool() =>
          VertexTypeLabel.enabled = true
          VertexTypeSelect.enabled = true
          EdgeTypeLabel.enabled = false
          EdgeTypeSelect.enabled = false
          EdgeDirected.enabled = false
        case AddEdgeTool() =>
          VertexTypeLabel.enabled = false
          VertexTypeSelect.enabled = false
          EdgeTypeLabel.enabled = true
          EdgeTypeSelect.enabled = true
          EdgeDirected.enabled = true
        case AddBangBoxTool() =>
          VertexTypeLabel.enabled = false
          VertexTypeSelect.enabled = false
          EdgeTypeLabel.enabled = false
          EdgeTypeSelect.enabled = false
          EdgeDirected.enabled = false
        case _ =>
      }
    case ButtonClicked (ReLayoutButton) =>
      graphDocument.reLayout();

    case ButtonClicked (ConnectButton) =>
     try{
        SockJson.connectSock();
        try{
          val mode = SockJson.requestMode ();
          /* TOOD: LYH - need to check that if the graph is valid, e.g. containing GN ? */
          val edata = SockJson.requestInit(mode, graphDocument.exportJson());
          graphDocument.loadGraph (edata);
          graphDocument.reLayout();
          setEvalButtonStatus (false, true, false, true)
        }
        catch{
          case _ => error ("Can't init graph", "graph can't be initialised")
        }
     }
     catch{
       case _ => error ("Can't connect to Isabelle", "socket err")
     }

    case ButtonClicked (DisconnectButton) =>
      SockJson.requestDeinit ();
      SockJson.closeSock ();
      setEvalButtonStatus (true, false, false, false);
    case ButtonClicked (PrevButton) =>
      try{
        val edata = SockJson.requestPrev();
        graphDocument.loadGraph (edata);
        graphDocument.reLayout();
        setEvalButtonStatus (false, true, true, true);
      }
      catch{
        case SockJsonError(_, SockJsonErrorType.ErrEval) =>
          setEvalButtonStatus (false, true, false, false);
          error ("Can't show the current step", "eval error")

        case SockJsonError(_, SockJsonErrorType.NoPrev) =>
          PrevButton.enabled_=(false)
          error ("No prev step", "already the first step begining now")

        case _ =>
          error ("Can't backtrack the previous step", "unknown error")
      }
    case ButtonClicked (NextButton) =>
      try{
        val edata = SockJson.requestNext();
        graphDocument.loadGraph (edata);
        graphDocument.reLayout();
        setEvalButtonStatus (false, true, true, true);
      }
      catch{
        case SockJsonError(_, SockJsonErrorType.ErrEval) =>
          setEvalButtonStatus (false, true, false, false);
          error ("Can't show the current step", "eval error")

        case SockJsonError(_, SockJsonErrorType.GoodEval) =>
          NextButton.enabled_=(false)
          error ("Congrats", "proof strategy language succeeds !")

        case _ =>
          error ("Can't eval the next step", "unknown error")
    }
  }
}