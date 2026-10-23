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

/** A message of the app, of a level as Wicket's feedback messages have it. */
export interface Message {
	level: 'info' | 'error';
	text: string;
}

/**
 * The messages of the app, rendered as Wicket's FeedbackPanel renders them, so that the page's
 * style sheet shows them the same.
 */
export function Feedback({ messages }: { messages: Message[] }) {
	if (!messages.length) {
		return null;
	}
	return (
		<ul className="feedbackPanel">
			{messages.map((message, index) => {
				const level = 'feedbackPanel' + message.level.toUpperCase();
				return <li key={index} className={level}><span className={level}>{message.text}</span></li>;
			})}
		</ul>
	);
}
