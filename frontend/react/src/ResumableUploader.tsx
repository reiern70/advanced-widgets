/*
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

import { forwardRef, useImperativeHandle, useLayoutEffect, useRef } from 'react';

import { ResumableUploader as CoreResumableUploader, type ResumableItem,
	type ResumableUploaderOptions, type TusError } from '@antilia/core';

import { useLook } from './Look.js';

export interface ResumableUploaderProps extends Omit<ResumableUploaderOptions,
	'look' | 'onComplete' | 'onError' | 'onChange' | 'metadata'> {
	/** What the server is told of a file besides its name and type. */
	metadata?(file: File): Record<string, string>;
	/** Called once the server holds the whole of a file: its address tells the server which. */
	onComplete?(item: ResumableItem): void;
	/** Called once a file failed for good. */
	onError?(item: ResumableItem, error: TusError): void;
	/** Called when a file is added, taken out or changes state, with all files. */
	onChange?(items: ResumableItem[]): void;
	id?: string;
	/** CSS classes of the element the uploader draws into. */
	className?: string;
}

/**
 * What a ref to {@link ResumableUploader} can do.
 */
export interface ResumableUploaderHandle {
	/** The uploader of the core, null before it is mounted. */
	readonly uploader: CoreResumableUploader | null;
	/** Adds files, as if chosen in the picker. */
	add(files: File[]): void;
	/** Pauses every file being sent or waiting for its turn. */
	pauseAll(): void;
	/** Starts or resumes every file paused, failed or waiting for its start button. */
	resumeAll(): void;
}

/**
 * The resumable uploader of the core: files sent in chunks to a tus server, each paused, resumed or
 * canceled by the user, going on after a broken connection or a reload without sending again what
 * the server holds. React renders the element, the core draws the picker and a row of each file
 * into it, in the look of the closest LookProvider. A new endpoint makes a new uploader; other
 * props change the one there is, the files being sent going on as they started. A ref gets a
 * {@link ResumableUploaderHandle}.
 */
export const ResumableUploader = forwardRef<ResumableUploaderHandle, ResumableUploaderProps>(
	function ResumableUploader(props, ref) {
		const { id, className, endpoint } = props;
		const look = useLook();
		const element = useRef<HTMLDivElement>(null);
		const instance = useRef<CoreResumableUploader | null>(null);
		const latest = useRef(props);
		latest.current = props;

		const options = (): ResumableUploaderOptions => {
			const { id: _id, className: _className, metadata, onComplete, onError, onChange,
				...rest } = latest.current;
			return { ...rest, look,
				metadata: metadata && ((file) => latest.current.metadata?.(file) ?? {}),
				onComplete: (item) => latest.current.onComplete?.(item),
				onError: (item, error) => latest.current.onError?.(item, error),
				onChange: (items) => latest.current.onChange?.(items) };
		};

		useImperativeHandle(ref, () => ({
			get uploader() {
				return instance.current;
			},
			add: (files) => instance.current?.add(files),
			pauseAll: () => instance.current?.pauseAll(),
			resumeAll: () => instance.current?.resumeAll()
		}), []);

		useLayoutEffect(() => {
			const core = new CoreResumableUploader(element.current!, options());
			instance.current = core;
			return () => {
				core.destroy();
				instance.current = null;
			};
		}, [endpoint]);

		const { metadata: _metadata, onComplete: _onComplete, onError: _onError,
			onChange: _onChange, ...settings } = props;
		const key = JSON.stringify(settings);
		const configured = useRef(key + look.name);
		useLayoutEffect(() => {
			if (configured.current !== key + look.name) {
				configured.current = key + look.name;
				instance.current?.configure(options());
			}
		}, [key, look]);

		return <div ref={element} id={id} className={className} />;
	});
