package org.korsakow.services.encoders.video;

/**
 * A non-comprehensive list of codecs. In a practical sense this should only cover formats we need to single out. 
 * @author d
 *
 */
public enum VideoCodec
{
	H264("mp4"),
	FLV("flv"),
	JPG("jpg"), // used for exporting single frames
	;

	private String fileExtension;
	VideoCodec(String fileExtension) {
		this.fileExtension = fileExtension;
	}
	
	public String getFileExtension() { return fileExtension; }
}
