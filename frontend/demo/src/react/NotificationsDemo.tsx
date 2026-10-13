/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

import { useState, type FormEvent } from 'react';

import { Button, Dropdown, NotificationsProvider, useNotifications,
	type NotificationPosition } from '@antilia/react';
import { NOTIFICATION_POSITIONS } from '@antilia/core';

import { dropdownTexts } from '../data.js';
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
						<Dropdown name="type" value={type} options={DEMO_TYPES.map((value) =>
							({ value, label: texts.types[value] }))}
							onChange={(value) => setType(value as DemoType)}
							{...dropdownTexts(texts)} />
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
						<Dropdown name="position" value={position}
							options={NOTIFICATION_POSITIONS.map((value) =>
								({ value, label: texts.positions[value] }))}
							onChange={(value) => setPosition(value as NotificationPosition)}
							{...dropdownTexts(texts)} />
					</label>
					<label>{texts.timeout}
						<Dropdown name="timeout" value={String(timeout)}
							options={DEMO_TIMEOUTS.map((value) =>
								({ value: String(value), label: texts.timeouts[value] }))}
							onChange={(value) => setTimeoutChoice(Number(value))}
							{...dropdownTexts(texts)} />
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
