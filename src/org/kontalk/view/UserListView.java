/*
 *  Kontalk Java client
 *  Copyright (C) 2014 Kontalk Devteam <devteam@kontalk.org>
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.kontalk.view;

import com.alee.extended.panel.GroupPanel;
import com.alee.laf.button.WebButton;
import com.alee.laf.checkbox.WebCheckBox;
import com.alee.laf.label.WebLabel;
import com.alee.laf.menu.WebMenuItem;
import com.alee.laf.menu.WebPopupMenu;
import com.alee.laf.optionpane.WebOptionPane;
import com.alee.laf.panel.WebPanel;
import com.alee.laf.rootpane.WebDialog;
import com.alee.laf.separator.WebSeparator;
import com.alee.laf.text.WebTextField;
import com.alee.managers.tooltip.WebCustomTooltip;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashSet;
import java.util.Observer;
import java.util.Set;
import javax.swing.ListSelectionModel;
import org.kontalk.model.User;
import org.kontalk.model.UserList;
import org.kontalk.system.Control;
import org.kontalk.util.Tr;
import org.kontalk.view.UserListView.UserItem;

/**
 * Display all user (aka contacts) in a brief list.
 * @author Alexander Bikadorov <abiku@cs.tu-berlin.de>
 */
final class UserListView extends ListView<UserItem, User> implements Observer {

    private final View mView;
    private final UserList mUserList;
    private final UserPopupMenu mPopupMenu;

    private WebCustomTooltip mTip = null;

    UserListView(final View view, UserList userList) {
        super();

        mView = view;

        mUserList = userList;

        this.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // right click popup menu
        mPopupMenu = new UserPopupMenu();

        // actions triggered by mouse events
        this.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                if (e.getClickCount() == 2) {
                    mView.selectThreadByUser(UserListView.this.getSelectedListValue());
                }
            }
            @Override
            public void mousePressed(MouseEvent e) {
                check(e);
            }
            @Override
            public void mouseReleased(MouseEvent e) {
                check(e);
            }
            private void check(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    UserListView.this.setSelectedIndex(locationToIndex(e.getPoint()));
                    UserListView.this.showPopupMenu(e);
                }
            }
            @Override
            public void mouseExited(MouseEvent e) {
                if (mTip != null)
                    mTip.closeTooltip();
            }
        });

        this.updateOnEDT();

        mUserList.addObserver(this);
    }

    @Override
    protected void updateOnEDT() {
        // TODO performance
        this.clearModel();
        for (User oneUser: mUserList.getAll()) {
            this.addItem(new UserItem(oneUser));
        }
    }

    private void showPopupMenu(MouseEvent e) {
        // note: only work when right click does also selection
        mPopupMenu.show(this.getSelectedListItem(), this, e.getX(), e.getY());
    }

    /** One item in the contact list representing a user. */
    class UserItem extends ListView<UserItem, User>.ListItem {

        private final WebLabel mNameLabel;
        private final WebLabel mJIDLabel;
        private final Color mBackround;

        UserItem(User user) {
            super(user);

            this.getValue();

            //this.setPaintFocus(true);
            this.setMargin(5);
            this.setLayout(new BorderLayout(10, 5));

            mNameLabel = new WebLabel("foo");
            mNameLabel.setFontSize(14);
            // if too long, draw three dots at the end
            Dimension size = mNameLabel.getPreferredSize();
            mNameLabel.setMinimumSize(size);
            mNameLabel.setPreferredSize(size);
            String name = !mValue.getName().isEmpty() ? mValue.getName() : Tr.tr("<unknown>");
            mNameLabel.setText(name);
            this.add(mNameLabel, BorderLayout.CENTER);

            mJIDLabel = new WebLabel("foo");
            mJIDLabel.setForeground(Color.GRAY);
            mJIDLabel.setFontSize(11);
            size = mJIDLabel.getPreferredSize();
            mJIDLabel.setMinimumSize(size);
            mJIDLabel.setPreferredSize(size);
            mJIDLabel.setText(mValue.getJID());
            this.add(mJIDLabel, BorderLayout.SOUTH);

            mBackround = mValue.getAvailable() == User.Available.YES ? View.LIGHT_BLUE : Color.WHITE;
            this.setBackground(mBackround);
        }

        @Override
        public String getTooltipText() {
            String no = Tr.tr("No");
            String dunno = Tr.tr("?");

            String isAvailable;
            if (mValue.getAvailable() == User.Available.YES)
                isAvailable = Tr.tr("Yes");
            else if (mValue.getAvailable() == User.Available.NO)
                isAvailable = no;
            else
                isAvailable = dunno;

            String status = mValue.getStatus().isEmpty() ? dunno : mValue.getStatus();

            String lastSeen = !mValue.getLastSeen().isPresent() ? dunno :
                    TOOLTIP_DATE_FORMAT.format(mValue.getLastSeen().get());

            String isBlocked = mValue.isBlocked() ? Tr.tr("YES") : no;

            String html = "<html><body>" +
                    //"<h3>Header</h3>" +
                    "<br>" +
                    Tr.tr("Available")+": " + isAvailable + "<br>" +
                    Tr.tr("Status")+": " + status + "<br>" +
                    Tr.tr("Blocked")+": " + isBlocked + "<br>" +
                    Tr.tr("Last seen")+": " + lastSeen + "<br>" +
                    "";

            return html;
        }

        @Override
        void repaint(boolean isSelected) {
            if (isSelected)
                this.setBackground(View.BLUE);
            else
                this.setBackground(mBackround);
        }

        @Override
        protected boolean contains(String search) {
            return mValue.getName().toLowerCase().contains(search) ||
                    mValue.getJID().toLowerCase().contains(search);
        }
    }

    private class UserPopupMenu extends WebPopupMenu {

        UserItem mSelectedUserView;
        WebMenuItem mBlockMenuItem;
        WebMenuItem mUnblockMenuItem;

        UserPopupMenu() {
            WebMenuItem newMenuItem = new WebMenuItem(Tr.tr("New Thread"));
            newMenuItem.setToolTipText(Tr.tr("Creates a new thread for this contact"));
            newMenuItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent event) {
                    Set<User> user = new HashSet<>();
                    user.add(mSelectedUserView.getValue());
                    UserListView.this.mView.callCreateNewThread(user);
                }
            });
            this.add(newMenuItem);

            WebMenuItem editMenuItem = new WebMenuItem(Tr.tr("Edit Contact"));
            editMenuItem.setToolTipText(Tr.tr("Edit this contact"));
            editMenuItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent event) {
                    WebDialog editUserDialog = new EditUserDialog(mSelectedUserView);
                    editUserDialog.setVisible(true);
                }
            });
            this.add(editMenuItem);

            mBlockMenuItem = new WebMenuItem(Tr.tr("Block Contact"));
            mBlockMenuItem.setToolTipText(Tr.tr("Block all messages from this contact"));
            mBlockMenuItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent event) {
                    UserListView.this.mView.callSetUserBlocking(mSelectedUserView.getValue(), true);
                }
            });
            this.add(mBlockMenuItem);

            mUnblockMenuItem = new WebMenuItem(Tr.tr("Unblock Contact"));
            mUnblockMenuItem.setToolTipText(Tr.tr("Unblock this contact"));
            mUnblockMenuItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent event) {
                    UserListView.this.mView.callSetUserBlocking(mSelectedUserView.getValue(), false);
                }
            });
            this.add(mUnblockMenuItem);

            WebMenuItem deleteMenuItem = new WebMenuItem(Tr.tr("Delete Contact"));
            deleteMenuItem.setToolTipText(Tr.tr("Delete this contact"));
            deleteMenuItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent event) {
                    // TODO delete threads/messages too? android client may add it
                    // to roster again? useful at all? only self-created contacts?
                }
            });
            // see above
            //this.add(deleteMenuItem);
        }

        void show(UserItem selectedUserView, Component invoker, int x, int y) {
            mSelectedUserView = selectedUserView;

            if (mSelectedUserView.getValue().isBlocked()) {
                mBlockMenuItem.setVisible(false);
                mUnblockMenuItem.setVisible(true);
            } else {
                mBlockMenuItem.setVisible(true);
                mUnblockMenuItem.setVisible(false);
            }

            Control.Status status = UserListView.this.mView.getCurrentStatus();
            mBlockMenuItem.setEnabled(status == Control.Status.CONNECTED);
            mUnblockMenuItem.setEnabled(status == Control.Status.CONNECTED);

            this.show(invoker, x, y);
        }

    }

    private class EditUserDialog extends WebDialog {

        private final UserItem mUserView;
        private final WebTextField mNameField;
        private final WebTextField mJIDField;
        private final WebCheckBox mEncryptionBox;

        EditUserDialog(UserItem userView) {

            mUserView = userView;

            this.setTitle(Tr.tr("Edit Contact"));
            //this.setSize(400, 280);
            this.setResizable(false);
            this.setModal(true);

            GroupPanel groupPanel = new GroupPanel(10, false);
            groupPanel.setMargin(5);

            // editable fields
            WebPanel namePanel = new WebPanel();
            namePanel.setLayout(new BorderLayout(10, 5));
            namePanel.add(new WebLabel(Tr.tr("Display Name:")), BorderLayout.WEST);
            mNameField = new WebTextField(mUserView.getValue().getName());
            mNameField.setInputPrompt(mUserView.getValue().getName());
            mNameField.setHideInputPromptOnFocus(false);
            namePanel.add(mNameField, BorderLayout.CENTER);
            groupPanel.add(namePanel);
            groupPanel.add(new WebSeparator(true, true));

            String hasKey = "<html>"+Tr.tr("Encryption Key")+": ";
            if (mUserView.getValue().hasKey()) {
                hasKey += Tr.tr("Available")+"</html>";
            } else {
                hasKey += "<font color='red'>"+Tr.tr("Not Available")+"</font></html>";
            }
            groupPanel.add(new WebLabel(hasKey));

            mEncryptionBox = new WebCheckBox(Tr.tr("Encryption"));
            mEncryptionBox.setAnimated(false);
            mEncryptionBox.setSelected(mUserView.getValue().getEncrypted());
            groupPanel.add(mEncryptionBox);
            groupPanel.add(new WebSeparator(true, true));

            groupPanel.add(new WebLabel("JID:"));
            mJIDField = new WebTextField(mUserView.getValue().getJID(), 38);
            mJIDField.setInputPrompt(mUserView.getValue().getJID());
            mJIDField.setHideInputPromptOnFocus(false);
            groupPanel.add(mJIDField);
            groupPanel.add(new WebSeparator(true, true));

            this.add(groupPanel, BorderLayout.CENTER);

            // buttons
            WebButton cancelButton = new WebButton(Tr.tr("Cancel"));
            cancelButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    EditUserDialog.this.dispose();
                }
            });
            final WebButton saveButton = new WebButton("Save");
            saveButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (!EditUserDialog.this.isConfirmed())
                        return;

                    EditUserDialog.this.saveUser();
                    EditUserDialog.this.dispose();
                }
            });
            this.getRootPane().setDefaultButton(saveButton);

            GroupPanel buttonPanel = new GroupPanel(2, cancelButton, saveButton);
            buttonPanel.setLayout(new FlowLayout(FlowLayout.TRAILING));
            this.add(buttonPanel, BorderLayout.SOUTH);

            this.pack();
        }

        private boolean isConfirmed() {
            if (!mJIDField.getText().equals(mUserView.getValue().getJID())) {
                String warningText =
                        Tr.tr("Changing the JID is only useful in very rare cases. Are you sure?");
                int selectedOption = WebOptionPane.showConfirmDialog(this,
                        warningText,
                        Tr.tr("Please Confirm"),
                        WebOptionPane.OK_CANCEL_OPTION,
                        WebOptionPane.WARNING_MESSAGE);
                if (selectedOption != WebOptionPane.OK_OPTION) {
                    return false;
                }
            }
            return true;
        }

        private void saveUser() {
            if (!mNameField.getText().isEmpty()) {
                mUserView.getValue().setName(mNameField.getText());
            }
            mUserView.getValue().setEncrypted(mEncryptionBox.isSelected());
            if (!mJIDField.getText().isEmpty()) {
                mUserView.getValue().setJID(mJIDField.getText());
            }
        }
    }
}
