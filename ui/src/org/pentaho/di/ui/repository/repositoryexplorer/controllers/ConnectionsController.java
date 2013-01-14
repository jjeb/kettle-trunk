/*******************************************************************************
 *
 * Pentaho Data Integration
 *
 * Copyright (C) 2002-2012 by Pentaho : http://www.pentaho.com
 *
 *******************************************************************************
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 ******************************************************************************/

package org.pentaho.di.ui.repository.repositoryexplorer.controllers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.database.DatabaseMeta;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.repository.ObjectId;
import org.pentaho.di.repository.Repository;
import org.pentaho.di.ui.core.database.dialog.DatabaseDialog;
import org.pentaho.di.ui.core.dialog.ErrorDialog;
import org.pentaho.di.ui.repository.repositoryexplorer.ControllerInitializationException;
import org.pentaho.di.ui.repository.repositoryexplorer.IUISupportController;
import org.pentaho.di.ui.repository.repositoryexplorer.RepositoryExplorer;
import org.pentaho.di.ui.repository.repositoryexplorer.model.UIDatabaseConnection;
import org.pentaho.di.ui.repository.repositoryexplorer.model.UIDatabaseConnections;
import org.pentaho.ui.xul.binding.Binding;
import org.pentaho.ui.xul.binding.BindingFactory;
import org.pentaho.ui.xul.binding.DefaultBindingFactory;
import org.pentaho.ui.xul.components.XulButton;
import org.pentaho.ui.xul.containers.XulTree;
import org.pentaho.ui.xul.swt.tags.SwtDialog;

public class ConnectionsController extends LazilyInitializedController implements IUISupportController {

  private static Class<?> PKG = RepositoryExplorer.class; // for i18n purposes, needed by Translator2!!   $NON-NLS-1$

  private XulTree connectionsTable = null;

  protected BindingFactory bf = null;

  private boolean isRepReadOnly = true;

  private Binding bindButtonNew = null;

  private Binding bindButtonEdit = null;

  private Binding bindButtonRemove = null;

  private Shell shell = null;

  private UIDatabaseConnections dbConnectionList = new UIDatabaseConnections();

  private DatabaseDialog databaseDialog;

  public ConnectionsController() {
  }

  @Override
  public String getName() {
    return "connectionsController"; //$NON-NLS-1$
  }

  public void init(Repository repository) throws ControllerInitializationException {
    this.repository = repository;
  }

  private DatabaseDialog getDatabaseDialog() {
    if (databaseDialog != null) {
      return databaseDialog;
    }
    databaseDialog = new DatabaseDialog(shell);
    return databaseDialog;
  }

  private void createBindings() {
    refreshConnectionList();
    connectionsTable = (XulTree) document.getElementById("connections-table"); //$NON-NLS-1$

    // Bind the connection table to a list of connections
    bf.setBindingType(Binding.Type.ONE_WAY);

    try {
      bf.createBinding(dbConnectionList, "children", connectionsTable, "elements").fireSourceChanged(); //$NON-NLS-1$ //$NON-NLS-2$
      (bindButtonNew = bf.createBinding(this, "repReadOnly", "connections-new", "disabled")).fireSourceChanged(); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
      (bindButtonEdit = bf.createBinding(this, "repReadOnly", "connections-edit", "disabled")).fireSourceChanged(); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
      (bindButtonRemove = bf.createBinding(this, "repReadOnly", "connections-remove", "disabled")).fireSourceChanged(); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

      if (repository != null) {
        bf.createBinding(connectionsTable, "selectedItems", this, "enableButtons"); //$NON-NLS-1$ //$NON-NLS-2$
      }
    } catch (Exception ex) {
      // convert to runtime exception so it bubbles up through the UI
      throw new RuntimeException(ex);
    }
  }
  
  @Override
  protected boolean doLazyInit() {
    setRepReadOnly(this.repository.getRepositoryMeta().getRepositoryCapabilities().isReadOnly());

    // Load the SWT Shell from the explorer dialog
    shell = ((SwtDialog) document.getElementById("repository-explorer-dialog")).getShell(); //$NON-NLS-1$
    bf = new DefaultBindingFactory();
    bf.setDocument(this.getXulDomContainer().getDocumentRoot());

    if (bf != null) {
      createBindings();
    }
    enableButtons(true, false, false);
    return true;
  }

  public Repository getRepository() {
    return repository;
  }

  public void setRepReadOnly(boolean isRepReadOnly) {
    try {
      if (this.isRepReadOnly != isRepReadOnly) {
        this.isRepReadOnly = isRepReadOnly;

        if (initialized) {
          bindButtonNew.fireSourceChanged();
          bindButtonEdit.fireSourceChanged();
          bindButtonRemove.fireSourceChanged();
        }
      }
    } catch (Exception e) {
      // convert to runtime exception so it bubbles up through the UI
      throw new RuntimeException(e);
    }
  }

  public boolean isRepReadOnly() {
    return isRepReadOnly;
  }

  private void refreshConnectionList() {
    final List<UIDatabaseConnection> tmpList = new ArrayList<UIDatabaseConnection>();
    Runnable r = new Runnable() {
      public void run() {
        try {
          ObjectId[] dbIdList = repository.getDatabaseIDs(false);
          for (ObjectId dbId : dbIdList) {
            DatabaseMeta dbMeta = repository.loadDatabaseMeta(dbId, null);
            tmpList.add(new UIDatabaseConnection(dbMeta));
          }
        } catch (KettleException e) {
          // convert to runtime exception so it bubbles up through the UI
          throw new RuntimeException(e);
        }
      }
    };
    doWithBusyIndicator(r);
    dbConnectionList.setChildren(tmpList);
  }

  public void createConnection() {
    try {
      DatabaseMeta databaseMeta = new DatabaseMeta();
      databaseMeta.initializeVariablesFrom(null);
      getDatabaseDialog().setDatabaseMeta(databaseMeta);

      String dbName = getDatabaseDialog().open();
      if (dbName != null && !dbName.equals(""))//$NON-NLS-1$
      {
        // See if this user connection exists...
        ObjectId idDatabase = repository.getDatabaseID(dbName);
        if (idDatabase == null) {
          repository.insertLogEntry(BaseMessages.getString(PKG,
              "ConnectionsController.Message.CreatingDatabase", getDatabaseDialog().getDatabaseMeta().getName()));//$NON-NLS-1$
          repository.save(getDatabaseDialog().getDatabaseMeta(), Const.VERSION_COMMENT_INITIAL_VERSION, null);
        } else {
          MessageBox mb = new MessageBox(shell, SWT.ICON_ERROR | SWT.OK);
          mb
              .setMessage(BaseMessages.getString(PKG,
                  "RepositoryExplorerDialog.Connection.Create.AlreadyExists.Message")); //$NON-NLS-1$
          mb.setText(BaseMessages.getString(PKG, "RepositoryExplorerDialog.Connection.Create.AlreadyExists.Title")); //$NON-NLS-1$
          mb.open();
        }
      }
      //    We should be able to tell the difference between a cancel and an empty database name
      //
      //      else {
      //        MessageBox mb = new MessageBox(shell, SWT.ICON_ERROR | SWT.OK);
      //        mb.setMessage(BaseMessages.getString(PKG, "RepositoryExplorerDialog.Connection.Edit.MissingName.Message")); //$NON-NLS-1$
      //        mb.setText(BaseMessages.getString(PKG, "RepositoryExplorerDialog.Connection.Edit.MissingName.Title")); //$NON-NLS-1$
      //        mb.open();
      //      }
    } catch (KettleException e) {
      new ErrorDialog(shell, BaseMessages.getString(PKG,
          "RepositoryExplorerDialog.Connection.Create.UnexpectedError.Title"), //$NON-NLS-1$
          BaseMessages.getString(PKG, "RepositoryExplorerDialog.Connection.Create.UnexpectedError.Message"), e); //$NON-NLS-1$
    } finally {
      refreshConnectionList();
    }
  }

  public void editConnection() {
    try {
      Collection<UIDatabaseConnection> connections = connectionsTable.getSelectedItems();

      if (connections != null && !connections.isEmpty()) {
        // Grab the first item in the list & send it to the database dialog
        DatabaseMeta databaseMeta = ((UIDatabaseConnection) connections.toArray()[0]).getDatabaseMeta();

        // Make sure this connection already exists and store its id for updating
        ObjectId idDatabase = repository.getDatabaseID(databaseMeta.getName());
        if (idDatabase == null) {
          MessageBox mb = new MessageBox(shell, SWT.ICON_ERROR | SWT.OK);
          mb.setMessage(BaseMessages.getString(PKG, "RepositoryExplorerDialog.Connection.Edit.DoesNotExists.Message")); //$NON-NLS-1$
          mb.setText(BaseMessages.getString(PKG, "RepositoryExplorerDialog.Connection.Edit.DoesNotExists.Title")); //$NON-NLS-1$
          mb.open();
        } else {
          getDatabaseDialog().setDatabaseMeta(databaseMeta);
          String dbName = getDatabaseDialog().open();
          if (dbName != null && !dbName.equals("")) //$NON-NLS-1$
          {
            repository.insertLogEntry(BaseMessages.getString(PKG,
                "ConnectionsController.Message.UpdatingDatabase", databaseMeta.getName()));//$NON-NLS-1$
            repository.save(databaseMeta, Const.VERSION_COMMENT_EDIT_VERSION, null);
          }
          //          We should be able to tell the difference between a cancel and an empty database name 
          //          
          //          else {
          //            MessageBox mb = new MessageBox(shell, SWT.ICON_ERROR | SWT.OK);
          //            mb.setMessage(BaseMessages.getString(PKG, "RepositoryExplorerDialog.Connection.Edit.MissingName.Message")); //$NON-NLS-1$
          //            mb.setText(BaseMessages.getString(PKG, "RepositoryExplorerDialog.Connection.Edit.MissingName.Title")); //$NON-NLS-1$
          //            mb.open();            
          //          }
        }
      } else {
        MessageBox mb = new MessageBox(shell, SWT.ICON_ERROR | SWT.OK);
        mb.setMessage(BaseMessages.getString(PKG, "RepositoryExplorerDialog.Connection.Edit.NoItemSelected.Message")); //$NON-NLS-1$
        mb.setText(BaseMessages.getString(PKG, "RepositoryExplorerDialog.Connection.Edit.NoItemSelected.Title")); //$NON-NLS-1$
        mb.open();
      }
    } catch (KettleException e) {
      new ErrorDialog(shell, BaseMessages.getString(PKG,
          "RepositoryExplorerDialog.Connection.Create.UnexpectedError.Title"), //$NON-NLS-1$
          BaseMessages.getString(PKG, "RepositoryExplorerDialog.Connection.Edit.UnexpectedError.Message"), e); //$NON-NLS-1$
    } finally {
      refreshConnectionList();
    }
  }

  public void removeConnection() {
    try {
      Collection<UIDatabaseConnection> connections = connectionsTable.getSelectedItems();

      if (connections != null && !connections.isEmpty()) {
        for (Object obj : connections) {
          if (obj != null && obj instanceof UIDatabaseConnection) {
            UIDatabaseConnection connection = (UIDatabaseConnection) obj;

            DatabaseMeta databaseMeta = connection.getDatabaseMeta();

            // Make sure this connection already exists and store its id for updating
            ObjectId idDatabase = repository.getDatabaseID(databaseMeta.getName());
            if (idDatabase == null) {
              MessageBox mb = new MessageBox(shell, SWT.ICON_ERROR | SWT.OK);
              mb.setMessage(BaseMessages.getString(PKG,
                  "RepositoryExplorerDialog.Connection.Delete.DoesNotExists.Message", databaseMeta.getName())); //$NON-NLS-1$
              mb.setText(BaseMessages.getString(PKG, "RepositoryExplorerDialog.Connection.Delete.Title")); //$NON-NLS-1$
              mb.open();
            } else {
              repository.deleteDatabaseMeta(databaseMeta.getName());
            }
          }
        }
      } else {
        MessageBox mb = new MessageBox(shell, SWT.ICON_ERROR | SWT.OK);
        mb.setMessage(BaseMessages.getString(PKG, "RepositoryExplorerDialog.Connection.Edit.NoItemSelected.Message")); //$NON-NLS-1$
        mb.setText(BaseMessages.getString(PKG, "RepositoryExplorerDialog.Connection.Delete.Title")); //$NON-NLS-1$
        mb.open();
      }
    } catch (KettleException e) {
      new ErrorDialog(shell, BaseMessages.getString(PKG,
          "RepositoryExplorerDialog.Connection.Create.UnexpectedError.Title"), //$NON-NLS-1$
          BaseMessages.getString(PKG, "RepositoryExplorerDialog.Connection.Remove.UnexpectedError.Message"), e); //$NON-NLS-1$
    } finally {
      refreshConnectionList();
    }
  }

  public void setEnableButtons(List<UIDatabaseConnection> connections) {
    boolean enableEdit = false;
    boolean enableRemove = false;
    if(connections != null && connections.size() > 0) {
      enableRemove = true;
      if(connections.size() == 1) {
        enableEdit = true;
      }
    }
    // Convenience - Leave 'new' enabled, modify 'edit' and 'remove'
    enableButtons(true, enableEdit, enableRemove);
  }
  public void enableButtons(boolean enableNew, boolean enableEdit, boolean enableRemove) {
    XulButton bNew = (XulButton) document.getElementById("connections-new"); //$NON-NLS-1$
    XulButton bEdit = (XulButton) document.getElementById("connections-edit"); //$NON-NLS-1$
    XulButton bRemove = (XulButton) document.getElementById("connections-remove"); //$NON-NLS-1$

    bNew.setDisabled(!enableNew);
    bEdit.setDisabled(!enableEdit);
    bRemove.setDisabled(!enableRemove);
  }
  
  public void tabClicked() {
    lazyInit();
  }


}