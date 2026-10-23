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
package com.antiliacode.widgets.demo.data;

import java.time.Instant;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * The {@link Access}es in the table {@code accesses}, also filtered, see {@link AccessFilter}; and
 * the two steps moving the old ones to {@code old_accesses}, run in one transaction by
 * {@link Accesses#archive(Instant)}.
 */
public interface IAccessRepository
	extends
		JpaRepository<Access, Long>,
		JpaSpecificationExecutor<Access>
{
	/**
	 * Copies the accesses before a time to the old accesses, with their ids, but those copied
	 * already.
	 *
	 * @param before
	 *            the time on the server's clock
	 * @return how many were copied
	 */
	@Modifying
	@Query(nativeQuery = true, value = "INSERT INTO old_accesses (id, session_id, user_name, " +
		"private_part, address, user_agent, accessed, time_zone, local_time) " +
		"SELECT id, session_id, user_name, private_part, address, user_agent, accessed, time_zone, " +
		"local_time FROM accesses a WHERE a.accessed < :before " +
		"AND NOT EXISTS (SELECT 1 FROM old_accesses o WHERE o.id = a.id)")
	int copyToOldAccesses(@Param("before") Instant before);

	/**
	 * Removes the accesses before a time that are among the old accesses, which keep them.
	 *
	 * @param before
	 *            the time on the server's clock
	 * @return how many were removed
	 */
	@Modifying
	@Query(nativeQuery = true, value = "DELETE FROM accesses WHERE accessed < :before " +
		"AND id IN (SELECT id FROM old_accesses)")
	int removeCopiedToOldAccesses(@Param("before") Instant before);
}
