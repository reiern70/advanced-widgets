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
package com.antiliacode.widgets.demo.data;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * The {@link OldAccess}es in the table {@code old_accesses}, also filtered, see
 * {@link AccessFilter}.
 */
public interface IOldAccessRepository
	extends
		JpaRepository<OldAccess, Long>,
		JpaSpecificationExecutor<OldAccess>
{
	/**
	 * @param userName
	 *            the name of an account
	 * @return how many of these accesses are the account's
	 */
	long countByUserName(String userName);

	/**
	 * @param userName
	 *            the name of an account
	 * @return how many of these accesses are the account's and opened the demos
	 */
	long countByUserNameAndPrivatePartTrue(String userName);

	/**
	 * Removes the accesses of an account, when it is deleted for good.
	 *
	 * @param userName
	 *            the name of the account
	 * @return how many were removed
	 */
	@Modifying
	@Query("DELETE FROM OldAccess a WHERE a.userName = :userName")
	int removeByUserName(@Param("userName") String userName);
}
