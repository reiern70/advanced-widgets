/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

/*
 * wicket-modal.js: registers Wicket.Modal for the Modal of ui-components, which calls
 * Wicket.Modal.attach(overlayId, options) each time it renders open. The behaviors stop, and the
 * focus goes back, when an update of the page removes the overlay, as closing the dialog does.
 */

import { onRemoving } from '../host.js';
import { isModified, MODAL_CLASSES, ModalDialog, type ModalOptions } from '../modal.js';
import { useWicketHost, wicket } from './host.js';

const dialogs = new Map<string, ModalDialog>();

const WicketModal = {

	/**
	 * Gives an open dialog its behaviors.
	 *
	 * @param id the markup id of the overlay
	 * @param options how the dialog behaves; onClose asks the server to close it
	 */
	attach(id: string, options: ModalOptions): void {
		WicketModal.detach(id);
		const overlay = document.getElementById(id);
		if (!overlay) {
			return;
		}
		const title = overlay.querySelector('.' + MODAL_CLASSES.title);
		if (title?.id) {
			overlay.querySelector('.' + MODAL_CLASSES.dialog)?.setAttribute('aria-labelledby',
				title.id);
		}
		const modal = new ModalDialog(overlay, options);
		dialogs.set(id, modal);
		onRemoving(overlay, () => {
			if (dialogs.get(id) === modal) {
				WicketModal.detach(id);
			}
		});
	},

	/**
	 * @param id the markup id of the overlay
	 * @returns the behaviors of the dialog open there, if any
	 */
	of(id: string): ModalDialog | undefined {
		return dialogs.get(id);
	},

	/**
	 * Asks to close a dialog, as its close button does, if canClose lets it.
	 *
	 * @param id the markup id of the overlay
	 * @returns whether the dialog is being closed
	 */
	requestClose(id: string): boolean {
		return dialogs.get(id)?.requestClose() ?? false;
	},

	/** Whether the user changed a field in a container since it was rendered, for canClose. */
	isModified,

	/**
	 * Stops the behaviors of a dialog and gives the focus back.
	 *
	 * @param id the markup id of the overlay
	 */
	detach(id: string): void {
		dialogs.get(id)?.destroy();
		dialogs.delete(id);
	}
};

const Wicket = wicket();
if (Wicket.Modal === undefined) {
	useWicketHost();
	Wicket.Modal = WicketModal;
}
