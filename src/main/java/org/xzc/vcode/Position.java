package org.xzc.vcode;

import java.io.File;
import java.nio.charset.Charset;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

public class Position implements IPosition {

	private static final Logger log = Logger.getLogger( Position.class );
	private static final Charset UTF8 = Charset.forName( "utf8" );
	private IWorker worker;
	//private String hint;
	private String tag;
	private File file;
	private boolean save;

	public Position(String tag, File file, boolean save) {
		this.tag = tag;
		this.file = file;
		this.save = save;
	}

	public void bindWorker(IWorker worker) {
		try {
			this.worker = worker;
			if (save) {
				byte[] data = worker.getVCodeData();
				FileUtils.writeByteArrayToFile( file, data );
			}
		} catch (Exception e) {
			throw new RuntimeException( e );
		}
		//hint = new String( data, UTF8 );
	}

	public void process() {
		worker.process( tag );
	}

	public void unbindWorker() {
		worker = null;
	}

	public String getTag() {
		return tag;
	}

	public void reject() {
		worker.onReject();
	}

}
