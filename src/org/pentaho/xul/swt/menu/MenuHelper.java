package org.pentaho.xul.swt.menu;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Shell;
import org.pentaho.di.core.xml.XMLHandler;
import org.pentaho.xul.Messages;
import org.pentaho.xul.swt.toolbar.Toolbar;
import org.pentaho.xul.swt.toolbar.ToolbarButton;
import org.pentaho.xul.swt.toolbar.ToolbarSeparator;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class MenuHelper {

	public static Toolbar createToolbarFromXul( Document doc, Shell shell, Messages xulMessages, Object caller ) {
		NodeList list = doc.getElementsByTagName( "toolbox" ); //$NON-NLS-1$
		Toolbar toolbar = null;
		if( list != null && list.getLength() > 0 ) {
			// we can only handle one menu bar
			Node toolboxNode = list.item( 0 );
			// get the top level menus
			Node toolbarNode = XMLHandler.getSubNode( toolboxNode, "toolbar" ); //$NON-NLS-1$
			while( toolbarNode != null ) {
				if( "toolbar".equals( toolbarNode.getNodeName() ) ) { //$NON-NLS-1$
			        	// create a top-level item
						String id = XMLHandler.getTagAttribute( toolbarNode, "id" ); //$NON-NLS-1$
				    	toolbar = new Toolbar(shell, id, null);
				    	
						Node buttonNode = toolbarNode.getFirstChild();
						while( buttonNode != null ) {
							if( "toolbarbutton".equals( buttonNode.getNodeName() ) ) { //$NON-NLS-1$
								String value = XMLHandler.getTagAttribute( buttonNode, "value" ); //$NON-NLS-1$
								String imagePath = XMLHandler.getTagAttribute( buttonNode, "image" ); //$NON-NLS-1$
								String hint = XMLHandler.getTagAttribute( buttonNode, "tooltiptext" ); //$NON-NLS-1$
								hint = localizeXulText( hint, xulMessages );
								ToolbarButton button = new ToolbarButton(shell, value, toolbar);
								button.setHint(hint);
//						        final Image image = ImageUtil.makeImageTransparent(shell.getDisplay(), 
//						        		new Image(shell.getDisplay(), caller.getClass().getResourceAsStream(imagePath)), new RGB(192, 192, 192)); 
								InputStream stream = caller.getClass().getResourceAsStream(imagePath);
								if( stream != null ) {
							        final Image image = new Image(shell.getDisplay(), stream); 
							        button.setImage(image);
								}

							}
							else if( "toolbarseparator".equals( buttonNode.getNodeName() ) ) { //$NON-NLS-1$
								new ToolbarSeparator(shell, toolbar);
							}
							buttonNode = buttonNode.getNextSibling();
						}
				}
				// see if there are more options
				toolbarNode = toolbarNode.getNextSibling();
			}
			return toolbar;
		}
		return null;
	}
	
    public static void createMenuFromXul( Menu menu, Node menuPopupNode, Messages xulMessages ) {
    	
		if( menuPopupNode == null ) {
			return;
		}
		Node menuItemNode = menuPopupNode.getFirstChild();
		while( menuItemNode != null ) {
			if( "menuitem".equals( menuItemNode.getNodeName() ) ) { //$NON-NLS-1$

				String id = XMLHandler.getTagAttribute( menuItemNode, "value" ); //$NON-NLS-1$
				String name = XMLHandler.getTagAttribute( menuItemNode, "label" ); //$NON-NLS-1$
				if( name == null ) {
					name = getMenuLabel( menuItemNode );
				}
				name = localizeXulText( name, xulMessages );
				String accessKey = XMLHandler.getTagAttribute( menuItemNode, "accesskey" ); //$NON-NLS-1$
				String accelText = XMLHandler.getTagAttribute( menuItemNode, "acceltext" ); //$NON-NLS-1$
				String typeName = XMLHandler.getTagAttribute( menuItemNode, "type" ); //$NON-NLS-1$
				int type = MenuChoice.TYPE_PLAIN;
				if( "checkbox".equals( typeName ) ) { //$NON-NLS-1$
					type = MenuChoice.TYPE_CHECKBOX;
				}
				
				if( accelText != null && accelText.startsWith( "%" ) ) { //$NON-NLS-1$
					// need to localize this in parts
					StringTokenizer tokenizer = new StringTokenizer( accelText, " " ); //$NON-NLS-1$
					StringBuffer buffer = new StringBuffer();
					while( tokenizer.hasMoreTokens() ) {
						String token = tokenizer.nextToken();
						if( token.startsWith( "%" ) ) { //$NON-NLS-1$
							buffer.append( xulMessages.getString( token.substring( 1 ) ) );
						} else {
							buffer.append( token );
						}
					}
					accelText = buffer.toString();
				}
	            new MenuChoice(menu, name, id, accelText, accessKey, type, xulMessages ); 
//	            menuBar.addMenuListener( "file-new", this, "newFile" ); //$NON-NLS-1$ //$NON-NLS-2$

			}
			else if( "menuseparator".equals( menuItemNode.getNodeName() ) ) { //$NON-NLS-1$
			String id = XMLHandler.getTagAttribute( menuItemNode, "value" ); //$NON-NLS-1$
				new MenuItemSeparator( menu, id );
			}
			else if( "menu".equals( menuItemNode.getNodeName() ) ) { //$NON-NLS-1$
        		// create a top-level item
			String id = XMLHandler.getTagAttribute( menuItemNode, "value" ); //$NON-NLS-1$
			String name = XMLHandler.getTagAttribute( menuItemNode, "label" ); //$NON-NLS-1$
			if( name == null ) {
				name = getMenuLabel( menuItemNode );
			}
			name = localizeXulText( name, xulMessages );
			String accessKey = XMLHandler.getTagAttribute( menuItemNode, "accesskey" ); //$NON-NLS-1$
        		Menu subMenu = new Menu(menu, name, id, accessKey ); 
        		// create a submenu if it exists
        		createMenuFromXul( subMenu, XMLHandler.getSubNode( menuItemNode, "menupopup" ), xulMessages ); //$NON-NLS-1$
		}
			menuItemNode = menuItemNode.getNextSibling();
		}
}

public static MenuBar createMenuBarFromXul( Document doc, Shell shell, Messages xulMessages ) {
	NodeList list = doc.getElementsByTagName( "menubar" ); //$NON-NLS-1$
	if( list != null && list.getLength() > 0 ) {
		MenuBar menuBar = new MenuBar( shell );
		// we can only handle one menu bar
		Node menubarNode = list.item( 0 );
		// get the top level menus
		Node menuNode = XMLHandler.getSubNode( menubarNode, "menu" ); //$NON-NLS-1$
		while( menuNode != null ) {
			if( "menu".equals( menuNode.getNodeName() ) ) { //$NON-NLS-1$
		        		// create a top-level item
					String id = XMLHandler.getTagAttribute( menuNode, "value" ); //$NON-NLS-1$
					String name = XMLHandler.getTagAttribute( menuNode, "label" ); //$NON-NLS-1$
					if( name == null ) {
						name = getMenuLabel( menuNode );
					}
					name = localizeXulText( name, xulMessages );
					String accessKey = XMLHandler.getTagAttribute( menuNode, "accesskey" ); //$NON-NLS-1$
		        		Menu menu = new Menu(menuBar, name, id, accessKey ); 
		        		// create a submenu if it exists
		        		createMenuFromXul( menu, XMLHandler.getSubNode( menuNode, "menupopup" ), xulMessages ); //$NON-NLS-1$
			}
			// see if there are more options
			menuNode = menuNode.getNextSibling();
		}
		return menuBar;
	}
	return null;
}

public static Map createPopupMenusFromXul( Document doc, Shell shell, Messages xulMessages, List menuIds ) {
	NodeList list = doc.getElementsByTagName( "popupset" ); //$NON-NLS-1$
	Map<String,Menu> map = new HashMap<String,Menu>();
	if( list != null && list.getLength() > 0 ) {
		Node popupSetNode = list.item( 0 );
		// get the top level menus
		Node menuNode = XMLHandler.getSubNode( popupSetNode, "menupopup" ); //$NON-NLS-1$
		while( menuNode != null ) {
			if( "menupopup".equals( menuNode.getNodeName() ) ) { //$NON-NLS-1$
		        		// create a top-level item
					String id = XMLHandler.getTagAttribute( menuNode, "id" ); //$NON-NLS-1$
					if( menuIds.contains( id ) ) {
		        			Menu menu = new PopupMenu(shell, id ); 
		        			map.put( id, menu );
		        			// create a submenu if it exists
		        			createMenuFromXul( menu, menuNode, xulMessages ); //$NON-NLS-1$
					}
			}
			// see if there are more options
			menuNode = menuNode.getNextSibling();
		}
	}
	return map;
}

public static Map createAllPopupMenusFromXul( Document doc, Shell shell, Messages xulMessages ) {
	NodeList list = doc.getElementsByTagName( "popupset" ); //$NON-NLS-1$
	Map<String,Menu> map = new HashMap<String,Menu>();
	if( list != null && list.getLength() > 0 ) {
		Node popupSetNode = list.item( 0 );
		// get the top level menus
		Node menuNode = XMLHandler.getSubNode( popupSetNode, "menupopup" ); //$NON-NLS-1$
		while( menuNode != null ) {
			if( "menupopup".equals( menuNode.getNodeName() ) ) { //$NON-NLS-1$
		        		// create a top-level item
					String id = XMLHandler.getTagAttribute( menuNode, "id" ); //$NON-NLS-1$
		        		Menu menu = new PopupMenu(shell, id ); 
		        		map.put( id, menu );
		        		// create a submenu if it exists
		        		createMenuFromXul( menu, menuNode, xulMessages ); //$NON-NLS-1$
			}
			// see if there are more options
			menuNode = menuNode.getNextSibling();
		}
	}
	return map;
}

public static String localizeXulText( String text, Messages messages ) {
    if( text.charAt( 0 ) == '%' ) {
    		text = messages.getString( text.substring( 1, text.length() ) );
    }
    return text;
}


private static String getMenuLabel( Node node ) {
	Node childNode = node.getFirstChild();
	while( childNode != null ) {

		if( "label".equals( childNode.getNodeName() ) ) { //$NON-NLS-1$
			return childNode.getTextContent();
		}
		// see if there are more options
		childNode = childNode.getNextSibling();
	}
	return null;
}

}
