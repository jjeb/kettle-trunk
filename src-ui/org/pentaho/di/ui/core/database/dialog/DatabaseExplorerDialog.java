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

import java.util.List;

import org.eclipse.swt.widgets.Shell;
import org.pentaho.di.core.database.DatabaseMeta;

/**
 * This class has been adapted to use the XUL version of the DatabaseExplorerDialog instead.
 * The old DatabaseExplorerDialog has been renamed to DatabaseExplorerDialogLegacy
 */
public class DatabaseExplorerDialog extends XulDatabaseExplorerDialog {

	public DatabaseExplorerDialog(Shell parent, int style, DatabaseMeta conn, List<DatabaseMeta> databases) {
		super(parent, conn, databases, false);
	}

	public DatabaseExplorerDialog(Shell parent, int style, DatabaseMeta conn, List<DatabaseMeta> databases, boolean aLook) {
		super(parent, conn, databases, aLook);
	}
}
/*
public class DatabaseExplorerDialog extends DatabaseExplorerDialogLegacy {

public DatabaseExplorerDialog(Shell parent, int style, DatabaseMeta conn, List<DatabaseMeta> databases) {
  super(parent, SWT.APPLICATION_MODAL | SWT.DIALOG_TRIM | SWT.RESIZE | SWT.MAX | SWT.MIN, conn, databases, false);
}

public DatabaseExplorerDialog(Shell parent, int style, DatabaseMeta conn, List<DatabaseMeta> databases, boolean aLook) {
  super(parent, SWT.APPLICATION_MODAL | SWT.DIALOG_TRIM | SWT.RESIZE | SWT.MAX | SWT.MIN, conn, databases, aLook);
}
}
*/