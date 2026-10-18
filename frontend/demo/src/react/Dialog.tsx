/*
 * Copyright 2026 Ernesto Reinaldo Barreiro (reiern70, reiern70@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import type { ReactNode } from 'react';

import { Modal } from '@antilia/react';

/**
 * The dialog of the demos: an open Modal, which behaves and looks like Wicket's ModalDialog with
 * its default theme, as the Wicket pages open theirs: Escape closes it, and it keeps the focus
 * while open.
 */
export function Dialog({ title, onClose, children }: { title: string; onClose(): void;
	children: ReactNode }) {
	return <Modal open title={title} onClose={onClose}>{children}</Modal>;
}
