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
package com.antiliacode.widgets.throttle;

import java.io.IOException;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

/**
 * How fast the server reads the files a session sends, as a slow network would bring them: at
 * full speed, or slowed down to a rate in bytes per second, so that the progress of an upload can
 * be watched on a fast network. See {@link Throttle}.
 *
 * @since 11.0.0
 */
public enum UploadSpeed
{
	/** As fast as the network goes. */
	FULL(0),
	/** A megabyte per second. */
	MEGABYTE(1024 * 1024),
	/** 256 kilobytes per second. */
	QUARTER_MEGABYTE(256 * 1024),
	/** 64 kilobytes per second. */
	SLOW(64 * 1024);

	private final long bytesPerSecond;

	UploadSpeed(long bytesPerSecond)
	{
		this.bytesPerSecond = bytesPerSecond;
	}

	/**
	 * @return the bytes read per second, 0 for as fast as the network goes
	 */
	public long getBytesPerSecond()
	{
		return bytesPerSecond;
	}

	/**
	 * A request whose body is read at a rate.
	 */
	static final class SlowRequest extends HttpServletRequestWrapper
	{
		private final long bytesPerSecond;

		private ServletInputStream body;

		SlowRequest(HttpServletRequest request, long bytesPerSecond)
		{
			super(request);
			this.bytesPerSecond = bytesPerSecond;
		}

		@Override
		public ServletInputStream getInputStream() throws IOException
		{
			if (body == null)
			{
				body = new SlowInputStream(super.getInputStream(), bytesPerSecond);
			}
			return body;
		}
	}

	/**
	 * A stream read at a rate: in chunks of a twentieth of a second's bytes, each waiting until
	 * the rate allows it.
	 */
	static final class SlowInputStream extends ServletInputStream
	{
		private final ServletInputStream in;

		private final long bytesPerSecond;

		private final long chunk;

		private final long started = System.nanoTime();

		private long read;

		SlowInputStream(ServletInputStream in, long bytesPerSecond)
		{
			this.in = in;
			this.bytesPerSecond = bytesPerSecond;
			chunk = Math.max(1, bytesPerSecond / 20);
		}

		private void pace() throws IOException
		{
			long due = started + read * 1_000_000_000L / bytesPerSecond;
			long wait = due - System.nanoTime();
			if (wait > 0)
			{
				try
				{
					Thread.sleep(wait / 1_000_000L, (int)(wait % 1_000_000L));
				}
				catch (InterruptedException e)
				{
					Thread.currentThread().interrupt();
					throw new IOException("interrupted while reading slowly", e);
				}
			}
		}

		@Override
		public int read() throws IOException
		{
			pace();
			int b = in.read();
			if (b >= 0)
			{
				read++;
			}
			return b;
		}

		@Override
		public int read(byte[] buffer, int offset, int length) throws IOException
		{
			pace();
			int count = in.read(buffer, offset, (int)Math.min(length, chunk));
			if (count > 0)
			{
				read += count;
			}
			return count;
		}

		@Override
		public boolean isFinished()
		{
			return in.isFinished();
		}

		@Override
		public boolean isReady()
		{
			return in.isReady();
		}

		@Override
		public void setReadListener(ReadListener listener)
		{
			in.setReadListener(listener);
		}

		@Override
		public void close() throws IOException
		{
			in.close();
		}
	}
}
