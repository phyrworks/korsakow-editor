package org.korsakow.services.encoders.sound;

public enum SoundFormat
{
	MP3("mp3"),
	WAV("wav"),
	AIF("aif"),
	;
	
	private String fileExtension;
	SoundFormat(String fileExtension) {
		this.fileExtension = fileExtension;
	}
	
	public String getFileExtension() { return fileExtension; }
}
