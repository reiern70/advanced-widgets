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

/* the grammar of a log, plugged in by a page: its levels are keywords */
Wicket.CodeEditor.registerLanguage('log', function (kit) {
	return kit.StreamLanguage.define({
		token: function (stream) {
			if (stream.match(/^(ERROR|WARN|INFO)\b/)) {
				return 'keyword';
			}
			stream.next();
			return null;
		}
	});
});
