package ctSim.util;

import java.io.IOException;
import java.io.OutputStream;

//$$ doc
public class BackslashNConverterStream extends OutputStream {
	private final OutputStream underlyingStream;
	private final byte[] lineEnding = 
		System.getProperty("line.separator").getBytes();
	
	public BackslashNConverterStream(OutputStream underlyingStream) {
		this.underlyingStream = underlyingStream;
	}
	
	@Override
	public void write(int b) throws IOException {
		if (b == '\n')
			underlyingStream.write(lineEnding);
		else
			underlyingStream.write(b);
	}
}
