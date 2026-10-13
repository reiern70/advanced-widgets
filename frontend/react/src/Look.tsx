/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

import { createContext, useContext, type ReactNode } from 'react';

import { ANTILIA_LOOK, lookNamed, type Look } from '@antilia/core';

export type { Look, LookPart } from '@antilia/core';

const LookContext = createContext<Look>(ANTILIA_LOOK);

export interface LookProviderProps {
	/** The look, or its name: 'antilia' or 'bootstrap', or that of a look registered. */
	look: Look | string;
	children?: ReactNode;
}

/**
 * Gives the components inside it a look, such as Bootstrap's, whose classes their parts then get;
 * without one they are in the Antilia look.
 */
export function LookProvider({ look, children }: LookProviderProps) {
	return (
		<LookContext.Provider value={typeof look === 'string' ? lookNamed(look) : look}>
			{children}
		</LookContext.Provider>
	);
}

/**
 * @returns the look the component is in, given by the closest LookProvider; the Antilia look
 * without one
 */
export function useLook(): Look {
	return useContext(LookContext);
}
