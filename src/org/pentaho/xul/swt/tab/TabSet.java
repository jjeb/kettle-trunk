package org.pentaho.xul.swt.tab;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabFolder2Listener;
import org.eclipse.swt.custom.CTabFolderEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.Composite;

public class TabSet implements SelectionListener, CTabFolder2Listener {

	protected CTabFolder tabfolder;
    protected List<TabItem> tabList = new ArrayList<TabItem>();
    protected int selectedIndex = -1;
    protected Font changedFont;
    protected Font unchangedFont;
	private List<TabListener> listeners = new ArrayList<TabListener>();
    
	public TabSet( Composite parent ) {
		super();
        tabfolder= new CTabFolder(parent, SWT.MULTI);
        
        tabfolder.setSimple(false);
        tabfolder.setUnselectedImageVisible(true);
        tabfolder.setUnselectedCloseVisible(true);
        tabfolder.addSelectionListener( this );
        tabfolder.addCTabFolder2Listener( this );
	}
	
	public void widgetSelected( SelectionEvent event ) {
		for( int i=0; i<tabList.size(); i++ ) {
			if( event.item.equals( (tabList.get(i)).getSwtTabItem() ) ) {
				selectedIndex = i;
				notifySelectListeners( (tabList.get(i)) );
			}
		}
	}

	public void widgetDefaultSelected( SelectionEvent event ) {
		widgetSelected( event );
	}

	 public void close(CTabFolderEvent event) {
			for( int i=0; i<tabList.size(); i++ ) {
				if( event.item.equals( (tabList.get(i)).getSwtTabItem() ) ) {
					event.doit = notifyCloseListeners( (tabList.get(i)) );
				}
			}
	 }
	
	 public void maximize(CTabFolderEvent event) {
		 
	 }

	 public void minimize(CTabFolderEvent event) {
		 
	 }
	
	 public void showList(CTabFolderEvent event) {
		 
	 }
	
	 public void restore(CTabFolderEvent event) {
		 
	 }
	

	public void notifySelectListeners( TabItem item ) {
		for( int i=0; i<listeners.size(); i++ ) {
			(listeners.get(i)).tabSelected( item );
		}
	}

	public void notifyDeselectListeners( TabItem item ) {
		for( int i=0; i<listeners.size(); i++ ) {
			(listeners.get(i)).tabDeselected( item );
		}
	}

	public boolean notifyCloseListeners( TabItem item ) {
		boolean doit = item.notifyCloseListeners( );
		for( int i=0; i<listeners.size(); i++ ) {
			doit  &= (listeners.get(i)).tabClose( item );
		}
		return doit;
	}

	public CTabFolder getSwtTabset() {
		return tabfolder;
	}
	
	public void addTab( TabItem item ) {
		tabList.add( item );
	}
	
	public void addKeyListener( KeyAdapter keys ) {
        tabfolder.addKeyListener(keys);
	}
	
	public int getSelectedIndex() {
		return selectedIndex;
	}
	
	public TabItem getSelected() {
		if( selectedIndex >= 0 && selectedIndex < tabList.size() ) {
			return tabList.get( selectedIndex );
		}
		return null;
	}
	
	public int indexOf( TabItem item ) {

	    return tabList.indexOf(item); 

	}

	public void setSelected( int index ) {
		if( index >= 0 && index < tabList.size() ) {
		    tabfolder.setSelection( (tabList.get( index )).getSwtTabItem() );
			selectedIndex = index;
			notifySelectListeners( tabList.get( index ) );
		}
	}

	public void setSelected( TabItem item ) {
		selectedIndex = indexOf( item );
		if( selectedIndex != -1 ) {
			setSelected( selectedIndex );
		}
	}
    
	public void remove( TabItem item ) {
		tabList.remove( item );
		item.dispose();
	}

	public Font getChangedFont() {
		return changedFont;
	}

	public void setChangedFont(Font changedFont) {
		this.changedFont = changedFont;
	}

	public Font getUnchangedFont() {
		return unchangedFont;
	}

	public void setUnchangedFont(Font unchangedFont) {
		this.unchangedFont = unchangedFont;
	}
	
	public void addListener( TabListener listener ) {
		listeners.add( listener );
	}
	
	public void removeListener( TabListener listener ) {
		listeners.remove( listener );
	}

}
