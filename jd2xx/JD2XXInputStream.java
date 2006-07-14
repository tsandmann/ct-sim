/*
	Copyright (c) 2004 Pablo Bleyer Kocik.

	Redistribution and use in source and binary forms, with or without
	modification, are permitted provided that the following conditions are met:

	1. Redistributions of source code must retain the above copyright notice, this
	list of conditions and the following disclaimer.

	2. Redistributions in binary form must reproduce the above copyright notice,
	this list of conditions and the following disclaimer in the documentation
	and/or other materials provided with the distribution.

	3. The name of the author may not be used to endorse or promote products
	derived from this software without specific prior written permission.

	THIS SOFTWARE IS PROVIDED BY THE AUTHOR "AS IS" AND ANY EXPRESS OR IMPLIED
	WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
	MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO
	EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
	SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
	PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
	BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER
	IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
	ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
	POSSIBILITY OF SUCH DAMAGE.
*/

package jd2xx;

import java.io.InputStream;
import java.io.IOException;

public class JD2XXInputStream extends InputStream {

	public JD2XX jd2xx = null;

	public JD2XXInputStream() {
	// TODO
	}

	public JD2XXInputStream(JD2XX j) {
		this.jd2xx = j;
	}

	public JD2XXInputStream(int dn) throws IOException {
		this.jd2xx = new JD2XX(dn);
	}

	public JD2XXInputStream(String dn, int f) throws IOException {
		this.jd2xx = new JD2XX(dn, f);
	}

	public JD2XXInputStream(int n, int f) throws IOException {
		this.jd2xx = new JD2XX(n, f);
	}

	/** 
	 * @see java.io.Closeable#close()
	 */
	@Override
	public void close(){
		// jd2xx.close();
		this.jd2xx = null;
	}

	public int read() throws IOException {
		return this.jd2xx.read();
	}

	public int read(byte[] b) throws IOException {
		return this.jd2xx.read(b);
	}
}
