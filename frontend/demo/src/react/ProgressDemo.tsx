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

import { useEffect, useState, type MouseEvent } from 'react';

import { ProgressBar } from '@antilia/react';

import { advance, TICK_MILLIS, type ProgressConfig } from '../progress.js';

/**
 * The Progress bar demo in React: the bars of the Wicket page, the first one advanced by a timer of
 * the app.
 */
export function ProgressDemo({ config }: { config: ProgressConfig }) {
	const { texts } = config;
	const [progress, setProgress] = useState(config.progress);
	const [running, setRunning] = useState(false);

	useEffect(() => {
		if (!running) {
			return;
		}
		const timer = window.setInterval(() => setProgress(advance), TICK_MILLIS);
		return () => window.clearInterval(timer);
	}, [running]);

	useEffect(() => {
		if (progress >= 100) {
			setRunning(false);
		}
	}, [progress]);

	const toggle = (event: MouseEvent) => {
		event.preventDefault();
		setRunning(!running && progress < 100);
	};
	const reset = (event: MouseEvent) => {
		event.preventDefault();
		setProgress(0);
		setRunning(false);
	};

	return (
		<div>
			<h3>{texts.known}</h3>
			<ProgressBar value={progress} />
			<p>
				<a href="#" onClick={toggle}>{running ? texts.pause : texts.start}</a>
				{' | '}
				<a href="#" onClick={reset}>{texts.reset}</a>
			</p>
			<h3>{texts.finished}</h3>
			<ProgressBar value={100} />
			<h3>{texts.unknown}</h3>
			<ProgressBar />
		</div>
	);
}
