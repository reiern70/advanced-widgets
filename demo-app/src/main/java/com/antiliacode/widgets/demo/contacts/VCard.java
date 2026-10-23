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
package com.antiliacode.widgets.demo.contacts;

/**
 * Writes a contact as a vCard 4.0 (RFC 6350), the file address books exchange contacts in.
 */
final class VCard
{
	private VCard()
	{
	}

	/**
	 * @return the vCard of the contact: name, email, phone, address, birthday and profession,
	 *         with lines ending in CR LF
	 */
	static String of(Contact contact)
	{
		return String.join("\r\n", "BEGIN:VCARD", "VERSION:4.0",
			"FN:" + text(contact.getName()),
			"N:" + text(contact.getLastName()) + ";" + text(contact.getFirstName()) + ";;;",
			"EMAIL:" + text(contact.getEmail()),
			"TEL;VALUE=uri:tel:" + contact.getPhone().replace(' ', '-'),
			"ADR:;;;" + text(contact.getCity()) + ";;;" +
				text(contact.getCountryName()),
			"BDAY:" + contact.getBirthDate().toString().replace("-", ""),
			"TITLE:" + text(contact.getProfessionName()),
			"END:VCARD", "");
	}

	/**
	 * @return the text with the characters vCard gives a meaning escaped
	 */
	private static String text(String value)
	{
		return value.replace("\\", "\\\\").replace(",", "\\,").replace(";", "\\;")
			.replace("\n", "\\n");
	}
}
