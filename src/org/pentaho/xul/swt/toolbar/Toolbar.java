package org.pentaho.xul.swt.toolbar;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ToolBar;
import org.pentaho.xul.EventHandler;
import org.pentaho.xul.XulObject;
import org.pentaho.xul.toolbar.XulToolbar;
import org.pentaho.xul.toolbar.XulToolbarButton;
import org.pentaho.xul.toolbar.XulToolbox;

public class Toolbar extends XulObject implements XulToolbar {

	private ToolBar  toolBar;
	private XulToolbox parent;
    private EventHandler handler;
	private Map<String,XulToolbarButton> buttonMap = new HashMap<String,XulToolbarButton>();
	
	public Toolbar(Shell shell, String id, XulToolbox parent) {
		super( id, parent );
		handler = new EventHandler();
		toolBar = new ToolBar(shell, SWT.HORIZONTAL | SWT.FLAT );

        FormData fdBar = new FormData();
        fdBar.left = new FormAttachment(0, 0);
        fdBar.top = new FormAttachment(0, 0);
        toolBar.setLayoutData(fdBar);

	}

	public void addMenuListener(String id, Object listener, String methodName) {
		handler.addMenuListener(id, listener, methodName);
	}

	public void dispose() {
		toolBar.dispose();
	}

	public XulToolbarButton getButtonById(String id) {
		return buttonMap.get( id );
	}

	public String[] getButtonIds() {
		Set<String> keys = buttonMap.keySet();
		String ids[] = keys.toArray( new String[keys.size()] );
		return ids;
	}

	public Object getNativeObject() {
		return toolBar;
	}

	public XulToolbox getToolbox() {
		return parent;
	}

	public boolean isDisposed() {
		return toolBar.isDisposed();
	}

	public void register(XulToolbarButton item, String id, String accessKey) {
		handler.register(item, id, accessKey);
	}

	public boolean handleMenuEvent( String id ) {
		return handler.handleMenuEvent(id);
	}

	public void setEnableById(String id, boolean enabled) {
		XulToolbarButton button = getButtonById( id );
		if( button != null ) {
			button.setEnable(enabled);
		}
	}

	public void setHintById(String id, String text) {
		XulToolbarButton button = getButtonById( id );
		if( button != null ) {
			button.setHint(text);
		}
	}

	public void setTextById(String id, String text) {
		XulToolbarButton button = getButtonById( id );
		if( button != null ) {
			button.setText(text);
		}
	}

}
