/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

export interface ProgressBarProps {
	/** The percentage, clamped to 0..100; null or left out for an indeterminate bar. */
	value?: number | null;
	/** CSS classes of the bar, next to wicket-progress-bar. */
	className?: string;
}

/**
 * A progress bar in the markup of the Wicket one, a native progress element under a label, shown
 * by the core's style sheet progress-bar.css: striped in the theme's colors, the stripes moving
 * unless the system asks for reduced motion.
 */
export function ProgressBar({ value, className }: ProgressBarProps) {
	const percentage = value == null ? null : Math.max(0, Math.min(100, Math.round(value)));
	return (
		<div className={['wicket-progress-bar', className].filter(Boolean).join(' ')}>
			{percentage === null ? <progress className="wicket-progress-bar-value" max={100} /> : (
				<>
					<progress className="wicket-progress-bar-value" max={100} value={percentage}>
						{percentage}%
					</progress>
					<span className="wicket-progress-bar-label" aria-hidden="true">{percentage}%</span>
				</>
			)}
		</div>
	);
}
