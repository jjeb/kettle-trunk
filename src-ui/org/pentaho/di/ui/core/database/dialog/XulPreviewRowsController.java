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

import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.swt.widgets.Shell;
import org.pentaho.di.core.database.DatabaseMeta;
import org.pentaho.ui.xul.binding.Binding;
import org.pentaho.ui.xul.binding.BindingFactory;
import org.pentaho.ui.xul.binding.DefaultBindingFactory;
import org.pentaho.ui.xul.binding.Binding.Type;
import org.pentaho.ui.xul.containers.XulDialog;
import org.pentaho.ui.xul.containers.XulTree;
import org.pentaho.ui.xul.containers.XulTreeRow;
import org.pentaho.ui.xul.impl.AbstractXulEventHandler;
import org.pentaho.ui.xul.swt.tags.SwtTreeCell;
import org.pentaho.ui.xul.swt.tags.SwtTreeCol;
import org.pentaho.ui.xul.swt.tags.SwtTreeCols;

public class XulPreviewRowsController extends AbstractXulEventHandler {

	private Shell shell;
	private DatabaseMeta databaseMeta;
	private String table;
	private int limit;
	private BindingFactory bf;
	private Binding rowCountBinding;
	private String rowCount;

	private static Log logger = LogFactory.getLog(XulStepFieldsController.class);

	public XulPreviewRowsController(Shell aShell, DatabaseMeta aDatabaseMeta, String aTable, int aLimit) {
		this.shell = aShell;
		this.databaseMeta = aDatabaseMeta;
		this.table = aTable;
		this.limit = aLimit;
		this.bf = new DefaultBindingFactory();
	}

	public void init() {
		createPreviewRows();

		this.bf.setDocument(super.document);
		this.bf.setBindingType(Type.ONE_WAY);
		this.rowCountBinding = this.bf.createBinding(this, "rowCount", "rowCountLabel", "value");
		fireBindings();
	}

	private void fireBindings() {
		try {
			this.rowCountBinding.fireSourceChanged();
		} catch (Exception e) {
			logger.info(e);
		}
	}

	private void createPreviewRows() {
		GetPreviewTableProgressDialog theProgressDialog = new GetPreviewTableProgressDialog(this.shell, this.databaseMeta, this.table, this.limit);
		List<Object[]> thePreviewData = theProgressDialog.open();

		// Adds table rows.
		Object[] theObj = null;
		XulTreeRow theRow = null;
		Object theValue = null;
		SwtTreeCell theCell = null;
		int theRowCount = 0;

		XulTree thePreviewTable = (XulTree) super.document.getElementById("table_data");
		thePreviewTable.getRootChildren().removeAll();
		Iterator<Object[]> theItr = thePreviewData.iterator();
		while (theItr.hasNext()) {
			theObj = theItr.next();
			theRow = thePreviewTable.getRootChildren().addNewRow();
			theRowCount++;
			for (int i = 0; i < theObj.length; i++) {
				theValue = theObj[i];
				theCell = new SwtTreeCell(null);
				theCell.setLabel(theValue == null ? "" : theValue.toString());					
				theRow.addCell(theCell);
			}
		}

		// Adds table columns.
		SwtTreeCol theColumn = null;
		String[] theFieldNames = theProgressDialog.getRowMeta().getFieldNames();
		SwtTreeCols theColumns = new SwtTreeCols(null, thePreviewTable, null, null);
		for (int i = 0; i < theFieldNames.length; i++) {
			theColumn = new SwtTreeCol(null, null, null, null);
			theColumn.setLabel(theFieldNames[i]);
			theColumns.addColumn(theColumn);
		}
		thePreviewTable.setColumns(theColumns);
		thePreviewTable.update();

		setRowCount("Rows of step: " + this.table + " (" + theRowCount + " rows)");
	}

	public void accept() {
		XulDialog theDialog = (XulDialog) super.document.getElementById("previewRowsDialog");
		theDialog.setVisible(false);
	}

	public void setRowCount(String aRowCount) {
		this.rowCount = aRowCount;
	}

	public String getRowCount() {
		return this.rowCount;
	}

	public String getName() {
		return "previewRows";
	}
}
