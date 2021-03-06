package org.pustefixframework.ide.eclipse.plugin.ui.settings;

import java.util.Collection;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import org.eclipse.jface.util.Assert;

import org.eclipse.ui.dialogs.ISelectionStatusValidator;


/**
 * Implementation of a <code>ISelectionValidator</code> to validate the
 * type of an element.
 * Empty selections are not accepted.
 */
public class TypedElementSelectionValidator implements ISelectionStatusValidator {

	private IStatus fgErrorStatus= Status.CANCEL_STATUS;
	private IStatus fgOKStatus= Status.OK_STATUS;

	private Class[] fAcceptedTypes;
	private boolean fAllowMultipleSelection;
	private Collection fRejectedElements;
	
	/**
	 * @param acceptedTypes The types accepted by the validator
	 * @param allowMultipleSelection If set to <code>true</code>, the validator
	 * allows multiple selection.
	 */
	public TypedElementSelectionValidator(Class[] acceptedTypes, boolean allowMultipleSelection) {
		this(acceptedTypes, allowMultipleSelection, null);
	}
	
	/**
	 * @param acceptedTypes The types accepted by the validator
	 * @param allowMultipleSelection If set to <code>true</code>, the validator
	 * allows multiple selection.
	 * @param rejectedElements A list of elements that are not accepted
	 */
	public TypedElementSelectionValidator(Class[] acceptedTypes, boolean allowMultipleSelection, Collection rejectedElements) {
		Assert.isNotNull(acceptedTypes);
		fAcceptedTypes= acceptedTypes;
		fAllowMultipleSelection= allowMultipleSelection;
		fRejectedElements= rejectedElements;
	}	
	
	/*
	 * @see org.eclipse.ui.dialogs.ISelectionValidator#isValid(java.lang.Object)
	 */
	public IStatus validate(Object[] elements) {
		if (isValid(elements)) {
			return fgOKStatus;
		}
		return fgErrorStatus;
	}	

	private boolean isOfAcceptedType(Object o) {
		for (int i= 0; i < fAcceptedTypes.length; i++) {
			if (fAcceptedTypes[i].isInstance(o)) {
				return true;
			}
		}
		return false;
	}
	
	private boolean isRejectedElement(Object elem) {
		return (fRejectedElements != null) && fRejectedElements.contains(elem);
	}
	
	protected boolean isSelectedValid(Object elem) {
		return true;
	}
	
	private boolean isValid(Object[] selection) {
		if (selection.length == 0) {
			return false;
		}
		
		if (!fAllowMultipleSelection && selection.length != 1) {
			return false;
		}
		
		for (int i= 0; i < selection.length; i++) {
			Object o= selection[i];	
			if (!isOfAcceptedType(o) || isRejectedElement(o) || !isSelectedValid(o)) {
				return false;
			}
		}
		return true;
	}
}
