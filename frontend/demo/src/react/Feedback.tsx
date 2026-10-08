/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
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
