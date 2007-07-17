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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.vfs.FileObject;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
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
import org.pentaho.di.core.SourceToTargetMapping;
import org.pentaho.di.core.changed.ChangedFlagInterface;
import org.pentaho.di.core.clipboard.ImageDataTransfer;
import org.pentaho.di.core.database.DatabaseMeta;
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
import org.pentaho.di.core.dialog.ShowBrowserDialog;
import org.pentaho.di.core.dialog.Splash;
import org.pentaho.di.core.dnd.DragAndDropContainer;
import org.pentaho.di.core.dnd.XMLTransfer;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.exception.KettleRowException;
import org.pentaho.di.core.gui.GUIResource;
import org.pentaho.di.core.gui.Point;
import org.pentaho.di.core.gui.SpoonFactory;
import org.pentaho.di.core.gui.SpoonInterface;
import org.pentaho.di.core.gui.UndoInterface;
import org.pentaho.di.core.gui.WindowProperty;
import org.pentaho.di.core.gui.XulHelper;
import org.pentaho.di.core.logging.LogWriter;
import org.pentaho.di.core.reflection.StringSearchResult;
import org.pentaho.di.core.row.RowMeta;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.row.ValueMeta;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.core.undo.TransAction;
import org.pentaho.di.core.util.EnvUtil;
import org.pentaho.di.core.util.ImageUtil;
import org.pentaho.di.core.variables.VariableSpace;
import org.pentaho.di.core.variables.Variables;
import org.pentaho.di.core.vfs.KettleVFS;
import org.pentaho.di.core.widget.TreeMemory;
import org.pentaho.di.core.xml.XMLHandler;
import org.pentaho.di.i18n.LanguageChoice;
import org.pentaho.di.job.JobEntryLoader;
import org.pentaho.di.job.JobEntryType;
import org.pentaho.di.job.JobMeta;
import org.pentaho.di.job.JobPlugin;
import org.pentaho.di.job.dialog.JobLoadProgressDialog;
import org.pentaho.di.job.dialog.JobSaveProgressDialog;
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
import org.pentaho.di.spoon.delegates.SpoonDelegates;
import org.pentaho.di.spoon.dialog.AnalyseImpactProgressDialog;
import org.pentaho.di.spoon.dialog.CheckTransProgressDialog;
import org.pentaho.di.spoon.dialog.SaveProgressDialog;
import org.pentaho.di.spoon.dialog.ShowCreditsDialog;
import org.pentaho.di.spoon.dialog.TipsDialog;
import org.pentaho.di.spoon.job.JobGraph;
import org.pentaho.di.spoon.job.JobHistory;
import org.pentaho.di.spoon.job.JobLog;
import org.pentaho.di.spoon.trans.TransGraph;
import org.pentaho.di.spoon.trans.TransHistory;
import org.pentaho.di.spoon.trans.TransLog;
import org.pentaho.di.spoon.wizards.CopyTableWizardPage1;
import org.pentaho.di.spoon.wizards.CopyTableWizardPage2;
import org.pentaho.di.trans.DatabaseImpact;
import org.pentaho.di.trans.HasDatabasesInterface;
import org.pentaho.di.trans.StepLoader;
import org.pentaho.di.trans.StepPlugin;
import org.pentaho.di.trans.TransExecutionConfiguration;
import org.pentaho.di.trans.TransHopMeta;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.dialog.TransHopDialog;
import org.pentaho.di.trans.dialog.TransLoadProgressDialog;
import org.pentaho.di.trans.step.BaseStep;
import org.pentaho.di.trans.step.StepDialogInterface;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.step.StepMetaInterface;
import org.pentaho.di.trans.step.StepPartitioningMeta;
import org.pentaho.di.trans.steps.selectvalues.SelectValuesMeta;
import org.pentaho.di.version.BuildVersion;
import org.pentaho.vfs.ui.VfsFileChooserDialog;
import org.pentaho.xul.menu.XulMenu;
import org.pentaho.xul.menu.XulMenuBar;
import org.pentaho.xul.menu.XulMenuItem;
import org.pentaho.xul.menu.XulPopupMenu;
import org.pentaho.xul.swt.menu.Menu;
import org.pentaho.xul.swt.menu.MenuChoice;
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
	public static final String APP_NAME = Messages.getString("Spoon.Application.Name"); // "Spoon";

	private static Spoon staticSpoon;
	
	private LogWriter log;
	private Display display;
	private Shell shell;
	private boolean destroy;

	private SashForm sashform;
	public TabSet tabfolder;

	public boolean shift;
	public boolean control;

	// THE HANDLERS
	public SpoonDelegates delegates = new SpoonDelegates(this);

	public RowMetaAndData variables = new RowMetaAndData(new RowMeta(), new Object[] {});

	/**
	 * These are the arguments that were given at Spoon launch time...
	 */
	private String[] arguments;

	private boolean stopped;

	private Cursor cursor_hourglass, cursor_hand;

	public Props props;

	public Repository rep;

	/**
	 * This contains a map with all the unnamed transformation (just a filename)
	 */

	private XulToolbar toolbar;

	private XulMenuBar menuBar;

	private Tree selectionTree;
	// private TreeItem tiTransBase, tiJobBase;

	private Tree coreObjectsTree;

	private static final String APPL_TITLE = APP_NAME;

	private static final String STRING_WELCOME_TAB_NAME = Messages.getString("Spoon.Title.STRING_WELCOME");

	private static final String FILE_WELCOME_PAGE = Messages.getString("Spoon.Title.STRING_DOCUMENT_WELCOME"); // "docs/English/welcome/kettle_document_map.html";

	public KeyAdapter defKeys;
	public KeyAdapter modKeys;

	// private Menu mBar;

	private Composite tabComp;

	private ExpandBar mainExpandBar;
	private ExpandBar expandBar;

	private TransExecutionConfiguration executionConfiguration;

	// private TreeItem tiTrans, tiJobs;

	private Menu spoonMenu; // Connections, Steps & hops

	private int coreObjectsState = STATE_CORE_OBJECTS_NONE;

	private boolean stepHistoryChanged;

	protected Map<String, FileListener> fileExtensionMap = new HashMap<String, FileListener>();
	protected Map<String, FileListener> fileNodeMap = new HashMap<String, FileListener>();

	private List<Object[]> menuListeners = new ArrayList<Object[]>();

    public Spoon(Display d) {
		this(null, d, null);
	}

	public Spoon(LogWriter l, Repository rep)
	{
		this(l, null, rep);
	}

	public Spoon(LogWriter log, Display d, Repository rep)
	{
		this.log = log;
		this.rep = rep;

		if (d != null)
		{
			display = d;
			destroy = false;
        } 
        else 
		{
			display = new Display();
			destroy = true;
		}
		shell = new Shell(display);
		shell.setText(APPL_TITLE);
		staticSpoon = this;
		SpoonFactory.setSpoonInstance(this);
	}

    public void init( TransMeta ti ) {
		FormLayout layout = new FormLayout();
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		shell.setLayout(layout);

		addFileListener(new TransFileListener(), "ktr", TransMeta.XML_TAG);

		addFileListener(new JobFileListener(), "kjb", JobMeta.XML_TAG);

		// INIT Data structure
		if (ti != null)
			delegates.trans.addTransformation(ti);

		if (!Props.isInitialized())
		{
			// log.logDetailed(toString(), "Load properties for Spoon...");
			log.logDetailed(toString(), Messages.getString("Spoon.Log.LoadProperties"));
            Props.init(display, Props.TYPE_PROPERTIES_SPOON);  // things to remember...
		}
		props = Props.getInstance();

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
		cursor_hand = new Cursor(display, SWT.CURSOR_HAND);

		// widgets = new WidgetContainer();

		defKeys = new KeyAdapter()
		{
			public void keyPressed(KeyEvent e)
			{

				boolean ctrl = ((e.stateMask & SWT.CONTROL) != 0);
				boolean alt = ((e.stateMask & SWT.ALT) != 0);

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
						key = new String(new char[] { c });
                    		} else {
						char c = (char) ('a' + (e.character - 1));
						key = new String(new char[] { c });
					}
                    } else 
                    if( key == null )
                    {
					char c = e.character;
					key = new String(new char[] { c });
				}

				menuBar.handleAccessKey(key, alt, ctrl);
			}
		};
		modKeys = new KeyAdapter()
		{
			public void keyPressed(KeyEvent e)
			{
				shift = (e.keyCode == SWT.SHIFT);
				control = (e.keyCode == SWT.CONTROL);
			}

			public void keyReleased(KeyEvent e)
			{
				shift = (e.keyCode == SWT.SHIFT);
				control = (e.keyCode == SWT.CONTROL);
			}
		};

        
        addBar();

        

		sashform = new SashForm(shell, SWT.HORIZONTAL);
		props.setLook(sashform);

		FormData fdSash = new FormData();
		fdSash.left = new FormAttachment(0, 0);
		fdSash.top = new FormAttachment((org.eclipse.swt.widgets.ToolBar) toolbar.getNativeObject(), 0);
		fdSash.bottom = new FormAttachment(100, 0);
		fdSash.right = new FormAttachment(100, 0);
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
				e.doit = quitFile();
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

	public void closeFile()
	{
		TransMeta transMeta = getActiveTransformation();
		if (transMeta != null)
		{
			// If a transformation is the current active tab, close it
			delegates.trans.closeTransformation(transMeta);
		} else
		{
			// Otherwise try to find the current open job and close it
			JobMeta jobMeta = getActiveJob();
			if (jobMeta != null)
				delegates.jobs.closeJob(jobMeta);
		}
	}

	public void closeSpoonBrowser()
	{
		delegates.tabs.removeTab(STRING_WELCOME_TAB_NAME);
		TabItem tab = delegates.tabs.findTabItem(STRING_WELCOME_TAB_NAME, TabMapEntry.OBJECT_TYPE_BROWSER);
		if (tab != null)
			tab.dispose();
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

		for (int t = 0; t < transMetas.length; t++)
		{
			TransMeta transMeta = transMetas[t];
			String filterString = esd.getFilterString();
			String filter = filterString;
            if (filter!=null) filter = filter.toUpperCase();

            List<StringSearchResult> stringList = transMeta.getStringList(esd.isSearchingSteps(), esd.isSearchingDatabases(), esd.isSearchingNotes());
			for (int i = 0; i < stringList.size(); i++)
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
		for (int t = 0; t < jobMetas.length; t++)
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

		if (rows.size() != 0)
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

	private void fillVariables(RowMetaAndData vars)
	{
		TransMeta[] transMetas = getLoadedTransformations();
		JobMeta[] jobMetas = getLoadedJobs();
        if ( (transMetas==null || transMetas.length==0) && (jobMetas==null || jobMetas.length==0)) return;

		Properties sp = new Properties();
		sp.putAll(System.getProperties());

		VariableSpace space = Variables.getADefaultVariableSpace();
		String keys[] = space.listVariables();
		for (int i = 0; i < keys.length; i++)
		{
			sp.put(keys[i], space.getVariable(keys[i]));
		}

		for (int t = 0; t < transMetas.length; t++)
		{
			TransMeta transMeta = transMetas[t];

			List<String> list = transMeta.getUsedVariables();
			for (int i = 0; i < list.size(); i++)
			{
				String varName = list.get(i);
				String varValue = sp.getProperty(varName, "");
                if (variables.getRowMeta().indexOfValue(varName)<0 && !varName.startsWith(Const.INTERNAL_VARIABLE_PREFIX))
				{
					variables.addValue(new ValueMeta(varName, ValueMetaInterface.TYPE_STRING), varValue);
				}
			}
		}

		for (int t = 0; t < jobMetas.length; t++)
		{
			JobMeta jobMeta = jobMetas[t];

			List<String> list = jobMeta.getUsedVariables();
			for (int i = 0; i < list.size(); i++)
			{
				String varName = list.get(i);
				String varValue = sp.getProperty(varName, "");
                if (variables.getRowMeta().indexOfValue(varName)<0 && !varName.startsWith(Const.INTERNAL_VARIABLE_PREFIX))
				{
					variables.addValue(new ValueMeta(varName, ValueMetaInterface.TYPE_STRING), varValue);
				}
			}
		}
	}

	public void getVariables()
	{
		fillVariables(variables);

		// Now ask the use for more info on these!
		EnterStringsDialog esd = new EnterStringsDialog(shell, SWT.NONE, variables);
		esd.setTitle(Messages.getString("Spoon.Dialog.SetVariables.Title"));
		esd.setMessage(Messages.getString("Spoon.Dialog.SetVariables.Message"));
		esd.setReadOnly(false);
		if (esd.open() != null)
		{
			// here was code to put the values in another place.
		}
	}

	public void showVariables()
	{
		fillVariables(variables);

		// Now ask the use for more info on these!
		EnterStringsDialog esd = new EnterStringsDialog(shell, SWT.NONE, variables);
		esd.setTitle(Messages.getString("Spoon.Dialog.ShowVariables.Title"));
		esd.setMessage(Messages.getString("Spoon.Dialog.ShowVariables.Message"));
		esd.setReadOnly(true);
		esd.open();
	}

	public void open()
	{
		shell.open();
		// Perhaps the transformation contains elements at startup?
		refreshTree(); // Do a complete refresh then...

		setShellText();

		if (props.showTips())
		{
			TipsDialog tip = new TipsDialog(shell);
			tip.open();
		}
	}

	public boolean readAndDispatch()
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

	private Map<String, Menu> menuMap = new HashMap<String, Menu>();

	public void addMenuListeners()
	{
		try
		{
			// first get the XML document
			URL url = XulHelper.getAndValidate(XUL_FILE_MENU_PROPERTIES);
				Properties props = new Properties();
				props.load(url.openStream());
				String ids[] = menuBar.getMenuItemIds();
				for (int i = 0; i < ids.length; i++)
				{
					String methodName = (String) props.get(ids[i]);
					if (methodName != null)
					{
						menuBar.addMenuListener(ids[i], this, methodName);
						toolbar.addMenuListener(ids[i], this, methodName);

					}
				}
				for (String id : menuMap.keySet())
				{
					PopupMenu menu = (PopupMenu) menuMap.get(id);
					ids = menu.getMenuItemIds();
					for (int i = 0; i < ids.length; i++)
					{
						String methodName = (String) props.get(ids[i]);
						if (methodName != null)
						{
							menu.addMenuListener(ids[i], this, methodName);
						}
					}
					for (int i = 0; i < menuListeners.size(); i++)
					{
						Object info[] = menuListeners.get(i);
						menu.addMenuListener((String) info[0], info[1], (String) info[2]);
					}
				}

			// now apply any overrides
    			for( int i=0; i<menuListeners.size(); i++ ) {
				Object info[] = menuListeners.get(i);
				menuBar.addMenuListener((String) info[0], info[1], (String) info[2]); //$NON-NLS-1$ //$NON-NLS-2$
			}
		} catch (Throwable t ) {
			// TODO log this
			t.printStackTrace();
			new ErrorDialog(shell, Messages.getString("Spoon.Exception.ErrorReadingXULFile.Title"), Messages.getString("Spoon.Exception.ErrorReadingXULFile.Message", XUL_FILE_MENU_PROPERTIES), new Exception(t));
		}
	}

    public void undoAction() {
		undoAction(getActiveUndoInterface());
	}

    public void redoAction() {
		redoAction(getActiveUndoInterface());
	}

    public void editUnselectAll( ) {
		editUnselectAll(getActiveTransformation());
	}

    public void editSelectAll( ) {
		editSelectAll(getActiveTransformation());
	}

    public void copySteps() {
		TransMeta transMeta = getActiveTransformation();
		copySelected(transMeta, transMeta.getSelectedSteps(), transMeta.getSelectedNotes());
	}

	public void addMenu()
	{
		
        if (menuBar!=null && !menuBar.isDisposed())
        {
        	menuBar.dispose();
        }
        
		try
		{
			menuBar = XulHelper.createMenuBar(XUL_FILE_MENUBAR, shell,new XulMessages());
			List<String> ids = new ArrayList<String>();
			ids.add("trans-class");
			ids.add("trans-class-new");
			ids.add("job-class");
			ids.add("trans-hop-class");
			ids.add("database-class");
			ids.add("partition-schema-class");
			ids.add("cluster-schema-class");
			ids.add("slave-cluster-class");
			ids.add("trans-inst");
			ids.add("job-inst");
			ids.add("step-plugin");
			ids.add("database-inst");
			ids.add("step-inst");
			ids.add("job-entry-copy-inst");
			ids.add("trans-hop-inst");
			ids.add("partition-schema-inst");
			ids.add("cluster-schema-inst");
			ids.add("slave-server-inst");
		
			this.menuMap = XulHelper.createPopupMenus(XUL_FILE_MENUS, shell,new XulMessages(),ids);// createMenuBarFromXul();
		} catch (Throwable t)
		{
			// TODO log this
			t.printStackTrace();
			new ErrorDialog(shell, Messages.getString("Spoon.Exception.ErrorReadingXULFile.Title"), Messages
					.getString("Spoon.Exception.ErrorReadingXULFile.Message", XUL_FILE_MENUS), new Exception(
					t));
		}

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
		TransGraph.editProperties(getActiveTransformation(), this, rep);
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
    	
		XulMenuItem sep = menuBar.getSeparatorById("file-last-separator"); //$NON-NLS-1$
		XulMenu msFile = null;
    		if( sep != null ) {
			msFile = sep.getMenu(); //$NON-NLS-1$
		}
    		if( msFile == null || sep == null ) {
    			// The menu system has been altered and we can't display the last used files
			return;
		}
		int idx = msFile.indexOf(sep);
		int max = msFile.getItemCount();
		// Remove everything until end...
		for (int i = max - 1; i > idx; i--)
		{
			XulMenuItem mi = msFile.getItem(i);
			msFile.remove(mi);
			mi.dispose();
		}

		// Previously loaded files...
		List<LastUsedFile> lastUsedFiles = props.getLastUsedFiles();
		for (int i = 0; i < lastUsedFiles.size(); i++)
		{
			final LastUsedFile lastUsedFile = lastUsedFiles.get(i);

			char chr = (char) ('1' + i);
			String accessKey = "ctrl-" + chr; //$NON-NLS-1$
			String accessText = "CTRL-" + chr; //$NON-NLS-1$
			String text = lastUsedFile.toString();
			String id = "last-file-" + i; //$NON-NLS-1$

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

			menuBar.addMenuListener(id, this, "lastFileSelect"); //$NON-NLS-1$
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

		try {
			toolbar = XulHelper.createToolbar(XUL_FILE_MENUBAR,shell,this,new XulMessages());
			
		} catch (Throwable t ) {
			log.logError(toString(), Const.getStackTracker(t));
			new ErrorDialog(shell, Messages.getString("Spoon.Exception.ErrorReadingXULFile.Title"), Messages.getString("Spoon.Exception.ErrorReadingXULFile.Message", XUL_FILE_MENUBAR), new Exception(t));
		}
	}

	private static final String STRING_SPOON_MAIN_TREE = Messages.getString("Spoon.MainTree.Label");
    private static final String STRING_SPOON_CORE_OBJECTS_TREE= Messages.getString("Spoon.CoreObjectsTree.Label");

	private void addTree()
	{
		Composite composite = new Composite(sashform, SWT.NONE);
		props.setLook(composite);

		FillLayout fillLayout = new FillLayout();
		fillLayout.spacing = Const.MARGIN;
		fillLayout.marginHeight = Const.MARGIN;
		fillLayout.marginWidth = Const.MARGIN;
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
				if (idx >= 0)
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
			header += item.getHeaderHeight();
		}

		for (int i = 0; i < items.length; i++)
		{
			ExpandItem item = items[i];
			item.setHeight(bounds.height - header - 15);
		}
	}

	public void addCoreObjectsExpandBar()
	{
		Composite composite = new Composite(mainExpandBar, SWT.BORDER);
		FormLayout formLayout = new FormLayout();
		formLayout.marginLeft = 20;
		formLayout.marginTop = Const.MARGIN;
		formLayout.marginBottom = Const.MARGIN;
		composite.setLayout(formLayout);

		expandBar = new ExpandBar(composite, SWT.V_SCROLL);
		expandBar.setBackground(GUIResource.getInstance().getColorBackground());
		expandBar.setSpacing(0);

		FormData expandData = new FormData();
		expandData.left = new FormAttachment(0, 0);
		expandData.right = new FormAttachment(100, 0);
		expandData.top = new FormAttachment(0, 0);
		expandData.bottom = new FormAttachment(100, 0);
		expandBar.setLayoutData(expandData);

		// collapse the other expandbar items if one gets expanded...
		expandBar.addExpandListener(new ExpandAdapter()
		{
			public void itemExpanded(ExpandEvent event)
			{
				ExpandItem item = (ExpandItem) event.item;
				int idx = expandBar.indexOf(item);
				if (idx >= 0)
				{
                        for (int i=0;i<expandBar.getItemCount();i++) if (i!=idx) expandBar.getItem(i).setExpanded(false);
					ScrolledComposite scrolledComposite = (ScrolledComposite) item.getControl();
					Composite composite = (Composite) scrolledComposite.getContent();
					composite.setFocus();
				}
			}
		});
		expandBar.addListener(SWT.Resize, new Listener()
		{
			public void handleEvent(Event event)
			{
				resizeExpandBar(expandBar);
			}
		});

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

		Rectangle rect = new Rectangle(0, 0, point.x + 100 - offset, point.y + 11);
		Rectangle iconBounds = icon.getBounds();

		final Image image = new Image(display, rect.width, rect.height);
		GC gc = new GC(image);
		if (props.isBrandingActive())
		{
			GUIResource.getInstance().drawPentahoGradient(display, gc, rect, false);
		}
		gc.drawImage(icon, 0, 2);
		gc.setForeground(GUIResource.getInstance().getColorBlack());
		// gc.setBackground(expandItem.getParent().getBackground());
		gc.setFont(GUIResource.getInstance().getFontBold());
		gc.drawText(string, iconBounds.width + 5, (iconBounds.height - point.y) / 2 + 2, true);
		expandItem.setImage(ImageUtil.makeImageTransparent(display, image, new RGB(255, 255, 255)));
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
		if (stepHistoryChanged || mainExpandBar.getItemCount() < 3)
		{
			boolean showTrans = getActiveTransformation() != null;

			// See if we need to bother.
			if (2 < mainExpandBar.getItemCount() && mainExpandBar.getItemCount() >= 3 - (showTrans ? 0 : 1))
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
				GridLayout layout = new GridLayout();
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
					if (stepPlugin != null)
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
			if (mainExpandBar.getItemCount() > 3 - (showTrans ? 0 : 1))
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

			stepHistoryChanged = false;
		}
	}

	private boolean previousShowTrans;
	private boolean previousShowJob;

	public void refreshCoreObjects()
	{
		refreshCoreObjectsHistory();

		boolean showTrans = getActiveTransformation() != null;
		boolean showJob = getActiveJob() != null;

		if (showTrans == previousShowTrans && showJob == previousShowJob)
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
			// ////////////////////////////////////////////////////////////////////////////////////////////////
			// TRANSFORMATIONS
			// ////////////////////////////////////////////////////////////////////////////////////////////////

			String locale = LanguageChoice.getInstance().getDefaultLocale().toString().toLowerCase();

			StepLoader steploader = StepLoader.getInstance();
			StepPlugin basesteps[] = steploader.getStepsWithType(StepPlugin.TYPE_ALL);
			String basecat[] = steploader.getCategories(StepPlugin.TYPE_ALL, locale);

			for (int i = 0; i < basecat.length; i++)
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

				for (int j = 0; j < basesteps.length; j++)
				{
					if (basesteps[j].getCategory(locale).equalsIgnoreCase(basecat[i]))
					{
                        final Image stepimg = (Image)GUIResource.getInstance().getImagesStepsSmall().get(basesteps[j].getID()[0]);
						String pluginName = basesteps[j].getDescription(locale);
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
				item.setHeight(scrolledComposite.computeSize(SWT.DEFAULT, SWT.DEFAULT).y + 10);
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

			// ////////////////////////////////////////////////////////////////////////////////////////////////
			// JOBS
			// ////////////////////////////////////////////////////////////////////////////////////////////////

			// First add a few "Special entries: Start, Dummy, OK, ERROR
			//
			JobEntryCopy startEntry = JobMeta.createStartEntry();
			JobEntryCopy dummyEntry = JobMeta.createDummyEntry();

			String specialText[] = new String[] { startEntry.getName(), dummyEntry.getName(), };
            String specialTooltip[] = new String[] { startEntry.getDescription(), dummyEntry.getDescription(),};
            Image  specialImage[]= new Image[] { GUIResource.getInstance().getImageStartSmall(), GUIResource.getInstance().getImageDummySmall() };

			for (int i = 0; i < specialText.length; i++)
			{
                addExpandBarItemLine(item, composite, specialImage[i], specialText[i], specialTooltip[i], false, specialText[i]);
			}

			JobEntryLoader jobEntryLoader = JobEntryLoader.getInstance();
			JobPlugin baseEntries[] = jobEntryLoader.getJobEntriesWithType(JobPlugin.TYPE_ALL);
			for (int i = 0; i < baseEntries.length; i++)
			{
				if (!baseEntries[i].getID().equals("SPECIAL"))
				{
                    final Image stepimg = GUIResource.getInstance().getImagesJobentriesSmall().get(baseEntries[i].getID());
					String pluginName = baseEntries[i].getDescription();
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
			item.setHeight(scrolledComposite.computeSize(SWT.DEFAULT, SWT.DEFAULT).y + 10);
			item.setExpanded(true);

			if (mainExpandBar.getItemCount() > 2)
			{
				ExpandItem historyItem = mainExpandBar.getItem(2);
				historyItem.setExpanded(false);
				historyItem.getControl().dispose();
				historyItem.dispose();
			}
		}

		mainExpandBar.redraw();

		previousShowTrans = showTrans;
		previousShowJob = showJob;
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
		fdCanvas.left = new FormAttachment(0, 0);
		fdCanvas.right = new FormAttachment(0, image.getBounds().width);
		fdCanvas.top = new FormAttachment(0, 0);
		fdCanvas.bottom = new FormAttachment(0, image.getBounds().height);
		canvas.setLayoutData(fdCanvas);

		Label name = new Label(lineComposite, SWT.LEFT);
		props.setLook(name);
		name.setText(pluginName);
		name.setToolTipText(pluginDescription);
        if (isPlugin) name.setFont(GUIResource.getInstance().getFontBold());
		FormData fdName = new FormData();
		fdName.left = new FormAttachment(canvas, Const.MARGIN);
		fdName.right = new FormAttachment(100, 0);
		fdName.top = new FormAttachment(canvas, 0, SWT.CENTER);
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
				int type = 0;
				String data = null;

				if (plugin instanceof StepPlugin)
				{
					StepPlugin stepPlugin = (StepPlugin) plugin;
					type = DragAndDropContainer.TYPE_BASE_STEP_TYPE;
					data = stepPlugin.getDescription(); // Step type description
				}
				if (plugin instanceof JobPlugin)
				{
					JobPlugin jobPlugin = (JobPlugin) plugin;
					type = DragAndDropContainer.TYPE_BASE_JOB_ENTRY;
                        data = jobPlugin.getDescription(); // Job Entry type description
				}
				if (plugin instanceof String)
				{
					type = DragAndDropContainer.TYPE_BASE_JOB_ENTRY;
					data = (String) plugin; // Job Entry type description
				}

				if (data != null)
				{
					event.data = new DragAndDropContainer(type, data);
                    }
                    else
				{
					event.doit = false;
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
		return delegates.tree.getTreeObjects(tree, selectionTree, coreObjectsTree);
	}


	private void addDragSourceToTree(final Tree tree)
	{
		delegates.tree.addDragSourceToTree(tree, selectionTree, coreObjectsTree);

	}

	/**
     * If you click in the tree, you might want to show the corresponding window.
	 */
	public void showSelection()
	{
		TreeSelection[] objects = getTreeObjects(selectionTree);
        if (objects.length!=1) return; // not yet supported, we can do this later when the OSX bug goes away

		TreeSelection object = objects[0];

		final Object selection = object.getSelection();
		final Object parent = object.getParent();

		TransMeta transMeta = null;
        if (selection instanceof TransMeta) transMeta = (TransMeta) selection;
        if (parent instanceof TransMeta) transMeta = (TransMeta) parent;
		if (transMeta != null)
		{
			// Search the corresponding TransGraph tab
            TabItem tabItem = delegates.tabs.findTabItem(makeTransGraphTabName(transMeta), TabMapEntry.OBJECT_TYPE_TRANSFORMATION_GRAPH);
			if (tabItem != null)
			{
				int current = tabfolder.getSelectedIndex();
				int desired = tabfolder.indexOf(tabItem);
                if (current!=desired) tabfolder.setSelected(desired);
				transMeta.setInternalKettleVariables();
				if (getCoreObjectsState() != STATE_CORE_OBJECTS_SPOON)
				{
                	// Switch the core objects in the lower left corner to the spoon trans types
					refreshCoreObjects();
				}
			}
		}

		JobMeta jobMeta = null;
        if (selection instanceof JobMeta) jobMeta = (JobMeta) selection;
        if (parent instanceof JobMeta) jobMeta = (JobMeta) parent;
		if (jobMeta != null)
		{
			// Search the corresponding TransGraph tab
            TabItem tabItem = delegates.tabs.findTabItem(delegates.tabs.makeJobGraphTabName(jobMeta), TabMapEntry.OBJECT_TYPE_JOB_GRAPH);
			if (tabItem != null)
			{
				int current = tabfolder.getSelectedIndex();
				int desired = tabfolder.indexOf(tabItem);
                if (current!=desired) tabfolder.setSelected(desired);
				jobMeta.setInternalKettleVariables();
				if (getCoreObjectsState() != STATE_CORE_OBJECTS_CHEF)
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
		newPartitioningSchema((TransMeta) selectionObjectParent);
	}

    public void newClusteringSchema() {
		newClusteringSchema((TransMeta) selectionObjectParent);
	}

    public void newSlaveServer() {
		newSlaveServer((TransMeta) selectionObjectParent);
	}

    public void editTransformationPropertiesPopup() {
		TransGraph.editProperties((TransMeta) selectionObject, this, rep);
	}

    public void addTransLog() {
		addTransLog((TransMeta) selectionObject);
	}

    public void addTransHistory() {
		addTransHistory((TransMeta) selectionObject, true);
	}

    public Map<String, Menu> getMenuMap() {
		return menuMap;
	}

    public void editJobProperties( String id ) {
    		if( "job-settings".equals( id ) ) {
			JobGraph.editProperties(getActiveJob(), this, rep);
    		}
    		else if( "job-inst-settings".equals( id ) ) {
			JobGraph.editProperties((JobMeta) selectionObject, this, rep);
		}
	}

	public void addJobLog()	{
		delegates.jobs.addJobLog((JobMeta) selectionObject);
	}

    public void addJobHistory() {
		addJobHistory((JobMeta) selectionObject, true);
	}

    public void newStep() {
		newStep(getActiveTransformation());
	}

	public void editConnection() {
		final DatabaseMeta databaseMeta = (DatabaseMeta) selectionObject;
		delegates.db.editConnection(databaseMeta);
	}

	public void dupeConnection() {
		final DatabaseMeta databaseMeta = (DatabaseMeta) selectionObject;
		final HasDatabasesInterface hasDatabasesInterface = (HasDatabasesInterface) selectionObjectParent;
		delegates.db.dupeConnection(hasDatabasesInterface, databaseMeta);
	}

	public void clipConnection() {
		final DatabaseMeta databaseMeta = (DatabaseMeta) selectionObject;
		delegates.db.clipConnection(databaseMeta);
	}

	public void delConnection() {
		final DatabaseMeta databaseMeta = (DatabaseMeta) selectionObject;
		final HasDatabasesInterface hasDatabasesInterface = (HasDatabasesInterface) selectionObjectParent;
		delegates.db.delConnection(hasDatabasesInterface, databaseMeta);
	}

	public void sqlConnection(){
		final DatabaseMeta databaseMeta = (DatabaseMeta) selectionObject;
		delegates.db.sqlConnection(databaseMeta);
	}

	public void clearDBCache(String id){
		if ("database-class-clear-cache".equals(id)){
			delegates.db.clearDBCache((DatabaseMeta) null);
		}
		if ("database-inst-clear-cache".equals(id)){
			final DatabaseMeta databaseMeta = (DatabaseMeta) selectionObject;
			delegates.db.clearDBCache(databaseMeta);
		}
	}

	public void exploreDB() {
		final DatabaseMeta databaseMeta = (DatabaseMeta) selectionObject;
		delegates.db.exploreDB(databaseMeta);
	}

	public void editStep() {
		final TransMeta transMeta = (TransMeta) selectionObjectParent;
		final StepMeta stepMeta = (StepMeta) selectionObject;
		delegates.steps.editStep(transMeta, stepMeta);
	}

	public void dupeStep()	{
		final TransMeta transMeta = (TransMeta) selectionObjectParent;
		final StepMeta stepMeta = (StepMeta) selectionObject;
		delegates.steps.dupeStep(transMeta, stepMeta);
	}

	public void delStep() {
		final TransMeta transMeta = (TransMeta) selectionObjectParent;
		final StepMeta stepMeta = (StepMeta) selectionObject;
		delegates.steps.delStep(transMeta, stepMeta);
	}

    public void shareObject( String id ) {
    		if( "database-inst-share".equals( id ) ) {
			final DatabaseMeta databaseMeta = (DatabaseMeta) selectionObject;
			shareObject(databaseMeta);
		}
    		if( "step-inst-share".equals( id ) ) {
			final StepMeta stepMeta = (StepMeta) selectionObject;
			shareObject(stepMeta);
		}
    		if( "partition-schema-inst-share".equals( id ) ) {
			final PartitionSchema partitionSchema = (PartitionSchema) selectionObject;
			shareObject(partitionSchema);
		}
    		if( "cluster-schema-inst-share".equals( id ) ) {
			final ClusterSchema clusterSchema = (ClusterSchema) selectionObject;
			shareObject(clusterSchema);
		}
    		if( "slave-server-inst-share".equals( id ) ) {
			final SlaveServer slaveServer = (SlaveServer) selectionObject;
			shareObject(slaveServer);
		}
	}

    public void editJobEntry() {
		final JobMeta jobMeta = (JobMeta) selectionObjectParent;
		final JobEntryCopy jobEntry = (JobEntryCopy) selectionObject;
		editJobEntry(jobMeta, jobEntry);
	}

	public void dupeJobEntry() {
		final JobMeta jobMeta = (JobMeta) selectionObjectParent;
		final JobEntryCopy jobEntry = (JobEntryCopy) selectionObject;
		delegates.jobs.dupeJobEntry(jobMeta, jobEntry);
	}

    public void deleteJobEntryCopies() {
		final JobMeta jobMeta = (JobMeta) selectionObjectParent;
		final JobEntryCopy jobEntry = (JobEntryCopy) selectionObject;
		deleteJobEntryCopies(jobMeta, jobEntry);
	}

    public void editHop() {
		final TransMeta transMeta = (TransMeta) selectionObjectParent;
		final TransHopMeta transHopMeta = (TransHopMeta) selectionObject;
		editHop(transMeta, transHopMeta);
	}

    public void delHop() {
		final TransMeta transMeta = (TransMeta) selectionObjectParent;
		final TransHopMeta transHopMeta = (TransHopMeta) selectionObject;
		delHop(transMeta, transHopMeta);
	}

    public void editPartitionSchema() {
		final TransMeta transMeta = (TransMeta) selectionObjectParent;
		final PartitionSchema partitionSchema = (PartitionSchema) selectionObject;
		editPartitionSchema(transMeta, partitionSchema);
	}

    public void delPartitionSchema() {
		final TransMeta transMeta = (TransMeta) selectionObjectParent;
		final PartitionSchema partitionSchema = (PartitionSchema) selectionObject;
		delPartitionSchema(transMeta, partitionSchema);
	}

    public void editClusterSchema() {
		final TransMeta transMeta = (TransMeta) selectionObjectParent;
		final ClusterSchema clusterSchema = (ClusterSchema) selectionObject;
		editClusterSchema(transMeta, clusterSchema);
	}

    public void delClusterSchema() {
		final TransMeta transMeta = (TransMeta) selectionObjectParent;
		final ClusterSchema clusterSchema = (ClusterSchema) selectionObject;
		delClusterSchema(transMeta, clusterSchema);
	}

    public void monitorClusterSchema() {
		final ClusterSchema clusterSchema = (ClusterSchema) selectionObject;
		monitorClusterSchema(clusterSchema);
	}

    public void editSlaveServer() {
		final SlaveServer slaveServer = (SlaveServer) selectionObject;
		editSlaveServer(slaveServer);
	}

    public void delSlaveServer() {
		final TransMeta transMeta = (TransMeta) selectionObjectParent;
		final SlaveServer slaveServer = (SlaveServer) selectionObject;
		delSlaveServer(transMeta, slaveServer);
	}

    public void addSpoonSlave() {
		final SlaveServer slaveServer = (SlaveServer) selectionObject;
		addSpoonSlave(slaveServer);
	}

	private void setMenu()
	{

		TreeSelection[] objects = getTreeObjects(selectionTree);
        if (objects.length!=1) return; // not yet supported, we can do this later when the OSX bug goes away

		TreeSelection object = objects[0];

		selectionObject = object.getSelection();
		Object selection = selectionObject;
		selectionObjectParent = object.getParent();
		// final Object grandParent = object.getGrandParent();

		// Not clicked on a real object: returns a class
		if (selection instanceof Class)
		{
			if (selection.equals(TransMeta.class))
			{
				// New
				spoonMenu = (Menu) menuMap.get("trans-class");
			}
			if (selection.equals(JobMeta.class))
			{
				// New
				spoonMenu = (Menu) menuMap.get("job-class");
			}
			if (selection.equals(TransHopMeta.class))
			{
				// New
				spoonMenu = (Menu) menuMap.get("trans-hop-class");
			}
			if (selection.equals(DatabaseMeta.class))
			{
				spoonMenu = (Menu) menuMap.get("database-class");
			}
			if (selection.equals(PartitionSchema.class))
			{
				// New
				spoonMenu = (Menu) menuMap.get("partition-schema-class");
			}
			if (selection.equals(ClusterSchema.class))
			{
				spoonMenu = (Menu) menuMap.get("cluster-schema-class");
			}
			if (selection.equals(SlaveServer.class))
			{
				spoonMenu = (Menu) menuMap.get("slave-cluster-class");
			}
        }
        else
		{

			if (selection instanceof TransMeta)
			{
				spoonMenu = (Menu) menuMap.get("trans-inst");
			}
			if (selection instanceof JobMeta)
			{
				spoonMenu = (Menu) menuMap.get("job-inst");
			}
			if (selection instanceof StepPlugin)
			{
				spoonMenu = (Menu) menuMap.get("step-plugin");
			}

			if (selection instanceof DatabaseMeta)
			{
				spoonMenu = (PopupMenu) menuMap.get("database-inst");
                // disable for now if the connection is an SAP R/3 type of database...
				XulMenuItem item = ((XulPopupMenu) spoonMenu).getMenuItemById("database-inst-explore");
				if( item != null ) {
					final DatabaseMeta databaseMeta = (DatabaseMeta) selection;
	                if (databaseMeta.getDatabaseType()==DatabaseMeta.TYPE_DATABASE_SAPR3) item.setEnabled(false);
				}
				item = ((XulPopupMenu) spoonMenu).getMenuItemById("database-inst-clear-cache");
				if( item != null ) {
					final DatabaseMeta databaseMeta = (DatabaseMeta) selectionObject;
					item.setText(Messages.getString("Spoon.Menu.Popup.CONNECTIONS.ClearDBCache")+databaseMeta.getName());//Clear DB Cache of 
				}

			}
			if (selection instanceof StepMeta)
			{
				spoonMenu = (Menu) menuMap.get("step-inst");
			}
			if (selection instanceof JobEntryCopy)
			{
				spoonMenu = (Menu) menuMap.get("job-entry-copy-inst");
			}
			if (selection instanceof TransHopMeta)
			{
				spoonMenu = (Menu) menuMap.get("trans-hop-inst");
			}
			if (selection instanceof PartitionSchema)
			{
				spoonMenu = (Menu) menuMap.get("partition-schema-inst");
			}
			if (selection instanceof ClusterSchema)
			{
				spoonMenu = (Menu) menuMap.get("cluster-schema-inst");
			}
			if (selection instanceof SlaveServer)
			{
				spoonMenu = (Menu) menuMap.get("slave-server-inst");
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

		final Object selection = object.getSelection();
		final Object parent = object.getParent();

		if (selection instanceof Class)
		{
			if (selection.equals(TransMeta.class)) newTransFile();
			if (selection.equals(JobMeta.class)) newJobFile();
			if (selection.equals(TransHopMeta.class)) newHop((TransMeta) parent);
			if (selection.equals(DatabaseMeta.class)) delegates.db.newConnection();
			if (selection.equals(PartitionSchema.class)) newPartitioningSchema((TransMeta) parent);
			if (selection.equals(ClusterSchema.class)) newClusteringSchema((TransMeta) parent);
			if (selection.equals(SlaveServer.class)) newSlaveServer((TransMeta) parent);
		}
                else
		{
			if (selection instanceof TransMeta) TransGraph.editProperties((TransMeta) selection, this, rep);
			if (selection instanceof JobMeta)JobGraph.editProperties((JobMeta) selection, this, rep);
			if (selection instanceof StepPlugin)newStep(getActiveTransformation());
			if (selection instanceof DatabaseMeta)delegates.db.editConnection((DatabaseMeta) selection);
			if (selection instanceof StepMeta) delegates.steps.editStep((TransMeta) parent, (StepMeta) selection);
			if (selection instanceof JobEntryCopy) editJobEntry((JobMeta) parent, (JobEntryCopy) selection);
			if (selection instanceof TransHopMeta) editHop((TransMeta) parent, (TransHopMeta) selection);
			if (selection instanceof PartitionSchema) editPartitionSchema((HasDatabasesInterface) parent, (PartitionSchema) selection);
			if (selection instanceof ClusterSchema)	editClusterSchema((TransMeta) parent, (ClusterSchema) selection);
			if (selection instanceof SlaveServer) editSlaveServer((SlaveServer) selection);
		}
	}

	protected void monitorClusterSchema(ClusterSchema clusterSchema)
	{
		for (int i = 0; i < clusterSchema.getSlaveServers().size(); i++)
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
		if (tabComp != null)
		{
			tabComp.dispose();
		}

		tabComp = new Composite(sashform, SWT.BORDER);
		props.setLook(tabComp);
		tabComp.setLayout(new FillLayout());

		tabfolder = new TabSet(tabComp);
		tabfolder.setChangedFont(GUIResource.getInstance().getFontBold());
		tabfolder.setUnchangedFont(GUIResource.getInstance().getFontGraph());
		props.setLook(tabfolder.getSwtTabset(), Props.WIDGET_STYLE_TAB);

		tabfolder.addKeyListener(defKeys);
		tabfolder.addKeyListener(modKeys);

		// tabfolder.setSelection(0);

		sashform.addKeyListener(defKeys);
		sashform.addKeyListener(modKeys);

		int weights[] = props.getSashWeights();
		sashform.setWeights(weights);
		sashform.setVisible(true);

		tabfolder.addListener(this);

	}

	public void tabDeselected(TabItem item)	{

	}

	public boolean tabClose(TabItem item)	{
		return delegates.tabs.tabClose(item);
	}

	public TabSet getTabSet(){
		return tabfolder;
	}

	public void tabSelected(TabItem item)	{
		delegates.tabs.tabSelected(item);
	}

	public String getRepositoryName()
	{
        if (rep==null) return null;
		return rep.getRepositoryInfo().getName();
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

			// Point min = new Point(loc.x, loc.y);
			Point min = new Point(99999999, 99999999);

			// Load the steps...
			for (int i = 0; i < nr; i++)
			{
				Node stepnode = XMLHandler.getSubNodeByNr(stepsnode, "step", i);
				steps[i] = new StepMeta(stepnode, transMeta.getDatabases(), transMeta.getCounters());

				if (loc != null)
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

			for (int i = 0; i < nr; i++)
			{
				Node hopnode = XMLHandler.getSubNodeByNr(hopsnode, "hop", i);
				hops[i] = new TransHopMeta(hopnode, alSteps);
			}

			// What's the difference between loc and min?
			// This is the offset:
			Point offset = new Point(loc.x - min.x, loc.y - min.y);

			// Undo/redo object positions...
			int position[] = new int[steps.length];

			for (int i = 0; i < steps.length; i++)
			{
				Point p = steps[i].getLocation();
				String name = steps[i].getName();

				steps[i].setLocation(p.x + offset.x, p.y + offset.y);
				steps[i].setDraw(true);

				// Check the name, find alternative...
				steps[i].setName(transMeta.getAlternativeStepname(name));
				transMeta.addStep(steps[i]);
				position[i] = transMeta.indexOfStep(steps[i]);
				steps[i].setSelected(true);
			}

			// Add the hops too...
			for (int i = 0; i < hops.length; i++)
			{
				transMeta.addTransHop(hops[i]);
			}

			// Load the notes...
			Node notesnode = XMLHandler.getSubNode(transnode, "notepads");
			nr = XMLHandler.countNodes(notesnode, "notepad");
            log.logDebug(toString(), Messages.getString("Spoon.Log.FoundNotepads",""+nr));//"I found "+nr+" notepads to paste."
			NotePadMeta notes[] = new NotePadMeta[nr];

			for (int i = 0; i < notes.length; i++)
			{
				Node notenode = XMLHandler.getSubNodeByNr(notesnode, "notepad", i);
				notes[i] = new NotePadMeta(notenode);
				Point p = notes[i].getLocation();
				notes[i].setLocation(p.x + offset.x, p.y + offset.y);
				transMeta.addNote(notes[i]);
				notes[i].setSelected(true);
			}

			// Set the source and target steps ...
			for (int i = 0; i < steps.length; i++)
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
		xml += "<transformation>" + Const.CR;
		xml += " <steps>" + Const.CR;

		for (int i = 0; i < stepMeta.length; i++)
		{
			xml += stepMeta[i].getXML();
		}

		xml += "    </steps>" + Const.CR;

		// 
		// Also check for the hops in between the selected steps...
		//

		xml += "<order>" + Const.CR;
		if (stepMeta != null)
			for (int i = 0; i < stepMeta.length; i++)
			{
				for (int j = 0; j < stepMeta.length; j++)
				{
					if (i != j)
					{
						TransHopMeta hop = transMeta.findTransHop(stepMeta[i], stepMeta[j], true);
						if (hop != null) // Ok, we found one...
						{
							xml += hop.getXML() + Const.CR;
						}
					}
				}
			}
		xml += "  </order>" + Const.CR;

		xml += "  <notepads>" + Const.CR;
		if (notePadMeta != null)
			for (int i = 0; i < notePadMeta.length; i++)
			{
				xml += notePadMeta[i].getXML();
			}
		xml += "   </notepads>" + Const.CR;

		xml += " </transformation>" + Const.CR;

		toClipboard(xml);
	}

	public void editHop(TransMeta transMeta, TransHopMeta transHopMeta)
	{
		// Backup situation BEFORE edit:
		String name = transHopMeta.toString();
		TransHopMeta before = (TransHopMeta) transHopMeta.clone();

		TransHopDialog hd = new TransHopDialog(shell, SWT.NONE, transHopMeta, transMeta);
		if (hd.open() != null)
		{
			// Backup situation for redo/undo:
			TransHopMeta after = (TransHopMeta) transHopMeta.clone();
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
		if (hd.open() != null)
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
		if (transMeta.findTransHop(newHop.getFromStep(), newHop.getToStep()) != null)
		{
			MessageBox mb = new MessageBox(shell, SWT.OK | SWT.ICON_ERROR);
            mb.setMessage(Messages.getString("Spoon.Dialog.HopExists.Message"));//"This hop already exists!"
			mb.setText(Messages.getString("Spoon.Dialog.HopExists.Title"));// Error!
			mb.open();
			ok = false;
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
			mb.setMessage(Messages.getString("SpoonGraph.Dialog.HopCausesLoop.Message")); //$NON-NLS-1$
			mb.setText(Messages.getString("SpoonGraph.Dialog.HopCausesLoop.Title")); //$NON-NLS-1$
			mb.open();
			ok = false;
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
		if (nrNextSteps == 2)
		{
			boolean distributes = false;

			if (props.showCopyOrDistributeWarning())
			{
                MessageDialogWithToggle md = new MessageDialogWithToggle(shell, 
						Messages.getString("System.Warning"),// "Warning!"
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

				distributes = (idx & 0xFF) == 1;
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

	public void openRepository()
	{
		int perms[] = new int[] { PermissionMeta.TYPE_PERMISSION_TRANSFORMATION };
		RepositoriesDialog rd = new RepositoriesDialog(display, perms, APP_NAME);
		rd.getShell().setImage(GUIResource.getInstance().getImageSpoon());
		if (rd.open())
		{
			// Close previous repository...

			if (rep != null)
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
				rep = null;
                new ErrorDialog(shell, Messages.getString("Spoon.Dialog.ErrorConnectingRepository.Title"), Messages.getString("Spoon.Dialog.ErrorConnectingRepository.Message",Const.CR), ke); //$NON-NLS-1$ //$NON-NLS-2$
			}

			TransMeta transMetas[] = getLoadedTransformations();
			for (int t = 0; t < transMetas.length; t++)
			{
				TransMeta transMeta = transMetas[t];

				for (int i = 0; i < transMeta.nrDatabases(); i++)
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
				for (int i = 0; i < oldDatabases.size(); i++)
				{
					DatabaseMeta oldDatabase = oldDatabases.get(i);
                    DatabaseMeta newDatabase = DatabaseMeta.findDatabase(transMeta.getDatabases(), oldDatabase.getName());

					// If it exists, change the settings...
					if (newDatabase != null)
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
				if (redi != null)
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
		if (rep != null)
		{
            RepositoryExplorerDialog erd = new RepositoryExplorerDialog(shell, SWT.NONE, rep, rep.getUserInfo());
			String objname = erd.open();
			if (objname != null)
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
						MessageBox mb = new MessageBox(shell, SWT.OK | SWT.ICON_ERROR);
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
                        delegates.jobs.addJobGraph(jobMeta);
						refreshTree();
						refreshGraph();
                    }
                    catch(Exception e)
					{
						MessageBox mb = new MessageBox(shell, SWT.OK | SWT.ICON_ERROR);
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
		if (rep != null)
		{
			UserInfo userinfo = rep.getUserInfo();
			UserDialog ud = new UserDialog(shell, SWT.NONE, rep, userinfo);
			UserInfo ui = ud.open();
			if (!userinfo.isReadonly())
			{
				if (ui != null)
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
		openFile(false);
	}

    public void importFile() {
		openFile(true);
	}

	public void openFile(boolean importfile)
	{
		if (rep == null || importfile) // Load from XML
		{
			FileDialog dialog = new FileDialog(shell, SWT.OPEN);
			dialog.setFilterExtensions(Const.STRING_TRANS_AND_JOB_FILTER_EXT);
			dialog.setFilterNames(Const.getTransformationAndJobFilterNames());
			String fname = dialog.open();
			if (fname != null)
			{
				openFile(fname, importfile);
			}
        }
        else
		{
			SelectObjectDialog sod = new SelectObjectDialog(shell, rep);
			if (sod.open() != null)
			{
				String type = sod.getObjectType();
				String name = sod.getObjectName();
				RepositoryDirectory repdir = sod.getDirectory();

				// Load a transformation
				if (RepositoryObject.STRING_OBJECT_TYPE_TRANSFORMATION.equals(type))
				{
					TransLoadProgressDialog tlpd = new TransLoadProgressDialog(shell, rep, name, repdir);
					TransMeta transMeta = tlpd.open();
					if (transMeta != null)
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
					if (jobMeta != null)
					{
                        props.addLastFile(LastUsedFile.FILE_TYPE_JOB, name, repdir.getPath(), true, rep.getName());
						saveSettings();
						addMenuLast();
						delegates.jobs.addJobGraph(jobMeta);
					}
					refreshGraph();
					refreshTree();
				}
			}
		}
	}

	private String lastFileOpened = "";

	// private String lastVfsUsername="";
	// private String lastVfsPassword="";

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
			lastFileOpened = selectedFile.getName().getFriendlyURI();
			openFile(selectedFile.getName().getFriendlyURI(), false);
		}
	}

    public void addFileListener( FileListener listener, String extension, String rootNodeName ) {
		fileExtensionMap.put(extension, listener);
		fileNodeMap.put(rootNodeName, listener);
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
				String extension = fname.substring(idx + 1);
				listener = fileExtensionMap.get(extension);
			}
			// otherwise try by looking at the root node
			Document document = XMLHandler.loadXMLFile(fname);
			Node root = document.getFirstChild();
        	if( listener == null ) {
				listener = fileNodeMap.get(root.getNodeName());
			}

        	if( listener != null ) {
				loaded = listener.open(root, fname, importfile);
			}
			if (!loaded)
			{
				// Give error back
				MessageBox mb = new MessageBox(shell, SWT.OK | SWT.ICON_ERROR);
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
		if (enterSelectionDialog.open() != null)
		{
			switch (enterSelectionDialog.getSelectionNr())
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
		int nr = 1;
		transMeta.setName(STRING_TRANSFORMATION + " " + nr);

		// See if a transformation with the same name isn't already loaded...
		while (findTransformation(delegates.tabs.makeTransGraphTabName(transMeta)) != null)
		{
			nr++;
			transMeta.setName(STRING_TRANSFORMATION + " " + nr); // rename
		}
		addTransGraph(transMeta);
		refreshTree();
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
                new ErrorDialog(shell, Messages.getString("Spoon.Dialog.ErrorReadingSharedObjects.Title"), Messages.getString("Spoon.Dialog.ErrorReadingSharedObjects.Message", delegates.tabs.makeJobGraphTabName(jobMeta)), e);
			}

			int nr = 1;
			jobMeta.setName(STRING_JOB + " " + nr);

            // See if a transformation with the same name isn't already loaded...
            while (findJob(delegates.tabs.makeJobGraphTabName(jobMeta))!=null)
			{
				nr++;
				jobMeta.setName(STRING_JOB + " " + nr); // rename
			}

			delegates.jobs.addJobGraph(jobMeta);
			refreshTree();
        }
        catch(Exception e)
		{
            new ErrorDialog(shell, Messages.getString("Spoon.Exception.ErrorCreatingNewJob.Title"), Messages.getString("Spoon.Exception.ErrorCreatingNewJob.Message"), e);
		}
	}

	public void loadRepositoryObjects(TransMeta transMeta)
	{
		// Load common database info from active repository...
		if (rep != null)
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

		boolean exit = true;

		saveSettings();

		if (props.showExitWarning())
		{
            MessageDialogWithToggle md = new MessageDialogWithToggle(shell, 
					Messages.getString("System.Warning"),// "Warning!"
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
		List<TabMapEntry> list = delegates.tabs.getTabs();

		for (TabMapEntry mapEntry : list)
		{
			TabItemInterface itemInterface = mapEntry.getObject();

			if (!itemInterface.canBeClosed())
			{
				// Show the tab
				tabfolder.setSelected(mapEntry.getTabItem());

				// Unsaved work that needs to changes to be applied?
				//
				int reply = itemInterface.showChangedWarning();
				if (reply == SWT.YES)
				{
					exit = itemInterface.applyChanges();
                }
                else
				{
					if (reply == SWT.CANCEL)
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

		boolean saved = false;

        log.logDetailed(toString(), Messages.getString("Spoon.Log.SaveToFileOrRepository"));//"Save to file or repository..."

		if (rep != null)
		{
			saved = saveToRepository(meta);
        }
        else
		{
			if (meta.getFilename() != null)
			{
				saved = save(meta, meta.getFilename());
            }
            else
			{
				saved = saveFileAs(meta);
			}
		}

		if (saved) // all was OK
		{
			saved = meta.saveSharedObjects();
		}

		try
		{
            if (props.useDBCache() && meta instanceof TransMeta) ((TransMeta)meta).getDbCache().saveCache(log);
        }
        catch(KettleException e)
		{
            new ErrorDialog(shell, Messages.getString("Spoon.Dialog.ErrorSavingDatabaseCache.Title"), Messages.getString("Spoon.Dialog.ErrorSavingDatabaseCache.Message"), e);//"An error occured saving the database cache to disk"
		}

        delegates.tabs.renameTabs(); // filename or name of transformation might have changed.
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
		if (rep != null)
		{
			boolean answer = true;
			boolean ask = ask_name;
			while (answer && (ask || Const.isEmpty(meta.getName())))
			{
				if (!ask)
				{
					MessageBox mb = new MessageBox(shell, SWT.OK | SWT.ICON_WARNING);
                    mb.setMessage(Messages.getString("Spoon.Dialog.PromptTransformationName.Message"));//"Please give this transformation a name before saving it in the database."
                    mb.setText(Messages.getString("Spoon.Dialog.PromptTransformationName.Title"));//"Transformation has no name."
					mb.open();
				}
				ask = false;
                if( meta instanceof TransMeta ) {
					answer = TransGraph.editProperties((TransMeta) meta, this, rep);
				}
                if( meta instanceof JobMeta ) {
					answer = JobGraph.editProperties((JobMeta) meta, this, rep);
				}
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
						mb.setText(Messages.getString("Spoon.Dialog.PromptOverwriteTransformation.Title"));// "Overwrite?"
						response = mb.open();
					}

					boolean saved = false;
					if (response == SWT.YES)
					{
						shell.setCursor(cursor_hourglass);

						// Keep info on who & when this transformation was created...
						if (meta.getCreatedUser() == null || meta.getCreatedUser().equals("-"))
						{
							meta.setCreatedDate(new Date());
							meta.setCreatedUser(rep.getUserInfo().getLogin());
						}
						else
						{

							meta.setCreatedDate(meta.getCreatedDate());
							meta.setCreatedUser(meta.getCreatedUser());
						}

                        // Keep info on who & when this transformation was changed...
						meta.setModifiedDate(new Date());
						meta.setModifiedUser(rep.getUserInfo().getLogin());

						SaveProgressDialog tspd = new SaveProgressDialog(shell, rep, meta);
						if (tspd.open())
						{
							saved = true;
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
		if (rep != null)
		{
			boolean answer = true;
			boolean ask = ask_name;
			while (answer && (ask || jobMeta.getName() == null || jobMeta.getName().length() == 0))
			{
				if (!ask)
				{
					MessageBox mb = new MessageBox(shell, SWT.OK | SWT.ICON_WARNING);
					mb.setMessage(Messages.getString("Spoon.Dialog.GiveJobANameBeforeSaving.Message")); //$NON-NLS-1$
					mb.setText(Messages.getString("Spoon.Dialog.GiveJobANameBeforeSaving.Title")); //$NON-NLS-1$
					mb.open();
				}
				ask = false;
				answer = JobGraph.editProperties(jobMeta, this, rep);
			}

			if (answer && jobMeta.getName() != null && jobMeta.getName().length() > 0)
			{
				if (!rep.getUserInfo().isReadonly())
				{
					boolean saved = false;
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
						if (jobMeta.getCreatedUser() == null || jobMeta.getCreatedUser().equals("-"))
						{
							jobMeta.setCreatedDate(new Date());
							jobMeta.setCreatedUser(rep.getUserInfo().getLogin());
						}
						else
						{

							jobMeta.setCreatedDate(jobMeta.getCreatedDate());
							jobMeta.setCreatedUser(jobMeta.getCreatedUser());
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
										Messages.getString("Spoon.Dialog.JobWasStoredInTheRepository.Title"), //$NON-NLS-1$
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

							saved = true;
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
		boolean saved = false;

        log.logBasic(toString(), Messages.getString("Spoon.Log.SaveAs"));//"Save as..."

		if (rep != null)
		{
			meta.setID(-1L);
			saved = saveToRepository(meta, true);
            delegates.tabs.renameTabs();
        }
        else
		{
			saved = saveXMLFile(meta);
                       delegates.tabs.renameTabs();
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
		boolean saved = false;

		FileDialog dialog = new FileDialog(shell, SWT.SAVE);
		// dialog.setFilterPath("C:\\Projects\\kettle\\source\\");
		String extensions[] = meta.getFilterExtensions();
		dialog.setFilterExtensions(extensions);
		dialog.setFilterNames(meta.getFilterNames());
		String fname = dialog.open();
		if (fname != null)
		{
			// Is the filename ending on .ktr, .xml?
			boolean ending = false;
			for (int i = 0; i < extensions.length - 1; i++)
			{
                if (fname.endsWith(extensions[i].substring(1))) ending=true;
			}
            if (fname.endsWith(meta.getDefaultExtension())) ending=true;
			if (!ending)
			{
				fname += meta.getDefaultExtension();
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
			if (id == SWT.YES)
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
		boolean saved = false;

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
			fname = selectedFile.getName().getFriendlyURI();
		}

		String extensions[] = meta.getFilterExtensions();
		if (fname != null)
		{
			// Is the filename ending on .ktr, .xml?
			boolean ending = false;
			for (int i = 0; i < extensions.length - 1; i++)
			{
                if (fname.endsWith(extensions[i].substring(1))) ending=true;
			}
            if (fname.endsWith(meta.getDefaultExtension())) ending=true;
			if (!ending)
			{
				fname += meta.getDefaultExtension();
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
			if (id == SWT.YES)
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
			String extension = fname.substring(idx + 1);
			listener = fileExtensionMap.get(extension);
		}
    	if( listener == null ) {
			listener = fileExtensionMap.get(meta.getDefaultExtension());
		}

    	if( listener != null ) {
			saved = listener.save(meta, fname);
		}
		return saved;
	}

	public boolean saveMeta(EngineMetaInterface meta, String fname)
	{
		meta.setFilename(fname);
		if (Const.isEmpty(meta.getName()) || delegates.jobs.isDefaultJobName(meta.getName()))
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

			saved = true;

			// Handle last opened files...
			props.addLastFile(meta.getFileType(), fname, null, false, null); //$NON-NLS-1$
			saveSettings();
			addMenuLast();

            log.logDebug(toString(), Messages.getString("Spoon.Log.FileWritten")+" ["+fname+"]"); //"File written to
			meta.setFilename(fname);
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
		mess += Messages.getString("System.CompanyInfo") + Const.CR;
        mess+="         "+Messages.getString("System.ProductWebsiteUrl")+Const.CR; //(c) 2001-2004 i-Bridge bvba     www.kettle.be
		mess += Const.CR;
		mess += Const.CR;
		mess += Const.CR;
		mess += "         Build version : " + BuildVersion.getInstance().getVersion() + Const.CR;
		mess += "         Build date    : " + BuildVersion.getInstance().getBuildDate() + Const.CR;

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
		if (eod.open() != null)
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
		boolean showAll = activeTransMeta == null && activeJobMeta == null;

		// get a list of transformations from the transformation map
		List<TransMeta> transformations = delegates.trans.getTransformationList();
		Collections.sort(transformations);
		TransMeta[] transMetas = transformations.toArray(new TransMeta[transformations.size()]);

		// get a list of jobs from the job map
		List<JobMeta> jobs = delegates.jobs.getJobList();
		Collections.sort(jobs);
		JobMeta[] jobMetas = jobs.toArray(new JobMeta[jobs.size()]);

		// Refresh the content of the tree for those transformations
		//
		// First remove the old ones.
		selectionTree.removeAll();

		// Now add the data back
		//
		if (!props.isOnlyActiveFileShownInTree() || showAll || activeTransMeta != null)
		{
			TreeItem tiTrans = new TreeItem(selectionTree, SWT.NONE);
			tiTrans.setText(STRING_TRANSFORMATIONS);
			tiTrans.setImage(GUIResource.getInstance().getImageBol());

			// Set expanded if this is the only transformation shown.
			if (props.isOnlyActiveFileShownInTree())
			{
				TreeMemory.getInstance().storeExpanded(STRING_SPOON_MAIN_TREE, tiTrans, true);
			}

			for (int t = 0; t < transMetas.length; t++)
			{
				TransMeta transMeta = transMetas[t];

                if (!props.isOnlyActiveFileShownInTree() || showAll || (activeTransMeta!=null && activeTransMeta.equals(transMeta)))
				{
					// Add a tree item with the name of transformation
					//
					TreeItem tiTransName = new TreeItem(tiTrans, SWT.NONE);
					String name = delegates.tabs.makeTransGraphTabName(transMeta);
					if (Const.isEmpty(name))name = STRING_TRANS_NO_NAME;
					tiTransName.setText(name);
					tiTransName.setImage(guiResource.getImageBol());

					// Set expanded if this is the only transformation shown.
					if (props.isOnlyActiveFileShownInTree())
					{
						TreeMemory.getInstance().storeExpanded(STRING_SPOON_MAIN_TREE, tiTransName, true);
					}

					// /////////////////////////////////////////////////////
					//
					// Now add the database connections
					//
					TreeItem tiDbTitle = new TreeItem(tiTransName, SWT.NONE);
					tiDbTitle.setText(STRING_CONNECTIONS);
					tiDbTitle.setImage(guiResource.getImageConnection());

					// Draw the connections themselves below it.
					for (int i = 0; i < transMeta.nrDatabases(); i++)
					{
						DatabaseMeta databaseMeta = transMeta.getDatabase(i);
						TreeItem tiDb = new TreeItem(tiDbTitle, SWT.NONE);
						tiDb.setText(databaseMeta.getName());
                        if (databaseMeta.isShared()) tiDb.setFont(guiResource.getFontBold());
						tiDb.setImage(guiResource.getImageConnection());
					}

					// /////////////////////////////////////////////////////
					//
					// The steps
					//
					TreeItem tiStepTitle = new TreeItem(tiTransName, SWT.NONE);
					tiStepTitle.setText(STRING_STEPS);
					tiStepTitle.setImage(guiResource.getImageBol());

					// Put the steps below it.
					for (int i = 0; i < transMeta.nrSteps(); i++)
					{
						StepMeta stepMeta = transMeta.getStep(i);
						TreeItem tiStep = new TreeItem(tiStepTitle, SWT.NONE);
						tiStep.setText(stepMeta.getName());
                        if (stepMeta.isShared()) tiStep.setFont(guiResource.getFontBold());
                        if (!stepMeta.isDrawn()) tiStep.setForeground(guiResource.getColorGray());
						tiStep.setImage(guiResource.getImageBol());
					}

					// /////////////////////////////////////////////////////
					//
					// The hops
					//
					TreeItem tiHopTitle = new TreeItem(tiTransName, SWT.NONE);
					tiHopTitle.setText(STRING_HOPS);
					tiHopTitle.setImage(guiResource.getImageHop());

					// Put the steps below it.
					for (int i = 0; i < transMeta.nrTransHops(); i++)
					{
						TransHopMeta hopMeta = transMeta.getTransHop(i);
						TreeItem tiHop = new TreeItem(tiHopTitle, SWT.NONE);
						tiHop.setText(hopMeta.toString());
						tiHop.setImage(guiResource.getImageHop());
					}

					// /////////////////////////////////////////////////////
					//
					// The partitions
					//
					TreeItem tiPartitionTitle = new TreeItem(tiTransName, SWT.NONE);
					tiPartitionTitle.setText(STRING_PARTITIONS);
					tiPartitionTitle.setImage(guiResource.getImageConnection());

					// Put the steps below it.
					for (int i = 0; i < transMeta.getPartitionSchemas().size(); i++)
					{
						PartitionSchema partitionSchema = transMeta.getPartitionSchemas().get(i);
						TreeItem tiPartition = new TreeItem(tiPartitionTitle, SWT.NONE);
						tiPartition.setText(partitionSchema.getName());
						tiPartition.setImage(guiResource.getImageBol());
                        if (partitionSchema.isShared()) tiPartition.setFont(guiResource.getFontBold());
					}

					// /////////////////////////////////////////////////////
					//
					// The slaves
					//
					TreeItem tiSlaveTitle = new TreeItem(tiTransName, SWT.NONE);
					tiSlaveTitle.setText(STRING_SLAVES);
					tiSlaveTitle.setImage(guiResource.getImageBol());

					// Put the steps below it.
					for (int i = 0; i < transMeta.getSlaveServers().size(); i++)
					{
						SlaveServer slaveServer = transMeta.getSlaveServers().get(i);
						TreeItem tiSlave = new TreeItem(tiSlaveTitle, SWT.NONE);
						tiSlave.setText(slaveServer.getName());
						tiSlave.setImage(guiResource.getImageBol());
                        if (slaveServer.isShared()) tiSlave.setFont(guiResource.getFontBold());
					}

					// /////////////////////////////////////////////////////
					//
					// The clusters
					//
					TreeItem tiClusterTitle = new TreeItem(tiTransName, SWT.NONE);
					tiClusterTitle.setText(STRING_CLUSTERS);
					tiClusterTitle.setImage(guiResource.getImageBol());

					// Put the steps below it.
					for (int i = 0; i < transMeta.getClusterSchemas().size(); i++)
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

		if (!props.isOnlyActiveFileShownInTree() || showAll || activeJobMeta != null)
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
			for (int t = 0; t < jobMetas.length; t++)
			{
				JobMeta jobMeta = jobMetas[t];

                if (!props.isOnlyActiveFileShownInTree() || showAll || (activeJobMeta!=null && activeJobMeta.equals(jobMeta)))
				{
					// Add a tree item with the name of job
					//
					TreeItem tiJobName = new TreeItem(tiJobs, SWT.NONE);
                    String name = delegates.tabs.makeJobGraphTabName(jobMeta);
                    if (Const.isEmpty(name)) name = STRING_JOB_NO_NAME;
					tiJobName.setText(name);
					tiJobName.setImage(guiResource.getImageBol());

					// Set expanded if this is the only job shown.
					if (props.isOnlyActiveFileShownInTree())
					{
						TreeMemory.getInstance().storeExpanded(STRING_SPOON_MAIN_TREE, tiJobName, true);
					}

					// /////////////////////////////////////////////////////
					//
					// Now add the database connections
					//
					TreeItem tiDbTitle = new TreeItem(tiJobName, SWT.NONE);
					tiDbTitle.setText(STRING_CONNECTIONS);
					tiDbTitle.setImage(guiResource.getImageConnection());

					// Draw the connections themselves below it.
					for (int i = 0; i < jobMeta.nrDatabases(); i++)
					{
						DatabaseMeta databaseMeta = jobMeta.getDatabase(i);
						TreeItem tiDb = new TreeItem(tiDbTitle, SWT.NONE);
						tiDb.setText(databaseMeta.getName());
                        if (databaseMeta.isShared()) tiDb.setFont(guiResource.getFontBold());
						tiDb.setImage(guiResource.getImageConnection());
					}

					// /////////////////////////////////////////////////////
					//
					// The job entries
					//
					TreeItem tiJobEntriesTitle = new TreeItem(tiJobName, SWT.NONE);
					tiJobEntriesTitle.setText(STRING_JOB_ENTRIES);
					tiJobEntriesTitle.setImage(guiResource.getImageBol());

					// Put the steps below it.
					for (int i = 0; i < jobMeta.nrJobEntries(); i++)
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
                            Image image = GUIResource.getInstance().getImagesJobentriesSmall().get(jobEntry.getEntry().getID());
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

		TabMapEntry tabMapEntry = delegates.tabs.getTab(tabText);
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
		if (transHistory != null)
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

		if (ti.length == 1)
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
		if (rename && transMeta.findStep(name) != null)
		{
			int i = 2;
			String newname = name + " " + i;
			while (transMeta.findStep(newname) != null)
			{
				i++;
				newname = name + " " + i;
			}
			name = newname;
		}

		StepLoader steploader = StepLoader.getInstance();
		StepPlugin stepPlugin = null;

		String locale = LanguageChoice.getInstance().getDefaultLocale().toString().toLowerCase();

		try
		{
			stepPlugin = steploader.findStepPluginWithDescription(description, locale);
			if (stepPlugin != null)
			{
				StepMetaInterface info = BaseStep.getStepInfo(stepPlugin, steploader);

				info.setDefault();

				if (openit)
				{
					StepDialogInterface dialog = this.getStepEntryDialog(info, transMeta, name);
                    if( dialog != null ) {
						name = dialog.open();
					}
				}
				inf = new StepMeta(stepPlugin.getID()[0], name, info);

                if (name!=null) // OK pressed in the dialog: we have a step-name
				{
					String newname = name;
					StepMeta stepMeta = transMeta.findStep(newname);
					int nr = 2;
					while (stepMeta != null)
					{
						newname = name + " " + nr;
						stepMeta = transMeta.findStep(newname);
						nr++;
					}
					if (nr > 2)
					{
						inf.setName(newname);
						MessageBox mb = new MessageBox(shell, SWT.OK | SWT.ICON_INFORMATION);
                        mb.setMessage(Messages.getString("Spoon.Dialog.ChangeStepname.Message",newname));//"This stepname already exists.  Spoon changed the stepname to ["+newname+"]"
						mb.setText(Messages.getString("Spoon.Dialog.ChangeStepname.Title"));// "Info!"
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
					stepHistoryChanged = true;

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
			if (stepPlugin != null && !Const.isEmpty(filename))
			{
                // OK, in stead of a normal error message, we give back the content of the error help file... (HTML)
				try
				{
					StringBuffer content = new StringBuffer();

					FileInputStream fis = new FileInputStream(new File(filename));
					int ch = fis.read();
					while (ch >= 0)
					{
						content.append((char) ch);
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
		if (transMeta != null)
		{
			changed = transMeta;
			fname = transMeta.getFilename();
			name = transMeta.getName();
		}
		JobMeta jobMeta = getActiveJob();
		if (jobMeta != null)
		{
			changed = jobMeta;
			fname = jobMeta.getFilename();
			name = jobMeta.getName();
		}

		String text = "";

		if (rep != null)
		{
			text += APPL_TITLE + " - [" + getRepositoryName() + "] ";
        }
        else
		{
			text += APPL_TITLE + " - ";
		}

		if (Const.isEmpty(name))
		{
			if (!Const.isEmpty(fname))
			{
				text += fname;
            }
            else
			{
				String tab = getActiveTabText();
				if (!Const.isEmpty(tab))
				{
					text += tab;
                }
                else
				{
                    text+=Messages.getString("Spoon.Various.NoName");//"[no name]"
                }
				}
			}
                else
		{
			text += name;
		}

		if (changed != null && changed.hasChanged())
		{
			text += " " + Messages.getString("Spoon.Various.Changed");
		}

		shell.setText(text);

		enableMenus();
		markTabsChanged();
	}

	public void enableMenus()
	{
		boolean enableTransMenu = getActiveTransformation() != null;
		boolean enableJobMenu = getActiveJob() != null;

		boolean enableRepositoryMenu = rep != null;

		// Only enable certain menu-items if we need to.
		menuBar.setEnableById("file-save", enableTransMenu || enableJobMenu);
		menuBar.setEnableById("file-save-as", enableTransMenu || enableJobMenu);
		menuBar.setEnableById("file-close", enableTransMenu || enableJobMenu);
		menuBar.setEnableById("file-print", enableTransMenu || enableJobMenu);

		menuBar.setEnableById("edit-undo", enableTransMenu || enableJobMenu);
		menuBar.setEnableById("edit-redo", enableTransMenu || enableJobMenu);

		menuBar.setEnableById("edit-clear-selection", enableTransMenu);
		menuBar.setEnableById("edit-select-all", enableTransMenu);
		menuBar.setEnableById("edit-copy", enableTransMenu);
		menuBar.setEnableById("edit-paste", enableTransMenu);

		// Transformations
		menuBar.setEnableById("trans-run", enableTransMenu);
		menuBar.setEnableById("trans-preview", enableTransMenu);
		menuBar.setEnableById("trans-verify", enableTransMenu);
		menuBar.setEnableById("trans-impact", enableTransMenu);
		menuBar.setEnableById("trans-get-sql", enableTransMenu);
		menuBar.setEnableById("trans-last-impact", enableTransMenu);
		menuBar.setEnableById("trans-last-check", enableTransMenu);
		menuBar.setEnableById("trans-last-preview", enableTransMenu);
		menuBar.setEnableById("trans-copy", enableTransMenu);
		// miTransPaste.setEnabled(enableTransMenu);
		menuBar.setEnableById("trans-copy-image", enableTransMenu);
		menuBar.setEnableById("trans-settings", enableTransMenu);

		// Jobs
		menuBar.setEnableById("job-run", enableJobMenu);
		menuBar.setEnableById("job-copy", enableJobMenu);
		menuBar.setEnableById("job-settings", enableJobMenu);

		menuBar.setEnableById("wizard-connection", enableTransMenu || enableJobMenu);
		menuBar.setEnableById("wizard-copy-table", enableTransMenu || enableJobMenu);
		menuBar.setEnableById("wizard-copy-tables", enableRepositoryMenu || enableTransMenu || enableJobMenu);

		menuBar.setEnableById("repository-disconnect", enableRepositoryMenu);
		menuBar.setEnableById("repository-explore", enableRepositoryMenu);
		menuBar.setEnableById("repository-edit-user", enableRepositoryMenu);

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
		for (TabMapEntry entry : delegates.tabs.getTabs())
		{
            if (entry.getTabItem().isDisposed()) continue;

			boolean changed = entry.getObject().hasContentChanged();
			entry.getTabItem().setChanged(changed);
		}
	}

	public void printFile()
	{
		TransMeta transMeta = getActiveTransformation();
		if (transMeta != null)
		{
			printTransFile(transMeta);
		}

		JobMeta jobMeta = getActiveJob();
		if (jobMeta != null)
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
		img_gc.fillRectangle(0, 0, max.x, max.y);

		// Draw the transformation...
		jobGraph.drawJob(img_gc, false);

		ps.printImage(shell, img);

		img_gc.dispose();
		img.dispose();
		ps.dispose();
	}

    
	private TransGraph getActiveTransGraph()
	{
		TabMapEntry mapEntry = delegates.tabs.getTab(tabfolder.getSelected().getText());
		if (mapEntry.getObject() instanceof TransGraph)	return (TransGraph) mapEntry.getObject();
		return null;
	}

     public JobGraph getActiveJobGraph()
     {
         TabMapEntry mapEntry = delegates.tabs.getTab(tabfolder.getSelected().getText());
         if (mapEntry.getObject() instanceof JobGraph) return (JobGraph) mapEntry.getObject();
         return null;
     }

	/**
	 * @return the Log tab associated with the active transformation
	 */
	public TransLog getActiveTransLog()
	{
		TransMeta transMeta = getActiveTransformation();
        if (transMeta==null) return null; // nothing to work with.

        return delegates.trans.findTransLogOfTransformation(transMeta);
    }
	
	
	/**
     * @return the Log tab associated with the active job
     */
    private JobLog getActiveJobLog()
    {
        JobMeta jobMeta = getActiveJob();
        if (jobMeta==null) return null; // nothing to work with.

        return delegates.jobs.findJobLogOfJob(jobMeta);
    }

	/**
	 * @return the history tab associated with the active transformation
	 */
	private TransHistory getActiveTransHistory()
	{
		TransMeta transMeta = getActiveTransformation();
        if (transMeta==null) return null; // nothing to work with.

		return delegates.trans.findTransHistoryOfTransformation(transMeta);
	}

	public EngineMetaInterface getActiveMeta()
	{
        if (tabfolder==null) return null;
		TabItem tabItem = tabfolder.getSelected();
        if (tabItem==null) return null;

		// What transformation is in the active tab?
		// TransLog, TransGraph & TransHist contain the same transformation
		//
		TabMapEntry mapEntry = delegates.tabs.getTab(tabfolder.getSelected().getText());
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
		return delegates.trans.getTransformation(tabItemText);
	}

	public JobMeta findJob(String tabItemText)
	{
		return delegates.jobs.getJob(tabItemText);
	}

	public TransMeta[] getLoadedTransformations()
	{
		List<TransMeta> list = delegates.trans.getTransformationList();
		return list.toArray(new TransMeta[list.size()]);
	}

	public JobMeta[] getLoadedJobs()
	{
		List<JobMeta> list = delegates.jobs.getJobList();
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

        if (undoInterface instanceof TransMeta) delegates.trans.undoTransformationAction((TransMeta)undoInterface, ta);
        if (undoInterface instanceof JobMeta) delegates.jobs.undoJobAction((JobMeta)undoInterface, ta);

		// Put what we undo in focus
		if (undoInterface instanceof TransMeta)
		{
			TransGraph transGraph = delegates.trans.findTransGraphOfTransformation((TransMeta) undoInterface);
			transGraph.forceFocus();
		}
		if (undoInterface instanceof JobMeta)
		{
			JobGraph jobGraph = delegates.jobs.findJobGraphOfJob((JobMeta) undoInterface);
			jobGraph.forceFocus();
		}
	}

	public void redoAction(UndoInterface undoInterface)
	{
        if (undoInterface==null) return;

		TransAction ta = undoInterface.nextUndo();
        if (ta==null) return;

		setUndoMenu(undoInterface); // something changed: change the menu

        if (undoInterface instanceof TransMeta) delegates.trans.redoTransformationAction((TransMeta)undoInterface, ta);
        if (undoInterface instanceof JobMeta) delegates.jobs.redoJobAction((JobMeta)undoInterface, ta);

		// Put what we redo in focus
		if (undoInterface instanceof TransMeta)
		{
			TransGraph transGraph = delegates.trans.findTransGraphOfTransformation((TransMeta) undoInterface);
			transGraph.forceFocus();
		}
		if (undoInterface instanceof JobMeta)
		{
			JobGraph jobGraph = delegates.jobs.findJobGraphOfJob((JobMeta) undoInterface);
			jobGraph.forceFocus();
		}
	}

	public void setUndoMenu(UndoInterface undoInterface)
	{
        if (shell.isDisposed()) return;

		TransAction prev = undoInterface != null ? undoInterface.viewThisUndo() : null;
		TransAction next = undoInterface != null ? undoInterface.viewNextUndo() : null;

		if (prev != null)
		{
			menuBar.setEnableById("edit-undo", true);
            menuBar.setTextById( "edit-undo",  Messages.getString("Spoon.Menu.Undo.Available", prev.toString() ) );
        } 
        else            
		{
			menuBar.setEnableById("edit-redo", false);
        		menuBar.setTextById( "edit-redo", Messages.getString("Spoon.Menu.Undo.NotAvailable"));//"Undo : not available \tCTRL-Z"
		}

		if (next != null)
		{
			menuBar.setEnableById("edit-redo", true);
        		menuBar.setTextById( "edit-redo", Messages.getString("Spoon.Menu.Redo.Available",next.toString()));//"Redo : "+next.toString()+" \tCTRL-Y"
        } 
        else            
		{
			menuBar.setEnableById("edit-redo", false);
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
		if (transMeta == null)return;
		TransGraph transGraph = delegates.trans.findTransGraphOfTransformation(transMeta);
		if (transGraph == null)return;

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
        TransGraph transGraph = delegates.trans.findTransGraphOfTransformation(transMeta);
        if (transGraph==null) return;

		CheckResultDialog crd = new CheckResultDialog(transMeta, shell, SWT.NONE, transGraph.getRemarks());
		String stepname = crd.open();
		if (stepname != null)
		{
			// Go to the indicated step!
			StepMeta stepMeta = transMeta.findStep(stepname);
			if (stepMeta != null)
			{
				delegates.steps.editStep(transMeta, stepMeta);
			}
		}
	}

	public void analyseImpact(TransMeta transMeta)
	{
		if (transMeta == null)return;
		TransGraph transGraph = delegates.trans.findTransGraphOfTransformation(transMeta);
		if (transGraph == null)return;

        AnalyseImpactProgressDialog aipd = new AnalyseImpactProgressDialog(shell, transMeta, transGraph.getImpact());
		transGraph.setImpactFinished(aipd.open());
        if (transGraph.isImpactFinished()) showLastImpactAnalyses(transMeta);
	}

	public void showLastImpactAnalyses(TransMeta transMeta)
	{
        if (transMeta==null) return;
        TransGraph transGraph = delegates.trans.findTransGraphOfTransformation(transMeta);
        if (transGraph==null) return;

		List<Object[]> rows = new ArrayList<Object[]>();
		RowMetaInterface rowMeta = null;
		for (int i = 0; i < transGraph.getImpact().size(); i++)
		{
			DatabaseImpact ii = (DatabaseImpact) transGraph.getImpact().get(i);
			RowMetaAndData row = ii.getRow();
			rowMeta = row.getRowMeta();
			rows.add(row.getData());
		}

		if (rows.size() > 0)
		{
			// Display all the rows...
            PreviewRowsDialog prd = new PreviewRowsDialog(shell, Variables.getADefaultVariableSpace(), SWT.NONE, "-", rowMeta, rows);
            prd.setTitleMessage(Messages.getString("Spoon.Dialog.ImpactAnalyses.Title"), Messages.getString("Spoon.Dialog.ImpactAnalyses.Message"));//"Impact analyses"  "Result of analyses:"
			prd.open();
        }
        else
		{
			MessageBox mb = new MessageBox(shell, SWT.OK | SWT.ICON_INFORMATION);
			if (transGraph.isImpactFinished())
			{
                mb.setMessage(Messages.getString("Spoon.Dialog.TransformationNoImpactOnDatabase.Message"));//"As far as I can tell, this transformation has no impact on any database."
            }
            else
			{
                mb.setMessage(Messages.getString("Spoon.Dialog.RunImpactAnalysesFirst.Message"));//"Please run the impact analyses first on this transformation."
			}
			mb.setText(Messages.getString("Spoon.Dialog.ImpactAnalyses.Title"));// Impact
			mb.open();
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
			delegates.jobs.addJobGraph(jobMeta); // create a new tab
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
		TransGraph transGraph = delegates.trans.findTransGraphOfTransformation(transMeta);
		if (transGraph == null)	return;

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

		CreateDatabaseWizard cdw = new CreateDatabaseWizard();
    	DatabaseMeta newDBInfo=cdw.createAndRunDatabaseWizard(shell, props, hasDatabasesInterface.getDatabases());
    	if(newDBInfo!=null){ //finished
			hasDatabasesInterface.addDatabase(newDBInfo);
			refreshTree();
			refreshGraph();
		}
	}

	public List<DatabaseMeta> getActiveDatabases()
	{
		Map<String, DatabaseMeta> map = new Hashtable<String, DatabaseMeta>();

		HasDatabasesInterface hasDatabasesInterface = getActiveHasDatabasesInterface();
		if (hasDatabasesInterface != null)
		{
			for (int i = 0; i < hasDatabasesInterface.nrDatabases(); i++)
			{
				map.put(hasDatabasesInterface.getDatabase(i).getName(), hasDatabasesInterface.getDatabase(i));
			}
		}
		if (rep != null)
		{
			try
			{
				List<DatabaseMeta> repDBs = rep.getDatabases();
				for (int i = 0; i < repDBs.size(); i++)
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
		databases.addAll(map.values());

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
                return delegates.db.copyTable(page1.getSourceDatabase(), page1.getTargetDatabase(), page2.getSelection());
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
		wd.setMinimumPageSize(700, 400);
		wd.open();
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
		if ("1.4".compareToIgnoreCase(version) > 0)
		{
			System.out.println("The System is running on Java version " + version);
			System.out.println("Unfortunately, it needs version 1.4 or higher to run.");
			return;
		}

		// Set default Locale:
		Locale.setDefault(Const.DEFAULT_LOCALE);

		LogWriter.setConsoleAppenderDebug();
		if (Const.isEmpty(optionLogfile))
		{
			log = LogWriter.getInstance(Const.SPOON_LOG_FILE, false, LogWriter.LOG_LEVEL_BASIC);
        }
        else
		{
			log = LogWriter.getInstance(optionLogfile.toString(), true, LogWriter.LOG_LEVEL_BASIC);
		}

        if (log.getRealFilename()!=null) log.logBasic(APP_NAME, Messages.getString("Spoon.Log.LoggingToFile")+log.getRealFilename());//"Logging goes to "

		if (!Const.isEmpty(optionLoglevel))
		{
			log.setLogLevel(optionLoglevel.toString());
            log.logBasic(APP_NAME, Messages.getString("Spoon.Log.LoggingAtLevel")+log.getLogLevelDesc());//"Logging is at level : "
		}

		/* Load the plugins etc. */
		StepLoader stloader = StepLoader.getInstance();
		if (!stloader.read())
		{
            log.logError(APP_NAME, Messages.getString("Spoon.Log.ErrorLoadingAndHaltSystem"));//Error loading steps & plugins... halting Spoon!
			return;
		}

		/* Load the plugins etc. we need to load jobentry */
		JobEntryLoader jeloader = JobEntryLoader.getInstance();
		if (!jeloader.read())
		{
			log.logError("Spoon", "Error loading job entries & plugins... halting Kitchen!");
			return;
		}

        
		init(null);

		SpoonFactory.setSpoonInstance(this);
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
					MessageBox mb = new MessageBox(shell, SWT.OK | SWT.ICON_ERROR);
                    mb.setMessage(Messages.getString("Spoon.Dialog.RepositoryUserCannotWork.Message"));//"Sorry, this repository user can't work with transformations from the repository."
					mb.setText(Messages.getString("Spoon.Dialog.RepositoryUserCannotWork.Title"));// "Error!"
					mb.open();

					userinfo = null;
					repositoryMeta = null;
                } else {
                   	String repName = repositoryMeta.getName();                	
					RepositoriesMeta repsinfo = new RepositoriesMeta(log);
					if (repsinfo.readData())
					{
						repositoryMeta = repsinfo.findRepository(repName);
						if (repositoryMeta != null)
						{
							// Define and connect to the repository...
							setRepository(new Repository(log, repositoryMeta, userinfo));
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
						if (rep.connect(Messages.getString("Spoon.Application.Name")))// "Spoon"
						{
                                if (Const.isEmpty(optionDirname)) optionDirname=new StringBuffer(RepositoryDirectory.DIRECTORY_SEPARATOR);

							// Check username, password
                                rep.userinfo = new UserInfo(rep, optionUsername.toString(), optionPassword.toString());

							if (rep.userinfo.getID() > 0)
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
                                					delegates.jobs.addJobGraph(jobMeta);
                                        		}
											}
										}
									}
								}
                                else
							{
                                    log.logError(APP_NAME, Messages.getString("Spoon.Log.UnableVerifyUser"));//"Can't verify username and password."
								rep.disconnect();
								rep = null;
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
				if (rep != null && rep.userinfo != null)
				{
					if (!rep.connect(Messages.getString("Spoon.Application.Name"))) // "Spoon"
					{
						setRepository(null);
					}
				}

				if (props.openLastFile())
				{
                    log.logDetailed(APP_NAME, Messages.getString("Spoon.Log.TryingOpenLastUsedFile"));//"Trying to open the last file used."

					List<LastUsedFile> lastUsedFiles = props.getLastUsedFiles();

					if (lastUsedFiles.size() > 0)
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
			rep = null;
		}

	}

    public void start() {
    	if( !selectRep( splash ) ) {
			splash.dispose();
			quitFile();
    	} else {
			handleStartOptions();
			open();

            if( splash != null ) {
				splash.dispose();
			}
			try
			{
				while (!isDisposed())
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

		String kettleRepname = Const.getEnvironmentVariable("KETTLE_REPOSITORY", null);
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
			getCommandLineArgs(args);
			createSpoon();
			setArguments(args.toArray(new String[args.size()]));
			start();
    	} catch (Throwable t) {
			log.logError(toString(), "Fatal error : " + t.toString());
			log.logError(toString(), Const.getStackTracker(t));
		}
	}

	/**
	 * This is the main procedure for Spoon.
	 * 
     * @param a Arguments are available in the "Get System Info" step.
	 */
	public static void main(String[] a) throws KettleException
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
		boolean useRepository = repositoryMeta != null;

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
			if (rep != null) // load from this repository...
			{
				if (rep.getName().equalsIgnoreCase(lastUsedFile.getRepositoryName()))
				{
                    RepositoryDirectory repdir = rep.getDirectoryTree().findDirectory(lastUsedFile.getDirectory());
					if (repdir != null)
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
                            delegates.jobs.addJobGraph(jobMeta);
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
		if (methodDescription != null)
		{
			int method = StepPartitioningMeta.getMethod(methodDescription);
			stepPartitioningMeta.setMethod(method);
			switch (method)
			{
            case StepPartitioningMeta.PARTITIONING_METHOD_NONE:  break;
			case StepPartitioningMeta.PARTITIONING_METHOD_MIRROR:
			case StepPartitioningMeta.PARTITIONING_METHOD_MOD:
				// Ask for a Partitioning Schema
				String schemaNames[] = transMeta.getPartitionSchemasNames();
				if (schemaNames.length == 0)
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
					if (partitionSchema != null)
					{
						idx = Const.indexOfString(partitionSchema.getName(), schemaNames);
					}
                    EnterSelectionDialog askSchema = new EnterSelectionDialog(shell, schemaNames, "Select a partition schema", "Select the partition schema to use:");
					String schemaName = askSchema.open(idx);
					if (schemaName != null)
					{
						idx = Const.indexOfString(schemaName, schemaNames);
						stepPartitioningMeta.setPartitionSchema(transMeta.getPartitionSchemas().get(idx));
					}
				}

				if (method == StepPartitioningMeta.PARTITIONING_METHOD_MOD)
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
		if (stepMeta.getClusterSchema() != null)
		{
			idx = transMeta.getClusterSchemas().indexOf(stepMeta.getClusterSchema());
		}
		String[] clusterSchemaNames = transMeta.getClusterSchemaNames();
        EnterSelectionDialog dialog = new EnterSelectionDialog(shell, clusterSchemaNames, "Cluster schema", "Select the cluster schema to use (cancel=clear)");
		String schemaName = dialog.open(idx);
		if (schemaName == null)
		{
			stepMeta.setClusterSchema(null);
        }
        else
		{
			ClusterSchema clusterSchema = transMeta.findClusterSchema(schemaName);
			stepMeta.setClusterSchema(clusterSchema);
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
     * This creates a new partitioning schema, edits it and adds it to the transformation metadata
	 * 
	 */
	public void newPartitioningSchema(TransMeta transMeta)
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
			if (rep != null && partitionSchema.getId() > 0)
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
			if (rep != null && clusterSchema.getId() > 0)
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
		delegates.slaves.newSlaveServer(transMeta);
	}

	public void delSlaveServer(TransMeta transMeta, SlaveServer slaveServer)
	{
		try
		{
			delegates.slaves.delSlaveServer(transMeta,slaveServer);
		} catch (KettleException e)
		{
            new ErrorDialog(shell, Messages.getString("Spoon.Dialog.ErrorDeletingSlave.Title"), Messages.getString("Spoon.Dialog.ErrorDeletingSlave.Message"), e); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

    
	/**
	 * Sends transformation to slave server
	 * @param executionConfiguration
	 */
	public void sendXMLToSlaveServer(TransMeta transMeta, TransExecutionConfiguration executionConfiguration){
		try
		{
			delegates.slaves.sendXMLToSlaveServer(transMeta,executionConfiguration);
		} catch (Exception e)
		{
			new ErrorDialog(shell, "Error", "Error sending transformation to server", e);
		}
	}

	public void splitTrans(TransMeta transMeta, boolean show, boolean post, boolean prepare, boolean start)	{
		try
		{
			delegates.trans.splitTrans(transMeta, show, post, prepare, start);
		} catch (Exception e)
		{
			new ErrorDialog(shell, "Split transformation", "There was an error during transformation split",
					e);
		}
	}

	public void runFile(){
		executeFile(true, false, false, false, null);
	}

	public void previewFile(){
		executeFile(true, false, false, true, null);
	}

	public void executeFile(boolean local, boolean remote, boolean cluster, boolean preview, Date replayDate)
	{
		TransMeta transMeta = getActiveTransformation();
		if (transMeta != null)	executeTransformation(transMeta, local, remote, cluster, preview, replayDate);

		JobMeta jobMeta = getActiveJob();
		if (jobMeta != null)executeJob(jobMeta, local, remote, cluster, preview, replayDate);

	}

    public void executeTransformation(TransMeta transMeta, boolean local, boolean remote, boolean cluster, boolean preview, Date replayDate)
	{
		try
		{
			delegates.trans.executeTransformation(transMeta, local, remote, cluster, preview, replayDate);
		} catch (Exception e)
		{
			new ErrorDialog(shell, "Split transformation", "There was an error during transformation split",
					e);
		}
	}

	public void executeJob(JobMeta jobMeta, boolean local, boolean remote, boolean cluster, boolean preview,
			Date replayDate)
	{
		delegates.jobs.addJobLog(jobMeta);
		JobLog jobLog = getActiveJobLog();
		jobLog.startJob(replayDate);
	}

	public void addSpoonSlave(SlaveServer slaveServer)
	{
		delegates.slaves.addSpoonSlave(slaveServer);
	}

	public void addTransLog(TransMeta transMeta)
	{
		delegates.trans.addTransLog(transMeta, true);
	}

	public void addTransHistory(TransMeta transMeta, boolean select)
	{
		delegates.trans.addTransHistory(transMeta, select);
	}

	public void addJobHistory(JobMeta jobMeta, boolean select)
	{
		delegates.jobs.addJobHistory(jobMeta, select);
	}

	public void pasteSteps()
	{
		// Is there an active TransGraph?
		TransGraph transGraph = getActiveTransGraph();
        if (transGraph==null) return;
		TransMeta transMeta = (TransMeta) transGraph.getMeta();

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

	public JobEntryCopy newJobEntry(JobMeta jobMeta, String typeDesc, boolean openit)
	{
		return delegates.jobs.newJobEntry(jobMeta, typeDesc, openit);
	}

	public JobEntryDialogInterface getJobEntryDialog(JobEntryInterface jei, JobMeta jobMeta)
	{

		return delegates.jobs.getJobEntryDialog(jei, jobMeta);
	}

	public StepDialogInterface getStepEntryDialog(StepMetaInterface stepMeta, TransMeta transMeta,
			String stepName)
	{
		try
		{
			return delegates.steps.getStepEntryDialog(stepMeta, transMeta, stepName);
		} catch (Throwable t)
		{
			log.logError("Could not create dialog for " + stepMeta.getDialogClassName(), t.getMessage());
		}
		return null;
	}

	public void editJobEntry(JobMeta jobMeta, JobEntryCopy je){
		delegates.jobs.editJobEntry(jobMeta, je);
	}

	public JobEntryTrans newJobEntry(JobMeta jobMeta, JobEntryType type){
		return delegates.jobs.newJobEntry(jobMeta, type);
	}

	public void deleteJobEntryCopies(JobMeta jobMeta, JobEntryCopy jobEntry){
		delegates.jobs.deleteJobEntryCopies(jobMeta, jobEntry);
	}

	public void pasteXML(JobMeta jobMeta, String clipcontent, Point loc){
		delegates.jobs.pasteXML(jobMeta, clipcontent, loc);
	}

	public void newJobHop(JobMeta jobMeta, JobEntryCopy fr, JobEntryCopy to){
		delegates.jobs.newJobHop(jobMeta, fr, to);
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
	public void ripDBWizard(){
		delegates.jobs.ripDBWizard();
	}

	public JobMeta ripDB(final List<DatabaseMeta> databases, final String jobName,
			final RepositoryDirectory repdir, final String directory, final DatabaseMeta sourceDbInfo,
			final DatabaseMeta targetDbInfo, final String[] tables)
	{
		return delegates.jobs.ripDB(databases, jobName, repdir, directory, sourceDbInfo, targetDbInfo, tables);
	}

	/**
	 * Set the core object state.
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

	public LogWriter getLog()
	{
		return log;
	}

	public Repository getRepository()
	{
		return rep;
	}

	public void setRepository(Repository rep)
	{
		this.rep = rep;
	}

	public void addMenuListener(String id, Object listener, String methodName)
	{
		menuListeners.add(new Object[] { id, listener, methodName });
	}

	public void addTransGraph(TransMeta transMeta)
	{
		delegates.trans.addTransGraph(transMeta);
	}

	public boolean addSpoonBrowser(String name, String urlString)
	{
		return delegates.tabs.addSpoonBrowser(name, urlString);
	}


	public TransExecutionConfiguration getExecutionConfiguration()
	{
		return executionConfiguration;
	}

	public Object[] messageDialogWithToggle(String dialogTitle, Image image, String message,
			int dialogImageType, String[] buttonLabels, int defaultIndex, String toggleMessage,
			boolean toggleState)
	{
		return GUIResource.getInstance().messageDialogWithToggle(shell, dialogTitle, image, message, dialogImageType,
				buttonLabels, defaultIndex, toggleMessage, toggleState);
	}
	
	public void editStepErrorHandling(TransMeta transMeta, StepMeta stepMeta)
	{
		delegates.steps.editStepErrorHandling(transMeta, stepMeta);
	}
	
	public String editStep(TransMeta transMeta, StepMeta stepMeta)
	{
		return delegates.steps.editStep(transMeta, stepMeta);
	}
	
	public void dupeStep(TransMeta transMeta, StepMeta stepMeta)
	{
		delegates.steps.dupeStep(transMeta, stepMeta);
	}
	
	public void delStep(TransMeta transMeta, StepMeta stepMeta)
	{
		delegates.steps.delStep(transMeta, stepMeta);
	}
	
	public String makeTransGraphTabName(TransMeta transMeta)
	{
		return delegates.tabs.makeTransGraphTabName(transMeta);
	}
	
	public void newConnection()
	{
		delegates.db.newConnection();
	}
	
	public void getSQL()
	{
		delegates.db.getSQL();
	}

}