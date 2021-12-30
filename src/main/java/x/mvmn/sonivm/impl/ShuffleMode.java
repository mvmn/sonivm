package x.mvmn.sonivm.impl;

public enum ShuffleMode {
	OFF, PLAYLIST, ARTIST, ALBUM, GENRE;

	public String toString() {
		String name = this.name();

		return name.substring(0, 1) + name.substring(1).toLowerCase();
	}
}
