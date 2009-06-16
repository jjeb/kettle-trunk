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

package org.pentaho.di.job.entry;

import java.util.List;

import org.pentaho.di.cluster.SlaveServer;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.changed.ChangedFlagInterface;
import org.pentaho.di.core.database.DatabaseMeta;
import org.pentaho.di.core.exception.KettleStepLoaderException;
import org.pentaho.di.core.exception.KettleXMLException;
import org.pentaho.di.core.gui.GUIPositionInterface;
import org.pentaho.di.core.gui.Point;
import org.pentaho.di.core.logging.LogWriter;
import org.pentaho.di.core.xml.XMLHandler;
import org.pentaho.di.core.xml.XMLInterface;
import org.pentaho.di.job.JobEntryLoader;
import org.pentaho.di.job.JobPlugin;
import org.pentaho.di.repository.ObjectId;
import org.pentaho.di.repository.Repository;
import org.w3c.dom.Node;

/**
 * This class describes the fact that a single JobEntry can be used multiple
 * times in the same Job. Therefor it contains a link to a JobEntry, a position,
 * a number, etc.
 * 
 * @author Matt
 * @since 01-10-2003
 * 
 */

public class JobEntryCopy implements Cloneable, XMLInterface, GUIPositionInterface, ChangedFlagInterface
{
	private static final String	XML_TAG	= "entry";

	private JobEntryInterface entry;

	private int nr; // Copy nr. 0 is the base copy...

	private boolean selected;

	private Point location;

    /**
     * Flag to indicate that the job entries following this one are launched in parallel
     */
	private boolean launchingInParallel;

	private boolean draw;

	private ObjectId id;

	public JobEntryCopy()
	{
		clear();
	}

	/**
	 * @deprecated Log is no longer required.
	 * @param log
	 */
	public JobEntryCopy(LogWriter log)
	{
		clear();
	}

	public JobEntryCopy(LogWriter log, JobEntryInterface entry)
	{
		setEntry(entry);
	}

	public String getXML()
	{
		StringBuffer retval = new StringBuffer();

		retval.append("    ").append(XMLHandler.openTag(XML_TAG)).append(Const.CR);
		retval.append(entry.getXML());

		retval.append("      ").append(XMLHandler.addTagValue("parallel", launchingInParallel));
		retval.append("      ").append(XMLHandler.addTagValue("draw", draw));
		retval.append("      ").append(XMLHandler.addTagValue("nr", nr));
		retval.append("      ").append(XMLHandler.addTagValue("xloc", location.x));
		retval.append("      ").append(XMLHandler.addTagValue("yloc", location.y));

		retval.append("      ").append(XMLHandler.closeTag(XML_TAG)).append(Const.CR);
		return retval.toString();
	}

	public JobEntryCopy(Node entrynode, List<DatabaseMeta> databases, List<SlaveServer> slaveServers, Repository rep) throws KettleXMLException
	{
		try
		{
			String stype = XMLHandler.getTagValue(entrynode, "type");

			JobPlugin jobPlugin = JobEntryLoader.getInstance().findJobEntriesWithID(stype);
			if (jobPlugin == null)
				throw new KettleStepLoaderException("No valid step/plugin specified (jobPlugin=null) for " + stype);

			// Get an empty JobEntry of the appropriate class...
			entry = JobEntryLoader.getInstance().getJobEntryClass(jobPlugin);
			if (entry != null)
			{
				// System.out.println("New JobEntryInterface built of type:
				// "+entry.getTypeDesc());
				entry.loadXML(entrynode, databases, slaveServers, rep);

				// Handle GUI information: nr & location?
				setNr(Const.toInt(XMLHandler.getTagValue(entrynode, "nr"), 0));
				setLaunchingInParallel("Y".equalsIgnoreCase(XMLHandler.getTagValue(entrynode, "parallel")));
				setDrawn("Y".equalsIgnoreCase(XMLHandler.getTagValue(entrynode, "draw")));
				int x = Const.toInt(XMLHandler.getTagValue(entrynode, "xloc"), 0);
				int y = Const.toInt(XMLHandler.getTagValue(entrynode, "yloc"), 0);
				setLocation(x, y);
			}
		} catch (Throwable e)
		{
			String message = "Unable to read Job Entry copy info from XML node : " + e.toString();
			LogWriter log = LogWriter.getInstance();
			log.logError(toString(), message);
			log.logError(toString(), Const.getStackTracker(e));
			throw new KettleXMLException(message, e);
		}
	}

	public void clear()
	{
		location = null;
		entry = null;
		nr = 0;
		launchingInParallel = false;
		setObjectId(null);
	}

	public Object clone()
	{
		JobEntryCopy ge = new JobEntryCopy();
		ge.replaceMeta(this);
		ge.setObjectId(null);
		return ge;
	}

	public void replaceMeta(JobEntryCopy jobEntryCopy)
	{
		entry = jobEntryCopy.entry;
		nr = jobEntryCopy.nr; // Copy nr. 0 is the base copy...

		selected = jobEntryCopy.selected;
		if (jobEntryCopy.location != null)
			location = new Point(jobEntryCopy.location.x, jobEntryCopy.location.y);
		launchingInParallel = jobEntryCopy.launchingInParallel;
		draw = jobEntryCopy.draw;

		id = jobEntryCopy.id;
	}

	public Object clone_deep()
	{
		JobEntryCopy ge = (JobEntryCopy) clone();

		// Copy underlying object as well...
		ge.entry = (JobEntryInterface) entry.clone();

		return ge;
	}

	public void setObjectId(ObjectId id)
	{
		this.id = id;
	}

	public boolean equals(Object o)
	{
		if (o == null) return false;
		JobEntryCopy je = (JobEntryCopy) o;
		return je.entry.getName().equalsIgnoreCase(entry.getName()) && je.getNr() == getNr();
	}

	public ObjectId getObjectId()
	{
		return id;
	}

	public void setEntry(JobEntryInterface je)
	{
		entry = je;
		if (entry!=null)
		{
			if (entry.getConfigId()==null)
		    {
				entry.setConfigId( JobEntryLoader.getInstance().getJobEntryID(entry) );
		    }
		}
	}

	public JobEntryInterface getEntry()
	{
		return entry;
	}

	/**
	 * @return entry in JobEntryInterface.typeCode[] for native jobs,
	 *         entry.getTypeCode() for plugins
	 */
	public String getTypeDesc()
	{
		JobPlugin plugin = JobEntryLoader.getInstance().findJobPluginWithID(entry.getTypeId());
		return plugin.getDescription();
	}

	public void setLocation(int x, int y)
	{
		int nx = (x >= 0 ? x : 0);
		int ny = (y >= 0 ? y : 0);

		Point loc = new Point(nx, ny);
		if (!loc.equals(location))
			setChanged();
		location = loc;
	}

	public void setLocation(Point loc)
	{
		if (loc != null && !loc.equals(location))
			setChanged();
		location = loc;
	}

	public Point getLocation()
	{
		return location;
	}

	public void setChanged()
	{
		setChanged(true);
	}

	public void setChanged(boolean ch)
	{
		entry.setChanged(ch);
	}

	public boolean hasChanged()
	{
		return entry.hasChanged();
	}

	public int getNr()
	{
		return nr;
	}

	public void setNr(int n)
	{
		nr = n;
	}

	public void setLaunchingInParallel(boolean p)
	{
		launchingInParallel = p;
	}

	public boolean isDrawn()
	{
		return draw;
	}

	public void setDrawn()
	{
		setDrawn(true);
	}

	public void setDrawn(boolean d)
	{
		draw = d;
	}

	public boolean isLaunchingInParallel()
	{
		return launchingInParallel;
	}

	public void setSelected(boolean sel)
	{
		selected = sel;
	}

	public void flipSelected()
	{
		selected = !selected;
	}

	public boolean isSelected()
	{
		return selected;
	}

	public void setDescription(String description)
	{
		entry.setDescription(description);
	}

	public String getDescription()
	{
		return entry.getDescription();
	}

	public boolean isStart()
	{
		return entry.isStart();
	}

	public boolean isDummy()
	{
		return entry.isDummy();
	}

	public boolean isTransformation()
	{
		return entry.isTransformation();
	}

	public boolean isJob()
	{
		return entry.isJob();
	}

	public boolean evaluates()
	{
		if (entry != null)
			return entry.evaluates();
		return false;
	}

	public boolean isUnconditional()
	{
		if (entry != null)
			return entry.isUnconditional();
		return true;
	}

	public boolean isEvaluation()
	{
		return entry.isEvaluation();
	}

	public boolean isMail()
	{
		return entry.isMail();
	}

	public boolean isSpecial()
	{
		return entry.isSpecial();
	}

	public String toString()
	{
		if( entry != null ) 
		{
			return entry.getName() + "." + getNr();
		} else {
			return "null."+getNr();
		}
	}

	public String getName()
	{
		if( entry != null ) 
		{
			return entry.getName();
		} else {
			return "null";
		}
	}

	public void setName(String name)
	{
		entry.setName(name);
	}

	public boolean resetErrorsBeforeExecution()
	{
		return entry.resetErrorsBeforeExecution();
	}
}
