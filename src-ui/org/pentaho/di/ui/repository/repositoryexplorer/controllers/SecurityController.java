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
 * Copyright (c) 2009 Pentaho Corporation.  All rights reserved.
 */
package org.pentaho.di.ui.repository.repositoryexplorer.controllers;

import java.util.Enumeration;
import java.util.ResourceBundle;

import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.repository.IRepositoryService;
import org.pentaho.di.repository.ObjectRecipient;
import org.pentaho.di.repository.Repository;
import org.pentaho.di.repository.RepositorySecurityManager;
import org.pentaho.di.ui.repository.repositoryexplorer.ControllerInitializationException;
import org.pentaho.di.ui.repository.repositoryexplorer.IUISupportController;
import org.pentaho.di.ui.repository.repositoryexplorer.RepositoryExplorer;
import org.pentaho.di.ui.repository.repositoryexplorer.model.IUIUser;
import org.pentaho.di.ui.repository.repositoryexplorer.model.UIObjectRegistery;
import org.pentaho.di.ui.repository.repositoryexplorer.model.UISecurity;
import org.pentaho.di.ui.repository.repositoryexplorer.model.UISecurityUser;
import org.pentaho.di.ui.repository.repositoryexplorer.model.UISecurity.Mode;
import org.pentaho.ui.xul.XulComponent;
import org.pentaho.ui.xul.binding.Binding;
import org.pentaho.ui.xul.binding.BindingConvertor;
import org.pentaho.ui.xul.binding.BindingFactory;
import org.pentaho.ui.xul.binding.DefaultBindingFactory;
import org.pentaho.ui.xul.components.XulButton;
import org.pentaho.ui.xul.components.XulConfirmBox;
import org.pentaho.ui.xul.components.XulMessageBox;
import org.pentaho.ui.xul.components.XulTextbox;
import org.pentaho.ui.xul.containers.XulDialog;
import org.pentaho.ui.xul.containers.XulListbox;
import org.pentaho.ui.xul.containers.XulTree;
import org.pentaho.ui.xul.impl.AbstractXulEventHandler;
import org.pentaho.ui.xul.util.XulDialogCallback;

/**
 *
 * This is the XulEventHandler for the browse panel of the repository explorer. It sets up the bindings for  
 * browse functionality.
 * 
 */
public class SecurityController extends AbstractXulEventHandler  implements IUISupportController {

  private ResourceBundle messages = new ResourceBundle() {

    @Override
    public Enumeration<String> getKeys() {
      return null;
    }

    @Override
    protected Object handleGetObject(String key) {
      return BaseMessages.getString(RepositoryExplorer.class, key);
    }
    
  };  

  public static final int USER_DECK = 0;

  protected XulDialog userDialog;

  private XulListbox userListBox;

  private XulTextbox username;

  private XulTextbox userPassword;

  private XulTextbox userDescription;

  private XulButton userEditButton;

  private XulButton userRemoveButton;

  private RepositorySecurityManager service;
  
  protected UISecurity security;

  protected BindingFactory bf;

  protected UISecurityUser securityUser;

  protected XulConfirmBox confirmBox = null;

  protected XulMessageBox messageBox = null;
  
  public SecurityController() {
  }

  public void init(Repository rep) throws ControllerInitializationException{
    try {
      // Get the service from the repository
      if(rep != null && rep.hasService(RepositorySecurityManager.class)) {
        service = (RepositorySecurityManager) rep.getService(RepositorySecurityManager.class);
      } else {
        throw new ControllerInitializationException(BaseMessages.getString(RepositoryExplorer.class, "SecurityController.ERROR_0001_UNABLE_TO_INITIAL_REPOSITORY_SERVICE", RepositorySecurityManager.class)); //$NON-NLS-1$
      }
      createModel();
      confirmBox = (XulConfirmBox) document.createElement("confirmbox");//$NON-NLS-1$
      messageBox = (XulMessageBox) document.createElement("messagebox");//$NON-NLS-1$
      bf = new DefaultBindingFactory();
      bf.setDocument(this.getXulDomContainer().getDocumentRoot());
    } catch (Exception e) {
      throw new ControllerInitializationException(e);
    }
    if (bf != null) {
      createBindings();
    }
    setInitialDeck();
  }

  protected void setInitialDeck() {
    changeToUserDeck();
  }

  public XulConfirmBox getConfirmBox() {
    return confirmBox;
  }

  public void setConfirmBox(XulConfirmBox confirmBox) {
    this.confirmBox = confirmBox;
  }

  public XulMessageBox getMessageBox() {
    return messageBox;
  }

  public void setMessageBox(XulMessageBox messageBox) {
    this.messageBox = messageBox;
  }
  protected void createModel()  throws Exception{
      createSecurityUser();
      createSecurity();
  }

  protected void createSecurityUser() throws Exception{
    securityUser = new UISecurityUser(service);    
  }
  protected void createSecurity()  throws Exception {
    security = new UISecurity(service);    
  }
  protected void createBindings() {
    //User Details Binding

    userEditButton = (XulButton) document.getElementById("user-edit");//$NON-NLS-1$
    userRemoveButton = (XulButton) document.getElementById("user-remove");//$NON-NLS-1$
    userDialog = (XulDialog) document.getElementById("add-user-dialog");//$NON-NLS-1$
    userListBox = (XulListbox) document.getElementById("users-list");//$NON-NLS-1$

    // Add User Binding

    username = (XulTextbox) document.getElementById("user-name");//$NON-NLS-1$
    userPassword = (XulTextbox) document.getElementById("user-password");//$NON-NLS-1$
    userDescription = (XulTextbox) document.getElementById("user-description");//$NON-NLS-1$

    bf.setBindingType(Binding.Type.BI_DIRECTIONAL);
    bf.createBinding(securityUser, "name", username, "value");//$NON-NLS-1$ //$NON-NLS-2$
    bf.createBinding(securityUser, "password", userPassword, "value");//$NON-NLS-1$ //$NON-NLS-2$
    bf.createBinding(securityUser, "description", userDescription, "value");//$NON-NLS-1$ //$NON-NLS-2$
    bf.createBinding(security, "selectedUserIndex", userListBox, "selectedIndex");//$NON-NLS-1$ //$NON-NLS-2$
    bf.setBindingType(Binding.Type.ONE_WAY);
    try {
      BindingConvertor<Integer, Boolean> buttonConverter = new BindingConvertor<Integer, Boolean>() {

        @Override
        public Boolean sourceToTarget(Integer value) {
          if (value != null && value >= 0) {
            return false;
          }
          return true;
        }

        @Override
        public Integer targetToSource(Boolean value) {
          // TODO Auto-generated method stub
          return null;
        }
      };
      bf.createBinding(userListBox, "selectedIndex", userEditButton, "disabled", buttonConverter);//$NON-NLS-1$ //$NON-NLS-2$
      bf.createBinding(userListBox, "selectedIndex", userRemoveButton, "disabled", buttonConverter);//$NON-NLS-1$ //$NON-NLS-2$

      bf.setBindingType(Binding.Type.ONE_WAY);
      // Action based security permissions
      bf.createBinding(userListBox, "selectedItem", security, "selectedUser");//$NON-NLS-1$ //$NON-NLS-2$
      bf.createBinding(security, "userList", userListBox, "elements").fireSourceChanged();//$NON-NLS-1$ //$NON-NLS-2$

      BindingConvertor<Mode, Boolean> modeBindingConverter = new BindingConvertor<Mode, Boolean>() {

        @Override
        public Boolean sourceToTarget(Mode arg0) {
          if (arg0.equals(Mode.ADD)) {
            return false;
          }
          return true;
        }

        @Override
        public Mode targetToSource(Boolean arg0) {
          // TODO Auto-generated method stub
          return null;
        }

      };
      bf.createBinding(securityUser, "mode", username, "disabled", modeBindingConverter);//$NON-NLS-1$ //$NON-NLS-2$
      
    } catch (Exception e) {
      // convert to runtime exception so it bubbles up through the UI
      throw new RuntimeException(e);
    }
  }

  public String getName() {
    return "iSecurityController"; //$NON-NLS-1$
  }

  public void showAddUserDialog() throws Exception {
      securityUser.clear();
      securityUser.setMode(Mode.ADD);
      userDialog.setTitle(messages.getString("AddUserDialog.Title"));//$NON-NLS-1$
      userDialog.show();
  }

  public void cancelAddUserDialog() throws Exception {
    userDialog.hide();
  }

  /**
   * addRole method is called when user has click ok on a add role dialog. The method add the 
   * role
   * @throws Exception
   */
  protected void addUser() throws Exception {
    if (service != null) {
      try {
        service.saveUserInfo(securityUser.getUserInfo());
        security.addUser(UIObjectRegistery.getInstance().constructUIRepositoryUser(securityUser.getUserInfo()));
      } catch (KettleException ke) {
        messageBox.setTitle(messages.getString("Dialog.Error"));//$NON-NLS-1$
        messageBox.setAcceptLabel(messages.getString("Dialog.Ok"));//$NON-NLS-1$
        messageBox.setMessage(BaseMessages.getString(RepositoryExplorer.class,
            "AddUser.UnableToAddUser", ke.getLocalizedMessage()));//$NON-NLS-1$
        messageBox.open();
      }
    }
    userDialog.hide();
  }

  public void showEditUserDialog() throws Exception {
    if (service != null) {
      securityUser.clear();
      securityUser.setUser(security.getSelectedUser());
      securityUser.setMode(Mode.EDIT);
      userDialog.setTitle(messages.getString("EditUserDialog.Title"));//$NON-NLS-1$
      userDialog.show();
    }
  }

  /**
   * updateUser method is called when user has click ok on a edit user dialog. The method updates the 
   * user
   * @throws Exception
   */

  protected void updateUser() throws Exception {
    if (service != null) {
      try {
        IUIUser uiUser = security.getSelectedUser();
        uiUser.setDescription(securityUser.getDescription());
        uiUser.setPassword(securityUser.getPassword());
        service.updateUser(uiUser.getUserInfo());
        security.updateUser(uiUser);
      } catch (KettleException ke) {
        messageBox.setTitle(messages.getString("Dialog.Error"));//$NON-NLS-1$
        messageBox.setAcceptLabel(messages.getString("Dialog.Ok"));//$NON-NLS-1$
        messageBox.setMessage(BaseMessages.getString(RepositoryExplorer.class,
            "UpdateUser.UnableToUpdateUser", ke.getLocalizedMessage()));//$NON-NLS-1$
        messageBox.open();
      }
    }
    userDialog.hide();
  }


  /**
   * removeUser method is called when user has click on a remove button in a user deck. It first
   * displays a confirmation message to the user and once the user selects ok, it remove the user
   * @throws Exception
   */
  public void removeUser() throws Exception {
    confirmBox.setTitle(messages.getString("ConfirmDialog.Title"));//$NON-NLS-1$
    confirmBox.setMessage(messages.getString("RemoveUserConfirmDialog.Message"));//$NON-NLS-1$
    confirmBox.setAcceptLabel(messages.getString("Dialog.Ok"));//$NON-NLS-1$
    confirmBox.setCancelLabel(messages.getString("Dialog.Cancel"));//$NON-NLS-1$
    confirmBox.addDialogCallback(new XulDialogCallback<Object>() {

      public void onClose(XulComponent sender, Status returnCode, Object retVal) {
        if (returnCode == Status.ACCEPT) {
          if (service != null) {
            if (security != null && security.getSelectedUser() != null) {
              try {
                service.delUser(security.getSelectedUser().getName());
                security.removeUser(security.getSelectedUser().getName());
              } catch (KettleException ke) {
                messageBox.setTitle(messages.getString("Dialog.Error"));//$NON-NLS-1$
                messageBox.setAcceptLabel(messages.getString("Dialog.Ok"));//$NON-NLS-1$
                messageBox.setMessage(BaseMessages.getString(RepositoryExplorer.class,
                    "RemoveUser.UnableToRemoveUser", ke.getLocalizedMessage()));//$NON-NLS-1$
                messageBox.open();
              }
            } else {
              messageBox.setTitle(messages.getString("Dialog.Error"));//$NON-NLS-1$
              messageBox.setAcceptLabel(messages.getString("Dialog.Ok"));//$NON-NLS-1$
              messageBox.setMessage(messages.getString("RemoveUser.NoUserSelected"));//$NON-NLS-1$
              messageBox.open();
            }
          }
        }
      }

      public void onError(XulComponent sender, Throwable t) {
        messageBox.setTitle(messages.getString("Dialog.Error"));//$NON-NLS-1$
        messageBox.setAcceptLabel(messages.getString("Dialog.Ok"));//$NON-NLS-1$
        messageBox.setMessage(BaseMessages.getString(RepositoryExplorer.class,
            "RemoveUser.UnableToRemoveUser", t.getLocalizedMessage()));//$NON-NLS-1$
        messageBox.open();
      }
    });
    confirmBox.open();
  }

  public void changeToUserDeck() {
    security.setSelectedDeck(ObjectRecipient.Type.USER);
  }

  /**
   * saveUser method is called when the user click on the ok button of a Add or Edit User dialog
   * Depending on the mode it calls add of update user method
   * @throws Exception
   */

  public void saveUser() throws Exception {
    if (securityUser.getMode().equals(Mode.ADD)) {
      addUser();
    } else {
      updateUser();
    }
  }
}
