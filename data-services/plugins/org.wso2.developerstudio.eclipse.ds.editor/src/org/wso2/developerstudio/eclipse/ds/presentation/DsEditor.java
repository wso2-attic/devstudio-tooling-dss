/*
 * Copyright 2009-2010 WSO2, Inc. (http://wso2.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wso2.developerstudio.eclipse.ds.presentation;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EventObject;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.emf.common.command.BasicCommandStack;
import org.eclipse.emf.common.command.Command;
import org.eclipse.emf.common.command.CommandStack;
import org.eclipse.emf.common.command.CommandStackListener;
import org.eclipse.emf.common.notify.AdapterFactory;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.ui.MarkerHelper;
import org.eclipse.emf.common.ui.ViewerPane;
import org.eclipse.emf.common.ui.editor.ProblemEditorPart;
import org.eclipse.emf.common.ui.viewer.IViewerProvider;
import org.eclipse.emf.common.util.BasicDiagnostic;
import org.eclipse.emf.common.util.Diagnostic;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EValidator;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.util.EContentAdapter;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.edit.command.SetCommand;
import org.eclipse.emf.edit.domain.AdapterFactoryEditingDomain;
import org.eclipse.emf.edit.domain.EditingDomain;
import org.eclipse.emf.edit.domain.IEditingDomainProvider;
import org.eclipse.emf.edit.provider.AdapterFactoryItemDelegator;
import org.eclipse.emf.edit.provider.ComposedAdapterFactory;
import org.eclipse.emf.edit.provider.ReflectiveItemProviderAdapterFactory;
import org.eclipse.emf.edit.provider.resource.ResourceItemProviderAdapterFactory;
import org.eclipse.emf.edit.ui.action.EditingDomainActionBarContributor;
import org.eclipse.emf.edit.ui.dnd.EditingDomainViewerDropAdapter;
import org.eclipse.emf.edit.ui.dnd.LocalTransfer;
import org.eclipse.emf.edit.ui.dnd.ViewerDragAdapter;
import org.eclipse.emf.edit.ui.provider.AdapterFactoryContentProvider;
import org.eclipse.emf.edit.ui.provider.AdapterFactoryLabelProvider;
import org.eclipse.emf.edit.ui.provider.UnwrappingSelectionProvider;
import org.eclipse.emf.edit.ui.util.EditUIMarkerHelper;
import org.eclipse.emf.edit.ui.util.EditUIUtil;
import org.eclipse.emf.edit.ui.view.ExtendedPropertySheetPage;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.eclipse.ui.dialogs.SaveAsDialog;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.ide.IGotoMarker;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.views.contentoutline.ContentOutline;
import org.eclipse.ui.views.contentoutline.ContentOutlinePage;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import org.eclipse.ui.views.properties.IPropertySheetEntry;
import org.eclipse.ui.views.properties.IPropertySheetPage;
import org.eclipse.ui.views.properties.PropertySheet;
import org.eclipse.ui.views.properties.PropertySheetPage;
import org.eclipse.ui.views.properties.PropertySheetSorter;
import org.wso2.developerstudio.eclipse.ds.DataService;
import org.wso2.developerstudio.eclipse.ds.DocumentRoot;
import org.wso2.developerstudio.eclipse.ds.DsPackage;
import org.wso2.developerstudio.eclipse.ds.command.DesignViewActionHandler;
import org.wso2.developerstudio.eclipse.ds.impl.DocumentRootImpl;
import org.wso2.developerstudio.eclipse.ds.presentation.custom.CustomAdapterFactoryContentProvider;
import org.wso2.developerstudio.eclipse.ds.presentation.data.DataSourcePage;
import org.wso2.developerstudio.eclipse.ds.presentation.md.DetailSectionUiUtil;
import org.wso2.developerstudio.eclipse.ds.presentation.md.MasterDetailsPage;
import org.wso2.developerstudio.eclipse.ds.presentation.source.DsObjectSourceEditor;
import org.wso2.developerstudio.eclipse.ds.provider.DsItemProviderAdapterFactory;

// TODO: Auto-generated Javadoc
/**
 * This is an example of a Ds model editor.
 * <!-- begin-user-doc --> <!--
 * end-user-doc -->
 * 
 * @generated NOT
 */
public class DsEditor extends FormEditor implements IEditingDomainProvider, ISelectionProvider,
                                        IMenuListener, IViewerProvider, IGotoMarker {

	private DsEditor dsEditor;

	private DesignViewActionHandler designViewActionHandler;

	private DataService dataService;
	/**
	 * This keeps track of the editing domain that is used to track all changes
	 * to the model.
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	protected AdapterFactoryEditingDomain editingDomain;

	/**
	 * This is the one adapter factory used for providing views of the model.
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	protected ComposedAdapterFactory adapterFactory;

	/**
	 * This is the content outline page.
	 * <!-- begin-user-doc --> <!--
	 * end-user-doc -->
	 * 
	 * @generated
	 */
	protected IContentOutlinePage contentOutlinePage;

	/**
	 * This is a kludge...
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	protected IStatusLineManager contentOutlineStatusLineManager;

	/**
	 * This is the content outline page's viewer.
	 * <!-- begin-user-doc --> <!--
	 * end-user-doc -->
	 * 
	 * @generated
	 */
	protected TreeViewer contentOutlineViewer;

	/**
	 * This is MasterDetail page
	 * 
	 */
	private MasterDetailsPage mdPage;

	/**
	 * This is Data Source detail page
	 * 
	 */
	private DataSourcePage dataSourcePage;
	/**
	 * This is SourceViwer
	 * 
	 */
	private DsObjectSourceEditor sourceEditor;

	/**
	 * Design view index
	 * 
	 */
	private static final int DESIGN_VIEW_INDEX = 0;

	/**
	 * Data Source Page index
	 * 
	 */
	private static final int DATA_SOURCE_PAGE_INDEX = 2;
	/**
	 * Source view index
	 * 
	 */
	private static final int SOURCE_VIEW_INDEX = 1;

	/**
	 * Resource holder for the Dseditor
	 * 
	 */
	private Resource domainResource;

	/**
	 * Keep track the changes in Source view.
	 * 
	 */
	private boolean isSourceModified;

	/**
	 * Keep track the state of design view.
	 * 
	 */
	private boolean designViewActivated;

	private boolean isSavingProcOk;
	/**
	 * This is the property sheet page.
	 * <!-- begin-user-doc --> <!--
	 * end-user-doc -->
	 * 
	 * @generated
	 */
	protected PropertySheetPage propertySheetPage;

	/**
	 * This is the viewer that shadows the selection in the content outline. The
	 * parent relation must be correctly defined for this to work. <!--
	 * begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	protected TreeViewer selectionViewer;

	/**
	 * This inverts the roll of parent and child in the content provider and
	 * show parents as a tree.
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	protected TreeViewer parentViewer;

	/**
	 * This shows how a tree view works.
	 * <!-- begin-user-doc --> <!--
	 * end-user-doc -->
	 * 
	 * @generated
	 */
	protected TreeViewer treeViewer;

	/**
	 * This shows how a list view works.
	 * A list viewer doesn't support icons.
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	protected ListViewer listViewer;

	/**
	 * This shows how a table view works.
	 * A table can be used as a list with icons.
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	protected TableViewer tableViewer;

	/**
	 * This shows how a tree view with columns works.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	protected TreeViewer treeViewerWithColumns;

	/**
	 * This keeps track of the active viewer pane, in the book. <!--
	 * begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	protected ViewerPane currentViewerPane;

	/**
	 * This keeps track of the active content viewer, which may be either one of
	 * the viewers in the pages or the content outline viewer. <!--
	 * begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	protected Viewer currentViewer;

	/**
	 * This listens to which ever viewer is active.
	 * <!-- begin-user-doc --> <!--
	 * end-user-doc -->
	 * 
	 * @generated
	 */
	protected ISelectionChangedListener selectionChangedListener;

	/**
	 * This keeps track of all the.
	 * {@link org.eclipse.jface.viewers.ISelectionChangedListener}s that are
	 * listening to this editor. <!-- begin-user-doc --> <!-- end-user-doc --> @generated
	 */
	protected Collection<ISelectionChangedListener> selectionChangedListeners =
	                                                                            new ArrayList<ISelectionChangedListener>();

	/**
	 * This keeps track of the selection of the editor as a whole. <!--
	 * begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	protected ISelection editorSelection = StructuredSelection.EMPTY;

	/**
	 * The MarkerHelper is responsible for creating workspace resource markers
	 * presented
	 * in Eclipse's Problems View.
	 * <!-- begin-user-doc --> <!--
	 * end-user-doc -->
	 * 
	 * @generated
	 */
	protected MarkerHelper markerHelper = new EditUIMarkerHelper();

	/**
	 * This listens for when the outline becomes active <!-- begin-user-doc -->
	 * <!-- end-user-doc -->. @generated
	 */
	protected IPartListener partListener = new IPartListener() {
		public void partActivated(IWorkbenchPart p) {
			if (p instanceof ContentOutline) {
				if (((ContentOutline) p).getCurrentPage() == contentOutlinePage) {
					getActionBarContributor().setActiveEditor(DsEditor.this);

					setCurrentViewer(contentOutlineViewer);
				}
			} else if (p instanceof PropertySheet) {
				if (((PropertySheet) p).getCurrentPage() == propertySheetPage) {
					getActionBarContributor().setActiveEditor(DsEditor.this);
					handleActivate();
				}
			} else if (p == DsEditor.this) {
				handleActivate();
			}
		}

		public void partBroughtToTop(IWorkbenchPart p) {
			// Ignore.
		}

		public void partClosed(IWorkbenchPart p) {
			// Ignore.
		}

		public void partDeactivated(IWorkbenchPart p) {
			// Ignore.
		}

		public void partOpened(IWorkbenchPart p) {
			// Ignore.
		}
	};

	/**
	 * Resources that have been removed since last activation. <!--
	 * begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	protected Collection<Resource> removedResources = new ArrayList<Resource>();

	/**
	 * Resources that have been changed since last activation. <!--
	 * begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	protected Collection<Resource> changedResources = new ArrayList<Resource>();

	/**
	 * Resources that have been saved. <!-- begin-user-doc --> <!-- end-user-doc
	 * -->
	 * 
	 * @generated
	 */
	protected Collection<Resource> savedResources = new ArrayList<Resource>();

	/**
	 * Map to store the diagnostic associated with a resource. <!--
	 * begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	protected Map<Resource, Diagnostic> resourceToDiagnosticMap =
	                                                              new LinkedHashMap<Resource, Diagnostic>();

	/**
	 * Controls whether the problem indication should be updated. <!--
	 * begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	protected boolean updateProblemIndication = true;

	/**
	 * Adapter used to update the problem indication when resources are demanded
	 * loaded.
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	protected EContentAdapter problemIndicationAdapter = new EContentAdapter() {

		public void notifyChanged(Notification notification) {
			if (notification.getNotifier() instanceof Resource) {
				switch (notification.getFeatureID(Resource.class)) {
					case Resource.RESOURCE__IS_LOADED:
					case Resource.RESOURCE__ERRORS:
					case Resource.RESOURCE__WARNINGS: {
						Resource resource = (Resource) notification.getNotifier();
						Diagnostic diagnostic = analyzeResourceProblems(resource, null);
						if (diagnostic.getSeverity() != Diagnostic.OK) {
							resourceToDiagnosticMap.put(resource, diagnostic);
						} else {
							resourceToDiagnosticMap.remove(resource);
						}

						if (updateProblemIndication) {
							getSite().getShell().getDisplay().asyncExec(new Runnable() {
								public void run() {
									updateProblemIndication();
								}
							});
						}
						break;
					}
				}
			} else {
				super.notifyChanged(notification);
			}
		}

		protected void setTarget(Resource target) {
			basicSetTarget(target);
		}

		protected void unsetTarget(Resource target) {
			basicUnsetTarget(target);
		}
	};

	/**
	 * This listens for workspace changes.
	 * <!-- begin-user-doc --> <!--
	 * end-user-doc -->
	 * 
	 * @generated
	 */
	protected IResourceChangeListener resourceChangeListener = new IResourceChangeListener() {
		public void resourceChanged(IResourceChangeEvent event) {
			IResourceDelta delta = event.getDelta();
			try {
				class ResourceDeltaVisitor implements IResourceDeltaVisitor {
					protected ResourceSet resourceSet = editingDomain.getResourceSet();
					protected Collection<Resource> changedResources = new ArrayList<Resource>();
					protected Collection<Resource> removedResources = new ArrayList<Resource>();

					public boolean visit(IResourceDelta delta) {
						if (delta.getResource().getType() == IResource.FILE) {
							if (delta.getKind() == IResourceDelta.REMOVED ||
							    delta.getKind() == IResourceDelta.CHANGED &&
							    delta.getFlags() != IResourceDelta.MARKERS) {
								Resource resource =
								                    resourceSet.getResource(URI.createPlatformResourceURI(delta.getFullPath()
								                                                                               .toString(),
								                                                                          true),
								                                            false);
								if (resource != null) {
									if (delta.getKind() == IResourceDelta.REMOVED) {
										removedResources.add(resource);
									} else if (!savedResources.remove(resource)) {
										changedResources.add(resource);
									}
								}
							}
						}

						return true;
					}

					public Collection<Resource> getChangedResources() {
						return changedResources;
					}

					public Collection<Resource> getRemovedResources() {
						return removedResources;
					}
				}

				final ResourceDeltaVisitor visitor = new ResourceDeltaVisitor();
				delta.accept(visitor);

				if (!visitor.getRemovedResources().isEmpty()) {
					getSite().getShell().getDisplay().asyncExec(new Runnable() {
						public void run() {
							removedResources.addAll(visitor.getRemovedResources());
							if (!isDirty()) {
								getSite().getPage().closeEditor(DsEditor.this, false);
							}
						}
					});
				}

				if (!visitor.getChangedResources().isEmpty()) {
					getSite().getShell().getDisplay().asyncExec(new Runnable() {
						public void run() {
							changedResources.addAll(visitor.getChangedResources());
							if (getSite().getPage().getActiveEditor() == DsEditor.this) {
								handleActivate();
							}
						}
					});
				}
			} catch (CoreException exception) {
				DsEditorPlugin.INSTANCE.log(exception);
			}
		}
	};

	/**
	 * Handles activation of the editor or it's associated views. <!--
	 * begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	protected void handleActivate() {
		// Recompute the read only state.
		//
		if (editingDomain.getResourceToReadOnlyMap() != null) {
			editingDomain.getResourceToReadOnlyMap().clear();

			// Refresh any actions that may become enabled or disabled.
			//
			setSelection(getSelection());
		}

		if (!removedResources.isEmpty()) {
			if (handleDirtyConflict()) {
				getSite().getPage().closeEditor(DsEditor.this, false);
			} else {
				removedResources.clear();
				changedResources.clear();
				savedResources.clear();
			}
		} else if (!changedResources.isEmpty()) {
			changedResources.removeAll(savedResources);
			handleChangedResources();
			changedResources.clear();
			savedResources.clear();
		}
	}

	/**
	 * Handles what to do with changed resources on activation. <!--
	 * begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	protected void handleChangedResources() {
		if (!changedResources.isEmpty() && (!isDirty() || handleDirtyConflict())) {
			if (isDirty()) {
				changedResources.addAll(editingDomain.getResourceSet().getResources());
			}
			editingDomain.getCommandStack().flush();

			updateProblemIndication = false;
			for (Resource resource : changedResources) {
				if (resource.isLoaded()) {
					resource.unload();
					try {
						resource.load(Collections.EMPTY_MAP);
					} catch (IOException exception) {
						if (!resourceToDiagnosticMap.containsKey(resource)) {
							resourceToDiagnosticMap.put(resource,
							                            analyzeResourceProblems(resource, exception));
						}
					}
				}
			}

			if (AdapterFactoryEditingDomain.isStale(editorSelection)) {
				setSelection(StructuredSelection.EMPTY);
			}

			updateProblemIndication = true;
			updateProblemIndication();
		}
	}

	/**
	 * Updates the problems indication with the information described in the
	 * specified diagnostic.
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	protected void updateProblemIndication() {
		if (updateProblemIndication) {
			BasicDiagnostic diagnostic =
			                             new BasicDiagnostic(
			                                                 Diagnostic.OK,
			                                                 "org.wso2.developerstudio.eclipse.ds.editor",
			                                                 0,
			                                                 null,
			                                                 new Object[] { editingDomain.getResourceSet() });
			for (Diagnostic childDiagnostic : resourceToDiagnosticMap.values()) {
				if (childDiagnostic.getSeverity() != Diagnostic.OK) {
					diagnostic.add(childDiagnostic);
				}
			}

			int lastEditorPage = getPageCount() - 1;
			if (lastEditorPage >= 0 && getEditor(lastEditorPage) instanceof ProblemEditorPart) {
				((ProblemEditorPart) getEditor(lastEditorPage)).setDiagnostic(diagnostic);
				if (diagnostic.getSeverity() != Diagnostic.OK) {
					setActivePage(lastEditorPage);
				}
			} else if (diagnostic.getSeverity() != Diagnostic.OK) {
				ProblemEditorPart problemEditorPart = new ProblemEditorPart();
				problemEditorPart.setDiagnostic(diagnostic);
				problemEditorPart.setMarkerHelper(markerHelper);
				try {
					addPage(++lastEditorPage, problemEditorPart, getEditorInput());
					setPageText(lastEditorPage, problemEditorPart.getPartName());
					setActivePage(lastEditorPage);
					showTabs();
				} catch (PartInitException exception) {
					DsEditorPlugin.INSTANCE.log(exception);
				}
			}

			if (markerHelper.hasMarkers(editingDomain.getResourceSet())) {
				markerHelper.deleteMarkers(editingDomain.getResourceSet());
				if (diagnostic.getSeverity() != Diagnostic.OK) {
					try {
						markerHelper.createMarkers(diagnostic);
					} catch (CoreException exception) {
						DsEditorPlugin.INSTANCE.log(exception);
					}
				}
			}
		}
	}

	/**
	 * Shows a dialog that asks if conflicting changes should be discarded. <!--
	 * begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @return true, if successful
	 * @generated
	 */
	protected boolean handleDirtyConflict() {
		return MessageDialog.openQuestion(getSite().getShell(),
		                                  getString("_UI_FileConflict_label"),
		                                  getString("_WARN_FileConflict"));
	}

	/**
	 * This creates a model editor. <!-- begin-user-doc --> <!-- end-user-doc
	 * -->
	 * 
	 * @generated
	 */
	public DsEditor() {
		super();
		initializeEditingDomain();
		dsEditor = this;
	}

	/**
	 * This is called during startup. <!-- begin-user-doc --> <!-- end-user-doc
	 * -->
	 * 
	 * @param site
	 *            the site
	 * @param editorInput
	 *            the editor input
	 * @generated NOT
	 */

	public void init(IEditorSite site, IEditorInput editorInput) {
		setSite(site);
		setInputWithNotify(editorInput);
		setPartName(editorInput.getName());
		site.setSelectionProvider(this);
		site.getPage().addPartListener(partListener);
		ResourcesPlugin.getWorkspace().addResourceChangeListener(resourceChangeListener,
		                                                         IResourceChangeEvent.POST_CHANGE);
		createModel();
		setSavingProcOk(true);

	}

	/**
	 * This sets up the editing domain for the model editor.
	 * <!-- begin-user-doc
	 * --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	protected void initializeEditingDomain() {
		// Create an adapter factory that yields item providers.
		//
		adapterFactory =
		                 new ComposedAdapterFactory(
		                                            ComposedAdapterFactory.Descriptor.Registry.INSTANCE);

		adapterFactory.addAdapterFactory(new ResourceItemProviderAdapterFactory());
		adapterFactory.addAdapterFactory(new DsItemProviderAdapterFactory());
		adapterFactory.addAdapterFactory(new ReflectiveItemProviderAdapterFactory());

		// Create the command stack that will notify this editor as commands are
		// executed.
		//
		BasicCommandStack commandStack = new BasicCommandStack();

		// Add a listener to set the most recent command's affected objects to
		// be the selection of the viewer with focus.
		//
		commandStack.addCommandStackListener(new CommandStackListener() {
			public void commandStackChanged(final EventObject event) {
				getContainer().getDisplay().asyncExec(new Runnable() {
					public void run() {
						firePropertyChange(IEditorPart.PROP_DIRTY);

						// Try to select the affected objects.
						//
						Command mostRecentCommand =
						                            ((CommandStack) event.getSource()).getMostRecentCommand();
						if (mostRecentCommand != null && mdPage != null &&
						    mdPage.getOutLineBlock() != null) {
							mdPage.getOutLineBlock()
							      .setSelectionToViewer(mostRecentCommand.getAffectedObjects());
						}
						if (propertySheetPage != null &&
						    !propertySheetPage.getControl().isDisposed()) {
							propertySheetPage.refresh();
						}
					}
				});
			}
		});

		// Create the editing domain with a special command stack.
		//
		editingDomain =
		                new AdapterFactoryEditingDomain(adapterFactory, commandStack,
		                                                new HashMap<Resource, Boolean>());
	}

	/**
	 * This is here for the listener to be able to call it.
	 * <!-- begin-user-doc
	 * --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */

	protected void firePropertyChange(int action) {
		super.firePropertyChange(action);
	}

	/**
	 * This sets the selection into whichever viewer is active. <!--
	 * begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @param collection
	 *            the new selection to viewer
	 * @generated
	 */
	public void setSelectionToViewer(Collection<?> collection) {
		final Collection<?> theSelection = collection;
		// Make sure it's okay.
		//
		if (theSelection != null && !theSelection.isEmpty()) {
			Runnable runnable = new Runnable() {
				public void run() {
					// Try to select the items in the current content viewer of
					// the editor.
					//
					if (currentViewer != null) {
						currentViewer.setSelection(new StructuredSelection(theSelection.toArray()),
						                           true);
					}
				}
			};
			getSite().getShell().getDisplay().asyncExec(runnable);
		}
	}

	/**
	 * This returns the editing domain as required by the
	 * {@link IEditingDomainProvider} interface.
	 * This is important for implementing the static methods of
	 * {@link AdapterFactoryEditingDomain} and for supporting
	 * {@link org.eclipse.emf.edit.ui.action.CommandAction}.
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	public EditingDomain getEditingDomain() {
		return editingDomain;
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->.
	 * 
	 * @generated
	 */
	public class ReverseAdapterFactoryContentProvider extends AdapterFactoryContentProvider {

		/**
		 * <!-- begin-user-doc --> <!-- end-user-doc -->.
		 * 
		 * @param adapterFactory
		 *            the adapter factory
		 * @generated
		 */
		public ReverseAdapterFactoryContentProvider(AdapterFactory adapterFactory) {
			super(adapterFactory);
		}

		/**
		 * <!-- begin-user-doc --> <!-- end-user-doc -->.
		 * 
		 * @param object
		 *            the object
		 * @return the elements
		 * @generated
		 */

		public Object[] getElements(Object object) {
			Object parent = super.getParent(object);
			return (parent == null ? Collections.EMPTY_SET : Collections.singleton(parent)).toArray();
		}

		/**
		 * <!-- begin-user-doc --> <!-- end-user-doc -->.
		 * 
		 * @param object
		 *            the object
		 * @return the children
		 * @generated
		 */

		public Object[] getChildren(Object object) {
			Object parent = super.getParent(object);
			return (parent == null ? Collections.EMPTY_SET : Collections.singleton(parent)).toArray();
		}

		/**
		 * <!-- begin-user-doc --> <!-- end-user-doc -->.
		 * 
		 * @param object
		 *            the object
		 * @return true, if successful
		 * @generated
		 */

		public boolean hasChildren(Object object) {
			Object parent = super.getParent(object);
			return parent != null;
		}

		/**
		 * <!-- begin-user-doc --> <!-- end-user-doc -->.
		 * 
		 * @param object
		 *            the object
		 * @return the parent
		 * @generated
		 */

		public Object getParent(Object object) {
			return null;
		}
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->.
	 * 
	 * @param viewerPane
	 *            the new current viewer pane
	 * @generated
	 */
	public void setCurrentViewerPane(ViewerPane viewerPane) {
		if (currentViewerPane != viewerPane) {
			if (currentViewerPane != null) {
				currentViewerPane.showFocus(false);
			}
			currentViewerPane = viewerPane;
		}
		setCurrentViewer(currentViewerPane.getViewer());
	}

	/**
	 * This makes sure that one content viewer, either for the current page or
	 * the outline view, if it has focus, is the current one. <!--
	 * begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @param viewer
	 *            the new current viewer
	 * @generated
	 */
	public void setCurrentViewer(Viewer viewer) {
		// If it is changing...
		//
		if (currentViewer != viewer) {
			if (selectionChangedListener == null) {
				// Create the listener on demand.
				//
				selectionChangedListener = new ISelectionChangedListener() {
					// This just notifies those things that are affected by the
					// section.
					//
					public void selectionChanged(SelectionChangedEvent selectionChangedEvent) {
						setSelection(selectionChangedEvent.getSelection());
					}
				};
			}

			// Stop listening to the old one.
			//
			if (currentViewer != null) {
				currentViewer.removeSelectionChangedListener(selectionChangedListener);
			}

			// Start listening to the new one.
			//
			if (viewer != null) {
				viewer.addSelectionChangedListener(selectionChangedListener);
			}

			// Remember it.
			//
			currentViewer = viewer;

			// Set the editors selection based on the current viewer's
			// selection.
			//
			setSelection(currentViewer == null ? StructuredSelection.EMPTY
			                                  : currentViewer.getSelection());
		}
	}

	/**
	 * This returns the viewer as required by the {@link IViewerProvider}
	 * interface.
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	public Viewer getViewer() {
		return currentViewer;
	}

	/**
	 * This creates a context menu for the viewer and adds a listener as well
	 * registering the menu for extension.
	 * <!-- begin-user-doc --> <!--
	 * end-user-doc -->
	 * 
	 * @generated
	 */
	public void createContextMenuFor(StructuredViewer viewer) {
		MenuManager contextMenu = new MenuManager("#PopUp");
		contextMenu.add(new Separator("additions"));
		contextMenu.setRemoveAllWhenShown(true);
		contextMenu.addMenuListener(this);
		Menu menu = contextMenu.createContextMenu(viewer.getControl());
		viewer.getControl().setMenu(menu);
		getSite().registerContextMenu(contextMenu, new UnwrappingSelectionProvider(viewer));

		int dndOperations = DND.DROP_COPY | DND.DROP_MOVE | DND.DROP_LINK;
		Transfer[] transfers = new Transfer[] { LocalTransfer.getInstance() };
		viewer.addDragSupport(dndOperations, transfers, new ViewerDragAdapter(viewer));
		viewer.addDropSupport(dndOperations, transfers,
		                      new EditingDomainViewerDropAdapter(editingDomain, viewer));
	}

	/**
	 * This is the method called to load a resource into the editing domain's
	 * resource set based on the editor's input.
	 * <!-- begin-user-doc --> <!--
	 * end-user-doc -->
	 * 
	 * @throws PartInitException
	 * @generated NOT
	 */
	public void createModel() {
		URI initialResourceURI = EditUIUtil.getURI(getEditorInput());
		Exception exception = null;
		Resource resource = null;
		try {
			// Load the resource through the editing domain.
			//
			resource = editingDomain.getResourceSet().getResource(initialResourceURI, true);
		} catch (Exception e) {
			exception = e;
			resource = editingDomain.getResourceSet().getResource(initialResourceURI, false);
		}

		try {
			resource.load(Collections.EMPTY_MAP);
			EList<EObject> contents = resource.getContents();

			if (!contents.isEmpty() && contents.get(0) instanceof DocumentRoot) {
				dataService = ((DocumentRoot) contents.get(0)).getData();

			}
		} catch (IOException e) {
			MessageDialog.openError(Display.getCurrent().getActiveShell(), "Error",
			                        "Can not load Data Service Configuration");
		}

		Diagnostic diagnostic = analyzeResourceProblems(resource, exception);
		if (diagnostic.getSeverity() != Diagnostic.OK) {
			resourceToDiagnosticMap.put(resource, analyzeResourceProblems(resource, exception));
		}
		editingDomain.getResourceSet().eAdapters().add(problemIndicationAdapter);
	}

	/**
	 * Adding pages to the DsEditor
	 * 
	 */

	protected void addPages() {
		try {

			mdPage = new MasterDetailsPage(this, adapterFactory, editingDomain);
			addPage(DESIGN_VIEW_INDEX, mdPage);
			setPageText(DESIGN_VIEW_INDEX, "Outline");

			sourceEditor = new DsObjectSourceEditor(this, editingDomain);
			addPage(SOURCE_VIEW_INDEX, sourceEditor.getEditor(), sourceEditor.getInput());
			setPageText(SOURCE_VIEW_INDEX, "Source");
			sourceEditor.init();

			dataSourcePage = new DataSourcePage(this, dataService);
			addPage(DATA_SOURCE_PAGE_INDEX, dataSourcePage);
			setPageText(DATA_SOURCE_PAGE_INDEX, "Data Sources");

			addDesignViewAction();

		} catch (PartInitException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Returns a diagnostic describing the errors and warnings listed in the
	 * resource
	 * and the specified exception (if any).
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	public Diagnostic analyzeResourceProblems(Resource resource, Exception exception) {
		if (!resource.getErrors().isEmpty() || !resource.getWarnings().isEmpty()) {
			BasicDiagnostic basicDiagnostic =
			                                  new BasicDiagnostic(
			                                                      Diagnostic.ERROR,
			                                                      "org.wso2.developerstudio.eclipse.ds.editor",
			                                                      0,
			                                                      getString("_UI_CreateModelError_message",
			                                                                resource.getURI()),
			                                                      new Object[] { exception == null
			                                                                                      ? (Object) resource
			                                                                                      : exception });
			basicDiagnostic.merge(EcoreUtil.computeDiagnostic(resource, true));
			return basicDiagnostic;
		} else if (exception != null) {
			return new BasicDiagnostic(
			                           Diagnostic.ERROR,
			                           "org.wso2.developerstudio.eclipse.ds.editor",
			                           0,
			                           getString("_UI_CreateModelError_message", resource.getURI()),
			                           new Object[] { exception });
		} else {
			return Diagnostic.OK_INSTANCE;
		}
	}

	/**
	 * If there is just one page in the multi-page editor part,
	 * this hides the single tab at the bottom.
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	protected void hideTabs() {
		if (getPageCount() <= 1) {
			setPageText(0, "");
			if (getContainer() instanceof CTabFolder) {
				((CTabFolder) getContainer()).setTabHeight(1);
				Point point = getContainer().getSize();
				getContainer().setSize(point.x, point.y + 6);
			}
		}
	}

	/**
	 * If there is more than one page in the multi-page editor part,
	 * this shows the tabs at the bottom.
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	protected void showTabs() {
		if (getPageCount() > 1) {
			setPageText(0, getString("_UI_SelectionPage_label"));
			if (getContainer() instanceof CTabFolder) {
				((CTabFolder) getContainer()).setTabHeight(SWT.DEFAULT);
				Point point = getContainer().getSize();
				getContainer().setSize(point.x, point.y - 6);
			}
		}
	}

	/**
	 * This is used to track the active viewer.
	 * <!-- begin-user-doc --> <!--
	 * end-user-doc -->
	 * 
	 * @generated NOT
	 */

	protected void pageChange(int pageIndex) {
		super.pageChange(pageIndex);

		switch (pageIndex) {
			case DESIGN_VIEW_INDEX: {

				designViewActivated = true;
				

				break;
			}
			case SOURCE_VIEW_INDEX: {

				designViewActivated = false;

				break;
			}
			case DATA_SOURCE_PAGE_INDEX: {

				designViewActivated = false;
				if (dataService != null && dataService.getConfig() != null &&
				    !dataService.getConfig().isEmpty())
					dataSourcePage.updateDataSourceViewer();
			}
		}

		if (contentOutlinePage != null) {
			handleContentOutlineSelection(contentOutlinePage.getSelection());
		}
	}

	/**
	 * Handle the design activated event and make sure the model
	 * rebuild with the newly changes.
	 * 
	 */
	private void saveSourceViewChanges() {

		setSavingProcOk(true);
		if (sourceEditor.isTmpFileHasContent() && sourceEditor.isModelChanged()) {
			DocumentRootImpl nwroot = null;
			DataService nwdata = null;
			DocumentRoot oldroot = null;

			try {
				domainResource = sourceEditor.reconstructModel();
				if (domainResource != null) {
					if (domainResource.getContents() != null &&
					    domainResource.getContents().size() != 0) {
						if (domainResource.getContents().get(0) != null) {
							if (domainResource.getContents().get(0) instanceof DocumentRootImpl)
								nwroot = (DocumentRootImpl) domainResource.getContents().get(0);

							if (nwroot != null) {
								if (nwroot.getData() != null) {
									nwdata = nwroot.getData();
								}
							}
						}
					}
				}

				if (editingDomain != null) {
					if (editingDomain.getResourceSet() != null) {
						if (editingDomain.getResourceSet().getResources() != null &&
						    editingDomain.getResourceSet().getResources().size() != 0) {
							if (editingDomain.getResourceSet().getResources().get(0) != null) {
								if (editingDomain.getResourceSet().getResources().get(0)
								                 .getContents() != null &&
								    editingDomain.getResourceSet().getResources().get(0)
								                 .getContents().size() != 0) {
									if (editingDomain.getResourceSet().getResources().get(0)
									                 .getContents().get(0) != null) {
										oldroot =
										          (DocumentRoot) editingDomain.getResourceSet()
										                                      .getResources()
										                                      .get(0).getContents()
										                                      .get(0);

									}
								}
							}
						}
					}
				}

				SetCommand addDataCommand =
				                            new SetCommand(editingDomain, oldroot,
				                                           DsPackage.Literals.DOCUMENT_ROOT__DATA,
				                                           nwdata);
				if (addDataCommand.canExecute()) {
					editingDomain.getCommandStack().execute(addDataCommand);
					mdPage.getOutLineBlock().getViewer().setAutoExpandLevel(TreeViewer.ALL_LEVELS);

					updateAllPagesWithNewDataServiceObject(nwdata);
				} else {
					//
				}

			} catch (Exception e) {
				MessageDialog.openError(Display.getCurrent().getActiveShell(),
				                        "Incomplete Data Service Configuration", e.getMessage());
				setSavingProcOk(false);
				this.setActivePage(SOURCE_VIEW_INDEX);
			}
		}
	}

	/**
	 * This is how the framework determines which interfaces we implement. <!--
	 * begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @param key
	 *            the key
	 * @return the adapter
	 * @generated
	 */
	@SuppressWarnings("rawtypes")
	public Object getAdapter(Class key) {
		if (key.equals(IContentOutlinePage.class)) {
			return showOutlineView() ? getContentOutlinePage() : null;
		} else if (key.equals(IPropertySheetPage.class)) {
			return getPropertySheetPage();
		} else if (key.equals(IGotoMarker.class)) {
			return this;
		} else {
			return super.getAdapter(key);
		}
	}

	/**
	 * This accesses a cached version of the content outliner. <!--
	 * begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @return the content outline page
	 * @generated
	 */
	public IContentOutlinePage getContentOutlinePage() {
		if (contentOutlinePage == null) {
			// The content outline is just a tree.
			//
			class MyContentOutlinePage extends ContentOutlinePage {

				public void createControl(Composite parent) {
					super.createControl(parent);
					contentOutlineViewer = getTreeViewer();
					contentOutlineViewer.addSelectionChangedListener(this);

					// Set up the tree viewer.
					//
					contentOutlineViewer.setContentProvider(new AdapterFactoryContentProvider(
					                                                                          adapterFactory));
					contentOutlineViewer.setLabelProvider(new AdapterFactoryLabelProvider(
					                                                                      adapterFactory));
					contentOutlineViewer.setInput(editingDomain.getResourceSet());

					// Make sure our popups work.
					//
					createContextMenuFor(contentOutlineViewer);

					if (!editingDomain.getResourceSet().getResources().isEmpty()) {
						// Select the root object in the view.
						//
						contentOutlineViewer.setSelection(new StructuredSelection(
						                                                          editingDomain.getResourceSet()
						                                                                       .getResources()
						                                                                       .get(0)),
						                                  true);
					}
				}

				public void makeContributions(IMenuManager menuManager,
				                              IToolBarManager toolBarManager,
				                              IStatusLineManager statusLineManager) {
					super.makeContributions(menuManager, toolBarManager, statusLineManager);
					contentOutlineStatusLineManager = statusLineManager;
				}

				public void setActionBars(IActionBars actionBars) {
					super.setActionBars(actionBars);
					getActionBarContributor().shareGlobalActions(this, actionBars);
				}
			}

			contentOutlinePage = new MyContentOutlinePage();

			// Listen to selection so that we can handle it is a special way.
			//
			contentOutlinePage.addSelectionChangedListener(new ISelectionChangedListener() {
				// This ensures that we handle selections correctly.
				//
				public void selectionChanged(SelectionChangedEvent event) {
					handleContentOutlineSelection(event.getSelection());
				}
			});
		}

		return contentOutlinePage;
	}

	/**
	 * This accesses a cached version of the property sheet. <!-- begin-user-doc
	 * --> <!-- end-user-doc -->
	 * 
	 * @return the property sheet page
	 * @generated NOT
	 */
	public IPropertySheetPage getPropertySheetPage() {
		if (propertySheetPage == null) {
			propertySheetPage = new CustomExtendedPropertySheetPage(editingDomain);
			propertySheetPage.setPropertySourceProvider(new CustomAdapterFactoryContentProvider(
			                                                                                    adapterFactory));
		}

		return propertySheetPage;
	}

	/**
	 * Customized property sheet sorting.
	 */
	private class CustomExtendedPropertySheetPage extends ExtendedPropertySheetPage {

		/**
		 * The Class CustomPropertySheetSorter.
		 */
		private class CustomPropertySheetSorter extends PropertySheetSorter {

			/**
			 * Sort.
			 * 
			 * @param entries
			 *            the entries {@inheritDoc}
			 */
			public void sort(IPropertySheetEntry[] entries) {
				// Do nothing.
			}
		}

		/**
		 * Instantiates a new custom extended property sheet page.
		 * 
		 * @param editingDomain
		 *            the editing domain
		 */
		public CustomExtendedPropertySheetPage(AdapterFactoryEditingDomain editingDomain) {
			super(editingDomain);
			setSorter(new CustomPropertySheetSorter());
		}

		/**
		 * Sets the selection to viewer.
		 * 
		 * @param selection
		 *            the new selection to viewer {@inheritDoc}
		 */
		public void setSelectionToViewer(List<?> selection) {
			DsEditor.this.setSelectionToViewer(selection);
			DsEditor.this.setFocus();
		}

		/**
		 * Sets the action bars.
		 * 
		 * @param actionBars
		 *            the new action bars {@inheritDoc}
		 */
		public void setActionBars(IActionBars actionBars) {
			super.setActionBars(actionBars);
			getActionBarContributor().shareGlobalActions(this, actionBars);
		}

		/**
		 * Sets the sorter.
		 * 
		 * @param sorter
		 *            the new sorter {@inheritDoc}
		 */
		protected void setSorter(PropertySheetSorter sorter) {
			if (sorter instanceof CustomPropertySheetSorter) {
				sorter = new CustomPropertySheetSorter();
			}
			super.setSorter(sorter);
		}
	}

	/**
	 * This deals with how we want selection in the outliner to affect the other
	 * views.
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	public void handleContentOutlineSelection(ISelection selection) {
		if (currentViewerPane != null && !selection.isEmpty() &&
		    selection instanceof IStructuredSelection) {
			Iterator<?> selectedElements = ((IStructuredSelection) selection).iterator();
			if (selectedElements.hasNext()) {
				// Get the first selected element.
				//
				Object selectedElement = selectedElements.next();

				// If it's the selection viewer, then we want it to select the
				// same selection as this selection.
				//
				if (currentViewerPane.getViewer() == selectionViewer) {
					ArrayList<Object> selectionList = new ArrayList<Object>();
					selectionList.add(selectedElement);
					while (selectedElements.hasNext()) {
						selectionList.add(selectedElements.next());
					}

					// Set the selection to the widget.
					//
					selectionViewer.setSelection(new StructuredSelection(selectionList));
				} else {
					// Set the input to the widget.
					//
					if (currentViewerPane.getViewer().getInput() != selectedElement) {
						currentViewerPane.getViewer().setInput(selectedElement);
						currentViewerPane.setTitle(selectedElement);
					}
				}
			}
		}
	}

	/**
	 * This is for implementing {@link IEditorPart} and simply tests the command
	 * stack.
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated NOT
	 */

	public boolean isDirty() {

		if (((BasicCommandStack) editingDomain.getCommandStack()).isSaveNeeded() ||
		    isSourceModified) {
			return true;
		} else {
			return false;
		}

	}

	/**
	 * This is for implementing {@link IEditorPart} and simply saves the model
	 * file.
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated NOT
	 */

	public void doSave(IProgressMonitor progressMonitor) {
		// Save only resources that have actually changed.
		//
		if (this.getActivePage() == SOURCE_VIEW_INDEX) {
			saveSourceViewChanges();
		}

		if (isSavingProcOk()) {

			final Map<Object, Object> saveOptions = new HashMap<Object, Object>();
			saveOptions.put(Resource.OPTION_SAVE_ONLY_IF_CHANGED,
			                Resource.OPTION_SAVE_ONLY_IF_CHANGED_MEMORY_BUFFER);

			// Do the work within an operation because this is a long running
			// activity that modifies the workbench.
			//
			WorkspaceModifyOperation operation = new WorkspaceModifyOperation() {
				// This is the method that gets invoked when the operation runs.
				//

				public void execute(IProgressMonitor monitor) {
					// Save the resources to the file system.
					//
					boolean first = true;
					for (Resource resource : editingDomain.getResourceSet().getResources()) {
						if ((first || !resource.getContents().isEmpty() || isPersisted(resource)) &&
						    !editingDomain.isReadOnly(resource)) {
							try {
								long timeStamp = resource.getTimeStamp();
								resource.save(saveOptions);
								if (resource.getTimeStamp() != timeStamp) {
									savedResources.add(resource);
								}
							} catch (Exception exception) {
								resourceToDiagnosticMap.put(resource,
								                            analyzeResourceProblems(resource,
								                                                    exception));
							}
							first = false;
						}
					}
				}
			};

			updateProblemIndication = false;
			try {
				// This runs the options, and shows progress.
				//
				new ProgressMonitorDialog(getSite().getShell()).run(true, false, operation);

				// Refresh the necessary state.
				//
				((BasicCommandStack) editingDomain.getCommandStack()).saveIsDone();

				if (isSourceModified)
					setSourceModified(false);
				firePropertyChange(IEditorPart.PROP_DIRTY);

			} catch (Exception exception) {
				// Something went wrong that shouldn't.
				//
				DsEditorPlugin.INSTANCE.log(exception);
			}
			updateProblemIndication = true;
			updateProblemIndication();

			if (designViewActivated && !isSourceModified) {

				try {
					
					deleteObjectsFromTheModel();
					sourceEditor.getEditor().getDocumentProvider().resetDocument(getEditorInput());
				} catch (CoreException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}

	}

	private void deleteObjectsFromTheModel() {
		if(dataService.getFeatureAllowRoles() != null)
		{
		if(StringUtils.isEmpty(dataService.getFeatureAllowRoles().getValue())){
			SetCommand setCmd = new SetCommand(editingDomain, dataService,
					DsPackage.Literals.DATA_SERVICE__FEATURE_ALLOW_ROLES,
					null);
			if (setCmd.canExecute()) {
				editingDomain.getCommandStack().execute(setCmd);
			}
		}
		}
		if(dataService.getPolicy() != null){
			if(StringUtils.isEmpty(dataService.getPolicy().getKey())){
				SetCommand setCmd = new SetCommand(editingDomain, dataService,
						DsPackage.Literals.DATA_SERVICE__POLICY,
						null);
				if (setCmd.canExecute()) {
					editingDomain.getCommandStack().execute(setCmd);
				}
			}
		}
		
	}

	/**
	 * This returns whether something has been persisted to the URI of the
	 * specified resource.
	 * The implementation uses the URI converter from the editor's resource set
	 * to try to open an input stream.
	 * <!-- begin-user-doc
	 * --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	protected boolean isPersisted(Resource resource) {
		boolean result = false;
		try {
			InputStream stream =
			                     editingDomain.getResourceSet().getURIConverter()
			                                  .createInputStream(resource.getURI());
			if (stream != null) {
				result = true;
				stream.close();
			}
		} catch (IOException e) {
			// Ignore
		}
		return result;
	}

	/**
	 * This always returns true because it is not currently supported. <!--
	 * begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @return true, if is save as allowed
	 * @generated
	 */

	public boolean isSaveAsAllowed() {
		return true;
	}

	/**
	 * This also changes the editor's input.
	 * <!-- begin-user-doc --> <!--
	 * end-user-doc -->
	 * 
	 * @generated
	 */

	public void doSaveAs() {
		SaveAsDialog saveAsDialog = new SaveAsDialog(getSite().getShell());
		saveAsDialog.open();
		IPath path = saveAsDialog.getResult();
		if (path != null) {
			IFile file = ResourcesPlugin.getWorkspace().getRoot().getFile(path);
			if (file != null) {
				doSaveAs(URI.createPlatformResourceURI(file.getFullPath().toString(), true),
				         new FileEditorInput(file));
			}
		}
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->.
	 * 
	 * @param uri
	 *            the uri
	 * @param editorInput
	 *            the editor input
	 * @generated
	 */
	protected void doSaveAs(URI uri, IEditorInput editorInput) {
		(editingDomain.getResourceSet().getResources().get(0)).setURI(uri);
		setInputWithNotify(editorInput);
		setPartName(editorInput.getName());
		IProgressMonitor progressMonitor =
		                                   getActionBars().getStatusLineManager() != null
		                                                                                 ? getActionBars().getStatusLineManager()
		                                                                                                  .getProgressMonitor()
		                                                                                 : new NullProgressMonitor();
		doSave(progressMonitor);
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->.
	 * 
	 * @param marker
	 *            the marker
	 * @generated
	 */
	public void gotoMarker(IMarker marker) {
		try {
			if (marker.getType().equals(EValidator.MARKER)) {
				String uriAttribute = marker.getAttribute(EValidator.URI_ATTRIBUTE, null);
				if (uriAttribute != null) {
					URI uri = URI.createURI(uriAttribute);
					EObject eObject = editingDomain.getResourceSet().getEObject(uri, true);
					if (eObject != null) {
						setSelectionToViewer(Collections.singleton(editingDomain.getWrapper(eObject)));
					}
				}
			}
		} catch (CoreException exception) {
			DsEditorPlugin.INSTANCE.log(exception);
		}
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->.
	 * 
	 * @generated
	 */

	public void setFocus() {
		if (currentViewerPane != null) {
			currentViewerPane.setFocus();
		} else {
			getControl(getActivePage()).setFocus();
		}
	}

	/**
	 * This implements {@link org.eclipse.jface.viewers.ISelectionProvider}.
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	public void addSelectionChangedListener(ISelectionChangedListener listener) {
		selectionChangedListeners.add(listener);
	}

	/**
	 * This implements {@link org.eclipse.jface.viewers.ISelectionProvider}.
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	public void removeSelectionChangedListener(ISelectionChangedListener listener) {
		selectionChangedListeners.remove(listener);
	}

	/**
	 * This implements {@link org.eclipse.jface.viewers.ISelectionProvider} to
	 * return this editor's overall selection.
	 * <!-- begin-user-doc --> <!--
	 * end-user-doc -->
	 * 
	 * @generated
	 */
	public ISelection getSelection() {
		return editorSelection;
	}

	/**
	 * This implements {@link org.eclipse.jface.viewers.ISelectionProvider} to
	 * set this editor's overall selection.
	 * Calling this result will notify the listeners.
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	public void setSelection(ISelection selection) {
		editorSelection = selection;

		for (ISelectionChangedListener listener : selectionChangedListeners) {
			listener.selectionChanged(new SelectionChangedEvent(this, selection));
		}
		setStatusLineManager(selection);
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->.
	 * 
	 * @param selection
	 *            the new status line manager
	 * @generated
	 */
	public void setStatusLineManager(ISelection selection) {
		IStatusLineManager statusLineManager =
		                                       currentViewer != null &&
		                                               currentViewer == contentOutlineViewer
		                                                                                    ? contentOutlineStatusLineManager
		                                                                                    : getActionBars().getStatusLineManager();

		if (statusLineManager != null) {
			if (selection instanceof IStructuredSelection) {
				Collection<?> collection = ((IStructuredSelection) selection).toList();
				switch (collection.size()) {
					case 0: {
						statusLineManager.setMessage(getString("_UI_NoObjectSelected"));
						break;
					}
					case 1: {
						String text =
						              new AdapterFactoryItemDelegator(adapterFactory).getText(collection.iterator()
						                                                                                .next());
						statusLineManager.setMessage(getString("_UI_SingleObjectSelected", text));
						break;
					}
					default: {
						statusLineManager.setMessage(getString("_UI_MultiObjectSelected",
						                                       Integer.toString(collection.size())));
						break;
					}
				}
			} else {
				statusLineManager.setMessage("");
			}
		}
	}

	/**
	 * This looks up a string in the plugin's plugin.properties file. <!--
	 * begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @param key
	 *            the key
	 * @return the string
	 * @generated
	 */
	private static String getString(String key) {
		return DsEditorPlugin.INSTANCE.getString(key);
	}

	/**
	 * This looks up a string in plugin.properties, making a substitution. <!--
	 * begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @param key
	 *            the key
	 * @param s1
	 *            the s1
	 * @return the string
	 * @generated
	 */
	private static String getString(String key, Object s1) {
		return DsEditorPlugin.INSTANCE.getString(key, new Object[] { s1 });
	}

	/**
	 * This implements {@link org.eclipse.jface.action.IMenuListener} to help
	 * fill the context menus with contributions from the Edit menu. <!--
	 * begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @param menuManager
	 *            the menu manager
	 * @generated
	 */
	public void menuAboutToShow(IMenuManager menuManager) {
		((IMenuListener) getEditorSite().getActionBarContributor()).menuAboutToShow(menuManager);
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->.
	 * 
	 * @return the action bar contributor
	 * @generated
	 */
	public EditingDomainActionBarContributor getActionBarContributor() {
		return (EditingDomainActionBarContributor) getEditorSite().getActionBarContributor();
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->.
	 * 
	 * @return the action bars
	 * @generated
	 */
	public IActionBars getActionBars() {
		return getActionBarContributor().getActionBars();
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->.
	 * 
	 * @return the adapter factory
	 * @generated
	 */
	public AdapterFactory getAdapterFactory() {
		return adapterFactory;
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->.
	 * 
	 * @generated NOT
	 */

	public void dispose() {
		updateProblemIndication = false;

		ResourcesPlugin.getWorkspace().removeResourceChangeListener(resourceChangeListener);

		getSite().getPage().removePartListener(partListener);

		adapterFactory.dispose();
		if (sourceEditor.getTempTag() != null) {
			sourceEditor.getTempTag().clearAndEnd();
		}

		if (getActionBarContributor().getActiveEditor() == this) {
			getActionBarContributor().setActiveEditor(null);
		}

		if (propertySheetPage != null) {
			propertySheetPage.dispose();
		}

		if (contentOutlinePage != null) {
			contentOutlinePage.dispose();
		}

		super.dispose();
	}

	/**
	 * Returns whether the outline view should be presented to the user. <!--
	 * begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @return true, if successful
	 * @generated
	 */
	protected boolean showOutlineView() {
		return true;
	}

	public void setSourceModified(boolean isSourceModified) {
		this.isSourceModified = isSourceModified;
	}

	public void fireTextPropertyChange() {
		firePropertyChange(IEditorPart.PROP_DIRTY);
	}

	public static int getDesignViewIndex() {
		return DESIGN_VIEW_INDEX;
	}

	public static int getSourceViewIndex() {
		return SOURCE_VIEW_INDEX;
	}

	/**
	 * update the all data service references to new data service object
	 * 
	 * @param newDataService
	 */
	private void updateAllPagesWithNewDataServiceObject(DataService newDataService) {

		this.dataService = newDataService;
		dataSourcePage.setDataService(newDataService);
	}

	public MasterDetailsPage getMdPage() {
		return mdPage;
	}

	public DataService getDataService() {
		return dataService;
	}

	private void addDesignViewAction() {

		designViewActionHandler = new DesignViewActionHandler();

		Display.getCurrent().addFilter(SWT.KeyDown, new Listener() {

			public void handleEvent(Event event) {
				IEditorPart editorPart =
				                         PlatformUI.getWorkbench().getActiveWorkbenchWindow()
				                                   .getActivePage().getActiveEditor();
				if (editorPart == DsEditor.this) {

					DsEditor dsEditor = (DsEditor) editorPart;

					if ((dsEditor.getActivePage() == DESIGN_VIEW_INDEX) &&
					    !DetailSectionUiUtil.isFocusedOnDetailSection) {
						if (event.keyCode == SWT.DEL) {

							designViewActionHandler.delete(dsEditor);
						}
					}
				}
			}
		});
	}

	public boolean isSavingProcOk() {
		return isSavingProcOk;
	}

	public void setSavingProcOk(boolean isSavingProcOk) {
		this.isSavingProcOk = isSavingProcOk;
	}

}
