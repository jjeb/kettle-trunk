package org.pentaho.di.ui.spoon.delegates;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.MessageBox;
import org.pentaho.di.cluster.SlaveServer;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.EngineMetaInterface;
import org.pentaho.di.core.NotePadMeta;
import org.pentaho.di.core.Result;
import org.pentaho.di.core.database.DatabaseMeta;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.gui.Point;
import org.pentaho.di.core.gui.SpoonInterface;
import org.pentaho.di.core.logging.LogWriter;
import org.pentaho.di.core.undo.TransAction;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransExecutionConfiguration;
import org.pentaho.di.trans.TransHopMeta;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.cluster.TransSplitter;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.ui.core.dialog.ShowMessageDialog;
import org.pentaho.di.ui.core.gui.GUIResource;
import org.pentaho.di.ui.spoon.Messages;
import org.pentaho.di.ui.spoon.Spoon;
import org.pentaho.di.ui.spoon.TabMapEntry;
import org.pentaho.di.ui.spoon.job.JobGraph;
import org.pentaho.di.ui.spoon.trans.TransGraph;
import org.pentaho.di.ui.spoon.trans.TransHistory;
import org.pentaho.di.ui.spoon.trans.TransHistoryRefresher;
import org.pentaho.di.ui.spoon.trans.TransLog;
import org.pentaho.di.ui.trans.dialog.TransExecutionConfigurationDialog;
import org.pentaho.xul.swt.tab.TabItem;

public class SpoonTransformationDelegate extends SpoonDelegate
{
	/**
	 * This contains a map between the name of a transformation and the
	 * TransMeta object. If the transformation has no name it will be mapped
	 * under a number [1], [2] etc.
	 */
	private Map<String, TransMeta> transformationMap;

	public SpoonTransformationDelegate(Spoon spoon)
	{
		super(spoon);
		transformationMap = new Hashtable<String, TransMeta>();
	}

	/**
	 * Add a transformation to the
	 * 
	 * @param transMeta
	 *            the transformation to add to the map
	 * @return the key used to store the transformation in the map
	 */
	public String addTransformation(TransMeta transMeta)
	{
		String key = spoon.delegates.tabs.makeTransGraphTabName(transMeta);

		TransMeta xform = (TransMeta) transformationMap.get(key);
		if (xform == null)
		{
			transformationMap.put(key, transMeta);
		} else
		{
			// found a transformation tab that has the same name, is it the same
			// as the one we want to load, if not warn the user of the duplicate
			// name
			// this check may produce false negatives, i.e., references that are
			// deemed
			// different when they in fact refer to the same entry. For example,
			// one of
			// the transforms may use a variable reference or an alternative but
			// equivalent5
			boolean same = false;
			if (transMeta.isRepReference() && xform.isRepReference())
			{
				// a repository value, check directory
				same = transMeta.getDirectory().getPath().equals(xform.getDirectory().getPath());
			} else if (transMeta.isFileReference() && xform.isFileReference())
			{
				// a file system entry, check file path
				same = transMeta.getFilename().equals(xform.getFilename());
			}

			if (!same)
			{
				ShowMessageDialog dialog = new ShowMessageDialog(spoon.getShell(), SWT.OK
						| SWT.ICON_INFORMATION, Messages.getString("Spoon.Dialog.TransAlreadyLoaded.Title"),
						"'" + key + "'" + Const.CR + Const.CR
								+ Messages.getString("Spoon.Dialog.TransAlreadyLoaded.Message"));
				dialog.setTimeOut(6);
				dialog.open();
				/*
				 * MessageBox mb = new MessageBox(shell, SWT.OK |
				 * SWT.ICON_INFORMATION);
				 * mb.setMessage("'"+key+"'"+Const.CR+Const.CR+Messages.getString("Spoon.Dialog.TransAlreadyLoaded.Message")); //
				 * Transformation is already loaded
				 * mb.setText(Messages.getString("Spoon.Dialog.TransAlreadyLoaded.Title")); //
				 * Sorry! mb.open();
				 */
			}
		}

		return key;
	}

	/**
	 * @param transMeta
	 *            the transformation to close, make sure it's ok to dispose of
	 *            it BEFORE you call this.
	 */
	public void closeTransformation(TransMeta transMeta)
	{
		String tabName = spoon.delegates.tabs.makeTransGraphTabName(transMeta);
		transformationMap.remove(tabName);

		// Close the associated tabs...
		TabItem graphTab = spoon.delegates.tabs.findTabItem(tabName,
				TabMapEntry.OBJECT_TYPE_TRANSFORMATION_GRAPH);
		if (graphTab != null)
		{
			graphTab.dispose();
			spoon.delegates.tabs.removeTab(tabName);
		}

		// Logging
		String logTabName = spoon.delegates.tabs.makeLogTabName(transMeta);
		TabItem logTab = spoon.delegates.tabs.findTabItem(logTabName,
				TabMapEntry.OBJECT_TYPE_TRANSFORMATION_LOG);
		if (logTab != null)
		{
			logTab.dispose();
			spoon.delegates.tabs.removeTab(logTabName);
		}

		// History
		String historyTabName = spoon.delegates.tabs.makeHistoryTabName(transMeta);
		TabItem historyTab = spoon.delegates.tabs.findTabItem(historyTabName,
				TabMapEntry.OBJECT_TYPE_TRANSFORMATION_HISTORY);
		if (historyTab != null)
		{
			historyTab.dispose();
			spoon.delegates.tabs.removeTab(historyTabName);
		}

		spoon.refreshTree();
	}

	public void addTransGraph(TransMeta transMeta)
	{
		String key = addTransformation(transMeta);
		if (key != null)
		{
			// See if there already is a tab for this graph
			// If no, add it
			// If yes, select that tab
			//
			String tabName = spoon.delegates.tabs.makeTransGraphTabName(transMeta);
			TabItem tabItem = spoon.delegates.tabs.findTabItem(tabName,
					TabMapEntry.OBJECT_TYPE_TRANSFORMATION_GRAPH);
			if (tabItem == null)
			{
				TransGraph transGraph = new TransGraph(spoon.tabfolder.getSwtTabset(), spoon, transMeta);
				tabItem = new TabItem(spoon.tabfolder, tabName, tabName);
				tabItem.setToolTipText(Messages.getString("Spoon.TabTrans.Tooltip", spoon.delegates.tabs
						.makeTransGraphTabName(transMeta)));
				tabItem.setImage(GUIResource.getInstance().getImageTransGraph());
				tabItem.setControl(transGraph);

				spoon.delegates.tabs.addTab(tabName, new TabMapEntry(tabItem, tabName, transGraph,
						TabMapEntry.OBJECT_TYPE_TRANSFORMATION_GRAPH));
			}
			int idx = spoon.tabfolder.indexOf(tabItem);

			// OK, also see if we need to open a new history window.
			if (transMeta.getLogConnection() != null && !Const.isEmpty(transMeta.getLogTable()) && !transMeta.isSlaveTransformation())
			{
				addTransHistory(transMeta, false);
			}
			// keep the focus on the graph
			spoon.tabfolder.setSelected(idx);

			spoon.setUndoMenu(transMeta);
			spoon.enableMenus();
		}
	}

	public TransHistory findTransHistoryOfTransformation(TransMeta transMeta)
	{
		if (transMeta == null)
			return null;

		// Now loop over the entries in the tab-map
		for (TabMapEntry mapEntry : spoon.delegates.tabs.getTabs())
		{
			if (mapEntry.getObject() instanceof TransHistory)
			{
				TransHistory transHistory = (TransHistory) mapEntry.getObject();
				if (transHistory.getMeta() != null && transHistory.getMeta().equals(transMeta))
					return transHistory;
			}
		}
		return null;
	}

	public TransLog findTransLogOfTransformation(TransMeta transMeta)
	{
		// Now loop over the entries in the tab-map
		for (TabMapEntry mapEntry : spoon.delegates.tabs.getTabs())
		{
			if (mapEntry.getObject() instanceof TransLog)
			{
				TransLog transLog = (TransLog) mapEntry.getObject();
				if (transLog.getMeta().equals(transMeta))
					return transLog;
			}
		}
		return null;
	}

	public void addTransLog(TransMeta transMeta, boolean setActive)
	{
		// See if there already is a tab for this log
		// If no, add it
		// If yes, select that tab
		// if setActive is true
		//
		String tabName = spoon.delegates.tabs.makeLogTabName(transMeta);
		TabItem tabItem = spoon.delegates.tabs.findTabItem(tabName,
				TabMapEntry.OBJECT_TYPE_TRANSFORMATION_LOG);
		if (tabItem == null)
		{
			TransLog transLog = new TransLog(spoon.tabfolder.getSwtTabset(), spoon, transMeta);
			tabItem = new TabItem(spoon.tabfolder, tabName, tabName);
			tabItem.setToolTipText(Messages.getString("Spoon.Title.ExecLogTransView.Tooltip",
					spoon.delegates.tabs.makeTransGraphTabName(transMeta)));
			tabItem.setControl(transLog);

			// If there is an associated history window, we want to keep that
			// one up-to-date as well.
			//
			TransHistory transHistory = findTransHistoryOfTransformation(transMeta);
			TabItem historyItem = spoon.delegates.tabs.findTabItem(spoon.delegates.tabs
					.makeHistoryTabName(transMeta), TabMapEntry.OBJECT_TYPE_TRANSFORMATION_HISTORY);

			if (transHistory != null && historyItem != null)
			{
				TransHistoryRefresher transHistoryRefresher = new TransHistoryRefresher(historyItem,
						transHistory);
				spoon.tabfolder.addListener(transHistoryRefresher);
				transLog.setTransHistoryRefresher(transHistoryRefresher);
			}

			spoon.delegates.tabs.addTab(tabName, new TabMapEntry(tabItem, tabName, transLog,
					TabMapEntry.OBJECT_TYPE_TRANSFORMATION_LOG));
		}
		if (setActive)
		{
			int idx = spoon.tabfolder.indexOf(tabItem);
			spoon.tabfolder.setSelected(idx);
		}
	}

	public void addTransHistory(TransMeta transMeta, boolean select)
	{
		// See if there already is a tab for this history view
		// If no, add it
		// If yes, select that tab
		//
		String tabName = spoon.delegates.tabs.makeHistoryTabName(transMeta);
		TabItem tabItem = spoon.delegates.tabs.findTabItem(tabName,
				TabMapEntry.OBJECT_TYPE_TRANSFORMATION_HISTORY);
		if (tabItem == null)
		{
			TransHistory transHistory = new TransHistory(spoon.tabfolder.getSwtTabset(), spoon, transMeta);
			tabItem = new TabItem(spoon.tabfolder, tabName, tabName);
			tabItem.setToolTipText(Messages.getString("Spoon.Title.ExecHistoryTransView.Tooltip",
					spoon.delegates.tabs.makeTransGraphTabName(transMeta)));
			tabItem.setControl(transHistory);

			// If there is an associated log window that's open, find it and add
			// a refresher
			TransLog transLog = findTransLogOfTransformation(transMeta);
			if (transLog != null)
			{
				TransHistoryRefresher transHistoryRefresher = new TransHistoryRefresher(tabItem, transHistory);
				spoon.tabfolder.addListener(transHistoryRefresher);
				transLog.setTransHistoryRefresher(transHistoryRefresher);
			}
			transHistory.markRefreshNeeded(); // will refresh when first
			// selected

			spoon.delegates.tabs.addTab(tabName, new TabMapEntry(tabItem, tabName, transHistory,
					TabMapEntry.OBJECT_TYPE_TRANSFORMATION_HISTORY));
		}
		if (select)
		{
			int idx = spoon.tabfolder.indexOf(tabItem);
			spoon.tabfolder.setSelected(idx);
		}
	}

	public void tabSelected(TabItem item)
	{
		List<TabMapEntry> collection = spoon.delegates.tabs.getTabs();

		// See which core objects to show
		//
		for (TabMapEntry entry : collection)
		{
			if (item.equals(entry.getTabItem()))
			{
				// TabItemInterface itemInterface = entry.getObject();

				//
				// Another way to implement this may be to keep track of the
				// state of the core object tree in method
				// addCoreObjectsToTree()
				//
				if (entry.getObject() instanceof TransGraph || entry.getObject() instanceof JobGraph)
				{
					EngineMetaInterface meta = entry.getObject().getMeta();
					if (meta != null)
					{
						meta.setInternalKettleVariables();
					}
					if (spoon.getCoreObjectsState() != SpoonInterface.STATE_CORE_OBJECTS_SPOON)
					{
						spoon.refreshCoreObjects();
					}
				}
			}
		}

		// Also refresh the tree
		spoon.refreshTree();
		spoon.enableMenus();
	}

	public List<TransMeta> getTransformationList()
	{
		return new ArrayList<TransMeta>(transformationMap.values());
	}

	public TransMeta getTransformation(String tabItemText)
	{
		return transformationMap.get(tabItemText);
	}

	public void addTransformation(String key, TransMeta entry)
	{
		transformationMap.put(key, entry);
	}

	public void removeTransformation(String key)
	{
		transformationMap.remove(key);
	}

	public TransMeta[] getLoadedTransformations()
	{
		List<TransMeta> list = new ArrayList<TransMeta>(transformationMap.values());
		return list.toArray(new TransMeta[list.size()]);
	}

	public TransGraph findTransGraphOfTransformation(TransMeta transMeta)
	{
		// Now loop over the entries in the tab-map
		for (TabMapEntry mapEntry : spoon.delegates.tabs.getTabs())
		{
			if (mapEntry.getObject() instanceof TransGraph)
			{
				TransGraph transGraph = (TransGraph) mapEntry.getObject();
				if (transGraph.getMeta().equals(transMeta))
					return transGraph;
			}
		}
		return null;
	}

	public boolean isDefaultTransformationName(String name)
	{
		if (!name.startsWith(Spoon.STRING_TRANSFORMATION))
			return false;

		// see if there are only digits behind the transformation...
		// This will detect:
		// "Transformation"
		// "Transformation "
		// "Transformation 1"
		// "Transformation 2"
		// ...
		for (int i = Spoon.STRING_TRANSFORMATION.length() + 1; i < name.length(); i++)
		{
			if (!Character.isDigit(name.charAt(i)))
				return false;
		}
		return true;
	}

	public void undoTransformationAction(TransMeta transMeta, TransAction transAction)
	{
		switch (transAction.getType())
		{
		// We created a new step : undo this...
		case TransAction.TYPE_ACTION_NEW_STEP:
			// Delete the step at correct location:
			for (int i = transAction.getCurrent().length - 1; i >= 0; i--)
			{
				int idx = transAction.getCurrentIndex()[i];
				transMeta.removeStep(idx);
			}
			spoon.refreshTree();
			spoon.refreshGraph();
			break;

		// We created a new connection : undo this...
		case TransAction.TYPE_ACTION_NEW_CONNECTION:
			// Delete the connection at correct location:
			for (int i = transAction.getCurrent().length - 1; i >= 0; i--)
			{
				int idx = transAction.getCurrentIndex()[i];
				transMeta.removeDatabase(idx);
			}
			spoon.refreshTree();
			spoon.refreshGraph();
			break;

		// We created a new note : undo this...
		case TransAction.TYPE_ACTION_NEW_NOTE:
			// Delete the note at correct location:
			for (int i = transAction.getCurrent().length - 1; i >= 0; i--)
			{
				int idx = transAction.getCurrentIndex()[i];
				transMeta.removeNote(idx);
			}
			spoon.refreshTree();
			spoon.refreshGraph();
			break;

		// We created a new hop : undo this...
		case TransAction.TYPE_ACTION_NEW_HOP:
			// Delete the hop at correct location:
			for (int i = transAction.getCurrent().length - 1; i >= 0; i--)
			{
				int idx = transAction.getCurrentIndex()[i];
				transMeta.removeTransHop(idx);
			}
			spoon.refreshTree();
			spoon.refreshGraph();
			break;

		// We created a new slave : undo this...
		case TransAction.TYPE_ACTION_NEW_SLAVE:
			// Delete the slave at correct location:
			for (int i = transAction.getCurrent().length - 1; i >= 0; i--)
			{
				int idx = transAction.getCurrentIndex()[i];
				transMeta.getSlaveServers().remove(idx);
			}
			spoon.refreshTree();
			spoon.refreshGraph();
			break;

		// We created a new slave : undo this...
		case TransAction.TYPE_ACTION_NEW_CLUSTER:
			// Delete the slave at correct location:
			for (int i = transAction.getCurrent().length - 1; i >= 0; i--)
			{
				int idx = transAction.getCurrentIndex()[i];
				transMeta.getClusterSchemas().remove(idx);
			}
			spoon.refreshTree();
			spoon.refreshGraph();
			break;

		//
		// DELETE
		//

		// We delete a step : undo this...
		case TransAction.TYPE_ACTION_DELETE_STEP:
			// un-Delete the step at correct location: re-insert
			for (int i = 0; i < transAction.getCurrent().length; i++)
			{
				StepMeta stepMeta = (StepMeta) transAction.getCurrent()[i];
				int idx = transAction.getCurrentIndex()[i];
				transMeta.addStep(idx, stepMeta);
			}
			spoon.refreshTree();
			spoon.refreshGraph();
			break;

		// We deleted a connection : undo this...
		case TransAction.TYPE_ACTION_DELETE_CONNECTION:
			// re-insert the connection at correct location:
			for (int i = 0; i < transAction.getCurrent().length; i++)
			{
				DatabaseMeta ci = (DatabaseMeta) transAction.getCurrent()[i];
				int idx = transAction.getCurrentIndex()[i];
				transMeta.addDatabase(idx, ci);
			}
			spoon.refreshTree();
			spoon.refreshGraph();
			break;

		// We delete new note : undo this...
		case TransAction.TYPE_ACTION_DELETE_NOTE:
			// re-insert the note at correct location:
			for (int i = 0; i < transAction.getCurrent().length; i++)
			{
				NotePadMeta ni = (NotePadMeta) transAction.getCurrent()[i];
				int idx = transAction.getCurrentIndex()[i];
				transMeta.addNote(idx, ni);
			}
			spoon.refreshTree();
			spoon.refreshGraph();
			break;

		// We deleted a hop : undo this...
		case TransAction.TYPE_ACTION_DELETE_HOP:
			// re-insert the hop at correct location:
			for (int i = 0; i < transAction.getCurrent().length; i++)
			{
				TransHopMeta hi = (TransHopMeta) transAction.getCurrent()[i];
				int idx = transAction.getCurrentIndex()[i];
				// Build a new hop:
				StepMeta from = transMeta.findStep(hi.getFromStep().getName());
				StepMeta to = transMeta.findStep(hi.getToStep().getName());
				TransHopMeta hinew = new TransHopMeta(from, to);
				transMeta.addTransHop(idx, hinew);
			}
			spoon.refreshTree();
			spoon.refreshGraph();
			break;

		//
		// CHANGE
		//

		// We changed a step : undo this...
		case TransAction.TYPE_ACTION_CHANGE_STEP:
			// Delete the current step, insert previous version.
			for (int i = 0; i < transAction.getCurrent().length; i++)
			{
				StepMeta prev = (StepMeta) ((StepMeta) transAction.getPrevious()[i]).clone();
				int idx = transAction.getCurrentIndex()[i];

				transMeta.getStep(idx).replaceMeta(prev);
			}
			spoon.refreshTree();
			spoon.refreshGraph();
			break;

		// We changed a connection : undo this...
		case TransAction.TYPE_ACTION_CHANGE_CONNECTION:
			// Delete & re-insert
			for (int i = 0; i < transAction.getCurrent().length; i++)
			{
				DatabaseMeta prev = (DatabaseMeta) transAction.getPrevious()[i];
				int idx = transAction.getCurrentIndex()[i];

				transMeta.getDatabase(idx).replaceMeta((DatabaseMeta) prev.clone());
			}
			spoon.refreshTree();
			spoon.refreshGraph();
			break;

		// We changed a note : undo this...
		case TransAction.TYPE_ACTION_CHANGE_NOTE:
			// Delete & re-insert
			for (int i = 0; i < transAction.getCurrent().length; i++)
			{
				int idx = transAction.getCurrentIndex()[i];
				transMeta.removeNote(idx);
				NotePadMeta prev = (NotePadMeta) transAction.getPrevious()[i];
				transMeta.addNote(idx, (NotePadMeta) prev.clone());
			}
			spoon.refreshTree();
			spoon.refreshGraph();
			break;

		// We changed a hop : undo this...
		case TransAction.TYPE_ACTION_CHANGE_HOP:
			// Delete & re-insert
			for (int i = 0; i < transAction.getCurrent().length; i++)
			{
				TransHopMeta prev = (TransHopMeta) transAction.getPrevious()[i];
				int idx = transAction.getCurrentIndex()[i];

				transMeta.removeTransHop(idx);
				transMeta.addTransHop(idx, (TransHopMeta) prev.clone());
			}
			spoon.refreshTree();
			spoon.refreshGraph();
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
			spoon.refreshGraph();
			break;

		// The position of a note has changed: undo this...
		case TransAction.TYPE_ACTION_POSITION_NOTE:
			for (int i = 0; i < transAction.getCurrentIndex().length; i++)
			{
				int idx = transAction.getCurrentIndex()[i];
				NotePadMeta npi = transMeta.getNote(idx);
				Point prev = transAction.getPreviousLocation()[i];
				npi.setLocation(prev);
			}
			spoon.refreshGraph();
			break;
		default:
			break;
		}

		// OK, now check if we need to do this again...
		if (transMeta.viewNextUndo() != null)
		{
			if (transMeta.viewNextUndo().getNextAlso())
				spoon.undoAction(transMeta);
		}
	}

	public void redoTransformationAction(TransMeta transMeta, TransAction transAction)
	{
		switch (transAction.getType())
		{
		case TransAction.TYPE_ACTION_NEW_STEP:
			// re-delete the step at correct location:
			for (int i = 0; i < transAction.getCurrent().length; i++)
			{
				StepMeta stepMeta = (StepMeta) transAction.getCurrent()[i];
				int idx = transAction.getCurrentIndex()[i];
				transMeta.addStep(idx, stepMeta);

				spoon.refreshTree();
				spoon.refreshGraph();
			}
			break;

		case TransAction.TYPE_ACTION_NEW_CONNECTION:
			// re-insert the connection at correct location:
			for (int i = 0; i < transAction.getCurrent().length; i++)
			{
				DatabaseMeta ci = (DatabaseMeta) transAction.getCurrent()[i];
				int idx = transAction.getCurrentIndex()[i];
				transMeta.addDatabase(idx, ci);
				spoon.refreshTree();
				spoon.refreshGraph();
			}
			break;

		case TransAction.TYPE_ACTION_NEW_NOTE:
			// re-insert the note at correct location:
			for (int i = 0; i < transAction.getCurrent().length; i++)
			{
				NotePadMeta ni = (NotePadMeta) transAction.getCurrent()[i];
				int idx = transAction.getCurrentIndex()[i];
				transMeta.addNote(idx, ni);
				spoon.refreshTree();
				spoon.refreshGraph();
			}
			break;

		case TransAction.TYPE_ACTION_NEW_HOP:
			// re-insert the hop at correct location:
			for (int i = 0; i < transAction.getCurrent().length; i++)
			{
				TransHopMeta hi = (TransHopMeta) transAction.getCurrent()[i];
				int idx = transAction.getCurrentIndex()[i];
				transMeta.addTransHop(idx, hi);
				spoon.refreshTree();
				spoon.refreshGraph();
			}
			break;

		//  
		// DELETE
		//
		case TransAction.TYPE_ACTION_DELETE_STEP:
			// re-remove the step at correct location:
			for (int i = transAction.getCurrent().length - 1; i >= 0; i--)
			{
				int idx = transAction.getCurrentIndex()[i];
				transMeta.removeStep(idx);
			}
			spoon.refreshTree();
			spoon.refreshGraph();
			break;

		case TransAction.TYPE_ACTION_DELETE_CONNECTION:
			// re-remove the connection at correct location:
			for (int i = transAction.getCurrent().length - 1; i >= 0; i--)
			{
				int idx = transAction.getCurrentIndex()[i];
				transMeta.removeDatabase(idx);
			}
			spoon.refreshTree();
			spoon.refreshGraph();
			break;

		case TransAction.TYPE_ACTION_DELETE_NOTE:
			// re-remove the note at correct location:
			for (int i = transAction.getCurrent().length - 1; i >= 0; i--)
			{
				int idx = transAction.getCurrentIndex()[i];
				transMeta.removeNote(idx);
			}
			spoon.refreshTree();
			spoon.refreshGraph();
			break;

		case TransAction.TYPE_ACTION_DELETE_HOP:
			// re-remove the hop at correct location:
			for (int i = transAction.getCurrent().length - 1; i >= 0; i--)
			{
				int idx = transAction.getCurrentIndex()[i];
				transMeta.removeTransHop(idx);
			}
			spoon.refreshTree();
			spoon.refreshGraph();
			break;

		//
		// CHANGE
		//

		// We changed a step : undo this...
		case TransAction.TYPE_ACTION_CHANGE_STEP:
			// Delete the current step, insert previous version.
			for (int i = 0; i < transAction.getCurrent().length; i++)
			{
				StepMeta stepMeta = (StepMeta) ((StepMeta) transAction.getCurrent()[i]).clone();
				transMeta.getStep(transAction.getCurrentIndex()[i]).replaceMeta(stepMeta);
			}
			spoon.refreshTree();
			spoon.refreshGraph();
			break;

		// We changed a connection : undo this...
		case TransAction.TYPE_ACTION_CHANGE_CONNECTION:
			// Delete & re-insert
			for (int i = 0; i < transAction.getCurrent().length; i++)
			{
				DatabaseMeta databaseMeta = (DatabaseMeta) transAction.getCurrent()[i];
				int idx = transAction.getCurrentIndex()[i];

				transMeta.getDatabase(idx).replaceMeta((DatabaseMeta) databaseMeta.clone());
			}
			spoon.refreshTree();
			spoon.refreshGraph();
			break;

		// We changed a note : undo this...
		case TransAction.TYPE_ACTION_CHANGE_NOTE:
			// Delete & re-insert
			for (int i = 0; i < transAction.getCurrent().length; i++)
			{
				NotePadMeta ni = (NotePadMeta) transAction.getCurrent()[i];
				int idx = transAction.getCurrentIndex()[i];

				transMeta.removeNote(idx);
				transMeta.addNote(idx, (NotePadMeta) ni.clone());
			}
			spoon.refreshTree();
			spoon.refreshGraph();
			break;

		// We changed a hop : undo this...
		case TransAction.TYPE_ACTION_CHANGE_HOP:
			// Delete & re-insert
			for (int i = 0; i < transAction.getCurrent().length; i++)
			{
				TransHopMeta hi = (TransHopMeta) transAction.getCurrent()[i];
				int idx = transAction.getCurrentIndex()[i];

				transMeta.removeTransHop(idx);
				transMeta.addTransHop(idx, (TransHopMeta) hi.clone());
			}
			spoon.refreshTree();
			spoon.refreshGraph();
			break;

		//
		// CHANGE POSITION
		//
		case TransAction.TYPE_ACTION_POSITION_STEP:
			for (int i = 0; i < transAction.getCurrentIndex().length; i++)
			{
				// Find & change the location of the step:
				StepMeta stepMeta = transMeta.getStep(transAction.getCurrentIndex()[i]);
				stepMeta.setLocation(transAction.getCurrentLocation()[i]);
			}
			spoon.refreshGraph();
			break;
		case TransAction.TYPE_ACTION_POSITION_NOTE:
			for (int i = 0; i < transAction.getCurrentIndex().length; i++)
			{
				int idx = transAction.getCurrentIndex()[i];
				NotePadMeta npi = transMeta.getNote(idx);
				Point curr = transAction.getCurrentLocation()[i];
				npi.setLocation(curr);
			}
			spoon.refreshGraph();
			break;
		default:
			break;
		}

		// OK, now check if we need to do this again...
		if (transMeta.viewNextUndo() != null)
		{
			if (transMeta.viewNextUndo().getNextAlso())
				spoon.redoAction(transMeta);
		}
	}

	public void executeTransformation(TransMeta transMeta, boolean local, boolean remote, boolean cluster, boolean preview,
			Date replayDate) throws KettleException {
		if (transMeta == null)
			return;

		TransExecutionConfiguration executionConfiguration = spoon.getExecutionConfiguration();

		// THIS IS USELESS NOTHING WAS SELECTED YET
		// executionConfiguration.setExecutingLocally(local);
		// executionConfiguration.setExecutingRemotely(remote);
		// executionConfiguration.setExecutingClustered(cluster);
		
		if (transMeta.isUsingAtLeastOneClusterSchema()) {
			executionConfiguration.setExecutingLocally(false);
			executionConfiguration.setExecutingRemotely(false);
			executionConfiguration.setExecutingClustered(true);
		}
		else
		{
			executionConfiguration.setExecutingLocally(true);
			executionConfiguration.setExecutingRemotely(false);
			executionConfiguration.setExecutingClustered(false);
		}

		Object data[] = spoon.variables.getData();
		String fields[] = spoon.variables.getRowMeta().getFieldNames();
		Map<String, String> variableMap = new HashMap<String, String>();
		for (int idx = 0; idx < fields.length; idx++) {
			variableMap.put(fields[idx], data[idx].toString());
		}

		executionConfiguration.getUsedVariables(transMeta);
		executionConfiguration.getUsedArguments(transMeta, spoon.getArguments());
		executionConfiguration.setVariables(variableMap);
		executionConfiguration.setReplayDate(replayDate);
		executionConfiguration.setLocalPreviewing(preview);

		executionConfiguration.setLogLevel(spoon.getLog().getLogLevel());
		// executionConfiguration.setSafeModeEnabled( transLog!=null &&
		// transLog.isSafeModeChecked() );

		TransExecutionConfigurationDialog dialog = new TransExecutionConfigurationDialog(spoon.getShell(), executionConfiguration, transMeta);
		if (dialog.open()) {
			addTransLog(transMeta, !executionConfiguration.isLocalPreviewing());
			TransLog transLog = spoon.getActiveTransLog();

			if (executionConfiguration.isExecutingLocally()) {
				if (executionConfiguration.isLocalPreviewing()) {
					transLog.preview(executionConfiguration);
				} else {
					transLog.start(executionConfiguration);
				}
			} else if (executionConfiguration.isExecutingRemotely()) {
				if (executionConfiguration.getRemoteServer() != null) {
					spoon.delegates.slaves.sendXMLToSlaveServer(transMeta, executionConfiguration);
					spoon.delegates.slaves.addSpoonSlave(executionConfiguration.getRemoteServer());
				} else {
					MessageBox mb = new MessageBox(spoon.getShell(), SWT.OK | SWT.ICON_INFORMATION);
					mb.setMessage(Messages.getString("Spoon.Dialog.NoRemoteServerSpecified.Message"));
					mb.setText(Messages.getString("Spoon.Dialog.NoRemoteServerSpecified.Title"));
					mb.open();
				}
			} else if (executionConfiguration.isExecutingClustered()) {
				splitTrans(transMeta, executionConfiguration);
			}
		}
	}
  
  protected void splitTrans(final TransMeta transMeta, final TransExecutionConfiguration executionConfiguration) throws KettleException {
    try
    {
      final TransSplitter transSplitter = new TransSplitter(transMeta);
      
      transSplitter.splitOriginalTransformation();
      
      TransMeta master = transSplitter.getMaster();
      SlaveServer masterServer = null;
      List<StepMeta> masterSteps = master.getTransHopSteps(false);
      
      // add transgraph of transmetas if showing is true
      SlaveServer slaves[] = transSplitter.getSlaveTargets();
      
      if (executionConfiguration.isClusterShowingTransformation()) {
        if (masterSteps.size() > 0) // If there is something that needs to
        // be done on the master...
        {
          masterServer = transSplitter.getMasterServer();
          addTransGraph(master);
        }
  
        // Then the slaves...
        //

        for (int i = 0; i < slaves.length; i++) {
          TransMeta slaveTrans = (TransMeta) transSplitter.getSlaveTransMap().get(slaves[i]);
          addTransGraph(slaveTrans);
        }
      }
      
      Trans.executeClustered(transSplitter, executionConfiguration);
      
      if (executionConfiguration.isClusterPosting()) {
        // Now add monitors for the master and all the slave servers
        //
        if (masterServer != null)
        {
          spoon.addSpoonSlave(masterServer);
          for (int i = 0; i < slaves.length; i++)
          {
            spoon.addSpoonSlave(slaves[i]);
          }
        }
      }
      
      // OK, we should also start monitoring of the cluster in the background.
      // Stop them all if one goes bad.
      // Also clean up afterwards, close sockets, etc.
      // 
      // Launch in a separate thread to prevent GUI blocking...
      //
      new Thread(new Runnable() {
				public void run() {
					Trans.monitorClusteredTransformation(transMeta.toString(), transSplitter, null);
					Result result = Trans.getClusteredTransformationResult(transMeta.toString(), transSplitter, null);
					LogWriter log = LogWriter.getInstance();
					log.logBasic(transMeta.toString(), "-----------------------------------------------------");
					log.logBasic(transMeta.toString(), "Got result back from clustered transformation:");
					log.logBasic(transMeta.toString(), "-----------------------------------------------------");
					log.logBasic(transMeta.toString(), "Errors : "+result.getNrErrors());
					log.logBasic(transMeta.toString(), "Input : "+result.getNrLinesInput());
					log.logBasic(transMeta.toString(), "Output : "+result.getNrLinesOutput());
					log.logBasic(transMeta.toString(), "Updated : "+result.getNrLinesUpdated());
					log.logBasic(transMeta.toString(), "Read : "+result.getNrLinesRead());
					log.logBasic(transMeta.toString(), "Written : "+result.getNrLinesWritten());
					log.logBasic(transMeta.toString(), "Rejected : "+result.getNrLinesRejected());
					log.logBasic(transMeta.toString(), "-----------------------------------------------------");
				}
			}).start();
	
    } 
    catch (Exception e)
    {
      throw new KettleException(e);
    }
  }

}
