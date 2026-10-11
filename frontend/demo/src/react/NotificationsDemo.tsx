/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

import { useState, type FormEvent } from 'react';

import { Button, NotificationsProvider, useNotifications,
	type NotificationPosition } from '@antilia/react';
import { NOTIFICATION_POSITIONS } from '@antilia/core';

import { DEMO_TIMEOUTS, DEMO_TYPES, notification, saved, type DemoType,
	type NotificationsConfig } from '../notifications.js';

/**
 * The Notifications demo in React: the forms of the Wicket page, showing their notifications with
 * useNotifications.
 */
export function NotificationsDemo({ config }: { config: NotificationsConfig }) {
	return (
		<NotificationsProvider closeLabel={config.texts.close} label={config.texts.label}>
			<Forms config={config} />
		</NotificationsProvider>
	);
}

function Forms({ config }: { config: NotificationsConfig }) {
	const { texts } = config;
	const notifications = useNotifications();
	const [type, setType] = useState<DemoType>(config.type);
	const [title, setTitle] = useState(config.title);
	const [message, setMessage] = useState(config.message);
	const [html, setHtml] = useState(config.html);
	const [position, setPosition] = useState<NotificationPosition>(config.position);
	const [timeout, setTimeoutChoice] = useState(config.timeout);
	const [name, setName] = useState('');

	const show = (event: FormEvent) => {
		event.preventDefault();
		notifications.show(notification({ type, title, message, html, position, timeout }));
	};
	const save = (event: FormEvent) => {
		event.preventDefault();
		notifications.show(saved(texts, name));
	};

	return (
		<div className="notifications-demo">
			<section>
				<h3>{texts.showTitle}</h3>
				<form className="notifications-demo__form" onSubmit={show}>
					<label>{texts.type}
						<select name="type" value={type}
							onChange={(event) => setType(event.currentTarget.value as DemoType)}>
							{DEMO_TYPES.map((value) =>
								<option key={value} value={value}>{texts.types[value]}</option>)}
						</select>
					</label>
					<label>{texts.title}
						<input type="text" name="title" value={title}
							onChange={(event) => setTitle(event.currentTarget.value)} />
					</label>
					<label className="notifications-demo__wide">{texts.message}
						<textarea name="message" rows={2} value={message}
							onChange={(event) => setMessage(event.currentTarget.value)} />
					</label>
					<label className="notifications-demo__check">
						<input type="checkbox" name="html" checked={html}
							onChange={(event) => setHtml(event.currentTarget.checked)} />
						{texts.html}
					</label>
					<label>{texts.position}
						<select name="position" value={position} onChange={(event) =>
							setPosition(event.currentTarget.value as NotificationPosition)}>
							{NOTIFICATION_POSITIONS.map((value) =>
								<option key={value} value={value}>{texts.positions[value]}</option>)}
						</select>
					</label>
					<label>{texts.timeout}
						<select name="timeout" value={timeout}
							onChange={(event) => setTimeoutChoice(Number(event.currentTarget.value))}>
							{DEMO_TIMEOUTS.map((value) =>
								<option key={value} value={value}>{texts.timeouts[value]}</option>)}
						</select>
					</label>
					<div className="notifications-demo__buttons">
						<Button type="submit" variant="primary" label={texts.show}
							className="notifications-demo__show" />
						<Button label={texts.closeAll} className="notifications-demo__close-all"
							onClick={() => notifications.closeAll()} />
					</div>
				</form>
			</section>
			<section>
				<h3>{texts.feedbackTitle}</h3>
				<form className="notifications-demo__form" onSubmit={save}>
					<label>{texts.name}
						<input type="text" name="name" value={name}
							onChange={(event) => setName(event.currentTarget.value)} />
					</label>
					<div className="notifications-demo__buttons">
						<Button type="submit" label={texts.save} className="notifications-demo__save" />
					</div>
				</form>
			</section>
		</div>
	);
}
