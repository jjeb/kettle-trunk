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

 
package org.pentaho.di.chef;
import java.util.List;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.DropTargetListener;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.pentaho.di.core.NotePadMeta;
import org.pentaho.di.core.Props;
import org.pentaho.di.core.SnapAllignDistribute;
import org.pentaho.di.core.dialog.EnterTextDialog;
import org.pentaho.di.job.JobHopMeta;
import org.pentaho.di.job.JobMeta;
import org.pentaho.di.job.entries.job.JobEntryJob;
import org.pentaho.di.job.entries.trans.JobEntryTrans;
import org.pentaho.di.job.entry.JobEntryCopy;
import org.pentaho.di.job.entry.JobEntryInterface;
import org.pentaho.di.spoon.Spoon;
import org.pentaho.di.spoon.TabItemInterface;
import org.pentaho.di.spoon.TabMapEntry;
import org.pentaho.di.spoon.TransPainter;
import org.pentaho.di.trans.TransMeta;

import be.ibridge.kettle.core.Const;
import be.ibridge.kettle.core.DragAndDropContainer;
import org.pentaho.di.core.GUIResource;
import be.ibridge.kettle.core.LogWriter;
import be.ibridge.kettle.core.Point;
import be.ibridge.kettle.core.Rectangle;
import be.ibridge.kettle.core.Redrawable;
import be.ibridge.kettle.core.XMLTransfer;
import org.pentaho.di.core.dialog.ErrorDialog;
import org.pentaho.di.core.util.StringUtil;
import be.ibridge.kettle.core.vfs.KettleVFS;




/**
 * Handles the display of Jobs in Chef, in a graphical form.
 * 
 * @author Matt
 * Created on 17-mei-2003
 *
 */

public class ChefGraph extends Composite implements Redrawable, TabItemInterface
{
	private static final int HOP_SEL_MARGIN = 9;

	private Shell shell;
	private Canvas canvas;
	private LogWriter log;
    private JobMeta jobMeta;
    // private Props props;
    
	private int iconsize;
	private int linewidth;
	private Point lastclick;

	private JobEntryCopy selected_entries[];
	private JobEntryCopy selected_icon;
	private Point          prev_locations[];
	private NotePadMeta    selected_note;
	private Point previous_note_location;
    private Point          lastMove;

	private JobHopMeta     hop_candidate;
	private Point drop_candidate;
	private Spoon spoon;

	private Point offset, iconoffset, noteoffset;
	private ScrollBar hori;
	private ScrollBar vert;

	// public boolean shift, control;
	private boolean split_hop;
	private int last_button;
	private JobHopMeta last_hop_split;
	private Rectangle selrect;

	private static final double theta = Math.toRadians(10); // arrowhead sharpness
	private static final int    size  = 30; // arrowhead length
	
	private int shadowsize;

    private Menu mPop;

    private Menu mPopAD;


	public ChefGraph(Composite par, final Spoon spoon, final JobMeta jobMeta) 
	{
		super(par, SWT.NONE);
		shell = par.getShell();
		this.log = LogWriter.getInstance();
		this.spoon = spoon;
		this.jobMeta = jobMeta;
        // this.props = Props.getInstance();
        
        setLayout(new FillLayout());
        
        canvas = new Canvas(this, SWT.V_SCROLL | SWT.H_SCROLL | SWT.NO_BACKGROUND);
        
		newProps();
		
		selrect = null;
		hop_candidate = null;
		last_hop_split = null;

		selected_entries = null;
		selected_note = null;
        
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
		
		canvas.addPaintListener(new PaintListener() {
			public void paintControl(PaintEvent e) {
				ChefGraph.this.paintControl(e);
			}
		});

		selected_entries = null;
		lastclick = null;

		canvas.addMouseListener(new MouseAdapter() 
		{
			public void mouseDoubleClick(MouseEvent e) 
			{
			    clearSettings();

				Point real = screen2real(e.x, e.y);
				
				JobEntryCopy jobentry = jobMeta.getChefGraphEntry(real.x, real.y, iconsize);
				if (jobentry != null) 
				{
					if (e.button==1) 
					{
						editEntry(jobentry);
					}
					else // launch Chef or Spoon 
					{
						launchStuff(jobentry);
					}
				} 
				else 
				{
					// Check if point lies on one of the many hop-lines...
					JobHopMeta online = findJobHop(real.x, real.y);
					if (online != null) 
					{
						// editJobHop(online);
					}
					else
					{
						NotePadMeta ni = jobMeta.getNote(real.x, real.y);
						if (ni!=null)
						{
							editNote(ni);
						}
					}

				}
			}

			public void mouseDown(MouseEvent e) 
			{
                clearSettings();
                
				last_button = e.button;
				Point real = screen2real(e.x, e.y);
                lastclick = new Point(real.x, real.y);

				// Clear the tooltip!
				setToolTipText(null);

				// Set the pop-up menu
				if (e.button==3)
                {
                    setMenu(real.x, real.y);
                    return;
                }
				
				JobEntryCopy je = jobMeta.getChefGraphEntry(real.x, real.y, iconsize);
				if (je != null) 
				{
					selected_entries = jobMeta.getSelectedEntries();
					selected_icon = je;
					// make sure this is correct!!!
					// When an icon is moved that is not selected, it gets selected too late.
					// It is not captured here, but in the mouseMoveListener...
					prev_locations = jobMeta.getSelectedLocations();

					Point p = je.getLocation();
					iconoffset = new Point(real.x - p.x, real.y - p.y);
				} 
				else 
				{
					// Dit we hit a note?
					NotePadMeta ni = jobMeta.getNote(real.x, real.y);
					if (ni!=null && last_button == 1)
					{
						selected_note = ni;
						Point loc = ni.getLocation();
						previous_note_location = new Point(loc.x, loc.y);
						noteoffset = new Point(real.x - loc.x, real.y - loc.y);
						// System.out.println("We hit a note!!");
					}
					else
					{
						selrect = new Rectangle(real.x, real.y, 0, 0);
					}
				}
				redraw();
			}

			public void mouseUp(MouseEvent e) 
			{
                boolean control = (e.stateMask & SWT.CONTROL) != 0;

				if (iconoffset==null) iconoffset=new Point(0,0);
				Point real = screen2real(e.x, e.y);
				Point icon = new Point(real.x - iconoffset.x, real.y - iconoffset.y);

				// See if we need to add a hop...
				if (hop_candidate != null) 
				{
					// hop doesn't already exist
					if (jobMeta.findJobHop(hop_candidate.from_entry, hop_candidate.to_entry) == null) 
					{
						if (!hop_candidate.from_entry.evaluates() && hop_candidate.from_entry.isUnconditional())
						{
							hop_candidate.setUnconditional();
						}
						else
						{
							hop_candidate.setConditional();
							int nr = jobMeta.findNrNextChefGraphEntries(hop_candidate.from_entry);
	
							// If there is one green link: make this one red! (or vice-versa)
							if (nr == 1) 
							{
								JobEntryCopy jge = jobMeta.findNextChefGraphEntry(hop_candidate.from_entry, 0);
								JobHopMeta other = jobMeta.findJobHop(hop_candidate.from_entry, jge);
								if (other != null) 
								{
									hop_candidate.setEvaluation(!other.getEvaluation());
								}
							}
						}
						
						jobMeta.addJobHop(hop_candidate);
						spoon.addUndoNew(jobMeta, new JobHopMeta[] { hop_candidate }, new int[] { jobMeta.indexOfJobHop(hop_candidate) } );
						spoon.refreshTree();
					}
					hop_candidate = null;
					selected_entries = null;
					last_button = 0;
					redraw();
				} 
				
				// Did we select a region on the screen?  
				else if (selrect != null) 
				{
					selrect.width  = real.x - selrect.x;
					selrect.height = real.y - selrect.y;

					jobMeta.unselectAll();
					jobMeta.selectInRect(selrect);
					selrect = null;
					redraw();
				}
				
				// Clicked on an icon?
				//
				else if (selected_icon != null)
				{
					if (e.button == 1)
					{
						if (lastclick.x == real.x && lastclick.y == real.y)
						{
							// Flip selection when control is pressed!
							if (control)
							{
								selected_icon.flipSelected();
							}
							else
							{
								// Otherwise, select only the icon clicked on!
								jobMeta.unselectAll();
								selected_icon.setSelected(true);
							}
						}
						else // We moved around some items: store undo info...
						if (selected_entries != null && prev_locations != null)
						{
							int indexes[] = jobMeta.getEntryIndexes(selected_entries); 
							spoon.addUndoPosition(jobMeta, selected_entries, indexes, prev_locations, jobMeta.getSelectedLocations());
						}
					}

					// OK, we moved the step, did we move it across a hop?
					// If so, ask to split the hop!
					if (split_hop)
					{
						JobHopMeta hi = findJobHop(icon.x + iconsize / 2, icon.y + iconsize / 2);
						if (hi != null)
						{
							int id = 0;
							if (!spoon.props.getAutoSplit())
							{
								MessageDialogWithToggle md = new MessageDialogWithToggle(shell, 
            						 Messages.getString("ChefGraph.Dialog.SplitHop.Title"),
            						 null,
            						 Messages.getString("ChefGraph.Dialog.SplitHop.Message")+Const.CR+hi.from_entry.getName()+" --> "+hi.to_entry.getName(),
            						 MessageDialog.QUESTION,
            						 new String[] { Messages.getString("System.Button.Yes"), Messages.getString("System.Button.No") },
            						 0,
            						 Messages.getString("ChefGraph.Dialog.SplitHop.Toggle"),
            						 spoon.props.getAutoSplit()
            						 );
								id = md.open();
								spoon.props.setAutoSplit(md.getToggleState());
							}
							
							if ( (id&0xFF) == 0)
							{
								JobHopMeta newhop1 = new JobHopMeta(hi.from_entry, selected_icon);
								jobMeta.addJobHop(newhop1);
								JobHopMeta newhop2 = new JobHopMeta(selected_icon, hi.to_entry);
								jobMeta.addJobHop(newhop2);
								if (!selected_icon.evaluates()) newhop2.setUnconditional();

								spoon.addUndoNew(jobMeta, new JobHopMeta[] { (JobHopMeta)newhop1.clone(), (JobHopMeta)newhop2.clone() }, new int[] { jobMeta.indexOfJobHop(newhop1), jobMeta.indexOfJobHop(newhop2)});
								int idx = jobMeta.indexOfJobHop(hi);
								spoon.addUndoDelete(jobMeta, new JobHopMeta[] { (JobHopMeta)hi.clone() }, new int[] { idx });
								jobMeta.removeJobHop(idx);
								spoon.refreshTree();

							}
						}
						split_hop = false;
					}

					selected_entries = null;
					redraw();
				}
 
				// Notes?
				else if (selected_note != null)
				{
					Point note = new Point(real.x - noteoffset.x, real.y - noteoffset.y);
					if (last_button == 1)
					{
						if (lastclick.x != real.x || lastclick.y != real.y)
						{
							int indexes[] = new int[] { jobMeta.indexOfNote(selected_note) };
							spoon.addUndoPosition(jobMeta, new NotePadMeta[] { selected_note }, indexes, new Point[] { previous_note_location }, new Point[] { note });
						}
					}
					selected_note = null;
				}
			}
		});

		canvas.addMouseMoveListener(new MouseMoveListener() 
		{
			public void mouseMove(MouseEvent e) 
			{
                boolean shift = (e.stateMask & SWT.SHIFT) != 0;

                // Remember the last position of the mouse for paste with keyboard
                lastMove = new Point(e.x, e.y);

				if (iconoffset==null) iconoffset=new Point(0,0);
				Point real = screen2real(e.x, e.y);
				Point icon = new Point(real.x - iconoffset.x, real.y - iconoffset.y);

				setToolTip(real.x, real.y);

				// First see if the icon we clicked on was selected.
				// If the icon was not selected, we should unselect all other icons,
				// selected and move only the one icon
				if (selected_icon != null && !selected_icon.isSelected())
				{
					jobMeta.unselectAll();
					selected_icon.setSelected(true);
					selected_entries = new JobEntryCopy[] { selected_icon };
					prev_locations = new Point[] { selected_icon.getLocation()};
				}
                
				// Did we select a region...?
				if (selrect != null) 
				{
					selrect.width = real.x - selrect.x;
					selrect.height = real.y - selrect.y;
					redraw();
				} 
				else
				
				// Or just one entry on the screen?
				if (selected_entries != null) 
				{
					if (last_button == 1 && !shift) 
					{
						/*
						 * One or more icons are selected and moved around...
						 * 
						 * new : new position of the ICON (not the mouse pointer)
						 * dx  : difference with previous position
						 */
						int dx = icon.x - selected_icon.getLocation().x;
						int dy = icon.y - selected_icon.getLocation().y;

						JobHopMeta hi =findJobHop(icon.x+iconsize/2, icon.y+iconsize/2);
						if (hi != null) 
						{
							//log.logBasic("MouseMove", "Split hop candidate B!");
							if (!jobMeta.isEntryUsedInHops(selected_icon)) 
							{
								//log.logBasic("MouseMove", "Split hop candidate A!");
								split_hop = true;
								last_hop_split = hi;
								hi.setSplit(true);
							}
						} 
						else 
						{
							if (last_hop_split != null) 
							{
								last_hop_split.setSplit(false);
								last_hop_split = null;
								split_hop = false;
							}
						}

						//
						// One or more job entries are being moved around!
						//
						for (int i = 0; i < jobMeta.nrJobEntries(); i++) 
						{
							JobEntryCopy je = jobMeta.getJobEntry(i);
							if (je.isSelected()) 
							{
								je.setLocation(je.getLocation().x + dx, je.getLocation().y + dy);

							}
						}
						// selected_icon.setLocation(icon.x, icon.y);

						redraw();
					} 
					else
                    //	The middle button perhaps?
					if (last_button == 2 || (last_button == 1 && shift))	
					{
						JobEntryCopy si = jobMeta.getChefGraphEntry(real.x, real.y, iconsize);
						if (si != null && !selected_icon.equals(si)) 
						{
							if (hop_candidate == null) 
							{
								hop_candidate =	new JobHopMeta(selected_icon, si);
								redraw();
							}
						} 
						else 
						{
							if (hop_candidate != null) 
							{
								hop_candidate = null;
								redraw();
							}
						}
					}
				}
				else
				// are we moving a note around? 
				if (selected_note!=null)
				{
					if (last_button==1)
					{
						Point note = new Point(real.x - noteoffset.x, real.y - noteoffset.y);
						selected_note.setLocation(note.x, note.y);
						redraw();
						//spoon.refreshGraph();  removed in 2.4.1 (SB: defect #4862)
					}
				}
			}
		});

		// Drag & Drop for steps
		Transfer[] ttypes = new Transfer[] { XMLTransfer.getInstance() };
		DropTarget ddTarget = new DropTarget(canvas, DND.DROP_MOVE);
		ddTarget.setTransfer(ttypes);
		ddTarget.addDropListener(new DropTargetListener() 
		{
			public void dragEnter(DropTargetEvent event) 
			{
				drop_candidate = getRealPosition(canvas, event.x, event.y);
				redraw();
			}

			public void dragLeave(DropTargetEvent event) 
			{
				drop_candidate = null;
				redraw();
			}

			public void dragOperationChanged(DropTargetEvent event) 
			{
			}

			public void dragOver(DropTargetEvent event) 
			{
				drop_candidate = getRealPosition(canvas, event.x, event.y);
				redraw();
			}

			public void drop(DropTargetEvent event) 
			{
				// no data to copy, indicate failure in event.detail 
				if (event.data == null)
				{
					event.detail = DND.DROP_NONE;
					return;
				}

				Point p = getRealPosition(canvas, event.x, event.y);
				
                try
                {
                    DragAndDropContainer container = (DragAndDropContainer)event.data;
                    String entry = container.getData();
                    
                    switch(container.getType())
                    {
                    case DragAndDropContainer.TYPE_BASE_JOB_ENTRY: // Create a new Job Entry on the canvas
                        {
                            JobEntryCopy jge = spoon.newJobEntry(jobMeta, entry, false);
                            if (jge != null) 
                            {
                                jge.setLocation(p.x, p.y);
                                jge.setDrawn();
                                redraw();
                            }
                        }
                        break;
                    case DragAndDropContainer.TYPE_JOB_ENTRY: // Drag existing one onto the canvas
                        {
                            JobEntryCopy jge = jobMeta.findJobEntry(entry, 0, true);
                            if (jge != null)  // Create duplicate of existing entry 
                            {
                                // There can be only 1 start!
                                if (jge.isStart() && jge.isDrawn()) 
                                {
                                    showOnlyStartOnceMessage(shell);
                                    return;
                                }

                                boolean jge_changed=false;
                                
                                // For undo :
                                JobEntryCopy before = (JobEntryCopy)jge.clone_deep();
                                
                                JobEntryCopy newjge = jge;
                                if (jge.isDrawn()) 
                                {
                                    newjge = (JobEntryCopy)jge.clone();
                                    if (newjge!=null)
                                    {
                                        // newjge.setEntry(jge.getEntry());
                                        log.logDebug(toString(), "entry aft = "+((Object)jge.getEntry()).toString()); //$NON-NLS-1$
                                        
                                        newjge.setNr(jobMeta.findUnusedNr(newjge.getName()));
                                        
                                        jobMeta.addJobEntry(newjge);
                                        spoon.addUndoNew(jobMeta, new JobEntryCopy[] {newjge}, new int[] { jobMeta.indexOfJobEntry(newjge)} );
                                    }
                                    else
                                    {
                                        log.logDebug(toString(), "jge is not cloned!"); //$NON-NLS-1$
                                    }
                                }
                                else
                                {
                                    log.logDebug(toString(), jge.toString()+" is not drawn"); //$NON-NLS-1$
                                    jge_changed=true;
                                }
                                newjge.setLocation(p.x, p.y);
                                newjge.setDrawn();
                                if (jge_changed)
                                {
                                    spoon.addUndoChange(jobMeta, new JobEntryCopy[] { before }, new JobEntryCopy[] {newjge}, new int[] { jobMeta.indexOfJobEntry(newjge)});
                                }
                                redraw();
                                spoon.refreshTree();
                                log.logBasic("DropTargetEvent", "DROP "+newjge.toString()+"!, type="+ JobEntryCopy.getTypeDesc(newjge.getType()));
                            } 
                            else
                            {
                                log.logError(toString(), "Unknown job entry dropped onto the canvas.");
                            }
                        }
                        break;
                    default: break;
					}
				}
                catch(Exception e)
                {
                    new ErrorDialog(shell, Messages.getString("ChefGraph.Dialog.ErrorDroppingObject.Message"), Messages.getString("Chefraph.Dialog.ErrorDroppingObject.Title"), e);
                }
            }
            
			public void dropAccept(DropTargetEvent event) 
			{
				drop_candidate = null;
			}
		});

		// Keyboard shortcuts...
		canvas.addKeyListener(new KeyAdapter() 
		{
			public void keyPressed(KeyEvent e) 
			{
			    // F2 --> rename Job entry
			    if (e.keyCode == SWT.F2)
                {
                    renameJobEntry();
                }

                if (e.character == 3) // CTRL-C
                {
                    spoon.copyJobEntries(jobMeta, jobMeta.getSelectedEntries());
                }
                if (e.character == 22) // CTRL-V
                {
                    String clipcontent = spoon.fromClipboard();
                    if (clipcontent != null)
                    {
                        if (lastMove != null)
                        {
                            spoon.pasteXML(jobMeta, clipcontent, lastMove);
                        }
                    }

                    //spoon.pasteSteps( );
                }
				if (e.keyCode == SWT.ESC) 
				{
					jobMeta.unselectAll();
					redraw();
				}
                // Delete
                if (e.keyCode == SWT.DEL)
                {
                    JobEntryCopy copies[] = jobMeta.getSelectedEntries();
                    if (copies != null && copies.length > 0)
                    {
                        delSelected();
                    }
                }
                // CTRL-UP : allignTop();
                if (e.keyCode == SWT.ARROW_UP && (e.stateMask & SWT.CONTROL) != 0)
                {
                    alligntop();
                }
                // CTRL-DOWN : allignBottom();
                if (e.keyCode == SWT.ARROW_DOWN && (e.stateMask & SWT.CONTROL) != 0)
                {
                    allignbottom();
                }
                // CTRL-LEFT : allignleft();
                if (e.keyCode == SWT.ARROW_LEFT && (e.stateMask & SWT.CONTROL) != 0)
                {
                    allignleft();
                }
                // CTRL-RIGHT : allignRight();
                if (e.keyCode == SWT.ARROW_RIGHT && (e.stateMask & SWT.CONTROL) != 0)
                {
                    allignright();
                }
                // ALT-RIGHT : distributeHorizontal();
                if (e.keyCode == SWT.ARROW_RIGHT && (e.stateMask & SWT.ALT) != 0)
                {
                    distributehorizontal();
                }
                // ALT-UP : distributeVertical();
                if (e.keyCode == SWT.ARROW_UP && (e.stateMask & SWT.ALT) != 0)
                {
                    distributevertical();
                }
                // ALT-HOME : snap to grid
                if (e.keyCode == SWT.HOME && (e.stateMask & SWT.ALT) != 0)
                {
                    snaptogrid(Const.GRID_SIZE);
                }
			}
		});

		canvas.addKeyListener(spoon.defKeys);

		setBackground(GUIResource.getInstance().getColorBackground());
	}

    public void redraw()
    {
        canvas.redraw();
    }
    
    public boolean forceFocus()
    {
        return canvas.forceFocus();
    }
    
    public boolean setFocus()
    {
        return canvas.setFocus();
    }
	
    public void renameJobEntry()
    {
        JobEntryCopy[] selection = jobMeta.getSelectedEntries();
        if (selection!=null && selection.length==1)
        {
            final JobEntryCopy jobEntryMeta = selection[0];
            
            // What is the location of the step?
            final String name = jobEntryMeta.getName();
            Point stepLocation = jobEntryMeta.getLocation();
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
            fdText.right= new FormAttachment(0, namePosition.x+width);
            fdText.top  = new FormAttachment(0, namePosition.y);
            text.setLayoutData(fdText);
            
            // Add a listener!
            // Catch the keys pressed when editing a Text-field...
            KeyListener lsKeyText = new KeyAdapter() 
                {
                    public void keyPressed(KeyEvent e) 
                    {
                        // "ENTER": close the text editor and copy the data over 
                        if (e.character == SWT.CR) 
                        {
                            String newName = text.getText();
                            text.dispose();
                            if (!name.equals(newName))
                                renameJobEntry(jobEntryMeta, newName);
                        }
                            
                        if (e.keyCode   == SWT.ESC)
                        {
                            text.dispose();
                        }
                    }
                };

            text.addKeyListener(lsKeyText);
            text.addFocusListener(new FocusAdapter()
                {
                    public void focusLost(FocusEvent e)
                    {
                        String newName = text.getText();
                        text.dispose();
                        if (!name.equals(newName))
                            renameJobEntry(jobEntryMeta, newName);
                    }
                }
            );
            
            this.layout(true, true);
            
            text.setFocus();
            text.setSelection(0, name.length());
        }
    }
    
    /**
     * Method gets called, when the user wants to change a job entries name and he indeed entered
     * a different name then the old one. Make sure that no other job entry matches this name
     * and rename in case of uniqueness.
     * 
     * @param jobEntry
     * @param newName
     */
    public void renameJobEntry(JobEntryCopy jobEntry, String newName)
    {
        JobEntryCopy[] jobs = jobMeta.getAllChefGraphEntries(newName);
        if (jobs != null && jobs.length > 0)
        {
            MessageBox mb = new MessageBox(shell, SWT.OK | SWT.ICON_INFORMATION);
            mb.setMessage(Messages.getString("Chef.Dialog.JobEntryNameExists.Message", newName));
            mb.setText(Messages.getString("Chef.Dialog.JobEntryNameExists.Title"));
            mb.open();
        }
        else
        {
            jobEntry.setName(newName);
            jobEntry.setChanged();
            spoon.refreshTree(); // to reflect the new name
            spoon.refreshGraph();
        }
    }
    
	public static void showOnlyStartOnceMessage(Shell shell)
    {
        MessageBox mb = new MessageBox(shell, SWT.YES | SWT.ICON_ERROR);
        mb.setMessage(Messages.getString("ChefGraph.Dialog.OnlyUseStartOnce.Message"));
        mb.setText(Messages.getString("ChefGraph.Dialog.OnlyUseStartOnce.Title"));
        mb.open();
    }

    public void delSelected()
    {
        JobEntryCopy[] copies = jobMeta.getSelectedEntries();
        int nrsels = copies.length;
        
        if (nrsels==0) return;
        
        MessageBox mb = new MessageBox(shell, SWT.YES | SWT.NO | SWT.ICON_WARNING);
        mb.setText(Messages.getString("Chef.Dialog.DeletionConfirm.Title"));
        mb.setMessage(Messages.getString("Chef.Dialog.DeletionConfirm.Message", Integer.toString(nrsels)));
        int answer = mb.open();
        if (answer==SWT.YES)
        {
            for (int i=0;i<copies.length;i++)
            {
                spoon.deleteJobEntryCopies(jobMeta, copies[i]);
            }
            spoon.refreshTree();
            spoon.refreshGraph();
        }
    }

    public void clearSettings()
	{
		selected_icon = null;
		selected_note = null;
		selected_entries = null;
		selrect = null;
		hop_candidate = null;
		last_hop_split = null;
		last_button = 0;
		iconoffset = null;
		for (int i = 0; i < jobMeta.nrJobHops(); i++)
			jobMeta.getJobHop(i).setSplit(false);
	}


	public Point screen2real(int x, int y)
	{
		getOffset();
		Point real;
		if (offset != null)
		{
			real = new Point(x - offset.x, y - offset.y);
		}
		else
		{
			real = new Point(x, y);
		}

		return real;
	}

	public Point real2screen(int x, int y)
	{
		getOffset();
		Point screen = new Point(x+offset.x, y+offset.y);
				
		return screen;
	}

	public Point getRealPosition(Composite canvas, int x, int y) 
	{
		Point p = new Point(0, 0);
		Composite follow = canvas;
		while (follow != null) 
		{
			Point xy = new Point(follow.getLocation().x, follow.getLocation().y);
			p.x += xy.x;
			p.y += xy.y;
			follow = follow.getParent();
		}

		p.x = x - p.x - 8;
		p.y = y - p.y - 48;

		return screen2real(p.x, p.y);
	}

	// See if location (x,y) is on a line between two steps: the hop!
	// return the HopInfo if so, otherwise: null	
	private JobHopMeta findJobHop(int x, int y) 
	{
		int i;
		JobHopMeta online = null;
		for (i = 0; i < jobMeta.nrJobHops(); i++) 
		{
			JobHopMeta hi = jobMeta.getJobHop(i);

			int line[] = getLine(hi.from_entry, hi.to_entry);

			if (line!=null && pointOnLine(x, y, line)) online = hi;
		}
		return online;
	}

	private int[] getLine(JobEntryCopy fs, JobEntryCopy ts) 
	{
		if (fs==null || ts==null) return null;
		
		Point from = fs.getLocation();
		Point to = ts.getLocation();
		offset = getOffset();

		int x1 = from.x + iconsize / 2;
		int y1 = from.y + iconsize / 2;

		int x2 = to.x + iconsize / 2;
		int y2 = to.y + iconsize / 2;

		return new int[] { x1, y1, x2, y2 };
	}

	private void setMenu(int x, int y) 
	{
        final int mousex = x;
        final int mousey = y;        

        // Re-use the popup menu if it was allocated beforehand...
        if (mPop!=null && !mPop.isDisposed())
        {
            MenuItem[] items = mPop.getItems();
            for (int i = 0; i < items.length; i++)
            {
                items[i].dispose();
            }
        }
        else
        {
            mPop = new Menu(this);
        }
        
		final JobEntryCopy jobEntry = jobMeta.getChefGraphEntry(x, y, iconsize);
		if (jobEntry != null) // We clicked on a Job Entry!
		{
			MenuItem miNewHop = null;

			int sels = jobMeta.nrSelected();
			if (sels == 2) 
			{
				miNewHop = new MenuItem(mPop, SWT.CASCADE);
				miNewHop.setText(Messages.getString("ChefGraph.PopupMenu.JobEntry.NewHop"));
			}

			final JobEntryInterface entry = jobEntry.getEntry();

			switch(jobEntry.getType())
			{
			case JobEntryInterface.TYPE_JOBENTRY_TRANSFORMATION:
				{
					MenuItem miLaunch = new MenuItem(mPop, SWT.CASCADE);
					miLaunch.setText(Messages.getString("ChefGraph.PopupMenu.JobEntry.LaunchSpoon"));
							
					miLaunch.addSelectionListener(new SelectionAdapter() 
						{
							public void widgetSelected(SelectionEvent e) 
							{
								openTransformation((JobEntryTrans)entry);
							}
						}
					);
				}
				break;
			case JobEntryInterface.TYPE_JOBENTRY_JOB:
				{
					MenuItem miLaunch = new MenuItem(mPop, SWT.CASCADE);
					miLaunch.setText(Messages.getString("ChefGraph.PopupMenu.JobEntry.LaunchChef"));
				
					miLaunch.addSelectionListener(new SelectionAdapter() 
						{
							public void widgetSelected(SelectionEvent e) 
							{
								launchChef((JobEntryJob)entry);
							}
						}
					);
				}
				break;
			default: break;
			}
			MenuItem miEditStep = new MenuItem(mPop, SWT.CASCADE);
			miEditStep.setText(Messages.getString("ChefGraph.PopupMenu.JobEntry.Edit"));
			
			MenuItem miEditDesc = new MenuItem(mPop, SWT.CASCADE);
			miEditDesc.setText(Messages.getString("ChefGraph.PopupMenu.JobEntry.EditDescription"));
			
			new MenuItem(mPop, SWT.SEPARATOR);
			//----------------------------------------------------------
			
			MenuItem miDupeStep = new MenuItem(mPop, SWT.CASCADE);
			miDupeStep.setText(Messages.getString("ChefGraph.PopupMenu.JobEntry.Duplicate"));

			MenuItem miCopy = new MenuItem(mPop, SWT.CASCADE);
			miCopy.setText(Messages.getString("ChefGraph.PopupMenu.JobEntry.CopyToClipboard"));
			
			
            // Allign & Distribute options...
            new MenuItem(mPop, SWT.SEPARATOR);
            MenuItem miPopAD = new MenuItem(mPop, SWT.CASCADE);
            miPopAD.setText(Messages.getString("ChefGraph.PopupMenu.JobEntry.AllignDistribute"));

            Menu mPopAD = new Menu(miPopAD);
            MenuItem miPopALeft = new MenuItem(mPopAD, SWT.CASCADE);
            miPopALeft.setText(Messages.getString("ChefGraph.PopupMenu.JobEntry.AllignDistribute.Left"));
            MenuItem miPopARight = new MenuItem(mPopAD, SWT.CASCADE);
            miPopARight.setText(Messages.getString("ChefGraph.PopupMenu.JobEntry.AllignDistribute.Right"));
            MenuItem miPopATop = new MenuItem(mPopAD, SWT.CASCADE);
            miPopATop.setText(Messages.getString("ChefGraph.PopupMenu.JobEntry.AllignDistribute.Top"));
            MenuItem miPopABottom = new MenuItem(mPopAD, SWT.CASCADE);
            miPopABottom.setText(Messages.getString("ChefGraph.PopupMenu.JobEntry.AllignDistribute.Bottom"));
            new MenuItem(mPopAD, SWT.SEPARATOR);
            MenuItem miPopDHoriz = new MenuItem(mPopAD, SWT.CASCADE);
            miPopDHoriz.setText(Messages.getString("ChefGraph.PopupMenu.JobEntry.AllignDistribute.Horizontally"));
            MenuItem miPopDVertic = new MenuItem(mPopAD, SWT.CASCADE);
            miPopDVertic.setText(Messages.getString("ChefGraph.PopupMenu.JobEntry.AllignDistribute.Vertically"));
            new MenuItem(mPopAD, SWT.SEPARATOR);
            MenuItem miPopSSnap = new MenuItem(mPopAD, SWT.CASCADE);
            miPopSSnap.setText(Messages.getString("ChefGraph.PopupMenu.JobEntry.AllignDistribute.SnapToGrid") + Const.GRID_SIZE + ")\tALT-HOME");
            miPopAD.setMenu(mPopAD);

            miPopALeft.addSelectionListener(new SelectionAdapter()
            {
                public void widgetSelected(SelectionEvent e)
                {
                    allignleft();
                }
            });
            miPopARight.addSelectionListener(new SelectionAdapter()
            {
                public void widgetSelected(SelectionEvent e)
                {
                    allignright();
                }
            });
            miPopATop.addSelectionListener(new SelectionAdapter()
            {
                public void widgetSelected(SelectionEvent e)
                {
                    alligntop();
                }
            });
            miPopABottom.addSelectionListener(new SelectionAdapter()
            {
                public void widgetSelected(SelectionEvent e)
                {
                    allignbottom();
                }
            });
            miPopDHoriz.addSelectionListener(new SelectionAdapter()
            {
                public void widgetSelected(SelectionEvent e)
                {
                    distributehorizontal();
                }
            });
            miPopDVertic.addSelectionListener(new SelectionAdapter()
            {
                public void widgetSelected(SelectionEvent e)
                {
                    distributevertical();
                }
            });
            miPopSSnap.addSelectionListener(new SelectionAdapter()
            {
                public void widgetSelected(SelectionEvent e)
                {
                    snaptogrid(Const.GRID_SIZE);
                }
            });

            if (sels <= 1)
            {
                miPopAD.setEnabled(false);
            }

			
			if (sels == 2) 
			{
				miNewHop.addSelectionListener(new SelectionAdapter() 
				{
					public void widgetSelected(SelectionEvent e) 
					{
						selected_entries = null;
						newHop();
					}
				});
			}

			miEditStep.addSelectionListener(new SelectionAdapter() 
			{
				public void widgetSelected(SelectionEvent e) 
				{
					selected_entries = null;
					editEntry(jobEntry);
				}
			});
			miEditDesc.addSelectionListener(new SelectionAdapter() 
			{
				public void widgetSelected(SelectionEvent e) 
				{
					String title = Messages.getString("ChefGraph.Dialog.EditDescription.Title");
					String message = Messages.getString("ChefGraph.Dialog.EditDescription.Message");
					EnterTextDialog dd = new EnterTextDialog(shell, title, message, jobEntry.getDescription());
					String des = dd.open();
					if (des != null) jobEntry.setDescription(des);
				}
			});
			miDupeStep.addSelectionListener(new SelectionAdapter() 
			{
				public void widgetSelected(SelectionEvent e) 
				{
					spoon.dupeJobEntry(jobMeta, jobEntry);
				}
			});
			miCopy.addSelectionListener(new SelectionAdapter() 
			{
				public void widgetSelected(SelectionEvent e) 
				{
				    spoon.copyJobEntries(jobMeta, jobMeta.getSelectedEntries());
				}
			});

			if (jobMeta.isEntryUsedInHops(jobEntry))
			{
				new MenuItem(mPop, SWT.SEPARATOR);
				MenuItem miDetach = new MenuItem(mPop, SWT.CASCADE);
				miDetach.setText(Messages.getString("ChefGraph.PopupMenu.Hop.Detach"));
				miDetach.addSelectionListener(new SelectionAdapter()
				{
					public void widgetSelected(SelectionEvent e)
					{
						detach(jobEntry);
						jobMeta.unselectAll();
					}
				});
			}
			if (jobEntry.isDrawn() && !jobMeta.isEntryUsedInHops(jobEntry)) 
			{
				new MenuItem(mPop, SWT.SEPARATOR);
				MenuItem miHide = new MenuItem(mPop, SWT.CASCADE);
				miHide.setText(Messages.getString("ChefGraph.PopupMenu.JobEntry.Hide"));
				miHide.addSelectionListener(new SelectionAdapter() 
				{
					public void widgetSelected(SelectionEvent e) 
					{
						jobEntry.setDrawn(false);
						// nr > 1: delete
						if (jobEntry.getNr() > 0) 
						{
							int ind = jobMeta.indexOfJobEntry(jobEntry);
							jobMeta.removeJobEntry(ind);
							spoon.addUndoDelete(jobMeta, new JobEntryCopy[] {jobEntry}, new int[] {ind});
						}
						redraw();
					}
				});
			}
			if (jobEntry.isDrawn()) 
			{
				MenuItem miDelete = new MenuItem(mPop, SWT.CASCADE);
				miDelete.setText(Messages.getString("ChefGraph.PopupMenu.JobEntry.Delete"));
				miDelete.addSelectionListener(new SelectionAdapter() 
				{
					public void widgetSelected(SelectionEvent e) 
					{
						spoon.deleteJobEntryCopies(jobMeta, jobEntry);
						redraw();
					}
				});
			}
            canvas.setMenu(mPop);
		}
		else // Clear the menu
		{
			final JobHopMeta hi = findJobHop(x, y);
			if (hi != null) // We clicked on a HOP!
			{
				// Evaluation...
				MenuItem miPopEval = new MenuItem(mPop, SWT.CASCADE);
				miPopEval.setText(Messages.getString("ChefGraph.PopupMenu.Hop.Evaluation"));

                if (mPopAD!=null && !mPopAD.isDisposed())
                {
                    mPopAD.dispose();
                }
				mPopAD = new Menu(miPopEval);
				MenuItem miPopEvalUncond = new MenuItem(mPopAD, SWT.CASCADE | SWT.CHECK);
				miPopEvalUncond.setText(Messages.getString("ChefGraph.PopupMenu.Hop.Evaluation.Unconditional"));
				miPopEvalUncond.addSelectionListener(new SelectionAdapter() { public void widgetSelected(SelectionEvent arg0) {	hi.setUnconditional(); spoon.refreshGraph();}} );
				
				MenuItem miPopEvalTrue = new MenuItem(mPopAD, SWT.CASCADE | SWT.CHECK);
				miPopEvalTrue.setText(Messages.getString("ChefGraph.PopupMenu.Hop.Evaluation.FollowWhenOK"));
				miPopEvalTrue.addSelectionListener(new SelectionAdapter() { public void widgetSelected(SelectionEvent arg0) {	hi.setConditional(); hi.setEvaluation(true); spoon.refreshGraph(); }} );
				
				MenuItem miPopEvalFalse = new MenuItem(mPopAD, SWT.CASCADE | SWT.CHECK);
				miPopEvalFalse.setText(Messages.getString("ChefGraph.PopupMenu.Hop.Evaluation.FollowWhenFailed"));
				miPopEvalFalse.addSelectionListener(new SelectionAdapter() { public void widgetSelected(SelectionEvent arg0) {	hi.setConditional(); hi.setEvaluation(false); spoon.refreshGraph(); }} );

				if (hi.isUnconditional())
				{
					miPopEvalUncond.setSelection(true);
					miPopEvalTrue.setSelection(false);
					miPopEvalFalse.setSelection(false);
				}
				else
				{
					if (hi.getEvaluation())
					{
						miPopEvalUncond.setSelection(false);
						miPopEvalTrue.setSelection(true);
						miPopEvalFalse.setSelection(false);						
					}
					else
					{
						miPopEvalUncond.setSelection(false);
						miPopEvalTrue.setSelection(false);
						miPopEvalFalse.setSelection(true);
					}
				}

				if (!hi.from_entry.evaluates())
				{
					miPopEvalTrue.setEnabled(false);
					miPopEvalFalse.setEnabled(false);
				}
				if (!hi.from_entry.isUnconditional())
				{
					miPopEvalUncond.setEnabled(false);
				}

				miPopEval.setMenu(mPopAD);


				MenuItem miFlipHop = new MenuItem(mPop, SWT.CASCADE);
				miFlipHop.setText(Messages.getString("ChefGraph.PopupMenu.Hop.FlipDirection"));
				MenuItem miDisHop = new MenuItem(mPop, SWT.CASCADE);
				if (hi.isEnabled()) miDisHop.setText(Messages.getString("ChefGraph.PopupMenu.Hop.Disable"));
				else                miDisHop.setText(Messages.getString("ChefGraph.PopupMenu.Hop.Enable"));
				MenuItem miDelHop = new MenuItem(mPop, SWT.CASCADE);
				miDelHop.setText(Messages.getString("ChefGraph.PopupMenu.Hop.Delete"));

				
				miFlipHop.addSelectionListener(new SelectionAdapter() 
					{
						public void widgetSelected(SelectionEvent e) 
						{
							selrect = null;
							JobEntryCopy dummy = hi.from_entry;
							hi.from_entry = hi.to_entry;
							hi.to_entry = dummy;
	
							if (jobMeta.hasLoop(hi.from_entry)) 
							{
								spoon.refreshGraph();
								MessageBox mb = new MessageBox(shell, SWT.YES | SWT.ICON_WARNING);
								mb.setMessage(Messages.getString("ChefGraph.Dialog.HopFlipCausesLoop.Message"));
								mb.setText(Messages.getString("ChefGraph.Dialog.HopFlipCausesLoop.Title"));
								mb.open();
	
								dummy = hi.from_entry;
								hi.from_entry = hi.to_entry;
								hi.to_entry = dummy;
								spoon.refreshGraph();
							} 
							else 
							{
								hi.setChanged();
								spoon.refreshGraph();
								spoon.refreshTree();
								spoon.setShellText();
							}
						}
					}
				);
				miDisHop.addSelectionListener(new SelectionAdapter() 
					{
						public void widgetSelected(SelectionEvent e) 
						{
							selrect = null;
							hi.setEnabled(!hi.isEnabled());
							spoon.refreshGraph();
							spoon.refreshTree();
						}
					}
				);
				miDelHop.addSelectionListener(new SelectionAdapter() 
					{
						public void widgetSelected(SelectionEvent e) 
						{
							selrect = null;
							int idx = jobMeta.indexOfJobHop(hi);
							jobMeta.removeJobHop(idx);
							spoon.refreshTree();
							spoon.refreshGraph();
						}
					}
				);
                canvas.setMenu(mPop);
			}
			else 
			{
				// Clicked on the background: maybe we hit a note?
				final NotePadMeta ni = jobMeta.getNote(x, y);
				if (ni!=null)
				{
					// Delete note
					// Edit note
					MenuItem miNoteEdit = new MenuItem(mPop, SWT.CASCADE); miNoteEdit.setText(Messages.getString("ChefGraph.PopupMenu.Note.Edit"));
					MenuItem miNoteDel  = new MenuItem(mPop, SWT.CASCADE); miNoteDel .setText(Messages.getString("ChefGraph.PopupMenu.Note.Delete"));

					miNoteEdit.addSelectionListener(
						new SelectionAdapter() 
						{ 
							public void widgetSelected(SelectionEvent e) 
							{ 
								selrect=null;
								editNote(ni);
							} 
						} 
					);
					miNoteDel.addSelectionListener(
						new SelectionAdapter() 
						{ 
							public void widgetSelected(SelectionEvent e) 
							{ 
								selrect=null; 
								int idx = jobMeta.indexOfNote(ni);
								if (idx>=0) 
								{
									jobMeta.removeNote(idx);
									spoon.addUndoDelete(jobMeta, new NotePadMeta[] {ni}, new int[] {idx} );
								} 
								redraw();
							} 
						} 
					);
					
                    canvas.setMenu(mPop);
				}
				else
				{
					// New note
					MenuItem miNoteNew = new MenuItem(mPop, SWT.CASCADE); miNoteNew.setText(Messages.getString("ChefGraph.PopupMenu.Note.New"));
					miNoteNew.addSelectionListener(
						new SelectionAdapter() 
						{ 
							public void widgetSelected(SelectionEvent e) 
							{ 
								selrect=null;
								String title = Messages.getString("ChefGraph.Dialog.EditNote.Title");
								String message = Messages.getString("ChefGraph.Dialog.EditNote.Message");
								EnterTextDialog dd = new EnterTextDialog(shell, title, message, "");
								String n = dd.open();
								if (n!=null) 
								{
									NotePadMeta npi = new NotePadMeta(n, lastclick.x, lastclick.y, Const.NOTE_MIN_SIZE, Const.NOTE_MIN_SIZE);
									jobMeta.addNote(npi);
									spoon.addUndoNew(jobMeta, new NotePadMeta[] {npi}, new int[] { jobMeta.indexOfNote(npi)} );
									redraw();
								} 
							} 
						} 
					);

                    MenuItem miPasteStep = new MenuItem(mPop, SWT.CASCADE);
                    miPasteStep.setText(Messages.getString("ChefGraph.PopupMenu.PasteStepFromClipboard"));

                    final String clipcontent = spoon.fromClipboard();
                    if (clipcontent == null) miPasteStep.setEnabled(false);
                    // Past steps on the clipboard to the transformation...
                    miPasteStep.addSelectionListener(new SelectionAdapter()
                    {
                        public void widgetSelected(SelectionEvent e)
                        {
                            Point loc = new Point(mousex, mousey);
                            spoon.pasteXML(jobMeta, clipcontent, loc);
                        }
                    });

                    // Transformation settings
                    new MenuItem(mPop, SWT.SEPARATOR);
                    MenuItem miSettings = new MenuItem(mPop, SWT.NONE);
                    miSettings.setText(Messages.getString("ChefGraph.PopupMenu.Settings"));
                    miSettings.addSelectionListener(new SelectionAdapter()
                    {
                        public void widgetSelected(SelectionEvent e)
                        {
                            spoon.editJobProperties(jobMeta);
                        }
                    });

                    canvas.setMenu(mPop);
				}
			}
		}
	}

	private void setToolTip(int x, int y) 
	{
        String newTip=null;
        
		final JobEntryCopy je = jobMeta.getChefGraphEntry(x, y, iconsize);
		if (je != null && je.isDrawn()) // We hover above a Step!
		{
			// Set the tooltip!
			String desc = je.getDescription();
			if (desc != null) 
			{
				int le = desc.length() >= 200 ? 200 : desc.length();
				newTip = desc.substring(0, le);
			} 
			else 
			{
				newTip = je.toString();
			}
		} 
		else 
		{
			offset = getOffset();
			JobHopMeta hi = findJobHop(x + offset.x, y + offset.x);
			if (hi != null) 
			{
				newTip=hi.toString();
			} 
			else 
			{
				newTip=null;
			}
		}
        
        if (newTip==null || !newTip.equalsIgnoreCase(getToolTipText())) 
        {
            canvas.setToolTipText(newTip);
        }
	}
	
	public void launchStuff(JobEntryCopy jobentry)
	{
		if (jobentry.getType()==JobEntryInterface.TYPE_JOBENTRY_JOB)
		{
			final JobEntryJob entry = (JobEntryJob)jobentry.getEntry();
			if ( ( entry!=null && entry.getFilename()!=null && spoon.rep==null) ||
			     ( entry!=null && entry.getName()!=null && spoon.rep!=null)
			   )
			{
				launchChef(entry);
			}
		}
		else
		if (jobentry.getType()==JobEntryInterface.TYPE_JOBENTRY_TRANSFORMATION)
		{
			final JobEntryTrans entry = (JobEntryTrans)jobentry.getEntry();
			if ( ( entry!=null && entry.getFilename()!=null && spoon.rep==null) ||
			     ( entry!=null && entry.getName()!=null && spoon.rep!=null)
			   )
			{
				openTransformation(entry);
			}
		}
	}
	
	private void openTransformation(JobEntryTrans entry)
	{
        String exactFilename = StringUtil.environmentSubstitute(entry.getFilename() );
        String exactTransname = StringUtil.environmentSubstitute(entry.getTransname() );
        
        // check, whether a tab of this name is already opened
        CTabItem tab = spoon.findCTabItem(exactFilename, TabMapEntry.OBJECT_TYPE_TRANSFORMATION_GRAPH);
        if (tab == null)
        {
            tab = spoon.findCTabItem(Const.filenameOnly(exactFilename), TabMapEntry.OBJECT_TYPE_TRANSFORMATION_GRAPH);
        }
        if (tab != null)
        {
            spoon.tabfolder.setSelection(tab);
            return;
        }
        
		// Load from repository?
		if ( Const.isEmpty(exactFilename) && !Const.isEmpty(exactTransname) )
		{
			try
			{
				// New transformation?
				//
				long id = spoon.rep.getTransformationID(exactTransname, entry.getDirectory().getID());
                TransMeta newTrans;
				if (id<0) // New
				{
                    newTrans = new TransMeta(null, exactTransname, entry.arguments);
				}
				else
				{
                    newTrans = new TransMeta(spoon.rep, exactTransname, entry.getDirectory());
				}
                spoon.addSpoonGraph(newTrans);
				newTrans.clearChanged();
				spoon.open();
			}
			catch(Throwable e)
			{
                new ErrorDialog(shell, Messages.getString("ChefGraph.Dialog.ErrorLaunchingSpoonCanNotLoadTransformation.Title"), Messages.getString("ChefGraph.Dialog.ErrorLaunchingSpoonCanNotLoadTransformation.Message"), (Exception)e); 
			}
		}
		else
		{
			try
			{
                // only try to load if the file exists...
                if (Const.isEmpty(exactFilename))
                {
                    throw new Exception(Messages.getString("ChefGraph.Exception.NoFilenameSpecified"));
                }
                TransMeta launchTransMeta = null;
                if (KettleVFS.fileExists(exactFilename))
                {
                    launchTransMeta = new TransMeta( exactFilename ); 
                }
                else
                {
                    launchTransMeta = new TransMeta();
                }
                
				launchTransMeta.clearChanged();
                launchTransMeta.setFilename( exactFilename );
                spoon.addSpoonGraph( launchTransMeta );
				spoon.open();
			}
			catch(Throwable e)
			{
                new ErrorDialog(shell, Messages.getString("ChefGraph.Dialog.ErrorLaunchingSpoonCanNotLoadTransformationFromXML.Title"), Messages.getString("ChefGraph.Dialog.ErrorLaunchingSpoonCanNotLoadTransformationFromXML.Message"), (Exception)e);
			}

		}
	}

	public void launchChef(JobEntryJob entry)
	{
        String exactFilename = StringUtil.environmentSubstitute(entry.getFilename() );
        String exactJobname = StringUtil.environmentSubstitute(entry.getJobName() );
        
		// Load from repository?
		if ( Const.isEmpty(exactFilename) && !Const.isEmpty(exactJobname) )
		{
			try
			{
				JobMeta newJobMeta = new JobMeta(log, spoon.rep, exactJobname, entry.getDirectory());
				newJobMeta.clearChanged();
				spoon.addChefGraph(newJobMeta);
			}
			catch(Throwable e)
			{
                new ErrorDialog(shell, Messages.getString("ChefGraph.Dialog.ErrorLaunchingChefCanNotLoadJob.Title"), Messages.getString("ChefGraph.Dialog.ErrorLaunchingChefCanNotLoadJob.Message"), new Exception(e));
			}
		}
		else
		{
			try
			{
                if (Const.isEmpty(exactFilename))
                {
                    throw new Exception(Messages.getString("ChefGraph.Exception.NoFilenameSpecified"));
                }

                JobMeta newJobMeta;
                if (KettleVFS.fileExists(exactFilename))
                {
                    newJobMeta = new JobMeta(log, exactFilename, spoon.rep);
                }
                else
                {
                    newJobMeta = new JobMeta(log);
                }
                
				newJobMeta.setFilename( exactFilename );
                newJobMeta.clearChanged();
                spoon.addChefGraph(newJobMeta);
			}
			catch(Throwable e)
			{
                new ErrorDialog(shell, Messages.getString("ChefGraph.Dialog.ErrorLaunchingChefCanNotLoadJobFromXML.Title"), Messages.getString("ChefGraph.Dialog.ErrorLaunchingChefCanNotLoadJobFromXML.Message"), new Exception(e));
			}
		}
	}

	public void paintControl(PaintEvent e) 
	{
		Point area = getArea();
		if (area.x==0 || area.y==0) return; // nothing to do!

		Display disp = shell.getDisplay();
        if (disp.isDisposed()) return; // Nothing to do!
        
		Image img = new Image(disp, area.x, area.y);
		GC gc = new GC(img);
		drawJob(gc, Props.getInstance().isBrandingActive());
		e.gc.drawImage(img, 0, 0);
		gc.dispose();
		img.dispose();

		// spoon.setShellText();
	}
    
	public void drawJob(GC gc, boolean branded) 
	{
        if (spoon.props.isAntiAliasingEnabled()) gc.setAntialias(SWT.ON);
        
		shadowsize = spoon.props.getShadowSize();

		gc.setBackground(GUIResource.getInstance().getColorBackground());

		Point area = getArea();
		Point max = jobMeta.getMaximum();
		Point thumb = getThumb(area, max);
		offset = getOffset(thumb, area);

		hori.setThumb(thumb.x);
		vert.setThumb(thumb.y);

        if (branded)
        {
            Image gradient= GUIResource.getInstance().getImageBanner();
            gc.drawImage(gradient, 0, 0);

            Image logo = GUIResource.getInstance().getImageKettleLogo();
            org.eclipse.swt.graphics.Rectangle logoBounds = logo.getBounds();
            gc.drawImage(logo, 20, area.y-logoBounds.height);
        }
        
		// First draw the notes...
        gc.setFont(GUIResource.getInstance().getFontNote());

        for (int i = 0; i < jobMeta.nrNotes(); i++) 
		{
			NotePadMeta ni = jobMeta.getNote(i);
			drawNote(gc, ni);
		}
        
        gc.setFont(GUIResource.getInstance().getFontGraph());
		
		if (shadowsize>0)
		for (int j = 0; j < jobMeta.nrJobEntries(); j++)
		{
			JobEntryCopy cge = jobMeta.getJobEntry(j);
			drawChefGraphEntryShadow(gc, cge);
		}

		// ... and then the rest on top of it...
		for (int i = 0; i < jobMeta.nrJobHops(); i++) 
		{
			JobHopMeta hi = jobMeta.getJobHop(i);
			drawJobHop(gc, hi, false);
		}

		if (hop_candidate != null) 
		{
			drawJobHop(gc, hop_candidate, true);
		}

		for (int j = 0; j < jobMeta.nrJobEntries(); j++) 
		{
			JobEntryCopy je = jobMeta.getJobEntry(j);
			drawChefGraphEntry(gc, je);
		}

		if (drop_candidate != null)
		{
			gc.setLineStyle(SWT.LINE_SOLID);
			gc.setForeground(GUIResource.getInstance().getColorBlack());
			Point screen = real2screen(drop_candidate.x, drop_candidate.y);
			gc.drawRectangle(screen.x, screen.y, iconsize, iconsize);
		}

		drawRect(gc, selrect);
	}

	private void drawJobHop(GC gc, JobHopMeta hi, boolean candidate) 
	{
		if (hi==null || hi.from_entry==null || hi.to_entry==null) return;
		if (!hi.from_entry.isDrawn() || !hi.to_entry.isDrawn())	return;
		
		if (shadowsize>0) drawLineShadow(gc, hi);
		drawLine(gc, hi, candidate);
	}
	
	public Image getIcon(JobEntryCopy je)
	{
		Image im=null;
		if (je==null) return null;
		
		switch (je.getType()) 
		{
		case JobEntryInterface.TYPE_JOBENTRY_SPECIAL        :
			if (je.isStart()) im = GUIResource.getInstance().getImageStart();
			if (je.isDummy()) im = GUIResource.getInstance().getImageDummy();
			break;
		default:
            im = (Image)GUIResource.getInstance().getImagesJobentries().get(je.getTypeDesc());
		}
		return im;
	}

	private void drawChefGraphEntry(GC gc, JobEntryCopy je) 
	{
		if (!je.isDrawn()) return;

		Point pt = je.getLocation();

		int x, y;
		if (pt != null) 
		{
			x = pt.x;
			y = pt.y;
		} 
		else 
		{
			x = 50;
			y = 50;
		}
		String name = je.getName();
		if (je.isSelected()) gc.setLineWidth(3);
		else			     gc.setLineWidth(1);
		gc.setBackground(GUIResource.getInstance().getColorRed());
		gc.setForeground(GUIResource.getInstance().getColorBlack());
		gc.fillRectangle(offset.x + x, offset.y + y, iconsize, iconsize);
		Image im = getIcon(je);
		if (im != null) // Draw the icon!
		{
			Rectangle bounds = new Rectangle(im.getBounds().x, im.getBounds().y, im.getBounds().width, im.getBounds().height);
			gc.drawImage(im, 0, 0, bounds.width, bounds.height, offset.x + x, offset.y + y, iconsize, iconsize);
		}
		gc.setBackground(GUIResource.getInstance().getColorWhite());
		gc.drawRectangle(offset.x + x - 1, offset.y + y - 1, iconsize + 1, iconsize + 1);
		//gc.setXORMode(true);
		Point textsize = new Point(gc.textExtent(""+name).x, gc.textExtent(""+name).y);

		gc.setBackground(GUIResource.getInstance().getColorBackground());
		gc.setLineWidth(1);
		
		int xpos = offset.x + x + (iconsize / 2) - (textsize.x / 2);
		int ypos = offset.y + y + iconsize + 5;

		if (shadowsize>0)
		{
			gc.setForeground(GUIResource.getInstance().getColorLightGray());
			gc.drawText(""+name, xpos+shadowsize, ypos+shadowsize, SWT.DRAW_TRANSPARENT);
		}
		
		gc.setForeground(GUIResource.getInstance().getColorBlack());
		gc.drawText(name, xpos, ypos, true);

	}

	private void drawChefGraphEntryShadow(GC gc, JobEntryCopy je) 
	{
		if (je==null) return;
		if (!je.isDrawn()) return;
		
		Point pt = je.getLocation();

		int x, y;
		if (pt != null) { x = pt.x; y = pt.y; }	else { x = 50; y = 50; }

		Point screen = real2screen(x, y);

		// Draw the shadow...
		gc.setBackground(GUIResource.getInstance().getColorLightGray());
		gc.setForeground(GUIResource.getInstance().getColorLightGray());
		int s = shadowsize;
		gc.fillRectangle(screen.x + s, screen.y + s, iconsize, iconsize);
	}

	private void drawNote(GC gc, NotePadMeta ni)
	{
		int flags = SWT.DRAW_DELIMITER | SWT.DRAW_TAB | SWT.DRAW_TRANSPARENT;

        org.eclipse.swt.graphics.Point ext = gc.textExtent(ni.getNote(), flags); 
		Point p = new Point(ext.x, ext.y);
		Point loc = ni.getLocation();
		Point note = real2screen(loc.x, loc.y);
		int margin = Const.NOTE_MARGIN;
		p.x += 2 * margin;
		p.y += 2 * margin;
		int width = ni.width;
		int height = ni.height;
		if (p.x > width)
			width = p.x;
		if (p.y > height)
			height = p.y;


		int noteshape[] = new int[] { note.x, note.y, // Top left
			note.x + width + 2 * margin, note.y, // Top right
			note.x + width + 2 * margin, note.y + height, // bottom right 1
			note.x + width, note.y + height + 2 * margin, // bottom right 2
			note.x + width, note.y + height, // bottom right 3
			note.x + width + 2 * margin, note.y + height, // bottom right 1
			note.x + width, note.y + height + 2 * margin, // bottom right 2
			note.x, note.y + height + 2 * margin // bottom left
		};
		int s = spoon.props.getShadowSize();
		int shadow[] = new int[] { note.x+s, note.y+s, // Top left
			note.x + width + 2 * margin+s, note.y+s, // Top right
			note.x + width + 2 * margin+s, note.y + height+s, // bottom right 1
			note.x + width+s, note.y + height + 2 * margin+s, // bottom right 2
			note.x+s, note.y + height + 2 * margin+s // bottom left
		};

		gc.setForeground(GUIResource.getInstance().getColorLightGray());
		gc.setBackground(GUIResource.getInstance().getColorLightGray());
		gc.fillPolygon(shadow);
		
		gc.setForeground(GUIResource.getInstance().getColorGray());
		gc.setBackground(GUIResource.getInstance().getColorYellow());

		gc.fillPolygon(noteshape);
		gc.drawPolygon(noteshape);
		//gc.fillRectangle(ni.xloc, ni.yloc, width+2*margin, heigth+2*margin);
		//gc.drawRectangle(ni.xloc, ni.yloc, width+2*margin, heigth+2*margin);
		gc.setForeground(GUIResource.getInstance().getColorBlack());
		gc.drawText(ni.getNote(), note.x + margin, note.y + margin, flags);

		ni.width = width; // Save for the "mouse" later on...
		ni.height = height;
	}

	private void drawLine(GC gc, JobHopMeta hi, boolean is_candidate) 
	{
		int line[] = getLine(hi.from_entry, hi.to_entry);

		gc.setLineWidth(linewidth);
		Color col;

		if (is_candidate) 
		{
			col = GUIResource.getInstance().getColorBlue();
		}
		else 
		if (hi.isEnabled()) 
		{
			if (hi.isUnconditional())
			{
				col = GUIResource.getInstance().getColorBlack();
			}
			else
			{
				if (hi.getEvaluation()) 
				{
					col = GUIResource.getInstance().getColorGreen(); 
				}
				else 
				{
					col = GUIResource.getInstance().getColorRed();
				}
			}
		} 
		else 
		{
			col = GUIResource.getInstance().getColorGray();
		}

		gc.setForeground(col);

		if (hi.isSplit()) gc.setLineWidth(linewidth + 2);
		drawArrow(gc, line);
		if (hi.isSplit()) gc.setLineWidth(linewidth);

		gc.setForeground(GUIResource.getInstance().getColorBlack());
		gc.setBackground(GUIResource.getInstance().getColorBackground());
	}

	private void drawLineShadow(GC gc, JobHopMeta hi)
	{
		int line[] = getLine(hi.from_entry, hi.to_entry);
		int s = shadowsize;
		for (int i=0;i<line.length;i++) line[i]+=s;

		gc.setLineWidth(linewidth);
		
		gc.setForeground(GUIResource.getInstance().getColorLightGray());

		drawArrow(gc, line);
	}

	private Point getArea() 
	{
        org.eclipse.swt.graphics.Rectangle rect = canvas.getClientArea();
		Point area = new Point(rect.width, rect.height);

		return area;
	}

	private Point getThumb(Point area, Point max)
	{
		Point thumb = new Point(0, 0);
		if (max.x <= area.x) thumb.x = 100;
		else                 thumb.x = 100 * area.x / max.x;
		
		if (max.y <= area.y) thumb.y = 100;
		else                 thumb.y = 100 * area.y / max.y;

		return thumb;
	}

	private Point getOffset() 
	{
		Point area = getArea();
		Point max = jobMeta.getMaximum();
		Point thumb = getThumb(area, max);

		return getOffset(thumb, area);

	}
	
	private Point getOffset(Point thumb, Point area) 
	{
		Point p = new Point(0, 0);
		Point sel = new Point(hori.getSelection(), vert.getSelection());

		if (thumb.x==0 || thumb.y==0) return p;

		p.x = -sel.x * area.x / thumb.x;
		p.y = -sel.y * area.y / thumb.y;

		return p;
	}

	public int sign(int n) 
	{
		return n < 0 ? -1 : (n > 0 ? 1 : 1);
	}

	private void newHop() 
	{
		JobEntryCopy fr = jobMeta.getSelected(0);
		JobEntryCopy to = jobMeta.getSelected(1);
		spoon.newJobHop(jobMeta, fr, to);
	}

	private void editEntry(JobEntryCopy je) 
	{
		spoon.editJobEntry(jobMeta, je);
	}
	
	private void editNote(NotePadMeta ni)
	{	
		NotePadMeta before = (NotePadMeta)ni.clone();
		String title = Messages.getString("ChefGraph.Dialog.EditNote.Title");
		String message = Messages.getString("ChefGraph.Dialog.EditNote.Message");
		EnterTextDialog dd = new EnterTextDialog(shell, title, message, ni.getNote());
		String n = dd.open();
		if (n!=null) 
		{
			spoon.addUndoChange(jobMeta, new NotePadMeta[] {before}, new NotePadMeta[] {ni}, new int[] {jobMeta.indexOfNote(ni)});
			ni.setChanged();
			ni.setNote( n );
			ni.width = Const.NOTE_MIN_SIZE;
			ni.height = Const.NOTE_MIN_SIZE;
			spoon.refreshGraph();
		} 
	}

	private void drawArrow(GC gc, int line[]) 
	{
		int mx, my;
		int x1 = line[0] + offset.x;
		int y1 = line[1] + offset.y;
		int x2 = line[2] + offset.x;
		int y2 = line[3] + offset.y;
		int x3;
		int y3;
		int x4;
		int y4;
		int a, b, dist;
		double factor;
		double angle;

		//gc.setLineWidth(1);
		//WuLine(gc, black, x1, y1, x2, y2);
		
		
		gc.drawLine(x1, y1, x2, y2);

		// What's the distance between the 2 points?
		a = Math.abs(x2 - x1);
		b = Math.abs(y2 - y1);
		dist = (int) Math.sqrt(a * a + b * b);

		// determine factor (position of arrow to left side or right side 0-->100%)
		if (dist >= 2 * iconsize) factor = 1.5; else factor = 1.2;
		
		// in between 2 points
		mx = (int) (x1 + factor * (x2 - x1) / 2);
		my = (int) (y1 + factor * (y2 - y1) / 2);

		// calculate points for arrowhead
		angle = Math.atan2(y2 - y1, x2 - x1) + Math.PI;

		x3 = (int) (mx + Math.cos(angle - theta) * size);
		y3 = (int) (my + Math.sin(angle - theta) * size);

		x4 = (int) (mx + Math.cos(angle + theta) * size);
		y4 = (int) (my + Math.sin(angle + theta) * size);

		// draw arrowhead
		//gc.drawLine(mx, my, x3, y3);
		//gc.drawLine(mx, my, x4, y4);
		//gc.drawLine( x3, y3, x4, y4 );
		Color fore = gc.getForeground();
		Color back = gc.getBackground();
		gc.setBackground(fore);
		gc.fillPolygon(new int[] {mx, my, x3, y3, x4, y4} );
		gc.setBackground(back);
	}

	private boolean pointOnLine(int x, int y, int line[]) 
	{
		int dx, dy;
		int pm = HOP_SEL_MARGIN / 2;
		boolean retval = false;

		for (dx = -pm; dx <= pm && !retval; dx++) 
		{
			for (dy = -pm; dy <= pm && !retval; dy++) 
			{
				retval = pointOnThinLine(x + dx, y + dy, line);
			}
		}

		return retval;
	}

	private boolean pointOnThinLine(int x, int y, int line[]) 
	{
		int x1 = line[0];
		int y1 = line[1];
		int x2 = line[2];
		int y2 = line[3];

		// Not in the square formed by these 2 points: ignore!		
		if (!(((x >= x1 && x <= x2) || (x >= x2 && x <= x1))
	       && ((y >= y1 && y <= y2) || (y >= y2 && y <= y1)))
	       )
			return false;

		double angle_line = Math.atan2(y2 - y1, x2 - x1) + Math.PI;
		double angle_point = Math.atan2(y - y1, x - x1) + Math.PI;

		// Same angle, or close enough?
		if (angle_point >= angle_line - 0.01
			&& angle_point <= angle_line + 0.01)
			return true;

		return false;
	}

    private SnapAllignDistribute createSnapAllignDistribute()
    {
        List elements = jobMeta.getSelectedDrawnJobEntryList();
        int[] indices = jobMeta.getEntryIndexes((JobEntryCopy[])elements.toArray(new JobEntryCopy[elements.size()]));

        return new SnapAllignDistribute(jobMeta, elements, indices, spoon, this);
    }

    private void snaptogrid(int size)
    {
        createSnapAllignDistribute().snaptogrid(size);
    }

    private void allignleft()
    {
        createSnapAllignDistribute().allignleft();
    }

    private void allignright()
    {
        createSnapAllignDistribute().allignright();
    }

    private void alligntop()
    {
        createSnapAllignDistribute().alligntop();
    }

    private void allignbottom()
    {
        createSnapAllignDistribute().allignbottom();
    }

    private void distributehorizontal()
    {
        createSnapAllignDistribute().distributehorizontal();
    }

    public void distributevertical()
    {
        createSnapAllignDistribute().distributevertical();
    }

	private void drawRect(GC gc, Rectangle rect) 
	{
		if (rect == null) return;
		
		gc.setLineStyle(SWT.LINE_DASHDOT);
		gc.setLineWidth(1);
		gc.setForeground(GUIResource.getInstance().getColorDarkGray());
		gc.drawRectangle(rect.x + offset.x, rect.y + offset.y,rect.width, rect.height);
		gc.setLineStyle(SWT.LINE_SOLID);
	}

	private void detach(JobEntryCopy je)
	{
		JobHopMeta hfrom = jobMeta.findJobHopTo(je);
		JobHopMeta hto   = jobMeta.findJobHopFrom(je);

		if (hfrom != null && hto != null)
		{
			if (jobMeta.findJobHop(hfrom.from_entry, hto.to_entry) == null)
			{
				JobHopMeta hnew = new JobHopMeta(hfrom.from_entry, hto.to_entry);
				jobMeta.addJobHop(hnew);
				spoon.addUndoNew(jobMeta, new JobHopMeta[] { (JobHopMeta)hnew.clone() }, new int[] { jobMeta.indexOfJobHop(hnew)});
			}
		}
		if (hfrom != null)
		{
			int fromidx = jobMeta.indexOfJobHop(hfrom);
			if (fromidx >= 0)
			{
				jobMeta.removeJobHop(fromidx);
				spoon.addUndoDelete(jobMeta, new JobHopMeta[] {hfrom}, new int[] {fromidx} );
			}
		}
		if (hto != null)
		{
			int toidx = jobMeta.indexOfJobHop(hto);
			if (toidx >= 0)
			{
				jobMeta.removeJobHop(toidx);
				spoon.addUndoDelete(jobMeta, new JobHopMeta[] {hto}, new int[] {toidx} );
			}
		}
		spoon.refreshTree();
		redraw();
	}

	public void newProps()
	{
		iconsize = spoon.props.getIconSize();
		linewidth = spoon.props.getLineWidth();
	}
	
	public String toString()
	{
		return Chef.APP_NAME;
	}

    /**
     * @return the jobMeta
     */
    public JobMeta getJobMeta()
    {
        return jobMeta;
    }

    /**
     * @param jobMeta the jobMeta to set
     */
    public void setJobMeta(JobMeta jobMeta)
    {
        this.jobMeta = jobMeta;
    }

    public boolean applyChanges()
    {
        return spoon.saveJobFile(jobMeta);
    }

    public boolean canBeClosed()
    {
        return !jobMeta.hasChanged();
    }

    public Object getManagedObject()
    {
        return jobMeta;
    }

    public boolean hasContentChanged()
    {
        return jobMeta.hasChanged();
    }

    public int showChangedWarning()
    {
        MessageBox mb = new MessageBox(shell, SWT.YES | SWT.NO | SWT.CANCEL | SWT.ICON_WARNING );
        mb.setMessage(Messages.getString("Chef.Dialog.FileChangedSaveFirst.Message", spoon.makeJobGraphTabName(jobMeta)));//"This model has changed.  Do you want to save it?"
        mb.setText(Messages.getString("Chef.Dialog.FileChangedSaveFirst.Title"));
        return mb.open();
    }   
}