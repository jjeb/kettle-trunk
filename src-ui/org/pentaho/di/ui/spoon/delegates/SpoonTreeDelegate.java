package org.pentaho.di.ui.spoon.delegates;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSource;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DragSourceListener;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.pentaho.di.cluster.ClusterSchema;
import org.pentaho.di.cluster.SlaveServer;
import org.pentaho.di.core.database.DatabaseMeta;
import org.pentaho.di.core.dnd.DragAndDropContainer;
import org.pentaho.di.core.dnd.XMLTransfer;
import org.pentaho.di.job.JobEntryLoader;
import org.pentaho.di.job.JobMeta;
import org.pentaho.di.job.JobPlugin;
import org.pentaho.di.job.entry.JobEntryCopy;
import org.pentaho.di.partition.PartitionSchema;
import org.pentaho.di.ui.core.ConstUI;
import org.pentaho.di.ui.spoon.delegates.SpoonDelegate;
import org.pentaho.di.trans.StepLoader;
import org.pentaho.di.trans.StepPlugin;
import org.pentaho.di.trans.TransHopMeta;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.ui.spoon.Spoon;
import org.pentaho.di.ui.spoon.TreeSelection;

public class SpoonTreeDelegate extends SpoonDelegate
{
	public SpoonTreeDelegate(Spoon spoon)
	{
		super(spoon);
	}

	/**
	 * @return The object that is selected in the tree or null if we couldn't
	 *         figure it out. (titles etc. == null)
	 */
	public TreeSelection[] getTreeObjects(final Tree tree, Tree selectionTree, Tree coreObjectsTree, Tree sharedObjectsTree)
	{
		List<TreeSelection> objects = new ArrayList<TreeSelection>();

		if (tree.equals(selectionTree))
		{
			TreeItem[] selection = selectionTree.getSelection();
			for (int s = 0; s < selection.length; s++)
			{
				TreeItem treeItem = selection[s];
				String[] path = ConstUI.getTreeStrings(treeItem);

				TreeSelection object = null;

				switch (path.length)
				{
				case 0:
					break;
				case 1: // ------complete-----
					if (path[0].equals(Spoon.STRING_TRANSFORMATIONS)) // the top level Transformations entry
					{
						object = new TreeSelection(path[0], TransMeta.class);
					}
					if (path[0].equals(Spoon.STRING_JOBS)) // the top level Jobs entry
					{
						object = new TreeSelection(path[0], JobMeta.class);
					}
					break;

				case 2: // ------complete-----
					if (path[0].equals(Spoon.STRING_BUILDING_BLOCKS)) // the top level Transformations entry
					{
						if (path[1].equals(Spoon.STRING_TRANS_BASE))
						{
							object = new TreeSelection(path[1], StepPlugin.class);
						}
					}
					if (path[0].equals(Spoon.STRING_TRANSFORMATIONS)) // Transformations title
					{
						object = new TreeSelection(path[1], spoon.delegates.trans.getTransformation(path[1]));
					}
					if (path[0].equals(Spoon.STRING_JOBS)) // Jobs title
					{
						object = new TreeSelection(path[1], spoon.delegates.jobs.getJob(path[1]));
					}
					break;

				case 3: // ------complete-----
					if (path[0].equals(Spoon.STRING_TRANSFORMATIONS)) // Transformations
					// title
					{
						TransMeta transMeta = spoon.delegates.trans.getTransformation(path[1]);
						if (path[2].equals(Spoon.STRING_CONNECTIONS))
							object = new TreeSelection(path[2], DatabaseMeta.class, transMeta);
						if (path[2].equals(Spoon.STRING_STEPS))
							object = new TreeSelection(path[2], StepMeta.class, transMeta);
						if (path[2].equals(Spoon.STRING_HOPS))
							object = new TreeSelection(path[2], TransHopMeta.class, transMeta);
						if (path[2].equals(Spoon.STRING_PARTITIONS))
							object = new TreeSelection(path[2], PartitionSchema.class, transMeta);
						if (path[2].equals(Spoon.STRING_SLAVES))
							object = new TreeSelection(path[2], SlaveServer.class, transMeta);
						if (path[2].equals(Spoon.STRING_CLUSTERS))
							object = new TreeSelection(path[2], ClusterSchema.class, transMeta);
					}
					if (path[0].equals(Spoon.STRING_JOBS)) // Jobs title
					{
						JobMeta jobMeta = spoon.delegates.jobs.getJob(path[1]);
						if (path[2].equals(Spoon.STRING_CONNECTIONS))
							object = new TreeSelection(path[2], DatabaseMeta.class, jobMeta);
						if (path[2].equals(Spoon.STRING_JOB_ENTRIES))
							object = new TreeSelection(path[2], JobEntryCopy.class, jobMeta);
					}
					break;

				case 4: // ------complete-----
					if (path[0].equals(Spoon.STRING_TRANSFORMATIONS)) // The name of a transformation
					{
						TransMeta transMeta = spoon.delegates.trans.getTransformation(path[1]);
						if (path[2].equals(Spoon.STRING_CONNECTIONS))
							object = new TreeSelection(path[3], transMeta.findDatabase(path[3]), transMeta);
						if (path[2].equals(Spoon.STRING_STEPS))
							object = new TreeSelection(path[3], transMeta.findStep(path[3]), transMeta);
						if (path[2].equals(Spoon.STRING_HOPS))
							object = new TreeSelection(path[3], transMeta.findTransHop(path[3]), transMeta);
						if (path[2].equals(Spoon.STRING_PARTITIONS))
							object = new TreeSelection(path[3], transMeta.findPartitionSchema(path[3]),
									transMeta);
						if (path[2].equals(Spoon.STRING_SLAVES))
							object = new TreeSelection(path[3], transMeta.findSlaveServer(path[3]), transMeta);
						if (path[2].equals(Spoon.STRING_CLUSTERS))
							object = new TreeSelection(path[3], transMeta.findClusterSchema(path[3]),
									transMeta);
					}
					if (path[0].equals(Spoon.STRING_JOBS)) // The name of a job
					{
						JobMeta jobMeta = spoon.delegates.jobs.getJob(path[1]);
						if (jobMeta != null && path[2].equals(Spoon.STRING_CONNECTIONS))
							object = new TreeSelection(path[3], jobMeta.findDatabase(path[3]), jobMeta);
						if (jobMeta != null && path[2].equals(Spoon.STRING_JOB_ENTRIES))
							object = new TreeSelection(path[3], jobMeta.findJobEntry(path[3]), jobMeta);
					}
					break;

				case 5:
					if (path[0].equals(Spoon.STRING_TRANSFORMATIONS)) // The name of a transformation
					{
						TransMeta transMeta = spoon.delegates.trans.getTransformation(path[1]);
						if (transMeta != null && path[2].equals(Spoon.STRING_CLUSTERS))
						{
							ClusterSchema clusterSchema = transMeta.findClusterSchema(path[3]);
							object = new TreeSelection(path[4], clusterSchema.findSlaveServer(path[4]),
									clusterSchema, transMeta);
						}
					}
					break;
				default:
					break;
				}

				if (object != null)
				{
					objects.add(object);
				}
			}
		}
		if (tree.equals(coreObjectsTree))
		{
			TreeItem[] selection = coreObjectsTree.getSelection();
			for (int s = 0; s < selection.length; s++)
			{
				TreeItem treeItem = selection[s];
				String[] path = ConstUI.getTreeStrings(treeItem);

				TreeSelection object = null;

				switch (path.length)
				{
				case 0:
					break;
				case 1:
					break; // nothing
				case 2: // Job entries
					if (path[0].equals(Spoon.STRING_JOB_BASE))
					{
						JobPlugin jobPlugin = JobEntryLoader.getInstance().findJobEntriesWithDescription(
								path[1]);
						if (jobPlugin != null)
						{
							object = new TreeSelection(path[1], jobPlugin);
						} else
						{
							object = new TreeSelection(path[1], JobPlugin.class);
						}
					}
					break;
				case 3: // Steps
					if (path[0].equals(Spoon.STRING_TRANS_BASE))
					{
						object = new TreeSelection(path[2], StepLoader.getInstance().findStepPluginWithDescription(path[2]));
					}
					break;
				default:
					break;
				}

				if (object != null)
				{
					objects.add(object);
				}
			}
		}
		if (tree.equals(sharedObjectsTree))
		{
			TreeItem[] selection = sharedObjectsTree.getSelection();
			for (int s = 0; s < selection.length; s++)
			{
				TreeItem treeItem = selection[s];
				String[] path = ConstUI.getTreeStrings(treeItem);

				TreeSelection object = null;

				switch (path.length)
				{
				case 0:
					break;
				case 1: // // the top level database connections entry
					break;
				case 2: 
					if (path[0].equals(Spoon.STRING_CONNECTIONS)) // click on a shared database connection... 
					{
						DatabaseMeta databaseMeta = DatabaseMeta.findDatabase(spoon.getSharedDatabases(), path[1]);
						object = new TreeSelection(path[1], databaseMeta);
					}
					break;
				}
				
				if (object != null)
				{
					objects.add(object);
				}
			}
		}

		return objects.toArray(new TreeSelection[objects.size()]);
	}

	public void addDragSourceToTree(final Tree tree,final Tree selectionTree,final Tree coreObjectsTree, final Tree sharedObjectsTree)
	{
		// Drag & Drop for steps
		Transfer[] ttypes = new Transfer[] { XMLTransfer.getInstance() };

		DragSource ddSource = new DragSource(tree, DND.DROP_MOVE);
		ddSource.setTransfer(ttypes);
		ddSource.addDragListener(new DragSourceListener()
		{
			public void dragStart(DragSourceEvent event)
			{
			}

			public void dragSetData(DragSourceEvent event)
			{
				TreeSelection[] treeObjects = getTreeObjects(tree,selectionTree,coreObjectsTree, sharedObjectsTree);
				if (treeObjects.length == 0)
				{
					event.doit = false;
					return;
				}

				int type = 0;
				String data = null;

				TreeSelection treeObject = treeObjects[0];
				Object object = treeObject.getSelection();
				JobMeta jobMeta = spoon.getActiveJob();

				if (object instanceof StepMeta)
				{
					StepMeta stepMeta = (StepMeta) object;
					type = DragAndDropContainer.TYPE_STEP;
					data = stepMeta.getName(); // name of the step.
				} else if (object instanceof StepPlugin)
				{
					StepPlugin stepPlugin = (StepPlugin) object;
					type = DragAndDropContainer.TYPE_BASE_STEP_TYPE;
					data = stepPlugin.getDescription(); // Step type description
				} else if (object instanceof DatabaseMeta)
				{
					DatabaseMeta databaseMeta = (DatabaseMeta) object;
					type = DragAndDropContainer.TYPE_DATABASE_CONNECTION;
					data = databaseMeta.getName();
				} else if (object instanceof TransHopMeta)
				{
					TransHopMeta hop = (TransHopMeta) object;
					type = DragAndDropContainer.TYPE_TRANS_HOP;
					data = hop.toString(); // nothing for really ;-)
				} else if (object instanceof JobEntryCopy)
				{
					JobEntryCopy jobEntryCopy = (JobEntryCopy) object;
					type = DragAndDropContainer.TYPE_JOB_ENTRY;
					data = jobEntryCopy.getName(); // name of the job entry.
				} else if (object instanceof JobPlugin)
				{
					JobPlugin jobPlugin = (JobPlugin) object;
					type = DragAndDropContainer.TYPE_BASE_JOB_ENTRY;
					data = jobPlugin.getDescription(); // Step type
				} else if (object instanceof Class && object.equals(JobPlugin.class))
				{
					JobEntryCopy dummy = null;
					if (jobMeta != null)
						dummy = jobMeta.findJobEntry(JobMeta.STRING_SPECIAL_DUMMY, 0, true);
					if (JobMeta.STRING_SPECIAL_DUMMY.equalsIgnoreCase(treeObject.getItemText())
							&& dummy != null)
					{
						// if dummy already exists, add a copy
						type = DragAndDropContainer.TYPE_JOB_ENTRY;
						data = dummy.getName();
					} else
					{
						type = DragAndDropContainer.TYPE_BASE_JOB_ENTRY;
						data = treeObject.getItemText();
					}
				} else
				{
					event.doit = false;
					return; // ignore anything else you drag.
				}

				event.data = new DragAndDropContainer(type, data);
			}

			public void dragFinished(DragSourceEvent event)
			{
			}
		});

	}

}
