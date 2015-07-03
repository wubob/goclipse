/*******************************************************************************
 * Copyright (c) 2014, 2014 Bruno Medeiros and other Contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Bruno Medeiros - initial API and implementation
 *******************************************************************************/
package com.googlecode.goclipse.ui.navigator;

import static melnorme.lang.ide.ui.views.StylerHelpers.fgColor;
import static melnorme.utilbox.core.Assert.AssertNamespace.assertFail;
import static melnorme.utilbox.core.CoreUtil.areEqual;

import java.nio.file.Path;

import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;

import com.googlecode.goclipse.core.GoProjectEnvironment;
import com.googlecode.goclipse.ui.GoPluginImages;
import com.googlecode.goclipse.ui.navigator.elements.GoPathEntryElement;
import com.googlecode.goclipse.ui.navigator.elements.GoRootElement;
import com.googlecode.goclipse.ui.navigator.elements.IGoProjectElement;

import melnorme.lang.ide.ui.views.LangNavigatorLabelProvider;
import melnorme.utilbox.misc.MiscUtil;

public class GoNavigatorLabelProvider extends LangNavigatorLabelProvider  {
	
	protected static final RGB LOCATION_ANNOTATION_FG = new RGB(128, 128, 128);
	
	@Override
	protected DefaultGetStyledTextSwitcher getStyledText_switcher() {
		return new DefaultGetStyledTextSwitcher() {
			@Override
			public StyledString visitOther(Object element) {
				return getStyledText_other(element);
			}
		};
	}
	
	public StyledString getStyledText_other(Object element) {
		
		if(element instanceof IGoProjectElement) {
			IGoProjectElement goProjectElement = (IGoProjectElement) element;
			
			StyledString baseText = new StyledString(goProjectElement.getName());
			
			if(element instanceof GoRootElement) {
				GoRootElement goRootElement = (GoRootElement) element;
				baseText.append(" - " + goRootElement.getDirectory().toString(), fgColor(LOCATION_ANNOTATION_FG));
				return baseText;
			}
			if(element instanceof GoPathEntryElement) {
				GoPathEntryElement goPathEntryElement = (GoPathEntryElement) element;
				
				baseText.append(" - ", fgColor(LOCATION_ANNOTATION_FG));
				
				String goPathEntryLocation = goPathEntryElement.getDirectory().toString();
				
				StyledString suffix;
				if(goPathEntryElement.isProjectInsideGoPath()) {
					suffix = new StyledString(goPathEntryLocation, 
						new ItalicStyler(fgColor(LOCATION_ANNOTATION_FG)));
				} else {
					suffix = new StyledString(goPathEntryLocation, fgColor(LOCATION_ANNOTATION_FG));
				}
				baseText.append(suffix);
				
				return baseText;
			}
			assertFail();
		}
		
		if (element instanceof IFileStore) {
			IFileStore fileStore = (IFileStore) element;
			return new StyledString(fileStore.getName());
		}
		
		return null;
	}
	
	@Override
	protected DefaultGetImageSwitcher getBaseImage_switcher() {
		return new DefaultGetImageSwitcher() {
			
			@Override
			public ImageDescriptor visitOther(Object element) {
				return getBaseImage_Other(element);
			}
		};
	}
	
	public ImageDescriptor getBaseImage_Other(Object element) {
		
		if(element instanceof IGoProjectElement) {
			if(element instanceof GoRootElement) {
				return GoPluginImages.NAVIGATOR_GOROOT_ENTRY.getDescriptor();
			}
			if(element instanceof GoPathEntryElement) {
				return GoPluginImages.NAVIGATOR_GOPATH_ENTRY.getDescriptor();
			}
			assertFail();
		}
		
		if (element instanceof IFileStore) {
			IFileStore fileStore = (IFileStore) element;
			
			try {
				if (fileStore.fetchInfo().isDirectory()) {
					return GoPluginImages.NAVIGATOR_SOURCE_PACKAGE_FOLDER.getDescriptor();
				}
				
				// TODO: should cleanup up this.
				
				IEditorDescriptor descriptor = IDE.getEditorDescriptor(fileStore.getName());
				if (descriptor != null) {
					return descriptor.getImageDescriptor();
				} else {
					return PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_OBJ_FILE);
				}
			} catch (PartInitException e) {
				
			}
		}
		
		if(element instanceof IResource) {
			IResource resource = (IResource) element;
			
			return getResourceImageDescriptor(resource);
		}
		
		return null;
	}
	
	protected ImageDescriptor getResourceImageDescriptor(IResource resource) {
		if (resource instanceof IFolder) {
			IFolder folder = (IFolder)resource;
			
			IProject project = resource.getProject();
			boolean isProjecInsideGoPath;
			try {
				isProjecInsideGoPath = GoProjectEnvironment.isProjectInsideGoPath(project);
			} catch (CoreException e) {
				return null;
			}
			
			if(resource.getParent() instanceof IProject && !isProjecInsideGoPath) {
				if("src".equals(resource.getName())) {
					return GoPluginImages.NAVIGATOR_SRC_FOLDER.getDescriptor();
				} else if("pkg".equals(resource.getName())) {
					return GoPluginImages.NAVIGATOR_PKG_FOLDER.getDescriptor();
				} else if("bin".equals(resource.getName())) {
					return GoPluginImages.NAVIGATOR_BIN_FOLDER.getDescriptor();
				}
			} else if(isSourcePackageFolder(folder, isProjecInsideGoPath)) {
				return GoPluginImages.NAVIGATOR_SOURCE_PACKAGE_FOLDER.getDescriptor();
			}
		} else if(resource instanceof IFile) {
			IFile file = (IFile) resource;
			
			if(areEqual("a", file.getFileExtension())) {
				return GoPluginImages.NAVIGATOR_GO_PACKAGE_FILE.getDescriptor();
			}
		}
		return null;
	}
	
	protected static boolean isSourcePackageFolder(IFolder folder, boolean isProjecInsideGoPath) {
		Path path = folder.getProjectRelativePath().toFile().toPath();
		
		if(isProjecInsideGoPath) {
			return isValidSourcePackageName(path);
		} else {
			if(path.startsWith("src")) {
				path = MiscUtil.createValidPath("src").relativize(path);
				return isValidSourcePackageName(path);
			}
			return false;
		}
	}
	
	@SuppressWarnings("unused")
	protected static boolean isValidSourcePackageName(Path path) {
		return true; // TODO: check for invalid names
	}
	
}