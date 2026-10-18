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
