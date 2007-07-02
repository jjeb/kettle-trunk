 /**********************************************************************
 **                                                                   **
 **               This code belongs to the KETTLE project.            **
 **                                                                   **
 ** Kettle, from version 2.2 on, is released into the public domain   **
 ** under the Lesser GNU Public License (LGPL).                       **
 **                                                                   **
 ** For more details, please read the document LICENSE.txt, included  **
 ** in this project                                                   **
 **                                                                   **
 ** http://www.kettle.be                                              **
 ** info@kettle.be                                                    **
 **                                                                   **
 **********************************************************************/
 
package org.pentaho.di.spoon;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.vfs.FileObject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSource;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DragSourceListener;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.ExpandAdapter;
import org.eclipse.swt.events.ExpandEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.printing.Printer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.ExpandBar;
import org.eclipse.swt.widgets.ExpandItem;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.pentaho.di.cluster.ClusterSchema;
import org.pentaho.di.cluster.SlaveServer;
import org.pentaho.di.cluster.dialog.ClusterSchemaDialog;
import org.pentaho.di.cluster.dialog.SlaveServerDialog;
import org.pentaho.di.core.AddUndoPositionInterface;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.DBCache;
import org.pentaho.di.core.EngineMetaInterface;
import org.pentaho.di.core.LastUsedFile;
import org.pentaho.di.core.NotePadMeta;
import org.pentaho.di.core.ObjectUsageCount;
import org.pentaho.di.core.PrintSpool;
import org.pentaho.di.core.Props;
import org.pentaho.di.core.RowMetaAndData;
import org.pentaho.di.core.SQLStatement;
import org.pentaho.di.core.SourceToTargetMapping;
import org.pentaho.di.core.changed.ChangedFlagInterface;
import org.pentaho.di.core.clipboard.ImageDataTransfer;
import org.pentaho.di.core.database.Database;
import org.pentaho.di.core.database.DatabaseMeta;
import org.pentaho.di.core.database.dialog.DatabaseDialog;
import org.pentaho.di.core.database.dialog.DatabaseExplorerDialog;
import org.pentaho.di.core.database.dialog.SQLEditor;
import org.pentaho.di.core.database.wizard.CreateDatabaseWizard;
import org.pentaho.di.core.dialog.CheckResultDialog;
import org.pentaho.di.core.dialog.EnterMappingDialog;
import org.pentaho.di.core.dialog.EnterOptionsDialog;
import org.pentaho.di.core.dialog.EnterSearchDialog;
import org.pentaho.di.core.dialog.EnterSelectionDialog;
import org.pentaho.di.core.dialog.EnterStringDialog;
import org.pentaho.di.core.dialog.EnterStringsDialog;
import org.pentaho.di.core.dialog.ErrorDialog;
import org.pentaho.di.core.dialog.PreviewRowsDialog;
import org.pentaho.di.core.dialog.SQLStatementsDialog;
import org.pentaho.di.core.dialog.ShowBrowserDialog;
import org.pentaho.di.core.dialog.ShowMessageDialog;
import org.pentaho.di.core.dialog.Splash;
import org.pentaho.di.core.dnd.DragAndDropContainer;
import org.pentaho.di.core.dnd.XMLTransfer;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.exception.KettleRowException;
import org.pentaho.di.core.exception.KettleStepException;
import org.pentaho.di.core.exception.KettleValueException;
import org.pentaho.di.core.gui.GUIResource;
import org.pentaho.di.core.gui.Point;
import org.pentaho.di.core.gui.SpoonFactory;
import org.pentaho.di.core.gui.SpoonInterface;
import org.pentaho.di.core.gui.UndoInterface;
import org.pentaho.di.core.gui.WindowProperty;
import org.pentaho.di.core.logging.LogWriter;
import org.pentaho.di.core.reflection.StringSearchResult;
import org.pentaho.di.core.row.RowMeta;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.row.ValueMeta;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.core.undo.TransAction;
import org.pentaho.di.core.util.EnvUtil;
import org.pentaho.di.core.util.ImageUtil;
import org.pentaho.di.core.variables.KettleVariables;
import org.pentaho.di.core.variables.VariableSpace;
import org.pentaho.di.core.variables.Variables;
import org.pentaho.di.core.vfs.KettleVFS;
import org.pentaho.di.core.widget.TreeMemory;
import org.pentaho.di.core.xml.XMLHandler;
import org.pentaho.di.i18n.LanguageChoice;
import org.pentaho.di.job.JobEntryLoader;
import org.pentaho.di.job.JobHopMeta;
import org.pentaho.di.job.JobMeta;
import org.pentaho.di.job.JobPlugin;
import org.pentaho.di.job.dialog.JobLoadProgressDialog;
import org.pentaho.di.job.dialog.JobSaveProgressDialog;
import org.pentaho.di.job.entries.special.JobEntrySpecial;
import org.pentaho.di.job.entries.sql.JobEntrySQL;
import org.pentaho.di.job.entries.trans.JobEntryTrans;
import org.pentaho.di.job.entry.JobEntryCopy;
import org.pentaho.di.job.entry.JobEntryDialogInterface;
import org.pentaho.di.job.entry.JobEntryInterface;
import org.pentaho.di.pan.CommandLineOption;
import org.pentaho.di.partition.PartitionSchema;
import org.pentaho.di.partition.dialog.PartitionSchemaDialog;
import org.pentaho.di.pkg.JarfileGenerator;
import org.pentaho.di.repository.PermissionMeta;
import org.pentaho.di.repository.RepositoriesMeta;
import org.pentaho.di.repository.Repository;
import org.pentaho.di.repository.RepositoryDirectory;
import org.pentaho.di.repository.RepositoryMeta;
import org.pentaho.di.repository.RepositoryObject;
import org.pentaho.di.repository.UserInfo;
import org.pentaho.di.repository.dialog.RepositoriesDialog;
import org.pentaho.di.repository.dialog.RepositoryExplorerDialog;
import org.pentaho.di.repository.dialog.SelectObjectDialog;
import org.pentaho.di.repository.dialog.UserDialog;
import org.pentaho.di.shared.SharedObjectInterface;
import org.pentaho.di.spoon.dialog.AnalyseImpactProgressDialog;
import org.pentaho.di.spoon.dialog.CheckTransProgressDialog;
import org.pentaho.di.spoon.dialog.GetJobSQLProgressDialog;
import org.pentaho.di.spoon.dialog.GetSQLProgressDialog;
import org.pentaho.di.spoon.dialog.SaveProgressDialog;
import org.pentaho.di.spoon.dialog.ShowCreditsDialog;
import org.pentaho.di.spoon.dialog.TipsDialog;
import org.pentaho.di.spoon.job.JobGraph;
import org.pentaho.di.spoon.job.JobHistory;
import org.pentaho.di.spoon.job.JobHistoryRefresher;
import org.pentaho.di.spoon.job.JobLog;
import org.pentaho.di.spoon.trans.TransGraph;
import org.pentaho.di.spoon.trans.TransHistory;
import org.pentaho.di.spoon.trans.TransHistoryRefresher;
import org.pentaho.di.spoon.trans.TransLog;
import org.pentaho.di.spoon.wizards.CopyTableWizardPage1;
import org.pentaho.di.spoon.wizards.CopyTableWizardPage2;
import org.pentaho.di.spoon.wizards.RipDatabaseWizardPage1;
import org.pentaho.di.spoon.wizards.RipDatabaseWizardPage2;
import org.pentaho.di.spoon.wizards.RipDatabaseWizardPage3;
import org.pentaho.di.trans.DatabaseImpact;
import org.pentaho.di.trans.HasDatabasesInterface;
import org.pentaho.di.trans.StepLoader;
import org.pentaho.di.trans.StepPlugin;
import org.pentaho.di.trans.TransConfiguration;
import org.pentaho.di.trans.TransExecutionConfiguration;
import org.pentaho.di.trans.TransHopMeta;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.cluster.TransSplitter;
import org.pentaho.di.trans.dialog.TransExecutionConfigurationDialog;
import org.pentaho.di.trans.dialog.TransHopDialog;
import org.pentaho.di.trans.dialog.TransLoadProgressDialog;
import org.pentaho.di.trans.step.BaseStep;
import org.pentaho.di.trans.step.StepDialogInterface;
import org.pentaho.di.trans.step.StepErrorMeta;
import org.pentaho.di.trans.step.StepErrorMetaDialog;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.step.StepMetaInterface;
import org.pentaho.di.trans.step.StepPartitioningMeta;
import org.pentaho.di.trans.steps.selectvalues.SelectValuesMeta;
import org.pentaho.di.trans.steps.tableinput.TableInputMeta;
import org.pentaho.di.trans.steps.tableoutput.TableOutputMeta;
import org.pentaho.di.version.BuildVersion;
import org.pentaho.di.www.AddTransServlet;
import org.pentaho.di.www.PrepareExecutionTransServlet;
import org.pentaho.di.www.StartExecutionTransServlet;
import org.pentaho.di.www.WebResult;
import org.pentaho.vfs.ui.VfsFileChooserDialog;
import org.pentaho.xul.menu.XulMenu;
import org.pentaho.xul.menu.XulMenuBar;
import org.pentaho.xul.menu.XulMenuItem;
import org.pentaho.xul.menu.XulPopupMenu;
import org.pentaho.xul.swt.menu.Menu;
import org.pentaho.xul.swt.menu.MenuChoice;
import org.pentaho.xul.swt.menu.MenuHelper;
import org.pentaho.xul.swt.menu.PopupMenu;
import org.pentaho.xul.swt.tab.TabItem;
import org.pentaho.xul.swt.tab.TabListener;
import org.pentaho.xul.swt.tab.TabSet;
import org.pentaho.xul.toolbar.XulToolbar;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * This class handles the main window of the Spoon graphical transformation editor.
 * 
 * @author Matt
 * @since 16-may-2003, i18n at 07-Feb-2006, redesign 01-Dec-2006
 */
public class Spoon implements AddUndoPositionInterface, TabListener, SpoonInterface
{
    public static final String APP_NAME = Messages.getString("Spoon.Application.Name");  //"Spoon";
    
    private static Spoon staticSpoon;
    
    private LogWriter log;
    private Display display;
    private Shell shell;
    private boolean destroy;
    
    private SashForm sashform;
    public TabSet tabfolder;
    
    public boolean shift;
    public boolean control;

    public RowMetaAndData variables;
    
    /**
     * These are the arguments that were given at Spoon launch time...
     */
    private String[] arguments;
    
    private boolean stopped;
    
    private Cursor cursor_hourglass, cursor_hand;
    
    public  Props props;
    
    public  Repository rep;
        
    /** 
     * This contains a map between the name of a transformation and the TransMeta object.
     * If the transformation has no name it will be mapped under a number [1], [2] etc. 
     */
    private Map<String,TransMeta> transformationMap;

    /** 
     * This contains a map between the name of a transformation and the TransMeta object.
     * If the transformation has no name it will be mapped under a number [1], [2] etc. 
     */
    private Map<String,JobMeta> jobMap;

    /**
     * This contains a map between the name of the tab name and the object name and type 
     */
    private Map<String,TabMapEntry> tabMap;
    
    /**
     * This contains a map with all the unnamed transformation (just a filename)
     */

    private XulToolbar toolbar;

    private XulMenuBar menuBar;
    
    private Tree selectionTree;
    // private TreeItem  tiTransBase, tiJobBase;

    private Tree coreObjectsTree;    
    
    public static final String STRING_TRANSFORMATIONS = Messages.getString("Spoon.STRING_TRANSFORMATIONS"); // Transformations
    public static final String STRING_JOBS            = Messages.getString("Spoon.STRING_JOBS");            // Jobs
    public static final String STRING_BUILDING_BLOCKS = Messages.getString("Spoon.STRING_BUILDING_BLOCKS"); // Building blocks
    public static final String STRING_ELEMENTS        = Messages.getString("Spoon.STRING_ELEMENTS");        // Model elements
    public static final String STRING_CONNECTIONS     = Messages.getString("Spoon.STRING_CONNECTIONS");     // Connections
    public static final String STRING_STEPS           = Messages.getString("Spoon.STRING_STEPS");           // Steps
    public static final String STRING_JOB_ENTRIES     = Messages.getString("Spoon.STRING_JOB_ENTRIES");     // Job entries
    public static final String STRING_HOPS            = Messages.getString("Spoon.STRING_HOPS");            // Hops
    public static final String STRING_PARTITIONS      = Messages.getString("Spoon.STRING_PARTITIONS");      // Database Partition schemas
    public static final String STRING_SLAVES          = Messages.getString("Spoon.STRING_SLAVES");          // Slave servers
    public static final String STRING_CLUSTERS        = Messages.getString("Spoon.STRING_CLUSTERS");        // Cluster Schemas
    public static final String STRING_TRANS_BASE      = Messages.getString("Spoon.STRING_BASE");            // Base step types
    public static final String STRING_JOB_BASE        = Messages.getString("Spoon.STRING_JOBENTRY_BASE");   // Base job entry types
    public static final String STRING_HISTORY         = Messages.getString("Spoon.STRING_HISTORY");         // Step creation history

    public static final String STRING_TRANS_NO_NAME   = Messages.getString("Spoon.STRING_TRANS_NO_NAME");   // <unnamed transformation>
    public static final String STRING_JOB_NO_NAME     = Messages.getString("Spoon.STRING_JOB_NO_NAME");     // <unnamed job>

    public static final String STRING_TRANSFORMATION  = Messages.getString("Spoon.STRING_TRANSFORMATION");  // Transformation
    public static final String STRING_JOB             = Messages.getString("Spoon.STRING_JOB");             // Job

    private static final String APPL_TITLE         = APP_NAME;

    private static final String STRING_WELCOME_TAB_NAME = Messages.getString("Spoon.Title.STRING_WELCOME"); 
    private static final String FILE_WELCOME_PAGE       = Messages.getString("Spoon.Title.STRING_DOCUMENT_WELCOME");  //"docs/English/welcome/kettle_document_map.html";
    
    public static final int STATE_CORE_OBJECTS_NONE     = 1;   // No core objects
    public static final int STATE_CORE_OBJECTS_CHEF     = 2;   // Chef state: job entries
    public static final int STATE_CORE_OBJECTS_SPOON    = 3;   // Spoon state: steps

	public static final String XUL_FILE_MENUBAR = "ui/menubar.xul";

	public static final String XUL_FILE_MENUS = "ui/menus.xul";

	public static final String XUL_FILE_MENU_PROPERTIES = "ui/menubar.properties";
            
    public  KeyAdapter defKeys;
    public  KeyAdapter modKeys;

//    private Menu mBar;

    private Composite tabComp;

    private ExpandBar mainExpandBar;
    private ExpandBar expandBar;
    
    private TransExecutionConfiguration executionConfiguration;

    // private TreeItem tiTrans, tiJobs;

    private Menu spoonMenu; // Connections, Steps & hops

    private int coreObjectsState = STATE_CORE_OBJECTS_NONE;
    
    private boolean stepHistoryChanged;
    
    protected Map<String,FileListener> fileExtensionMap = new HashMap<String,FileListener>();
    protected Map<String,FileListener> fileNodeMap = new HashMap<String,FileListener>();

    private List<Object[]> menuListeners = new ArrayList<Object[]>();

    public Spoon(Display d) {
    	this( null, d, null );
    }

    public Spoon(LogWriter l, Repository rep)
    {
        this(l, null, rep);
    }
    
    public Spoon(LogWriter log, Display d, Repository rep)
    {
        this.log        = log;
        this.rep = rep;
        
        if (d!=null) 
        {
            display=d;
            destroy=false;
        } 
        else 
        {
            display=new Display();
            destroy=true;
        } 
        shell=new Shell(display);
        shell.setText(APPL_TITLE);
        staticSpoon = this;
        SpoonFactory.setSpoonInstance( this );
    }
    
    public void init( TransMeta ti ) {
        FormLayout layout = new FormLayout();
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        shell.setLayout (layout);
        
        transformationMap = new Hashtable<String,TransMeta>();
        jobMap = new Hashtable<String,JobMeta>();
        tabMap = new Hashtable<String,TabMapEntry>();
        
        TransFileListener transListener = new TransFileListener();
        addFileListener( transListener, "ktr", TransMeta.XML_TAG );
        
        JobFileListener jobListener = new JobFileListener();
        addFileListener( jobListener, "kjb", JobMeta.XML_TAG );

        // INIT Data structure
        if (ti!=null)
        {
            addTransformation(ti);
        }
        
        if (!Props.isInitialized()) 
        {
            //log.logDetailed(toString(), "Load properties for Spoon...");
            log.logDetailed(toString(),Messages.getString("Spoon.Log.LoadProperties"));
            Props.init(display, Props.TYPE_PROPERTIES_SPOON);  // things to remember...
        }
        props=Props.getInstance();
        
        // Load settings in the props
        loadSettings();
        
        executionConfiguration = new TransExecutionConfiguration();
        
        // Clean out every time we start, auto-loading etc, is not a good idea
        // If they are needed that often, set them in the kettle.properties file
        //
        variables = new RowMetaAndData(new RowMeta(), new Object[] {}); 
        
        // props.setLook(shell);
        
        shell.setImage(GUIResource.getInstance().getImageSpoon());
        
        cursor_hourglass = new Cursor(display, SWT.CURSOR_WAIT);
        cursor_hand      = new Cursor(display, SWT.CURSOR_HAND);
        
        // widgets = new WidgetContainer();
        
        defKeys = new KeyAdapter() 
            {
                public void keyPressed(KeyEvent e) 
                {
                    
                    boolean ctrl = (( e.stateMask&SWT.CONTROL)!=0);
                    boolean alt  = (( e.stateMask&SWT.ALT)!=0);
                    
                    String key = null;
                    
                    switch( e.keyCode ) {
                    case SWT.ESC: key = "esc"; break; //$NON-NLS-1$
                    case SWT.F1: key = "f1"; break; //$NON-NLS-1$
                    case SWT.F2: key = "f2"; break; //$NON-NLS-1$
                    case SWT.F3: key = "f3"; break; //$NON-NLS-1$
                    case SWT.F4: key = "f4"; break; //$NON-NLS-1$
                    case SWT.F5: key = "f5"; break; //$NON-NLS-1$
                    case SWT.F6: key = "f6"; break; //$NON-NLS-1$
                    case SWT.F7: key = "f7"; break; //$NON-NLS-1$
                    case SWT.F8: key = "f8"; break; //$NON-NLS-1$
                    case SWT.F9: key = "f9"; break; //$NON-NLS-1$
                    case SWT.F10: key = "f10"; break; //$NON-NLS-1$
                    case SWT.F11: key = "f12"; break; //$NON-NLS-1$
                    case SWT.F12: key = "f12"; break; //$NON-NLS-1$
                    case SWT.ARROW_UP: key = "up"; break; //$NON-NLS-1$
                    case SWT.ARROW_DOWN: key = "down"; break; //$NON-NLS-1$
                    case SWT.ARROW_LEFT: key = "left"; break; //$NON-NLS-1$
                    case SWT.ARROW_RIGHT: key = "right"; break; //$NON-NLS-1$
                    case SWT.HOME: key = "home"; break; //$NON-NLS-1$
                    default: ;
                    }
                    
                    if( key == null && ctrl) {
                    		// get the character
                    		if(e.character >= '0' && e.character <= '9' ) {
                    			char c = e.character;
                    			key = new String( new char[] { c } );
                    		} else {
                        		char c = (char) ('a' + (e.character - 1) );
                        		key = new String( new char[] { c } );
                    		}
                    } else {
                			char c = e.character;
                			key = new String( new char[] { c } );
                    }
                    
                    menuBar.handleAccessKey( key, alt, ctrl );
                }
            };
        modKeys = new KeyAdapter() 
            {
                public void keyPressed(KeyEvent e) 
                {
                    shift = (e.keyCode == SWT.SHIFT  );
                    control = (e.keyCode == SWT.CONTROL);                    
                }

                public void keyReleased(KeyEvent e) 
                {
                    shift = (e.keyCode == SWT.SHIFT  );
                    control = (e.keyCode == SWT.CONTROL);                    
                }
            };

        
        addBar();

        

        sashform = new SashForm(shell, SWT.HORIZONTAL);
        props.setLook(sashform);
        
        FormData fdSash = new FormData();
        fdSash.left = new FormAttachment(0, 0);
        fdSash.top = new FormAttachment((org.eclipse.swt.widgets.ToolBar)toolbar.getNativeObject(), 0);
        fdSash.bottom = new FormAttachment(100, 0);
        fdSash.right  = new FormAttachment(100, 0);
        sashform.setLayoutData(fdSash);

        // Set the shell size, based upon previous time...
        WindowProperty winprop = props.getScreen(APPL_TITLE);
        if (winprop!=null) winprop.setShell(shell); 
        else 
        {
            shell.pack();
            shell.setMaximized(true); // Default = maximized!
        }

        addMenu();
        addTree();
        addCoreObjectsExpandBar();
        addTabs();
                
        // In case someone dares to press the [X] in the corner ;-)
        shell.addShellListener( 
            new ShellAdapter() 
            { 
                public void shellClosed(ShellEvent e) 
                { 
                    e.doit=quitFile(); 
                } 
            } 
        );
        
        shell.addKeyListener(defKeys);
        shell.addKeyListener(modKeys);
        
        // Add a browser widget
        if (props.showWelcomePageOnStartup())
        {
            showWelcomePage();
        }

        shell.layout();        
    }

    public Shell getShell() {
    		return shell;
    }
    
    public static Spoon getInstance()
    {
        return staticSpoon;
    }

    public XulMenuBar getMenuBar() {
    		return menuBar;
    }

    /**
     * Add a transformation to the 
     * @param transMeta the transformation to add to the map
     * @return the key used to store the transformation in the map
     */
    public String addTransformation(TransMeta transMeta)
    {
        String key = makeTransGraphTabName(transMeta);

        if (transformationMap.get(key)==null)
        {
            transformationMap.put(key, transMeta);
        }
        else
        {
            ShowMessageDialog dialog = new ShowMessageDialog(shell, SWT.OK | SWT.ICON_INFORMATION, Messages.getString("Spoon.Dialog.TransAlreadyLoaded.Title"), "'"+key+"'"+Const.CR+Const.CR+Messages.getString("Spoon.Dialog.TransAlreadyLoaded.Message"));
            dialog.setTimeOut(6);
            dialog.open();
            /*
            MessageBox mb = new MessageBox(shell, SWT.OK | SWT.ICON_INFORMATION);
            mb.setMessage("'"+key+"'"+Const.CR+Const.CR+Messages.getString("Spoon.Dialog.TransAlreadyLoaded.Message")); // Transformation is already loaded
            mb.setText(Messages.getString("Spoon.Dialog.TransAlreadyLoaded.Title")); // Sorry!
            mb.open();
            */
        }
        
        return key;
    }
    
    /**
     * Add a job to the job map 
     * @param jobMeta the job to add to the map
     * @return the key used to store the transformation in the map
     */
    public String addJob(JobMeta jobMeta)
    {
        String key = makeJobGraphTabName(jobMeta);
        if (jobMap.get(key)==null)
        {
            jobMap.put(key, jobMeta);
        }
        else
        {
            ShowMessageDialog dialog = new ShowMessageDialog(shell, SWT.OK | SWT.ICON_INFORMATION, Messages.getString("Spoon.Dialog.JobAlreadyLoaded.Title"), "'"+key+"'"+Const.CR+Const.CR+Messages.getString("Spoon.Dialog.JobAlreadyLoaded.Message"));
            dialog.setTimeOut(6);
            dialog.open();
            /*
            MessageBox mb = new MessageBox(shell, SWT.OK | SWT.ICON_INFORMATION);
            mb.setMessage("'"+key+"'"+Const.CR+Const.CR+Messages.getString("Spoon.Dialog.JobAlreadyLoaded.Message")); // Transformation is already loaded
            mb.setText(Messages.getString("Spoon.Dialog.JobAlreadyLoaded.Title")); // Sorry!
            mb.open();
            */
        }
        
        return key;
    }
    
    
    public void closeFile()
    {
        TransMeta transMeta = getActiveTransformation();
        if (transMeta!=null)
        {
            // If a transformation is the current active tab, close it
            closeTransformation(transMeta);
        }
        else
        {
            // Otherwise try to find the current open job and close it
            JobMeta jobMeta = getActiveJob();
            if (jobMeta!=null) closeJob(jobMeta);
        }
    }

    /**
     * @param transMeta the transformation to close, make sure it's ok to dispose of it BEFORE you call this.
     */
    public void closeTransformation(TransMeta transMeta)
    {
        String tabName = makeTransGraphTabName(transMeta);
        transformationMap.remove(tabName);
        
        // Close the associated tabs...
        TabItem graphTab = findTabItem(tabName, TabMapEntry.OBJECT_TYPE_TRANSFORMATION_GRAPH);
        if (graphTab!=null)
        {
            graphTab.dispose();
            tabMap.remove(tabName);
        }
        
        // Logging
        String logTabName = makeLogTabName(transMeta);
        TabItem logTab = findTabItem(logTabName, TabMapEntry.OBJECT_TYPE_TRANSFORMATION_LOG);
        if (logTab!=null)
        {
            logTab.dispose();
            tabMap.remove(logTabName);
        }
        
        //History
        String historyTabName = makeHistoryTabName(transMeta);
        TabItem historyTab = findTabItem(historyTabName, TabMapEntry.OBJECT_TYPE_TRANSFORMATION_HISTORY);
        if (historyTab!=null) 
        {
            historyTab.dispose();
            tabMap.remove(historyTabName);
        }
        
        refreshTree();
    }

    /**
     * @param transMeta the transformation to close, make sure it's ok to dispose of it BEFORE you call this.
     */
    public void closeJob(JobMeta jobMeta)
    {
        String tabName = makeJobGraphTabName(jobMeta);
        jobMap.remove(tabName);
        
        // Close the associated tabs...
        TabItem graphTab = findTabItem(tabName, TabMapEntry.OBJECT_TYPE_JOB_GRAPH);
        if (graphTab!=null)
        {
            tabMap.remove(tabName);
            graphTab.dispose();
        }
        
        // Logging
        String logTabName = makeJobLogTabName(jobMeta);
        TabItem logTab = findTabItem(logTabName, TabMapEntry.OBJECT_TYPE_JOB_LOG);
        if (logTab!=null)
        {
            logTab.dispose();
            tabMap.remove(logTabName);
        }
        
        //History
        String historyTabName = makeJobHistoryTabName(jobMeta);
        TabItem historyTab = findTabItem(historyTabName, TabMapEntry.OBJECT_TYPE_JOB_HISTORY);
        if (historyTab!=null) 
        {
            historyTab.dispose();
            tabMap.remove(historyTabName);
        }
        
        refreshTree();
    }
    
    public void closeSpoonBrowser()
    {
        tabMap.remove(STRING_WELCOME_TAB_NAME);
        TabItem tab = findTabItem(STRING_WELCOME_TAB_NAME, TabMapEntry.OBJECT_TYPE_BROWSER);
        if (tab!=null) tab.dispose();
    }
    
    /**
     * Search the transformation meta-data.
     *
     */
    public void searchMetaData()
    {
        TransMeta[] transMetas = getLoadedTransformations();
        JobMeta[] jobMetas = getLoadedJobs();
        if ( (transMetas==null || transMetas.length==0) && (jobMetas==null || jobMetas.length==0)) return;
        
        EnterSearchDialog esd = new EnterSearchDialog(shell);
        if (!esd.open())
        {
            return;
        }

        List<Object[]> rows = new ArrayList<Object[]>();

        for (int t=0;t<transMetas.length;t++)
        {
            TransMeta transMeta = transMetas[t];
            String filterString = esd.getFilterString();
            String filter = filterString;
            if (filter!=null) filter = filter.toUpperCase();
            
            List<StringSearchResult> stringList = transMeta.getStringList(esd.isSearchingSteps(), esd.isSearchingDatabases(), esd.isSearchingNotes());
            for (int i=0;i<stringList.size();i++)
            {
                StringSearchResult result = (StringSearchResult) stringList.get(i);

                boolean add = Const.isEmpty(filter);
                if (filter!=null && result.getString().toUpperCase().indexOf(filter)>=0) add=true;
                if (filter!=null && result.getFieldName().toUpperCase().indexOf(filter)>=0) add=true;
                if (filter!=null && result.getParentObject().toString().toUpperCase().indexOf(filter)>=0) add=true;
                if (filter!=null && result.getGrandParentObject().toString().toUpperCase().indexOf(filter)>=0) add=true;
                
                if (add) rows.add(result.toRow().getData());
            }
        }

        RowMetaInterface rowMeta = null;
        for (int t=0;t<jobMetas.length;t++)
        {
            JobMeta jobMeta = jobMetas[t];
            String filterString = esd.getFilterString();
            String filter = filterString;
            if (filter!=null) filter = filter.toUpperCase();
            
            List<StringSearchResult> stringList = jobMeta.getStringList(esd.isSearchingSteps(), esd.isSearchingDatabases(), esd.isSearchingNotes());
            for (StringSearchResult result : stringList)
            {
                boolean add = Const.isEmpty(filter);
                if (filter!=null && result.getString().toUpperCase().indexOf(filter)>=0) add=true;
                if (filter!=null && result.getFieldName().toUpperCase().indexOf(filter)>=0) add=true;
                if (filter!=null && result.getParentObject().toString().toUpperCase().indexOf(filter)>=0) add=true;
                if (filter!=null && result.getGrandParentObject().toString().toUpperCase().indexOf(filter)>=0) add=true;
                
                RowMetaAndData row = result.toRow();
                
                rowMeta = row.getRowMeta();
                if (add) rows.add(row.getData());
            }
        }

        if (rows.size()!=0)
        {
            PreviewRowsDialog prd = new PreviewRowsDialog(shell, Variables.getADefaultVariableSpace(), SWT.NONE, Messages.getString("Spoon.StringSearchResult.Subtitle"), rowMeta, rows);
            String title = Messages.getString("Spoon.StringSearchResult.Title");
            String message = Messages.getString("Spoon.StringSearchResult.Message");
            prd.setTitleMessage(title, message);
            prd.open();
        }
        else
        {
            MessageBox mb = new MessageBox(shell, SWT.OK | SWT.ICON_INFORMATION);
            mb.setMessage(Messages.getString("Spoon.Dialog.NothingFound.Message")); // Nothing found that matches your criteria
            mb.setText(Messages.getString("Spoon.Dialog.NothingFound.Title")); // Sorry!
            mb.open();
        }
    }

    public void getVariables()
    {
        TransMeta[] transMetas = getLoadedTransformations();
        JobMeta[] jobMetas = getLoadedJobs();
        if ( (transMetas==null || transMetas.length==0) && (jobMetas==null || jobMetas.length==0)) return;
        
        KettleVariables kettleVariables = KettleVariables.getInstance();
        Properties sp = new Properties();
        sp.putAll(kettleVariables.getProperties());
        sp.putAll(System.getProperties());
        
        for (int t=0;t<transMetas.length;t++)
        {
            TransMeta transMeta = transMetas[t];
            
            List<String> list = transMeta.getUsedVariables();
            for (int i=0;i<list.size();i++)
            {
                String varName = list.get(i);
                String varValue = sp.getProperty(varName, "");
                if (variables.getRowMeta().indexOfValue(varName)<0 && !varName.startsWith(Const.INTERNAL_VARIABLE_PREFIX))
                {
                    variables.addValue(new ValueMeta(varName, ValueMetaInterface.TYPE_STRING), varValue);
                }
            }
        }
        
        for (int t=0;t<jobMetas.length;t++)
        {
            JobMeta jobMeta = jobMetas[t];
            
            List<String> list = jobMeta.getUsedVariables();
            for (int i=0;i<list.size();i++)
            {
                String varName = list.get(i);
                String varValue = sp.getProperty(varName, "");
                if (variables.getRowMeta().indexOfValue(varName)<0 && !varName.startsWith(Const.INTERNAL_VARIABLE_PREFIX))
                {
                    variables.addValue(new ValueMeta(varName, ValueMetaInterface.TYPE_STRING), varValue);
                }
            }
        }
            
        // Now ask the use for more info on these!
        EnterStringsDialog esd = new EnterStringsDialog(shell, SWT.NONE, variables);
        esd.setTitle(Messages.getString("Spoon.Dialog.SetVariables.Title"));
        esd.setMessage(Messages.getString("Spoon.Dialog.SetVariables.Message"));
        esd.setReadOnly(false); 
        if (esd.open()!=null)
        {
            for (int i=0;i<variables.getRowMeta().size();i++)
            {
                ValueMetaInterface valueMeta = variables.getRowMeta().getValueMeta(i);
                Object valueData = variables.getData()[i];
                
                String string;
                try
                {
                    string = valueMeta.getString(valueData);
                }
                catch (KettleValueException e)
                {
                    // can be ignored, we only work with strings
                    string = null;
                }
                
                if (!Const.isEmpty(string))
                {
                    kettleVariables.setVariable(valueMeta.getName(), string);
                }
            }
        }
    }
    
    public void showVariables()
    {
        Properties sp = new Properties();
        KettleVariables kettleVariables = KettleVariables.getInstance();
        sp.putAll(kettleVariables.getProperties());
        sp.putAll(System.getProperties());

        RowMetaAndData allVars = new RowMetaAndData();
        
        for (String key : kettleVariables.getProperties().keySet())
        {
            String value = kettleVariables.getVariable(key);
            allVars.addValue(new ValueMeta(key, ValueMetaInterface.TYPE_STRING), value);
        }
        
        // Now ask the use for more info on these!
        EnterStringsDialog esd = new EnterStringsDialog(shell, SWT.NONE, allVars);
        esd.setTitle(Messages.getString("Spoon.Dialog.ShowVariables.Title"));
        esd.setMessage(Messages.getString("Spoon.Dialog.ShowVariables.Message"));
        esd.setReadOnly(true); 
        esd.open();
    }
    
    public void open()
    {       
        shell.open();
        
        // Shared database entries to load from repository?
        // loadRepositoryObjects();
        
        // Load shared objects from XML file.
        // loadSharedObjects();
        
        // Perhaps the transformation contains elements at startup?
        refreshTree();  // Do a complete refresh then...
        
        setShellText();
        
        if (props.showTips()) 
        {
            TipsDialog tip = new TipsDialog(shell);
            tip.open();
        }
    }
    
    public boolean readAndDispatch ()
    {
        return display.readAndDispatch();
    }
    
    /**
     * @return check whether or not the application was stopped.
     */
    public boolean isStopped()
    {
        return stopped;
    }
    
    /**
     * @param stopped True to stop this application.
     */
    public void setStopped(boolean stopped)
    {
        this.stopped = stopped;
    }
    
    /**
     * @param destroy Whether or not to distroy the display.
     */
    public void setDestroy(boolean destroy)
    {
        this.destroy = destroy;
    }
    
    /**
     * @return Returns whether or not we should distroy the display.
     */
    public boolean doDestroy()
    {
        return destroy;
    }
    
    /**
     * @param arguments The arguments to set.
     */
    public void setArguments(String[] arguments)
    {
        this.arguments = arguments;
    }

    /**
     * @return Returns the arguments.
     */
    public String[] getArguments()
    {
        return arguments;
    }
    
    public synchronized void dispose()
    {
        setStopped(true);
        cursor_hand.dispose();
        cursor_hourglass.dispose();
        
        if (destroy && !display.isDisposed()) display.dispose();        
    }

    public boolean isDisposed()
    {
        return display.isDisposed();
    }

    public void sleep()
    {
        display.sleep();
    }
    
    private Map<String,Menu> menuMap = new HashMap<String,Menu>();
    
    public void createMenuBarFromXul() {
    		
        if (menuBar!=null && !menuBar.isDisposed())
        {
        	menuBar.dispose();
        }

        XulMessages xulMessages = new XulMessages();
        
		try {
    		// first get the XML document
    		File xulFile = new File( XUL_FILE_MENUBAR );
    		if( xulFile.exists() ) {
    			Document doc = XMLHandler.loadXMLFile( xulFile );
    			menuBar = MenuHelper.createMenuBarFromXul( doc, shell, xulMessages );
    	        shell.setMenuBar((org.eclipse.swt.widgets.Menu)menuBar.getNativeObject());
    		}
    		else
    		{
    			throw new KettleException(Messages.getString("Spoon.Exception.XULFileNotFound.Message", XUL_FILE_MENUBAR));
    		}
		} catch (Throwable t ) {
			log.logError(toString(), Const.getStackTracker(t));
			new ErrorDialog(shell, Messages.getString("Spoon.Exception.ErrorReadingXULFile.Title"), Messages.getString("Spoon.Exception.ErrorReadingXULFile.Message", XUL_FILE_MENUBAR), new Exception(t));
		}
		
		try {
    		File xulFile = new File( XUL_FILE_MENUS ); //$NON-NLS-1$
    		if( xulFile.exists() ) {
    			Document doc = XMLHandler.loadXMLFile( xulFile );
    			List<String> ids = new ArrayList<String>();
    			ids.add( "trans-class" );
    			ids.add( "trans-class-new" );
    			ids.add( "job-class" );
    			ids.add( "trans-hop-class" );
    			ids.add( "database-class" );
    			ids.add( "partition-schema-class" );
    			ids.add( "cluster-schema-class" );
    			ids.add( "slave-cluster-class" );
    			ids.add( "trans-inst" );
    			ids.add( "job-inst" );
    			ids.add( "step-plugin" );
    			ids.add( "database-inst" );
    			ids.add( "step-inst" );
    			ids.add( "job-entry-copy-inst" );
    			ids.add( "trans-hop-inst" );
    			ids.add( "partition-schema-inst" );
    			ids.add( "cluster-schema-inst" );
    			ids.add( "slave-server-inst" );
    			
    			menuMap = MenuHelper.createPopupMenusFromXul( doc, shell, xulMessages, ids );
    		}
    		else
    		{
    			throw new KettleException(Messages.getString("Spoon.Exception.XULFileNotFound.Message", XUL_FILE_MENUS));
    		}
		} catch (Throwable t ) {
			// TODO log this
			t.printStackTrace();
			new ErrorDialog(shell, Messages.getString("Spoon.Exception.ErrorReadingXULFile.Title"), Messages.getString("Spoon.Exception.ErrorReadingXULFile.Message", XUL_FILE_MENUS), new Exception(t));
		}
    }
    
    public void addMenuListeners() {
		try {
			// first get the XML document
    			File propertiesFile = new File( XUL_FILE_MENU_PROPERTIES ); 
    			if( propertiesFile.exists() ) {
    				Properties props = new Properties();
    				props.load( new FileInputStream( propertiesFile ) );
    				String ids[] = menuBar.getMenuItemIds();
    				for( int i=0; i<ids.length; i++ ) {
        				String methodName = (String) props.get( ids[i] );
        				if( methodName != null ) {
                            menuBar.addMenuListener( ids[i], this, methodName ); 
                            toolbar.addMenuListener( ids[i], this, methodName );

        				}
    				}
    				for( String id : menuMap.keySet() ) {
    					PopupMenu menu = (PopupMenu) menuMap.get( id );
        				ids = menu.getMenuItemIds();
        				for( int i=0; i<ids.length; i++ ) {
            				String methodName = (String) props.get( ids[i] );
            				if( methodName != null ) {
            					menu.addMenuListener( ids[i], this, methodName ); 
            				}
        				}
            			for( int i=0; i<menuListeners.size(); i++ ) {
            				Object info[] = menuListeners.get( i );
            				menu.addMenuListener( (String) info[0], info[1], (String) info[2] ); 
            			}
    				}

    			}
    			else
    			{
    				throw new KettleException(Messages.getString("Spoon.Exception.XULFileNotFound.Message", XUL_FILE_MENU_PROPERTIES));
    			}
    			
    			// now apply any overrides
    			for( int i=0; i<menuListeners.size(); i++ ) {
    				Object info[] = menuListeners.get( i );
    				menuBar.addMenuListener( (String) info[0], info[1], (String) info[2] ); //$NON-NLS-1$ //$NON-NLS-2$
    			}
		} catch (Throwable t ) {
			// TODO log this
			t.printStackTrace();
			new ErrorDialog(shell, Messages.getString("Spoon.Exception.ErrorReadingXULFile.Title"), Messages.getString("Spoon.Exception.ErrorReadingXULFile.Message", XUL_FILE_MENU_PROPERTIES), new Exception(t));
		}
    }
    
    public void undoAction() {
		undoAction( getActiveUndoInterface() );
    }

    public void redoAction() {
		redoAction( getActiveUndoInterface() );
    }

    public void editUnselectAll( ) {
		editUnselectAll( getActiveTransformation() );
    }

    public void editSelectAll( ) {
    		editSelectAll( getActiveTransformation() );
    }

    public void copySteps() {
        TransMeta transMeta = getActiveTransformation();
        copySelected(transMeta, transMeta.getSelectedSteps(), transMeta.getSelectedNotes());
    }
    
    public void addMenu()
    {
		createMenuBarFromXul();
		addMenuListeners();
        addMenuLast();
        
    }

    public void executeTransformation() {
    		executeTransformation(getActiveTransformation(), true, false, false, false, null);
    }
    
    public void previewTransformation() {
    		executeTransformation(getActiveTransformation(), true, false, false, true, null);
    }
    
    public void checkTrans() {
    		checkTrans(getActiveTransformation());
    }
    
    public void analyseImpact() {
    		analyseImpact(getActiveTransformation());
    }
    
    public void showLastImpactAnalyses() {
    		showLastImpactAnalyses(getActiveTransformation());
    }
    
    public void showLastTransPreview() {
    		TransLog spoonLog = getActiveTransLog(); 
    		if (spoonLog!=null) {
    			spoonLog.showPreview();
    		}
    }
    
    public void copyTransformation() {
    		copyTransformation(getActiveTransformation());
    }
    
    public void copyTransformationImage() {
    		copyTransformationImage(getActiveTransformation());
    }
    
    public void editTransformationProperties() {
    		getActiveTransformation().editProperties(this, rep); 
    }
    
    public void executeJob() {
    		executeJob(getActiveJob(), true, false, false, false, null);
    }
    
    public void copyJob() {
    		copyJob(getActiveJob());
    }
    
    public void showCredits() {
    		ShowCreditsDialog scd = new ShowCreditsDialog(shell, props, GUIResource.getInstance().getImageCredits()); 
    		scd.open();
    	}

    public void showTips() {
		new TipsDialog(shell).open();
    }

    public void showWelcomePage()
    {
        try
        {
            File file = new File(FILE_WELCOME_PAGE);
            addSpoonBrowser(STRING_WELCOME_TAB_NAME, file.toURI().toURL().toString()); // ./docs/English/tips/index.htm
        }
        catch (MalformedURLException e1)
        {
            log.logError(toString(), Const.getStackTracker(e1));
        } 
    }

    public void addMenuLast()
    {
    	
    		XulMenuItem sep = menuBar.getSeparatorById( "file-last-separator" ); //$NON-NLS-1$
    		XulMenu msFile = null;
    		if( sep != null ) {
    			msFile = sep.getMenu( ); //$NON-NLS-1$
    		}
    		if( msFile == null || sep == null ) {
    			// The menu system has been altered and we can't display the last used files
    			return;
    		}
    		int idx = msFile.indexOf( sep );
    		int max = msFile.getItemCount();
        // Remove everything until end... 
        for (int i=max-1;i>idx;i--)
        {
            XulMenuItem mi = msFile.getItem(i);
            msFile.remove( mi );
           mi.dispose();
        }

        // Previously loaded files...
        List<LastUsedFile> lastUsedFiles = props.getLastUsedFiles();
        for (int i = 0; i < lastUsedFiles.size(); i++)
        {
            final LastUsedFile lastUsedFile = lastUsedFiles.get(i);

            char chr = (char) ('1' + i);
            String accessKey = "ctrl-"+chr; //$NON-NLS-1$
            String accessText = "CTRL-"+chr; //$NON-NLS-1$
            String text = lastUsedFile.toString();
            String id = "last-file-"+i; //$NON-NLS-1$

            if (i > 9)
            {
            			accessKey = null;
            			accessText = null;
            }

            MenuChoice miFileLast = new MenuChoice(msFile, text, id, accessText, accessKey, MenuChoice.TYPE_PLAIN, null);
            
            if (lastUsedFile.isTransformation())
            {
                miFileLast.setImage(GUIResource.getInstance().getImageTransGraph());
            }
            else
            if (lastUsedFile.isJob())
            {
                miFileLast.setImage(GUIResource.getInstance().getImageJobGraph());
            }

            menuBar.addMenuListener( id, this, "lastFileSelect" ); //$NON-NLS-1$
        }

    }
    
    public void lastFileSelect(String id) {

		int idx = Integer.parseInt(id.substring("last-file-".length()));
		List<LastUsedFile> lastUsedFiles = props.getLastUsedFiles();
		final LastUsedFile lastUsedFile = (LastUsedFile) lastUsedFiles.get(idx);

		// If the file comes from a repository and it's not the same as
		// the one we're connected to, ask for a username/password!
		//
		boolean cancelled = false;
		if (lastUsedFile.isSourceRepository()
				&& (rep == null || !rep.getRepositoryInfo().getName().equalsIgnoreCase(lastUsedFile.getRepositoryName()))) {
			// Ask for a username password to get the required repository access
			//
			int perms[] = new int[] {
				PermissionMeta.TYPE_PERMISSION_TRANSFORMATION
			};
			RepositoriesDialog rd = new RepositoriesDialog(display, perms, Messages.getString("Spoon.Application.Name")); // RepositoriesDialog.ToolName="Spoon"
			rd.setRepositoryName(lastUsedFile.getRepositoryName());
			if (rd.open()) {
				// Close the previous connection...
				if (rep != null)
					rep.disconnect();
				rep = new Repository(log, rd.getRepository(), rd.getUser());
				try {
					rep.connect(APP_NAME);
				} catch (KettleException ke) {
					rep = null;
					new ErrorDialog( shell, Messages.getString("Spoon.Dialog.UnableConnectRepository.Title"), Messages.getString("Spoon.Dialog.UnableConnectRepository.Message"), ke); //$NON-NLS-1$ //$NON-NLS-2$
				}
			} else {
				cancelled = true;
			}
		}

		if (!cancelled) {
			try {
				RepositoryMeta meta = (rep == null ? null : rep.getRepositoryInfo());
				loadLastUsedFile(lastUsedFile, meta);
				addMenuLast();
				refreshHistory();
			} catch (KettleException ke) {
				// "Error loading transformation", "I was unable to load this
				// transformation from the
				// XML file because of an error"
				new ErrorDialog(shell, Messages.getString("Spoon.Dialog.LoadTransformationError.Title"), Messages
						.getString("Spoon.Dialog.LoadTransformationError.Message"), ke);
			}
		}

	}
    
    private void addBar()
    {

        XulMessages xulMessages = new XulMessages();
        
		try {
    		// first get the XML document
    		File xulFile = new File( XUL_FILE_MENUBAR );
    		if( xulFile.exists() ) {
    			Document doc = XMLHandler.loadXMLFile( xulFile );
    	    	toolbar = MenuHelper.createToolbarFromXul( doc, shell, xulMessages, this );
    		}
    		else
    		{
    			throw new KettleException(Messages.getString("Spoon.Exception.XULFileNotFound.Message", XUL_FILE_MENUBAR));
    		}
		} catch (Throwable t ) {
			log.logError(toString(), Const.getStackTracker(t));
			new ErrorDialog(shell, Messages.getString("Spoon.Exception.ErrorReadingXULFile.Title"), Messages.getString("Spoon.Exception.ErrorReadingXULFile.Message", XUL_FILE_MENUBAR), new Exception(t));
		}

/*
 * ToolbarButton button = new ToolbarButton(shell, "file-new-trans", toolbar);
 * final Image imFileNewTrans = ImageUtil.makeImageTransparent(display, new
 * Image(display,
 * getClass().getResourceAsStream(Const.IMAGE_DIRECTORY+"newtrans.png")), new
 * RGB(192, 192, 192)); button.setImage(imFileNewTrans);
 * button.setHint(Messages.getString("Spoon.Tooltip.NewFile"));
 * 
 * final ToolItem tiFileNew = new ToolItem(tBar, SWT.PUSH); final Image
 * imFileNew = new Image(display,
 * getClass().getResourceAsStream(Const.IMAGE_DIRECTORY+"new.png"));
 * tiFileNew.setImage(imFileNew); tiFileNew.addSelectionListener(new
 * SelectionAdapter() { public void widgetSelected(SelectionEvent e) {
 * newFile(); }});
 * tiFileNew.setToolTipText(Messages.getString("Spoon.Tooltip.NewFile"));
 * 
 * final ToolItem tiFileNewTrans = new ToolItem(tBar, SWT.PUSH); final Image
 * imFileNewTrans = ImageUtil.makeImageTransparent(display, new Image(display,
 * getClass().getResourceAsStream(Const.IMAGE_DIRECTORY+"newtrans.png")), new
 * RGB(192, 192, 192)); tiFileNewTrans.setImage(imFileNewTrans);
 * tiFileNewTrans.addSelectionListener(new SelectionAdapter() { public void
 * widgetSelected(SelectionEvent e) { newTransFile(); }});
 * tiFileNewTrans.setToolTipText(Messages.getString("Spoon.Tooltip.NewTranformation"));
 * 
 * final ToolItem tiFileNewJob = new ToolItem(tBar, SWT.PUSH); final Image
 * imFileNewJob = ImageUtil.makeImageTransparent(display, new Image(display,
 * getClass().getResourceAsStream(Const.IMAGE_DIRECTORY+"newjob.png")), new
 * RGB(192, 192, 192)); tiFileNewJob.setImage(imFileNewJob);
 * tiFileNewJob.addSelectionListener(new SelectionAdapter() { public void
 * widgetSelected(SelectionEvent e) { newJobFile(); }});
 * tiFileNewJob.setToolTipText(Messages.getString("Spoon.Tooltip.NewJob"));
 * 
 * new ToolItem(tBar, SWT.SEPARATOR);
 * 
 * final ToolItem tiFileOpen = new ToolItem(tBar, SWT.PUSH); final Image
 * imFileOpen = new Image(display,
 * getClass().getResourceAsStream(Const.IMAGE_DIRECTORY+"open.png"));
 * tiFileOpen.setImage(imFileOpen); tiFileOpen.addSelectionListener(new
 * SelectionAdapter() { public void widgetSelected(SelectionEvent e) {
 * openFile(false); }});
 * tiFileOpen.setToolTipText(Messages.getString("Spoon.Tooltip.OpenTranformation"));//Open
 * tranformation
 * 
 * tiFileSave = new ToolItem(tBar, SWT.PUSH); final Image imFileSave = new
 * Image(display,
 * getClass().getResourceAsStream(Const.IMAGE_DIRECTORY+"save.png"));
 * tiFileSave.setImage(imFileSave); tiFileSave.addSelectionListener(new
 * SelectionAdapter() { public void widgetSelected(SelectionEvent e) {
 * saveFile(); }});
 * tiFileSave.setToolTipText(Messages.getString("Spoon.Tooltip.SaveCurrentTranformation"));//Save
 * current transformation
 * 
 * tiFileSaveAs = new ToolItem(tBar, SWT.PUSH); final Image imFileSaveAs = new
 * Image(display,
 * getClass().getResourceAsStream(Const.IMAGE_DIRECTORY+"saveas.png"));
 * tiFileSaveAs.setImage(imFileSaveAs); tiFileSaveAs.addSelectionListener(new
 * SelectionAdapter() { public void widgetSelected(SelectionEvent e) {
 * saveFileAs(); }});
 * tiFileSaveAs.setToolTipText(Messages.getString("Spoon.Tooltip.SaveDifferentNameTranformation"));//Save
 * transformation with different name
 * 
 * new ToolItem(tBar, SWT.SEPARATOR); tiFilePrint = new ToolItem(tBar,
 * SWT.PUSH); final Image imFilePrint = new Image(display,
 * getClass().getResourceAsStream(Const.IMAGE_DIRECTORY+"print.png"));
 * tiFilePrint.setImage(imFilePrint); tiFilePrint.addSelectionListener(new
 * SelectionAdapter() { public void widgetSelected(SelectionEvent e) {
 * printFile(); }});
 * tiFilePrint.setToolTipText(Messages.getString("Spoon.Tooltip.Print"));//Print
 * 
 * new ToolItem(tBar, SWT.SEPARATOR); tiFileRun = new ToolItem(tBar, SWT.PUSH);
 * final Image imFileRun = new Image(display,
 * getClass().getResourceAsStream(Const.IMAGE_DIRECTORY+"run.png"));
 * tiFileRun.setImage(imFileRun); tiFileRun.addSelectionListener(new
 * SelectionAdapter() { public void widgetSelected(SelectionEvent e) {
 * executeFile(true, false, false, false, null); }});
 * tiFileRun.setToolTipText(Messages.getString("Spoon.Tooltip.RunTranformation"));//Run
 * this transformation
 * 
 * tiFilePreview = new ToolItem(tBar, SWT.PUSH); final Image imFilePreview = new
 * Image(display,
 * getClass().getResourceAsStream(Const.IMAGE_DIRECTORY+"preview.png"));
 * tiFilePreview.setImage(imFilePreview); tiFilePreview.addSelectionListener(new
 * SelectionAdapter() { public void widgetSelected(SelectionEvent e) {
 * executeFile(true, false, false, true, null); }});
 * tiFilePreview.setToolTipText(Messages.getString("Spoon.Tooltip.PreviewTranformation"));//Preview
 * this transformation
 * 
 * tiFileReplay = new ToolItem(tBar, SWT.PUSH); final Image imFileReplay = new
 * Image(display,
 * getClass().getResourceAsStream(Const.IMAGE_DIRECTORY+"replay.png"));
 * tiFileReplay.setImage(imFileReplay); tiFileReplay.addSelectionListener(new
 * SelectionAdapter() { public void widgetSelected(SelectionEvent e) {
 * executeFile(true, false, false, true, null); }});
 * tiFileReplay.setToolTipText(Messages.getString("Spoon.Tooltip.ReplayTranformation"));
 * 
 * new ToolItem(tBar, SWT.SEPARATOR); tiFileCheck = new ToolItem(tBar,
 * SWT.PUSH); final Image imFileCheck = new Image(display,
 * getClass().getResourceAsStream(Const.IMAGE_DIRECTORY+"check.png"));
 * tiFileCheck.setImage(imFileCheck); tiFileCheck.addSelectionListener(new
 * SelectionAdapter() { public void widgetSelected(SelectionEvent e) {
 * checkTrans(getActiveTransformation()); }});
 * tiFileCheck.setToolTipText(Messages.getString("Spoon.Tooltip.VerifyTranformation"));//Verify
 * this transformation
 * 
 * new ToolItem(tBar, SWT.SEPARATOR); tiImpact = new ToolItem(tBar, SWT.PUSH);
 * final Image imImpact = new Image(display,
 * getClass().getResourceAsStream(Const.IMAGE_DIRECTORY+"impact.png")); // Can't
 * seem to get the transparency correct for this image! ImageData idImpact =
 * imImpact.getImageData(); int impactPixel = idImpact.palette.getPixel(new
 * RGB(255, 255, 255)); idImpact.transparentPixel = impactPixel; Image imImpact2 =
 * new Image(display, idImpact); tiImpact.setImage(imImpact2);
 * tiImpact.addSelectionListener(new SelectionAdapter() { public void
 * widgetSelected(SelectionEvent e) { analyseImpact(getActiveTransformation());
 * }});
 * tiImpact.setToolTipText(Messages.getString("Spoon.Tooltip.AnalyzeTranformation"));//Analyze
 * the impact of this transformation on the database(s)
 * 
 * new ToolItem(tBar, SWT.SEPARATOR); tiSQL = new ToolItem(tBar, SWT.PUSH);
 * final Image imSQL = new Image(display,
 * getClass().getResourceAsStream(Const.IMAGE_DIRECTORY+"SQLbutton.png")); //
 * Can't seem to get the transparency correct for this image! ImageData idSQL =
 * imSQL.getImageData(); int sqlPixel= idSQL.palette.getPixel(new RGB(255, 255,
 * 255)); idSQL.transparentPixel = sqlPixel; Image imSQL2= new Image(display,
 * idSQL); tiSQL.setImage(imSQL2); tiSQL.addSelectionListener(new
 * SelectionAdapter() { public void widgetSelected(SelectionEvent e) { getSQL();
 * }});
 * tiSQL.setToolTipText(Messages.getString("Spoon.Tooltip.GenerateSQLForTranformation"));//Generate
 * the SQL needed to run this transformation
 * 
 * tBar.addDisposeListener(new DisposeListener() { public void
 * widgetDisposed(DisposeEvent e) { imFileNew.dispose(); imFileOpen.dispose();
 * imFileSave.dispose(); imFileSaveAs.dispose(); } } );
 * tBar.addKeyListener(defKeys); tBar.addKeyListener(modKeys); tBar.pack();
 */
    }

    private static final String STRING_SPOON_MAIN_TREE = Messages.getString("Spoon.MainTree.Label");
    private static final String STRING_SPOON_CORE_OBJECTS_TREE= Messages.getString("Spoon.CoreObjectsTree.Label");

    private void addTree()
    {
        Composite composite = new Composite(sashform, SWT.NONE);
        props.setLook(composite);
        
        FillLayout fillLayout = new FillLayout();
        fillLayout.spacing = Const.MARGIN;
        fillLayout.marginHeight= Const.MARGIN;
        fillLayout.marginWidth= Const.MARGIN;
        composite.setLayout(fillLayout);
        
        mainExpandBar = new ExpandBar(composite, SWT.NO_BACKGROUND);
        props.setLook(mainExpandBar);
        mainExpandBar.setSpacing(0);

        mainExpandBar.addExpandListener(new ExpandAdapter()
            {
                public void itemExpanded(ExpandEvent event)
                {
                    ExpandItem item = (ExpandItem) event.item;
                    int idx = mainExpandBar.indexOf(item);
                    if (idx>=0)
                    {
                        for (int i=0;i<mainExpandBar.getItemCount();i++) if (i!=idx) mainExpandBar.getItem(i).setExpanded(false);
                        Control control = item.getControl();
                        control.setFocus();
                        refreshCoreObjectsHistory(); // only refreshes when visible. 
                    }
                }
            }
        );
        mainExpandBar.setBackground(GUIResource.getInstance().getColorBackground());
        mainExpandBar.setForeground(GUIResource.getInstance().getColorBlack());

        // // Split the left side of the screen in half
        // leftSash = new SashForm(mainExpandBar, SWT.VERTICAL);
        
        // Now set up the main CSH tree
        selectionTree = new Tree(mainExpandBar, SWT.SINGLE | SWT.BORDER);
        props.setLook(selectionTree);
        selectionTree.setLayout(new FillLayout());
        
        ExpandItem treeItem = new ExpandItem(mainExpandBar, SWT.NONE);
        treeItem.setControl(selectionTree);
        treeItem.setHeight(shell.getBounds().height);
        setHeaderImage(treeItem, GUIResource.getInstance().getImageLogoSmall(), STRING_SPOON_MAIN_TREE, 0);
        
        // Add a tree memory as well...
        TreeMemory.addTreeListener(selectionTree, STRING_SPOON_MAIN_TREE);
        
        selectionTree.addSelectionListener(new SelectionAdapter() { public void widgetSelected(SelectionEvent e) { setMenu(); } });
        selectionTree.addSelectionListener(new SelectionAdapter() { public void widgetSelected(SelectionEvent e) { showSelection(); } });
        selectionTree.addSelectionListener(new SelectionAdapter() { public void widgetDefaultSelected(SelectionEvent e){ doubleClickedInTree(); } });
        
        // Keyboard shortcuts!
        selectionTree.addKeyListener(defKeys);
        selectionTree.addKeyListener(modKeys);
        
        mainExpandBar.addKeyListener(defKeys);
        mainExpandBar.addKeyListener(modKeys);

        // Set a listener on the tree
        addDragSourceToTree(selectionTree); 
        
        mainExpandBar.addListener(SWT.Resize, new Listener()
            {
                public void handleEvent(Event event)
                {
                    resizeExpandBar(mainExpandBar);
                }
            }
        );
    }
        
    protected void resizeExpandBar(ExpandBar bar)
    {
        Rectangle bounds = bar.getBounds();
        
        // Adjust the sizes of the
        int header = 0;
        ExpandItem[] items = bar.getItems();
        for (int i = 0; i < items.length; i++)
        {
            ExpandItem item = items[i];
            header+=item.getHeaderHeight();
        }
        
        for (int i = 0; i < items.length; i++)
        {
            ExpandItem item = items[i];
            item.setHeight(bounds.height-header-15);
        }
    }

    private void drawPentahoGradient(GC gc, Rectangle rect, boolean vertical)
    {
        if (!vertical)
        {
            gc.setForeground(display.getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
            gc.setBackground(GUIResource.getInstance().getColorPentaho());
            gc.fillGradientRectangle(rect.x, rect.y, 2*rect.width/3, rect.height, vertical);
            gc.setForeground(GUIResource.getInstance().getColorPentaho());
            gc.setBackground(display.getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
            gc.fillGradientRectangle(rect.x+2*rect.width/3, rect.y, rect.width/3+1, rect.height, vertical);
        }
        else
        {
            gc.setForeground(display.getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
            gc.setBackground(GUIResource.getInstance().getColorPentaho());
            gc.fillGradientRectangle(rect.x, rect.y, rect.width, 2*rect.height/3, vertical);
            gc.setForeground(GUIResource.getInstance().getColorPentaho());
            gc.setBackground(display.getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
            gc.fillGradientRectangle(rect.x, rect.y+2*rect.height/3, rect.width, rect.height/3+1, vertical);
        }
    }

    public void addCoreObjectsExpandBar()
    {
        Composite composite = new Composite(mainExpandBar, SWT.BORDER);
        FormLayout formLayout = new FormLayout();
        formLayout.marginLeft=20;
        formLayout.marginTop=Const.MARGIN;
        formLayout.marginBottom=Const.MARGIN;
        composite.setLayout(formLayout);
        
        expandBar = new ExpandBar(composite, SWT.V_SCROLL);
        expandBar.setBackground(GUIResource.getInstance().getColorBackground());
        expandBar.setSpacing(0);
        
        FormData expandData = new FormData();
        expandData.left=new FormAttachment(0, 0);
        expandData.right=new FormAttachment(100, 0);
        expandData.top=new FormAttachment(0, 0);
        expandData.bottom=new FormAttachment(100, 0);
        expandBar.setLayoutData(expandData);
        
        // collapse the other expandbar items if one gets expanded...
        expandBar.addExpandListener(new ExpandAdapter()
            {
                public void itemExpanded(ExpandEvent event)
                {
                    ExpandItem item = (ExpandItem) event.item;
                    int idx = expandBar.indexOf(item);
                    if (idx>=0)
                    {
                        for (int i=0;i<expandBar.getItemCount();i++) if (i!=idx) expandBar.getItem(i).setExpanded(false);
                        ScrolledComposite scrolledComposite = (ScrolledComposite) item.getControl();
                        Composite composite = (Composite) scrolledComposite.getContent();
                        composite.setFocus();
                    }
                }
            }
        );
        expandBar.addListener(SWT.Resize, new Listener()
            {
                public void handleEvent(Event event)
                {
                    resizeExpandBar(expandBar);
                }
            }
        );
        
        
        ExpandItem expandItem = new ExpandItem(mainExpandBar, SWT.NONE);
        expandItem.setControl(composite);
        expandItem.setHeight(shell.getBounds().height);
        setHeaderImage(expandItem, GUIResource.getInstance().getImageLogoSmall(), STRING_SPOON_CORE_OBJECTS_TREE, 0);
        
        refreshCoreObjects();
    }
    
    private void setHeaderImage(ExpandItem expandItem, Image icon, String string, int offset)
    {
        // Draw just an image with text and all...
        Image img = new Image(display, 1, 1);
        GC tmpGC = new GC(img);
        org.eclipse.swt.graphics.Point point = tmpGC.textExtent(STRING_SPOON_MAIN_TREE);
        tmpGC.dispose();
        img.dispose();
        
        Rectangle rect = new Rectangle(0, 0, point.x + 100-offset, point.y+11);
        Rectangle iconBounds = icon.getBounds();
        
        final Image image = new Image(display, rect.width, rect.height);
        GC gc = new GC(image);
        if (props.isBrandingActive())
        {
            drawPentahoGradient(gc, rect, false);
        }
        gc.drawImage(icon, 0, 2);
        gc.setForeground(GUIResource.getInstance().getColorBlack());
        // gc.setBackground(expandItem.getParent().getBackground());
        gc.setFont(GUIResource.getInstance().getFontBold());
        gc.drawText(string, iconBounds.width+5, (iconBounds.height-point.y)/2+2, true);
        expandItem.setImage( ImageUtil.makeImageTransparent(display, image, new RGB(255, 255, 255)) );
        expandItem.addDisposeListener(new DisposeListener() { public void widgetDisposed(DisposeEvent event) { image.dispose(); } });
        
        /*
        {
            expandItem.setImage(icon);
            expandItem.setText(string);
        }
        */
    }

    private void refreshCoreObjectsHistory()
    {
        if (stepHistoryChanged || mainExpandBar.getItemCount()<3)
        {
            boolean showTrans = getActiveTransformation()!=null;

            // See if we need to bother.
            if (2<mainExpandBar.getItemCount() && mainExpandBar.getItemCount()>=3-(showTrans?0:1))
            {
                ExpandItem item = mainExpandBar.getItem(2);
                if (!item.getExpanded()) return; // no, don't bother
            }
            
            if (showTrans)
            {
                // create the history expand-item.
                ScrolledComposite scrolledHistoryComposite = new ScrolledComposite(mainExpandBar, SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER);
        
                scrolledHistoryComposite.setLayout(new FillLayout());
                
                Composite historyComposite = new Composite(scrolledHistoryComposite, SWT.NONE);
                props.setLook(historyComposite);
                GridLayout layout = new GridLayout ();
                layout.marginLeft = 15;
                layout.verticalSpacing = Const.MARGIN;
                historyComposite.setLayout(layout);
                
                ExpandItem historyExpandItem = new ExpandItem(mainExpandBar, SWT.NONE);
                
                List<ObjectUsageCount> pluginHistory = props.getPluginHistory();
                String locale = LanguageChoice.getInstance().getDefaultLocale().toString().toLowerCase();
                
                for (int i=0;i<pluginHistory.size() && i<10;i++) // top 10 maximum, the rest is not interesting anyway -- for GUI performance reasons
                {
                    ObjectUsageCount usage = (ObjectUsageCount) pluginHistory.get(i);
                    
                    StepPlugin stepPlugin = StepLoader.getInstance().findStepPluginWithID(usage.getObjectName());
                    if (stepPlugin!=null)
                    {
                        final Image stepimg = (Image)GUIResource.getInstance().getImagesStepsSmall().get(stepPlugin.getID()[0]);
                        String pluginName   = stepPlugin.getDescription(locale)+" ("+usage.getNrUses()+")";
                        String pluginDescription = stepPlugin.getTooltip(locale);
                        boolean isPlugin = stepPlugin.isPlugin();
                        
                        addExpandBarItemLine(historyExpandItem, historyComposite, stepimg, pluginName, pluginDescription, isPlugin, stepPlugin);
                    }
                }
                
                historyComposite.layout();
                org.eclipse.swt.graphics.Rectangle bounds = historyComposite.getBounds();
                
                scrolledHistoryComposite.setMinSize(bounds.width, bounds.height);
                scrolledHistoryComposite.setContent(historyComposite);
                scrolledHistoryComposite.setExpandHorizontal(true);
                scrolledHistoryComposite.setExpandVertical(true);
                
                historyExpandItem.setControl(scrolledHistoryComposite);
                historyExpandItem.setHeight(scrolledHistoryComposite.computeSize(SWT.DEFAULT, SWT.DEFAULT).y);
                setHeaderImage(historyExpandItem, GUIResource.getInstance().getImageLogoSmall(), STRING_HISTORY, 0);
                scrolledHistoryComposite.layout(true, true);
            }
            
            boolean expanded = false;
            if (mainExpandBar.getItemCount()>3-(showTrans?0:1))
            {
                ExpandItem item = mainExpandBar.getItem(2);
                expanded = item.getExpanded();
                item.setExpanded(false);
                // item.getControl().dispose();
                item.dispose();
            }
            
            if (showTrans)
            {
                mainExpandBar.getItem(2).setExpanded(expanded);
            }
            resizeExpandBar(mainExpandBar);
            
            mainExpandBar.redraw();
            
            stepHistoryChanged=false;
        }
    }
    
    private boolean previousShowTrans;
    private boolean previousShowJob;
     
    private void refreshCoreObjects()
    {
        refreshCoreObjectsHistory();

        boolean showTrans = getActiveTransformation()!=null;
        boolean showJob = getActiveJob()!=null;
        
        if (showTrans==previousShowTrans && showJob==previousShowJob)
        {
            return;
        }
        
        // First remove all the entries that where present...
        ExpandItem[] expandItems = expandBar.getItems();
        for (int i = 0; i < expandItems.length; i++)
        {
            ExpandItem item = expandItems[i];
            item.getControl().dispose();
            item.dispose();
        }
        
        if (showTrans)
        {
            // Fill the base components...
            //
            //////////////////////////////////////////////////////////////////////////////////////////////////
            // TRANSFORMATIONS
            //////////////////////////////////////////////////////////////////////////////////////////////////
            
            String locale = LanguageChoice.getInstance().getDefaultLocale().toString().toLowerCase();
    
            StepLoader steploader = StepLoader.getInstance();
            StepPlugin basesteps[] = steploader.getStepsWithType(StepPlugin.TYPE_ALL);
            String basecat[] = steploader.getCategories(StepPlugin.TYPE_ALL, locale);
            
            for (int i=0;i<basecat.length;i++)
            {
                ScrolledComposite scrolledComposite = new ScrolledComposite(expandBar, SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER);
                scrolledComposite.setLayout(new FillLayout());
                scrolledComposite.addKeyListener(defKeys);
                scrolledComposite.addKeyListener(modKeys);
                
                final Composite composite = new Composite(scrolledComposite, SWT.NONE);
                props.setLook(composite);
                composite.addKeyListener(defKeys);
                composite.addKeyListener(modKeys);
                
                GridLayout layout = new GridLayout();
                layout.marginLeft = 20;
                layout.verticalSpacing = Const.MARGIN;
                composite.setLayout(layout);
                
                ExpandItem item = new ExpandItem(expandBar, SWT.NONE);

                for (int j=0;j<basesteps.length;j++)
                {
                    if (basesteps[j].getCategory(locale).equalsIgnoreCase(basecat[i]))
                    {
                        final Image stepimg = (Image)GUIResource.getInstance().getImagesStepsSmall().get(basesteps[j].getID()[0]);
                        String pluginName        = basesteps[j].getDescription(locale);
                        String pluginDescription = basesteps[j].getTooltip(locale);
                        boolean isPlugin = basesteps[j].isPlugin();
                        
                        addExpandBarItemLine(item, composite, stepimg, pluginName, pluginDescription, isPlugin, basesteps[j]);
                    }
                }
                
                composite.pack();
                org.eclipse.swt.graphics.Rectangle bounds = composite.getBounds();
                
                scrolledComposite.setMinSize(bounds.width, bounds.height);
                scrolledComposite.setContent(composite);
                scrolledComposite.setExpandHorizontal(true);
                scrolledComposite.setExpandVertical(true);
                
                item.setControl(scrolledComposite);
                item.setHeight(scrolledComposite.computeSize(SWT.DEFAULT, SWT.DEFAULT).y+10);
                setHeaderImage(item, GUIResource.getInstance().getImageArrow(), basecat[i], layout.marginLeft);
            }
        }
        
        if (showJob)
        {
            ScrolledComposite scrolledComposite = new ScrolledComposite(expandBar, SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER);
            scrolledComposite.setLayout(new FillLayout());
            
            Composite composite = new Composite(scrolledComposite, SWT.NONE);
            props.setLook(composite);
            
            GridLayout layout = new GridLayout();
            layout.marginLeft = 20;
            layout.verticalSpacing = Const.MARGIN;
            composite.setLayout(layout);
            
            ExpandItem item = new ExpandItem(expandBar, SWT.NONE);

            //////////////////////////////////////////////////////////////////////////////////////////////////
            // JOBS
            //////////////////////////////////////////////////////////////////////////////////////////////////

            // First add a few "Special entries: Start, Dummy, OK, ERROR
            //
            JobEntryCopy startEntry = JobMeta.createStartEntry();
            JobEntryCopy dummyEntry = JobMeta.createDummyEntry();
            
            String specialText[] = new String[] { startEntry.getName(), dummyEntry.getName(), };
            String specialTooltip[] = new String[] { startEntry.getDescription(), dummyEntry.getDescription(),};
            Image  specialImage[]= new Image[] { GUIResource.getInstance().getImageStartSmall(), GUIResource.getInstance().getImageDummySmall() };
            
            for (int i=0;i<specialText.length;i++)
            {
                addExpandBarItemLine(item, composite, specialImage[i], specialText[i], specialTooltip[i], false, specialText[i]);
            }
            
            JobEntryLoader jobEntryLoader = JobEntryLoader.getInstance();
            JobPlugin baseEntries[] = jobEntryLoader.getJobEntriesWithType(JobPlugin.TYPE_ALL);
            for (int i=0;i<baseEntries.length;i++)
            {
                if (!baseEntries[i].getID().equals("SPECIAL"))
                {
                    final Image stepimg = GUIResource.getInstance().getImagesJobentriesSmall().get(baseEntries[i].getID());
                    String pluginName        = baseEntries[i].getDescription();
                    String pluginDescription = baseEntries[i].getTooltip();
                    boolean isPlugin = baseEntries[i].isPlugin();
                    
                    addExpandBarItemLine(item, composite, stepimg, pluginName, pluginDescription, isPlugin, baseEntries[i]);
                }
            }

            composite.pack();
            org.eclipse.swt.graphics.Rectangle bounds = composite.getBounds();
            
            scrolledComposite.setMinSize(bounds.width, bounds.height);
            scrolledComposite.setContent(composite);
            scrolledComposite.setExpandHorizontal(true);
            scrolledComposite.setExpandVertical(true);
            
            item.setControl(scrolledComposite);
            setHeaderImage(item, GUIResource.getInstance().getImageArrow(), STRING_JOB_ENTRIES, layout.marginLeft);
            item.setHeight(scrolledComposite.computeSize(SWT.DEFAULT, SWT.DEFAULT).y+10);
            item.setExpanded(true);
            
            if (mainExpandBar.getItemCount()>2)
            {
                ExpandItem historyItem = mainExpandBar.getItem(2);
                historyItem.setExpanded(false);
                historyItem.getControl().dispose();
                historyItem.dispose();
            }
        }
                
        mainExpandBar.redraw();
        
        previousShowTrans = showTrans;
        previousShowJob =  showJob;        
    }
    
    private void addExpandBarItemLine(ExpandItem expandItem, Composite composite, Image image, String pluginName, String pluginDescription, boolean isPlugin, Object plugin)
    {
        // Add a line with the step as a new composite
        Composite lineComposite = new Composite(composite, SWT.NONE);
        props.setLook(lineComposite);
        lineComposite.setLayout(new FormLayout());
        lineComposite.addKeyListener(defKeys);
        lineComposite.addKeyListener(modKeys);
        
        Label canvas = new Label(lineComposite, SWT.NONE);
        canvas.setToolTipText(pluginDescription);
        props.setLook(canvas);
        canvas.setImage(image);
        FormData fdCanvas = new FormData();
        fdCanvas.left=new FormAttachment(0, 0);
        fdCanvas.right=new FormAttachment(0, image.getBounds().width);
        fdCanvas.top=new FormAttachment(0, 0);
        fdCanvas.bottom=new FormAttachment(0, image.getBounds().height);
        canvas.setLayoutData(fdCanvas);
        
        Label name = new Label(lineComposite, SWT.LEFT);
        props.setLook(name);
        name.setText(pluginName);
        name.setToolTipText(pluginDescription);
        if (isPlugin) name.setFont(GUIResource.getInstance().getFontBold());
        FormData fdName = new FormData();
        fdName.left=new FormAttachment(canvas,Const.MARGIN);
        fdName.right=new FormAttachment(100,0);
        fdName.top=new FormAttachment(canvas, 0, SWT.CENTER);
        name.setLayoutData(fdName);
        
        addDragSourceToLine(canvas, plugin);
        addDragSourceToLine(name, plugin);
    }

    private void addDragSourceToLine(final Control control, final Object plugin)
    {
        // Drag & Drop for steps
        Transfer[] ttypes = new Transfer[] { XMLTransfer.getInstance() };
        
        DragSource ddSource = new DragSource(control, DND.DROP_MOVE | DND.DROP_MOVE);
        ddSource.setTransfer(ttypes);
        ddSource.addDragListener(new DragSourceListener() 
            {
                public void dragStart(DragSourceEvent event){ }
    
                public void dragSetData(DragSourceEvent event) 
                {
                    int type=0;
                    String data=null;
                    
                    if (plugin instanceof StepPlugin)
                    {
                        StepPlugin stepPlugin = (StepPlugin) plugin;
                        type=DragAndDropContainer.TYPE_BASE_STEP_TYPE;
                        data = stepPlugin.getDescription(); // Step type description
                    }
                    if (plugin instanceof JobPlugin)
                    {
                        JobPlugin jobPlugin = (JobPlugin) plugin;
                        type=DragAndDropContainer.TYPE_BASE_JOB_ENTRY;
                        data = jobPlugin.getDescription(); // Job Entry type description
                    }
                    if (plugin instanceof String)
                    {
                        type=DragAndDropContainer.TYPE_BASE_JOB_ENTRY;
                        data = (String)plugin; // Job Entry type description
                    }

                    if (data!=null)
                    {
                        event.data = new DragAndDropContainer(type, data);
                    }
                    else
                    {
                        event.doit=false;
                    }
                }
    
                public void dragFinished(DragSourceEvent event) {}
            }
        );

    }

    
    
    
    
    protected void shareObject(SharedObjectInterface sharedObjectInterface)
    {
        sharedObjectInterface.setShared(true);
        refreshTree();
    }

    /**
     * @return The object that is selected in the tree or null if we couldn't figure it out. (titles etc. == null)
     */
    public TreeSelection[] getTreeObjects(final Tree tree)
    {
        List<TreeSelection> objects = new ArrayList<TreeSelection>();
        
        if (tree.equals(selectionTree))
        {
            TreeItem[] selection = selectionTree.getSelection();
            for (int s=0;s<selection.length;s++)
            {
                TreeItem treeItem = selection[s];
                String[] path = Const.getTreeStrings(treeItem);
                
                TreeSelection object = null;
                
                switch(path.length)
                {
                case 0: break;
                case 1: // ------complete-----
                    if (path[0].equals(STRING_TRANSFORMATIONS)) // the top level Transformations entry
                    {
                        object = new TreeSelection(path[0], TransMeta.class);
                    }
                    if (path[0].equals(STRING_JOBS)) // the top level Jobs entry
                    {
                        object = new TreeSelection(path[0], JobMeta.class);
                    }
                    break;
                    
                case 2: // ------complete-----
                    if (path[0].equals(STRING_BUILDING_BLOCKS)) // the top level Transformations entry
                    {
                        if (path[1].equals(STRING_TRANS_BASE))
                        {
                            object = new TreeSelection(path[1], StepPlugin.class);
                        }
                    }
                    if (path[0].equals(STRING_TRANSFORMATIONS)) // Transformations title
                    {
                        object = new TreeSelection(path[1], findTransformation(path[1]));
                    }
                    if (path[0].equals(STRING_JOBS)) // Jobs title
                    {
                        object = new TreeSelection(path[1], findJob(path[1]));
                    }
                    break;
                        
                case 3:  // ------complete-----
                    if (path[0].equals(STRING_TRANSFORMATIONS)) // Transformations title
                    {
                        TransMeta transMeta = findTransformation(path[1]);
                        if (path[2].equals(STRING_CONNECTIONS)) object = new TreeSelection(path[2], DatabaseMeta.class, transMeta);
                        if (path[2].equals(STRING_STEPS)) object = new TreeSelection(path[2], StepMeta.class, transMeta);
                        if (path[2].equals(STRING_HOPS)) object = new TreeSelection(path[2], TransHopMeta.class, transMeta);
                        if (path[2].equals(STRING_PARTITIONS)) object = new TreeSelection(path[2], PartitionSchema.class, transMeta);
                        if (path[2].equals(STRING_SLAVES)) object = new TreeSelection(path[2], SlaveServer.class, transMeta);
                        if (path[2].equals(STRING_CLUSTERS)) object = new TreeSelection(path[2], ClusterSchema.class, transMeta);
                    }
                    if (path[0].equals(STRING_JOBS)) // Jobs title
                    {
                        JobMeta jobMeta = findJob(path[1]);
                        if (path[2].equals(STRING_CONNECTIONS)) object = new TreeSelection(path[2], DatabaseMeta.class, jobMeta);
                        if (path[2].equals(STRING_JOB_ENTRIES)) object = new TreeSelection(path[2], JobEntryCopy.class, jobMeta);
                    }
                    break;
                    
                case 4:  // ------complete-----
                    if (path[0].equals(STRING_TRANSFORMATIONS)) // The name of a transformation
                    {
                        TransMeta transMeta = findTransformation(path[1]);
                        if (path[2].equals(STRING_CONNECTIONS)) object = new TreeSelection(path[3], transMeta.findDatabase(path[3]), transMeta);
                        if (path[2].equals(STRING_STEPS)) object = new TreeSelection(path[3], transMeta.findStep(path[3]), transMeta);
                        if (path[2].equals(STRING_HOPS)) object = new TreeSelection(path[3], transMeta.findTransHop(path[3]), transMeta);
                        if (path[2].equals(STRING_PARTITIONS)) object = new TreeSelection(path[3], transMeta.findPartitionSchema(path[3]), transMeta);
                        if (path[2].equals(STRING_SLAVES)) object = new TreeSelection(path[3], transMeta.findSlaveServer(path[3]), transMeta);
                        if (path[2].equals(STRING_CLUSTERS)) object = new TreeSelection(path[3], transMeta.findClusterSchema(path[3]), transMeta);
                    }
                    if (path[0].equals(STRING_JOBS)) // The name of a job
                    {
                        JobMeta jobMeta = findJob(path[1]);
                        if (jobMeta!=null && path[2].equals(STRING_CONNECTIONS)) object = new TreeSelection(path[3], jobMeta.findDatabase(path[3]), jobMeta);
                        if (jobMeta!=null && path[2].equals(STRING_JOB_ENTRIES)) object = new TreeSelection(path[3], jobMeta.findJobEntry(path[3]), jobMeta);
                    }
                    break;
                    
                case 5:
                    if (path[0].equals(STRING_TRANSFORMATIONS)) // The name of a transformation
                    {
                        TransMeta transMeta = findTransformation(path[1]);
                        if (transMeta!=null && path[2].equals(STRING_CLUSTERS))
                        {
                            ClusterSchema clusterSchema = transMeta.findClusterSchema(path[3]);
                            object = new TreeSelection(path[4], clusterSchema.findSlaveServer(path[4]), clusterSchema, transMeta);
                        }
                    }
                    break;
                default: break;
                }
                
                if (object!=null)
                {
                    objects.add(object);
                }
            }
        }
        if (tree.equals(coreObjectsTree))
        {
            TreeItem[] selection = coreObjectsTree.getSelection();
            for (int s=0;s<selection.length;s++)
            {
                TreeItem treeItem = selection[s];
                String[] path = Const.getTreeStrings(treeItem);
                
                TreeSelection object = null;
                
                switch(path.length)
                {
                case 0: break;
                case 1: break; // nothing
                case 2: // Job entries
                    if (path[0].equals(STRING_JOB_BASE))
                    {
                        JobPlugin jobPlugin = JobEntryLoader.getInstance().findJobEntriesWithDescription(path[1]);
                        if (jobPlugin!=null)
                        {
                            object = new TreeSelection(path[1], jobPlugin);
                        }
                        else
                        {
                            object = new TreeSelection(path[1], JobPlugin.class); // Special entries Start, Dummy, ...
                        }
                    }
                    break;
                case 3: // Steps
                    if (path[0].equals(STRING_TRANS_BASE))
                    {
                        object = new TreeSelection(path[2], StepLoader.getInstance().findStepPluginWithDescription(path[2]));
                    }
                    break;
                default: break;
                }
                
                if (object!=null)
                {
                    objects.add(object);
                }
            }
        }
        
        return  objects.toArray(new TreeSelection[objects.size()]);
    }
    
    private void addDragSourceToTree(final Tree tree)
    {
        // Drag & Drop for steps
        Transfer[] ttypes = new Transfer[] { XMLTransfer.getInstance() };
        
        DragSource ddSource = new DragSource(tree, DND.DROP_MOVE);
        ddSource.setTransfer(ttypes);
        ddSource.addDragListener(new DragSourceListener() 
            {
                public void dragStart(DragSourceEvent event){ }
    
                public void dragSetData(DragSourceEvent event) 
                {
                    TreeSelection[] treeObjects = getTreeObjects(tree);
                    if (treeObjects.length==0)
                    {
                        event.doit=false;
                        return;
                    }
                    
                    int type=0;
                    String data = null;
                    
                    TreeSelection treeObject = treeObjects[0];
                    Object object = treeObject.getSelection();
                    JobMeta jobMeta = getActiveJob();
                    
                    if (object instanceof StepMeta)
                    {
                        StepMeta stepMeta = (StepMeta)object;
                    	type = DragAndDropContainer.TYPE_STEP;
                        data=stepMeta.getName(); // name of the step.
                    }
                    else
                	if (object instanceof StepPlugin)
                    {
                        StepPlugin stepPlugin = (StepPlugin)object;
                    	type = DragAndDropContainer.TYPE_BASE_STEP_TYPE;
                        data=stepPlugin.getDescription(); // Step type description
                    }
                    else
                    if (object instanceof DatabaseMeta)
                    {
                        DatabaseMeta databaseMeta = (DatabaseMeta) object;
                    	type = DragAndDropContainer.TYPE_DATABASE_CONNECTION;
                        data=databaseMeta.getName();
                    }
                    else
                    if (object instanceof TransHopMeta)
                    {
                        TransHopMeta hop = (TransHopMeta) object;
                    	type = DragAndDropContainer.TYPE_TRANS_HOP;
                        data=hop.toString(); // nothing for really ;-)
                    }
                    else
                    if (object instanceof JobEntryCopy)
                    {
                        JobEntryCopy jobEntryCopy = (JobEntryCopy)object;
                        type = DragAndDropContainer.TYPE_JOB_ENTRY;
                        data=jobEntryCopy.getName(); // name of the job entry.
                    }
                    else
                    if (object instanceof JobPlugin)
                    {
                        JobPlugin jobPlugin = (JobPlugin)object;
                        type = DragAndDropContainer.TYPE_BASE_JOB_ENTRY;
                        data=jobPlugin.getDescription(); // Step type
                    }
                    else
                    if (object instanceof Class && object.equals(JobPlugin.class))
                    {
                        JobEntryCopy dummy = null;
                        if (jobMeta!=null) dummy = jobMeta.findJobEntry(JobMeta.STRING_SPECIAL_DUMMY, 0, true);
                        if (JobMeta.STRING_SPECIAL_DUMMY.equalsIgnoreCase(treeObject.getItemText()) && dummy!=null)
                        {
                            // if dummy already exists, add a copy
                            type = DragAndDropContainer.TYPE_JOB_ENTRY;
                            data=dummy.getName();
                        }
                        else
                        {
                            type = DragAndDropContainer.TYPE_BASE_JOB_ENTRY;
                            data = treeObject.getItemText();
                        }
                    }
                    else
                    {
                        event.doit=false;
                    	return; // ignore anything else you drag.
                    }

                	event.data = new DragAndDropContainer(type, data);
                }
    
                public void dragFinished(DragSourceEvent event) {}
            }
        );

    }

    /**
     * If you click in the tree, you might want to show the corresponding window.
     */
    public void showSelection()
    {
        TreeSelection[] objects = getTreeObjects(selectionTree);
        if (objects.length!=1) return; // not yet supported, we can do this later when the OSX bug goes away

        TreeSelection object = objects[0];
        
        final Object selection   = object.getSelection();
        final Object parent      = object.getParent();
        
        TransMeta transMeta = null;
        if (selection instanceof TransMeta) transMeta = (TransMeta) selection;
        if (parent instanceof TransMeta) transMeta = (TransMeta) parent;
        if (transMeta!=null)
        {
            // Search the corresponding TransGraph tab
            TabItem tabItem = findTabItem(makeTransGraphTabName(transMeta), TabMapEntry.OBJECT_TYPE_TRANSFORMATION_GRAPH);
            if (tabItem!=null)
            {
                int current = tabfolder.getSelectedIndex();
                int desired = tabfolder.indexOf(tabItem); 
                if (current!=desired) tabfolder.setSelected(desired);
                transMeta.setInternalKettleVariables();
                if ( getCoreObjectsState() != STATE_CORE_OBJECTS_SPOON )
                {
                	// Switch the core objects in the lower left corner to the spoon trans types
                    refreshCoreObjects();
                }                
            }
        }
        
        JobMeta jobMeta = null;
        if (selection instanceof JobMeta) jobMeta = (JobMeta) selection;
        if (parent instanceof JobMeta) jobMeta = (JobMeta) parent;
        if (jobMeta!=null)
        {
            // Search the corresponding TransGraph tab
            TabItem tabItem = findTabItem(makeJobGraphTabName(jobMeta), TabMapEntry.OBJECT_TYPE_JOB_GRAPH);
            if (tabItem!=null)
            {
                int current = tabfolder.getSelectedIndex();
                int desired = tabfolder.indexOf(tabItem); 
                if (current!=desired) tabfolder.setSelected(desired);
                jobMeta.setInternalKettleVariables();
                if ( getCoreObjectsState() != STATE_CORE_OBJECTS_CHEF )
                {
                	// Switch the core objects in the lower left corner to the spoon job types
                    refreshCoreObjects();
                }                
            }
        }        
    }
    
    private Object selectionObjectParent = null;
    private Object selectionObject = null;

    public void newHop() {
		newHop((TransMeta) selectionObjectParent);
	}
    
    public void sortHops() {
    		((TransMeta) selectionObjectParent).sortHops(); 
    		refreshTree();
    }
    
    public void newDatabasePartitioningSchema() {
    		newDatabasePartitioningSchema((TransMeta) selectionObjectParent);
    }
    
    public void newClusteringSchema() {
    		newClusteringSchema((TransMeta) selectionObjectParent);
    }
    
    public void newSlaveServer() {
    		newSlaveServer((TransMeta) selectionObjectParent);
    }
    
    public void editTransformationPropertiesPopup() {
    		((TransMeta)selectionObject).editProperties(this, rep);
    }
    
    public void addTransLog() {
    		addTransLog((TransMeta)selectionObject);
    }
    
    public void addTransHistory() {
    		addTransHistory((TransMeta)selectionObject, true);
    }
    
    public Map<String, Menu> getMenuMap() {
    		return menuMap;
    }
    
    public void editJobProperties( String id ) {
    		if( "job-settings".equals( id ) ) {
    			getActiveJob().editProperties(this, rep);
    		}
    		else if( "job-inst-settings".equals( id ) ) {
    			( (JobMeta)selectionObject).editProperties(this, rep);	
    		}
    }

    public void addJobLog() {
    		addJobLog((JobMeta)selectionObject);
    }
    
    public void addJobHistory() {
    		addJobHistory((JobMeta)selectionObject, true);
    }

    public void newStep() {
    		newStep(getActiveTransformation());
    }
    
    public void editConnection() {
    		final DatabaseMeta databaseMeta = (DatabaseMeta) selectionObject;
    		editConnection(databaseMeta);
    } 
    
    public void dupeConnection() {
		final DatabaseMeta databaseMeta = (DatabaseMeta) selectionObject;
        final HasDatabasesInterface hasDatabasesInterface = (HasDatabasesInterface) selectionObjectParent;
    		dupeConnection(hasDatabasesInterface, databaseMeta); 
    }
    
    public void clipConnection() {
		final DatabaseMeta databaseMeta = (DatabaseMeta) selectionObject;
    		clipConnection(databaseMeta);
    }
    
    public void delConnection() {
		final DatabaseMeta databaseMeta = (DatabaseMeta) selectionObject;
        final HasDatabasesInterface hasDatabasesInterface = (HasDatabasesInterface) selectionObjectParent;
    		delConnection(hasDatabasesInterface, databaseMeta);
    }
    
    public void sqlConnection() {
		final DatabaseMeta databaseMeta = (DatabaseMeta) selectionObject;
    		sqlConnection(databaseMeta);
    }
    
    public void clearDBCache( String id ) {
		if( "database-class-clear-cache".equals( id ) ) {
	        clearDBCache( (DatabaseMeta) null);
		}
		if( "database-inst-clear-cache".equals( id ) ) {
			final DatabaseMeta databaseMeta = (DatabaseMeta) selectionObject;
    		clearDBCache(databaseMeta);
		}
    }
    
    public void exploreDB() {
		final DatabaseMeta databaseMeta = (DatabaseMeta) selectionObject;
    		exploreDB(databaseMeta);
    }
    
    public void editStep() {
        final TransMeta transMeta = (TransMeta)selectionObjectParent;
        final StepMeta stepMeta = (StepMeta)selectionObject;
    		editStep(transMeta, stepMeta); 
    }

    public void dupeStep() {
        final TransMeta transMeta = (TransMeta)selectionObjectParent;
        final StepMeta stepMeta = (StepMeta)selectionObject;
    		dupeStep(transMeta, stepMeta);  
    }
    
    public void delStep() {
        final TransMeta transMeta = (TransMeta)selectionObjectParent;
        final StepMeta stepMeta = (StepMeta)selectionObject;
    		delStep(transMeta, stepMeta);  
    }
    
    public void shareObject( String id ) {
    		if( "database-inst-share".equals( id ) ) {
    			final DatabaseMeta databaseMeta = (DatabaseMeta) selectionObject;
        		shareObject(databaseMeta);
    		}
    		if( "step-inst-share".equals( id ) ) {
    	        final StepMeta stepMeta = (StepMeta)selectionObject;
        		shareObject(stepMeta); 
    		}
    		if( "partition-schema-inst-share".equals( id ) ) {
    	        final PartitionSchema partitionSchema = (PartitionSchema)selectionObject;
        		shareObject(partitionSchema);
    		}
    		if( "cluster-schema-inst-share".equals( id ) ) {
    		    final ClusterSchema clusterSchema = (ClusterSchema)selectionObject;
    			shareObject(clusterSchema);    		
    		}
    		if( "slave-server-inst-share".equals( id ) ) {
    		    final SlaveServer slaveServer = (SlaveServer)selectionObject;
    		    shareObject(slaveServer);
    		}
    }
    
    public void editJobEntry() {
        final JobMeta jobMeta = (JobMeta)selectionObjectParent;
        final JobEntryCopy jobEntry = (JobEntryCopy)selectionObject;
        editJobEntry(jobMeta, jobEntry); 
    }

    public void dupeJobEntry() {
        final JobMeta jobMeta = (JobMeta)selectionObjectParent;
        final JobEntryCopy jobEntry = (JobEntryCopy)selectionObject;
    		dupeJobEntry(jobMeta, jobEntry);
    }
    
    public void deleteJobEntryCopies() {
        final JobMeta jobMeta = (JobMeta)selectionObjectParent;
        final JobEntryCopy jobEntry = (JobEntryCopy)selectionObject;
    		deleteJobEntryCopies(jobMeta, jobEntry);
    }
    
    public void editHop() {
        final TransMeta transMeta = (TransMeta)selectionObjectParent;
        final TransHopMeta transHopMeta = (TransHopMeta)selectionObject;
        editHop(transMeta, transHopMeta);
    }
    
    public void delHop() {
        final TransMeta transMeta = (TransMeta)selectionObjectParent;
        final TransHopMeta transHopMeta = (TransHopMeta)selectionObject;
        delHop(transMeta, transHopMeta);
    }

    public void editPartitionSchema() {
        final TransMeta transMeta = (TransMeta)selectionObjectParent;
        final PartitionSchema partitionSchema = (PartitionSchema)selectionObject;
    		editPartitionSchema(transMeta, partitionSchema);
    }   
    
    public void delPartitionSchema() {
        final TransMeta transMeta = (TransMeta)selectionObjectParent;
        final PartitionSchema partitionSchema = (PartitionSchema)selectionObject;
    		delPartitionSchema(transMeta, partitionSchema);
    }   
    
    public void editClusterSchema() {
        final TransMeta transMeta = (TransMeta)selectionObjectParent;
        final ClusterSchema clusterSchema = (ClusterSchema)selectionObject;
    		editClusterSchema(transMeta, clusterSchema);
    }   

    public void delClusterSchema() {
        final TransMeta transMeta = (TransMeta)selectionObjectParent;
        final ClusterSchema clusterSchema = (ClusterSchema)selectionObject;
    		delClusterSchema(transMeta, clusterSchema);    
    	}   
    
    public void monitorClusterSchema() {
        final ClusterSchema clusterSchema = (ClusterSchema)selectionObject;
    		monitorClusterSchema(clusterSchema);
    }   

    public void editSlaveServer() {
        final SlaveServer slaveServer = (SlaveServer)selectionObject;
    		editSlaveServer(slaveServer);
    }   

    public void delSlaveServer() {
        final TransMeta transMeta = (TransMeta)selectionObjectParent;
        final SlaveServer slaveServer = (SlaveServer)selectionObject;
    		delSlaveServer(transMeta, slaveServer);
    }   

    public void addSpoonSlave() {
        final SlaveServer slaveServer = (SlaveServer)selectionObject;
    		addSpoonSlave(slaveServer);
    }   
    
    private void setMenu()
    {
    	
        TreeSelection[] objects = getTreeObjects(selectionTree);
        if (objects.length!=1) return; // not yet supported, we can do this later when the OSX bug goes away

        TreeSelection object = objects[0];
        
        selectionObject   = object.getSelection();
        Object selection = selectionObject;
        selectionObjectParent      = object.getParent();
        // final Object grandParent = object.getGrandParent();

        // Not clicked on a real object: returns a class
        if (selection instanceof Class)
        {
            if (selection.equals(TransMeta.class))
            {
                // New
        			spoonMenu = (Menu) menuMap.get( "trans-class" );
            }
            if (selection.equals(JobMeta.class))
            {
                // New
    				spoonMenu = (Menu) menuMap.get( "job-class" );
            }
            if (selection.equals(TransHopMeta.class))
            {
                // New
				spoonMenu = (Menu) menuMap.get( "trans-hop-class" );
			}
            if (selection.equals(DatabaseMeta.class))
            {
            		spoonMenu = (Menu) menuMap.get( "database-class" );
            }
            if (selection.equals(PartitionSchema.class))
            {
                // New
        			spoonMenu = (Menu) menuMap.get( "partition-schema-class" );
            }
            if (selection.equals(ClusterSchema.class))
            {
        			spoonMenu = (Menu) menuMap.get( "cluster-schema-class" );
            }
            if (selection.equals(SlaveServer.class))
            {
        			spoonMenu = (Menu) menuMap.get( "slave-cluster-class" );
            }
        }
        else
        {
        	
            if (selection instanceof TransMeta)
            {
    				spoonMenu = (Menu) menuMap.get( "trans-inst" );
            }
            if (selection instanceof JobMeta)
            {
				spoonMenu = (Menu) menuMap.get( "job-inst" );
            }
            if (selection instanceof StepPlugin)
            {
				spoonMenu = (Menu) menuMap.get( "step-plugin" );
            }

            if (selection instanceof DatabaseMeta)
            {
				spoonMenu = (PopupMenu) menuMap.get( "database-inst" );
                // disable for now if the connection is an SAP R/3 type of database...
				XulMenuItem item = ((XulPopupMenu)spoonMenu).getMenuItemById( "database-inst-explore" );
				if( item != null ) {
	                final DatabaseMeta databaseMeta = (DatabaseMeta) selection;
	                if (databaseMeta.getDatabaseType()==DatabaseMeta.TYPE_DATABASE_SAPR3) item.setEnabled(false);
				}
				item = ((XulPopupMenu)spoonMenu).getMenuItemById( "database-inst-clear-cache" );
				if( item != null ) {
					final DatabaseMeta databaseMeta = (DatabaseMeta) selectionObject;
					item.setText(Messages.getString("Spoon.Menu.Popup.CONNECTIONS.ClearDBCache")+databaseMeta.getName());//Clear DB Cache of 
				}

            }
            if (selection instanceof StepMeta)
            {
				spoonMenu = (Menu) menuMap.get( "step-inst" );
            }
            if (selection instanceof JobEntryCopy)
            {
				spoonMenu = (Menu) menuMap.get( "job-entry-copy-inst" );
            }
            if (selection instanceof TransHopMeta)
            {
				spoonMenu = (Menu) menuMap.get( "trans-hop-inst" );
            }
            if (selection instanceof PartitionSchema)
            {
				spoonMenu = (Menu) menuMap.get( "partition-schema-inst" );
            }
            if (selection instanceof ClusterSchema)
            {
				spoonMenu = (Menu) menuMap.get( "cluster-schema-inst" );
            }
            if (selection instanceof SlaveServer)
            {
				spoonMenu = (Menu) menuMap.get( "slave-server-inst" );
            }

        }
        if( spoonMenu != null ) {
            selectionTree.setMenu(spoonMenu.getSwtMenu());
        }
    }
    
    /**
     * Reaction to double click
     *
     */
    private void doubleClickedInTree()
    {        
        TreeSelection[] objects = getTreeObjects(selectionTree);
        if (objects.length!=1) return; // not yet supported, we can do this later when the OSX bug goes away

        TreeSelection object = objects[0];
        
        final Object selection   = object.getSelection();
        final Object parent      = object.getParent();
                
        if (selection instanceof Class)
        {
            if (selection.equals(TransMeta.class)) newTransFile();
            if (selection.equals(JobMeta.class)) newJobFile();
            if (selection.equals(TransHopMeta.class)) newHop((TransMeta)parent);
            if (selection.equals(DatabaseMeta.class)) newConnection();
            if (selection.equals(PartitionSchema.class)) newDatabasePartitioningSchema((TransMeta)parent);
            if (selection.equals(ClusterSchema.class)) newClusteringSchema((TransMeta)parent);
            if (selection.equals(SlaveServer.class)) newSlaveServer((TransMeta)parent);
        }
        else
        {
            if (selection instanceof TransMeta) ((TransMeta)selection).editProperties(this, rep);
            if (selection instanceof JobMeta) ((JobMeta)selection).editProperties(this, rep);
            if (selection instanceof StepPlugin) newStep(getActiveTransformation());
            if (selection instanceof DatabaseMeta) editConnection((DatabaseMeta) selection);
            if (selection instanceof StepMeta) editStep((TransMeta)parent, (StepMeta)selection);
            if (selection instanceof JobEntryCopy) editJobEntry((JobMeta)parent, (JobEntryCopy)selection);
            if (selection instanceof TransHopMeta) editHop((TransMeta)parent, (TransHopMeta)selection);
            if (selection instanceof PartitionSchema) editPartitionSchema((HasDatabasesInterface)parent, (PartitionSchema)selection);
            if (selection instanceof ClusterSchema) editClusterSchema((TransMeta)parent, (ClusterSchema)selection);
            if (selection instanceof SlaveServer) editSlaveServer((SlaveServer)selection);
        }
    }
    
    protected void monitorClusterSchema(ClusterSchema clusterSchema)
    {
        for (int i=0;i<clusterSchema.getSlaveServers().size();i++)
        {
            SlaveServer slaveServer = clusterSchema.getSlaveServers().get(i);
            addSpoonSlave(slaveServer);
        }
    }
    
    protected void editSlaveServer(SlaveServer slaveServer)
    {
        SlaveServerDialog dialog = new SlaveServerDialog(shell, slaveServer);
        if (dialog.open())
        {
            refreshTree();
            refreshGraph();
        }
    }
    

    private void addTabs()
    {
        if (tabComp!=null)
        {
            tabComp.dispose();
        }
        
        tabComp = new Composite(sashform, SWT.BORDER );
        props.setLook(tabComp);
        tabComp.setLayout(new FillLayout());
        
        tabfolder= new TabSet(tabComp);
        tabfolder.setChangedFont( GUIResource.getInstance().getFontBold() );
        tabfolder.setUnchangedFont( GUIResource.getInstance().getFontGraph() );
        props.setLook(tabfolder.getSwtTabset(), Props.WIDGET_STYLE_TAB);
                
        tabfolder.addKeyListener(defKeys);
        tabfolder.addKeyListener(modKeys);
        
        // tabfolder.setSelection(0);
        
        sashform.addKeyListener(defKeys);
        sashform.addKeyListener(modKeys);
        
        int weights[] = props.getSashWeights();
        sashform.setWeights(weights);
        sashform.setVisible(true);      
        
        tabfolder.addListener( this );
        
    }

	public void tabDeselected( TabItem item ) {
		
	}

	public boolean tabClose(TabItem item) {
        // Try to find the tab-item that's being closed.
        List<TabMapEntry> collection = new ArrayList<TabMapEntry>();
        collection.addAll(tabMap.values());
        
        boolean close = true;
        for (TabMapEntry entry : collection)
        {
            if (item.equals(entry.getTabItem())) 
            {
                TabItemInterface itemInterface = entry.getObject();
                
                // Can we close this tab?
                if (!itemInterface.canBeClosed())
                {
                    int reply = itemInterface.showChangedWarning();
                    if (reply==SWT.YES)
                    {
                        close=itemInterface.applyChanges();
                    }
                    else
                    {
                        if (reply==SWT.CANCEL)
                        {
                            close = false;
                        }
                        else
                        {
                            close = true;
                        }
                    }
                }
                                
                // Also clean up the log/history associated with this transformation/job
                //                            
                if (close)
                {
                    if (entry.getObject() instanceof TransGraph)
                    {
                        TransMeta transMeta = (TransMeta)entry.getObject().getManagedObject();
                        closeTransformation(transMeta);
                        refreshTree();
                    }
                    else if (entry.getObject() instanceof JobGraph)
                    {
                        JobMeta jobMeta = (JobMeta)entry.getObject().getManagedObject();
                        closeJob(jobMeta);
                        refreshTree();
                    }
                    else if (entry.getObject() instanceof SpoonBrowser)
                    {
                        closeSpoonBrowser();
                        refreshTree();
                    }
                    
                    if (entry.getObject() instanceof Composite)
                    {
                        Composite comp = (Composite)entry.getObject();
                        if (comp != null && !comp.isDisposed())
                            comp.dispose();
                    }
                }                            
            }
        }
        return close;
    }

	public TabSet getTabSet() {
		return tabfolder;
	}
	
	public void tabSelected( TabItem item ) {
        ArrayList<TabMapEntry> collection = new ArrayList<TabMapEntry>();
        collection.addAll(tabMap.values());
		
        // See which core objects to show
        //
        for (TabMapEntry entry : collection)
        {
            if (item.equals(entry.getTabItem())) 
            {
                // TabItemInterface itemInterface = entry.getObject();
                
                //
                // Another way to implement this may be to keep track of the
                // state of the core object tree in method addCoreObjectsToTree()
                //
                if ( entry.getObject() instanceof TransGraph || entry.getObject() instanceof JobGraph)
                {
                	EngineMetaInterface meta = entry.getObject().getMeta();
                   if ( meta != null )
                   {
                	   meta.setInternalKettleVariables();
                   }
                   if ( getCoreObjectsState() != STATE_CORE_OBJECTS_SPOON )
                   {
                       refreshCoreObjects();
                   }
                }
            }
        }
        
        // Also refresh the tree
        refreshTree();
        enableMenus();
	}        	

    public String getRepositoryName()
    {
        if (rep==null) return null;
        return rep.getRepositoryInfo().getName();
    }

    public void sqlConnection(DatabaseMeta databaseMeta)
    {
        SQLEditor sql = new SQLEditor(shell, SWT.NONE, databaseMeta, DBCache.getInstance(), "");
        sql.open();
    }
    
    public void editConnection(DatabaseMeta databaseMeta)
    {
        HasDatabasesInterface hasDatabasesInterface = getActiveHasDatabasesInterface();
        if (hasDatabasesInterface==null) return; // program error, exit just to make sure.
        
        DatabaseMeta before = (DatabaseMeta)databaseMeta.clone();

        DatabaseDialog con = new DatabaseDialog(shell, databaseMeta);
        con.setDatabases(hasDatabasesInterface.getDatabases());
        String newname = con.open(); 
        if (!Const.isEmpty(newname))  // null: CANCEL
        {                
            // newname = db.verifyAndModifyDatabaseName(transMeta.getDatabases(), name);
            
            // Store undo/redo information
            DatabaseMeta after = (DatabaseMeta)databaseMeta.clone();
            addUndoChange((UndoInterface)hasDatabasesInterface, new DatabaseMeta[] { before }, new DatabaseMeta[] { after }, new int[] { hasDatabasesInterface.indexOfDatabase(databaseMeta) } );
            
            saveConnection(databaseMeta);
            
            refreshTree();
        }
        setShellText();
    }

    public void dupeConnection(HasDatabasesInterface hasDatabasesInterface, DatabaseMeta databaseMeta)
    {
        String name = databaseMeta.getName();
        int pos = hasDatabasesInterface.indexOfDatabase(databaseMeta);                
        if (databaseMeta!=null)
        {
            DatabaseMeta databaseMetaCopy = (DatabaseMeta)databaseMeta.clone();
            String dupename = Messages.getString("Spoon.Various.DupeName") +name; //"(copy of) "
            databaseMetaCopy.setName(dupename);

            DatabaseDialog con = new DatabaseDialog(shell, databaseMetaCopy);
            String newname = con.open(); 
            if (newname != null)  // null: CANCEL
            {
                databaseMetaCopy.verifyAndModifyDatabaseName(hasDatabasesInterface.getDatabases(), name);
                hasDatabasesInterface.addDatabase(pos+1, databaseMetaCopy);
                addUndoNew((UndoInterface)hasDatabasesInterface, new DatabaseMeta[] { (DatabaseMeta)databaseMetaCopy.clone() }, new int[] { pos+1 });
                saveConnection(databaseMetaCopy);             
                refreshTree();
            }
        }
    }
    
    public void clipConnection(DatabaseMeta databaseMeta)
    {
        String xml = XMLHandler.getXMLHeader() + databaseMeta.getXML();
        toClipboard(xml);
    }


    /**
     * Delete a database connection
     * @param name The name of the database connection.
     */
    public void delConnection(HasDatabasesInterface hasDatabasesInterface, DatabaseMeta db)
    {
        int pos = hasDatabasesInterface.indexOfDatabase(db);                
        boolean worked=false;
        
        // delete from repository?
        if (rep!=null)
        {
            if (!rep.getUserInfo().isReadonly())
            {
                try
                {
                    long id_database = rep.getDatabaseID(db.getName());
                    rep.delDatabase(id_database);
        
                    worked=true;
                }
                catch(KettleException dbe)
                {
                    
                    new ErrorDialog(shell, Messages.getString("Spoon.Dialog.ErrorDeletingConnection.Title"), Messages.getString("Spoon.Dialog.ErrorDeletingConnection.Message",db.getName()), dbe);//"Error deleting connection ["+db+"] from repository!"
                }
            }
            else
            {
                new ErrorDialog(shell, Messages.getString("Spoon.Dialog.ErrorDeletingConnection.Title"),Messages.getString("Spoon.Dialog.ErrorDeletingConnection.Message",db.getName()) , new KettleException(Messages.getString("Spoon.Dialog.Exception.ReadOnlyUser")));//"Error deleting connection ["+db+"] from repository!" //This user is read-only!
            }
        }

        if (rep==null || worked)
        {
            addUndoDelete((UndoInterface)hasDatabasesInterface, new DatabaseMeta[] { (DatabaseMeta)db.clone() }, new int[] { pos });
            hasDatabasesInterface.removeDatabase(pos);
        }

        refreshTree();
        setShellText();
    }
    
    public String editStep(TransMeta transMeta, StepMeta stepMeta)
    {
        boolean refresh = false;
        try
        {
            String name = stepMeta.getName();

            // Before we do anything, let's store the situation the way it was...
            StepMeta before = (StepMeta) stepMeta.clone();
            StepMetaInterface stepint = stepMeta.getStepMetaInterface();
            StepDialogInterface dialog = stepint.getDialog(shell, stepMeta.getStepMetaInterface(), transMeta, name);
            dialog.setRepository(rep);
            String stepname = dialog.open();

            if (stepname != null)
            {
                // 
                // See if the new name the user enter, doesn't collide with another step.
                // If so, change the stepname and warn the user!
                //
                String newname = stepname;
                StepMeta smeta = transMeta.findStep(newname, stepMeta);
                int nr = 2;
                while (smeta != null)
                {
                    newname = stepname + " " + nr;
                    smeta = transMeta.findStep(newname);
                    nr++;
                }
                if (nr > 2)
                {
                    stepname = newname;
                    MessageBox mb = new MessageBox(shell, SWT.OK | SWT.ICON_INFORMATION);
                    mb.setMessage(Messages.getString("Spoon.Dialog.StepnameExists.Message", stepname)); // $NON-NLS-1$
                    mb.setText(Messages.getString("Spoon.Dialog.StepnameExists.Title")); // $NON-NLS-1$
                    mb.open();
                }
                
                if (!stepname.equals(name)) refresh=true;
                
                stepMeta.setName(stepname);
                
                // 
                // OK, so the step has changed...
                // Backup the situation for undo/redo
                //
                StepMeta after = (StepMeta) stepMeta.clone();
                addUndoChange(transMeta, new StepMeta[] { before }, new StepMeta[] { after }, new int[] { transMeta.indexOfStep(stepMeta) });
            }
            else
            {
                // Scenario: change connections and click cancel...
                // Perhaps new connections were created in the step dialog?
                if (transMeta.haveConnectionsChanged())
                {
                    refresh=true;
                }
            }
            refreshGraph(); // name is displayed on the graph too.
            
            // TODO: verify "double pathway" steps for bug #4365
            // After the step was edited we can complain about the possible deadlock here.
            //
        }
        catch (Throwable e)
        {
            if (shell.isDisposed()) return null;
            new ErrorDialog(shell, Messages.getString("Spoon.Dialog.UnableOpenDialog.Title"), Messages.getString("Spoon.Dialog.UnableOpenDialog.Message"), new Exception(e));//"Unable to open dialog for this step"
        }
        
        if (refresh) refreshTree(); // Perhaps new connections were created in the step dialog or the step name changed.
        
        return stepMeta.getName();
    }
    
    public void editStepErrorHandling(TransMeta transMeta, StepMeta stepMeta)
    {
        if (stepMeta!=null && stepMeta.supportsErrorHandling())
        {
            StepErrorMeta stepErrorMeta = stepMeta.getStepErrorMeta();
            if (stepErrorMeta==null)
            {
                stepErrorMeta = new StepErrorMeta(transMeta, stepMeta);
            }
            List<StepMeta> targetSteps = new ArrayList<StepMeta>();
            int nrNext = transMeta.findNrNextSteps(stepMeta);
            for (int i=0;i<nrNext;i++) targetSteps.add( transMeta.findNextStep(stepMeta, i) );
            
            // now edit this stepErrorMeta object:
            StepErrorMetaDialog dialog = new StepErrorMetaDialog(shell, stepErrorMeta, transMeta, targetSteps);
            if (dialog.open())
            {
                stepMeta.setStepErrorMeta(stepErrorMeta);
                refreshGraph();
            }
        }
    }


    public void dupeStep(TransMeta transMeta, StepMeta stepMeta)
    {
        log.logDebug(toString(), Messages.getString("Spoon.Log.DuplicateStep")+stepMeta.getName());//Duplicate step: 
        
        StepMeta stMeta = (StepMeta)stepMeta.clone();
        if (stMeta!=null)
        {
            String newname = transMeta.getAlternativeStepname(stepMeta.getName());
            int nr=2;
            while (transMeta.findStep(newname)!=null)
            {
                newname = stepMeta.getName()+" (copy "+nr+")";
                nr++;
            }
            stMeta.setName(newname);
            // Don't select this new step!
            stMeta.setSelected(false);
            Point loc = stMeta.getLocation();
            stMeta.setLocation(loc.x+20, loc.y+20);
            transMeta.addStep(stMeta);
            addUndoNew(transMeta, new StepMeta[] { (StepMeta)stMeta.clone() }, new int[] { transMeta.indexOfStep(stMeta) });
            refreshTree();
            refreshGraph();
        }
    }

    public void clipStep(StepMeta stepMeta)
    {
        String xml = stepMeta.getXML();
        toClipboard(xml);
    }

    public void pasteXML(TransMeta transMeta, String clipcontent, Point loc)
    {
        try
        {
            Document doc = XMLHandler.loadXMLString(clipcontent);
            Node transnode = XMLHandler.getSubNode(doc, "transformation");
            // De-select all, re-select pasted steps...
            transMeta.unselectAll();
            
            Node stepsnode = XMLHandler.getSubNode(transnode, "steps");
            int nr = XMLHandler.countNodes(stepsnode, "step");
            log.logDebug(toString(), Messages.getString("Spoon.Log.FoundSteps",""+nr)+loc);//"I found "+nr+" steps to paste on location: "
            StepMeta steps[] = new StepMeta[nr];
            
            //Point min = new Point(loc.x, loc.y);
            Point min = new Point(99999999,99999999);

            // Load the steps...
            for (int i=0;i<nr;i++)
            {
                Node stepnode = XMLHandler.getSubNodeByNr(stepsnode, "step", i);
                steps[i] = new StepMeta(stepnode, transMeta.getDatabases(), transMeta.getCounters());

                if (loc!=null)
                {
                    Point p = steps[i].getLocation();
                    
                    if (min.x > p.x) min.x = p.x;
                    if (min.y > p.y) min.y = p.y;
                }
            }
            
            // Load the hops...
            Node hopsnode = XMLHandler.getSubNode(transnode, "order");
            nr = XMLHandler.countNodes(hopsnode, "hop");
            log.logDebug(toString(), Messages.getString("Spoon.Log.FoundHops",""+nr));//"I found "+nr+" hops to paste."
            TransHopMeta hops[] = new TransHopMeta[nr];
            
            ArrayList<StepMeta> alSteps = new ArrayList<StepMeta>();
            for (int i=0;i<steps.length;i++) alSteps.add(steps[i]);
            
            for (int i=0;i<nr;i++)
            {
                Node hopnode = XMLHandler.getSubNodeByNr(hopsnode, "hop", i);
                hops[i] = new TransHopMeta(hopnode, alSteps);
            }

            // What's the difference between loc and min?
            // This is the offset:
            Point offset = new Point(loc.x-min.x, loc.y-min.y);
            
            // Undo/redo object positions...
            int position[] = new int[steps.length];
            
            for (int i=0;i<steps.length;i++)
            {
                Point p = steps[i].getLocation();
                String name = steps[i].getName();
                
                steps[i].setLocation(p.x+offset.x, p.y+offset.y);
                steps[i].setDraw(true);
                
                // Check the name, find alternative...
                steps[i].setName( transMeta.getAlternativeStepname(name) );
                transMeta.addStep(steps[i]);
                position[i] = transMeta.indexOfStep(steps[i]);
                steps[i].setSelected(true);
            }
            
            // Add the hops too...
            for (int i=0;i<hops.length;i++)
            {
                transMeta.addTransHop(hops[i]);
            }

            // Load the notes...
            Node notesnode = XMLHandler.getSubNode(transnode, "notepads");
            nr = XMLHandler.countNodes(notesnode, "notepad");
            log.logDebug(toString(), Messages.getString("Spoon.Log.FoundNotepads",""+nr));//"I found "+nr+" notepads to paste."
            NotePadMeta notes[] = new NotePadMeta[nr];
            
            for (int i=0;i<notes.length;i++)
            {
                Node notenode = XMLHandler.getSubNodeByNr(notesnode, "notepad", i);
                notes[i] = new NotePadMeta(notenode);
                Point p = notes[i].getLocation();
                notes[i].setLocation(p.x+offset.x, p.y+offset.y);
                transMeta.addNote(notes[i]);
                notes[i].setSelected(true);
            }
            
            // Set the source and target steps ...
            for (int i=0;i<steps.length;i++)
            {
                StepMetaInterface smi = steps[i].getStepMetaInterface();
                smi.searchInfoAndTargetSteps(transMeta.getSteps());
            }
            
            // Save undo information too...
            addUndoNew(transMeta, steps, position, false);

            int hoppos[] = new int[hops.length];
            for (int i=0;i<hops.length;i++) hoppos[i] = transMeta.indexOfTransHop(hops[i]);
            addUndoNew(transMeta, hops, hoppos, true);
            
            int notepos[] = new int[notes.length];
            for (int i=0;i<notes.length;i++) notepos[i] = transMeta.indexOfNote(notes[i]);
            addUndoNew(transMeta, notes, notepos, true);
    
            if (transMeta.haveStepsChanged())
            {
                refreshTree();
                refreshGraph();
            }
        }
        catch(KettleException e)
        {
            new ErrorDialog(shell, Messages.getString("Spoon.Dialog.UnablePasteSteps.Title"),Messages.getString("Spoon.Dialog.UnablePasteSteps.Message") , e);//"Error pasting steps...", "I was unable to paste steps to this transformation"
        }
    }
    
    public void copySelected(TransMeta transMeta, StepMeta stepMeta[], NotePadMeta notePadMeta[])
    {
        if (stepMeta==null || stepMeta.length==0) return;
        
        String xml = XMLHandler.getXMLHeader();
        xml+="<transformation>"+Const.CR;
        xml+=" <steps>"+Const.CR;

        for (int i=0;i<stepMeta.length;i++)
        {
            xml+=stepMeta[i].getXML();
        }
        
        xml+="    </steps>"+Const.CR;
        
        // 
        // Also check for the hops in between the selected steps...
        //
        
        xml+="<order>"+Const.CR;
        if (stepMeta!=null)
        for (int i=0;i<stepMeta.length;i++)
        {
            for (int j=0;j<stepMeta.length;j++)
            {
                if (i!=j)
                {
                    TransHopMeta hop = transMeta.findTransHop(stepMeta[i], stepMeta[j], true); 
                    if (hop!=null) // Ok, we found one...
                    {
                        xml+=hop.getXML()+Const.CR;
                    }
                }
            }
        }
        xml+="  </order>"+Const.CR;
        
        xml+="  <notepads>"+Const.CR;
        if (notePadMeta!=null)
        for (int i=0;i<notePadMeta.length;i++)
        {
            xml+= notePadMeta[i].getXML();
        }
        xml+="   </notepads>"+Const.CR; 

        xml+=" </transformation>"+Const.CR;
        
        toClipboard(xml);
    }

    public void delStep(TransMeta transMeta, StepMeta stepMeta)
    {
        log.logDebug(toString(), Messages.getString("Spoon.Log.DeleteStep")+stepMeta.getName());//"Delete step: "
        
        for (int i=transMeta.nrTransHops()-1;i>=0;i--)
        {
            TransHopMeta hi = transMeta.getTransHop(i);
            if ( hi.getFromStep().equals(stepMeta) || hi.getToStep().equals(stepMeta) )
            {
                addUndoDelete(transMeta, new TransHopMeta[] { hi }, new int[] { transMeta.indexOfTransHop(hi) }, true);
                transMeta.removeTransHop(i);
                refreshTree();
            }
        }
        
        int pos = transMeta.indexOfStep(stepMeta);
        transMeta.removeStep(pos);
        addUndoDelete(transMeta, new StepMeta[] { stepMeta }, new int[] { pos });
        
        refreshTree();
        refreshGraph();
    }   

    public void editHop(TransMeta transMeta, TransHopMeta transHopMeta)
    {
        // Backup situation BEFORE edit:
        String name = transHopMeta.toString();
        TransHopMeta before = (TransHopMeta)transHopMeta.clone();
        
        TransHopDialog hd = new TransHopDialog(shell, SWT.NONE, transHopMeta, transMeta);
        if (hd.open()!=null)
        {
            // Backup situation for redo/undo:
            TransHopMeta after = (TransHopMeta)transHopMeta.clone();
            addUndoChange(transMeta, new TransHopMeta[] { before }, new TransHopMeta[] { after }, new int[] { transMeta.indexOfTransHop(transHopMeta) } );
        
            String newname = transHopMeta.toString();
            if (!name.equalsIgnoreCase(newname)) 
            {
                refreshTree(); 
                refreshGraph(); // color, nr of copies...
            }
        }
        setShellText();
    }

    public void delHop(TransMeta transMeta, TransHopMeta transHopMeta)
    {
        int i = transMeta.indexOfTransHop(transHopMeta);
        addUndoDelete(transMeta, new Object[] { (TransHopMeta)transHopMeta.clone() }, new int[] { transMeta.indexOfTransHop(transHopMeta) });
        transMeta.removeTransHop(i);
        refreshTree();
        refreshGraph();
    }

    public void newHop(TransMeta transMeta, StepMeta fr, StepMeta to)
    {
        TransHopMeta hi = new TransHopMeta(fr, to);
        
        TransHopDialog hd = new TransHopDialog(shell, SWT.NONE, hi, transMeta);
        if (hd.open()!=null)
        {
            newHop(transMeta, hi);
        }
    }
    
    public void newHop(TransMeta transMeta, TransHopMeta transHopMeta)
    {
        if (checkIfHopAlreadyExists(transMeta, transHopMeta))
        {
            transMeta.addTransHop(transHopMeta);
            int idx = transMeta.indexOfTransHop(transHopMeta);
            
            if (!performNewTransHopChecks(transMeta, transHopMeta))
            {
                // Some error occurred: loops, existing hop, etc.
                // Remove it again...
                //
                transMeta.removeTransHop(idx);
            }
            else
            {
                addUndoNew(transMeta, new TransHopMeta[] { transHopMeta }, new int[] { transMeta.indexOfTransHop(transHopMeta) });
            }
            
            // Just to make sure
            transHopMeta.getFromStep().drawStep();
            transHopMeta.getToStep().drawStep();
            
            refreshTree();
            refreshGraph();
        }
    }
    
    /**
     * @param transMeta
     * @param newHop
     * @return true when the hop was added, false if there was an error
     */
    public boolean checkIfHopAlreadyExists(TransMeta transMeta, TransHopMeta newHop)
    {
        boolean ok = true;
        if (transMeta.findTransHop(newHop.getFromStep(), newHop.getToStep())!=null)
        {
            MessageBox mb = new MessageBox(shell, SWT.OK | SWT.ICON_ERROR );
            mb.setMessage(Messages.getString("Spoon.Dialog.HopExists.Message"));//"This hop already exists!"
            mb.setText(Messages.getString("Spoon.Dialog.HopExists.Title"));//Error!
            mb.open();
            ok=false;
        }
        
        return ok;
    }

    
    /**
     * @param transMeta
     * @param newHop
     * @return true when the hop was added, false if there was an error
     */
    public boolean performNewTransHopChecks(TransMeta transMeta, TransHopMeta newHop)
    {
        boolean ok = true;

        if (transMeta.hasLoop(newHop.getFromStep()) || transMeta.hasLoop(newHop.getToStep()))
        {
            MessageBox mb = new MessageBox(shell, SWT.YES | SWT.ICON_WARNING);
            mb.setMessage(Messages.getString("TransGraph.Dialog.HopCausesLoop.Message")); //$NON-NLS-1$
            mb.setText(Messages.getString("TransGraph.Dialog.HopCausesLoop.Title")); //$NON-NLS-1$
            mb.open();
            ok=false;
        }
        
        try
        {
            if (!newHop.getToStep().getStepMetaInterface().excludeFromRowLayoutVerification())
            {
                transMeta.checkRowMixingStatically(newHop.getToStep(), null);
            }
        }
        catch(KettleRowException re)
        {
            // Show warning about mixing rows with conflicting layouts...
            new ErrorDialog(shell, Messages.getString("TransGraph.Dialog.HopCausesRowMixing.Title"), Messages.getString("SpoonGraph.Dialog.HopCausesRowMixing.Message"), re);
        }
        
        verifyCopyDistribute(transMeta, newHop.getFromStep());
        
        return ok;
    }


    public void verifyCopyDistribute(TransMeta transMeta, StepMeta fr)
    {
        int nrNextSteps = transMeta.findNrNextSteps(fr);
        
        // don't show it for 3 or more hops, by then you should have had the message
        if (nrNextSteps==2) 
        {
            boolean distributes = false;
            
            if (props.showCopyOrDistributeWarning())
            {
                MessageDialogWithToggle md = new MessageDialogWithToggle(shell, 
                        Messages.getString("System.Warning"),//"Warning!" 
                        null,
                        Messages.getString("Spoon.Dialog.CopyOrDistribute.Message", fr.getName(), Integer.toString(nrNextSteps)),
                        MessageDialog.WARNING,
                        new String[] { Messages.getString("Spoon.Dialog.CopyOrDistribute.Copy"), Messages.getString("Spoon.Dialog.CopyOrDistribute.Distribute") },//"Copy Distribute 
                        0,
                        Messages.getString("Spoon.Message.Warning.NotShowWarning"),//"Please, don't show this warning anymore."
                        !props.showCopyOrDistributeWarning()
                    );
                int idx = md.open();
                props.setShowCopyOrDistributeWarning(!md.getToggleState());
                props.saveProps();
                
                distributes = (idx&0xFF)==1;
            }
               
            if (distributes)
            {
                fr.setDistributes(true);
            }
            else
            {
                fr.setDistributes(false);
            }
            
            refreshTree();
            refreshGraph();
        }
    }

    public void newHop(TransMeta transMeta)
    {
        newHop(transMeta, null, null);
    }
    
    public void newConnection()
    {
        HasDatabasesInterface hasDatabasesInterface = getActiveHasDatabasesInterface();
        if (hasDatabasesInterface==null) return;
        
        DatabaseMeta databaseMeta = new DatabaseMeta(); 
        DatabaseDialog con = new DatabaseDialog(shell, databaseMeta);
        String con_name = con.open(); 
        if (!Const.isEmpty(con_name))
        {
            databaseMeta.verifyAndModifyDatabaseName(hasDatabasesInterface.getDatabases(), null);
            hasDatabasesInterface.addDatabase(databaseMeta);
            addUndoNew((UndoInterface)hasDatabasesInterface, new DatabaseMeta[] { (DatabaseMeta)databaseMeta.clone() }, new int[] { hasDatabasesInterface.indexOfDatabase(databaseMeta) });
            saveConnection(databaseMeta);
            refreshTree();
        }
    }
    
    public void saveConnection(DatabaseMeta db)
    {
        // Also add to repository?
        if (rep!=null)
        {
            if (!rep.userinfo.isReadonly())
            {
                try
                {
                    rep.lockRepository();
                    rep.insertLogEntry("Saving database '"+db.getName()+"'");
                    
                    db.saveRep(rep);
                    log.logDetailed(toString(), Messages.getString("Spoon.Log.SavedDatabaseConnection",db.getDatabaseName()));//"Saved database connection ["+db+"] to the repository."
                    
                    // Put a commit behind it!
                    rep.commit();
                    
                    db.setChanged(false);
                }
                catch(KettleException ke)
                {
                    rep.rollback(); // In case of failure: undo changes!
                    new ErrorDialog(shell, Messages.getString("Spoon.Dialog.ErrorSavingConnection.Title"),Messages.getString("Spoon.Dialog.ErrorSavingConnection.Message",db.getDatabaseName()), ke);//"Can't save...","Error saving connection ["+db+"] to repository!"
                }
                finally
                {
                    try
                    {
                        rep.unlockRepository();
                    }
                    catch(KettleException e)
                    {
                        new ErrorDialog(shell, "Error", "Unexpected error unlocking the repository database", e);
                    }

                }
            }
            else
            {
                new ErrorDialog(shell, Messages.getString("Spoon.Dialog.UnableSave.Title"),Messages.getString("Spoon.Dialog.ErrorSavingConnection.Message",db.getDatabaseName()), new KettleException(Messages.getString("Spoon.Dialog.Exception.ReadOnlyRepositoryUser")));//This repository user is read-only!
            }
        }
    }
    
    public void openRepository()
    {
        int perms[] = new int[] { PermissionMeta.TYPE_PERMISSION_TRANSFORMATION };
        RepositoriesDialog rd = new RepositoriesDialog(display, perms, APP_NAME);
        rd.getShell().setImage(GUIResource.getInstance().getImageSpoon());
        if (rd.open())
        {
            // Close previous repository...
            
            if (rep!=null) 
            {
                rep.disconnect();
            }
            
            rep = new Repository(log, rd.getRepository(), rd.getUser());
            try
            {
                rep.connect(APP_NAME);
            }
            catch(KettleException ke)
            {
                rep=null;
                new ErrorDialog(shell, Messages.getString("Spoon.Dialog.ErrorConnectingRepository.Title"), Messages.getString("Spoon.Dialog.ErrorConnectingRepository.Message",Const.CR), ke); //$NON-NLS-1$ //$NON-NLS-2$
            }
            
            TransMeta transMetas[] = getLoadedTransformations();
            for (int t=0;t<transMetas.length;t++)
            {
                TransMeta transMeta = transMetas[t];
                
                for (int i=0;i<transMeta.nrDatabases();i++) 
                {
                    transMeta.getDatabase(i).setID(-1L);
                }
            
                // Set for the existing transformation the ID at -1!
                transMeta.setID(-1L);

                // Keep track of the old databases for now.
                List<DatabaseMeta> oldDatabases = transMeta.getDatabases();
            
                // In order to re-match the databases on name (not content), we need to load the databases from the new repository.
                // NOTE: for purposes such as DEVELOP - TEST - PRODUCTION sycles.
                
                // first clear the list of databases, partition schemas, slave servers, clusters
                transMeta.setDatabases(new ArrayList<DatabaseMeta>());
                transMeta.setPartitionSchemas(new ArrayList<PartitionSchema>());
                transMeta.setSlaveServers(new ArrayList<SlaveServer>());
                transMeta.setClusterSchemas(new ArrayList<ClusterSchema>());
    
                // Read them from the new repository.
                try
                {
                    transMeta.readSharedObjects(rep);
                }
                catch(KettleException e)
                {
                    new ErrorDialog(shell, Messages.getString("Spoon.Dialog.ErrorReadingSharedObjects.Title"), Messages.getString("Spoon.Dialog.ErrorReadingSharedObjects.Message", makeTransGraphTabName(transMeta)), e);
                }
            
                // Then we need to re-match the databases at save time...
                for (int i=0;i<oldDatabases.size();i++)
                {
                    DatabaseMeta oldDatabase = oldDatabases.get(i);
                    DatabaseMeta newDatabase = DatabaseMeta.findDatabase(transMeta.getDatabases(), oldDatabase.getName());
                    
                    // If it exists, change the settings...
                    if (newDatabase!=null)
                    {
                        // 
                        // A database connection with the same name exists in the new repository.
                        // Change the old connections to reflect the settings in the new repository 
                        //
                        oldDatabase.setDatabaseInterface(newDatabase.getDatabaseInterface());
                    }
                    else
                    {
                        // 
                        // The old database is not present in the new repository: simply add it to the list.
                        // When the transformation gets saved, it will be added to the repository.
                        //
                        transMeta.addDatabase(oldDatabase);
                    }
                }
                
                // For the existing transformation, change the directory too:
                // Try to find the same directory in the new repository...
                RepositoryDirectory redi = rep.getDirectoryTree().findDirectory(transMeta.getDirectory().getPath());
                if (redi!=null)
                {
                    transMeta.setDirectory(redi);
                }
                else
                {
                    transMeta.setDirectory(rep.getDirectoryTree()); // the root is the default!
                }
            }
            
            refreshTree();
            setShellText();
        }
        else
        {
            // Not cancelled? --> Clear repository...
            if (!rd.isCancelled())
            {
                closeRepository();
            }
        }
    }
    
    public void exploreRepository()
    {
        if (rep!=null)
        {
            RepositoryExplorerDialog erd = new RepositoryExplorerDialog(shell, SWT.NONE, rep, rep.getUserInfo());
            String objname = erd.open();
            if (objname!=null)
            {
                String object_type = erd.getObjectType();
                RepositoryDirectory repdir = erd.getObjectDirectory();
                
                // Try to open the selected transformation.
                if (object_type.equals(RepositoryExplorerDialog.STRING_TRANSFORMATIONS))
                {
                    try
                    {
                        TransLoadProgressDialog progressDialog = new TransLoadProgressDialog(shell, rep, objname, repdir);
                        TransMeta transMeta = progressDialog.open();
                        transMeta.clearChanged();
                        transMeta.setFilename(objname);
                        addTransGraph(transMeta);
                        refreshTree();
                        refreshGraph();
                    }
                    catch(Exception e)
                    {
                        MessageBox mb = new MessageBox(shell, SWT.OK | SWT.ICON_ERROR );
                        mb.setMessage(Messages.getString("Spoon.Dialog.ErrorOpening.Message")+objname+Const.CR+e.getMessage());//"Error opening : "
                        mb.setText(Messages.getString("Spoon.Dialog.ErrorOpening.Title"));
                        mb.open();
                    }
                }
                else
                // Try to open the selected job.
                if (object_type.equals(RepositoryExplorerDialog.STRING_JOBS))
                {
                    try
                    {
                        JobLoadProgressDialog progressDialog = new JobLoadProgressDialog(shell, rep, objname, repdir);
                        JobMeta jobMeta = progressDialog.open();
                        jobMeta.clearChanged();
                        jobMeta.setFilename(objname);
                        addJobGraph(jobMeta);
                        refreshTree();
                        refreshGraph();
                    }
                    catch(Exception e)
                    {
                        MessageBox mb = new MessageBox(shell, SWT.OK | SWT.ICON_ERROR );
                        mb.setMessage(Messages.getString("Spoon.Dialog.ErrorOpening.Message")+objname+Const.CR+e.getMessage());//"Error opening : "
                        mb.setText(Messages.getString("Spoon.Dialog.ErrorOpening.Title"));
                        mb.open();
                    }
                }
                
            }
        }
    }
    
    public void editRepositoryUser()
    {
        if (rep!=null)
        {
            UserInfo userinfo = rep.getUserInfo();
            UserDialog ud = new UserDialog(shell, SWT.NONE, rep, userinfo);
            UserInfo ui = ud.open();
            if (!userinfo.isReadonly())
            {
                if (ui!=null)
                {
                    try
                    {
                        ui.saveRep(rep);
                    }
                    catch(KettleException e)
                    {
                        MessageBox mb = new MessageBox(shell, SWT.ICON_WARNING | SWT.OK);
                        mb.setMessage(Messages.getString("Spoon.Dialog.UnableChangeUser.Message")+Const.CR+e.getMessage());//Sorry, I was unable to change this user in the repository: 
                        mb.setText(Messages.getString("Spoon.Dialog.UnableChangeUser.Title"));//"Edit user"
                        mb.open();
                    }
                }
            }
            else
            {
                MessageBox mb = new MessageBox(shell, SWT.ICON_WARNING | SWT.OK);
                mb.setMessage(Messages.getString("Spoon.Dialog.NotAllowedChangeUser.Message"));//"Sorry, you are not allowed to change this user."
                mb.setText(Messages.getString("Spoon.Dialog.NotAllowedChangeUser.Title"));
                mb.open();
            }       
        }
    }
    
    public void closeRepository()
    {
        if (rep!=null) rep.disconnect();
        rep = null;
        setShellText();
    }

    public void openFile() {
    		openFile( false );
    }
    
    public void importFile() {
    		openFile( true );
    }
    
    public void openFile(boolean importfile)
    {
        if (rep==null || importfile)  // Load from XML
        {
            FileDialog dialog = new FileDialog(shell, SWT.OPEN);
            dialog.setFilterExtensions(Const.STRING_TRANS_AND_JOB_FILTER_EXT);
            dialog.setFilterNames(Const.getTransformationAndJobFilterNames());
            String fname = dialog.open();
            if (fname!=null)
            {
                openFile(fname, importfile);
            }
        }
        else
        {
            SelectObjectDialog sod = new SelectObjectDialog(shell, rep);
            if (sod.open()!=null)
            {
                String type = sod.getObjectType();
                String name = sod.getObjectName();
                RepositoryDirectory repdir  = sod.getDirectory();
                
                // Load a transformation
                if (RepositoryObject.STRING_OBJECT_TYPE_TRANSFORMATION.equals(type))
                {
                    TransLoadProgressDialog tlpd = new TransLoadProgressDialog(shell, rep, name, repdir);
                    TransMeta transMeta = tlpd.open();
                    if (transMeta!=null)
                    {
                        log.logDetailed(toString(),Messages.getString("Spoon.Log.LoadToTransformation",name,repdir.getDirectoryName()) );//"Transformation ["+transname+"] in directory ["+repdir+"] loaded from the repository."
                        props.addLastFile(LastUsedFile.FILE_TYPE_TRANSFORMATION, name, repdir.getPath(), true, rep.getName());
                        addMenuLast();
                        transMeta.clearChanged();
                        transMeta.setFilename(name);
                        addTransGraph(transMeta);
                    }
                    refreshGraph();
                    refreshTree();
                    refreshHistory();
                }
                else
                // Load a job
                if (RepositoryObject.STRING_OBJECT_TYPE_JOB.equals(type))
                {
                    JobLoadProgressDialog jlpd = new JobLoadProgressDialog(shell, rep, name, repdir);
                    JobMeta jobMeta = jlpd.open();
                    if (jobMeta!=null)
                    {
                        props.addLastFile(LastUsedFile.FILE_TYPE_JOB, name, repdir.getPath(), true, rep.getName());
                        saveSettings();
                        addMenuLast();
                        addJobGraph(jobMeta);
                    }
                    refreshGraph();
                    refreshTree();
                }
            }
        }
    }
    
    private String lastFileOpened="";
//    private String lastVfsUsername="";
//    private String lastVfsPassword="";
    
    public void openFileVFSFile()
    {
      FileObject initialFile = null;
      FileObject rootFile = null;
      try {
        initialFile = KettleVFS.getFileObject(lastFileOpened);
        rootFile = initialFile.getFileSystem().getRoot();
      } catch (IOException e) {
        e.printStackTrace();
        String message = e.getMessage();
        if (e.getCause() != null) {
          message = e.getCause().getMessage();
        }
        MessageBox messageDialog = new MessageBox(shell, SWT.ICON_ERROR | SWT.OK);
        messageDialog.setText("Error");
        messageDialog.setMessage(message);
        messageDialog.open();

        // bring up a dialog to prompt for userid/password and try again
        // lastVfsUsername = "";
        // lastVfsPassword = "";
//        if (lastFileOpened != null && lastFileOpened.indexOf("@") == -1) {
//          lastFileOpened = lastFileOpened.substring(0, lastFileOpened.indexOf("//")+2) + lastVfsUsername + ":" + lastVfsPassword + "@" + lastFileOpened.substring(lastFileOpened.indexOf("//")+2);
//        }
//        openFileVFSFile();
        return;
      }
      
      VfsFileChooserDialog vfsFileChooser = new VfsFileChooserDialog(rootFile, initialFile);
      FileObject selectedFile = vfsFileChooser.open(shell, null, Const.STRING_TRANS_AND_JOB_FILTER_EXT, Const.getTransformationAndJobFilterNames(), VfsFileChooserDialog.VFS_DIALOG_OPEN);
      if (selectedFile != null) {
        lastFileOpened=selectedFile.getName().getFriendlyURI();
        openFile(selectedFile.getName().getFriendlyURI(), false);
      }      
    }
    
    public void addFileListener( FileListener listener, String extension, String rootNodeName ) {
    	fileExtensionMap.put( extension, listener );
    	fileNodeMap.put( rootNodeName, listener );
    }
    
    public void openFile(String fname, boolean importfile)
    {
        // Open the XML and see what's in there.
        // We expect a single <transformation> or <job> root at this time...
        try
        {
        	
            boolean loaded = false;
            FileListener listener = null;
        	// match by extension first
        	int idx = fname.lastIndexOf('.');
        	if( idx != -1 ) {
            	String extension = fname.substring(  idx + 1 );
            	listener = fileExtensionMap.get( extension );
        	}
        	// otherwise try by looking at the root node
            Document document = XMLHandler.loadXMLFile(fname);
            Node root = document.getFirstChild();
        	if( listener == null ) {
            	listener = fileNodeMap.get( root.getNodeName() );
        	}
        	
        	if( listener != null ) {
        		loaded = listener.open(root, fname, importfile);
        	}
            if (!loaded)
            {
                // Give error back
                MessageBox mb = new MessageBox(shell, SWT.OK | SWT.ICON_ERROR );
                mb.setMessage(Messages.getString("Spoon.UnknownFileType.Message", fname));
                mb.setText(Messages.getString("Spoon.UnknownFileType.Title"));
                mb.open();
            }
        }
        catch(KettleException e)
        {
            new ErrorDialog(shell, Messages.getString("Spoon.Dialog.ErrorOpening.Title"), Messages.getString("Spoon.Dialog.ErrorOpening.Message")+fname, e);
        }
    }

    public Props getProperties() {
    	return props;
    }
    
    public void newFile()
    {
        String[] choices = new String[] { STRING_TRANSFORMATION, STRING_JOB };
        EnterSelectionDialog enterSelectionDialog = new EnterSelectionDialog(shell, choices, Messages.getString("Spoon.Dialog.NewFile.Title"), Messages.getString("Spoon.Dialog.NewFile.Message"));
        if (enterSelectionDialog.open()!=null)
        {
            switch( enterSelectionDialog.getSelectionNr() )
            {
            case 0: newTransFile(); break;
            case 1: newJobFile(); break;
            }
        }
    }
    
    public void newTransFile()
    {
        TransMeta transMeta = new TransMeta();
        try
        {
            transMeta.readSharedObjects(rep);
            transMeta.clearChanged();
        }
        catch(Exception e)
        {
            new ErrorDialog(shell, Messages.getString("Spoon.Exception.ErrorReadingSharedObjects.Title"), Messages.getString("Spoon.Exception.ErrorReadingSharedObjects.Message"), e);
        }
        int nr=1;
        transMeta.setName( STRING_TRANSFORMATION+" "+nr );
        
        // See if a transformation with the same name isn't already loaded...
        while (findTransformation(makeTransGraphTabName(transMeta))!=null)
        {
            nr++;
            transMeta.setName( STRING_TRANSFORMATION+" "+nr ); // rename
        }
        addTransGraph(transMeta);
        refreshTree();
    }
    
    public boolean isDefaultTransformationName(String name)
    {
        if (!name.startsWith(STRING_TRANSFORMATION)) return false;
        
        // see if there are only digits behind the transformation...
        // This will detect:
        //   "Transformation" 
        //   "Transformation " 
        //   "Transformation 1" 
        //   "Transformation 2"
        //   ...
        for (int i=STRING_TRANSFORMATION.length()+1;i<name.length();i++)
        {
            if (!Character.isDigit(name.charAt(i))) return false;
        }
        return true;
    }
    
    public void newJobFile()
    {
        try
        {
            JobMeta jobMeta = new JobMeta(log);
            try
            {
                jobMeta.readSharedObjects(rep);
            }
            catch(KettleException e)
            {
                new ErrorDialog(shell, Messages.getString("Spoon.Dialog.ErrorReadingSharedObjects.Title"), Messages.getString("Spoon.Dialog.ErrorReadingSharedObjects.Message", makeJobGraphTabName(jobMeta)), e);
            }
            
            int nr=1;
            jobMeta.setName( STRING_JOB+" "+nr );
            
            // See if a transformation with the same name isn't already loaded...
            while (findJob(makeJobGraphTabName(jobMeta))!=null)
            {
                nr++;
                jobMeta.setName( STRING_JOB+" "+nr ); // rename
            }

            addJobGraph(jobMeta);
            refreshTree();
        }
        catch(Exception e)
        {
            new ErrorDialog(shell, Messages.getString("Spoon.Exception.ErrorCreatingNewJob.Title"), Messages.getString("Spoon.Exception.ErrorCreatingNewJob.Message"), e);
        }
    }
    
    public boolean isDefaultJobName(String name)
    {
        if (!name.startsWith(STRING_JOB)) return false;
        
        // see if there are only digits behind the job...
        // This will detect:
        //   "Job" 
        //   "Job " 
        //   "Job 1" 
        //   "Job 2"
        //   ...
        for (int i=STRING_JOB.length()+1;i<name.length();i++)
        {
            if (!Character.isDigit(name.charAt(i))) return false;
        }
        return true;
    }

    public void loadRepositoryObjects(TransMeta transMeta)
    {
        // Load common database info from active repository...
        if (rep!=null)
        {
            try
            {
                transMeta.readSharedObjects(rep);
            }
            catch(Exception e)
            {
                new ErrorDialog(shell, Messages.getString("Spoon.Error.UnableToLoadSharedObjects.Title"), Messages.getString("Spoon.Error.UnableToLoadSharedObjects.Message"), e);
            }

        }
    }
    
    public boolean quitFile()
    {
        log.logDetailed(toString(), Messages.getString("Spoon.Log.QuitApplication"));//"Quit application."

        boolean exit        = true;
        
        saveSettings();

        if (props.showExitWarning())
        {
            MessageDialogWithToggle md = new MessageDialogWithToggle(shell, 
                    Messages.getString("System.Warning"),//"Warning!" 
                    null,
                    Messages.getString("Spoon.Message.Warning.PromptExit"), // Are you sure you want to exit?
                    MessageDialog.WARNING,
                    new String[] { Messages.getString("Spoon.Message.Warning.Yes"), Messages.getString("Spoon.Message.Warning.No") },//"Yes", "No" 
                    1,
                    Messages.getString("Spoon.Message.Warning.NotShowWarning"),//"Please, don't show this warning anymore."
                    !props.showExitWarning()
              );
            int idx = md.open();
            props.setExitWarningShown(!md.getToggleState());
            props.saveProps();
            if ((idx&0xFF)==1) return false; // No selected: don't exit!
        }
           
        
        
        // Check all tabs to see if we can close them...
        List<TabMapEntry> list = new ArrayList<TabMapEntry>();
        list.addAll(tabMap.values());
        
        for (TabMapEntry mapEntry : list)
        {
            TabItemInterface itemInterface = mapEntry.getObject();
            
            if (!itemInterface.canBeClosed())
            {
                // Show the tab
                tabfolder.setSelected( mapEntry.getTabItem() );
                
                // Unsaved work that needs to changes to be applied?
                //
                int reply= itemInterface.showChangedWarning();
                if (reply==SWT.YES)
                {
                    exit=itemInterface.applyChanges();
                }
                else
                {
                    if (reply==SWT.CANCEL)
                    {
                        exit = false;
                    }
                    else // SWT.NO
                    {
                        exit = true;
                    }
                }
            }
        }
        
        if (exit) // we have asked about it all and we're still here.  Now close all the tabs, stop the running transformations
        {
            for (TabMapEntry mapEntry : list)
            {
                if (!mapEntry.getObject().canBeClosed())
                {
                    // Unsaved transformation?
                    //
                    if (mapEntry.getObject() instanceof TransGraph)
                    {
                        TransMeta transMeta = (TransMeta) mapEntry.getObject().getManagedObject();
                        if (transMeta.hasChanged())
                        {
                            mapEntry.getTabItem().dispose();
                        }
                    }
                    // A running transformation?
                    //
                    if (mapEntry.getObject() instanceof TransLog)
                    {
                        TransLog transLog = (TransLog) mapEntry.getObject();
                        if (transLog.isRunning())
                        {
                            transLog.stop();
                            mapEntry.getTabItem().dispose();
                        }
                    }
                }
            }
        }
          

        if (exit) dispose();
            
        return exit;
    }
    
    public boolean saveFile()
    {
        TransMeta transMeta = getActiveTransformation();
        if (transMeta!=null) return saveToFile(transMeta);

        JobMeta jobMeta = getActiveJob();
        if (jobMeta!=null) return saveToFile(jobMeta);

        return false;
    }
    
    public boolean saveToFile(EngineMetaInterface meta)
    {
        if (meta==null) return false;
        
        boolean saved=false;
        
        log.logDetailed(toString(), Messages.getString("Spoon.Log.SaveToFileOrRepository"));//"Save to file or repository..."
        
        if (rep!=null)
        {
            saved=saveToRepository(meta);
        }
        else
        {
            if (meta.getFilename()!=null)
            {
                saved=save(meta, meta.getFilename());
            }
            else
            {
                saved=saveFileAs(meta);
            }
        }
        
        if (saved) // all was OK
        {
            saved=meta.saveSharedObjects();
        }
        
        try
        {
            if (props.useDBCache() && meta instanceof TransMeta) ((TransMeta)meta).getDbCache().saveCache(log);
        }
        catch(KettleException e)
        {
            new ErrorDialog(shell, Messages.getString("Spoon.Dialog.ErrorSavingDatabaseCache.Title"), Messages.getString("Spoon.Dialog.ErrorSavingDatabaseCache.Message"), e);//"An error occured saving the database cache to disk"
        }
        
        renameTabs(); // filename or name of transformation might have changed.
        refreshTree();

        return saved;
    }
    
    public boolean saveToRepository(EngineMetaInterface meta)
    {
        return saveToRepository(meta, false);
    }

    public boolean saveToRepository(EngineMetaInterface meta, boolean ask_name)
    {
        log.logDetailed(toString(), Messages.getString("Spoon.Log.SaveToRepository"));//"Save to repository..."
        if (rep!=null)
        {
            boolean answer = true;
            boolean ask    = ask_name;
            while (answer && ( ask || Const.isEmpty(meta.getName()) ) )
            {
                if (!ask)
                {
                    MessageBox mb = new MessageBox(shell, SWT.OK | SWT.ICON_WARNING);
                    mb.setMessage(Messages.getString("Spoon.Dialog.PromptTransformationName.Message"));//"Please give this transformation a name before saving it in the database."
                    mb.setText(Messages.getString("Spoon.Dialog.PromptTransformationName.Title"));//"Transformation has no name."
                    mb.open();
                }
                ask=false;
                answer = meta.editProperties(this, rep);
            }
            
            if (answer && !Const.isEmpty(meta.getName()))
            {
                if (!rep.getUserInfo().isReadonly())
                {
                    int response = SWT.YES;
                    if (meta.showReplaceWarning(rep))
                    {
                        MessageBox mb = new MessageBox(shell, SWT.YES | SWT.NO | SWT.ICON_QUESTION);
                        mb.setMessage(Messages.getString("Spoon.Dialog.PromptOverwriteTransformation.Message",meta.getName(),Const.CR));//"There already is a transformation called ["+transMeta.getName()+"] in the repository."+Const.CR+"Do you want to overwrite the transformation?"
                        mb.setText(Messages.getString("Spoon.Dialog.PromptOverwriteTransformation.Title"));//"Overwrite?"
                        response = mb.open();
                    }
                    
                    boolean saved=false;
                    if (response == SWT.YES)
                    {
                        shell.setCursor(cursor_hourglass);
						
						// Keep info on who & when this transformation was created...
						if (  meta.getCreatedUser()==null || meta.getCreatedUser().equals("-"))
						{
							meta.setCreatedDate( new Date() );   
							meta.setCreatedUser( rep.getUserInfo().getLogin() );
						}
						else
						{

							meta.setCreatedDate( meta.getCreatedDate() );                 
							meta.setCreatedUser( meta.getCreatedUser());
						}

                        // Keep info on who & when this transformation was changed...
                        meta.setModifiedDate( new Date() );   
                        meta.setModifiedUser( rep.getUserInfo().getLogin() );

                        SaveProgressDialog tspd = new SaveProgressDialog(shell, rep, meta);
                        if (tspd.open())
                        {
                            saved=true;
                            if (!props.getSaveConfirmation())
                            {
                                MessageDialogWithToggle md = new MessageDialogWithToggle(shell, 
                                     Messages.getString("Spoon.Message.Warning.SaveOK"), //"Save OK!"
                                     null,
                                     Messages.getString("Spoon.Message.Warning.TransformationWasStored"),//"This transformation was stored in repository"
                                     MessageDialog.QUESTION,
                                     new String[] { Messages.getString("Spoon.Message.Warning.OK") },//"OK!"
                                     0,
                                     Messages.getString("Spoon.Message.Warning.NotShowThisMessage"),//"Don't show this message again."
                                     props.getSaveConfirmation()
                                     );
                                md.open();
                                props.setSaveConfirmation(md.getToggleState());
                            }
    
                            // Handle last opened files...
                            props.addLastFile(LastUsedFile.FILE_TYPE_TRANSFORMATION, meta.getName(), meta.getDirectory().getPath(), true, getRepositoryName());
                            saveSettings();
                            addMenuLast();
    
                            setShellText();
                        }
                        shell.setCursor(null);
                    }
                    return saved;
                }
                else
                {
                    MessageBox mb = new MessageBox(shell, SWT.CLOSE | SWT.ICON_ERROR);
                    mb.setMessage(Messages.getString("Spoon.Dialog.OnlyreadRepository.Message"));//"Sorry, the user you're logged on with, can only read from the repository"
                    mb.setText(Messages.getString("Spoon.Dialog.OnlyreadRepository.Title"));//"Transformation not saved!"
                    mb.open();
                }
            }
        }
        else
        {
            MessageBox mb = new MessageBox(shell, SWT.OK | SWT.ICON_ERROR);
            mb.setMessage(Messages.getString("Spoon.Dialog.NoRepositoryConnection.Message"));//"There is no repository connection available."
            mb.setText(Messages.getString("Spoon.Dialog.NoRepositoryConnection.Title"));//"No repository available."
            mb.open();
        }
        return false;
    }

    public boolean saveJobRepository(JobMeta jobMeta)
    {
        return saveToRepository(jobMeta, false);
    }

    public boolean saveJobRepository(JobMeta jobMeta, boolean ask_name)
    {
        log.logDetailed(toString(), "Save to repository..."); //$NON-NLS-1$
        if (rep!=null)
        {
            boolean answer = true;
            boolean ask    = ask_name;
            while (answer && ( ask || jobMeta.getName()==null || jobMeta.getName().length()==0 ) )
            {
                if (!ask)
                {
                    MessageBox mb = new MessageBox(shell, SWT.OK | SWT.ICON_WARNING);
                    mb.setMessage(Messages.getString("Spoon.Dialog.GiveJobANameBeforeSaving.Message")); //$NON-NLS-1$
                    mb.setText(Messages.getString("Spoon.Dialog.GiveJobANameBeforeSaving.Title")); //$NON-NLS-1$
                    mb.open();
                }
                ask=false;
                answer = jobMeta.editProperties(this, rep);
            }
            
            if (answer && jobMeta.getName()!=null && jobMeta.getName().length()>0)
            {
                if (!rep.getUserInfo().isReadonly())
                {
                    boolean saved=false;
                    int response = SWT.YES;
                    if (jobMeta.showReplaceWarning(rep))
                    {
                        MessageBox mb = new MessageBox(shell, SWT.YES | SWT.NO | SWT.ICON_QUESTION);
                        mb.setMessage("'"+jobMeta.getName()+"'"+Const.CR+Const.CR+Messages.getString("Spoon.Dialog.FileExistsOverWrite.Message")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                        mb.setText(Messages.getString("Spoon.Dialog.FileExistsOverWrite.Title")); //$NON-NLS-1$
                        response = mb.open();
                    }
                    
                    if (response == SWT.YES)
                    {

						// Keep info on who & when this job was created...
						if (  jobMeta.getCreatedUser()==null || jobMeta.getCreatedUser().equals("-"))
						{
							jobMeta.setCreatedDate( new Date() );
							jobMeta.setCreatedUser( rep.getUserInfo().getLogin() );
						}
						else
						{

							jobMeta.setCreatedDate( jobMeta.getCreatedDate() );                 
							jobMeta.setCreatedUser( jobMeta.getCreatedUser());
						}


                        // Keep info on who & when this job was changed...
                        jobMeta.modifiedDate = new Date();
                        jobMeta.modifiedUser = rep.getUserInfo().getLogin();

                        JobSaveProgressDialog jspd = new JobSaveProgressDialog(shell, rep, jobMeta);
                        if (jspd.open())
                        {
                            if (!props.getSaveConfirmation())
                            {
                                MessageDialogWithToggle md = new MessageDialogWithToggle(shell, 
                                     Messages.getString("Spoon.Dialog.JobWasStoredInTheRepository.Title"),  //$NON-NLS-1$
                                     null,
                                     Messages.getString("Spoon.Dialog.JobWasStoredInTheRepository.Message"), //$NON-NLS-1$
                                     MessageDialog.QUESTION,
                                     new String[] { Messages.getString("System.Button.OK") }, //$NON-NLS-1$
                                     0,
                                     Messages.getString("Spoon.Dialog.JobWasStoredInTheRepository.Toggle"), //$NON-NLS-1$
                                     props.getSaveConfirmation()
                                     );
                                md.open();
                                props.setSaveConfirmation(md.getToggleState());
                            }
    
                            // Handle last opened files...
                            props.addLastFile(LastUsedFile.FILE_TYPE_JOB, jobMeta.getName(), jobMeta.getDirectory().getPath(), true, rep.getName());
                            saveSettings();
                            addMenuLast();
    
                            setShellText();
                            
                            saved=true;
                        }
                    }
                    return saved;
                }
                else
                {
                    MessageBox mb = new MessageBox(shell, SWT.CLOSE | SWT.ICON_ERROR);
                    mb.setMessage(Messages.getString("Spoon.Dialog.UserCanOnlyReadFromTheRepositoryJobNotSaved.Message")); //$NON-NLS-1$
                    mb.setText(Messages.getString("Spoon.Dialog.UserCanOnlyReadFromTheRepositoryJobNotSaved.Title")); //$NON-NLS-1$
                    mb.open();
                }
            }
        }
        else
        {
            MessageBox mb = new MessageBox(shell, SWT.OK | SWT.ICON_ERROR);
            mb.setMessage(Messages.getString("Spoon.Dialog.NoRepositoryConnectionAvailable.Message")); //$NON-NLS-1$
            mb.setText(Messages.getString("Spoon.Dialog.NoRepositoryConnectionAvailable.Title")); //$NON-NLS-1$
            mb.open();
        }
        return false;
    }

    public boolean saveFileAs()
    {
        TransMeta transMeta = getActiveTransformation();
        if (transMeta!=null) return saveFileAs(transMeta);

        JobMeta jobMeta = getActiveJob();
        if (jobMeta!=null) return saveFileAs(jobMeta);

        return false;
    }
    
    public boolean saveFileAs(EngineMetaInterface meta)
    {
        boolean saved=false;
        
        log.logBasic(toString(), Messages.getString("Spoon.Log.SaveAs"));//"Save as..."

        if (rep!=null)
        {
            meta.setID(-1L);
            saved=saveToRepository(meta, true);
            renameTabs();
        }
        else
        {
            saved=saveXMLFile(meta);
            renameTabs();
        }
        
        refreshTree();
        
        return saved;
    }
    
    public boolean saveXMLFile()
    {
        TransMeta transMeta = getActiveTransformation();
        if (transMeta!=null) return saveXMLFile( (EngineMetaInterface) transMeta);

        JobMeta jobMeta = getActiveJob();
        if (jobMeta!=null) return saveXMLFile( (EngineMetaInterface) jobMeta);
        
        return false;
    }
    
    public boolean saveXMLFile(EngineMetaInterface meta)
    {
        log.logBasic(toString(), "Save file as..."); //$NON-NLS-1$
        boolean saved=false;


        FileDialog dialog = new FileDialog(shell, SWT.SAVE);
        //dialog.setFilterPath("C:\\Projects\\kettle\\source\\");
        String extensions[] = meta.getFilterExtensions();
        dialog.setFilterExtensions(extensions);
        dialog.setFilterNames     (meta.getFilterNames());
        String fname = dialog.open();
        if (fname!=null) 
        {
            // Is the filename ending on .ktr, .xml?
            boolean ending=false;
            for (int i=0;i<extensions.length-1;i++)
            {
                if (fname.endsWith(extensions[i].substring(1))) ending=true;
            }
            if (fname.endsWith(meta.getDefaultExtension())) ending=true;
            if (!ending)
            {
                fname+=meta.getDefaultExtension();
            }
            // See if the file already exists...
            int id = SWT.YES;
            try
            {
                FileObject f = KettleVFS.getFileObject(fname);
                if (f.exists())
                {
                    MessageBox mb = new MessageBox(shell, SWT.NO | SWT.YES | SWT.ICON_WARNING);
                    mb.setMessage(Messages.getString("Spoon.Dialog.PromptOverwriteFile.Message"));//"This file already exists.  Do you want to overwrite it?"
                    mb.setText(Messages.getString("Spoon.Dialog.PromptOverwriteFile.Title"));//"This file already exists!"
                    id = mb.open();
                }
            }
            catch(Exception e)
            {
                // TODO do we want to show an error dialog here?  My first guess is not, but we might.
            }
            if (id==SWT.YES)
            {
                save(meta, fname);
            }
        } 
        return saved;
    }

    public boolean saveXMLFileToVfs()
    {
        TransMeta transMeta = getActiveTransformation();
        if (transMeta!=null) return saveXMLFileToVfs( (EngineMetaInterface) transMeta);

        JobMeta jobMeta = getActiveJob();
        if (jobMeta!=null) return saveXMLFileToVfs( (EngineMetaInterface) jobMeta);
        
        return false;
    }
    
    public boolean saveXMLFileToVfs(EngineMetaInterface meta)
    {
        log.logBasic(toString(), "Save file as..."); //$NON-NLS-1$
        boolean saved=false;

        FileObject rootFile = null;
        FileObject initialFile = null;
        try {
          initialFile = KettleVFS.getFileObject(lastFileOpened);
          rootFile = KettleVFS.getFileObject(lastFileOpened).getFileSystem().getRoot();
        } catch (Exception e) {
          e.printStackTrace();
          MessageBox messageDialog = new MessageBox(shell, SWT.ICON_ERROR | SWT.OK);
          messageDialog.setText("Error");
          messageDialog.setMessage(e.getMessage());
          messageDialog.open();
          return false;
        }
        
        String fname = null;
        VfsFileChooserDialog vfsFileChooser = new VfsFileChooserDialog(rootFile, initialFile);
        FileObject selectedFile = vfsFileChooser.open(shell, "Untitled", Const.STRING_TRANS_AND_JOB_FILTER_EXT, Const.getTransformationAndJobFilterNames(), VfsFileChooserDialog.VFS_DIALOG_SAVEAS);
        if (selectedFile != null) {
          fname=selectedFile.getName().getFriendlyURI();
        }        


        String extensions[] = meta.getFilterExtensions();
        if (fname!=null) 
        {
            // Is the filename ending on .ktr, .xml?
            boolean ending=false;
            for (int i=0;i<extensions.length-1;i++)
            {
                if (fname.endsWith(extensions[i].substring(1))) ending=true;
            }
            if (fname.endsWith(meta.getDefaultExtension())) ending=true;
            if (!ending)
            {
                fname+=meta.getDefaultExtension();
            }
            // See if the file already exists...
            int id = SWT.YES;
            try
            {
                FileObject f = KettleVFS.getFileObject(fname);
                if (f.exists())
                {
                    MessageBox mb = new MessageBox(shell, SWT.NO | SWT.YES | SWT.ICON_WARNING);
                    mb.setMessage(Messages.getString("Spoon.Dialog.PromptOverwriteFile.Message"));//"This file already exists.  Do you want to overwrite it?"
                    mb.setText(Messages.getString("Spoon.Dialog.PromptOverwriteFile.Title"));//"This file already exists!"
                    id = mb.open();
                }
            }
            catch(Exception e)
            {
                // TODO do we want to show an error dialog here?  My first guess is not, but we might.
            }
            if (id==SWT.YES)
            {
                save(meta, fname);
            }
        } 
        return saved;
    }

    public boolean save(EngineMetaInterface meta, String fname) {
        boolean saved = false;
        FileListener listener = null;
    	// match by extension first
    	int idx = fname.lastIndexOf('.');
    	if( idx != -1 ) {
        	String extension = fname.substring(  idx + 1 );
        	listener = fileExtensionMap.get( extension );
    	}
    	if( listener == null ) {
        	listener = fileExtensionMap.get( meta.getDefaultExtension() );
    	}
    	
    	if( listener != null ) {
    		saved = listener.save(meta, fname);
    	}
    	return saved;
    }

    public boolean saveMeta(EngineMetaInterface meta, String fname)
    {
    	meta.setFilename(fname);
        if (Const.isEmpty(meta.getName()) || isDefaultJobName(meta.getName()))
        {
        	meta.nameFromFilename();
        }

        boolean saved = false;
        String xml = XMLHandler.getXMLHeader() + meta.getXML();
        try
        {
            DataOutputStream dos = new DataOutputStream(KettleVFS.getOutputStream(fname, false));
            dos.write(xml.getBytes(Const.XML_ENCODING));
            dos.close();
            
            saved=true;

            // Handle last opened files...
            props.addLastFile(meta.getFileType(), fname, null, false, null); //$NON-NLS-1$
            saveSettings();
            addMenuLast();

            log.logDebug(toString(), Messages.getString("Spoon.Log.FileWritten")+" ["+fname+"]"); //"File written to
            meta.setFilename( fname );
            meta.clearChanged();
            setShellText();
        }
        catch(Exception e)
        {
            log.logDebug(toString(), Messages.getString("Spoon.Log.ErrorOpeningFileForWriting")+e.toString());//"Error opening file for writing! --> "
            new ErrorDialog(shell, Messages.getString("Spoon.Dialog.ErrorSavingFile.Title"), Messages.getString("Spoon.Dialog.ErrorSavingFile.Message")+Const.CR+e.toString(), e);
        }
        return saved;
    }
    
    public void helpAbout()
    {
        MessageBox mb = new MessageBox(shell, SWT.OK | SWT.ICON_INFORMATION | SWT.CENTER);
        String mess = Messages.getString("System.ProductInfo")+Const.VERSION+Const.CR+Const.CR+Const.CR;//Kettle - Spoon version 
        mess+=Messages.getString("System.CompanyInfo")+Const.CR;
        mess+="         "+Messages.getString("System.ProductWebsiteUrl")+Const.CR; //(c) 2001-2004 i-Bridge bvba     www.kettle.be
        mess+=Const.CR; 
        mess+=Const.CR; 
        mess+=Const.CR; 
        mess+="         Build version : "+BuildVersion.getInstance().getVersion()+Const.CR;
        mess+="         Build date    : "+BuildVersion.getInstance().getBuildDate()+Const.CR;
        
        mb.setMessage(mess);
        mb.setText(APP_NAME);
        mb.open();
    }

    public void editUnselectAll(TransMeta transMeta)
    {
        transMeta.unselectAll(); 
        // spoongraph.redraw();
    }
    
    public void editSelectAll(TransMeta transMeta)
    {
        transMeta.selectAll(); 
        // spoongraph.redraw();
    }
    
    public void editOptions()
    {
        EnterOptionsDialog eod = new EnterOptionsDialog(shell);
        if (eod.open()!=null)
        {
            props.saveProps();
            loadSettings();
            changeLooks();

            MessageBox mb = new MessageBox(shell, SWT.ICON_INFORMATION);
            mb.setMessage(Messages.getString("Spoon.Dialog.PleaseRestartApplication.Message"));
            mb.setText(Messages.getString("Spoon.Dialog.PleaseRestartApplication.Title"));
            mb.open();
        } 
    }
    
    /**
     * Refresh the object selection tree (on the left of the screen)
     * @param complete true refreshes the complete tree, false tries to do a differential update to avoid flickering.
     */
    public void refreshTree()
    {
        if (shell.isDisposed()) return;
        
        GUIResource guiResource = GUIResource.getInstance();
        TransMeta activeTransMeta = getActiveTransformation();
        JobMeta activeJobMeta = getActiveJob();
        boolean showAll = activeTransMeta==null && activeJobMeta==null;
        
        // get a list of transformations from the transformation map
        List<TransMeta> transformations = new ArrayList<TransMeta>(transformationMap.values());
        Collections.sort(transformations);
        TransMeta[] transMetas = transformations.toArray(new TransMeta[transformations.size()]);
        
        // get a list of jobs from the job map
        List<JobMeta> jobs = new ArrayList<JobMeta>(jobMap.values());
        Collections.sort(jobs);
        JobMeta[] jobMetas = jobs.toArray(new JobMeta[jobs.size()]);

        // Refresh the content of the tree for those transformations
        //
        // First remove the old ones.
        selectionTree.removeAll();
        
        // Now add the data back 
        //
        if (!props.isOnlyActiveFileShownInTree() || showAll || activeTransMeta!=null )
        {
            TreeItem tiTrans = new TreeItem(selectionTree, SWT.NONE); 
            tiTrans.setText(STRING_TRANSFORMATIONS);
            tiTrans.setImage(GUIResource.getInstance().getImageBol());
            
            // Set expanded if this is the only transformation shown.
            if (props.isOnlyActiveFileShownInTree())
            {
                TreeMemory.getInstance().storeExpanded(STRING_SPOON_MAIN_TREE, tiTrans, true);
            }

            for (int t=0;t<transMetas.length;t++)
            {
                TransMeta transMeta = transMetas[t];
                
                if (!props.isOnlyActiveFileShownInTree() || showAll || (activeTransMeta!=null && activeTransMeta.equals(transMeta)))
                {
                    // Add a tree item with the name of transformation
                    //
                    TreeItem tiTransName = new TreeItem(tiTrans, SWT.NONE);
                    String name = makeTransGraphTabName(transMeta);
                    if (Const.isEmpty(name)) name = STRING_TRANS_NO_NAME;
                    tiTransName.setText(name);
                    tiTransName.setImage(guiResource.getImageBol());
                    
                    // Set expanded if this is the only transformation shown.
                    if (props.isOnlyActiveFileShownInTree())
                    {
                        TreeMemory.getInstance().storeExpanded(STRING_SPOON_MAIN_TREE, tiTransName, true);
                    }

                    ///////////////////////////////////////////////////////
                    //
                    // Now add the database connections
                    //
                    TreeItem tiDbTitle = new TreeItem(tiTransName, SWT.NONE);
                    tiDbTitle.setText(STRING_CONNECTIONS);
                    tiDbTitle.setImage(guiResource.getImageConnection());
                    
                    // Draw the connections themselves below it.
                    for (int i=0;i<transMeta.nrDatabases();i++)
                    {
                        DatabaseMeta databaseMeta = transMeta.getDatabase(i);
                        TreeItem tiDb = new TreeItem(tiDbTitle, SWT.NONE);
                        tiDb.setText(databaseMeta.getName());
                        if (databaseMeta.isShared()) tiDb.setFont(guiResource.getFontBold());
                        tiDb.setImage(guiResource.getImageConnection());
                    }
        
                    ///////////////////////////////////////////////////////
                    //
                    // The steps
                    //
                    TreeItem tiStepTitle = new TreeItem(tiTransName, SWT.NONE);
                    tiStepTitle.setText(STRING_STEPS);
                    tiStepTitle.setImage(guiResource.getImageBol());
                    
                    // Put the steps below it.
                    for (int i=0;i<transMeta.nrSteps();i++)
                    {
                        StepMeta stepMeta = transMeta.getStep(i);
                        TreeItem tiStep = new TreeItem(tiStepTitle, SWT.NONE);
                        tiStep.setText(stepMeta.getName());
                        if (stepMeta.isShared()) tiStep.setFont(guiResource.getFontBold());
                        if (!stepMeta.isDrawn()) tiStep.setForeground(guiResource.getColorGray());
                        tiStep.setImage(guiResource.getImageBol());
                    }
                    
                    ///////////////////////////////////////////////////////
                    //
                    // The hops
                    //
                    TreeItem tiHopTitle = new TreeItem(tiTransName, SWT.NONE);
                    tiHopTitle.setText(STRING_HOPS);
                    tiHopTitle.setImage(guiResource.getImageHop());
                    
                    // Put the steps below it.
                    for (int i=0;i<transMeta.nrTransHops();i++)
                    {
                        TransHopMeta hopMeta = transMeta.getTransHop(i);
                        TreeItem tiHop = new TreeItem(tiHopTitle, SWT.NONE);
                        tiHop.setText(hopMeta.toString());
                        tiHop.setImage(guiResource.getImageHop());
                    }
        
                    ///////////////////////////////////////////////////////
                    //
                    // The partitions
                    //
                    TreeItem tiPartitionTitle = new TreeItem(tiTransName, SWT.NONE);
                    tiPartitionTitle.setText(STRING_PARTITIONS);
                    tiPartitionTitle.setImage(guiResource.getImageConnection());
                    
                    // Put the steps below it.
                    for (int i=0;i<transMeta.getPartitionSchemas().size();i++)
                    {
                        PartitionSchema partitionSchema = transMeta.getPartitionSchemas().get(i);
                        TreeItem tiPartition = new TreeItem(tiPartitionTitle, SWT.NONE);
                        tiPartition.setText(partitionSchema.getName());
                        tiPartition.setImage(guiResource.getImageBol());
                        if (partitionSchema.isShared()) tiPartition.setFont(guiResource.getFontBold());
                    }
        
                    ///////////////////////////////////////////////////////
                    //
                    // The slaves
                    //
                    TreeItem tiSlaveTitle = new TreeItem(tiTransName, SWT.NONE);
                    tiSlaveTitle.setText(STRING_SLAVES);
                    tiSlaveTitle.setImage(guiResource.getImageBol());
                    
                    // Put the steps below it.
                    for (int i=0;i<transMeta.getSlaveServers().size();i++)
                    {
                        SlaveServer slaveServer = transMeta.getSlaveServers().get(i);
                        TreeItem tiSlave = new TreeItem(tiSlaveTitle, SWT.NONE);
                        tiSlave.setText(slaveServer.getName());
                        tiSlave.setImage(guiResource.getImageBol());
                        if (slaveServer.isShared()) tiSlave.setFont(guiResource.getFontBold());
                    }
                    
                    ///////////////////////////////////////////////////////
                    //
                    // The clusters
                    //
                    TreeItem tiClusterTitle = new TreeItem(tiTransName, SWT.NONE);
                    tiClusterTitle.setText(STRING_CLUSTERS);
                    tiClusterTitle.setImage(guiResource.getImageBol());
                    
                    // Put the steps below it.
                    for (int i=0;i<transMeta.getClusterSchemas().size();i++)
                    {
                        ClusterSchema clusterSchema = transMeta.getClusterSchemas().get(i);
                        TreeItem tiCluster = new TreeItem(tiClusterTitle, SWT.NONE);
                        tiCluster.setText(clusterSchema.toString());
                        tiCluster.setImage(guiResource.getImageBol());
                        if (clusterSchema.isShared()) tiCluster.setFont(guiResource.getFontBold());
                    }
                }
            }
        }
        
        if (!props.isOnlyActiveFileShownInTree() || showAll || activeJobMeta!=null )
        {
            TreeItem tiJobs = new TreeItem(selectionTree, SWT.NONE); 
            tiJobs.setText(STRING_JOBS);
            tiJobs.setImage(GUIResource.getInstance().getImageBol());

            // Set expanded if this is the only job shown.
            if (props.isOnlyActiveFileShownInTree())
            {
                tiJobs.setExpanded(true);
                TreeMemory.getInstance().storeExpanded(STRING_SPOON_MAIN_TREE, tiJobs, true);
            }

            // Now add the jobs
            //
            for (int t=0;t<jobMetas.length;t++)
            {
                JobMeta jobMeta = jobMetas[t];
             
                if (!props.isOnlyActiveFileShownInTree() || showAll || (activeJobMeta!=null && activeJobMeta.equals(jobMeta)))
                {
                    // Add a tree item with the name of job
                    //
                    TreeItem tiJobName = new TreeItem(tiJobs, SWT.NONE);
                    String name = makeJobGraphTabName(jobMeta);
                    if (Const.isEmpty(name)) name = STRING_JOB_NO_NAME;
                    tiJobName.setText(name);
                    tiJobName.setImage(guiResource.getImageBol());
                    
                    // Set expanded if this is the only job shown.
                    if (props.isOnlyActiveFileShownInTree())
                    {
                        TreeMemory.getInstance().storeExpanded(STRING_SPOON_MAIN_TREE, tiJobName, true);
                    }

                    ///////////////////////////////////////////////////////
                    //
                    // Now add the database connections
                    //
                    TreeItem tiDbTitle = new TreeItem(tiJobName, SWT.NONE);
                    tiDbTitle.setText(STRING_CONNECTIONS);
                    tiDbTitle.setImage(guiResource.getImageConnection());
                    
                    // Draw the connections themselves below it.
                    for (int i=0;i<jobMeta.nrDatabases();i++)
                    {
                        DatabaseMeta databaseMeta = jobMeta.getDatabase(i);
                        TreeItem tiDb = new TreeItem(tiDbTitle, SWT.NONE);
                        tiDb.setText(databaseMeta.getName());
                        if (databaseMeta.isShared()) tiDb.setFont(guiResource.getFontBold());
                        tiDb.setImage(guiResource.getImageConnection());
                    }
        
                    ///////////////////////////////////////////////////////
                    //
                    // The job entries
                    //
                    TreeItem tiJobEntriesTitle = new TreeItem(tiJobName, SWT.NONE);
                    tiJobEntriesTitle.setText(STRING_JOB_ENTRIES);
                    tiJobEntriesTitle.setImage(guiResource.getImageBol());
                    
                    // Put the steps below it.
                    for (int i=0;i<jobMeta.nrJobEntries();i++)
                    {
                        JobEntryCopy jobEntry = jobMeta.getJobEntry(i);
                        
                        TreeItem tiJobEntry = Const.findTreeItem(tiJobEntriesTitle, jobEntry.getName());
                        if (tiJobEntry!=null) continue; // only show it once
                        
                        tiJobEntry = new TreeItem(tiJobEntriesTitle, SWT.NONE);
                        tiJobEntry.setText(jobEntry.getName());
                        // if (jobEntry.isShared()) tiStep.setFont(guiResource.getFontBold()); TODO: allow job entries to be shared as well...
                        if (jobEntry.isStart())
                        {
                            tiJobEntry.setImage(GUIResource.getInstance().getImageStart());
                        }
                        else
                        if (jobEntry.isDummy())
                        {
                            tiJobEntry.setImage(GUIResource.getInstance().getImageDummy());
                        }
                        else
                        {
                            Image image = GUIResource.getInstance().getImagesJobentriesSmall().get(jobEntry.getTypeDesc());
                            tiJobEntry.setImage(image);
                        }
                    }
                }
            }
        }

        
        // Set the expanded state of the complete tree.
        TreeMemory.setExpandedFromMemory(selectionTree, STRING_SPOON_MAIN_TREE);

        refreshCoreObjectsHistory();
        
        selectionTree.setFocus();
        setShellText();
    }
    
    public String getActiveTabText()
    {
        if (tabfolder.getSelected()==null) return null;
        return tabfolder.getSelected().getText();
    }
    
    public void refreshGraph()
    {
        if (shell.isDisposed()) return;
        
        String tabText = getActiveTabText();
        if (tabText==null) return;
        
        TabMapEntry tabMapEntry = tabMap.get(tabText);
        if (tabMapEntry.getObject() instanceof TransGraph)
        {
            TransGraph transGraph = (TransGraph) tabMapEntry.getObject();
            transGraph.redraw();
        }
        if (tabMapEntry.getObject() instanceof JobGraph)
        {
            JobGraph jobGraph = (JobGraph) tabMapEntry.getObject();
            jobGraph.redraw();
        }
         
        setShellText();
    }
    
    public void refreshHistory()
    {
        final TransHistory transHistory = getActiveTransHistory();
        if (transHistory!=null)
        {
            transHistory.markRefreshNeeded();
        }
    }

    public StepMeta newStep(TransMeta transMeta)
    {
        return newStep(transMeta, true, true);
    }
    
    public StepMeta newStep(TransMeta transMeta, boolean openit, boolean rename)
    {
        if (transMeta==null) return null;
        TreeItem ti[] = selectionTree.getSelection();
        StepMeta inf = null;
        
        if (ti.length==1)
        {
            String steptype = ti[0].getText();
            log.logDebug(toString(), Messages.getString("Spoon.Log.NewStep")+steptype);//"New step: "
            
            inf = newStep(transMeta, steptype, steptype, openit, rename);
        }

        return inf;
    }

    /**
     * Allocate new step, optionally open and rename it.
     * 
     * @param name Name of the new step
     * @param description Description of the type of step
     * @param openit Open the dialog for this step?
     * @param rename Rename this step?
     * 
     * @return The newly created StepMeta object.
     * 
     */
    public StepMeta newStep(TransMeta transMeta, String name, String description, boolean openit, boolean rename)
    {
        StepMeta inf = null;
        
        // See if we need to rename the step to avoid doubles!
        if (rename && transMeta.findStep(name)!=null)
        {
            int i=2;
            String newname = name+" "+i;
            while (transMeta.findStep(newname)!=null)
            {
                i++;
                newname = name+" "+i;
            }
            name=newname;
        }

        StepLoader steploader = StepLoader.getInstance();
        StepPlugin stepPlugin = null;
        
        String locale = LanguageChoice.getInstance().getDefaultLocale().toString().toLowerCase();
        
        try
        {
            stepPlugin = steploader.findStepPluginWithDescription(description, locale);
            if (stepPlugin!=null)
            {
                StepMetaInterface info = BaseStep.getStepInfo(stepPlugin, steploader);
    
                info.setDefault();
                
                if (openit)
                {
                    StepDialogInterface dialog = info.getDialog(shell, info, transMeta, name);
                    name = dialog.open();
                }
                inf=new StepMeta(stepPlugin.getID()[0], name, info);
    
                if (name!=null) // OK pressed in the dialog: we have a step-name
                {
                    String newname=name;
                    StepMeta stepMeta = transMeta.findStep(newname);
                    int nr=2;
                    while (stepMeta!=null)
                    {
                        newname = name+" "+nr;
                        stepMeta = transMeta.findStep(newname);
                        nr++;
                    }
                    if (nr>2)
                    {
                        inf.setName(newname);
                        MessageBox mb = new MessageBox(shell, SWT.OK | SWT.ICON_INFORMATION);
                        mb.setMessage(Messages.getString("Spoon.Dialog.ChangeStepname.Message",newname));//"This stepname already exists.  Spoon changed the stepname to ["+newname+"]"
                        mb.setText(Messages.getString("Spoon.Dialog.ChangeStepname.Title"));//"Info!"
                        mb.open();
                    }
                    inf.setLocation(20, 20); // default location at (20,20)
                    transMeta.addStep(inf);
        
                    // Save for later:
                    // if openit is false: we drag&drop it onto the canvas!
                    if (openit)
                    {   
                        addUndoNew(transMeta, new StepMeta[] { inf }, new int[] { transMeta.indexOfStep(inf) });
                    }
                    
                    // Also store it in the pluginHistory list...
                    props.increasePluginHistory(stepPlugin.getID()[0]);
                    stepHistoryChanged=true;
        
                    refreshTree();
                }
                else
                {
                	return null; // Cancel pressed in dialog.
                }
                setShellText();
            }   
        }
        catch(KettleException e)
        {
            String filename = stepPlugin.getErrorHelpFile();
            if (stepPlugin!=null && !Const.isEmpty(filename))
            {
                // OK, in stead of a normal error message, we give back the content of the error help file... (HTML)
                try
                {
                    StringBuffer content=new StringBuffer();
                    
                    FileInputStream fis = new FileInputStream(new File(filename));
                    int ch = fis.read();
                    while (ch>=0)
                    {
                        content.append( (char)ch);
                        ch = fis.read();
                    }

                    ShowBrowserDialog sbd = new ShowBrowserDialog(shell, Messages.getString("Spoon.Dialog.ErrorHelpText.Title"), content.toString());//"Error help text"
                    sbd.open();
                }
                catch(Exception ex)
                {
                    new ErrorDialog(shell, Messages.getString("Spoon.Dialog.ErrorShowingHelpText.Title"), Messages.getString("Spoon.Dialog.ErrorShowingHelpText.Message"), ex);//"Error showing help text"
                }
            }
            else
            {
                new ErrorDialog(shell, Messages.getString("Spoon.Dialog.UnableCreateNewStep.Title"),Messages.getString("Spoon.Dialog.UnableCreateNewStep.Message") , e);//"Error creating step"  "I was unable to create a new step"
            }
            return null;
        }
        catch(Throwable e)
        {
            if (!shell.isDisposed()) new ErrorDialog(shell, Messages.getString("Spoon.Dialog.ErrorCreatingStep.Title"), Messages.getString("Spoon.Dialog.UnableCreateNewStep.Message"), new Exception(e));//"Error creating step"
            return null;
        }
                
        return inf;
    }

    /*
    private void setTreeImages()
    {

        TreeItem tiBaseCat[]=tiTransBase.getItems();
        for (int x=0;x<tiBaseCat.length;x++)
        {
            tiBaseCat[x].setImage(GUIResource.getInstance().getImageBol());
            
            TreeItem ti[] = tiBaseCat[x].getItems();
            for (int i=0;i<ti.length;i++)
            {
                TreeItem stepitem = ti[i];
                String description = stepitem.getText();
                
                StepLoader steploader = StepLoader.getInstance();
                StepPlugin sp = steploader.findStepPluginWithDescription(description);
                if (sp!=null)
                {
                    Image stepimg = (Image)GUIResource.getInstance().getImagesStepsSmall().get(sp.getID()[0]);
                    if (stepimg!=null)
                    {
                        stepitem.setImage(stepimg);
                    }
                }
            }
        }
    }
    */
    
    public void setShellText()
    {
        if (shell.isDisposed()) return;
        
        String fname = null;
        String name = null;
        ChangedFlagInterface changed = null;

        TransMeta transMeta = getActiveTransformation();
        if (transMeta!=null)
        {
            changed = transMeta;
            fname = transMeta.getFilename();
            name = transMeta.getName();
        }
        JobMeta jobMeta = getActiveJob();
        if (jobMeta!=null)
        {
            changed = jobMeta;
            fname = jobMeta.getFilename();
            name = jobMeta.getName();
        }

        String text = "";
        
        if (rep!=null)
        {
            text+= APPL_TITLE+" - ["+getRepositoryName()+"] ";
        }
        else
        {
            text+= APPL_TITLE+" - ";
        }
        
        if (Const.isEmpty(name))
        {
            if (!Const.isEmpty(fname))
            {
                text+=fname;
            }
            else
            {
                String tab = getActiveTabText();
                if (!Const.isEmpty(tab)) 
                {
                    text+=tab;
                }
                else
                {
                    text+=Messages.getString("Spoon.Various.NoName");//"[no name]"
                }
            }
        }
        else
        {
            text+=name;
        }

        if (changed!=null && changed.hasChanged())
        {
            text+=" "+Messages.getString("Spoon.Various.Changed");
        }
        
        shell.setText(text);
        
        enableMenus();
        markTabsChanged();
    }
    
    public void enableMenus()
    {
        boolean enableTransMenu = getActiveTransformation()!=null;
        boolean enableJobMenu   = getActiveJob()!=null;
        
        boolean enableRepositoryMenu = rep!=null;
        
        // Only enable certain menu-items if we need to.
        menuBar.setEnableById( "file-save", enableTransMenu || enableJobMenu);
        menuBar.setEnableById( "file-save-as", enableTransMenu || enableJobMenu);
        menuBar.setEnableById( "file-close", enableTransMenu || enableJobMenu);
        menuBar.setEnableById( "file-print", enableTransMenu || enableJobMenu);

        menuBar.setEnableById( "edit-undo", enableTransMenu || enableJobMenu );
        menuBar.setEnableById( "edit-redo", enableTransMenu || enableJobMenu );

        menuBar.setEnableById( "edit-clear-selection", enableTransMenu);
        menuBar.setEnableById( "edit-select-all", enableTransMenu);
        menuBar.setEnableById( "edit-copy", enableTransMenu);
        menuBar.setEnableById( "edit-paste", enableTransMenu);

        // Transformations
        menuBar.setEnableById( "trans-run", enableTransMenu);
        menuBar.setEnableById( "trans-preview", enableTransMenu);
        menuBar.setEnableById( "trans-verify", enableTransMenu);
        menuBar.setEnableById( "trans-impact", enableTransMenu);
        menuBar.setEnableById( "trans-get-sql", enableTransMenu);
        menuBar.setEnableById( "trans-last-impact", enableTransMenu);
        menuBar.setEnableById( "trans-last-check", enableTransMenu);
        menuBar.setEnableById( "trans-last-preview", enableTransMenu);
        menuBar.setEnableById( "trans-copy", enableTransMenu);
        // miTransPaste.setEnabled(enableTransMenu);
        menuBar.setEnableById( "trans-copy-image", enableTransMenu);
        menuBar.setEnableById( "trans-settings", enableTransMenu);

        // Jobs
        menuBar.setEnableById( "job-run", enableJobMenu);   
        menuBar.setEnableById( "job-copy", enableJobMenu);
        menuBar.setEnableById( "job-settings", enableJobMenu);

        menuBar.setEnableById( "wizard-connection", enableTransMenu || enableJobMenu);
        menuBar.setEnableById( "wizard-copy-table", enableTransMenu || enableJobMenu);
        menuBar.setEnableById( "wizard-copy-tables", enableRepositoryMenu || enableTransMenu || enableJobMenu);
        
        menuBar.setEnableById( "repository-disconnect", enableRepositoryMenu);
        menuBar.setEnableById( "repository-explore", enableRepositoryMenu);
        menuBar.setEnableById( "repository-edit-user", enableRepositoryMenu);
        
        // Do the bar as well
        toolbar.setEnableById("sql", enableTransMenu || enableJobMenu);
        toolbar.setEnableById("impact", enableTransMenu);
        toolbar.setEnableById("check", enableTransMenu);
        toolbar.setEnableById("replay", enableTransMenu);
        toolbar.setEnableById("preview", enableTransMenu);
        toolbar.setEnableById("run", enableTransMenu || enableJobMenu);
        toolbar.setEnableById("print", enableTransMenu || enableJobMenu);
        toolbar.setEnableById("saveas", enableTransMenu || enableJobMenu);
        toolbar.setEnableById("save", enableTransMenu || enableJobMenu);
        
        // What steps & plugins to show?
        refreshCoreObjects();
    }
    
    private void markTabsChanged()
    {
        for (TabMapEntry entry : tabMap.values())
        {
            if (entry.getTabItem().isDisposed()) continue;
            
            boolean changed = entry.getObject().hasContentChanged();
            entry.getTabItem().setChanged( changed );
        }
    }
    
    public void printFile()
    {
        TransMeta transMeta = getActiveTransformation();
        if (transMeta!=null)
        {
            printTransFile(transMeta);
        }
        
        JobMeta jobMeta = getActiveJob();
        if (jobMeta!=null)
        {
            printJobFile(jobMeta);
        }
    }
    
    private void printTransFile(TransMeta transMeta)
    {
        TransGraph transGraph = getActiveTransGraph();
        if (transGraph==null) return;
        
        PrintSpool ps = new PrintSpool();
        Printer printer = ps.getPrinter(shell);
        
        // Create an image of the screen
        Point max = transMeta.getMaximum();
        
        Image img = transGraph.getTransformationImage(printer, max.x, max.y, false);

        ps.printImage(shell, img);
        
        img.dispose();
        ps.dispose();
    }
    
    private void printJobFile(JobMeta jobMeta)
    {
        JobGraph jobGraph = getActiveJobGraph();
        if (jobGraph==null) return;
        
        PrintSpool ps = new PrintSpool();
        Printer printer = ps.getPrinter(shell);
        
        // Create an image of the screen
        Point max = jobMeta.getMaximum();
        
        PaletteData pal = ps.getPaletteData();      
        
        ImageData imd = new ImageData(max.x, max.y, printer.getDepth(), pal);
        Image img = new Image(printer, imd);
        
        GC img_gc = new GC(img);
        
        // Clear the background first, fill with background color...
        img_gc.setForeground(GUIResource.getInstance().getColorBackground());
        img_gc.fillRectangle(0,0,max.x, max.y);
        
        // Draw the transformation...
        jobGraph.drawJob(img_gc, false);
        
        ps.printImage(shell, img);
        
        img_gc.dispose();
        img.dispose();
        ps.dispose();
    }

    
    private TransGraph getActiveTransGraph()
    {
        TabMapEntry mapEntry = tabMap.get(tabfolder.getSelected().getText());
        if (mapEntry.getObject() instanceof TransGraph) return (TransGraph) mapEntry.getObject();
        return null;
    }
    
    private JobGraph getActiveJobGraph()
    {
        TabMapEntry mapEntry = tabMap.get(tabfolder.getSelected().getText());
        if (mapEntry.getObject() instanceof JobGraph) return (JobGraph) mapEntry.getObject();
        return null;
    }

    /**
     * @return the Log tab associated with the active transformation
     */
    private TransLog getActiveTransLog()
    {
        TransMeta transMeta = getActiveTransformation();
        if (transMeta==null) return null; // nothing to work with.

        return findTransLogOfTransformation(transMeta);
    }
    
    /**
     * @return the Log tab associated with the active job
     */
    private JobLog getActiveJobLog()
    {
        JobMeta jobMeta = getActiveJob();
        if (jobMeta==null) return null; // nothing to work with.

        return findJobLogOfJob(jobMeta);
    }
    
    public TransGraph findTransGraphOfTransformation(TransMeta transMeta)
    {
        // Now loop over the entries in the tab-map
        for (TabMapEntry mapEntry : tabMap.values())
        {
            if (mapEntry.getObject() instanceof TransGraph)
            {
                TransGraph transGraph = (TransGraph) mapEntry.getObject();
                if (transGraph.getMeta().equals(transMeta)) return transGraph;
            }
        }
        return null;
    }
    
    public JobGraph findJobGraphOfJob(JobMeta jobMeta)
    {
        // Now loop over the entries in the tab-map
        for (TabMapEntry mapEntry : tabMap.values())
        {
            if (mapEntry.getObject() instanceof JobGraph)
            {
                JobGraph jobGraph = (JobGraph) mapEntry.getObject();
                if (jobGraph.getMeta().equals(jobMeta)) return jobGraph;
            }
        }
        return null;
    }
    
    public TransLog findTransLogOfTransformation(TransMeta transMeta)
    {
        // Now loop over the entries in the tab-map
        for (TabMapEntry mapEntry : tabMap.values())
        {
            if (mapEntry.getObject() instanceof TransLog)
            {
                TransLog transLog = (TransLog) mapEntry.getObject();
                if (transLog.getMeta().equals(transMeta)) return transLog;
            }
        }
        return null;
    }
    
    public JobLog findJobLogOfJob(JobMeta jobMeta)
    {
        // Now loop over the entries in the tab-map
        for (TabMapEntry mapEntry : tabMap.values())
        {
            if (mapEntry.getObject() instanceof JobLog)
            {
                JobLog jobLog = (JobLog) mapEntry.getObject();
                if (jobLog.getMeta().equals(jobMeta)) return jobLog;
            }
        }
        return null;
    }
    
    public TransHistory findTransHistoryOfTransformation(TransMeta transMeta)
    {
        if (transMeta==null) return null;
        
        // Now loop over the entries in the tab-map
        for (TabMapEntry mapEntry : tabMap.values())
        {
            if (mapEntry.getObject() instanceof TransHistory)
            {
                TransHistory transHistory = (TransHistory) mapEntry.getObject();
                if (transHistory.getMeta()!=null && transHistory.getMeta().equals(transMeta)) return transHistory;
            }
        }
        return null;
    }
    
    public JobHistory findJobHistoryOfJob(JobMeta jobMeta)
    {
        if (jobMeta==null) return null;
        
        // Now loop over the entries in the tab-map
        for (TabMapEntry mapEntry : tabMap.values())
        {
            if (mapEntry.getObject() instanceof JobHistory)
            {
                JobHistory jobHistory = (JobHistory) mapEntry.getObject();
                if (jobHistory.getMeta()!=null && jobHistory.getMeta().equals(jobMeta)) return jobHistory;
            }
        }
        return null;
    }

    /**
     * @return the history tab associated with the active transformation
     */
    private TransHistory getActiveTransHistory()
    {
        TransMeta transMeta = getActiveTransformation();
        if (transMeta==null) return null; // nothing to work with.
        
        return findTransHistoryOfTransformation(transMeta);
    }
    
    public EngineMetaInterface getActiveMeta()
    {
        if (tabfolder==null) return null;
        TabItem tabItem = tabfolder.getSelected();
        if (tabItem==null) return null;
        
        // What transformation is in the active tab?
        // TransLog, TransGraph & TransHist contain the same transformation
        //
        TabMapEntry mapEntry = tabMap.get(tabfolder.getSelected().getText());
        EngineMetaInterface meta = null;
        if (mapEntry != null)
        {
            if (mapEntry.getObject() instanceof TransGraph) meta = (mapEntry.getObject()).getMeta();
            if (mapEntry.getObject() instanceof TransLog) meta = ( mapEntry.getObject()).getMeta();
            if (mapEntry.getObject() instanceof TransHistory) meta = ( mapEntry.getObject()).getMeta();
            if (mapEntry.getObject() instanceof JobGraph) meta = ( mapEntry.getObject()).getMeta();
            if (mapEntry.getObject() instanceof JobLog) meta = ( mapEntry.getObject()).getMeta();
            if (mapEntry.getObject() instanceof JobHistory) meta = ( mapEntry.getObject()).getMeta();
        }
        
        return meta;
    }
    
    /**
     * @return The active TransMeta object by looking at the selected TransGraph, TransLog, TransHist
     *         If nothing valueable is selected, we return null
     */
    public TransMeta getActiveTransformation()
    {
        EngineMetaInterface meta = getActiveMeta();
        if( meta instanceof TransMeta ) {
        	return (TransMeta) meta;
        }
        return null;
    }
    
    /**
     * @return The active JobMeta object by looking at the selected JobGraph, JobLog, JobHist
     *         If nothing valueable is selected, we return null
     */
    public JobMeta getActiveJob()
    {
        EngineMetaInterface meta = getActiveMeta();
        if( meta instanceof JobMeta ) {
        	return (JobMeta) meta;
        }
        return null;
    }

    public UndoInterface getActiveUndoInterface()
    {
    	return (UndoInterface) this.getActiveMeta();
    }

    public TransMeta findTransformation(String tabItemText)
    {
        return transformationMap.get(tabItemText);
    }
    
    public JobMeta findJob(String tabItemText)
    {
        return jobMap.get(tabItemText);
    }
    
    public TransMeta[] getLoadedTransformations()
    {
        List<TransMeta> list = new ArrayList<TransMeta>(transformationMap.values());
        return list.toArray(new TransMeta[list.size()]);
    }
    
    public JobMeta[] getLoadedJobs()
    {
        List<JobMeta> list = new ArrayList<JobMeta>(jobMap.values());
        return list.toArray(new JobMeta[list.size()]);
    }

    public void saveSettings()
    {
        WindowProperty winprop = new WindowProperty(shell);
        winprop.setName(APPL_TITLE);
        props.setScreen(winprop);
        
        props.setLogLevel(log.getLogLevelDesc());
        props.setLogFilter(log.getFilter());
        props.setSashWeights(sashform.getWeights());
        props.saveProps();
    }

    public void loadSettings()
    {
        log.setLogLevel(props.getLogLevel());
        log.setFilter(props.getLogFilter());

        // transMeta.setMaxUndo(props.getMaxUndo());
        DBCache.getInstance().setActive(props.useDBCache());
    }
    
    public void changeLooks()
    {
        props.setLook(selectionTree);
        props.setLook(tabfolder.getSwtTabset(), Props.WIDGET_STYLE_TAB);
        
        GUIResource.getInstance().reload();

        refreshTree();
        refreshGraph();
    }
    
    public void undoAction(UndoInterface undoInterface)
    {
        if (undoInterface==null) return;
        
        TransAction ta = undoInterface.previousUndo();
        if (ta==null) return;
        
        setUndoMenu(undoInterface); // something changed: change the menu
        
        if (undoInterface instanceof TransMeta) undoTransformationAction((TransMeta)undoInterface, ta);
        if (undoInterface instanceof JobMeta) undoJobAction((JobMeta)undoInterface, ta);
        
        // Put what we undo in focus
        if (undoInterface instanceof TransMeta)
        {
            TransGraph transGraph = findTransGraphOfTransformation((TransMeta)undoInterface);
            transGraph.forceFocus();
        }
        if (undoInterface instanceof JobMeta)
        {
            JobGraph jobGraph = findJobGraphOfJob((JobMeta)undoInterface);
            jobGraph.forceFocus();
        }
    }

    private void undoTransformationAction(TransMeta transMeta, TransAction transAction)
    {
        switch(transAction.getType())
        {
            //
            // NEW
            //

            // We created a new step : undo this...
            case TransAction.TYPE_ACTION_NEW_STEP:
                // Delete the step at correct location:
                for (int i=transAction.getCurrent().length-1;i>=0;i--)
                {
                    int idx = transAction.getCurrentIndex()[i];
                    transMeta.removeStep(idx);
                }
                refreshTree();
                refreshGraph();
                break;
    
            // We created a new connection : undo this...
            case TransAction.TYPE_ACTION_NEW_CONNECTION:
                // Delete the connection at correct location:
                for (int i=transAction.getCurrent().length-1;i>=0;i--)
                {
                    int idx = transAction.getCurrentIndex()[i];
                    transMeta.removeDatabase(idx);
                }
                refreshTree();
                refreshGraph();
                break;
    
            // We created a new note : undo this...
            case TransAction.TYPE_ACTION_NEW_NOTE:
                // Delete the note at correct location:
                for (int i=transAction.getCurrent().length-1;i>=0;i--)
                {
                    int idx = transAction.getCurrentIndex()[i];
                    transMeta.removeNote(idx);
                }
                refreshTree();
                refreshGraph();
                break;
    
            // We created a new hop : undo this...
            case TransAction.TYPE_ACTION_NEW_HOP:
                // Delete the hop at correct location:
                for (int i=transAction.getCurrent().length-1;i>=0;i--)
                {
                    int idx = transAction.getCurrentIndex()[i];
                    transMeta.removeTransHop(idx);
                }
                refreshTree();
                refreshGraph();
                break;
                
            // We created a new slave : undo this...
            case TransAction.TYPE_ACTION_NEW_SLAVE:
                // Delete the slave at correct location:
                for (int i=transAction.getCurrent().length-1;i>=0;i--)
                {
                    int idx = transAction.getCurrentIndex()[i];
                    transMeta.getSlaveServers().remove(idx);
                }
                refreshTree();
                refreshGraph();
                break;

                // We created a new slave : undo this...
            case TransAction.TYPE_ACTION_NEW_CLUSTER:
                // Delete the slave at correct location:
                for (int i=transAction.getCurrent().length-1;i>=0;i--)
                {
                    int idx = transAction.getCurrentIndex()[i];
                    transMeta.getClusterSchemas().remove(idx);
                }
                refreshTree();
                refreshGraph();
                break;
                
            //
            // DELETE
            //

            // We delete a step : undo this...
            case TransAction.TYPE_ACTION_DELETE_STEP:
                // un-Delete the step at correct location: re-insert
                for (int i=0;i<transAction.getCurrent().length;i++)
                {
                    StepMeta stepMeta = (StepMeta)transAction.getCurrent()[i];
                    int idx = transAction.getCurrentIndex()[i];
                    transMeta.addStep(idx, stepMeta);
                }
                refreshTree();
                refreshGraph();
                break;
    
            // We deleted a connection : undo this...
            case TransAction.TYPE_ACTION_DELETE_CONNECTION:
                // re-insert the connection at correct location:
                for (int i=0;i<transAction.getCurrent().length;i++)
                {
                    DatabaseMeta ci = (DatabaseMeta)transAction.getCurrent()[i];
                    int idx = transAction.getCurrentIndex()[i];
                    transMeta.addDatabase(idx, ci);
                }
                refreshTree();
                refreshGraph();
                break;
    
            // We delete new note : undo this...
            case TransAction.TYPE_ACTION_DELETE_NOTE:
                // re-insert the note at correct location:
                for (int i=0;i<transAction.getCurrent().length;i++)
                {
                    NotePadMeta ni = (NotePadMeta)transAction.getCurrent()[i];
                    int idx = transAction.getCurrentIndex()[i];
                    transMeta.addNote(idx, ni);
                }
                refreshTree();
                refreshGraph();
                break;
    
            // We deleted a hop : undo this...
            case TransAction.TYPE_ACTION_DELETE_HOP:
                // re-insert the hop at correct location:
                for (int i=0;i<transAction.getCurrent().length;i++)
                {
                    TransHopMeta hi = (TransHopMeta)transAction.getCurrent()[i];
                    int idx = transAction.getCurrentIndex()[i];
                    // Build a new hop:
                    StepMeta from = transMeta.findStep(hi.getFromStep().getName());
                    StepMeta to   = transMeta.findStep(hi.getToStep().getName());
                    TransHopMeta hinew = new TransHopMeta(from, to);
                    transMeta.addTransHop(idx, hinew);
                }
                refreshTree();
                refreshGraph();
                break;


            //
            // CHANGE
            //

            // We changed a step : undo this...
            case TransAction.TYPE_ACTION_CHANGE_STEP:
                // Delete the current step, insert previous version.
                for (int i=0;i<transAction.getCurrent().length;i++)
                {
                    StepMeta prev = (StepMeta) ((StepMeta)transAction.getPrevious()[i]).clone();
                    int idx = transAction.getCurrentIndex()[i];

                    transMeta.getStep(idx).replaceMeta(prev);
                }
                refreshTree();
                refreshGraph();
                break;
    
            // We changed a connection : undo this...
            case TransAction.TYPE_ACTION_CHANGE_CONNECTION:
                // Delete & re-insert
                for (int i=0;i<transAction.getCurrent().length;i++)
                {
                    DatabaseMeta prev = (DatabaseMeta)transAction.getPrevious()[i];
                    int idx = transAction.getCurrentIndex()[i];

                    transMeta.getDatabase(idx).replaceMeta((DatabaseMeta) prev.clone());
                }
                refreshTree();
                refreshGraph();
                break;
    
            // We changed a note : undo this...
            case TransAction.TYPE_ACTION_CHANGE_NOTE:
                // Delete & re-insert
                for (int i=0;i<transAction.getCurrent().length;i++)
                {
                    int idx = transAction.getCurrentIndex()[i];
                    transMeta.removeNote(idx);
                    NotePadMeta prev = (NotePadMeta)transAction.getPrevious()[i];
                    transMeta.addNote(idx, (NotePadMeta) prev.clone());
                }
                refreshTree();
                refreshGraph();
                break;
    
            // We changed a hop : undo this...
            case TransAction.TYPE_ACTION_CHANGE_HOP:
                // Delete & re-insert
                for (int i=0;i<transAction.getCurrent().length;i++)
                {
                    TransHopMeta prev = (TransHopMeta)transAction.getPrevious()[i];
                    int idx = transAction.getCurrentIndex()[i];

                    transMeta.removeTransHop(idx);
                    transMeta.addTransHop(idx, (TransHopMeta) prev.clone());
                }
                refreshTree();
                refreshGraph();
                break;

            //
            // POSITION
            //
                
            // The position of a step has changed: undo this...
            case TransAction.TYPE_ACTION_POSITION_STEP:
                // Find the location of the step:
                for (int i = 0; i < transAction.getCurrentIndex().length; i++) 
                {
                    StepMeta stepMeta = transMeta.getStep(transAction.getCurrentIndex()[i]);
                    stepMeta.setLocation(transAction.getPreviousLocation()[i]);
                }
                refreshGraph();
                break;
    
            // The position of a note has changed: undo this...
            case TransAction.TYPE_ACTION_POSITION_NOTE:
                for (int i=0;i<transAction.getCurrentIndex().length;i++)
                {
                    int idx = transAction.getCurrentIndex()[i];
                    NotePadMeta npi = transMeta.getNote(idx);
                    Point prev = transAction.getPreviousLocation()[i];
                    npi.setLocation(prev);
                }
                refreshGraph();
                break;
            default: break;
        }
        
        // OK, now check if we need to do this again...
        if (transMeta.viewNextUndo()!=null)
        {
            if (transMeta.viewNextUndo().getNextAlso()) undoAction(transMeta);
        }
    }
    
    private void undoJobAction(JobMeta jobMeta, TransAction transAction)
    {
        switch(transAction.getType())
        {
            //
            // NEW
            //

            // We created a new entry : undo this...
            case TransAction.TYPE_ACTION_NEW_JOB_ENTRY:
                // Delete the entry at correct location:
                {
                    int idx[] = transAction.getCurrentIndex();
                    for (int i=idx.length-1;i>=0;i--) jobMeta.removeJobEntry(idx[i]);
                    refreshTree();
                    refreshGraph();
                }
                break;
    
            // We created a new note : undo this...
            case TransAction.TYPE_ACTION_NEW_NOTE:
                // Delete the note at correct location:
                {
                    int idx[] = transAction.getCurrentIndex();
                    for (int i=idx.length-1;i>=0;i--) jobMeta.removeNote(idx[i]);
                    refreshTree();
                    refreshGraph();
                }
                break;
    
            // We created a new hop : undo this...
            case TransAction.TYPE_ACTION_NEW_JOB_HOP:
                // Delete the hop at correct location:
                {
                    int idx[] = transAction.getCurrentIndex();
                    for (int i=idx.length-1;i>=0;i--) jobMeta.removeJobHop(idx[i]);
                    refreshTree();
                    refreshGraph();
                }
                break;

            //
            // DELETE
            //

            // We delete an entry : undo this...
            case TransAction.TYPE_ACTION_DELETE_STEP:
                // un-Delete the entry at correct location: re-insert
                {
                    JobEntryCopy ce[] = (JobEntryCopy[])transAction.getCurrent();
                    int idx[] = transAction.getCurrentIndex();
                    for (int i=0;i<ce.length;i++) jobMeta.addJobEntry(idx[i], ce[i]);
                    refreshTree();
                    refreshGraph();
                }
                break;
    
            // We delete new note : undo this...
            case TransAction.TYPE_ACTION_DELETE_NOTE:
                // re-insert the note at correct location:
                {
                    NotePadMeta ni[] = (NotePadMeta[])transAction.getCurrent();
                    int idx[] = transAction.getCurrentIndex();
                    for (int i=0;i<idx.length;i++) jobMeta.addNote(idx[i], ni[i]);
                    refreshTree();
                    refreshGraph();
                }
                break;
    
            // We deleted a new hop : undo this...
            case TransAction.TYPE_ACTION_DELETE_JOB_HOP:
                // re-insert the hop at correct location:
                {
                    JobHopMeta hi[] = (JobHopMeta[])transAction.getCurrent();
                    int idx[] = transAction.getCurrentIndex();
                    for (int i=0;i<hi.length;i++)
                    {
                        jobMeta.addJobHop(idx[i], hi[i]);
                    }
                    refreshTree();
                    refreshGraph();
                }
                break;


            //
            // CHANGE
            //

            // We changed a job entry: undo this...
            case TransAction.TYPE_ACTION_CHANGE_JOB_ENTRY:
                // Delete the current job entry, insert previous version.
                {
                    for (int i=0;i<transAction.getPrevious().length;i++)
                    {
                        JobEntryCopy copy = (JobEntryCopy) ((JobEntryCopy)transAction.getPrevious()[i]).clone();
                        jobMeta.getJobEntry(transAction.getCurrentIndex()[i]).replaceMeta(copy);
                    }
                    refreshTree();
                    refreshGraph();
                }
                break;
    
            // We changed a note : undo this...
            case TransAction.TYPE_ACTION_CHANGE_NOTE:
                // Delete & re-insert
                {
                    NotePadMeta prev[] = (NotePadMeta[])transAction.getPrevious();
                    int idx[] = transAction.getCurrentIndex();
                    for (int i=0;i<idx.length;i++)
                    {
                        jobMeta.removeNote(idx[i]);
                        jobMeta.addNote(idx[i], prev[i]);
                    }
                    refreshTree();
                    refreshGraph();
                }
                break;
    
            // We changed a hop : undo this...
            case TransAction.TYPE_ACTION_CHANGE_JOB_HOP:
                // Delete & re-insert
                {
                    JobHopMeta prev[] = (JobHopMeta[])transAction.getPrevious();
                    int idx[] = transAction.getCurrentIndex();
                    for (int i=0;i<idx.length;i++)
                    {
                        jobMeta.removeJobHop(idx[i]);
                        jobMeta.addJobHop(idx[i], prev[i]);
                    }
                    refreshTree();
                    refreshGraph();
                }
                break;

            //
            // POSITION
            //
                
            // The position of a step has changed: undo this...
            case TransAction.TYPE_ACTION_POSITION_JOB_ENTRY:
                // Find the location of the step:
                {
                    int  idx[] = transAction.getCurrentIndex();
                    Point  p[] = transAction.getPreviousLocation();
                    for (int i = 0; i < p.length; i++) 
                    {
                        JobEntryCopy entry = jobMeta.getJobEntry(idx[i]);
                        entry.setLocation(p[i]);
                    }
                    refreshGraph();
                }
                break;
    
            // The position of a note has changed: undo this...
            case TransAction.TYPE_ACTION_POSITION_NOTE:
                int idx[] = transAction.getCurrentIndex();
                Point prev[] = transAction.getPreviousLocation();
                for (int i=0;i<idx.length;i++)
                {
                    NotePadMeta npi = jobMeta.getNote(idx[i]);
                    npi.setLocation(prev[i]);
                }
                refreshGraph();
                break;
            default: break;
        }
    }


    public void redoAction(UndoInterface undoInterface)
    {
        if (undoInterface==null) return;
        
        TransAction ta = undoInterface.nextUndo();
        if (ta==null) return;
        
        setUndoMenu(undoInterface); // something changed: change the menu

        if (undoInterface instanceof TransMeta) redoTransformationAction((TransMeta)undoInterface, ta);
        if (undoInterface instanceof JobMeta) redoJobAction((JobMeta)undoInterface, ta);

        // Put what we redo in focus
        if (undoInterface instanceof TransMeta)
        {
            TransGraph transGraph = findTransGraphOfTransformation((TransMeta)undoInterface);
            transGraph.forceFocus();
        }
        if (undoInterface instanceof JobMeta)
        {
            JobGraph jobGraph = findJobGraphOfJob((JobMeta)undoInterface);
            jobGraph.forceFocus();
        }
    }
    

    private void redoTransformationAction(TransMeta transMeta, TransAction transAction)
    {
        switch(transAction.getType())
        {
        //
        // NEW
        //
        case TransAction.TYPE_ACTION_NEW_STEP:
            // re-delete the step at correct location:
            for (int i=0;i<transAction.getCurrent().length;i++)
            {
                StepMeta stepMeta = (StepMeta)transAction.getCurrent()[i];
                int idx = transAction.getCurrentIndex()[i];
                transMeta.addStep(idx, stepMeta);
                                
                refreshTree();
                refreshGraph();
            }
            break;

        case TransAction.TYPE_ACTION_NEW_CONNECTION:
            // re-insert the connection at correct location:
            for (int i=0;i<transAction.getCurrent().length;i++)
            {
                DatabaseMeta ci = (DatabaseMeta)transAction.getCurrent()[i];
                int idx = transAction.getCurrentIndex()[i];
                transMeta.addDatabase(idx, ci);
                refreshTree();
                refreshGraph();
            }
            break;

        case TransAction.TYPE_ACTION_NEW_NOTE:
            // re-insert the note at correct location:
            for (int i=0;i<transAction.getCurrent().length;i++)
            {
                NotePadMeta ni = (NotePadMeta)transAction.getCurrent()[i];
                int idx = transAction.getCurrentIndex()[i];
                transMeta.addNote(idx, ni);
                refreshTree();
                refreshGraph();
            }
            break;

        case TransAction.TYPE_ACTION_NEW_HOP:
            // re-insert the hop at correct location:
            for (int i=0;i<transAction.getCurrent().length;i++)
            {
                TransHopMeta hi = (TransHopMeta)transAction.getCurrent()[i];
                int idx = transAction.getCurrentIndex()[i];
                transMeta.addTransHop(idx, hi);
                refreshTree();
                refreshGraph();
            }
            break;
        
        //  
        // DELETE
        //
        case TransAction.TYPE_ACTION_DELETE_STEP:
            // re-remove the step at correct location:
            for (int i=transAction.getCurrent().length-1;i>=0;i--)
            {
                int idx = transAction.getCurrentIndex()[i];
                transMeta.removeStep(idx);
            }
            refreshTree();
            refreshGraph();
            break;

        case TransAction.TYPE_ACTION_DELETE_CONNECTION:
            // re-remove the connection at correct location:
            for (int i=transAction.getCurrent().length-1;i>=0;i--)
            {
                int idx = transAction.getCurrentIndex()[i];
                transMeta.removeDatabase(idx);
            }
            refreshTree();
            refreshGraph();
            break;

        case TransAction.TYPE_ACTION_DELETE_NOTE:
            // re-remove the note at correct location:
            for (int i=transAction.getCurrent().length-1;i>=0;i--)
            {
                int idx = transAction.getCurrentIndex()[i];
                transMeta.removeNote(idx);
            }
            refreshTree();
            refreshGraph();
            break;

        case TransAction.TYPE_ACTION_DELETE_HOP:
            // re-remove the hop at correct location:
            for (int i=transAction.getCurrent().length-1;i>=0;i--)
            {
                int idx = transAction.getCurrentIndex()[i];
                transMeta.removeTransHop(idx);
            }
            refreshTree();
            refreshGraph();
            break;

        //
        // CHANGE
        //

        // We changed a step : undo this...
        case TransAction.TYPE_ACTION_CHANGE_STEP:
            // Delete the current step, insert previous version.
            for (int i=0;i<transAction.getCurrent().length;i++)
            {
                StepMeta stepMeta = (StepMeta) ((StepMeta)transAction.getCurrent()[i]).clone();
                transMeta.getStep(transAction.getCurrentIndex()[i]).replaceMeta(stepMeta);
            }
            refreshTree();
            refreshGraph();
            break;

        // We changed a connection : undo this...
        case TransAction.TYPE_ACTION_CHANGE_CONNECTION:
            // Delete & re-insert
            for (int i=0;i<transAction.getCurrent().length;i++)
            {
                DatabaseMeta databaseMeta = (DatabaseMeta)transAction.getCurrent()[i];
                int idx = transAction.getCurrentIndex()[i];

                transMeta.getDatabase(idx).replaceMeta((DatabaseMeta) databaseMeta.clone());
            }
            refreshTree();
            refreshGraph();
            break;

        // We changed a note : undo this...
        case TransAction.TYPE_ACTION_CHANGE_NOTE:
            // Delete & re-insert
            for (int i=0;i<transAction.getCurrent().length;i++)
            {
                NotePadMeta ni = (NotePadMeta)transAction.getCurrent()[i];
                int idx = transAction.getCurrentIndex()[i];

                transMeta.removeNote(idx);
                transMeta.addNote(idx, (NotePadMeta) ni.clone());
            }
            refreshTree();
            refreshGraph();
            break;

        // We changed a hop : undo this...
        case TransAction.TYPE_ACTION_CHANGE_HOP:
            // Delete & re-insert
            for (int i=0;i<transAction.getCurrent().length;i++)
            {
                TransHopMeta hi = (TransHopMeta)transAction.getCurrent()[i];
                int idx = transAction.getCurrentIndex()[i];

                transMeta.removeTransHop(idx);
                transMeta.addTransHop(idx, (TransHopMeta) hi.clone());
            }
            refreshTree();
            refreshGraph();
            break;

        //
        // CHANGE POSITION
        //
        case TransAction.TYPE_ACTION_POSITION_STEP:
            for (int i=0;i<transAction.getCurrentIndex().length;i++)
            {
                // Find & change the location of the step:
                StepMeta stepMeta = transMeta.getStep(transAction.getCurrentIndex()[i]);
                stepMeta.setLocation(transAction.getCurrentLocation()[i]);
            }
            refreshGraph();
            break;
        case TransAction.TYPE_ACTION_POSITION_NOTE:
            for (int i=0;i<transAction.getCurrentIndex().length;i++)
            {
                int idx = transAction.getCurrentIndex()[i];
                NotePadMeta npi = transMeta.getNote(idx);
                Point curr = transAction.getCurrentLocation()[i];
                npi.setLocation(curr);
            }
            refreshGraph();
            break;
        default: break;
        }
        
        // OK, now check if we need to do this again...
        if (transMeta.viewNextUndo()!=null)
        {
            if (transMeta.viewNextUndo().getNextAlso()) redoAction(transMeta);
        }
    }
    
    private void redoJobAction(JobMeta jobMeta, TransAction transAction)
    {
        switch(transAction.getType())
        {
        //
        // NEW
        //
        case TransAction.TYPE_ACTION_NEW_JOB_ENTRY:
            // re-delete the entry at correct location:
            {
                JobEntryCopy si[] = (JobEntryCopy[])transAction.getCurrent();
                int idx[] = transAction.getCurrentIndex();
                for (int i=0;i<idx.length;i++) jobMeta.addJobEntry(idx[i], si[i]);
                refreshTree();
                refreshGraph();
            }
            break;

        case TransAction.TYPE_ACTION_NEW_NOTE:
            // re-insert the note at correct location:
            {
                NotePadMeta ni[] = (NotePadMeta[])transAction.getCurrent();
                int idx[] = transAction.getCurrentIndex();
                for (int i=0;i<idx.length;i++) jobMeta.addNote(idx[i], ni[i]);
                refreshTree();
                refreshGraph();
            }
            break;

        case TransAction.TYPE_ACTION_NEW_JOB_HOP:
            // re-insert the hop at correct location:
            {
                JobHopMeta hi[] = (JobHopMeta[])transAction.getCurrent();
                int idx[] = transAction.getCurrentIndex();
                for (int i=0;i<idx.length;i++) jobMeta.addJobHop(idx[i], hi[i]);
                refreshTree();
                refreshGraph();
            }
            break;
        
        //  
        // DELETE
        //
        case TransAction.TYPE_ACTION_DELETE_JOB_ENTRY:
            // re-remove the entry at correct location:
            {
                int idx[] = transAction.getCurrentIndex();
                for (int i=idx.length-1;i>=0;i--) jobMeta.removeJobEntry(idx[i]);
                refreshTree();
                refreshGraph();
            }
            break;

        case TransAction.TYPE_ACTION_DELETE_NOTE:
            // re-remove the note at correct location:
            {
                int idx[] = transAction.getCurrentIndex();
                for (int i=idx.length-1;i>=0;i--) jobMeta.removeNote(idx[i]);
                refreshTree();
                refreshGraph();
            }
            break;

        case TransAction.TYPE_ACTION_DELETE_JOB_HOP:
            // re-remove the hop at correct location:
            {
                int idx[] = transAction.getCurrentIndex();
                for (int i=idx.length-1;i>=0;i--) jobMeta.removeJobHop(idx[i]);
                refreshTree();
                refreshGraph();
            }
            break;

        //
        // CHANGE
        //

        // We changed a step : undo this...
        case TransAction.TYPE_ACTION_CHANGE_JOB_ENTRY:
            // replace with "current" version.
            {    
                for (int i=0;i<transAction.getCurrent().length;i++)
                {
                    JobEntryCopy copy = (JobEntryCopy) ((JobEntryCopy)(transAction.getCurrent()[i])).clone_deep();
                    jobMeta.getJobEntry(transAction.getCurrentIndex()[i]).replaceMeta(copy);
                }
                refreshTree();
                refreshGraph();
            }
            break;

        // We changed a note : undo this...
        case TransAction.TYPE_ACTION_CHANGE_NOTE:
            // Delete & re-insert
            {
                NotePadMeta ni[] = (NotePadMeta[])transAction.getCurrent();
                int idx[] = transAction.getCurrentIndex();
                
                for (int i=0;i<idx.length;i++)
                {
                    jobMeta.removeNote(idx[i]);
                    jobMeta.addNote(idx[i], ni[i]);
                }
                refreshTree();
                refreshGraph();
            }
            break;

        // We changed a hop : undo this...
        case TransAction.TYPE_ACTION_CHANGE_JOB_HOP:
            // Delete & re-insert
            {
                JobHopMeta hi[] = (JobHopMeta[])transAction.getCurrent();
                int idx[] = transAction.getCurrentIndex();

                for (int i=0;i<idx.length;i++)
                {
                    jobMeta.removeJobHop(idx[i]);
                    jobMeta.addJobHop(idx[i], hi[i]);
                }
                refreshTree();
                refreshGraph();
            }
            break;

        //
        // CHANGE POSITION
        //
        case TransAction.TYPE_ACTION_POSITION_JOB_ENTRY:
            {
                // Find the location of the step:
                int idx[] = transAction.getCurrentIndex();
                Point p[] = transAction.getCurrentLocation();
                for (int i = 0; i < p.length; i++) 
                {
                    JobEntryCopy entry = jobMeta.getJobEntry(idx[i]);
                    entry.setLocation(p[i]);
                }
                refreshGraph();
            }
            break;
        case TransAction.TYPE_ACTION_POSITION_NOTE:
            {
                int idx[] = transAction.getCurrentIndex();
                Point curr[] = transAction.getCurrentLocation();
                for (int i=0;i<idx.length;i++)
                {
                    NotePadMeta npi = jobMeta.getNote(idx[i]);
                    npi.setLocation(curr[i]);
                }
                refreshGraph();
            }
            break;
        default: break;
        }
    }


    public void setUndoMenu(UndoInterface undoInterface)
    {
        if (shell.isDisposed()) return;

        TransAction prev = undoInterface!=null ? undoInterface.viewThisUndo() : null;
        TransAction next = undoInterface!=null ? undoInterface.viewNextUndo() : null;
        
        if (prev!=null) 
        {
            menuBar.setEnableById( "edit-undo", true );
            menuBar.setTextById( "edit-undo",  Messages.getString("Spoon.Menu.Undo.Available", prev.toString() ) );
        } 
        else            
        {
        		menuBar.setEnableById( "edit-redo", false );
        		menuBar.setTextById( "edit-redo", Messages.getString("Spoon.Menu.Undo.NotAvailable"));//"Undo : not available \tCTRL-Z"
        } 

        if (next!=null) 
        {
        		menuBar.setEnableById( "edit-redo", true);
        		menuBar.setTextById( "edit-redo", Messages.getString("Spoon.Menu.Redo.Available",next.toString()));//"Redo : "+next.toString()+" \tCTRL-Y"
        } 
        else            
        {
        		menuBar.setEnableById( "edit-redo", false);
        		menuBar.setTextById( "edit-redo", Messages.getString("Spoon.Menu.Redo.NotAvailable"));//"Redo : not available \tCTRL-Y"          
        } 
    }


    public void addUndoNew(UndoInterface undoInterface, Object obj[], int position[])
    {
        addUndoNew(undoInterface, obj, position, false);
    }   

    public void addUndoNew(UndoInterface undoInterface, Object obj[], int position[], boolean nextAlso)
    {
        undoInterface.addUndo(obj, null, position, null, null, TransMeta.TYPE_UNDO_NEW, nextAlso);
        setUndoMenu(undoInterface);
    }

    // Undo delete object
    public void addUndoDelete(UndoInterface undoInterface, Object obj[], int position[])
    {
        addUndoDelete(undoInterface, obj, position, false);
    }   

    // Undo delete object
    public void addUndoDelete(UndoInterface undoInterface, Object obj[], int position[], boolean nextAlso)
    {
        undoInterface.addUndo(obj, null, position, null, null, TransMeta.TYPE_UNDO_DELETE, nextAlso);
        setUndoMenu(undoInterface);
    }   
    
    // Change of step, connection, hop or note...
    public void addUndoPosition(UndoInterface undoInterface, Object obj[], int pos[], Point prev[], Point curr[])
    {
        // It's better to store the indexes of the objects, not the objects itself!
        undoInterface.addUndo(obj, null, pos, prev, curr, JobMeta.TYPE_UNDO_POSITION, false);
        setUndoMenu(undoInterface);
    }

    // Change of step, connection, hop or note...
    public void addUndoChange(UndoInterface undoInterface, Object from[], Object to[], int[] pos)
    {
        addUndoChange(undoInterface, from, to, pos, false);
    }

    // Change of step, connection, hop or note...
    public void addUndoChange(UndoInterface undoInterface, Object from[], Object to[], int[] pos, boolean nextAlso)
    {
        undoInterface.addUndo(from, to, pos, null, null, JobMeta.TYPE_UNDO_CHANGE, nextAlso);
        setUndoMenu(undoInterface);
    }

    
    
    /**
     * Checks *all* the steps in the transformation, puts the result in remarks list
     */
    public void checkTrans(TransMeta transMeta)
    {
        checkTrans(transMeta, false);
    }
    

    /**
     * Check the steps in a transformation
     * 
     * @param only_selected True: Check only the selected steps...
     */
    public void checkTrans(TransMeta transMeta, boolean only_selected)
    {
        if (transMeta==null) return;
        TransGraph transGraph = findTransGraphOfTransformation(transMeta);
        if (transGraph==null) return;

        CheckTransProgressDialog ctpd = new CheckTransProgressDialog(shell, transMeta, transGraph.getRemarks(), only_selected);
        ctpd.open(); // manages the remarks arraylist...
        showLastTransCheck();
    }

    /**
     * Show the remarks of the last transformation check that was run.
     * @see #checkTrans()
     */
    public void showLastTransCheck()
    {
        TransMeta transMeta = getActiveTransformation();
        if (transMeta==null) return;
        TransGraph transGraph = findTransGraphOfTransformation(transMeta);
        if (transGraph==null) return;
        
        CheckResultDialog crd = new CheckResultDialog(transMeta, shell, SWT.NONE, transGraph.getRemarks());
        String stepname = crd.open();
        if (stepname!=null)
        {
            // Go to the indicated step!
            StepMeta stepMeta = transMeta.findStep(stepname);
            if (stepMeta!=null)
            {
                editStep(transMeta, stepMeta);
            }
        }
    }

    public void clearDBCache(DatabaseMeta databaseMeta)
    {
        if (databaseMeta!=null)
        {
            DBCache.getInstance().clear(databaseMeta.getName());
        }
        else
        {
            DBCache.getInstance().clear(null);
        }
    }

    public void exploreDB(DatabaseMeta databaseMeta)
    {
        List<DatabaseMeta> databases = null;
        HasDatabasesInterface activeHasDatabasesInterface = getActiveHasDatabasesInterface();
        if (activeHasDatabasesInterface!=null) databases = activeHasDatabasesInterface.getDatabases();
        
        DatabaseExplorerDialog std = new DatabaseExplorerDialog(shell, SWT.NONE, databaseMeta, databases, true );
        std.open();
    }
    
    public void analyseImpact(TransMeta transMeta)
    {
        if (transMeta==null) return;
        TransGraph transGraph = findTransGraphOfTransformation(transMeta);
        if (transGraph==null) return;

        AnalyseImpactProgressDialog aipd = new AnalyseImpactProgressDialog(shell, transMeta, transGraph.getImpact());
        transGraph.setImpactFinished( aipd.open() );
        if (transGraph.isImpactFinished()) showLastImpactAnalyses(transMeta);
    }
    
    public void showLastImpactAnalyses(TransMeta transMeta)
    {
        if (transMeta==null) return;
        TransGraph transGraph = findTransGraphOfTransformation(transMeta);
        if (transGraph==null) return;

        List<Object[]> rows = new ArrayList<Object[]>();
        RowMetaInterface rowMeta = null;
        for (int i=0;i<transGraph.getImpact().size();i++)
        {
            DatabaseImpact ii = (DatabaseImpact)transGraph.getImpact().get(i);
            RowMetaAndData row = ii.getRow();
            rowMeta = row.getRowMeta();
            rows.add(row.getData());
        }
        
        if (rows.size()>0)
        {
            // Display all the rows...
            PreviewRowsDialog prd = new PreviewRowsDialog(shell, Variables.getADefaultVariableSpace(), SWT.NONE, "-", rowMeta, rows);
            prd.setTitleMessage(Messages.getString("Spoon.Dialog.ImpactAnalyses.Title"), Messages.getString("Spoon.Dialog.ImpactAnalyses.Message"));//"Impact analyses"  "Result of analyses:"
            prd.open();
        }
        else
        {
            MessageBox mb = new MessageBox(shell, SWT.OK | SWT.ICON_INFORMATION );
            if (transGraph.isImpactFinished())
            {
                mb.setMessage(Messages.getString("Spoon.Dialog.TransformationNoImpactOnDatabase.Message"));//"As far as I can tell, this transformation has no impact on any database."
            }
            else
            {
                mb.setMessage(Messages.getString("Spoon.Dialog.RunImpactAnalysesFirst.Message"));//"Please run the impact analyses first on this transformation."
            }
            mb.setText(Messages.getString("Spoon.Dialog.ImpactAnalyses.Title"));//Impact
            mb.open();
        }
    }
    
    public void getSQL()
    {
        TransMeta transMeta = getActiveTransformation();
        if (transMeta!=null) getTransSQL(transMeta);
        JobMeta jobMeta = getActiveJob();
        if (jobMeta!=null) getJobSQL(jobMeta);
    }
    
    /**
     * Get & show the SQL required to run the loaded transformation...
     *
     */
    public void getTransSQL(TransMeta transMeta)
    {
        GetSQLProgressDialog pspd = new GetSQLProgressDialog(shell, transMeta);
        List<SQLStatement> stats = pspd.open();
        if (stats!=null) // null means error, but we already displayed the error
        {
            if (stats.size()>0)
            {
                SQLStatementsDialog ssd = new SQLStatementsDialog(shell, Variables.getADefaultVariableSpace(), SWT.NONE, stats);
                ssd.open();
            }
            else
            {
                MessageBox mb = new MessageBox(shell, SWT.OK | SWT.ICON_INFORMATION );
                mb.setMessage(Messages.getString("Spoon.Dialog.NoSQLNeedEexecuted.Message"));//As far as I can tell, no SQL statements need to be executed before this transformation can run.
                mb.setText(Messages.getString("Spoon.Dialog.NoSQLNeedEexecuted.Title"));//"SQL"
                mb.open();
            }
        }
    }
    
    /**
     * Get & show the SQL required to run the loaded job entry...
     *
     */
    public void getJobSQL(JobMeta jobMeta)
    {
        GetJobSQLProgressDialog pspd = new GetJobSQLProgressDialog(shell, jobMeta, rep);
        List<SQLStatement> stats = pspd.open();
        if (stats!=null) // null means error, but we already displayed the error
        {
            if (stats.size()>0)
            {
                SQLStatementsDialog ssd = new SQLStatementsDialog(shell, (VariableSpace)jobMeta, SWT.NONE, stats);
                ssd.open();
            }
            else
            {
                MessageBox mb = new MessageBox(shell, SWT.OK | SWT.ICON_INFORMATION );
                mb.setMessage(Messages.getString("Spoon.Dialog.JobNoSQLNeedEexecuted.Message")); //$NON-NLS-1$
                mb.setText(Messages.getString("Spoon.Dialog.JobNoSQLNeedEexecuted.Title")); //$NON-NLS-1$
                mb.open();
            }
        }
    }
    
    public void toClipboard(String cliptext)
    {
        GUIResource.getInstance().toClipboard(cliptext);
    }
    
    public String fromClipboard()
    {
        return GUIResource.getInstance().fromClipboard();
    }
    
    /**
     * Paste transformation from the clipboard...
     *
     */
    public void pasteTransformation()
    {
        log.logDetailed(toString(), Messages.getString("Spoon.Log.PasteTransformationFromClipboard"));//"Paste transformation from the clipboard!"
        String xml = fromClipboard();
        try
        {
            Document doc = XMLHandler.loadXMLString(xml);
            TransMeta transMeta = new TransMeta(XMLHandler.getSubNode(doc, TransMeta.XML_TAG));
            addTransGraph(transMeta); // create a new tab
            refreshGraph();
            refreshTree();
        }
        catch(KettleException e)
        {
            new ErrorDialog(shell, Messages.getString("Spoon.Dialog.ErrorPastingTransformation.Title"),  Messages.getString("Spoon.Dialog.ErrorPastingTransformation.Message"), e);//Error pasting transformation  "An error occurred pasting a transformation from the clipboard"
        }
    }
    
    /**
     * Paste job from the clipboard...
     *
     */
    public void pasteJob()
    {
        String xml = fromClipboard();
        try
        {
            Document doc = XMLHandler.loadXMLString(xml);
            JobMeta jobMeta = new JobMeta(log, XMLHandler.getSubNode(doc, JobMeta.XML_TAG), rep);
            addJobGraph(jobMeta); // create a new tab
            refreshGraph();
            refreshTree();
        }
        catch(KettleException e)
        {
            new ErrorDialog(shell, Messages.getString("Spoon.Dialog.ErrorPastingJob.Title"),  Messages.getString("Spoon.Dialog.ErrorPastingJob.Message"), e);//Error pasting transformation  "An error occurred pasting a transformation from the clipboard"
        }
    }

    
    public void copyTransformation(TransMeta transMeta)
    {
        if (transMeta==null) return;
        toClipboard(XMLHandler.getXMLHeader() + transMeta.getXML());
    }
    
    public void copyJob(JobMeta jobMeta)
    {
        if (jobMeta==null) return;
        toClipboard(XMLHandler.getXMLHeader() + jobMeta.getXML());
    }
    
    public void copyTransformationImage(TransMeta transMeta)
    {
        TransGraph transGraph = findTransGraphOfTransformation(transMeta);
        if (transGraph==null) return;
        
        Clipboard clipboard = GUIResource.getInstance().getNewClipboard();
        
        Point area = transMeta.getMaximum();
        Image image = transGraph.getTransformationImage(Display.getCurrent(), area.x, area.y, false);
        clipboard.setContents(new Object[] { image.getImageData() }, new Transfer[]{ImageDataTransfer.getInstance()});
    }
    
    /**
     * @return Either a TransMeta or JobMeta object
     */
    public HasDatabasesInterface getActiveHasDatabasesInterface()
    {
        TransMeta transMeta = getActiveTransformation();
        if (transMeta!=null) return transMeta;
        return getActiveJob();
    }

	/**
	 * Shows a wizard that creates a new database connection...
	 *
	 */
    public void createDatabaseWizard()
    {
        HasDatabasesInterface hasDatabasesInterface = getActiveHasDatabasesInterface();
        if (hasDatabasesInterface==null) return; // nowhere to put the new database
        
    	CreateDatabaseWizard cdw=new CreateDatabaseWizard();
    	DatabaseMeta newDBInfo=cdw.createAndRunDatabaseWizard(shell, props, hasDatabasesInterface.getDatabases());
    	if(newDBInfo!=null){ //finished
    		hasDatabasesInterface.addDatabase(newDBInfo);
    		refreshTree();
    		refreshGraph();
    	}
    }
    
    public List<DatabaseMeta> getActiveDatabases()
    {
        Map<String,DatabaseMeta> map = new Hashtable<String,DatabaseMeta>();
        
        HasDatabasesInterface hasDatabasesInterface = getActiveHasDatabasesInterface();
        if (hasDatabasesInterface!=null)
        {
            for (int i=0;i<hasDatabasesInterface.nrDatabases();i++)
            {
                map.put(hasDatabasesInterface.getDatabase(i).getName(), hasDatabasesInterface.getDatabase(i));
            }
        }
        if (rep!=null)
        {
            try
            {
                List<DatabaseMeta> repDBs = rep.getDatabases();
                for (int i=0;i<repDBs.size();i++)
                {
                    DatabaseMeta databaseMeta = (DatabaseMeta) repDBs.get(i);
                    map.put(databaseMeta.getName(), databaseMeta);
                }
            }
            catch(Exception e)
            {
                log.logError(toString(), "Unexpected error reading databases from the repository: "+e.toString());
                log.logError(toString(), Const.getStackTracker(e));
            }
        }
        
        List<DatabaseMeta> databases = new ArrayList<DatabaseMeta>();
        databases.addAll( map.values() );
        
        return databases;
    }
        
    /**
     * Create a transformation that extracts tables & data from a database.<p><p>
     * 
     * 0) Select the database to rip<p>
     * 1) Select the table in the database to copy<p>
     * 2) Select the database to dump to<p>
     * 3) Select the repository directory in which it will end up<p>
     * 4) Select a name for the new transformation<p>
     * 6) Create 1 transformation for the selected table<p> 
     */
    public void copyTableWizard()
    {
        List<DatabaseMeta> databases = getActiveDatabases();
        if (databases.size()==0) return; // Nothing to do here
        
        final CopyTableWizardPage1 page1 = new CopyTableWizardPage1("1", databases);
        page1.createControl(shell);
        final CopyTableWizardPage2 page2 = new CopyTableWizardPage2("2");
        page2.createControl(shell);

        Wizard wizard = new Wizard() 
        {
            public boolean performFinish() 
            {
                return copyTable(page1.getSourceDatabase(), page1.getTargetDatabase(), page2.getSelection());
            }
            
            /**
             * @see org.eclipse.jface.wizard.Wizard#canFinish()
             */
            public boolean canFinish()
            {
                return page2.canFinish();
            }
        };
                
        wizard.addPage(page1);
        wizard.addPage(page2);
                
        WizardDialog wd = new WizardDialog(shell, wizard);
        wd.setMinimumPageSize(700,400);
        wd.open();
    }

    public boolean copyTable(DatabaseMeta sourceDBInfo, DatabaseMeta targetDBInfo, String tablename )
    {
        try
        {
            //
            // Create a new transformation...
            //
            TransMeta meta = new TransMeta();
            meta.addDatabase(sourceDBInfo);
            meta.addDatabase(targetDBInfo);
            
            //
            // Add a note
            //
            String note = Messages.getString("Spoon.Message.Note.ReadInformationFromTableOnDB",tablename,sourceDBInfo.getDatabaseName() )+Const.CR;//"Reads information from table ["+tablename+"] on database ["+sourceDBInfo+"]"
            note+=Messages.getString("Spoon.Message.Note.WriteInformationToTableOnDB",tablename,targetDBInfo.getDatabaseName() );//"After that, it writes the information to table ["+tablename+"] on database ["+targetDBInfo+"]"
            NotePadMeta ni = new NotePadMeta(note, 150, 10, -1, -1);
            meta.addNote(ni);
    
            // 
            // create the source step...
            //
            String fromstepname = Messages.getString("Spoon.Message.Note.ReadFromTable",tablename); //"read from ["+tablename+"]";
            TableInputMeta tii = new TableInputMeta();
            tii.setDatabaseMeta(sourceDBInfo);
            tii.setSQL("SELECT * FROM "+tablename);
            
            StepLoader steploader = StepLoader.getInstance();
            
            String fromstepid = steploader.getStepPluginID(tii);
            StepMeta fromstep = new StepMeta(fromstepid, fromstepname, tii);
            fromstep.setLocation(150,100);
            fromstep.setDraw(true);
            fromstep.setDescription(Messages.getString("Spoon.Message.Note.ReadInformationFromTableOnDB",tablename,sourceDBInfo.getDatabaseName() ));
            meta.addStep(fromstep);
            
            //
            // add logic to rename fields in case any of the field names contain reserved words...
            // Use metadata logic in SelectValues, use SelectValueInfo...
            //
            Database sourceDB = new Database(sourceDBInfo);
            sourceDB.shareVariablesWith(meta);
            sourceDB.connect();
            
            // Get the fields for the input table...
            RowMetaInterface fields = sourceDB.getTableFields(tablename);
            
            // See if we need to deal with reserved words...
            int nrReserved = targetDBInfo.getNrReservedWords(fields); 
            if (nrReserved>0)
            {
                SelectValuesMeta svi = new SelectValuesMeta();
                svi.allocate(0,0,nrReserved);
                int nr = 0;
                for (int i=0;i<fields.size();i++)
                {
                    ValueMetaInterface v = fields.getValueMeta(i);
                    if (targetDBInfo.isReservedWord( v.getName() ) )
                    {
                        svi.getMetaName()[nr] = v.getName();
                        svi.getMetaRename()[nr] = targetDBInfo.quoteField( v.getName() );
                        nr++;
                    }
                }
                
                String selstepname =Messages.getString("Spoon.Message.Note.HandleReservedWords"); //"Handle reserved words";
                String selstepid = steploader.getStepPluginID(svi);
                StepMeta selstep = new StepMeta(selstepid, selstepname, svi );
                selstep.setLocation(350,100);
                selstep.setDraw(true);
                selstep.setDescription(Messages.getString("Spoon.Message.Note.RenamesReservedWords",targetDBInfo.getDatabaseTypeDesc()) );//"Renames reserved words for "+targetDBInfo.getDatabaseTypeDesc()
                meta.addStep(selstep);
                
                TransHopMeta shi = new TransHopMeta(fromstep, selstep);
                meta.addTransHop(shi);
                fromstep = selstep;
            }
            
            // 
            // Create the target step...
            //
            //
            // Add the TableOutputMeta step...
            //
            String tostepname = Messages.getString("Spoon.Message.Note.WriteToTable",tablename); // "write to ["+tablename+"]";
            TableOutputMeta toi = new TableOutputMeta();
            toi.setDatabaseMeta( targetDBInfo );
            toi.setTablename( tablename );
            toi.setCommitSize( 200 );
            toi.setTruncateTable( true );
            
            String tostepid = steploader.getStepPluginID(toi);
            StepMeta tostep = new StepMeta(tostepid, tostepname, toi);
            tostep.setLocation(550,100);
            tostep.setDraw(true);
            tostep.setDescription(Messages.getString("Spoon.Message.Note.WriteInformationToTableOnDB2",tablename,targetDBInfo.getDatabaseName() ));//"Write information to table ["+tablename+"] on database ["+targetDBInfo+"]"
            meta.addStep(tostep);
            
            //
            // Add a hop between the two steps...
            //
            TransHopMeta hi = new TransHopMeta(fromstep, tostep);
            meta.addTransHop(hi);
            
            // OK, if we're still here: overwrite the current transformation...
            // Set a name on this generated transformation
            // 
            String name = "Copy table from ["+sourceDBInfo.getName()+"] to ["+targetDBInfo.getName()+"]";
            String transName = name;
            int nr=1;
            if (findTransformation(transName)!=null)
            {
                nr++;
                transName = name+" "+nr;
            }
            meta.setName(transName);
            addTransGraph(meta);
            
            refreshGraph();
            refreshTree();
        }
        catch(Exception e)
        {
            new ErrorDialog(shell, Messages.getString("Spoon.Dialog.UnexpectedError.Title"), Messages.getString("Spoon.Dialog.UnexpectedError.Message"), new KettleException(e.getMessage(), e));//"Unexpected error"  "An unexpected error occurred creating the new transformation" 
            return false;
        }
        return true;
    }
        
    public String toString()
    {
        return APP_NAME;
    }
    
    public void createSpoon( ) throws KettleException {

	    StringBuffer optionLogfile = getCommandLineOption("logfile").getArgument();
	    StringBuffer optionLoglevel = getCommandLineOption("level").getArgument();
    	
        // Before anything else, check the runtime version!!!
        String version = Const.JAVA_VERSION;
        if ("1.4".compareToIgnoreCase(version)>0)
        {
            System.out.println("The System is running on Java version "+version);
            System.out.println("Unfortunately, it needs version 1.4 or higher to run.");
            return;
        }

        // Set default Locale:
        Locale.setDefault(Const.DEFAULT_LOCALE);
        
        LogWriter.setConsoleAppenderDebug();
        if (Const.isEmpty(optionLogfile))
        {
            log=LogWriter.getInstance(Const.SPOON_LOG_FILE, false, LogWriter.LOG_LEVEL_BASIC);
        }
        else
        {
            log=LogWriter.getInstance( optionLogfile.toString(), true, LogWriter.LOG_LEVEL_BASIC );
        }

        if (log.getRealFilename()!=null) log.logBasic(APP_NAME, Messages.getString("Spoon.Log.LoggingToFile")+log.getRealFilename());//"Logging goes to "
        
        if (!Const.isEmpty(optionLoglevel)) 
        {
            log.setLogLevel(optionLoglevel.toString());
            log.logBasic(APP_NAME, Messages.getString("Spoon.Log.LoggingAtLevel")+log.getLogLevelDesc());//"Logging is at level : "
        }
        
        /* Load the plugins etc.*/
        StepLoader stloader = StepLoader.getInstance();
        if (!stloader.read())
        {
            log.logError(APP_NAME, Messages.getString("Spoon.Log.ErrorLoadingAndHaltSystem"));//Error loading steps & plugins... halting Spoon!
            return;
        }
        
        /* Load the plugins etc. we need to load jobentry*/
        JobEntryLoader jeloader = JobEntryLoader.getInstance();
        if (!jeloader.read())
        {
            log.logError("Spoon", "Error loading job entries & plugins... halting Kitchen!");
            return;
        }

        
        init(null);
        
        SpoonFactory.setSpoonInstance( this );
        staticSpoon = this;
        setDestroy(true);
        
        log.logBasic(APP_NAME, Messages.getString("Spoon.Log.MainWindowCreated"));//Main window is created.
        
    	
    }
    
    public boolean selectRep( Splash splash ) {
        RepositoryMeta repositoryMeta = null;
        UserInfo userinfo = null;

	    StringBuffer optionRepname = getCommandLineOption("rep").getArgument();
	    StringBuffer optionFilename = getCommandLineOption("file").getArgument();

        if (Const.isEmpty(optionRepname) && Const.isEmpty(optionFilename) && props.showRepositoriesDialogAtStartup())
        {       
            log.logBasic(APP_NAME, Messages.getString("Spoon.Log.AskingForRepository"));//"Asking for repository"

            int perms[] = new int[] { PermissionMeta.TYPE_PERMISSION_TRANSFORMATION, PermissionMeta.TYPE_PERMISSION_JOB };
            splash.hide();
            RepositoriesDialog rd = new RepositoriesDialog(display, perms, Messages.getString("Spoon.Application.Name"));//"Spoon"
            if (rd.open())
            {
                repositoryMeta = rd.getRepository();
                userinfo = rd.getUser();
                if (!userinfo.useTransformations())
                {
                    MessageBox mb = new MessageBox(shell, SWT.OK | SWT.ICON_ERROR );
                    mb.setMessage(Messages.getString("Spoon.Dialog.RepositoryUserCannotWork.Message"));//"Sorry, this repository user can't work with transformations from the repository."
                    mb.setText(Messages.getString("Spoon.Dialog.RepositoryUserCannotWork.Title"));//"Error!"
                    mb.open();
                    
                    userinfo = null;
                    repositoryMeta  = null;
                } else {
                    RepositoriesMeta repsinfo = new RepositoriesMeta(log);
                    if (repsinfo.readData())
                    {
                        repositoryMeta = repsinfo.findRepository(optionRepname.toString());
                        if (repositoryMeta!=null)
                        {
                            // Define and connect to the repository...
                            setRepository( new Repository(log, repositoryMeta, userinfo) );
                        } else {
                            log.logError(APP_NAME, Messages.getString("Spoon.Log.NoRepositoryRrovided"));//"No repository provided, can't load transformation."
                        }
                    } else {
                        log.logError(APP_NAME, Messages.getString("Spoon.Log.NoRepositoriesDefined"));//"No repositories defined on this system."
                    }

                }
            }
            else
            {
                // Exit point: user pressed CANCEL!
                if (rd.isCancelled()) 
                {
                    return false;
                }
            }
        }
        return true;
    }
    
    public void handleStartOptions( ) {
    	
	    StringBuffer optionRepname = getCommandLineOption("rep").getArgument();
	    StringBuffer optionFilename = getCommandLineOption("file").getArgument();
	    StringBuffer optionDirname = getCommandLineOption("dir").getArgument();
		StringBuffer optionTransname = getCommandLineOption("trans").getArgument();
	    StringBuffer optionJobname = getCommandLineOption("job").getArgument();
		StringBuffer optionUsername = getCommandLineOption("user").getArgument();
	    StringBuffer optionPassword = getCommandLineOption("pass").getArgument();
	    
	    
        try
        {
            // Read kettle transformation specified on command-line?
            if (!Const.isEmpty(optionRepname) || !Const.isEmpty(optionFilename))
            {           
                if (!Const.isEmpty(optionRepname))
                {
                	if( rep != null ) {
                            if (rep.connect(Messages.getString("Spoon.Application.Name")))//"Spoon"
                            {
                                if (Const.isEmpty(optionDirname)) optionDirname=new StringBuffer(RepositoryDirectory.DIRECTORY_SEPARATOR);
                                
                                // Check username, password
                                rep.userinfo = new UserInfo(rep, optionUsername.toString(), optionPassword.toString());
                                
                                if (rep.userinfo.getID()>0)
                                {
                                	// Options /file, /job and /trans are mutually exclusive
                                	int t = (Const.isEmpty(optionFilename) ? 0 : 1) +
                                			(Const.isEmpty(optionJobname) ? 0 : 1) +
                                			(Const.isEmpty(optionTransname) ? 0 : 1);
                                	if (t > 1)
                                	{
                                        log.logError(APP_NAME, Messages.getString("Spoon.Log.MutuallyExcusive")); // "More then one mutually exclusive options /file, /job and /trans are specified."                              		
                                	}
                                	else if (t == 1)
                                	{
                                		if (!Const.isEmpty(optionFilename))
                                		{
                                            openFile(optionFilename.toString(), false);
                                		}
                                		else
                                		{
                                			// OK, if we have a specified job or transformation, try to load it...
                                			// If not, keep the repository logged in.
                                			RepositoryDirectory repdir = rep.getDirectoryTree().findDirectory(optionDirname.toString());
                                			if (repdir == null)
                                			{
                                				log.logError(APP_NAME, Messages.getString("Spoon.Log.UnableFindDirectory", optionDirname.toString())); //"Can't find directory ["+dirname+"] in the repository."
                                			}
                                			else {
                                				if (!Const.isEmpty(optionTransname))
                                				{
                                					TransMeta transMeta = new TransMeta(rep, optionTransname.toString(), repdir);
                                					transMeta.setFilename(optionRepname.toString());
                                					transMeta.clearChanged();
                                					transMeta.setInternalKettleVariables();
                                					addTransGraph(transMeta);
                                				}
                                				else
                                        		{
                                					// Try to load a specified job if any
                                					JobMeta jobMeta = new JobMeta(log, rep, optionJobname.toString(), repdir);
                                					jobMeta.setFilename(optionRepname.toString());
                                					jobMeta.clearChanged();
                                					jobMeta.setInternalKettleVariables();
                                					addJobGraph(jobMeta);
                                        		}
                                			}
                                        }
                                	}
                                }
                                else
                                {
                                    log.logError(APP_NAME, Messages.getString("Spoon.Log.UnableVerifyUser"));//"Can't verify username and password."
                                    rep.disconnect();
                                    rep=null;
                                }
                            }
                            else
                            {
                                log.logError(APP_NAME, Messages.getString("Spoon.Log.UnableConnectToRepository"));//"Can't connect to the repository."
                            }
                        }
                    else
                    {
                        log.logError(APP_NAME, Messages.getString("Spoon.Log.NoRepositoriesDefined"));//"No repositories defined on this system."
                    }
                }
                else
                if (!Const.isEmpty(optionFilename))
                {
                    openFile(optionFilename.toString(), false);
                }
            }
            else // Normal operations, nothing on the commandline...
            {
                // Can we connect to the repository?
                if (rep!=null && rep.userinfo!=null)
                {
                    if (!rep.connect(Messages.getString("Spoon.Application.Name"))) //"Spoon"
                    {
                        setRepository(null);
                    }
                }
    
                if (props.openLastFile())
                {
                    log.logDetailed(APP_NAME, Messages.getString("Spoon.Log.TryingOpenLastUsedFile"));//"Trying to open the last file used."
                    
                    List<LastUsedFile> lastUsedFiles = props.getLastUsedFiles();
                    
                    if (lastUsedFiles.size()>0)
                    {
                        LastUsedFile lastUsedFile = (LastUsedFile) lastUsedFiles.get(0);
                        RepositoryMeta repInfo = (rep == null) ? null : rep.getRepositoryInfo();
                        loadLastUsedFile(lastUsedFile, repInfo);
                    }
                }
            }
        }
        catch(KettleException ke)
        {
            log.logError(APP_NAME, Messages.getString("Spoon.Log.ErrorOccurred")+Const.CR+ke.getMessage());//"An error occurred: "
            log.logError(APP_NAME, Const.getStackTracker(ke));
            rep=null;
        }

    }
    
    public void start() {
    	if( !selectRep( splash ) ) {
            splash.dispose();
            quitFile();
    	} else {
    		handleStartOptions(  );
            open ();

            if( splash != null ) {
                splash.dispose();
            }
            try
            {
                while (!isDisposed ()) 
                {
                    if (!readAndDispatch ()) sleep ();
                }
            }
            catch(Throwable e)
            {
                log.logError(APP_NAME, Messages.getString("Spoon.Log.UnexpectedErrorOccurred")+Const.CR+e.getMessage());//"An unexpected error occurred in Spoon: probable cause: please close all windows before stopping Spoon! "
                log.logError(APP_NAME, Const.getStackTracker(e));

            }
            dispose();

            log.logBasic(APP_NAME, APP_NAME+" "+Messages.getString("Spoon.Log.AppHasEnded"));//" has ended."

            // Close the logfile
            log.close();
    	}

    }
    
    public Splash splash;

    public CommandLineOption options[];
    
    public CommandLineOption getCommandLineOption( String opt ) {
    	for( int i=0; i<options.length;i++ ) {
    		if( options[i].getOption().equals( opt ) ) {
    			return options[i];
    		}
    	}
    	return null;
    }
    
    public void getCommandLineArgs( ArrayList<String> args ) {

		options = new CommandLineOption[] 
            {
			    new CommandLineOption("rep", "Repository name", new StringBuffer()),
			    new CommandLineOption("user", "Repository username", new StringBuffer()),
			    new CommandLineOption("pass", "Repository password", new StringBuffer()),
			    new CommandLineOption("job", "The name of the job to launch", new StringBuffer()),
			    new CommandLineOption("trans", "The name of the transformation to launch", new StringBuffer()),
			    new CommandLineOption("dir", "The directory (don't forget the leading /)", new StringBuffer()),
			    new CommandLineOption("file", "The filename (Transformation in XML) to launch", new StringBuffer()),
			    new CommandLineOption("level", "The logging level (Basic, Detailed, Debug, Rowlevel, Error, Nothing)", new StringBuffer()),
			    new CommandLineOption("logfile", "The logging file to write to", new StringBuffer()),
			    new CommandLineOption("log", "The logging file to write to (deprecated)", new StringBuffer(), false, true),
            };

		// Parse the options...
		CommandLineOption.parseArguments(args, options);

		String kettleRepname  = Const.getEnvironmentVariable("KETTLE_REPOSITORY", null);
        String kettleUsername = Const.getEnvironmentVariable("KETTLE_USER", null);
        String kettlePassword = Const.getEnvironmentVariable("KETTLE_PASSWORD", null);

        if (!Const.isEmpty(kettleRepname )) options[0].setArgument(new StringBuffer(kettleRepname));
        if (!Const.isEmpty(kettleUsername)) options[1].setArgument(new StringBuffer(kettleUsername));
        if (!Const.isEmpty(kettlePassword)) options[2].setArgument(new StringBuffer(kettlePassword));

    }
    
    public void createSplash() {
        splash = new Splash(display);
    }
    
    public void run( ArrayList<String> args ) {
    	try {
        	createSplash();
            getCommandLineArgs( args );
            createSpoon( );
            setArguments(args.toArray(new String[args.size()]));
            start();
    	} catch (Throwable t) {
    		t.printStackTrace();
    		log.logError("Fatal error", t.getMessage());
    	}
    }
    
    /**
     * This is the main procedure for Spoon.
     * 
     * @param a Arguments are available in the "Get System Info" step.
     */
    public static void main (String [] a) throws KettleException
    {

    	EnvUtil.environmentInit();
        ArrayList<String> args = new ArrayList<String>();
        for (int i=0;i<a.length;i++) args.add(a[i]);
        
        Display display = new Display();
        Spoon spoon = new Spoon(display);
        spoon.run(args);
        
        // Kill all remaining things in this VM!
        System.exit(0);
    }

    private void loadLastUsedFile(LastUsedFile lastUsedFile, RepositoryMeta repositoryMeta) throws KettleException
    {
        boolean useRepository = repositoryMeta!=null;
        
        // Perhaps we need to connect to the repository?
        if (lastUsedFile.isSourceRepository())
        {
            if (!Const.isEmpty(lastUsedFile.getRepositoryName()))
            {
                if (useRepository && !lastUsedFile.getRepositoryName().equalsIgnoreCase(repositoryMeta.getName()))
                {
                    // We just asked...
                    useRepository = false;
                }
            }
        }
        
        if (useRepository && lastUsedFile.isSourceRepository())
        {
            if (rep!=null) // load from this repository...
            {
                if (rep.getName().equalsIgnoreCase(lastUsedFile.getRepositoryName()))
                {
                    RepositoryDirectory repdir = rep.getDirectoryTree().findDirectory(lastUsedFile.getDirectory());
                    if (repdir!=null)
                    {
                        // Are we loading a transformation or a job?
                        if (lastUsedFile.isTransformation())
                        {
                            log.logDetailed(APP_NAME, Messages.getString("Spoon.Log.AutoLoadingTransformation",lastUsedFile.getFilename(), lastUsedFile.getDirectory()));//"Auto loading transformation ["+lastfiles[0]+"] from repository directory ["+lastdirs[0]+"]"
                            TransLoadProgressDialog tlpd = new TransLoadProgressDialog(shell, rep, lastUsedFile.getFilename(), repdir);
                            TransMeta transMeta = tlpd.open(); // = new TransInfo(log, win.rep, lastfiles[0], repdir);
                            if (transMeta != null) 
                            {
                                props.addLastFile(LastUsedFile.FILE_TYPE_TRANSFORMATION, lastUsedFile.getFilename(), repdir.getPath(), true, rep.getName());
                                transMeta.setFilename(lastUsedFile.getFilename());
                                transMeta.clearChanged();
                                addTransGraph(transMeta);
                                refreshTree();
                            }
                        }
                        else
                        if (lastUsedFile.isJob())
                        {
                            JobLoadProgressDialog progressDialog = new JobLoadProgressDialog(shell, rep, lastUsedFile.getFilename(), repdir);
                            JobMeta jobMeta = progressDialog.open();
                            props.addLastFile(LastUsedFile.FILE_TYPE_JOB, lastUsedFile.getFilename(), repdir.getPath(), true, rep.getName());
                            jobMeta.clearChanged();
                            addJobGraph(jobMeta);
                        }
                        refreshTree();
                    }
                }
            }
        }

        if (!lastUsedFile.isSourceRepository() && !Const.isEmpty(lastUsedFile.getFilename()))
        {
            if (lastUsedFile.isTransformation())
            {
            	openFile(lastUsedFile.getFilename(), false);
            	/*
                TransMeta transMeta = new TransMeta(lastUsedFile.getFilename());
                transMeta.setFilename(lastUsedFile.getFilename());
                transMeta.clearChanged();
                props.addLastFile(LastUsedFile.FILE_TYPE_TRANSFORMATION, lastUsedFile.getFilename(), null, false, null);
                addTransGraph(transMeta);
                */
            }
            if (lastUsedFile.isJob())
            {
            	openFile(lastUsedFile.getFilename(), false);
            	/*
                JobMeta jobMeta = new JobMeta(log, lastUsedFile.getFilename(), rep);
                jobMeta.setFilename(lastUsedFile.getFilename());
                jobMeta.clearChanged();
                props.addLastFile(LastUsedFile.FILE_TYPE_JOB, lastUsedFile.getFilename(), null, false, null);
                addJobGraph(jobMeta);
                */
            }
            refreshTree();
        }                       
    }

    /**
     * Create a new SelectValues step in between this step and the previous.
     * If the previous fields are not there, no mapping can be made, same with the required fields.
     * @param stepMeta The target step to map against.
     */
    public void generateMapping(TransMeta transMeta, StepMeta stepMeta)
    {
        try
        {
            if (stepMeta!=null)
            {
                StepMetaInterface smi = stepMeta.getStepMetaInterface();
                RowMetaInterface targetFields = smi.getRequiredFields();
                RowMetaInterface sourceFields = transMeta.getPrevStepFields(stepMeta);
                
                // Build the mapping: let the user decide!!
                String[] source = sourceFields.getFieldNames();
                for (int i=0;i<source.length;i++)
                {
                    ValueMetaInterface v = sourceFields.getValueMeta(i);
                    source[i]+=EnterMappingDialog.STRING_ORIGIN_SEPARATOR+v.getOrigin()+")";
                }
                String[] target = targetFields.getFieldNames();
                
                EnterMappingDialog dialog = new EnterMappingDialog(shell, source, target);
                List<SourceToTargetMapping> mappings = dialog.open();
                if (mappings!=null)
                {
                    // OK, so we now know which field maps where.
                    // This allows us to generate the mapping using a SelectValues Step...
                    SelectValuesMeta svm = new SelectValuesMeta();
                    svm.allocate(mappings.size(), 0, 0);
                    
                    for (int i=0;i<mappings.size();i++)
                    {
                        SourceToTargetMapping mapping = mappings.get(i);
                        svm.getSelectName()[i] = sourceFields.getValueMeta(mapping.getSourcePosition()).getName();
                        svm.getSelectRename()[i] = target[mapping.getTargetPosition()];
                        svm.getSelectLength()[i] = -1;
                        svm.getSelectPrecision()[i] = -1;
                    }
          // a new comment         
                    // Now that we have the meta-data, create a new step info object
                    
                    String stepName = stepMeta.getName()+" Mapping";
                    stepName = transMeta.getAlternativeStepname(stepName);  // if it's already there, rename it.
                    
                    StepMeta newStep = new StepMeta("SelectValues", stepName, svm);
                    newStep.setLocation(stepMeta.getLocation().x+20, stepMeta.getLocation().y+20);
                    newStep.setDraw(true);

                    transMeta.addStep(newStep);
                    addUndoNew(transMeta, new StepMeta[] { newStep }, new int[] { transMeta.indexOfStep(newStep) });
                    
                    // Redraw stuff...
                    refreshTree();
                    refreshGraph();
                }
            }
            else
            {
                System.out.println("No target to do mapping against!");
            }
        }
        catch(KettleException e)
        {
            new ErrorDialog(shell, "Error creating mapping", "There was an error when Kettle tried to generate a mapping against the target step", e);
        }
    }

    public void editPartitioning(TransMeta transMeta, StepMeta stepMeta)
    {
        StepPartitioningMeta stepPartitioningMeta = stepMeta.getStepPartitioningMeta();
        if (stepPartitioningMeta==null) stepPartitioningMeta = new StepPartitioningMeta();
        
        String[] options = StepPartitioningMeta.methodDescriptions;
        EnterSelectionDialog dialog = new EnterSelectionDialog(shell, options, "Partioning method", "Select the partitioning method");
        String methodDescription = dialog.open(stepPartitioningMeta.getMethod());
        if (methodDescription!=null)
        {
            int method = StepPartitioningMeta.getMethod(methodDescription);
            stepPartitioningMeta.setMethod(method);
            switch(method)
            {
            case StepPartitioningMeta.PARTITIONING_METHOD_NONE:  break;
            case StepPartitioningMeta.PARTITIONING_METHOD_MIRROR:  
            case StepPartitioningMeta.PARTITIONING_METHOD_MOD:
                // Ask for a Partitioning Schema
                String schemaNames[] = transMeta.getPartitionSchemasNames();
                if (schemaNames.length==0)
                {
                    MessageBox box = new MessageBox(shell, SWT.ICON_ERROR | SWT.OK);
                    box.setText("Create a partition schema");
                    box.setMessage("You first need to create one or more partition schemas in the transformation settings dialog before you can select one!");
                    box.open();
                }
                else
                {
                    // Set the partitioning schema too.
                    PartitionSchema partitionSchema = stepPartitioningMeta.getPartitionSchema();
                    int idx = -1;
                    if (partitionSchema!=null)
                    {
                        idx = Const.indexOfString(partitionSchema.getName(), schemaNames);
                    }
                    EnterSelectionDialog askSchema = new EnterSelectionDialog(shell, schemaNames, "Select a partition schema", "Select the partition schema to use:");
                    String schemaName = askSchema.open(idx);
                    if (schemaName!=null)
                    {
                        idx = Const.indexOfString(schemaName, schemaNames);
                        stepPartitioningMeta.setPartitionSchema(transMeta.getPartitionSchemas().get(idx));
                    }
                }
                
                if (method==StepPartitioningMeta.PARTITIONING_METHOD_MOD)
                {
                    // ask for a fieldname
                    EnterStringDialog stringDialog = new EnterStringDialog(shell, Const.NVL(stepPartitioningMeta.getFieldName(), ""), "Fieldname", "Enter a field name to partition on");
                    String fieldName = stringDialog.open();
                    stepPartitioningMeta.setFieldName(fieldName);
                }
                break;
            }
            refreshGraph();
        }
    }
    
    /**
     * Select a clustering schema for this step.
     * 
     * @param stepMeta The step to set the clustering schema for.
     */
    public void editClustering(TransMeta transMeta, StepMeta stepMeta)
    {
        int idx = -1;
        if (stepMeta.getClusterSchema()!=null)
        {
            idx = transMeta.getClusterSchemas().indexOf( stepMeta.getClusterSchema() );
        }
        String[] clusterSchemaNames = transMeta.getClusterSchemaNames();
        EnterSelectionDialog dialog = new EnterSelectionDialog(shell, clusterSchemaNames, "Cluster schema", "Select the cluster schema to use (cancel=clear)");
        String schemaName = dialog.open(idx);
        if (schemaName==null)
        {
            stepMeta.setClusterSchema(null);
        }
        else
        {
            ClusterSchema clusterSchema = transMeta.findClusterSchema(schemaName);
            stepMeta.setClusterSchema( clusterSchema );
        }
        
        refreshTree();
        refreshGraph();
    }

    
    public void createKettleArchive(TransMeta transMeta)
    {
        if (transMeta==null) return;
        JarfileGenerator.generateJarFile(transMeta);
    }
    


    /**
     * This creates a new database partitioning schema, edits it and adds it to the transformation metadata
     *
     */
    public void newDatabasePartitioningSchema(TransMeta transMeta)
    {
        PartitionSchema partitionSchema = new PartitionSchema();
        
        PartitionSchemaDialog dialog = new PartitionSchemaDialog(shell, partitionSchema, transMeta.getDatabases());
        if (dialog.open())
        {
            transMeta.getPartitionSchemas().add(partitionSchema);
            refreshTree();
        }
    }
    
    private void editPartitionSchema(HasDatabasesInterface hasDatabasesInterface, PartitionSchema partitionSchema)
    {
        PartitionSchemaDialog dialog = new PartitionSchemaDialog(shell, partitionSchema, hasDatabasesInterface.getDatabases());
        if (dialog.open())
        {
            refreshTree();
        }
    }
    

    private void delPartitionSchema(TransMeta transMeta, PartitionSchema partitionSchema)
    {
        try
        {
            if (rep!=null && partitionSchema.getId()>0)
            {
                // remove the partition schema from the repository too...
                rep.delPartitionSchema(partitionSchema.getId());
            }
            
            int idx = transMeta.getPartitionSchemas().indexOf(partitionSchema);
            transMeta.getPartitionSchemas().remove(idx);
            refreshTree();
        }
        catch(KettleException e)
        {
            new ErrorDialog(shell, Messages.getString("Spoon.Dialog.ErrorDeletingClusterSchema.Title"), Messages.getString("Spoon.Dialog.ErrorDeletingClusterSchema.Message"), e); //$NON-NLS-1$ //$NON-NLS-2$
        }
    }

    /**
     * This creates a new clustering schema, edits it and adds it to the transformation metadata
     *
     */
    public void newClusteringSchema(TransMeta transMeta)
    {
        ClusterSchema clusterSchema = new ClusterSchema();
        
        ClusterSchemaDialog dialog = new ClusterSchemaDialog(shell, clusterSchema, transMeta.getSlaveServers());
        if (dialog.open())
        {
            transMeta.getClusterSchemas().add(clusterSchema);
            refreshTree();
        }
    }

    private void editClusterSchema(TransMeta transMeta, ClusterSchema clusterSchema)
    {
        ClusterSchemaDialog dialog = new ClusterSchemaDialog(shell, clusterSchema, transMeta.getSlaveServers());
        if (dialog.open())
        {
            refreshTree();
        }
    }

    private void delClusterSchema(TransMeta transMeta, ClusterSchema clusterSchema)
    {
        try
        {
            if (rep!=null && clusterSchema.getId()>0)
            {
                // remove the partition schema from the repository too...
                rep.delClusterSchema(clusterSchema.getId());
            }
            
            int idx = transMeta.getClusterSchemas().indexOf(clusterSchema);
            transMeta.getClusterSchemas().remove(idx);
            refreshTree();
        }
        catch(KettleException e)
        {
            new ErrorDialog(shell, Messages.getString("Spoon.Dialog.ErrorDeletingPartitionSchema.Title"), Messages.getString("Spoon.Dialog.ErrorDeletingPartitionSchema.Message"), e); //$NON-NLS-1$ //$NON-NLS-2$
        }
    }
    
    /**
     * This creates a slave server, edits it and adds it to the transformation metadata
     *
     */
    public void newSlaveServer(TransMeta transMeta)
    {
        SlaveServer slaveServer = new SlaveServer();
        
        SlaveServerDialog dialog = new SlaveServerDialog(shell, slaveServer);
        if (dialog.open())
        {
            transMeta.getSlaveServers().add(slaveServer);
            refreshTree();
        }
    }

    public void delSlaveServer(TransMeta transMeta, SlaveServer slaveServer)
    {
        try
        {
            if (rep!=null && slaveServer.getId()>0)
            {
                // remove the slave server from the repository too...
                rep.delSlave(slaveServer.getId());
            }
            
            int idx = transMeta.getSlaveServers().indexOf(slaveServer);
            transMeta.getSlaveServers().remove(idx);
            refreshTree();
        }
        catch(KettleException e)
        {
            new ErrorDialog(shell, Messages.getString("Spoon.Dialog.ErrorDeletingSlave.Title"), Messages.getString("Spoon.Dialog.ErrorDeletingSlave.Message"), e); //$NON-NLS-1$ //$NON-NLS-2$
        }
    }
    
    
    /**
     * Sends transformation to slave server
     * @param executionConfiguration 
     */
    public void sendXMLToSlaveServer(TransMeta transMeta, TransExecutionConfiguration executionConfiguration)
    {
        SlaveServer slaveServer = executionConfiguration.getRemoteServer();
        
        try
        {
            if (slaveServer==null) throw new KettleException("No slave server specified");
            if (Const.isEmpty(transMeta.getName())) throw new KettleException("The transformation needs a name to uniquely identify it by on the remote server.");
            
            String xml = new TransConfiguration(transMeta, executionConfiguration).getXML();
            String reply = slaveServer.sendXML(xml, AddTransServlet.CONTEXT_PATH+"/?xml=Y");
            WebResult webResult = WebResult.fromXMLString(reply);
            if (!webResult.getResult().equalsIgnoreCase(WebResult.STRING_OK))
            {
                throw new KettleException("There was an error posting the transformation on the remote server: "+Const.CR+webResult.getMessage());
            }
            
            reply = slaveServer.getContentFromServer(PrepareExecutionTransServlet.CONTEXT_PATH+"/?name="+transMeta.getName()+"&xml=Y");
            webResult = WebResult.fromXMLString(reply);
            if (!webResult.getResult().equalsIgnoreCase(WebResult.STRING_OK))
            {
                throw new KettleException("There was an error preparing the transformation for excution on the remote server: "+Const.CR+webResult.getMessage());
            }
            
            reply = slaveServer.getContentFromServer(StartExecutionTransServlet.CONTEXT_PATH+"/?name="+transMeta.getName()+"&xml=Y");
            webResult = WebResult.fromXMLString(reply);
            if (!webResult.getResult().equalsIgnoreCase(WebResult.STRING_OK))
            {
                throw new KettleException("There was an error starting the transformation on the remote server: "+Const.CR+webResult.getMessage());
            }
        }
        catch (Exception e)
        {
            new ErrorDialog(shell, "Error", "Error sending transformation to server", e);
        }
    }

    private void splitTrans(TransMeta transMeta, boolean show, boolean post, boolean prepare, boolean start)
    {
        try
        {
            if (Const.isEmpty(transMeta.getName())) throw new KettleException("The transformation needs a name to uniquely identify it by on the remote server.");

            TransSplitter transSplitter = new TransSplitter(transMeta);
            transSplitter.splitOriginalTransformation();
            
            // Send the transformations to the servers...
            //
            // First the master...
            //
            TransMeta master = transSplitter.getMaster();
            SlaveServer masterServer = null;
            List<StepMeta> masterSteps = master.getTransHopSteps(false);
            if (masterSteps.size()>0) // If there is something that needs to be done on the master...
            {
                masterServer = transSplitter.getMasterServer();
                if (show) addTransGraph(master);
                if (post)
                {
                    String masterReply = masterServer.sendXML(new TransConfiguration(master, executionConfiguration).getXML(), AddTransServlet.CONTEXT_PATH+"/?xml=Y");
                    WebResult webResult = WebResult.fromXMLString(masterReply);
                    if (!webResult.getResult().equalsIgnoreCase(WebResult.STRING_OK))
                    {
                        throw new KettleException("An error occurred sending the master transformation: "+webResult.getMessage());
                    }
                }
            }
            
            // Then the slaves...
            //
            SlaveServer slaves[] = transSplitter.getSlaveTargets();
            for (int i=0;i<slaves.length;i++)
            {
                TransMeta slaveTrans = (TransMeta) transSplitter.getSlaveTransMap().get(slaves[i]);
                if (show) addTransGraph(slaveTrans);
                if (post)
                {
                    TransConfiguration transConfiguration = new TransConfiguration(slaveTrans, executionConfiguration);
                    RowMetaAndData variables = transConfiguration.getTransExecutionConfiguration().getVariables();
                    variables.addValue(new ValueMeta(Const.INTERNAL_VARIABLE_SLAVE_TRANS_NUMBER, ValueMetaInterface.TYPE_STRING), Integer.toString(i));
                    variables.addValue(new ValueMeta(Const.INTERNAL_VARIABLE_CLUSTER_SIZE, ValueMetaInterface.TYPE_STRING), Integer.toString(slaves.length));
                    String slaveReply = slaves[i].sendXML(transConfiguration.getXML(), AddTransServlet.CONTEXT_PATH+"/?xml=Y");
                    WebResult webResult = WebResult.fromXMLString(slaveReply);
                    if (!webResult.getResult().equalsIgnoreCase(WebResult.STRING_OK))
                    {
                        throw new KettleException("An error occurred sending a slave transformation: "+webResult.getMessage());
                    }
                }
            }
            
            if (post)
            {
                if (prepare)
                {
                    // Prepare the master...
                    if (masterSteps.size()>0) // If there is something that needs to be done on the master...
                    {
                        String masterReply = masterServer.getContentFromServer(PrepareExecutionTransServlet.CONTEXT_PATH+"/?name="+master.getName()+"&xml=Y");
                        WebResult webResult = WebResult.fromXMLString(masterReply);
                        if (!webResult.getResult().equalsIgnoreCase(WebResult.STRING_OK))
                        {
                            throw new KettleException("An error occurred while preparing the execution of the master transformation: "+webResult.getMessage());
                        }
                    }
                    
                    // Prepare the slaves
                    for (int i=0;i<slaves.length;i++)
                    {
                        TransMeta slaveTrans = (TransMeta) transSplitter.getSlaveTransMap().get(slaves[i]);
                        String slaveReply = slaves[i].getContentFromServer(PrepareExecutionTransServlet.CONTEXT_PATH+"/?name="+slaveTrans.getName()+"&xml=Y");
                        WebResult webResult = WebResult.fromXMLString(slaveReply);
                        if (!webResult.getResult().equalsIgnoreCase(WebResult.STRING_OK))
                        {
                            throw new KettleException("An error occurred while preparing the execution of a slave transformation: "+webResult.getMessage());
                        }
                    }
                }
                
                if (start)
                {
                    // Start the master...
                    if (masterSteps.size()>0) // If there is something that needs to be done on the master...
                    {
                        String masterReply = masterServer.getContentFromServer(StartExecutionTransServlet.CONTEXT_PATH+"/?name="+master.getName()+"&xml=Y");
                        WebResult webResult = WebResult.fromXMLString(masterReply);
                        if (!webResult.getResult().equalsIgnoreCase(WebResult.STRING_OK))
                        {
                            throw new KettleException("An error occurred while starting the execution of the master transformation: "+webResult.getMessage());
                        }
                    }
                    
                    // Start the slaves
                    for (int i=0;i<slaves.length;i++)
                    {
                        TransMeta slaveTrans = (TransMeta) transSplitter.getSlaveTransMap().get(slaves[i]);
                        String slaveReply = slaves[i].getContentFromServer(StartExecutionTransServlet.CONTEXT_PATH+"/?name="+slaveTrans.getName()+"&xml=Y");
                        WebResult webResult = WebResult.fromXMLString(slaveReply);
                        if (!webResult.getResult().equalsIgnoreCase(WebResult.STRING_OK))
                        {
                            throw new KettleException("An error occurred while starting the execution of a slave transformation: "+webResult.getMessage());
                        }
                    }
                }
                
                // Now add monitors for the master and all the slave servers
                addSpoonSlave(masterServer);
                for (int i=0;i<slaves.length;i++)
                {
                    addSpoonSlave(slaves[i]);
                }
            }
        }
        catch(Exception e)
        {
            new ErrorDialog(shell, "Split transformation", "There was an error during transformation split", e);
        }
    }

    public void runFile() {
    	executeFile( true, false, false, false, null);
    }

    public void previewFile() {
    	executeFile( true, false, false, true, null);
    }

    public void executeFile(boolean local, boolean remote, boolean cluster, boolean preview, Date replayDate)
    {
        TransMeta transMeta = getActiveTransformation();
        if (transMeta!=null) executeTransformation(transMeta, local, remote, cluster, preview, replayDate);
        
        JobMeta jobMeta = getActiveJob();
        if (jobMeta!=null) executeJob(jobMeta, local, remote, cluster, preview, replayDate);
        
    }

    public void executeTransformation(TransMeta transMeta, boolean local, boolean remote, boolean cluster, boolean preview, Date replayDate)
    {
        if (transMeta==null) return;
        
        executionConfiguration.setExecutingLocally(local);
        executionConfiguration.setExecutingRemotely(remote);
        executionConfiguration.setExecutingClustered(cluster);
        
        executionConfiguration.getUsedVariables(transMeta);
        executionConfiguration.getUsedArguments(transMeta, arguments);
        executionConfiguration.setReplayDate(replayDate);
        executionConfiguration.setLocalPreviewing(preview);
        
        executionConfiguration.setLogLevel(log.getLogLevel());
        // executionConfiguration.setSafeModeEnabled( transLog!=null && transLog.isSafeModeChecked() );
        
        TransExecutionConfigurationDialog dialog = new TransExecutionConfigurationDialog(shell, executionConfiguration, transMeta);
        if (dialog.open())
        {
            addTransLog(transMeta, !executionConfiguration.isLocalPreviewing());
            TransLog transLog = getActiveTransLog();
            
            if (executionConfiguration.isExecutingLocally())
            {
                if (executionConfiguration.isLocalPreviewing())
                {
                    transLog.preview(executionConfiguration);
                }
                else
                {
                    transLog.start(executionConfiguration);
                }
            }
            else if(executionConfiguration.isExecutingRemotely())
            {
                if (executionConfiguration.getRemoteServer()!=null)
                {
                    sendXMLToSlaveServer(transMeta, executionConfiguration);
                    addSpoonSlave(executionConfiguration.getRemoteServer());
                }
                else
                {
                    MessageBox mb = new MessageBox(shell, SWT.OK | SWT.ICON_INFORMATION);
                    mb.setMessage(Messages.getString("Spoon.Dialog.NoRemoteServerSpecified.Message")); 
                    mb.setText(Messages.getString("Spoon.Dialog.NoRemoteServerSpecified.Title"));
                    mb.open();
                }
            }
            else if (executionConfiguration.isExecutingClustered())
            {
                splitTrans( transMeta,
                        executionConfiguration.isClusterShowingTransformation(), 
                        executionConfiguration.isClusterPosting(),
                        executionConfiguration.isClusterPreparing(),
                        executionConfiguration.isClusterStarting()
                        );
            }
        }
        arguments = executionConfiguration.getArgumentStrings();
    }
    
    public void executeJob(JobMeta jobMeta, boolean local, boolean remote, boolean cluster, boolean preview, Date replayDate)
    {
        addJobLog(jobMeta);
        JobLog jobLog = getActiveJobLog();
        jobLog.startJob(replayDate);
    }
    
    public TabItem findTabItem(String tabItemText, int objectType)
    {
        for (TabMapEntry entry : tabMap.values())
        {                       
            if (entry.getTabItem().isDisposed()) continue;
            if (objectType == entry.getObjectType() && entry.getTabItem().getText().equalsIgnoreCase(tabItemText))
            {
                return entry.getTabItem();
            }
        }
        return null;
    }

    private void addSpoonSlave(SlaveServer slaveServer)
    {
        // See if there is a SpoonSlave for this slaveServer...
        String tabName = makeSlaveTabName(slaveServer);
        TabItem tabItem=findTabItem(tabName, TabMapEntry.OBJECT_TYPE_SLAVE_SERVER);
        if (tabItem==null)
        {
            SpoonSlave spoonSlave = new SpoonSlave(tabfolder.getSwtTabset(), SWT.NONE, this, slaveServer);
            tabItem = new TabItem(tabfolder, tabName, tabName );
            tabItem.setToolTipText("Status of slave server : "+slaveServer.getName()+" : "+slaveServer.getServerAndPort());
            tabItem.setControl(spoonSlave);
            
            tabMap.put(tabName, new TabMapEntry(tabItem, tabName, spoonSlave, TabMapEntry.OBJECT_TYPE_SLAVE_SERVER));
        }
        int idx = tabfolder.indexOf(tabItem);
        tabfolder.setSelected(idx);        
    }
    
    public void addTransGraph(TransMeta transMeta)
    {
        String key = addTransformation(transMeta);
        if (key!=null)
        {
            // See if there already is a tab for this graph
            // If no, add it
            // If yes, select that tab
            //
            String tabName = makeTransGraphTabName(transMeta);
            TabItem tabItem=findTabItem(tabName, TabMapEntry.OBJECT_TYPE_TRANSFORMATION_GRAPH);
            if (tabItem==null)
            {
                TransGraph transGraph = new TransGraph(tabfolder.getSwtTabset(), this, transMeta);
                tabItem = new TabItem(tabfolder, tabName, tabName);
                tabItem.setToolTipText(Messages.getString("Spoon.TabTrans.Tooltip", makeTransGraphTabName(transMeta)));
                tabItem.setImage(GUIResource.getInstance().getImageTransGraph());
                tabItem.setControl(transGraph);
                
                tabMap.put(tabName, new TabMapEntry(tabItem, tabName, transGraph, TabMapEntry.OBJECT_TYPE_TRANSFORMATION_GRAPH));
            }
            int idx = tabfolder.indexOf(tabItem);
            
            // OK, also see if we need to open a new history window.
            if (transMeta.getLogConnection()!=null && !Const.isEmpty(transMeta.getLogTable()))
            {
                addTransHistory(transMeta, false);
            }
            // keep the focus on the graph
            tabfolder.setSelected(idx);
            
            setUndoMenu(transMeta);
            enableMenus();
        }
    }
    
    public void addJobGraph(JobMeta jobMeta)
    {
        String key = addJob(jobMeta);
        if (key!=null)
        {
            // See if there already is a tab for this graph
            // If no, add it
            // If yes, select that tab
            //
            String tabName = makeJobGraphTabName(jobMeta);
            TabItem tabItem=findTabItem(tabName, TabMapEntry.OBJECT_TYPE_JOB_GRAPH);
            if (tabItem==null)
            {
                JobGraph jobGraph = new JobGraph(tabfolder.getSwtTabset(), this, jobMeta);
                tabItem = new TabItem(tabfolder, tabName, tabName );
                tabItem.setToolTipText(Messages.getString("Spoon.TabJob.Tooltip", makeJobGraphTabName(jobMeta)));
                tabItem.setImage(GUIResource.getInstance().getImageJobGraph());
                tabItem.setControl(jobGraph);
                
                tabMap.put(tabName, new TabMapEntry(tabItem, tabName, jobGraph, TabMapEntry.OBJECT_TYPE_JOB_GRAPH));
            }
            int idx = tabfolder.indexOf(tabItem);
            
            // OK, also see if we need to open a new history window.
            if (jobMeta.getLogConnection()!=null && !Const.isEmpty(jobMeta.getLogTable()))
            {
                addJobHistory(jobMeta, false);
            }
            // keep the focus on the graph
            tabfolder.setSelected(idx);
            
            setUndoMenu(jobMeta);
            enableMenus();
        }
    }
    
    public boolean addSpoonBrowser(String name, String urlString)
    {
        try
        {
            // OK, now we have the HTML, create a new browset tab.
            
            // See if there already is a tab for this browser
            // If no, add it
            // If yes, select that tab
            //
            TabItem tabItem=findTabItem(name, TabMapEntry.OBJECT_TYPE_BROWSER);
            if (tabItem==null)
            {
                SpoonBrowser browser = new SpoonBrowser(tabfolder.getSwtTabset(), this, urlString);
                tabItem = new TabItem(tabfolder, name, name );
                tabItem.setImage(GUIResource.getInstance().getImageLogoSmall());
                tabItem.setControl(browser.getComposite());
                
                tabMap.put(name, new TabMapEntry(tabItem, name, browser, TabMapEntry.OBJECT_TYPE_BROWSER));
            }
            int idx = tabfolder.indexOf(tabItem);
            
            // keep the focus on the graph
            tabfolder.setSelected(idx);
            return true;
        }
        catch(Throwable e)
        {
            return false;
        }
    }
    
    public String makeLogTabName(TransMeta transMeta)
    {
        return Messages.getString("Spoon.Title.LogTransView", makeTransGraphTabName(transMeta));
    }
    
    public String makeJobLogTabName(JobMeta jobMeta)
    {
        return Messages.getString("Spoon.Title.LogJobView", makeJobGraphTabName(jobMeta));
    }
    
    public String makeTransGraphTabName(TransMeta transMeta)
    {
        if (Const.isEmpty(transMeta.getName()) && Const.isEmpty(transMeta.getFilename()))
            return STRING_TRANS_NO_NAME;
        
        if (Const.isEmpty(transMeta.getName()) || isDefaultTransformationName(transMeta.getName()))
        {
            transMeta.nameFromFilename();
        }

        return transMeta.getName();
    }
    
    public String makeJobGraphTabName(JobMeta jobMeta)
    {
        if (Const.isEmpty(jobMeta.getName()) && Const.isEmpty(jobMeta.getFilename()))
            return STRING_JOB_NO_NAME;

        if (Const.isEmpty(jobMeta.getName()) || isDefaultJobName(jobMeta.getName()))
        {
            jobMeta.nameFromFilename();
        }

        return jobMeta.getName();
    }
    
    public String makeHistoryTabName(TransMeta transMeta)
    {
        return Messages.getString("Spoon.Title.LogTransHistoryView", makeTransGraphTabName(transMeta));
    }
    
    public String makeJobHistoryTabName(JobMeta jobMeta)
    {
        return Messages.getString("Spoon.Title.LogJobHistoryView", makeJobGraphTabName(jobMeta));
    }
    
    public String makeSlaveTabName(SlaveServer slaveServer)
    {
        return "Slave server: "+slaveServer.getName();
    }

    public void addTransLog(TransMeta transMeta)
    {
        addTransLog(transMeta, true);
    } 

    public void addTransLog(TransMeta transMeta, boolean setActive)
    {
        // See if there already is a tab for this log
        // If no, add it
        // If yes, select that tab
    	//   if setActive is true
        //
        String tabName = makeLogTabName(transMeta);
        TabItem tabItem=findTabItem(tabName, TabMapEntry.OBJECT_TYPE_TRANSFORMATION_LOG);
        if (tabItem==null)
        {
            TransLog transLog = new TransLog(tabfolder.getSwtTabset(), this, transMeta);
            tabItem = new TabItem(tabfolder, tabName, tabName );
            tabItem.setToolTipText(Messages.getString("Spoon.Title.ExecLogTransView.Tooltip", makeTransGraphTabName(transMeta)));
            tabItem.setControl(transLog);

            // If there is an associated history window, we want to keep that one up-to-date as well.
            //
            TransHistory transHistory = findTransHistoryOfTransformation(transMeta);
            TabItem historyItem = findTabItem(makeHistoryTabName(transMeta), TabMapEntry.OBJECT_TYPE_TRANSFORMATION_HISTORY);
            
            if (transHistory!=null && historyItem!=null)
            {
                TransHistoryRefresher transHistoryRefresher = new TransHistoryRefresher(historyItem, transHistory);
                tabfolder.addListener(transHistoryRefresher);
                transLog.setTransHistoryRefresher(transHistoryRefresher);
            }

            
            tabMap.put(tabName, new TabMapEntry(tabItem, tabName, transLog, TabMapEntry.OBJECT_TYPE_TRANSFORMATION_LOG));
        }
        if ( setActive )
        {
            int idx = tabfolder.indexOf(tabItem);
            tabfolder.setSelected(idx);
        }
    }
    
    public void addJobLog(JobMeta jobMeta)
    {
        // See if there already is a tab for this log
        // If no, add it
        // If yes, select that tab
        //
        String tabName = makeJobLogTabName(jobMeta);
        TabItem tabItem=findTabItem(tabName, TabMapEntry.OBJECT_TYPE_JOB_LOG);
        if (tabItem==null)
        {
            JobLog jobLog = new JobLog(tabfolder.getSwtTabset(), this, jobMeta);
            tabItem = new TabItem(tabfolder, tabName, tabName );
            tabItem.setText(tabName);
            tabItem.setToolTipText(Messages.getString("Spoon.Title.ExecLogJobView.Tooltip", makeJobGraphTabName(jobMeta)));
            tabItem.setControl(jobLog);

            // If there is an associated history window, we want to keep that one up-to-date as well.
            //
            JobHistory jobHistory = findJobHistoryOfJob(jobMeta);
            TabItem historyItem = findTabItem(makeJobHistoryTabName(jobMeta), TabMapEntry.OBJECT_TYPE_JOB_HISTORY);
            
            if (jobHistory!=null && historyItem!=null)
            {
                JobHistoryRefresher jobHistoryRefresher = new JobHistoryRefresher(historyItem, jobHistory);
                tabfolder.addListener(jobHistoryRefresher);
                jobLog.setJobHistoryRefresher(jobHistoryRefresher);
            }

            
            tabMap.put(tabName, new TabMapEntry(tabItem, tabName, jobLog, TabMapEntry.OBJECT_TYPE_JOB_LOG));
        }
        int idx = tabfolder.indexOf(tabItem);
        tabfolder.setSelected(idx);
    }

    
    public void addTransHistory(TransMeta transMeta, boolean select)
    {
        // See if there already is a tab for this history view
        // If no, add it
        // If yes, select that tab
        //
        String tabName = makeHistoryTabName(transMeta);
        TabItem tabItem=findTabItem(tabName, TabMapEntry.OBJECT_TYPE_TRANSFORMATION_HISTORY);
        if (tabItem==null)
        {
            TransHistory transHistory = new TransHistory(tabfolder.getSwtTabset(), this, transMeta);
            tabItem = new TabItem(tabfolder, tabName, tabName);
            tabItem.setToolTipText(Messages.getString("Spoon.Title.ExecHistoryTransView.Tooltip", makeTransGraphTabName(transMeta)));
            tabItem.setControl(transHistory);
            
            // If there is an associated log window that's open, find it and add a refresher
            TransLog transLog = findTransLogOfTransformation(transMeta);
            if (transLog!=null)
            {
                TransHistoryRefresher transHistoryRefresher = new TransHistoryRefresher(tabItem, transHistory);
                tabfolder.addListener(transHistoryRefresher);
                transLog.setTransHistoryRefresher(transHistoryRefresher);
            }
            transHistory.markRefreshNeeded(); // will refresh when first selected
                        
            tabMap.put(tabName, new TabMapEntry(tabItem, tabName, transHistory, TabMapEntry.OBJECT_TYPE_TRANSFORMATION_HISTORY));
        }
        if (select)
        {
            int idx = tabfolder.indexOf(tabItem);
            tabfolder.setSelected(idx);
        }
    }
    
    public void addJobHistory(JobMeta jobMeta, boolean select)
    {
        // See if there already is a tab for this history view
        // If no, add it
        // If yes, select that tab
        //
        String tabName = makeJobHistoryTabName(jobMeta);
        TabItem tabItem=findTabItem(tabName, TabMapEntry.OBJECT_TYPE_JOB_HISTORY);
        if (tabItem==null)
        {
            JobHistory jobHistory = new JobHistory(tabfolder.getSwtTabset(), this, jobMeta);
            tabItem = new TabItem(tabfolder, tabName, tabName );
            tabItem.setToolTipText(Messages.getString("Spoon.Title.ExecHistoryJobView.Tooltip", makeJobGraphTabName(jobMeta)));
            tabItem.setControl(jobHistory);
            
            // If there is an associated log window that's open, find it and add a refresher
            JobLog jobLog = findJobLogOfJob(jobMeta);
            if (jobLog!=null)
            {
                JobHistoryRefresher jobHistoryRefresher = new JobHistoryRefresher(tabItem, jobHistory);
                tabfolder.addListener(jobHistoryRefresher);
                jobLog.setJobHistoryRefresher(jobHistoryRefresher);
            }
            jobHistory.markRefreshNeeded(); // will refresh when first selected
                        
            tabMap.put(tabName, new TabMapEntry(tabItem, tabName, jobHistory, TabMapEntry.OBJECT_TYPE_JOB_HISTORY));
        }
        if (select)
        {
            int idx = tabfolder.indexOf(tabItem);
            tabfolder.setSelected(idx);
        }
    }

    /**
     * Rename the tabs
     */
    public void renameTabs()
    {
        Collection<TabMapEntry> collection = tabMap.values();
        List<TabMapEntry> list = new ArrayList<TabMapEntry>();
        list.addAll(collection);
        
        for (TabMapEntry entry:list)
        {
            if (entry.getTabItem().isDisposed())
            {
                // this should not be in the map, get rid of it.
                tabMap.remove(entry.getObjectName());
                continue;
            }
            
            String before = entry.getTabItem().getText();
            Object managedObject = entry.getObject().getManagedObject();
            if (managedObject!=null)
            {
                if (entry.getObject() instanceof TransGraph)
                {
                    entry.getTabItem().setText( makeTransGraphTabName((TransMeta) managedObject) );
                    entry.getTabItem().setToolTipText( Messages.getString("Spoon.TabTrans.Tooltip", makeTransGraphTabName((TransMeta) managedObject)) );
                }
                else if (entry.getObject() instanceof TransLog)       entry.getTabItem().setText( makeLogTabName((TransMeta) managedObject) );
                else if (entry.getObject() instanceof TransHistory)   entry.getTabItem().setText( makeHistoryTabName((TransMeta) managedObject) );
                else if (entry.getObject() instanceof JobGraph)
                {
                    entry.getTabItem().setText( makeJobGraphTabName((JobMeta) managedObject) );
                    entry.getTabItem().setToolTipText( Messages.getString("Spoon.TabJob.Tooltip", makeJobGraphTabName((JobMeta) managedObject)) );
                }
                else if (entry.getObject() instanceof JobLog)        entry.getTabItem().setText( makeJobLogTabName( (JobMeta) managedObject ) );
                else if (entry.getObject() instanceof JobHistory)    entry.getTabItem().setText( makeJobHistoryTabName((JobMeta) managedObject) );
            }

            String after = entry.getTabItem().getText();

            if (!before.equals(after))
            {
                entry.setObjectName(after);
                tabMap.remove(before);
                tabMap.put(after, entry);
                
                // Also change the transformation map
                if (entry.getObject() instanceof TransGraph)
                {
                    transformationMap.remove(before);
                    transformationMap.put(after, (TransMeta)entry.getObject().getManagedObject());
                }
                // Also change the job map
                if (entry.getObject() instanceof JobGraph)
                {
                    jobMap.remove(before);
                    jobMap.put(after, (JobMeta)entry.getObject().getManagedObject());
                }            
            }
        }
        setShellText();
    }
    
    /**
     * This contains a map between the name of a transformation and the TransMeta object.
     * If the transformation has no name it will be mapped under a number [1], [2] etc. 

     * @return the transformation map
     */
    public Map<String, TransMeta> getTransformationMap()
    {
        return transformationMap;
    }

    /**
     * @param transformationMap the transformation map to set
     */
    public void setTransformationMap(Map<String,TransMeta> transformationMap)
    {
        this.transformationMap = transformationMap;
    }

    /**
     * @return the tabMap
     */
    public Map<String, TabMapEntry> getTabMap()
    {
        return tabMap;
    }

    /**
     * @param tabMap the tabMap to set
     */
    public void setTabMap(Map<String,TabMapEntry> tabMap)
    {
        this.tabMap = tabMap;
    }

    public void pasteSteps()
    {
        // Is there an active TransGraph?
        TransGraph transGraph = getActiveTransGraph();
        if (transGraph==null) return;
        TransMeta transMeta = (TransMeta)transGraph.getMeta();
        
        String clipcontent = fromClipboard();
        if (clipcontent != null)
        {
            Point lastMove = transGraph.getLastMove();
            if (lastMove != null)
            {
                pasteXML(transMeta, clipcontent, lastMove);
            }
        }
    }
    
    
    //////////////////////////////////////////////////////////////////////////////////////////////////
    //
    //
    //
    // Job manipulation steps...
    //
    //
    //
    //////////////////////////////////////////////////////////////////////////////////////////////////
    
    public JobEntryCopy newJobEntry(JobMeta jobMeta, String type_desc, boolean openit)
    {
        JobEntryLoader jobLoader = JobEntryLoader.getInstance();
        JobPlugin jobPlugin = null; 
        
        try
        {
            jobPlugin = jobLoader.findJobEntriesWithDescription(type_desc);
            if (jobPlugin==null)
            {
                // Check if it's not START or DUMMY
                if (JobMeta.STRING_SPECIAL_START.equals(type_desc) || JobMeta.STRING_SPECIAL_DUMMY.equals(type_desc))
                {
                    jobPlugin = jobLoader.findJobEntriesWithID(JobMeta.STRING_SPECIAL);
                }
            }
            
            if (jobPlugin!=null)
            {
                // Determine name & number for this entry.
                String basename = type_desc;
                int nr = jobMeta.generateJobEntryNameNr(basename);
                String entry_name = basename+" "+nr; //$NON-NLS-1$
                
                // Generate the appropriate class...
                JobEntryInterface jei = jobLoader.getJobEntryClass(jobPlugin); 
                jei.setName(entry_name);
        
                if (jei.isSpecial())
                {
                    if (JobMeta.STRING_SPECIAL_START.equals(type_desc))
                    {
                        // Check if start is already on the canvas...
                        if (jobMeta.findStart()!=null) 
                        {
                            JobGraph.showOnlyStartOnceMessage(shell);
                            return null;
                        }
                        ((JobEntrySpecial)jei).setStart(true);
                        jei.setName(JobMeta.STRING_SPECIAL_START);
                    }
                    if (JobMeta.STRING_SPECIAL_DUMMY.equals(type_desc))
                    {
                        ((JobEntrySpecial)jei).setDummy(true);
                        jei.setName(JobMeta.STRING_SPECIAL_DUMMY);
                    }
                }
                
                if (openit)
                {
                    JobEntryDialogInterface d = jei.getDialog(shell,jei,jobMeta,entry_name,rep);
                    if (d.open()!=null)
                    {
                        JobEntryCopy jge = new JobEntryCopy();
                        jge.setEntry(jei);
                        jge.setLocation(50,50);
                        jge.setNr(0);
                        jobMeta.addJobEntry(jge);
                        addUndoNew(jobMeta, new JobEntryCopy[] { jge }, new int[] { jobMeta.indexOfJobEntry(jge) });
                        refreshGraph();
                        refreshTree();
                        return jge;
                    }
                    else
                    {
                        return null;
                    }
                }
                else
                {
                    JobEntryCopy jge = new JobEntryCopy();
                    jge.setEntry(jei);
                    jge.setLocation(50,50);
                    jge.setNr(0);
                    jobMeta.addJobEntry(jge);
                    addUndoNew(jobMeta, new JobEntryCopy[] { jge }, new int[] { jobMeta.indexOfJobEntry(jge) });
                    refreshGraph();
                    refreshTree();
                    return jge;
                }
            }
            else
            {
                return null;
            }
        }
        catch(Throwable e)
        {
            new ErrorDialog(shell, Messages.getString("Spoon.ErrorDialog.UnexpectedErrorCreatingNewJobGraphEntry.Title"), Messages.getString("Spoon.ErrorDialog.UnexpectedErrorCreatingNewJobGraphEntry.Message"),new Exception(e));  //$NON-NLS-1$ //$NON-NLS-2$
            return null;
        }
    }

    public void editJobEntry(JobMeta jobMeta, JobEntryCopy je)
    {
        try
        {
            log.logBasic(toString(), "edit job graph entry: "+je.getName()); //$NON-NLS-1$
            
            JobEntryCopy before =(JobEntryCopy)je.clone_deep();
            boolean entry_changed=false;
            
            JobEntryInterface jei = je.getEntry();
            
            if (jei.isSpecial())
            {
                JobEntrySpecial special = (JobEntrySpecial) jei;
                if (special.isDummy()) return;
            }
            
            JobEntryDialogInterface d = jei.getDialog(shell, jei, jobMeta,je.getName(),rep); 
            if (d!=null)
            {
                if (d.open()!=null)
                {
                    entry_changed=true;
                }
        
                if (entry_changed)
                {
                    JobEntryCopy after = (JobEntryCopy) je.clone();
                    addUndoChange(jobMeta, new JobEntryCopy[] { before }, new JobEntryCopy[] { after }, new int[] { jobMeta.indexOfJobEntry(je) } );
                    refreshGraph();
                    refreshTree();
                }
            }
            else
            {
                MessageBox mb = new MessageBox(shell, SWT.OK | SWT.ICON_INFORMATION);
                mb.setMessage(Messages.getString("Spoon.Dialog.JobEntryCanNotBeChanged.Message")); //$NON-NLS-1$
                mb.setText(Messages.getString("Spoon.Dialog.JobEntryCanNotBeChanged.Title")); //$NON-NLS-1$
                mb.open();
            }

        }
        catch(Exception e)
        {
            if (!shell.isDisposed()) new ErrorDialog(shell, Messages.getString("Spoon.ErrorDialog.ErrorEditingJobEntry.Title"), Messages.getString("Spoon.ErrorDialog.ErrorEditingJobEntry.Message"), e); //$NON-NLS-1$ //$NON-NLS-2$
        }
    }

    
    public JobEntryTrans newJobEntry(JobMeta jobMeta, int type)
    {
        JobEntryTrans je = new JobEntryTrans();
        je.setType(type);
        String basename = JobEntryInterface.typeDesc[type]; 
        int nr = jobMeta.generateJobEntryNameNr(basename);
        je.setName(basename+" "+nr); //$NON-NLS-1$

        setShellText();
        
        return je;
    }

    public void deleteJobEntryCopies(JobMeta jobMeta, JobEntryCopy jobEntry)
    {
        String name = jobEntry.getName();
        // TODO Show warning "Are you sure?  This operation can't be undone." + clear undo buffer.
        
        // First delete all the hops using entry with name:
        JobHopMeta hi[] = jobMeta.getAllJobHopsUsing(name);
        if (hi.length>0)
        {
            int hix[] = new int[hi.length];
            for (int i=0;i<hi.length;i++) hix[i] = jobMeta.indexOfJobHop(hi[i]);
            
            addUndoDelete(jobMeta, hi, hix);
            for (int i=hix.length-1;i>=0;i--) jobMeta.removeJobHop(hix[i]);
        }
        
        // Then delete all the entries with name:
        JobEntryCopy je[] = jobMeta.getAllJobGraphEntries(name);
        int jex[] = new int[je.length];
        for (int i=0;i<je.length;i++) jex[i] = jobMeta.indexOfJobEntry(je[i]);

        if (je.length>0) addUndoDelete(jobMeta, je, jex);
        for (int i=jex.length-1;i>=0;i--) jobMeta.removeJobEntry(jex[i]);
        
        jobMeta.clearUndo();
        setUndoMenu(jobMeta);
        refreshGraph();
        refreshTree();        
    }

    public void dupeJobEntry(JobMeta jobMeta, JobEntryCopy jobEntry)
    {
        if (jobEntry!=null && !jobEntry.isStart())
        {
            JobEntryCopy dupejge = (JobEntryCopy)jobEntry.clone();
            dupejge.setNr( jobMeta.findUnusedNr(dupejge.getName()) );
            if (dupejge.isDrawn())
            {
                Point p = jobEntry.getLocation();
                dupejge.setLocation(p.x+10, p.y+10);
            }
            jobMeta.addJobEntry(dupejge);
            refreshGraph();
            refreshTree();
            setShellText();
        }
    }
    
    
    public void copyJobEntries(JobMeta jobMeta, JobEntryCopy jec[])
    {
        if (jec==null || jec.length==0) return;
        
        String xml = XMLHandler.getXMLHeader();
        xml+="<jobentries>"+Const.CR; //$NON-NLS-1$

        for (int i=0;i<jec.length;i++)
        {
            xml+=jec[i].getXML();
        }
        
        xml+="    </jobentries>"+Const.CR; //$NON-NLS-1$
        
        toClipboard(xml);
    }

    public void pasteXML(JobMeta jobMeta, String clipcontent, Point loc)
    {
        try
        {
            Document doc = XMLHandler.loadXMLString(clipcontent);

            // De-select all, re-select pasted steps...
            jobMeta.unselectAll();
            
            Node entriesnode = XMLHandler.getSubNode(doc, "jobentries"); //$NON-NLS-1$
            int nr = XMLHandler.countNodes(entriesnode, "entry"); //$NON-NLS-1$
            log.logDebug(toString(), "I found "+nr+" job entries to paste on location: "+loc); //$NON-NLS-1$ //$NON-NLS-2$
            JobEntryCopy entries[] = new JobEntryCopy[nr];
            
            //Point min = new Point(loc.x, loc.y);
            Point min = new Point(99999999,99999999);
            
            for (int i=0;i<nr;i++)
            {
                Node entrynode = XMLHandler.getSubNodeByNr(entriesnode, "entry", i); //$NON-NLS-1$
                entries[i] = new JobEntryCopy(entrynode, jobMeta.getDatabases(), rep);

                String name = jobMeta.getAlternativeJobentryName(entries[i].getName());
                entries[i].setName(name);

                if (loc!=null)
                {
                    Point p = entries[i].getLocation();
                    
                    if (min.x > p.x) min.x = p.x;
                    if (min.y > p.y) min.y = p.y;
                }
            }
            
            // What's the difference between loc and min?
            // This is the offset:
            Point offset = new Point(loc.x-min.x, loc.y-min.y);
            
            // Undo/redo object positions...
            int position[] = new int[entries.length];
            
            for (int i=0;i<entries.length;i++)
            {
                Point p = entries[i].getLocation();
                String name = entries[i].getName();
                
                entries[i].setLocation(p.x+offset.x, p.y+offset.y);
                
                // Check the name, find alternative...
                entries[i].setName( jobMeta.getAlternativeJobentryName(name) );
                jobMeta.addJobEntry(entries[i]);
                position[i] = jobMeta.indexOfJobEntry(entries[i]);
            }
            
            // Save undo information too...
            addUndoNew(jobMeta, entries, position);

            if (jobMeta.hasChanged())
            {
                refreshTree();
                refreshGraph();
            }
        }
        catch(KettleException e)
        {
            new ErrorDialog(shell, Messages.getString("Spoon.ErrorDialog.ErrorPasingJobEntries.Title"), Messages.getString("Spoon.ErrorDialog.ErrorPasingJobEntries.Message"), e); //$NON-NLS-1$ //$NON-NLS-2$
        }
    }
    
    public void newJobHop(JobMeta jobMeta, JobEntryCopy fr, JobEntryCopy to)
    {
        JobHopMeta hi = new JobHopMeta(fr, to);
        jobMeta.addJobHop(hi);
        addUndoNew(jobMeta, new JobHopMeta[] {hi}, new int[] { jobMeta.indexOfJobHop(hi)} );
        refreshGraph();
        refreshTree();
    }

    /**
     * Create a job that extracts tables & data from a database.<p><p>
     * 
     * 0) Select the database to rip<p>
     * 1) Select the tables in the database to rip<p>
     * 2) Select the database to dump to<p>
     * 3) Select the repository directory in which it will end up<p>
     * 4) Select a name for the new job<p>
     * 5) Create an empty job with the selected name.<p>
     * 6) Create 1 transformation for every selected table<p>
     * 7) add every created transformation to the job & evaluate<p>
     * 
     */
    public void ripDBWizard()
    {
        final List<DatabaseMeta> databases = getActiveDatabases();
        if (databases.size() == 0) return; // Nothing to do here

        final RipDatabaseWizardPage1 page1 = new RipDatabaseWizardPage1("1", databases); //$NON-NLS-1$
        page1.createControl(shell);
        final RipDatabaseWizardPage2 page2 = new RipDatabaseWizardPage2("2"); //$NON-NLS-1$
        page2.createControl(shell);
        final RipDatabaseWizardPage3 page3 = new RipDatabaseWizardPage3("3", rep); //$NON-NLS-1$
        page3.createControl(shell);

        Wizard wizard = new Wizard()
        {
            public boolean performFinish()
            {
                JobMeta jobMeta = ripDB(
                        databases, 
                        page3.getJobname(),  
                        page3.getRepositoryDirectory(),
                        page3.getDirectory(),
                        page1.getSourceDatabase(), 
                        page1.getTargetDatabase(), 
                        page2.getSelection()
                      );
                if (jobMeta==null) return false;
                
                if (page3.getRepositoryDirectory()!=null)
                {
                    saveToRepository(jobMeta);
                }
                else
                {
                	saveToFile(jobMeta);
                }
                
                addJobGraph(jobMeta);
                return true;
            }

            /**
             * @see org.eclipse.jface.wizard.Wizard#canFinish()
             */
            public boolean canFinish()
            {
                return page3.canFinish();
            }
        };

        wizard.addPage(page1);
        wizard.addPage(page2);
        wizard.addPage(page3);

        WizardDialog wd = new WizardDialog(shell, wizard);
        wd.setMinimumPageSize(700, 400);
        wd.open();
    }

    public JobMeta ripDB(
                final List<DatabaseMeta> databases, 
                final String jobname, 
                final RepositoryDirectory repdir, 
                final String directory,
                final DatabaseMeta sourceDbInfo, 
                final DatabaseMeta targetDbInfo, 
                final String[] tables
            )
    {
        //
        // Create a new job...
        //
        final JobMeta jobMeta = new JobMeta(log);
        jobMeta.setDatabases(databases);
        jobMeta.setFilename(null);
        jobMeta.setName(jobname);

        if (rep!=null)
        {
            jobMeta.setDirectory(repdir);
        }
        else
        {
            jobMeta.setFilename(Const.createFilename(directory, jobname, Const.STRING_JOB_DEFAULT_EXT));
        }

        refreshTree();
        refreshGraph();

        final Point location = new Point(50, 50);

        // The start entry...
        final JobEntryCopy start = JobMeta.createStartEntry();
        start.setLocation(new Point(location.x, location.y));
        start.setDrawn();
        jobMeta.addJobEntry(start);

        // final Thread parentThread = Thread.currentThread();

        // Create a dialog with a progress indicator!
        IRunnableWithProgress op = new IRunnableWithProgress()
        {
            public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException
            {
                // This is running in a new process: copy some KettleVariables info
                // LocalVariables.getInstance().createKettleVariables(Thread.currentThread().getName(),
                // parentThread.getName(), true);

                monitor.beginTask(Messages.getString("Spoon.RipDB.Monitor.BuildingNewJob"), tables.length); //$NON-NLS-1$
                monitor.worked(0);
                JobEntryCopy previous = start;

                // Loop over the table-names...
                for (int i = 0; i < tables.length && !monitor.isCanceled(); i++)
                {
                    monitor.setTaskName(Messages.getString("Spoon.RipDB.Monitor.ProcessingTable") + tables[i] + "]..."); //$NON-NLS-1$ //$NON-NLS-2$
                    //
                    // Create the new transformation...
                    //
                    String transname = Messages.getString("Spoon.RipDB.Monitor.Transname1") + sourceDbInfo + "].[" + tables[i] + Messages.getString("Spoon.RipDB.Monitor.Transname2") + targetDbInfo + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

                    TransMeta transMeta = new TransMeta((String) null, transname, null);

                    if (repdir!=null)
                    {
                        transMeta.setDirectory(repdir);
                    }
                    else
                    {
                        transMeta.setFilename(Const.createFilename(directory, transname, Const.STRING_TRANS_DEFAULT_EXT));
                    }
                    
                    // Add the source & target db
                    transMeta.addDatabase(sourceDbInfo);
                    transMeta.addDatabase(targetDbInfo);

                    //
                    // Add a note
                    //
                    String note = Messages.getString("Spoon.RipDB.Monitor.Note1") + tables[i] + Messages.getString("Spoon.RipDB.Monitor.Note2") + sourceDbInfo + "]" + Const.CR; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                    note += Messages.getString("Spoon.RipDB.Monitor.Note3") + tables[i] + Messages.getString("Spoon.RipDB.Monitor.Note4") + targetDbInfo + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                    NotePadMeta ni = new NotePadMeta(note, 150, 10, -1, -1);
                    transMeta.addNote(ni);

                    //
                    // Add the TableInputMeta step...
                    // 
                    String fromstepname = Messages.getString("Spoon.RipDB.Monitor.FromStep.Name") + tables[i] + "]"; //$NON-NLS-1$ //$NON-NLS-2$
                    TableInputMeta tii = new TableInputMeta();
                    tii.setDatabaseMeta(sourceDbInfo);
                    tii.setSQL("SELECT * FROM " + sourceDbInfo.quoteField(tables[i])); //$NON-NLS-1$

                    String fromstepid = StepLoader.getInstance().getStepPluginID(tii);
                    StepMeta fromstep = new StepMeta(fromstepid, fromstepname, tii);
                    fromstep.setLocation(150, 100);
                    fromstep.setDraw(true);
                    fromstep.setDescription(Messages.getString("Spoon.RipDB.Monitor.FromStep.Description") + tables[i] + Messages.getString("Spoon.RipDB.Monitor.FromStep.Description2") + sourceDbInfo + "]"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                    transMeta.addStep(fromstep);

                    //
                    // Add the TableOutputMeta step...
                    //
                    String tostepname = Messages.getString("Spoon.RipDB.Monitor.ToStep.Name") + tables[i] + "]"; //$NON-NLS-1$ //$NON-NLS-2$
                    TableOutputMeta toi = new TableOutputMeta();
                    toi.setDatabaseMeta(targetDbInfo);
                    toi.setTablename(tables[i]);
                    toi.setCommitSize(100);
                    toi.setTruncateTable(true);

                    String tostepid = StepLoader.getInstance().getStepPluginID(toi);
                    StepMeta tostep = new StepMeta(tostepid, tostepname, toi);
                    tostep.setLocation(500, 100);
                    tostep.setDraw(true);
                    tostep.setDescription(Messages.getString("Spoon.RipDB.Monitor.ToStep.Description1") + tables[i] + Messages.getString("Spoon.RipDB.Monitor.ToStep.Description2") + targetDbInfo + "]"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                    transMeta.addStep(tostep);

                    //
                    // Add a hop between the two steps...
                    //
                    TransHopMeta hi = new TransHopMeta(fromstep, tostep);
                    transMeta.addTransHop(hi);

                    //
                    // Now we generate the SQL needed to run for this transformation.
                    //
                    // First set the limit to 1 to speed things up!
                    String tmpSql = tii.getSQL();
                    tii.setSQL(tii.getSQL() + sourceDbInfo.getLimitClause(1));
                    String sql = ""; //$NON-NLS-1$
                    try
                    {
                        sql = transMeta.getSQLStatementsString();
                    }
                    catch (KettleStepException kse)
                    {
                        throw new InvocationTargetException(kse,
                                Messages.getString("Spoon.RipDB.Exception.ErrorGettingSQLFromTransformation") + transMeta + "] : " + kse.getMessage()); //$NON-NLS-1$ //$NON-NLS-2$
                    }
                    // remove the limit
                    tii.setSQL(tmpSql);

                    //
                    // Now, save the transformation...
                    //
                    boolean ok;
                    if (rep!=null)
                    {
                        ok=saveToRepository(transMeta);
                    }
                    else
                    {
                        ok=saveToFile(transMeta);
                    }
                    if (!ok)
                    {
                        throw new InvocationTargetException(new Exception(Messages.getString("Spoon.RipDB.Exception.UnableToSaveTransformationToRepository")), Messages.getString("Spoon.RipDB.Exception.UnableToSaveTransformationToRepository")); //$NON-NLS-1$
                    }

                    // We can now continue with the population of the job...
                    // //////////////////////////////////////////////////////////////////////

                    location.x = 250;
                    if (i > 0) location.y += 100;

                    //
                    // We can continue defining the job.
                    //
                    // First the SQL, but only if needed!
                    // If the table exists & has the correct format, nothing is done
                    //
                    if (!Const.isEmpty(sql))
                    {
                        String jesqlname = Messages.getString("Spoon.RipDB.JobEntrySQL.Name") + tables[i] + "]"; //$NON-NLS-1$ //$NON-NLS-2$
                        JobEntrySQL jesql = new JobEntrySQL(jesqlname);
                        jesql.setDatabase(targetDbInfo);
                        jesql.setSQL(sql);
                        jesql.setDescription(Messages.getString("Spoon.RipDB.JobEntrySQL.Description") + targetDbInfo + "].[" + tables[i] + "]"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

                        JobEntryCopy jecsql = new JobEntryCopy();
                        jecsql.setEntry(jesql);
                        jecsql.setLocation(new Point(location.x, location.y));
                        jecsql.setDrawn();
                        jobMeta.addJobEntry(jecsql);

                        // Add the hop too...
                        JobHopMeta jhi = new JobHopMeta(previous, jecsql);
                        jobMeta.addJobHop(jhi);
                        previous = jecsql;
                    }

                    //
                    // Add the jobentry for the transformation too...
                    //
                    String jetransname = Messages.getString("Spoon.RipDB.JobEntryTrans.Name") + tables[i] + "]"; //$NON-NLS-1$ //$NON-NLS-2$
                    JobEntryTrans jetrans = new JobEntryTrans(jetransname);
                    jetrans.setTransname(transMeta.getName());
                    if (rep!=null)
                    {
                        jetrans.setDirectory(transMeta.getDirectory());
                    }
                    else
                    {
                        jetrans.setFileName( Const.createFilename("${"+Const.INTERNAL_VARIABLE_JOB_FILENAME_DIRECTORY+"}", transMeta.getName(), Const.STRING_TRANS_DEFAULT_EXT) );
                    }

                    JobEntryCopy jectrans = new JobEntryCopy(log, jetrans);
                    jectrans.setDescription(Messages.getString("Spoon.RipDB.JobEntryTrans.Description1") + Const.CR + Messages.getString("Spoon.RipDB.JobEntryTrans.Description2") + sourceDbInfo + "].[" + tables[i] + "]" + Const.CR + Messages.getString("Spoon.RipDB.JobEntryTrans.Description3") + targetDbInfo + "].[" + tables[i] + "]"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$
                    jectrans.setDrawn();
                    location.x += 400;
                    jectrans.setLocation(new Point(location.x, location.y));
                    jobMeta.addJobEntry(jectrans);

                    // Add a hop between the last 2 job entries.
                    JobHopMeta jhi2 = new JobHopMeta(previous, jectrans);
                    jobMeta.addJobHop(jhi2);
                    previous = jectrans;

                    monitor.worked(1);
                }

                monitor.worked(100);
                monitor.done();
            }
        };

        try
        {
            ProgressMonitorDialog pmd = new ProgressMonitorDialog(shell);
            pmd.run(false, true, op);
        }
        catch (InvocationTargetException e)
        {
            new ErrorDialog(shell, Messages.getString("Spoon.ErrorDialog.RipDB.ErrorRippingTheDatabase.Title"), Messages.getString("Spoon.ErrorDialog.RipDB.ErrorRippingTheDatabase.Message"), e); //$NON-NLS-1$ //$NON-NLS-2$
            return null;
        }
        catch (InterruptedException e)
        {
            new ErrorDialog(shell, Messages.getString("Spoon.ErrorDialog.RipDB.ErrorRippingTheDatabase.Title"), Messages.getString("Spoon.ErrorDialog.RipDB.ErrorRippingTheDatabase.Message"), e); //$NON-NLS-1$ //$NON-NLS-2$
            return null;
        }
        finally
        {
            refreshGraph();
            refreshTree();
        }
        
        return jobMeta;
    }

    /**
     * Set the core object state.
     * 
     * @param state
     */
    public void setCoreObjectsState(int state)
    {
    	coreObjectsState = state;
    }
    
    /**
     * Get the core object state.
     * 
     * @return state.
     */
    public int getCoreObjectsState()
    {
    	return coreObjectsState;
    }
    
    public LogWriter getLog( ) {
    		return log;
    }
    
    public Repository getRepository() {
		return rep;
}

    public void setRepository( Repository rep ) {
		this.rep = rep;
}

    public void addMenuListener( String id, Object listener, String methodName ) {
    		menuListeners.add( new Object[] { id, listener, methodName } );
    }
}