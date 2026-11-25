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

import { useState } from 'react';

import { ResumableUploader, type ResumableItem } from '@antilia/react';

import { receive, RECEIVED_MAX, sizeOf, type Receipt,
	type ResumableUploadConfig } from '../resumable-upload.js';

/**
 * The Resumable upload demo in React: the uploader of the core sending to the tus resource of the
 * page, two files at once, and the files the server received whole, as it tells them.
 */
export function ResumableUploadDemo({ config }: { config: ResumableUploadConfig }) {
	const { texts } = config;
	const [received, setReceived] = useState<Receipt[]>([]);
	const [failed, setFailed] = useState<string | null>(null);

	const completed = async (item: ResumableItem) => {
		const receipt = item.storedName ? await receive(config, item.storedName, item.name) : null;
		if (receipt) {
			setFailed(null);
			setReceived((before) => [receipt, ...before].slice(0, RECEIVED_MAX));
		} else {
			setFailed(`${texts['received.failed']}: ${item.name}`);
		}
	};

	return (
		<div className="resumable-demo">
			<ResumableUploader className="resumable-demo__uploads" endpoint={config.endpoint}
				query={config.query}
				parallel={2} pollMillis={1000} chunkSize={config.chunkSize} fileMaxSize={config.fileMaxSize}
				locale={config.locale} texts={config.uploaderTexts}
				pickerTexts={config.pickerTexts} onComplete={completed} />
			<section className="resumable-demo__received">
				<h3>{texts.received}</h3>
				{failed && <p className="resumable-demo__hint" role="alert">{failed}</p>}
				{received.length ? (
					<table className="resumable-demo__table">
						<thead>
							<tr>
								<th>{texts['received.name']}</th>
								<th>{texts['received.size']}</th>
								<th>{texts['received.sha256']}</th>
							</tr>
						</thead>
						<tbody>
							{received.map((receipt, index) => (
								<tr key={receipt.sha256 + index}>
									<td>{receipt.name}</td>
									<td className="resumable-demo__number">
										{sizeOf(receipt.size, config.locale)}</td>
									<td className="resumable-demo__digest">{receipt.sha256}</td>
								</tr>
							))}
						</tbody>
					</table>
				) : <p className="resumable-demo__hint">{texts['received.none']}</p>}
			</section>
		</div>
	);
}
