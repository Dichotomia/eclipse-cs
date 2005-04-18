/*
 * fiscusutils: An eclipse plugin with a set of utilities
 * Copyright (C) 2003, 2004��Lars K�dderitzsch
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package com.atlassw.tools.eclipse.checkstyle.projectconfig.filters;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.CheckedTreeSelectionDialog;
import org.eclipse.ui.internal.ide.misc.ContainerContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;

import com.atlassw.tools.eclipse.checkstyle.CheckstylePlugin;

/**
 * Editor dialog for the package filter.
 * 
 * @author Lars K�dderitzsch
 */
public class PackageFilterEditor implements IFilterEditor
{

    //
    // constants
    //

    //
    // attributes
    //

    /** the dialog for this editor. */
    private CheckedTreeSelectionDialog mDialog;

    /** the input for the editor. */
    private IProject                   mInputProject;

    /** the filter data. */
    private List                       mFilterData;

    //
    // methods
    //

    /**
     * @see IFilterEditor#openEditor(org.eclipse.swt.widgets.Shell)
     */
    public int openEditor(Shell parent)
    {

        this.mDialog = new CheckedTreeSelectionDialog(parent, WorkbenchLabelProvider
                .getDecoratingWorkbenchLabelProvider(), new ContainerContentProvider());

        //initialize the dialog with the filter data
        initCheckedTreeSelectionDialog();

        //open the dialog
        int retCode = this.mDialog.open();

        //actualize the filter data
        if (Window.OK == retCode)
        {
            this.mFilterData = this.getFilterDataFromDialog();
        }

        return retCode;
    }

    /**
     * @see IFilterEditor#setInputProject(org.eclipse.core.resources.IProject)
     */
    public void setInputProject(IProject input)
    {
        this.mInputProject = input;
    }

    /**
     * @see IFilterEditor#setFilterData(java.lang.String)
     */
    public void setFilterData(List filterData)
    {
        this.mFilterData = filterData;
    }

    /**
     * @see IFilterEditor#getFilterData()
     */
    public List getFilterData()
    {
        return this.mFilterData;
    }

    /**
     * Helper method to initialize the dialog.
     */
    private void initCheckedTreeSelectionDialog()
    {

        this.mDialog.setTitle(CheckstylePlugin.getResourceString("msgCheckstylePackageSelTitle"));
        this.mDialog.setMessage(CheckstylePlugin.getResourceString("msgCheckstylePackageSel"));
        this.mDialog.setBlockOnOpen(true);

        this.mDialog.addFilter(new NonSourceDirsFilter());
        this.mDialog.setInput(this.mInputProject);

        //display the filter data
        if (this.mInputProject != null && this.mFilterData != null)
        {

            List selectedElements = new ArrayList();
            List expandedElements = new ArrayList();

            int size = mFilterData != null ? mFilterData.size() : 0;
            for (int i = 0; i < size; i++)
            {

                IPath path = new Path((String) mFilterData.get(i));

                selectedElements.add(this.mInputProject.findMember(path));

                //get all parent elements to expand
                while (path.segmentCount() > 0)
                {
                    path = path.removeLastSegments(1);
                    expandedElements.add(this.mInputProject.findMember(path));
                }
            }

            this.mDialog.setInitialSelections(selectedElements.toArray());
            this.mDialog.setExpandedElements(expandedElements.toArray());
        }
    }

    /**
     * Helper method to extract the edited data from the dialog.
     * 
     * @return the filter data
     */
    private List getFilterDataFromDialog()
    {

        Object[] checked = this.mDialog.getResult();

        List result = new ArrayList();
        for (int i = 0; i < checked.length; i++)
        {

            if (checked[i] instanceof IResource)
            {
                result.add(((IResource) checked[i]).getProjectRelativePath().toString());
            }
        }
        return result;
    }

    /**
     * Filters non source directories.
     * 
     * @author Lars K�dderitzsch
     */
    private class NonSourceDirsFilter extends ViewerFilter
    {

        /**
         * @see org.eclipse.jface.viewers.ViewerFilter#select(org.eclipse.jface.viewers.Viewer,
         *      java.lang.Object, java.lang.Object)
         */
        public boolean select(Viewer viewer, Object parentElement, Object element)
        {

            boolean passes = true;

            if (element instanceof IFolder)
            {

                passes = false;

                IFolder folder = (IFolder) element;

                IJavaElement el = JavaCore.create(folder);
                if (el != null && el.exists())
                {

                    if (el instanceof IPackageFragmentRoot)
                    {
                        passes = !((IPackageFragmentRoot) el).isArchive();
                    }
                    else if (el instanceof IPackageFragment)
                    {
                        passes = !((IPackageFragment) el).isDefaultPackage();
                    }
                }
            }
            return passes;
        }
    }
}