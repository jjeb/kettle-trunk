/*
 * This program is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License, version 2.1 as published by the Free Software
 * Foundation.
 *
 * You should have received a copy of the GNU Lesser General Public License along with this
 * program; if not, you can obtain a copy at http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 * or from the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * Copyright (c) 2009 Pentaho Corporation..  All rights reserved.
 * 
 * Author: Ezequiel Cuellar
 */
package org.pentaho.di.ui.core.database.dialog;

import java.util.Enumeration;
import java.util.List;
import java.util.ResourceBundle;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.swt.widgets.Shell;
import org.pentaho.di.core.database.DatabaseMeta;
import org.pentaho.di.ui.spoon.SpoonPluginManager;
import org.pentaho.ui.xul.XulDomContainer;
import org.pentaho.ui.xul.XulOverlay;
import org.pentaho.ui.xul.XulRunner;
import org.pentaho.ui.xul.containers.XulDialog;
import org.pentaho.ui.xul.impl.XulEventHandler;
import org.pentaho.ui.xul.swt.SwtXulLoader;
import org.pentaho.ui.xul.swt.SwtXulRunner;
import org.pentaho.di.i18n.BaseMessages;

public class XulDatabaseExplorerDialog {

  private static final Class<?> PKG = XulDatabaseExplorerDialog.class;
  
	private Shell shell;
	private XulDomContainer container;
	private XulRunner runner;
	private XulDatabaseExplorerController controller;
	private DatabaseMeta databaseMeta;
	private List<DatabaseMeta> databases;
	private static Log logger = LogFactory.getLog(XulDatabaseExplorerDialog.class);
	private static final String XUL = "org/pentaho/di/ui/core/database/dialog/database_explorer.xul";

	public XulDatabaseExplorerDialog(Shell aShell, DatabaseMeta aDatabaseMeta, List<DatabaseMeta> aDataBases, boolean aLook) {
		this.shell = aShell;
		this.databaseMeta = aDatabaseMeta;
		this.databases = aDataBases;
		this.controller = new XulDatabaseExplorerController(this.shell, this.databaseMeta, this.databases, aLook);
	}

	public Object open() {
		try {

			SwtXulLoader theLoader = new SwtXulLoader();
			theLoader.setOuterContext(this.shell);

			this.container = theLoader.loadXul(XUL, new XulDatabaseExplorerResourceBundle());

			SpoonPluginManager theSpoonPluginManager = SpoonPluginManager.getInstance();
			List<XulOverlay> theXulOverlays = theSpoonPluginManager.getOverlaysforContainer("database_dialog");
			List<XulEventHandler> theXulEventHandlers = theSpoonPluginManager.getEventHandlersforContainer("database_dialog");

			for (XulEventHandler handler : theXulEventHandlers) {
				handler.setData(this.controller);
				this.container.addEventHandler(handler);
			}

			for (XulOverlay overlay : theXulOverlays) {
				this.container.loadOverlay(overlay.getOverlayUri());
			}

			this.container.addEventHandler(this.controller);

			this.runner = new SwtXulRunner();
			this.runner.addContainer(this.container);
			this.runner.initialize();

			XulDialog theExplorerDialog = (XulDialog) this.container.getDocumentRoot().getElementById("databaseExplorerDialog");
			theExplorerDialog.show();

		} catch (Exception e) {
			logger.info(e);
		}
		return this.controller.getSelectedTable();
	}

	public void setSelectedSchema(String aSchema) {
		this.controller.setSelectedSchema(aSchema);
	}

	public String getSchemaName() {
		return this.controller.getSelectedSchema();
	}

	public void setSelectedTable(String aTable) {
		this.controller.setSelectedTable(aTable);
	}

	public String getTableName() {
		return this.controller.getSelectedTable();
	}

	public void setSplitSchemaAndTable(boolean aSplit) {
		this.controller.setSplitSchemaAndTable(aSplit);
	}

	public boolean getSplitSchemaAndTable() {
		return this.controller.getSplitSchemaAndTable();
	}

	private static class XulDatabaseExplorerResourceBundle extends ResourceBundle {
		@Override
		public Enumeration<String> getKeys() {
			return null;
		}

		@Override
		protected Object handleGetObject(String key) {
			return BaseMessages.getString(PKG, key);
		}
	}
}
