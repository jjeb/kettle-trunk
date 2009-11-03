/* Copyright (c) 2007 Pentaho Corporation.  All rights reserved. 
 * This software was developed by Pentaho Corporation and is provided under the terms 
 * of the GNU Lesser General Public License, Version 2.1. You may not use 
 * this file except in compliance with the license. If you need a copy of the license, 
 * please go to http://www.gnu.org/licenses/lgpl-2.1.txt. The Original Code is Pentaho 
 * Data Integration.  The Initial Developer is Pentaho Corporation.
 *
 * Software distributed under the GNU Lesser Public License is distributed on an "AS IS" 
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or  implied. Please refer to 
 * the license for the specific language governing your rights and limitations.*/

package org.pentaho.di.ui.spoon.trans;

import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Timer;
import java.util.TimerTask;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.window.DefaultToolTip;
import org.eclipse.jface.window.ToolTip;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.DropTargetListener;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.pentaho.di.core.CheckResultInterface;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.EngineMetaInterface;
import org.pentaho.di.core.NotePadMeta;
import org.pentaho.di.core.Props;
import org.pentaho.di.core.dnd.DragAndDropContainer;
import org.pentaho.di.core.dnd.XMLTransfer;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.gui.GUIPositionInterface;
import org.pentaho.di.core.gui.Point;
import org.pentaho.di.core.gui.Redrawable;
import org.pentaho.di.core.gui.SnapAllignDistribute;
import org.pentaho.di.core.gui.SpoonInterface;
import org.pentaho.di.core.logging.CentralLogStore;
import org.pentaho.di.core.logging.HasLogChannelInterface;
import org.pentaho.di.core.logging.Log4jKettleLayout;
import org.pentaho.di.core.logging.LogChannelInterface;
import org.pentaho.di.core.logging.LogParentProvidedInterface;
import org.pentaho.di.core.logging.LogWriter;
import org.pentaho.di.core.logging.LoggingObjectInterface;
import org.pentaho.di.core.logging.LoggingRegistry;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.xml.XMLHandler;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.i18n.LanguageChoice;
import org.pentaho.di.lineage.TransDataLineage;
import org.pentaho.di.repository.Repository;
import org.pentaho.di.repository.RepositoryLock;
import org.pentaho.di.repository.RepositoryObjectType;
import org.pentaho.di.shared.SharedObjects;
import org.pentaho.di.trans.DatabaseImpact;
import org.pentaho.di.trans.StepLoader;
import org.pentaho.di.trans.StepPlugin;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransExecutionConfiguration;
import org.pentaho.di.trans.TransHopMeta;
import org.pentaho.di.trans.TransListener;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.debug.BreakPointListener;
import org.pentaho.di.trans.debug.StepDebugMeta;
import org.pentaho.di.trans.debug.TransDebugMeta;
import org.pentaho.di.trans.step.RemoteStep;
import org.pentaho.di.trans.step.StepErrorMeta;
import org.pentaho.di.trans.step.StepIOMetaInterface;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.step.errorhandling.StreamInterface;
import org.pentaho.di.trans.step.errorhandling.StreamInterface.StreamType;
import org.pentaho.di.trans.steps.mapping.MappingMeta;
import org.pentaho.di.trans.steps.tableinput.TableInputMeta;
import org.pentaho.di.ui.core.ConstUI;
import org.pentaho.di.ui.core.PropsUI;
import org.pentaho.di.ui.core.dialog.EnterNumberDialog;
import org.pentaho.di.ui.core.dialog.EnterTextDialog;
import org.pentaho.di.ui.core.dialog.ErrorDialog;
import org.pentaho.di.ui.core.dialog.PreviewRowsDialog;
import org.pentaho.di.ui.core.dialog.StepFieldsDialog;
import org.pentaho.di.ui.core.gui.GUIResource;
import org.pentaho.di.ui.core.gui.XulHelper;
import org.pentaho.di.ui.core.widget.CheckBoxToolTip;
import org.pentaho.di.ui.core.widget.CheckBoxToolTipListener;
import org.pentaho.di.ui.repository.dialog.RepositoryExplorerDialog;
import org.pentaho.di.ui.repository.dialog.RepositoryRevisionBrowserDialogInterface;
import org.pentaho.di.ui.spoon.AreaOwner;
import org.pentaho.di.ui.spoon.Spoon;
import org.pentaho.di.ui.spoon.TabItemInterface;
import org.pentaho.di.ui.spoon.TransPainter;
import org.pentaho.di.ui.spoon.XulMessages;
import org.pentaho.di.ui.spoon.dialog.DeleteMessageBox;
import org.pentaho.di.ui.spoon.dialog.EnterPreviewRowsDialog;
import org.pentaho.di.ui.spoon.dialog.NotePadDialog;
import org.pentaho.di.ui.spoon.dialog.SearchFieldsProgressDialog;
import org.pentaho.di.ui.trans.dialog.TransDialog;
import org.pentaho.xul.menu.XulMenu;
import org.pentaho.xul.menu.XulMenuChoice;
import org.pentaho.xul.menu.XulPopupMenu;
import org.pentaho.xul.swt.menu.MenuChoice;
import org.pentaho.xul.toolbar.XulToolbar;
import org.pentaho.xul.toolbar.XulToolbarButton;

/**
 * This class handles the display of the transformations in a graphical way using icons, arrows, etc.
 * One transformation is handled per TransGraph
 * 
 * @author Matt
 * @since 17-mei-2003
 * 
 */
public class TransGraph extends Composite implements Redrawable, TabItemInterface, LogParentProvidedInterface, MouseListener, MouseMoveListener {
  private static Class<?> PKG = Spoon.class; // for i18n purposes, needed by Translator2!!   $NON-NLS-1$

  private LogChannelInterface log;

  private static final int HOP_SEL_MARGIN = 9;

  private static final String XUL_FILE_TRANS_TOOLBAR = "ui/trans-toolbar.xul";

  public static final String XUL_FILE_TRANS_TOOLBAR_PROPERTIES = "ui/trans-toolbar.properties";

  public final static String START_TEXT = BaseMessages.getString(PKG, "TransLog.Button.StartTransformation"); //$NON-NLS-1$

  public final static String PAUSE_TEXT = BaseMessages.getString(PKG, "TransLog.Button.PauseTransformation"); //$NON-NLS-1$

  public final static String RESUME_TEXT = BaseMessages.getString(PKG, "TransLog.Button.ResumeTransformation"); //$NON-NLS-1$

  public final static String STOP_TEXT = BaseMessages.getString(PKG, "TransLog.Button.StopTransformation"); //$NON-NLS-1$

  private TransMeta transMeta;

  public Trans trans;

  private Shell shell;

  private Composite mainComposite;

  private Canvas canvas;

  private DefaultToolTip toolTip;

  private CheckBoxToolTip helpTip;

  private XulToolbar toolbar;

  private int iconsize;

  private Point lastclick;

  private Point lastMove;

  private Point previous_step_locations[];

  private Point previous_note_locations[];

  private StepMeta selectedSteps[];

  private StepMeta selectedStep;
  
  private List<StepMeta> mouseOverSteps;

  private NotePadMeta selectedNotes[];

  private NotePadMeta selectedNote;

  private TransHopMeta candidate;
  
  private boolean showHopInputIcons;

  private Point drop_candidate;

  private Spoon spoon;

  private Point offset, iconoffset, noteoffset;

  private ScrollBar hori;

  private ScrollBar vert;

  // public boolean           shift, control;

  private boolean split_hop;

  private int last_button;

  private TransHopMeta last_hop_split;

  private Rectangle selectionRegion;

  /**
   * A list of remarks on the current Transformation...
   */
  private List<CheckResultInterface> remarks;

  /**
   * A list of impacts of the current transformation on the used databases.
   */
  private List<DatabaseImpact> impact;

  /**
   * Indicates whether or not an impact analysis has already run.
   */
  private boolean impactFinished;

  private TransHistoryRefresher spoonHistoryRefresher;

  private TransDebugMeta lastTransDebugMeta;

  protected Map<String, org.pentaho.xul.swt.menu.Menu> menuMap = new HashMap<String, org.pentaho.xul.swt.menu.Menu>();

  protected int currentMouseX = 0;

  protected int currentMouseY = 0;

  protected NotePadMeta ni = null;

  protected TransHopMeta currentHop;

  protected StepMeta currentStep;

  private List<AreaOwner> areaOwners;

  // private Text filenameLabel;
  private SashForm sashForm;

  public Composite extraViewComposite;

  public CTabFolder extraViewTabFolder;

  private boolean initialized;

  private boolean running;

  private boolean halted;

  private boolean halting;

  private boolean debug;

  private boolean pausing;

  private float magnification = 1.0f;

  public TransLogDelegate transLogDelegate;

  public TransGridDelegate transGridDelegate;

  public TransHistoryDelegate transHistoryDelegate;

  public TransPerfDelegate transPerfDelegate;

  private Combo zoomLabel;
  
  /** A map that keeps track of which log line was written by which step */
  private Map<StepMeta, String> stepLogMap;

  private StepMeta	startHopStep;
  private Point     endHopLocation;
  private boolean	startErrorHopStep;
  
  private StepMeta	noInputStep;

  private StepMeta	endHopStep;

  private StreamType candidateHopType;

  private StreamInterface	startTargetHopStream;
  
  

  public void setCurrentNote(NotePadMeta ni) {
    this.ni = ni;
  }

  public NotePadMeta getCurrentNote() {
    return ni;
  }

  public TransHopMeta getCurrentHop() {
    return currentHop;
  }

  public void setCurrentHop(TransHopMeta currentHop) {
    this.currentHop = currentHop;
  }

  public StepMeta getCurrentStep() {
    return currentStep;
  }

  public void setCurrentStep(StepMeta currentStep) {
    this.currentStep = currentStep;
  }

  public TransGraph(Composite parent, final Spoon spoon, final TransMeta transMeta) {
    super(parent, SWT.NONE);
    this.shell = parent.getShell();
    this.spoon = spoon;
    this.transMeta = transMeta;
    this.areaOwners = new ArrayList<AreaOwner>();
    this.log = spoon.getLog();

    this.mouseOverSteps = new ArrayList<StepMeta>();
    
    transLogDelegate = new TransLogDelegate(spoon, this);
    transGridDelegate = new TransGridDelegate(spoon, this);
    transHistoryDelegate = new TransHistoryDelegate(spoon, this);
    transPerfDelegate = new TransPerfDelegate(spoon, this);

    setLayout(new FormLayout());

    // Add a tool-bar at the top of the tab
    // The form-data is set on the native widget automatically
    //
    addToolBar();

    setControlStates(); // enable / disable the icons in the toolbar too.

    // The main composite contains the graph view, but if needed also 
    // a view with an extra tab containing log, etc.
    //
    mainComposite = new Composite(this, SWT.NONE);
    mainComposite.setLayout(new FillLayout());
    FormData fdMainComposite = new FormData();
    fdMainComposite.left = new FormAttachment(0, 0);
    fdMainComposite.top = new FormAttachment((Control) toolbar.getNativeObject(), 0);
    fdMainComposite.right = new FormAttachment(100, 0);
    fdMainComposite.bottom = new FormAttachment(100, 0);
    mainComposite.setLayoutData(fdMainComposite);

    // To allow for a splitter later on, we will add the splitter here...
    //
    sashForm = new SashForm(mainComposite, SWT.VERTICAL);
    
    // Add a canvas below it, use up all space initially
    //
    canvas = new Canvas(sashForm, SWT.V_SCROLL | SWT.H_SCROLL | SWT.NO_BACKGROUND | SWT.BORDER);

    sashForm.setWeights(new int[] { 100, });

    try {
      // first get the XML document
      menuMap = XulHelper.createPopupMenus(SpoonInterface.XUL_FILE_MENUS, shell, new XulMessages(), "trans-graph-hop",
          "trans-graph-entry", "trans-graph-background", "trans-graph-note");
    } catch (Throwable t) {
      // TODO log this
      t.printStackTrace();
    }

    toolTip = new DefaultToolTip(canvas, ToolTip.NO_RECREATE, true);
    toolTip.setRespectMonitorBounds(true);
    toolTip.setRespectDisplayBounds(true);
    toolTip.setPopupDelay(350);
    toolTip.setShift(new org.eclipse.swt.graphics.Point(ConstUI.TOOLTIP_OFFSET, ConstUI.TOOLTIP_OFFSET));

    helpTip = new CheckBoxToolTip(canvas);
    helpTip.addCheckBoxToolTipListener(new CheckBoxToolTipListener() {

      public void checkBoxSelected(boolean enabled) {
        spoon.props.setShowingHelpToolTips(enabled);
      }
    });

    iconsize = spoon.props.getIconSize();

    clearSettings();

    remarks = new ArrayList<CheckResultInterface>();
    impact = new ArrayList<DatabaseImpact>();
    impactFinished = false;

    hori = canvas.getHorizontalBar();
    vert = canvas.getVerticalBar();

    hori.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        redraw();
      }
    });
    vert.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        redraw();
      }
    });
    hori.setThumb(100);
    vert.setThumb(100);

    hori.setVisible(true);
    vert.setVisible(true);

    setVisible(true);
    newProps();

    canvas.setBackground(GUIResource.getInstance().getColorBackground());

    canvas.addPaintListener(new PaintListener() {
      public void paintControl(PaintEvent e) {
        if (!spoon.isStopped())
          TransGraph.this.paintControl(e);
      }
    });

    canvas.addMouseWheelListener(new MouseWheelListener() {

      public void mouseScrolled(MouseEvent e) {
    	boolean control = (e.stateMask & SWT.CONTROL) != 0;
    	if (control) {
	        if (e.count == 3) {
	          // scroll up
	          zoomIn();
	        } else if (e.count == -3) {
	          // scroll down 
	          zoomOut();
	        }
    	}
      }
    });
    
    selectedSteps = null;
    lastclick = null;

    /*
     * Handle the mouse...
     */

    canvas.addMouseListener(this);
    canvas.addMouseMoveListener(this);

    // Drag & Drop for steps
    Transfer[] ttypes = new Transfer[] { XMLTransfer.getInstance() };
    DropTarget ddTarget = new DropTarget(canvas, DND.DROP_MOVE);
    ddTarget.setTransfer(ttypes);
    ddTarget.addDropListener(new DropTargetListener() {
      public void dragEnter(DropTargetEvent event) {
        clearSettings();

        drop_candidate = PropsUI.calculateGridPosition(getRealPosition(canvas, event.x, event.y));

        redraw();
      }

      public void dragLeave(DropTargetEvent event) {
        drop_candidate = null;
        redraw();
      }

      public void dragOperationChanged(DropTargetEvent event) {
      }

      public void dragOver(DropTargetEvent event) {
        drop_candidate = PropsUI.calculateGridPosition(getRealPosition(canvas, event.x, event.y));

        redraw();
      }

      public void drop(DropTargetEvent event) {
        // no data to copy, indicate failure in event.detail
        if (event.data == null) {
          event.detail = DND.DROP_NONE;
          return;
        }

        // System.out.println("Dropping a step!!");

        // What's the real drop position?
        Point p = getRealPosition(canvas, event.x, event.y);

        // 
        // We expect a Drag and Drop container... (encased in XML)
        try {
          DragAndDropContainer container = (DragAndDropContainer) event.data;

          StepMeta stepMeta = null;
          boolean newstep = false;

          switch (container.getType()) {
            // Put an existing one on the canvas.
            case DragAndDropContainer.TYPE_STEP: {
              // Drop hidden step onto canvas....		                
              stepMeta = transMeta.findStep(container.getData());
              if (stepMeta != null) {
                if (stepMeta.isDrawn() || transMeta.isStepUsedInTransHops(stepMeta)) {
                  MessageBox mb = new MessageBox(shell, SWT.OK);
                  mb.setMessage(BaseMessages.getString(PKG, "TransGraph.Dialog.StepIsAlreadyOnCanvas.Message")); //$NON-NLS-1$
                  mb.setText(BaseMessages.getString(PKG, "TransGraph.Dialog.StepIsAlreadyOnCanvas.Title")); //$NON-NLS-1$
                  mb.open();
                  return;
                }
                // This step gets the drawn attribute and position set below.
              } else {
                // Unknown step dropped: ignore this to be safe!
                return;
              }
            }
              break;

            // Create a new step 
            case DragAndDropContainer.TYPE_BASE_STEP_TYPE: {
              // Not an existing step: data refers to the type of step to create
              String steptype = container.getData();
              stepMeta = spoon.newStep(transMeta, steptype, steptype, false, true);
              if (stepMeta != null) {
                newstep = true;
              } else {
                return; // Cancelled pressed in dialog or unable to create step.
              }
            }
              break;

            // Create a new TableInput step using the selected connection...
            case DragAndDropContainer.TYPE_DATABASE_CONNECTION: {
              newstep = true;
              String connectionName = container.getData();
              TableInputMeta tii = new TableInputMeta();
              tii.setDatabaseMeta(transMeta.findDatabase(connectionName));

              StepLoader steploader = StepLoader.getInstance();
              String stepID = steploader.getStepPluginID(tii);
              StepPlugin stepPlugin = steploader.findStepPluginWithID(stepID);
              String stepName = transMeta.getAlternativeStepname(stepPlugin.getDescription());
              stepMeta = new StepMeta(stepID, stepName, tii);
              if (spoon.editStep(transMeta, stepMeta) != null) {
                transMeta.addStep(stepMeta);
                spoon.refreshTree();
                spoon.refreshGraph();
              } else {
                return;
              }
            }
              break;

            // Drag hop on the canvas: create a new Hop...
            case DragAndDropContainer.TYPE_TRANS_HOP: {
              newHop();
              return;
            }

            default: {
              // Nothing we can use: give an error!
              MessageBox mb = new MessageBox(shell, SWT.OK);
              mb.setMessage(BaseMessages.getString(PKG, "TransGraph.Dialog.ItemCanNotBePlacedOnCanvas.Message")); //$NON-NLS-1$
              mb.setText(BaseMessages.getString(PKG, "TransGraph.Dialog.ItemCanNotBePlacedOnCanvas.Title")); //$NON-NLS-1$
              mb.open();
              return;
            }
          }

          transMeta.unselectAll();

          StepMeta before = null;
          if (!newstep) { 
        	  before= (StepMeta) stepMeta.clone();
          }

          stepMeta.drawStep();
          stepMeta.setSelected(true);
          PropsUI.setLocation(stepMeta, p.x, p.y);

          if (newstep) {
            spoon.addUndoNew(transMeta, new StepMeta[] { stepMeta }, new int[] { transMeta.indexOfStep(stepMeta) });
          } else {
            spoon.addUndoChange(transMeta, new StepMeta[] { before }, new StepMeta[] { (StepMeta) stepMeta.clone() }, new int[] { transMeta.indexOfStep(stepMeta) });
          }

          canvas.forceFocus();
          redraw();

          // See if we want to draw a tool tip explaining how to create new hops...
          //
          if (newstep && transMeta.nrSteps() > 1 && transMeta.nrSteps() < 5 && spoon.props.isShowingHelpToolTips()) {
            showHelpTip(p.x, p.y, BaseMessages.getString(PKG, "TransGraph.HelpToolTip.CreatingHops.Title"), 
            		BaseMessages.getString(PKG, "TransGraph.HelpToolTip.CreatingHops.Message"));
          }
        } catch (Exception e) {
          new ErrorDialog(shell, BaseMessages.getString(PKG, "TransGraph.Dialog.ErrorDroppingObject.Message"), 
        		  BaseMessages.getString(PKG, "TransGraph.Dialog.ErrorDroppingObject.Title"), e);
        }
      }

      public void dropAccept(DropTargetEvent event) {
      }
    });

    // Keyboard shortcuts...
    addKeyListener(canvas);
    addKeyListener(this);
    // addKeyListener(filenameLabel);

    canvas.addKeyListener(spoon.defKeys);
    // filenameLabel.addKeyListener(spoon.defKeys);

    setBackground(GUIResource.getInstance().getColorBackground());
    
    // Add a timer to set correct the state of the run/stop buttons every 2 seconds...
    //
    final Timer timer = new Timer();
    TimerTask timerTask = new TimerTask() {
		
			public void run() {
				// setControlStates();
			}
		};
	timer.schedule(timerTask, 2000, 1000);
	
	// Make sure the timer stops when we close the tab...
	//
	addDisposeListener(new DisposeListener() {
		public void widgetDisposed(DisposeEvent arg0) {
			timer.cancel();
		}
	});
  }
  
  public void mouseDoubleClick(MouseEvent e) {
      clearSettings();

      Point real = screen2real(e.x, e.y);

      // Hide the tooltip!
      hideToolTips();

      StepMeta stepMeta = transMeta.getStep(real.x, real.y, iconsize);
      if (stepMeta != null) {
        if (e.button == 1)
          editStep(stepMeta);
        else
          editDescription(stepMeta);
      } else {
        // Check if point lies on one of the many hop-lines...
        TransHopMeta online = findHop(real.x, real.y);
        if (online != null) {
          editHop(online);
        } else {
          NotePadMeta ni = transMeta.getNote(real.x, real.y);
          if (ni != null) {
            selectedNote = null;
            editNote(ni);
          } else {
            // See if the double click was in one of the area's...
            //
            for (AreaOwner areaOwner : areaOwners) {
              if (areaOwner.contains(real.x, real.y)) {
                if (areaOwner.getParent() instanceof StepMeta
                    && areaOwner.getOwner().equals(TransPainter.STRING_PARTITIONING_CURRENT_STEP)) {
                  StepMeta step = (StepMeta) areaOwner.getParent();
                  spoon.editPartitioning(transMeta, step);
                  break;
                }
              }
            }

          }
        }
      }
    }

    public void mouseDown(MouseEvent e) {

      boolean alt = (e.stateMask & SWT.ALT) != 0;
      boolean control = (e.stateMask & SWT.CONTROL) != 0;
      boolean shift = (e.stateMask & SWT.SHIFT) != 0;

      last_button = e.button;
      Point real = screen2real(e.x, e.y);
      lastclick = new Point(real.x, real.y);

      // Hide the tooltip!
      hideToolTips();

      // Set the pop-up menu
      if (e.button == 3) {
        setMenu(real.x, real.y);
        return;
      }
      
      // A single left click on one of the area owners...
      //
      if (e.button==1) {
    	  AreaOwner areaOwner = getVisibleAreaOwner(real.x, real.y);
    	  if (areaOwner!=null) {
    		  switch(areaOwner.getAreaType()) {
    		  case STEP_OUTPUT_HOP_ICON:
 		    	 // Click on the output icon means: start of drag
 	    		 // Action: We show the input icons on the other steps...
 	    		 //
 	    		 showHopInputIcons = true;
     			 selectedStep = null;
 	    		 startHopStep = (StepMeta)areaOwner.getParent();
 	    		 candidateHopType = null;
 	    		 startErrorHopStep = false;
 	    		 startTargetHopStream= null;
 	    		 break;
    		  case STEP_INFO_HOP_ICON:
    			  // See if we have a start hop step set...
    			  // If so, we can create a new info hop...
    			  //
    			  if (startHopStep!=null) {
    				  StepMeta stepMeta = (StepMeta)areaOwner.getParent();
    				  StreamInterface stream = (StreamInterface)areaOwner.getOwner();
    				  candidate = new TransHopMeta(startHopStep, stepMeta);
    				  stream.setStepMeta(startHopStep);
    			  }
    		      startHopStep=null;
    		      break;
    		  case STEP_INPUT_HOP_ICON:
  		    	 // Click on the input icon means: create a new hop
  	    		 //
    			  if (startHopStep!=null) {
    				  StepMeta stepMeta = (StepMeta)areaOwner.getParent();
    				  candidate = new TransHopMeta(startHopStep, stepMeta);
    			  }
  	    		 break;
    		  case STEP_ERROR_HOP_ICON:
   		    	 // Click on the error icon means: create a new error hop 
   	    		 //
     			  if (startHopStep==null) {
     				 startHopStep = (StepMeta)areaOwner.getParent();
     				  startErrorHopStep = true;
     			  }
   	    		 break;
    		  case STEP_TARGET_HOP_ICON:
    		      // Click on the target icon means: create a new target hop 
    	    	  //
      			  if (startHopStep==null) {
      				 startHopStep = (StepMeta)areaOwner.getParent();
      				 startTargetHopStream = (StreamInterface)areaOwner.getOwner();
      			  }
    	    		 break;
    		  default:
    		      startHopStep=null;
    		  }
    	  }
      }
      
      if (candidate!=null) {
    	  addCandidateAsHop();
      }
      
      // Did we click on a step?
      StepMeta stepMeta = transMeta.getStep(real.x, real.y, iconsize);
      if (stepMeta != null) {
        // ALT-Click: edit error handling
        if (e.button == 1 && alt && stepMeta.supportsErrorHandling()) {
          spoon.editStepErrorHandling(transMeta, stepMeta);
          return;
        } 
        // SHIFT CLICK is start of drag to create a new hop
        //
        else if (e.button== 2 || (e.button==1 && shift)) {
        	startHopStep = stepMeta;
        } else {
	        selectedSteps = transMeta.getSelectedSteps();
	        selectedStep = stepMeta;
	        // 
	        // When an icon is moved that is not selected, it gets
	        // selected too late.
	        // It is not captured here, but in the mouseMoveListener...
	        previous_step_locations = transMeta.getSelectedStepLocations();
	
	        Point p = stepMeta.getLocation();
	        iconoffset = new Point(real.x - p.x, real.y - p.y);
        }
      } else {
        // Did we hit a note?
        NotePadMeta ni = transMeta.getNote(real.x, real.y);
        if (ni != null && last_button == 1) {
          selectedNotes = transMeta.getSelectedNotes();
          selectedNote = ni;
          Point loc = ni.getLocation();

          previous_note_locations = transMeta.getSelectedNoteLocations();

          noteoffset = new Point(real.x - loc.x, real.y - loc.y);
        } else {
          if (!control)
            selectionRegion = new Rectangle(real.x, real.y, 0, 0);
        }
      }
      redraw();
    }

	public void mouseUp(MouseEvent e) {
      boolean control = (e.stateMask & SWT.CONTROL) != 0;

      if (iconoffset == null)
        iconoffset = new Point(0, 0);
      Point real = screen2real(e.x, e.y);
      Point icon = new Point(real.x - iconoffset.x, real.y - iconoffset.y);

      // Quick new hop option? (drag from one step to another)
      //
      if (candidate != null) {
    	addCandidateAsHop();
    	redraw();
      }
      // Did we select a region on the screen? Mark steps in region as
      // selected
      //
      else {
        if (selectionRegion != null) {
          selectionRegion.width = real.x - selectionRegion.x;
          selectionRegion.height = real.y - selectionRegion.y;

          transMeta.unselectAll();
          selectInRect(transMeta, selectionRegion);
          selectionRegion = null;
          redraw();
        }
        // Clicked on an icon?
        //
        else {
          if (selectedStep != null && startHopStep==null) {
            if (e.button == 1) {
              Point realclick = screen2real(e.x, e.y);
              if (lastclick.x == realclick.x && lastclick.y == realclick.y) {
                // Flip selection when control is pressed!
                if (control) {
                  selectedStep.flipSelected();
                } else {
                  // Otherwise, select only the icon clicked on!
                  transMeta.unselectAll();
                  selectedStep.setSelected(true);
                }
              } else {
                // Find out which Steps & Notes are selected
                selectedSteps = transMeta.getSelectedSteps();
                selectedNotes = transMeta.getSelectedNotes();

                // We moved around some items: store undo info...
                // 
                boolean also = false;
                if (selectedNotes != null && previous_note_locations != null) {
                  int indexes[] = transMeta.getNoteIndexes(selectedNotes);
                  addUndoPosition(selectedNotes, indexes, previous_note_locations, transMeta
                      .getSelectedNoteLocations(), also);
                  also = selectedSteps != null && selectedSteps.length > 0;
                }
                if (selectedSteps != null && previous_step_locations != null) {
                  int indexes[] = transMeta.getStepIndexes(selectedSteps);
                  addUndoPosition(selectedSteps, indexes, previous_step_locations, transMeta.getSelectedStepLocations(), also);
                }
              }
            }

            // OK, we moved the step, did we move it across a hop?
            // If so, ask to split the hop!
            if (split_hop) {
              TransHopMeta hi = findHop(icon.x + iconsize / 2, icon.y + iconsize / 2, selectedStep);
              if (hi != null) {
                int id = 0;
                if (!spoon.props.getAutoSplit()) {
                  MessageDialogWithToggle md = new MessageDialogWithToggle(
                      shell,
                      BaseMessages.getString(PKG, "TransGraph.Dialog.SplitHop.Title"), null, //$NON-NLS-1$
                      BaseMessages.getString(PKG, "TransGraph.Dialog.SplitHop.Message") + Const.CR + hi.toString(), MessageDialog.QUESTION, new String[] { //$NON-NLS-1$
                      BaseMessages.getString(PKG, "System.Button.Yes"), BaseMessages.getString(PKG, "System.Button.No") }, 0, BaseMessages.getString(PKG, "TransGraph.Dialog.Option.SplitHop.DoNotAskAgain"), spoon.props.getAutoSplit()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                  MessageDialogWithToggle.setDefaultImage(GUIResource.getInstance().getImageSpoon());
                  id = md.open();
                  spoon.props.setAutoSplit(md.getToggleState());
                }

                if ((id & 0xFF) == 0) // Means: "Yes" button clicked!
                {
                  // Only split A-->--B by putting C in between IF...
                  // C-->--A or B-->--C don't exists...
                  // A ==> hi.getFromStep()
                  // B ==> hi.getToStep();
                  // C ==> selected_step
                  //
                  if (transMeta.findTransHop(selectedStep, hi.getFromStep()) == null && transMeta.findTransHop(hi.getToStep(), selectedStep) == null) {
                	  
                    TransHopMeta newhop1 = new TransHopMeta(hi.getFromStep(), selectedStep);
                    transMeta.addTransHop(newhop1);
                    spoon.addUndoNew(transMeta, new TransHopMeta[] { newhop1 }, new int[] { transMeta.indexOfTransHop(newhop1) }, true);
                    TransHopMeta newhop2 = new TransHopMeta(selectedStep, hi.getToStep());
                    transMeta.addTransHop(newhop2);
                    spoon.addUndoNew(transMeta, new TransHopMeta[] { newhop2 }, new int[] { transMeta.indexOfTransHop(newhop2) }, true);
                    int idx = transMeta.indexOfTransHop(hi);
                    spoon.addUndoDelete(transMeta, new TransHopMeta[] { hi }, new int[] { idx }, true);
                    transMeta.removeTransHop(idx);
                    spoon.refreshTree();
                    
                  } else {
                    // Silently discard this hop-split attempt. 
                  }
                }
              }
              split_hop = false;
            }

            selectedSteps = null;
            selectedNotes = null;
            selectedStep = null;
            selectedNote = null;
            startHopStep = null;
            endHopLocation = null;
            redraw();
          }

          // Notes?
          else {
            if (selectedNote != null) {
              if (e.button == 1) {
                if (lastclick.x == e.x && lastclick.y == e.y) {
                  // Flip selection when control is pressed!
                  if (control) {
                    selectedNote.flipSelected();
                  } else {
                    // Otherwise, select only the note clicked on!
                    transMeta.unselectAll();
                    selectedNote.setSelected(true);
                  }
                } else {
                  // Find out which Steps & Notes are selected
                  selectedSteps = transMeta.getSelectedSteps();
                  selectedNotes = transMeta.getSelectedNotes();

                  // We moved around some items: store undo info...
                  boolean also = false;
                  if (selectedNotes != null && previous_note_locations != null) {
                    int indexes[] = transMeta.getNoteIndexes(selectedNotes);
                    addUndoPosition(selectedNotes, indexes, previous_note_locations, transMeta.getSelectedNoteLocations(), also);
                    also = selectedSteps != null && selectedSteps.length > 0;
                  }
                  if (selectedSteps != null && previous_step_locations != null) {
                    int indexes[] = transMeta.getStepIndexes(selectedSteps);
                    addUndoPosition(selectedSteps, indexes, previous_step_locations, transMeta.getSelectedStepLocations(), also);
                  }
                }
              }

              selectedNotes = null;
              selectedSteps = null;
              selectedStep = null;
              selectedNote = null;
              startHopStep = null;
              endHopLocation = null;
            }
          }
        }
      }

      last_button = 0;
    }
  
    private void addCandidateAsHop() {
      if (transMeta.findTransHop(candidate) == null) {
        spoon.newHop(transMeta, candidate);
      }
	  if (startErrorHopStep) {
		  // Automatically configure the step error handling too!
		  //
		  StepErrorMeta errorMeta = candidate.getFromStep().getStepErrorMeta();
		  if (errorMeta==null) {
			  errorMeta=new StepErrorMeta(transMeta, candidate.getFromStep());
		  }
		  errorMeta.setEnabled(true);
		  errorMeta.setTargetStep(candidate.getToStep());
		  candidate.getFromStep().setStepErrorMeta(errorMeta);
		  startErrorHopStep=false;
	  }
	  if (startTargetHopStream!=null) {
		  // Auto-configure the target in the source step...
		  //
		  startTargetHopStream.setStepMeta(candidate.getToStep());
		  startTargetHopStream.setStepname(candidate.getToStep().getName());
		  startTargetHopStream=null;
	  }
	  
      candidate = null;
      selectedSteps = null;
      startHopStep = null;
      endHopLocation = null;
      // redraw();
	}

	public void mouseMove(MouseEvent e) {
        boolean shift = (e.stateMask & SWT.SHIFT) != 0;
        noInputStep = null;

        // disable the tooltip
        //
        toolTip.hide();

        // Remember the last position of the mouse for paste with keyboard
        //
        lastMove = new Point(e.x, e.y);
        Point real = screen2real(e.x, e.y);

        if (iconoffset == null) {
          iconoffset = new Point(0, 0);
        }
        Point icon = new Point(real.x - iconoffset.x, real.y - iconoffset.y);

        if (noteoffset == null) {
          noteoffset = new Point(0, 0);
        }
        Point note = new Point(real.x - noteoffset.x, real.y - noteoffset.y);

        // Show a tool tip upon mouse-over of an object on the canvas
        //
        if (!helpTip.isVisible()) {
          setToolTip(real.x, real.y, e.x, e.y);
        }
        
        // Moved over an area?
        //
        endHopStep=null;
        AreaOwner areaOwner = getVisibleAreaOwner(real.x, real.y);
        if (areaOwner!=null) {
        	endHopStep = null;
        	switch (areaOwner.getAreaType()) {
        	case STEP_ICON :
        		final StepMeta stepMeta = (StepMeta) areaOwner.getOwner(); 
        		if (!mouseOverSteps.contains(stepMeta)) {
        			addStepMouseOverDelayTimer(stepMeta);
        			redraw();
        		}
    			break;
        	case STEP_INFO_HOP_ICON :
        		if (startHopStep!=null) {
					candidateHopType = StreamType.INFO;
					endHopStep = (StepMeta)areaOwner.getParent();
					redraw();
        		}
        		break;
        	case STEP_INPUT_HOP_ICON :
        		if (startHopStep!=null) {
        			 endHopStep = (StepMeta)areaOwner.getParent();
        			 redraw();
        			 candidateHopType = null;
        		}
        		break;
        	case STEP_OUTPUT_HOP_ICON :
        		break;
            default:
            	break;
        	}
        }

        // 
        // First see if the icon we clicked on was selected.
        // If the icon was not selected, we should un-select all other
        // icons, selected and move only the one icon
        //
        if (selectedStep != null && !selectedStep.isSelected()) {
          transMeta.unselectAll();
          selectedStep.setSelected(true);
          selectedSteps = new StepMeta[] { selectedStep };
          previous_step_locations = new Point[] { selectedStep.getLocation() };
          redraw();
        } 
        else if (selectedNote != null && !selectedNote.isSelected()) {
          transMeta.unselectAll();
          selectedNote.setSelected(true);
          selectedNotes = new NotePadMeta[] { selectedNote };
          previous_note_locations = new Point[] { selectedNote.getLocation() };
          redraw();
        }
        
        // Did we select a region...?
        //
        else if (selectionRegion != null && startHopStep==null) {
          selectionRegion.width = real.x - selectionRegion.x;
          selectionRegion.height = real.y - selectionRegion.y;
          redraw();
        }
        // Move around steps & notes
        //
        else if (selectedStep != null && last_button == 1 && !shift && startHopStep==null) {
            /*
             * One or more icons are selected and moved around...
             * 
             * new : new position of the ICON (not the mouse pointer) dx : difference with previous
             * position
             */
            int dx = icon.x - selectedStep.getLocation().x;
            int dy = icon.y - selectedStep.getLocation().y;

            // See if we have a hop-split candidate
            //
            TransHopMeta hi = findHop(icon.x + iconsize / 2, icon.y + iconsize / 2, selectedStep);
            if (hi != null) {
              // OK, we want to split the hop in 2
              // 
              if (!hi.getFromStep().equals(selectedStep) && !hi.getToStep().equals(selectedStep)) {
                split_hop = true;
                last_hop_split = hi;
                hi.split = true;
              }
            } else {
              if (last_hop_split != null) {
                last_hop_split.split = false;
                last_hop_split = null;
                split_hop = false;
              }
            }

            selectedNotes = transMeta.getSelectedNotes();
            selectedSteps = transMeta.getSelectedSteps();

            // Adjust location of selected steps...
            if (selectedSteps != null) {
              for (int i = 0; i < selectedSteps.length; i++) {
                StepMeta stepMeta = selectedSteps[i];
                PropsUI.setLocation(stepMeta, stepMeta.getLocation().x + dx, stepMeta.getLocation().y + dy);
              }
            }
            // Adjust location of selected hops...
            if (selectedNotes != null) {
              for (int i = 0; i < selectedNotes.length; i++) {
                NotePadMeta ni = selectedNotes[i];
                PropsUI.setLocation(ni, ni.getLocation().x + dx, ni.getLocation().y + dy);
              }
            }

            redraw();
        }
    	
        // Are we creating a new hop with the middle button or pressing SHIFT?
        //
        else if (startHopStep!=null) {
        	StepMeta stepMeta = transMeta.getStep(real.x, real.y, iconsize);
            endHopLocation = new Point(real.x, real.y);
            if (stepMeta != null && !startHopStep.equals(stepMeta)) {
            	StepIOMetaInterface ioMeta = stepMeta.getStepMetaInterface().getStepIOMeta();
            	if (candidate == null) {
            		// See if the step accepts input.  If not, we can't create a new hop...
            		//
            		if (ioMeta.isInputAcceptor()) {
		                candidate = new TransHopMeta(startHopStep, stepMeta);
		                endHopLocation=null;
            		} else {
            			noInputStep=stepMeta;
            			toolTip.setImage(null);
            			toolTip.setText("This step does not accept any input from other steps");
            			toolTip.show(new org.eclipse.swt.graphics.Point(real.x, real.y));
            		}
                }
            } else {
              if (candidate != null) {
                candidate = null;
                redraw();
              } else {
            	  if (!showHopInputIcons) {
            		  showHopInputIcons=true;
            	  }
              }
          }
            
      	  redraw();
        } else {
        	  showHopInputIcons = false;
        	  redraw();
        }
        // Move around notes & steps
        //
        if (selectedNote != null) {
          if (last_button == 1 && !shift) {
            /*
             * One or more notes are selected and moved around...
             * 
             * new : new position of the note (not the mouse pointer) dx : difference with previous
             * position
             */
            int dx = note.x - selectedNote.getLocation().x;
            int dy = note.y - selectedNote.getLocation().y;

            selectedNotes = transMeta.getSelectedNotes();
            selectedSteps = transMeta.getSelectedSteps();

            // Adjust location of selected steps...
            if (selectedSteps != null)
              for (int i = 0; i < selectedSteps.length; i++) {
                StepMeta stepMeta = selectedSteps[i];
                PropsUI.setLocation(stepMeta, stepMeta.getLocation().x + dx, stepMeta.getLocation().y + dy);
              }
            // Adjust location of selected hops...
            if (selectedNotes != null)
              for (int i = 0; i < selectedNotes.length; i++) {
                NotePadMeta ni = selectedNotes[i];
                PropsUI.setLocation(ni, ni.getLocation().x + dx, ni.getLocation().y + dy);
              }

            redraw();
          }
        }
      }
    
  private void addStepMouseOverDelayTimer(final StepMeta stepMeta) {
	  mouseOverSteps.add(stepMeta);
		new Thread(new DelayTimer(3, new DelayListener() {
			public void expired() {
				mouseOverSteps.remove(stepMeta);
				asyncRedraw();
			}
		})).start();
	}

	protected void asyncRedraw() {
		spoon.getDisplay().asyncExec(new Runnable() {
			public void run() {
				if (!TransGraph.this.isDisposed()) {
					TransGraph.this.redraw();
				}
			}
		});
	}

private void addToolBar() {

    try {
      toolbar = XulHelper.createToolbar(XUL_FILE_TRANS_TOOLBAR, TransGraph.this, TransGraph.this, new XulMessages());
      ToolBar toolBar = (ToolBar) toolbar.getNativeObject();
      toolBar.pack();
      
      // Hack alert : more XUL limitations...
      //
      ToolItem sep = new ToolItem(toolBar, SWT.SEPARATOR);

      zoomLabel = new Combo(toolBar, SWT.DROP_DOWN);
      zoomLabel.setItems(TransPainter.magnificationDescriptions);
      zoomLabel.addSelectionListener(new SelectionAdapter() {
        public void widgetSelected(SelectionEvent arg0) {
          readMagnification();
        }
      });

      zoomLabel.addKeyListener(new KeyAdapter() {
        public void keyPressed(KeyEvent event) {
          if (event.character == SWT.CR) {
            readMagnification();
          }
        }
      });

      setZoomLabel();
      zoomLabel.pack();

      sep.setWidth(80);
      sep.setControl(zoomLabel);
      toolBar.pack();

      // Add a few default key listeners
      //
      toolBar.addKeyListener(spoon.defKeys);

      addToolBarListeners();
      toolBar.layout(true, true);
    } catch (Throwable t) {
      log.logError(Const.getStackTracker(t));
      new ErrorDialog(shell, BaseMessages.getString(PKG, "Spoon.Exception.ErrorReadingXULFile.Title"), 
    		  BaseMessages.getString(PKG, "Spoon.Exception.ErrorReadingXULFile.Message", XUL_FILE_TRANS_TOOLBAR), new Exception(t));
    }
  }

  private void setZoomLabel() {
    zoomLabel.setText(Integer.toString(Math.round(magnification * 100)) + "%");
  }
  /**
   * Allows for magnifying to any percentage entered by the user...
   */
  private void readMagnification(){
    String possibleText = zoomLabel.getText();
    possibleText = possibleText.replace("%", "");

    float possibleFloatMagnification;
    try {
      possibleFloatMagnification = Float.parseFloat(possibleText) / 100;
      magnification = possibleFloatMagnification;
      if (zoomLabel.getText().indexOf('%') < 0) {
        zoomLabel.setText(zoomLabel.getText().concat("%"));
      }
    } catch (Exception e) {
      MessageBox mb = new MessageBox(shell, SWT.YES | SWT.ICON_ERROR);
      mb.setMessage(BaseMessages.getString(PKG, "TransGraph.Dialog.InvalidZoomMeasurement.Message", zoomLabel.getText())); //$NON-NLS-1$
      mb.setText(BaseMessages.getString(PKG, "TransGraph.Dialog.InvalidZoomMeasurement.Title")); //$NON-NLS-1$
      mb.open();
    }
    redraw();
  }

  public void addToolBarListeners() {
    try {
      // first get the XML document
      URL url = XulHelper.getAndValidate(XUL_FILE_TRANS_TOOLBAR_PROPERTIES);
      Properties props = new Properties();
      props.load(url.openStream());
      String ids[] = toolbar.getMenuItemIds();
      for (int i = 0; i < ids.length; i++) {
        String methodName = (String) props.get(ids[i]);
        if (methodName != null) {
          toolbar.addMenuListener(ids[i], this, methodName);

        }
      }

    } catch (Throwable t) {
      t.printStackTrace();
      new ErrorDialog(shell, BaseMessages.getString(PKG, "Spoon.Exception.ErrorReadingXULFile.Title"), 
    		  BaseMessages.getString(PKG, "Spoon.Exception.ErrorReadingXULFile.Message", XUL_FILE_TRANS_TOOLBAR_PROPERTIES), new Exception(t));
    }
  }

  protected void hideToolTips() {
    toolTip.hide();
    helpTip.hide();
  }

  private void showHelpTip(int x, int y, String tipTitle, String tipMessage) {

    helpTip.setTitle(tipTitle);
    helpTip.setMessage(tipMessage.replaceAll("\n", Const.CR));
    helpTip.setCheckBoxMessage(BaseMessages.getString(PKG, "TransGraph.HelpToolTip.DoNotShowAnyMoreCheckBox.Message"));

    // helpTip.hide();
    // int iconSize = spoon.props.getIconSize();
    org.eclipse.swt.graphics.Point location = new org.eclipse.swt.graphics.Point(x - 5, y - 5);

    helpTip.show(location);
  }

  /**
     * Select all the steps in a certain (screen) rectangle
     *
     * @param rect The selection area as a rectangle
     */
  public void selectInRect(TransMeta transMeta, Rectangle rect) {
    if (rect.height < 0 || rect.width < 0) {
      Rectangle rectified = new Rectangle(rect.x, rect.y, rect.width, rect.height);

      // Only for people not dragging from left top to right bottom
      if (rectified.height < 0) {
        rectified.y = rectified.y + rectified.height;
        rectified.height = -rectified.height;
      }
      if (rectified.width < 0) {
        rectified.x = rectified.x + rectified.width;
        rectified.width = -rectified.width;
      }
      rect = rectified;
    }

    for (int i = 0; i < transMeta.nrSteps(); i++) {
      StepMeta stepMeta = transMeta.getStep(i);
      Point a = stepMeta.getLocation();
      if (rect.contains(a.x, a.y))
        stepMeta.setSelected(true);
    }

    for (int i = 0; i < transMeta.nrNotes(); i++) {
      NotePadMeta ni = transMeta.getNote(i);
      Point a = ni.getLocation();
      Point b = new Point(a.x + ni.width, a.y + ni.height);
      if (rect.contains(a.x, a.y) && rect.contains(b.x, b.y))
        ni.setSelected(true);
    }
  }

  private void addKeyListener(Control control) {
    KeyAdapter keyAdapter = new KeyAdapter() {
      public void keyPressed(KeyEvent e) {
        // F2 --> rename step
        if (e.keyCode == SWT.F2) {
          renameStep();
        }

        if (e.keyCode == SWT.ESC) {
            clearSettings();
            redraw();
          }

        if (e.keyCode == SWT.DEL) {
          StepMeta stepMeta[] = transMeta.getSelectedSteps();
          if (stepMeta != null && stepMeta.length > 0) {
            delSelected(null);
          }
        }

        // CTRL-UP : allignTop();
        if (e.keyCode == SWT.ARROW_UP && (e.stateMask & SWT.CONTROL) != 0) {
          alligntop();
        }
        // CTRL-DOWN : allignBottom();
        if (e.keyCode == SWT.ARROW_DOWN && (e.stateMask & SWT.CONTROL) != 0) {
          allignbottom();
        }
        // CTRL-LEFT : allignleft();
        if (e.keyCode == SWT.ARROW_LEFT && (e.stateMask & SWT.CONTROL) != 0) {
          allignleft();
        }
        // CTRL-RIGHT : allignRight();
        if (e.keyCode == SWT.ARROW_RIGHT && (e.stateMask & SWT.CONTROL) != 0) {
          allignright();
        }
        // ALT-RIGHT : distributeHorizontal();
        if (e.keyCode == SWT.ARROW_RIGHT && (e.stateMask & SWT.ALT) != 0) {
          distributehorizontal();
        }
        // ALT-UP : distributeVertical();
        if (e.keyCode == SWT.ARROW_UP && (e.stateMask & SWT.ALT) != 0) {
          distributevertical();
        }
        // ALT-HOME : snap to grid
        if (e.keyCode == SWT.HOME && (e.stateMask & SWT.ALT) != 0) {
        snaptogrid(ConstUI.GRID_SIZE);
        }
        
        if (e.character == 'E' && (e.stateMask & SWT.CTRL) != 0) {
        	checkErrorVisuals();
        }

        // SPACE : over a step: show output fields...
        if (e.character == ' ' && lastMove != null) {
          
          // TODO: debugging code, remove later on!
          //
          dumpLoggingRegistry();
          
          Point real = screen2real(lastMove.x, lastMove.y);

          // Hide the tooltip!
          hideToolTips();

          // Set the pop-up menu
          StepMeta stepMeta = transMeta.getStep(real.x, real.y, iconsize);
          if (stepMeta != null) {
            // OK, we found a step, show the output fields...
            inputOutputFields(stepMeta, false);
          }
        }
      }
    };
    control.addKeyListener(keyAdapter);
  }

  public void redraw() {
    canvas.redraw();
    setZoomLabel();
  }

  public boolean forceFocus() {
    return canvas.forceFocus();
  }

  public boolean setFocus() {
    return canvas.setFocus();
  }

  public void renameStep() {
    StepMeta[] selection = transMeta.getSelectedSteps();
    if (selection != null && selection.length == 1) {
      final StepMeta stepMeta = selection[0];

      // What is the location of the step?
      String name = stepMeta.getName();
      Point stepLocation = stepMeta.getLocation();
      Point realStepLocation = real2screen(stepLocation.x, stepLocation.y);

      // The location of the step name?
      GC gc = new GC(shell.getDisplay());
      gc.setFont(GUIResource.getInstance().getFontGraph());
      Point namePosition = TransPainter.getNamePosition(gc, name, realStepLocation, iconsize);
      int width = gc.textExtent(name).x + 30;
      gc.dispose();

      // at this very point, create a new text widget...
      final Text text = new Text(this, SWT.SINGLE | SWT.BORDER);
      text.setText(name);
      FormData fdText = new FormData();
      fdText.left = new FormAttachment(0, namePosition.x);
      fdText.right = new FormAttachment(0, namePosition.x + width);
      fdText.top = new FormAttachment(0, namePosition.y);
      text.setLayoutData(fdText);

      // Add a listener!
      // Catch the keys pressed when editing a Text-field...
      KeyListener lsKeyText = new KeyAdapter() {
        public void keyPressed(KeyEvent e) {
          // "ENTER": close the text editor and copy the data over 
          if (e.character == SWT.CR) {
            String newName = text.getText();
            text.dispose();
            renameStep(stepMeta, newName);
          }

          if (e.keyCode == SWT.ESC) {
            text.dispose();
          }
        }
      };

      text.addKeyListener(lsKeyText);
      text.addFocusListener(new FocusAdapter() {
        public void focusLost(FocusEvent e) {
          String newName = text.getText();
          text.dispose();
          renameStep(stepMeta, newName);
        }
      });

      this.layout(true, true);

      text.setFocus();
      text.setSelection(0, name.length());
    }
  }

  public void renameStep(StepMeta stepMeta, String stepname) {
    String newname = stepname;

    StepMeta smeta = transMeta.findStep(newname, stepMeta);
    int nr = 2;
    while (smeta != null) {
      newname = stepname + " " + nr;
      smeta = transMeta.findStep(newname);
      nr++;
    }
    if (nr > 2) {
      stepname = newname;
      MessageBox mb = new MessageBox(shell, SWT.OK | SWT.ICON_INFORMATION);
      mb.setMessage(BaseMessages.getString(PKG, "Spoon.Dialog.StepnameExists.Message", stepname)); // $NON-NLS-1$
      mb.setText(BaseMessages.getString(PKG, "Spoon.Dialog.StepnameExists.Title")); // $NON-NLS-1$
      mb.open();
    }
    stepMeta.setName(stepname);
    stepMeta.setChanged();
    spoon.refreshTree(); // to reflect the new name
    spoon.refreshGraph();
  }

  public void clearSettings() {
    selectedStep = null;
    noInputStep = null;
    selectedNote = null;
    selectedSteps = null;
    selectionRegion = null;
    candidate = null;
    last_hop_split = null;
    last_button = 0;
    iconoffset = null;
    startHopStep = null;
    endHopLocation = null;
    showHopInputIcons =false;
    mouseOverSteps.clear();
    for (int i = 0; i < transMeta.nrTransHops(); i++)
      transMeta.getTransHop(i).split = false;
  }

  public String[] getDropStrings(String str, String sep) {
    StringTokenizer strtok = new StringTokenizer(str, sep);
    String retval[] = new String[strtok.countTokens()];
    int i = 0;
    while (strtok.hasMoreElements()) {
      retval[i] = strtok.nextToken();
      i++;
    }
    return retval;
  }

  public Point screen2real(int x, int y) {
    offset = getOffset();
    Point real;
    if (offset != null) {
      real = new Point(Math.round((x / magnification - offset.x)), Math.round((y / magnification - offset.y)));
    } else {
      real = new Point(x, y);
    }

    return real;
  }

  public Point real2screen(int x, int y) {
    offset = getOffset();
    Point screen = new Point(x + offset.x, y + offset.y);

    return screen;
  }

  public Point getRealPosition(Composite canvas, int x, int y) {
    Point p = new Point(0, 0);
    Composite follow = canvas;
    while (follow != null) {
      org.eclipse.swt.graphics.Point loc = follow.getLocation();
      Point xy = new Point(loc.x, loc.y);
      p.x += xy.x;
      p.y += xy.y;
      follow = follow.getParent();
    }

    int offsetX = -16;
    int offsetY = -64;
    if (Const.isOSX()) {
      offsetX = -2;
      offsetY = -24;
    }
    p.x = x - p.x + offsetX;
    p.y = y - p.y + offsetY;

    return screen2real(p.x, p.y);
  }

  /**
   *  See if location (x,y) is on a line between two steps: the hop!
   *  @param x
   *  @param y
   *  @return the transformation hop on the specified location, otherwise: null 
   */
  private TransHopMeta findHop(int x, int y) {
    return findHop(x, y, null);
  }

  /**
   *  See if location (x,y) is on a line between two steps: the hop!
   *  @param x
   *  @param y
   *  @param exclude the step to exclude from the hops (from or to location). Specify null if no step is to be excluded.
   *  @return the transformation hop on the specified location, otherwise: null 
   */
  private TransHopMeta findHop(int x, int y, StepMeta exclude) {
    int i;
    TransHopMeta online = null;
    for (i = 0; i < transMeta.nrTransHops(); i++) {
      TransHopMeta hi = transMeta.getTransHop(i);
      StepMeta fs = hi.getFromStep();
      StepMeta ts = hi.getToStep();

      if (fs == null || ts == null)
        return null;

      // If either the "from" or "to" step is excluded, skip this hop.
      //
      if (exclude != null && (exclude.equals(fs) || exclude.equals(ts)))
        continue;

      int line[] = getLine(fs, ts);

      if (pointOnLine(x, y, line))
        online = hi;
    }
    return online;
  }

  private int[] getLine(StepMeta fs, StepMeta ts) {
    Point from = fs.getLocation();
    Point to = ts.getLocation();
    offset = getOffset();

    int x1 = from.x + iconsize / 2;
    int y1 = from.y + iconsize / 2;

    int x2 = to.x + iconsize / 2;
    int y2 = to.y + iconsize / 2;

    return new int[] { x1, y1, x2, y2 };
  }

  public void hideStep() {
    for (int i = 0; i < transMeta.nrSteps(); i++) {
      StepMeta sti = transMeta.getStep(i);
      if (sti.isDrawn() && sti.isSelected()) {
        sti.hideStep();
        spoon.refreshTree();
      }
    }
    getCurrentStep().hideStep();
    spoon.refreshTree();
    redraw();
  }

  public void checkSelectedSteps() {
    spoon.checkTrans(transMeta, true);
  }

  public void detachStep() {
    detach(getCurrentStep());
    selectedSteps = null;
  }

  public void generateMappingToThisStep() {
    spoon.generateFieldMapping(transMeta, getCurrentStep());
  }

  public void partitioning() {
    spoon.editPartitioning(transMeta, getCurrentStep());
  }

  public void clustering() {
	StepMeta[] selected = transMeta.getSelectedSteps();
	if (selected!=null && selected.length>0) {
		spoon.editClustering(transMeta, transMeta.getSelectedSteps());
	} else {
		spoon.editClustering(transMeta, getCurrentStep());
	}
  }

  public void errorHandling() {
    spoon.editStepErrorHandling(transMeta, getCurrentStep());
  }

  public void newHopChoice() {
    selectedSteps = null;
    newHop();
  }

  public void editStep() {
    selectedSteps = null;
    editStep(getCurrentStep());
  }

  public void editDescription() {
    editDescription(getCurrentStep());
  }

  public void setDistributes() {
    getCurrentStep().setDistributes(true);
    spoon.refreshGraph();
    spoon.refreshTree();
  }

  public void setCopies() {
    getCurrentStep().setDistributes(false);
    spoon.refreshGraph();
    spoon.refreshTree();
  }

  public void copies() {
    final boolean multipleOK = checkNumberOfCopies(transMeta, getCurrentStep());
    selectedSteps = null;
    String tt = BaseMessages.getString(PKG, "TransGraph.Dialog.NrOfCopiesOfStep.Title"); //$NON-NLS-1$
    String mt = BaseMessages.getString(PKG, "TransGraph.Dialog.NrOfCopiesOfStep.Message"); //$NON-NLS-1$
    EnterNumberDialog nd = new EnterNumberDialog(shell, getCurrentStep().getCopies(), tt, mt);
    int cop = nd.open();
    if (cop >= 0) {
      if (cop == 0)
        cop = 1;

      if (!multipleOK) {
        cop = 1;

        MessageBox mb = new MessageBox(shell, SWT.YES | SWT.ICON_WARNING);
        mb.setMessage(BaseMessages.getString(PKG, "TransGraph.Dialog.MultipleCopiesAreNotAllowedHere.Message")); //$NON-NLS-1$
        mb.setText(BaseMessages.getString(PKG, "TransGraph.Dialog.MultipleCopiesAreNotAllowedHere.Title")); //$NON-NLS-1$
        mb.open();

      }

      if (getCurrentStep().getCopies() != cop) {
        getCurrentStep().setCopies(cop);
        spoon.refreshGraph();
      }
    }
  }

  public void dupeStep() {
    try {
      if (transMeta.nrSelectedSteps() <= 1) {
        spoon.dupeStep(transMeta, getCurrentStep());
      } else {
        for (int i = 0; i < transMeta.nrSteps(); i++) {
          StepMeta stepMeta = transMeta.getStep(i);
          if (stepMeta.isSelected()) {
            spoon.dupeStep(transMeta, stepMeta);
          }
        }
      }
    } catch (Exception ex) {
      new ErrorDialog(
          shell,
          BaseMessages.getString(PKG, "TransGraph.Dialog.ErrorDuplicatingStep.Title"), BaseMessages.getString(PKG, "TransGraph.Dialog.ErrorDuplicatingStep.Message"), ex); //$NON-NLS-1$ //$NON-NLS-2$
    }
  }

  public void copyStep() {
    spoon.copySelected(transMeta, transMeta.getSelectedSteps(), transMeta.getSelectedNotes());
  }

  public void delSelected() {
    delSelected(getCurrentStep());
  }

  public void fieldsBefore() {
    selectedSteps = null;
    inputOutputFields(getCurrentStep(), true);
  }

  public void fieldsAfter() {
    selectedSteps = null;
    inputOutputFields(getCurrentStep(), false);
  }
  
  public void fieldsLineage() {
	  TransDataLineage tdl = new TransDataLineage(transMeta);
	  try {
		  tdl.calculateLineage();
	  }
	  catch(Exception e) {
		  new ErrorDialog(shell, "Lineage error", "Unexpected lineage calculation error", e);
	  }
  }

  public void editHop() {
    selectionRegion = null;
    editHop(getCurrentHop());
  }

  public void flipHopDirection() {
    selectionRegion = null;
    TransHopMeta hi = getCurrentHop();

    hi.flip();

    if (transMeta.hasLoop(hi.getFromStep())) {
      spoon.refreshGraph();
      MessageBox mb = new MessageBox(shell, SWT.YES | SWT.ICON_WARNING);
      mb.setMessage(BaseMessages.getString(PKG, "TransGraph.Dialog.LoopsAreNotAllowed.Message")); //$NON-NLS-1$
      mb.setText(BaseMessages.getString(PKG, "TransGraph.Dialog.LoopsAreNotAllowed.Title")); //$NON-NLS-1$
      mb.open();

      hi.flip();
      spoon.refreshGraph();
    } else {
      hi.setChanged();
      spoon.refreshGraph();
      spoon.refreshTree();
      spoon.setShellText();
    }
  }

  public void enableHop() {
    selectionRegion = null;
    TransHopMeta hi = getCurrentHop();
    TransHopMeta before = (TransHopMeta) hi.clone();
    hi.setEnabled(!hi.isEnabled());
    if (transMeta.hasLoop(hi.getToStep())) {
      hi.setEnabled(!hi.isEnabled());
      MessageBox mb = new MessageBox(shell, SWT.OK | SWT.ICON_WARNING);
      mb.setMessage(BaseMessages.getString(PKG, "TransGraph.Dialog.LoopAfterHopEnabled.Message")); //$NON-NLS-1$
      mb.setText(BaseMessages.getString(PKG, "TransGraph.Dialog.LoopAfterHopEnabled.Title")); //$NON-NLS-1$
      mb.open();
    } else {
      TransHopMeta after = (TransHopMeta) hi.clone();
      spoon.addUndoChange(transMeta, new TransHopMeta[] { before }, new TransHopMeta[] { after }, new int[] { transMeta
          .indexOfTransHop(hi) });
      spoon.refreshGraph();
      spoon.refreshTree();
    }
  }

  public void deleteHop() {
    selectionRegion = null;
    TransHopMeta hi = getCurrentHop();
    int idx = transMeta.indexOfTransHop(hi);
    spoon.addUndoDelete(transMeta, new TransHopMeta[] { (TransHopMeta) hi.clone() }, new int[] { idx });
    transMeta.removeTransHop(idx);
    spoon.refreshTree();
    spoon.refreshGraph();
  }

  public void editNote() {
    selectionRegion = null;
    editNote(getCurrentNote());
  }

  public void deleteNote() {
    selectionRegion = null;
    int idx = transMeta.indexOfNote(ni);
    if (idx >= 0) {
      transMeta.removeNote(idx);
      spoon.addUndoDelete(transMeta, new NotePadMeta[] { (NotePadMeta) ni.clone() }, new int[] { idx });
      redraw();
    }
  }

  public void raiseNote() {
    selectionRegion = null;
    int idx = transMeta.indexOfNote(getCurrentNote());
    if (idx >= 0) {
      transMeta.raiseNote(idx);
      //TBD: spoon.addUndoRaise(transMeta, new NotePadMeta[] {getCurrentNote()}, new int[] {idx} );
    }
    redraw();
  }

  public void lowerNote() {
    selectionRegion = null;
    int idx = transMeta.indexOfNote(getCurrentNote());
    if (idx >= 0) {
      transMeta.lowerNote(idx);
      //TBD: spoon.addUndoLower(transMeta, new NotePadMeta[] {getCurrentNote()}, new int[] {idx} );
    }
    redraw();
  }

  public void newNote() {
    selectionRegion = null;
    String title = BaseMessages.getString(PKG, "TransGraph.Dialog.NoteEditor.Title"); //$NON-NLS-1$
    NotePadDialog dd = new NotePadDialog(shell, title); //$NON-NLS-1$
    NotePadMeta n = dd.open();
    if (n != null)
    {
        NotePadMeta npi = new NotePadMeta(n.getNote(), lastclick.x, lastclick.y, ConstUI.NOTE_MIN_SIZE, 
        		ConstUI.NOTE_MIN_SIZE,n.getFontName(),n.getFontSize(), n.isFontBold(), n.isFontItalic(),
        		n.getFontColorRed(),n.getFontColorGreen(),n.getFontColorBlue(),
        		n.getBackGroundColorRed(), n.getBackGroundColorGreen(),n.getBackGroundColorBlue(), 
        		n.getBorderColorRed(), n.getBorderColorGreen(),n.getBorderColorBlue(), 
        		n.isDrawShadow());
        transMeta.addNote(npi);
        spoon.addUndoNew(transMeta, new NotePadMeta[] { npi }, new int[] { transMeta.indexOfNote(npi) });
        redraw();
      }
  }

  public void paste() {
    final String clipcontent = spoon.fromClipboard();
    Point loc = new Point(currentMouseX, currentMouseY);
    spoon.pasteXML(transMeta, clipcontent, loc);
  }

  public void settings() {
    editProperties(transMeta, spoon, spoon.getRepository(), true);
  }

  public void newStep(String description) {
    StepMeta stepMeta = spoon.newStep(transMeta, description, description, false, true);
    PropsUI.setLocation(stepMeta, currentMouseX, currentMouseY);
    stepMeta.setDraw(true);
    redraw();
  }

  /**
   * This sets the popup-menu on the background of the canvas based on the xy coordinate of the mouse. This method is
   * called after a mouse-click.
   * 
   * @param x X-coordinate on screen
   * @param y Y-coordinate on screen
   */
  private synchronized void setMenu(int x, int y) {
    try {
      currentMouseX = x;
      currentMouseY = y;

      final StepMeta stepMeta = transMeta.getStep(x, y, iconsize);
      if (stepMeta != null) // We clicked on a Step!
      {
        setCurrentStep(stepMeta);

        XulPopupMenu menu = (XulPopupMenu) menuMap.get("trans-graph-entry"); //$NON-NLS-1$
        if (menu != null) {
          int sels = transMeta.nrSelectedSteps();

          XulMenuChoice item = menu.getMenuItemById("trans-graph-entry-newhop"); //$NON-NLS-1$
          menu.addMenuListener("trans-graph-entry-newhop", this, TransGraph.class, "newHop"); //$NON-NLS-1$ //$NON-NLS-2$
          item.setEnabled(sels == 2);
          
          item = menu.getMenuItemById("trans-graph-entry-open-mapping"); // $NON-NLS-1$
          menu.addMenuListener("trans-graph-entry-open-mapping", this, TransGraph.class, "openMapping"); //$NON-NLS-1$ //$NON-NLS-2$
          item.setEnabled(stepMeta.isMapping());

          item = menu.getMenuItemById("trans-graph-entry-align-snap"); //$NON-NLS-1$
          item.setText(BaseMessages.getString(PKG, "TransGraph.PopupMenu.SnapToGrid") + ConstUI.GRID_SIZE + ")\tALT-HOME");

          XulMenu aMenu = menu.getMenuById("trans-graph-entry-align"); //$NON-NLS-1$

          if (aMenu != null) {
            aMenu.setEnabled(sels > 1);
          }

          menu.addMenuListener("trans-graph-entry-align-left", this, "allignleft"); //$NON-NLS-1$ //$NON-NLS-2$
          menu.addMenuListener("trans-graph-entry-align-right", this, "allignright"); //$NON-NLS-1$ //$NON-NLS-2$
          menu.addMenuListener("trans-graph-entry-align-top", this, "alligntop"); //$NON-NLS-1$ //$NON-NLS-2$
          menu.addMenuListener("trans-graph-entry-align-bottom", this, "allignbottom"); //$NON-NLS-1$ //$NON-NLS-2$
          menu.addMenuListener("trans-graph-entry-align-horz", this, "distributehorizontal"); //$NON-NLS-1$ //$NON-NLS-2$
          menu.addMenuListener("trans-graph-entry-align-vert", this, "distributevertical"); //$NON-NLS-1$ //$NON-NLS-2$
          menu.addMenuListener("trans-graph-entry-align-snap", this, "snaptogrid"); //$NON-NLS-1$ //$NON-NLS-2$

          item = menu.getMenuItemById("trans-graph-entry-data-movement-distribute"); //$NON-NLS-1$
          item.setChecked(stepMeta.isDistributes());
          item = menu.getMenuItemById("trans-graph-entry-data-movement-copy"); //$NON-NLS-1$
          item.setChecked(!stepMeta.isDistributes());

          item = menu.getMenuItemById("trans-graph-entry-hide"); //$NON-NLS-1$
          item.setEnabled(stepMeta.isDrawn() && !transMeta.isStepUsedInTransHops(stepMeta));

          item = menu.getMenuItemById("trans-graph-entry-detach"); //$NON-NLS-1$
          item.setEnabled(transMeta.isStepUsedInTransHops(stepMeta));

          item = menu.getMenuItemById("trans-graph-entry-errors"); //$NON-NLS-1$
          item.setEnabled(stepMeta.supportsErrorHandling());

          menu.addMenuListener("trans-graph-entry-newhop", this, "newHopChoice"); //$NON-NLS-1$ //$NON-NLS-2$
          menu.addMenuListener("trans-graph-entry-edit", this, "editStep"); //$NON-NLS-1$ //$NON-NLS-2$
          menu.addMenuListener("trans-graph-entry-edit-description", this, "editDescription"); //$NON-NLS-1$ //$NON-NLS-2$

          menu.addMenuListener("trans-graph-entry-data-movement-distribute", this, "setDistributes"); //$NON-NLS-1$ //$NON-NLS-2$
          menu.addMenuListener("trans-graph-entry-data-movement-copy", this, "setCopies"); //$NON-NLS-1$ //$NON-NLS-2$
          menu.addMenuListener("trans-graph-entry-copies", this, "copies"); //$NON-NLS-1$ //$NON-NLS-2$
          menu.addMenuListener("trans-graph-entry-copy", this, "copyStep"); //$NON-NLS-1$ //$NON-NLS-2$
          menu.addMenuListener("trans-graph-entry-duplicate", this, "dupeStep"); //$NON-NLS-1$ //$NON-NLS-2$
          menu.addMenuListener("trans-graph-entry-delete", this, "delSelected"); //$NON-NLS-1$ //$NON-NLS-2$
          menu.addMenuListener("trans-graph-entry-hide", this, "hideStep"); //$NON-NLS-1$ //$NON-NLS-2$
          menu.addMenuListener("trans-graph-entry-detach", this, "detachStep"); //$NON-NLS-1$ //$NON-NLS-2$
          menu.addMenuListener("trans-graph-entry-inputs", this, "fieldsBefore"); //$NON-NLS-1$ //$NON-NLS-2$
          menu.addMenuListener("trans-graph-entry-outputs", this, "fieldsAfter"); //$NON-NLS-1$ //$NON-NLS-2$
          // menu.addMenuListener("trans-graph-entry-lineage", this, "fieldsLineage"); //$NON-NLS-1$ //$NON-NLS-2$
          menu.addMenuListener("trans-graph-entry-verify", this, "checkSelectedSteps"); //$NON-NLS-1$ //$NON-NLS-2$
          menu.addMenuListener("trans-graph-entry-mapping", this, "generateMappingToThisStep"); //$NON-NLS-1$ //$NON-NLS-2$
          menu.addMenuListener("trans-graph-entry-partitioning", this, "partitioning"); //$NON-NLS-1$ //$NON-NLS-2$
          menu.addMenuListener("trans-graph-entry-clustering", this, "clustering"); //$NON-NLS-1$ //$NON-NLS-2$
          menu.addMenuListener("trans-graph-entry-errors", this, "errorHandling"); //$NON-NLS-1$ //$NON-NLS-2$
          menu.addMenuListener("trans-graph-entry-preview", this, "preview"); //$NON-NLS-1$ //$NON-NLS-2$
          ConstUI.displayMenu((Menu)menu.getNativeObject(), canvas);
        }
      } else {
        final TransHopMeta hi = findHop(x, y);
        if (hi != null) // We clicked on a HOP!
        {
          XulPopupMenu menu = (XulPopupMenu) menuMap.get("trans-graph-hop"); //$NON-NLS-1$
          if (menu != null) {
            setCurrentHop(hi);

            XulMenuChoice item = menu.getMenuItemById("trans-graph-hop-enabled"); //$NON-NLS-1$
            if (item != null) {
              if (hi.isEnabled()) {
                item.setText(BaseMessages.getString(PKG, "TransGraph.PopupMenu.DisableHop")); //$NON-NLS-1$
              } else {
                item.setText(BaseMessages.getString(PKG, "TransGraph.PopupMenu.EnableHop")); //$NON-NLS-1$
              }
            }

            menu.addMenuListener("trans-graph-hop-edit", this, "editHop"); //$NON-NLS-1$ //$NON-NLS-2$
            menu.addMenuListener("trans-graph-hop-flip", this, "flipHopDirection"); //$NON-NLS-1$ //$NON-NLS-2$
            menu.addMenuListener("trans-graph-hop-enabled", this, "enableHop"); //$NON-NLS-1$ //$NON-NLS-2$
            menu.addMenuListener("trans-graph-hop-delete", this, "deleteHop"); //$NON-NLS-1$ //$NON-NLS-2$

            ConstUI.displayMenu((Menu)menu.getNativeObject(), canvas);
          }
        } else {
          // Clicked on the background: maybe we hit a note?
          final NotePadMeta ni = transMeta.getNote(x, y);
          setCurrentNote(ni);
          if (ni != null) {

            XulPopupMenu menu = (XulPopupMenu) menuMap.get("trans-graph-note"); //$NON-NLS-1$
            if (menu != null) {

              menu.addMenuListener("trans-graph-note-edit", this, "editNote"); //$NON-NLS-1$ //$NON-NLS-2$
              menu.addMenuListener("trans-graph-note-delete", this, "deleteNote"); //$NON-NLS-1$ //$NON-NLS-2$
              menu.addMenuListener("trans-graph-note-raise", this, "raiseNote"); //$NON-NLS-1$ //$NON-NLS-2$
              menu.addMenuListener("trans-graph-note-lower", this, "lowerNote"); //$NON-NLS-1$ //$NON-NLS-2$
              ConstUI.displayMenu((Menu)menu.getNativeObject(), canvas);
            }
          } else {

            XulPopupMenu menu = (XulPopupMenu) menuMap.get("trans-graph-background"); //$NON-NLS-1$
            if (menu != null) {

              menu.addMenuListener("trans-graph-background-new-note", this, "newNote"); //$NON-NLS-1$ //$NON-NLS-2$
              menu.addMenuListener("trans-graph-background-paste", this, "paste"); //$NON-NLS-1$ //$NON-NLS-2$
              menu.addMenuListener("trans-graph-background-settings", this, "settings"); //$NON-NLS-1$ //$NON-NLS-2$

              final String clipcontent = spoon.fromClipboard();
              XulMenuChoice item = menu.getMenuItemById("trans-graph-background-paste"); //$NON-NLS-1$
              if (item != null) {
                item.setEnabled(clipcontent != null);
              }
              String locale = LanguageChoice.getInstance().getDefaultLocale().toString().toLowerCase();

              XulMenu subMenu = menu.getMenuById("trans-graph-background-new-step");
              if (subMenu.getItemCount() == 0) {
                // group these by type so the menu doesn't stretch the height of the screen and to be friendly to testing tools
                StepLoader steploader = StepLoader.getInstance();
                // get a list of the categories
                String basecat[] = steploader.getCategories(StepPlugin.TYPE_ALL, locale);
                // get all the plugins
                StepPlugin basesteps[] = steploader.getStepsWithType(StepPlugin.TYPE_ALL);

                XulMessages xulMessages = new XulMessages();
                for (int cat = 0; cat < basecat.length; cat++) {
                  // create a submenu for this category
                  org.pentaho.xul.swt.menu.Menu catMenu = new org.pentaho.xul.swt.menu.Menu(
                      (org.pentaho.xul.swt.menu.Menu) subMenu, basecat[cat], basecat[cat], null);
                  for (int step = 0; step < basesteps.length; step++) {
                    // find the steps for this category
                    if (basesteps[step].getCategory(locale).equalsIgnoreCase(basecat[cat])) {
                      // create a menu option for this step
                      final String name = basesteps[step].getDescription();
                      new MenuChoice(catMenu, name, name, null, null, MenuChoice.TYPE_PLAIN, xulMessages);
                      menu.addMenuListener(name, this, "newStep"); //$NON-NLS-1$ //$NON-NLS-2$
                    }
                  }
                }

              }
              
              menu.addMenuListener("trans-graph-background-select-all", this, "selectAll"); //$NON-NLS-1$ //$NON-NLS-2$
              menu.addMenuListener("trans-graph-background-clear-selection", this, "clearSelection"); //$NON-NLS-1$ //$NON-NLS-2$

              ConstUI.displayMenu((Menu)menu.getNativeObject(), canvas);
            }

          }
        }
      }
    } catch (Throwable t) {
      // TODO: fix this: log somehow, is IGNORED for now.
      t.printStackTrace();
    }
  }
  
  public void selectAll() {
	  spoon.editSelectAll();
  }
  
  public void clearSelection() {
	  spoon.editUnselectAll();
  }

  private boolean checkNumberOfCopies(TransMeta transMeta, StepMeta stepMeta) {
    boolean enabled = true;
    List<StepMeta> prevSteps = transMeta.findPreviousSteps(stepMeta);
    for (StepMeta prevStep : prevSteps) {
      // See what the target steps are.  
      // If one of the target steps is our original step, we can't start multiple copies
      // 
      String[] targetSteps = prevStep.getStepMetaInterface().getStepIOMeta().getTargetStepnames();
      if (targetSteps != null) {
        for (int t = 0; t < targetSteps.length && enabled; t++) {
          if (!Const.isEmpty(targetSteps[t]) && targetSteps[t].equalsIgnoreCase(stepMeta.getName()))
            enabled = false;
        }
      }
    }
    return enabled;
  }

  private AreaOwner setToolTip(int x, int y, int screenX, int screenY) {
		AreaOwner subject = null;

		if (!spoon.getProperties().showToolTips())
			return subject;

		canvas.setToolTipText(null);

		String newTip = null;
		Image tipImage = null;

		final TransHopMeta hi = findHop(x, y);
		// check the area owner list...
		//
		StringBuffer tip = new StringBuffer();
		AreaOwner areaOwner = getVisibleAreaOwner(x, y);
		if (areaOwner!=null) {
			switch (areaOwner.getAreaType()) {
			case REMOTE_INPUT_STEP:
				StepMeta step = (StepMeta) areaOwner.getParent();
				tip.append("Remote input steps:").append(Const.CR).append("-----------------------").append(Const.CR);
				for (RemoteStep remoteStep : step.getRemoteInputSteps()) {
					tip.append(remoteStep.toString()).append(Const.CR);
				}
				break;
			case REMOTE_OUTPUT_STEP:
				step = (StepMeta) areaOwner.getParent();
				tip.append("Remote output steps:").append(Const.CR).append("-----------------------").append(Const.CR);
				for (RemoteStep remoteStep : step.getRemoteOutputSteps()) {
					tip.append(remoteStep.toString()).append(Const.CR);
				}
				break;
			case STEP_PARTITIONING:
				step = (StepMeta) areaOwner.getParent();
				tip.append("Step partitioning:").append(Const.CR).append("-----------------------").append(Const.CR);
				tip.append(step.getStepPartitioningMeta().toString()).append(Const.CR);
				if (step.getTargetStepPartitioningMeta() != null) {
					tip.append(Const.CR).append(Const.CR).append("TARGET: " + step.getTargetStepPartitioningMeta().toString()).append(Const.CR);
				}
				break;
			case STEP_ERROR_ICON:
				String log = (String) areaOwner.getParent();
				tip.append(log);
				tipImage = GUIResource.getInstance().getImageStepError();
				break;
			case HOP_COPY_ICON:
				step = (StepMeta) areaOwner.getParent();
				tip.append(BaseMessages.getString(PKG, "TransGraph.Hop.Tooltip.HopTypeCopy", step.getName(), Const.CR));
				tipImage = GUIResource.getInstance().getImageCopyHop();
				break;
			case HOP_INFO_ICON:
				StepMeta from = ((StepMeta[]) (areaOwner.getParent()))[0];
				StepMeta to = ((StepMeta[]) (areaOwner.getParent()))[1];
				tip.append(BaseMessages.getString(PKG, "TransGraph.Hop.Tooltip.HopTypeInfo", to.getName(), from.getName(), Const.CR));
				tipImage = GUIResource.getInstance().getImageInfoHop();
				break;
			case HOP_ERROR_ICON:
				from = ((StepMeta[]) (areaOwner.getParent()))[0];
				to = ((StepMeta[]) (areaOwner.getParent()))[1];
				tip.append(BaseMessages.getString(PKG, "TransGraph.Hop.Tooltip.HopTypeError", from.getName(), to.getName(), Const.CR));
				tipImage = GUIResource.getInstance().getImageErrorHop();
				break;
			case HOP_INFO_STEP_COPIES_ERROR:
				from = ((StepMeta[]) (areaOwner.getParent()))[0];
				to = ((StepMeta[]) (areaOwner.getParent()))[1];
				tip.append(BaseMessages.getString(PKG, "TransGraph.Hop.Tooltip.InfoStepCopies", from.getName(), to.getName(), Const.CR));
				tipImage = GUIResource.getInstance().getImageStepError();
				break;
			case REPOSITORY_LOCK_IMAGE:
				RepositoryLock lock = (RepositoryLock) areaOwner.getOwner();
				tip.append(BaseMessages.getString(PKG, "TransGraph.Locked.Tooltip", Const.CR, lock.getLogin(), lock.getUsername(), lock.getMessage(), XMLHandler.date2string(lock.getLockDate())));
				tipImage = GUIResource.getInstance().getImageLocked();
				break;
			case STEP_INPUT_HOP_ICON:
				// StepMeta subjectStep = (StepMeta) (areaOwner.getParent());
				tip.append(BaseMessages.getString(PKG, "TransGraph.StepInputConnector.Tooltip"));
				tipImage = GUIResource.getInstance().getImageHopInput();
				break;
			case STEP_OUTPUT_HOP_ICON:
				//subjectStep = (StepMeta) (areaOwner.getParent());
				tip.append(BaseMessages.getString(PKG, "TransGraph.StepOutputConnector.Tooltip"));
				tipImage = GUIResource.getInstance().getImageHopOutput();
				break;
			case STEP_INFO_HOP_ICON:
				// subjectStep = (StepMeta) (areaOwner.getParent());
				StreamInterface stream = (StreamInterface) areaOwner.getOwner();
				tip.append(BaseMessages.getString(PKG, "TransGraph.StepInfoConnector.Tooltip")+Const.CR+stream.toString());
				tipImage = GUIResource.getInstance().getImageHopOutput();
				break;
			case STEP_TARGET_HOP_ICON:
				// subjectStep = (StepMeta) (areaOwner.getParent());
				stream = (StreamInterface) areaOwner.getOwner();
				tip.append(BaseMessages.getString(PKG, "TransGraph.StepTargetConnector.Tooltip")+Const.CR+stream.toString());
				tipImage = GUIResource.getInstance().getImageHopOutput();
				break;
			case STEP_ERROR_HOP_ICON:
				tip.append(BaseMessages.getString(PKG, "TransGraph.StepSupportsErrorHandling.Tooltip"));
				tipImage = GUIResource.getInstance().getImageHopOutput();
				break;
			}
		}

		if (hi != null) // We clicked on a HOP!
		{
			// Set the tooltip for the hop:
			tip.append(Const.CR).append("Hop information: ").append(newTip = hi.toString()).append(Const.CR);
		}

		if (tip.length() == 0) {
			newTip = null;
		} else {
			newTip = tip.toString();
		}

		if (newTip == null) {
			toolTip.hide();
			if (hi != null) // We clicked on a HOP!
			{
				// Set the tooltip for the hop:
				newTip = BaseMessages.getString(PKG, "TransGraph.Dialog.HopInfo") + Const.CR + BaseMessages.getString(PKG, "TransGraph.Dialog.HopInfo.SourceStep") + " " + hi.getFromStep().getName() + Const.CR
						+ BaseMessages.getString(PKG, "TransGraph.Dialog.HopInfo.TargetStep") + " " + hi.getToStep().getName() + Const.CR + BaseMessages.getString(PKG, "TransGraph.Dialog.HopInfo.Status") + " "
						+ (hi.isEnabled() ? BaseMessages.getString(PKG, "TransGraph.Dialog.HopInfo.Enable") : BaseMessages.getString(PKG, "TransGraph.Dialog.HopInfo.Disable"));
				toolTip.setText(newTip);
				if (hi.isEnabled())
					toolTip.setImage(GUIResource.getInstance().getImageHop());
				else
					toolTip.setImage(GUIResource.getInstance().getImageDisabledHop());
				toolTip.show(new org.eclipse.swt.graphics.Point(screenX, screenY));
			} else {
				newTip = null;
			}

		} else if (!newTip.equalsIgnoreCase(getToolTipText())) {
			if (tipImage != null) {
				toolTip.setImage(tipImage);
			} else {
				toolTip.setImage(GUIResource.getInstance().getImageSpoon());
			}
			toolTip.setText(newTip);
			toolTip.hide();
			toolTip.show(new org.eclipse.swt.graphics.Point(x, y));
		}

		return subject;
	}
  
  public AreaOwner getVisibleAreaOwner(int x, int y) {
		for (int i=areaOwners.size()-1;i>=0;i--) {
			AreaOwner areaOwner = areaOwners.get(i);
			if (areaOwner.contains(x, y)) {
				return areaOwner;
			}
		}
		return null;
  }

  public void delSelected(StepMeta stMeta) {
    if (transMeta.nrSelectedSteps() == 0) {
      spoon.delStep(transMeta, stMeta);
      return;
    }

    // Get the list of steps that would be deleted
    List<String> stepList = new ArrayList<String>();
    for (int i = transMeta.nrSteps() - 1; i >= 0; i--) {
      StepMeta stepMeta = transMeta.getStep(i);
      if (stepMeta.isSelected() || (stMeta != null && stMeta.equals(stepMeta))) {
        stepList.add(stepMeta.getName());
      }
    }

    // Create and display the delete confirmation dialog
    MessageBox mb = new DeleteMessageBox(shell, BaseMessages.getString(PKG, "TransGraph.Dialog.Warning.DeleteSteps.Message"), //$NON-NLS-1$
        stepList);
    int result = mb.open();
    if (result == SWT.YES) {
      // Delete the steps
      for (int i = transMeta.nrSteps() - 1; i >= 0; i--) {
        StepMeta stepMeta = transMeta.getStep(i);
        if (stepMeta.isSelected() || (stMeta != null && stMeta.equals(stepMeta))) {
          spoon.delStep(transMeta, stepMeta);
        }
      }
    }
  }

  public void editDescription(StepMeta stepMeta) {
    String title = BaseMessages.getString(PKG, "TransGraph.Dialog.StepDescription.Title"); //$NON-NLS-1$
    String message = BaseMessages.getString(PKG, "TransGraph.Dialog.StepDescription.Message"); //$NON-NLS-1$
    EnterTextDialog dd = new EnterTextDialog(shell, title, message, stepMeta.getDescription());
    String d = dd.open();
    if (d != null)
      stepMeta.setDescription(d);
  }

  /**
   * Display the input- or outputfields for a step.
   * 
   * @param stepMeta The step (it's metadata) to query
   * @param before set to true if you want to have the fields going INTO the step, false if you want to see all the
   * fields that exit the step.
   */
  private void inputOutputFields(StepMeta stepMeta, boolean before) {
    spoon.refreshGraph();

    SearchFieldsProgressDialog op = new SearchFieldsProgressDialog(transMeta, stepMeta, before);
    try {
      final ProgressMonitorDialog pmd = new ProgressMonitorDialog(shell);

      // Run something in the background to cancel active database queries, forecably if needed!
      Runnable run = new Runnable() {
        public void run() {
          IProgressMonitor monitor = pmd.getProgressMonitor();
          while (pmd.getShell() == null || (!pmd.getShell().isDisposed() && !monitor.isCanceled())) {
            try {
              Thread.sleep(250);
            } catch (InterruptedException e) {
            }
            ;
          }

          if (monitor.isCanceled()) // Disconnect and see what happens!
          {
            try {
              transMeta.cancelQueries();
            } catch (Exception e) {
            }
            ;
          }
        }
      };
      // Dump the cancel looker in the background!
      new Thread(run).start();

      pmd.run(true, true, op);
    } catch (InvocationTargetException e) {
      new ErrorDialog(
          shell,
          BaseMessages.getString(PKG, "TransGraph.Dialog.GettingFields.Title"), BaseMessages.getString(PKG, "TransGraph.Dialog.GettingFields.Message"), e); //$NON-NLS-1$ //$NON-NLS-2$
    } catch (InterruptedException e) {
      new ErrorDialog(
          shell,
          BaseMessages.getString(PKG, "TransGraph.Dialog.GettingFields.Title"), BaseMessages.getString(PKG, "TransGraph.Dialog.GettingFields.Message"), e); //$NON-NLS-1$ //$NON-NLS-2$
    }

    RowMetaInterface fields = op.getFields();

    if (fields != null && fields.size() > 0) {
      StepFieldsDialog sfd = new StepFieldsDialog(shell, transMeta, SWT.NONE, stepMeta.getName(), fields);
      String sn = (String) sfd.open();
      if (sn != null) {
        StepMeta esi = transMeta.findStep(sn);
        if (esi != null) {
          editStep(esi);
        }
      }
    } else {
      MessageBox mb = new MessageBox(shell, SWT.OK | SWT.ICON_INFORMATION);
      mb.setMessage(BaseMessages.getString(PKG, "TransGraph.Dialog.CouldntFindFields.Message")); //$NON-NLS-1$
      mb.setText(BaseMessages.getString(PKG, "TransGraph.Dialog.CouldntFindFields.Title")); //$NON-NLS-1$
      mb.open();
    }

  }

  public void paintControl(PaintEvent e) {
    Point area = getArea();
    if (area.x == 0 || area.y == 0)
      return; // nothing to do!

    Display disp = shell.getDisplay();

    Image img = getTransformationImage(disp, area.x, area.y, true, magnification);
    e.gc.drawImage(img, 0, 0);
    img.dispose();

    // spoon.setShellText();
  }

  public Image getTransformationImage(Device device, int x, int y, boolean branded, float magnificationFactor) {
    TransPainter transPainter = new TransPainter(transMeta, new Point(x, y), hori, vert, candidate, drop_candidate,
        selectionRegion, areaOwners, mouseOverSteps);
    transPainter.setMagnification(magnificationFactor);
    transPainter.setStepLogMap(stepLogMap);
    transPainter.setShowingHopInputIcons(showHopInputIcons);
    transPainter.setStartHopStep(startHopStep);
    transPainter.setEndHopLocation(endHopLocation);
    transPainter.setNoInputStep(noInputStep);
    transPainter.setEndHopStep(endHopStep);
    transPainter.setCandidateHopType(candidateHopType);
    transPainter.setStartErrorHopStep(startErrorHopStep);
    Image img = transPainter.getTransformationImage(device, PropsUI.getInstance().isBrandingActive());

    return img;
  }

  private Point getArea() {
    org.eclipse.swt.graphics.Rectangle rect = canvas.getClientArea();
    Point area = new Point(rect.width, rect.height);

    return area;
  }

  private Point magnifyPoint(Point p) {
    return new Point(Math.round(p.x * magnification), Math.round(p.y * magnification));
  }

  private Point getThumb(Point area, Point transMax) {
    Point resizedMax = magnifyPoint(transMax);

    Point thumb = new Point(0, 0);
    if (resizedMax.x <= area.x)
      thumb.x = 100;
    else
      thumb.x = 100 * area.x / resizedMax.x;

    if (resizedMax.y <= area.y)
      thumb.y = 100;
    else
      thumb.y = 100 * area.y / resizedMax.y;

    return thumb;
  }

  private Point getOffset() {
    Point area = getArea();
    Point max = transMeta.getMaximum();
    Point thumb = getThumb(area, max);

    return getOffset(thumb, area);
  }

  private Point getOffset(Point thumb, Point area) {
    Point p = new Point(0, 0);
    Point sel = new Point(hori.getSelection(), vert.getSelection());

    if (thumb.x == 0 || thumb.y == 0)
      return p;

    p.x = -sel.x * area.x / thumb.x;
    p.y = -sel.y * area.y / thumb.y;

    return p;
  }

  public int sign(int n) {
    return n < 0 ? -1 : (n > 0 ? 1 : 1);
  }

  private void editStep(StepMeta stepMeta) {
    spoon.editStep(transMeta, stepMeta);
  }

  private void editNote(NotePadMeta ni) {
    NotePadMeta before = (NotePadMeta) ni.clone();

    String title = BaseMessages.getString(PKG, "TransGraph.Dialog.EditNote.Title"); //$NON-NLS-1$
    NotePadDialog dd = new NotePadDialog(shell, title, ni);
    NotePadMeta n = dd.open();

    if (n != null)
    {
        ni.setChanged();
        ni.setNote(n.getNote());
        ni.setFontName(n.getFontName());
        ni.setFontSize(n.getFontSize());
        ni.setFontBold(n.isFontBold());
        ni.setFontItalic(n.isFontItalic());
        // font color
        ni.setFontColorRed(n.getFontColorRed());
        ni.setFontColorGreen(n.getFontColorGreen());
        ni.setFontColorBlue(n.getFontColorBlue());
        // background color
        ni.setBackGroundColorRed(n.getBackGroundColorRed());
        ni.setBackGroundColorGreen(n.getBackGroundColorGreen());
        ni.setBackGroundColorBlue(n.getBackGroundColorBlue());
        // border color
        ni.setBorderColorRed(n.getBorderColorRed());
        ni.setBorderColorGreen(n.getBorderColorGreen());
        ni.setBorderColorBlue(n.getBorderColorBlue());
        ni.setDrawShadow(n.isDrawShadow());
        ni.width = ConstUI.NOTE_MIN_SIZE;
        ni.height = ConstUI.NOTE_MIN_SIZE;

        NotePadMeta after = (NotePadMeta) ni.clone();
        spoon.addUndoChange(transMeta, new NotePadMeta[] { before }, new NotePadMeta[] { after }, new int[] { transMeta
                .indexOfNote(ni) });
        spoon.refreshGraph();
    }
  }

  private void editHop(TransHopMeta transHopMeta) {
    String name = transHopMeta.toString();
    if(log.isDebug()) log.logDebug(BaseMessages.getString(PKG, "TransGraph.Logging.EditingHop") + name); //$NON-NLS-1$
    spoon.editHop(transMeta, transHopMeta);
  }

  private void newHop() {
    StepMeta fr = transMeta.getSelectedStep(0);
    StepMeta to = transMeta.getSelectedStep(1);
    spoon.newHop(transMeta, fr, to);
  }

  private boolean pointOnLine(int x, int y, int line[]) {
    int dx, dy;
    int pm = HOP_SEL_MARGIN / 2;
    boolean retval = false;

    for (dx = -pm; dx <= pm && !retval; dx++) {
      for (dy = -pm; dy <= pm && !retval; dy++) {
        retval = pointOnThinLine(x + dx, y + dy, line);
      }
    }

    return retval;
  }

  private boolean pointOnThinLine(int x, int y, int line[]) {
    int x1 = line[0];
    int y1 = line[1];
    int x2 = line[2];
    int y2 = line[3];

    // Not in the square formed by these 2 points: ignore!
    if (!(((x >= x1 && x <= x2) || (x >= x2 && x <= x1)) && ((y >= y1 && y <= y2) || (y >= y2 && y <= y1))))
      return false;

    double angle_line = Math.atan2(y2 - y1, x2 - x1) + Math.PI;
    double angle_point = Math.atan2(y - y1, x - x1) + Math.PI;

    // Same angle, or close enough?
    if (angle_point >= angle_line - 0.01 && angle_point <= angle_line + 0.01)
      return true;

    return false;
  }

  private SnapAllignDistribute createSnapAllignDistribute() {
    List<GUIPositionInterface> elements = transMeta.getSelectedDrawnStepsList();
    int[] indices = transMeta.getStepIndexes(elements.toArray(new StepMeta[elements.size()]));

    return new SnapAllignDistribute(transMeta, elements, indices, spoon, this);
  }

  public void snaptogrid() {
    snaptogrid(ConstUI.GRID_SIZE);
  }

  private void snaptogrid(int size) {
    createSnapAllignDistribute().snaptogrid(size);
  }

  public void allignleft() {
    createSnapAllignDistribute().allignleft();
  }

  public void allignright() {
    createSnapAllignDistribute().allignright();
  }

  public void alligntop() {
    createSnapAllignDistribute().alligntop();
  }

  public void allignbottom() {
    createSnapAllignDistribute().allignbottom();
  }

  public void distributehorizontal() {
    createSnapAllignDistribute().distributehorizontal();
  }

  public void distributevertical() {
    createSnapAllignDistribute().distributevertical();
  }

  public void zoomIn() {
    /*      if (magnificationIndex+1<TransPainter.magnifications.length) {
            magnification = TransPainter.magnifications[++magnificationIndex];
          }
    */
    magnification += .1f;
    redraw();
  }

  public void zoomOut() {
    /*      if (magnificationIndex>0) {
            magnification = TransPainter.magnifications[--magnificationIndex];
          }
    */
    magnification -= .1f;
    redraw();
  }

  public void zoom100Percent() {
    //magnificationIndex=TransPainter.MAGNIFICATION_100_PERCENT_INDEX;
    //magnification = TransPainter.magnifications[magnificationIndex];
    magnification = 1.0f;
    redraw();
  }

  private void detach(StepMeta stepMeta) {
    TransHopMeta hfrom = transMeta.findTransHopTo(stepMeta);
    TransHopMeta hto = transMeta.findTransHopFrom(stepMeta);

    if (hfrom != null && hto != null) {
      if (transMeta.findTransHop(hfrom.getFromStep(), hto.getToStep()) == null) {
        TransHopMeta hnew = new TransHopMeta(hfrom.getFromStep(), hto.getToStep());
        transMeta.addTransHop(hnew);
        spoon.addUndoNew(transMeta, new TransHopMeta[] { hnew }, new int[] { transMeta.indexOfTransHop(hnew) });
        spoon.refreshTree();
      }
    }
    if (hfrom != null) {
      int fromidx = transMeta.indexOfTransHop(hfrom);
      if (fromidx >= 0) {
        transMeta.removeTransHop(fromidx);
        spoon.refreshTree();
      }
    }
    if (hto != null) {
      int toidx = transMeta.indexOfTransHop(hto);
      if (toidx >= 0) {
        transMeta.removeTransHop(toidx);
        spoon.refreshTree();
      }
    }
    spoon.refreshTree();
    redraw();
  }

  // Preview the selected steps...
  public void preview() {
    spoon.previewTransformation();
  }

  public void newProps() {
    iconsize = spoon.props.getIconSize();
  }

  public String toString() {
    return this.getClass().getName();
  }

  public EngineMetaInterface getMeta() {
    return transMeta;
  }

  /**
   * @return the transMeta
   * /
  public TransMeta getTransMeta()
  {
      return transMeta;
  }

  /**
   * @param transMeta the transMeta to set
   */
  public void setTransMeta(TransMeta transMeta) {
    this.transMeta = transMeta;
  }

  // Change of step, connection, hop or note...
  public void addUndoPosition(Object obj[], int pos[], Point prev[], Point curr[]) {
    addUndoPosition(obj, pos, prev, curr, false);
  }

  // Change of step, connection, hop or note...
  public void addUndoPosition(Object obj[], int pos[], Point prev[], Point curr[], boolean nextAlso) {
    // It's better to store the indexes of the objects, not the objects itself!
    transMeta.addUndo(obj, null, pos, prev, curr, TransMeta.TYPE_UNDO_POSITION, nextAlso);
    spoon.setUndoMenu(transMeta);
  }

  /*
   * Shows a 'model has changed' warning if required
   * @return true if nothing has changed or the changes are rejected by the user.
   */
  public int showChangedWarning() {
    MessageBox mb = new MessageBox(shell, SWT.YES | SWT.NO | SWT.CANCEL | SWT.ICON_WARNING);
    mb.setMessage(BaseMessages.getString(PKG, "Spoon.Dialog.PromptSave.Message", spoon.makeTabName(transMeta, true)));//"This model has changed.  Do you want to save it?"
    mb.setText(BaseMessages.getString(PKG, "Spoon.Dialog.PromptSave.Title"));
    return mb.open();
  }

  public boolean applyChanges() throws KettleException {
    return spoon.saveToFile(transMeta);
  }

  public boolean canBeClosed() {
    return !transMeta.hasChanged();
  }

  public TransMeta getManagedObject() {
    return transMeta;
  }

  public boolean hasContentChanged() {
    return transMeta.hasChanged();
  }

  public List<CheckResultInterface> getRemarks() {
    return remarks;
  }

  public void setRemarks(List<CheckResultInterface> remarks) {
    this.remarks = remarks;
  }

  public List<DatabaseImpact> getImpact() {
    return impact;
  }

  public void setImpact(List<DatabaseImpact> impact) {
    this.impact = impact;
  }

  public boolean isImpactFinished() {
    return impactFinished;
  }

  public void setImpactFinished(boolean impactHasRun) {
    this.impactFinished = impactHasRun;
  }

  /**
   * @return the lastMove
   */
  public Point getLastMove() {
    return lastMove;
  }

  public static boolean editProperties(TransMeta transMeta, Spoon spoon, Repository rep, boolean allowDirectoryChange) {
    return editProperties(transMeta, spoon, rep, allowDirectoryChange, null);

  }

  public static boolean editProperties(TransMeta transMeta, Spoon spoon, Repository rep, boolean allowDirectoryChange,
      TransDialog.Tabs currentTab) {
    if (transMeta == null)
      return false;

    TransDialog tid = new TransDialog(spoon.getShell(), SWT.NONE, transMeta, rep, currentTab);
    tid.setDirectoryChangeAllowed(allowDirectoryChange);
    TransMeta ti = tid.open();

    // Load shared objects
    //
    if (tid.isSharedObjectsFileChanged()) {
      try {
        SharedObjects sharedObjects = rep!=null ? rep.readTransSharedObjects(transMeta) : transMeta.readSharedObjects();
        spoon.sharedObjectsFileMap.put(sharedObjects.getFilename(), sharedObjects);
      } catch (KettleException e) {
        new ErrorDialog(spoon.getShell(), BaseMessages.getString(PKG, "Spoon.Dialog.ErrorReadingSharedObjects.Title"), 
        		BaseMessages.getString(PKG, "Spoon.Dialog.ErrorReadingSharedObjects.Message", spoon.makeTabName(transMeta, true)), e);
      }
      
      // If we added properties, add them to the variables too, so that they appear in the CTRL-SPACE variable completion.
      //
      spoon.setParametersAsVariablesInUI(transMeta, transMeta);
      
      spoon.refreshTree();
      spoon.delegates.tabs.renameTabs(); // cheap operation, might as will do it anyway
    }

    spoon.setShellText();
    return ti != null;
  }

  public void newFileDropDown() {
    spoon.newFileDropDown(toolbar);
  }

  public void openFile() {
    spoon.openFile();
  }

  public void saveFile() throws KettleException {
    spoon.saveFile();
  }

  public void saveFileAs() throws KettleException {
    spoon.saveFileAs();
  }

  public void saveXMLFileToVfs() {
    spoon.saveXMLFileToVfs();
  }

  public void printFile() {
    spoon.printFile();
  }

  public void runTransformation() {
    spoon.runFile();
  }

  public void pauseTransformation() {
    pauseResume();
  }

  public void stopTransformation() {
    stop();
  }

  public void previewFile() {
    spoon.previewFile();
  }

  public void debugFile() {
    spoon.debugFile();
  }

  public void transReplay() {
    spoon.replayTransformation();
  }

  public void checkTrans() {
    spoon.checkTrans();
  }

  public void analyseImpact() {
    spoon.analyseImpact();
  }

  public void getSQL() {
    spoon.getSQL();
  }

  public void exploreDatabase() {
    spoon.exploreDatabase();
  }

  public void showExecutionResults() {
    
    if (extraViewComposite == null || extraViewComposite.isDisposed()) {
      addAllTabs();
    } else {
      disposeExtraView();
    }
  }
  
  public void browseVersionHistory() {
		  try {
			  if (spoon.rep.exists(transMeta.getName(), transMeta.getRepositoryDirectory(), RepositoryObjectType.TRANSFORMATION)) {
				RepositoryRevisionBrowserDialogInterface dialog = RepositoryExplorerDialog.getVersionBrowserDialog(shell, spoon.rep, transMeta.getName(), transMeta.getRepositoryDirectory(), transMeta.getRepositoryElementType());
				String versionLabel = dialog.open();
				if (versionLabel!=null) {
					spoon.loadObjectFromRepository(transMeta.getName(), transMeta.getRepositoryElementType(), transMeta.getRepositoryDirectory(), versionLabel);
				}
			  } else {
				  MessageBox box = new MessageBox(shell, SWT.CLOSE | SWT.ICON_ERROR);
				  box.setText("Sorry");
				  box.setMessage("Can't find this transformation in the repository");
				  box.open();
			  }
		} catch (Exception e) {
			new ErrorDialog(shell, BaseMessages.getString(PKG, "TransGraph.VersionBrowserException.Title"), BaseMessages.getString(PKG, "TransGraph.VersionBrowserException.Message"), e);
		}
  }

  /**
   * If the extra tab view at the bottom is empty, we close it.
   */
  public void checkEmptyExtraView() {
    if (extraViewTabFolder.getItemCount() == 0) {
      disposeExtraView();
    }
  }

  private void disposeExtraView() {


    extraViewComposite.dispose();
    sashForm.layout();
    sashForm.setWeights(new int[] { 100, });

    XulToolbarButton button = toolbar.getButtonById("trans-show-results");
    button.setImage(GUIResource.getInstance().getImageShowResults());
    button.setHint(BaseMessages.getString(PKG, "Spoon.Tooltip.ShowExecutionResults"));

  }

  private void minMaxExtraView() {
    // What is the state?
    //
    boolean maximized = sashForm.getMaximizedControl() != null;
    if (maximized) {
      // Minimize 
      //
      sashForm.setMaximizedControl(null);
      minMaxButton.setImage(GUIResource.getInstance().getImageMaximizePanel());
      minMaxButton.setToolTipText(BaseMessages.getString(PKG, "TransGraph.ExecutionResultsPanel.MaxButton.Tooltip"));
    } else {
      // Maximize
      //
      sashForm.setMaximizedControl(extraViewComposite);
      minMaxButton.setImage(GUIResource.getInstance().getImageMinimizePanel());
      minMaxButton.setToolTipText(BaseMessages.getString(PKG, "TransGraph.ExecutionResultsPanel.MinButton.Tooltip"));
    }
  }

  /**
   * @return the toolbar
   */
  public XulToolbar getToolbar() {
    return toolbar;
  }

  /**
   * @param toolbar the toolbar to set
   */
  public void setToolbar(XulToolbar toolbar) {
    this.toolbar = toolbar;
  }

  private Label closeButton;

  private Label minMaxButton;

  /**
   * Add an extra view to the main composite SashForm
   */
  public void addExtraView() {
    extraViewComposite = new Composite(sashForm, SWT.NONE);
    FormLayout extraCompositeFormLayout = new FormLayout();
    extraCompositeFormLayout.marginWidth = 2;
    extraCompositeFormLayout.marginHeight = 2;
    extraViewComposite.setLayout(extraCompositeFormLayout);

    // Put a close and max button to the upper right corner...
    //
    closeButton = new Label(extraViewComposite, SWT.NONE);
    closeButton.setImage(GUIResource.getInstance().getImageClosePanel());
    closeButton.setToolTipText(BaseMessages.getString(PKG, "TransGraph.ExecutionResultsPanel.CloseButton.Tooltip"));
    FormData fdClose = new FormData();
    fdClose.right = new FormAttachment(100, 0);
    fdClose.top = new FormAttachment(0, 0);
    closeButton.setLayoutData(fdClose);
    closeButton.addMouseListener(new MouseAdapter() {
      public void mouseDown(MouseEvent e) {
        disposeExtraView();
      }
    });

    minMaxButton = new Label(extraViewComposite, SWT.NONE);
    minMaxButton.setImage(GUIResource.getInstance().getImageMaximizePanel());
    minMaxButton.setToolTipText(BaseMessages.getString(PKG, "TransGraph.ExecutionResultsPanel.MaxButton.Tooltip"));
    FormData fdMinMax = new FormData();
    fdMinMax.right = new FormAttachment(closeButton, -Const.MARGIN);
    fdMinMax.top = new FormAttachment(0, 0);
    minMaxButton.setLayoutData(fdMinMax);
    minMaxButton.addMouseListener(new MouseAdapter() {
      public void mouseDown(MouseEvent e) {
        minMaxExtraView();
      }
    });

    // Add a label at the top: Results
    //
    Label wResultsLabel = new Label(extraViewComposite, SWT.LEFT);
    wResultsLabel.setFont(GUIResource.getInstance().getFontMediumBold());
    wResultsLabel.setBackground(GUIResource.getInstance().getColorLightGray());
    wResultsLabel.setText(BaseMessages.getString(PKG, "TransLog.ResultsPanel.NameLabel"));
    FormData fdResultsLabel = new FormData();
    fdResultsLabel.left = new FormAttachment(0, 0);
    fdResultsLabel.right = new FormAttachment(minMaxButton, -Const.MARGIN);
    fdResultsLabel.top = new FormAttachment(0, 0);
    wResultsLabel.setLayoutData(fdResultsLabel);

    // Add a tab folder ...
    //
    extraViewTabFolder = new CTabFolder(extraViewComposite, SWT.MULTI);
    spoon.props.setLook(extraViewTabFolder, Props.WIDGET_STYLE_TAB);

    extraViewTabFolder.addMouseListener(new MouseAdapter() {

      @Override
      public void mouseDoubleClick(MouseEvent arg0) {
        if (sashForm.getMaximizedControl() == null) {
          sashForm.setMaximizedControl(extraViewComposite);
        } else {
          sashForm.setMaximizedControl(null);
        }
      }

    });

    FormData fdTabFolder = new FormData();
    fdTabFolder.left = new FormAttachment(0, 0);
    fdTabFolder.right = new FormAttachment(100, 0);
    fdTabFolder.top = new FormAttachment(wResultsLabel, Const.MARGIN);
    fdTabFolder.bottom = new FormAttachment(100, 0);
    extraViewTabFolder.setLayoutData(fdTabFolder);

    sashForm.setWeights(new int[] { 60, 40, });
  }

  public void checkErrors() {
    if (trans != null) {
      if (!trans.isFinished()) {
        if (trans.getErrors() != 0) {
          trans.killAll();
        }
      }
    }
  }

  public synchronized void start(TransExecutionConfiguration executionConfiguration) throws KettleException {
    if (!running) // Not running, start the transformation...
    {
      // Auto save feature...
      if (transMeta.hasChanged()) {
        if (spoon.props.getAutoSave()) {
          spoon.saveToFile(transMeta);
        } else {
          MessageDialogWithToggle md = new MessageDialogWithToggle(
              shell,
              BaseMessages.getString(PKG, "TransLog.Dialog.FileHasChanged.Title"), //$NON-NLS-1$
              null,
              BaseMessages.getString(PKG, "TransLog.Dialog.FileHasChanged1.Message") + Const.CR + BaseMessages.getString(PKG, "TransLog.Dialog.FileHasChanged2.Message") + Const.CR, //$NON-NLS-1$ //$NON-NLS-2$
              MessageDialog.QUESTION, new String[] {
                  BaseMessages.getString(PKG, "System.Button.Yes"), BaseMessages.getString(PKG, "System.Button.No") }, //$NON-NLS-1$ //$NON-NLS-2$
              0, BaseMessages.getString(PKG, "TransLog.Dialog.Option.AutoSaveTransformation"), //$NON-NLS-1$
              spoon.props.getAutoSave());
          int answer = md.open();
          if ((answer & 0xFF) == 0) {
            spoon.saveToFile(transMeta);
          }
          spoon.props.setAutoSave(md.getToggleState());
        }
      }

      if (((transMeta.getName() != null && spoon.rep != null) || // Repository available & name set
          (transMeta.getFilename() != null && spoon.rep == null) // No repository & filename set
          )
          && !transMeta.hasChanged() // Didn't change
      ) {
        if (trans == null || (trans != null && trans.isFinished())) {
          try {
            // Set the requested logging level.
            LogWriter.getInstance().setLogLevel(executionConfiguration.getLogLevel());

            transMeta.injectVariables(executionConfiguration.getVariables());
            
            // Set the named parameters
            Map<String, String> paramMap = executionConfiguration.getParams();
            Set<String> keys = paramMap.keySet();
            for ( String key : keys )  {
            	transMeta.setParameterValue(key, Const.NVL(paramMap.get(key), ""));
            }
            
            transMeta.activateParameters();
            
            // Do we need to clear the log before running?
            //
            if (executionConfiguration.isClearingLog()) {
            	transLogDelegate.clearLog();
            }
            
            // Also make sure to clear the log entries in the central log store & registry 
            //
			if (trans!=null) {
				CentralLogStore.discardLines(trans.getLogChannelId(), true);
			}
            
            // Important: even though transMeta is passed to the Trans constructor, it is not the same object as is in memory
            // To be able to completely test this, we need to run it as we would normally do in pan
            //
            trans = new Trans(transMeta, spoon.rep, transMeta.getName(), transMeta.getRepositoryDirectory().getPath(), transMeta.getFilename());
            trans.setReplayDate(executionConfiguration.getReplayDate());
            trans.setRepository(executionConfiguration.getRepository());
            trans.setMonitored(true);
            log.logBasic(BaseMessages.getString(PKG, "TransLog.Log.TransformationOpened")); //$NON-NLS-1$
          } catch (KettleException e) {
            trans = null;
            new ErrorDialog(
                shell,
                BaseMessages.getString(PKG, "TransLog.Dialog.ErrorOpeningTransformation.Title"), BaseMessages.getString(PKG, "TransLog.Dialog.ErrorOpeningTransformation.Message"), e); //$NON-NLS-1$ //$NON-NLS-2$
          }
          if (trans != null) {
            Map<String, String> arguments = executionConfiguration.getArguments();
            final String args[];
            if (arguments != null)
              args = convertArguments(arguments);
            else
              args = null;

            log.logMinimal(BaseMessages.getString(PKG, "TransLog.Log.LaunchingTransformation") + trans.getTransMeta().getName() + "]..."); //$NON-NLS-1$ //$NON-NLS-2$
            
            trans.setSafeModeEnabled(executionConfiguration.isSafeModeEnabled());

            // Launch the step preparation in a different thread. 
            // That way Spoon doesn't block anymore and that way we can follow the progress of the initialization
            //
            final Thread parentThread = Thread.currentThread();

            shell.getDisplay().asyncExec(new Runnable() {
              public void run() {
                addAllTabs();
                prepareTrans(parentThread, args);
              }
            });

            log.logMinimal(BaseMessages.getString(PKG, "TransLog.Log.StartedExecutionOfTransformation")); //$NON-NLS-1$

            setControlStates();
          }
        } else {
          MessageBox m = new MessageBox(shell, SWT.OK | SWT.ICON_WARNING);
          m.setText(BaseMessages.getString(PKG, "TransLog.Dialog.DoNoStartTransformationTwice.Title")); //$NON-NLS-1$
          m.setMessage(BaseMessages.getString(PKG, "TransLog.Dialog.DoNoStartTransformationTwice.Message")); //$NON-NLS-1$
          m.open();
        }
      } else {
        if (transMeta.hasChanged()) {
          MessageBox m = new MessageBox(shell, SWT.OK | SWT.ICON_WARNING);
          m.setText(BaseMessages.getString(PKG, "TransLog.Dialog.SaveTransformationBeforeRunning.Title")); //$NON-NLS-1$
          m.setMessage(BaseMessages.getString(PKG, "TransLog.Dialog.SaveTransformationBeforeRunning.Message")); //$NON-NLS-1$
          m.open();
        } else if (spoon.rep != null && transMeta.getName() == null) {
          MessageBox m = new MessageBox(shell, SWT.OK | SWT.ICON_WARNING);
          m.setText(BaseMessages.getString(PKG, "TransLog.Dialog.GiveTransformationANameBeforeRunning.Title")); //$NON-NLS-1$
          m.setMessage(BaseMessages.getString(PKG, "TransLog.Dialog.GiveTransformationANameBeforeRunning.Message")); //$NON-NLS-1$
          m.open();
        } else {
          MessageBox m = new MessageBox(shell, SWT.OK | SWT.ICON_WARNING);
          m.setText(BaseMessages.getString(PKG, "TransLog.Dialog.SaveTransformationBeforeRunning2.Title")); //$NON-NLS-1$
          m.setMessage(BaseMessages.getString(PKG, "TransLog.Dialog.SaveTransformationBeforeRunning2.Message")); //$NON-NLS-1$
          m.open();
        }
      }
    }
  }

  public void addAllTabs() {

	CTabItem tabItemSelection = null;
	if (extraViewTabFolder!=null && !extraViewTabFolder.isDisposed()) {
		tabItemSelection = extraViewTabFolder.getSelection();
	}

    transHistoryDelegate.addTransHistory();
    transLogDelegate.addTransLog();
    transGridDelegate.addTransGrid();
    transPerfDelegate.addTransPerf();
    
    if (tabItemSelection!=null) {
    	extraViewTabFolder.setSelection(tabItemSelection);
    } else {
    	extraViewTabFolder.setSelection(transGridDelegate.getTransGridTab());
    }
    
    XulToolbarButton button = toolbar.getButtonById("trans-show-results");
    button.setImage(GUIResource.getInstance().getImageHideResults());
    button.setHint(BaseMessages.getString(PKG, "Spoon.Tooltip.HideExecutionResults"));

  }

  public synchronized void debug(TransExecutionConfiguration executionConfiguration, TransDebugMeta transDebugMeta) {
    if (!running) {
      try {
        this.lastTransDebugMeta = transDebugMeta;

        LogWriter.getInstance().setLogLevel(executionConfiguration.getLogLevel());
        if(log.isDetailed()) log.logDetailed(BaseMessages.getString(PKG, "TransLog.Log.DoPreview")); //$NON-NLS-1$
        String[] args = null;
        Map<String, String> arguments = executionConfiguration.getArguments();
        if (arguments != null) {
          args = convertArguments(arguments);
        }
        transMeta.injectVariables(executionConfiguration.getVariables());
        
        // Set the named parameters
        Map<String, String> paramMap = executionConfiguration.getParams();
        Set<String> keys = paramMap.keySet();
        for ( String key : keys )  {
        	transMeta.setParameterValue(key, Const.NVL(paramMap.get(key), ""));
        }
        
        transMeta.activateParameters();        

        // Do we need to clear the log before running?
        //
        if (executionConfiguration.isClearingLog()) {
        	transLogDelegate.clearLog();
        }
        
        // Create a new transformation to execution
        //
        trans = new Trans(transMeta);
        trans.setSafeModeEnabled(executionConfiguration.isSafeModeEnabled());
        trans.setPreview(true);
        trans.prepareExecution(args);

        // Add the row listeners to the allocated threads
        //
        transDebugMeta.addRowListenersToTransformation(trans);

        // What method should we call back when a break-point is hit?
        //
        transDebugMeta.addBreakPointListers(new BreakPointListener() {
          public void breakPointHit(TransDebugMeta transDebugMeta, StepDebugMeta stepDebugMeta,
              RowMetaInterface rowBufferMeta, List<Object[]> rowBuffer) {
            showPreview(transDebugMeta, stepDebugMeta, rowBufferMeta, rowBuffer);
          }
        });

        // Start the threads for the steps...
        //
        startThreads();

        debug = true;

        // Show the execution results view...
        //
        shell.getDisplay().asyncExec(new Runnable() {
          public void run() {
            addAllTabs();
          }
        });
      } catch (Exception e) {
        new ErrorDialog(
            shell,
            BaseMessages.getString(PKG, "TransLog.Dialog.UnexpectedErrorDuringPreview.Title"), BaseMessages.getString(PKG, "TransLog.Dialog.UnexpectedErrorDuringPreview.Message"), e); //$NON-NLS-1$ //$NON-NLS-2$
      }
    } else {
      MessageBox m = new MessageBox(shell, SWT.OK | SWT.ICON_WARNING);
      m.setText(BaseMessages.getString(PKG, "TransLog.Dialog.DoNoPreviewWhileRunning.Title")); //$NON-NLS-1$
      m.setMessage(BaseMessages.getString(PKG, "TransLog.Dialog.DoNoPreviewWhileRunning.Message")); //$NON-NLS-1$
      m.open();
    }
    checkErrorVisuals();
  }

  public synchronized void showPreview(final TransDebugMeta transDebugMeta, final StepDebugMeta stepDebugMeta,
      final RowMetaInterface rowBufferMeta, final List<Object[]> rowBuffer) {
    shell.getDisplay().asyncExec(new Runnable() {

      public void run() {

        if (isDisposed())
          return;

        spoon.enableMenus();

        // The transformation is now paused, indicate this in the log dialog...
        //
        pausing = true;

        setControlStates();
        checkErrorVisuals();

        PreviewRowsDialog previewRowsDialog = new PreviewRowsDialog(shell, transMeta, SWT.APPLICATION_MODAL,
            stepDebugMeta.getStepMeta().getName(), rowBufferMeta, rowBuffer);
        previewRowsDialog.setProposingToGetMoreRows(true);
        previewRowsDialog.setProposingToStop(true);
        previewRowsDialog.open();

        if (previewRowsDialog.isAskingForMoreRows()) {
          // clear the row buffer.
          // That way if you click resume, you get the next N rows for the step :-)
          //
          rowBuffer.clear();

          // Resume running: find more rows...
          //
          pauseResume();
        }

        if (previewRowsDialog.isAskingToStop()) {
          // Stop running
          //
          stop();
        }

      }

    });
  }

  private String[] convertArguments(Map<String, String> arguments) {
    String[] argumentNames = arguments.keySet().toArray(new String[arguments.size()]);
    Arrays.sort(argumentNames);

    String args[] = new String[argumentNames.length];
    for (int i = 0; i < args.length; i++) {
      String argumentName = argumentNames[i];
      args[i] = arguments.get(argumentName);
    }
    return args;
  }

  public void stop() {
    if (running && !halting) {
      halting = true;
      trans.stopAll();
      log.logMinimal(BaseMessages.getString(PKG, "TransLog.Log.ProcessingOfTransformationStopped")); //$NON-NLS-1$

      running = false;
      initialized = false;
      halted = false;
      halting = false;

      setControlStates();

      transMeta.setInternalKettleVariables(); // set the original vars back as they may be changed by a mapping
    }
  }

  public synchronized void pauseResume() {
    if (running) {
      // Get the pause toolbar item
      //
      if (!pausing) {
        pausing = true;
        trans.pauseRunning();
        setControlStates();
      } else {
        pausing = false;
        trans.resumeRunning();
        setControlStates();
      }
    }
  }

  public synchronized void setControlStates() {
	if (this.isDisposed()) return;
	if (getDisplay().isDisposed()) return;
	
    getDisplay().asyncExec(new Runnable() {

      public void run() {
        // Start/Run button...
        //
        XulToolbarButton runButton = toolbar.getButtonById("trans-run");
        if (runButton != null) {
          runButton.setEnable(!running);
        }

        // Pause button...
        //
        XulToolbarButton pauseButton = toolbar.getButtonById("trans-pause");
        if (pauseButton != null) {
          pauseButton.setEnable(running);
          pauseButton.setText(pausing ? RESUME_TEXT : PAUSE_TEXT);
          pauseButton.setHint(pausing ? BaseMessages.getString(PKG, "Spoon.Tooltip.ResumeTranformation") : 
        	  BaseMessages.getString(PKG, "Spoon.Tooltip.PauseTranformation"));
        }

        // Stop button...
        //
        XulToolbarButton stopButton = toolbar.getButtonById("trans-stop");
        if (stopButton != null) {
          stopButton.setEnable(running);
        }

        // Debug button...
        //
        XulToolbarButton debugButton = toolbar.getButtonById("trans-debug");
        if (debugButton != null) {
          debugButton.setEnable(!running);
        }

        // Preview button...
        //
        XulToolbarButton previewButton = toolbar.getButtonById("trans-preview");
        if (previewButton != null) {
          previewButton.setEnable(!running);
        }

        // version browser button...
        //
        XulToolbarButton versionsButton = toolbar.getButtonById("browse-versions");
        if (versionsButton != null) {
          boolean hasRepository = spoon.rep!=null;
          boolean enabled = hasRepository && spoon.rep.getRepositoryMeta().getRepositoryCapabilities().supportsRevisions(); 
          versionsButton.setEnable(enabled);
        }

      }

    });

  }

  private synchronized void prepareTrans(final Thread parentThread, final String[] args) {
    Runnable runnable = new Runnable() {
      public void run() {
        try {
          trans.prepareExecution(args);
          initialized = true;
        } catch (KettleException e) {
          log.logError(trans.getName(), "Preparing transformation execution failed", e);
          initialized = false;
      	  checkErrorVisuals();
        }
        halted = trans.hasHaltedSteps();
        checkStartThreads();// After init, launch the threads.
      }
    };
    Thread thread = new Thread(runnable);
    thread.start();
  }

  private void checkStartThreads() {
    if (initialized && !running && trans != null) {
      startThreads();
    }
  }

  private synchronized void startThreads() {
    running = true;
    try {
      // Add a listener to the transformation.
      // If the transformation is done, we want to do the end processing, etc.
      //
      trans.addTransListener(new TransListener() {

        public void transFinished(Trans trans) {
          checkTransEnded();
          // checkErrors();
        }
      });

      trans.startThreads();

      setControlStates();
    } catch (KettleException e) {
      log.logError("Error starting step threads", e);
  	  checkErrorVisuals();
    }

    // See if we have to fire off the performance graph updater etc.
    //
    getDisplay().asyncExec(new Runnable() {
      public void run() {
        if (transPerfDelegate.getTransPerfTab() != null) {
          // If there is a tab open, try to the correct content on there now
          //
          transPerfDelegate.setupContent();
          transPerfDelegate.layoutPerfComposite();
        }
      }
    });
  }
  
  private String lastLog;

  private void checkTransEnded() {
		if (trans != null) {
			if (trans.isFinished() && (running || halted)) {
				log.logMinimal(BaseMessages.getString(PKG, "TransLog.Log.TransformationHasFinished")); //$NON-NLS-1$

				running = false;
				initialized = false;
				halted = false;
				halting = false;

				if (spoonHistoryRefresher != null)
					spoonHistoryRefresher.markRefreshNeeded();

				setControlStates();

				// OK, also see if we had a debugging session going on.
				// If so and we didn't hit a breakpoint yet, display the show
				// preview dialog...
				//
				if (debug && lastTransDebugMeta != null && lastTransDebugMeta.getTotalNumberOfHits() == 0) {
					debug = false;
					showLastPreviewResults();
				}
				debug = false;

				checkErrorVisuals();

				shell.getDisplay().asyncExec(new Runnable() {
					public void run() {
						redraw();
					}
				});
			}
		}
	}

  private void checkErrorVisuals() {
      if (trans.getErrors()>0) {
      	// Get the logging text and filter it out.  Store it in the stepLogMap...
      	//
          stepLogMap = new HashMap<StepMeta, String>();
          lastLog = null;
          shell.getDisplay().syncExec(new Runnable() {
			
				public void run() {
					lastLog = transLogDelegate.getLoggingText();
				}
			});
          
          if (!Const.isEmpty(lastLog)) {
          	String lines[] = lastLog.split(Const.CR);
          	for (int i=0;i<lines.length && i<30;i++) {
          		if (lines[i].indexOf(Log4jKettleLayout.ERROR_STRING)>=0) {
          			// This is an error line, log it!
          			// Determine to which step it belongs!
          			//
          			for (StepMeta stepMeta : transMeta.getSteps()) {
          				if (lines[i].indexOf(stepMeta.getName())>=0) {
          					
          					String line = lines[i];
          					int index = lines[i].indexOf(") : "); // $NON-NLS-1$   TODO: define this as a more generic marker / constant value
          					if (index>0) line=lines[i].substring(index+3);
          					
          					String str = stepLogMap.get(stepMeta);
          					if (str==null) {
          						stepLogMap.put(stepMeta, line);
          					} else {
          						stepLogMap.put(stepMeta, str+Const.CR+line);
          					}
          				}
          			}
          		}
          	}
          }
      } else {
      	stepLogMap = null;
      } 
      // Redraw the canvas to show the error icons etc.
      //
      shell.getDisplay().asyncExec(new Runnable() { public void run() { redraw(); } });
  }

  public synchronized void showLastPreviewResults() {
    if (lastTransDebugMeta == null || lastTransDebugMeta.getStepDebugMetaMap().isEmpty())
      return;

    final List<String> stepnames = new ArrayList<String>();
    final List<RowMetaInterface> rowMetas = new ArrayList<RowMetaInterface>();
    final List<List<Object[]>> rowBuffers = new ArrayList<List<Object[]>>();

    // Assemble the buffers etc in the old style...
    //
    for (StepMeta stepMeta : lastTransDebugMeta.getStepDebugMetaMap().keySet()) {
      StepDebugMeta stepDebugMeta = lastTransDebugMeta.getStepDebugMetaMap().get(stepMeta);

      stepnames.add(stepMeta.getName());
      rowMetas.add(stepDebugMeta.getRowBufferMeta());
      rowBuffers.add(stepDebugMeta.getRowBuffer());
    }

    getDisplay().asyncExec(new Runnable() {
      public void run() {
        EnterPreviewRowsDialog dialog = new EnterPreviewRowsDialog(shell, SWT.NONE, stepnames, rowMetas, rowBuffers);
        dialog.open();
      }
    });
  }
  
  /**
   * Open the transformation mentioned in the mapping...
   */
  public void openMapping() {
	  try {
		  MappingMeta meta = (MappingMeta) this.currentStep.getStepMetaInterface();
		  TransMeta mappingMeta = MappingMeta.loadMappingMeta(meta.getFileName(), meta.getTransName(), meta.getDirectoryPath(), spoon.rep, transMeta);
		  mappingMeta.clearChanged();
		  spoon.addTransGraph(mappingMeta);
	  } catch(Exception e) {
		  new ErrorDialog(shell, BaseMessages.getString(PKG, "TransGraph.Exception.UnableToLoadMapping.Title"), BaseMessages.getString(PKG, "TransGraph.Exception.UnableToLoadMapping.Message"), e);
	  }
  }

  /**
   * @return the running
   */
  public boolean isRunning() {
    return running;
  }

  /**
   * @param running the running to set
   */
  public void setRunning(boolean running) {
    this.running = running;
  }

  /**
   * @return the lastTransDebugMeta
   */
  public TransDebugMeta getLastTransDebugMeta() {
    return lastTransDebugMeta;
  }

  /**
   * @return the halting
   */
  public boolean isHalting() {
    return halting;
  }

  /**
   * @param halting the halting to set
   */
  public void setHalting(boolean halting) {
    this.halting = halting;
  }

	/**
	 * @return the stepLogMap
	 */
	public Map<StepMeta, String> getStepLogMap() {
		return stepLogMap;
	}

	/**
	 * @param stepLogMap
	 *            the stepLogMap to set
	 */
	public void setStepLogMap(Map<StepMeta, String> stepLogMap) {
		this.stepLogMap = stepLogMap;
	}

	public void dumpLoggingRegistry() {
		LoggingRegistry registry = LoggingRegistry.getInstance();
		Map<String, LoggingObjectInterface> loggingMap = registry.getMap();
		
		for (LoggingObjectInterface loggingObject : loggingMap.values()) {
			System.out.println(loggingObject.getLogChannelId()+" - "+loggingObject.getObjectName()+" - "+loggingObject.getObjectType());
		}
		
	}

	public HasLogChannelInterface getLogChannelProvider() {
		return trans;
	}
}